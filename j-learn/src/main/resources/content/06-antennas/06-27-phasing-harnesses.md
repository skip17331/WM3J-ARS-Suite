---
id: 06-27
title: Phasing Harnesses & Stacking
chapter: 06
section: 27
level: mixed
status: draft
---

# Phasing Harnesses & Stacking

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Two antennas pointed the same way give you more gain than one — but you cannot just tie their feedlines together at a tee and expect it to work. **Two 50 Ω antennas in parallel present 25 Ω**, a 2:1 mismatch, and unless the two feedlines are electrically identical the antennas won't even be in phase. A *phasing harness* is the short, carefully-cut cable assembly that solves both problems at once: it keeps the antennas in phase **and** transforms the combined impedance back to something your coax wants to see.

This section covers combining **identical antennas, fed in phase, for gain** — the stacked-Yagi, stacked-vertical, or bayed-dipole case. Steering a pattern by feeding elements *out* of phase (cardioid verticals, four-squares) is a different problem; see "What this section does not cover" below.

## The impedance problem

Antennas combine like resistors. Tie *N* identical antennas of impedance Z directly in parallel and the feedpoint sees **Z / N**:

| What you parallel | Combined impedance | SWR on 50 Ω coax |
|-------------------|--------------------|------------------|
| Two 50 Ω antennas | 25 Ω | 2.0:1 |
| Three 50 Ω antennas | 16.7 Ω | 3.0:1 |
| Four 50 Ω antennas | 12.5 Ω | 4.0:1 |

So the harness has to *raise* each antenna's impedance before the junction, so that the parallel combination lands back at 50 Ω.

## The Q-section harness

The standard fix uses a **quarter-wave line as an impedance transformer** (the same trick as §06-03). A quarter-wave of line with characteristic impedance Z₀ transforms a load Z_L to:

```
Z_in = Z₀² / Z_L
```

For two antennas you want each ¼λ section to turn its 50 Ω antenna into **100 Ω**, so the two 100 Ω legs in parallel make 50 Ω. Solve for the line impedance:

```
Z₀ = √(Z_in × Z_L) = √(100 × 50) = 70.7 Ω
```

- **70.7 Ω** is the textbook answer. Nobody stocks it, so in practice you use **75 Ω** cable (RG-11, RG-6, RG-59).
- With real 75 Ω line: 75² / 50 = 112.5 Ω per leg → two in parallel = **56 Ω → ~1.13:1**. Close enough that no one cares.

Both ¼λ sections must be the **same electrical length** (an odd multiple of ¼λ). That equal length is what keeps the two antennas fed in phase — the quarter-wave does double duty: impedance transform *and* equal delay.

> **Advanced —** The general rule for combining *N* identical antennas of impedance Z_a back to a target Z_t with one transformer per element is `Z₀ = √(Z_a × N × Z_t)`. For two 50 Ω antennas to 50 Ω that's √(50 × 2 × 50) = 70.7 Ω. An alternative for two elements is to parallel them directly to 25 Ω and use a *single* ¼λ matching section of √(25 × 50) = 35 Ω back to the rig — but 35 Ω coax is awkward stock (you'd parallel two 70 Ω lines), and the twin-75 Ω-Q-section approach is preferred because the per-element sections also equalize phase. Either way the harness is narrowband: the ¼λ transform is exact on one frequency and drifts as you move off it, so a stacked monoband Yagi harness is fine but a stacked tribander needs a harness designed for the compromise.

## Cutting the harness

A quarter-wave physical length is:

```
L (ft) = 246 × VF / f(MHz)         L (m) = 75 × VF / f(MHz)
```

where **VF is the velocity factor of the actual cable** — measure it, don't trust the spec to better than ±2% (see §18-03).

| Band | Cable | VF | ¼λ section length |
|------|-------|-----|-------------------|
| 20 m (14.1 MHz) | RG-11 (solid PE) | 0.66 | 11.5 ft (3.51 m) |
| 6 m (50.1 MHz) | RG-11 (solid PE) | 0.66 | 3.24 ft (0.99 m) |
| 2 m (146 MHz) | RG-6 (foam) | 0.85 | 1.43 ft (0.44 m) |

Cut **both** sections from the **same reel, to the same length**, and terminate them identically — any difference in length or VF between the two legs becomes a phase error and skews the pattern.

## What stacking buys you

Two identical antennas, stacked and fed in phase, give:

- **Up to ~2.5–3 dB gain** over a single antenna (the theoretical limit for doubling aperture is 3 dB; real harness loss and imperfect spacing eat a little).
- **A narrower pattern in the stacking plane** — stack vertically and the elevation lobe tightens (lower takeoff for DX); bay horizontally and the azimuth beamwidth narrows.
- The gain only materializes at a sensible **stacking distance** (roughly 0.5–1 λ between Yagis, optimized per design). Too close and they interact and lose gain; too far and grating lobes appear. See §06-07 for the pattern view.

## Three or more antennas

For a 4-stack, either run a **4-way harness** (each leg `Z₀ = √(50 × 4 × 50) = 100 Ω` quarter-wave — i.e. paralleled to give 50 Ω, though 100 Ω coax is rare), or — far more common — build it in **two tiers**: combine the antennas in pairs with twin-75 Ω Q-sections, then combine the two pair-junctions with a second harness. Tiered harnesses keep every section at a buildable impedance and keep the phase bookkeeping simple.

## What this section does *not* cover

This is about feeding identical antennas **in phase** for gain. Deliberately feeding elements **out of phase** to *steer* a pattern — 90° offsets for a cardioid two-element vertical, four-squares, and the Christman / Lewallen / Collins feed methods — is covered in §06-29 (Phased Arrays & Directivity). Those harnesses use unequal electrical lengths *on purpose*; everything here assumes equal lengths and broadside (in-phase) combining.

## Common mistakes

- **Tying the coax together at a tee.** Two 50 Ω antennas → 25 Ω → 2:1, and no phase control. This is the mistake the whole section exists to prevent.
- **Using 50 Ω cable for the Q-sections.** A ¼λ of 50 Ω transforms 50 → 50 (no transform), so you're back to paralleling 50 Ω antennas into 25 Ω. The sections *must* be 75 Ω (≈70 Ω).
- **Unequal leg lengths or different cable.** Becomes a phase error → the pattern tilts or splits and you lose the stacking gain.
- **Wrong velocity factor** when cutting — the number-one reason a homebrew harness lands on the wrong frequency. Measure it (§18-03).
- **Forgetting harness loss.** The harness adds insertion loss and splits power; budget it (§11).
- **Stacking mismatched antennas** (different heights, different ground, dissimilar designs). Unequal feedpoint impedances mean the legs no longer combine to 50 Ω and the currents are unbalanced.

## See also

- §06-29 — Phased arrays & directivity (the same elements fed *out* of phase, for a steerable beam)
- §06-03 — Impedance transformation (the ¼λ transformer this is built from)
- §06-07 — Radiation patterns (what stacking does to the lobe; stacking distance)
- §18-03 — Velocity factor (cutting phasing and matching lines accurately)
- §06-04 — Baluns and chokes (still want a choke at the array's common feedpoint)
- §11 — Power budget & ERP (harness loss and power split)
