package io.github.geanyl17.openalarm.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.geanyl17.openalarm.core.Mission
import io.github.geanyl17.openalarm.missions.MissionScreen
import io.github.geanyl17.openalarm.ui.resources.Res
import io.github.geanyl17.openalarm.ui.resources.alarm
import io.github.geanyl17.openalarm.ui.resources.dismiss
import io.github.geanyl17.openalarm.ui.resources.emergency_call
import io.github.geanyl17.openalarm.ui.resources.ic_alarm
import io.github.geanyl17.openalarm.ui.resources.snooze_for
import io.github.geanyl17.openalarm.ui.resources.turn_off
import io.github.geanyl17.openalarm.ui.theme.DefaultSeedColor
import io.github.geanyl17.openalarm.ui.theme.OpenAlarmTheme
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.seconds

/**
 * The screen shown while an alarm rings, themed with the alarm's own color and showing its cover
 * [photo], if it has one. With [missions], the alarm only turns off once they're done;
 * [onMissionInteraction] is called on every tap in them. [startWithMission] opens the missions
 * straight away, for example from the notification. [onEmergencyCall] opens the emergency dialer:
 * a ringing alarm keeps its screen in front, but it must never stand in the way of an emergency call.
 */
@Composable
fun RingingScreen(
    label: String,
    colorArgb: Int?,
    photo: ImageBitmap?,
    snoozeMinutes: Int,
    missions: List<Mission>,
    use24Hour: Boolean,
    startWithMission: Boolean,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
    onMissionInteraction: () -> Unit,
    onEmergencyCall: () -> Unit,
) {
    OpenAlarmTheme(seedColor = colorArgb?.let { Color(it) } ?: DefaultSeedColor) {
        val now = rememberNow(tick = 1.seconds).toLocalDateTime(TimeZone.currentSystemDefault())
        var solving by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(startWithMission) {
            if (startWithMission && missions.isNotEmpty()) solving = true
        }

        if (solving) {
            Surface(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formatTime(now.hour, now.minute, use24Hour),
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onSnooze) {
                            Text(stringResource(Res.string.snooze_for, snoozeMinutes))
                        }
                    }
                    MissionScreen(
                        missions = missions,
                        onInteraction = onMissionInteraction,
                        onComplete = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    EmergencyCallButton(onEmergencyCall, Modifier.align(Alignment.CenterHorizontally))
                }
            }
        } else {
            Ringing(
                now = now,
                label = label,
                photo = photo,
                snoozeMinutes = snoozeMinutes,
                use24Hour = use24Hour,
                needsMission = missions.isNotEmpty(),
                onSnooze = onSnooze,
                onEmergencyCall = onEmergencyCall,
                onTurnOff = {
                    if (missions.isEmpty()) {
                        onDismiss()
                    } else {
                        onMissionInteraction()
                        solving = true
                    }
                },
            )
        }
    }
}

@Composable
private fun Ringing(
    now: LocalDateTime,
    label: String,
    photo: ImageBitmap?,
    snoozeMinutes: Int,
    use24Hour: Boolean,
    needsMission: Boolean,
    onSnooze: () -> Unit,
    onEmergencyCall: () -> Unit,
    onTurnOff: () -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primaryContainer) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EmergencyCallButton(onEmergencyCall)
                Spacer(Modifier.height(8.dp))
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
            photo?.let {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .clip(RoundedCornerShape(28.dp)),
                )
            }
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = onSnooze, modifier = Modifier.fillMaxWidth().height(64.dp)) {
                    Text(stringResource(Res.string.snooze_for, snoozeMinutes), style = MaterialTheme.typography.titleMedium)
                }
                Button(onClick = onTurnOff, modifier = Modifier.fillMaxWidth().height(72.dp)) {
                    Text(
                        text = stringResource(if (needsMission) Res.string.turn_off else Res.string.dismiss),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmergencyCallButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(contentColor = LocalContentColor.current),
        modifier = modifier,
    ) {
        Text(stringResource(Res.string.emergency_call))
    }
}
