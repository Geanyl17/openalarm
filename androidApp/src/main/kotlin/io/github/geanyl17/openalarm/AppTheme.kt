package io.github.geanyl17.openalarm

import android.content.Context
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import io.github.geanyl17.openalarm.core.ThemeSettings
import io.github.geanyl17.openalarm.ui.theme.isDark
import android.graphics.Color as AndroidColor

// The same scrims enableEdgeToEdge() uses behind three-button navigation.
private val LightScrim = AndroidColor.argb(0xE6, 0xFF, 0xFF, 0xFF)
private val DarkScrim = AndroidColor.argb(0x80, 0x1B, 0x1B, 0x1B)

/**
 * The main color of the wallpaper, or of the color the user picked in the phone's wallpaper settings,
 * as Android 12 and later use it for their own theme. Null on older versions.
 */
fun wallpaperColor(context: Context): Color? =
    if (Build.VERSION.SDK_INT >= 31) Color(context.getColor(android.R.color.system_accent1_500)) else null

/**
 * Shows [content] once the settings are loaded, which only takes a moment, with the status and
 * navigation bar icons matching the app's light or dark mode rather than the phone's.
 */
@Composable
fun ComponentActivity.WithThemeSettings(content: @Composable (ThemeSettings) -> Unit) {
    val theme by appGraph.theme.collectAsState()
    theme?.let { current ->
        val dark = current.isDark()
        LaunchedEffect(dark) {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark },
                navigationBarStyle = SystemBarStyle.auto(LightScrim, DarkScrim) { dark },
            )
        }
        content(current)
    }
}
