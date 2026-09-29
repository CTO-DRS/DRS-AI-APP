package com.drs.ai.core.ai

import kotlinx.coroutines.flow.StateFlow

/** Abstract local LLM engine. Any backend (llama.cpp today, others tomorrow) plugs in behind this contract. */
interface LlmEngine {
    val state: StateFlow<EngineState>
    val isLoaded: Boolean
    val loadedModelName: String?
    val nCtx: Int

    /** Load a model. Returns null on success or an error message. */
    suspend fun load(modelPath: String, displayName: String, nCtx: Int, nThreads: Int, nBatch: Int,
                     embedMode: Boolean = false, gpuLayers: Int = 0): String?

    suspend fun unload()

    /** Approximate token count for a text (exact when a model is loaded). */
    fun tokenCount(text: String): Int

    /**
     * Run generation. Streams token pieces to [onToken]; return false from the callback to stop.
     * Returns GenResult, or throws [EngineException] on failure.
     */
    suspend fun generate(prompt: String, cfg: GenConfig, onToken: (String) -> Unit): GenResult

    /** Embed a text (requires embedMode = true at load). Null vector or error message on failure. */
    suspend fun embed(text: String): Pair<FloatArray?, String?>

    /** KV-cache position used by the last generation (honest context accounting). */
    fun ctxUsed(): Int

    /** Clear KV cache / reset session state. */
    fun resetSession()
}

data class GenConfig(
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val topP: Float = 0.95f,
    val minP: Float = 0.05f,
    val repeatPenalty: Float = 1.1f,
    val maxTokens: Int = 512,
    val seed: Long = -1L, // -1 = random
    val stopSequences: List<String> = emptyList()
)

data class GenResult(
    val text: String,
    val tokens: Int,
    val tokPerSec: Float,
    val ctxUsed: Int,
    val stoppedByStopSequence: Boolean,
    val cacheHit: Boolean // v1.1: true when generation reused prefix KV-cache without full re-encode
)

class EngineException(message: String) : Exception(message)

sealed interface EngineState {
    data object Idle : EngineState
    data class Loading(val model: String) : EngineState
    data class Ready(val model: String) : EngineState
    data object Generating : EngineState
    data class Error(val message: String) : EngineState
}
