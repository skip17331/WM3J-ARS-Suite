---
id: 06-00
title: Antennas — Overview
chapter: 06
section: 00
level: simple
status: draft
---

# Antennas — Overview

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The antenna is the most consequential decision in your station. A 100 W radio into a great antenna will routinely outperform a 1500 W amplifier into a compromise antenna. Money spent on the antenna pays back tenfold over money spent on anything else — including the radio itself, beyond a basic working transceiver.

This chapter walks the antenna families an HF/VHF operator actually meets, then covers the supporting concepts (matching, baluns, radiation patterns, modeling) that decide whether any given antenna will work in your specific installation.

## How the chapter is organized

The chapter splits into two groups: the **antenna families** an operator actually builds, and the **physics and engineering** sections that explain why some antennas work in your yard and others don't.

The antenna families:

| § | Topic | Why it's here |
|---|-------|---------------|
| 06-01 | Dipoles | The default reference antenna; everything else is compared to it |
| 06-02 | Inverted V | The dipole most hams actually build (one center support) |
| 06-03 | Verticals | Quarter-wave, half-wave, ground-plane, no-radial designs |
| 06-04 | EFHW (End-Fed Half-Wave) | One support, one feedline, multi-band — the modern wire favorite |
| 06-05 | Magnetic loops | Tiny, high-Q, the apartment-dweller's secret weapon |
| 06-06 | Full-wave loops | Quad, delta, skyloop — quiet, efficient, big |
| 06-07 | Rhombic | The classic high-gain wire DX antenna |
| 06-08 | Traps | How one antenna can cover several bands |
| 06-18 | Folded dipole | Same length as a dipole, 4× the feed impedance, wider bandwidth |
| 06-19 | OCFD / Windom | Off-center feed → one balun matches several harmonic bands |
| 06-20 | G5RV / ZS6BKW | The famous (and famously misunderstood) ladder-matched wires |
| 06-21 | Doublet | Any-length wire + ladder line + tuner = every HF band |
| 06-22 | Fan dipole | One feedpoint, a full-size dipole per band, no tuner |
| 06-23 | Sloper / half-sloper | Slanted dipole, and the tower-fed quarter-wave DX wire |
| 06-24 | Random wire | The non-resonant 9:1-fed wire for when nothing else fits |
| 06-25 | Inverted-L | The classic 160/80 m DX wire from one tall-ish support |
| 06-26 | Linked dipole | The SOTA/POTA favorite — full efficiency, no tuner, flip a link to change band |

The physics and engineering sections:

| § | Topic | Why it's here |
|---|-------|---------------|
| 06-09 | Smith charts | The diagnostic tool for any matching problem |
| 06-10 | Feedline effects | What the coax does to your "antenna" SWR |
| 06-11 | Impedance transformation | Matching networks, transformers, ATUs |
| 06-12 | Baluns and chokes | When you need them, when you don't |
| 06-13 | Ground-plane effects | Why the same antenna at two heights performs differently |
| 06-14 | Modeling concepts | NEC, MMANA, EZNEC — what they do and don't tell you |
| 06-15 | Radiation patterns | Reading azimuth and elevation plots |
| 06-16 | Polarization | Horizontal, vertical, and why it matters |
| 06-17 | Diversity | Combining antennas for better receive |

> **Note on numbering —** §06-18 through §06-26 are wire-antenna families added after the original chapter was laid out. Section IDs are stable across releases (so reading-state pointers don't go stale), so they carry higher numbers but belong with the antenna families above, not after the engineering sections.

## What to read first

If you're brand new and trying to put up your first HF antenna, read **06-01 (Dipoles)** and **06-02 (Inverted V)** — together they cover what 80% of new HF stations use. Then read **06-13 (Ground-plane effects)** so you understand *why height matters* before you start arguing with your spouse about how high to put it.

If you have an antenna and it doesn't seem to work, read **06-12 (Baluns and chokes)** and **06-10 (Feedline effects)** — those two cover most of "I built it from the magazine article and it still won't tune."

If you want multi-band from one feedline, the choice is laid out across **06-04 (EFHW)**, **06-20 (G5RV/ZS6BKW)**, **06-21 (Doublet)**, **06-22 (Fan dipole)**, and **06-26 (Linked dipole)** — each trades tuner-vs-no-tuner, efficiency, and mechanical complexity differently.

If you're modeling antennas in EZNEC or MMANA, you already know enough to skip the early sections and head to **06-14** and **06-15**.

## A note on "best antenna"

There is no best antenna. There is only the best antenna *for your space, your budget, your bands, and your operating goals*. A guy with a 2-acre lot will build a different system than a guy in a third-floor apartment, and both can have rewarding stations.

What this chapter will *not* do is give you a single recommendation. What it will do is teach you enough to evaluate any antenna design and predict roughly how it will work for you before you spend money on it.

## What you will not learn here

- **Beam antennas (Yagis, log periodics)** — planned for their own chapter.
- **Phased arrays, beverages, and phasing harnesses** — phasing/stacking lines for matching and feeding two or more elements in phase are a chapter of their own (planned); top-band receive arrays live there too.
- **VHF/UHF antennas (J-poles, slim Jims, Diamond verticals)** — covered in the VHF/UHF operating material.
- **Satellite and EME antennas (turnstiles, circularly-polarized Yagis, EME dishes and arrays)** — these belong with the satellite/weak-signal material rather than the general HF/VHF antenna families here; see §07 (Satellites). A dedicated satellite/EME antenna treatment is planned.

## Where the suite helps

- **J-Hub Antenna Tab** — builds dipole and EFHW length tables for your QTH (frequency in, length out, accounting for the 0.95 velocity factor of bare wire).
- **§09 (Antenna Calculator)** — interactive calculator referenced by this chapter.
- **§10 (Feedline & SWR)** — works hand-in-glove with §06-10 and §06-11.
- **§18 (Coax & Connectors)** — the reference appendix when picking a feedline.
