package io.github.geanyl17.openalarm.missions

import io.github.geanyl17.openalarm.core.Difficulty
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MemoryPatternsTest {
    private val patterns = MemoryPatterns(Random(seed = 7))

    @Test
    fun patternsHaveTheRequestedLengthAndValidTiles() {
        repeat(2_000) {
            val pattern = patterns.next(length = 6)
            assertEquals(6, pattern.size)
            assertTrue(pattern.all { it in 0 until MemoryPatterns.TILE_COUNT }, pattern.toString())
        }
    }

    @Test
    fun theSameTileNeverLightsTwiceInARow() {
        repeat(2_000) {
            val pattern = patterns.next(length = 7)
            assertTrue(pattern.zipWithNext().none { (a, b) -> a == b }, pattern.toString())
        }
    }

    @Test
    fun harderDifficultiesUseLongerPatterns() {
        val lengths = Difficulty.entries.map { it.patternLength }
        assertEquals(lengths.sorted(), lengths)
        assertTrue(lengths.first() > MemoryPatterns.MIN_LENGTH)
    }
}
