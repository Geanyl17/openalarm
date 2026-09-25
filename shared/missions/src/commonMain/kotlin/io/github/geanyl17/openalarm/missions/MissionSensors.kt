package io.github.geanyl17.openalarm.missions

import androidx.compose.runtime.staticCompositionLocalOf
import io.github.geanyl17.openalarm.core.MissionType
import kotlinx.coroutines.flow.Flow

/**
 * The phone hardware that movement missions use, provided by the platform. Each one is null when the
 * phone doesn't have it, and missions that need it are swapped for math.
 */
interface MissionSensors {
    /** Ambient light in lux, for as long as it's collected. */
    val light: Flow<Float>? get() = null

    /** A value for every step taken, for as long as it's collected. */
    val steps: Flow<Unit>? get() = null

    /** Asks for permission to count steps, where the platform needs one. Steps are counted less well without it. */
    fun requestSteps() = Unit

    /** The ID of every NFC tag held to the phone, in hex, for as long as it's collected. Null without NFC. */
    val nfcTags: Flow<String>? get() = null

    /** Whether NFC is switched on. */
    fun isNfcOn(): Boolean = false

    /** Opens the phone's settings to switch NFC on. */
    fun openNfcSettings() = Unit
}

/** The sensors for missions on this screen. Without a platform to provide them, there are none. */
val LocalMissionSensors = staticCompositionLocalOf<MissionSensors> { object : MissionSensors {} }

/** Whether this phone has what [type] needs. */
fun MissionSensors.canRun(type: MissionType): Boolean = when (type) {
    MissionType.LightsOn -> light != null
    MissionType.Steps -> steps != null
    MissionType.NfcTag -> nfcTags != null
    else -> true
}

/**
 * Whether the alarm editor offers [this] mission. The NFC tag and Alertness missions are held back for now,
 * as harder to set up and use than the rest; alarms that already have one still run it.
 */
val MissionType.offered: Boolean get() = this != MissionType.NfcTag && this != MissionType.Alertness

/** Missions that need something done away from the screen, which a player might not manage. */
internal val MissionType.usesHardware: Boolean
    get() = this == MissionType.LightsOn || this == MissionType.Steps || this == MissionType.NfcTag
