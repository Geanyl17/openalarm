package io.github.geanyl17.openalarm.core

import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

/**
 * One alarm as the user set it up. [id] is 0 until the alarm is first saved.
 *
 * This class is stored as JSON, so renaming or removing a property needs a migration.
 */
@Serializable
data class Alarm(
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val repeat: RepeatDays = RepeatDays.Once,
    val label: String = "",
    val enabled: Boolean = true,
    val vibrate: Boolean = true,
    /** Raise the volume gradually instead of starting at full volume. */
    val fadeIn: Boolean = true,
    val snoozeMinutes: Int = DEFAULT_SNOOZE_MINUTES,
    /**
     * How many times the alarm can be snoozed before it has to be turned off, or null for no limit.
     * Under a limit, each snooze is also shorter than the last and adds a mission round.
     */
    val snoozeLimit: Int? = null,
    /** Snoozes taken since the alarm was last turned off. */
    val snoozesTaken: Int = 0,
    /** Seed color (ARGB) for this alarm's screens, or null for the app's theme color. */
    val colorArgb: Int? = null,
    /** Platform sound reference (a URI on Android), or null for the default alarm sound. */
    val sound: String? = null,
    /** Platform reference to a cover photo shown when the alarm rings (a file URI on Android), or null for none. */
    val photo: String? = null,
    /** Challenges to complete, in order, before the alarm turns off. Empty means a plain dismiss button. */
    val missions: List<Mission> = emptyList(),
    /** When a snoozed alarm rings again, in epoch milliseconds; null when it isn't snoozed. */
    val snoozedUntil: Long? = null,
    /** Surprise check-ins after the alarm is turned off, to catch anyone who went back to sleep. */
    val checkIns: Boolean = false,
    /** When the pending check-ins are due, in epoch milliseconds. */
    val checkInsAt: List<Long> = emptyList(),
) {
    init {
        require(hour in 0..23) { "Hour out of range: $hour" }
        require(minute in 0..59) { "Minute out of range: $minute" }
        require(snoozeMinutes in 1..MAX_SNOOZE_MINUTES) { "Snooze length out of range: $snoozeMinutes" }
        require(snoozeLimit == null || snoozeLimit in 0..MAX_SNOOZE_LIMIT) { "Snooze limit out of range: $snoozeLimit" }
        require(snoozesTaken >= 0) { "Negative snoozes taken: $snoozesTaken" }
    }

    val time: LocalTime get() = LocalTime(hour, minute)

    val repeats: Boolean get() = !repeat.isEmpty

    companion object {
        const val DEFAULT_SNOOZE_MINUTES = 10
        const val MAX_SNOOZE_MINUTES = 60
        const val MAX_SNOOZE_LIMIT = 5
    }
}
