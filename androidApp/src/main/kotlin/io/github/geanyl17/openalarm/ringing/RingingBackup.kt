package io.github.geanyl17.openalarm.ringing

import android.app.AlarmManager
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import io.github.geanyl17.openalarm.alarm.AlarmReceiver
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * While an alarm rings, [RingingService] keeps pushing this backup one minute into the future. If the app
 * dies mid-ring (a crash, the system killing it, or the user stopping it), the pushes stop and the backup
 * rings the alarm again.
 *
 * The ringing alarm is also saved to storage, so it rings again after a reboot, and after a force stop
 * as soon as the app is opened.
 *
 * A backup that fires after the alarm was snoozed or dismissed does nothing: those alarms are no longer
 * due at [triggerAt], so nothing rings.
 */
internal object RingingBackup {
    private const val TAG = "RingingBackup"
    private const val PREFERENCES = "ringing"
    private const val KEY_RINGING_AT = "ringing_at"
    val delay = 1.minutes

    fun arm(context: Context, triggerAt: Instant) = schedule(context, triggerAt, after = delay)

    /** Saves that the alarms due at [triggerAt] are ringing, until [cancel]. */
    fun remember(context: Context, triggerAt: Instant) {
        preferences(context).edit { putLong(KEY_RINGING_AT, triggerAt.toEpochMilliseconds()) }
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)
            .cancel(AlarmReceiver.pendingIntent(context, null, AlarmReceiver.REQUEST_BACKUP))
        preferences(context).edit { remove(KEY_RINGING_AT) }
    }

    /**
     * Rings again an alarm that was still ringing when the phone switched off or the app was stopped.
     * It goes through AlarmManager, which may start the ringing service where the app itself can't.
     */
    fun ringAgainIfInterrupted(context: Context) {
        if (RingingSession.state.value != null) return
        val at = preferences(context).getLong(KEY_RINGING_AT, -1)
        if (at >= 0) schedule(context, Instant.fromEpochMilliseconds(at), after = 1.seconds)
    }

    private fun schedule(context: Context, triggerAt: Instant, after: Duration) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + after.inWholeMilliseconds,
                AlarmReceiver.pendingIntent(context, triggerAt, AlarmReceiver.REQUEST_BACKUP),
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Not allowed to schedule exact alarms", e)
        }
    }

    // Device-protected, like the alarms themselves, so it can be read before the first unlock after a reboot.
    private fun preferences(context: Context): SharedPreferences =
        context.createDeviceProtectedStorageContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
}
