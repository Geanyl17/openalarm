package io.github.geanyl17.openalarm.missions

import io.github.geanyl17.openalarm.core.Difficulty
import kotlin.random.Random

/** An arithmetic problem, such as "17 × 6 + 38", and its answer. */
data class MathProblem(val question: String, val answer: Int)

/**
 * Makes problems that take a switched-on brain but never pen and paper. Answers are never
 * negative, so the keypad needs no minus key.
 */
class MathProblems(private val random: Random = Random.Default) {

    fun next(difficulty: Difficulty): MathProblem = when (difficulty) {
        Difficulty.Easy -> add(random.nextInt(2, 20), random.nextInt(2, 20))
        Difficulty.Normal -> when (random.nextInt(3)) {
            0 -> add(random.nextInt(10, 100), random.nextInt(10, 100))
            1 -> random.nextInt(30, 100).let { a -> subtract(a, random.nextInt(10, a)) }
            else -> multiply(random.nextInt(3, 13), random.nextInt(3, 13))
        }
        Difficulty.Hard -> {
            val a = random.nextInt(11, 20)
            val b = random.nextInt(3, 10)
            val c = random.nextInt(10, 100)
            if (random.nextBoolean() || c > a * b) {
                MathProblem("$a × $b + $c", a * b + c)
            } else {
                MathProblem("$a × $b − $c", a * b - c)
            }
        }
    }

    private fun add(a: Int, b: Int) = MathProblem("$a + $b", a + b)

    private fun subtract(a: Int, b: Int) = MathProblem("$a − $b", a - b)

    private fun multiply(a: Int, b: Int) = MathProblem("$a × $b", a * b)
}
