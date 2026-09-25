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
    /** Seed color (ARGB) for this alarm's screens, or null for the app's theme color. */
    val colorArgb: Int? = null,
    /** Platform sound reference (a content URI on Android), or null for the default alarm sound. */
    val sound: String? = null,
    /** Challenges to complete, in order, before the alarm turns off. Empty means a plain dismiss button. */
    val missions: List<Mission> = emptyList(),
    /** When a snoozed alarm rings again, in epoch milliseconds; null when it isn't snoozed. */
    val snoozedUntil: Long? = null,
) {
    init {
        require(hour in 0..23) { "Hour out of range: $hour" }
        require(minute in 0..59) { "Minute out of range: $minute" }
        require(snoozeMinutes in 1..MAX_SNOOZE_MINUTES) { "Snooze length out of range: $snoozeMinutes" }
    }

    val time: LocalTime get() = LocalTime(hour, minute)

    val repeats: Boolean get() = !repeat.isEmpty

    companion object {
        const val DEFAULT_SNOOZE_MINUTES = 9
        const val MAX_SNOOZE_MINUTES = 60
    }
}
