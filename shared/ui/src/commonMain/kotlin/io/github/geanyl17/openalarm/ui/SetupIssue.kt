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
import io.github.geanyl17.openalarm.ui.resources.open_settings
import io.github.geanyl17.openalarm.ui.resources.setup_exact_alarms_body
import io.github.geanyl17.openalarm.ui.resources.setup_exact_alarms_title
import io.github.geanyl17.openalarm.ui.resources.setup_full_screen_body
import io.github.geanyl17.openalarm.ui.resources.setup_full_screen_title
import io.github.geanyl17.openalarm.ui.resources.setup_notifications_action
import io.github.geanyl17.openalarm.ui.resources.setup_notifications_body
import io.github.geanyl17.openalarm.ui.resources.setup_notifications_title
import org.jetbrains.compose.resources.stringResource

/** Something the user has to allow before alarms can ring reliably. The platform decides which apply. */
enum class SetupIssue {
    /** Without notifications, a ringing alarm can't appear at all. */
    Notifications,

    /** Without full-screen notifications, the ringing screen won't cover the lock screen. */
    FullScreenAlarms,

    /** Without exact alarms, alarms may ring minutes late. */
    ExactAlarms,
}

@Composable
internal fun SetupIssueCard(issue: SetupIssue, onFix: () -> Unit) {
    val (title, body, action) = when (issue) {
        SetupIssue.Notifications -> Triple(
            Res.string.setup_notifications_title,
            Res.string.setup_notifications_body,
            Res.string.setup_notifications_action,
        )
        SetupIssue.FullScreenAlarms -> Triple(
            Res.string.setup_full_screen_title,
            Res.string.setup_full_screen_body,
            Res.string.open_settings,
        )
        SetupIssue.ExactAlarms -> Triple(
            Res.string.setup_exact_alarms_title,
            Res.string.setup_exact_alarms_body,
            Res.string.open_settings,
        )
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(body), style = MaterialTheme.typography.bodyMedium)
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
