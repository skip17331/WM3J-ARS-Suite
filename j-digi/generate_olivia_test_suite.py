#!/usr/bin/env python3
"""
generate_olivia_test_suite.py

Generate known-answer Olivia MFSK WAV files for decoder testing.

Expected text:
    CQ CQ CQ DE W3XYZ K

Olivia protocol (Jalocha/SP9VRC, matches j-digi's OliviaTransmitter):
  * 8 tones, 500 Hz bandwidth, 1500 Hz centre (the 8/500 variant)
  * Tone spacing  = bandwidth / numTones = 62.5 Hz
  * Symbol rate   = tone spacing          = 62.5 baud
  * Samples/sym   = sampleRate / symRate  = 128 (at 8 kHz)
  * Bits/symbol   = log2(numTones)        = 3
  * Block size    = 64 symbols
  * Chars/block   = bits/symbol           = 3
  * Charset       = ASCII 0x20–0x5F (64 chars)
  * FEC           = Walsh-Hadamard per bit plane
  * Phase         = continuous across tone changes

Encoding (per block of 3 chars c0, c1, c2):
  For each symbol position k in 0..63:
     bit_b of tone index = popcount(c_b AND k) AND 1, for b = 0,1,2
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

NUM_TONES        = 8
BANDWIDTH_HZ     = 500
DEFAULT_CENTER   = 1500.0
BITS_PER_SYMBOL  = 3            # log2(8)
BLOCK_SYMBOLS    = 64
CHAR_SET_SIZE    = 64           # 2^6
CHAR_OFFSET      = 0x20
LEAD_BLOCKS      = 3            # lead-in + lead-out blocks of spaces


@dataclass(frozen=True)
class OliviaConfig:
    center_hz: float
    name: str


def normalize_text(text: str) -> list[int]:
    """ASCII → 6-bit Olivia codes (space = 0). Lowercase → uppercase."""
    codes = []
    for ch in text:
        if 'a' <= ch <= 'z':
            ch = chr(ord(ch) - 32)
        code = ord(ch) - CHAR_OFFSET
        codes.append(code if 0 <= code < CHAR_SET_SIZE else 0)
    return codes


def build_symbol_stream(text: str) -> list[int]:
    """Map text → list of tone indices, padded with lead-in/lead-out blocks."""
    codes = normalize_text(text)

    content_blocks = (len(codes) + BITS_PER_SYMBOL - 1) // BITS_PER_SYMBOL
    total_blocks   = LEAD_BLOCKS + content_blocks + LEAD_BLOCKS
    total_chars    = total_blocks * BITS_PER_SYMBOL

    padded = [0] * total_chars              # zero-filled = space
    content_start = LEAD_BLOCKS * BITS_PER_SYMBOL
    for i, c in enumerate(codes):
        if content_start + i < total_chars:
            padded[content_start + i] = c

    symbols = []
    for blk in range(total_blocks):
        for k in range(BLOCK_SYMBOLS):
            sym = 0
            for b in range(BITS_PER_SYMBOL):
                char_code = padded[blk * BITS_PER_SYMBOL + b]
                walsh_bit = bin(char_code & k).count("1") & 1
                sym |= (walsh_bit << b)
            symbols.append(sym)
    return symbols


def encode_olivia_wave(cfg: OliviaConfig, text: str) -> list[float]:
    # Tone frequencies — same formula as the Java transmitter
    tone_spacing = BANDWIDTH_HZ / NUM_TONES
    lowest_tone  = cfg.center_hz - BANDWIDTH_HZ / 2.0 + tone_spacing / 2.0
    tone_freqs   = [lowest_tone + i * tone_spacing for i in range(NUM_TONES)]
    spS          = max(1, round(SAMPLE_RATE / tone_spacing))

    symbols = build_symbol_stream(text)
    samples: list[float] = []

    samples.extend([0.0] * round(LEAD_IN_SECONDS * SAMPLE_RATE))

    phase = 0.0
    for sym in symbols:
        freq = tone_freqs[sym]
        phase_step = 2.0 * math.pi * freq / SAMPLE_RATE
        for _ in range(spS):
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
    out_dir = "olivia_test_suite"
    os.makedirs(out_dir, exist_ok=True)

    configs = [
        OliviaConfig(1500.0, "olivia_8_500_1500hz"),  # baseline default
        OliviaConfig(1200.0, "olivia_8_500_1200hz"),  # off-center low
        OliviaConfig(1800.0, "olivia_8_500_1800hz"),  # off-center high
    ]

    for cfg in configs:
        samples = encode_olivia_wave(cfg, TEXT)
        path = os.path.join(out_dir, f"{cfg.name}.wav")
        write_wav(path, samples)
        dur = len(samples) / SAMPLE_RATE
        print(f"Wrote {path}  ({dur:.2f} s)")

    print("\nExpected text:")
    print(repr(TEXT))


if __name__ == "__main__":
    main()
