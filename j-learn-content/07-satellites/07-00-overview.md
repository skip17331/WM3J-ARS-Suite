---
id: 07-00
title: Satellites — Overview
chapter: 07
section: 00
level: simple
status: published
---

# Satellites — Overview

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The first amateur satellite, OSCAR-1, was launched as a hitchhiker payload in 1961 — three months after Sputnik. Sixty-five years later, dozens of amateur satellites are in orbit at any given time: FM "easy sats" anyone can work with a handheld and a small Yagi, linear transponder satellites for SSB/CW QSOs through orbiting birds, the International Space Station's amateur radio gear, packet store-and-forward birds, CubeSats from universities, and one geostationary amateur satellite (Es'hail-2 / QO-100) that puts amateur radio on television-quality 24/7 coverage of half the Earth.

This chapter teaches you enough to work satellites: the orbital mechanics behind why Doppler shift happens, the difference between FM and linear transponder satellites, how to predict passes and where to point your antenna, what Keplerian elements are and why your tracking software needs them updated, and the specific frequencies and procedures for the common amateur birds.

## Why bother with satellites?

Three reasons amateurs work satellites:

1. **It's a different operating skill.** The compressed pass time (5-15 minutes), the need to manage Doppler shift, the requirement to track an object moving 7 km/sec across the sky — none of this is like HF or VHF/UHF terrestrial work.
2. **The DX is different.** A 5W handheld talking through a satellite covers a footprint of thousands of kilometers. You can work all over a continent on 5W if you catch the right pass. Some operators chase **WAS via satellite** as a goal.
3. **The ISS.** Astronauts occasionally operate amateur radio from the ISS — voice contacts, packet, and SSTV. You can work astronauts from your back yard with a $200 setup.

## How the chapter is organized

| § | Topic | What you find |
|---|-------|---------------|
| 07-01 | FM vs linear satellites | The two main satellite types, each with different operating procedures |
| 07-02 | Doppler shift | Why the frequency changes during a pass, by how much, and how to compensate |
| 07-03 | Keplerian elements | The 6 numbers that describe an orbit; updating them in your tracking software |
| 07-04 | Tracking strategies | Getting the antenna pointed; manual aim, automated rotators, "look and shoot" |
| 07-05 | ISS packet & APRS | The amateur radio gear on the International Space Station |
| 07-06 | Footprints | Where on Earth a satellite is visible at any given moment |
| 07-07 | Pass prediction | Calculating when a satellite will be over you, for how long, and where |
| 07-08 | Doppler correction tables | Quick-reference numbers for common satellites |

## What you need to work satellites

The minimum kit:

- **A dual-band radio** capable of full duplex (transmit on one band while receiving on another) — most modern amateur HTs and base radios. Full duplex is essential for linear transponder satellites; FM-only "easy sats" can be worked with simplex.
- **A directional antenna** with some gain — a handheld Arrow II Yagi or a beam on a small mast works well for VHF/UHF satellites.
- **A computer or app** running pass-prediction and tracking software (Gpredict, SatPC32, AMSAT pass predictor, or J-Sat).
- **Updated Keplerian elements** for the satellites you want to work (these decay slightly each week; refresh weekly).
- **Knowledge of which satellites are operational** — AMSAT publishes a status list; check before each session.

For more serious satellite work (linear transponders, weak-signal modes, EME):

- **An automated rotator** to track the satellite during a pass.
- **Doppler-correction-aware radio** (some modern rigs auto-correct based on tracking software's lookup).
- **Pre-amplifier at the antenna** for weaker signals.
- **Higher-gain antennas** (cross-Yagi pairs, helices) for marginal modes.

## What you will not learn here

- **EME (moonbounce)** — covered separately (planned ch. 30). Different orbital body, different rules.
- **Specific contest rules** for satellite contests (some QRP and AMSAT-organized).
- **Building or launching CubeSats** — that's a satellite-engineering topic, not an operating one.

## Where the suite helps

- **J-Sat module** — the satellite-tracking and pass-prediction tool in the suite. Shows currently-visible satellites, computes Doppler shift in real time, and provides cross-band radio control for radios that support it.
- **§04-08** — repeater band-plan coverage of satellite sub-bands.
- **§20-04** — satellite sub-band reference (frequencies reserved for satellite uplinks/downlinks).

## See also

- §07-01 — FM vs. linear (the two satellite categories)
- §07-02 — Doppler shift (the most-counterintuitive part of satellite ops)
- §20-04 — Satellite sub-bands (frequencies)
- §04 — Repeaters & bandplans
- §06-07 — Radiation patterns (relevant for satellite antenna gain)
