# Contributing to OpenAlarm

Thanks for helping! OpenAlarm is at an early stage, so there's a lot left to shape. The [roadmap](ROADMAP.md) describes what we're building and in what order.

## Ways to help

- **Code:** pick an issue labeled `good first issue` or `help wanted`, or an unchecked item from the roadmap. Comment on the issue first, so two people don't build the same thing.
- **Testing on your phone:** how reliably alarms fire differs a lot between phone makers. Reports from real devices are very valuable, especially when an alarm *didn't* ring.
- **Ideas:** new missions, Wake Guard tricks and theme ideas are welcome as issues.
- **Themes and translations** open up in later phases (see the roadmap).

## Development setup

You need:

- JDK 17 or newer (Android Studio includes one)
- The Android SDK with API level 37 (Android Studio installs it)
- For iOS work (Phase 5): a Mac with Xcode

```bash
git clone https://github.com/Geanyl17/openalarm.git
cd openalarm
./gradlew check :androidApp:assembleDebug
```

To run the app, open the folder in Android Studio and pick a device or emulator. Every build also compiles the shared code for iOS, so iOS problems show up early. Linking the iOS app and running iOS tests need macOS, and those steps are skipped automatically elsewhere.

### Project layout

| Path | Contents |
|---|---|
| `androidApp/` | The Android app and its alarm engine: scheduling, the ringing service and the ringing screen. |
| `shared/core/` | Platform-independent alarm logic: the alarm model, when an alarm rings next, snooze and dismiss. |
| `shared/data/` | Alarm storage. |
| `shared/missions/` | The missions you complete to turn an alarm off, and their screens. |
| `shared/ui/` | Compose Multiplatform screens and the theme engine, which the iOS app will reuse. |
| `tools/` | Scripts that generate bundled assets. |
| `gradle/libs.versions.toml` | All dependency versions. |

See [Code layout](ROADMAP.md#code-layout) in the roadmap for what's planned.

### Testing alarms by hand

Unit tests can't show that an alarm really wakes a locked phone, so check changes to the alarm engine on a device or emulator:

1. Set an alarm two or three minutes ahead and lock the screen. It should wake the screen and ring over the lock screen.
2. Try Snooze and Dismiss, both on the ringing screen and from the notification.
3. Set a PIN, restart the phone and don't unlock it. The alarm must still ring; this is the Direct Boot case.

`adb logcat -s AlarmScheduler AlarmReceiver RescheduleReceiver AlarmPlayer` shows what the engine is doing.

## Making a change

1. Fork the repository and create a branch from `main`.
2. Keep each pull request focused on one feature or fix.
3. Add or update tests for logic changes, especially anything involving time, scheduling or missions.
4. Run `./gradlew check` before pushing.
5. If you change how alarms fire, say in the pull request which phone and Android version you tested on, and whether the screen was locked.
6. For UI changes, include screenshots in light and dark mode.

Code follows the [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html), and `.editorconfig` sets up your editor for them. Compose code follows the [Compose API guidelines](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md).

### Ground rules

These come from the roadmap's [principles](ROADMAP.md#principles):

- **No proprietary dependencies.** That means no Google Play Services, Firebase, ML Kit or closed-source SDKs; otherwise F-Droid can't build the app.
- **No internet.** The app ships without the internet permission, and `./gradlew check` fails if a change adds it. Features that need a network come later and are opt-in.
- **No tracking or analytics.**
- **Missions must never trap anyone.** Every mission needs a fallback for when it can't run.

### Commit messages

Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/): a type, an optional scope, and a short summary in the imperative, such as `feat(android): ring alarms over the lock screen`. Keep each commit to one feature or fix, and use the body to explain why when that isn't obvious.

| Type | Use it for |
|---|---|
| `feat` | A new feature |
| `fix` | A bug fix |
| `docs` | Documentation only |
| `test` | Adding or changing tests |
| `refactor` | A code change that doesn't change behavior |
| `build` | Gradle setup and dependencies |
| `ci` | GitHub Actions workflows |
| `chore` | Other housekeeping |

The scope names the part of the app: `core`, `data`, `ui`, `android`, and later `ios` or `missions`.

## Sign off your commits

Every commit must be signed off. This certifies that you wrote the change, or otherwise have the right to submit it, under the project's license. It's called the [Developer Certificate of Origin](https://developercertificate.org/) (DCO). Add the sign-off with `-s`:

```bash
git commit -s -m "Add Stroop mission"
```

This adds a line like `Signed-off-by: Your Name <you@example.com>` using your git name and email. CI checks every commit in a pull request. If you forgot, run `git commit --amend -s` to fix the last commit, or `git rebase --signoff main` to fix all of them, then force-push.

### License of contributions

OpenAlarm is licensed under GPL-3.0-or-later with an [additional permission for app store distribution](APP_STORE_EXCEPTION.md). By signing off, you agree that your contribution is licensed under the same terms.

### Assets: images, sounds, fonts and ML models

Bundled assets need a license that's compatible with the app: CC0, CC BY, SIL OFL, Apache-2.0 or MIT. Add each one to [CREDITS.md](CREDITS.md) with its source and license. "Non-commercial" (CC NC) and unlicensed assets can't be used.

## Reporting bugs

Please use the issue templates. If an alarm didn't ring, choose **Alarm didn't ring**; it asks for the phone details needed to track these problems down.

## Code of Conduct

Everyone taking part follows the [Code of Conduct](CODE_OF_CONDUCT.md).
