package io.github.geanyl17.openalarm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.geanyl17.openalarm.ui.resources.Res
import io.github.geanyl17.openalarm.ui.resources.alarm
import io.github.geanyl17.openalarm.ui.resources.dismiss
import io.github.geanyl17.openalarm.ui.resources.ic_alarm
import io.github.geanyl17.openalarm.ui.resources.snooze_for
import io.github.geanyl17.openalarm.ui.theme.DefaultSeedColor
import io.github.geanyl17.openalarm.ui.theme.OpenAlarmTheme
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.seconds

/** The screen shown while an alarm rings, themed with the alarm's own color. */
@Composable
fun RingingScreen(
    label: String,
    colorArgb: Int?,
    snoozeMinutes: Int,
    use24Hour: Boolean,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
) {
    OpenAlarmTheme(seedColor = colorArgb?.let { Color(it) } ?: DefaultSeedColor) {
        val now = rememberNow(tick = 1.seconds).toLocalDateTime(TimeZone.currentSystemDefault())
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primaryContainer) {
            Column(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier.padding(top = 64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_alarm),
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                    )
                    Text(
                        text = formatTime(now.hour, now.minute, use24Hour),
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 88.sp, lineHeight = 96.sp),
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = label.ifBlank { stringResource(Res.string.alarm) },
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                    )
                }
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(onClick = onSnooze, modifier = Modifier.fillMaxWidth().height(64.dp)) {
                        Text(stringResource(Res.string.snooze_for, snoozeMinutes), style = MaterialTheme.typography.titleMedium)
                    }
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(72.dp)) {
                        Text(stringResource(Res.string.dismiss), style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
    }
}
