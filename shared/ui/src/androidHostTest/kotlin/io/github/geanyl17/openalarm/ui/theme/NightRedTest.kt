package io.github.geanyl17.openalarm.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import io.github.geanyl17.openalarm.core.ThemeMode
import kotlin.test.Test
import kotlin.test.assertTrue

class NightRedTest {

    /**
     * Every color in the scheme, found by reflection, so a color that a newer Material 3 version adds
     * can't slip through without being turned red.
     */
    private fun ColorScheme.allColors(): Map<String, Color> =
        ColorScheme::class.java.methods
            .filter { it.name.startsWith("get") && it.parameterCount == 0 && it.returnType == Long::class.javaPrimitiveType }
            .associate { it.name.substringBefore('-') to Color((it.invoke(this) as Long).toULong()) }

    @Test
    fun nightRedHasNoGreenOrBlueAnywhere() {
        for (seed in listOf(DefaultSeedColor, Color.Blue, Color.Green, Color.White, Color.Gray)) {
            val colors = openAlarmColorScheme(seed, ThemeMode.NightRed, systemDark = false).allColors()
            assertTrue(colors.size >= 40, "Only found ${colors.size} colors")
            val notRed = colors.filterValues { it.green != 0f || it.blue != 0f }
            assertTrue(notRed.isEmpty(), "Not pure red: ${notRed.keys}")
        }
    }
}
