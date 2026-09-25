package io.github.geanyl17.openalarm.missions

import io.github.geanyl17.openalarm.core.Difficulty
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MathProblemsTest {
    private val problems = MathProblems(Random(seed = 42))

    @Test
    fun everyAnswerIsCorrectAndNeverNegative() {
        for (difficulty in Difficulty.entries) {
            repeat(2_000) {
                val problem = problems.next(difficulty)
                assertEquals(evaluate(problem.question), problem.answer, problem.question)
                assertTrue(problem.answer >= 0, problem.question)
            }
        }
    }

    @Test
    fun easyProblemsAddSmallNumbers() {
        repeat(2_000) {
            val (a, operator, b) = problems.next(Difficulty.Easy).question.split(" ")
            assertEquals("+", operator)
            assertTrue(a.toInt() in 2..19 && b.toInt() in 2..19)
        }
    }

    @Test
    fun normalProblemsMixOperators() {
        val operators = List(2_000) { problems.next(Difficulty.Normal).question.split(" ")[1] }.toSet()
        assertEquals(setOf("+", "−", "×"), operators)
    }

    @Test
    fun hardProblemsMultiplyThenAddOrSubtract() {
        repeat(2_000) {
            val tokens = problems.next(Difficulty.Hard).question.split(" ")
            assertEquals(5, tokens.size)
            assertEquals("×", tokens[1])
            assertTrue(tokens[0].toInt() in 11..19 && tokens[2].toInt() in 3..9 && tokens[4].toInt() in 10..99)
        }
    }

    /** Evaluates "a op b" or "a × b op c", the only forms the generator makes. */
    private fun evaluate(question: String): Int {
        val tokens = question.split(" ")
        fun apply(left: Int, operator: String, right: Int) = when (operator) {
            "+" -> left + right
            "−" -> left - right
            "×" -> left * right
            else -> error("Unknown operator $operator in $question")
        }
        val first = apply(tokens[0].toInt(), tokens[1], tokens[2].toInt())
        return if (tokens.size == 3) first else apply(first, tokens[3], tokens[4].toInt())
    }
}
