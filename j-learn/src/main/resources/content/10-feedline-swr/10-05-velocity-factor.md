---
id: 10-05
title: Velocity Factor
chapter: 10
section: 05
level: mixed
status: draft
---

# Velocity Factor

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The wave inside a coax travels at a fraction of the speed of light. The fraction is the **velocity factor (VF)**, determined by the dielectric between conductors. Solid polyethylene gives VF ≈ 0.66; foam dielectrics give VF in the 0.78–0.88 range. This affects the *electrical* length of any feedline-based device — quarter-wave matching sections, half-wave repeater sections, phasing harnesses, and stubs.

The full VF table and computation method are in §18-03. This section focuses on the SWR-and-feedline implications: how VF interacts with line-impedance transformation, where electrical length matters, and common pitfalls.

## VF basics

For a cable with VF ≤ 1, the wavelength inside the cable at frequency f is:

**λ_cable = λ_free / VF = (c × VF) / f**

In feet at f in MHz: **λ_cable (ft) = (984 × VF) / f**.

For RG-213 (VF 0.66) at 14 MHz: λ_cable = 984 × 0.66 / 14 = **46.4 ft**. Quarter-wave is 11.6 ft; half-wave is 23.2 ft.

For LMR-400 (VF 0.85) at the same frequency: λ_cable = 984 × 0.85 / 14 = **59.7 ft**. Quarter-wave 14.9 ft; half-wave 29.9 ft.

A quarter-wave LMR-400 section is **28% longer** than a quarter-wave RG-213 section at the same frequency.

## Why VF matters for SWR

VF affects the cable's **electrical length** at any given frequency. Electrical length determines:

1. **Where you measure SWR.** Feedline transforms impedance over its length (see §10-06); a half-wave electrical line preserves the antenna's true impedance at the rig end, but other lengths transform it. Knowing electrical length tells you what transformation to expect.
2. **Where matching networks resonate.** A quarter-wave matching section at 14 MHz needs a specific physical length — and that length depends on VF.
3. **Where stubs are tuned.** Quarter-wave shorted stubs (used in some matching schemes) need precise length.

## VF in matching sections

The classic application: **75-ohm matching section** between a 50-ohm coax and a high-impedance antenna (e.g., a quad or delta loop). For an antenna at ~120 Ω feed, a quarter-wave of 75-ohm coax transforms to:

**Z_in = Z₀² / Z_load = 75² / 120 ≈ 47 Ω**

Close enough to 50 to give acceptable SWR.

The quarter-wave length depends on the matching cable's VF (RG-11 75-Ω cable typically VF 0.66, RG-6 typically VF 0.83). Cut the matching section to the wrong physical length, and the impedance transformation is wrong, and your SWR is bad.

For a 14 MHz matching section using RG-11 (VF 0.66): λ/4 = 984 × 0.66 / 14 / 4 = **11.6 ft**.
For RG-6 (VF 0.83): λ/4 = 984 × 0.83 / 14 / 4 = **14.6 ft**.

Always **multiply by VF**.

## VF in phasing harnesses

Multi-element antenna systems sometimes use **electrical-length-tuned cables** to introduce phase delays between elements. A quarter-wavelength of feedline gives 90° phase shift; a half-wavelength gives 180°.

For example, two phased verticals fed in quadrature need feedlines that differ by exactly λ/4 in the cable. Cutting these to wrong lengths gives wrong phase shifts and wrong patterns.

The lengths depend on VF.

## How to verify VF on your specific cable

The published spec is approximate (typically ±2% from batch to batch). For critical applications:

1. Cut a sample of the cable to a measured length, say 30 ft.
2. Connect one end to a NanoVNA in S11 mode; leave the far end open.
3. Sweep the VNA across HF.
4. The first SWR peak (lowest frequency open-circuit resonance) corresponds to a quarter-wavelength.
5. From that frequency, compute: VF = (4 × f_peak × physical_length) / c.

Example: 30 ft of unknown cable. NanoVNA shows lowest open-end SWR peak at 5.4 MHz. VF = (4 × 5.4 × 10^6 × 30 × 0.305) / 3 × 10^8 = **0.66**. Consistent with solid PE dielectric.

NanoVNA's TDR display does this automatically — it varies VF assumption to make the trace's distance scale match a measured length, displaying the resulting VF on screen.

## Cable-aging effect on VF

Real cables drift slightly over years:

- **Foam dielectric absorbs water** under humid conditions, increasing the average dielectric constant. This decreases VF by 1-2% over decades.
- **Solid PE** is stable; no significant VF drift.
- **Direct-burial cables** absorb water faster; VF can drift 3-5% over a decade.

For length-tuned matching sections in service for 20+ years, it's worth re-measuring VF periodically. If VF has drifted, the matching section's frequency has shifted; you may need to re-cut.

## VF and propagation delay

VF also determines how long a wave takes to travel from one end of the cable to the other:

**Delay (μs) = length (ft) × 1.016 / VF**

(984 ft/μs in vacuum, divided by VF, gives delay per μs.)

For RG-213 (VF 0.66), 100 ft: 100 × 1.016 / 0.66 = **154 μs**.
For LMR-400 (VF 0.85), 100 ft: 100 × 1.016 / 0.85 = **120 μs**.

This propagation delay is normally small but matters for:

- **TDR measurements** (computing distance to a fault from time-of-arrival of reflection).
- **Fast switching** (e.g., transmitter PTT timing in some configurations).
- **Computer-control loops** (some CAT-controlled systems care about delays through the feedline; usually negligible).

## Cable types and their VFs (quick reference)

| Cable | Dielectric | VF |
|-------|-----------|------|
| RG-58, RG-8, RG-213 | Solid PE | 0.66 |
| RG-8X, RG-213-foam | Foam PE | 0.78–0.84 |
| 9913 | Air-spaced PE | 0.84 |
| 9914 | Foam PE | 0.82 |
| LMR-400 | Foam PE (closed-cell) | 0.85 |
| LMR-400-UF | Foam PE | 0.83 |
| LMR-600 | Foam PE | 0.87 |
| LDF4-50A (1/2" hardline) | Foam PE | 0.88 |
| LDF5-50A (7/8" hardline) | Foam PE | 0.89 |
| 450 Ω window line | Mostly air | 0.91 |
| 600 Ω open wire | Air | 0.95 |

(Full table in §18-03.)

## Common VF mistakes

- **Cutting "a 22-ft quarter-wave on 14 MHz" from RG-213.** Without VF correction, that's actually 22 × 0.66 = 14.5 ft electrical. The RIGHT length is 22 / 0.66 = 33 ft physical, no — wait, you want quarter-wave electrical, so physical = electrical × VF only if VF<1 means cable is "shorter than free-space" — actually you want physical > electrical (the cable's wave is slower, so you need more cable to make the same number of electrical wavelengths). Re-derive: λ_cable = c × VF / f; quarter-wave physical = λ_cable / 4 = c × VF / (4f) — for 14 MHz with VF 0.66, that's 0.66 × 984 / (4 × 14) = 11.6 ft. The free-space quarter-wave is 17.6 ft. The cable quarter-wave is **shorter** because the wave moves slower in the cable, so it traverses less physical distance per cycle.

(Correction noted — wrap the wave is slower in cable, so a wavelength fits in less cable distance. VF < 1 means physical length is shorter than free-space length for the same number of wavelengths. The 11.6 ft is correct.)

- **Trusting "0.85" for all foam cables.** Belden 9913 is air-spaced (0.84), 9914 is foam (0.82), LMR-400 is closed-cell foam (0.85). Look up your specific cable.
- **Using bare-wire VF (~0.95) for coax-fed antennas.** A wire dipole's electrical length depends on the bare-wire VF, but the coax to it is a separate calculation with the coax's own VF. The two are independent; don't confuse them.
- **Skipping the verify-by-sweep step.** Even with the right VF and length, your specific cut may be off by a few percent. Sweep with a NanoVNA and trim if needed.

## See also

- §10-00 — Chapter overview
- §10-01 — Coax loss by frequency
- §10-06 — Impedance transformation (where VF determines electrical length)
- §18-03 — Velocity factor (full reference table)
- §06-10 — Feedline effects
- §06-11 — Impedance transformation (Smith chart context)
- §09 — Antenna calculator (auto-computes lengths)
