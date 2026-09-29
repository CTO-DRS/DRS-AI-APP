package com.drs.ai.core.inference

import com.drs.ai.core.ai.EngineException
import com.drs.ai.core.ai.EngineState
import com.drs.ai.core.ai.GenConfig
import com.drs.ai.core.ai.GenResult
import com.drs.ai.core.ai.LlmEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * llama.cpp-backed implementation of [LlmEngine]. Single-mutex serialization keeps the
 * native context safe; generation runs on Dispatchers.IO.
 */
class LlamaCppEngine : LlmEngine {

    private val _state = MutableStateFlow<EngineState>(EngineState.Idle)
    override val state: StateFlow<EngineState> = _state

    private val mutex = Mutex()
    private var handle: Long = 0L
    private var embedMode = false
    private var modelName: String? = null
    private var loadedCtx = 0
    private val approxTokensCache = HashMap<String, Int>()

    override val isLoaded: Boolean get() = handle != 0L
    override val loadedModelName: String? get() = modelName
    override val nCtx: Int get() = loadedCtx

    override suspend fun load(modelPath: String, displayName: String, nCtx: Int, nThreads: Int, nBatch: Int, embedMode: Boolean): String? =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                if (!LlamaNative.available) return@withContext "Native library libdrs_core_jni.so is unavailable on this device"
                _state.value = EngineState.Loading(displayName)
                unloadLocked()
                val h = try { LlamaNative.nativeCreate() } catch (t: Throwable) {
                    val msg = "Engine init failed: ${t.message ?: t.javaClass.simpleName}"
                    _state.value = EngineState.Error(msg); return@withContext msg
                }
                if (h == 0L) {
                    val msg = "Engine init failed (null handle)"
                    _state.value = EngineState.Error(msg); return@withContext msg
                }
                val err = LlamaNative.nativeLoad(h, modelPath, nCtx, nThreads, nBatch, embedMode)
                if (err != null) {
                    LlamaNative.nativeDestroy(h)
                    handle = 0L
                    _state.value = EngineState.Error(err)
                    return@withContext err
                }
                handle = h
                this@LlamaCppEngine.embedMode = embedMode
                modelName = displayName
                loadedCtx = nCtx
                _state.value = EngineState.Ready(displayName)
                null
            }
        }

    override suspend fun unload() = mutex.withLock { withContext(Dispatchers.IO) { unloadLocked() } }

    private fun unloadLocked() {
        if (handle != 0L) {
            try { LlamaNative.nativeDestroy(handle) } catch (_: Throwable) {}
            handle = 0L
        }
        modelName = null
        loadedCtx = 0
        if (_state.value !is EngineState.Error) _state.value = EngineState.Idle
    }

    override fun tokenCount(text: String): Int {
        if (handle != 0L) {
            return try { LlamaNative.nativeTokenCount(handle, text) } catch (_: Throwable) { approx(text) }
        }
        return approx(text)
    }

    private fun approx(text: String): Int = approxTokensCache.getOrPut(text) { (text.length / 3.6).toInt().coerceAtLeast(1) }

    override suspend fun generate(prompt: String, cfg: GenConfig, onToken: (String) -> Unit): GenResult =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                if (handle == 0L) throw EngineException("No model loaded")
                _state.value = EngineState.Generating
                try {
                    val sink = object : LlamaNative.Sink {
                        override fun onToken(piece: String): Boolean = try { onToken(piece); true } catch (t: Throwable) { false }
                    }
                    val err = LlamaNative.nativeGenerate(
                        handle, prompt, cfg.maxTokens, cfg.temperature, cfg.topK, cfg.topP,
                        cfg.minP, cfg.repeatPenalty, cfg.seed, cfg.stopSequences.toTypedArray(), sink
                    )
                    if (err != null) throw EngineException(err)
                    val stats = LlamaNative.nativeCacheStats(handle)
                    val ctxUsed = LlamaNative.nativeCtxUsed(handle)
                    // stats: [prefixHits, fullReencodes, lastPrefixLen, lastTokens, lastMs, lastStopFlag]
                    val tokens = stats.getOrNull(3)?.toInt() ?: 0
                    val ms = stats.getOrNull(4)?.coerceAtLeast(1L) ?: 1L
                    val tps = if (ms > 0) tokens * 1000.0f / ms else 0f
                    GenResult(
                        text = "",
                        tokens = tokens,
                        tokPerSec = tps,
                        ctxUsed = ctxUsed,
                        stoppedByStopSequence = stats.getOrNull(5)?.toInt() == 1,
                        cacheHit = stats.getOrNull(2)?.let { it > 0 } ?: false
                    )
                } catch (e: Exception) {
                    _state.value = EngineState.Error(e.message ?: "generation failed")
                    throw e
                } finally {
                    if (handle != 0L) _state.value = EngineState.Ready(modelName ?: "")
                }
            }
        }

    override suspend fun embed(text: String): Pair<FloatArray?, String?> = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (handle == 0L) return@withContext null to "No embedder model loaded"
            if (!embedMode) return@withContext null to "Engine not loaded in embed mode"
            try {
                val v = LlamaNative.nativeEmbed(handle, text)
                if (v == null) null to "Embedding failed (model may not support embeddings)" else v to null
            } catch (t: Throwable) {
                null to (t.message ?: "embedding error")
            }
        }
    }

    override fun ctxUsed(): Int = if (handle != 0L) try { LlamaNative.nativeCtxUsed(handle) } catch (_: Throwable) { 0 } else 0

    override fun resetSession() {
        if (handle != 0L) try { LlamaNative.nativeReset(handle) } catch (_: Throwable) {}
    }
}
