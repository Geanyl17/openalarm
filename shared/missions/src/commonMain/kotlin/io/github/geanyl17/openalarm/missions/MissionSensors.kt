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
}

/** The sensors for missions on this screen. Without a platform to provide them, there are none. */
val LocalMissionSensors = staticCompositionLocalOf<MissionSensors> { object : MissionSensors {} }

/** Whether this phone has what [type] needs. */
fun MissionSensors.canRun(type: MissionType): Boolean = when (type) {
    MissionType.LightsOn -> light != null
    else -> true
}

/** Missions that need something done away from the screen, which a player might not manage. */
internal val MissionType.usesHardware: Boolean get() = this == MissionType.LightsOn
