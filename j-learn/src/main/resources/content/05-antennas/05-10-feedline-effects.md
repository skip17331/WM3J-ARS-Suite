---
id: 05-10
title: Feedline Effects
chapter: 05
section: 10
level: mixed
status: draft
---

# Feedline Effects

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The SWR you read at your radio is **not** the SWR at your antenna. Sometimes the difference is small — half a tenth, no consequence. Sometimes the difference is dramatic, especially with high-loss feedline and significant antenna mismatch. Understanding what the feedline does to your apparent antenna behavior is essential to diagnosing antennas correctly.

This section covers the four big effects: **matched-line loss**, **additional loss from mismatch**, **impedance transformation along the line**, and **velocity factor and electrical length**.

## Matched-line loss

Even with a perfect 1:1 SWR, every foot of coax loses some power. The loss is specified per 100 feet at a specific frequency, in dB. Common types:

| Coax | Loss per 100 ft @ 14 MHz | Loss per 100 ft @ 144 MHz | Loss per 100 ft @ 432 MHz |
|------|--------------------------|----------------------------|----------------------------|
| RG-58 | 1.6 dB | 6.6 dB | 12.0 dB |
| RG-8X (Mini-8) | 1.0 dB | 4.5 dB | 8.0 dB |
| RG-213 / RG-8 | 0.65 dB | 2.8 dB | 5.0 dB |
| LMR-400 | 0.45 dB | 1.5 dB | 2.7 dB |
| LMR-600 | 0.27 dB | 0.95 dB | 1.7 dB |
| 9913 / 9914 | 0.5 dB | 1.7 dB | 3.0 dB |
| Hardline (Andrew LDF4-50A 1/2") | 0.18 dB | 0.7 dB | 1.3 dB |
| 450 Ω ladder line | 0.08 dB | 0.3 dB | 0.6 dB |

**Loss roughly doubles every doubling of frequency (skin-effect dominated)**. A coax that's fine on 80 m may be terrible on 70 cm.

**One dB of feedline loss is one S-unit of signal both ways** — 1 dB transmit + 1 dB receive = 2 dB on each QSO compared to no-loss feed.

## Additional loss from mismatch

When SWR is high, the wave reflects back and forth along the feedline, traveling that loss path multiple times. Real total loss > matched-line loss.

The formula is messy in closed form, but a practical table:

| Matched-line loss | SWR at antenna | Additional loss |
|-------------------|---------------|----------------|
| 1.0 dB | 2:1 | +0.2 dB |
| 1.0 dB | 5:1 | +0.7 dB |
| 1.0 dB | 10:1 | +1.5 dB |
| 3.0 dB | 2:1 | +0.4 dB |
| 3.0 dB | 5:1 | +1.5 dB |
| 3.0 dB | 10:1 | +3.0 dB |
| 6.0 dB | 5:1 | +3.0 dB |
| 6.0 dB | 10:1 | +5.5 dB |

**The bigger the matched-line loss, the worse the SWR penalty.** A 100-ft run of RG-58 at 50 MHz has 4 dB matched loss; with a 5:1 SWR antenna at the far end, you're losing 6+ dB total. With LMR-400 in the same scenario, total loss is closer to 2.5 dB.

This is why **lossy feedline hides bad SWR**. With 6 dB of matched loss, a complete short circuit at the antenna end shows only **3 dB return** at the rig (because the reflection has been attenuated 6 dB out and 6 dB back; the SWR at the rig measures as ~2:1, even though the antenna end is fully reflective). The rig shows a "good" SWR; the antenna is dead. Always trust an antenna analyzer at the antenna end, not the radio's meter at the rig.

## Impedance transformation along the line

A length of transmission line **transforms** the impedance you see at one end into a different impedance at the other end. The transformation depends on:

- **The antenna's impedance** at the far end
- **The feedline's characteristic impedance Z₀** (50 Ω for normal coax, 75 Ω for TV coax, 450 Ω for ladder line)
- **The electrical length of the line** (in wavelengths or fractions thereof)

The headline rule: if your line is exactly an **electrical half-wavelength** at the operating frequency, **the impedance at the rig end equals the impedance at the antenna end.** No transformation. (Plus loss, of course.)

If the line is a **quarter-wavelength**, it acts as an *impedance inverter*: Z_in = Z₀² / Z_load. So a quarter-wave of 50-ohm coax with a 200-Ω load presents 12.5 Ω at the rig end. Useful and dangerous: useful for matching networks, dangerous because random coax lengths near the antenna can create misleading SWR readings.

For other lengths, the impedance traces a **circle on the Smith chart** (see §05-09), which is why two operators with the same antenna and different feedline lengths can report wildly different SWR readings at the radio.

> ⚙️ **Advanced —** The transmission-line equation: Z_in = Z₀ × (Z_L + jZ₀ tan βℓ) / (Z₀ + jZ_L tan βℓ), where β = 2π/λ and ℓ is the line length. For matched line (Z_L = Z₀), Z_in = Z₀ at any length. For ℓ = λ/2, tan βℓ = 0 and the equation collapses to Z_in = Z_L. For ℓ = λ/4, tan βℓ = ∞ and L'Hôpital gives Z_in = Z₀² / Z_L. All other points are on the Smith circle. This is also why "make your feedline a half-wave on the band of interest" is a common matching trick — you can read the antenna's true impedance directly at the rig.

## Velocity factor and electrical length

The wave in a coax does not travel at the speed of light — it travels at **velocity factor (VF) times c**. Common VFs:

| Cable | VF |
|-------|-----|
| RG-58, RG-8, RG-213 (solid PE dielectric) | 0.66 |
| LMR-400, foam-PE dielectrics | 0.85 |
| Hardline (foam) | 0.88 |
| 450 Ω ladder line | 0.91 |
| Twinlead (300 Ω TV ribbon) | 0.82 |

A "30-foot" length of RG-58 is **30 × 0.66 = 19.8 ft electrical** when measured by RF wavelength. So if you want exactly an electrical half-wave on 14 MHz (where free-space half-wave = 35 ft), you need 35 × 0.66 = 23.1 ft of RG-58.

The J-Hub feedline calculator (see §08) handles this; manual calculation: physical length = (492 × VF) / f(MHz) for an electrical half-wave.

## Practical implications

1. **Choose feedline appropriate to your frequency and length.** Don't use RG-58 for a 100-ft run at 144 MHz; you'd lose two-thirds of your power. Use LMR-400 or hardline.
2. **An antenna's "real" SWR is what an analyzer says at the antenna feedpoint**, not what the rig says at the shack end. If you're tuning, climb up or use a portable analyzer.
3. **Cut feedlines to convenient electrical lengths when you can.** Half-wave or full-wave lengths preserve impedance through the line; this makes troubleshooting easier and lets you use the rig's analyzer/SWR meter as a reasonable proxy for antenna behavior.
4. **A high-SWR antenna with a long, lossy feedline still TXs** — but it's losing power as feedline heat, not as RF in the air. Total system loss is what matters.

## When feedline isn't the problem

If the SWR is the same at the rig and at the antenna feedpoint, **the feedline is not the cause** — the antenna or the matching at the antenna is. If the SWR at the rig is *much lower* than the SWR at the antenna feedpoint, your feedline is hiding a real problem with loss.

This is one of the most useful sweep checks in antenna debugging: do the measurement twice, once at each end. If they're different by more than 0.5, your feedline has significant loss in this configuration and you should consider better feedline.

## See also

- §05-09 — Smith charts (the tool for visualizing line transformation)
- §05-11 — Impedance transformation (when transformation is desired)
- §05-12 — Baluns and chokes
- §09-05 — Feedline mismatch and SWR
- §22 — Coax and connectors (loss and types reference)
- §13 — High-SWR troubleshooting
