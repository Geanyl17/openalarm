# OpenAlarm

**A free, open-source alarm clock that makes you prove you're awake.**

To turn off an OpenAlarm alarm, you complete a mission: photograph something in another room, scan a barcode, tap an NFC tag or pass a reaction test. A few minutes later it checks in again, in case you went back to bed.

- **No ads, no subscriptions, no account.**
- **No tracking and no internet.** The app doesn't have the internet permission, and everything runs on your phone.
- **Your colors.** Pick any color and the whole app re-themes around it, with text that stays readable.
- **Free software**, licensed GPL-3.0-or-later.

> [!NOTE]
> **Early development.** There's nothing to install yet. The [roadmap](ROADMAP.md) shows what's planned and what's done.

## Planned features

- **Missions:** Color Hunt, Barcode, Photo Match, Object Hunt, NFC Tag, Steps, Lights On, Math, Memory, Stroop, and the Alertness Gate reaction test. You can chain several together.
- **Wake Guard:** a backup alarm that only a finished mission can cancel, surprise follow-up checks, snoozes that cost you more each time, and more.
- **Reliable ringing** that survives reboots, time-zone and daylight-saving changes, and phones that restart overnight.
- **Themes:** any color, light, dark or AMOLED black, a red night mode, and a different color for each alarm.

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
