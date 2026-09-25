package io.github.geanyl17.openalarm.ringing

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.lifecycleScope
import io.github.geanyl17.openalarm.AlarmMedia
import io.github.geanyl17.openalarm.WithThemeSettings
import io.github.geanyl17.openalarm.wallpaperColor
import io.github.geanyl17.openalarm.ui.RingingScreen
import io.github.geanyl17.openalarm.ui.theme.ProvideAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

/** The full-screen ringing alarm, shown over the lock screen. Closes itself once the alarm stops. */
class RingingActivity : ComponentActivity() {
    private var startWithMission by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        // Back must not make a ringing alarm go away.
        onBackPressedDispatcher.addCallback(this) {}
        startWithMission = intent.getBooleanExtra(EXTRA_START_MISSION, false)
        val use24Hour = DateFormat.is24HourFormat(this)
        lifecycleScope.launch { finishWhenRingingStops() }

        setContent {
            val ringing by RingingSession.state.collectAsState()
            // Null for a moment while the ringing service loads the alarm; the screen stays blank until then.
            ringing?.let { current ->
                val photo by produceState<ImageBitmap?>(null, current.photo) {
                    value = current.photo?.let { withContext(Dispatchers.IO) { AlarmMedia.loadPhoto(it, AlarmMedia.PHOTO_SIZE) } }
                }
                WithThemeSettings { theme ->
                    ProvideAppTheme(theme, wallpaperColor(this)) {
                        RingingScreen(
                            label = current.label,
                            colorArgb = current.first.colorArgb,
                            photo = photo,
                            snoozeMinutes = current.snoozeMinutes,
                            snoozesLeft = current.snoozesLeft,
                            missions = current.missions,
                            use24Hour = use24Hour,
                            startWithMission = startWithMission,
                            onSnooze = { startService(RingingService.intent(this, RingingService.ACTION_SNOOZE)) },
                            onDismiss = { startService(RingingService.intent(this, RingingService.ACTION_DISMISS)) },
                            onMissionInteraction = RingingSession::missionInteraction,
                            onEmergencyCall = ::callEmergencyServices,
                            checkInUntil = current.checkInUntil,
                            onCheckedIn = { startService(RingingService.intent(this, RingingService.ACTION_CHECKED_IN)) },
                        )
                    }
                }
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

    // "On screen" means in front of everything: the app switcher, for one, keeps this screen visible
    // behind it. Before Android 10 there's no way to tell, so being started has to do.
    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT < 29) RingingSession.setScreenVisible(true)
    }

    override fun onTopResumedActivityChanged(isTopResumedActivity: Boolean) {
        super.onTopResumedActivityChanged(isTopResumedActivity)
        RingingSession.setScreenVisible(isTopResumedActivity)
    }

    override fun onStop() {
        super.onStop()
        RingingSession.setScreenVisible(false)
    }

    // The volume can't be turned down while the alarm rings. RingingService also puts it back up
    // if it's lowered some other way.
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean = keyCode.isVolumeDown() || super.onKeyDown(keyCode, event)

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean = keyCode.isVolumeDown() || super.onKeyUp(keyCode, event)

    private fun Int.isVolumeDown() = this == KeyEvent.KEYCODE_VOLUME_DOWN || this == KeyEvent.KEYCODE_VOLUME_MUTE

    /** Opens the emergency dialer. The alarm stays silent and lets the phone be used for a while. */
    private fun callEmergencyServices() {
        RingingSession.emergencyCall()
        // The phone app's emergency dialer works over the lock screen. A phone without it gets the
        // regular dialer, and a locked one offers its own emergency call button on the way.
        for (dialer in listOf(Intent(ACTION_EMERGENCY_DIAL), Intent(Intent.ACTION_DIAL))) {
            try {
                startActivity(dialer)
                return
            } catch (e: ActivityNotFoundException) {
                Log.w(TAG, "No activity for ${dialer.action}", e)
            }
        }
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
        private const val TAG = "RingingActivity"
        private const val ACTION_EMERGENCY_DIAL = "com.android.phone.EmergencyDialer.DIAL"
        private const val EXTRA_START_MISSION = "start_mission"
        private val RINGING_START_TIMEOUT = 5.seconds

        /** Opens the ringing screen; with [startMission], it goes straight to the alarm's missions. */
        fun intent(context: Context, startMission: Boolean = false): Intent =
            Intent(context, RingingActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
                .putExtra(EXTRA_START_MISSION, startMission)
    }
}
