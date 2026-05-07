---
id: 01-07
title: Prediction Models
chapter: 01
section: 07
level: advanced
status: draft
---

# Prediction Models

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

> ⚙️ **Advanced —** This entire chapter is at engineering depth. The basics in §01-01 through §01-06 cover what you need for everyday operating; this section is for operators who want to plan an expedition, design a circuit budget, or understand why VOACAP says one thing and reality says another.

Propagation models take the physical state of the ionosphere — built from worldwide ionosonde measurements, monthly statistics, and parametric tweaks — and compute the expected signal-to-noise ratio for a specified path, frequency, time, and antenna configuration. They are not crystal balls; they are statistical climatology. Used right, they're the most powerful planning tool you have.

## The standard models

| Model | Origin | Best for | Notes |
|-------|--------|----------|-------|
| **VOACAP** | NTIA / Voice of America | HF circuits, point-to-point | The de-facto amateur standard; freely available |
| **ICEPAC** | NTIA | HF, including high-latitude paths | More accurate than VOACAP for auroral paths |
| **REC533** | ITU-R | Reference for ITU planning | Used internally by many other tools |
| **HFProp / Proppy** | Multiple | User-friendly amateur front-end | Wraps REC533 / ITURHFProp |
| **ITURHFProp** | ITU-R | Modern reference implementation | Successor recommendation to REC533 |

### VOACAP — the workhorse

Originally written for Voice of America to plan their HF broadcast schedules. Released to the public in the 1990s and ported to Windows by Greg Hand. It is the model that almost every amateur tool — VOACAPWeb, ITSHFBC, hf-prop, propagation maps in N1MM and DXLab — actually calls under the hood.

**Inputs:**
- Transmit and receive locations (lat/lon)
- Month and time (UTC)
- SSN (smoothed sunspot number) for the period
- Antenna patterns and gains at both ends
- Transmit power
- Required reliability (50%, 90% etc.)
- Required SNR for the mode

**Outputs (per frequency, per UTC hour):**
- **MUF** — predicted maximum usable frequency
- **MUFday** — fraction of days the mode is open
- **REL** — reliability (fraction of days SNR target is met)
- **SNR** — predicted signal-to-noise in dB-Hz
- **SDBW** — signal power in dB relative to 1 W
- **DBU** — signal field strength in dB µV/m
- Path multi-hop mode geometry (1F, 2F, 1E1F, etc.)

**What it gets right:** monthly average behavior of well-traveled mid-latitude paths, the rough shape of the daily MUF curve, the relative ranking of bands at a given hour.

**What it gets wrong:** day-to-day variability, transient events (flares, CMEs, auroras), high-latitude paths during disturbed conditions, sporadic E (it doesn't try), greyline-specific enhancements.

> ⚙️ **Advanced —** VOACAP's ionosphere model is **CCIR Recommendation 894** (the URSI-coefficient model), driven by a single-number SSN input. The model is parametric: it interpolates between empirically measured monthly median ionospheric parameters at a global grid of measurement points. It does not couple to real-time space weather. There are several "modes" of computation (Method 30 is the most common modern one), and the optional **VOAAREA** mode produces world maps of expected reliability or SNR for a fixed transmitter.

### ICEPAC

Built by NTIA as an upgrade to VOACAP, particularly for high-latitude paths where VOACAP's auroral-zone model is weak. ICEPAC implements ICED (Ionospheric Conductivity and Electron Density) for the auroral region. For paths through the polar cap or auroral oval, ICEPAC predictions are noticeably better than VOACAP. Otherwise the two are roughly equivalent.

### ITURHFProp

The current ITU-R recommendation for HF prediction (ITU-R Rec. P.533-14). It's the modern reference for international circuit planning. The model is more sophisticated than VOACAP — it uses a finer ionospheric grid, includes spread-F effects, and handles low-frequency loss more accurately. The main reason VOACAP still dominates among amateurs is inertia: the ham tools are written around VOACAP, and ITURHFProp's outputs aren't yet wrapped in nicer front-ends.

### Proppy / HFProp

Web-based amateur tools that consume ITURHFProp under the hood. Proppy (`soundbytes.asia/proppy`) gives world maps, point-to-point predictions, and 24-hour band-by-band forecasts in a clean browser UI. For a quick "what's good in the next 6 hours" answer, Proppy is the easiest tool to use.

## How to interpret model output

The headline number is **reliability** — the fraction of days the SNR target will be met for that frequency at that hour. A reliability of 0.5 means "the signal is good enough on half the days." A reliability of 0.9 means "almost always."

Common gotchas:

- **Reliability is per hour, not per minute.** A 90% reliability hour can still have several minutes of fading inside it.
- **The SNR target depends on the mode.** SSB needs ~10 dB; CW needs ~3 dB; FT8 works at –24 dB. A circuit that VOACAP says is "unreliable for SSB" can be wide open for FT8.
- **Monthly median, not today.** The model's prediction is a statistical center for the month — today might be much better or much worse.
- **MUFday is not MUF.** The MUF is the predicted highest reflected frequency; MUFday is the fraction of days the path is open at all. They tell different stories.

## Inputs to be picky about

### SSN input
Use the **smoothed monthly mean** (SSN-12, the 12-month centered running mean), not yesterday's sunspot count. A daily count of 200 doesn't help you plan a weekend QSO if the smoothed mean is 130.

For the current month you can use the official **NOAA predicted SSN** for that month. SIDC publishes both the smoothed and the predicted values monthly.

### Antenna model
The default in many ham VOACAP front-ends is "isotropic" — which makes the predictions pessimistic for any real ham antenna. If you have even a basic dipole, model it properly: VOACAP includes patterns for many standard antennas, and you can supply your own gain-vs-elevation table.

A dipole at 10 m height on 20 m has very different elevation gain at 5° (DX-friendly) vs 60° (NVIS-friendly). The model only knows what you tell it.

### Required SNR
Default is often 38 dB-Hz (suitable for SSB). For CW use 24 dB-Hz; for FT8 use 14 dB-Hz; for FT4 use 17 dB-Hz; for RTTY use 32 dB-Hz. Wrong SNR target will make you skip openings that are actually fine for your mode.

### Power
The model linearly scales SNR with TX power, which is roughly true. 100 W vs 1 kW is a 10 dB difference in predicted SNR.

## When the model is wrong, why?

Common causes of model-vs-reality divergence:

1. **Solar activity has changed since the SSN you supplied.** VOACAP doesn't update with real time data; if today's SFI is 80 and you put in SSN 130, VOACAP overestimates the MUF.
2. **Geomagnetic storm in progress.** Models assume quiet conditions. K=6 days will eat 10–20 dB out of polar-zone predictions.
3. **Sporadic E is open** — VOACAP doesn't know about it, so 6 m predictions look terrible while you're working JA on 50.110.
4. **Path length is short.** Models do worst on paths under 1500 km because the geometry is unusual.
5. **Antenna model is wrong.** This is the single most common source of "the model is broken" complaints. Real antennas behave very differently from the textbook plot.
6. **Local QRM/QRN.** The model predicts the path; your noise floor predicts whether you can use it.

## Practical workflow

A reasonable planning workflow for, say, a DX-pedition or a contest weekend:

1. **Pick the SSN-12 from SIDC's prediction for the month.** Note that the prediction is updated monthly — use the latest.
2. **Run a 24-hour Proppy or VOACAP prediction** for each band you intend to operate, between your QTH and your target region.
3. **Look for the bands and hours where reliability > 0.7** for your mode. These are your "scheduled" hours.
4. **Look for marginal hours (reliability 0.3–0.7)** — these are worth checking but may or may not work.
5. **Cross-reference with the real-time SFI/K** when the day comes. If actual conditions are quieter, drop a band; if they're better, climb a band.
6. **Use PSK Reporter as ground truth.** The model predicts, PSK Reporter measures.

## Tools roundup

- **VOACAPOnline** (`voacap.com/online.html`) — runs VOACAP in a browser, point-to-point or area coverage.
- **Proppy** (`soundbytes.asia/proppy`) — ITURHFProp-based, very clean UI, world maps.
- **ITSHFBC** — Greg Hand's Windows VOACAP front-end; the desktop reference tool.
- **VOAProp** — older but still useful Windows app, shows world maps with great-circle paths overlaid.
- **PSK Reporter** — empirical reality check; shows what's actually being heard right now on every band.

## Bottom line

Models tell you **where to look first**. They don't tell you what's open right now. Combine them with the real-time SFI/K and with PSK Reporter, and you have a complete picture: the model gives you the climatology, the indices give you today's weather, and PSK Reporter gives you "and here's what just happened."

## See also

- §01-01 — solar indices that feed the model
- §01-02 — the MUF and LUF that the model is computing
- §01-04 — the layers the model is simulating
- §01-05 — the SSN-12 input and where the cycle is now
