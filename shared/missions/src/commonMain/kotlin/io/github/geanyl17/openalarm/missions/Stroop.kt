package io.github.geanyl17.openalarm.missions

import io.github.geanyl17.openalarm.core.Difficulty
import kotlin.random.Random

/** Ink colors from the Okabe–Ito palette, which people with color blindness can tell apart too. */
internal enum class Ink(val argb: Long) {
    Blue(0xFF0072B2),
    Orange(0xFFE69F00),
    Pink(0xFFCC79A7),
    Green(0xFF009E73),
}

/**
 * A color [word] written in a different [ink]. The answer is the ink, picked from [choices]. On hard,
 * each choice is written in the ink of another choice ([choiceInks]) to trip you up some more.
 */
internal data class StroopRound(val word: Ink, val ink: Ink, val choices: List<Ink>, val choiceInks: List<Ink>?)

internal val Difficulty.stroopColors: Int
    get() = if (this == Difficulty.Easy) 3 else 4

/** Makes Stroop rounds, where the word never matches its ink. */
internal class StroopRounds(private val random: Random = Random.Default) {
    fun next(difficulty: Difficulty): StroopRound {
        val palette = Ink.entries.take(difficulty.stroopColors)
        val ink = palette.random(random)
        val word = (palette - ink).random(random)
        val choices = palette.shuffled(random)
        val choiceInks = if (difficulty == Difficulty.Hard) {
            // Shifting by a random amount other than zero never leaves a choice in its own ink.
            val shift = 1 + random.nextInt(choices.size - 1)
            choices.indices.map { choices[(it + shift) % choices.size] }
        } else {
            null
        }
        return StroopRound(word, ink, choices, choiceInks)
    }
}
