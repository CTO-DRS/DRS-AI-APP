package com.drs.ai.core.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.drs.ai.MainActivity
import com.drs.ai.R
import com.drs.ai.data.dao.ReminderDao
import com.drs.ai.data.db.Reminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Local reminder scheduling via AlarmManager. 100% on-device: no network, no accounts,
 * no analytics — a reminder is just a timestamp and an alarm.
 */
class ReminderScheduler(private val context: Context, private val dao: ReminderDao) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val alarmManager get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(reminder: Reminder) {
        if (!reminder.enabled || reminder.triggerAt <= System.currentTimeMillis()) return
        val pi = pendingIntent(reminder.id)
        val exact = if (Build.VERSION.SDK_INT >= 31) alarmManager.canScheduleExactAlarms() else true
        if (exact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.triggerAt, pi)
        } else {
            // honest fallback: inexact window instead of failing silently
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, reminder.triggerAt, 10 * 60_000L, pi)
        }
    }

    fun cancel(reminder: Reminder) {
        alarmManager.cancel(pendingIntent(reminder.id))
    }

    /** Re-arm every enabled future reminder (used after boot and after edits). */
    fun scheduleAll() {
        scope.launch {
            dao.upcoming(System.currentTimeMillis()).forEach { schedule(it) }
        }
    }

    suspend fun upsertAndSchedule(reminder: Reminder) {
        if (reminder.id != 0L) {
            dao.update(reminder)
            schedule(reminder)
        } else {
            val id = dao.insert(reminder)
            schedule(reminder.copy(id = id))
        }
    }

    suspend fun complete(reminder: Reminder) {
        cancel(reminder)
        when (reminder.repeatMode) {
            Reminder.REPEAT_ONCE -> dao.delete(reminder)
            Reminder.REPEAT_DAILY -> advance(reminder, 24 * 3600_000L)
            Reminder.REPEAT_WEEKLY -> advance(reminder, 7 * 24 * 3600_000L)
            Reminder.REPEAT_CUSTOM -> advance(reminder, reminder.intervalMillis.coerceAtLeast(60_000L))
        }
    }

    private suspend fun advance(reminder: Reminder, delta: Long) {
        var next = reminder.triggerAt + delta
        while (next <= System.currentTimeMillis()) next += delta
        val updated = reminder.copy(triggerAt = next)
        dao.update(updated)
        schedule(updated)
    }

    private fun pendingIntent(id: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_FIRE)
            .putExtra(ReminderReceiver.EXTRA_ID, id)
        return PendingIntent.getBroadcast(
            context, id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        fun canScheduleExact(context: Context): Boolean =
            if (Build.VERSION.SDK_INT >= 31) {
                (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
            } else true
    }
}

/** Fires the notification when an alarm goes off; re-arms repeating reminders. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val id = intent.getLongExtra(EXTRA_ID, -1)
        if (id <= 0) return

        val appContext = context.applicationContext
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val db = com.drs.ai.data.db.DrsDatabase.get(appContext)
                val reminder = db.reminderDao().byId(id)
                if (reminder != null && reminder.enabled) {
                    notify(appContext, reminder)
                    if (reminder.repeatMode != Reminder.REPEAT_ONCE) {
                        // advance to the next occurrence
                        val delta = when (reminder.repeatMode) {
                            Reminder.REPEAT_DAILY -> 24 * 3600_000L
                            Reminder.REPEAT_WEEKLY -> 7 * 24 * 3600_000L
                            else -> reminder.intervalMillis.coerceAtLeast(60_000L)
                        }
                        var next = reminder.triggerAt + delta
                        while (next <= System.currentTimeMillis()) next += delta
                        val updated = reminder.copy(triggerAt = next)
                        db.reminderDao().update(updated)
                        ReminderScheduler(appContext, db.reminderDao()).schedule(updated)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun notify(context: Context, reminder: Reminder) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val format = SimpleDateFormat("EEE HH:mm", Locale.getDefault())
        val open = Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val contentPi = PendingIntent.getActivity(
            context, (reminder.id % 100000).toInt(), open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val text = buildString {
            if (reminder.note.isNotBlank()) { append(reminder.note.trim()); append("\n") }
            append(format.format(Date(reminder.triggerAt)))
            if (reminder.repeatMode != Reminder.REPEAT_ONCE) {
                append(" • ")
                append(Reminder.repeatLabel(reminder.repeatMode, reminder.intervalMillis))
            }
        }
        val notification = NotificationCompat.Builder(context, ReminderNotifications.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle(reminder.title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .build()
        try {
            nm.notify(reminder.id.toInt(), notification)
        } catch (_: SecurityException) {
            // notifications disabled — the reminder remains visible in the app
        }
    }

    companion object {
        const val ACTION_FIRE = "com.drs.ai.reminders.FIRE"
        const val EXTRA_ID = "reminder_id"
    }
}

/** Re-arms alarms after a reboot (alarms do not survive restarts). */
class BootReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val db = com.drs.ai.data.db.DrsDatabase.get(context.applicationContext)
        ReminderScheduler(context.applicationContext, db.reminderDao()).scheduleAll()
    }
}

/** Notification channel + helper. */
object ReminderNotifications {
    const val CHANNEL_ID = "drs_reminders"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminders_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.reminders_channel_desc)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}
