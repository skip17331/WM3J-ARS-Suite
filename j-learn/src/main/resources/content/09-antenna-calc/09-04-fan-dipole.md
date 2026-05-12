---
id: 09-04
title: Fan Dipole
chapter: 09
section: 04
level: mixed
status: draft
---

# Fan Dipole

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What it is

A fan dipole is several half-wave dipoles for different bands, all fed from **one common feedpoint**. Each band has its own pair of legs, with the legs spread at the tips like a fan to keep them from touching. On the band where a particular pair is resonant, that pair radiates; the other pairs look like high-impedance opens and don't load the feedpoint.

Result: a multi-band antenna with **direct 50 Ω feed and no traps**. Tradeoffs: more wire (one dipole per band), more space at the tips, and noticeable mutual coupling between elements that shifts each band's resonance.

## How it works

When two half-wave dipoles share a feedpoint, the closer they are, the more they couple to each other. The coupling has two effects:

1. **Resonant frequency shift** — adjacent elements detune each other slightly. The "shorter" element (higher freq) appears slightly inductive at the longer's resonance, pulling resonance up; the longer pulls the shorter's resonance down.
2. **Apparent length shortening** — to compensate, each element must be cut shorter than its standalone half-wave length.

The empirical shortening factor depends on tip separation:

| Tip-to-tip separation | Shortening factor (k) |
|----------------------:|----------------------:|
| 2 in | ~0.92 (8% shorter) |
| 4 in | ~0.95 (5% shorter) |
| 6 in | ~0.97 (3% shorter) |
| 12 in | ~0.99 (1% shorter) |

The bigger the separation, the smaller the coupling — but the more lateral space the antenna eats. 4–6 inches is a typical compromise.

Each element is fed in parallel through the same balun. Off-resonance elements present high impedance and don't disturb the active band's match.

## Calculator inputs and outputs

The Antenna Workshop calculator (`Fan Dipole`) takes:

- **Up to 4 band frequencies** (MHz)
- **End-to-end separation** (in) between adjacent dipole tips

And returns:

- Coupling correction factor
- Per-band total length and per-leg length
- Construction notes (single feedpoint, 1:1 balun)

## Worked example — three-band fan for 40 / 20 / 15m

```
band 1: 7.150 MHz
band 2: 14.150 MHz
band 3: 21.200 MHz
tip separation: 4 in → k = 0.95

40m: 468 / 7.150 × 0.95 = 62.20 ft total (31.10 ft each leg)
20m: 468 / 14.150 × 0.95 = 31.42 ft total (15.71 ft each leg)
15m: 468 / 21.200 × 0.95 = 20.96 ft total (10.48 ft each leg)
```

All three pairs share one center insulator, fed via a single 1:1 current balun to 50 Ω coax. Total wire used: ~115 ft.

Compared to a flat 40m dipole alone (~66 ft), the fan adds ~50 ft of wire and gives you 3 bands without a tuner.

## Common mistakes

- **Touching tips.** Each band's tips must be physically separated to keep coupling predictable. A spacer (PVC, fiberglass) at the ends keeps them apart.
- **Cutting all elements to standalone length.** Without the coupling correction, all three bands resonate too high. Apply k = 0.92–0.97 based on your spacing.
- **Trimming bands in random order.** Trim from **lowest-frequency band first** (longest wire). Each subsequent band is then trimmed with the lower-band wires already in place — they affect resonance.
- **Using a 4:1 or 9:1 balun.** All elements present 50–70 Ω resonant Z; a 1:1 current balun is correct.
- **Excessively wide separation thinking it solves coupling.** It does, but at >12 in tips you've used 25+ ft of horizontal space for marginal benefit.

> ⚙️ **Advanced —** Mutual coupling coefficient between two parallel half-wave dipoles separated by *d* (perpendicular spacing) follows the Carter / King–Sandler formulation. For d ≪ λ, coupling can exceed 0.5 (i.e., elements are strongly coupled); at d > λ/4, coupling drops below 0.1. The fan's tip-only spacing reduces *average* coupling but the closely-spaced center-insulator region still drives most of the interaction. Modeling in NEC-2 with both elements gives more accurate trim numbers than the empirical k.

## Build & trim notes

1. **Build all bands at once before trimming.** Coupling means trimming one element changes the others' resonance.
2. **Trim from longest to shortest.** Get the lowest-freq band resonant, then move up.
3. **Keep tip-spacing equal** along the entire span — uneven spacing produces unbalanced patterns.
4. **Use a single robust center insulator.** All bands meet at one point; commercial multi-band insulators (DX Engineering, MFJ) make this clean.
5. **A 1:1 current balun is mandatory** — fan geometry actively encourages common-mode current.

## See also

- §06 — Antennas (theory chapter on parallel-element interactions)
- §09-00 — Antenna Workshop overview
- §09-02 — Flat Dipole (single-band baseline)
- §09-05 — Trapped Dipole (alternative for multi-band — uses traps instead of fan)
- §09-15 — NanoVNA Trim Workflow
