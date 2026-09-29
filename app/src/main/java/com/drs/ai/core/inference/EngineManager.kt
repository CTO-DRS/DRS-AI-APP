package com.drs.ai.core.inference

import com.drs.ai.core.ai.EngineState
import com.drs.ai.core.ai.LlmEngine

/**
 * Owns the chat engine and the embedder engine instances. Engine-agnostic:
 * swap LlamaCppEngine for another LlmEngine implementation without touching callers.
 */
class EngineManager(
    val chat: LlmEngine = LlamaCppEngine(),
    val embedder: LlmEngine = LlamaCppEngine()
) {
    val chatState: EngineState get() = chat.state.value

    suspend fun unloadAll() {
        chat.unload()
        embedder.unload()
    }

    suspend fun unloadIfAuto(autoUnload: Boolean) {
        if (autoUnload) unloadAll()
    }

    fun isChatLoaded(): Boolean = chat.isLoaded
    fun isEmbedderLoaded(): Boolean = embedder.isLoaded
}
