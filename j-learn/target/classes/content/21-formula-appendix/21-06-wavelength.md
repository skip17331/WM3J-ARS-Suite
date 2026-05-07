---
id: 21-06
title: Wavelength
chapter: 21
section: 06
level: simple
status: draft
---

# Wavelength

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The wavelength of a radio wave is the physical distance one cycle covers as it propagates. Antennas are sized in fractions of a wavelength (half-wave, quarter-wave, full-wave loop), feedlines have transformations every half-wavelength, and field-strength meters care about the fraction of a wavelength they're away from a source.

If you know one number from this appendix by heart, this is the one.

## Equations

In free space (or air, close enough):

```
λ(m) = 300 / f(MHz)
λ(ft) = 984 / f(MHz)
```

The full physics:

```
λ = c / f
```

## Variables

| Symbol | Quantity | Units |
|--------|----------|-------|
| λ | Wavelength | meters (m) |
| f | Frequency | Hertz (Hz) |
| c | Speed of light | 2.998 × 10⁸ m/s in vacuum (≈ 300,000,000 for ham work) |

## In practical wire (with end effects)

A real half-wave dipole or quarter-wave vertical is **about 5% shorter** than the free-space half-wavelength because the wire's ends have capacitance to surrounding objects. This gives the famous formulas:

```
Half-wave dipole length (ft) = 468 / f(MHz)
Quarter-wave vertical length (ft) = 234 / f(MHz)
Half-wave dipole length (m) = 142.5 / f(MHz)
Quarter-wave vertical length (m) = 71.25 / f(MHz)
```

The 468 / 234 numbers assume thin wire, well above ground, away from other conductors. Real installations are within ±5% of these and are trimmed with an analyzer.

## In coax (with velocity factor)

A wavelength of RF inside coax is **shorter** than free-space wavelength by the cable's velocity factor (VF, typically 0.66–0.85; see §22-03):

```
λ_coax(m) = (300 / f(MHz)) × VF
```

Quarter-wave matching stubs and half-wave repeaters use the cable's λ_coax, not the free-space λ.

## Quick reference table — common ham frequencies

| Band | f (MHz, mid-band) | λ free-space (m) | λ free-space (ft) | ½λ dipole (ft) | ¼λ vertical (ft) |
|------|------:|------:|------:|------:|------:|
| 160m | 1.900 | 158 | 518 | 246.3 | 123.2 |
| 80m | 3.700 | 81 | 266 | 126.5 | 63.2 |
| 40m | 7.150 | 42 | 138 | 65.5 | 32.7 |
| 30m | 10.125 | 30 | 97 | 46.2 | 23.1 |
| 20m | 14.150 | 21 | 70 | 33.1 | 16.5 |
| 17m | 18.118 | 17 | 54 | 25.8 | 12.9 |
| 15m | 21.225 | 14 | 46 | 22.0 | 11.0 |
| 12m | 24.940 | 12 | 39 | 18.8 | 9.4 |
| 10m | 28.500 | 10 | 35 | 16.4 | 8.2 |
| 6m | 50.150 | 6.0 | 19.6 | 9.3 | 4.7 |
| 2m | 146.000 | 2.05 | 6.7 | 3.2 | 1.6 |
| 70cm | 446.000 | 0.67 | 2.21 | 1.05 | 0.52 |
| 23cm | 1296.000 | 0.23 | 0.76 | 0.36 | 0.18 |

## Worked example — quarter-wave matching stub on 20m

You want to match a 73 Ω folded dipole to 50 Ω coax at 14.150 MHz using a quarter-wave matching section. The matching section needs to be `Z = √(73 × 50) ≈ 60 Ω`. With RG-11 coax (75 Ω, VF = 0.66) you'd use:

```
Quarter wave in air: λ/4 = 300 / (4 × 14.15) ≈ 5.30 m
Inside RG-11 (VF = 0.66): 5.30 × 0.66 ≈ 3.50 m  (about 11.5 ft)
```

Cut a 11.5 ft length of 75 Ω cable, insert it between the antenna and the 50 Ω feedline, and you get a near-perfect impedance match at 14.150 MHz. (See §05-09 Smith charts and §05-11 impedance transformation for the geometry of why this works.)

## Common mistakes

- **Using the free-space formula for wire-cut antennas.** The 5% end-effect correction matters. Use 468 / 234 ft.
- **Using the free-space formula for coax stubs.** Coax has VF; multiply free-space λ by VF.
- **Mixing units.** "λ = 300 / f" gives meters when f is in MHz. "λ = 984 / f" gives feet when f is in MHz.
- **Forgetting to convert KHz / GHz.** 14150 (in kHz) divided into 300 gives 0.021 — meters or millimeters? Do the conversion to MHz first.

## See also

- §05 — Antennas (every antenna section uses these formulas)
- §08-01 — Dipole length calculator
- §08-03 — Quarter-wave vertical
- §22-03 — Velocity factor (where the VF correction comes from)
- §21-05 — Resonant Frequency (related — where reactance balances)
