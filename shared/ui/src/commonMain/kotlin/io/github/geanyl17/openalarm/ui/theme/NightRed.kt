package io.github.geanyl17.openalarm.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.sqrt

/** Colors lighter than this are text, icons and accents; darker ones are backgrounds. */
private const val FOREGROUND_LUMINANCE = 0.18f

// Pure red can't get very bright, so text and backgrounds are kept far apart to stay readable:
// text and icons are at least this red, backgrounds at most this red.
private const val MIN_FOREGROUND_RED = 0xF5
private const val MAX_BACKGROUND_RED = 0x1E

/**
 * Turns a dark [scheme] into red on black, with no green or blue at all. Light colors (text, icons,
 * accents) become bright red and dark ones (backgrounds, containers) nearly black, keeping their order,
 * so every text color stays readable on every background.
 */
internal fun nightRed(scheme: ColorScheme): ColorScheme {
    fun red(color: Color): Color {
        val luminance = color.luminance()
        val level = if (luminance >= FOREGROUND_LUMINANCE) {
            MIN_FOREGROUND_RED + (0xFF - MIN_FOREGROUND_RED) * (luminance - FOREGROUND_LUMINANCE) / (1 - FOREGROUND_LUMINANCE)
        } else {
            MAX_BACKGROUND_RED * sqrt(luminance / FOREGROUND_LUMINANCE)
        }
        return Color(red = level / 255f, green = 0f, blue = 0f, alpha = color.alpha)
    }
    return with(scheme) {
        copy(
            primary = red(primary),
            onPrimary = red(onPrimary),
            primaryContainer = red(primaryContainer),
            onPrimaryContainer = red(onPrimaryContainer),
            inversePrimary = red(inversePrimary),
            secondary = red(secondary),
            onSecondary = red(onSecondary),
            secondaryContainer = red(secondaryContainer),
            onSecondaryContainer = red(onSecondaryContainer),
            tertiary = red(tertiary),
            onTertiary = red(onTertiary),
            tertiaryContainer = red(tertiaryContainer),
            onTertiaryContainer = red(onTertiaryContainer),
            background = red(background),
            onBackground = red(onBackground),
            surface = red(surface),
            onSurface = red(onSurface),
            surfaceVariant = red(surfaceVariant),
            onSurfaceVariant = red(onSurfaceVariant),
            surfaceTint = red(surfaceTint),
            inverseSurface = red(inverseSurface),
            inverseOnSurface = red(inverseOnSurface),
            error = red(error),
            onError = red(onError),
            errorContainer = red(errorContainer),
            onErrorContainer = red(onErrorContainer),
            outline = red(outline),
            outlineVariant = red(outlineVariant),
            scrim = red(scrim),
            surfaceBright = red(surfaceBright),
            surfaceDim = red(surfaceDim),
            surfaceContainer = red(surfaceContainer),
            surfaceContainerHigh = red(surfaceContainerHigh),
            surfaceContainerHighest = red(surfaceContainerHighest),
            surfaceContainerLow = red(surfaceContainerLow),
            surfaceContainerLowest = red(surfaceContainerLowest),
            primaryFixed = red(primaryFixed),
            primaryFixedDim = red(primaryFixedDim),
            onPrimaryFixed = red(onPrimaryFixed),
            onPrimaryFixedVariant = red(onPrimaryFixedVariant),
            secondaryFixed = red(secondaryFixed),
            secondaryFixedDim = red(secondaryFixedDim),
            onSecondaryFixed = red(onSecondaryFixed),
            onSecondaryFixedVariant = red(onSecondaryFixedVariant),
            tertiaryFixed = red(tertiaryFixed),
            tertiaryFixedDim = red(tertiaryFixedDim),
            onTertiaryFixed = red(onTertiaryFixed),
            onTertiaryFixedVariant = red(onTertiaryFixedVariant),
        )
    }
}
