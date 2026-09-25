#!/usr/bin/env python3
"""Generates androidApp/src/main/res/raw/alarm_fallback.wav.

This is the sound OpenAlarm plays when no other alarm sound works, for example when the chosen
sound file was deleted, or when the phone restarted overnight and user files are still locked.
It's synthesized, so the app ships no third-party audio. It loops as a repeating two-note chime.

Run from the repository root:  python3 tools/generate_fallback_alarm_sound.py
"""

import math
import struct
import wave
from pathlib import Path

RATE = 22050
OUTPUT = Path(__file__).resolve().parent.parent / "androidApp/src/main/res/raw/alarm_fallback.wav"


def tone(frequency, seconds, amplitude=0.8):
    count = int(RATE * seconds)
    release = int(RATE * 0.01)
    for i in range(count):
        t = i / RATE
        envelope = min(1.0, t / 0.005) * math.exp(-t * 8) * min(1.0, (count - i) / release)
        wave_value = math.sin(2 * math.pi * frequency * t) + 0.3 * math.sin(4 * math.pi * frequency * t)
        yield amplitude * envelope * wave_value / 1.3


def silence(seconds):
    return [0.0] * int(RATE * seconds)


def main():
    samples = [*tone(880.0, 0.22), *silence(0.06), *tone(1318.51, 0.22), *silence(0.5)]
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(OUTPUT), "wb") as out:
        out.setnchannels(1)
        out.setsampwidth(2)
        out.setframerate(RATE)
        out.writeframes(b"".join(struct.pack("<h", round(max(-1.0, min(1.0, s)) * 32767)) for s in samples))
    print(f"Wrote {OUTPUT} ({len(samples) / RATE:.2f} s)")


if __name__ == "__main__":
    main()
