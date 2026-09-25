package io.github.geanyl17.openalarm.core

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/** The days of the week an alarm repeats on, as a 7-bit mask (bit 0 = Monday). Empty means it rings once. */
@Serializable
@JvmInline
value class RepeatDays(val mask: Int) {
    init {
        require(mask in 0..ALL_DAYS) { "Invalid repeat mask: $mask" }
    }

    val isEmpty: Boolean get() = mask == 0

    /** The selected days, Monday first. */
    val days: List<DayOfWeek> get() = DayOfWeek.entries.filter { it in this }

    operator fun contains(day: DayOfWeek): Boolean = mask and day.bit != 0

    fun toggle(day: DayOfWeek): RepeatDays = RepeatDays(mask xor day.bit)

    companion object {
        private const val ALL_DAYS = 0b111_1111

        val Once = RepeatDays(0)
        val EveryDay = RepeatDays(ALL_DAYS)
        val WorkDays = of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        val Weekend = of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

        fun of(vararg days: DayOfWeek): RepeatDays = RepeatDays(days.fold(0) { mask, day -> mask or day.bit })
    }
}

private val DayOfWeek.bit: Int get() = 1 shl (isoDayNumber - 1)
