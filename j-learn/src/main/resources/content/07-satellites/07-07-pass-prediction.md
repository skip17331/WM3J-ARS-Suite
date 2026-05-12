---
id: 07-07
title: Pass Prediction
chapter: 07
section: 07
level: simple
status: draft
---

# Pass Prediction

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A satellite pass is a window of time when the satellite is visible from your location — between AOS (acquisition of signal, when it crests your horizon) and LOS (loss of signal, when it drops back below). For a low-Earth-orbit (LEO) satellite, that window is typically 5 to 15 minutes. For a higher orbit, longer; for geostationary, the satellite is either always visible or never visible.

This section covers how pass prediction works, what numbers it gives you, and how to choose which passes to work. The math itself is done by software (see §07-03 for the Keplerian element propagation behind it); this section is about what to do with the output.

## What pass prediction tells you

For any satellite + your station + a future time window, the predictor outputs:

| Output | What it means |
|--------|---------------|
| **AOS time** | When the satellite first crests above your horizon (UTC) |
| **AOS azimuth** | Compass direction where the satellite first appears |
| **Maximum elevation** | Highest angle above horizon during the pass (the "peak" of the pass) |
| **Time of max elevation** | When that peak occurs |
| **Azimuth at max elevation** | Direction at the peak (roughly the middle of the pass) |
| **LOS time** | When the satellite drops back below the horizon |
| **LOS azimuth** | Where the satellite disappears |
| **Pass duration** | LOS − AOS, typically 5–15 min for LEO |
| **Maximum range** | Slant distance to satellite at LOS or AOS (the "horizon" of your station) |
| **Minimum range** | Slant distance at maximum elevation (closest approach) |

For each pass, you also get a continuous **az/el profile** — a curve showing the satellite's azimuth and elevation throughout the pass.

## Reading a pass prediction

A typical prediction list might look like:

```
Sat name      AOS UTC     Max El   AOS Az   LOS UTC     Duration
AO-91         18:32:15      52°N     282°    18:43:01    10m 46s
AO-92         19:05:48      78°NE    255°    19:16:29    10m 41s
ISS           19:42:11       6°S     145°    19:48:55     6m 44s
RS-44         20:14:33      35°SW    220°    20:32:18    17m 45s
```

What you'd extract from this:

- **AO-91 at 18:32 UTC** is a high pass (52° max), good for working. Rising in the SW, setting in the NE.
- **AO-92 at 19:05 UTC** is even higher (78° max) — almost overhead. Premier pass for chasing DX.
- **ISS at 19:42 UTC** is a low pass (6° max) — very brief, weak signal, skip unless you need ISS specifically.
- **RS-44 at 20:14 UTC** is a moderate pass (35°), 17 minutes long — the linear transponder satellite is great for SSB QSOs.

You'd typically work the high passes; low passes are skipped except when nothing else is available.

## Maximum elevation: the most important number

A pass's maximum elevation determines how good the geometry is:

| Max elevation | Quality | Notes |
|---------------|---------|-------|
| 5–10° | Marginal | Long slant range; weak signals; brief |
| 10–25° | OK | Workable; useful for FM birds |
| 25–50° | Good | Strong signals; full range of Doppler |
| 50–75° | Excellent | Approaching overhead; solid contacts |
| 75–90° | Best | Directly overhead at peak; minimum slant range |

A 90° pass goes straight overhead — best signal-to-noise, but creates the "azimuth flip" problem for rotators (see §07-04).

## How many passes per day

For a LEO satellite with ~90 minute orbital period, the satellite makes ~16 orbits per day. Of those, typically:

- 6–8 passes are visible from your location (the satellite goes around the Earth, but you're only on one side at a time).
- 2–4 of those visible passes are "good" (max elevation ≥ 25°).
- 1–2 are "best" (max elevation ≥ 50°).
- 1 occasionally goes near zenith (max elevation ≥ 75°).

So a typical day from a US location: ~6–8 visible passes per LEO satellite, of which maybe 2–3 are worth queuing up for. Multiply by the 5–10 active amateur LEO satellites, and you have several dozen passes to choose from each day.

## Picking passes to work

For an FM bird, prioritize:

1. **High elevation** (50+ degrees max).
2. **Time of day** that fits your schedule. Many operators are on lunch break or after work.
3. **Specific satellite operational status**. Always check AMSAT or J-Sat status before — some sats are intermittent.
4. **Pile-up considerations**. Prime time (19:00 local on a weekday) brings 50+ stations to the most popular birds; off-peak is friendlier.

For a linear transponder, prioritize:

1. **Long passes** (max duration is often more useful than max elevation for SSB QSOs).
2. **Direction matching your antenna** (if not on a rotator, pick passes that align with your fixed antenna).
3. **Pass timing** that allows for gradual operation (longer slow QSOs, not rushed FM exchange).

## Pass forecast horizons

How far ahead can you predict?

- **Within the next 24 hours**: very accurate (timing within seconds, azimuth within fraction of a degree).
- **Next 7 days**: still accurate (TLE good for ~7 days for LEO).
- **2–4 weeks**: degrades; timing errors grow, especially for low-altitude LEO sats subject to drag.
- **1+ month**: speculative — refresh TLEs and re-predict.

For "I want to work the bird tomorrow at 7 PM," any current TLE will work. For "I want to plan a contest weekend in two months," predict close to the date with fresh TLEs.

## Tools that show pass predictions

| Tool | Platform | Cost | Strengths |
|------|----------|------|-----------|
| Heavens-Above | Web | Free | Excellent visualization, easy to read |
| AMSAT pass predictor | Web | Free | Authoritative AMSAT-curated satellite list |
| Gpredict | Linux/Mac/Windows | Free | Open-source; integrates with rotator/radio |
| SatPC32 | Windows | Paid | Long-running favorite; great radio control |
| ISS Detector | iOS/Android | Free + premium | Good for ISS specifically |
| AMSAT-NA app | iOS/Android | Free | Simple pass list |
| J-Sat | Suite-bundled | Free | Built-in to ARS Suite |

For most users, Heavens-Above or AMSAT-NA's app suffice for "when's the next pass" answers.

## Generating pass predictions yourself

The math behind pass prediction is the **SGP4 propagator** consuming a TLE plus your station's coordinates and the time window. Implementations:

- Python: `sgp4` package and `pyephem` or `skyfield` for astronomy operations.
- C/C++: NORAD's reference SGP4 implementation, free.
- Web: many JavaScript libraries (sgp4-js, satellite-js, etc.).

A typical DIY workflow:

1. Fetch TLE from CelesTrak.
2. Set your station's coordinates (lat, lon, altitude).
3. For each minute over the next 24 hours, compute the satellite's position and your line-of-sight angle.
4. When the elevation angle goes from negative to positive: AOS. From positive to negative: LOS. Track the maximum elevation in between.
5. Repeat for each visible pass.

If you're a programmer interested in this, building a homebrew predictor takes an afternoon; using the existing libraries is a few lines of code.

## Common pass-prediction pitfalls

- **Wrong observer location.** A 1° lat/lon error = 100 km position error = noticeable timing changes. Get your station coordinates from your GPS or a precise mapping tool.
- **Wrong altitude.** Tracking software often defaults to "0 m above sea level"; if you're on a mountain at 1000m elevation, your horizon is lower (you see further), and predictions will be slightly off.
- **System clock wrong.** NTP-sync your computer; satellite predictions are time-critical.
- **Stale TLE.** Predictions assume current orbital state; old TLE = old state = wrong predictions.
- **Confusing UTC and local time.** Most predictions are in UTC. Your local time = UTC ± your timezone offset. Daylight saving time complicates this.
- **Underestimating low-elevation passes.** A 5° pass may technically appear in your prediction but have so much path loss and atmospheric attenuation that working it is impractical.

## See also

- §07-00 — Chapter overview
- §07-03 — Keplerian elements (the input to pass prediction)
- §07-04 — Tracking strategies (what to do during the pass)
- §07-06 — Footprints (the geometry of who's reachable when)
- §07-08 — Doppler correction tables (reference for active passes)
