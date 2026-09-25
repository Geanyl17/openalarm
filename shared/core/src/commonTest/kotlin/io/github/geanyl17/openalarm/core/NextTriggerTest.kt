package io.github.geanyl17.openalarm.core

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class NextTriggerTest {
    private val newYork = TimeZone.of("America/New_York")

    // In June 2026, New York is on EDT (UTC-4). 2026-06-10 is a Wednesday, 2026-06-12 a Friday.
    private fun at(iso: String) = Instant.parse(iso)

    @Test
    fun oneTimeAlarmLaterTodayRingsToday() {
        val alarm = Alarm(hour = 9, minute = 30)
        assertEquals(at("2026-06-10T13:30:00Z"), alarm.nextTrigger(at("2026-06-10T12:00:00Z"), newYork))
    }

    @Test
    fun oneTimeAlarmEarlierTodayRingsTomorrow() {
        val alarm = Alarm(hour = 7, minute = 0)
        assertEquals(at("2026-06-11T11:00:00Z"), alarm.nextTrigger(at("2026-06-10T12:00:00Z"), newYork))
    }

    @Test
    fun alarmAtExactlyNowRingsTomorrow() {
        val alarm = Alarm(hour = 8, minute = 0)
        assertEquals(at("2026-06-11T12:00:00Z"), alarm.nextTrigger(at("2026-06-10T12:00:00Z"), newYork))
    }

    @Test
    fun workdayAlarmOnFridayEveningRingsOnMonday() {
        val alarm = Alarm(hour = 7, minute = 0, repeat = RepeatDays.WorkDays)
        // Friday 20:00 EDT is Saturday 00:00 UTC.
        assertEquals(at("2026-06-15T11:00:00Z"), alarm.nextTrigger(at("2026-06-13T00:00:00Z"), newYork))
    }

    @Test
    fun disabledAlarmNeverRings() {
        assertNull(Alarm(hour = 7, minute = 0, enabled = false).nextTrigger(at("2026-06-10T00:00:00Z"), newYork))
    }

    @Test
    fun pendingSnoozeRingsBeforeTheRegularTime() {
        val snoozedUntil = at("2026-06-10T11:09:00Z")
        val alarm = Alarm(hour = 7, minute = 0, snoozedUntil = snoozedUntil.toEpochMilliseconds())
        assertEquals(snoozedUntil, alarm.nextTrigger(at("2026-06-10T11:01:00Z"), newYork))
    }

    @Test
    fun expiredSnoozeIsIgnored() {
        val alarm = Alarm(hour = 7, minute = 0, snoozedUntil = at("2026-06-10T11:09:00Z").toEpochMilliseconds())
        assertEquals(at("2026-06-11T11:00:00Z"), alarm.nextTrigger(at("2026-06-10T12:00:00Z"), newYork))
    }

    @Test
    fun alarmInsideTheSpringForwardGapMovesToAfterTheGap() {
        // On 2026-03-08, New York clocks jump from 2:00 EST to 3:00 EDT, so 2:30 doesn't exist.
        val alarm = Alarm(hour = 2, minute = 30, repeat = RepeatDays.EveryDay)
        assertEquals(at("2026-03-08T07:30:00Z"), alarm.nextTrigger(at("2026-03-08T05:00:00Z"), newYork))
    }

    @Test
    fun alarmOnSpringForwardDayUsesTheNewOffset() {
        val alarm = Alarm(hour = 7, minute = 0)
        assertEquals(at("2026-03-08T11:00:00Z"), alarm.nextTrigger(at("2026-03-08T05:00:00Z"), newYork))
    }

    @Test
    fun alarmInTheFallBackOverlapRingsOnlyOnce() {
        // On 2026-11-01, New York clocks go from 2:00 EDT back to 1:00 EST, so 1:30 happens twice.
        val alarm = Alarm(hour = 1, minute = 30, repeat = RepeatDays.EveryDay)
        val first = alarm.nextTrigger(at("2026-11-01T04:00:00Z"), newYork)
        assertEquals(at("2026-11-01T05:30:00Z"), first)
        // Right after it rang, the next time is the following night, not the second 1:30 an hour later.
        assertEquals(at("2026-11-02T06:30:00Z"), alarm.nextTrigger(first!!, newYork))
    }

    @Test
    fun sameAlarmRingsAtTheLocalTimeOfEachTimeZone() {
        val alarm = Alarm(hour = 7, minute = 0)
        val after = at("2026-06-10T00:00:00Z")
        assertEquals(at("2026-06-10T05:00:00Z"), alarm.nextTrigger(after, TimeZone.of("Europe/Berlin")))
        assertEquals(at("2026-06-10T11:00:00Z"), alarm.nextTrigger(after, newYork))
    }

    @Test
    fun nextUpcomingPicksTheEarliestAlarm() {
        val alarms = listOf(
            Alarm(id = 1, hour = 7, minute = 0),
            Alarm(id = 2, hour = 6, minute = 30),
            Alarm(id = 3, hour = 5, minute = 0, enabled = false),
        )
        val next = alarms.nextUpcoming(at("2026-06-10T00:00:00Z"), newYork)
        assertEquals(2, next?.alarm?.id)
        assertEquals(at("2026-06-10T10:30:00Z"), next?.at)
    }

    @Test
    fun nextUpcomingIsNullWhenEveryAlarmIsOff() {
        assertNull(listOf(Alarm(hour = 7, minute = 0, enabled = false)).nextUpcoming(at("2026-06-10T00:00:00Z"), newYork))
    }

    @Test
    fun dueAtFindsTheAlarmsRingingAtThatMoment() {
        val alarms = listOf(Alarm(id = 1, hour = 7, minute = 0), Alarm(id = 2, hour = 7, minute = 0), Alarm(id = 3, hour = 8, minute = 0))
        val sevenAm = at("2026-06-10T11:00:00Z")
        assertEquals(listOf(1L, 2L), alarms.dueAt(sevenAm, newYork).map { it.id })
        assertTrue(alarms.dueAt(sevenAm + 1.minutes, newYork).isEmpty())
    }
}
