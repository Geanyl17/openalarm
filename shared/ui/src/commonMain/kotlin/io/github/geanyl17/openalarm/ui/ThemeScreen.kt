package io.github.geanyl17.openalarm.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import io.github.geanyl17.openalarm.core.ThemeMode
import io.github.geanyl17.openalarm.core.ThemeSettings
import io.github.geanyl17.openalarm.ui.resources.Res
import io.github.geanyl17.openalarm.ui.resources.back
import io.github.geanyl17.openalarm.ui.resources.color
import io.github.geanyl17.openalarm.ui.resources.ic_arrow_back
import io.github.geanyl17.openalarm.ui.resources.mode_black
import io.github.geanyl17.openalarm.ui.resources.mode_dark
import io.github.geanyl17.openalarm.ui.resources.mode_light
import io.github.geanyl17.openalarm.ui.resources.mode_night_red
import io.github.geanyl17.openalarm.ui.resources.mode_system
import io.github.geanyl17.openalarm.ui.resources.theme
import io.github.geanyl17.openalarm.ui.resources.theme_mode
import io.github.geanyl17.openalarm.ui.resources.wallpaper_colors
import io.github.geanyl17.openalarm.ui.theme.DefaultSeedColor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Picks the app-wide theme. Changes apply right away, so the screen itself is the preview. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThemeScreen(
    theme: ThemeSettings,
    wallpaperColorsAvailable: Boolean,
    onChange: (ThemeSettings) -> Unit,
    onClose: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.theme)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(painterResource(Res.drawable.ic_arrow_back), contentDescription = stringResource(Res.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            SectionTitle(stringResource(Res.string.theme_mode))
            ChoiceChips(ThemeMode.entries, theme.mode, label = { modeName(it) }, onSelect = { onChange(theme.copy(mode = it)) })

            SectionTitle(stringResource(Res.string.color))
            if (wallpaperColorsAvailable) {
                SwitchRow(stringResource(Res.string.wallpaper_colors), theme.wallpaperColors) { onChange(theme.copy(wallpaperColors = it)) }
            }
            if (!wallpaperColorsAvailable || !theme.wallpaperColors) {
                AlarmColorPicker(
                    selected = theme.colorArgb ?: DefaultSeedColor.toArgb(),
                    onSelect = { onChange(theme.copy(colorArgb = it)) },
                    withThemeColor = false,
                )
            }
        }
    }
}

@Composable
private fun modeName(mode: ThemeMode): String = stringResource(
    when (mode) {
        ThemeMode.System -> Res.string.mode_system
        ThemeMode.Light -> Res.string.mode_light
        ThemeMode.Dark -> Res.string.mode_dark
        ThemeMode.Black -> Res.string.mode_black
        ThemeMode.NightRed -> Res.string.mode_night_red
    },
)
