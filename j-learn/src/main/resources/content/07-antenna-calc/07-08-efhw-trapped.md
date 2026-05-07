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

The Antenna Workshop calculator (`EFHW (Trapped)`) takes:

- **Highest band frequency** (MHz) — innermost segment
- **Next band down** (MHz)
- **Optional 3rd band** (MHz, even lower)
- **Unun ratio** — 49:1 or 64:1

And returns:

- Length of each segment (unun → trap 1, trap 1 → trap 2 or end, etc.)
- Trap design frequencies for each trap
- Counterpoise length
- Unun spec (turns count for the chosen ratio)

The actual trap component values come from **§07-13 Trap Design**.

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
- §17-05 — Resonant Frequency
