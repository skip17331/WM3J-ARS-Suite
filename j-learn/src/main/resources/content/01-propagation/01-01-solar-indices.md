---
id: 01-01
title: Solar Indices (SFI, A-index, K-index)
chapter: 01
section: 01
level: mixed
status: draft
---

# Solar Indices (SFI, A-index, K-index)

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Three numbers tell you almost everything about today's HF propagation. They appear on every cluster, every solar dashboard, every QST cover blurb. Learn them once and you can read the bands at a glance.

## The three numbers, plain English

| Number | What it tells you | Want it to be… |
|--------|-------------------|-----------------|
| **SFI** (Solar Flux Index) | How "loud" the sun is right now | **High** — more SFI means higher MUF means higher bands open |
| **K-index** | How disturbed Earth's magnetic field is right now (over the last 3 hours) | **Low** — high K = absorption, fading, polar blackout |
| **A-index** | How disturbed it's been over the last 24 hours (running average of K) | **Low** — same reason |

Quick reading:

- **SFI 70** = quiet sun, only the lowest HF bands work well, no DX above 20 m.
- **SFI 130–180** = good conditions, 15 m and 10 m wake up.
- **SFI 200+** = great conditions, 10 m and 6 m can open worldwide.
- **K = 0–2** = quiet — bands sound clean.
- **K = 4** = noisy, signals fading, paths through the auroral zone are unreliable.
- **K = 5+** = magnetic storm, polar paths dead, even mid-latitude paths suffer.
- **A under 10** = quiet day. **A over 30** = bad day, especially for north-south paths through the auroral zone.

## SFI in detail

Officially the **10.7 cm solar radio flux** — the radio brightness of the entire sun at 2800 MHz. Measured every day at noon by Penticton observatory in Canada and broadcast worldwide. Reported in solar flux units (SFU); 1 SFU = 10⁻²² W·m⁻²·Hz⁻¹.

Why this number? Because the same physical processes that make the sun bright at 10.7 cm also produce the **EUV** (extreme ultraviolet) radiation that ionizes our F-layer. EUV is hard to measure from the ground (atmosphere absorbs it). 10.7 cm radiation passes through cleanly and tracks EUV closely enough to be a great proxy.

> **Advanced —** The 10.7 cm proxy isn't perfect. EUV peaks slightly earlier in the solar cycle than 10.7 cm does, and at very high SFI levels (200+) the relationship saturates. Models that need real ionization rates use the **F10.7** or — better — direct EUV measurements from the GOES SUVI instrument and SDO/EVE. For everyday operating, treat SFI as a linear ionization gauge and you'll be right far more often than wrong.

Typical range: **65 (deep solar minimum) to 300+ (cycle peak)**. During Cycle 24's maximum (~2014) we saw 150–180. Cycle 25 is currently producing readings in the 150–220 range; numbers above 200 are common.

## K-index in detail

The **K-index** is a quasi-logarithmic measure of how much Earth's magnetic field at a given site has been wiggling around in the last three hours. It runs from **0 (perfectly quiet) to 9 (severe storm)**.

You will see two flavors:

- **Local K** — measured at one observatory.
- **Planetary K (Kp)** — combined from a worldwide network of observatories. This is what amateur dashboards usually show. NOAA publishes Kp every three hours.

What it means at the radio:

| Kp | Conditions | Notes |
|----|------------|-------|
| 0–1 | Very quiet | Best DX conditions; deep noise floor |
| 2–3 | Quiet | Normal operating |
| 4   | Unsettled | Noticeable fading on some paths |
| 5   | Minor storm (G1) | Polar paths degraded; aurora possible at high latitudes |
| 6   | Moderate storm (G2) | Auroral zone paths blacked out, mid-latitudes affected |
| 7–8 | Strong storm (G3–G4) | Widespread blackouts; visual aurora down to mid-latitudes |
| 9   | Extreme (G5) | Blackout of HF; possible damage to satellites and grids |

> **Advanced —** The K-index is logarithmic with a non-uniform spacing. Each step roughly doubles the disturbance level: K=4 is about twice K=3 in nT terms, K=5 about twice K=4, etc. The full scale was designed by Bartels in 1939 and tied to specific ranges of horizontal magnetic field deviation in nT, with the boundaries calibrated per observatory so all stations report comparable numbers. The "estimated K" you see in real time is a forecast; the official Kp arrives a few hours later.

## A-index in detail

The A-index is a **24-hour summary** of K. It's a linear daily average, not logarithmic, so it's directly comparable day to day.

Rough scale:

- **A = 0–7** — quiet day
- **A = 8–15** — unsettled
- **A = 16–29** — active
- **A = 30–49** — minor storm day
- **A = 50–99** — major storm
- **A = 100+** — severe storm day

The A-index is more useful than K for understanding the *trend*. Three days of A=25 in a row tells you the band conditions have been chronically poor; you would not see that pattern in a single K reading.

## Putting them together

| SFI | K | A | What you do |
|-----|---|---|-------------|
| 80, 1, 5 | Low SFI, quiet | Stay on 40 m and 80 m for any reliable QSO. 20 m might open mid-day. |
| 150, 2, 8 | Average to good | Try every band 80 m through 15 m. 10 m might surprise you. |
| 200, 4, 22 | Good flux, but stirred up | High bands open in theory; expect QSB and noise. Avoid polar paths. |
| 200, 6, 60 | Storm conditions | High bands degraded badly, especially north-south. Consider 80 m / 40 m and wait it out. |
| 70, 0, 3 | Quiet sun, quiet field | Low bands only. Good day for 160 m DX if there is any. |

## Where to get them

- **hamqsl.com** — banner image you can embed; shows SFI, A, K, plus bandcondition guesses.
- **NOAA SWPC** — `services.swpc.noaa.gov` — the authoritative source. JSON endpoints for SFI, K, A, X-ray flux, solar wind.
- **J-Hub Dashboard** — pulls from NOAA SWPC every few minutes and shows the headline numbers next to your station info.

## Common mistakes

- **Treating high SFI as a guarantee.** SFI sets the *ceiling* — it tells you the bands *could* open. Whether they actually do depends on the K-index and on the path you're trying.
- **Ignoring the K when calling a DX-pedition on the polar path.** SFI 200, K 5: 10 m might be wide open from FL to EU but dead from FL to JA over the pole.
- **Confusing K with A.** A=10, K=4 is "quiet last 21 hours, started getting noisy in the last 3." A=40, K=2 is "rough day, but it's settling now." Read both, not just one.

## See also

- §01-04 — what each layer does, and why SFI matters most for F2.
- §01-05 — how SFI varies over an 11-year cycle.
- §01-07 — how prediction models (VOACAP) consume these numbers as inputs.
