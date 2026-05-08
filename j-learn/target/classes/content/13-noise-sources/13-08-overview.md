---
id: 13-08
title: Power-Line Noise — Overview
chapter: 13
section: 08
level: simple
status: draft
---

# Power-Line Noise — Overview

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Power-line noise is its own beast. Unlike the household sources covered in §13-01 through §13-07, the source is **utility infrastructure** — poles, transformers, insulators, hardware — not anything inside your house. You can't fix it with a ferrite core; the fix involves the utility company. This subsection walks the diagnosis, the documentation, and the working-with-the-utility process.

## What "power-line noise" means

A few specific failure modes in the high-voltage distribution system that produce HF interference:

| Cause | Sound | Section |
|-------|-------|---------|
| Cracked / contaminated insulator | Sharp crackle, weather-related | §13-09 |
| Bad transformer (saturated, loose laminations) | 60 Hz buzz | §13-10 |
| Loose hardware (bolts, ground straps) | Intermittent crackle | §13-11 |
| Corona discharge | Whoosh / buzz, weather-related | §13-12 |

Symptoms in your station:

- Persistent broadband hash, often **worse in dry weather** (bad insulators) or **wet weather** (corona on contaminated surfaces).
- Often loudest on lower bands (160 m, 80 m), but can extend across all of HF.
- Doesn't change when you turn off your house breakers — proves it's external.
- May correlate with weather: high humidity, fog, light rain often makes it worse.

## How this subsection is organized

| § | Topic |
|---|-------|
| §13-09 | Arcing insulators (the most common cause) |
| §13-10 | Bad transformers |
| §13-11 | Loose hardware |
| §13-12 | Corona discharge |
| §13-13 | AM radio identification |
| §13-14 | SDR identification |
| §13-15 | Utility documentation (the most important practical section) |

Read §13-15 first if you've already identified power-line noise and want to know what to do about it. Read the rest if you're trying to characterize what specifically you're dealing with.

## The diagnostic difference vs household RFI

Power-line noise differs from household RFI in characteristic ways:

- **Persistent across seasons** (your neighbor's bad charger probably gets replaced eventually; a bad insulator can stay there for years).
- **Affects multiple receivers** in the same area — not just yours.
- **Direction-finds to a specific pole** — not to a house.
- **Not affected by anything you do in your house.**

This last point is the key test. Turn off all your breakers. If the noise stays, it's external. Then the question is: which pole?

## Working with the utility

This is the part most operators dread. Some realities:

- **Utilities are required to address harmful interference** to licensed services under FCC Part 15.
- **Utilities don't always respond promptly.** RFI is rarely their top priority.
- **Persistence wins.** Operators who document, follow up, and escalate get their problems fixed.
- **The right contact matters.** Most utilities have a specific "RFI" or "EMI" group, separate from line maintenance.

§13-15 covers the practical procedure in detail.

## How long does fixing it take?

Realistic timelines:

- **From first complaint to first utility visit**: 1–4 weeks.
- **From visit to identified problem**: 1 visit if it's an obvious bad insulator; multiple visits if intermittent.
- **From identification to repair**: 1–6 months, sometimes more.
- **Total cycle**: typically 3–9 months from first complaint to silence.

Set expectations. The utility will move at its own pace.

## A note on perspective

Power-line noise is genuinely outside your control. You can't filter it from your house. The most you can do is:

- Document the problem precisely.
- Communicate it to the utility clearly.
- Follow up persistently.
- Consider receive-antenna improvements that null the source (a Beverage receive antenna pointed away from the offending pole works well).
- In severe cases, consider antenna placement that reduces pickup (further from the line, lower angle).

Many operators learn to live with some level of power-line noise because the utility is slow. That's a reasonable response — but don't accept it as permanent. File the complaint. Follow up.

## See also

- §12 — household RFI (different beast; this overview helps distinguish)
- §13-00 — chapter overview (covers both household and power-line halves)
- §13-15 — utility documentation procedure
