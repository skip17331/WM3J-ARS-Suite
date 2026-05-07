---
id: 20-00
title: Maintenance — Overview
chapter: 20
section: 00
level: simple
status: draft
---

# Maintenance — Overview

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A radio station is a system of components installed outdoors, indoors, on poles, in vehicles, behind walls — exposed to UV, water, ice, vibration, current cycling, and the slow march of firmware bugs. None of it is permanent. Coax water-ingresses, batteries lose capacity, connectors corrode, antennas droop, firmware develops new failure modes nobody saw at launch. **A station that worked great five years ago will work much less great today if nothing has been touched in that time** — and the slide is gradual enough that you may not notice until you wonder why your S-meter reads two units lower than your neighbor's.

This chapter is the maintenance discipline. Five sections covering the things that wear out, how often to check them, and what to do when you find degradation.

## How the chapter is organized

| § | Topic | Cadence |
|---|-------|---------|
| 20-01 | Battery maintenance | Monthly check, annual capacity test |
| 20-02 | Firmware updates | Quarterly check, manual install |
| 20-03 | Scheduled inspections | Spring + fall walk-through |
| 20-04 | Coax replacement | Triggered by symptoms; preventive every 7-15 years |

## The maintenance mindset

Three principles that apply across every section:

1. **Schedule beats reaction.** A 30-minute walk-through twice a year beats a panicked Saturday troubleshooting an antenna that quietly degraded for two years.
2. **Document baselines.** Today's SWR sweep, today's noise floor on each band, today's measured battery capacity — written down, dated. Six months later, when something changes, you know it changed because you have the comparison number.
3. **Replace before failure.** Coax that has shown moisture is past the inflection point; cells that have lost 30% capacity will lose another 30% in half the time. Catch the slope early; replace before the system fails on the worst possible weekend.

## A simple station-maintenance schedule

For a typical home station with one or two HF antennas, a tower or wire support, an HF radio, an amplifier (maybe), and one or two VHF/UHF radios:

| Frequency | Tasks |
|-----------|-------|
| Weekly | Glance at logs (rig, j-hub, error counts), check noise floor sounds normal |
| Monthly | Sweep all antennas with analyzer (compare to baseline); inspect battery voltage / float charging; check firmware notification feeds for your major gear |
| Quarterly | Walk antennas with binoculars from the ground; check for visible damage, water staining at connectors; review and apply firmware updates |
| Spring | Full antenna inspection up close (climb, ladder, or rope-pull); coax connector check; run battery capacity test |
| Fall | Repeat spring; check for damage from summer storms; weatherproof anything that needs re-doing before winter |
| Annually | Audit station inventory (per §18); review log/data backups; review uploaders' status |

## Tools you'll need

- **Antenna analyzer** (NanoVNA, RigExpert, MFJ-269) — the single most-used maintenance tool.
- **Voltmeter / multimeter** — for battery and DC supply checks.
- **DC clamp meter** — for measuring current on wires you don't want to disconnect.
- **Binoculars** — for ground-level inspection of high-up antennas.
- **A camera** — phone is fine. Photograph everything before disassembly so you remember the cable routing and orientation.
- **Coax patch tools** — wire cutters, soldering iron, heat shrink, weatherproofing tape, dielectric grease, replacement connectors.
- **Battery load tester** — for the deep-cycle batteries on emcomm/standby use.
- **A station log** (paper or digital) — log each maintenance check with date, baseline numbers, and any items found.

## What you will not learn here

- **Tower climbing safety** — its own chapter (planned 31). Tower work has its own discipline beyond what's here.
- **Lightning protection** — its own chapter (planned 29). Surge protection and grounding for storm season is detailed there.
- **Electrical safety, circuit-breaker testing** — covered in the broader station-electrical chapter (planned 33).
- **Computer-side maintenance** — backups, OS updates, etc., live in the station-software chapter (planned 32). This chapter is hardware/firmware-focused.

## See also

- §18 — Shack inventory (knowing what you have makes maintenance possible)
- §13 — High-SWR troubleshooting (the symptom side of antenna problems)
- §14 — Station troubleshooting
- §22 — Coax & connectors reference (for coax-replacement decisions)
