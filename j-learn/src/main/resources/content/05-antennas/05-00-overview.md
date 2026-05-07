---
id: 05-00
title: Antennas — Overview
chapter: 05
section: 00
level: simple
status: draft
---

# Antennas — Overview

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The antenna is the most consequential decision in your station. A 100 W radio into a great antenna will routinely outperform a 1500 W amplifier into a compromise antenna. Money spent on the antenna pays back tenfold over money spent on anything else — including the radio itself, beyond a basic working transceiver.

This chapter walks the antenna families an HF/VHF operator actually meets, then covers the supporting concepts (matching, baluns, radiation patterns, modeling) that decide whether any given antenna will work in your specific installation.

## How the chapter is organized

The chapter goes antenna-by-antenna for the first nine sections, then pivots to the *physics and engineering* sections that explain why some antennas work in your yard and others don't.

| § | Topic | Why it's here |
|---|-------|---------------|
| 05-01 | Dipoles | The default reference antenna; everything else is compared to it |
| 05-02 | Inverted V | The dipole most hams actually build (one center support) |
| 05-03 | Verticals | Quarter-wave, half-wave, ground-plane, no-radial designs |
| 05-04 | EFHW (End-Fed Half-Wave) | One support, one feedline, multi-band — the modern wire favorite |
| 05-05 | Magnetic loops | Tiny, high-Q, the apartment-dweller's secret weapon |
| 05-06 | Full-wave loops | Quad, delta, skyloop — quiet, efficient, big |
| 05-07 | Rhombic | The classic high-gain wire DX antenna |
| 05-08 | Traps | How one antenna can cover several bands |
| 05-09 | Smith charts | The diagnostic tool for any matching problem |
| 05-10 | Feedline effects | What the coax does to your "antenna" SWR |
| 05-11 | Impedance transformation | Matching networks, transformers, ATUs |
| 05-12 | Baluns and chokes | When you need them, when you don't |
| 05-13 | Ground-plane effects | Why the same antenna at two heights performs differently |
| 05-14 | Modeling concepts | NEC, MMANA, EZNEC — what they do and don't tell you |
| 05-15 | Radiation patterns | Reading azimuth and elevation plots |

## What to read first

If you're brand new and trying to put up your first HF antenna, read **05-01 (Dipoles)** and **05-02 (Inverted V)** — together they cover what 80% of new HF stations use. Then read **05-13 (Ground-plane effects)** so you understand *why height matters* before you start arguing with your spouse about how high to put it.

If you have an antenna and it doesn't seem to work, read **05-12 (Baluns and chokes)** and **05-10 (Feedline effects)** — those two cover most of "I built it from the magazine article and it still won't tune."

If you're modeling antennas in EZNEC or MMANA, you already know enough to skip the early sections and head to **05-14** and **05-15**.

## A note on "best antenna"

There is no best antenna. There is only the best antenna *for your space, your budget, your bands, and your operating goals*. A guy with a 2-acre lot will build a different system than a guy in a third-floor apartment, and both can have rewarding stations.

What this chapter will *not* do is give you a single recommendation. What it will do is teach you enough to evaluate any antenna design and predict roughly how it will work for you before you spend money on it.

## What you will not learn here

- **Beam antennas (Yagis, log periodics)** — these get their own chapter (planned: 27).
- **Phased arrays and beverages** — top-band-specific, planned for chapter 28.
- **VHF/UHF antennas (J-poles, slim Jims, Diamond verticals)** — covered in the VHF/UHF operating chapter (planned: 30).
- **Satellite antennas (turnstiles, Yagis with circular polarization)** — see §06 (Satellites).

## Where the suite helps

- **J-Hub Antenna Tab** — builds dipole and EFHW length tables for your QTH (frequency in, length out, accounting for the 0.95 velocity factor of bare wire).
- **§08 (Antenna Calculator)** — interactive calculator referenced by this chapter.
- **§09 (Feedline & SWR)** — works hand-in-glove with §05-10 and §05-11.
- **§22 (Coax & Connectors)** — the reference appendix when picking a feedline.
