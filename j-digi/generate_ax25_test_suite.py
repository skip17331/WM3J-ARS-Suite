#!/usr/bin/env python3
"""
generate_ax25_test_suite.py

Generate known-answer AX.25 / Bell 202 AFSK WAV files for decoder testing.

Each frame is a UI (unnumbered information) packet with a printable payload,
encoded as 1200-baud AFSK (1200 Hz mark / 2200 Hz space), NRZI on the wire,
HDLC bit-stuffed, with CRC-16-CCITT (X.25 polynomial) FCS.

Generates:
  ax25_clean.wav    one packet, no noise
  ax25_burst.wav    three packets back-to-back
  ax25_noisy.wav    one packet with low-level white noise
"""

from __future__ import annotations

import math
import os
import random
import struct
import wave
from dataclasses import dataclass, field
from typing import Iterable

SAMPLE_RATE = 8000
AMPLITUDE = 0.55
MARK_HZ = 1200.0
SPACE_HZ = 2200.0
BAUD = 1200.0
SAMPLES_PER_BIT = SAMPLE_RATE / BAUD

LEAD_SECONDS = 0.30
TAIL_SECONDS = 0.30
PREAMBLE_FLAGS = 24   # ~160 ms of flag preamble
POSTAMBLE_FLAGS = 4


@dataclass(frozen=True)
class Address:
    call: str
    ssid: int = 0
    has_been_repeated: bool = False  # H-bit for digipeaters; C/R for dest/src

    def encode(self, is_last: bool) -> bytes:
        call = self.call.upper().ljust(6, " ")
        if len(call) > 6:
            raise ValueError(f"Callsign too long: {self.call!r}")
        out = bytearray()
        for ch in call:
            out.append((ord(ch) & 0x7F) << 1)
        ssid_byte = ((self.ssid & 0x0F) << 1) | 0x60   # reserved bits 5/6 = 1
        if self.has_been_repeated:
            ssid_byte |= 0x80
        if is_last:
            ssid_byte |= 0x01
        out.append(ssid_byte)
        return bytes(out)


@dataclass
class Ax25Frame:
    dest: Address
    src: Address
    digis: list[Address] = field(default_factory=list)
    control: int = 0x03    # UI
    pid: int = 0xF0        # no L3 protocol
    info: bytes = b""

    def serialize(self) -> bytes:
        addrs = bytearray()
        # destination is never last; source is last iff no digis.
        addrs += self.dest.encode(False)
        addrs += self.src.encode(len(self.digis) == 0)
        for i, d in enumerate(self.digis):
            addrs += d.encode(i == len(self.digis) - 1)
        body = bytes(addrs) + bytes([self.control, self.pid]) + self.info
        fcs = crc16_ccitt(body)
        return body + bytes([fcs & 0xFF, (fcs >> 8) & 0xFF])


def crc16_ccitt(data: bytes) -> int:
    """X.25 / HDLC FCS: init 0xFFFF, poly 0x1021 (reversed 0x8408),
    LSB-first byte processing, final XOR 0xFFFF."""
    crc = 0xFFFF
    for byte in data:
        for i in range(8):
            bit = (byte >> i) & 1
            lsb = (crc & 1) ^ bit
            crc >>= 1
            if lsb:
                crc ^= 0x8408
    return crc ^ 0xFFFF


def hdlc_bit_stream(frame_bytes: bytes,
                    preamble_flags: int = PREAMBLE_FLAGS,
                    postamble_flags: int = POSTAMBLE_FLAGS) -> list[int]:
    """Build the bit stream: flags, frame (bit-stuffed), flags. LSB-first."""
    bits: list[int] = []
    flag_bits = [0, 1, 1, 1, 1, 1, 1, 0]  # 0x7E LSB-first

    for _ in range(preamble_flags):
        bits.extend(flag_bits)

    ones = 0
    for byte in frame_bytes:
        for i in range(8):
            b = (byte >> i) & 1
            bits.append(b)
            if b == 1:
                ones += 1
                if ones == 5:
                    bits.append(0)
                    ones = 0
            else:
                ones = 0

    for _ in range(postamble_flags):
        bits.extend(flag_bits)
    return bits


def nrzi_encode(bits: Iterable[int], start_level: int = 1) -> list[int]:
    level = start_level
    out: list[int] = []
    for b in bits:
        if b == 0:
            level ^= 1
        out.append(level)
    return out


def render_afsk(levels: list[int]) -> list[float]:
    """Render a sequence of bit levels (1=mark, 0=space) into audio samples
    with a continuous phase accumulator across bit boundaries."""
    samples: list[float] = []
    phase = 0.0
    bit_idx = 0.0
    for level in levels:
        freq = MARK_HZ if level == 1 else SPACE_HZ
        next_bit_idx = bit_idx + SAMPLES_PER_BIT
        n0 = int(round(bit_idx))
        n1 = int(round(next_bit_idx))
        count = n1 - n0
        d_phase = 2.0 * math.pi * freq / SAMPLE_RATE
        for _ in range(count):
            samples.append(AMPLITUDE * math.sin(phase))
            phase += d_phase
        bit_idx = next_bit_idx
    return samples


def silence(seconds: float) -> list[float]:
    return [0.0] * int(round(seconds * SAMPLE_RATE))


def add_white_noise(samples: list[float], stddev: float, seed: int = 1) -> list[float]:
    rng = random.Random(seed)
    return [s + rng.gauss(0.0, stddev) for s in samples]


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


def render_frame_audio(frame: Ax25Frame) -> list[float]:
    bits = hdlc_bit_stream(frame.serialize())
    levels = nrzi_encode(bits)
    return render_afsk(levels)


def main() -> None:
    out_dir = "ax25_test_suite"
    os.makedirs(out_dir, exist_ok=True)

    # Frame 1: classic APRS-style position-ish text. We don't pretend to be
    # full APRS — the payload is just printable ASCII so the decoder text
    # path is exercised end-to-end.
    frame1 = Ax25Frame(
        dest=Address("APRS"),
        src=Address("WM3J", ssid=7),
        digis=[Address("WIDE1", ssid=1), Address("WIDE2", ssid=2)],
        info=b">Test packet from j-digi AX.25 decoder!",
    )
    frame2 = Ax25Frame(
        dest=Address("CQ"),
        src=Address("WM3J"),
        info=b"Hello AX.25 world",
    )
    frame3 = Ax25Frame(
        dest=Address("BEACON"),
        src=Address("N0CALL", ssid=9),
        digis=[Address("WIDE1", ssid=1, has_been_repeated=True)],
        info=b"!4044.00N/07400.00W>Beacon test",
    )

    # clean: single frame, padding before and after
    clean = silence(LEAD_SECONDS) + render_frame_audio(frame1) + silence(TAIL_SECONDS)
    write_wav(os.path.join(out_dir, "ax25_clean.wav"), clean)
    print(f"Wrote {out_dir}/ax25_clean.wav  ({len(clean)/SAMPLE_RATE:.2f}s)")

    # burst: three frames back-to-back with short inter-frame gaps
    burst = silence(LEAD_SECONDS)
    for f in (frame1, frame2, frame3):
        burst += render_frame_audio(f)
        burst += silence(0.08)
    burst += silence(TAIL_SECONDS)
    write_wav(os.path.join(out_dir, "ax25_burst.wav"), burst)
    print(f"Wrote {out_dir}/ax25_burst.wav  ({len(burst)/SAMPLE_RATE:.2f}s)")

    # noisy: single frame with modest gaussian noise
    noisy = silence(LEAD_SECONDS) + render_frame_audio(frame1) + silence(TAIL_SECONDS)
    noisy = add_white_noise(noisy, stddev=0.06)
    write_wav(os.path.join(out_dir, "ax25_noisy.wav"), noisy)
    print(f"Wrote {out_dir}/ax25_noisy.wav  ({len(noisy)/SAMPLE_RATE:.2f}s)")

    print()
    print("Expected decoded packets (one per frame):")
    print("  WM3J-7>APRS,WIDE1-1,WIDE2-2 [ctrl=03 pid=F0] :>Test packet from j-digi AX.25 decoder!")
    print("  WM3J>CQ [ctrl=03 pid=F0] :Hello AX.25 world")
    print("  N0CALL-9>BEACON,WIDE1-1* [ctrl=03 pid=F0] :!4044.00N/07400.00W>Beacon test")


if __name__ == "__main__":
    main()
