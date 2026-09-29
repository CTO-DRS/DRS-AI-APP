package com.drs.ai.features.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.drs.ai.AppGraph
import com.drs.ai.data.db.ChatMessage
import com.drs.ai.data.db.ChatSession
import com.drs.ai.data.db.PromptTemplate
import com.drs.ai.core.memory.MemoryManager
import com.drs.ai.core.models.HardwareProfiler
import com.drs.ai.core.models.ModelRepository
import com.drs.ai.core.rag.RagPipeline
import com.drs.ai.data.settings.SettingsRepository
import com.drs.ai.core.inference.EngineManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Chat orchestration: builds prompts (system + memories + RAG + history), streams
 * generation into the UI, supports stop / regenerate, and guards context overflow.
 *
 * v1.4 — smart context compression (ROADMAP): instead of hard-trimming old turns,
 * older history is summarized locally by the model into a rolling session summary.
 * Nothing leaves the device; the summary lives in the session row and older
 * messages stay readable in the transcript.
 */
class ChatViewModel(
    private val engines: EngineManager,
    private val dao: com.drs.ai.data.dao.ChatDao,
    private val templateDao: com.drs.ai.data.dao.TemplateDao,
    private val settingsRepo: SettingsRepository,
    private val memory: MemoryManager,
    private val rag: RagPipeline,
    private val appContext: Context
) : ViewModel() {

    val sessions = dao.observeSessions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val templates: StateFlow<List<PromptTemplate>> = templateDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _session = MutableStateFlow<ChatSession?>(null)
    val session: StateFlow<ChatSession?> = _session

    private val _generating = MutableStateFlow(false)
    val generating: StateFlow<Boolean> = _generating

    private val _streamText = MutableStateFlow("")
    val streamText: StateFlow<String> = _streamText

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice

    private val _lastStats = MutableStateFlow<Pair<Int, Float>?>(null)
    val lastStats: StateFlow<Pair<Int, Float>?> = _lastStats

    private val _ctxUsed = MutableStateFlow(0)
    val ctxUsed: StateFlow<Int> = _ctxUsed

    private val _ragSources = MutableStateFlow<List<String>>(emptyList())
    val ragSources: StateFlow<List<String>> = _ragSources

    private var genJob: Job? = null
    private val genMutex = Mutex()

    init {
        newSession()
    }

    fun newSession() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val id = dao.insertSession(ChatSession(title = "", createdAt = now, updatedAt = now))
            _session.value = ChatSession(id = id, title = "", createdAt = now, updatedAt = now)
            _messages.value = emptyList()
            _ragSources.value = emptyList()
            _lastStats.value = null
            _ctxUsed.value = 0
            engines.chat.resetSession()
        }
    }

    fun openSession(id: Long) {
        viewModelScope.launch {
            _session.value = dao.getSession(id)
            _messages.value = dao.messagesForSession(id)
            engines.chat.resetSession()
            _ctxUsed.value = 0
            _ragSources.value = emptyList()
            _lastStats.value = null
        }
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch {
            dao.clearSessionMessages(id)
            dao.deleteSession(id)
            if (_session.value?.id == id) newSession()
        }
    }

    // v1.4 — session management
    fun setPinned(id: Long, pinned: Boolean) {
        viewModelScope.launch { dao.setPinned(id, pinned) }
    }

    fun renameSession(id: Long, title: String) {
        viewModelScope.launch { dao.renameSession(id, title.trim()) }
    }

    fun stop() {
        genJob?.cancel()
        genJob = null
    }

    fun deleteMessage(m: ChatMessage) {
        viewModelScope.launch {
            dao.deleteMessagesFrom(m.sessionId, m.createdAt)
            _messages.value = dao.messagesForSession(m.sessionId)
        }
    }

    fun saveToMemory(text: String) {
        viewModelScope.launch {
            memory.add(text)
            _notice.value = "memory_saved"
        }
    }

    // v1.4 — prompt template library
    fun addTemplate(title: String, content: String, category: String) {
        viewModelScope.launch {
            templateDao.insert(PromptTemplate(title = title.trim(), content = content, category = category, createdAt = System.currentTimeMillis()))
        }
    }

    fun updateTemplate(t: PromptTemplate, title: String, content: String, category: String) {
        viewModelScope.launch {
            templateDao.update(t.copy(title = title.trim(), content = content, category = category))
        }
    }

    fun deleteTemplate(t: PromptTemplate) {
        viewModelScope.launch { templateDao.delete(t) }
    }

    fun onTemplateUsed(id: Long) {
        viewModelScope.launch { templateDao.incrementUse(id) }
    }

    fun send(userText: String) {
        val text = userText.trim()
        if (text.isEmpty() || _generating.value) return
        genJob = viewModelScope.launch { runTurn(text, null) }
    }

    fun regenerate(assistantMessage: ChatMessage) {
        if (_generating.value) return
        genJob = viewModelScope.launch {
            dao.deleteMessagesFrom(assistantMessage.sessionId, assistantMessage.createdAt)
            val msgs = dao.messagesForSession(assistantMessage.sessionId)
            _messages.value = msgs
            val lastUser = msgs.lastOrNull { it.role == "user" } ?: return@launch
            engines.chat.resetSession()
            _ctxUsed.value = 0
            runTurn(lastUser.content, lastUser)
        }
    }

    private suspend fun runTurn(userText: String, regenerateOf: ChatMessage?) {
        val s = _session.value ?: return
        val active = AppGraph.container.models.activeModel(ModelRepository.KIND_CHAT)
        if (active == null) {
            _error.value = "need_model"
            return
        }
        val settings = settingsRepo.current()
        val engine = engines.chat

        _error.value = null
        _notice.value = null
        _generating.value = true
        _streamText.value = ""
        _ragSources.value = emptyList()

        try {
            genMutex.withLock {
                if (!engine.isLoaded) {
                    _notice.value = "loading_model"
                    val profile = HardwareProfiler.probe(appContext)
                    val threads = settings.threads.takeIf { it > 0 } ?: profile.recThreads
                    val ctx = settings.contextSize.coerceAtMost(active.ctxLen?.toInt() ?: settings.contextSize)
                    val err = engine.load(active.path, active.name, ctx, threads, settings.batchSize,
                                          gpuLayers = settings.gpuLayers)
                    if (err != null) {
                        _error.value = "load_failed: $err"
                        return
                    }
                }

                val now = System.currentTimeMillis()
                if (regenerateOf == null) {
                    dao.insertMessage(ChatMessage(sessionId = s.id, role = "user", content = userText, createdAt = now))
                }

                var all = dao.messagesForSession(s.id)
                // exclude the latest user message from history (it is the turn being sent)
                if (all.isNotEmpty() && all.last().role == "user") all = all.dropLast(1)

                val budgetTokens = (engine.nCtx.takeIf { it > 0 } ?: settings.contextSize) - settings.gen.maxTokens - 128
                var summary = _session.value?.summary
                var coveredUpTo = _session.value?.summaryUpTo ?: 0L

                // ── v1.4 smart context compression ─────────────────────────────
                // If the uncovered history would overflow the budget, summarize the
                // oldest uncovered turns locally and roll them into the session summary.
                val userTokens = estimateTokens(userText)
                if (estimateTokens(all.filter { it.createdAt > coveredUpTo }) + userTokens > budgetTokens) {
                    val tailBudget = budgetTokens * 45 / 100
                    val uncovered = all.filter { it.createdAt > coveredUpTo }
                    val tail = mutableListOf<ChatMessage>()
                    var total = 0
                    for (m in uncovered.reversed()) {
                        val t = estimateTokens(m.content) + 8
                        if (total + t > tailBudget) break
                        total += t
                        tail.add(0, m)
                    }
                    val dropped = uncovered.dropLast(tail.size)
                    if (dropped.isNotEmpty()) {
                        val newSummary = summarize(engine, summary, dropped)
                        if (!newSummary.isNullOrBlank()) {
                            summary = newSummary
                            coveredUpTo = maxOf(coveredUpTo, dropped.last().createdAt)
                            dao.getSession(s.id)?.let { sess ->
                                dao.updateSession(sess.copy(summary = summary, summaryUpTo = coveredUpTo))
                                _session.value = sess.copy(summary = summary, summaryUpTo = coveredUpTo)
                            }
                            _notice.value = "context_compressed"
                        }
                    }
                }

                // Only messages not yet covered by the summary go into the prompt
                val history = all.filter { it.createdAt > coveredUpTo }
                _messages.value = dao.messagesForSession(s.id)

                val ragBlock = if (settings.ragEnabled && engines.embedder.isLoaded) {
                    val (block, sources) = rag.buildRagBlock(userText)
                    _ragSources.value = sources
                    block
                } else null

                val system = buildString {
                    append(settings.systemPrompt.ifBlank {
                        "You are DRS AI, a helpful assistant running fully offline on the user's device."
                    })
                    if (settings.replyLang.isNotBlank()) {
                        append(" Always reply in language code: ").append(settings.replyLang).append('.')
                    }
                }

                val prompt = buildString {
                    append("System: ").append(system).append('\n')
                    val memBlock = memory.buildBlock()
                    if (memBlock != null) { append(memBlock).append('\n') }
                    if (ragBlock != null) { append(ragBlock) }
                    if (!summary.isNullOrBlank()) {
                        append("— Summary of earlier conversation (older turns were compressed into this) —\n")
                        append(summary).append("\n\n")
                    }
                    append('\n')
                    for (m in history) {
                        append(if (m.role == "user") "User: " else "Assistant: ").append(m.content).append('\n')
                    }
                    append("User: ").append(userText).append('\n')
                    append("Assistant:")
                }

                val sb = StringBuilder()
                var lastEmit = 0L
                val started = System.currentTimeMillis()

                val result = engine.generate(prompt, com.drs.ai.core.ai.GenConfig(
                    temperature = settings.gen.temperature,
                    topK = settings.gen.topK,
                    topP = settings.gen.topP,
                    minP = settings.gen.minP,
                    repeatPenalty = settings.gen.repeatPenalty,
                    maxTokens = settings.gen.maxTokens,
                    seed = settings.gen.seed
                )) { piece ->
                    sb.append(piece)
                    val t = System.currentTimeMillis()
                    if (t - lastEmit > 60) { // throttle recompositions, keep latency low
                        lastEmit = t
                        _streamText.value = sb.toString()
                    }
                    true
                }

                val finalText = sb.toString()
                val elapsed = (System.currentTimeMillis() - started).coerceAtLeast(1)
                val tps = result.tokens * 1000f / elapsed

                dao.insertMessage(
                    ChatMessage(
                        sessionId = s.id, role = "assistant", content = finalText,
                        createdAt = System.currentTimeMillis(), tokens = result.tokens, tokPerSec = tps,
                        sources = _ragSources.value.joinToString(",").ifEmpty { null }
                    )
                )
                _messages.value = dao.messagesForSession(s.id)
                _streamText.value = ""
                _lastStats.value = result.tokens to tps
                _ctxUsed.value = result.ctxUsed

                val sess = dao.getSession(s.id)
                if (sess != null) {
                    dao.updateSession(sess.copy(
                        title = sess.title.ifBlank { userText.take(40) },
                        updatedAt = System.currentTimeMillis(),
                        model = active.name
                    ))
                    _session.value = dao.getSession(s.id)
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            val partial = _streamText.value
            if (partial.isNotBlank()) {
                dao.insertMessage(
                    ChatMessage(sessionId = s.id, role = "assistant", content = partial, createdAt = System.currentTimeMillis())
                )
                _messages.value = dao.messagesForSession(s.id)
            }
            _streamText.value = ""
            throw e
        } catch (e: Exception) {
            _error.value = e.message ?: "generation failed"
        } finally {
            _generating.value = false
        }
    }

    /**
     * Local summarization for context compression — runs on the already-loaded chat
     * model with low temperature and a hard token cap, so it costs at most a couple
     * of seconds. Any failure keeps the previous behavior (hard trim) intact.
     */
    private suspend fun summarize(
        engine: com.drs.ai.core.ai.LlmEngine,
        previous: String?,
        dropped: List<ChatMessage>
    ): String? = try {
        val prompt = buildString {
            append("Summarize the following conversation excerpt in at most 120 words. ")
            append("Preserve names, facts, decisions, dates and numbers. ")
            append("Reply with the summary text only — no preamble, no quotes.\n\n")
            if (!previous.isNullOrBlank()) {
                append("Existing summary (merge its content in):\n").append(previous).append("\n\n")
            }
            append("Conversation excerpt:\n")
            for (m in dropped) {
                append(if (m.role == "user") "User: " else "Assistant: ")
                append(m.content.take(600)).append('\n')
            }
        }
        val r = engine.generate(
            prompt,
            com.drs.ai.core.ai.GenConfig(
                temperature = 0.25f, topK = 40, topP = 0.9f, minP = 0.05f,
                repeatPenalty = 1.1f, maxTokens = 200, seed = -1L
            )
        ) { true }
        r.text.trim().takeIf { it.isNotBlank() }
    } catch (t: Throwable) {
        null // honest fallback: compression failed → keep going without it
    }

    private fun estimateTokens(text: String): Int = (text.length / 3.6).toInt().coerceAtLeast(1)

    private fun estimateTokens(messages: List<ChatMessage>): Int {
        var chars = 0
        for (m in messages) chars += m.content.length + 8
        return (chars / 3.6).toInt().coerceAtLeast(1)
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val c = AppGraph.container
                return ChatViewModel(c.engines, c.db.chatDao(), c.db.templateDao(), c.settings, c.memory, c.rag, c.appContext) as T
            }
        }
    }
}
