#!/usr/bin/env python3
"""
generate_rtty_test_suite.py

Generate known-answer RTTY WAV files for decoder testing.

Expected text:
    CQ CQ CQ DE W3XYZ W3XYZ TEST K\r
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
TEXT = "CQ CQ CQ DE W3XYZ W3XYZ TEST K\r"

# ITA2 / Baudot mapping, LSB-first on the wire.
LETTERS_TO_CODE = {
    "E": 0x01,
    "\n": 0x02,
    "A": 0x03,
    " ": 0x04,
    "S": 0x05,
    "I": 0x06,
    "U": 0x07,
    "\r": 0x08,
    "D": 0x09,
    "R": 0x0A,
    "J": 0x0B,
    "N": 0x0C,
    "F": 0x0D,
    "C": 0x0E,
    "K": 0x0F,
    "T": 0x10,
    "Z": 0x11,
    "L": 0x12,
    "W": 0x13,
    "H": 0x14,
    "Y": 0x15,
    "P": 0x16,
    "Q": 0x17,
    "O": 0x18,
    "B": 0x19,
    "G": 0x1A,
    "M": 0x1C,
    "X": 0x1D,
    "V": 0x1E,
}

FIGURES_TO_CODE = {
    "3": 0x01,
    "\n": 0x02,
    "-": 0x03,
    " ": 0x04,
    "'": 0x05,
    "8": 0x06,
    "7": 0x07,
    "\r": 0x08,
    "$": 0x09,
    "4": 0x0A,
    ",": 0x0C,
    "!": 0x0D,
    ":": 0x0E,
    "(": 0x0F,
    "5": 0x10,
    '"': 0x11,
    ")": 0x12,
    "2": 0x13,
    "#": 0x14,
    "6": 0x15,
    "0": 0x16,
    "1": 0x17,
    "9": 0x18,
    "?": 0x19,
    "&": 0x1A,
    ".": 0x1C,
    "/": 0x1D,
    ";": 0x1E,
}

LTRS = 0x1F
FIGS = 0x1B


@dataclass(frozen=True)
class RttyConfig:
    baud: float
    mark_hz: float
    space_hz: float
    stop_bits: float
    reverse: bool
    name: str


def text_to_baudot_codes(text: str) -> list[int]:
    codes: list[int] = []
    letters_mode = True

    for ch in text:
        up = ch.upper()

        if up in LETTERS_TO_CODE:
            if not letters_mode:
                codes.append(LTRS)
                letters_mode = True
            codes.append(LETTERS_TO_CODE[up])
            continue

        if up in FIGURES_TO_CODE:
            if letters_mode:
                codes.append(FIGS)
                letters_mode = False
            codes.append(FIGURES_TO_CODE[up])
            continue

        raise ValueError(f"Unsupported character for ITA2: {ch!r}")

    return codes


def append_tone(samples: list[float], freq_hz: float, duration_s: float, phase: float) -> float:
    count = int(round(duration_s * SAMPLE_RATE))
    two_pi_f = 2.0 * math.pi * freq_hz
    for i in range(count):
        t = i / SAMPLE_RATE
        samples.append(AMPLITUDE * math.sin(two_pi_f * t + phase))
    phase += two_pi_f * (count / SAMPLE_RATE)
    return math.fmod(phase, 2.0 * math.pi)


def encode_rtty_wave(cfg: RttyConfig, text: str) -> list[float]:
    codes = text_to_baudot_codes(text)
    bit_s = 1.0 / cfg.baud

    # "Normal": MARK idle, SPACE start bit, 5 data bits LSB-first, MARK stop.
    # "Reverse": swap which tone represents mark/space.
    mark_freq = cfg.space_hz if cfg.reverse else cfg.mark_hz
    space_freq = cfg.mark_hz if cfg.reverse else cfg.space_hz

    samples: list[float] = []
    phase = 0.0

    phase = append_tone(samples, mark_freq, LEAD_IN_SECONDS, phase)

    for code in codes:
        # start bit = SPACE
        phase = append_tone(samples, space_freq, bit_s, phase)

        # 5 data bits, LSB first
        for bit_index in range(5):
            bit = (code >> bit_index) & 0x01
            freq = mark_freq if bit else space_freq
            phase = append_tone(samples, freq, bit_s, phase)

        # stop bits = MARK
        phase = append_tone(samples, mark_freq, bit_s * cfg.stop_bits, phase)

    phase = append_tone(samples, mark_freq, TAIL_SECONDS, phase)
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
    out_dir = "rtty_test_suite"
    os.makedirs(out_dir, exist_ok=True)

    configs = [
        RttyConfig(45.45, 2125.0, 2295.0, 1.0, False, "rtty_4545_170_1p0_normal"),
        RttyConfig(45.45, 2125.0, 2295.0, 1.5, False, "rtty_4545_170_1p5_normal"),
        RttyConfig(45.45, 2125.0, 2295.0, 1.0, True,  "rtty_4545_170_1p0_reverse"),
        RttyConfig(45.45, 2125.0, 2295.0, 1.5, True,  "rtty_4545_170_1p5_reverse"),
    ]

    for cfg in configs:
        samples = encode_rtty_wave(cfg, TEXT)
        path = os.path.join(out_dir, f"{cfg.name}.wav")
        write_wav(path, samples)
        print(f"Wrote {path}")

    print("\nExpected text:")
    print(repr(TEXT))


if __name__ == "__main__":
    main()
