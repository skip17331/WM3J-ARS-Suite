---
id: 07-09
title: J-Pole
chapter: 07
section: 09
level: simple
status: draft
---

# J-Pole

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What it is

A J-pole is a vertical, omnidirectional VHF/UHF antenna built from a half-wave radiator and a quarter-wave matching stub. Originally designed for Zeppelin airships (hence sometimes "Zepp" antenna), the J-pole has become the standard homebrew FM repeater antenna because it's:

- **Cheap** — typically built from copper plumbing pipe (~$15 for a 2m J-pole)
- **Simple** — half-wave radiator + quarter-wave matching stub, that's it
- **Effective** — vertical polarization, omnidirectional, ~3 dBi gain (similar to a rubber-duck whip but actually radiating)
- **No radials needed** — the matching stub provides the counterpoise

Variants include the **Slim Jim** (folded J-pole with end-end feeding), the **Copper Cactus** (multi-band J-pole), and the **2m Eggbeater** (horizontal version for satellites).

## How it works

A half-wave wire fed at one end has a very high impedance (~2500 Ω). The J-pole exploits a **quarter-wave shorted transmission line** (the "stub") to transform that high impedance to ~50 Ω. The stub acts as a quarter-wave impedance transformer — at the bottom (shorted) it's an open at the operating frequency; at the top it's a short.

The feed point is **near the bottom of the stub** — typically 5–15% up from the shorted base. Sliding the feed point up the stub shifts the impedance, allowing a clean SWR minimum at the design frequency.

The feed point's exact position depends on the stub's characteristic impedance (which varies with conductor diameter and spacing). For 1/2" copper pipe with 1.5" spacing, the feed is about 1/10 the stub length from the bottom.

## Calculator inputs and outputs

The Antenna Workshop calculator (`J-Pole`) takes:

- **Center frequency** (MHz)
- **Material** — affects velocity factor (copper pipe ≈ 0.96, aluminum ≈ 0.96, 450 Ω ladder line ≈ 0.91)

And returns:

- Half-wave radiator length (long element)
- Quarter-wave matching stub length
- Stub spacing
- Feed point distance from bottom (slidable)
- Bottom-of-stub connection note (shorted)

## Worked example — 2m J-pole from copper pipe

```
freq = 146.000 MHz
material: copper pipe (VF = 0.96)

half-wave radiator: 5905.5 × 0.96 / 146.000 = 38.83 in (98.6 cm, ~3.2 ft)
quarter-wave stub: 19.42 in (49.3 cm, ~1.6 ft)
stub spacing: 1.5 in
feed point from bottom: ~1.94 in (5–10% of stub)
bottom of stub: shorted (jumper between the two pipes)
```

Total antenna height: 38.83 + 19.42 = ~58.3 in (4.85 ft) plus the support pole. Fits on a chimney or porch railing.

A typical build uses **1/2" Type-M copper pipe**, **two 90° elbows** to form the stub, and a **PL-259 / N connector** soldered to the feed-point position. Total cost: under $20.

## Common mistakes

- **Wrong velocity factor.** Copper pipe is ~0.96; ladder line is ~0.91. Plugging in the wrong VF gives an antenna 5% off frequency.
- **Stub spacing affects matching.** 1.5" is a common compromise, but very-wide stubs (3+ inches) have higher characteristic impedance and need different feed-point geometry.
- **Feed point not adjustable.** Build it sliding — solder the feed connector to a small clamp that you can move up/down the stub for SWR minimum, then secure permanently.
- **Forgetting to short the stub bottom.** A jumper (typically a length of solid #14 wire soldered between the two pipes at the bottom) is essential. Without it, the stub doesn't transform impedance.
- **Insulating the antenna from a metal support.** Some operators mount J-poles on aluminum masts and find SWR shifts when the mast contacts the bottom of the stub. Use a fiberglass or wooden insulator at the mounting point.

> ⚙️ **Advanced —** The J-pole is a special case of the more general "shunt-fed" matching topology. The Slim Jim variant folds the radiator back, doubling the effective wire length in the same physical space and slightly improving low-angle radiation; the trade-off is more complex matching geometry.

## Build & trim notes

1. **Cut the radiator and stub** to the calculated lengths plus 1 inch trim margin each.
2. **Form the J-shape** with two 90° pipe elbows; solder the bottom jumper.
3. **Slide the feed connector** up/down the stub while sweeping with an analyzer; find SWR minimum at the operating frequency.
4. **Solder the connector permanently** at that position.
5. **Mount with a non-metallic standoff** at least 6 inches from any conductive support to prevent detuning.
6. **Final sweep** — typical SWR 1.1:1 at center, < 1.5:1 across the 2m band.
7. **Weatherproof** the feed connector — water in a J-pole's connector is the most common failure mode.

## See also

- §02 — Repeaters & Bandplans (where J-poles do their work)
- §07-00 — Antenna Workshop overview
- §07-15 — NanoVNA Trim Workflow
- §15-06 — Wavelength (1/2-wave and 1/4-wave math)
- §15-13 — Smith Chart Basics (for understanding the matching stub's impedance transformation)
