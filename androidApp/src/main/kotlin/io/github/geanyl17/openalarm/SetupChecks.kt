package io.github.geanyl17.openalarm

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import io.github.geanyl17.openalarm.ui.SetupIssue

/** The permissions alarms still need, in the order the user should fix them. */
internal fun Context.missingSetup(): List<SetupIssue> = buildList {
    // Also false when the POST_NOTIFICATIONS permission hasn't been granted on Android 13+.
    if (!NotificationManagerCompat.from(this@missingSetup).areNotificationsEnabled()) {
        add(SetupIssue.Notifications)
    }
    if (Build.VERSION.SDK_INT >= 34 && !getSystemService(NotificationManager::class.java).canUseFullScreenIntent()) {
        add(SetupIssue.FullScreenAlarms)
    }
    // USE_EXACT_ALARM (Android 13+) can't be revoked; SCHEDULE_EXACT_ALARM on Android 12 can.
    if (Build.VERSION.SDK_INT >= 31 && !getSystemService(AlarmManager::class.java).canScheduleExactAlarms()) {
        add(SetupIssue.ExactAlarms)
    }
    // Only an app that may display over other apps can bring the ringing screen back when it's left.
    if (!Settings.canDrawOverlays(this@missingSetup)) {
        add(SetupIssue.DisplayOverApps)
    }
}
