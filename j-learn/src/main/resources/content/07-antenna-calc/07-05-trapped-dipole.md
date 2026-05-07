---
id: 07-05
title: Trapped Dipole
chapter: 07
section: 05
level: mixed
status: draft
---

# Trapped Dipole

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What it is

A trapped dipole is a single-wire, multi-band antenna in which **resonant L-C "trap" circuits** isolate sections of the wire on different bands. On the highest band, only the wire inside the innermost traps radiates. Step down a band: the next-outer traps "open up" (look like high-Z opens), and a longer section of wire radiates. Step down again: another pair of traps opens.

The result is a single wire (one feedpoint, one balun, one coax run) that resonates on multiple bands — in less horizontal space than a fan dipole.

## How it works

Each trap is a parallel L-C circuit resonant at the boundary between two bands. At its design frequency, the parallel L-C presents a very high impedance (an open circuit, ideally). Below the trap's resonant frequency, the L dominates and the trap looks inductive (acting as a loading coil — which lets it shorten the antenna physically). Above its design frequency, the C dominates and it looks like a small capacitive reactance.

A **trap dipole for 80/40/20 m** has traps resonant at 14 MHz at the inner boundary and at 7 MHz at the outer boundary:

- **On 20 m**: both traps look like opens. Only the inner segment (~33 ft total) radiates as a 20m dipole.
- **On 40 m**: the outer (7 MHz) trap looks like an open; the inner (14 MHz) trap looks inductive — adding loading inductance. Inner segment + first outer extension radiate.
- **On 80 m**: both traps look inductive (loading the antenna). Full wire radiates as a (heavily loaded) 80m dipole.

The trap acting as loading inductance below resonance is why the antenna is **shorter than three independent dipoles would suggest** — typically 6–8% per trap.

## Calculator inputs and outputs

The Antenna Workshop calculator (`Trapped Dipole`) takes:

- **Up to 3 band frequencies** (MHz, in descending order — innermost band first)
- **Trap effective length factor** (k) — accounts for the antenna's electrical shortening below trap resonance (typical 0.90–0.94)

And returns:

- Inner segment length (each side, resonant on highest band alone)
- Trap 1 design frequency
- Outer segment 1 length
- Trap 2 design frequency (if 3rd band specified)
- Outer segment 2 length
- Total wire length per side
- Match recommendation (50 Ω coax, 1:1 balun)

The actual trap component values come from **§07-13 Trap Design** — the calculator's outputs feed directly into that.

## Worked example — 80 / 40 / 20 m trap dipole

```
band 1 (highest): 14.150 MHz
band 2: 7.150 MHz
band 3 (lowest): 3.700 MHz
trap shortening factor k = 0.92

inner segment (20m alone): 468 / 14.150 / 2 = 16.54 ft each side
trap 1 resonant at: 14.150 MHz
total length at 40m (k applied): 468 / 7.150 × 0.92 = 60.21 ft
outer segment 1 (each side) = (60.21 − 33.08) / 2 = 13.57 ft
trap 2 resonant at: 7.150 MHz
total length at 80m (k×k applied): 468 / 3.700 × 0.92² = 107.05 ft
outer segment 2 (each side) = (107.05 − 33.08 − 27.14) / 2 = 23.42 ft

total wire per side: 16.54 + 13.57 + 23.42 = 53.53 ft
```

The full trapped dipole is **~107 ft tip-to-tip** — vs. ~127 ft for a full-length 80m dipole (a ~16% reduction in span). This is the main reason traps are used: shorter antenna in less space.

## Common mistakes

- **Buying mismatched traps.** A 14 MHz trap from one vendor doesn't necessarily match a 14 MHz trap from another — different L:C ratios produce different shortening. Build all traps the same way, or buy from one vendor.
- **Skimping on trap voltage rating.** Voltage across the trap can exceed √(P × Q × X_L) at resonance. For a 100 W trap at Q = 100 and X_L = 500 Ω, that's ~700 V peak. For legal limit (1500 W), >2.5 kV — needs vacuum capacitors.
- **Using lossy traps.** Each trap adds ~0.3–1 dB of loss at the band where it's not at resonance (acting as inductive loading). Three traps = ~1–3 dB total loss. Air-core, large-diameter coils minimize this.
- **Trying to trim from outermost in.** Build and trim from **innermost band out** — the inner segment doesn't depend on outer-band traps' values.
- **Forgetting traps detune over temperature.** Sealed weatherproof traps drift slightly with temperature (±0.1% per 20°C is typical for a quality build). Re-check resonance seasonally.

> ⚙️ **Advanced —** A trap dipole acts as a *resonant trap* + *loading coil* below resonance. The effective inductance presented at the feedpoint at the lowest band is large, narrowing the bandwidth. A 2-trap 80/40/20 dipole typically has 80m bandwidth of ~50 kHz for SWR ≤ 2:1, vs. ~250 kHz for a full-length 80m dipole. This is the bandwidth tradeoff for the space saving.

## Build & trim notes

1. **Build the traps first** using the §07-13 Trap Design calculator. Verify each trap's resonance with a NanoVNA or dip meter on the bench before installing.
2. **Construct the inner segment** and trim it for the highest band — bypass outer segments with shorting wires during this trim.
3. **Add the first pair of traps**, attach outer-1 segments, and trim the next-band resonance.
4. **Repeat** for each subsequent trap pair.
5. **Final SWR sweep at all bands** — expect higher SWR than a single-band dipole (typical 1.5:1 at center, rising to 2:1 at band edges).
6. **Re-seal the traps** every spring. Water in a trap shifts its resonance and can cause arcing.

## See also

- §04-08 — Traps (theory chapter for trap antennas)
- §07-00 — Antenna Workshop overview
- §07-13 — Trap Design (component values, coil winding)
- §07-04 — Fan Dipole (alternative approach to multi-band)
- §07-15 — NanoVNA Trim Workflow
- §17-05 — Resonant Frequency
- §17-11 — Q Factor (relevant to trap loss)
