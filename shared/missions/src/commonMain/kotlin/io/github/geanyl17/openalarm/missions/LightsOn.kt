package io.github.geanyl17.openalarm.missions

import io.github.geanyl17.openalarm.core.Difficulty
import io.github.geanyl17.openalarm.core.Mission
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** How bright the room has to be, in lux. A bedside lamp gives about 50, ceiling lights 100 to 300. */
internal val Difficulty.lightsOnLux: Float
    get() = when (this) {
        Difficulty.Easy -> 30f
        Difficulty.Normal -> 80f
        Difficulty.Hard -> 200f
    }

/** How long the room has to stay bright: 10 seconds a round. */
internal val Mission.lightsOnDuration: Duration get() = (10 * rounds).seconds

/** How long the room has been bright without a break. Any dark moment starts it over. */
internal class BrightTimer(private val minLux: Float) {
    private var brightSince: Duration? = null

    /** Takes the latest reading at [now], a time on any steady clock, and says how long it's been bright. */
    fun update(lux: Float, now: Duration): Duration {
        if (lux < minLux) {
            brightSince = null
            return Duration.ZERO
        }
        val since = brightSince ?: now.also { brightSince = it }
        return now - since
    }
}
