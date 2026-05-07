---
id: 16-00
title: Noise Sources — Overview
chapter: 16
section: 00
level: simple
status: draft
---

# Noise Sources — Overview

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

This chapter is a per-category guide to the most common noise sources in modern households. Each section covers what the source looks like in operation, what it sounds like on the air, and the specific filtering or replacement strategies that work for that category.

For the higher-level workflow of how to find a noise source, see chapter 15. This chapter assumes you've already identified what kind of device you're dealing with and are looking for fix specifics.

## How this chapter is organized

| § | Source category | Common in |
|---|-----------------|-----------|
| 16-01 | Switching power supplies | Phone chargers, laptop bricks, every wall wart |
| 16-02 | LED lights | Modern bulbs, especially dimmed |
| 16-03 | Solar inverters | Roof-mounted PV systems |
| 16-04 | Ethernet over power | Powerline networking adapters |
| 16-05 | HVAC | Furnace blowers, AC compressors, heat pumps |
| 16-06 | Battery chargers | Power tool, e-bike, vehicle |
| 16-07 | Motor brushes | Drills, mixers, ceiling fans, vacuum cleaners |

Read whichever section matches what you've identified.

## Why these sources specifically

These categories produce the noise that most amateur operators encounter. They aren't the only sources — there are dozens more — but together they account for the majority of noise complaints reported by hams.

Notably absent from this chapter: power-line noise, which has its own chapter (17) because diagnosing and fixing it involves the utility company.

## A general principle: source vs receptor

Some sources can be made quieter (replace, filter at the device). Others are immune to fixing — a powerline adapter is *designed* to use the HF spectrum and can't be made quiet without abandoning it.

For sources you can't fix, the next move is to harden the receptor — your radio. That means:

- **Better antennas** with deeper noise nulls (loops, Beverages).
- **Noise blankers and digital noise reduction** in the radio.
- **Operating during quiet hours** when offending devices aren't on.
- **Operating from a different physical location** (mobile, portable, away from the noisy environment).

Each per-category section discusses what's possible at the source and what's not.

## What's "normal" noise

Even in a perfectly clean environment, you'll have some baseline noise:

- **Atmospheric noise (QRN)** — natural lightning at distance, especially loud on lower bands (160 m, 80 m) in summer.
- **Galactic noise** — picked up at higher HF and VHF; constant, very low.
- **Solar noise during high SFI** — slight increase in noise floor during high solar activity.

These can't be filtered or eliminated. They set the floor below which you can't go.

For most operators, the realistic noise floor target is:

- **Rural** — S0 to S2 on 80 m at midnight in winter.
- **Suburban** — S2 to S5.
- **Urban / apartment** — S5+, with peaks during evening hours.

If you're at S7+ all the time and you live somewhere that should be quieter, this chapter will help you get back to baseline.

## See also

- §15 — RFI workflow (read this first if you haven't isolated a source yet)
- §17 — power-line noise (different chapter, different procedure)
- §13-06 — feedline routing (related — common-mode current matters here too)
