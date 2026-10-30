---
id: 18-02
title: Loss Tables
chapter: 18
section: 02
level: mixed
status: published
---

# Loss Tables

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

This section is the lookup table for **matched-line loss** of common amateur cables across HF, VHF, and UHF. Loss is measured in dB per 100 ft of cable, terminated in its characteristic impedance (50 Ω) at the operating frequency.

For *additional* loss caused by SWR mismatch, see §06-02. For the use of these numbers in feedline-system design, see §06-03. This section just gives you the numbers.

## Master loss table (dB per 100 ft, matched line)

| Cable | 1.8 MHz | 3.5 MHz | 7 MHz | 14 MHz | 28 MHz | 50 MHz | 144 MHz | 432 MHz | 1296 MHz |
|-------|---------|---------|-------|--------|--------|--------|---------|---------|----------|
| RG-58 | 0.4 | 0.6 | 0.9 | 1.6 | 2.5 | 3.5 | 6.6 | 12.0 | 22.5 |
| RG-58A/U | 0.3 | 0.5 | 0.7 | 1.2 | 1.9 | 2.8 | 5.2 | 9.5 | 18.0 |
| RG-8X / Mini-8 | 0.3 | 0.4 | 0.6 | 1.0 | 1.6 | 2.4 | 4.5 | 8.0 | 15.5 |
| RG-8 / RG-213 | 0.2 | 0.3 | 0.4 | 0.65 | 1.0 | 1.5 | 2.8 | 5.0 | 9.5 |
| RG-214 (silver braid) | 0.2 | 0.3 | 0.4 | 0.6 | 0.9 | 1.4 | 2.5 | 4.5 | 8.5 |
| 9913 (Belden) | 0.15 | 0.2 | 0.3 | 0.5 | 0.8 | 1.2 | 2.0 | 3.6 | 6.5 |
| 9914 (Belden) | 0.15 | 0.25 | 0.35 | 0.5 | 0.85 | 1.2 | 2.0 | 3.7 | 7.0 |
| LMR-400 | 0.13 | 0.2 | 0.27 | 0.45 | 0.7 | 1.0 | 1.5 | 2.7 | 4.8 |
| LMR-400-UF | 0.16 | 0.25 | 0.35 | 0.55 | 0.85 | 1.2 | 1.85 | 3.4 | 6.0 |
| LMR-600 | 0.07 | 0.1 | 0.15 | 0.27 | 0.4 | 0.6 | 0.95 | 1.7 | 3.0 |
| LDF4-50A (1/2" hardline) | 0.05 | 0.07 | 0.1 | 0.18 | 0.27 | 0.4 | 0.7 | 1.3 | 2.4 |
| LDF5-50A (7/8" hardline) | 0.025 | 0.05 | 0.07 | 0.1 | 0.16 | 0.22 | 0.4 | 0.75 | 1.4 |
| 450 Ω ladder line (window line) | 0.04 | 0.05 | 0.08 | 0.13 | 0.2 | 0.3 | 0.6 | 1.2 | n/a |

(All values are typical at 25 °C, sea level, no SWR. Manufacturers publish slightly different numbers — these are mid-range typical values. Real-world loss can vary ±15% within nominal product spec.)

## How to use the table

For a cable run of length L feet at frequency f:

**Total matched loss (dB) = (table value at f) × L / 100**

A 75-ft run of LMR-400 at 14 MHz: 0.45 × 75/100 = **0.34 dB matched loss**.

A 200-ft run of RG-58 at 432 MHz: 12.0 × 200/100 = **24 dB matched loss**. (= the 100W signal loses 24 dB of round-trip; 100 W transmit, ~0.4 W reaching the antenna. Don't.)

## Loss in S-units

A useful conversion: **1 dB ≈ 1/6 of an S-unit** (S-units are nominally 6 dB each). Some practical conversions:

| Total feedline loss | Effective S-unit penalty (each direction) |
|---------------------|-------------------------------------------|
| 0.5 dB | Negligible |
| 1.5 dB | 0.25 S-unit |
| 3.0 dB | 0.5 S-unit |
| 6.0 dB | 1 S-unit |
| 12.0 dB | 2 S-units |

A 200 ft RG-8X run at 144 MHz has 9 dB matched loss = 1.5 S-units of receive penalty *and* 1.5 S-units of transmit-power loss. Total round-trip impact: 3 S-units of "what you'd hear vs. what you'd hear with no feedline." That's a real signal.

## Loss vs. frequency: the slope

Loss in coax is dominated at HF/VHF/UHF by **conductor skin-effect loss**, which scales as √f (square root of frequency). A few observations:

- Doubling frequency increases loss by roughly √2 ≈ 41%.
- Going from 14 MHz to 144 MHz (10×) increases loss by √10 ≈ 3.2× (table values: RG-213 at 14 MHz is 0.65; at 144 MHz it's 2.8 — close to 4.3×, slightly worse than √f because dielectric loss starts to matter at VHF).

**At HF, conductor loss dominates.** At UHF, dielectric loss starts to matter and the slope steepens. At GHz frequencies, dielectric loss is the bigger half.

> **Advanced —** Total coax loss is the sum of: (a) conductor (skin-effect) loss, scaling as √f and proportional to 1/(D log(D/d)) where D is shield ID and d is center conductor OD; (b) dielectric loss, scaling as f × tan(δ) where δ is the dielectric loss angle; (c) radiation loss, usually negligible for coax. The crossover frequency where dielectric loss equals conductor loss is in the 1-10 GHz range for typical PE-dielectric cables, lower for foam or air dielectrics. This is why hardline (with foam dielectric and large geometry) is most cost-effective at VHF/UHF — both factors are reduced.

## Loss with elevated SWR

The table values are *matched-line* loss — what you get when SWR is 1:1 at the antenna end. When SWR is higher, the wave bounces back and forth, traveling the lossy line multiple times. **Total loss exceeds matched-line loss.**

The relation, expressed as total loss = matched + extra:

| Matched-line loss | SWR at antenna | Total loss | Extra |
|-------------------|---------------|-----------|-------|
| 1 dB | 1.5:1 | 1.05 dB | +0.05 |
| 1 dB | 2:1 | 1.18 dB | +0.18 |
| 1 dB | 3:1 | 1.5 dB | +0.5 |
| 1 dB | 5:1 | 2.1 dB | +1.1 |
| 1 dB | 10:1 | 3.4 dB | +2.4 |
| 3 dB | 2:1 | 3.4 dB | +0.4 |
| 3 dB | 5:1 | 4.5 dB | +1.5 |
| 3 dB | 10:1 | 6.0 dB | +3.0 |
| 6 dB | 2:1 | 6.5 dB | +0.5 |
| 6 dB | 5:1 | 8.5 dB | +2.5 |
| 6 dB | 10:1 | 11.5 dB | +5.5 |

**The bigger the matched-line loss, the more SWR hurts.** A short, low-loss cable feeding a high-SWR antenna is much friendlier than a long, lossy cable feeding the same high-SWR antenna.

This is why high-SWR coax problems are often hidden by long lossy cables — see §06-02 and §12-07.

## Power handling vs. frequency

Coax power-handling capability **decreases with frequency** (because matched-line loss is what heats the cable, and loss is higher at higher frequency). Approximate power ratings at 50% duty cycle, sea level, 25 °C:

| Cable | 30 MHz | 144 MHz | 432 MHz | 1296 MHz |
|-------|--------|---------|---------|----------|
| RG-58 | 700 W | 350 W | 200 W | 100 W |
| RG-8X | 1500 W | 600 W | 350 W | 180 W |
| RG-213 | 5000 W | 2200 W | 1300 W | 700 W |
| LMR-400 | 4000 W | 2500 W | 1500 W | 800 W |
| LMR-600 | 8000 W | 4500 W | 2900 W | 1700 W |
| LDF4-50A | 7500 W | 5000 W | 3000 W | 1700 W |

(These are continuous-carrier ratings. SSB voice is intermittent — derate by ~30% for safety. **Digital modes are 100% duty cycle**; use the continuous values directly.)

A 1500 W amplifier into RG-58 at 144 MHz (rated 350 W) is asking for cable failure — heating, dielectric softening, possible internal arc-over. Match cable to power.

## Loss with elevation (high altitude)

Coax power handling decreases at high altitudes because the air pressure is lower; voltage breakdown happens at lower fields. Above 5000 ft, derate by roughly 20%. Above 10000 ft, by 40%.

For sea-level home stations, ignore this. For mountaintop installs (Field Day, mountain-top SOTA repeaters), the derate matters at 1500 W operation.

## "Air-spaced ladder line" — different rules

450-Ω window line (and similar 300-Ω, 600-Ω lines) has loss numbers far lower than coax of comparable size. The line is mostly air, dielectric loss is negligible, and the larger conductor spacing keeps conductor loss low.

The trade: **ladder line is balanced**, must be kept away from metal, has SWR pattern dependent on routing, and requires a balanced tuner or balun to feed an unbalanced rig. This is why amateur stations almost universally use coax despite the loss penalty — coax just routes more easily.

For specific antennas (doublet, G5RV, multi-band with balanced tuner), ladder line is the right answer. For the generic amateur installation, coax is.

## Common mistakes

- **Comparing cables only at 14 MHz.** A cable that's 30% better at HF may be 60% better at UHF — or vice versa. Compare across the bands you actually use.
- **Ignoring SWR-induced extra loss.** A 4:1 SWR antenna with a long RG-58 run can have 40% more total loss than the matched-line value alone.
- **Using outdated numbers.** Old (pre-2010) cables vs. modern equivalents have different loss numbers; don't apply 1985 RG-8 numbers to 2024 LMR-400.
- **Treating manufacturer loss specs as exact.** Real-world cables vary ±15% from spec; production batches differ; old cables degrade.

## See also

- §18-00 — Overview
- §18-01 — Coax types
- §18-03 — Velocity factor (for electrical-length calculations)
- §06-02 — Feedline effects (the conceptual treatment)
- §12-01 — Coax issues troubleshooting
- §11 — Power budget
