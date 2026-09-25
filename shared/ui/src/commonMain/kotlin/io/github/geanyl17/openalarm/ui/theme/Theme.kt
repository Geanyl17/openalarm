package io.github.geanyl17.openalarm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.materialkolor.dynamicColorScheme
import io.github.geanyl17.openalarm.core.ThemeMode
import io.github.geanyl17.openalarm.core.ThemeSettings

/** OpenAlarm's out-of-the-box color, a sunrise orange. */
val DefaultSeedColor = Color(0xFFF4743B)

/** The app-wide theme: the color everything is built from, and the mode. */
@Immutable
data class AppTheme(val seedColor: Color = DefaultSeedColor, val mode: ThemeMode = ThemeMode.System)

val LocalAppTheme = staticCompositionLocalOf { AppTheme() }

/** Whether [this] mode is dark, given whether the phone is in dark mode. */
fun ThemeMode.isDark(systemDark: Boolean): Boolean = when (this) {
    ThemeMode.System -> systemDark
    ThemeMode.Light -> false
    ThemeMode.Dark, ThemeMode.Black, ThemeMode.NightRed -> true
}

/** Whether these settings make the app dark right now. */
@Composable
fun ThemeSettings.isDark(): Boolean = mode.isDark(isSystemInDarkTheme())

/**
 * Builds the full Material 3 color scheme from one [seedColor]. Every color in the app comes from
 * here, so picking a single color re-themes everything. [amoled] makes dark backgrounds pure black.
 */
fun openAlarmColorScheme(seedColor: Color, darkTheme: Boolean, amoled: Boolean = false): ColorScheme =
    dynamicColorScheme(seedColor = seedColor, isDark = darkTheme, isAmoled = amoled)

/** The color scheme for [mode], where [systemDark] says whether the phone is in dark mode. */
fun openAlarmColorScheme(seedColor: Color, mode: ThemeMode, systemDark: Boolean): ColorScheme = when (mode) {
    ThemeMode.NightRed -> nightRed(openAlarmColorScheme(seedColor, darkTheme = true, amoled = true))
    else -> openAlarmColorScheme(seedColor, mode.isDark(systemDark), amoled = mode == ThemeMode.Black)
}

/**
 * Makes [settings] the theme for [content]. [wallpaperColor] is the wallpaper's main color, or null
 * on phones that don't offer one.
 */
@Composable
fun ProvideAppTheme(settings: ThemeSettings, wallpaperColor: Color?, content: @Composable () -> Unit) {
    val seed = wallpaperColor?.takeIf { settings.wallpaperColors } ?: settings.colorArgb?.let { Color(it) } ?: DefaultSeedColor
    CompositionLocalProvider(LocalAppTheme provides AppTheme(seed, settings.mode), content = content)
}

/** Themes [content] with [seedColor], or with the app's color when that's null, in the app's mode. */
@Composable
fun OpenAlarmTheme(seedColor: Color? = null, content: @Composable () -> Unit) {
    val app = LocalAppTheme.current
    val seed = seedColor ?: app.seedColor
    val systemDark = isSystemInDarkTheme()
    val colorScheme = remember(seed, app.mode, systemDark) { openAlarmColorScheme(seed, app.mode, systemDark) }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
