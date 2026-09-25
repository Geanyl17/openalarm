package io.github.geanyl17.openalarm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.geanyl17.openalarm.ui.resources.Res
import io.github.geanyl17.openalarm.ui.resources.app_name
import io.github.geanyl17.openalarm.ui.resources.color_blue
import io.github.geanyl17.openalarm.ui.resources.color_green
import io.github.geanyl17.openalarm.ui.resources.color_purple
import io.github.geanyl17.openalarm.ui.resources.color_red
import io.github.geanyl17.openalarm.ui.resources.color_sunrise
import io.github.geanyl17.openalarm.ui.resources.color_teal
import io.github.geanyl17.openalarm.ui.resources.early_development
import io.github.geanyl17.openalarm.ui.resources.ic_alarm
import io.github.geanyl17.openalarm.ui.resources.pick_a_color
import io.github.geanyl17.openalarm.ui.theme.DefaultSeedColor
import io.github.geanyl17.openalarm.ui.theme.OpenAlarmTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private class SeedPreset(val name: StringResource, val color: Color)

private val SeedPresets = listOf(
    SeedPreset(Res.string.color_sunrise, DefaultSeedColor),
    SeedPreset(Res.string.color_red, Color(0xFFD32F2F)),
    SeedPreset(Res.string.color_purple, Color(0xFF7B1FA2)),
    SeedPreset(Res.string.color_blue, Color(0xFF1976D2)),
    SeedPreset(Res.string.color_teal, Color(0xFF00897B)),
    SeedPreset(Res.string.color_green, Color(0xFF689F38)),
)

/** Placeholder home screen until the alarm list arrives in Phase 1. */
@Composable
fun App() {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    OpenAlarmTheme(seedColor = SeedPresets[selected].color) {
        Surface(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_alarm),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(96.dp),
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(Res.string.app_name),
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.early_development),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(32.dp))
                Text(
                    text = stringResource(Res.string.pick_a_color),
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.height(12.dp))
                SeedColorPicker(selected = selected, onSelect = { selected = it })
            }
        }
    }
}

@Composable
private fun SeedColorPicker(selected: Int, onSelect: (Int) -> Unit) {
    FlowRow(
        modifier = Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        SeedPresets.forEachIndexed { index, preset ->
            val name = stringResource(preset.name)
            val isSelected = index == selected
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(index) })
                    .semantics { contentDescription = name }
                    .then(
                        if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        else Modifier,
                    )
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(preset.color),
            )
        }
    }
}
