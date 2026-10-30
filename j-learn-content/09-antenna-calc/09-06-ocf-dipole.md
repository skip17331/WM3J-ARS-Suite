---
id: 09-06
title: OCF Dipole (Windom)
chapter: 09
section: 06
level: mixed
status: published
---

# OCF Dipole (Windom)

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What it is

An **off-center-fed (OCF) dipole** is a half-wave dipole at the lowest band, but **fed off-center** — typically at about 1/3 of the way from one end. The off-center feed exploits a fortunate property of half-wave wire: at certain feed points, the impedance is roughly band-independent across the harmonic bands (2×, 4×, 8× the fundamental).

This makes the OCF a **multi-band antenna with a single, robust impedance** that a 4:1 or 6:1 balun can transform to 50 Ω. Multi-band, no traps, no fan, no tuner needed for the harmonic bands.

The classical "Windom" antenna is a specific variant fed with a single wire (no coax, just a feed wire from the rig). The modern Carolina Windom (Hy-Gain, Force 12, MyAntennas, etc.) feeds via 4:1 or 6:1 balun and 50 Ω coax.

## How it works

A half-wave dipole has a current distribution that peaks at the center and goes to zero at the ends. The voltage distribution is the opposite — zero at center, peaks at the ends. **Impedance** at any point on the wire = V/I at that point.

At the **center**, low V × high I = ~73 Ω.
At the **ends**, high V × low I = several thousand Ω.
At a **specific point** about 33–36% from one end, the impedance is around 200–300 Ω **and roughly the same on the harmonic bands** (2nd, 4th, 8th harmonic).

The Carolina Windom variant (most common today) uses a feed offset of ~36% (Δ ≈ 0.18 from center), giving:

- ~200 Ω feed Z → match with **4:1 balun**
- 80m / 40m / 20m / 15m / 10m all usable with that one match

The 30 / 17 / 12 m (WARC) bands are odd harmonics (3×, 6×, etc.) and have *different* feed Z — typically requiring a tuner.

## Calculator inputs and outputs

The Antenna Workshop calculator (`OCF Dipole (Windom)`) takes:

- **Lowest band frequency** (MHz) — fundamental
- **Feed offset from center** (% of total length, typical 33%)
- **Balun ratio** — 4:1, 6:1, or 9:1

And returns:

- Total length, short-leg length, long-leg length
- Target feed-point impedance for the chosen balun
- Bands covered as harmonics
- Common-mode choke recommendation

## Worked example — Carolina Windom for 80 / 40 / 20 / 15 / 10 m

```
freq (lowest band) = 7.150 MHz   (we want 80m fundamental — actually 3.7 MHz)

Let's use 80m fundamental for clarity:
freq = 3.700 MHz
offset = 36%
balun = 4:1

total length = 468 / 3.700 = 126.5 ft
short leg = 126.5 × 0.36 = 45.5 ft
long leg = 126.5 × 0.64 = 81.0 ft

target feed-Z: ~200 Ω → 4:1 balun → 50 Ω coax
harmonics covered: 3.7 MHz (80m fundamental), 7.4 (40m), 14.8 (20m), 29.6 (10m)
```

A real Carolina Windom commercial antenna (MyAntennas EFHW-8010 / OCF) is typically ~131 ft of 14 AWG insulated wire with the balun positioned at the 36% point — covers 80/40/20/15/10 from one feedpoint.

## Common mistakes

- **Skipping the common-mode choke** at the rig end. The OCF's asymmetric feed point puts substantial common-mode current on the coax — a 1:1 line isolator (a few feet of coax wound around a stack of FT240-31 or 43 cores) at the rig is essential.
- **Wrong balun ratio.** A 4:1 expects 200 Ω; a 6:1 expects 300 Ω. Match the balun to the feed offset you actually built.
- **Wide HOA-friendly enclosure on the balun** that lets water in. Sealed, drained, mounted with the connectors **down**. Re-seal yearly.
- **Trying to use it on WARC bands without a tuner.** The 3rd / 6th harmonics have different impedance — tuner required.
- **Building a "Windom" with a single-wire feed.** That's the historical Windom but suffers from RFI and loss. Modern OCF uses balanced or coax feed via balun.

> **Advanced —** The exact off-center feed point that gives band-independent impedance varies slightly with wire diameter, height, and ground conductivity. The "Carolina" 36% point is empirical; some operators get better results at 30% (with 6:1 balun). NEC-2 modeling can find the optimum for your installation. The trade-off: closer to 1/3 from center → higher Z (300 Ω, needs 6:1) and slightly better band-flatness; closer to 36% → lower Z (200 Ω, needs 4:1) and slightly more bandwidth on each band.

## Build & trim notes

1. **Pre-cut the total length** based on your fundamental's calculator output. Don't worry about the offset until after raising it.
2. **Mark the feed point** at 33–36% from one end. The balun mounts here.
3. **Use a quality 4:1 (or 6:1) current balun** rated for full power. The Balun Designs 4119 (4:1, 5 kW) is a common reliable choice.
4. **Add a 1:1 common-mode choke** (1:1 isolator) at the rig end of the coax — this is non-negotiable.
5. **Sweep all bands** — expect SWR ≤ 2:1 on the harmonic bands; WARC bands need the tuner.
6. **Trim the long leg first** if 80m is high in frequency; trim both proportionally.

## See also

- §06 — Antennas (theory chapter)
- §09-00 — Antenna Workshop overview
- §09-02 — Flat Dipole (center-fed comparison)
- §09-04 — Fan Dipole (alternative multi-band approach)
- §09-15 — NanoVNA Trim Workflow
- §17-04 — Impedance (where the off-center math comes from)
