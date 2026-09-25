package io.github.geanyl17.openalarm.missions

import io.github.geanyl17.openalarm.core.Difficulty
import io.github.geanyl17.openalarm.core.Mission
import io.github.geanyl17.openalarm.core.MissionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class LightsOnTest {

    @Test
    fun brightTimeCountsFromTheFirstBrightReading() {
        val timer = BrightTimer(minLux = 80f)
        assertEquals(Duration.ZERO, timer.update(10f, 0.seconds))
        assertEquals(Duration.ZERO, timer.update(150f, 2.seconds))
        assertEquals(5.seconds, timer.update(150f, 7.seconds))
    }

    @Test
    fun aDarkMomentStartsItOver() {
        val timer = BrightTimer(minLux = 80f)
        timer.update(150f, 0.seconds)
        assertEquals(9.seconds, timer.update(150f, 9.seconds))
        assertEquals(Duration.ZERO, timer.update(20f, 10.seconds))
        assertEquals(1.seconds, timer.update(150f, 12.seconds).let { timer.update(150f, 13.seconds) })
    }

    @Test
    fun harderMeansBrighterAndMoreRoundsMeansLonger() {
        assertTrue(Difficulty.Easy.lightsOnLux < Difficulty.Normal.lightsOnLux)
        assertTrue(Difficulty.Normal.lightsOnLux < Difficulty.Hard.lightsOnLux)
        assertEquals(20.seconds, Mission(MissionType.LightsOn, rounds = 2).lightsOnDuration)
    }
}
