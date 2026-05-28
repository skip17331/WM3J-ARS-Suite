---
id: 01-02
title: MUF and LUF
chapter: 01
section: 02
level: mixed
status: draft
---

# MUF and LUF

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

For any path between two stations on Earth, there is a **window of frequencies that will work** — too high and your signal punches through the ionosphere into space, too low and it's swallowed by absorption. The top of the window is the **MUF** (Maximum Usable Frequency); the bottom is the **LUF** (Lowest Usable Frequency).

## MUF — what it is

The **maximum frequency at which a one-hop signal launched at the optimum take-off angle for a given path will still be refracted back to Earth** by the ionosphere.

Above the MUF, the ionosphere is too thin to bend your signal back; it sails on into space. Below the MUF, you get a return — though signal strength varies wildly with how far below.

Several flavors you'll see quoted:

| Term | Meaning |
|------|---------|
| **MUF (path)** | MUF for the specific great-circle path between two stations |
| **MUF (3000)** | MUF for a 3000 km hop — the standard reference distance |
| **F2 MUF** | MUF for an F2-layer hop (the usual one for HF DX) |
| **E MUF** | MUF for an E-layer hop (much shorter range, ~1500 km max) |
| **Es MUF** | MUF for sporadic-E, often pushing into the low VHF |

When operators say "the MUF is 22 MHz" with no qualifier, they mean the F2-layer MUF for a typical 3000 km hop in their location.

## LUF — what it is

The **lowest frequency that will produce a usable signal** over the path. Set primarily by **D-layer absorption** during daylight (chapter §01-04). At night the D-layer disappears and the LUF drops dramatically.

Practical LUF varies with:

- **Time of day** — daytime LUF is much higher than nighttime LUF
- **Solar activity** — high SFI both raises the MUF *and* the LUF (more ionization absorbs more)
- **Path geometry** — more daylight in the path = higher LUF
- **Required SNR** — LUF for SSB is higher than LUF for FT8 because FT8 needs less signal

Typical values for a 3000 km daytime path on a quiet day: LUF 6–8 MHz, MUF 18–25 MHz. Same path at night: LUF 3 MHz, MUF 12–18 MHz.

## The operating window — be near the top, not the bottom

Two rules of thumb every HF operator learns:

1. **Operate at 80–90% of the MUF for best results.** Right at the MUF, signals are strongest because absorption is minimal and refraction is most efficient. Just above the MUF, you'll hear nothing. Way below the MUF, signals are present but absorption eats them.

2. **The "FOT" (Frequency of Optimum Traffic) is conventionally 85% of MUF.** This is the frequency that produces a reliable signal 90% of the time. Old-school commercial HF planners fixed schedules around the FOT.

So if a software MUF map says "MUF to Europe is 24 MHz", the band most likely to give you a clean QSO is **20 MHz × 0.85 ≈ 17 m or 20 m** — depending on which actual ham band falls in that window.

## How MUF varies

| Driver | Effect on MUF |
|--------|---------------|
| **Time of day** | Peaks an hour or two after local noon at the path midpoint |
| **Season** | Higher in winter than summer at mid-latitudes (winter F2 anomaly) |
| **Solar cycle** | Tracks SFI strongly — at SFI 200 the MUF is roughly twice what it is at SFI 80 |
| **Latitude** | Equatorial paths run higher MUFs than polar paths |
| **Geomagnetic activity** | High K knocks the MUF down, especially on polar paths |

## How distance affects MUF

A signal launched at a low take-off angle (1°–5° above the horizon) covers a long hop — up to about 4000 km in one F2 bounce. Higher take-off angle = shorter hop. Because the geometry of refraction works in your favor at low angles, **MUF rises with distance**, up to the maximum hop range.

Quick numbers (for SFI ~150, daytime, mid-latitudes):

| Hop distance | Approx MUF |
|-------------:|-----------:|
| 500 km       | 12 MHz     |
| 1500 km      | 18 MHz     |
| 3000 km      | 24 MHz     |
| 4000 km      | 28 MHz     |

This is why someone in Florida calling Italy at noon (5000+ km, two hops) might be loud on 10 m, while the same operator can't reach Atlanta (1000 km) on 10 m at all.

## The skip zone

Because MUF rises with distance, there's a **dead zone** between the maximum range of ground wave (a few hundred km on HF) and the minimum range of one-hop sky wave. This is the **skip zone**. A skip zone of 800 km on 20 m is normal at midday; on 10 m it can be 1500 km.

If you can't reach a nearby station on a band but easily work overseas, you're in their skip zone. Drop a band — the lower frequency has a smaller skip zone.

## LUF and absorption

D-layer absorption goes roughly as **1/f²** — halve the frequency, quadruple the absorption. That's why 80 m is dead during the day (D-layer takes a 30 dB chunk out of every signal) but wide open after sunset.

> **Advanced —** The full daytime absorption coefficient is approximately
> `L (dB) ≈ 677.2 · sec(χ) · I / (f + fH)²`
> where χ is the solar zenith angle, I is the absorption index (related to SFI and time of year), f is the operating frequency in MHz, and fH is the gyrofrequency (~1.4 MHz at temperate latitudes). The `sec(χ)` term is why the LUF rises sharply when the path passes through tropical latitudes around local noon — the sun is more directly overhead and the path through the D-layer is shortest.

## Math behind the MUF (for the curious)

The MUF for an oblique path relates to the **critical frequency** `foF2` (the highest vertical-incidence frequency that still reflects from the F2 layer) by the **secant law**:

```
MUF ≈ foF2 · sec(φ)
```

where φ is the angle of incidence at the bottom of the F2 layer. For very low takeoff angles, sec(φ) approaches 3.5, which is why MUF is roughly **3 to 3.5 times foF2** for long hops. A foF2 of 8 MHz at noon translates to a path MUF in the 24–28 MHz range — comfortably on 10 m and 12 m.

> **Advanced —** Real ionograms use the **M(3000)F2 factor** instead of computing the geometry from scratch. Standard ionosondes report foF2 (vertical critical frequency) and M(3000)F2 (the ratio of the 3000 km MUF to foF2). The 3000 km MUF is then `foF2 × M(3000)F2`. Typical M(3000)F2 values run 2.5 to 3.4 depending on latitude and time. For other distances, scale linearly between zero (where MUF = foF2) and 4000 km (where MUF is the maximum).

## Tools

- **PSKReporter** is the empirical MUF detector: if a station 4000 km away is hearing your 14 MHz signal, your MUF on that path is at least 14 MHz.
- **VOACAP** (covered in §01-07) computes predicted MUF and LUF for any path you give it.
- Real-time global MUF maps are produced by the **DLR ionosphere group** and other agencies, derived from a worldwide network of digisondes; many ham dashboards mirror them.

## See also

- §01-04 — the ionospheric layers that set the MUF (F2) and the LUF (D)
- §01-05 — how the MUF rides up and down with the solar cycle
- §01-07 — VOACAP and how it estimates path MUF
- §01-08 — the quick decision tree for "what band right now"
