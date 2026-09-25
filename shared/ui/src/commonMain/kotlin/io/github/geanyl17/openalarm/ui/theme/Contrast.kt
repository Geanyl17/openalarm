package io.github.geanyl17.openalarm.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

/** WCAG AA minimum contrast ratio for normal-size text. */
const val MIN_TEXT_CONTRAST = 4.5f

/** WCAG 2 contrast ratio between two colors: 1 for identical colors, up to 21 for black on white. */
fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    return (max(la, lb) + 0.05f) / (min(la, lb) + 0.05f)
}
