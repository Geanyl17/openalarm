package io.github.geanyl17.openalarm.missions

import io.github.geanyl17.openalarm.core.Difficulty
import kotlin.random.Random

/** How many tiles light up in one pattern. */
val Difficulty.patternLength: Int
    get() = when (this) {
        Difficulty.Easy -> 4
        Difficulty.Normal -> 5
        Difficulty.Hard -> 7
    }

/** Makes the patterns for the memory mission, on a 3×3 grid of tiles numbered 0 to 8. */
class MemoryPatterns(private val random: Random = Random.Default) {

    /** A random pattern. The same tile never lights twice in a row, so every step is visible. */
    fun next(length: Int): List<Int> {
        val tiles = mutableListOf<Int>()
        repeat(length) {
            var tile: Int
            do {
                tile = random.nextInt(TILE_COUNT)
            } while (tile == tiles.lastOrNull())
            tiles += tile
        }
        return tiles
    }

    companion object {
        const val TILE_COUNT = 9

        /** Patterns never get shorter than this, even after repeated misses. */
        const val MIN_LENGTH = 3
    }
}
