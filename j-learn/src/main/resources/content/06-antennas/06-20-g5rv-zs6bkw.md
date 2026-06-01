---
id: 06-20
title: G5RV and ZS6BKW
chapter: 06
section: 20
level: mixed
status: draft
---

# G5RV and ZS6BKW

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The G5RV is the most misunderstood antenna in amateur radio. It is *not* a no-tuner all-band antenna, and Louis Varney never claimed it was — he designed it in 1946 as a **20 m antenna** (three half-waves in phase) that happened to be usable elsewhere. The ZS6BKW, designed decades later by Brian Austin, is what people *think* the G5RV is: a mathematically optimized version that actually does match five bands without a tuner.

Both are a flat-top wire fed through a section of balanced line that acts as an impedance transformer, then coax to the shack.

## The G5RV

| Part | Full-size | Half-size |
|------|-----------|-----------|
| Flat-top | 102 ft (31.1 m) | 51 ft (15.5 m) |
| Matching section | ~34 ft of 300 Ω, or ~31 ft of 450 Ω line | half of full |
| Then | 1:1 choke + 50/75 Ω coax to shack | same |
| Designed band | 20 m | 40 m |

On 20 m the flat-top is 3 half-waves long and the matching section transforms its high feed impedance down near 50 Ω — that's the band it actually works well on without help. On **every other band you need an ATU.** With a tuner the full-size G5RV is a serviceable 80–10 m antenna; without one, it's a 20 m antenna that's mediocre everywhere else.

## The ZS6BKW (the one you probably want)

Brian Austin (ZS6BKW, also G0GSF) re-ran the math to find the flat-top length *and* matching-section length that put the feed impedance inside 2:1 of 50 Ω on the most bands simultaneously:

| Part | Dimension |
|------|-----------|
| Flat-top | ~93 ft (28.4 m) |
| Matching section | ~40 ft (12.2 m) of 450 Ω window line — **scaled by the line's velocity factor** |
| Then | 1:1 choke + coax |
| No-tuner bands | 40, 20, 17, 12, 10 m |

Note what's *missing*: no 80, no 30, no 15 m without a tuner (15 m falls where the matching transform misses). But on the five bands it does cover, it's a genuine no-ATU antenna — which the G5RV is not.

> **Advanced —** The flat-top and matching section together form a two-section transmission-line transformer. The flat-top length sets the load impedance presented to the matching line on each band; the matching-line length (electrical, not physical) rotates that impedance around the Smith chart toward 50 Ω. Because the rotation depends on *electrical* length, the matching section must be scaled by the **velocity factor** of the line you actually use (≈0.91 for 450 Ω window line, ≈0.82 for solid 300 Ω). Use a published physical length with the wrong line VF and you detune all five bands at once. See §06-01 for the Smith-chart view.

## The velocity-factor trap

This is the mistake that ruins more G5RV/ZS6BKW builds than anything else: **the matching section is specified as an electrical length.** If you buy "450 Ω ladder line" with a different VF than the design assumed, you must rescale the physical length. Measure or look up your line's VF and multiply. Get this wrong and the whole transformer is in the wrong place.

## Feeding the bottom of the matching section

The junction between the balanced matching section and the coax is a balance-to-unbalance transition — put a **1:1 current choke** there (§06-04). Without it the coax shield becomes part of the antenna and the patterns and SWR wander. Keep the matching section in the clear, away from metal and the ground, since it radiates a little and its impedance shifts near conductors.

## When to pick one

- **ZS6BKW** — you want a single horizontal wire that's no-tuner on 40/20/17/12/10 and you have ~93 ft of span.
- **G5RV** — you already own one, or you want a 20 m wire and will run a tuner for everything else.

## When to avoid

- You want a true all-band no-compromise antenna — use a **doublet** (§06-21) fed with ladder line straight into a balanced tuner. It beats both on every band because there's no fixed matching section forcing compromises.
- You need 80 m — neither covers it without a tuner, and on 80 the short flat-top is electrically tiny.

## Common mistakes

- **"It's an all-band antenna."** The G5RV is not. Believe the band tables above.
- **Wrong matching-line velocity factor** — the number-one detuner.
- **No choke at the coax junction** — feedline radiation, wandering SWR.
- **Coax-feeding it directly** — defeats the entire matching scheme; the balanced section is the antenna's matching transformer.

## See also

- §06-10 — Dipoles (the flat-top is a dipole)
- §06-21 — Doublet (the better all-band choice — no fixed matching section)
- §06-01 — Smith charts (how the matching section is found)
- §06-04 — Baluns and chokes (the 1:1 at the coax junction)
- §06-03 — Impedance transformation
