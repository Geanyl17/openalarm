# OpenAlarm roadmap

OpenAlarm is a free, open-source alarm clock that makes you prove you're awake. To turn an alarm off you complete a **mission**, such as photographing something in another room, scanning a barcode or passing a reaction test. Afterwards, **Wake Guard** keeps checking that you didn't go back to sleep.

This document covers what we're building, the decisions behind it, and the order we're building it in. Want to help? See [CONTRIBUTING.md](CONTRIBUTING.md).

**Why another alarm app?** The popular challenge alarms are closed-source and funded by ads or subscriptions. The open-source clocks, such as AOSP DeskClock and Fossify Clock, are reliable but have no challenges. OpenAlarm aims to be both.

## Principles

1. **It must ring.** A missed alarm is the one unforgivable bug. Reliability comes before every feature.
2. **The opponent is you at 6 AM: sleepy, not clever.** A defense only has to take more effort than getting up, so OpenAlarm never needs spyware-level permissions.
3. **Never trap anyone.** Every mission has a fallback for a broken camera, a revoked permission or a lost NFC tag. Emergency calls always work, and color-based missions have color-blind-safe versions.
4. **Private by design.** Everything runs on the phone. The app doesn't have the internet permission, and camera frames are analyzed in memory and never saved.
5. **Easy to contribute to.** Missions and themes are plug-ins, so adding one takes a single pull request.

## Decisions

| Topic | Decision | Why |
|---|---|---|
| Platforms | Android first, iOS later | Android is where missions can actually be enforced, and where F-Droid is. |
| Stack | Kotlin Multiplatform + Compose Multiplatform | The alarm engine must be native code on each OS anyway. Kotlin is native on Android, and iOS can reuse the shared logic and UI. |
| License | GPL-3.0-or-later with an [app store permission](APP_STORE_EXCEPTION.md) | Copies and forks must stay open source, and the app can still ship on the App Store and Google Play. |
| Dependencies | Open source only | No Google Play Services, ML Kit or Firebase, so F-Droid can build the app from source. No AGPL-licensed ML models (such as Ultralytics YOLO). |

**iOS limits:** since iOS 26, AlarmKit lets third-party apps ring real alarms, but the system alarm screen always lets the user stop the alarm. On iPhone, the Dead Man's Switch (see [Wake Guard](#wake-guard)) is therefore the main enforcement: stopping without finishing the mission means a backup alarm rings a few minutes later.

## Reliability requirements (Android)

- Alarms are scheduled with `AlarmManager.setAlarmClock()`, which fires even in Doze. The app declares `USE_EXACT_ALARM`, which Android grants automatically and Google Play allows for alarm clock apps.
- A full-screen-intent notification shows the ringing screen over the lock screen. Users can revoke this on Android 14+, so the app checks for it and asks.
- The sound plays from a foreground service on the alarm audio stream.
- Alarms are re-armed after a reboot, a time or time-zone change, and an app update.
- **Direct Boot:** if the phone restarts overnight and hasn't been unlocked yet, alarms still fire, because alarm data lives in device-protected storage.
- **Daylight saving:** 2:30 AM doesn't exist on the spring-forward day, and 1:30 AM happens twice in the fall. The code that works out the next ring time is tested for both.
- If a custom sound is missing, the alarm falls back to a bundled sound. It never rings silently.
- **Reliability self-test** during setup: it checks every permission, links to the phone maker's battery settings ([dontkillmyapp.com](https://dontkillmyapp.com)), and runs a one-minute test alarm.
- **Alarm log** of when each alarm was scheduled, fired and dismissed, with a "copy diagnostics" button for bug reports.

## Missions

To dismiss an alarm, you complete one or more missions, which can be chained (for example: tap the NFC tag in the bathroom, then photograph your toothbrush). While you're solving, the volume drops. If you stop interacting for about 20 seconds, it returns to full volume.

### Camera missions

| Mission | How it works | Tech | Phase |
|---|---|---|---|
| **Color Hunt** | "Find something *teal*": aim at an object of a randomly chosen color. | Color analysis of live camera frames, no ML | 1 |
| **Barcode** | Scan a barcode you registered earlier (toothpaste, a bag of coffee). | zxing-cpp | 1 |
| **Lights On** | The room must stay bright for 20 seconds. Bright light helps shake off grogginess. | Light sensor or camera exposure | 2 |
| **Photo Match** | Photograph a spot during setup (bathroom sink, coffee maker), then photograph it again to dismiss. | On-device image similarity against your reference photos | 3 |
| **Object Hunt** | "Photograph a *toothbrush*": a random pick from objects you said are outside your bedroom. | Apache-2.0 object detector (EfficientDet-Lite) on LiteRT | 3 |

Camera missions only accept the live camera, never gallery images. The target must stay in frame for about a second, and frames are never saved.

### Movement and brain missions

| Mission | How it works | Phase |
|---|---|---|
| **Math** | Adjustable difficulty. | 1 |
| **Memory** | Repeat a flashing sequence. | 1 |
| **Steps** | Walk a set number of steps. | 2 |
| **NFC Tag** | Tap an NFC sticker placed away from your bed. Instant once you're up, impossible from bed. | 2 |
| **Stroop** | The word "RED" is printed in blue, and you tap *blue*. Hard to do half asleep. | 2 |
| **Alertness Gate** | A reaction test of about 45 seconds, based on the Psychomotor Vigilance Task sleep researchers use to measure drowsiness. You pass when you're close to your own daytime speed. After 3 failed tries it swaps in another mission. | 2 |
| **Hum It** | The app plays a note, and you hum it back for 2 seconds. | 4 |

## Wake Guard

Waking up is the easy part; the real problem is dozing off two minutes after dismissing the alarm. Wake Guard combines these measures:

1. **Dead Man's Switch.** Every ringing alarm arms a hidden backup alarm 3 minutes out. While you work on the mission, the app keeps pushing the backup back. If the app stops doing that (it was killed, the phone was switched off, or you pressed Stop on iOS), the backup rings. Only a finished mission cancels it.
2. **Aftershocks.** One or two surprise check-ins at random times 5 to 15 minutes after dismissal. You get 60 seconds to tap "I'm up", which appears at a random spot on the screen. Miss it and the full alarm returns with a harder mission. On Android they're skipped if the step counter shows you're walking around.
3. **Snooze Tax.** Snoozes are limited (for example to 2), each one is shorter than the last (9, then 5, then 2 minutes), and each adds a mission step.
4. **Wake Map.** During setup you register spots and objects in other rooms. Each morning one is picked at random, so you can't keep the answer next to your bed.
5. **Reboot-proof, plus an Honesty Log.** Switch the phone off mid-alarm and it rings again after booting. Force-stopping the app in Settings is the one way out Android can't block. It takes enough fiddling that you'll be awake anyway, and the app notices it the next time it opens, logs it and resets your wake-up streak.
6. **Volume floor (Android).** The volume buttons can't turn a ringing alarm below the level you set.
7. **Later, opt-in:** a sunrise ramp that brightens the screen or smart lights (through Home Assistant) before the alarm, and a buddy ping that notifies a friend through self-hostable [ntfy](https://ntfy.sh) if the mission isn't done 15 minutes in. These would be the first features to use the internet, so they stay optional.

## Colors and themes

- **One color builds the whole theme.** Pick any color and the app generates full light and dark Material 3 palettes in which all text stays readable. Tests check this across a range of seed colors.
- **Modes:** System, Light, Dark, AMOLED black, and Night Red (red-on-black for setting alarms in bed: no blue light, and it doesn't ruin your night vision).
- **Material You:** optionally match your wallpaper colors (Android 12+).
- **A color for each alarm,** used on its card, its ringing screen and its notification. On iOS it also tints the system alarm, which AlarmKit supports.
- **Ringing screen styles:** a solid color, a gradient, or an animated sunrise made from the alarm's color.
- **Shareable themes:** export and import themes as a short code. A `themes/` folder in this repo accepts community themes, and CI checks their contrast.
- **Contrast guard:** warns about hand-picked colors that are hard to read, and offers a fix.

## Distribution

1. GitHub Releases and [IzzyOnDroid](https://apt.izzysoft.de/fdroid/), a third-party F-Droid repository that's quick to get into.
2. [F-Droid](https://f-droid.org), which builds the app from source.
3. Google Play.
4. Apple App Store (Phase 5).

Every channel gets the same fully open-source build. There's no separate Google Play version.

## Code layout

```
openalarm/
├── androidApp/     # Android app and alarm engine: scheduling, receivers, ringing service and screen
├── shared/
│   ├── core/       # alarm model, next-ring-time math, snooze and dismiss rules (later: Wake Guard state machine)
│   ├── data/       # alarm storage (a versioned JSON file)
│   ├── ui/         # Compose Multiplatform screens and the theme engine
│   └── missions/   # the missions: math and memory so far, camera missions next
├── tools/          # scripts that generate bundled assets, such as the fallback alarm sound
├── iosApp/         # (Phase 5) AlarmKit alarm engine in Swift, Live Activity
├── models/         # (Phase 3) on-device ML models, their licenses and conversion scripts
└── themes/         # (Phase 4) community themes as JSON
```

Each mission declares what it needs (camera, NFC, step counter, microphone) and whether it can run on the current phone. That lets the ringing screen chain missions and swap in a fallback when one can't run.

## Phases

### Phase 0: a contributor-ready repo

- [x] License (GPL-3.0-or-later with app store permission), README, contributing guide, Code of Conduct
- [x] Issue templates, including "Alarm didn't ring", and a pull request template
- [x] Kotlin Multiplatform project: Android app plus a shared UI module (with iOS targets declared)
- [x] Seed-color theme engine, with tests that text stays readable
- [x] CI: build, unit tests, Android lint, iOS compile check, debug APK download, commit sign-off check
- [ ] Private contact address for Code of Conduct reports

### Phase 1: "It always rings" (first Android release)

- [x] Alarms: time, repeat days, label, fade-in, vibration, snooze length
- [ ] Choosing the alarm sound (for now it's the phone's default alarm sound)
- [x] Ringing screen over the lock screen, with snooze and dismiss
- [x] Re-arming after a reboot, a clock or time-zone change, and an app update
- [x] Direct Boot: alarms ring even before the first unlock after a reboot
- [x] Daylight-saving gaps and overlaps, covered by tests
- [x] Never silent: falls back to a bundled sound, and a backup alarm re-rings within a minute if the app dies while ringing
- [x] Setup checks for notifications, full-screen alarms and exact alarms
- [ ] Reliability self-test and alarm log
- [x] Missions: Math and Memory, with the alarm quiet while you solve and loud again if you stop
- [ ] Missions: Color Hunt and Barcode (camera)
- [x] A color for each alarm
- [ ] App color picker, AMOLED black
- [ ] Release on GitHub and IzzyOnDroid

### Phase 2: "You can't fall back asleep"

- [ ] Wake Guard 1–6
- [ ] Mission chains and wake-up streaks
- [ ] Missions: Steps, NFC Tag, Lights On, Stroop, Alertness Gate
- [ ] Submit to F-Droid

### Phase 3: smarter camera missions

- [ ] Photo Match, Object Hunt, Wake Map
- [ ] Tooling to prepare, license-check and ship the ML models

### Phase 4: polish

- [ ] Night Red, Material You, ringing screen styles
- [ ] Theme sharing and the community theme gallery
- [ ] Hum It mission, home screen widgets
- [ ] Translations through Hosted Weblate
- [ ] Release on Google Play

### Phase 5: iPhone

- [ ] AlarmKit alarm engine, lock-screen alarm tinted with each alarm's color
- [ ] Missions in the app, with the Dead Man's Switch as the main enforcement
- [ ] Release on the App Store

### Later

- [ ] Sunrise ramp, Home Assistant and webhooks, buddy ping through ntfy
- [ ] Wear OS app
- [ ] Sleep stats (on the phone only)
