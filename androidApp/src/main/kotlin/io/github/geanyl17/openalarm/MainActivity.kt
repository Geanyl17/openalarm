package io.github.geanyl17.openalarm

import android.Manifest
import android.content.Intent
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
import io.github.geanyl17.openalarm.ui.App
import io.github.geanyl17.openalarm.ui.SetupIssue
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var setupIssues by mutableStateOf(emptyList<SetupIssue>())

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
