package io.github.geanyl17.openalarm.missions

import io.github.geanyl17.openalarm.core.Difficulty
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class AlertnessGateTest {
    private val baseline = 300.milliseconds

    private fun result(vararg millis: Int, falseStarts: Int = 0) = AlertnessResult(millis.map { it.milliseconds }, falseStarts)

    @Test
    fun theMedianIgnoresASingleSlip() {
        val run = result(290, 300, 310, 1500, 280, 305, 295, 300, 315, 290)
        assertEquals(300.milliseconds, run.median)
        assertEquals(1, run.lapses)
    }

    @Test
    fun beingAboutAsFastAsDuringTheDayPasses() {
        assertTrue(result(330, 350, 340, 360, 345, 355, 335, 350, 340, 365).passes(baseline, Difficulty.Normal))
    }

    @Test
    fun beingMuchSlowerFails() {
        assertFalse(result(420, 450, 400, 480, 430, 460, 440, 410, 470, 445).passes(baseline, Difficulty.Normal))
    }

    @Test
    fun harderAllowsLessSlowdownAndFewerLapses() {
        val groggy = result(360, 370, 355, 380, 365, 375, 360, 370, 365, 600)
        assertTrue(groggy.passes(baseline, Difficulty.Easy))
        assertTrue(groggy.passes(baseline, Difficulty.Normal))
        assertFalse(groggy.passes(baseline, Difficulty.Hard))
    }

    @Test
    fun tappingAtRandomFailsOnFalseStarts() {
        assertFalse(result(250, 260, 240, 255, 245, 250, 260, 240, 250, 255, falseStarts = 3).passes(baseline, Difficulty.Easy))
    }
}
