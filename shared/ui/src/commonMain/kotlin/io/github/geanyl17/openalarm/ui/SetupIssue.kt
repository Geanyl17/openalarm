package io.github.geanyl17.openalarm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.geanyl17.openalarm.ui.resources.Res
import io.github.geanyl17.openalarm.ui.resources.setup_exact_alarms_action
import io.github.geanyl17.openalarm.ui.resources.setup_exact_alarms_title
import io.github.geanyl17.openalarm.ui.resources.setup_full_screen_action
import io.github.geanyl17.openalarm.ui.resources.setup_full_screen_title
import io.github.geanyl17.openalarm.ui.resources.setup_notifications_action
import io.github.geanyl17.openalarm.ui.resources.setup_notifications_title
import io.github.geanyl17.openalarm.ui.resources.setup_overlay_action
import io.github.geanyl17.openalarm.ui.resources.setup_overlay_title
import org.jetbrains.compose.resources.stringResource

/** Something the user has to allow before alarms can ring reliably. The platform decides which apply. */
enum class SetupIssue {
    /** Without notifications, a ringing alarm can't appear at all. */
    Notifications,

    /** Without full-screen notifications, the ringing screen won't cover the lock screen. */
    FullScreenAlarms,

    /** Without exact alarms, alarms may ring minutes late. */
    ExactAlarms,

    /** Without "display over other apps", the ringing screen can be left and won't come back. */
    DisplayOverApps,
}

@Composable
internal fun SetupIssueCard(issue: SetupIssue, onFix: () -> Unit) {
    val (title, action) = when (issue) {
        SetupIssue.Notifications -> Res.string.setup_notifications_title to Res.string.setup_notifications_action
        SetupIssue.FullScreenAlarms -> Res.string.setup_full_screen_title to Res.string.setup_full_screen_action
        SetupIssue.ExactAlarms -> Res.string.setup_exact_alarms_title to Res.string.setup_exact_alarms_action
        SetupIssue.DisplayOverApps -> Res.string.setup_overlay_title to Res.string.setup_overlay_action
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = onFix,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(action))
            }
        }
    }
}
