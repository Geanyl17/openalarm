package io.github.geanyl17.openalarm

import android.Manifest
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import io.github.geanyl17.openalarm.missions.LocalMissionSensors
import io.github.geanyl17.openalarm.ringing.RingingBackup
import io.github.geanyl17.openalarm.ui.AlarmPhotos
import io.github.geanyl17.openalarm.ui.AlarmSounds
import io.github.geanyl17.openalarm.ui.App
import io.github.geanyl17.openalarm.ui.SetupIssue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var setupIssues by mutableStateOf(emptyList<SetupIssue>())
    private var onSoundChosen: ((String?) -> Unit)? = null
    private var onPhotoChosen: ((String) -> Unit)? = null

    private val soundPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val onChosen = onSoundChosen
        onSoundChosen = null
        val picked = result.data?.let { IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java) }
        if (result.resultCode != RESULT_OK || picked == null || onChosen == null) return@registerForActivityResult
        // The default is stored as null, so it follows the phone's default if that changes later.
        if (picked == defaultAlarmSound) onChosen(null) else copyInBackground({ AlarmMedia.soundForAlarm(this, picked) }, onChosen)
    }

    private val photoPicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val onChosen = onPhotoChosen
        onPhotoChosen = null
        if (uri != null && onChosen != null) copyInBackground({ AlarmMedia.importPhoto(this, uri) }, onChosen)
    }

    private val stepsPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        // Nothing to do either way: without the permission, steps are counted with the accelerometer.
    }

    private val sensors by lazy {
        AndroidMissionSensors(this) {
            if (Build.VERSION.SDK_INT >= 29) stepsPermission.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    private val defaultAlarmSound: Uri get() = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

    private val sounds = object : AlarmSounds {
        override fun name(sound: String): String? =
            AlarmMedia.localSoundName(this@MainActivity, sound)
                ?: runCatching { RingtoneManager.getRingtone(this@MainActivity, sound.toUri())?.getTitle(this@MainActivity) }.getOrNull()

        override fun choose(current: String?, onChosen: (String?) -> Unit) {
            onSoundChosen = onChosen
            soundPicker.launch(
                Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                    .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                    .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                    // An alarm must never be silent.
                    .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                    .putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, defaultAlarmSound)
                    .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, current?.let(AlarmMedia::pickerUri) ?: defaultAlarmSound),
            )
        }
    }

    private val photos = object : AlarmPhotos {
        override fun choose(onChosen: (String) -> Unit) {
            onPhotoChosen = onChosen
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        override suspend fun load(photo: String, maxSize: Int): ImageBitmap? =
            withContext(Dispatchers.IO) { AlarmMedia.loadPhoto(photo, maxSize) }
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
        val graph = appGraph
        // "Force stop" in the system settings deletes the app's scheduled alarms; opening the app re-arms them.
        graph.scope.launch { graph.controller.reschedule() }
        lifecycleScope.launch(Dispatchers.IO) {
            graph.controller.alarms.collect { AlarmMedia.deleteUnused(this@MainActivity, it) }
        }
        setContent {
            WithThemeSettings { theme ->
                CompositionLocalProvider(LocalMissionSensors provides sensors) {
                    App(
                        controller = graph.controller,
                        setupIssues = setupIssues,
                        onFixSetupIssue = ::fixSetupIssue,
                        sounds = sounds,
                        photos = photos,
                        use24Hour = DateFormat.is24HourFormat(this),
                        theme = theme,
                        wallpaperColor = wallpaperColor(this),
                        onThemeChange = graph::setTheme,
                        wakeUps = graph.wakeLog.wakeUps,
                    )
                }
            }
        }
    }

    /** Copies a picked file into the app off the main thread, then reports it, or says it couldn't be read. */
    private fun copyInBackground(copy: () -> String?, onCopied: (String) -> Unit) {
        lifecycleScope.launch {
            val copied = withContext(Dispatchers.IO) { copy() }
            if (copied != null) {
                onCopied(copied)
            } else {
                Toast.makeText(this@MainActivity, R.string.import_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The user may have just come back from granting a permission in the system settings.
        setupIssues = missingSetup()
        // Force-stopping the app doesn't turn off a ringing alarm: it rings again as soon as the app is opened.
        RingingBackup.ringAgainIfInterrupted(this, RingingBackup.reasonOnAppOpen(this))
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
            SetupIssue.DisplayOverApps -> openAppSettings(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        }
    }

    private fun openNotificationSettings() {
        startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
    }

    private fun openAppSettings(action: String) {
        startActivity(Intent(action, Uri.fromParts("package", packageName, null)))
    }
}
