package io.github.geanyl17.openalarm.core

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/** An alarm together with the moment it rings next. */
data class UpcomingAlarm(val alarm: Alarm, val at: Instant)

/**
 * The next moment this alarm rings, strictly after [after]: its regular time, a snooze or a check-in.
 * A switched-off alarm still has its pending check-ins, since one-time alarms switch off once they're done.
 */
fun Alarm.nextTrigger(after: Instant, timeZone: TimeZone): Instant? {
    val checkIn = checkInsAt.map { Instant.fromEpochMilliseconds(it) }.filter { it > after }.minOrNull()
    if (!enabled) return checkIn
    val snooze = snoozedUntil?.let { Instant.fromEpochMilliseconds(it) }?.takeIf { it > after }
    val regular = nextRegularTrigger(after, timeZone)
    return listOfNotNull(snooze, regular, checkIn).minOrNull()
}

/** Whether this alarm is due at [at] for a check-in, rather than to ring. */
fun Alarm.isCheckInAt(at: Instant): Boolean = at.toEpochMilliseconds() in checkInsAt

private fun Alarm.nextRegularTrigger(after: Instant, timeZone: TimeZone): Instant? {
    val startDate = after.toLocalDateTime(timeZone).date
    // Eight days covers the rest of today plus a full week of repeat days.
    for (offset in 0..7) {
        val date = startDate.plus(offset, DateTimeUnit.DAY)
        if (repeats && date.dayOfWeek !in repeat) continue
        // In a daylight-saving gap (2:30 on spring-forward day) this moves the time forward by the gap.
        // In an overlap (1:30 on fall-back day) it picks the first 1:30, so the alarm rings only once.
        val candidate = LocalDateTime(date, time).toInstant(timeZone)
        if (candidate > after) return candidate
    }
    return null
}

/** The alarm that rings soonest after [after]; ties go to the lowest id. */
fun Iterable<Alarm>.nextUpcoming(after: Instant, timeZone: TimeZone): UpcomingAlarm? =
    mapNotNull { alarm -> alarm.nextTrigger(after, timeZone)?.let { UpcomingAlarm(alarm, it) } }
        .minWithOrNull(compareBy<UpcomingAlarm> { it.at }.thenBy { it.alarm.id })

/** The alarms scheduled to ring at exactly [at]. */
fun Iterable<Alarm>.dueAt(at: Instant, timeZone: TimeZone): List<Alarm> =
    filter { it.nextTrigger(at - 1.milliseconds, timeZone) == at }
