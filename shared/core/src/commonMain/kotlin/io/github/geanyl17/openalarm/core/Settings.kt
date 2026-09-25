package io.github.geanyl17.openalarm.core

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** App-wide settings. Stored as JSON, so renaming or removing a property needs a migration. */
@Serializable
data class Settings(
    val theme: ThemeSettings = ThemeSettings(),
    /** The user's median reaction time when wide awake, in milliseconds, for the Alertness Gate. Null until measured. */
    val alertnessBaselineMillis: Long? = null,
)

/** How the app looks. An alarm with its own color keeps it, in the mode chosen here. */
@Serializable
data class ThemeSettings(
    /** The color the whole theme is built from (ARGB), or null for OpenAlarm's own sunrise orange. */
    val colorArgb: Int? = null,
    /** Take the color from the wallpaper instead, on phones that support it (Android 12 and later). */
    val wallpaperColors: Boolean = false,
    val mode: ThemeMode = ThemeMode.System,
)

@Serializable
enum class ThemeMode {
    /** Light or dark, following the phone. */
    @SerialName("system")
    System,

    @SerialName("light")
    Light,

    @SerialName("dark")
    Dark,

    /** Dark with pure black backgrounds, which saves power on OLED screens. */
    @SerialName("black")
    Black,

    /** Red on black, for setting alarms in bed: no blue light, and it keeps your eyes used to the dark. */
    @SerialName("night_red")
    NightRed,
}

/** Where the settings are stored. */
interface SettingsRepository {
    val settings: Flow<Settings>

    /** Applies [transform] to the settings as one atomic update. */
    suspend fun update(transform: (Settings) -> Settings)
}
