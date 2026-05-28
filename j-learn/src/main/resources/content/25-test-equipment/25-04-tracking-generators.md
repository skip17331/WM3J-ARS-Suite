---
id: 25-04
title: Tracking Generators
chapter: 25
section: 04
level: mixed
status: draft
---

# Tracking Generators

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A **tracking generator (TG)** is a signal source that's locked to a spectrum analyzer's sweep. As the SA's receiver tunes across a band, the TG transmits a clean tone at exactly the same frequency. Connect TG-output to a device-under-test, then device-output to the SA input, and you get a real-time plot of the device's **frequency response** — gain (or loss) versus frequency, drawn live on the screen.

This turns the SA from a passive observer of the RF environment into an active **scalar network analyzer** (SNA) — like a VNA but without phase, just amplitude.

## What it's good for

Three classic amateur use cases:

### 1. Filter alignment

Build or buy a band-pass filter for, say, 20 m. You want to verify:

- **Insertion loss** at center frequency (target: <1 dB for a passive LC filter)
- **Bandwidth** at 3 dB and 30 dB down
- **Stopband rejection** in each direction (target: >40 dB at second harmonic and below band edge)

With a TG + SA, you sweep input frequency from 1 to 50 MHz and watch the output response curve in real-time. As you tune the filter's variable caps, you watch the response shift on-screen and tune for the shape you want. **Without** a TG, you'd inject one frequency at a time, write down the dB reading, change frequency, repeat — taking hours instead of seconds.

```
   S21 (dB)
      │
    0 │   ───╲                       ╱───      stop band
      │      ╲                      ╱
   -10│       ╲                    ╱
      │        ╲                  ╱
   -20│         ╲                ╱            3 dB BW
      │          ╲              ╱           ←───────→
   -30│           ╲            ╱
      │            ╲__________╱        ← pass band, ~1 dB IL
   -40│
      │
      └──────────────────────────────────────► frequency
           13     14     15     MHz
```

### 2. Antenna sweeping (when a VNA isn't available)

You can sweep an antenna with a TG + SA using a **directional coupler** or **return-loss bridge**:

- TG output → coupler input
- Coupler output → antenna
- Coupler reflected port → SA input

The SA shows you "return loss" (in dB) vs. frequency. A perfect match shows infinite return loss (deep dip); a mismatch shows a shallower dip.

Return loss to SWR conversion: `SWR = (1 + ρ) / (1 - ρ)` where `ρ = 10^(-RL/20)`. A 14 dB return loss corresponds to ~1.5:1 SWR.

In practice this works but a NanoVNA is faster, gives complex impedance, and costs less. The TG + SA approach is mostly used when you already have an SA and want one more capability without buying a separate VNA. (And the SA approach handles much higher dynamic range than a NanoVNA, which can matter for high-Q narrowband filter measurements.)

### 3. Amplifier gain plots

For a preamp, low-noise amp, or power amplifier, the TG sweeps the input and the SA reads the output. You see **gain vs. frequency** directly.

For a small-signal preamp:

- TG output (typical -10 to -20 dBm) → preamp input
- preamp output → SA (with appropriate input attenuation to keep the SA happy)
- Display shows gain dropping off at the band edges; flat in the middle

For a high-power PA: you cannot sweep at full power without a calibrated attenuator chain. Run the PA at reduced drive (a few watts), use a high-power coupler, attenuate the coupled output to safe SA input level, and sweep with the TG. Note that an amplifier's small-signal gain may differ from its full-power gain because of gain compression — for compression behavior you measure at one frequency with varied input level, not with a swept TG.

> **Advanced —** A true scalar network analyzer goes one step further than a swept TG: it can normalize out the cable/coupler frequency response (insertion-loss calibration) by capturing a "thru" sweep first, then displaying the DUT response relative to thru. Most modern SAs with TGs offer this as "normalize" or "trace math" mode. Without it, the TG output flatness and cable losses contaminate the measurement.

## What's in the box

TGs come in three flavors:

### Built into the SA

The TinySA Ultra, Siglent SVA1015X, and most mid-tier bench SAs have a TG built in. A single back-panel output jack provides the tracking signal. This is the simplest setup — connect a cable from TG out → DUT in, then DUT out → SA in.

### External TG matched to a specific SA

Older HP / Tek SAs sometimes had external tracking generators that connected via a special "sweep sync" cable. The TG had to be matched to the SA model (HP 85640A for the 8590 series, for example).

### Sweep-synced signal generator

A separate signal generator with a "sweep input" jack accepts a 0–10 V ramp from the SA's sweep output and tracks the SA's frequency. Less convenient but allows mixing brands.

## How it actually works under the hood

The SA's first LO is a tunable oscillator that gets mixed with the input to produce a fixed IF. As the SA sweeps, the LO sweeps. The TG takes a copy of the LO, mixes it back down with a second fixed oscillator at the IF, and produces an output at exactly the SA's currently-tuned input frequency.

Because the same physical LO drives both the SA and the TG, the timing is automatic — there's no synchronization to fail. The TG output is always at exactly the frequency the SA is tuned to.

This is why "tracking" generators are so much better than just running two independent generators in parallel: the SA always sees the TG signal exactly at the center of its receive bandwidth, with no offset or drift.

## Setting up a sweep

A typical filter-alignment session:

1. **Bypass the DUT first.** Connect TG output directly to SA input. Confirm a flat trace at the TG's reference level (typically -10 dBm). If it's not flat, **normalize** — engage the SA's normalize/trace-math feature to flatten the reference.
2. **Insert the DUT.** Connect TG → DUT input, DUT output → SA. The trace now shows the DUT's S21.
3. **Set span** to cover the DUT's expected pass band plus enough stop band to see rejection.
4. **Set RBW** to the smallest practical for the span. A 100 kHz RBW on a 50 MHz span sweeps quickly; a 1 kHz RBW takes much longer but reveals fine detail.
5. **Adjust DUT** (variable caps, tuning slugs, etc.) and watch the trace update live.
6. **Set markers** for the key frequencies: center, 3 dB points, 30 dB points, harmonic frequencies.

## TG output level — keep it low

The TG output level matters for two reasons:

- **Don't overdrive the DUT** — a TG at 0 dBm into a tiny crystal filter can saturate it and make the response curve look distorted. Start at -20 dBm and increase only if needed for SNR.
- **Don't overdrive the SA input** after the DUT — if the DUT is an amplifier with +30 dB gain and the TG is at 0 dBm, the SA sees +30 dBm and burns out. Pad appropriately.

A safe starting point is **-20 dBm TG output**, with attenuators added after gain stages as needed.

## Cheap-end TG options

- **TinySA Ultra** ($130) — built-in TG with output -47 to -10 dBm, very useful for HF / VHF / UHF amateur filter work.
- **NanoVNA in S21 mode** — not exactly a TG, but functionally equivalent for amplitude-only measurements: port 1 sources, port 2 receives, the sweep is correlated. Works to ~1.5 GHz reliably.
- **Used HP 8753C VNA** — way overkill but provides amplitude AND phase, much better dynamic range than a TG-equipped SA.

## Common mistakes

- **Forgetting to normalize.** The TG output isn't perfectly flat. Without normalization, you're measuring DUT + TG response combined, which biases the readings.
- **Wrong RBW for the sweep speed.** Too-narrow RBW + too-fast sweep = the SA filter hasn't settled before moving on, so the trace under-reads. The SA will warn ("UNCAL" indicator on lab gear).
- **Saturating the DUT.** Especially for high-Q filters where small drive levels matter.
- **Saturating the SA after a gain stage.** Forgetting to add padding when measuring amplifier gain.
- **Confusing return loss with insertion loss.** A coupler measurement gives return loss (reflection); a thru measurement gives insertion loss (transmission). They look similar on the screen but mean opposite things.

> **Advanced —** For real engineering work on a filter, you want both amplitude *and* phase response. A scalar TG/SA combo only gives amplitude. The phase response matters for cascaded filters (group delay distortion), for matching network design (where you need to know the DUT's reactive component), and for pulse-fidelity work (CW keying through a filter). Step up to a VNA when phase information becomes load-bearing.

## See also

- §25-03 — Spectrum Analyzers (TG host)
- §25-01 — NanoVNA Advanced (S21 measurements, the VNA alternative)
- §09-15 — NanoVNA Trim Workflow
- §17 — Formulas (return loss / SWR / dB)
