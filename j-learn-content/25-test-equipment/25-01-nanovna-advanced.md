---
id: 25-01
title: NanoVNA — Advanced Techniques
chapter: 25
section: 01
level: advanced
status: published
---

# NanoVNA — Advanced Techniques

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

This section assumes you have read **§09-15 NanoVNA Trim Workflow** and can already do a basic SOL (short-open-load) calibration, sweep an antenna, and read SWR / R / X. Here we go deeper: what the calibration actually means, how to interpret the Smith chart for matching-network work, how to use the time-domain mode as a TDR, and the saturation / overdrive gotchas that make some readings worse than useless if you don't know about them.

## The calibration kit and what it actually does

A NanoVNA ships with three (sometimes four) calibration "standards":

- **Open** — an unterminated SMA female. Reflects everything in phase. ρ = +1.
- **Short** — an SMA female with the center pin shorted to the shield. Reflects everything 180° out of phase. ρ = -1.
- **Load** — a precision 50 Ω termination. Absorbs everything. ρ = 0.
- **Thru** (sometimes) — a male-male barrel that connects the two ports for S21 (transmission) calibration.

Calibration measures the analyzer's known-imperfect response when looking at each known standard, then computes correction coefficients that cancel out:

- **Directivity error** — coupler leakage from the source port that pretends to be reflection
- **Source match error** — re-reflections at the source coupler
- **Reflection tracking error** — frequency-dependent gain/phase of the reflection path

After calibration, the analyzer reports the true reflection at the **calibration reference plane** — the point where the open/short/load were connected.

> **Advanced —** The standards are themselves imperfect. A real "short" has a small offset delay (the length from the SMA face to the actual short) and a tiny inductance. A real "open" has fringing capacitance. Professional calibration kits ship with measured polynomial coefficients for each standard. The NanoVNA assumes "ideal" standards, which is part of why its accuracy degrades above ~1.5 GHz. For HF amateur work, this error is negligible. For 23 cm / microwave work, it matters and is the main reason to step up to a calibrated lab VNA.

## The reference plane and port extension

The "reference plane" is **wherever your calibration ended**. If you SOL-calibrated at the end of a 6 ft coax pigtail, the reference plane is at the end of that pigtail. Anything you connect after that point is what gets measured.

This matters because almost nobody calibrates at the antenna feedpoint. You calibrate in the shack and walk the NanoVNA out to the antenna, or use a short jumper. Two ways to handle the extra cable:

### Method 1 — Calibrate through the jumper

Bring the SOL standards to the antenna. SOL-calibrate at the very end of the jumper. Now the jumper is part of the analyzer system and is fully corrected for.

This is the right answer if you can do it. The downside: you need the standards at the antenna location, in the rain, at the top of the tower, in the field.

### Method 2 — Port extension (electrical delay)

SOL-calibrate at the analyzer's SMA port, then tell the analyzer "there's X meters of coax after my port" and let it mathematically remove that cable's phase shift. Most NanoVNA firmware (NanoVNA-H4 stock, NanoVNA-Saver, NanoVNA-App) supports this under **"electrical delay"** or **"port extension."**

Procedure:

1. SOL-cal at the analyzer port.
2. Connect the jumper cable, **leave the far end open** (or shorted).
3. Sweep. Look at the Smith chart. An open jumper traces an arc — you want to electrically "rotate" that arc back to the open point (right side of the chart, ρ = +1) at all frequencies.
4. In NanoVNA-Saver, adjust "electrical delay" (in ps or ns) until the trace collapses to the open / short point.
5. The delay you found is the cable's electrical length. Save it; reuse it.

Now any measurement is referenced as if the analyzer were at the far end of the cable. The cable's loss is *not* compensated, only its phase / delay — but for HF cables a few meters long the loss is well below 1 dB and usually ignorable.

## Cable velocity factor measurement

You can use the time-domain mode (next section) or the open-trace method (above) to measure a cable's **velocity factor (VF)** — the fraction of the speed of light that signals propagate at in that cable.

Method:

1. Cut a known length of cable (say, **10.00 m measured with a tape**).
2. SOL-cal the analyzer.
3. Connect the cable. **Leave the far end open.**
4. Switch to time-domain mode (low-pass step).
5. Find the reflection peak — the open at the far end. Read its **time** value from the display, in ns. Call this `t_round` (round-trip time).
6. One-way time: `t_one = t_round / 2`.
7. Velocity in cable: `v = length / t_one`.
8. Velocity factor: `VF = v / c` where `c = 0.2998 m/ns`.

Example: 10 m of cable, `t_round` = 100 ns, so `t_one` = 50 ns, `v` = 10 m / 50 ns = 0.2 m/ns, `VF` = 0.2 / 0.2998 ≈ **0.667** (typical RG-58/U).

Now you have the VF for **your specific cable lot**, not the manufacturer's nominal value. This matters when you're cutting **stubs** to specific electrical lengths — a 5% VF error means a 5% length error, which on a 1/4-wave 144 MHz stub is 2.5 cm.

## Time-domain mode (the NanoVNA as a TDR)

The NanoVNA's time-domain mode synthesizes a TDR response by computing the inverse FFT of the frequency-domain reflection. You see a trace of reflection vs. distance along the cable.

Two modes:

- **Low-pass step** — simulates a step input. Easiest to read: opens show as a positive step up, shorts as a negative step down, mismatches as intermediate steps.
- **Bandpass impulse** — narrower-band, used for measuring discontinuities at specific frequencies. Less common for cable-fault hunting.

A clean cable shows:

```
amplitude
   │
   │     incident                 reflection
   │     ┐                        ┌─────────  (open)
1.0│─────┘
   │
   │
 0 │
   │
-1.0│                              └─────────  (short, if shorted instead)
   │
   └──────────────────────────────────────── time / distance
   0 ns                          50 ns
   0 m                           ~5 m (depends on VF)
```

A cable with a fault in the middle:

```
amplitude
   │              partial reflection
   │              ┌────             ┌────────  (open at far end)
   │              │   └─────────────┘
1.0│──────────────┘
   │              ▲                 ▲
   │              fault at ~2.5 m   end at ~5 m
   └──────────────────────────────────────── distance
```

Reading faults: an open at distance `d` shows as a step up at time `2d/v`. A short shows as a step down. A connector with a small impedance bump shows as a small bidirectional ripple. Water in a connector typically shows as a **partial dip** then return to nominal — the wet section is locally low-Z.

The NanoVNA's distance resolution is roughly `c × VF / (2 × span)`. With a 0–900 MHz sweep and VF=0.66, that's about **11 cm**. You can locate a fault to within ~10 cm in HF/VHF coax — good enough to find the bad connector or the chewed-through spot.

See **§25-07 TDR** for the dedicated TDR section that covers fault hunting in more depth.

## S21 (thru) measurements

The second port on a NanoVNA is a receive port that can measure **S21 — the transmission coefficient through a two-port network**. With both an SOL reflection cal and a "thru" cal, you can measure:

- **Filter response** — sweep a low-pass / band-pass filter; see the actual rejection in dB
- **Amplifier gain** — verify a preamp's gain vs. frequency
- **Cable loss** — direct measurement of insertion loss
- **Trap resonant frequency** — series resonant traps show a deep notch in S21

Procedure:

1. SOL-cal port 1 (reflection).
2. Connect port 1 directly to port 2 with a male-male barrel. Press "ISOLN" / "THRU" — this calibrates the thru path.
3. Disconnect the barrel. Insert the device under test (DUT) between port 1 and port 2.
4. Read S21 in dB at each frequency.

> **Advanced —** The NanoVNA's S21 dynamic range is limited — typically ~70 dB at HF, dropping to ~40 dB above 1 GHz. This is *not* a deficiency to fix; it's the architecture (single-conversion receiver, no preselection). For measuring filter stopbands that exceed 50–60 dB you need a real network analyzer. The TinySA Ultra used as a "poor man's scalar network analyzer" can get a bit further by adding receiver gain.

## Smith chart interpretation depth

The Smith chart is a polar plot of the complex reflection coefficient `ρ = (Z - 50) / (Z + 50)`. Real values on the horizontal axis, imaginary values arcing top and bottom. Beyond the basic "we want to be near the center" guidance from §09-15:

- **Top half** of the chart is inductive (positive reactance, +jX).
- **Bottom half** is capacitive (negative reactance, -jX).
- **Right side** is high impedance (open at the extreme right).
- **Left side** is low impedance (short at the extreme left).
- **Center** is 50 + j0 Ω — perfect match.

Common antenna traces:

| Trace pattern | Meaning |
|---------------|---------|
| Loop crosses through center | Resonant antenna; size of loop = bandwidth |
| Spiral inward | Lossy antenna (or long lossy feedline); SWR low but radiation low too |
| Loop never near center | Mismatched feed Z; need matching network or geometry change |
| Loop in upper half only | Antenna is electrically too long across the band |
| Loop in lower half only | Antenna is electrically too short across the band |
| Tight cluster near edge | Very high Q, narrow-band antenna (small loop, etc.) |

Matching-network design is essentially: read where the trace is, then add reactive components (Ls and Cs) that slide the trace toward center on the chart. Each component traces out a known arc — series L rotates clockwise around constant-resistance circles, shunt C rotates counterclockwise around constant-conductance circles, and so on. NanoVNA-Saver and other PC apps will overlay match-component arcs in real-time so you can design the network graphically.

See §17-13 (Smith Chart Basics) for the geometry; the NanoVNA gives you the chart and live tracking.

## Saturation, overdrive, and other lies the NanoVNA can tell

The NanoVNA's source level is around **-13 dBm to -10 dBm** (about 50 μV across 50 Ω, or roughly 0 dBμV). That's a small signal. Two consequences:

### 1. External RF can swamp the receiver

If you're standing in a strong RF field — under an AM broadcast tower, near a 1500 W contest station, on a tower with HF antennas active — the NanoVNA's receiver can be overdriven by the ambient field instead of seeing its own reflection. Symptoms: noisy traces, impossible-looking impedances, sweep-to-sweep drift.

Fix: turn off nearby transmitters, move the analyzer, or add a small (~10 dB) attenuator between analyzer and DUT to swamp ambient pickup.

### 2. Active devices can saturate the source

A NanoVNA's source can't drive much. Trying to measure a high-power amplifier's input return loss with the amp powered on can damage the analyzer (the amp's bias may leak back into the source). Always **power the DUT off** unless you specifically know it's safe.

For S21 measurements through an amplifier you must use a **directional coupler and external attenuator** ahead of the analyzer's RX port — see EMRFD chapters on amplifier characterization.

### 3. Above 1.5 GHz, the harmonic-rich source produces fake resonances

The cheap NanoVNA source is a square wave with harmonic content. Above its fundamental range, the analyzer extends coverage by **using the source's odd harmonics**. This works but is noisier and prone to spurious-looking resonances. If a sweep above 1.5 GHz shows strange dips that don't correlate with anything physical, suspect the harmonic-extended range and confirm on a real VNA.

## Common mistakes

- **Skipping calibration when switching cable.** Every adapter, every jumper change, invalidates the cal. Re-cal at the new reference plane.
- **Calibrating with stale standards.** If the SMA female of your "short" standard is bent or oxidized, the cal will encode the damage. Replace standards if dropped or visibly damaged.
- **Trusting readings on a moving antenna.** Wind moves wires; sweeps blur. Sweep on calm days.
- **Confusing the chart center with "good."** Center = 50 Ω, but a dummy load is 50 Ω and doesn't radiate. SWR alone can't distinguish a good antenna from a lossy one.
- **Not saving the calibration file.** Most NanoVNA models support saving cal sets to internal slots. After a successful cal, save it — recalling is faster than recalibrating.

## See also

- §09-15 — NanoVNA Trim Workflow (the basic workflow this section builds on)
- §25-07 — TDR — Time-Domain Reflectometry (using the time-domain mode for cable faults)
- §25-09 — Calibration Workflows (general calibration discipline)
- §17-13 — Smith Chart Basics
- §17-07 — SWR
- §10 — Feedline & SWR
