package io.github.geanyl17.openalarm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import io.github.geanyl17.openalarm.core.Alarm
import io.github.geanyl17.openalarm.ui.resources.Res
import io.github.geanyl17.openalarm.ui.resources.cancel
import io.github.geanyl17.openalarm.ui.resources.ic_edit
import io.github.geanyl17.openalarm.ui.resources.minutes_short
import io.github.geanyl17.openalarm.ui.resources.minutes_unit
import io.github.geanyl17.openalarm.ui.resources.select
import io.github.geanyl17.openalarm.ui.resources.snooze_custom
import io.github.geanyl17.openalarm.ui.resources.snooze_length
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

internal val SnoozePresets = listOf(5, 10, 15, 20)

/** The preset snooze lengths, plus a custom one picked on a wheel. */
@Composable
internal fun SnoozeLengthPicker(minutes: Int, onSelect: (Int) -> Unit) {
    var pickingCustom by rememberSaveable { mutableStateOf(false) }
    val custom = minutes.takeIf { it !in SnoozePresets }

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SnoozePresets.forEach { preset ->
            FilterChip(
                selected = preset == minutes,
                onClick = { onSelect(preset) },
                label = { Text(stringResource(Res.string.minutes_short, preset)) },
            )
        }
        // Shows the custom length once there is one. The pencil marks the chip that opens the picker.
        FilterChip(
            selected = custom != null,
            onClick = { pickingCustom = true },
            label = {
                Text(if (custom == null) stringResource(Res.string.snooze_custom) else stringResource(Res.string.minutes_short, custom))
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(Res.drawable.ic_edit),
                    // So screen readers say "Custom, 12 min" once a custom length is set.
                    contentDescription = if (custom == null) null else stringResource(Res.string.snooze_custom),
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            },
        )
    }

    if (pickingCustom) {
        SnoozeLengthDialog(
            initial = minutes,
            onDismiss = { pickingCustom = false },
            onSelect = {
                onSelect(it)
                pickingCustom = false
            },
        )
    }
}

@Composable
private fun SnoozeLengthDialog(initial: Int, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    var minutes by remember { mutableIntStateOf(initial) }
    val visibleItems = wheelVisibleItems()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.snooze_length)) },
        text = {
            WheelRow(visibleItems) {
                Wheel(
                    values = (1..Alarm.MAX_SNOOZE_MINUTES).toList(),
                    initial = initial,
                    label = Int::toString,
                    description = stringResource(Res.string.snooze_length),
                    onSelect = { minutes = it },
                    visibleItems = visibleItems,
                    looping = false,
                )
                Text(
                    text = stringResource(Res.string.minutes_unit),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    // The wheel already tells screen readers what it's for.
                    modifier = Modifier.padding(start = 4.dp).clearAndSetSemantics {},
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(minutes) }) { Text(stringResource(Res.string.select)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        },
    )
}
