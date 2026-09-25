package io.github.geanyl17.openalarm.ringing

import android.app.AlarmManager
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import io.github.geanyl17.openalarm.alarm.AlarmReceiver
import io.github.geanyl17.openalarm.core.Escape
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
    private const val KEY_INTERRUPTED_BY = "interrupted_by"
    val delay = 1.minutes

    fun arm(context: Context, triggerAt: Instant) = schedule(context, triggerAt, after = delay)

    /** Saves that the alarms due at [triggerAt] are ringing, until [cancel]. */
    fun remember(context: Context, triggerAt: Instant) {
        preferences(context).edit { putLong(KEY_RINGING_AT, triggerAt.toEpochMilliseconds()) }
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)
            .cancel(AlarmReceiver.pendingIntent(context, null, AlarmReceiver.REQUEST_BACKUP))
        preferences(context).edit {
            remove(KEY_RINGING_AT)
            remove(KEY_INTERRUPTED_BY)
        }
    }

    /**
     * Rings again an alarm that was still ringing when the phone switched off or the app was stopped.
     * It goes through AlarmManager, which may start the ringing service where the app itself can't.
     * [reason] is what the Honesty Log will say happened, or null if it's nothing to log (an app update).
     */
    fun ringAgainIfInterrupted(context: Context, reason: Escape?) {
        if (RingingSession.state.value != null) return
        val preferences = preferences(context)
        val at = preferences.getLong(KEY_RINGING_AT, -1)
        if (at < 0) return
        // The first reason found is the right one: opening the app after a reboot doesn't make it a force stop.
        if (!preferences.contains(KEY_INTERRUPTED_BY)) preferences.edit { putString(KEY_INTERRUPTED_BY, reason?.name.orEmpty()) }
        schedule(context, Instant.fromEpochMilliseconds(at), after = 1.seconds)
    }

    /**
     * Why the app is being opened with an alarm still ringing. A force stop deletes the app's pending
     * alarms, the backup included, while the Active apps list's Stop button leaves the backup to ring.
     */
    fun reasonOnAppOpen(context: Context): Escape =
        if (AlarmReceiver.exists(context, AlarmReceiver.REQUEST_BACKUP)) Escape.AppStopped else Escape.ForceStop

    /**
     * Whether the alarms due at [triggerAt] were already ringing when the app stopped, and if so why, as far
     * as the Honesty Log is concerned. The backup ringing on its own means the app was stopped.
     */
    fun interruption(context: Context, triggerAt: Instant): Escape? {
        val preferences = preferences(context)
        if (preferences.getLong(KEY_RINGING_AT, -1) != triggerAt.toEpochMilliseconds()) return null
        val reason = preferences.getString(KEY_INTERRUPTED_BY, null)
        preferences.edit { remove(KEY_INTERRUPTED_BY) }
        return when {
            reason == null -> Escape.AppStopped
            reason.isEmpty() -> null
            else -> Escape.entries.find { it.name == reason }
        }
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
