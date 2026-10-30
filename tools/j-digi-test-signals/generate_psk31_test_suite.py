#!/usr/bin/env python3
"""
generate_psk31_test_suite.py

Generate known-answer PSK31 (BPSK31) WAV files for decoder testing.

Expected text:
    CQ CQ CQ DE W3XYZ K

PSK31 protocol (matches j-digi's Psk31Transmitter):
  * Symbol rate          : 31.25 baud
  * Samples/symbol       : 256 (at 8 kHz)
  * Default carrier      : 1000 Hz
  * Differential BPSK    : bit '0' = phase reversal, bit '1' = no change
  * Raised-cosine shaping over the symbol when the phase reverses
  * Preamble             : 32 '0' bits (steady idle — gives the decoder
                           a clean carrier signature to lock onto)
  * Postamble            : 32 '1' bits (mark steady state)
  * Per-character        : varicode bits + "00" separator

Each variant exercises a different decoder code path:
  * Speed                : symbol rate is fixed at 31.25 by spec
  * Off-center tone      : 800 / 1200 Hz to exercise AFC snap from 1000 Hz default
"""

from __future__ import annotations

import math
import os
import struct
import wave
from dataclasses import dataclass

SAMPLE_RATE = 8000
AMPLITUDE = 0.50
LEAD_IN_SECONDS = 0.75
TAIL_SECONDS = 0.50
TEXT = "CQ CQ CQ DE W3XYZ K"

SYMBOL_RATE = 31.25
LEAD_ONES = 64        # steady carrier (no reversals) — lets AFC lock on the real carrier
PREAMBLE_ZEROS = 32
POSTAMBLE_ONES = 32

# PSK31 varicode table — mirrors j-digi's Psk31Varicode.ASCII_TO_VARICODE.
VARICODE: dict[int, str] = {
    32: "1",            # space
    33: "111111111",
    34: "101011111",
    35: "111110101",
    36: "111011011",
    37: "1011010101",
    38: "1010111011",
    39: "101111111",
    40: "11111011",
    41: "11110111",
    42: "101101111",
    43: "111011111",
    44: "1110101",
    45: "110101",
    46: "1010111",
    47: "110101111",
    48: "10110111", 49: "10111101", 50: "11101101", 51: "11111111",
    52: "101110111", 53: "101011011", 54: "101101011", 55: "110101101",
    56: "110101011", 57: "110110111",
    58: "11110101", 59: "110111101", 60: "111101101", 61: "1010101",
    62: "111010111", 63: "1010101111", 64: "1010111101",
    65: "1111101", 66: "11101011", 67: "10101101", 68: "10110101",
    69: "1110111", 70: "11011011", 71: "11111101", 72: "101010101",
    73: "1111111", 74: "111111101", 75: "101111101", 76: "11010111",
    77: "10111011", 78: "11011101", 79: "10101011", 80: "11010101",
    81: "111011101", 82: "10101111", 83: "1101111", 84: "1101101",
    85: "101010111", 86: "110110101", 87: "101011101", 88: "101110101",
    89: "101111011", 90: "1010101101",
}


@dataclass(frozen=True)
class Psk31Config:
    carrier_hz: float
    name: str


def text_to_bits(text: str) -> str:
    """Map text → bit stream: each char's varicode followed by "00" separator."""
    bits: list[str] = []
    for ch in text:
        code = VARICODE.get(ord(ch))
        if code is None:
            raise ValueError(f"Unsupported character for PSK31 test suite: {ch!r}")
        bits.append(code)
        bits.append("00")
    return "".join(bits)


def encode_psk31_wave(cfg: Psk31Config, text: str) -> list[float]:
    samples_per_symbol = int(round(SAMPLE_RATE / SYMBOL_RATE))
    phase_step = 2.0 * math.pi * cfg.carrier_hz / SAMPLE_RATE

    bit_stream = (
        ("1" * LEAD_ONES)           # steady carrier — AFC can lock here
        + ("0" * PREAMBLE_ZEROS)    # standard PSK31 idle (reversals)
        + text_to_bits(text)
        + ("1" * POSTAMBLE_ONES)
    )

    samples: list[float] = []

    # Lead-in silence
    samples.extend([0.0] * int(round(LEAD_IN_SECONDS * SAMPLE_RATE)))

    phase = 0.0
    state_sign = 1   # current "steady" sign of the BPSK output

    for bit in bit_stream:
        start_sign = state_sign
        end_sign = -state_sign if bit == "0" else state_sign

        for i in range(samples_per_symbol):
            mu = i / samples_per_symbol
            if start_sign == end_sign:
                baseband = end_sign
            else:
                # raised-cosine interpolation across the symbol
                baseband = start_sign + (end_sign - start_sign) * 0.5 * (1.0 - math.cos(math.pi * mu))
            samples.append(AMPLITUDE * baseband * math.sin(phase))
            phase += phase_step
            if phase >= 2.0 * math.pi:
                phase -= 2.0 * math.pi

        state_sign = end_sign

    # Tail silence
    samples.extend([0.0] * int(round(TAIL_SECONDS * SAMPLE_RATE)))
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
    out_dir = "psk31_test_suite"
    os.makedirs(out_dir, exist_ok=True)

    configs = [
        Psk31Config(1000.0, "psk31_1000hz"),     # baseline — default carrier
        Psk31Config(800.0,  "psk31_800hz"),      # off-center: AFC snap from 1000
        Psk31Config(1200.0, "psk31_1200hz"),     # off-center the other way
        Psk31Config(1500.0, "psk31_1500hz"),     # bigger AFC jump
    ]

    for cfg in configs:
        samples = encode_psk31_wave(cfg, TEXT)
        path = os.path.join(out_dir, f"{cfg.name}.wav")
        write_wav(path, samples)
        dur = len(samples) / SAMPLE_RATE
        print(f"Wrote {path}  ({dur:.2f} s)")

    print("\nExpected text:")
    print(repr(TEXT))


if __name__ == "__main__":
    main()
