---
id: 29-10
title: Mode Identification (V/U, U/V, L/U, etc.)
chapter: 29
section: 10
level: mixed
status: draft
---

# Mode Identification (V/U, U/V, L/U, etc.)

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What "mode" means in satellite parlance

A satellite's **mode designation** tells you which band the uplink is on and which band the downlink is on. The format is `Uplink/Downlink`, using single-letter band codes:

| Letter | Band             | Frequency range (amateur) |
|--------|------------------|---------------------------|
| H      | HF               | < 30 MHz                  |
| V      | VHF (2 m)        | 144–148 MHz               |
| U      | UHF (70 cm)      | 420–450 MHz               |
| L      | L band (23 cm)   | 1240–1300 MHz             |
| S      | S band (13 cm)   | 2400–2450 MHz             |
| C      | C band (5 cm)    | 5650–5925 MHz             |
| X      | X band (3 cm)    | 10.0–10.5 GHz             |
| K      | K band (1.2 cm)  | 24.0–24.25 GHz            |

So **V/U** means "uplink on 2 m, downlink on 70 cm" — you transmit on a VHF frequency and receive on UHF.

## The four modes you'll encounter

For active amateur satellites today, four mode combinations cover almost everything:

| Mode | Uplink → Downlink | Examples | Notes |
|------|-------------------|----------|-------|
| **V/U** | 2 m → 70 cm | AO-91, AO-92 (defunct), SO-50 (FM), RS-44 (linear) | The classic. Most FM birds and many linear birds. Slightly easier on the uplink side (VHF less crowded for uplink). |
| **U/V** | 70 cm → 2 m | FO-29 (linear), most CAS-series | Reverse direction. Linear birds favor this because UHF uplink antennas are smaller. |
| **L/U** | 23 cm → 70 cm | AO-40 (defunct), some experimental cubesats | Higher uplink frequency means smaller dish/yagi but the uplink power amp is more expensive. |
| **V/S** | 2 m → 13 cm | AO-40 alternate mode, AMSAT Phase 3D | S-band downlink requires a low-noise preamp at the antenna feedpoint. |

> ⚙️ **Advanced —** Higher-band downlinks (S, C, X, K) require progressively higher gain dish antennas, low-noise preamps mounted at the feedpoint, and precise pointing accuracy (degrees of error rather than degrees of beamwidth). The trade-off is reduced terrestrial QRM on those bands compared to 2 m and 70 cm.

## Reading mode designations from AMSAT-NA status

The AMSAT-NA satellite status page (`amsat.org/status`) lists active satellites with their current mode. A typical entry looks like:

```
Satellite: RS-44 (Ratio M / RS-44)
Mode: Linear V/U inverting
Uplink: 145.935 - 145.995 MHz LSB / CW
Downlink: 435.610 - 435.670 MHz USB / CW
```

This tells you:

- Mode is V/U (VHF uplink, UHF downlink)
- It's a *linear* transponder (passband, not FM channel)
- It's *inverting* — uplink LSB shows as downlink USB; rising uplink frequency = falling downlink frequency
- The passband is 60 kHz wide on both ends

Cross-link the uplink and downlink ranges in the satellite band plan ([§20-04](../20-band-plans/20-04-satellite.md)) before configuring your rig.

## Rig mode setup

For each mode, the relevant rig settings:

| Mode    | TX band setting     | RX band setting     | Common rig | Comments |
|---------|---------------------|---------------------|-----------|----------|
| V/U FM  | 2 m FM              | 70 cm FM            | Any dual-band HT or mobile | Simplest case. SO-50 etc. |
| V/U linear | 2 m USB/LSB      | 70 cm USB/LSB       | IC-9700, FT-991A, two-rig setup | Need full-duplex (§29-01) |
| U/V linear | 70 cm USB/LSB    | 2 m USB/LSB         | Same | Reversed bands |
| L/U     | 1.2 GHz USB/LSB     | 70 cm USB/LSB       | IC-9700 (with L-band option), external transverter | Less common; AO-40 era |
| V/S     | 2 m USB/LSB         | 2.4 GHz USB/LSB     | IC-9700 has built-in S-band RX | Needs S-band preamp + dish |

> ⚙️ **Advanced —** The Icom IC-9700 is currently the only mass-market satellite-dedicated transceiver covering V/U/L on one chassis with built-in full-duplex and integrated Doppler tracking. Older operators built equivalent setups from a dual-rig pair (e.g., FT-817 for the downlink + FT-857 for the uplink, linked via Hamlib). For S-band you still need an external converter or a software-defined radio (Airspy + 2.4 GHz LNA).

## Why the same satellite has different modes over its life

A satellite's mode can change over time:

- **AO-7** has two modes (A: 2 m up / 10 m down, B: 70 cm up / 2 m down) and switches based on solar charge state. Active mode is published on AMSAT-NA status.
- **Some cubesats** rotate modes on a schedule to give different user groups time on the bird.
- **Mothballed modes** appear when a transponder fails — the operator may turn off the linear mode and leave only the beacon active.

Always check the **live status page** before keying up. A satellite listed as "Mode: B" on AMSAT-NA but currently in Mode A will simply not hear your uplink.

## Common confusion points

- **V/U vs U/V matters.** They're not symmetric. If you call on the wrong uplink band you'll be transmitting into empty spectrum.
- **"Mode B" historically.** Old satellite mode names used letters (A, B, J, K, S, T) referring to specific uplink/downlink pairs. AMSAT modernized to the Uplink/Downlink letter format. Some legacy documentation still uses the old names — match by frequency, not by letter, when in doubt.
- **Crossband repeaters in space.** A "crossband linear transponder" is just a satellite running V/U or U/V. The vocabulary changes; the concept is identical.

## See also

- [§29-09 Linear Transponder Strategy](29-09-linear-transponder-strategy.md)
- [§29-01 Full-Duplex Operation](29-01-full-duplex.md)
- [§07-01 FM vs Linear](../07-satellites/07-01-fm-vs-linear.md)
- [§20-04 Satellite Band Plan](../20-band-plans/20-04-satellite.md)
