package io.github.geanyl17.openalarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.geanyl17.openalarm.MainActivity
import io.github.geanyl17.openalarm.core.AlarmScheduler
import io.github.geanyl17.openalarm.core.UpcomingAlarm

private const val TAG = "AlarmScheduler"

/**
 * Schedules only the next alarm, with [AlarmManager.setAlarmClock]: it fires even in Doze and
 * shows the alarm icon in the status bar. After it rings, the next one gets scheduled.
 */
class AndroidAlarmScheduler(private val context: Context) : AlarmScheduler {

    override fun schedule(next: UpcomingAlarm?) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val fire = AlarmReceiver.pendingIntent(context, next?.at)
        if (next == null) {
            alarmManager.cancel(fire)
            Log.i(TAG, "No alarm is on")
            return
        }
        // Tapping the alarm icon on the lock screen or in quick settings opens the app.
        val showAlarms = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        try {
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(next.at.toEpochMilliseconds(), showAlarms), fire)
            Log.i(TAG, "Next alarm: #${next.alarm.id} at ${next.at}")
        } catch (e: SecurityException) {
            // Only happens on Android 12 if the user turned exact alarms off; the setup card asks them to allow it.
            Log.e(TAG, "Not allowed to schedule exact alarms", e)
        }
    }
}
