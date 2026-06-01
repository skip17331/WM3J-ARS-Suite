---
id: 09-03
title: Inverted-V Dipole
chapter: 09
section: 03
level: simple
status: published
---

# Inverted-V Dipole

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What it is

The inverted-V is a flat dipole with the apex (center / feedpoint) at a single tall support, and the two legs sloping down to stakes or low anchor points. It needs **only one tall support** instead of two — making it the natural choice when you have one good tree, one mast, or one tower.

The pattern is similar to a flat dipole but slightly more omnidirectional, with a small reduction in gain at low takeoff angles. Feed impedance drops to about 50 Ω at typical droop angles, often eliminating the need for a tuner.

## How it works

When the dipole legs slope down rather than running horizontally, two things happen electrically:

1. The **legs couple to each other** through the ground at the tips, slightly raising the resonant frequency. To compensate, the antenna is cut **3–5% shorter** than a flat dipole at the same frequency.
2. The **feed impedance drops** because the legs are closer together — typically from ~70 Ω flat to ~50 Ω at 30°–45° droop.

The empirical correction:

```
length_inverted_v = length_flat × (1 − 0.03 × angle/45°)
```

So at 30° droop, the antenna is ~2% shorter than a flat dipole; at 45°, ~3% shorter.

A 1:1 current balun at the apex is mandatory — the geometry encourages common-mode current on the coax shield, which a balun blocks.

## Calculator inputs and outputs

The Antenna Workshop calculator (`Inverted-V Dipole`) takes:

- **Frequency** (MHz) — band center
- **Apex height** (ft) — height of the center support
- **Droop angle** (degrees from horizontal) — typically 30–45°

And returns:

- Total wire length (corrected for droop)
- Each leg's slope length
- Tip height above ground (for safety check)
- Expected feed Z and balun recommendation

## Worked example — 40m inverted-V on a 35 ft mast

```
freq = 7.150 MHz
apex height = 35 ft
droop angle = 30°

flat-dipole length = 468 / 7.150 = 65.45 ft
correction at 30° = 1 − 0.03 × (30/45) = 0.98
inverted-V length = 65.45 × 0.98 = 64.14 ft

each leg slope = 64.14 / 2 = 32.07 ft
tip height = 35 − 32.07 × cos(60°) = 35 − 16 = 19 ft above ground
```

The tips are well above head-height, the apex is on the mast, and the antenna only needs **one 35-foot support** plus two stakes for the legs.

Expected feed Z is ~50 Ω at 30° droop — direct match to 50 Ω coax through a 1:1 balun.

## Common mistakes

- **Steep droop angles (>60°).** The antenna becomes too vertical: feed Z drops below 30 Ω, takeoff angle climbs, gain drops.
- **Tip too low to ground.** The high-voltage ends arc to grass or vegetation, especially in wet weather. Keep tips at least 8 ft up; 10+ ft is better.
- **Asymmetric installation.** If one leg droops 30° and the other 45°, the antenna develops a directional pattern and detunes asymmetrically.
- **Skipping the balun.** Common-mode current on the coax is significantly worse for inverted-V than flat dipole geometry.
- **Trimming for a flat-dipole length.** Will resonate ~3% high. Account for the droop correction.

> **Advanced —** The inverted-V has slightly lower peak gain than a flat dipole at the same height (~0.5 dB) but more uniform azimuth coverage. Pattern is omnidirectional in the horizontal plane vs. the flat dipole's slight figure-8. For low-angle DX work, prefer flat dipole at full λ/4+ height; for general regional / NVIS work, inverted-V is often the better choice.

## Build & trim notes

1. **Pre-cut wire about 5% long** as with a flat dipole — easier to trim shorter than splice.
2. **Mount the apex first**, then attach the legs and stake them at the desired droop angle.
3. **Sweep with the antenna at full installed height.** SWR and resonance depend strongly on tip-to-ground distance.
4. **Trim symmetrically** from both ends; ~1 cm = ~10 kHz shift on 20m, ~3 kHz on 40m.
5. **Stake the legs to non-conductive supports** (wooden stakes, fiberglass spreaders) — metal stakes detune.

## See also

- §06 — Antennas (theory chapter)
- §09-00 — Antenna Workshop overview
- §09-02 — Flat Dipole (the no-droop version)
- §09-15 — NanoVNA Trim Workflow
- §17-06 — Wavelength
