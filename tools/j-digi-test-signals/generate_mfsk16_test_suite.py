#!/usr/bin/env python3
"""
generate_mfsk16_test_suite.py

Generate known-answer MFSK16 WAV files for decoder testing.

Expected text:
    CQ CQ CQ DE W3XYZ K

MFSK16 protocol (matches j-digi's Mfsk16Transmitter):
  * 16 tones, 15.625 Hz spacing = symbol rate
  * Samples/symbol = 512 at 8 kHz
  * Tone bins 89..104 → 1390.625 .. 1625 Hz (centre ≈ 1500 Hz)
  * Each byte → 8 LSB-first data bits → K=7 r=1/2 conv encoder → 16 encoded
    bits → 4 nibbles → 4 IFK+ tones, where tx[n] = (prev + nibble + 1) mod 16
  * Preamble  : 16 bytes of 0x00
  * Postamble : 8 bytes of 0x00
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

NUM_TONES        = 16
FIRST_BIN        = 89
SAMPLES_PER_SYM  = 512
PREAMBLE_BYTES   = 16
POSTAMBLE_BYTES  = 8

# K=7 r=1/2 generator polynomials (standard, identical to ViterbiK7).
G0 = 0b1011011    # 91
G1 = 0b1111001    # 121
K  = 7


@dataclass(frozen=True)
class Mfsk16Config:
    bin_shift: int          # offset added to FIRST_BIN (0 = default, 1500 Hz centre)
    name: str


def encode_byte_to_nibbles(b: int, conv_state: int) -> tuple[list[int], int]:
    """LSB-first 8 bits through K=7 r=1/2 encoder; return 4 nibbles + new state."""
    encoded = 0  # 16 encoded bits, bit 15 = first out, MSB-first within each nibble
    for bit in range(8):
        u = (b >> bit) & 1
        sr = (u << (K - 1)) | conv_state
        o0 = bin(sr & G0).count("1") & 1
        o1 = bin(sr & G1).count("1") & 1
        conv_state = sr >> 1
        encoded |= (o0 << (15 - bit * 2))
        encoded |= (o1 << (14 - bit * 2))
    nibbles = [(encoded >> (12 - n * 4)) & 0x0F for n in range(4)]
    return nibbles, conv_state


def build_tone_stream(text: str) -> list[int]:
    """Encode text → list of IFK+ tone indices."""
    payload = bytes(text, "ascii")
    all_bytes = bytes([0] * PREAMBLE_BYTES) + payload + bytes([0] * POSTAMBLE_BYTES)
    tones: list[int] = []
    prev_tone  = 0
    conv_state = 0
    for b in all_bytes:
        nibbles, conv_state = encode_byte_to_nibbles(b, conv_state)
        for nibble in nibbles:
            tone = (prev_tone + nibble + 1) % NUM_TONES
            tones.append(tone)
            prev_tone = tone
    return tones


def encode_mfsk16_wave(cfg: Mfsk16Config, text: str) -> list[float]:
    # Tone table — phase continuous across tone changes
    spacing  = SAMPLE_RATE / SAMPLES_PER_SYM           # 15.625 Hz
    base_bin = FIRST_BIN + cfg.bin_shift
    tone_freqs = [(base_bin + i) * spacing for i in range(NUM_TONES)]

    tones = build_tone_stream(text)
    samples: list[float] = []
    samples.extend([0.0] * round(LEAD_IN_SECONDS * SAMPLE_RATE))

    phase = 0.0
    for tone_idx in tones:
        freq = tone_freqs[tone_idx]
        phase_step = 2.0 * math.pi * freq / SAMPLE_RATE
        for _ in range(SAMPLES_PER_SYM):
            samples.append(AMPLITUDE * math.sin(phase))
            phase += phase_step
            if phase >= 2.0 * math.pi:
                phase -= 2.0 * math.pi

    samples.extend([0.0] * round(TAIL_SECONDS * SAMPLE_RATE))
    return samples


def write_wav(path: str, samples: list[float]) -> None:
    with wave.open(path, "wb") as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(SAMPLE_RATE)
        frames = bytearray()
        for s in samples:
            clipped = max(-1.0, min(1.0, s))
            frames.extend(struct.pack("<h", int(round(clipped * 32767.0))))
        wf.writeframes(frames)


def main() -> None:
    out_dir = "mfsk16_test_suite"
    os.makedirs(out_dir, exist_ok=True)

    # Bin shift of N moves the entire 16-tone block by N × 15.625 Hz.
    configs = [
        Mfsk16Config(0,   "mfsk16_1500hz"),     # baseline (bins 89..104)
        Mfsk16Config(-19, "mfsk16_1203hz"),     # shift down (~ -297 Hz)
        Mfsk16Config( 19, "mfsk16_1797hz"),     # shift up   (~ +297 Hz)
    ]

    for cfg in configs:
        samples = encode_mfsk16_wave(cfg, TEXT)
        path = os.path.join(out_dir, f"{cfg.name}.wav")
        write_wav(path, samples)
        dur = len(samples) / SAMPLE_RATE
        print(f"Wrote {path}  ({dur:.2f} s)")

    print("\nExpected text:")
    print(repr(TEXT))


if __name__ == "__main__":
    main()
