---
id: 06-26
title: Linked Dipole
chapter: 06
section: 26
level: mixed
status: draft
---

# Linked Dipole

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A linked dipole is a multi-band dipole whose legs are built in segments joined by small insulated connectors — the **links**. Open a link and the outer segment falls away electrically, shortening the antenna so it's resonant on a higher band. Close it again and you're back on the lower band. It's the SOTA and POTA operator's favorite: full dipole efficiency on every band, no tuner, no lossy unun, and it rolls up to almost nothing — at the price of having to walk out and physically flip links to change band.

## How it's built

Start with a dipole cut for your **lowest** band. At the points where the wire would be the right half-length for each higher band, insert a link on **each leg** (symmetry matters):

```
feed
 │
 ●────[40 m segment]────◯links◯────[adds to 30 m]────◯links◯────[adds to 20 m]  (one leg)
```

- **All links closed** → full length → lowest band (e.g. 40 m).
- **Open the outer links** → 30 m length.
- **Open the next pair in** → 20 m length.

You tune from the **highest band first** (innermost links, shortest configuration), trimming that segment, then add a segment and tune the next band down, and so on outward. Each segment is trimmed with everything beyond its link disconnected, so the bands don't interact the way a fan dipole's do.

## What it's good at

| Property | Linked dipole |
|----------|---------------|
| Efficiency | Full resonant dipole on every band — best in class |
| Match | ~50 Ω on each band, no tuner |
| Feed | One coax + 1:1 choke balun |
| Weight / pack size | Tiny — just wire and link hardware |
| Band change | **Manual** — lower the antenna, flip links, raise it again |

For a hilltop activation where you carry everything on your back and want a clean 50 Ω match on 40/30/20 m, nothing beats it on the efficiency-per-gram axis.

## The links themselves

The link is the whole design problem — it's a connector that must be insulated when open, conductive when closed, light, and survive being flipped in the field with cold fingers:

- **Anderson Powerpoles**, small **bullet/banana** pairs, or simple **insulated hooks/loops** are all common.
- Build in a little **strain relief** so the mechanical load isn't carried by the electrical connection — the link is the weak point that fails.
- Mark or color-code which link is which band; in the field you won't remember.

> **Advanced —** Opening a link doesn't perfectly isolate the outer segment — there's still stray capacitance across the open connector and along the dangling wire, which pulls the active band's resonance down slightly. It's small, but it's why you trim each segment *in its operating configuration* (links beyond it open) rather than computing lengths from 468/f and trusting them. The dangling outer segments also load the antenna a hair; keeping them taut and in line, rather than letting them flap, keeps the tuning repeatable trip-to-trip.

## When to pick a linked dipole

- **Portable / SOTA / POTA** — the lightest full-efficiency multi-band antenna.
- You want a guaranteed ~50 Ω match with **no tuner** and no unun losses.
- You're fine changing bands occasionally rather than constantly (you must lower it each time).

## When to avoid

- **Contest or fast band-hopping** from a fixed station — lowering the antenna per band change is a non-starter; use a fan dipole (§06-22) or EFHW (§06-13).
- The antenna is hard to reach (high, over water, in a tree) — you can't get to the links.

## Common mistakes

- **Tuning in the wrong order** — start with the highest band (shortest), trim, then work down/outward.
- **Flimsy link hardware** — the mechanical failure point; add strain relief and use rugged connectors.
- **Asymmetric links** — both legs must be linked at matching points or the pattern and match skew.
- **Computing lengths and not trimming** — stray link capacitance means you trim each segment in place.

## See also

- §06-10 — Dipoles (the resonant baseline each configuration becomes)
- §06-22 — Fan dipole (multiband with no band-change handling, but heavier)
- §06-13 — EFHW (the other portable multiband favorite — unun vs. links trade-off)
- §06-11 — Inverted V (the usual portable hang for a linked dipole)
- §09 — Antenna calculator
