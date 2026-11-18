#!/usr/bin/env python3
"""
generate_olivia_test_suite.py

Known-answer Olivia 8/500 WAV files for J-Digi's decoder tests, in STANDARD Olivia (punch list 3-12), as fldigi and the other
Olivia programs send it. Written straight from the published description of the mode (Pawel Jalocha, SP9VRC) and fldigi's
reference behaviour, separately from J-Digi's Java, so the two check each other.

Expected text:
    CQ CQ CQ DE W3XYZ K

The mode:
  * 8 tones, 500 Hz: tone spacing 62.5 Hz, 62.5 baud, 128 samples per symbol at 8 kHz
  * a block is 64 symbols and carries 3 characters (one per bit of the 3-bit symbol), 7-bit ASCII
  * each character c becomes 64 chips: a vector with +1 at c (c < 64) or -1 at c - 64, through an inverse fast Hadamard
    transform (pairs (a, b) -> (a - b, a + b), largest stride first)
  * chip t of character f is negated where bit (13 f + t) mod 64 of 0xE257E6D0291574EC is set
  * a negative chip t of character f sets bit (f + t) mod 3 of symbol t
  * symbol s goes out as tone gray(s) = s ^ (s >> 1)
  * each tone is shaped by 1 - cos over two symbol periods (overlapping its neighbours by half), and the phase steps a
    quarter turn either way between symbols
  * like fldigi, tones sit on multiples of 31.25 Hz (half the spacing): the first is bin int((f0 - 234.375) / 31.25) + 1

Idle: blocks of NUL characters before and after (fldigi shows nothing for NUL).
"""

from __future__ import annotations

import math
import os
import random
import struct
import wave

SAMPLE_RATE = 8000
SPS = 128                      # samples per symbol
TONES = 8
BPS = 3
BLOCK = 64
SCRAMBLE = 0xE257E6D0291574EC
AMPLITUDE = 0.45
LEAD_BLOCKS = 2
TEXT = "CQ CQ CQ DE W3XYZ K"


def ifht(v: list[int]) -> None:
    step = len(v) // 2
    while step:
        for p in range(0, len(v), 2 * step):
            for q in range(p, p + step):
                a, b = v[q], v[q + step]
                v[q], v[q + step] = a - b, a + b
        step //= 2


def encode_block(chars: list[int]) -> list[int]:
    sym = [0] * BLOCK
    for f, c in enumerate(chars):
        c &= 0x7F
        w = [0] * BLOCK
        if c < BLOCK:
            w[c] = 1
        else:
            w[c - BLOCK] = -1
        ifht(w)
        for t in range(BLOCK):
            chip = -w[t] if (SCRAMBLE >> ((f * 13 + t) % 64)) & 1 else w[t]
            if chip < 0:
                sym[t] |= 1 << ((f + t) % BPS)
    return [s ^ (s >> 1) for s in sym]


def symbols_for(text: str) -> list[int]:
    codes = [ord(ch) & 0x7F for ch in text]
    codes += [0] * (-len(codes) % BPS)
    codes = [0] * (LEAD_BLOCKS * BPS) + codes + [0] * (LEAD_BLOCKS * BPS)
    out: list[int] = []
    for i in range(0, len(codes), BPS):
        out += encode_block(codes[i:i + BPS])
    return out


def first_tone_hz(centre: float) -> float:
    bin_hz = SAMPLE_RATE / (2 * SPS)                       # 31.25
    offset = 500 * (1 - 0.5 / TONES) / 2                   # 234.375
    first_bin = int((centre - offset) / bin_hz) + 1
    return first_bin * bin_hz


def modulate(symbols: list[int], centre: float, seed: int) -> list[float]:
    rnd = random.Random(seed)
    f0 = first_tone_hz(centre)
    out = [0.0] * ((len(symbols) + 1) * SPS)
    phase = 0.0
    for k, s in enumerate(symbols):
        w = 2 * math.pi * (f0 + 62.5 * s) / SAMPLE_RATE
        for n in range(2 * SPS):
            shape = 0.5 * (1 - math.cos(math.pi * n / SPS))
            out[k * SPS + n] += AMPLITUDE * shape * math.cos(phase + w * n)
        phase += w * SPS + (math.pi / 2 if rnd.random() < 0.5 else -math.pi / 2)
    return out


def write_wav(path: str, samples: list[float]) -> None:
    with wave.open(path, "wb") as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(SAMPLE_RATE)
        wf.writeframes(b"".join(struct.pack("<h", max(-32767, min(32767, int(round(x * 32767))))) for x in samples))


def main() -> None:
    out_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "olivia_test_suite")
    os.makedirs(out_dir, exist_ok=True)
    silence = [0.0] * int(0.75 * SAMPLE_RATE)
    for centre in (1500, 1200, 1800):
        samples = silence + modulate(symbols_for(TEXT), centre, seed=centre) + silence[: SAMPLE_RATE // 2]
        path = os.path.join(out_dir, f"olivia_8_500_{centre}hz.wav")
        write_wav(path, samples)
        print(f"{path}: first tone {first_tone_hz(centre):.2f} Hz, {len(samples) / SAMPLE_RATE:.1f} s")


if __name__ == "__main__":
    main()
