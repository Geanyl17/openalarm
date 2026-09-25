package io.github.geanyl17.openalarm.ringing

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import io.github.geanyl17.openalarm.ui.RingingScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

/** The full-screen ringing alarm, shown over the lock screen. Closes itself once the alarm stops. */
class RingingActivity : ComponentActivity() {
    private var startWithMission by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        enableEdgeToEdge()
        // Back must not make a ringing alarm go away.
        onBackPressedDispatcher.addCallback(this) {}
        startWithMission = intent.getBooleanExtra(EXTRA_START_MISSION, false)
        val use24Hour = DateFormat.is24HourFormat(this)
        lifecycleScope.launch { finishWhenRingingStops() }

        setContent {
            val ringing by RingingSession.state.collectAsState()
            // Null for a moment while the ringing service loads the alarm; the screen stays blank until then.
            ringing?.let { current ->
                RingingScreen(
                    label = current.label,
                    colorArgb = current.first.colorArgb,
                    snoozeMinutes = current.first.snoozeMinutes,
                    missions = current.missions,
                    use24Hour = use24Hour,
                    startWithMission = startWithMission,
                    onSnooze = { startService(RingingService.intent(this, RingingService.ACTION_SNOOZE)) },
                    onDismiss = { startService(RingingService.intent(this, RingingService.ACTION_DISMISS)) },
                    onMissionInteraction = RingingSession::missionInteraction,
                )
            }
        }
    }

    /**
     * The notification that opens this screen is posted before the service has loaded the alarm, so
     * ringing may not have started yet. Wait for it (unless it never starts, as with a stale
     * notification), then close once it stops.
     */
    private suspend fun finishWhenRingingStops() {
        withTimeoutOrNull(RINGING_START_TIMEOUT) { RingingSession.state.first { it != null } }
        RingingSession.state.first { it == null }
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // The notification's "Turn off" button was tapped while this screen was already open.
        if (intent.getBooleanExtra(EXTRA_START_MISSION, false)) startWithMission = true
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    companion object {
        private const val EXTRA_START_MISSION = "start_mission"
        private val RINGING_START_TIMEOUT = 5.seconds

        /** Opens the ringing screen; with [startMission], it goes straight to the alarm's missions. */
        fun intent(context: Context, startMission: Boolean = false): Intent =
            Intent(context, RingingActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
                .putExtra(EXTRA_START_MISSION, startMission)
    }
}
