---
id: 15-00
title: Noise Sources — Overview
chapter: 15
section: 00
level: simple
status: published
---

# Noise Sources — Overview

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

This chapter is a per-category guide to the most common noise sources you'll meet on the air. Each section covers what the source looks like in operation, what it sounds like, and the specific filtering or replacement strategies that work for that category.

For the higher-level workflow of how to *find* a noise source, see chapter 14 (RFI). This chapter assumes you've already identified what kind of device or infrastructure you're dealing with and are looking for fix specifics.

## Two subsections

The chapter is organized in two halves:

**§15-01 through §15-07 — household sources.** Things inside your home (or your neighbor's) that you can usually fix yourself with ferrites, replacement, or relocation.

**§15-08 through §15-15 — power-line noise.** Utility infrastructure problems — bad insulators, transformers, hardware. You can't fix these with a ferrite. The fix involves the utility company, and the procedure is different.

## Section index

| § | Source category | Subsection |
|---|-----------------|------------|
| §15-01 | Switching power supplies | Household |
| §15-02 | LED lights | Household |
| §15-03 | Solar inverters | Household |
| §15-04 | Ethernet over power | Household |
| §15-05 | HVAC | Household |
| §15-06 | Battery chargers | Household |
| §15-07 | Motor brushes | Household |
| §15-08 | Power-line noise — overview | Power-line |
| §15-09 | Arcing insulators | Power-line |
| §15-10 | Bad transformers | Power-line |
| §15-11 | Loose hardware | Power-line |
| §15-12 | Corona discharge | Power-line |
| §15-13 | AM radio identification | Power-line |
| §15-14 | SDR identification | Power-line |
| §15-15 | Utility documentation | Power-line |

Read whichever section matches what you've identified.

## A general principle: source vs receptor

Some sources can be made quieter (replace, filter at the device). Others are immune to fixing — a powerline-Ethernet adapter is *designed* to use the HF spectrum and can't be made quiet without abandoning it. Power-line noise is similar: you can't fix it from inside your house at all.

For sources you can't fix, the next move is to harden the receptor — your radio. That means:

- **Better antennas** with deeper noise nulls (loops, Beverages).
- **Noise blankers and digital noise reduction** in the radio.
- **Operating during quiet hours** when offending devices aren't on.
- **Operating from a different physical location** (mobile, portable, away from the noisy environment).

Each per-category section discusses what's possible at the source and what isn't.

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

- §14 — RFI workflow (read this first if you haven't isolated a source yet)
- §12-06 — feedline routing (related — common-mode current matters here too)
- §15-08 — power-line noise overview (start of the second half of this chapter)
