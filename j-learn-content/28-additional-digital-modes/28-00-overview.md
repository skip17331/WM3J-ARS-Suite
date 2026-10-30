---
id: 28-00
title: Overview — Additional Digital Modes
chapter: 28
section: 00
level: simple
status: published
---

# Additional Digital Modes — Overview

Chapter 03 covers the digital modes most operators meet first: **FT8/FT4**, **RTTY**, **PSK31**, **JS8Call**, **APRS**, and **Packet**. Those six dominate the on-air activity you see on a typical waterfall. This chapter covers the rest — modes that are niche but operationally important, especially in emergency communications and weak-signal manual QSO work.

## Why a second digital-modes chapter?

The amateur digital landscape has two distinct halves:

1. **Popular modes** (covered in §03). Used for casual contacts, DX hunting, and contesting. Operator-friendly, well-documented, large communities. FT8 alone accounts for the majority of HF QSOs logged worldwide.

2. **Specialty modes** (this chapter). Each solves a specific problem the popular modes don't:
   - **Email over radio** when the internet is down → Winlink (§28-01)
   - **High-throughput data on HF without expensive hardware** → VARA HF (§28-02)
   - **Same idea on VHF/UHF FM** → VARA FM (§28-03)
   - **Manual chat below the noise floor** → Olivia (§28-04)
   - **Faster manual chat with weak-signal tolerance** → MFSK16 / MFSK32 (§28-05)
   - **Visually-readable text without software** → Hellschreiber (§28-06)
   - **Commercial-grade HF data reliability** → Pactor (§28-07)
   - **QRP-friendly selective-call chat** → FSQ (§28-08)
   - **AX.25 packet that survives HF fading** → Robust Packet (§28-09)

The chapter closes with a tactical summary — **§28-10 Digital Messaging Workflows** — showing how emcomm operators actually pick among these modes during a real incident.

## The emcomm thread

Most modes in this chapter exist because of emergency communications. When commercial power and internet fail, hams need to move structured messages reliably between served agencies, shelters, EOCs, and the wider relief network. The popular modes (FT8, PSK31) weren't designed for that — they're QSO modes, not message-transport modes.

The chapter's heaviest cross-references go to **§21 Emergency Comms**: ICS forms, the National Traffic System (NTS), and the practical workflows of an ARES/RACES deployment.

## What's *not* in this chapter

- **Digital voice** (D-STAR, DMR, System Fusion, M17) — see §24 Digital Voice & Hotspots.
- **WSPR, FST4W, Q65** — weak-signal beacons and EME modes — see §03-07 (WSPR) and §29 Satellite & EME (where Q65 is covered).
- **POCSAG / pager modes**, niche commercial relics not commonly used by hams.

## Table of contents

| § | Title | Level | Notes |
|---|-------|-------|-------|
| 28-00 | Overview | simple | This page |
| 28-01 | Winlink | mixed | Radio email; emcomm backbone |
| 28-02 | VARA HF | mixed | Soundcard modem replacing Pactor on HF |
| 28-03 | VARA FM | simple | Same on VHF/UHF FM |
| 28-04 | Olivia | mixed | Sub-noise-floor manual chat |
| 28-05 | MFSK16 / MFSK32 | mixed | Faster cousins of Olivia |
| 28-06 | Hellschreiber | mixed | Painted-text mode from the 1930s |
| 28-07 | Pactor | advanced | Commercial-grade HF data, expensive hardware |
| 28-08 | FSQ | mixed | QRP-friendly selective-call chat |
| 28-09 | Robust Packet | mixed | AX.25 hardened for HF |
| 28-10 | Digital messaging workflows | mixed | How emcomm picks among these |

## See also

- §03 — Digital Modes (the popular six)
- §21 — Emergency Comms (where most of these modes earn their keep)
- §20 — Band Plans (where each mode lives within a band)
- §24 — Digital Voice & Hotspots (the voice side of "digital")
