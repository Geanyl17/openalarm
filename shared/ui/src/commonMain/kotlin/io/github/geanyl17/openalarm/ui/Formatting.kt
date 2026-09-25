package io.github.geanyl17.openalarm.ui

import androidx.compose.runtime.Composable
import io.github.geanyl17.openalarm.core.RepeatDays
import io.github.geanyl17.openalarm.ui.resources.Res
import io.github.geanyl17.openalarm.ui.resources.date_short
import io.github.geanyl17.openalarm.ui.resources.day_friday
import io.github.geanyl17.openalarm.ui.resources.day_letter_friday
import io.github.geanyl17.openalarm.ui.resources.day_letter_monday
import io.github.geanyl17.openalarm.ui.resources.day_letter_saturday
import io.github.geanyl17.openalarm.ui.resources.day_letter_sunday
import io.github.geanyl17.openalarm.ui.resources.day_letter_thursday
import io.github.geanyl17.openalarm.ui.resources.day_letter_tuesday
import io.github.geanyl17.openalarm.ui.resources.day_letter_wednesday
import io.github.geanyl17.openalarm.ui.resources.day_monday
import io.github.geanyl17.openalarm.ui.resources.day_saturday
import io.github.geanyl17.openalarm.ui.resources.day_short_friday
import io.github.geanyl17.openalarm.ui.resources.day_short_monday
import io.github.geanyl17.openalarm.ui.resources.day_short_saturday
import io.github.geanyl17.openalarm.ui.resources.day_short_sunday
import io.github.geanyl17.openalarm.ui.resources.day_short_thursday
import io.github.geanyl17.openalarm.ui.resources.day_short_tuesday
import io.github.geanyl17.openalarm.ui.resources.day_short_wednesday
import io.github.geanyl17.openalarm.ui.resources.day_sunday
import io.github.geanyl17.openalarm.ui.resources.day_thursday
import io.github.geanyl17.openalarm.ui.resources.day_tuesday
import io.github.geanyl17.openalarm.ui.resources.day_wednesday
import io.github.geanyl17.openalarm.ui.resources.duration_days_hours
import io.github.geanyl17.openalarm.ui.resources.duration_hours_minutes
import io.github.geanyl17.openalarm.ui.resources.duration_minutes
import io.github.geanyl17.openalarm.ui.resources.duration_under_a_minute
import io.github.geanyl17.openalarm.ui.resources.month_1
import io.github.geanyl17.openalarm.ui.resources.month_10
import io.github.geanyl17.openalarm.ui.resources.month_11
import io.github.geanyl17.openalarm.ui.resources.month_12
import io.github.geanyl17.openalarm.ui.resources.month_2
import io.github.geanyl17.openalarm.ui.resources.month_3
import io.github.geanyl17.openalarm.ui.resources.month_4
import io.github.geanyl17.openalarm.ui.resources.month_5
import io.github.geanyl17.openalarm.ui.resources.month_6
import io.github.geanyl17.openalarm.ui.resources.month_7
import io.github.geanyl17.openalarm.ui.resources.month_8
import io.github.geanyl17.openalarm.ui.resources.month_9
import io.github.geanyl17.openalarm.ui.resources.repeat_every_day
import io.github.geanyl17.openalarm.ui.resources.repeat_once
import io.github.geanyl17.openalarm.ui.resources.repeat_weekdays
import io.github.geanyl17.openalarm.ui.resources.repeat_weekends
import io.github.geanyl17.openalarm.ui.resources.time_am
import io.github.geanyl17.openalarm.ui.resources.time_pm
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration

/** "07:30" on a 24-hour clock, "7:30 AM" on a 12-hour clock. */
@Composable
fun formatTime(hour: Int, minute: Int, use24Hour: Boolean): String {
    val minutes = minute.toString().padStart(2, '0')
    if (use24Hour) return "${hour.toString().padStart(2, '0')}:$minutes"
    val hour12 = if (hour % 12 == 0) 12 else hour % 12
    return stringResource(if (hour < 12) Res.string.time_am else Res.string.time_pm, "$hour12:$minutes")
}

/** "7 h 32 min". Rounded down, so a screen that updates every few seconds never overstates the time left. */
@Composable
internal fun formatDuration(duration: Duration): String {
    val (text, args) = durationParts(duration)
    return stringResource(text, *args)
}

internal suspend fun durationText(duration: Duration): String {
    val (text, args) = durationParts(duration)
    return getString(text, *args)
}

private fun durationParts(duration: Duration): Pair<StringResource, Array<Any>> {
    val seconds = duration.inWholeSeconds
    if (seconds < 60) return Res.string.duration_under_a_minute to emptyArray()
    val totalMinutes = seconds / 60
    val days = totalMinutes / (24 * 60)
    val hours = totalMinutes / 60 % 24
    val minutes = totalMinutes % 60
    return when {
        days > 0 -> Res.string.duration_days_hours to arrayOf(days, hours)
        hours > 0 -> Res.string.duration_hours_minutes to arrayOf(hours, minutes)
        else -> Res.string.duration_minutes to arrayOf(minutes)
    }
}

@Composable
internal fun repeatSummary(repeat: RepeatDays): String = when (repeat) {
    RepeatDays.Once -> stringResource(Res.string.repeat_once)
    RepeatDays.EveryDay -> stringResource(Res.string.repeat_every_day)
    RepeatDays.WorkDays -> stringResource(Res.string.repeat_weekdays)
    RepeatDays.Weekend -> stringResource(Res.string.repeat_weekends)
    else -> repeat.days.map { stringResource(it.shortName) }.joinToString(", ")
}

/** "Fri, Sep 25". */
@Composable
internal fun formatDate(date: LocalDate): String =
    stringResource(Res.string.date_short, stringResource(date.dayOfWeek.shortName), stringResource(Months[date.month.ordinal]), date.day)

private val Months = listOf(
    Res.string.month_1, Res.string.month_2, Res.string.month_3, Res.string.month_4, Res.string.month_5, Res.string.month_6,
    Res.string.month_7, Res.string.month_8, Res.string.month_9, Res.string.month_10, Res.string.month_11, Res.string.month_12,
)

internal val DayOfWeek.letter: StringResource
    get() = when (this) {
        DayOfWeek.MONDAY -> Res.string.day_letter_monday
        DayOfWeek.TUESDAY -> Res.string.day_letter_tuesday
        DayOfWeek.WEDNESDAY -> Res.string.day_letter_wednesday
        DayOfWeek.THURSDAY -> Res.string.day_letter_thursday
        DayOfWeek.FRIDAY -> Res.string.day_letter_friday
        DayOfWeek.SATURDAY -> Res.string.day_letter_saturday
        DayOfWeek.SUNDAY -> Res.string.day_letter_sunday
    }

internal val DayOfWeek.shortName: StringResource
    get() = when (this) {
        DayOfWeek.MONDAY -> Res.string.day_short_monday
        DayOfWeek.TUESDAY -> Res.string.day_short_tuesday
        DayOfWeek.WEDNESDAY -> Res.string.day_short_wednesday
        DayOfWeek.THURSDAY -> Res.string.day_short_thursday
        DayOfWeek.FRIDAY -> Res.string.day_short_friday
        DayOfWeek.SATURDAY -> Res.string.day_short_saturday
        DayOfWeek.SUNDAY -> Res.string.day_short_sunday
    }

internal val DayOfWeek.fullName: StringResource
    get() = when (this) {
        DayOfWeek.MONDAY -> Res.string.day_monday
        DayOfWeek.TUESDAY -> Res.string.day_tuesday
        DayOfWeek.WEDNESDAY -> Res.string.day_wednesday
        DayOfWeek.THURSDAY -> Res.string.day_thursday
        DayOfWeek.FRIDAY -> Res.string.day_friday
        DayOfWeek.SATURDAY -> Res.string.day_saturday
        DayOfWeek.SUNDAY -> Res.string.day_sunday
    }
