#!/usr/bin/env python3
"""
generate_dominoex_test_suite.py

Generate known-answer DominoEX WAV files for decoder testing.

Expected text:
    CQ CQ CQ DE W3XYZ K

DominoEX8 protocol (matches j-digi's DominoExTransmitter, default variant):
  * 18 tones, 7.8125 Hz spacing = symbol rate
  * Samples/symbol = 1024 at 8 kHz
  * Tone bins straddle 1500 Hz: firstBin = round(1500 / binWidth) - 8
    With binWidth = 8000/1024 = 7.8125 Hz, firstBin = round(192) - 8 = 184
    → tones at bins 184..201 → 1437.5 .. 1570.3 Hz
  * Each byte → 2 nibbles (high first), each nibble → 1 IFK+ tone
    tx_tone = (prev + nibble + 1) mod 18
  * Preamble  : 16 bytes of 0x00
  * Postamble :  8 bytes of 0x00

Variants generated:
  baseline (centred near 1500 Hz)  — bin shift 0
  lower    (centred near 1300 Hz)  — bin shift -26 (~200 Hz down)
  upper    (centred near 1700 Hz)  — bin shift +26 (~200 Hz up)
"""

from __future__ import annotations

import math
import os
import struct
import wave
from dataclasses import dataclass

SAMPLE_RATE = 8000
AMPLITUDE = 0.45
LEAD_IN_SECONDS = 0.75
TAIL_SECONDS = 0.50
TEXT = "CQ CQ CQ DE W3XYZ K"

NUM_TONES        = 18
SAMPLES_PER_SYM  = 1024            # DominoEX8 at 8 kHz
DEFAULT_FIRST_BIN = round(1500.0 / (SAMPLE_RATE / SAMPLES_PER_SYM)) - 8  # 184
PREAMBLE_BYTES   = 16
POSTAMBLE_BYTES  = 8


@dataclass(frozen=True)
class DominoExConfig:
    bin_shift: int
    name: str


def build_tone_stream(text: str) -> list[int]:
    """Encode text → list of IFK+ tone indices (mod 18, 2 tones/byte)."""
    payload = bytes(text, "ascii")
    all_bytes = bytes([0] * PREAMBLE_BYTES) + payload + bytes([0] * POSTAMBLE_BYTES)
    tones: list[int] = []
    prev_tone = 0
    for b in all_bytes:
        hi = (b >> 4) & 0x0F
        lo =  b       & 0x0F
        t0 = (prev_tone + hi + 1) % NUM_TONES
        tones.append(t0); prev_tone = t0
        t1 = (prev_tone + lo + 1) % NUM_TONES
        tones.append(t1); prev_tone = t1
    return tones


def synthesize(tones: list[int], first_bin: int) -> list[float]:
    bin_width = SAMPLE_RATE / SAMPLES_PER_SYM   # 7.8125 Hz
    tone_freqs = [(first_bin + i) * bin_width for i in range(NUM_TONES)]

    samples: list[float] = []
    # lead-in silence
    samples.extend([0.0] * int(LEAD_IN_SECONDS * SAMPLE_RATE))

    phase = 0.0
    for tone_idx in tones:
        freq = tone_freqs[tone_idx]
        step = 2.0 * math.pi * freq / SAMPLE_RATE
        for _ in range(SAMPLES_PER_SYM):
            samples.append(math.sin(phase) * AMPLITUDE)
            phase += step
            if phase >= 2.0 * math.pi:
                phase -= 2.0 * math.pi

    # tail silence
    samples.extend([0.0] * int(TAIL_SECONDS * SAMPLE_RATE))
    return samples


def write_wav(path: str, samples: list[float]) -> None:
    pcm = b"".join(struct.pack("<h", max(-32768, min(32767, int(s * 32767)))) for s in samples)
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SAMPLE_RATE)
        w.writeframes(pcm)


def main() -> None:
    here = os.path.dirname(os.path.abspath(__file__))
    out_dir = os.path.join(here, "dominoex_test_suite")
    os.makedirs(out_dir, exist_ok=True)

    bin_width = SAMPLE_RATE / SAMPLES_PER_SYM
    configs = [
        DominoExConfig(bin_shift=0,   name="dominoex8_1500hz"),
        DominoExConfig(bin_shift=-26, name="dominoex8_1300hz"),
        DominoExConfig(bin_shift=+26, name="dominoex8_1700hz"),
    ]

    tones = build_tone_stream(TEXT)
    print(f"Tone stream: {len(tones)} tones, {len(tones) * SAMPLES_PER_SYM / SAMPLE_RATE:.2f}s")

    for cfg in configs:
        first_bin = DEFAULT_FIRST_BIN + cfg.bin_shift
        centre_hz = (first_bin + 8.5) * bin_width
        samples = synthesize(tones, first_bin)
        out_path = os.path.join(out_dir, cfg.name + ".wav")
        write_wav(out_path, samples)
        print(f"  wrote {out_path}  firstBin={first_bin}  centre≈{centre_hz:.1f} Hz")


if __name__ == "__main__":
    main()
