package io.github.geanyl17.openalarm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.geanyl17.openalarm.ui.resources.Res
import io.github.geanyl17.openalarm.ui.resources.color_blue
import io.github.geanyl17.openalarm.ui.resources.color_default
import io.github.geanyl17.openalarm.ui.resources.color_green
import io.github.geanyl17.openalarm.ui.resources.color_purple
import io.github.geanyl17.openalarm.ui.resources.color_red
import io.github.geanyl17.openalarm.ui.resources.color_sunrise
import io.github.geanyl17.openalarm.ui.resources.color_teal
import io.github.geanyl17.openalarm.ui.theme.DefaultSeedColor
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
)

@Composable
internal fun AlarmColorPicker(selected: Int?, onSelect: (Int?) -> Unit) {
    FlowRow(
        modifier = Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AlarmColors.forEach { option ->
            val name = stringResource(option.name)
            val isSelected = option.argb == selected
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(option.argb) })
                    .semantics { contentDescription = name }
                    .then(
                        if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        else Modifier,
                    )
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(option.argb?.let { Color(it) } ?: MaterialTheme.colorScheme.primary),
            )
        }
    }
}
