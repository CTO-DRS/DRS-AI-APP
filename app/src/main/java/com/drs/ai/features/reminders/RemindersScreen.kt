package com.drs.ai.features.reminders

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.drs.ai.AppGraph
import com.drs.ai.R
import com.drs.ai.data.db.Reminder
import com.drs.ai.ui.components.EmptyState
import com.drs.ai.ui.components.GradientBanner
import com.drs.ai.ui.components.SectionCard
import com.drs.ai.ui.components.ScreenHeader
import com.drs.ai.ui.components.SectionTitle
import com.drs.ai.ui.components.entrance
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Smart reminders — everything local: Room + AlarmManager + honest UI.
 * "Smart" = on-device AI suggestions (when a model is loaded) + quick templates.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(nav: NavHostController) {
    val context = LocalContext.current
    val container = AppGraph.container
    val vm = remember { RemindersViewModel(context.applicationContext, container.reminders, container.db.reminderDao()) }
    val reminders by vm.reminders.collectAsState()
    val aiBusy by vm.aiBusy.collectAsState()
    val aiError by vm.aiError.collectAsState()
    val canExact = vm.canScheduleExact

    var showAdd by remember { mutableStateOf(false) }
    var wishText by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<Pair<Long, String>>>(emptyList()) }

    val active = reminders.count { it.enabled }
    val next = reminders.filter { it.enabled && it.triggerAt > System.currentTimeMillis() }.minByOrNull { it.triggerAt }
    val fmt = remember { SimpleDateFormat("EEE d MMM • HH:mm", Locale.getDefault()) }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
        ) {
            ScreenHeader(stringResource(R.string.nav_reminders), stringResource(R.string.reminders_subtitle))

            GradientBanner(
                title = if (active > 0) stringResource(R.string.reminders_active_n, active)
                        else stringResource(R.string.reminders_none_active),
                subtitle = next?.let { stringResource(R.string.reminders_next, fmt.format(Date(it.triggerAt)), it.title) }
                    ?: stringResource(R.string.reminders_next_none),
                trailing = {
                    Icon(Icons.Filled.Alarm, null, Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            )

            if (!canExact) {
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(R.string.reminders_inexact_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // ── Smart assist (on-device AI) ─────────────────────────────────
            SectionTitle(stringResource(R.string.reminders_smart_title))
            SectionCard {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        stringResource(R.string.reminders_smart_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = wishText,
                            onValueChange = { wishText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(stringResource(R.string.reminders_smart_placeholder), maxLines = 1) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                vm.suggestFromPrompt(wishText) { list ->
                                    suggestions = list
                                }
                            },
                            enabled = !aiBusy
                        ) {
                            if (aiBusy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    aiError?.let { err ->
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (err == "no_model") stringResource(R.string.reminders_need_model)
                            else stringResource(R.string.reminders_ai_failed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (suggestions.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Column(Modifier.animateContentSize()) {
                            suggestions.forEach { (ts, title) ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text(fmt.format(Date(ts)), style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    TextButton(onClick = {
                                        vm.addSuggestion(ts, title)
                                        suggestions = suggestions - (ts to title)
                                    }) { Text(stringResource(R.string.reminders_add)) }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // ── Quick templates ─────────────────────────────────────────────
            SectionTitle(stringResource(R.string.reminders_templates))
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TemplateChip(Icons.Filled.WbSunny, stringResource(R.string.tpl_morning)) { vm.addQuickPreset(QuickTemplate.MORNING) }
                TemplateChip(Icons.Filled.WaterDrop, stringResource(R.string.tpl_hydration)) { vm.addQuickPreset(QuickTemplate.HYDRATION) }
                TemplateChip(Icons.Filled.Medication, stringResource(R.string.tpl_medication)) { vm.addQuickPreset(QuickTemplate.MEDICATION) }
                TemplateChip(Icons.Filled.CenterFocusStrong, stringResource(R.string.tpl_focus)) { vm.addQuickPreset(QuickTemplate.FOCUS) }
                TemplateChip(Icons.Filled.Bedtime, stringResource(R.string.tpl_sleep)) { vm.addQuickPreset(QuickTemplate.EARLY_SLEEP) }
            }

            Spacer(Modifier.height(18.dp))

            // ── List ────────────────────────────────────────────────────────
            SectionTitle(stringResource(R.string.reminders_yours))
            if (reminders.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Alarm,
                    title = stringResource(R.string.reminders_empty_title),
                    hint = stringResource(R.string.reminders_empty_hint),
                    actionLabel = stringResource(R.string.reminders_add_first),
                    onAction = { showAdd = true }
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    reminders.forEachIndexed { i, r ->
                        ReminderCard(
                            reminder = r,
                            index = i,
                            onToggle = { vm.toggle(r) },
                            onComplete = { vm.complete(r) },
                            onDelete = { vm.delete(r) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(90.dp))
        }

        FloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Filled.Add, null)
        }
    }

    if (showAdd) {
        AddReminderDialog(onDismiss = { showAdd = false }, onAdd = { title, note, ts, mode, interval ->
            vm.add(title, note, ts, mode, interval)
            showAdd = false
        })
    }
}

@Composable
private fun TemplateChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, Modifier.size(16.dp)) }
    )
}

@Composable
private fun ReminderCard(
    reminder: Reminder,
    index: Int,
    onToggle: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val fmt = remember { SimpleDateFormat("EEE d MMM • HH:mm", Locale.getDefault()) }
    val repeat = Reminder.repeatLabel(reminder.repeatMode, reminder.intervalMillis)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .entrance(key = reminder.id, delayMs = index * 40),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (reminder.enabled) MaterialTheme.colorScheme.surfaceContainer
            else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        reminder.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (reminder.aiSuggested) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Filled.AutoAwesome, null, Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
                if (reminder.note.isNotBlank()) {
                    Text(
                        reminder.note, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2
                    )
                }
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        fmt.format(Date(reminder.triggerAt)),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (reminder.enabled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (repeat.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "⟳ $repeat",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
            IconButton(onClick = onComplete) {
                Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error)
            }
            Switch(checked = reminder.enabled, onCheckedChange = { onToggle() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddReminderDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, note: String, triggerAt: Long, repeatMode: Int, interval: Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var repeatMode by remember { mutableStateOf(Reminder.REPEAT_ONCE) }
    var intervalMinutes by remember { mutableStateOf(120L) }

    val cal = remember { java.util.Calendar.getInstance().apply { add(java.util.Calendar.HOUR_OF_DAY, 1) } }
    var pickedDate by remember { mutableStateOf(cal.timeInMillis) }
    var pickedHour by remember { mutableStateOf(cal.get(java.util.Calendar.HOUR_OF_DAY)) }
    var pickedMinute by remember { mutableStateOf(cal.get(java.util.Calendar.MINUTE)) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val timeText = String.format(Locale.getDefault(), "%02d:%02d", pickedHour, pickedMinute)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reminders_add_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text(stringResource(R.string.reminders_field_title)) },
                    singleLine = true, shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text(stringResource(R.string.reminders_field_note)) },
                    singleLine = true, shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Row {
                    TextButton(onClick = { showDatePicker = true }, Modifier.weight(1f)) {
                        Text(dateFmt.format(Date(pickedDate)))
                    }
                    TextButton(onClick = { showTimePicker = true }, Modifier.weight(1f)) {
                        Text(timeText)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.reminders_repeat),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = repeatMode == Reminder.REPEAT_ONCE,
                        onClick = { repeatMode = Reminder.REPEAT_ONCE },
                        label = { Text(stringResource(R.string.repeat_once)) }
                    )
                    FilterChip(
                        selected = repeatMode == Reminder.REPEAT_DAILY,
                        onClick = { repeatMode = Reminder.REPEAT_DAILY },
                        label = { Text(stringResource(R.string.repeat_daily)) }
                    )
                    FilterChip(
                        selected = repeatMode == Reminder.REPEAT_WEEKLY,
                        onClick = { repeatMode = Reminder.REPEAT_WEEKLY },
                        label = { Text(stringResource(R.string.repeat_weekly)) }
                    )
                    FilterChip(
                        selected = repeatMode == Reminder.REPEAT_CUSTOM,
                        onClick = { repeatMode = Reminder.REPEAT_CUSTOM },
                        label = { Text(stringResource(R.string.repeat_custom)) }
                    )
                }
                if (repeatMode == Reminder.REPEAT_CUSTOM) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.reminders_custom_every, intervalMinutes / 60),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(30L, 60L, 120L, 240L, 1440L).forEach { mins ->
                            FilterChip(
                                selected = intervalMinutes == mins,
                                onClick = { intervalMinutes = mins },
                                label = { Text(if (mins >= 1440) "${mins / 1440}d" else "${mins / 60}h") }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val c = java.util.Calendar.getInstance().apply {
                        timeInMillis = pickedDate
                        set(java.util.Calendar.HOUR_OF_DAY, pickedHour)
                        set(java.util.Calendar.MINUTE, pickedMinute)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    onAdd(title, note, c.timeInMillis, repeatMode, intervalMinutes * 60_000L)
                },
                enabled = title.isNotBlank()
            ) { Text(stringResource(R.string.reminders_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = pickedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { pickedDate = it }
                    showDatePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) } }
        ) {
            DatePicker(state = state)
        }
    }

    if (showTimePicker) {
        val tstate = rememberTimePickerState(initialHour = pickedHour, initialMinute = pickedMinute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.reminders_pick_time)) },
            text = { TimePicker(state = tstate) },
            confirmButton = {
                TextButton(onClick = {
                    pickedHour = tstate.hour; pickedMinute = tstate.minute
                    showTimePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}
