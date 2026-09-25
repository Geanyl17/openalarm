package io.github.geanyl17.openalarm.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import io.github.geanyl17.openalarm.ui.theme.DefaultSeedColor
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ColorWheelTest {

    @Test
    fun colorsSurviveTheTripThroughHsv() {
        val colors = listOf(Color.Red, Color.Green, Color.Blue, Color(0xFFFDD835), DefaultSeedColor, Color(0xFF1976D2), Color.Gray, Color.White)
        for (color in colors) {
            val back = Hsv.from(color).toColor()
            assertTrue(
                abs(back.red - color.red) < 0.005f && abs(back.green - color.green) < 0.005f && abs(back.blue - color.blue) < 0.005f,
                "${color.hex()} came back as ${back.hex()}",
            )
        }
    }

    @Test
    fun knownColorsHaveTheExpectedHue() {
        assertEquals(0f, Hsv.from(Color.Red).hue, 0.5f)
        assertEquals(120f, Hsv.from(Color.Green).hue, 0.5f)
        assertEquals(240f, Hsv.from(Color.Blue).hue, 0.5f)
        assertEquals(0f, Hsv.from(Color.Gray).saturation, 0.001f)
    }

    @Test
    fun everyPointOnTheWheelMapsBackToItsColor() {
        val radius = 300f
        for (hue in 0 until 360 step 15) {
            for (saturation in listOf(0.1f, 0.5f, 1f)) {
                val hsv = Hsv(hue.toFloat(), saturation, 0.8f)
                val back = wheelHsv(wheelOffset(hsv, radius), radius, value = 0.8f)
                assertEquals(hsv.hue, back.hue, 0.5f, "hue at $hsv")
                assertEquals(hsv.saturation, back.saturation, 0.001f, "saturation at $hsv")
            }
        }
    }

    @Test
    fun touchesOutsideTheWheelCountAsItsEdge() {
        val hsv = wheelHsv(Offset(0f, 500f), radius = 100f, value = 1f)
        assertEquals(1f, hsv.saturation)
        // Straight down from the center is 90°, because the sweep runs clockwise from 3 o'clock.
        assertEquals(90f, hsv.hue, 0.001f)
    }

    @Test
    fun hexCodesAreUppercaseWithoutAlpha() {
        assertEquals("#FDD835", Color(0xFFFDD835).hex())
        assertEquals("#000000", Color.Black.hex())
    }
}
