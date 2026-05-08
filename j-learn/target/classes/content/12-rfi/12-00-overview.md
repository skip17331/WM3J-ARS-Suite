---
id: 12-00
title: RFI — Overview
chapter: 12
section: 00
level: simple
status: draft
---

# RFI — Overview

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What this chapter is about

**RFI** = Radio Frequency Interference. Two flavors matter to amateur operators:

1. **RFI you cause** — your transmissions interfering with neighbors' TVs, audio, computers, alarm systems. The problem here is usually **not** that you're transmitting too much power, but that the affected device has poor RF immunity. Still your problem to fix, mostly via filters at the affected device.

2. **RFI you suffer** — noise from somewhere else that buries the signals you're trying to receive. Could be your own house (switching power supplies, LED lights, plasma TVs, ethernet-over-power) or your neighbor's, or a power line, or commercial equipment several blocks away.

This chapter focuses on **RFI you suffer** — what generates it, how to track it down, and what to do about it. Causing-RFI is touched on too because the workflow is similar.

## How this chapter is organized

| § | Topic |
|---|-------|
| 15-01 | What is RFI — the basic concepts and terminology |
| 15-02 | Common household sources you might be making yourself |
| 15-03 | Identifying buzzing / hash / specific noises by sound |
| 15-04 | Ferrite selection — picking the right cores for filtering |
| 15-05 | Isolation workflow — narrowing down where the noise is from |
| 15-06 | Step-by-step elimination — the practical procedure |
| 15-07 | AM radio sniffer — the cheapest direction-finding tool |
| 15-08 | SDR waterfall — modern direction-finding with cheap dongles |

The two technique chapters (15-07 and 15-08) tell you *how* to find RFI sources. The diagnostic chapters (15-01 through 15-06) tell you *what to do* once you find one.

## A quick framework

Every RFI source has three properties you want to characterize:

1. **What does it sound like?** Steady hash, crackling, intermittent buzz, modulated tone, etc. — the sound is a fingerprint.
2. **Where is it coming from?** Inside your house, your neighbor's house, a power line down the street, the substation a mile away. Direction-find with a portable AM radio or SDR.
3. **What's generating it?** Switching power supply, LED lights, plasma TV, electric fence, broken insulator on a pole. The sound and direction usually point you at the device.

Once you know what and where, you fix it: replace the offending device, add filters, add chokes, or contact the utility.

## How bad is bad?

Some baseline noise floors to anchor expectations:

| Environment | Typical 14 MHz noise | What it means |
|-------------|---------------------:|---------------|
| Rural, no power lines, no neighbors | S0–S2 | Excellent; only quiet days approach this |
| Suburban, modest interference | S3–S5 | Common; weak signals fade in and out |
| Urban / dense | S5–S8 | Listening is harder; strong signals only |
| Apartment, ethernet over power, neighbors with everything | S8–S9+ | Difficult; need NB / DSP / better antennas |

If you're seeing S9 noise on 80 m at midnight in a quiet rural area, something is wrong. Tracking it down is worth the effort — the difference between S2 and S9 is 7 S-units = 42 dB = factor-of-10,000 in received power.

## When you're the cause

If a neighbor reports interference *from* your station:

- **Stay polite.** Most neighbor-RFI complaints are about devices that don't have proper RF immunity, but the neighbor doesn't know that and assumes it's your fault.
- **Visit and listen.** Confirm the problem; identify which device(s) are affected.
- **Try the cheapest fix first** — a snap-on ferrite on the device's power cable, audio cable, or speaker leads often solves it for under $10.
- **Document everything.** Date, what you did, what worked.
- **Reference ARRL's "RFI" book and the FCC Part 15 rules.** The legal landscape favors you (FCC has explicitly stated that home electronics must accept RFI from licensed transmitters), but smashing a neighbor with that fact rarely improves the situation.

## See also

- §10 — high SWR (sometimes related)
- §13 — specific noise sources by category
- §13 — power-line noise specifically
