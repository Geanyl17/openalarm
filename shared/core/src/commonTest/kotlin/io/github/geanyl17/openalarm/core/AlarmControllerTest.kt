package io.github.geanyl17.openalarm.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class AlarmControllerTest {
    private val newYork = TimeZone.of("America/New_York")
    private val clock = FixedClock(Instant.parse("2026-06-10T12:00:00Z")) // 08:00 in New York
    private val repository = FakeRepository()
    private val scheduled = mutableListOf<UpcomingAlarm?>()
    private val controller = AlarmController(repository, { scheduled += it }, clock, { newYork })

    @Test
    fun savingANewAlarmAssignsAnIdAndSchedulesIt() = runTest {
        val saved = controller.save(Alarm(hour = 9, minute = 0))
        assertEquals(1, saved.id)
        assertEquals(UpcomingAlarm(saved, Instant.parse("2026-06-10T13:00:00Z")), scheduled.last())
    }

    @Test
    fun dismissSwitchesOffOneTimeAlarmsButKeepsRepeatingOnes() = runTest {
        val once = controller.save(Alarm(hour = 8, minute = 0))
        val daily = controller.save(Alarm(hour = 8, minute = 0, repeat = RepeatDays.EveryDay))
        controller.dismiss(listOf(once.id, daily.id))
        assertFalse(repository.get(once.id)!!.enabled)
        assertTrue(repository.get(daily.id)!!.enabled)
        assertEquals(daily.id, scheduled.last()?.alarm?.id)
    }

    @Test
    fun snoozeRingsAgainAfterTheSnoozeLength() = runTest {
        val alarm = controller.save(Alarm(hour = 8, minute = 0, snoozeMinutes = 5))
        controller.snooze(listOf(alarm.id))
        val expected = clock.now() + 5.minutes
        assertEquals(expected.toEpochMilliseconds(), repository.get(alarm.id)!!.snoozedUntil)
        assertEquals(expected, scheduled.last()?.at)
    }

    @Test
    fun snoozesUnderALimitGetShorterAndAreCountedUntilTheAlarmIsTurnedOff() = runTest {
        val alarm = controller.save(Alarm(hour = 8, minute = 0, snoozeMinutes = 10, snoozeLimit = 2))
        controller.snooze(listOf(alarm.id))
        controller.snooze(listOf(alarm.id))
        val snoozed = repository.get(alarm.id)!!
        assertEquals(2, snoozed.snoozesTaken)
        assertEquals((clock.now() + 5.minutes).toEpochMilliseconds(), snoozed.snoozedUntil)
        controller.dismiss(listOf(alarm.id))
        assertEquals(0, repository.get(alarm.id)!!.snoozesTaken)
    }

    @Test
    fun editingAnAlarmClearsItsSnooze() = runTest {
        val alarm = controller.save(Alarm(hour = 8, minute = 0))
        controller.snooze(listOf(alarm.id))
        controller.save(repository.get(alarm.id)!!.copy(label = "Gym"))
        assertNull(repository.get(alarm.id)!!.snoozedUntil)
    }

    @Test
    fun deletingTheLastAlarmCancelsTheSchedule() = runTest {
        val alarm = controller.save(Alarm(hour = 9, minute = 0))
        controller.delete(alarm.id)
        assertNull(scheduled.last())
        assertTrue(repository.alarms.first().isEmpty())
    }

    @Test
    fun switchingAnAlarmOffUnschedulesIt() = runTest {
        val alarm = controller.save(Alarm(hour = 9, minute = 0))
        controller.setEnabled(alarm.id, false)
        assertNull(scheduled.last())
    }
}

internal class FakeWakeLogRepository : WakeLogRepository {
    private val state = MutableStateFlow<List<WakeUp>>(emptyList())
    override val wakeUps: Flow<List<WakeUp>> = state

    override suspend fun update(transform: (List<WakeUp>) -> List<WakeUp>) = state.update(transform)
}

internal class FixedClock(private val now: Instant) : Clock {
    override fun now(): Instant = now
}

internal class FakeRepository : AlarmRepository {
    private val state = MutableStateFlow<List<Alarm>>(emptyList())
    private var nextId = 1L

    override val alarms: Flow<List<Alarm>> = state.map { list -> list.sortedWith(compareBy({ it.hour }, { it.minute }, { it.id })) }

    override suspend fun get(id: Long): Alarm? = state.value.find { it.id == id }

    override suspend fun save(alarm: Alarm): Alarm {
        val saved = if (alarm.id == 0L) alarm.copy(id = nextId++) else alarm
        state.update { list -> list.filterNot { it.id == saved.id } + saved }
        return saved
    }

    override suspend fun delete(id: Long) = state.update { list -> list.filterNot { it.id == id } }

    override suspend fun update(ids: Collection<Long>, transform: (Alarm) -> Alarm) =
        state.update { list -> list.map { if (it.id in ids) transform(it) else it } }
}
