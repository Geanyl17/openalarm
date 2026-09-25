package io.github.geanyl17.openalarm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.geanyl17.openalarm.ui.resources.Res
import io.github.geanyl17.openalarm.ui.resources.color_blue
import io.github.geanyl17.openalarm.ui.resources.color_custom
import io.github.geanyl17.openalarm.ui.resources.color_default
import io.github.geanyl17.openalarm.ui.resources.color_green
import io.github.geanyl17.openalarm.ui.resources.color_purple
import io.github.geanyl17.openalarm.ui.resources.color_red
import io.github.geanyl17.openalarm.ui.resources.color_sunrise
import io.github.geanyl17.openalarm.ui.resources.color_teal
import io.github.geanyl17.openalarm.ui.resources.color_yellow
import io.github.geanyl17.openalarm.ui.theme.DefaultSeedColor
import io.github.geanyl17.openalarm.ui.theme.openAlarmColorScheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** A color the user can give an alarm. A null [argb] means "use the app's theme color". */
internal class AlarmColor(val name: StringResource, val argb: Int?)

internal val AlarmColors = listOf(
    AlarmColor(Res.string.color_default, null),
    AlarmColor(Res.string.color_sunrise, DefaultSeedColor.toArgb()),
    AlarmColor(Res.string.color_red, 0xFFD32F2F.toInt()),
    AlarmColor(Res.string.color_purple, 0xFF7B1FA2.toInt()),
    AlarmColor(Res.string.color_blue, 0xFF1976D2.toInt()),
    AlarmColor(Res.string.color_teal, 0xFF00897B.toInt()),
    AlarmColor(Res.string.color_green, 0xFF689F38.toInt()),
    AlarmColor(Res.string.color_yellow, 0xFFFDD835.toInt()),
)

private val Rainbow = Brush.sweepGradient(
    listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red),
)

/** The preset colors, plus a custom one picked on a color wheel. */
@Composable
internal fun AlarmColorPicker(selected: Int?, onSelect: (Int?) -> Unit) {
    var pickingCustom by rememberSaveable { mutableStateOf(false) }
    val custom = selected?.takeIf { argb -> AlarmColors.none { it.argb == argb } }
    // The editor is themed with the alarm's own color, so "Theme color" has to show the app theme explicitly.
    val darkTheme = isSystemInDarkTheme()
    val appThemeColor = remember(darkTheme) { openAlarmColorScheme(DefaultSeedColor, darkTheme).primary }

    FlowRow(
        modifier = Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AlarmColors.forEach { option ->
            Swatch(
                name = stringResource(option.name),
                fill = SolidColor(option.argb?.let { Color(it) } ?: appThemeColor),
                selected = option.argb == selected,
                onClick = { onSelect(option.argb) },
            )
        }
        // Shows the custom color once there is one; until then, a rainbow says "any color".
        Swatch(
            name = stringResource(Res.string.color_custom),
            fill = custom?.let { SolidColor(Color(it)) } ?: Rainbow,
            selected = custom != null,
            onClick = { pickingCustom = true },
        )
    }

    if (pickingCustom) {
        ColorWheelDialog(
            initial = selected?.let { Color(it) } ?: DefaultSeedColor,
            onDismiss = { pickingCustom = false },
            onSelect = { color ->
                onSelect(color.toArgb())
                pickingCustom = false
            },
        )
    }
}

@Composable
private fun Swatch(name: String, fill: Brush, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = name }
            .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
            .padding(6.dp)
            .clip(CircleShape)
            .background(fill),
    )
}
