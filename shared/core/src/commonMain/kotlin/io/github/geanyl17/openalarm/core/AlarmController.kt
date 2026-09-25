package io.github.geanyl17.openalarm.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Every change to alarms goes through here, so the scheduled OS alarm always matches what's stored.
 * Snoozes, turning alarms off and ways around them also go into [wakeLog].
 */
class AlarmController(
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler,
    private val clock: Clock = Clock.System,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
    private val random: Random = Random.Default,
    private val wakeLog: WakeLog? = null,
) {
    val alarms: Flow<List<Alarm>> get() = repository.alarms

    suspend fun get(id: Long): Alarm? = repository.get(id)

    /** Saves a new or edited alarm. Editing an alarm cancels any pending snooze. */
    suspend fun save(alarm: Alarm): Alarm {
        if (alarm.id != 0L) logSkippedMission(alarm.id)
        val saved = repository.save(alarm.copy(snoozedUntil = null, snoozesTaken = 0))
        reschedule()
        return saved
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) {
        if (!enabled) logSkippedMission(id)
        repository.update(listOf(id)) { it.copy(enabled = enabled, snoozedUntil = null, snoozesTaken = 0) }
        reschedule()
    }

    suspend fun delete(id: Long) {
        logSkippedMission(id, includingCheckIns = true)
        repository.delete(id)
        reschedule()
    }

    /** The alarms that should ring at [at], the moment the OS woke the app for. */
    suspend fun dueAt(at: Instant): List<Alarm> = repository.alarms.first().dueAt(at, timeZone())

    /**
     * Turns off ringing alarms: one-time alarms switch off, and snoozes are cleared. Alarms with
     * check-ins get one or two, at random times 5 to 15 minutes from now.
     */
    suspend fun dismiss(ids: Collection<Long>) {
        val now = clock.now()
        repository.update(ids) {
            it.copy(
                enabled = it.repeats,
                snoozedUntil = null,
                snoozesTaken = 0,
                checkInsAt = if (it.checkIns) checkInTimes(now).map(Instant::toEpochMilliseconds) else emptyList(),
            )
        }
        wakeLog?.turnedOff(ids)
        reschedule()
    }

    /** The user answered the check-in due at [at]. */
    suspend fun checkedIn(ids: Collection<Long>, at: Instant) {
        repository.update(ids) { it.copy(checkInsAt = it.checkInsAt - at.toEpochMilliseconds()) }
        wakeLog?.checkedIn(ids)
        reschedule()
    }

    /** One check-in 5 to 15 minutes out, or two: one in the first half of that window and one in the second. */
    private fun checkInTimes(now: Instant): List<Instant> {
        fun between(from: Int, to: Int) = now + (from * 60 + random.nextInt((to - from) * 60)).seconds
        return if (random.nextBoolean()) listOf(between(5, 15)) else listOf(between(5, 10), between(10, 15))
    }

    /** Makes the ringing alarms ring again after their snooze length, and counts the snooze. */
    suspend fun snooze(ids: Collection<Long>) {
        val now = clock.now()
        repository.update(ids) {
            it.copy(snoozedUntil = (now + it.nextSnoozeMinutes.minutes).toEpochMilliseconds(), snoozesTaken = it.snoozesTaken + 1)
        }
        wakeLog?.snoozed(ids)
        reschedule()
    }

    /**
     * Switching off, editing or deleting a snoozed alarm cancels the snooze, so its mission is never done.
     * That's allowed, but it goes into the Honesty Log. Deleting also cancels pending check-ins.
     */
    private suspend fun logSkippedMission(id: Long, includingCheckIns: Boolean = false) {
        val alarm = repository.get(id) ?: return
        val snoozedWithMission = alarm.snoozedUntil != null && alarm.missions.isNotEmpty()
        if (snoozedWithMission || (includingCheckIns && alarm.checkInsAt.isNotEmpty())) {
            wakeLog?.escaped(listOf(id), Escape.TurnedOffInApp)
        }
    }

    /** Schedules the next alarm with the OS. Returns it, or null if no alarm is on. */
    suspend fun reschedule(): UpcomingAlarm? {
        val next = repository.alarms.first().nextUpcoming(clock.now(), timeZone())
        scheduler.schedule(next)
        return next
    }
}
