---
id: 09-00
title: Antenna Workshop — Overview
chapter: 09
section: 00
level: simple
status: published
---

# Antenna Workshop — Overview

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

This chapter is the **antenna design and calculation workshop** for the ARS Suite. It pairs a comprehensive set of calculators (lengths, trap values, loading coils, Yagi geometry, OCF feed points, EFHW transformer ratios) with a **questionnaire-driven recommender** that suggests appropriate antenna types based on your living situation, lot, supports, and operating goals.

The unique value here is **integration**. There are excellent single-purpose calculators scattered across the web — W4SAT for dipole length, K7MEM for Yagi design, KV5R for loading coils, eHam threads for trap design, etc. — but no central place that pulls them together, links them to your specific QTH constraints, and suggests *what to build*. That's what this chapter and the J-Hub **Antenna Workshop** tab do.

## What's in this chapter

| § | Section | Purpose |
|---|---------|---------|
| 09-00 | This overview | How the chapter is organized |
| 09-01 | Antenna Recommender | The questionnaire and scoring rules |
| 09-02 | Flat Dipole | Half-wave dipole, length math, height-vs-Z |
| 09-03 | Inverted-V Dipole | Apex height, droop angle, length correction |
| 09-04 | Fan Dipole | Multi-band parallel dipoles, mutual coupling |
| 09-05 | Trapped Dipole | Multi-band with traps, leg lengths between traps |
| 09-06 | OCF Dipole (Windom) | Off-center feed, balun ratio, harmonic feed-Z |
| 09-07 | EFHW (No Traps) | Half-wave end-fed, 49:1 / 64:1 unun, harmonics |
| 09-08 | EFHW (Trapped) | Multi-band end-fed with traps |
| 09-09 | J-Pole | VHF/UHF half-wave + matching stub |
| 09-10 | Yagi-Uda | Driven, reflector, directors, spacing, expected gain/F-B |
| 09-11 | Vertical Antennas | ¼λ / ⅝λ / ½λ + radial design |
| 09-12 | Loading for Shortened Antennas | Base, center, top loading coils |
| 09-13 | Trap Design & Manufacturing | L-C values + physical wire-on-form construction |
| 09-14 | Magnetic Loop | Tuning capacitor, Q, voltage breakdown |
| 09-15 | NanoVNA Trim Workflow | The standard "build → sweep → trim" loop |

## How the workshop is organized

Three layers, each appropriate for a different question:

| Layer | Question it answers | Where it lives |
|-------|---------------------|----------------|
| **Recommender** | "What antenna should I build?" | §09-01 + UI wizard |
| **Calculators** | "Given antenna type X, what dimensions?" | Each per-antenna section + UI calc panels |
| **Trim workflow** | "I built it; what does the sweep tell me?" | §09-15 |

The recommender is the entry point for new operators or anyone evaluating a new QTH; the calculators are the working tools for design; the trim workflow is the post-build verification.

## The interactive Antenna Workshop UI

The chapter pairs with a J-Hub web UI tab — **Antenna Workshop** — that implements the actual calculations and the recommender wizard. Every chapter section that documents a calculator includes a **▶ Launch in calculator** button (same pattern as §05 → Morse Trainer) that jumps to the matching panel in the UI.

The UI is browser-side JavaScript — calculations run instantly with no server round-trip, and you can change inputs and watch outputs update in real time. The math is documented in this chapter so the calculator isn't a black box; if a number looks wrong you can find the formula here and check the work.

## What this chapter is *not*

- **Not** an antenna theory chapter — that's **§06 Antennas**, which explains how each type works, polarization, ground effects, modeling. This chapter assumes you've decided what to build (or are using the recommender to decide) and need the numbers.
- **Not** a buyers' guide — the calculators output dimensions and material specs, not "buy this kit." Pre-built antennas have their own kits (Buddipole, Hustler, Cushcraft, MFJ, DX Engineering); the calculator is for the homebrewer or anyone wanting to verify a kit's claims.
- **Not** a substitute for NEC-2 / 4nec2 / EZNEC modeling. For Yagi optimization, ground-plane modeling, complex arrays, you'll outgrow the closed-form calculations here. They're the right starting point and good enough for most installs.

## Underlying math

All formulas live in the chapter cards and reference back to **§17 Formula Appendix** for the foundational math:

| When you need... | Look at... |
|------------------|-----------|
| Half-wave length, quarter-wave length | §17-06 Wavelength |
| Resonant L-C for traps | §17-05 Resonant Frequency |
| Reactance of loading coils | §17-03 Reactance |
| Smith chart math for matching networks | §17-13 Smith Chart Basics |
| Q calculation for traps and mag loops | §17-11 Q Factor |
| Loss budget through feedline | §17-09 Feedline Loss |

The §17 cards have the full derivations; §09 cards have the *applied* math (with end-effect corrections, height factors, etc.) for each antenna type.

## How accurate are these calculations?

For the lengths (dipole, EFHW, vertical), expect **±3–8%** off the calculator's prediction in real installations. Variables not captured in closed-form math:

- **Wire diameter and end effects** — fat aluminum tubing has a different end-correction than thin wire.
- **Height above ground (in wavelengths)** — affects feed Z and length resonance significantly.
- **Nearby conductors** — gutters, downspouts, other antennas, towers all detune.
- **Ground conductivity** — sandy soil and salt marsh behave very differently.
- **Construction quality** — sloppy soldering at the feed point, water in the balun, kinked element joints.

Plan to **trim** every antenna after installation. The calculators give you a starting length; the analyzer tells you the truth. §09-15 walks through the trim workflow.

> **Advanced —** For installations where ±5% length error matters (matching restored antique gear, EME on 1296 MHz, narrow-band beacons), use a NEC-2 or NEC-4 modeling package (4nec2, EZNEC, MMANA-GAL) instead of these closed-form formulas. The calculator's purpose is to get you 95% of the way for 1% of the modeling effort.

## See also

- §06 — Antennas (theory chapter, deep dives on each antenna family)
- §12 — High-SWR troubleshooting (when the trim sweep doesn't match the calculator)
- §17 — Formula Appendix (foundational math)
- §18 — Coax & Connectors (feedline choice — affects what your antenna sees)
