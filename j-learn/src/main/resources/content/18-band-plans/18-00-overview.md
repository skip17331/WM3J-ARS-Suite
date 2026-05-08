---
id: 18-00
title: Band Plans — Overview
chapter: 18
section: 00
level: simple
status: draft
---

# Band Plans — Overview

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A band plan is the agreed-upon division of an amateur band into segments for different operating modes — CW here, digital there, voice in this slice, beacons in that one. Some of these divisions are **legal requirements** set by the FCC (47 CFR §97.301 and §97.305 in the United States); others are **voluntary conventions** organized by IARU regional bodies, national societies, and decades of operator practice.

This chapter covers both: what the FCC requires you to do (legal sub-bands by license class), and what the operating community expects you to do (voluntary conventions for finding QSOs of a particular type). Following both is what keeps an amateur band usable for everyone.

## Two distinct kinds of "band plan"

Every operator needs to understand both layers:

| Layer | What it says | Source | Penalty for violation |
|-------|--------------|--------|------------------------|
| **Legal** | "On 80 m, Generals can transmit phone from 3.800 to 4.000 MHz." | FCC 47 CFR §97.301 (US); equivalent national regulator elsewhere | License sanctions, fines |
| **Voluntary** | "On 20 m, CW from 14.000–14.150, digital from 14.070–14.110, SSB from 14.150–14.350." | IARU regional band plans, ARRL band-plan documents, club conventions, ITU recommendations | Social — you'll get told, sometimes loudly |

Most amateurs treat both layers as binding. Following the voluntary plan keeps the band orderly; ignoring it gets you yelled at on a contest weekend.

## How the chapter is organized

| § | Topic | Coverage |
|---|-------|----------|
| 24-01 | HF | 160m, 80m, 60m, 40m, 30m, 20m, 17m, 15m, 12m, 10m — both regulatory and voluntary |
| 24-02 | VHF | 6m, 2m, 1.25m — repeater allocations, weak-signal segments, FM voice |
| 24-03 | UHF | 70cm, 33cm, 23cm — repeater pairs, satellite sub-bands, weak-signal |
| 24-04 | Satellite sub-bands | Specific segments reserved for satellite uplinks/downlinks; ITU footnotes |
| 24-05 | Regional variations | IARU regions, country-specific allocations, DX considerations |

## Why band plans exist

Two real reasons:

### 1. Modes don't share well

A 1500 W SSB voice station next door to an FT8 receiver kills the FT8 receiver's ability to decode anything. A high-power CW signal will splatter across nearby bandwidth. Different modes need their own segments to coexist.

### 2. Operating expectations need predictability

If you're tuning across 20 m looking for a CW QSO, you expect to find them in 14.000-14.150. If you're calling CQ on 14.245, you expect SSB voice, not someone trying to start an FT8 contact at that frequency. The voluntary band plan codifies these expectations.

## Anatomy of a typical HF voluntary band

For a 20 m band (14.000–14.350 MHz) example:

```
14.000──┬──────CW────────┬──Digital──┬──────────SSB Phone──────────┬──14.350
        │                │           │                              │
     14.000           14.070      14.150                          14.350
                                       
       Wide CW  Narrow CW    PSK/RTTY/   Lower SSB phone    Upper SSB phone
                              FT8/FT4    (DX/contests)      (general/nets)
```

Within these voluntary segments, finer conventions exist:

- **Beacon networks** (NCDXF on 14.100, IARU on 14.099-14.101) — keep clear.
- **DX windows** (e.g., 14.020-14.040 for DX CW; 14.180-14.200 for DX SSB) — when DX-pedition is operating split, this is the window.
- **Net frequencies** (e.g., 14.300 for the Maritime Mobile Service Net) — historic and ongoing.
- **Mode-specific frequencies** (FT8 on 14.074, JS8 on 14.078, FT4 on 14.080, etc.) — these are *de facto* standards.

## License classes and their segments

In the US, the FCC license class determines which sub-bands of each band you may use:

- **Technician class** — limited HF (10m phone and below), full VHF/UHF.
- **General class** — most HF, with some sub-band restrictions.
- **Amateur Extra class** — most HF, including portions reserved for Extras.

The exact boundaries for each license class on each band are detailed in §18-01 (HF). A general operator running outside a General-permitted segment is in violation of FCC rules — specifically 47 CFR §97.301.

## Mode flexibility within a band plan

The voluntary band plan describes *typical* mode usage; it doesn't legally restrict you. You can transmit SSB voice in a CW-conventional segment if you want. But:

- You'll be interfering with operators who expect CW there.
- You're likely to get told (in CW, by someone slow enough you can copy: "You are not in a phone segment").
- During a contest, the displeasure can be loud.

The legal sub-bands (FCC 97.301) DO limit your mode use — for example, 80 m phone is restricted to 3.800–4.000 MHz for Generals. See §18-01 for the details.

## How band plans evolve

Band plans change. New modes (FT8 in 2017, FT4 in 2018) get assigned new sub-bands. Old modes (RTTY at lower frequencies) shift as digital usage moves around. Bands get re-allocated by the FCC (60 m was added in 2003 with five specific channels; expanded in 2017). Voluntary segments shift as operating practices change.

This means the band plan you learned 10 years ago may not be current. Check current ARRL or IARU band-plan documents at least annually, especially if you've been off the air.

## What the suite gives you

J-Hub's Cluster tab and Logging tab use band-plan data internally to identify the band of a given frequency. The data behind those features is in J-Hub's `band_plans.json` configuration. If you operate in a region not well-supported by the default config, you can edit it.

## What you will not learn here

- **The legal regulations of countries other than the US.** Briefly covered in §18-05 (regional variations); for full details, consult your country's regulator.
- **Specific contest rules.** Contest frequency conventions vary by sponsor (CQ Magazine vs. ARRL contests vs. IARU vs. WAE). Read the contest's specific rules.
- **Net-specific frequencies and schedules.** These are tracked by the various nets themselves (Maritime Mobile Service Net at 14.300, North American Traffic at 7.230, etc.).

## See also

- §18-01 — HF (the bulk of HF detail)
- §18-02 — VHF
- §18-03 — UHF
- §18-04 — Satellite sub-bands
- §18-05 — Regional variations
- §02 — Repeaters & bandplans (operating-procedure focused, complementary to this chapter's reference focus)
- §05 — Satellites (detailed satellite operation; this chapter covers just the band-plan slot)
