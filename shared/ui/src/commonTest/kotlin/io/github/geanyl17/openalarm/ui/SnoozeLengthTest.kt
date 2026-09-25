package io.github.geanyl17.openalarm.ui

import io.github.geanyl17.openalarm.core.Alarm
import kotlin.test.Test
import kotlin.test.assertTrue

class SnoozeLengthTest {

    @Test
    fun newAlarmsStartOnAPreset() {
        // Otherwise every new alarm would open with an odd "custom" length selected.
        assertTrue(Alarm.DEFAULT_SNOOZE_MINUTES in SnoozePresets)
    }

    @Test
    fun presetsAreValidSnoozeLengths() {
        assertTrue(SnoozePresets.all { it in 1..Alarm.MAX_SNOOZE_MINUTES })
    }
}
