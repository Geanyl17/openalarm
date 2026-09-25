package io.github.geanyl17.openalarm.missions

import io.github.geanyl17.openalarm.core.Difficulty
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** How fast a tap has to come after "Tap!". Someone half asleep takes noticeably longer. */
internal val Difficulty.reactionLimit: Duration
    get() = when (this) {
        Difficulty.Easy -> 900.milliseconds
        Difficulty.Normal -> 650.milliseconds
        Difficulty.Hard -> 500.milliseconds
    }

/** Nobody reacts faster than this, so a quicker tap was a guess made before "Tap!" showed up. */
internal val MIN_REACTION = 100.milliseconds

internal enum class ReactionResult { TooSoon, TooSlow, InTime }

/** Judges a tap that came [sinceGo] after "Tap!" appeared, or before it did if that's null. */
internal fun judgeReaction(sinceGo: Duration?, limit: Duration): ReactionResult = when {
    sinceGo == null || sinceGo < MIN_REACTION -> ReactionResult.TooSoon
    sinceGo <= limit -> ReactionResult.InTime
    else -> ReactionResult.TooSlow
}
