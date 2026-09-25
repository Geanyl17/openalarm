package io.github.geanyl17.openalarm.ringing

import android.app.AlarmManager
import android.content.Context
import android.util.Log
import io.github.geanyl17.openalarm.alarm.AlarmReceiver
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * While an alarm rings, [RingingService] keeps pushing this backup one minute into the future. If the app
 * dies mid-ring (a crash, or the system killing it), the pushes stop and the backup rings the alarm again.
 *
 * A backup that fires after the alarm was snoozed or dismissed does nothing: those alarms are no longer
 * due at [triggerAt], so nothing rings.
 */
internal object RingingBackup {
    private const val TAG = "RingingBackup"
    val delay = 1.minutes

    fun arm(context: Context, triggerAt: Instant) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + delay.inWholeMilliseconds,
                AlarmReceiver.pendingIntent(context, triggerAt, AlarmReceiver.REQUEST_BACKUP),
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Not allowed to schedule exact alarms", e)
        }
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)
            .cancel(AlarmReceiver.pendingIntent(context, null, AlarmReceiver.REQUEST_BACKUP))
    }
}
