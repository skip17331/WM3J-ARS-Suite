---
id: 09-14
title: Magnetic Loop
chapter: 09
section: 14
level: advanced
status: draft
---

# Magnetic Loop

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What it is

A **magnetic loop** is a small, single-turn loop antenna — typically 0.05 to 0.25 wavelengths in circumference — tuned to resonance with a high-Q variable capacitor. Despite its small size (1–4 ft diameter on HF), a well-built mag loop can perform comparably to a full-size dipole at the same height.

The catch: very narrow bandwidth (typically 30–80 kHz at 14 MHz) and dangerous high voltages across the tuning capacitor at full power. Mag loops live up to their reputation as the **stealth antenna** of choice for apartments, attics, and HOA-restricted yards.

## How it works

A loop with circumference much less than a wavelength has very low radiation resistance (fractions of an ohm) but very high inductive reactance (hundreds of ohms). Tuned with a series capacitor that cancels the inductive reactance, the loop becomes resonant and presents a usable impedance to a feed.

```
loop inductance L ≈ µ₀ × π × b × (ln(8b/a) − 2)

where:
  b = loop radius (m)
  a = conductor radius (m)
```

The required tuning capacitor:

```
C = 1 / (4π² · f² · L)
```

The resulting **Q is very high** (200–500 for practical loops), which gives:

- **Narrow bandwidth** — BW = f / Q  →  35 kHz at 14 MHz with Q = 400
- **Voltage step-up** — Voltage across the cap = √(P × Q × X_L)
- **Electrical "smallness"** — the loop's actual radiation resistance is tiny, but with high Q, useful efficiency emerges

The voltage at the capacitor for a 100 W TX, Q = 400 loop with X_L = 250 Ω is:

```
V_cap = √(100 × 400 × 250) = √(10,000,000) ≈ 3160 V peak
```

— hence the requirement for **vacuum capacitors** at any non-trivial power.

A **coupling loop** about 1/5 the diameter of the main loop, mounted concentric, transforms the main loop's impedance to ~50 Ω.

## Calculator inputs and outputs

The Antenna Workshop calculator (`Magnetic Loop`) takes:

- **Operating frequency** (MHz)
- **Loop diameter** (ft)
- **Conductor diameter** (in) — the pipe / tubing
- **Transmit power** (W)

And returns:

- Loop circumference and circumference / wavelength ratio
- Loop inductance (µH)
- Required tuning capacitance (pF)
- Estimated Q
- 3-dB bandwidth at the operating frequency
- Voltage across the tuning capacitor at full power
- Recommended capacitor voltage rating

## Worked example — 3 ft diameter mag loop on 20m at 25 W

```
freq = 14.175 MHz
diameter = 3 ft (0.91 m)
conductor = 0.5 in copper pipe (0.013 m)
power = 25 W

circumference = π × 0.91 = 2.86 m
circumference / λ = 2.86 / 21.16 = 0.135  (good — between 0.1 and 0.2)

L = µ₀ × π × 0.455 × (ln(8 × 0.455 / 0.0064) − 2)
  ≈ 1.2 µH (Wheeler approx)

C = 1 / (4π² × (14.175e6)² × 1.2e-6)  ≈ 105 pF

X_L = 2π × 14.175e6 × 1.2e-6 = 107 Ω
Q ≈ 400 (rough — depends on conductor losses)
V_cap = √(25 × 400 × 107) = ~1030 V peak

Vacuum capacitor recommended for any margin.
3-dB bandwidth = 14.175 MHz / 400 = 35 kHz
```

A 3-ft mag loop with a vacuum capacitor and a 5-inch coupling loop is a complete, stealth-friendly 20m antenna for ~$200–400 in materials. Bandwidth is narrow enough that you'll re-tune frequently.

## Common mistakes

- **Air-variable capacitor for transmit.** Standard air-variable caps from old AM tube radios are rated for ~500 V max. Mag loop caps see 1–5 kV at modest power. **Vacuum capacitor is mandatory** beyond QRP.
- **Cheap connector at the cap.** The tuning point is the highest-voltage spot in the antenna; spark-gap arcing is common at high-power runs.
- **Loop too small.** Below 0.05 λ circumference, efficiency drops to <10%. At 0.1 λ, ~40%. At 0.2 λ, ~80%. Aim for 0.15–0.25 λ.
- **Wrong coupling-loop size.** Too small → too high SWR; too large → too low SWR. Start with 1/5 of main loop diameter and trim experimentally.
- **Mounting too close to ground.** Mag loops want to be 6–10 ft above ground for cleanest pattern. Indoor installations couple to the floor / ceiling and detune.
- **Using the loop as a bench-tested antenna.** A loop's Q and tuning shift dramatically when moved indoors / outdoors. Verify final tuning at the operating location.

> ⚙️ **Advanced —** A small mag loop's radiation pattern is bidirectional in the plane of the loop — figure-8 with deep nulls perpendicular to the loop's plane. This is occasionally useful for nulling out a specific noise source. The pattern's nulls are sharp (>30 dB at the null direction) for properly-built loops. Larger loops (approaching ½λ) become more like full-wave loops with omnidirectional patterns and lower Q.

## Build & trim notes

1. **Start with the loop.** Bend a 3 ft length of 1/2" copper pipe (or use a flex copper coil) into a circle. Solder the ends to a vacuum capacitor's terminals.
2. **Wind a coupling loop** of insulated wire (~5 turns of magnet wire, or a small copper ring) about 1/5 of main loop diameter. Mount it inside or just outside the main loop, concentric.
3. **Connect the coupling loop to coax** — feed point of the antenna.
4. **Mount on a non-conductive support** at intended operating height.
5. **Tune with a NanoVNA** — adjust the capacitor for SWR minimum at the desired frequency.
6. **Re-tune for each band change** — the cap's setting will be different; high-Q means narrow bandwidth.

## See also

- §06-05 — Magnetic Loops (theory chapter)
- §09-00 — Antenna Workshop overview
- §17-03 — Reactance
- §17-05 — Resonant Frequency
- §17-11 — Q Factor
- §17-12 — Bandwidth
