---
id: 17-10
title: Decibels
chapter: 17
section: 10
level: simple
status: published
---

# Decibels

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The decibel is a logarithmic ratio. Every gain, loss, signal level, antenna pattern, S-meter reading, and cable spec in radio uses it. You don't need to memorize tables — you need to internalize four numbers.

## Equations

For power ratios:

```
ratio(dB) = 10 × log₁₀(P₂ / P₁)
```

For voltage / current ratios (when measured at the same impedance):

```
ratio(dB) = 20 × log₁₀(V₂ / V₁)
```

To convert dB back to a linear ratio:

```
P₂ / P₁ = 10^(dB / 10)
V₂ / V₁ = 10^(dB / 20)
```

## Variables

| Symbol | Quantity | Notes |
|--------|----------|-------|
| dB | Decibel ratio | dimensionless |
| P₁, P₂ | Powers being compared | watts (or any consistent unit) |
| V₁, V₂ | Voltages being compared | volts (must be at same impedance for the formula to apply) |

## The four numbers to memorize

| Power ratio | dB |
|------------:|---:|
| 2× | +3 dB |
| 10× | +10 dB |
| 100× | +20 dB |
| 1000× | +30 dB |

That's enough to estimate any dB / power conversion in your head.

## Combining the numbers

dB add when ratios multiply, so build up by combining:

```
20× = 10× × 2× = +10 + 3 = +13 dB
50× = 100× / 2× = +20 − 3 = +17 dB
4×  = 2× × 2×   = +3 + 3 = +6 dB
8×  = 2× × 2× × 2× = +3 + 3 + 3 = +9 dB
0.5× = 1/2× = −3 dB (loss)
0.25× = 1/4× = −6 dB
```

## Reference units (dB plus a "0 reference")

| Unit | What "0" means | Use |
|------|-----------------|-----|
| dBW | 1 W | Power ratios in absolute watts |
| dBm | 1 mW | Receiver sensitivity, signal levels |
| dBµV | 1 µV | RF measurement, EMI |
| dBi | Isotropic radiator | Antenna gain (vs. omnidirectional radiator) |
| dBd | Half-wave dipole | Antenna gain (vs. dipole) |
| dBc | Carrier reference | Spurious / harmonic levels |

```
0 dBm = 1 mW
30 dBm = 1 W
60 dBm = 1 kW
−100 dBm = 0.1 pW = approximate noise floor of a quiet receiver
```

## Worked example — converting between dB and watts

A rig outputs 100 W; what's that in dBW?

```
P(dBW) = 10 × log₁₀(100 / 1) = 10 × 2 = 20 dBW
```

100 W = 20 dBW. Equivalently 100 W = 50 dBm (50 dB above 1 mW).

Going the other way: a receiver's sensitivity spec is "−105 dBm." What does that mean in linear units?

```
P = 10^(−105/10) mW = 10^(−10.5) mW = 3.16 × 10⁻¹¹ mW = 3.16 × 10⁻¹⁴ W
```

About 32 femtowatts — vanishingly small. That's why receivers use selective filtering and low-noise amplifiers.

## Worked example — the S-meter scale

S-units on a receiver are *nominally* 6 dB apart (so each S unit is a 4× power increase). S9 is *nominally* −73 dBm on an HF rig calibrated to that standard.

| S-meter | dBm | µV (50 Ω) |
|---------|-----|-----------|
| S0 | −127 | 0.1 |
| S1 | −121 | 0.2 |
| S3 | −109 | 0.8 |
| S6 | −91 | 6.3 |
| S9 | −73 | 50 |
| S9+10 | −63 | 158 |
| S9+30 | −43 | 1.58 mV |

So "20 dB over S9" is roughly 1000× more power than S9 — almost always a strong local signal or a tropo-ducted DX. Note that S-meter calibration varies wildly between rigs; treat the scale as relative, not absolute.

## Common mistakes

- **Using 20 × log instead of 10 × log for power.** Voltage uses 20×; power uses 10×. Voltage doubles (factor of 2) is +6 dB; power doubles (factor of 2) is +3 dB.
- **Dropping the reference.** "S6" without saying what S-unit definition is meaningless. "−73 dBm" at the antenna terminal is exact.
- **Adding ratios instead of multiplying them.** When ratios multiply (cascaded gains/losses), dBs add. A 1 dB feedline loss + 6 dBd antenna gain = +5 dBd total. NOT 7 dB.
- **Computing in dBµV but converting incorrectly.** dBµV = 20 × log(V/1µV). 1 µV across 50 Ω = 0 dBµV ≈ −113 dBm. There's a fixed 107 dB difference between dBµV and dBm at 50 Ω (60 dB from "µV → V" and 47 dB from the resistance term).

> **Advanced —** dB is dimensionless because it's a ratio. The "dB" in dBm, dBW, etc. is technically referring to dB-relative-to-the-named-reference. Strictly speaking, you can't "add" dBm and dBm directly — that would mean "add the ratios," but each one's reference is 1 mW, so summing gives a meaningless number. To combine signals in linear power, convert each to mW first, sum, then convert back. The same caveat applies to averaging dB readings.

## See also

- §17-08 — ERP (uses dB throughout)
- §17-09 — Feedline loss (cable specs in dB / 100 ft)
- §17-12 — Bandwidth (uses 3-dB and 6-dB points)
