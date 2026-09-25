package io.github.geanyl17.openalarm.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SnoozeTaxTest {
    private val mission = Mission(MissionType.Math, Difficulty.Normal, rounds = 3)

    @Test
    fun withoutALimitSnoozingNeverChangesAnything() {
        val alarm = Alarm(hour = 7, minute = 0, snoozeMinutes = 10, missions = listOf(mission), snoozesTaken = 4)
        assertTrue(alarm.canSnooze)
        assertNull(alarm.snoozesLeft)
        assertEquals(10, alarm.nextSnoozeMinutes)
        assertEquals(listOf(mission), alarm.missionsDue)
    }

    @Test
    fun eachSnoozeUnderALimitIsHalfAsLongDownToAMinute() {
        val alarm = Alarm(hour = 7, minute = 0, snoozeMinutes = 10, snoozeLimit = 5)
        assertEquals(listOf(10, 5, 2, 1, 1), (0..4).map { alarm.copy(snoozesTaken = it).nextSnoozeMinutes })
    }

    @Test
    fun theLimitRunsOut() {
        val alarm = Alarm(hour = 7, minute = 0, snoozeLimit = 2)
        assertEquals(listOf(true, true, false), (0..2).map { alarm.copy(snoozesTaken = it).canSnooze })
        assertEquals(listOf(2, 1, 0), (0..2).map { alarm.copy(snoozesTaken = it).snoozesLeft })
    }

    @Test
    fun aLimitOfZeroMeansNoSnoozing() {
        assertFalse(Alarm(hour = 7, minute = 0, snoozeLimit = 0).canSnooze)
    }

    @Test
    fun everySnoozeUnderALimitAddsARoundToTheFirstMission() {
        val second = Mission(MissionType.Memory, Difficulty.Easy, rounds = 1)
        val alarm = Alarm(hour = 7, minute = 0, snoozeLimit = 3, snoozesTaken = 2, missions = listOf(mission, second))
        assertEquals(listOf(mission.copy(rounds = 5), second), alarm.missionsDue)
        val maxed = alarm.copy(missions = listOf(mission.copy(rounds = Mission.MAX_ROUNDS)))
        assertEquals(Mission.MAX_ROUNDS, maxed.missionsDue.single().rounds)
    }
}
