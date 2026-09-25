package io.github.geanyl17.openalarm.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import io.github.geanyl17.openalarm.core.ThemeMode

// Keeps each pixel's brightness but puts all of it in the red channel.
private val RedOnly = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            0.2126f, 0.7152f, 0.0722f, 0f, 0f,
            0f, 0f, 0f, 0f, 0f,
            0f, 0f, 0f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ),
    ),
)

@Composable
fun isNightRed(): Boolean = LocalAppTheme.current.mode == ThemeMode.NightRed

/** Turns photos red in Night red mode, so they don't give off blue light either; null otherwise. */
@Composable
fun nightRedFilter(): ColorFilter? = if (isNightRed()) RedOnly else null
