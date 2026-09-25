package io.github.geanyl17.openalarm.ringing

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.geanyl17.openalarm.appGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Rings due alarms as a foreground service: plays the sound, holds a wake lock, and shows the
 * notification that brings up [RingingActivity]. Stops when the user snoozes or dismisses.
 */
class RingingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var player: AlarmPlayer
    private lateinit var wakeLock: PowerManager.WakeLock
    private var timeout: Job? = null
    private var heartbeat: Job? = null
    private var stopping = false

    override fun onCreate() {
        super.onCreate()
        player = AlarmPlayer(this)
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OpenAlarm:ringing")
            .apply { setReferenceCounted(false) }
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
            ACTION_SNOOZE -> scope.launch { finish { ids -> appGraph.controller.snooze(ids) } }
            ACTION_DISMISS -> scope.launch { finish { ids -> appGraph.controller.dismiss(ids) } }
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

        player.start(sound = ringing.first.sound, vibrate = ringing.alarms.any { it.vibrate }, fadeIn = ringing.first.fadeIn)
        heartbeat = scope.launch {
            while (true) {
                RingingBackup.arm(this@RingingService, at)
                delay(RingingBackup.delay / 2)
            }
        }
        timeout = scope.launch {
            delay(RING_TIMEOUT)
            // Nobody reacted. Snooze instead of ringing forever, so the alarm comes back.
            // It runs in a new coroutine because finish() cancels this one.
            scope.launch { finish { ids -> controller.snooze(ids) } }
        }
    }

    private suspend fun finish(action: suspend (ids: List<Long>) -> Unit) {
        val ringing = RingingSession.state.value
        stopping = true
        timeout?.cancel()
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

    private fun goForeground(ringing: Ringing?) {
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED else 0
        ServiceCompat.startForeground(this, RingingNotification.ID, RingingNotification.build(this, ringing), type)
    }

    companion object {
        const val ACTION_RING = "io.github.geanyl17.openalarm.action.RING"
        const val ACTION_SNOOZE = "io.github.geanyl17.openalarm.action.SNOOZE"
        const val ACTION_DISMISS = "io.github.geanyl17.openalarm.action.DISMISS"
        private const val EXTRA_TRIGGER_AT = "trigger_at"
        private val RING_TIMEOUT = 10.minutes
        private val WAKE_LOCK_TIMEOUT = RING_TIMEOUT + 1.minutes

        fun ring(context: Context, at: Instant) {
            ContextCompat.startForegroundService(
                context,
                intent(context, ACTION_RING).putExtra(EXTRA_TRIGGER_AT, at.toEpochMilliseconds()),
            )
        }

        fun intent(context: Context, action: String): Intent = Intent(context, RingingService::class.java).setAction(action)
    }
}
