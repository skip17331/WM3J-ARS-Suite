#!/usr/bin/env python3
"""
generate_cw_test_suite.py

Generate known-answer CW (Morse) WAV files for decoder testing.

Expected text:
    CQ CQ CQ DE W3XYZ K

Timing follows the ITU PARIS formula:
    1 dit            = 1200 ms / WPM
    1 dah            = 3 dits
    intra-character  = 1 dit
    inter-character  = 3 dits
    inter-word       = 7 dits

Each variant exercises a different decoder code path:
    * WPM range (15 / 20 / 25 / 30) — speed adaptation EMA
    * Off-center tone (500 / 900 Hz) — AFC snap from 700 Hz default
    * Soft envelope (raised-cosine rise/fall) — slicer hysteresis
"""

from __future__ import annotations

import math
import os
import struct
import wave
from dataclasses import dataclass

SAMPLE_RATE = 8000
AMPLITUDE = 0.65
LEAD_IN_SECONDS = 0.75
TAIL_SECONDS = 0.50
TEXT = "CQ CQ CQ DE W3XYZ K"

# Raised-cosine key shaping ramp (in milliseconds) to suppress key clicks.
# 5 ms is typical for a soft CW envelope; 0 disables shaping.
DEFAULT_RAMP_MS = 5.0

# Morse table — must match CwMorse.java (ITU).
MORSE = {
    "A": ".-",    "B": "-...",  "C": "-.-.",  "D": "-..",   "E": ".",
    "F": "..-.",  "G": "--.",   "H": "....",  "I": "..",    "J": ".---",
    "K": "-.-",   "L": ".-..",  "M": "--",    "N": "-.",    "O": "---",
    "P": ".--.",  "Q": "--.-",  "R": ".-.",   "S": "...",   "T": "-",
    "U": "..-",   "V": "...-",  "W": ".--",   "X": "-..-",  "Y": "-.--",
    "Z": "--..",
    "0": "-----", "1": ".----", "2": "..---", "3": "...--", "4": "....-",
    "5": ".....", "6": "-....", "7": "--...", "8": "---..", "9": "----.",
}


@dataclass(frozen=True)
class CwConfig:
    wpm: float
    tone_hz: float
    ramp_ms: float
    name: str


def text_to_morse(text: str) -> list[str]:
    """Map text → list of element strings, with ' ' marking word breaks."""
    elements: list[str] = []
    for ch in text:
        if ch == " ":
            elements.append(" ")
            continue
        up = ch.upper()
        if up not in MORSE:
            raise ValueError(f"Unsupported character for CW: {ch!r}")
        elements.append(MORSE[up])
    return elements


def append_tone(samples: list[float], freq_hz: float, duration_s: float,
                phase: float, ramp_ms: float) -> float:
    """Append a keyed tone (with optional raised-cosine envelope)."""
    count = int(round(duration_s * SAMPLE_RATE))
    if count == 0:
        return phase
    two_pi_f = 2.0 * math.pi * freq_hz
    ramp_samples = int(round(ramp_ms / 1000.0 * SAMPLE_RATE))
    ramp_samples = min(ramp_samples, count // 2)

    for i in range(count):
        env = 1.0
        if ramp_samples > 0:
            if i < ramp_samples:
                env = 0.5 * (1.0 - math.cos(math.pi * i / ramp_samples))
            elif i >= count - ramp_samples:
                k = count - 1 - i
                env = 0.5 * (1.0 - math.cos(math.pi * k / ramp_samples))
        t = i / SAMPLE_RATE
        samples.append(AMPLITUDE * env * math.sin(two_pi_f * t + phase))

    phase += two_pi_f * (count / SAMPLE_RATE)
    return math.fmod(phase, 2.0 * math.pi)


def append_silence(samples: list[float], duration_s: float) -> None:
    count = int(round(duration_s * SAMPLE_RATE))
    samples.extend([0.0] * count)


def encode_cw_wave(cfg: CwConfig, text: str) -> list[float]:
    dit_s = 1.2 / cfg.wpm  # PARIS: 1 dit = 1200 ms / WPM
    samples: list[float] = []
    phase = 0.0

    append_silence(samples, LEAD_IN_SECONDS)

    elements = text_to_morse(text)

    for idx, code in enumerate(elements):
        if code == " ":
            # Inter-word space = 7 dits.  Already 3 dits of inter-char space
            # has been emitted after the previous character, so add 4 more.
            append_silence(samples, 4 * dit_s)
            continue

        for i, sym in enumerate(code):
            mark_s = dit_s if sym == "." else 3 * dit_s
            phase = append_tone(samples, cfg.tone_hz, mark_s, phase, cfg.ramp_ms)
            # Intra-character gap = 1 dit (skip after the last element)
            if i < len(code) - 1:
                append_silence(samples, dit_s)

        # Inter-character gap = 3 dits (skip after the last character)
        if idx < len(elements) - 1 and elements[idx + 1] != " ":
            append_silence(samples, 3 * dit_s)
        elif idx < len(elements) - 1:
            # next is a word break — emit only 3 dits here; the word-break
            # branch will pad the remaining 4 dits.
            append_silence(samples, 3 * dit_s)

    append_silence(samples, TAIL_SECONDS)
    return samples


def write_wav(path: str, samples: list[float]) -> None:
    with wave.open(path, "wb") as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(SAMPLE_RATE)

        frames = bytearray()
        for s in samples:
            clipped = max(-1.0, min(1.0, s))
            pcm = int(round(clipped * 32767.0))
            frames.extend(struct.pack("<h", pcm))

        wf.writeframes(frames)


def main() -> None:
    out_dir = "cw_test_suite"
    os.makedirs(out_dir, exist_ok=True)

    configs = [
        # Baseline: 20 WPM at default 700 Hz with 5 ms shaping
        CwConfig(20.0, 700.0, DEFAULT_RAMP_MS, "cw_20wpm_700hz_soft"),
        # Speed variants — exercise PARIS EMA tracking
        CwConfig(15.0, 700.0, DEFAULT_RAMP_MS, "cw_15wpm_700hz_soft"),
        CwConfig(25.0, 700.0, DEFAULT_RAMP_MS, "cw_25wpm_700hz_soft"),
        CwConfig(30.0, 700.0, DEFAULT_RAMP_MS, "cw_30wpm_700hz_soft"),
        # Off-center tones — exercise AFC snap (within 300–1500 Hz window)
        CwConfig(20.0, 500.0, DEFAULT_RAMP_MS, "cw_20wpm_500hz_soft"),
        CwConfig(20.0, 900.0, DEFAULT_RAMP_MS, "cw_20wpm_900hz_soft"),
        # Hard-keyed (no shaping) — worst-case slicer transients
        CwConfig(20.0, 700.0, 0.0,             "cw_20wpm_700hz_hard"),
    ]

    for cfg in configs:
        samples = encode_cw_wave(cfg, TEXT)
        path = os.path.join(out_dir, f"{cfg.name}.wav")
        write_wav(path, samples)
        dur = len(samples) / SAMPLE_RATE
        print(f"Wrote {path}  ({dur:.2f} s)")

    print("\nExpected text:")
    print(repr(TEXT))


if __name__ == "__main__":
    main()
