package io.github.geanyl17.openalarm.alarm

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.geanyl17.openalarm.ringing.RingingService
import io.github.geanyl17.openalarm.ringing.StartupWakeLock
import kotlin.time.Instant

/** Woken by AlarmManager when an alarm is due. Hands over to [RingingService] straight away. */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val at = intent.getLongExtra(EXTRA_TRIGGER_AT, -1)
        if (at < 0) return
        Log.i(TAG, "Alarm fired for ${Instant.fromEpochMilliseconds(at)}")
        // The system only keeps the CPU awake until onReceive returns; this bridges the gap
        // until the ringing service holds its own wake lock.
        StartupWakeLock.acquire(context)
        RingingService.ring(context, Instant.fromEpochMilliseconds(at))
    }

    companion object {
        private const val TAG = "AlarmReceiver"
        private const val ACTION_FIRE = "io.github.geanyl17.openalarm.action.FIRE"
        private const val EXTRA_TRIGGER_AT = "trigger_at"

        /** The next scheduled alarm. */
        const val REQUEST_NEXT = 0

        /** The backup that re-rings an alarm if the app dies while it's ringing. */
        const val REQUEST_BACKUP = 1

        /** Whether the PendingIntent for [requestCode] exists. A force stop deletes them all. */
        fun exists(context: Context, requestCode: Int): Boolean = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, AlarmReceiver::class.java).setAction(ACTION_FIRE),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) != null

        /** Fires for the alarms due at [at]. [requestCode] keeps the next alarm and the backup apart. */
        fun pendingIntent(context: Context, at: Instant?, requestCode: Int = REQUEST_NEXT): PendingIntent {
            val intent = Intent(context, AlarmReceiver::class.java).setAction(ACTION_FIRE)
            if (at != null) intent.putExtra(EXTRA_TRIGGER_AT, at.toEpochMilliseconds())
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
