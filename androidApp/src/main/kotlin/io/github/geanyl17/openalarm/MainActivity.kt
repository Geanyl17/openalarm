package io.github.geanyl17.openalarm

import android.Manifest
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import io.github.geanyl17.openalarm.ui.AlarmSounds
import io.github.geanyl17.openalarm.ui.App
import io.github.geanyl17.openalarm.ui.SetupIssue
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var setupIssues by mutableStateOf(emptyList<SetupIssue>())
    private var onSoundChosen: ((String?) -> Unit)? = null

    private val soundPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val picked = result.data?.let { IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java) }
        if (result.resultCode == RESULT_OK && picked != null) {
            // The default is stored as null, so it follows the phone's default if that changes later.
            onSoundChosen?.invoke(picked.takeUnless { it == defaultAlarmSound }?.toString())
        }
        onSoundChosen = null
    }

    private val defaultAlarmSound: Uri get() = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

    private val sounds = object : AlarmSounds {
        override fun name(sound: String): String? =
            runCatching { RingtoneManager.getRingtone(this@MainActivity, sound.toUri())?.getTitle(this@MainActivity) }.getOrNull()

        override fun choose(current: String?, onChosen: (String?) -> Unit) {
            onSoundChosen = onChosen
            soundPicker.launch(
                Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                    .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                    .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                    // An alarm must never be silent.
                    .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                    .putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, defaultAlarmSound)
                    .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, current?.toUri() ?: defaultAlarmSound),
            )
        }
    }

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // Once the user has said no twice, Android stops showing the dialog, so go to the settings screen instead.
        if (!granted && Build.VERSION.SDK_INT >= 33 && !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            openNotificationSettings()
        }
        setupIssues = missingSetup()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val graph = appGraph
        // "Force stop" in the system settings deletes the app's scheduled alarms; opening the app re-arms them.
        graph.scope.launch { graph.controller.reschedule() }
        setContent {
            App(
                controller = graph.controller,
                setupIssues = setupIssues,
                onFixSetupIssue = ::fixSetupIssue,
                sounds = sounds,
                use24Hour = DateFormat.is24HourFormat(this),
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // The user may have just come back from granting a permission in the system settings.
        setupIssues = missingSetup()
    }

    private fun fixSetupIssue(issue: SetupIssue) {
        when (issue) {
            SetupIssue.Notifications ->
                if (Build.VERSION.SDK_INT >= 33) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    openNotificationSettings()
                }
            SetupIssue.FullScreenAlarms ->
                if (Build.VERSION.SDK_INT >= 34) openAppSettings(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
            SetupIssue.ExactAlarms ->
                if (Build.VERSION.SDK_INT >= 31) openAppSettings(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        }
    }

    private fun openNotificationSettings() {
        startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
    }

    private fun openAppSettings(action: String) {
        startActivity(Intent(action, Uri.fromParts("package", packageName, null)))
    }
}
