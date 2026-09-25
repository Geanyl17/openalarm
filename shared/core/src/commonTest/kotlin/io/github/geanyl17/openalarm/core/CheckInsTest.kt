package io.github.geanyl17.openalarm.core

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class CheckInsTest {
    private val newYork = TimeZone.of("America/New_York")
    private val now = Instant.parse("2026-06-10T11:00:00Z") // 07:00 in New York

    @Test
    fun aSwitchedOffAlarmStillHasItsCheckIns() {
        val checkIn = now + 8.minutes
        val alarm = Alarm(hour = 7, minute = 0, enabled = false, checkInsAt = listOf(checkIn.toEpochMilliseconds()))
        assertEquals(checkIn, alarm.nextTrigger(now, newYork))
        assertTrue(alarm.isCheckInAt(checkIn))
        assertEquals(listOf(alarm), listOf(alarm).dueAt(checkIn, newYork))
    }

    @Test
    fun theSoonestOfCheckInAndRegularTimeComesFirst() {
        val checkIn = now + 8.minutes
        val alarm = Alarm(hour = 7, minute = 5, checkInsAt = listOf(checkIn.toEpochMilliseconds()))
        assertEquals(now + 5.minutes, alarm.nextTrigger(now, newYork))
        assertEquals(checkIn, alarm.nextTrigger(now + 5.minutes, newYork))
    }

    @Test
    fun turningAnAlarmOffSchedulesOneOrTwoCheckInsFiveToFifteenMinutesLater() = runTest {
        val counts = mutableSetOf<Int>()
        for (seed in 0 until 40) {
            val repository = FakeRepository()
            val controller = AlarmController(repository, {}, FixedClock(now), { newYork }, Random(seed))
            val alarm = controller.save(Alarm(hour = 7, minute = 0, checkIns = true))
            controller.dismiss(listOf(alarm.id))
            val times = repository.get(alarm.id)!!.checkInsAt.map { Instant.fromEpochMilliseconds(it) }
            counts += times.size
            assertTrue(times.all { it >= now + 5.minutes && it <= now + 15.minutes }, "seed $seed: $times")
            assertEquals(times.sorted(), times)
        }
        assertEquals(setOf(1, 2), counts)
    }

    @Test
    fun alarmsWithoutCheckInsGetNone() = runTest {
        val repository = FakeRepository()
        val controller = AlarmController(repository, {}, FixedClock(now), { newYork })
        val alarm = controller.save(Alarm(hour = 7, minute = 0))
        controller.dismiss(listOf(alarm.id))
        assertEquals(emptyList(), repository.get(alarm.id)!!.checkInsAt)
    }

    @Test
    fun answeringACheckInRemovesItAndSchedulesTheNext() = runTest {
        val repository = FakeRepository()
        val scheduled = mutableListOf<UpcomingAlarm?>()
        val controller = AlarmController(repository, { scheduled += it }, FixedClock(now), { newYork })
        val first = now + 6.minutes
        val second = now + 12.minutes
        val alarm = repository.save(
            Alarm(hour = 7, minute = 0, enabled = false, checkIns = true, checkInsAt = listOf(first, second).map { it.toEpochMilliseconds() }),
        )
        controller.checkedIn(listOf(alarm.id), first)
        assertEquals(listOf(second.toEpochMilliseconds()), repository.get(alarm.id)!!.checkInsAt)
        assertEquals(second, scheduled.last()?.at)
    }

    @Test
    fun aHarderMissionIsOneLevelUpWithAnotherRound() {
        assertEquals(Mission(MissionType.Memory, Difficulty.Normal, 3), Mission(MissionType.Memory, Difficulty.Easy, 2).harder())
        assertEquals(Mission(MissionType.Math, Difficulty.Hard, Mission.MAX_ROUNDS), Mission(MissionType.Math, Difficulty.Hard, Mission.MAX_ROUNDS).harder())
    }
}
