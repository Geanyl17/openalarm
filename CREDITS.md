# Credits

Third-party assets bundled with OpenAlarm (images, sounds, fonts, ML models), with their sources and licenses. Code libraries are listed in [gradle/libs.versions.toml](gradle/libs.versions.toml).

| Asset | Used in | Source | License |
|---|---|---|---|
| Icons: alarm, add, close, delete, keyboard, schedule, backspace, check | App icon, notification, app screens, math keypad | [Material Icons](https://github.com/google/material-design-icons) by Google | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) |

The fallback alarm sound (`androidApp/src/main/res/raw/alarm_fallback.wav`) isn't a third-party asset. It's synthesized by [tools/generate_fallback_alarm_sound.py](tools/generate_fallback_alarm_sound.py) and licensed like the rest of OpenAlarm.
