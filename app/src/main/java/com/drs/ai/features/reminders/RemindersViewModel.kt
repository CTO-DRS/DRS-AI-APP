package com.drs.ai.features.reminders

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drs.ai.data.db.Reminder
import com.drs.ai.core.reminders.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class RemindersViewModel(
    private val appContext: Context,
    private val scheduler: ReminderScheduler,
    private val dao: com.drs.ai.data.dao.ReminderDao
) : ViewModel() {

    val reminders: StateFlow<List<Reminder>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val canScheduleExact = ReminderScheduler.canScheduleExact(appContext)

    private val _aiBusy = MutableStateFlow(false)
    val aiBusy: StateFlow<Boolean> = _aiBusy

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError

    fun add(title: String, note: String, triggerAt: Long, repeatMode: Int, intervalMillis: Long) {
        if (title.isBlank() || triggerAt <= System.currentTimeMillis()) return
        viewModelScope.launch(Dispatchers.IO) {
            scheduler.upsertAndSchedule(
                Reminder(
                    title = title.trim(),
                    note = note.trim(),
                    triggerAt = triggerAt,
                    repeatMode = repeatMode,
                    intervalMillis = intervalMillis
                )
            )
        }
    }

    fun toggle(reminder: Reminder) {
        viewModelScope.launch(Dispatchers.IO) {
            val newEnabled = !reminder.enabled
            dao.setEnabled(reminder.id, newEnabled)
            val updated = reminder.copy(enabled = newEnabled)
            if (newEnabled) scheduler.schedule(updated) else scheduler.cancel(updated)
        }
    }

    fun complete(reminder: Reminder) {
        viewModelScope.launch(Dispatchers.IO) { scheduler.complete(reminder) }
    }

    fun delete(reminder: Reminder) {
        viewModelScope.launch(Dispatchers.IO) {
            scheduler.cancel(reminder)
            dao.delete(reminder)
        }
    }

    /**
     * Smart assist: the on-device model proposes up to 5 reminders from a natural
     * language wish. Fully offline — requires a loaded chat model.
     */
    fun suggestFromPrompt(wish: String, onResult: (List<Pair<Long, String>>) -> Unit) {
        val engine = com.drs.ai.AppGraph.container.engines.chat
        if (!engine.isLoaded) {
            _aiError.value = "no_model"
            return
        }
        if (wish.isBlank()) return
        _aiBusy.value = true
        _aiError.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val locale = if (Locale.getDefault().language == "ar") "Arabic" else "English"
                val prompt = buildString {
                    appendLine("You convert a user's wish into concrete reminders.")
                    appendLine("Current date/time: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(java.util.Date())}")
                    appendLine("Answer with up to 5 lines, one reminder per line, EXACTLY this format and nothing else:")
                    appendLine("YYYY-MM-DD HH:MM | Short reminder title")
                    appendLine("Wish: $wish")
                }
                val sb = StringBuilder()
                val cfg = com.drs.ai.core.ai.GenConfig(
                    maxTokens = 220, temperature = 0.3f, topK = 40, topP = 0.9f,
                    repeatPenalty = 1.1f, seed = -1L, stopSequences = listOf("\n\n\n")
                )
                engine.generate(prompt, cfg) { piece ->
                    sb.append(piece)
                    true
                }
                val now = System.currentTimeMillis()
                val fmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { isLenient = true }
                val parsed = sb.lines().mapNotNull { line ->
                    val m = Regex("(\\d{4}-\\d{2}-\\d{2})\\s+(\\d{1,2}:\\d{2})\\s*\\|\\s*(.+)").find(line.trim()) ?: return@mapNotNull null
                    val (d, t, title) = m.destructured
                    val ts = try { fmt.parse("$d $t")?.time } catch (_: Exception) { null } ?: return@mapNotNull null
                    if (title.isBlank()) return@mapNotNull null
                    ts to title.trim().take(80)
                }.filter { it.first > now - 60_000 }
                 .distinctBy { it.first / 60000 to it.second.lowercase() }
                 .take(5)
                if (parsed.isEmpty()) _aiError.value = "parse_failed"
                onResult(parsed)
            } catch (t: Throwable) {
                _aiError.value = t.message ?: "failed"
            } finally {
                _aiBusy.value = false
            }
        }
    }

    /** Apply one AI suggestion as a real reminder. */
    fun addSuggestion(ts: Long, title: String) {
        add(title = title, note = "", triggerAt = ts, repeatMode = Reminder.REPEAT_ONCE, intervalMillis = 0)
    }

    /** Quick presets used by the "smart templates" chips. */
    fun addQuickPreset(template: QuickTemplate) {
        val cal = Calendar.getInstance().apply { timeInMillis = System.currentTimeMillis() }
        val title: String
        when (template) {
            QuickTemplate.MORNING -> {
                cal.set(Calendar.HOUR_OF_DAY, 7); cal.set(Calendar.MINUTE, 30)
                if (cal.timeInMillis <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1)
                title = "morning_brief"
            }
            QuickTemplate.HYDRATION -> {
                cal.add(Calendar.HOUR_OF_DAY, 2)
                title = "drink_water"
            }
            QuickTemplate.MEDICATION -> {
                cal.add(Calendar.HOUR_OF_DAY, 8)
                title = "medication"
            }
            QuickTemplate.FOCUS -> {
                cal.add(Calendar.MINUTE, 50)
                title = "focus_break"
            }
            QuickTemplate.EARLY_SLEEP -> {
                cal.set(Calendar.HOUR_OF_DAY, 22); cal.set(Calendar.MINUTE, 30)
                if (cal.timeInMillis <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1)
                title = "early_sleep"
            }
        }
        val repeat = if (template == QuickTemplate.HYDRATION || template == QuickTemplate.MEDICATION)
            Reminder.REPEAT_CUSTOM else Reminder.REPEAT_ONCE
        val interval = if (repeat == Reminder.REPEAT_CUSTOM) 2 * 3600_000L else 0L
        add(title, "", cal.timeInMillis, repeat, interval)
    }
}

enum class QuickTemplate { MORNING, HYDRATION, MEDICATION, FOCUS, EARLY_SLEEP }
