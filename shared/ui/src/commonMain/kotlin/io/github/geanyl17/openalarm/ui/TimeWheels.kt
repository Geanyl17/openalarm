package io.github.geanyl17.openalarm.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.geanyl17.openalarm.ui.resources.Res
import io.github.geanyl17.openalarm.ui.resources.am
import io.github.geanyl17.openalarm.ui.resources.am_pm
import io.github.geanyl17.openalarm.ui.resources.hour
import io.github.geanyl17.openalarm.ui.resources.minute
import io.github.geanyl17.openalarm.ui.resources.pm
import org.jetbrains.compose.resources.stringResource

/**
 * Scroll wheels for picking a time. Flick, drag or tap a number to move it into the highlighted band.
 * Reports every change as [hour] (0–23) and [minute].
 */
@Composable
internal fun TimeWheels(
    hour: Int,
    minute: Int,
    use24Hour: Boolean,
    onChange: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentHour by rememberUpdatedState(hour)
    val currentMinute by rememberUpdatedState(minute)
    val am = stringResource(Res.string.am)
    val pm = stringResource(Res.string.pm)
    val visibleItems = wheelVisibleItems()

    WheelRow(visibleItems, modifier) {
        if (use24Hour) {
            Wheel(
                values = (0..23).toList(),
                initial = hour,
                label = ::twoDigits,
                description = stringResource(Res.string.hour),
                onSelect = { onChange(it, currentMinute) },
                visibleItems = visibleItems,
            )
        } else {
            Wheel(
                values = (1..12).toList(),
                initial = to12Hour(hour),
                label = Int::toString,
                description = stringResource(Res.string.hour),
                onSelect = { onChange(to24Hour(it, isPm = currentHour >= 12), currentMinute) },
                visibleItems = visibleItems,
            )
        }
        Text(
            text = ":",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Wheel(
            values = (0..59).toList(),
            initial = minute,
            label = ::twoDigits,
            description = stringResource(Res.string.minute),
            onSelect = { onChange(currentHour, it) },
            visibleItems = visibleItems,
        )
        if (!use24Hour) {
            Spacer(Modifier.width(8.dp))
            Wheel(
                values = listOf(false, true),
                initial = hour >= 12,
                label = { isPm -> if (isPm) pm else am },
                description = stringResource(Res.string.am_pm),
                onSelect = { isPm -> onChange(to24Hour(to12Hour(currentHour), isPm), currentMinute) },
                visibleItems = visibleItems,
                looping = false,
                width = 72.dp,
            )
        }
    }
}

private fun twoDigits(value: Int): String = value.toString().padStart(2, '0')

private fun to12Hour(hour: Int): Int = if (hour % 12 == 0) 12 else hour % 12

private fun to24Hour(hour12: Int, isPm: Boolean): Int = hour12 % 12 + if (isPm) 12 else 0
