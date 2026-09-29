// DRS AI — JNI bridge for llama.cpp mtmd multimodal (libdrs_vision_jni.so)
#include "ggml-backend.h"
#include "llama.h"
#include "ggml.h"
#include "mtmd.h"
#include "mtmd-helper.h"

#include <jni.h>
#include <android/log.h>
#include <chrono>
#include <cstring>
#include <map>
#include <memory>
#include <mutex>
#include <mutex>

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "drs-vision", __VA_ARGS__)

namespace {

struct VisionCtx {
    llama_model*   model    = nullptr;
    llama_context* lctx     = nullptr;
    mtmd_context*  mtmd     = nullptr;
    int            threads  = 2;
    std::string    last_error;
    std::mutex     mtx;
};

std::mutex g_vision_mtx;
std::map<long, std::unique_ptr<VisionCtx>> g_visions;
long g_next_id = 1;
VisionCtx* g_last_err_ctx = nullptr; // error surfaced via nativeVisionLastError

VisionCtx* find_vision(long id) {
    std::lock_guard<std::mutex> lock(g_vision_mtx);
    auto it = g_visions.find(id);
    return it == g_visions.end() ? nullptr : it->second.get();
}

std::string to_string(JNIEnv* env, jstring js) {
    if (!js) return "";
    const char* chars = env->GetStringUTFChars(js, nullptr);
    std::string s(chars ? chars : "");
    if (chars) env->ReleaseStringUTFChars(js, chars);
    return s;
}

jstring to_jstring(JNIEnv* env, const std::string& s) {
    return env->NewStringUTF(s.c_str());
}

class JniSink {
public:
    JniSink(JNIEnv* env, jobject sink) : env_(env), sink_(sink), mid_(nullptr) {
        if (sink) {
            jclass cls = env->GetObjectClass(sink);
            mid_ = env->GetMethodID(cls, "onToken", "(Ljava/lang/String;)Z");
            env_->DeleteLocalRef(cls);
        }
    }
    bool valid() const { return sink_ && mid_; }
    bool on_token(const std::string& piece) {
        if (!valid()) return true;
        jstring js = env_->NewStringUTF(piece.c_str());
        if (!js) return false;
        jboolean cont = env_->CallBooleanMethod(sink_, mid_, js);
        env_->DeleteLocalRef(js);
        if (env_->ExceptionCheck()) { env_->ExceptionClear(); return false; }
        return cont == JNI_TRUE;
    }
private:
    JNIEnv* env_;
    jobject sink_;
    jmethodID mid_;
};

} // namespace

extern "C" {

// Registers dynamic backends (CPU/Vulkan) from the APK lib dir + llama init.
// Self-contained: vision does not depend on libdrs_core_jni.so.
JNIEXPORT jstring JNICALL
Java_com_drs_ai_core_vision_VisionNative_nativeInitBackends(JNIEnv* env, jobject, jstring backendDir) {
    static std::once_flag init_once;
    std::string dir = to_string(env, backendDir);
    std::call_once(init_once, [&dir] {
        ggml_backend_load_all_from_path(dir.c_str());
        llama_backend_init();
    });
    return nullptr;
}

JNIEXPORT jlong JNICALL
Java_com_drs_ai_core_vision_VisionNative_nativeVisionCreate(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lock(g_vision_mtx);
    long id = g_next_id++;
    g_visions[id] = std::make_unique<VisionCtx>();
    return id;
}

JNIEXPORT void JNICALL
Java_com_drs_ai_core_vision_VisionNative_nativeVisionDestroy(JNIEnv*, jobject, jlong handle) {
    std::lock_guard<std::mutex> lock(g_vision_mtx);
    auto it = g_visions.find(handle);
    if (it == g_visions.end()) return;
    auto* v = it->second.get();
    std::lock_guard<std::mutex> vlock(v->mtx);
    if (v->mtmd)  mtmd_free(v->mtmd);
    if (v->lctx)  llama_free(v->lctx);
    if (v->model) llama_model_free(v->model);
    g_visions.erase(it);
}

JNIEXPORT jstring JNICALL
Java_com_drs_ai_core_vision_VisionNative_nativeVisionLoad(JNIEnv* env, jobject, jlong handle,
        jstring chatModelPath, jstring mmprojPath, jint nCtx, jint nThreads) {
    auto* v = find_vision(handle);
    if (!v) return to_jstring(env, "invalid vision handle");
    std::lock_guard<std::mutex> vlock(v->mtx);

    std::string chat = to_string(env, chatModelPath);
    std::string proj = to_string(env, mmprojPath);
    v->threads = std::max(1, (int) nThreads);

    static std::once_flag backend_once;
    std::call_once(backend_once, [] { llama_backend_init(); });
    llama_model_params mp = llama_model_default_params();
    mp.use_mmap = true;
    mp.n_gpu_layers = 0;
    v->model = llama_model_load_from_file(chat.c_str(), mp);
    if (!v->model) {
        v->last_error = "failed to load chat model: " + chat;
        return to_jstring(env, v->last_error);
    }

    llama_context_params cp = llama_context_default_params();
    cp.n_ctx           = (uint32_t) std::max(1024, nCtx);
    cp.n_batch         = 256;
    cp.n_ubatch        = 256;
    cp.n_threads       = v->threads;
    cp.n_threads_batch = v->threads;
    cp.embeddings      = false;
    v->lctx = llama_init_from_model(v->model, cp);
    if (!v->lctx) {
        llama_model_free(v->model); v->model = nullptr;
        v->last_error = "failed to create vision llama_context";
        return to_jstring(env, v->last_error);
    }

    mtmd_context_params mparams = mtmd_context_params_default();
    mparams.use_gpu       = false;
    mparams.print_timings = false;
    mparams.n_threads     = v->threads;
    mparams.verbosity     = GGML_LOG_LEVEL_ERROR;
    v->mtmd = mtmd_init_from_file(proj.c_str(), v->model, mparams);
    if (!v->mtmd) {
        llama_free(v->lctx); v->lctx = nullptr;
        llama_model_free(v->model); v->model = nullptr;
        v->last_error = "failed to init mtmd from mmproj: " + proj;
        return to_jstring(env, v->last_error);
    }
    return nullptr;
}

JNIEXPORT jstring JNICALL
Java_com_drs_ai_core_vision_VisionNative_nativeVisionGenerate(JNIEnv* env, jobject, jlong handle,
        jstring question, jbyteArray jpeg, jint maxTokens, jfloat temperature, jobject sinkObj) {
    auto* v = find_vision(handle);
    if (!v) return to_jstring(env, "invalid vision handle");
    std::lock_guard<std::mutex> vlock(v->mtx);
    if (!v->mtmd || !v->lctx || !v->model) return to_jstring(env, "vision models not loaded");

    std::string q = to_string(env, question);
    jsize len = env->GetArrayLength(jpeg);
    std::vector<uint8_t> img(len);
    env->GetByteArrayRegion(jpeg, 0, len, reinterpret_cast<jbyte*>(img.data()));

    // 1) bitmap from JPEG buffer
    mtmd_bitmap* bitmap = mtmd_helper_bitmap_init_from_buf(v->mtmd, img.data(), (size_t) len);
    if (!bitmap) {
        v->last_error = "cannot decode image (mtmd)";
        return to_jstring(env, v->last_error);
    }

    // 2) prompt: question + default media marker
    std::string text = q;
    if (!text.empty()) text += " ";
    text += mtmd_default_marker();
    mtmd_input_text input{ text.c_str(), /*add_special*/ false, /*parse_special*/ true };

    mtmd_input_chunks* chunks = mtmd_input_chunks_init();
    if (!chunks) { mtmd_bitmap_free(bitmap); return to_jstring(env, "chunks init failed"); }

    int32_t res = mtmd_tokenize(v->mtmd, chunks, &input, (const mtmd_bitmap**) &bitmap, 1);
    mtmd_bitmap_free(bitmap);
    if (res != 0) {
        mtmd_input_chunks_free(chunks);
        v->last_error = "mtmd_tokenize failed (code " + std::to_string(res) + ")";
        return to_jstring(env, v->last_error);
    }

    // 3) eval media + prompt
    llama_pos n_past = 0;
    res = mtmd_helper_eval_chunks(v->mtmd, v->lctx, chunks, 0, 0, 256, /*logits_last*/ true, &n_past);
    mtmd_input_chunks_free(chunks);
    if (res != 0) {
        v->last_error = "mtmd eval failed (code " + std::to_string(res) + ")";
        return to_jstring(env, v->last_error);
    }

    // 4) sampling loop
    llama_sampler_chain_params sparams = llama_sampler_chain_default_params();
    sparams.no_perf = true;
    llama_sampler* chain = llama_sampler_chain_init(sparams);
    llama_sampler_chain_add(chain, llama_sampler_init_top_k(40));
    llama_sampler_chain_add(chain, llama_sampler_init_top_p(0.95f, 1));
    llama_sampler_chain_add(chain, llama_sampler_init_temp(std::max(0.05f, temperature)));
    llama_sampler_chain_add(chain, llama_sampler_init_dist(
        (uint32_t)(std::chrono::steady_clock::now().time_since_epoch().count() & 0xFFFFFFFFu)));

    JniSink sink(env, sinkObj);
    const llama_vocab* vocab = llama_model_get_vocab(v->model);
    char buf[256];
    llama_batch batch = llama_batch_init(1, 0, 1);
    std::string error;

    for (int32_t i = 0; i < maxTokens; ++i) {
        llama_token next = llama_sampler_sample(chain, v->lctx, -1);
        if (llama_vocab_is_eog(vocab, next)) break;
        int n = llama_token_to_piece(vocab, next, buf, sizeof(buf), 0, 0);
        if (n > 0) {
            std::string piece(buf, (size_t) n);
            if (sink.valid() && !sink.on_token(piece)) break;
        }
        batch.n_tokens = 0;
        batch.token[0] = next;
        batch.pos[0]   = n_past;
        batch.n_seq_id[0] = 1;
        batch.seq_id[0][0] = 0;
        batch.logits[0] = 1;
        batch.n_tokens = 1;
        if (llama_decode(v->lctx, batch) != 0) { error = "vision decode failed"; break; }
        n_past++;
    }
    llama_batch_free(batch);
    llama_sampler_free(chain);

    // reset KV for next image
    llama_memory_seq_rm(llama_get_memory(v->lctx), 0, 0, -1);

    if (!error.empty()) return to_jstring(env, error);
    return nullptr;
}

JNIEXPORT void JNICALL
Java_com_drs_ai_core_vision_VisionNative_nativeVisionReset(JNIEnv*, jobject, jlong handle) {
    auto* v = find_vision(handle);
    if (!v) return;
    std::lock_guard<std::mutex> vlock(v->mtx);
    if (v->lctx) llama_memory_seq_rm(llama_get_memory(v->lctx), 0, 0, -1);
}

JNIEXPORT jstring JNICALL
Java_com_drs_ai_core_vision_VisionNative_nativeVisionLastError(JNIEnv* env, jobject, jlong handle) {
    auto* v = find_vision(handle);
    if (!v || v->last_error.empty()) return nullptr;
    return to_jstring(env, v->last_error);
}

JNIEXPORT jstring JNICALL
Java_com_drs_ai_core_vision_VisionNative_nativeVisionLibraryVersion(JNIEnv* env, jobject) {
    return to_jstring(env, ggml_version());
}

} // extern "C"
