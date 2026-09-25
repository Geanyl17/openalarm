package io.github.geanyl17.openalarm.ringing

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.geanyl17.openalarm.appGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Rings due alarms as a foreground service: plays the sound, holds a wake lock, and shows the
 * notification that brings up [RingingActivity]. Stops when the user snoozes or dismisses.
 *
 * A ringing alarm can't be pushed aside. Its screen comes back when the user leaves it, its volume can't
 * be turned down, its notification can't be swiped away, and it rings again after a reboot.
 */
class RingingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var player: AlarmPlayer
    private lateinit var audioManager: AudioManager
    private lateinit var power: PowerManager
    private lateinit var wakeLock: PowerManager.WakeLock
    private var timeout: Job? = null
    private var heartbeat: Job? = null
    private var quietWhileSolving: Job? = null
    private var keepOnScreen: Job? = null
    private var stopping = false
    private var notificationId = RingingNotification.ID
    private var notificationAlerts = true

    override fun onCreate() {
        super.onCreate()
        player = AlarmPlayer(this)
        audioManager = getSystemService(AudioManager::class.java)
        power = getSystemService(PowerManager::class.java)
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OpenAlarm:ringing").apply { setReferenceCounted(false) }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RING -> {
                // Android requires startForeground() right away; the notification is filled in once the alarms load.
                goForeground(RingingSession.state.value)
                wakeLock.acquire(WAKE_LOCK_TIMEOUT.inWholeMilliseconds)
                StartupWakeLock.release()
                stopping = false
                val at = Instant.fromEpochMilliseconds(intent.getLongExtra(EXTRA_TRIGGER_AT, System.currentTimeMillis()))
                scope.launch { ring(at) }
            }
            // Past the snooze limit, only turning the alarm off stops it.
            ACTION_SNOOZE -> if (RingingSession.state.value?.snoozeMinutes != null) scope.launch { finish { ids -> appGraph.controller.snooze(ids) } }
            ACTION_DISMISS -> scope.launch { finish { ids -> appGraph.controller.dismiss(ids) } }
            // The notification was swiped away, but the alarm is still ringing: it comes straight back.
            ACTION_SHOW_AGAIN -> if (RingingSession.state.value != null) updateNotification(notificationAlerts) else stopSelf()
            else -> if (RingingSession.state.value == null) stopSelf()
        }
        return START_NOT_STICKY
    }

    private suspend fun ring(at: Instant) {
        val controller = appGraph.controller
        val due = controller.dueAt(at)
        // Schedule the following alarm right away, so a long ring can't make the app miss it.
        controller.reschedule()
        if (stopping) return

        val current = RingingSession.state.value
        if (due.isEmpty()) {
            // The alarm was changed or deleted at the last moment.
            if (current == null) stopRinging()
            return
        }
        val ringing = Ringing((current?.alarms.orEmpty() + due).distinctBy { it.id })
        RingingSession.start(ringing)
        goForeground(ringing)
        // If something was already ringing, the new alarm just joins in.
        if (current != null) return

        // Until it's turned off or snoozed, the alarm rings again after a reboot or if the app is stopped.
        RingingBackup.remember(this, at)
        // Open the ringing screen straight away. Android allows that only while OpenAlarm is on screen or may display
        // over other apps; otherwise the notification opens it: full screen when locked, as a banner when in use.
        startActivity(RingingActivity.intent(this))
        player.start(sound = ringing.first.sound, vibrate = ringing.alarms.any { it.vibrate }, fadeIn = ringing.first.fadeIn)
        heartbeat = scope.launch {
            while (true) {
                RingingBackup.arm(this@RingingService, at)
                delay(RingingBackup.delay / 2)
            }
        }
        restartTimeout()
        quietWhileSolving = scope.launch {
            RingingSession.missionInteractions.collectLatest {
                // Someone is working on the mission: keep the alarm quiet and don't give up on them.
                restartTimeout()
                player.setQuiet(true)
                delay(QUIET_WHILE_SOLVING)
                // They stopped, so the alarm gets loud again.
                player.setQuiet(false)
            }
        }
        keepOnScreen = scope.launch { keepOnScreen() }
    }

    /**
     * Keeps the ringing alarm in front until it's turned off or snoozed: leave the ringing screen (Home, the
     * app switcher, another app) and it comes straight back. The volume can't be turned down either. Calls
     * are the exception: during a phone call or an emergency call, the alarm stays silent and out of the way.
     */
    private suspend fun keepOnScreen() {
        RingingSession.screenVisible.collectLatest { visible ->
            if (visible) {
                if (notificationAlerts) replaceNotification()
            } else {
                // Reopening the ringing screen also takes it out of the front for a few milliseconds.
                // Reacting to that would reopen it again, and again.
                delay(COMEBACK_DELAY)
                // Without the permission to bring the ringing screen back, the notification has to call the user back.
                if (!notificationAlerts && !Settings.canDrawOverlays(this)) updateNotification(alert = true)
            }
            while (true) {
                val mode = audioManager.mode
                val emergencyCall = RingingSession.onEmergencyCall
                player.setSilenced(emergencyCall || mode == AudioManager.MODE_RINGTONE || mode == AudioManager.MODE_IN_CALL)
                player.keepVolumeUp()
                // Other calls, such as video calls, only keep the ringing screen from covering them.
                if (!visible && !emergencyCall && mode == AudioManager.MODE_NORMAL) bringBack()
                delay(if (visible) CHECK_INTERVAL else COMEBACK_INTERVAL)
            }
        }
    }

    /** Opens the ringing screen again. From the background, Android allows that only with "display over other apps". */
    private fun bringBack() {
        // With the screen off, it can wait until the screen is turned on again.
        if (!power.isInteractive || !Settings.canDrawOverlays(this)) return
        startActivity(RingingActivity.intent(this))
    }

    private fun restartTimeout() {
        wakeLock.acquire(WAKE_LOCK_TIMEOUT.inWholeMilliseconds)
        timeout?.cancel()
        timeout = scope.launch {
            delay(RING_TIMEOUT)
            // Nobody reacted. Snooze instead of ringing forever, so the alarm comes back.
            // It runs in a new coroutine because finish() cancels this one.
            scope.launch { finish { ids -> appGraph.controller.snooze(ids) } }
        }
    }

    private suspend fun finish(action: suspend (ids: List<Long>) -> Unit) {
        val ringing = RingingSession.state.value
        stopping = true
        keepOnScreen?.cancel()
        timeout?.cancel()
        quietWhileSolving?.cancel()
        heartbeat?.cancel()
        RingingBackup.cancel(this)
        player.stop()
        RingingSession.end()
        if (ringing != null) action(ringing.alarms.map { it.id })
        stopRinging()
    }

    private fun stopRinging() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        if (wakeLock.isHeld) wakeLock.release()
        StartupWakeLock.release()
        stopSelf()
    }

    override fun onDestroy() {
        player.stop()
        RingingSession.end()
        if (wakeLock.isHeld) wakeLock.release()
        scope.cancel()
        super.onDestroy()
    }

    // A banner on top of the ringing screen would only cover it, so the notification alerts only while the screen isn't showing.
    private fun goForeground(ringing: Ringing?) {
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED else 0
        ServiceCompat.startForeground(this, notificationId, notification(ringing, alert = !RingingSession.screenVisible.value), type)
    }

    private fun updateNotification(alert: Boolean) {
        getSystemService(NotificationManager::class.java)
            .notify(notificationId, notification(RingingSession.state.value, alert))
    }

    /**
     * Swaps an alerting notification for a quiet one under another ID. Updating it in place isn't enough: Android
     * keeps a banner it has already shown, for example after the ringing screen opened over the lock screen and
     * the phone was unlocked. Removing the old notification takes its banner with it.
     */
    private fun replaceNotification() {
        val old = notificationId
        notificationId = if (old == RingingNotification.ID) RingingNotification.OTHER_ID else RingingNotification.ID
        goForeground(RingingSession.state.value)
        getSystemService(NotificationManager::class.java).cancel(old)
    }

    private fun notification(ringing: Ringing?, alert: Boolean): Notification {
        notificationAlerts = alert
        return RingingNotification.build(this, ringing, alert)
    }

    companion object {
        const val ACTION_RING = "io.github.geanyl17.openalarm.action.RING"
        const val ACTION_SNOOZE = "io.github.geanyl17.openalarm.action.SNOOZE"
        const val ACTION_DISMISS = "io.github.geanyl17.openalarm.action.DISMISS"
        const val ACTION_SHOW_AGAIN = "io.github.geanyl17.openalarm.action.SHOW_AGAIN"
        private const val EXTRA_TRIGGER_AT = "trigger_at"
        private val RING_TIMEOUT = 10.minutes
        private val WAKE_LOCK_TIMEOUT = RING_TIMEOUT + 1.minutes
        private val QUIET_WHILE_SOLVING = 20.seconds
        private val CHECK_INTERVAL = 1.seconds
        private val COMEBACK_DELAY = 200.milliseconds
        private val COMEBACK_INTERVAL = 500.milliseconds

        fun ring(context: Context, at: Instant) {
            ContextCompat.startForegroundService(
                context,
                intent(context, ACTION_RING).putExtra(EXTRA_TRIGGER_AT, at.toEpochMilliseconds()),
            )
        }

        fun intent(context: Context, action: String): Intent = Intent(context, RingingService::class.java).setAction(action)
    }
}
