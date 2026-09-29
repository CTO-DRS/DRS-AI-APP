package com.drs.ai.core.inference

import android.content.Context

/**
 * JNI bridge to the native DRS core (llama.cpp). All functions return null on success
 * or a human-readable error string; nulls keep exception handling local to Kotlin.
 */
object LlamaNative {
    @Volatile var available: Boolean = false
        private set

    init {
        available = try {
            System.loadLibrary("drs_core_jni")
            true
        } catch (t: Throwable) {
            false
        }
    }

    interface Sink {
        /** Return true to continue, false to stop generation. */
        fun onToken(piece: String): Boolean
    }

    @Volatile private var backendsReady = false

    /** Directory holding the native libs; set by [initBackends] at app startup. */
    @Volatile var backendDir: String = ""
        private set

    /**
     * Register dynamic ggml backends (CPU always; Vulkan when the device exposes
     * Vulkan 1.1+) from the APK's native library directory. Idempotent.
     */
    fun initBackends(context: Context): Boolean {
        if (backendsReady) return true
        return try {
            val dir = context.applicationInfo.nativeLibraryDir ?: ""
            nativeInitBackends(dir)
            backendDir = dir
            backendsReady = true
            true
        } catch (t: Throwable) {
            false
        }
    }

    /** Non-empty string = detected GPU device(s), "" = CPU-only, null = probe failed. */
    fun gpuDevices(): String? = try { nativeGpuDevices() } catch (t: Throwable) { null }

    external fun nativeInitBackends(backendDir: String): String?
    external fun nativeGpuDevices(): String?
    external fun nativeCreate(): Long
    external fun nativeDestroy(handle: Long)
    external fun nativeLoad(handle: Long, modelPath: String, nCtx: Int, nThreads: Int, nBatch: Int,
                            embedMode: Boolean, nGpuLayers: Int, backendDir: String): String?
    external fun nativeGenerate(
        handle: Long,
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        topK: Int,
        topP: Float,
        minP: Float,
        repeatPenalty: Float,
        seed: Long,
        stop: Array<out String>,
        sink: Sink
    ): String?
    external fun nativeTokenCount(handle: Long, text: String): Int
    external fun nativeEmbed(handle: Long, text: String): FloatArray?
    external fun nativeCtxUsed(handle: Long): Int
    external fun nativeReset(handle: Long)
    external fun nativeCacheStats(handle: Long): LongArray // [prefixHits, fullReencodes, lastPrefixLen]
    external fun nativeLibraryVersion(): String
}
