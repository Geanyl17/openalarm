package io.github.geanyl17.openalarm.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Every change to alarms goes through here, so the scheduled OS alarm always matches what's stored.
 */
class AlarmController(
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler,
    private val clock: Clock = Clock.System,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) {
    val alarms: Flow<List<Alarm>> get() = repository.alarms

    suspend fun get(id: Long): Alarm? = repository.get(id)

    /** Saves a new or edited alarm. Editing an alarm cancels any pending snooze. */
    suspend fun save(alarm: Alarm): Alarm {
        val saved = repository.save(alarm.copy(snoozedUntil = null, snoozesTaken = 0))
        reschedule()
        return saved
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) {
        repository.update(listOf(id)) { it.copy(enabled = enabled, snoozedUntil = null, snoozesTaken = 0) }
        reschedule()
    }

    suspend fun delete(id: Long) {
        repository.delete(id)
        reschedule()
    }

    /** The alarms that should ring at [at], the moment the OS woke the app for. */
    suspend fun dueAt(at: Instant): List<Alarm> = repository.alarms.first().dueAt(at, timeZone())

    /** Turns off ringing alarms: one-time alarms switch off, and snoozes are cleared. */
    suspend fun dismiss(ids: Collection<Long>) {
        repository.update(ids) { it.copy(enabled = it.repeats, snoozedUntil = null, snoozesTaken = 0) }
        reschedule()
    }

    /** Makes the ringing alarms ring again after their snooze length, and counts the snooze. */
    suspend fun snooze(ids: Collection<Long>) {
        val now = clock.now()
        repository.update(ids) {
            it.copy(snoozedUntil = (now + it.nextSnoozeMinutes.minutes).toEpochMilliseconds(), snoozesTaken = it.snoozesTaken + 1)
        }
        reschedule()
    }

    /** Schedules the next alarm with the OS. Returns it, or null if no alarm is on. */
    suspend fun reschedule(): UpcomingAlarm? {
        val next = repository.alarms.first().nextUpcoming(clock.now(), timeZone())
        scheduler.schedule(next)
        return next
    }
}
