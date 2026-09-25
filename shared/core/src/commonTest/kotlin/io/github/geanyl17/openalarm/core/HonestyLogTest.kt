package io.github.geanyl17.openalarm.core

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class HonestyLogTest {
    private val clock = FixedClock(Instant.parse("2026-06-10T11:00:00Z")) // 07:00 in New York
    private val repository = FakeRepository()
    private val log = WakeLog(FakeWakeLogRepository(), clock)
    private val controller = AlarmController(repository, {}, clock, { TimeZone.of("America/New_York") }, wakeLog = log)
    private val withMission = Alarm(hour = 7, minute = 0, missions = listOf(Mission()))

    private suspend fun ringAndSnooze(alarm: Alarm): Alarm {
        val saved = controller.save(alarm)
        log.rang(listOf(saved), checkIn = false)
        controller.snooze(listOf(saved.id))
        return saved
    }

    @Test
    fun switchingOffASnoozedAlarmSkipsItsMissionAndIsLogged() = runTest {
        val alarm = ringAndSnooze(withMission)
        controller.setEnabled(alarm.id, false)
        val wakeUp = log.wakeUps.first().single()
        assertEquals(listOf(Escape.TurnedOffInApp), wakeUp.escapes)
        assertEquals(1, wakeUp.snoozes)
    }

    @Test
    fun editingOrDeletingASnoozedAlarmIsLoggedToo() = runTest {
        val edited = ringAndSnooze(withMission)
        controller.save(edited.copy(label = "Changed"))
        val deleted = ringAndSnooze(withMission.copy(hour = 8))
        controller.delete(deleted.id)
        assertEquals(listOf(listOf(Escape.TurnedOffInApp), listOf(Escape.TurnedOffInApp)), log.wakeUps.first().map { it.escapes })
    }

    @Test
    fun anAlarmWithoutAMissionCanBeSwitchedOffFreely() = runTest {
        val alarm = ringAndSnooze(withMission.copy(missions = emptyList()))
        controller.setEnabled(alarm.id, false)
        assertEquals(emptyList(), log.wakeUps.first().single().escapes)
    }

    @Test
    fun turningOffAndCheckingInAreRecorded() = runTest {
        val alarm = controller.save(withMission.copy(checkIns = true))
        log.rang(listOf(alarm), checkIn = false)
        controller.dismiss(listOf(alarm.id))
        controller.checkedIn(listOf(alarm.id), clock.now())
        val wakeUp = log.wakeUps.first().single()
        assertEquals(clock.now().toEpochMilliseconds(), wakeUp.offAt)
        assertEquals(1, wakeUp.checkIns)
        assertEquals(emptyList(), wakeUp.escapes)
    }
}
