---
id: 15-08
title: SDR Waterfall
chapter: 15
section: 08
level: simple
status: draft
---

# SDR Waterfall

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A software-defined radio with a waterfall display turns RFI from a sound problem into a picture problem. You see exactly which frequencies are affected, the temporal pattern, and often the source's modulation signature. Combined with the AM radio sniffer (§15-07), an SDR waterfall is the modern RFI-hunter's complete toolkit.

## What a waterfall shows

A waterfall display plots:

- **X-axis**: frequency (a few kHz to a few MHz wide, depending on settings).
- **Y-axis**: time (newest at top, scrolling down).
- **Color**: signal strength (typically blue = weak, red/yellow = strong).

In one screen you can see:

- A constant carrier (vertical bright line at one frequency).
- Hash from a switching supply (broad, fuzzy band of brightness).
- Bursts (short bright stripes at intervals).
- A weak signal you might be trying to receive (faint colored line).

The visual pattern is often diagnostic on its own.

## Cheap SDRs that work

In rough order of usefulness for RFI work:

| SDR | Approximate price | Notes |
|-----|------------------:|-------|
| **RTL-SDR Blog v3 dongle** | $35 | The standard cheap option; covers 500 kHz to 1.7 GHz; great for everything |
| **Airspy HF+ Discovery** | $169 | Better dynamic range; best for HF only |
| **SDRplay RSP1A** | $109 | Wide coverage, good performance, mid-range |
| **HackRF One** | $300 | Wider tuning, transmit-capable; overkill for receive-only RFI |
| **KiwiSDR** | $300+ for a receiver, but free worldwide via online ones | Web-accessible; great for verifying that a noise source is local vs propagated |

For RFI work specifically, an **RTL-SDR Blog v3** is enough. The reasonable upgrade is the Airspy HF+ Discovery if you're doing serious HF receive work and want better dynamic range.

## Software

Common choices:

- **SDR#** (Windows) — free, the most popular Windows SDR app.
- **GQRX** (Linux/Mac) — free, full-featured.
- **SDRangel** (cross-platform) — more complex, more capable.
- **HDSDR** (Windows) — older, still good for SDR work.
- **CubicSDR** (cross-platform) — clean UI, suitable for waterfall work.

All have a waterfall mode. Pick whichever runs cleanly on your computer.

## Setting it up for RFI work

1. **Connect the SDR to your station antenna** for full sensitivity.
   - Or, for sniffing in different rooms: use a short whip antenna and walk around with the dongle plugged into a laptop.
2. **Set the waterfall span** to 200–500 kHz initially (wide enough to see broad sources, narrow enough to see detail).
3. **Set the waterfall sweep speed** to medium (you want to see at least 30 seconds of history).
4. **Set the color range** to maximize contrast — bring the noise floor up to a clear blue and the signals to bright orange/red.

Take a screenshot of "normal" so you have a reference. Then watch how it changes when you turn devices on/off in the house.

## Reading common patterns

### Constant broad hash

```
Frequency:     ─────────────────────────────────────
0 MHz                                          30 MHz

Time ↓
   Bright haze across all frequencies, constant brightness
```

A switching power supply, plasma TV, solar inverter. Look for a fundamental at the switching frequency (often 50–100 kHz) and harmonics every 50–100 kHz across the band.

### Tonal source

```
Frequency:     ─────│─────────│─────────│──────│────
                    f1        f2        f3     f4

Time ↓
   Bright vertical lines at specific frequencies, constant
```

A digital oscillator. The frequency spacing between lines tells you the fundamental — if lines are at 1.000, 2.000, 3.000, 4.000 MHz, the source has a 1 MHz fundamental.

### Bursty source

```
Frequency:     ───────────────│││───────────────────
                              ┌─┐
Time ↓                        │█│ Burst, then silence
                              │ │
                              ─ ─
                              ┌─┐
                              │█│ Another burst
```

Powerline networking, Wi-Fi sync, some smart-home devices. The burst pattern (regular intervals or random) tells you about the protocol.

### Modulated source

```
Frequency:     ─────────│─────────│─────────│───────
Time ↓         Bright vertical lines that wobble or breathe
```

A radio station with AM modulation. Mod sidebands appear within ±10 kHz of carriers.

### Power-line corona

```
Frequency:     ───── all frequencies, with 60 Hz pulse ───
Time ↓         Brightness pulses at 60 Hz/120 Hz rate
```

Power-line noise has a characteristic "buzz" with 60 Hz periodicity, visible as a pulsation at the rate of the AC frequency.

## Combining with the AM sniffer

The two tools complement each other:

- **AM sniffer** tells you *where* the source is (direction).
- **SDR waterfall** tells you *what* the source is doing (frequency, modulation, time pattern).

Workflow: use the SDR to characterize the noise, then take the AM radio out for direction-finding with the SDR's pattern in mind.

## A specific use case: identifying ham band-specific noise

If you have noise on a specific band but not others, the waterfall is invaluable:

1. Set SDR to span the troublesome band.
2. Look at the noise pattern.
3. Compare to neighboring bands.
4. The pattern usually points at the source — a tonal source means an oscillator; broadband means a switching device; bursty means a digital protocol.

For example, if you have noise on 14 MHz but not 7 MHz, and the waterfall shows a tonal signal at 14.250 MHz with sidebands every 1 kHz, you're probably picking up an SSB station's audio modulation (not RFI at all). Different problem!

## Real-time direction finding with an SDR + portable computer

A laptop, an RTL-SDR, and a small directional antenna together make a portable RFI hunting station:

1. Walk to a suspected location.
2. Look at the waterfall on your laptop.
3. Watch the noise level as you rotate the antenna.
4. The peak orientation points at the source.

This is more precise than the AM sniffer and gives you frequency information at the same time. It's also more cumbersome.

## Recording for analysis

Many SDR programs let you save the IQ data stream for replay later. Useful for:

- Sending to others for diagnosis.
- Analyzing noise patterns in detail with offline tools.
- Comparing "before" and "after" of a fix attempt.

A 10-second IQ recording at HF rates is typically 50–100 MB. Compress before sending.

## Web SDRs as a sanity check

If you suspect a noise might be propagated (an HF station you're picking up, a long-distance interference source), tune to the same frequency on a remote KiwiSDR (e.g., one in Europe, Australia, or the other US coast):

- If the noise is there too → it's propagated.
- If the noise is absent → it's local to your station.

This single test has saved many operators from chasing nonexistent local sources.

## See also

- §15-07 — AM radio sniffer (the directional companion to the SDR's spectral view)
- §15-03 — sound-based identification (links to waterfall patterns above)
- §17 — power-line noise (waterfall patterns are diagnostic)
