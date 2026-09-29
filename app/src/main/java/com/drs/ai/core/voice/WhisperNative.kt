package com.drs.ai.core.voice

/**
 * JNI bridge to whisper.cpp (statically linked with its own ggml, symbols hidden
 * to avoid ABI collision with llama's ggml).
 */
object WhisperNative {
    @Volatile var available: Boolean = false
        private set

    init {
        available = try {
            System.loadLibrary("drs_whisper_jni")
            true
        } catch (t: Throwable) {
            false
        }
    }

    @Volatile private var backendsReady = false

    /** Register CPU backend registry from the APK lib dir (whisper runs on CPU). Idempotent. */
    fun initBackends(context: android.content.Context): Boolean {
        if (backendsReady) return true
        return try {
            nativeInitBackends(context.applicationInfo.nativeLibraryDir)
            backendsReady = true
            true
        } catch (t: Throwable) { false }
    }

    external fun nativeInitBackends(backendDir: String): String?

    external fun nativeWhisperCreate(): Long
    external fun nativeWhisperDestroy(handle: Long)
    external fun nativeWhisperLoad(handle: Long, modelPath: String, threads: Int): String?
    external fun nativeWhisperTranscribe(handle: Long, pcm: FloatArray, language: String?, translate: Boolean): String?
    external fun nativeWhisperLastError(handle: Long): String?
}
