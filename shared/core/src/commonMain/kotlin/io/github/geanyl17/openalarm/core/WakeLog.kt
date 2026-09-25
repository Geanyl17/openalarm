package io.github.geanyl17.openalarm.core

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * One wake-up: an alarm from its first ring until it was turned off, with its snoozes, check-ins and any
 * way around it that was used. Stored as JSON, so renaming or removing a property needs a migration.
 */
@Serializable
data class WakeUp(
    val alarmId: Long,
    val label: String = "",
    /** When the alarm first rang, in epoch milliseconds. */
    val rangAt: Long,
    /** When it was turned off, or null while it's still ringing or snoozed. */
    val offAt: Long? = null,
    val snoozes: Int = 0,
    /** Check-ins answered in time. */
    val checkIns: Int = 0,
    /** The Honesty Log: every way around the alarm used during this wake-up. */
    val escapes: List<Escape> = emptyList(),
) {
    /** Counts toward the wake-up streak: nothing that breaks it happened. */
    val honest: Boolean get() = escapes.none { it.breaksStreak }
}

/** A way around a ringing alarm, recorded in the Honesty Log. */
@Serializable
enum class Escape {
    /** The app was force-stopped in the system settings while the alarm rang. */
    @SerialName("force_stop")
    ForceStop,

    /** The alarm was switched off, edited or deleted in the app before its mission was done. */
    @SerialName("turned_off_in_app")
    TurnedOffInApp,

    /** A check-in went unanswered. */
    @SerialName("missed_check_in")
    MissedCheckIn,

    /** The phone restarted while the alarm rang. That can also be a flat battery, so it doesn't break the streak. */
    @SerialName("phone_off")
    PhoneOff,

    /** The app was stopped while the alarm rang, for example from the Active apps list. The system can do that too. */
    @SerialName("app_stopped")
    AppStopped,

    ;

    val breaksStreak: Boolean get() = this == ForceStop || this == TurnedOffInApp || this == MissedCheckIn
}

/** Consecutive days with honest wake-ups, now and at best. Days without an alarm don't break it. */
data class Streak(val current: Int, val best: Int)

/** Where the wake-up log is stored. */
interface WakeLogRepository {
    /** Oldest first. */
    val wakeUps: Flow<List<WakeUp>>

    suspend fun update(transform: (List<WakeUp>) -> List<WakeUp>)
}

/** Records wake-ups as alarms ring, get snoozed and are turned off. */
class WakeLog(private val repository: WakeLogRepository, private val clock: Clock = Clock.System) {
    val wakeUps: Flow<List<WakeUp>> get() = repository.wakeUps

    suspend fun rang(alarms: List<Alarm>, checkIn: Boolean) = update { it.rang(alarms, clock.now(), checkIn) }

    suspend fun snoozed(ids: Collection<Long>) = update { it.snoozed(ids, clock.now()) }

    suspend fun turnedOff(ids: Collection<Long>) = update { it.turnedOff(ids, clock.now()) }

    suspend fun checkedIn(ids: Collection<Long>) = update { it.checkedIn(ids, clock.now()) }

    suspend fun escaped(ids: Collection<Long>, escape: Escape) = update { it.escaped(ids, escape, clock.now()) }

    private suspend fun update(transform: (List<WakeUp>) -> List<WakeUp>) =
        repository.update { transform(it).takeLast(MAX_WAKE_UPS) }

    companion object {
        /** About a year of mornings. */
        const val MAX_WAKE_UPS = 500
    }
}

/** A ring this long after the first one still belongs to the same wake-up (snoozes, check-ins). */
private val SAME_WAKE_UP = 12.hours

/** The index of [id]'s latest wake-up, if it began recently enough to still be going on at [now]. */
private fun List<WakeUp>.latest(id: Long, now: Instant): Int? {
    val index = indexOfLast { it.alarmId == id }
    return index.takeIf { it >= 0 && now - Instant.fromEpochMilliseconds(this[it].rangAt) < SAME_WAKE_UP }
}

private fun List<WakeUp>.updateLatest(ids: Collection<Long>, now: Instant, transform: (WakeUp) -> WakeUp): List<WakeUp> {
    val indices = ids.mapNotNull { latest(it, now) }.toSet()
    return mapIndexed { index, wakeUp -> if (index in indices) transform(wakeUp) else wakeUp }
}

/**
 * A ring continues its alarm's wake-up when that's still going (a snooze ending, or ringing again after
 * an interruption) or when it's a check-in. Otherwise it starts a new wake-up.
 */
internal fun List<WakeUp>.rang(alarms: List<Alarm>, at: Instant, checkIn: Boolean): List<WakeUp> {
    val continuing = alarms.filter { alarm -> latest(alarm.id, at)?.let { this[it].offAt == null || checkIn } == true }
    val new = (alarms - continuing.toSet()).map { WakeUp(alarmId = it.id, label = it.label, rangAt = at.toEpochMilliseconds()) }
    return this + new
}

internal fun List<WakeUp>.snoozed(ids: Collection<Long>, now: Instant) =
    updateLatest(ids, now) { if (it.offAt == null) it.copy(snoozes = it.snoozes + 1) else it }

/** Turning off again, after a missed check-in brought the alarm back, keeps the first time. */
internal fun List<WakeUp>.turnedOff(ids: Collection<Long>, now: Instant) =
    updateLatest(ids, now) { it.copy(offAt = it.offAt ?: now.toEpochMilliseconds()) }

internal fun List<WakeUp>.checkedIn(ids: Collection<Long>, now: Instant) =
    updateLatest(ids, now) { it.copy(checkIns = it.checkIns + 1) }

/** Turning an alarm off in the app also ends its wake-up. */
internal fun List<WakeUp>.escaped(ids: Collection<Long>, escape: Escape, now: Instant) =
    updateLatest(ids, now) {
        it.copy(
            escapes = it.escapes + escape,
            offAt = if (escape == Escape.TurnedOffInApp) it.offAt ?: now.toEpochMilliseconds() else it.offAt,
        )
    }

/**
 * Counts days on which every wake-up was honest. A day counts once each of its wake-ups was turned off,
 * or as soon as one breaks the streak.
 */
fun List<WakeUp>.streak(timeZone: TimeZone): Streak {
    val days: Map<LocalDate, List<WakeUp>> = filter { it.offAt != null || !it.honest }
        .groupBy { Instant.fromEpochMilliseconds(it.rangAt).toLocalDateTime(timeZone).date }
    var current = 0
    var best = 0
    for (day in days.keys.sorted()) {
        current = if (days.getValue(day).all { it.honest }) current + 1 else 0
        best = maxOf(best, current)
    }
    return Streak(current, best)
}
