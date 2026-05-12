---
id: 07-08
title: EFHW (Trapped)
chapter: 07
section: 08
level: mixed
status: draft
---

# EFHW — End-Fed Half-Wave (Trapped)

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What it is

A trapped EFHW combines the **end-fed half-wave** topology (§07-07) with **trap circuits** (§07-13) to create a multi-band antenna in **less physical space** than a non-trapped EFHW. Each trap, resonant at one band, isolates a section of the wire above that band; below the trap's resonance the trap loads the antenna inductively.

It's the natural solution for portable / SOTA / POTA operations where a 134 ft wire isn't practical, and for backyards too small for a non-trapped 80m EFHW.

## How it works

A non-trapped 80m EFHW is ~134 ft long. Add a trap at the 32 ft mark resonant at **14.150 MHz** and:

- **On 20m**: the trap looks like an open. Only the inner 32 ft radiates as a 20m EFHW.
- **On 40m**: the trap looks slightly inductive (loading), and the wire effectively continues. Inner 32 ft + outer ~35 ft radiate as a (loaded) 40m EFHW.
- **On 80m**: the trap is even more inductive — full wire radiates as a (heavily loaded) 80m EFHW.

Compared to the non-trapped EFHW (which uses harmonics), the trapped EFHW gives you **specific bands you choose** rather than 1×/2×/3×/4× the fundamental. You can have a 40 + 20 + 17 + 15 m antenna by picking specific traps — not possible with a harmonic-only EFHW.

## Calculator inputs and outputs

The Antenna Workshop calculator (`EFHW (Trapped)`) is **length-driven**: specify available wire length and the design fits within it.

Inputs:

- **Number of bands** (2, 3, or 4)
- **Band frequencies** (MHz, one per band)
- **Available total wire length** (ft) — `0` means no constraint, build full-size
- **Trap capacitor** (pF)
- **Power** (W PEP)
- **Unun ratio** (49:1 or 64:1)

The calculator returns the same 9-section structured output as the trapped dipole — Overview, Segment Lengths, Loading Components, Placement, Feedpoint/Matching, SWR, Bandwidth, Performance Opinion, Warnings — adapted for the EFHW topology (single wire, unun feed, counterpoise).

## Shortened trapped EFHW design

A trapped EFHW already fits more bands into less wire than the harmonic-only version (§07-07). A shortened design shrinks it further by re-specing the traps with higher L / lower C (same f_r) and adding an inline seg-1 loading coil when needed.

### Mechanism

Same physics as the shortened trapped dipole (§07-05): the trap's below-resonance reactance is

```
X_trap(f) = (2π · f · L) / (1 − (f/f_r)²)
```

and we keep f_r fixed while increasing L (and decreasing C) to raise X on every band below.

For the EFHW specifically, the **first trap sees the most voltage** — it sits closer to the high-impedance end of the wire (just inside the unun). The calculator applies a ~2.5× voltage multiplier on top of the dipole-equivalent V_peak, which is what drives the brutal capacitor-type recommendations on shortened EFHW designs.

If seg 1 is shortened by more than ~10%, an inline loading coil is required inside seg 1 (placed roughly ⅓ to ½ along its length). Its inductance is computed from the missing physical length the same way as the trap loading.

### Bandwidth model

Same loaded-antenna BW formula as the dipole, but Rr is based on the EFHW characteristic input resistance (~36 Ω at full size at the unun input) rather than 73 Ω:

```
Rr ≈ 36 · (built/ideal)²
BW ≈ f · (Rr + X_load / Q) / X_load            Q ≈ 100 (traps), 200 (coils)
```

capped at f / 15.

### Worked example — 40/20/15 in 47 ft

```
Bands: 21.200 / 14.150 / 7.150 MHz
Available length: 47 ft (full = 66.85 ft, s = 70%)
Trap cap: 100 pF    Power: 100 W    Unun: 49:1

Segments:
  Seg 1 (unun → trap 1, ideal 22.5 ft):  15.8 ft  ← shortened
  Seg 2 (trap 1 → trap 2, ideal 11.2 ft): 7.9 ft  ← shortened
  Seg 3 (trap 2 → end, ideal 33.1 ft):   23.2 ft  ← shortened
  Total wire: 46.9 ft

Seg-1 inline coil: ~5.9 µH (placed midway along seg 1)

Trap 1 (21.2 MHz):  L ≈ 1.74 µH,  C ≈ 32 pF,  V_cap ≈ 3800 V → vacuum cap
Trap 2 (14.15 MHz): L ≈ 6.11 µH,  C ≈ 21 pF,  V_cap ≈ 5800 V → vacuum cap

Rr ≈ 18 Ω on every band (vs 36 Ω full-size)
Est. 2:1 BW:  15m ≈ 600 kHz,  20m ≈ 400 kHz,  40m ≈ 150 kHz
```

The voltage budget is brutal even at 100 W — at full power, both traps want vacuum capacitors. **At 25 W (typical SOTA) voltages halve** (V_peak scales with √P), so trap 1 drops to ~1900 V (doorknob territory) and trap 2 to ~2900 V (still doorknob with margin). **At QRP (5 W)** voltages drop to ~850 V / ~1300 V where high-grade mica works.

### Tradeoff guide

| L_avail / L_full | Wire length (40/20/15) | Verdict | Trap V @ 100W | Cap class @ 100W |
|------------------|------------------------|---------|----------------|------------------|
| 100%             | 67 ft                  | full size — best        | ~900 V  | mica             |
| 85%              | 57 ft                  | mild — practical        | ~2100 V | doorknob         |
| 70%              | 47 ft                  | moderate — SOTA-grade   | ~3800 V | vacuum           |
| 60%              | 40 ft                  | heavy — marginal        | ~5500 V | vacuum           |
| < 40%            | < 27 ft                | infeasible              | —       | —                |

> ⚙️ **Advanced —** EFHW trap voltage at full power exceeds the dipole equivalent because the trap nearest the unun sits in the high-Z region of the wire. For legal-limit (1500 W) shorty EFHW operation, **all** traps need vacuum capacitors. The cost stack ($150–600 in caps alone) means full-length EFHW + tuner is almost always the right call at high power.

### When to use a shorty EFHW

- SOTA / POTA / portable where pack size matters
- Apartment / HOA where 67 ft doesn't fit
- Stealth installations (shorter = less visible)
- Field Day rapid setup (smaller area cleared, fewer guy points)

### When NOT to use a shorty EFHW

- High power (> 200 W) — trap voltage gets brutal
- Single-band operation — a regular EFHW (no traps) is simpler and more efficient
- New builders — start with a non-trapped EFHW (§07-07) to learn the unun + counterpoise + trim workflow first

## Worked example — portable 40 / 20 / 15 m EFHW

```
band 1 (highest): 21.200 MHz
band 2: 14.150 MHz
band 3: 7.150 MHz
unun: 49:1

segment 1 (15m EFHW): 478 / 21.200 = 22.5 ft
trap 1 resonant: 21.200 MHz

segment 2 (20m extension):
  total at 20m = 478 / 14.150 = 33.8 ft
  segment 2 length = 33.8 − 22.5 = 11.3 ft
trap 2 resonant: 14.150 MHz

segment 3 (40m extension):
  total at 40m = 478 / 7.150 = 66.8 ft
  segment 3 length = 66.8 − 33.8 = 33.0 ft

total wire: 66.8 ft
counterpoise: ~3.3 ft (5% of innermost segment 1 → that's only 1.1 ft. Use total/20 ≈ 3 ft instead for harmonic balancing)
```

A 67-foot trapped EFHW covers 40 / 20 / 15m precisely (no harmonic overlap). Excellent for SOTA — packs into a small bag, deploys with one tree, covers three bands cleanly.

## Common mistakes

- **Trap voltage rating wrong.** EFHW traps see HIGHER voltage than dipole traps because the wire's high-impedance end is on the unun side. A trap at the inner third of the wire might see 3–5 kV at full power. Vacuum capacitors only for legal-limit operation.
- **Wrong order of construction.** Build & trim from the unun outward — innermost segment first.
- **Skipping the bench test.** Each trap must be verified before installation. A NanoVNA or dip meter on the bench gives you precise resonance.
- **Counterpoise sizing.** With multiple bands, the optimal counterpoise length is a compromise — try ~5% of the longest active radiating section.
- **Trying to add a 4th band.** Three bands is the practical maximum for a sane build. Use a tuner for the WARC bands instead of stacking more traps.

> ⚙️ **Advanced —** Each trap on a trapped EFHW adds ~0.5–1.5 dB of loss on the bands below its resonance (it's acting as a lossy loading coil). On the lowest band of a 3-trap design, total trap loss can be 2–4 dB — compare to a non-trapped EFHW (essentially no loss, just length). The space savings come at this efficiency cost.

## Build & trim notes

1. **Build all traps first** using §07-13 Trap Design. Verify each on a NanoVNA.
2. **Cut the innermost segment** to 478/f for the highest band; install the unun at one end.
3. **Sweep on the highest band**, trim segment 1 for resonance.
4. **Add the first trap** and segment 2, sweep on the next band, trim segment 2.
5. **Repeat** for the third trap and segment.
6. **Final sweep on all bands** — expect SWR 1.5:1–2.5:1 typical.
7. **Weatherproof every trap** — water shifts resonance and ruins the entire antenna.

## See also

- §04-08 — Traps (theory)
- §04-04 — EFHW (theory)
- §07-00 — Antenna Workshop overview
- §07-07 — EFHW (No Traps) — simpler, harmonic-only variant
- §07-13 — Trap Design
- §15-05 — Resonant Frequency
