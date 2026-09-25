package io.github.geanyl17.openalarm.core

import kotlinx.datetime.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RepeatDaysTest {

    @Test
    fun containsExactlyTheChosenDays() {
        val days = RepeatDays.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)
        assertTrue(DayOfWeek.MONDAY in days)
        assertTrue(DayOfWeek.WEDNESDAY in days)
        assertFalse(DayOfWeek.TUESDAY in days)
        assertEquals(listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), days.days)
    }

    @Test
    fun toggleAddsAndRemovesADay() {
        val days = RepeatDays.Once.toggle(DayOfWeek.SUNDAY)
        assertEquals(listOf(DayOfWeek.SUNDAY), days.days)
        assertTrue(days.toggle(DayOfWeek.SUNDAY).isEmpty)
    }

    @Test
    fun presetsCoverTheExpectedDays() {
        assertEquals(DayOfWeek.entries, RepeatDays.EveryDay.days)
        assertEquals(
            listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
            RepeatDays.WorkDays.days,
        )
        assertEquals(listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), RepeatDays.Weekend.days)
        assertTrue(RepeatDays.Once.isEmpty)
    }

    @Test
    fun rejectsMasksOutsideTheWeek() {
        assertFailsWith<IllegalArgumentException> { RepeatDays(0b1000_0000) }
        assertFailsWith<IllegalArgumentException> { RepeatDays(-1) }
    }
}
