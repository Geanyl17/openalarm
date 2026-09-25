package io.github.geanyl17.openalarm.missions

import io.github.geanyl17.openalarm.core.Difficulty
import io.github.geanyl17.openalarm.core.Mission
import io.github.geanyl17.openalarm.core.MissionType
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StepsTest {
    private val sampleNanos = 20_000_000L // 50 readings a second

    /** Counts steps in [seconds] of readings whose strength is [magnitude] at each moment. */
    private fun steps(seconds: Int, magnitude: (t: Double) -> Double): Int {
        val detector = StepDetector()
        var steps = 0
        var time = 0L
        while (time < seconds * 1_000_000_000L) {
            val value = magnitude(time / 1e9).toFloat()
            // Split between the axes, like a phone held at an angle.
            if (detector.onAcceleration(value * 0.6f, value * 0.8f, 0f, time)) steps++
            time += sampleNanos
        }
        return steps
    }

    @Test
    fun walkingAtTwoStepsASecondCountsTwoStepsASecond() {
        val count = steps(10) { t -> 9.81 + 2.5 * sin(2 * PI * 2 * t) }
        assertTrue(count in 18..21, "Counted $count")
    }

    @Test
    fun aPhoneLyingStillCountsNothing() {
        val random = Random(1)
        assertEquals(0, steps(10) { 9.81 + random.nextDouble(-0.1, 0.1) })
    }

    @Test
    fun shakingFasterThanAnyoneRunsIsCappedAtRunningSpeed() {
        val count = steps(10) { t -> 9.81 + 6 * sin(2 * PI * 8 * t) }
        assertTrue(count <= 36, "Counted $count")
    }

    @Test
    fun harderAndMoreRoundsMeanMoreSteps() {
        assertEquals(60, Mission(MissionType.Steps, Difficulty.Normal, rounds = 3).stepsTarget)
        assertEquals(10, Mission(MissionType.Steps, Difficulty.Easy, rounds = 1).stepsTarget)
        assertEquals(150, Mission(MissionType.Steps, Difficulty.Hard, rounds = 5).stepsTarget)
    }
}
