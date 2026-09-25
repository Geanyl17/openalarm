package io.github.geanyl17.openalarm.missions

import io.github.geanyl17.openalarm.core.Difficulty
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StroopTest {

    @Test
    fun wordsNeverMatchTheirInkAndTheInkIsAChoice() {
        val rounds = StroopRounds(Random(7))
        for (difficulty in Difficulty.entries) {
            repeat(200) {
                val round = rounds.next(difficulty)
                assertNotEquals(round.word, round.ink)
                assertTrue(round.ink in round.choices)
                assertTrue(round.word in round.choices)
                assertEquals(difficulty.stroopColors, round.choices.toSet().size)
                if (difficulty == Difficulty.Hard) {
                    val inks = round.choiceInks!!
                    assertEquals(round.choices.toSet(), inks.toSet())
                    round.choices.zip(inks).forEach { (choice, ink) -> assertNotEquals(choice, ink) }
                } else {
                    assertNull(round.choiceInks)
                }
            }
        }
    }
}
