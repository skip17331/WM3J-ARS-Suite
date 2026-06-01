---
id: 29-00
title: Satellite Advanced Topics
chapter: 29
section: 00
level: mixed
status: published
---

# Satellite Advanced Topics

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Chapter 7 introduced the basics — FM birds with an Arrow II, Doppler shift, the mechanics of an LEO pass. This chapter is Phase 2: what you do after you've worked SO-50 a few dozen times and the FM-bird-CQ-and-grid-exchange routine has lost its novelty. The next stage is linear transponders, full-duplex operation, decent antenna geometry, and the engineering questions that come with weak-signal work on a satellite that's 1500 km away and moving at 7.5 km/sec.

The split between "beginner" and "advanced" satellite work isn't really about license class or experience years — it's about whether you've made the equipment and operating-style commitments. The Phase 1 operator has an HT and an Arrow and chases FM-bird grids on lunch breaks. The Phase 2 operator has a full-duplex base radio, separate Yagis with a rotator (or at minimum a polarization-switchable handheld setup), tracking software with CAT control, and the patience to work SSB through a transponder where every other operator can also hear what's happening across the passband.

## What's in this chapter

This chapter assumes you've read chapter 7 and can hold your own on an FM bird. It covers:

- **Full-duplex operation** (§29-01) — hearing yourself come back through the satellite. Non-negotiable for linear birds, very helpful for FM. Two-radio vs sat-radio approaches.
- **Linear transponder etiquette** (§29-02) — sharing a 60 kHz passband with a dozen other QSOs without becoming the operator everyone complains about. The "alligator" problem and signal-level discipline.
- **Antennas — Arrow handheld** (§29-03) — the Arrow 146/437-10WBP, the canonical portable satellite Yagi, and how to actually point it during a pass.
- **Antennas — eggbeater omni** (§29-04) — the M2 EB-144 / EB-432 family. When pointing isn't required and what you give up in gain.
- **Antennas — helicals** (§29-05) — circular polarization, RHCP and LHCP, the helix-on-a-boom homebrew, and tower-mounted variants for serious ground stations.
- **Polarization switching** (§29-06) — crossed Yagis, switchable phasing harnesses, sequenced rotor control, and Faraday rotation on linear sats.
- **Mast-mounted preamps** (§29-07) — why LNA placement at the antenna feedpoint matters, the coax-loss problem, SAW filters for image rejection, bias-T power feed.
- **Doppler automation** (§29-08) — SatPC32, MacDoppler, GPredict + Hamlib. Configuring CAT control, USB latency, and why the IC-9700 does Doppler in firmware.
- **Linear transponder strategy** (§29-09) — finding a slot, inverted sideband, low power, the uplink-ahead-downlink-behind Doppler asymmetry, tracking operators across a passband.
- **Mode nomenclature** (§29-10) — V/U, U/V, L/U, V/S — reading AMSAT-NA's status page and knowing what bands to set up before AOS.

## How to use this chapter

The sections are roughly independent. If you already have a tracking-software setup and just need help with linear transponder operating, skip ahead to §29-02 and §29-09. If you're shopping antennas, read §29-03 through §29-06 in order. The polarization and preamp sections (§29-06, §29-07) are infrastructure-level topics — read them once you've decided you want a permanent satellite station rather than a portable kit.

> **Advanced —** A satellite station built out per this chapter — IC-9700, crossed Yagis with polarization switching, mast-mounted preamps, az/el rotor, tracking-software CAT — runs around $4000-6000 as of 2026. That's an order of magnitude more than the Arrow + HT kit in chapter 7. Most operators step up incrementally: full-duplex base radio first, then a proper Yagi (single polarization), then a rotor, then crossed Yagis with polarization switching, then a mast preamp last. Each step buys a measurable improvement.

## What this chapter does NOT cover

- **AO-7 mode A / mode B HF satellite work.** AO-7 is still partially operational from 1974 (yes, really) and works in eclipse; it's a fascinating niche, but it's covered in §30 (Operating Specialties) rather than here.
- **QO-100 / Es'hail-2 geostationary operation.** Continuous coverage, no Doppler, no pass timing — operationally different enough that it deserves its own treatment. See §30.
- **APRS and digipeating via ISS.** Packet via satellite is its own world; the basics are in chapter 3, and ISS-specific operating is in chapter 7 at a beginner level.

## Prerequisite cross-references

- [§07 — Satellites](../07-satellites/07-00-overview.md) — the basic chapter; everything here assumes you've internalized it.
- [§07-02 — Doppler Shift](../07-satellites/07-02-doppler-shift.md) — the math and physics; §29-08 and §29-09 build on it.
- [§22-08 — Split-Frequency Operation](../22-operating-practice/22-08-split-frequency.md) — satellite operation is essentially split done automatically; the conceptual framework is the same.
- [§06 — Antennas](../06-antennas/06-00-overview.md) — Yagi gain, polarization, and feedpoint impedance theory.
- [§20-04 — Satellite Band Plan](../20-band-plans/20-04-satellite.md) — the uplink/downlink frequency pairs you'll need.

## See also

- §29-01 through §29-10 (this chapter)
- §07 — Satellites (basics)
- §30 — Operating Specialties (QO-100, AO-7, EME)
