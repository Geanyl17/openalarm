package io.github.geanyl17.openalarm.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A challenge that must be completed to turn an alarm off.
 *
 * Stored as JSON inside [Alarm]. The serial names are part of the file format; don't rename them.
 * Unknown types from a newer app version are read as the defaults, so an old version can still
 * load the file.
 */
@Serializable
data class Mission(
    val type: MissionType = MissionType.Math,
    val difficulty: Difficulty = Difficulty.Normal,
    /** How many problems, patterns, taps or words in a row, or for Lights On, how many 10-second stretches. */
    val rounds: Int = 3,
) {
    init {
        require(rounds in 1..MAX_ROUNDS) { "Rounds out of range: $rounds" }
    }

    /** The same mission, one difficulty level up and with one more round. */
    fun harder(): Mission = copy(
        difficulty = Difficulty.entries[(difficulty.ordinal + 1).coerceAtMost(Difficulty.entries.lastIndex)],
        rounds = (rounds + 1).coerceAtMost(MAX_ROUNDS),
    )

    companion object {
        const val MAX_ROUNDS = 10
    }
}

@Serializable
enum class MissionType {
    /** Solve arithmetic problems. */
    @SerialName("math")
    Math,

    /** Repeat a pattern of tiles that light up. */
    @SerialName("memory")
    Memory,

    /** Tap as soon as the screen says so, fast enough to show you're awake. */
    @SerialName("reaction")
    Reaction,

    /** Name the ink color of a color word, not the word itself. */
    @SerialName("stroop")
    Stroop,

    /** Keep the room bright for a while. */
    @SerialName("lights_on")
    LightsOn,
}

@Serializable
enum class Difficulty {
    @SerialName("easy")
    Easy,

    @SerialName("normal")
    Normal,

    @SerialName("hard")
    Hard,
}
