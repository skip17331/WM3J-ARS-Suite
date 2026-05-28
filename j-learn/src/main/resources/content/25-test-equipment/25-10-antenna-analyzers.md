---
id: 25-10
title: Antenna Analyzers
chapter: 25
section: 10
level: mixed
status: draft
---

# Antenna Analyzers

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

An **antenna analyzer** is a small battery-powered instrument that tells you, at a glance, what the antenna and feedline look like at any frequency you pick — SWR, impedance, sometimes return loss, and on better models the resistive (R) and reactive (X) components separately. It's the instrument most amateurs reach for first when an antenna is misbehaving, and the instrument that turned antenna work from "tune and pray" into "measure and adjust."

A modern VNA (§25-01) does everything an antenna analyzer does and more, but analyzers are still in heavy use because they're:

- **Battery-powered and compact** — easy to carry up a ladder or out to a hilltop install.
- **Single-task simple** — knob, screen, reading. No PC, no calibration kit on every trip.
- **Cheap when you already have a VNA at home** — a $150 RigExpert lives in the antenna-work toolbox; the NanoVNA stays at the bench for serious diagnosis.

## What an antenna analyzer measures

An analyzer is, internally, a tiny low-power signal source (a few milliwatts) that sweeps the frequency you set, drives the antenna through a directional coupler or bridge, and measures the reflected signal. From that single measurement it derives:

| Quantity | Symbol | What it means |
|----------|--------|---------------|
| SWR | — | Standing-wave ratio. `1.0` = perfect match; `>3` is generally unacceptable on HF. |
| Return loss | RL (dB) | Same information as SWR in dB form. `20 dB` = excellent; `10 dB` = SWR ~2. |
| Resistance | R (Ω) | Real part of impedance. At resonance this dominates. |
| Reactance | X (Ω) | Imaginary part. `+X` = inductive (antenna too long); `−X` = capacitive (too short). |
| Magnitude | |Z| (Ω) | Total impedance magnitude `√(R² + X²)`. |
| Phase | θ (deg) | Phase of the reflection coefficient. Advanced use. |

Cheap analyzers (MFJ-259B, MFJ-269) show SWR + R + X. Better ones (RigExpert AA-230 / AA-650, SARK-110, Comet CAA-500) sweep a range and plot SWR or impedance on a small graphical screen. The most-capable models do **Smith-chart** plots and TDR cable-length measurement.

## Anatomy of an analyzer

```
  ┌─────────────────────────────────────────────────────────────┐
  │                                                             │
  │   ┌─────────────┐    ┌──────────────┐    ┌─────────────┐    │
  │   │  Signal     │───►│  Directional │───►│   To DUT    │    │ ←── SO-239
  │   │  source     │    │  bridge or   │    │ (antenna /   │
  │   │  (PLL/DDS)  │    │  coupler     │    │  feedline)  │
  │   └─────────────┘    └──────┬───────┘    └─────────────┘
  │                             │ reflected sample
  │                             ▼
  │                       ┌───────────┐
  │                       │ detector  │ ── reads forward & reflected
  │                       │ + ADC     │    voltages, computes R, X, SWR
  │                       └─────┬─────┘
  │                             ▼
  │                       ┌───────────┐
  │                       │  CPU /    │ ── displays on screen
  │                       │  display  │
  │                       └───────────┘
  └─────────────────────────────────────────────────────────────┘
```

The signal source is usually a programmable DDS or PLL synthesizer running at a few milliwatts (much less than your rig's lowest power level). The detector is a diode or log-amp pair that measures forward and reflected voltage; some analyzers use I/Q demodulation to recover phase, which is what lets them separate R from X.

## Reading the screen

A typical screen at one frequency:

```
   ┌──────────────────────────────────┐
   │ 14.175 MHz                        │
   │                                   │
   │  SWR  1.4                          │
   │  R    42 Ω                         │
   │  X   -18 Ω        ← capacitive     │
   │  |Z|  46 Ω                         │
   └──────────────────────────────────┘
```

Read it like this:

- **SWR 1.4** — usable; rig will run flat-out.
- **R 42 Ω** — close to 50 Ω; antenna is near resonance.
- **X −18 Ω** — slightly capacitive at this frequency, so the antenna is electrically a bit short. Adding ~3% to total length would push resonance down to 14.175 and zero the X.

If you'd swept the band and plotted it, you'd see a curve like:

```
SWR
3.0 ┤ ●                                    ●
2.5 ┤  ●                                  ●
2.0 ┤   ●                                ●
1.5 ┤    ●                              ●
1.2 ┤     ●●                          ●●
1.0 ┤       ●●●●●●●●●●●●●●●●●●●●●●●●●●
    └─────────────────────────────────────── freq
    14.0          14.2          14.4
                  ↑ resonance (lowest SWR)
```

The frequency at the lowest point of the SWR curve is the **resonant frequency** — that's where R is closest to 50 Ω and X passes through zero. The width of the dip tells you the antenna's bandwidth; sharp dips = high-Q narrow antennas (mobile whips, traps); broad dips = low-Q wide antennas (fan dipoles, EFHWs).

## Use cases

### 1. Trim a dipole or end-fed wire

The classic first job. Connect the analyzer at the feedpoint (not the shack — see "where to connect" below), sweep across the band, find the lowest-SWR frequency, and trim or extend the wire.

- **Resonance too high in frequency** (e.g. dipole shows lowest SWR at 14.300 but you want 14.200) → wire is too short. **Add length** — equal amounts each side for a center-fed dipole.
- **Resonance too low** → wire is too long. **Cut length** — small bites, recheck after each.

Rule of thumb on HF: 1% length change moves resonance by ~1% of frequency. So a 20 m dipole moving from 14.300 to 14.200 needs roughly +0.7% length, or ~7 cm per side on a 10.1 m half.

### 2. Verify SWR across the band

Sweep from the bottom to the top of the band and plot SWR. Identifies sharp resonances (good for narrow-band antennas) or broad ones (good for fan dipoles and EFHWs covering multiple segments). Catches a misbehaving balun or feedline as a flat-line high SWR everywhere.

### 3. Check feedline + antenna together

If you must measure from the shack instead of the antenna feedpoint, the analyzer will see the *combination* of feedline and antenna. SWR is preserved by a lossless line, but R and X are not — the line transforms them. So:

- **SWR sweep is still meaningful** — resonance still appears at the antenna's true resonant frequency.
- **R/X readings are NOT the antenna's** — they're the transformed values at the analyzer end. Don't tune for `X = 0` at the shack; tune for **SWR minimum**.

### 4. Identify cable problems

A dead-short or open at the far end of a coax gives a characteristic SWR pattern: SWR is infinite at the antenna, but lossy cable damps it down at the shack. A reasonable rule:

- SWR varies wildly with frequency in a periodic pattern → reflection from far end, possibly an open or short.
- SWR is high and flat across the entire range → the antenna may be disconnected, or there's a major fault at the feedpoint connector.
- SWR is moderate (1.5–3) and varies smoothly with frequency → the antenna is at least connected and probably has a real resonance somewhere outside your sweep range.

### 5. Tune a manual antenna tuner / matchbox

With the rig off and the analyzer plugged into the matchbox input, adjust the L and C until the analyzer shows 1.0:1. Then put the rig in and transmit at low power — no more guess-and-listen with the tuner. The tuner setting found at low signal level is exact for that frequency.

### 6. Find resonance on a mystery antenna

Connect to the feed of a random wire / mobile whip / experimental loop and sweep wide. The lowest SWR in the sweep is where the antenna wants to work. Some antennas have multiple resonances — multi-band wires, end-feds with traps — and the analyzer shows all of them.

## Where to connect the analyzer

Where you measure matters more than most operators realize. The same antenna can show SWR 1.2 from the shack and SWR 1.8 from the feedpoint, or vice versa, depending on feedline loss and length.

```
       ╱─────────╲   ← antenna at the top of a 12 m pole
      ╱           ╲
      ╲           ╱
       ╲ feedpt  ╱
        ┃ ●─────────── [ANALYZER A] best — directly at feedpoint
        ┃
        ┃ coax (say 25 m of RG-58)
        ┃
        ┃ ●─────────── [ANALYZER B] OK for SWR sweep; R/X readings
        ┃              are transformed
        ┃
       ─┴── shack
```

| Position | Pros | Cons |
|----------|------|------|
| **At feedpoint** | Reads the antenna's true R, X, SWR | Requires climbing or walking out to the antenna |
| **At shack end of feedline** | Convenient | Cable transforms R/X; lossy cable masks reflections from a bad antenna |

For a serious tune, climb up or bring the antenna down. For a sanity check or "did anything change since yesterday?" — shack end is fine.

> **Advanced —** A half-wavelength of feedline (counting velocity factor) transforms impedance back to itself. So if you tune at a frequency where the feedline is electrically `n × λ/2` long, the shack reading equals the feedpoint reading. This is occasionally useful, but only if you know the feedline's actual electrical length precisely — which you can measure with the analyzer's TDR mode on better units, or with a NanoVNA.

## Gotchas

- **Receiving broadcast RF.** Antenna analyzers are receivers as well as transmitters. A strong AM broadcast station can confuse the reading — you'll see SWR jumping around as the BC carrier modulates. Solution: try a different sweep range, or put the antenna up at night when broadcasters lower power.
- **DC on the line.** Some PoE injectors, lightning arresters with bias-T, or remote rotators push DC up the coax. **Disconnect the rig and any DC-injection device** before connecting the analyzer — the input is a diode bridge that doesn't like DC. Most analyzers fail silently and report nonsense if there's DC present.
- **Lightning protection.** Disconnect lightning arresters that bypass to ground at DC; their gas-discharge tubes are fine for RF but the analyzer's bridge can read low against them.
- **Battery state.** Low batteries cause the internal synthesizer to drift, leading to misaligned readings. If a sweep shows resonance moving 50 kHz between two sweeps minutes apart, charge the batteries.
- **Connector quality.** A loose PL-259 on the analyzer's SO-239 reads as a high-SWR antenna. Wiggle, tighten, and reseat before blaming the antenna.

## Antenna analyzer vs NanoVNA

Both measure impedance vs frequency. Differences in practice:

| Feature | Antenna analyzer | NanoVNA |
|---------|------------------|---------|
| Battery | Built-in NiMH or Li-ion | Built-in (smaller capacity) |
| Calibration | Factory; no field cal | OSLT cal required per frequency span |
| Smith chart | Some models | All models, large display |
| 2-port S-parameters | No | Yes (S21 cable loss, filter response) |
| Frequency range | Limited (often 1–230 MHz or 1–500 MHz) | 50 kHz – 1.5 GHz typical |
| Price | $130 (RigExpert AA-35 Zoom) – $500 (AA-1500) | $50 (NanoVNA-H) – $300 (LiteVNA, NanoVNA-F V3) |
| Ruggedness | Built for the field — climbing belts, ladder bags | Bench instrument; fragile screen, no protective case |
| Ease of use | One-knob simple | PC software (NanoVNA-Saver) significantly extends usability |

Most active amateurs end up owning both — the analyzer for routine antenna work on the ladder, the VNA for serious diagnosis at the bench.

## Calibration check

An analyzer is read from the SO-239 outward. Before each session:

1. **Open** — Disconnect from anything. SWR should read high (typically ∞ or "OPEN" on graphical units).
2. **Short** — Touch a screwdriver across center pin to shell. SWR should again read high (a short is also a perfect reflection).
3. **50 Ω load** — Screw on a known-good 50 Ω dummy load. SWR should read 1.0, R = 50 Ω, X = 0 across the analyzer's range.

If the 50 Ω load shows SWR 1.5 or R = 40 Ω, the analyzer is out of cal or has a problem. RigExpert and SARK units can be field-calibrated with a USB-connected PC; MFJ units must be returned to MFJ.

## See also

- §09-15 — NanoVNA Trim Workflow (the basic VNA-driven version of the same workflow)
- §10 — Feedline & SWR
- §17 — Formulas (SWR ↔ return loss math)
- §25-01 — NanoVNA — Advanced Techniques
- §25-07 — TDR — Time-Domain Reflectometry (cable length and fault location)
- §25-11 — Dip Meters (a related but very different LC-measurement tool)
