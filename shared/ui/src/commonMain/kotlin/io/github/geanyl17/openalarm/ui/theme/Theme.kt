package io.github.geanyl17.openalarm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.materialkolor.dynamicColorScheme

/** OpenAlarm's out-of-the-box color, a sunrise orange. */
val DefaultSeedColor = Color(0xFFF4743B)

/**
 * Builds the full Material 3 color scheme from one [seedColor]. Every color in the app comes from
 * here, so picking a single color re-themes everything. [amoled] makes dark backgrounds pure black.
 */
fun openAlarmColorScheme(seedColor: Color, darkTheme: Boolean, amoled: Boolean = false): ColorScheme =
    dynamicColorScheme(seedColor = seedColor, isDark = darkTheme, isAmoled = amoled)

@Composable
fun OpenAlarmTheme(
    seedColor: Color = DefaultSeedColor,
    darkTheme: Boolean = isSystemInDarkTheme(),
    amoled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = remember(seedColor, darkTheme, amoled) {
        openAlarmColorScheme(seedColor, darkTheme, amoled)
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
