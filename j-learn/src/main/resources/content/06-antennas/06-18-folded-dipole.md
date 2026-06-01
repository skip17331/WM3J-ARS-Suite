---
id: 06-18
title: Folded Dipole
chapter: 06
section: 18
level: mixed
status: draft
---

# Folded Dipole

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A folded dipole is a half-wave dipole built from two parallel conductors, shorted together at both ends, with the feedline tapped into the *middle of one conductor*. It is the same length as an ordinary dipole and radiates the same figure-eight pattern — but it does two useful things the plain dipole doesn't: it raises the feed impedance by 4× and it widens the usable bandwidth.

You meet folded dipoles in two places without always realizing it: the driven element of nearly every commercial Yagi is a folded dipole, and the flat "twin-lead" antenna taped behind an old FM tuner is a folded dipole made of 300 Ω ribbon.

## The headline numbers

| Property | Value |
|----------|-------|
| Total length | Same as a plain dipole — 468 / f(MHz) feet, 143 / f(MHz) m |
| Feed-point impedance (equal-diameter conductors) | ~280–300 Ω (4 × the ~70 Ω of a plain dipole) |
| Pattern | Figure-eight broadside to the wire — identical to a plain dipole |
| Gain over a dipole | 0 dBd — folding does **not** add gain |
| Bandwidth | Roughly 2× a single-wire dipole of the same band |
| Polarization | Same as the wire |

The gain is identical to a plain dipole. Anyone who tells you a folded dipole "has gain" is confusing the 4:1 impedance step-up with radiated gain. They are unrelated.

## Why the impedance goes up 4×

The two conductors carry the antenna current in parallel, so the current *entering the feedpoint* is half what a single-wire dipole would draw for the same radiated power. Half the current at the same power means four times the impedance: 70 Ω becomes ~280 Ω. That number is what makes the folded dipole convenient.

> **Advanced —** The 4:1 ratio holds only when the two conductors have equal diameter. With unequal-diameter conductors the step-up ratio is set by the conductor-diameter and spacing geometry and can be designed for 1.5:1, 2:1, 9:1, and other values — this is how some matching "gamma-free" driven elements and the wider-bandwidth multiwire dipoles set their feed impedance. The general transmission-line model treats the folded section as a shorted stub in parallel with the radiating-mode impedance; see §06-03.

## Feeding it

The ~300 Ω feedpoint matches three common things directly:

- **300 Ω TV twinlead** — connect it straight across. This is the classic attic/closet FM and 2 m receive antenna.
- **450 Ω window line into a tuner** — fine, the tuner finishes the small remaining mismatch.
- **50 Ω coax through a 4:1 balun** — 300 Ω ÷ 4 = 75 Ω, an SWR of ~1.5:1 on 50 Ω. Use a **4:1 current balun** (§06-04), not a voltage balun, and you have a clean coax-fed folded dipole.

## Building one from window line

The easiest homebrew folded dipole *is* a length of 450 Ω window line:

1. Cut the window line to the dipole length (468 / f).
2. At the exact center, cut **one** conductor and separate the ends — that gap is your feedpoint.
3. At each far end, short the two conductors together (solder, then weatherproof).
4. Feed the gap with a 4:1 balun to coax, or with twinlead/ladder line to a tuner.

That's a complete, wide-band, single-band antenna for the cost of a few feet of window line.

## When to pick a folded dipole

- You want the **widest bandwidth** a simple wire dipole can give — e.g., all of 80 m under 2:1, or a 6 m dipole that covers the whole band.
- You want to feed with **twinlead or 450 Ω line** without a separate matching section.
- You're building a **Yagi driven element** and want the 4× impedance to swallow the impedance drop that parasitic elements cause.

## When it's not worth it

- You're matching 50 Ω coax and don't need the bandwidth — a plain dipole + 1:1 choke is simpler.
- Mechanical: two parallel conductors with rigid spacing are fussier to build and heavier than a single wire.

## Common mistakes

- **Expecting gain.** There is none over a plain dipole.
- **Using a 4:1 voltage balun** where a current balun belongs. The folded dipole is balanced; you want common-mode suppression, which only the current type gives. See §06-04.
- **Forgetting to short the far ends.** An unshorted "folded dipole" is just a weird open-wire stub and won't work.
- **Unequal spacing along the span.** The spacing sets the impedance; keep it constant with spreaders or use window line.

## See also

- §06-10 — Dipoles (the reference this is folded from)
- §06-03 — Impedance transformation (the 4:1 step-up explained)
- §06-04 — Baluns and chokes (which 4:1 to use)
- §06-22 — Fan dipole (the other way to get multiband from dipoles)
- §06-30 — Beams (the folded dipole as a Yagi driven element)
- §09 — Antenna calculator
