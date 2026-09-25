package io.github.geanyl17.openalarm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.geanyl17.openalarm.core.Escape
import io.github.geanyl17.openalarm.core.Streak
import io.github.geanyl17.openalarm.core.WakeUp
import io.github.geanyl17.openalarm.ui.resources.Res
import io.github.geanyl17.openalarm.ui.resources.back
import io.github.geanyl17.openalarm.ui.resources.check_ins_count
import io.github.geanyl17.openalarm.ui.resources.escape_app_stopped
import io.github.geanyl17.openalarm.ui.resources.escape_force_stop
import io.github.geanyl17.openalarm.ui.resources.escape_missed_check_in
import io.github.geanyl17.openalarm.ui.resources.escape_phone_off
import io.github.geanyl17.openalarm.ui.resources.escape_turned_off_in_app
import io.github.geanyl17.openalarm.ui.resources.ic_arrow_back
import io.github.geanyl17.openalarm.ui.resources.no_wake_ups
import io.github.geanyl17.openalarm.ui.resources.snoozes_count
import io.github.geanyl17.openalarm.ui.resources.still_going
import io.github.geanyl17.openalarm.ui.resources.streak_best
import io.github.geanyl17.openalarm.ui.resources.streak_current
import io.github.geanyl17.openalarm.ui.resources.streak_days
import io.github.geanyl17.openalarm.ui.resources.up_after
import io.github.geanyl17.openalarm.ui.resources.wake_ups
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

/** The wake-up log: the streak, then every wake-up, newest first, with the Honesty Log's entries. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WakeUpsScreen(wakeUps: List<WakeUp>, streak: Streak, use24Hour: Boolean, onClose: () -> Unit) {
    val timeZone = TimeZone.currentSystemDefault()
    val days: List<Pair<LocalDate, List<WakeUp>>> = remember(wakeUps) {
        wakeUps.asReversed()
            .groupBy { Instant.fromEpochMilliseconds(it.rangAt).toLocalDateTime(timeZone).date }
            .toList()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.wake_ups)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(painterResource(Res.drawable.ic_arrow_back), contentDescription = stringResource(Res.string.back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = padding.calculateTopPadding() + 8.dp, end = 16.dp, bottom = padding.calculateBottomPadding() + 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    StreakCard(stringResource(Res.string.streak_current), streak.current, Modifier.weight(1f))
                    StreakCard(stringResource(Res.string.streak_best), streak.best, Modifier.weight(1f))
                }
            }
            if (days.isEmpty()) {
                item {
                    Text(
                        stringResource(Res.string.no_wake_ups),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            days.forEach { (day, dayWakeUps) ->
                item(key = day.toString()) {
                    Text(
                        text = formatDate(day),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp, start = 4.dp),
                    )
                }
                items(dayWakeUps, key = { "${it.alarmId}-${it.rangAt}" }) { wakeUp -> WakeUpCard(wakeUp, use24Hour, timeZone) }
            }
        }
    }
}

@Composable
private fun StreakCard(title: String, days: Int, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(pluralStringResource(Res.plurals.streak_days, days, days), style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun WakeUpCard(wakeUp: WakeUp, use24Hour: Boolean, timeZone: TimeZone) {
    val rang = Instant.fromEpochMilliseconds(wakeUp.rangAt)
    val time = rang.toLocalDateTime(timeZone)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = listOfNotNull(formatTime(time.hour, time.minute, use24Hour), wakeUp.label.ifBlank { null }).joinToString(" · "),
                style = MaterialTheme.typography.titleMedium,
            )
            val summary = listOfNotNull(
                wakeUp.offAt?.let { stringResource(Res.string.up_after, formatDuration(Instant.fromEpochMilliseconds(it) - rang)) }
                    ?: stringResource(Res.string.still_going),
                wakeUp.snoozes.takeIf { it > 0 }?.let { pluralStringResource(Res.plurals.snoozes_count, it, it) },
                wakeUp.checkIns.takeIf { it > 0 }?.let { pluralStringResource(Res.plurals.check_ins_count, it, it) },
            )
            Text(summary.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            wakeUp.escapes.forEach { escape ->
                Text(
                    text = stringResource(escape.description),
                    style = MaterialTheme.typography.bodyMedium,
                    // Only what breaks the streak is shown as a problem.
                    color = if (escape.breaksStreak) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val Escape.description
    get() = when (this) {
        Escape.ForceStop -> Res.string.escape_force_stop
        Escape.TurnedOffInApp -> Res.string.escape_turned_off_in_app
        Escape.MissedCheckIn -> Res.string.escape_missed_check_in
        Escape.PhoneOff -> Res.string.escape_phone_off
        Escape.AppStopped -> Res.string.escape_app_stopped
    }
