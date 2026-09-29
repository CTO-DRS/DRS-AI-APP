package com.drs.ai.core.vision

/**
 * JNI bridge to the native mtmd (multimodal) module of llama.cpp.
 */
object VisionNative {
    @Volatile var available: Boolean = false
        private set

    init {
        available = try {
            System.loadLibrary("drs_vision_jni")
            true
        } catch (t: Throwable) {
            false
        }
    }

    interface Sink {
        fun onToken(piece: String): Boolean
    }

    external fun nativeVisionCreate(): Long
    external fun nativeVisionDestroy(handle: Long)
    external fun nativeVisionLoad(handle: Long, chatModelPath: String, mmprojPath: String, nCtx: Int, nThreads: Int): String?
    external fun nativeVisionGenerate(
        handle: Long,
        question: String,
        jpeg: ByteArray,
        maxTokens: Int,
        temperature: Float,
        sink: Sink
    ): String?
    external fun nativeVisionReset(handle: Long)
    external fun nativeVisionLastError(handle: Long): String?
    external fun nativeVisionLibraryVersion(): String
}
