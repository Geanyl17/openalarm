package io.github.geanyl17.openalarm.missions

import io.github.geanyl17.openalarm.core.Difficulty
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class ReactionTest {

    @Test
    fun tapsAreJudgedAgainstTheLimit() {
        val limit = 650.milliseconds
        assertEquals(ReactionResult.TooSoon, judgeReaction(null, limit))
        assertEquals(ReactionResult.TooSoon, judgeReaction(40.milliseconds, limit))
        assertEquals(ReactionResult.InTime, judgeReaction(300.milliseconds, limit))
        assertEquals(ReactionResult.InTime, judgeReaction(limit, limit))
        assertEquals(ReactionResult.TooSlow, judgeReaction(651.milliseconds, limit))
    }

    @Test
    fun harderReactionsAllowLessTime() {
        assertTrue(Difficulty.Easy.reactionLimit > Difficulty.Normal.reactionLimit)
        assertTrue(Difficulty.Normal.reactionLimit > Difficulty.Hard.reactionLimit)
    }
}
