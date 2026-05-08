---
id: 14-00
title: Maintenance — Overview
chapter: 14
section: 00
level: simple
status: draft
---

# Maintenance — Overview

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A radio station is a system of components installed outdoors, indoors, on poles, in vehicles, behind walls — exposed to UV, water, ice, vibration, current cycling, and the slow march of firmware bugs. None of it is permanent. Coax water-ingresses, batteries lose capacity, connectors corrode, antennas droop, firmware develops new failure modes nobody saw at launch. **A station that worked great five years ago will work much less great today if nothing has been touched in that time** — and the slide is gradual enough that you may not notice until you wonder why your S-meter reads two units lower than your neighbor's.

This chapter is the maintenance discipline. Ten sections covering the things that wear out, how often to check them, and what to do when you find degradation.

## How the chapter is organized

| § | Topic | Cadence |
|---|-------|---------|
| §14-01 | Battery maintenance | Monthly check, annual capacity test |
| §14-02 | Firmware updates | Quarterly check, manual install |
| §14-03 | Scheduled inspections (high-level walk-through) | Monthly + spring/fall deep dives |
| §14-04 | Coax replacement | Triggered by symptoms; preventive every 7-15 years |
| §14-05 | Tower & mast inspection | Monthly visual + annual climb |
| §14-06 | Guy lines, turnbuckles, thimbles, clamps | Annual climb; full audit every 5 years |
| §14-07 | Ground system inspection | Quarterly + annual resistance test |
| §14-08 | Coax inspection (distinct from replacement) | Quarterly visual + annual electrical |
| §14-09 | Cable entry & water intrusion | Quarterly + annual sealant check |

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
| Annually | Audit station inventory (per **J-Vault**); review log/data backups; review uploaders' status |

## Tools you'll need

- **Antenna analyzer** (NanoVNA, RigExpert, MFJ-269) — the single most-used maintenance tool.
- **Voltmeter / multimeter** — for battery and DC supply checks.
- **DC clamp meter** — for measuring current on wires you don't want to disconnect.
- **Binoculars** — for ground-level inspection of high-up antennas.
- **A camera** — phone is fine. Photograph everything before disassembly so you remember the cable routing and orientation.
- **Coax patch tools** — wire cutters, soldering iron, heat shrink, weatherproofing tape, dielectric grease, replacement connectors.
- **Battery load tester** — for the deep-cycle batteries on emcomm/standby use.
- **A station log** (paper or digital) — log each maintenance check with date, baseline numbers, and any items found.

## Master inspection-frequency reference

When in doubt, this is the cadence:

| Frequency | Tasks |
|-----------|-------|
| **Weekly** | Glance at logs (rig, J-Hub, error counts); listen for changes in noise floor |
| **Monthly** | Antenna SWR sweep vs baseline; battery voltage / float status; visual scan of tower from ground (binoculars); firmware notifications check |
| **Quarterly** | Walk all coax runs (visual jacket + connector check); inspect station ground bus; tower base inspection; cable-entry exterior visual; lightning arrestor check |
| **After every storm** (>40 mph wind, ice loading, lightning) | Tower + guys + cable-entry walk; arrestor reset/inspection; SWR sweep on affected antennas |
| **Spring (annual)** | Climb inspection of tower + antennas; open and re-seal every outdoor connector; battery capacity test; ground-rod resistance test; SWR baseline refresh |
| **Fall (annual)** | Spring repeat focused on winter prep; re-seal anything suspect; firmware backup; battery backup load test |
| **Every 3-5 years** | Disassemble and re-build cable-entry seal; check guy hardware torque |
| **Every 5 years** | Professional rigger tower inspection; full ground-system audit |
| **Every 7-15 years** | Coax run replacement (preventive, per cable type and exposure) |
| **Every 25-30 years** | Guy cable + turnbuckle replacement |

## What you will not learn here

- **Tower climbing safety details** — addressed at the level needed for inspection in §14-05; full climbing certification (ANSI Z359, NIA, NATE) requires hands-on training, not a written guide.
- **Detailed lightning protection design** — surge arrestors, single-point grounding, and bonding are in §14-07 and §11-05; designing a complete NFPA 780 system at scale needs an engineer.
- **Computer-side maintenance** — OS, backups, software updates live with each module. This chapter is the antenna/feedline/structural/hardware side.

## See also

- **J-Vault** — Shack inventory (knowing what you have makes maintenance possible)
- §10 — High-SWR troubleshooting (the symptom side of antenna problems)
- §11 — Station troubleshooting
- §11-05 — Grounding (the troubleshooting view of the ground system)
- §16 — Coax & connectors reference (for coax-replacement decisions)
