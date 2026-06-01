---
id: 06-21
title: Doublet (All-Band Tuned)
chapter: 06
section: 21
level: mixed
status: draft
---

# Doublet (All-Band Tuned)

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A doublet is a center-fed wire of *any convenient length*, fed with low-loss balanced line into a tuner. It is not cut to resonance on any band — that's the point. The ladder line carries the inevitable high SWR with almost no loss, and the tuner matches whatever impedance shows up at the shack end. For an operator who wants **every HF band from one wire and isn't afraid of a tuner**, the doublet is the most honest antenna there is: no fixed matching section to compromise it, no traps to fail, no balun straining on reactive loads.

## Why it works: the ladder-line trick

On a coax-fed antenna, high SWR means high loss — the mismatch energy sloshes back and forth through the lossy dielectric and heats the coax. **Open-wire / window line is different.** At 450 Ω with mostly-air dielectric, line loss is a fraction of a dB per hundred feet even at a 10:1 or 20:1 SWR. So you can deliberately run the antenna far off resonance, let the SWR on the balanced line be enormous, and lose almost nothing. The tuner cleans up the match at the shack.

| Feedline at 10:1 SWR, 100 ft, 14 MHz | Approx. loss |
|--------------------------------------|--------------|
| RG-58 coax | several dB — unusable |
| RG-213 coax | ~1.5–2 dB |
| 450 Ω window line | ~0.3 dB |

That difference is the whole reason the doublet exists.

## How long to make it

Pick a length that is **not** an exact multiple of a half-wavelength on any band you want, because at those lengths the center feedpoint impedance soars to thousands of ohms and becomes hard to match. Popular "plays nice everywhere" lengths:

| Length | Notes |
|--------|-------|
| 135 ft (41 m) | The classic all-band doublet; ½ λ on 80 m, covers 80–10 |
| 88 ft (27 m) | The Field Day favorite; covers 40–10 well, fits smaller lots |
| 44 ft (13 m) | Compact; 40–10 with a wide-range tuner |
| 102 ft (31 m) | The G5RV flat-top, but fed straight into a tuner (no matching section) |

A **center-fed Zepp** is the same antenna described with older terminology — a doublet fed with open-wire line.

## Feeding it: the tuner question

The doublet wants a **balanced** match. Two ways:

- **True balanced (link-coupled or differential) tuner** — the right answer. Balanced in, balanced out, no balun stressed by reactive loads.
- **4:1 (or 1:1) current balun ahead of an unbalanced tuner** — the common compromise. Works, but the balun sees the antenna's wild reactive impedance and can heat or saturate on the worst bands. A **1:1 current balun** placed at the *low-impedance* output of the tuner usually behaves better than a 4:1 at the high-impedance input.

> **Advanced —** Putting a balun on the antenna side of the tuner means it operates into whatever impedance the line presents on each band — sometimes a few ohms, sometimes a few thousand, often highly reactive. Voltage baluns flash over on the high-impedance bands; 4:1 baluns over-transform the low-impedance ones. A current (choke) balun on the 50 Ω side of an unbalanced tuner, or a genuine balanced tuner, sidesteps all of this. This is why "my tuner won't match 80 m" on a doublet is usually a balun problem, not a tuner problem.

## Practical install notes

- Keep the window line **clear of metal, gutters, and the ground** — at least 3–4 in from anything; its impedance and balance degrade near conductors.
- Avoid running it parallel to the tower or against the wall; bring it away from the antenna at as close to a right angle as you can.
- Twist or use standoffs at the entry; don't let it flap against the building.
- A doublet has no "designed band," so it has no SWR spec — the tuner makes the SWR. Judge it by on-air performance, not by a bandscope sweep.

## When to pick a doublet

- You want **literally every HF band** from one wire.
- You already run a tuner (most modern rigs that don't have one internally pair with an external).
- You want the simplest possible multi-band antenna with no failure-prone parts.

## When to avoid

- You refuse to run a tuner — then an EFHW (§06-13) or ZS6BKW (§06-20) gives you no-tuner bands instead.
- You can't route open-wire line cleanly into the shack (apartments, lots of metal) — coax-fed designs are easier there.

## Common mistakes

- **Coax-feeding a doublet.** Kills the low-loss advantage; now it's a bad random antenna.
- **A half-wave-multiple length** on a wanted band — unmatchably high feedpoint Z.
- **A 4:1 voltage balun at the tuner input** — the classic "won't tune 80/15 m" cause.
- **Window line against metal** — detunes and unbalances it.

## See also

- §06-10 — Dipoles (a doublet is a dipole you don't cut to resonance)
- §06-20 — G5RV / ZS6BKW (a doublet with a fixed matching section instead of a tuner)
- §06-03 — Impedance transformation (tuners and baluns)
- §06-02 — Feedline effects (why ladder line wins at high SWR)
- §10-04 — Feedline and SWR
