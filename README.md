# OpenAlarm

**A free, open-source alarm clock that makes you prove you're awake.**

To turn off an OpenAlarm alarm, you complete a mission. Today that means solving math problems or repeating a memory pattern. Next come missions that get you out of bed: photographing something in another room, scanning a barcode, tapping an NFC tag, and follow-up checks in case you went back to sleep.

- **No ads, no subscriptions, no account.**
- **No tracking and no internet.** The app doesn't have the internet permission, and everything runs on your phone.
- **Your colors.** Give each alarm any color, and its screens re-theme around it, with text that stays readable.
- **Free software**, licensed GPL-3.0-or-later.

## Install

OpenAlarm runs on Android 8.0 and newer. Download `OpenAlarm-<version>.apk` from the [latest release](https://github.com/Geanyl17/openalarm/releases/latest) and open it on your phone. To get updates automatically, add OpenAlarm to [Obtainium](https://github.com/ImranR98/Obtainium) with [this link](https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/Geanyl17/openalarm).

To check that an APK really comes from this project, compare its signing certificate, for example with [AppVerifier](https://github.com/soupslurpr/AppVerifier):

```text
io.github.geanyl17.openalarm
48:CC:DA:A5:B1:D8:E6:1F:1F:5C:26:D0:C6:7B:7E:E8:78:3E:27:22:3A:96:68:04:FD:7C:75:60:C2:B7:BC:72
```

## What it does today

- **Alarms that always ring:** at the exact minute and over the lock screen, after reboots (even before the first unlock), clock, time-zone and daylight-saving changes. If the app is killed mid-alarm, a backup alarm rings again within a minute, and an alarm whose sound can't play falls back to a bundled one.
- **No way around it:** a ringing alarm keeps its screen in front until you turn it off or snooze it, its volume can't be turned down, and it rings again after a reboot. Emergency calls and phone calls always come first.
- **Missions:** Math and Memory. The alarm turns quiet while you solve and gets loud again if you stop.
- **Setting alarms:** scroll wheels with a "Rings in 7 h 32 min" check, repeat days, labels, any alarm sound on the phone, including your own, a volume that rises gradually, and snoozes from 1 to 60 minutes.
- **A color and a cover photo for each alarm,** the color from presets or a color wheel.

[CHANGELOG.md](CHANGELOG.md) lists every change.

## Planned

- **More missions:** Color Hunt, Barcode, Photo Match, Object Hunt, NFC Tag, Steps, Lights On, Stroop, and the Alertness Gate reaction test. You'll be able to chain several together.
- **Wake Guard:** a backup alarm that only a finished mission can cancel, surprise follow-up checks, snoozes that cost you more each time, and more.
- **Themes:** an app-wide color, light, dark or AMOLED black, and a red night mode.
- **iPhone,** and Google Play.

The [roadmap](ROADMAP.md) has the details.

## Building

You need JDK 17 or newer and the Android SDK. [Android Studio](https://developer.android.com/studio) includes both.

```bash
./gradlew :androidApp:installDebug   # build and install on a connected phone or emulator
./gradlew check                      # run tests and lint
```

Each CI run on GitHub also builds a debug APK, which you can download from the run's page under **Actions**.

## Contributing

Contributions are welcome: code, testing on your phone, mission ideas, themes and translations. Start with [CONTRIBUTING.md](CONTRIBUTING.md). Everyone taking part follows the [Code of Conduct](CODE_OF_CONDUCT.md).

## License

OpenAlarm is free software: you can redistribute it and/or modify it under the terms of the [GNU General Public License](LICENSE), version 3 or (at your option) any later version, with an [additional permission](APP_STORE_EXCEPTION.md) for app store distribution. Third-party assets are listed in [CREDITS.md](CREDITS.md).
