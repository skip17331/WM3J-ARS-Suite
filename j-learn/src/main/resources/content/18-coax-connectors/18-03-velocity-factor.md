---
id: 18-03
title: Velocity Factor
chapter: 18
section: 03
level: mixed
status: draft
---

# Velocity Factor

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The wave traveling inside a coax does **not** move at the speed of light. It moves at the speed of light in vacuum *multiplied by* a factor less than 1 — the velocity factor (VF), determined by the dielectric between the inner and outer conductors. Solid polyethylene gives VF ≈ 0.66; foam dielectrics give VF in the 0.78–0.88 range; air-spaced gives VF up to 0.95.

This affects you whenever you need an **electrical length** of cable to do a specific job: a quarter-wave matching section, a phasing line, a half-wave repeater of antenna impedance, a stub. The physical length is the electrical length divided by the VF.

## Reference VF table

| Cable | Dielectric | VF |
|-------|-----------|------|
| RG-58, RG-8, RG-213 | Solid polyethylene | 0.66 |
| RG-214 | Solid polyethylene | 0.66 |
| RG-8X / Mini-8 | Foam polyethylene | 0.78–0.84 (varies by maker) |
| 9913 (Belden) | Air-spaced PE | 0.84 |
| 9914 (Belden) | Foam polyethylene | 0.82 |
| LMR-400 | Foam PE (closed-cell) | 0.85 |
| LMR-400-UF | Foam PE | 0.83 |
| LMR-600 | Foam PE | 0.87 |
| LDF4-50A (1/2" hardline) | Foam PE | 0.88 |
| LDF5-50A (7/8" hardline) | Foam PE | 0.89 |
| Heliax (Andrew, generic) | Foam | 0.85–0.90 |
| RG-6 (TV, 75 Ω) | Foam PE | 0.83 |
| RG-11 (TV, 75 Ω) | Foam PE | 0.83 |
| 450 Ω window line | Mostly air | 0.91 |
| 300 Ω twin-lead (TV) | Solid PE | 0.82 |
| 600 Ω open-wire | Air | 0.95 |
| Bare-wire dipole (in air) | Air | 0.95–0.97 |

VF is determined by the dielectric's relative permittivity (ε_r) according to:

**VF = 1 / √ε_r**

Air: ε_r = 1.0006, VF ≈ 1.
Solid PE: ε_r = 2.25, VF = 0.667.
Foam PE: ε_r = 1.4–1.6, VF = 0.79–0.85.

## Computing electrical lengths

For a cable with velocity factor VF, the wavelength **inside the cable** at frequency f is:

**λ_cable (m) = (300 × VF) / f(MHz)**

Or in feet:

**λ_cable (ft) = (984 × VF) / f(MHz)**

A quarter-wave is λ/4; a half-wave is λ/2; etc.

### Common-fraction lookup

For RG-58/RG-213 (VF = 0.66) at the band centers:

| Band | Quarter-wave | Half-wave |
|------|--------------|-----------|
| 80 m (3.65 MHz) | 44.4 ft | 88.9 ft |
| 40 m (7.15 MHz) | 22.7 ft | 45.4 ft |
| 30 m (10.1 MHz) | 16.0 ft | 32.1 ft |
| 20 m (14.2 MHz) | 11.4 ft | 22.9 ft |
| 17 m (18.1 MHz) | 9.0 ft | 18.0 ft |
| 15 m (21.2 MHz) | 7.6 ft | 15.3 ft |
| 12 m (24.9 MHz) | 6.5 ft | 13.0 ft |
| 10 m (28.5 MHz) | 5.7 ft | 11.4 ft |
| 6 m (50.1 MHz) | 3.2 ft | 6.5 ft |
| 2 m (146 MHz) | 1.1 ft | 2.2 ft |
| 70 cm (446 MHz) | 0.36 ft | 0.73 ft |

For LMR-400 (VF = 0.85), multiply each value by 0.85/0.66 = 1.288.

For 75-Ω TV coax (VF = 0.83), multiply by 0.83/0.66 = 1.258. (Useful for quarter-wave 75-Ω matching sections.)

The J-Hub feedline calculator (in §09) does these calculations interactively — input cable type, frequency, fraction; get physical length.

## Why the electrical length matters

Three common uses of electrical-length-tuned cable:

### 1. Quarter-wave matching section

A quarter-wave length of transmission line transforms impedance via:

**Z_in = Z₀² / Z_load**

For a quad antenna at ~120 Ω and a target match to 50 Ω, you want Z₀ such that Z₀² / 120 = 50, giving Z₀ = √6000 = 77.5 Ω. Standard 75-Ω coax (close enough; gives 75² / 120 = 47 Ω, ~1.07:1 SWR) works well as a quarter-wave matching section.

The physical length must be a true quarter-wave **at the design frequency** in the cable's velocity factor. Cut to the table value above, then verify by sweeping with a NanoVNA — the dip should be at your target frequency.

### 2. Half-wave impedance repeater

A half-wave length of line **preserves the impedance** at both ends (regardless of Z₀). If you want to read your antenna's true feedpoint impedance at the rig, make your feedline an electrical half-wave on the operating band.

This is sometimes called the "1:1 half-wave transformer" trick. Useful for diagnostic SWR sweeps when you suspect feedline transformation is hiding the antenna's actual impedance.

### 3. Phasing harness for arrays

Multi-element phased arrays (two-element phased verticals, four-square arrays, etc.) need precise electrical-length cables to feed each element with the right phase. A 90° phase shift requires a quarter-wave; 180° requires a half-wave; 45° a 1/8-wave.

The physical lengths depend critically on the cable's actual VF — measure your batch, don't trust the spec to better than ±2%.

## Measuring VF on your actual cable

Manufacturer specs give nominal VF; your specific batch may differ ±2%. For critical length-tuned applications, **measure**:

1. Cut a sample of the cable to a known length, say 30 ft.
2. Connect one end to a NanoVNA in S11 mode; leave the other end open.
3. Sweep across HF/VHF.
4. Note the frequencies where SWR peaks (open-circuited line resonances) — these are odd quarter-wave multiples.
5. From the lowest such peak, compute: f_peak = c × VF / (4 × physical_length); rearrange to VF = (4 × f_peak × length) / c.

Example: 30-ft sample of RG-213, lowest open-end SWR peak at 5.4 MHz. VF = (4 × 5.4 × 30) / 984 = 0.66. Confirms the spec.

A NanoVNA's TDR mode does this automatically (it varies VF assumption to make the trace look right; you see the resulting VF on screen).

## VF and "the cable is 50 ft long" — but what's it really

Cable length matters for two purposes:

- **Physical routing**: how much you need to span. Just measure.
- **Electrical length**: how the wave is delayed and transformed. Use VF.

For most amateur use, the cable is significantly longer than the wavelength of interest, and you don't care about *exact* electrical length — you just care that the cable reaches and that the loss isn't excessive. **VF only matters when you're cutting cable for a specific phasing or matching purpose.**

## VF aging

Real cables experience **slight VF drift over years**:

- **Foam dielectric absorbs water**, increasing the average dielectric constant slightly, decreasing VF. Typically <1% over 10 years for protected cables; more for buried cables.
- **Solid PE is stable** — no significant VF drift in service.
- **Heat aging** of foam dielectrics can shift VF by 1-2% over decades.

For length-tuned cables (quarter-wave matching sections in service for 20+ years), it's worth re-sweeping occasionally to check whether the resonance has shifted. If it has, the cable's electrical length has changed, which means VF has changed, which means dielectric has aged — the section is approaching end of life.

## Common mistakes

- **Cutting cable based on physical length when electrical length is what matters.** A 22-ft "quarter-wave on 14 MHz" cable cut from RG-213 is an electrical 22 / 0.66 = 33 ft — it's actually a half-wave plus some, not a quarter-wave. Always multiply by VF.
- **Trusting "0.85" for all foam cables.** Belden 9913 is air-spaced (0.84), 9914 is foam (0.82), LMR-400 is closed-cell foam (0.85). They look similar; their VFs differ.
- **Using bare-wire VF (0.95) for coax-fed antennas.** A wire dipole's "antenna length" depends on the bare-wire VF; the coax to it is separate. They're independent.
- **Skipping the verify-by-sweep step.** Even with the right VF and length, your cut may be slightly off. Sweep and trim; don't trust calculation alone.

## See also

- §18-00 — Overview
- §18-02 — Loss tables
- §06-02 — Feedline effects (electrical length transformations)
- §06-03 — Impedance transformation
- §09 — Antenna calculator (computes lengths automatically)
- §17-09 — Feedline-loss formula in the formula appendix
