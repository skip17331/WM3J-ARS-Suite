---
id: 01-04
title: Ionospheric Layers (D / E / F1 / F2)
chapter: 01
section: 04
level: mixed
status: published
---

# Ionospheric Layers (D / E / F1 / F2)

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The ionosphere is not a single layer — it's a stack of distinct ionized regions stretching from about 60 km to over 500 km up. Each layer has its own personality: when it appears, what frequencies it bends, what frequencies it absorbs. Once you know the cast of characters, every other propagation phenomenon makes sense.

## The cast, in altitude order

| Layer | Height | When present | What it does to your signal |
|-------|--------|--------------|------------------------------|
| **D** | ~60–90 km | Daylight only | Absorbs MF and lower HF. Your enemy on 80 m and 160 m during the day. |
| **E** | ~90–150 km | Daylight (regular E); sporadic any time (Es) | Reflects up to ~10 MHz over short hops; sporadic E reaches the low VHF |
| **F1** | ~150–220 km | Daytime only, summer mostly | A weak helper that merges into F2 at night |
| **F2** | ~220–500+ km | Always present, varies wildly | The workhorse of HF DX |

The "always above" rule: D is below E is below F1 is below F2. Higher layers exist whether or not lower ones do.

## D layer — the daytime wall

D is the lowest, densest, and most absorptive layer. It only exists when the sun is shining; sunset begins its decay and within an hour or two it's essentially gone.

**What it absorbs:** mostly the MF band (530–1700 kHz, your AM broadcast) and the lower HF bands (160 m, 80 m, 40 m). Absorption goes roughly as **1/f²** — halve the frequency, quadruple the absorption.

**What it doesn't bother:** anything above ~10 MHz passes through D with negligible loss. That's why 20 m, 17 m, and up work fine in daylight.

**Why D absorbs but doesn't reflect:** the D-layer's electron density is relatively low, but it sits in air dense enough that electrons collide with neutral molecules many millions of times per second. Each collision robs energy from the radio wave. The wave is too high in frequency to be bent back, and the collisions convert RF energy to heat.

> **Advanced —** D-layer absorption (the so-called *non-deviative* loss) follows
> `L (dB) ≈ K · sec(χ) / (f + fH)²`
> where χ is the solar zenith angle, fH is the gyrofrequency (~1.4 MHz at temperate latitudes), and K depends on the time of year and solar conditions. The `sec(χ)` term explains why noon absorption is much worse than morning or afternoon absorption — the sun shines straight through the D-layer at noon, but at slant angles your signal traverses much more ionized material on its way up to the F-layer and back down.

## E layer — the short-range bouncer

The regular E-layer exists during daylight, peaks around noon, fades after sunset. Heights are around 100 km, electron density too low to bend HF beyond about 10–12 MHz.

**Useful bands:** ground-wave-augmenting on 160 m, 80 m, 40 m; usable for short-hop work on 30 m, sometimes 20 m.

**Maximum hop range:** about 1500 km in one bounce because the layer is low.

**Sporadic E (Es)** is a different beast — clouds of unusually dense E-layer ionization that can reflect signals up into the low VHF. Es lives in chapter §01-06; for now, just know that it's the same altitude as regular E but vastly more reflective and far more capricious.

## F1 layer — the helper

F1 forms by day, mostly summer, in the 150–220 km range. Its electron density is lower than F2's, so it can only bend lower frequencies.

For most amateur purposes you can ignore F1. It usually acts as a weak adjunct to F2, occasionally giving a short low-take-off-angle path. At sunset it merges with F2 into a single F-layer, and the merged layer is what carries you through the night.

> **Advanced —** F1 produces a "ledge" in the electron density profile that creates a secondary critical frequency, foF1. At slant incidence, F1 can support a separate hop pattern with a shorter ground range than F2. In ionograms F1 shows up as a knee just below the F2 trace; if F1 dominates, low-take-off-angle DX paths can land at unexpectedly close ranges.

## F2 layer — the DX workhorse

This is **the** layer for long-distance HF. F2 sits between 220 km and (during very disturbed conditions) 500 km up. It's ionized by extreme ultraviolet from the sun, and unlike D and E, it persists through the night because the air is so thin that ions take many hours to recombine.

**What it does:** bends frequencies from a few MHz up to ~30 MHz (sometimes 50+ MHz at solar maximum) back to Earth. A single F2 hop covers up to about 4000 km. Multi-hop paths via F2 can take you anywhere on the planet.

**Time of day:**
- **Daytime**: F2 is fully ionized, MUF is at its highest. Best high-band conditions.
- **Sunset**: F2 starts to lose ionization but slowly. F1 merges into F2.
- **Night**: F2 alone, weaker but still working. The lower bands are usable now because the D-layer is gone.
- **Pre-dawn**: F2 has been ionizing all night and is at its weakest. MUF is at its lowest.

**Seasonal patterns:**
- **Winter F2** at mid-latitudes is often *better* than summer F2 (the "winter anomaly") — colder days produce a more efficient ionization-vs-recombination balance.
- **Equinoxes** (March, September) consistently produce the best HF DX of the year.

**Solar cycle:** F2 ionization tracks SFI almost linearly. At solar minimum (SFI ~70) the F2 MUF rarely exceeds 18 MHz. At solar maximum (SFI 200+) it routinely reaches 35 MHz.

> **Advanced —** F2 is created mainly by photoionization of atomic oxygen by EUV in the 10–100 nm wavelength range. The peak of the F2 layer (the "hmF2" altitude) varies from ~220 km at high latitudes to ~350 km in the equatorial belt, and it can rise to over 500 km during geomagnetic storms when neutral winds drive the ionization upward. The peak F2 electron density (NmF2) is what determines foF2 via `foF2 ≈ 9 · sqrt(NmF2 [m⁻³] / 10¹²)`. A foF2 of 8 MHz corresponds to NmF2 ≈ 7.9 × 10¹¹ electrons/m³.

## Putting it together — a typical day

Here's what happens to the ionosphere over a 24-hour period at mid-latitudes during average solar activity:

| Local time | What's up there | What works |
|------------|-----------------|------------|
| 02:00 | F2 only, weakening | 40 m & 80 m DX; 160 m possible |
| 05:00 | F2 at minimum | Lowest MUF of the day; 80 m DX peaks |
| **Sunrise (greyline)** | D vanishing, F2 still ionized | 80 m / 160 m DX windows |
| 09:00 | D, E, F1, F2 all forming | High bands wake up |
| 13:00 | All layers fully developed | Best 10 m / 15 m DX |
| 17:00 | D weakening, F2 still strong | Bands transitioning, 20 m great |
| **Sunset (greyline)** | D shutting down | 40 m & 80 m DX windows |
| 20:00 | F1 merging into F2, no D | 40 m / 30 m / 20 m DX peak |
| 23:00 | F2 only | High bands closing, low bands prime |

## Why this matters for your operating

When someone says "the band is dead", they usually mean one of:

- "**D-layer is too thick today**" (40 m at noon during an X-class flare)
- "**MUF dropped below this band**" (10 m at night)
- "**F2 was disturbed by a CME**" (everything sounds fluttery)
- "**No sporadic E**" (6 m in winter)

Knowing which condition is biting tells you what to do. If it's D-layer absorption, wait for sunset. If MUF is too low, drop a band. If F2 is disturbed, be patient or move to lower bands.

## See also

- §01-01 — solar indices, especially SFI which drives F2 ionization
- §01-02 — MUF/LUF: the practical consequence of the layers above
- §01-03 — greyline: the D-layer's daily disappearance
- §01-06 — sporadic E: the wildcard E-layer event
