---
id: 09-05
title: Trapped Dipole
chapter: 09
section: 05
level: mixed
status: draft
---

# Trapped Dipole

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

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

The Antenna Workshop calculator (`Trapped Dipole`) is **length-driven**: you specify how much space you have, and the design fits inside it.

Inputs:

- **Number of bands** (2, 3, or 4)
- **Band frequencies** (MHz, one per band)
- **Available total length** (ft, tip-to-tip) — `0` means no constraint, build full-size
- **Trap capacitor** (pF) — your standard build value, used to compute trap L
- **Power** (W PEP) — drives the required capacitor voltage rating

The output is organized into 9 sections:

1. **Overview** — design type and rationale for any shortening
2. **Segment Length Plan** — ideal full-size lengths vs. as-built lengths
3. **Loading Components** — per-trap L, C, capacitor voltage and type; any inner-segment loading coil; capacitive-hat advice
4. **Trap/Coil Placement** — placement rules and a per-side wire layout
5. **Feedpoint Impedance & Matching** — estimated Z on each band and matching recommendation
6. **Expected SWR Behavior** — qualitative curve shape and the narrowest band
7. **2:1 SWR Bandwidth per Band** — bandwidth estimate using the loaded-antenna BW formula
8. **Performance & Efficiency Opinion** — verdict, radiation resistance trend, trap insertion loss
9. **Warnings & Practical Notes** — flags for marginal designs and alternative topologies

The L and C values feed directly into **§09-13 Trap Design** for the build recipe.

## Shortened trapped dipole design

When the available antenna length is less than the full-size requirement, the calculator applies a uniform shortening factor `s = L_avail / L_full` to every segment and re-specs the traps to make up the missing inductive loading.

### Mechanism

A trap is a parallel L-C circuit resonant at one band, `f_r`. Below f_r it looks inductive; that inductive reactance is what makes the antenna "behave" electrically longer than its physical length. The trap's reactance at any frequency `f` below resonance is:

```
X_trap(f) = (2π · f · L) / (1 − (f/f_r)²)
```

For a shortened design we keep `f_r` fixed (so the trap still isolates its band) but **increase L and decrease C in lockstep** (the product stays constant). At every lower band the trap then presents a higher inductive reactance — which fills in the missing physical length.

The required extra reactance per side, for a missing physical length δ on the target band:

```
X_required ≈ Z₀ · tan(2π · δ / λ_band)        Z₀ ≈ 600 Ω
L_required = X_required · (1 − (f/f_r)²) / (2π · f)
C_required = 1 / (4π² · f_r² · L_required)
```

If the **inner segment** is also shortened (when the user's length budget forces it), the calculator adds a **center loading coil per leg** to make the inner segment electrically resonant on the highest band. The coil's inductance is computed the same way (X = Z₀·tan(2π·δ/λ_1), L = X/(2π·f_1)).

### Bandwidth model

The 2:1 SWR bandwidth on each band is estimated using the loaded-antenna formula:

```
BW ≈ f · (Rr + X_load / Q) / X_load
```

where:

- `Rr` = radiation resistance, falling as `73 · (built/ideal)²` with shortening
- `X_load` = total loading reactance presented on that band (trap or coil)
- `Q` ≈ 100 for air-core traps, ≈ 200 for air-core loading coils

This is capped at `f / 15` (the natural Q of a thin-wire half-wave element) so full-size designs don't return absurdly wide bandwidths.

### Worked example — 80/40/20 in 75 ft

```
Bands: 14.150 / 7.150 / 3.700 MHz
Available length: 75 ft (full-size requirement = 126.5 ft, s = 59.3%)
Trap cap: 100 pF    Power: 100 W

Per-side segments:
  Inner (λ/2 at 20m, ideal 16.54 ft):       9.81 ft  ← shortened
  Outer 1 (extends to 40m, ideal 16.19 ft): 9.60 ft  ← shortened
  Outer 2 (extends to 80m, ideal 30.52 ft): 18.09 ft ← shortened
  Total per side / tip-to-tip:               37.50 / 75.00 ft

Center loading coil per leg:  4.70 µH

Trap 1 (14.15 MHz):  L = 4.35 µH,  C = 29 pF,  V_cap ≈ 1970 V → doorknob
Trap 2 (7.15 MHz):   L = 10.67 µH, C = 46 pF,  V_cap ≈ 2190 V → doorknob

Radiation resistance: ~26 Ω on every band (vs 73 Ω full-size)
Est. 2:1 BW:  20m ≈ 940 kHz,  40m ≈ 770 kHz,  80m ≈ 320 kHz
```

### Tradeoff guide (representative)

| L_avail / L_full | Verdict | Lowest-band BW | Trap voltage @ 100W | Cap class |
|------------------|---------|----------------|---------------------|-----------|
| 100%             | full size — best        | ~250 kHz | ~340 V  | mica            |
| 85%              | mild — practical        | ~180 kHz | ~900 V  | mica/doorknob   |
| 70%              | moderate — popular QTH  | ~100 kHz | ~1700 V | doorknob        |
| 60%              | heavy — marginal        | ~50 kHz  | ~2200 V | doorknob/vacuum |
| 40%              | extreme — narrow & lossy| ~25 kHz  | ~2600 V | vacuum          |
| < 40%            | infeasible — pick another topology | — | — | — |

### When to consider alternatives

If the calc flags your design as marginal (s < 0.55) or infeasible (s < 0.40), consider:

- **Loaded vertical with elevated radials** — comparable footprint, often better efficiency on the lowest band
- **Magnetic loop** — very narrow BW but excellent efficiency for a small footprint
- **Drop the lowest band** — build full-size for the higher bands, use a tuner for the dropped band
- **Add capacitive end-hats** — reduces required loading reactance ~20–40%, broadens BW

### When NOT to use a shortened design

- Legal-limit (1500 W) operation — trap capacitor voltage gets brutal
- DX contesting — bandwidth narrowing hurts band-edge operation
- Educational/first-antenna build — start with full-length; the math and trim procedure are simpler

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

> **Advanced —** A trap dipole acts as a *resonant trap* + *loading coil* below resonance. The effective inductance presented at the feedpoint at the lowest band is large, narrowing the bandwidth. A 2-trap 80/40/20 dipole typically has 80m bandwidth of ~50 kHz for SWR ≤ 2:1, vs. ~250 kHz for a full-length 80m dipole. This is the bandwidth tradeoff for the space saving.

## Build & trim notes

1. **Build the traps first** using the §09-13 Trap Design calculator. Verify each trap's resonance with a NanoVNA or dip meter on the bench before installing.
2. **Construct the inner segment** and trim it for the highest band — bypass outer segments with shorting wires during this trim.
3. **Add the first pair of traps**, attach outer-1 segments, and trim the next-band resonance.
4. **Repeat** for each subsequent trap pair.
5. **Final SWR sweep at all bands** — expect higher SWR than a single-band dipole (typical 1.5:1 at center, rising to 2:1 at band edges).
6. **Re-seal the traps** every spring. Water in a trap shifts its resonance and can cause arcing.

## See also

- §06-17 — Traps (theory chapter for trap antennas)
- §09-00 — Antenna Workshop overview
- §09-13 — Trap Design (component values, coil winding)
- §09-04 — Fan Dipole (alternative approach to multi-band)
- §09-15 — NanoVNA Trim Workflow
- §17-05 — Resonant Frequency
- §17-11 — Q Factor (relevant to trap loss)
