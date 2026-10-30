---
id: 06-05
title: Ground-Plane Effects
chapter: 06
section: 05
level: mixed
status: published
---

# Ground-Plane Effects

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The same antenna, hung at two different heights, produces wildly different signal reports. The same vertical, on two different soils, will work DX from one location and fall flat from the other. **Ground is the most-overlooked single factor in amateur antenna performance.**

This section covers the three things ground does to your antenna: **change the feedpoint impedance**, **change the radiation pattern (especially the takeoff angle)**, and **eat power as ground loss**. All three are real, all three matter, and all three depend on antenna height, antenna type, and soil characteristics.

## What "ground" actually means

A perfect ground would be an infinite, perfectly conducting plane. Real earth is none of those things:

- **Soil conductivity** varies by ~3 orders of magnitude. Salt water > wet farmland > dry farmland > forest > rock > sand.
- **Soil dielectric constant** also varies, less dramatically.
- **Real ground is finite** — the antenna's near-field reaches the ground at all directions and partially reflects, partially absorbs.
- **The ground's apparent properties depend on frequency**: a soil that's "average" at 14 MHz may be much worse at 1.8 MHz.

For modeling, the FCC and ARRL use these standard ground categories:

| Category | Conductivity (mS/m) | Dielectric ε | Examples |
|----------|--------------------|--------------|----------|
| Salt water | 5000 | 80 | Ocean coast |
| Fresh water | 30 | 80 | Lake, river |
| Very good | 30 | 20 | Wet rich farmland, marsh |
| Good (typical) | 10 | 14 | Suburban yard, average soil |
| Average | 5 | 13 | Dry farmland |
| Poor | 2 | 13 | Sandy soil |
| Very poor | 1 | 5 | Dry rocky soil, mountains |

Most amateur installations are "good" to "average" ground unless you live by the ocean (great) or in the desert / on bedrock (terrible).

## Effect on impedance

A horizontal antenna's feedpoint impedance varies dramatically with height above ground:

| Height (in λ) | Dipole feed impedance |
|---------------|------------------------|
| 0.05 λ | ~10 Ω (very lossy) |
| 0.1 λ | ~25 Ω |
| 0.2 λ | ~50 Ω |
| 0.3 λ | ~70 Ω |
| 0.4 λ | ~85 Ω |
| 0.5 λ | ~95 Ω |
| 0.7 λ | ~75 Ω (close to free-space value) |
| 1.0 λ | ~80 Ω |
| Free space | ~73 Ω |

What this means: **a dipole at 0.2 λ (about 28 ft on 20 m, 14 ft on 10 m) has a much different impedance than the same antenna at 0.5 λ.** This is one reason an antenna can "work better" after you raise it — not just pattern, but match.

For a vertical, ground proximity is even more dramatic; the feed impedance and the ground losses are tightly coupled.

## Effect on radiation pattern

The wave radiated downward by a horizontal antenna reflects off the ground and combines with the upward-radiated wave at some elevation angle. Where they add constructively, you get a **lobe**; where they cancel, a **null**.

The mathematics: a horizontal antenna at height **h** above perfect ground has its first elevation-pattern peak at:

**Elevation angle (degrees) = arcsin(λ / 4h) ≈ 14.3° / h(λ)**

| Height | First elevation-pattern peak |
|--------|------------------------------|
| 0.25 λ | 90° (straight up — useful for NVIS!) |
| 0.5 λ | 30° (good for moderate DX) |
| 1.0 λ | 14° (excellent for DX) |
| 1.5 λ | 9° (very-low-angle DX, rare openings) |
| 2.0 λ | 7° (extremely low angle; the nulls and other lobes start to bite) |

This is why **height changes everything for DX**. A 20 m dipole at 33 ft (0.5 λ) has peak radiation at 30° — fine for short-haul DX and regional. At 66 ft (1 λ), peak is 14° — much better for transcontinental and transoceanic. At 100 ft (1.5 λ), peak is 9° — competition-grade for low-angle DX.

For 80 m, "1 λ high" is **270 ft** — far beyond any amateur tower. That's why 80 m DX is hard from a typical lot: every dipole is necessarily a low one.

## NVIS (Near Vertical Incidence Skywave)

There's one case where you *want* a low antenna: NVIS, talking to stations 100–500 km away. The signal has to take a steep elevation angle (60–90°) to hit the ionosphere and bounce back into your geographic region — exactly the pattern of a low (0.1–0.25 λ) horizontal antenna.

**Optimal NVIS antenna**: horizontal dipole at 0.15–0.20 λ above ground. On 80 m that's 40–50 ft, on 40 m 20–26 ft. Both heights are easily achievable, and NVIS-specific dipoles (often inverted V) are the workhorse antennas for emcomm and short-range nets.

NVIS is the one case where "low is good." For everything else, more height is better.

## Effect on ground loss

A vertical antenna's quarter-wave radiator drives a *displacement current* into the ground in its near field. That current flows through real, lossy soil before returning via radials or local conductors. **Energy lost in this near-field ground current never radiates** — it heats the dirt.

Loss numbers:

| Antenna | Ground type | Estimated loss (dB) |
|---------|-------------|---------------------|
| Vertical, no radials, average ground | 5 dB |
| Vertical, 4 radials, average ground | 3.5 dB |
| Vertical, 16 radials, average ground | 1.5 dB |
| Vertical, 60 radials, average ground | 0.5 dB |
| Vertical, 16 radials, salt water | < 0.1 dB |
| Vertical, no radials, salt water | < 0.5 dB (salt water IS the ground plane) |
| Horizontal dipole at 0.5 λ, average ground | 0.5 dB |
| Horizontal dipole at 0.1 λ, average ground | 2 dB+ |

**Vertical antennas are far more ground-sensitive** than horizontal antennas, which is why they need radial systems. Salt water magically makes a vertical work amazingly well — coast stations have the best HF verticals on Earth, no matter what radial system they use, just because the ground is conductive enough that very little power is lost to it.

## Practical implications

- **Horizontal antennas: prioritize height above all else.** Every doubling of height (in fractions of a wavelength) brings the takeoff angle down by a factor of 2 and the lowest lobe gain up by about 3 dB.
- **Verticals: prioritize radial system.** Going from 4 radials to 32 radials buys you 2–3 dB on most installations. Going from 32 to 60 buys you another 1 dB. Past 60 is diminishing returns.
- **For NVIS**: do the *opposite* of DX — keep the antenna low.
- **If you live near salt water**: any vertical you put up will outperform an average inland vertical with full radials. Take advantage of it.
- **For modeling**: don't use "perfect ground" or "free space" except for sanity checks. Use the realistic ground type for your soil. The pattern shapes change.

> **Advanced —** The Sommerfeld formulation (1909) is the canonical treatment of an antenna over imperfect ground; it requires evaluating the Sommerfeld integrals. Modern moment-method codes (NEC) approximate this with the Norton-derived Reflection Coefficient method (faster, slightly less accurate at low heights) or with a fully evaluated Sommerfeld solver (more accurate, much slower). For amateur antenna heights of 0.1 λ and above, the reflection-coefficient method matches measured patterns to within 0.5 dB. Below 0.1 λ, ground losses become inseparable from feedpoint impedance and the simpler models start to lie.

## How to measure ground at your QTH

If you suspect ground is the difference between your antenna's modeled and observed performance:

1. **Drive a steel rod 4 ft into the ground.** Drive a second rod 50 feet away. Measure the resistance between them with a 4-wire ohmmeter or via the ARRL "soil conductivity meter" technique. The resistance corresponds to soil conductivity in mS/m (consult an ARRL Antenna Book conductivity-vs-resistance chart; typical numbers: 5 Ω for very good ground, 50 Ω for poor, 500 Ω for very poor).
2. **Compare with FCC ground-conductivity maps** (available at fcc.gov; designed for AM broadcast but the map applies to amateur HF too).
3. **Treat your soil as one of the categories above** when modeling, picking the worse of measured and mapped value if uncertain.

## Common mistakes

- **Putting a horizontal antenna at 8 ft and expecting DX.** That's a rounded zenith pattern; great for NVIS, mediocre at best for DX.
- **Putting a vertical with 4 radials and assuming "ground is ground."** Half your power may be heating dirt.
- **Ignoring ground when modeling.** Free-space and perfect-ground modeling miss the most important pattern feature for low antennas: the elevation pattern.
- **Forgetting that ground type matters for verticals.** A move from a sandy lot to a damp lot can be 3 dB on a vertical, even if you didn't change anything else.

## See also

- §06-12 — Verticals (most ground-sensitive antenna)
- §06-07 — Radiation patterns (where the height-vs-angle effects show up)
- §06-06 — Modeling concepts (handling ground in NEC-style code)
- §11 — Power budget / ERP (ground losses are real losses)
