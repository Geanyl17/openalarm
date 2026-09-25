package io.github.geanyl17.openalarm.missions

import androidx.compose.runtime.staticCompositionLocalOf
import io.github.geanyl17.openalarm.core.Difficulty
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * The Alertness Gate is a short version of the Psychomotor Vigilance Task, which sleep researchers use to
 * measure drowsiness: tap as soon as a counter starts, [ALERTNESS_TRIALS] times, at random moments.
 */
const val ALERTNESS_TRIALS = 10

/** A reaction this slow is a lapse: in the PVT, a sign of dozing off for a moment. */
internal val LAPSE = 500.milliseconds

/** A counter that runs this long without a tap stops by itself, as a lapse. */
internal val NO_RESPONSE = 1_500.milliseconds

/** How fast most people react on a phone when wide awake, for when the user hasn't measured their own speed. */
val TYPICAL_REACTION = 350.milliseconds

/** The user's own daytime reaction speed, measured in the alarm editor, or null if it hasn't been. */
val LocalAlertnessBaseline = staticCompositionLocalOf<Duration?> { null }

/** One run of the test: every reaction time, and how often a tap came before the counter started. */
data class AlertnessResult(val reactions: List<Duration>, val falseStarts: Int) {
    /** The PVT's main measure, which a single slip or lucky guess barely moves. */
    val median: Duration
        get() = reactions.sorted().let { sorted ->
            if (sorted.isEmpty()) Duration.INFINITE else (sorted[(sorted.size - 1) / 2] + sorted[sorted.size / 2]) / 2
        }

    val lapses: Int get() = reactions.count { it >= LAPSE }

    /** Whether this is close enough to [baseline], the user's daytime speed, to count as awake. */
    fun passes(baseline: Duration, difficulty: Difficulty): Boolean =
        median <= baseline * difficulty.tolerance && lapses <= difficulty.maxLapses && falseStarts <= MAX_FALSE_STARTS
}

/** How much slower than during the day the median may be. */
private val Difficulty.tolerance: Double
    get() = when (this) {
        Difficulty.Easy -> 1.4
        Difficulty.Normal -> 1.25
        Difficulty.Hard -> 1.15
    }

private val Difficulty.maxLapses: Int
    get() = when (this) {
        Difficulty.Easy -> 2
        Difficulty.Normal -> 1
        Difficulty.Hard -> 0
    }

/** Tapping at random gives these away. */
private const val MAX_FALSE_STARTS = 2

/** Tries before the gate gives way to math, so nobody gets stuck. */
internal const val ALERTNESS_TRIES = 3
