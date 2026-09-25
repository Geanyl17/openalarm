package io.github.geanyl17.openalarm.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.geanyl17.openalarm.core.Alarm
import io.github.geanyl17.openalarm.core.Streak
import io.github.geanyl17.openalarm.core.nextUpcoming
import io.github.geanyl17.openalarm.missions.missionName
import io.github.geanyl17.openalarm.ui.resources.Res
import io.github.geanyl17.openalarm.ui.resources.add_alarm
import io.github.geanyl17.openalarm.ui.resources.alarm_at
import io.github.geanyl17.openalarm.ui.resources.app_name
import io.github.geanyl17.openalarm.ui.resources.ic_add
import io.github.geanyl17.openalarm.ui.resources.ic_palette
import io.github.geanyl17.openalarm.ui.resources.ic_alarm
import io.github.geanyl17.openalarm.ui.resources.ic_history
import io.github.geanyl17.openalarm.ui.resources.next_alarm_in
import io.github.geanyl17.openalarm.ui.resources.no_alarm_on
import io.github.geanyl17.openalarm.ui.resources.no_alarms_title
import io.github.geanyl17.openalarm.ui.resources.snoozed_until
import io.github.geanyl17.openalarm.ui.resources.streak_in_a_row
import io.github.geanyl17.openalarm.ui.resources.theme
import io.github.geanyl17.openalarm.ui.resources.wake_ups
import io.github.geanyl17.openalarm.ui.theme.isNightRed
import io.github.geanyl17.openalarm.ui.theme.nightRedFilter
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import org.jetbrains.compose.resources.pluralStringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlarmListScreen(
    alarms: List<Alarm>?,
    setupIssues: List<SetupIssue>,
    use24Hour: Boolean,
    photos: AlarmPhotos,
    snackbarHostState: SnackbarHostState,
    onFixSetupIssue: (SetupIssue) -> Unit,
    onAdd: () -> Unit,
    onEdit: (Alarm) -> Unit,
    onToggle: (Alarm, Boolean) -> Unit,
    onOpenTheme: () -> Unit,
    streak: Streak,
    onOpenWakeUps: () -> Unit,
) {
    val now = rememberNow(tick = 15.seconds)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenWakeUps) {
                        Icon(painterResource(Res.drawable.ic_history), contentDescription = stringResource(Res.string.wake_ups))
                    }
                    IconButton(onClick = onOpenTheme) {
                        Icon(painterResource(Res.drawable.ic_palette), contentDescription = stringResource(Res.string.theme))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(painterResource(Res.drawable.ic_add), contentDescription = stringResource(Res.string.add_alarm))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        // Null while the alarms file is still being read, which takes a moment on first launch.
        if (alarms == null) return@Scaffold
        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = padding.calculateStartPadding(layoutDirection) + 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                end = padding.calculateEndPadding(layoutDirection) + 16.dp,
                // Leave room so the add button never covers the last alarm.
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(setupIssues) { issue -> SetupIssueCard(issue, onFix = { onFixSetupIssue(issue) }) }
            if (alarms.isEmpty()) {
                item { EmptyState(Modifier.fillParentMaxHeight(0.7f)) }
            } else {
                item { NextAlarmSummary(alarms, now, streak, onOpenWakeUps) }
                items(alarms, key = { it.id }) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        use24Hour = use24Hour,
                        photos = photos,
                        now = now,
                        onClick = { onEdit(alarm) },
                        onToggle = { enabled -> onToggle(alarm, enabled) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NextAlarmSummary(alarms: List<Alarm>, now: Instant, streak: Streak, onOpenWakeUps: () -> Unit) {
    val next = alarms.nextUpcoming(now, TimeZone.currentSystemDefault())
    Column(Modifier.padding(horizontal = 4.dp)) {
        Text(
            text = if (next == null) {
                stringResource(Res.string.no_alarm_on)
            } else {
                stringResource(Res.string.next_alarm_in, formatDuration(next.at - now))
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (streak.current > 0) {
            Text(
                text = pluralStringResource(Res.plurals.streak_in_a_row, streak.current, streak.current),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onOpenWakeUps),
            )
        }
    }
}

@Composable
private fun AlarmCard(
    alarm: Alarm,
    use24Hour: Boolean,
    photos: AlarmPhotos,
    now: Instant,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    val time = formatTime(alarm.hour, alarm.minute, use24Hour)
    val textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = if (alarm.enabled) 1f else 0.5f)
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (alarm.photo == null) {
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(alarm.colorArgb?.takeUnless { isNightRed() }?.let { Color(it) } ?: MaterialTheme.colorScheme.primary),
                )
            } else {
                val photo = rememberPhoto(photos, alarm.photo, maxSize = 256)
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                ) {
                    photo?.let {
                        Image(
                            bitmap = it,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            alpha = if (alarm.enabled) 1f else 0.5f,
                            colorFilter = nightRedFilter(),
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(time, style = MaterialTheme.typography.displaySmall, color = textColor)
                Text(
                    text = listOfNotNull(
                        alarm.label.ifBlank { null },
                        repeatSummary(alarm.repeat),
                        alarm.missions.map { missionName(it.type) }.ifEmpty { null }?.joinToString(" + "),
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val snoozedUntil = alarm.snoozedUntil?.let { Instant.fromEpochMilliseconds(it) }
                if (alarm.enabled && snoozedUntil != null && snoozedUntil > now) {
                    val local = snoozedUntil.toLocalDateTime(TimeZone.currentSystemDefault())
                    Text(
                        text = stringResource(Res.string.snoozed_until, formatTime(local.hour, local.minute, use24Hour)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            val switchDescription = stringResource(Res.string.alarm_at, time)
            Switch(
                checked = alarm.enabled,
                onCheckedChange = onToggle,
                modifier = Modifier.semantics { contentDescription = switchDescription },
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_alarm),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(stringResource(Res.string.no_alarms_title), style = MaterialTheme.typography.titleLarge)
    }
}
