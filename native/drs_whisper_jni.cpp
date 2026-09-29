// DRS AI — JNI bridge for whisper.cpp v1.7.4 (libdrs_whisper_jni.so)
// whisper + its own ggml are statically linked into this library with hidden
// visibility so their symbols never interpose with libllama's ggml at runtime.
#include "ggml-backend.h"
#include "whisper.h"

#include <jni.h>
#include <android/log.h>
#include <cstring>
#include <map>
#include <memory>
#include <mutex>
#include <string>

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "drs-whisper", __VA_ARGS__)

namespace {

struct WhisperCtx {
    whisper_context* ctx = nullptr;
    int              threads = 2;
    std::string      last_error;
    std::mutex       mtx;
};

std::mutex g_map_mtx;
std::map<long, std::unique_ptr<WhisperCtx>> g_ctxs;
long g_next_id = 1;

WhisperCtx* find(long id) {
    std::lock_guard<std::mutex> lock(g_map_mtx);
    auto it = g_ctxs.find(id);
    return it == g_ctxs.end() ? nullptr : it->second.get();
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

} // namespace

extern "C" {

// Registers dynamic backends from the APK lib dir (whisper itself runs on CPU).
// Self-contained: voice does not depend on libdrs_core_jni.so.
JNIEXPORT jstring JNICALL
Java_com_drs_ai_core_voice_WhisperNative_nativeInitBackends(JNIEnv* env, jobject, jstring backendDir) {
    static std::once_flag init_once;
    std::string dir = to_string(env, backendDir);
    std::call_once(init_once, [&dir] {
        ggml_backend_load_all_from_path(dir.c_str()); // CPU registry; whisper does its own backend init
    });
    return nullptr;
}

JNIEXPORT jlong JNICALL
Java_com_drs_ai_core_voice_WhisperNative_nativeWhisperCreate(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lock(g_map_mtx);
    long id = g_next_id++;
    g_ctxs[id] = std::make_unique<WhisperCtx>();
    return id;
}

JNIEXPORT void JNICALL
Java_com_drs_ai_core_voice_WhisperNative_nativeWhisperDestroy(JNIEnv*, jobject, jlong handle) {
    std::lock_guard<std::mutex> lock(g_map_mtx);
    auto it = g_ctxs.find(handle);
    if (it == g_ctxs.end()) return;
    auto* w = it->second.get();
    std::lock_guard<std::mutex> wlock(w->mtx);
    if (w->ctx) whisper_free(w->ctx);
    g_ctxs.erase(it);
}

JNIEXPORT jstring JNICALL
Java_com_drs_ai_core_voice_WhisperNative_nativeWhisperLoad(JNIEnv* env, jobject, jlong handle,
        jstring modelPath, jint threads) {
    auto* w = find(handle);
    if (!w) return to_jstring(env, "invalid whisper handle");
    std::lock_guard<std::mutex> wlock(w->mtx);

    std::string path = to_string(env, modelPath);
    w->threads = std::max(1, (int) threads);

    whisper_context_params cparams = whisper_context_default_params();
    cparams.use_gpu = false; // CPU-only in this build, reported honestly
    w->ctx = whisper_init_from_file_with_params(path.c_str(), cparams);
    if (!w->ctx) {
        w->last_error = "failed to init whisper from: " + path;
        return to_jstring(env, w->last_error);
    }
    return nullptr;
}

JNIEXPORT jstring JNICALL
Java_com_drs_ai_core_voice_WhisperNative_nativeWhisperTranscribe(JNIEnv* env, jobject, jlong handle,
        jfloatArray pcm, jstring language, jboolean translate) {
    auto* w = find(handle);
    if (!w) { return nullptr; } // caller treats null with null lastError as "invalid handle"
    std::lock_guard<std::mutex> wlock(w->mtx);
    if (!w->ctx) {
        w->last_error = "whisper model not loaded";
        return nullptr;
    }

    jsize n = env->GetArrayLength(pcm);
    std::vector<float> samples(n);
    env->GetFloatArrayRegion(pcm, 0, n, samples.data());

    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.n_threads        = w->threads;
    params.translate        = translate == JNI_TRUE;
    params.language         = nullptr; // set below if provided
    params.print_progress   = false;
    params.print_special    = false;
    params.print_realtime   = false;
    params.print_timestamps = false;
    params.suppress_blank   = true;
    params.single_segment   = false;
    params.no_timestamps    = true;

    std::string lang;
    if (language) {
        lang = to_string(env, language);
        if (!lang.empty() && lang != "auto") {
            params.language = lang.c_str();
        }
    }

    if (whisper_full(w->ctx, params, samples.data(), (int) samples.size()) != 0) {
        w->last_error = "whisper_full failed";
        return nullptr;
    }

    std::string text;
    const int n_segments = whisper_full_n_segments(w->ctx);
    for (int i = 0; i < n_segments; ++i) {
        const char* seg = whisper_full_get_segment_text(w->ctx, i);
        if (seg) text += seg;
    }
    return to_jstring(env, text);
}

JNIEXPORT jstring JNICALL
Java_com_drs_ai_core_voice_WhisperNative_nativeWhisperLastError(JNIEnv* env, jobject, jlong handle) {
    auto* w = find(handle);
    if (!w || w->last_error.empty()) return nullptr;
    return to_jstring(env, w->last_error);
}

} // extern "C"
