package io.github.geanyl17.openalarm.core

/** Whether the alarm can still be snoozed while it rings. */
val Alarm.canSnooze: Boolean get() = snoozeLimit == null || snoozesTaken < snoozeLimit

/** Snoozes left before the alarm has to be turned off, or null if there's no limit. */
val Alarm.snoozesLeft: Int? get() = snoozeLimit?.let { (it - snoozesTaken).coerceAtLeast(0) }

/** How long the next snooze lasts. Under a limit, each is half as long as the one before, down to a minute. */
val Alarm.nextSnoozeMinutes: Int
    get() = if (snoozeLimit == null) snoozeMinutes else (snoozeMinutes shr snoozesTaken.coerceAtMost(30)).coerceAtLeast(1)

/** The missions to do now: under a snooze limit, every snooze taken adds a round to the first one. */
val Alarm.missionsDue: List<Mission>
    get() = if (snoozeLimit == null || snoozesTaken == 0) {
        missions
    } else {
        missions.mapIndexed { index, mission ->
            if (index == 0) mission.copy(rounds = (mission.rounds + snoozesTaken).coerceAtMost(Mission.MAX_ROUNDS)) else mission
        }
    }
