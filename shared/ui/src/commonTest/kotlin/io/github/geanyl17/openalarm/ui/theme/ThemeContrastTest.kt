package io.github.geanyl17.openalarm.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import io.github.geanyl17.openalarm.core.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThemeContrastTest {

    // Includes awkward seeds on purpose: pure yellow is very light, and black, white and gray have no hue.
    private val seeds = listOf(
        DefaultSeedColor,
        Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta,
        Color.Black, Color.White, Color.Gray, Color(0xFF8B4513),
    ) + wheelColors()

    /** A grid over the custom color wheel, since an alarm can have any color on it. */
    private fun wheelColors() = buildList {
        for (hue in 0 until 360 step 15) {
            for (saturation in listOf(0.3f, 0.65f, 1f)) {
                for (value in listOf(0.15f, 0.55f, 1f)) add(Color.hsv(hue.toFloat(), saturation, value))
            }
        }
    }

    @Test
    fun contrastRatioMatchesWcagReferenceValues() {
        assertEquals(21f, contrastRatio(Color.Black, Color.White), absoluteTolerance = 0.01f)
        assertEquals(1f, contrastRatio(Color.Red, Color.Red), absoluteTolerance = 0.001f)
        assertEquals(4.54f, contrastRatio(Color(0xFF767676), Color.White), absoluteTolerance = 0.01f)
    }

    @Test
    fun everySeedColorGivesReadableText() {
        val failures = buildList {
            for (seed in seeds) {
                for (mode in ThemeMode.entries) {
                    for (systemDark in listOf(false, true)) {
                        val scheme = openAlarmColorScheme(seed, mode, systemDark)
                        for ((pair, colors) in textOnBackgroundPairs(scheme)) {
                            val ratio = contrastRatio(colors.first, colors.second)
                            if (ratio < MIN_TEXT_CONTRAST) {
                                val hex = seed.toArgb().toUInt().toString(16)
                                add("seed #$hex $mode (phone dark=$systemDark): $pair is only $ratio:1")
                            }
                        }
                    }
                }
            }
        }
        assertTrue(failures.isEmpty(), "Text below $MIN_TEXT_CONTRAST:1 contrast:\n" + failures.joinToString("\n"))
    }

    private fun textOnBackgroundPairs(s: ColorScheme): Map<String, Pair<Color, Color>> = mapOf(
        "onSurface on surface" to (s.onSurface to s.surface),
        "onSurfaceVariant on surface" to (s.onSurfaceVariant to s.surface),
        "onPrimary on primary" to (s.onPrimary to s.primary),
        "onPrimaryContainer on primaryContainer" to (s.onPrimaryContainer to s.primaryContainer),
        "onSecondaryContainer on secondaryContainer" to (s.onSecondaryContainer to s.secondaryContainer),
        "onError on error" to (s.onError to s.error),
    )
}
