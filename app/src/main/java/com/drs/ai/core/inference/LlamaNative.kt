package com.drs.ai.core.inference

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

    external fun nativeCreate(): Long
    external fun nativeDestroy(handle: Long)
    external fun nativeLoad(handle: Long, modelPath: String, nCtx: Int, nThreads: Int, nBatch: Int, embedMode: Boolean): String?
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
