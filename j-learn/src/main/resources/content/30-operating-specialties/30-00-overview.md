---
id: 30-00
title: Overview — Operating Specialties
chapter: 30
section: 00
level: mixed
status: published
---

# Operating Specialties — Overview

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Most amateur radio operators spend their on-air time on a handful of well-trodden paths: HF SSB ragchews, FM repeaters, FT8, the occasional contest weekend. The bands offer far more than that. There's a layer of **specialty operating** that uses propagation paths and operating styles the casual ham never touches — paths that turn 80 m into a statewide intercom, paths that bounce VHF signals off ionized meteor trails, paths that go *through the moon*.

This chapter is a tour of those specialties. Each one solves a problem that conventional operating can't, or opens a band-and-mode combination that rewards the operator who knows where to look.

## The propagation specialties

The first half of the chapter is about **getting signals to places conventional propagation won't take them**. Each mode exploits a specific physical mechanism:

| Specialty | Mechanism | Bands | Range |
|-----------|-----------|-------|-------|
| **NVIS** | Near-vertical skywave — signal goes nearly straight up, comes back close | 80 m, 40 m, sometimes 60 m | 0–300 mi (no skip zone) |
| **Meteor scatter** | Reflection off ionized meteor trails 80–120 km up | 6 m, 2 m | 500–2200 km |
| **EME (moonbounce)** | Reflection off the lunar surface | 2 m, 70 cm, 23 cm, up | Anywhere the Moon is mutually visible |
| **Tropospheric ducting** | Refraction in a weather-driven atmospheric layer | 6 m → 23 cm | 500–1500 mi |
| **Aircraft scatter** | Reflection off airliner fuselages | 2 m, 70 cm, 23 cm | 100–500 mi |

These are all "weak-signal" modes in spirit — see §01-09 for the propagation side of the same story. This chapter focuses on the *operating side*: how to actually run a contact, what equipment is needed, what protocol the on-air community uses, and when each shines.

## The portable / mobile specialties

The second half is about **where the operator and station are physically located** — not just at a desk at home.

| Specialty | What it is |
|-----------|------------|
| **Maritime mobile (/MM)** | Operating from a vessel at sea |
| **Aeronautical mobile (/AM)** | Operating from aircraft (mostly balloons and drones in practice) |
| **SOTA** | Activating mountain summits worldwide |
| **POTA** | Activating parks — drive-up version of SOTA |

SOTA and POTA in particular have transformed amateur radio in the last 15 years. They turned portable operating from a fringe activity into a mainstream pursuit, with database-backed point systems, structured exchanges, and tens of thousands of active participants worldwide.

## Why specialty modes matter

A few reasons to bother:

- **They keep amateur radio interesting** when you've already worked all states on every common HF band. There's always one more rare propagation mode, one more remote summit, one more contest where a specialty technique gives you an edge.
- **They build skills** that come back when conditions get marginal — knowing how to dig out a weak EME signal trains the same ear that helps you copy a marginal HF DX through QRN.
- **They reach places conventional ops can't.** NVIS gets your signal to the next county over when ground wave fails. EME gets you to *any* mutually visible station on Earth without ionospheric help. POTA puts you on a band from a state park 50 miles from the nearest neighbor.
- **They're often the first to recover after band collapses.** When the F-layer dies (solar minimum, geomagnetic storm), tropo and meteor scatter on VHF carry on.

## Common threads

Every specialty in this chapter shares a few traits:

- **Specialized equipment matters but isn't always exotic.** A NVIS dipole is just a regular 80 m dipole strung low. A POTA station is a 100 W rig and a wire. EME is the exception — that one really does need big antennas and a kilowatt.
- **Operating protocol is specific.** Each specialty has its own QSO format, its own scheduling system (or lack of one), its own database of contacts and points.
- **The community is small and helpful.** Specialty operators talk to each other on dedicated reflectors, websites, and on-air nets. The barrier to entry is mostly *finding* the community — the operators themselves welcome newcomers.

## Sections in this chapter

| § | Title | Level |
|---|-------|-------|
| 30-01 | NVIS — Near-Vertical-Incidence Skywave | mixed |
| 30-02 | Meteor Scatter Operating | advanced |
| 30-03 | EME — Earth-Moon-Earth Basics | advanced |
| 30-04 | Tropospheric Ducting | mixed |
| 30-05 | Aircraft Scatter | mixed |
| 30-06 | Maritime Mobile (/MM) | mixed |
| 30-07 | Aeronautical Mobile (/AM) | advanced |
| 30-08 | SOTA — Summits On The Air | simple |
| 30-09 | POTA — Parks On The Air | simple |

## See also

- [§01-09 — Weak-Signal VHF/UHF](../01-propagation/01-09-weak-signal.md) — propagation physics for EME, tropo, MS, aurora
- [§07 — Satellites](../07-satellites/) — the comparison case: a deliberate orbital reflector instead of a natural one
- [§22 — Operating Practice](../22-operating-practice/) — calling discipline, split, identifying
- [§23 — HF Operating Techniques](../23-hf-operating/) — the conventional baseline these specialties depart from
- [§06 — Antennas](../06-antennas/) — antenna choices for portable, NVIS, and weak-signal work
- [§21 — Emergency Communications](../21-emcomm/) — NVIS overlaps heavily with statewide emcomm
