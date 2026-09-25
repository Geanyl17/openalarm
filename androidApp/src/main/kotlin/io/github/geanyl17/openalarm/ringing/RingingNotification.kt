package io.github.geanyl17.openalarm.ringing

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import io.github.geanyl17.openalarm.R

/** The notification shown while an alarm rings. Its full-screen intent opens [RingingActivity] over the lock screen. */
object RingingNotification {
    const val ID = 1
    private const val CHANNEL_ID = "ringing"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_ringing),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_ringing_description)
            // RingingService plays the sound itself, on the alarm volume.
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun build(context: Context, ringing: Ringing?): Notification {
        val ringingScreen = PendingIntent.getActivity(
            context,
            0,
            RingingActivity.intent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(ringing?.label?.ifBlank { null } ?: context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_text))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(ringingScreen)
            .setFullScreenIntent(ringingScreen, true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(0, context.getString(R.string.action_snooze), serviceAction(context, RingingService.ACTION_SNOOZE, 1))
        if (ringing?.missions.isNullOrEmpty()) {
            builder.addAction(0, context.getString(R.string.action_dismiss), serviceAction(context, RingingService.ACTION_DISMISS, 2))
        } else {
            // No dismiss button: the alarm only turns off once its mission is done on the ringing screen.
            val startMission = PendingIntent.getActivity(
                context,
                3,
                RingingActivity.intent(context, startMission = true),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(0, context.getString(R.string.action_turn_off), startMission)
        }
        ringing?.first?.colorArgb?.let { builder.setColor(it) }
        return builder.build()
    }

    private fun serviceAction(context: Context, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(context, requestCode, RingingService.intent(context, action), PendingIntent.FLAG_IMMUTABLE)
}
