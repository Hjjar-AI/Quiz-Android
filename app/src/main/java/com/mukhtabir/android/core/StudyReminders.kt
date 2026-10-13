package com.mukhtabir.android.core

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mukhtabir.android.MainActivity
import com.mukhtabir.android.R
import java.time.ZonedDateTime

/** Local, opt-in, generic reminders. No credentials, exam data or answer drafts are stored. */
class StudyReminders(context: Context) {
    private val context = context.applicationContext
    private val prefs = this.context.getSharedPreferences("study_reminders", Context.MODE_PRIVATE)
    val enabled: Boolean get() = prefs.getBoolean("enabled", false)
    val hour: Int get() = prefs.getInt("hour", 19).coerceIn(0, 23)
    val minute: Int get() = prefs.getInt("minute", 0).coerceIn(0, 59)
    private val owner: Long get() = prefs.getLong("owner", 0)

    fun bind(userId: Long) {
        require(userId > 0)
        if (owner != userId) {
            clear()
            check(prefs.edit().putLong("owner", userId).commit())
        }
        if (enabled) schedule()
    }

    fun update(userId: Long, enabled: Boolean, hour: Int, minute: Int): Result<Unit> = runCatching {
        require(userId > 0 && userId == owner && hour in 0..23 && minute in 0..59)
        if (enabled) check(notificationsAllowed())
        cancelAlarm()
        check(prefs.edit().putBoolean("enabled", enabled).putInt("hour", hour).putInt("minute", minute).commit())
        try { if (enabled) schedule() } catch (error: Exception) {
            prefs.edit().putBoolean("enabled", false).commit()
            throw error
        }
    }

    fun clear() {
        // Cancellation happens even if storage is temporarily unavailable.
        cancelAlarm()
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        check(prefs.edit().clear().commit())
    }

    fun notificationsAllowed(): Boolean {
        channel()
        val permitted = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context,
            Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val manager = context.getSystemService(NotificationManager::class.java)
        val channelEnabled = Build.VERSION.SDK_INT < 26 || manager.getNotificationChannel(CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE
        return permitted && channelEnabled && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    internal fun schedule() {
        if (!enabled || owner <= 0 || !hasSession()) return
        val now = ZonedDateTime.now()
        var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        context.getSystemService(AlarmManager::class.java).set(AlarmManager.RTC_WAKEUP,
            next.toInstant().toEpochMilli(), alarmIntent())
    }

    internal fun deliver() {
        if (!enabled || owner <= 0 || !hasSession()) return
        if (notificationsAllowed()) {
            val open = PendingIntent.getActivity(context, NOTIFICATION_ID,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(AppPreferences.wrapContext(context).getString(R.string.reminder_notification_title))
                .setContentText(AppPreferences.wrapContext(context).getString(R.string.reminder_notification_body))
                .setContentIntent(open).setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT).build()
            try { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
            catch (_: SecurityException) { /* Permission may have been revoked after the check. */ }
        }
        schedule()
    }

    private fun hasSession(): Boolean = runCatching {
        com.mukhtabir.android.data.remote.PersistentCookieJar(context).hasSessionCookie()
    }.getOrDefault(false)

    private fun alarmIntent(): PendingIntent = PendingIntent.getBroadcast(context, NOTIFICATION_ID,
        Intent(context, StudyReminderReceiver::class.java).setAction(ACTION_REMIND),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private fun cancelAlarm() { context.getSystemService(AlarmManager::class.java).cancel(alarmIntent()) }
    private fun channel() {
        if (Build.VERSION.SDK_INT >= 26) context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(CHANNEL_ID, AppPreferences.wrapContext(context).getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT))
    }
    companion object {
        const val ACTION_REMIND = "com.mukhtabir.android.STUDY_REMINDER"
        private const val CHANNEL_ID = "study_reminders"
        private const val NOTIFICATION_ID = 8101
    }
}

class StudyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val localized = AppPreferences.wrapContext(context)
        val reminders = StudyReminders(localized)
        try {
            if (intent.action == StudyReminders.ACTION_REMIND) reminders.deliver()
            else if (intent.action in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED)) reminders.schedule()
        } catch (_: Exception) { /* A later authenticated launch can reschedule. */ }
    }
}
