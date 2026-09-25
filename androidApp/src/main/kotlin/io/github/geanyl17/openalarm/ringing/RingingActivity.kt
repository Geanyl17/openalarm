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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.geanyl17.openalarm.ui.RingingScreen

/** The full-screen ringing alarm, shown over the lock screen. Closes itself once the alarm stops. */
class RingingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        enableEdgeToEdge()
        // Back must not make a ringing alarm go away.
        onBackPressedDispatcher.addCallback(this) {}
        val use24Hour = DateFormat.is24HourFormat(this)

        setContent {
            val ringing by RingingSession.state.collectAsState()
            val current = ringing
            if (current == null) {
                LaunchedEffect(Unit) { finish() }
            } else {
                RingingScreen(
                    label = current.label,
                    colorArgb = current.first.colorArgb,
                    snoozeMinutes = current.first.snoozeMinutes,
                    use24Hour = use24Hour,
                    onSnooze = { startService(RingingService.intent(this, RingingService.ACTION_SNOOZE)) },
                    onDismiss = { startService(RingingService.intent(this, RingingService.ACTION_DISMISS)) },
                )
            }
        }
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
        fun intent(context: Context): Intent =
            Intent(context, RingingActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
    }
}
