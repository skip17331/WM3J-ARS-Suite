---
id: 04-00
title: Repeaters & Bandplans — Overview
chapter: 04
section: 00
level: simple
status: draft
---

# Repeaters & Bandplans — Overview

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What this chapter is for

VHF and UHF — the bands where most newcomers start, where the local repeater club hangs out, where weather nets meet, where ARES exercises happen. To use these bands well you need to understand two things:

1. **How repeaters work** — offsets, tones, simplex, linked systems.
2. **What goes where** — band plans, calling frequencies, frequency coordination.

Both are practical, both are easy to learn, and both will save you embarrassment at your first net.

## How the chapter is organized

| § | Topic | Why it's here |
|---|-------|---------------|
| 02-01 | What is a repeater | The basics — input, output, duplex, geographic coverage |
| 02-02 | Offsets, tones, CTCSS, DCS | The settings that make your radio actually open the repeater |
| 02-03 | Band plans | Where to operate on each band, by mode |
| 02-04 | Simplex calling frequencies | Direct radio-to-radio without a repeater |
| 02-05 | Linked systems (AllStar, DMR, Fusion, D-STAR) | When repeaters talk to each other across the internet |
| 02-06 | Duplexers | The magic that lets a repeater transmit and receive simultaneously |
| 02-07 | Frequency coordination | Why the local coordinator matters and how to play nicely |
| 02-08 | Custom offset calculator | Programming non-standard repeaters |

## Where to start

If you just got a 2 m / 70 cm radio and want to use the local repeater, read **02-01** then **02-02** and you're operational.

If you're trying to figure out why your radio "isn't doing anything" on a frequency you found in a directory, **02-02** (tones) is almost certainly your problem.

If you want to understand the digital alphabet soup (DMR, Fusion, D-STAR, AllStar), go straight to **02-05**.

## What you will not learn here

- Specific equipment programming — every brand of radio has its own menus. Read your manual.
- Repeater building or duplexer tuning beyond a conceptual level — that's a whole hobby of its own.
- HF operating practices — see chapter 24 (band plans) and the per-mode chapters in 25.

## Quick reference for the impatient

A typical 2 m repeater looks like this in your radio's memory:

```
Frequency: 146.940 MHz   (this is the OUTPUT — what you receive)
Offset:    –0.6 MHz      (your TRANSMIT is 600 kHz lower → 146.340)
Tone:      100.0 Hz      (CTCSS tone you must transmit)
Mode:      FM
```

If your radio doesn't make a sound when you key up, your tone is probably wrong. If it makes a sound but nobody answers, you may be hitting a different repeater, or one with no traffic.
