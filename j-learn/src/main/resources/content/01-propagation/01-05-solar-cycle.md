---
id: 01-05
title: Solar Cycle
chapter: 01
section: 05
level: mixed
status: draft
---

# Solar Cycle

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The sun goes through an **~11-year cycle** of activity. At minimum it's quiet — few sunspots, low solar flux, only the lower bands work for DX. At maximum it's energetic — many sunspots, high flux, 10 m and 6 m open daily. Knowing where you are in the cycle tells you which bands are worth a long-term antenna investment and which DX hunts are realistic.

## What "solar cycle" actually means

Two things vary together:

1. **Sunspot count** — dark patches on the sun's surface where strong magnetic fields suppress convection. Counted daily; reported as the **SSN** (sunspot number) or smoothed monthly mean (SSN-12).
2. **Solar flux** — total radio brightness at 10.7 cm, the **SFI** introduced in §01-01.

When you see a chart of "the solar cycle", it's usually SSN or SFI plotted over decades. The shape: a steep rise of 3–4 years, a peak that lasts 1–2 years, and a longer decline of 5–6 years before the next minimum.

Cycles are numbered. Cycle 1 began in 1755. We are currently in **Cycle 25**, which began at minimum in late 2019 and is around its peak as of 2025–2026.

## What changes between minimum and maximum

| Aspect | Solar minimum | Solar maximum |
|--------|---------------|---------------|
| Sunspot number | 0–25 | 100–250 |
| 10.7 cm flux (SFI) | 65–90 | 150–280 |
| Daytime MUF (mid-lat) | 14–18 MHz | 28–35 MHz |
| 10 m DX | Almost none, except sporadic E | Open daily, worldwide |
| 6 m F2 | Essentially never | Possible during big bursts |
| 80 m / 160 m DX | Excellent — quiet sun, low noise | Still good, but more storms |
| Auroras | Rare | Common |
| Geomagnetic storms | Few | Many — and severe |

The takeaway: high solar activity gives you the **high bands**; low solar activity quiets things down enough for **the low bands** to shine. There's something for everyone in every part of the cycle.

## Where in the cycle are we right now?

Cycle 25 timeline (as best we know):

- **Minimum:** December 2019 (very deep — long stretch of zero-sunspot days in 2018–2020)
- **Predicted peak:** mid-2024 to mid-2025 (early forecasts said late 2025; the cycle came in stronger and earlier than predicted)
- **Predicted next minimum:** ~2030

Cycle 25 has surprised forecasters by exceeding the official NOAA prediction (peak SSN around 115). Actual smoothed sunspot numbers are running 150+, the highest since Cycle 23 in the early 2000s. SFI readings of 200+ are common in 2025–2026.

> **Advanced —** Cycle prediction is hard. The Solar Cycle Prediction Panel uses a combination of polar magnetic field strength at the previous minimum, dynamo models, and statistical analysis of past cycles. Cycle 25's stronger-than-predicted performance has been attributed to the polar field at minimum being slightly stronger than initially measured, plus the dynamo model under-weighting recent cycle behavior. The next official update is in 2027.

## Bands by part of cycle

**Solar minimum (~2018–2021, ~2029–2032):**
- **Daily workhorses:** 40 m, 80 m, 160 m
- **Daytime DX:** 20 m, occasionally 17 m
- **High bands (15 / 12 / 10):** rare openings, mostly via sporadic E
- **6 m:** sporadic E in summer; no F2 at all
- **DX strategy:** quiet sun = quiet bands = 80 m / 160 m DX is at its best

**Cycle rise (~2021–2024):**
- 17 m and 15 m wake up first, then 12 m, then 10 m
- 10 m starts producing DX during daytime, expanding from equatorial paths outward
- Geomagnetic storms become common — bring chaos to the high bands
- F2 on 6 m becomes possible at peak

**Solar maximum (~2024–2027 for Cycle 25):**
- **All HF bands open during the day** somewhere on the planet
- **10 m is the headline band** — worldwide DX with low power, simple antennas
- **6 m F2** opens occasionally, an event when it does
- Auroras visible at lower latitudes
- Polar paths suffer during storms — even short paths can blackout briefly

**Cycle decline (~2027–2030):**
- Mirror image of the rise — the high bands close down progressively
- Bands above 10 m drop off first, then 10 m itself becomes intermittent
- Operators "tune up" their low-band antennas in preparation for the next minimum

## Why does the cycle exist?

The standard explanation is the **dynamo model**: the sun's interior is a churning ball of plasma with differential rotation (equator faster than poles) and convection cells. Over years, this winds up the solar magnetic field like rubber bands until it twists into knots, which break through the surface as sunspots. The whole field then reverses polarity and the cycle starts over.

> **Advanced —** The 11-year sunspot cycle is actually one half of a 22-year **Hale cycle**: each successive 11-year cycle has reversed magnetic polarity in its sunspot pairs. There is also a longer **Gleissberg cycle** of about 80–90 years, modulating the amplitude of the 11-year cycles, plus a **Suess/de Vries cycle** of about 200 years apparent in cosmogenic isotope records. None of these long-period modulations are fully explained by current dynamo theory. Periods of unusually deep cycles (like the Dalton Minimum 1790–1830 and the Maunder Minimum 1645–1715) have profound climatic and propagation consequences but happen too rarely to predict reliably.

## What about the Maunder Minimum?

Between 1645 and 1715, sunspots nearly disappeared from the sun for ~70 years. This was before radio existed, but the climate impact (the "Little Ice Age") tells us such deep slumps are real. There is no convincing theory yet for *why* they happen. If we entered another Maunder-like minimum, HF DX above 14 MHz would essentially vanish for a generation. Most current models say this is unlikely but not impossible.

## Practical operating advice

- **Plan antenna investments around your operating life, not the current cycle.** A 10 m yagi is glorious in 2025 but expensive scrap in 2030. A 40 m wire is useful every year of your life.
- **Chase rare DX during the cycle peak.** DX-peditions know it too — most of the big rare-DXCC operations happen in the 4–5 years around solar max.
- **Start low band DXing during the cycle decline.** You'll have several years of quiet sun ahead of you to work the world on 80 m and 160 m.
- **Watch the smoothed monthly SSN, not the daily one.** A few quiet days during solar max don't mean the cycle is collapsing.
- **Solar storms are not the cycle.** A flare that takes out 10 m for 6 hours is weather, not climate.

## Where to track it

- **NOAA SWPC Solar Cycle Progression** — `swpc.noaa.gov/products/solar-cycle-progression` — official chart of observed and predicted SSN.
- **SILSO** (Royal Observatory of Belgium) — the keeper of the official sunspot number.
- **Spaceweather.com** — daily observed SSN, accessible commentary.
- **J-Hub Dashboard** — current SFI; for historical context, look at the SSN graph on any of the above.

## See also

- §01-01 — the solar indices that report the cycle's day-by-day state
- §01-04 — F2 ionization, which is what the cycle actually drives
- §01-08 — band choice when you don't have time to think
