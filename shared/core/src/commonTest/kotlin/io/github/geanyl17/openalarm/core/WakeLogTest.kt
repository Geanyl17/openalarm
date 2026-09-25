package io.github.geanyl17.openalarm.core

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class WakeLogTest {
    private val utc = TimeZone.UTC
    private val morning = Instant.parse("2026-06-10T07:00:00Z")
    private val alarm = Alarm(id = 1, hour = 7, minute = 0, label = "Work")

    private fun List<WakeUp>.honestMorning(at: Instant) = rang(listOf(alarm), at, checkIn = false).turnedOff(listOf(1), at + 3.minutes)

    @Test
    fun aRingStartsAWakeUpAndSnoozesContinueIt() {
        val log = emptyList<WakeUp>()
            .rang(listOf(alarm), morning, checkIn = false)
            .snoozed(listOf(1), morning + 1.minutes)
            .rang(listOf(alarm), morning + 11.minutes, checkIn = false)
            .turnedOff(listOf(1), morning + 12.minutes)
        assertEquals(
            listOf(WakeUp(1, "Work", morning.toEpochMilliseconds(), (morning + 12.minutes).toEpochMilliseconds(), snoozes = 1)),
            log,
        )
    }

    @Test
    fun checkInsBelongToTheWakeUpTheyFollow() {
        val log = emptyList<WakeUp>()
            .honestMorning(morning)
            .rang(listOf(alarm), morning + 10.minutes, checkIn = true)
            .checkedIn(listOf(1), morning + 10.minutes)
        assertEquals(1, log.size)
        assertEquals(1, log.single().checkIns)
    }

    @Test
    fun theNextMorningIsANewWakeUp() {
        val log = emptyList<WakeUp>().honestMorning(morning).honestMorning(morning + 1.days)
        assertEquals(2, log.size)
    }

    @Test
    fun aWakeUpLeftOpenDoesNotSwallowTheNextDay() {
        val log = emptyList<WakeUp>()
            .rang(listOf(alarm), morning, checkIn = false)
            .snoozed(listOf(1), morning)
            .rang(listOf(alarm), morning + 1.days, checkIn = false)
        assertEquals(2, log.size)
    }

    @Test
    fun turningOffInTheAppIsLoggedAndEndsTheWakeUp() {
        val log = emptyList<WakeUp>()
            .rang(listOf(alarm), morning, checkIn = false)
            .snoozed(listOf(1), morning + 1.minutes)
            .escaped(listOf(1), Escape.TurnedOffInApp, morning + 2.minutes)
        assertEquals(listOf(Escape.TurnedOffInApp), log.single().escapes)
        assertEquals((morning + 2.minutes).toEpochMilliseconds(), log.single().offAt)
    }

    @Test
    fun streaksCountHonestDaysAndSkipDaysWithoutAlarms() {
        var log = emptyList<WakeUp>()
        log = log.honestMorning(morning).honestMorning(morning + 1.days)
        // A weekend without alarms, then two more mornings.
        log = log.honestMorning(morning + 4.days).honestMorning(morning + 5.days)
        assertEquals(Streak(current = 4, best = 4), log.streak(utc))
    }

    @Test
    fun cheatingResetsTheStreakButKeepsTheBest() {
        var log = emptyList<WakeUp>().honestMorning(morning).honestMorning(morning + 1.days).honestMorning(morning + 2.days)
        log = log.rang(listOf(alarm), morning + 3.days, checkIn = false).escaped(listOf(1), Escape.ForceStop, morning + 3.days)
        assertEquals(Streak(current = 0, best = 3), log.streak(utc))
        log = log.turnedOff(listOf(1), morning + 3.days + 5.minutes).honestMorning(morning + 4.days)
        assertEquals(Streak(current = 1, best = 3), log.streak(utc))
    }

    @Test
    fun aMissedCheckInBreaksTheStreakButARestartDoesNot() {
        val restarted = emptyList<WakeUp>().rang(listOf(alarm), morning, checkIn = false)
            .escaped(listOf(1), Escape.PhoneOff, morning + 1.minutes)
            .turnedOff(listOf(1), morning + 2.minutes)
        assertEquals(1, restarted.streak(utc).current)
        val missed = restarted.escaped(listOf(1), Escape.MissedCheckIn, morning + 10.minutes)
        assertEquals(0, missed.streak(utc).current)
    }

    @Test
    fun aWakeUpStillGoingDoesNotCountYet() {
        val log = emptyList<WakeUp>().honestMorning(morning).rang(listOf(alarm), morning + 1.days, checkIn = false)
        assertEquals(1, log.streak(utc).current)
    }

    @Test
    fun onlyAlarmsRingingTogetherShareATurnOff() {
        val other = Alarm(id = 2, hour = 7, minute = 0)
        val log = emptyList<WakeUp>().rang(listOf(alarm, other), morning, checkIn = false).turnedOff(listOf(2), morning + 1.hours)
        assertEquals(listOf(null, (morning + 1.hours).toEpochMilliseconds()), log.map { it.offAt })
        assertTrue(log.all { it.honest })
    }
}
