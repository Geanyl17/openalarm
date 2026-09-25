# Changelog

All notable changes to OpenAlarm. Versions follow [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- A snooze limit for each alarm. Under a limit, each snooze is half as long as the one before (10, then 5, then 2 minutes) and adds a round to the mission. Once the snoozes are used up, only turning the alarm off stops it.
- Check-ins: after you turn an alarm off, it checks once or twice, 5 to 15 minutes later, that you're still up. Tap "I'm up", which appears somewhere different each time, within 60 seconds, or the alarm rings again with a harder mission.
- A Reaction mission: tap as soon as the screen says so, within 0.9, 0.65 or 0.5 seconds depending on the difficulty. A miss costs a round, and after 3 misses you can switch to math.
- A Colors mission: a color word is written in another color, and you tap the color, not the word. Its colors can be told apart with color blindness too. A wrong answer costs a round, and after 3 you can switch to math.
- Mission chains: up to 3 missions on one alarm, done one after another.
- A Lights On mission: keep the room bright for 10 seconds a round, brighter on harder difficulties. On a phone without a light sensor it's math instead, and after a minute you can switch to math if your room's lights aren't bright enough.
- A Steps mission: walk 10, 20 or 30 steps a round, depending on the difficulty. It uses the phone's step sensor if you allow physical activity access, and the accelerometer otherwise.
- Check-ins are answered for you if the step counter shows you've walked 100 steps since turning the alarm off.
- A Theme screen, behind the palette button on the alarm list: pick the app's color, or take it from the wallpaper (Android 12 and later), and a mode: System, Light, Dark, Black (pure black backgrounds for OLED screens) or Night red (red on black, with no blue light, for setting alarms in bed). Alarms with their own color keep it, in the chosen mode.
- Wake-ups, behind the history button on the alarm list: every wake-up with how long it took to get up, its snoozes and check-ins, and your streak of mornings in a row. The Honesty Log part of it shows each way around an alarm: force-stopping the app, turning a snoozed alarm off in the app (which skips its mission), and missed check-ins break the streak. A restart or the app being stopped mid-alarm is shown too, but doesn't count against you, since a flat battery or the system can cause it.

### Changed

- Selected options in the alarm editor show a check mark, so they don't rely on color alone.

## [0.2.0] - 2026-09-25

### Added

- A ringing alarm can't be pushed aside. Leave its screen with Home, the app switcher or another app, and it comes straight back until you turn the alarm off or snooze it. This needs the "Display over other apps" permission, which the alarm list asks for.
- The volume buttons can't turn a ringing alarm down, and its notification comes back if it's swiped away.
- An alarm that's still ringing rings again after a reboot or an app update, and within a minute if the app is stopped from the Active apps list. After a force stop, it rings again as soon as the app is opened.
- An emergency call button on the ringing screen. During an emergency call or a phone call, the alarm is silent and stays out of the way.
- Your own alarm sounds: add them with + in the phone's sound picker. OpenAlarm keeps its own copy of each one it uses, so it plays even before the first unlock after a reboot, and after the original is deleted.
- A cover photo for each alarm, shown on the ringing screen and in the alarm list.

### Changed

- Less text: the setup cards, the alarm editor and the missions drop their explanations and keep just the labels.

### Fixed

- The alarm's notification banner no longer covers the top of the ringing screen.
- The ringing screen's clock no longer wraps onto two lines at 10, 11 and 12 o'clock.

## [0.1.0] - 2026-09-25

The first release, for Android 8.0 and newer.

### Alarms that always ring

- Alarms ring at the exact minute, full screen over the lock screen.
- They're set again after a reboot, a clock or time-zone change and an app update, and daylight-saving changes are handled.
- They ring even before the phone has been unlocked for the first time after a reboot.
- If the app crashes or is killed while an alarm rings, a backup alarm rings it again within a minute.
- An alarm is never silent: if its sound can't be played, a bundled sound takes over.
- The alarm list warns about missing permissions that would stop alarms from ringing, and links to the setting that fixes each one.

### Missions

- Math: solve arithmetic problems, from sums of small numbers to problems like 17 × 6 + 38.
- Memory: repeat a pattern of lit tiles. Patterns get shorter after repeated misses, and after three misses you can switch to math.
- While you work on a mission, the alarm turns quiet and stops vibrating. Stop for 20 seconds and it gets loud again.
- An alarm with a mission can't be dismissed from its notification.

### Setting alarms

- Scroll wheels for the time, and "Rings in 7 h 32 min" underneath, so a wrong hour or AM/PM stands out.
- Repeat days, a label, vibration, and a volume that rises over 30 seconds.
- Any alarm sound on the phone, picked with Android's sound picker.
- Snooze for 5, 10, 15 or 20 minutes, or any length from 1 to 60.
- A color for each alarm, from presets or a color wheel. The editor and the ringing screen take on the color, and text stays readable in any color.

### Privacy

- No internet permission, no ads, no tracking and no account.

[Unreleased]: https://github.com/Geanyl17/openalarm/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/Geanyl17/openalarm/releases/tag/v0.2.0
[0.1.0]: https://github.com/Geanyl17/openalarm/releases/tag/v0.1.0
