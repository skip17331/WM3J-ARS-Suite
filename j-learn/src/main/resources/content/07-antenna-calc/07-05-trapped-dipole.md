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
- **Shorten beyond standard (%)** — 0 = conventional trapped dipole; 10–50% = shorty variant (see below)
- **Standard trap capacitor (pF)** — what you have on the bench; common choice is 100 pF
- **Power (W PEP)** — for capacitor voltage rating
- **Shorten inner segment too** — adds a center loading coil per side (off by default)

And returns:

- Inner segment length (each side)
- For each trap: design frequency, L (µH), C (pF), and required capacitor peak voltage with type recommendation
- Outer segment length(s)
- Total wire length per side, tip-to-tip span
- For shorty mode: estimated efficiency penalty and bandwidth narrowing
- Match recommendation (50 Ω coax, 1:1 balun)

The calculator's L and C values feed directly into **§07-13 Trap Design** for the build recipe.

## Shorty trapped dipole (non-ideal trap positions)

A **shorty trapped dipole** moves the trap inward from its natural resonant position so the antenna fits in less space. Because the trap below its resonant frequency already looks inductive (acting as a loading coil), making the trap *more inductive* (bigger L, smaller C, same f_r) lets it do more loading work — compensating for the now-too-short outer segment.

### How it works

The trap's reactance at any frequency f below its resonant f_r is:

```
X_trap(f) = (2π · f · L) / (1 − (f/f_r)²)
```

Keep f_r fixed (so the trap still isolates band 1), but **increase L and decrease C in lockstep**. The product L × C stays the same (f_r is preserved), but X_trap at every lower band scales up — providing the extra inductive loading needed for the shortened outer segment.

The needed extra reactance per side, for a missing physical length δ:

```
X_required ≈ Z₀ · tan(2π · δ / λ_band)
```

where Z₀ ≈ 600 Ω (rough characteristic impedance of wire over earth). Then:

```
L_required = X_required · (1 − (f/f_r)²) / (2π · f)
C_required = 1 / (4π² · f_r² · L_required)
```

### Worked example — 30% shorty 80/40/20

```
band 1: 14.150 MHz    band 2: 7.150 MHz    band 3: 3.700 MHz
shorten = 30%         standard cap = 100 pF    power = 100 W
shorten inner: no

Standard trapped 80/40/20: tip-to-tip ~107 ft, traps 1.27 µH / 100 pF.
30% shorty: tip-to-tip ~85 ft (outer segments shortened, inner kept full).

Per-side lengths:
  Inner segment (full λ/2 at 20m): 16.54 ft
  Outer 1 (was 13.57 ft):           9.50 ft
  Outer 2 (was 23.42 ft):          16.39 ft
  Total per side:                  42.43 ft  → tip-to-tip 84.86 ft

Trap 1 (14 MHz):
  L:  3.14 µH (vs 1.27 µH standard)
  C:  40 pF (vs 100 pF standard)
  V_cap peak at 100 W: ~1670 V → doorknob ceramic

Trap 2 (7 MHz):
  L:  8.12 µH
  C:  61 pF
  V_cap peak at 100 W: ~1910 V → doorknob ceramic

Est. efficiency penalty (80m): ~0.95 dB
Est. 2:1 SWR bandwidth (80m): ~49% of standard trapped (≈ 25 kHz)
```

If you enable **shorten inner segment too**, the inner drops to 11.6 ft each side and a center loading coil (~1.8 µH per leg) goes at the feedpoint. Total tip-to-tip falls to ~75 ft, but you take an additional efficiency hit on 20m and the coil at the feedpoint complicates the balun mounting.

### Tradeoffs

| Shortening | Tip-to-tip | Bandwidth | Efficiency | Trap cap voltage @ 100W | Cap type |
|------------|-----------|-----------|------------|--------------------------|----------|
| 0% (std)   | 107 ft    | 100%      | reference  | ~340 V                   | silvered mica |
| 15%        | 96 ft     | ~72%      | -0.6 dB    | ~900 V                   | mica or doorknob |
| 30%        | 85 ft     | ~49%      | -0.95 dB   | ~1700 V                  | doorknob |
| 45%        | 74 ft     | ~30%      | -1.5 dB    | ~2700 V                  | doorknob or vacuum |

Don't push past ~40% unless you're QRP and have very limited space — the bandwidth penalty makes operating on AM/SSB awkward (you may not cover the whole phone segment of a band).

### When to use a shorty

- Backyard too small for a full-length 80m trapped dipole (need < 90 ft)
- Apartment / HOA constrained installation
- Portable use where wire bag size matters

### When NOT to use a shorty

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
- §15-05 — Resonant Frequency
- §15-11 — Q Factor (relevant to trap loss)
