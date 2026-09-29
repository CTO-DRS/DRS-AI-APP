// DRS AI — JNI bridge for drs::Engine (libdrs_core_jni.so)
#include "drs_core.h"

#include <jni.h>
#include <android/log.h>
#include <cstring>
#include <map>
#include <memory>
#include <mutex>

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "drs-jni", __VA_ARGS__)

namespace {

std::mutex g_handles_mtx;
std::map<long, std::unique_ptr<drs::Engine>> g_engines;
long g_next_id = 1;

// last generation stats (single-engine-at-a-time usage is guaranteed by Kotlin mutex)
struct LastStats { int64_t tokens = 0, ms = 0, prefix = 0, stop = 0, ctx_used = 0; };
LastStats g_last_stats;

drs::Engine* find_engine(long id) {
    std::lock_guard<std::mutex> lock(g_handles_mtx);
    auto it = g_engines.find(id);
    return it == g_engines.end() ? nullptr : it->second.get();
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

// TokenSink adapter: calls sink.onToken(String) -> boolean
class JniSink {
public:
    JniSink(JNIEnv* env, jobject sink) : env_(env), sink_(sink), mid_(nullptr) {
        if (sink) {
            jclass cls = env->GetObjectClass(sink);
            mid_ = env->GetMethodID(cls, "onToken", "(Ljava/lang/String;)Z");
            env_->DeleteLocalRef(cls);
        }
    }
    bool valid() const { return sink_ != nullptr && mid_ != nullptr; }
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

JNIEXPORT jlong JNICALL
Java_com_drs_ai_core_inference_LlamaNative_nativeCreate(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lock(g_handles_mtx);
    long id = g_next_id++;
    g_engines[id] = std::make_unique<drs::Engine>();
    return id;
}

JNIEXPORT void JNICALL
Java_com_drs_ai_core_inference_LlamaNative_nativeDestroy(JNIEnv*, jobject, jlong handle) {
    std::lock_guard<std::mutex> lock(g_handles_mtx);
    g_engines.erase(handle);
}

JNIEXPORT jstring JNICALL
Java_com_drs_ai_core_inference_LlamaNative_nativeLoad(JNIEnv* env, jobject, jlong handle,
        jstring modelPath, jint nCtx, jint nThreads, jint nBatch, jboolean embedMode) {
    auto* e = find_engine(handle);
    if (!e) return to_jstring(env, "invalid engine handle");
    drs::GenParams gp;
    gp.n_ctx      = nCtx;
    gp.n_threads  = nThreads;
    gp.n_batch    = nBatch;
    gp.n_ubatch   = nBatch;
    gp.embed_mode = embedMode == JNI_TRUE;
    std::string err = e->load(to_string(env, modelPath), gp);
    return err.empty() ? nullptr : to_jstring(env, err);
}

JNIEXPORT jstring JNICALL
Java_com_drs_ai_core_inference_LlamaNative_nativeGenerate(JNIEnv* env, jobject, jlong handle,
        jstring prompt, jint maxTokens, jfloat temperature, jint topK, jfloat topP,
        jfloat minP, jfloat repeatPenalty, jlong seed, jobjectArray stop, jobject sinkObj) {
    auto* e = find_engine(handle);
    if (!e) return to_jstring(env, "invalid engine handle");

    drs::RunParams rp;
    rp.max_tokens     = maxTokens;
    rp.temperature    = temperature;
    rp.top_k          = topK;
    rp.top_p          = topP;
    rp.min_p          = minP;
    rp.repeat_penalty = repeatPenalty;
    rp.seed           = (long) seed;

    std::vector<std::string> stop_seqs;
    if (stop) {
        jsize n = env->GetArrayLength(stop);
        for (jsize i = 0; i < n; ++i) {
            jstring js = (jstring) env->GetObjectArrayElement(stop, i);
            if (js) { stop_seqs.push_back(to_string(env, js)); env->DeleteLocalRef(js); }
        }
    }

    JniSink jni_sink(env, sinkObj);
    drs::TokenSink ts;
    if (jni_sink.valid()) {
        ts = [&jni_sink](const std::string& piece, int32_t) { return jni_sink.on_token(piece); };
    }

    drs::RunStats stats;
    std::string err = e->generate(to_string(env, prompt), stop_seqs, rp, ts, stats);
    g_last_stats.tokens   = stats.tokens;
    g_last_stats.ms       = stats.milliseconds;
    g_last_stats.prefix   = stats.prefix_len;
    g_last_stats.stop     = stats.stopped_by_stop_seq ? 1 : 0;
    g_last_stats.ctx_used = stats.ctx_used;
    if (!err.empty()) return to_jstring(env, err);
    return nullptr;
}

JNIEXPORT jint JNICALL
Java_com_drs_ai_core_inference_LlamaNative_nativeTokenCount(JNIEnv* env, jobject, jlong handle, jstring text) {
    auto* e = find_engine(handle);
    if (!e) return 0;
    return e->token_count(to_string(env, text));
}

JNIEXPORT jfloatArray JNICALL
Java_com_drs_ai_core_inference_LlamaNative_nativeEmbed(JNIEnv* env, jobject, jlong handle, jstring text) {
    auto* e = find_engine(handle);
    if (!e) return nullptr;
    std::vector<float> out;
    std::string err;
    if (!e->embed(to_string(env, text), out, err)) {
        LOGE("embed failed: %s", err.c_str());
        return nullptr;
    }
    jfloatArray arr = env->NewFloatArray((jsize) out.size());
    if (!arr) return nullptr;
    env->SetFloatArrayRegion(arr, 0, (jsize) out.size(), out.data());
    return arr;
}

JNIEXPORT jint JNICALL
Java_com_drs_ai_core_inference_LlamaNative_nativeCtxUsed(JNIEnv*, jobject, jlong handle) {
    auto* e = find_engine(handle);
    if (!e) return 0;
    return (jint) e->cached_tokens();
}

JNIEXPORT void JNICALL
Java_com_drs_ai_core_inference_LlamaNative_nativeReset(JNIEnv*, jobject, jlong handle) {
    auto* e = find_engine(handle);
    if (e) e->reset_session();
}

JNIEXPORT jlongArray JNICALL
Java_com_drs_ai_core_inference_LlamaNative_nativeCacheStats(JNIEnv* env, jobject, jlong handle) {
    auto* e = find_engine(handle);
    jlong stats[6] = {0, 0, 0, 0, 0, 0};
    if (e) {
        stats[0] = (jlong) e->cache_prefix_hits();
        stats[1] = (jlong) e->cache_full_reencodes();
        stats[2] = g_last_stats.prefix;
        stats[3] = g_last_stats.tokens;
        stats[4] = g_last_stats.ms;
        stats[5] = g_last_stats.stop;
    }
    jlongArray arr = env->NewLongArray(6);
    if (!arr) return nullptr;
    env->SetLongArrayRegion(arr, 0, 6, stats);
    return arr;
}

JNIEXPORT jstring JNICALL
Java_com_drs_ai_core_inference_LlamaNative_nativeLibraryVersion(JNIEnv* env, jobject) {
    return to_jstring(env, drs::Engine::library_version());
}

} // extern "C"
