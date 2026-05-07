---
id: 09-00
title: Power Budget & ERP — Overview
chapter: 09
section: 00
level: simple
status: draft
---

# Power Budget & ERP — Overview

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A "100 W station" doesn't put 100 W on the air. The transmitter delivers some watts; the feedline consumes some; the antenna re-radiates some — and the *direction* it radiates determines whether the signal makes it to your contact. Everything between PA output and the wave heading toward the other operator is the **power budget** of your station.

This chapter is the bookkeeping. Five sections covering each link in the chain (transmitter, feedline, antenna), the math that connects them, and a worked example for portable / field operation.

## How the chapter is organized

| § | Topic | What it covers |
|---|-------|----------------|
| 11-01 | TX power | What's actually leaving the rig: rated output, fold-back, duty cycle effects |
| 11-02 | Feedline loss | Watts dissipated in the cable run (matched loss + mismatch loss) |
| 11-03 | Antenna gain | What gain in dBi or dBd actually means; the direction-dependence |
| 11-04 | ERP / EIRP output | Putting it together: the radiated power in the relevant direction |
| 11-05 | Portable budget | A complete worked example for QRP / portable / SOTA-style operation |

## The four numbers

Every power-budget calculation tracks four quantities:

| Quantity | Symbol | Where measured | Typical amateur values |
|----------|--------|----------------|-------------------------|
| Transmitter output | P_tx | Rig output port | 5–100 W (most stations); up to 1500 W (legal limit) |
| Power at antenna feedpoint | P_ant | After feedline loss | 70–95% of P_tx for typical installs |
| Effective Radiated Power | ERP | What the antenna sends in its peak direction | P_ant × dBd gain |
| Equivalent Isotropic Radiated Power | EIRP | What an isotropic radiator at the antenna would have to transmit | ERP × 1.64 (= ERP + 2.15 dB) |

The relationship: **EIRP = P_tx × feedline_efficiency × antenna_gain (linear, vs. isotropic)**.

In dB: **EIRP (dBW) = P_tx (dBW) − feedline_loss (dB) + antenna_gain (dBi)**.

## A quick example

Setup: 100 W transmitter, 75 ft of LMR-400 at 14 MHz, 6 dBi gain dipole.

- **P_tx**: 100 W = 20 dBW.
- **Matched-line loss**: 0.45 × 0.75 = 0.34 dB.
- **SWR-induced extra loss** (assuming SWR 1.5:1): ~0.04 dB.
- **Total feedline loss**: 0.38 dB.
- **P_ant**: 20 dBW − 0.38 = 19.62 dBW = **91.6 W** at the dipole feedpoint.
- **Antenna gain**: 6 dBi (peak direction).
- **EIRP**: 19.62 + 6 = 25.62 dBW = **365 W EIRP** in the direction of the dipole's broadside maximum.

So your 100 W station sends out 365 W of EIRP toward whoever's broadside to your dipole, 92 W of feedpoint power into the antenna, and the rest (8 W) is heat in the feedline.

## Where ERP/EIRP actually matters

Three contexts where the budget calculation is more than academic:

1. **RF safety compliance** (§06): MPE limits are computed against EIRP; you need this number to check your station's compliance with FCC §97.13.
2. **DX path budget**: how much signal will reach a station 5000 km away depends on EIRP, propagation losses, and the receiver's sensitivity. Useful for predicting whether a marginal QSO is worth attempting.
3. **Antenna comparison**: switching from a dipole (6 dBi gain) to a Yagi (12 dBi gain) is a 6 dB gain in EIRP — equivalent to quadrupling transmit power. That kind of comparison drives station-upgrade decisions.

## What's not in this chapter

- **Antenna pattern shape and how gain is measured** — see §04-15.
- **Specific feedline loss values per cable / frequency** — see §19-02.
- **Detailed Smith chart impedance work** — see §04-09.
- **Propagation losses on the path to a distant station** — see §01.
- **RF safety calculation specifics** — see §06-05.

## Where the suite helps

- **§10 (RF Exposure Calculator)**: takes power budget output (EIRP) and computes MPE compliance distances.
- **J-Hub Antenna Tab**: shows real-time SWR data for power-budget computations.
- **§07 (Antenna Calculator)**: complementary — computes physical lengths.

## See also

- §06-05 — ERP / EIRP for RF safety
- §09-05 — Portable budget (worked example)
- §19 — Coax & connectors (the feedline-loss source)
- §04-15 — Radiation patterns (where antenna gain comes from)
- §01 — Propagation (the path-loss side)
