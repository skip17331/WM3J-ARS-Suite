---
id: 05-02
title: Inverted V
chapter: 05
section: 02
level: mixed
status: draft
---

# Inverted V

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The inverted V is a dipole with the center supported high and the ends drooping toward the ground. It's the antenna most hams *actually* end up with on HF, because it solves a real problem: a flat-top dipole needs **two** tall supports, and most yards only have **one** good tree, mast, or roof peak.

Think of an inverted V as a dipole that has been compromised — and the compromise is usually worth it.

## What you give up vs. a flat-top dipole

| Property | Flat-top dipole | Inverted V (90° between legs) | Inverted V (120° between legs) |
|----------|-----------------|-------------------------------|-------------------------------|
| Feed-point Z | ~70 Ω | ~50 Ω | ~60 Ω |
| Pattern | Sharp figure-8 | Rounded figure-8 (some omnidirectional fill) | Closer to flat-top pattern |
| Gain (broadside) | Reference (0 dBd) | About −1 dBd | About −0.3 dBd |
| Resonant length | 468/f ft | About 95% of dipole length (need to shorten) | About 98% (barely changes) |
| Required supports | 2 tall | 1 tall | 1 tall |
| Polarization | Pure horizontal | Mostly horizontal, some vertical | Almost all horizontal |

The takeaway: a moderately drooping V (120° apex angle, not a tight 90°) loses almost nothing compared to a flat-top, and gives you back one whole support. **The flat-top vs. inverted-V argument is mostly philosophical for most installations.**

## The 120° rule

The most-quoted rule of thumb is to keep the angle between the legs **at or above 120°** (i.e., each leg drooping no more than 30° below horizontal). Below that, two things start to happen:

1. **Feed impedance drops.** The currents in the two legs are no longer parallel, and they begin to cancel each other in the far field. The impedance falls toward 30 Ω.
2. **Gain drops, especially broadside.** The pattern fills in off the ends but loses its broadside peak.

A common geometry that works well: center support at 40 ft, ends tied off at 8 ft. For a 40-m inverted V (each leg ~33 ft), that's about a 110° apex angle — borderline acceptable. Better to raise the ends (or lower the center, but you'd rather not) to bring the apex angle to 120° or more.

## End height matters more than people think

The ends of an inverted V are the **high-voltage ends** of the antenna. There is significant E-field there. Three reasons to keep the ends high:

1. **Safety.** A 1500 W signal at the end of an antenna can produce several kV of RF voltage. People and pets get RF burns from grabbing the wire (see §07-07).
2. **Detuning by surroundings.** The ends are sensitive to nearby conductors — gutters, fences, tree branches, the chain-link fence. A drooping end too close to one of these will pull the resonance.
3. **Lossy ground coupling.** Capacitive coupling to wet earth at the ends bleeds power into the ground.

**Minimum end height: 8 feet.** Better: 12–15 feet. The center can be 30–40 feet and the ends at 12 feet, and you have a fine antenna.

## Cutting and trimming

Length is *slightly* shorter than a flat-top dipole. Empirical rule for a moderately drooping V (120° apex):

- Total length ≈ **463 / f(MHz)** feet (vs. 468 for flat-top)

That's a 1% reduction. For most installations that difference is within the trimming margin you'd cut anyway, so just use the 468 number and trim down.

If you have a steeply drooping V (under 100° apex), expect to trim 3–5% off the dipole length, and expect the feed impedance to be lower than 50 Ω.

## Feeding it

Same as a flat-top dipole: **1:1 current balun at the feedpoint, coax down to the rig.** Common-mode current is *worse* on an inverted V than a flat-top, because the slope means the antenna is no longer perfectly symmetric (the two halves see different impedances to ground). Skip the choke balun and you will hear about it on the air.

## Pattern characteristics

An inverted V at 0.4 λ above ground typically shows:

- **Maximum radiation broadside to the wire** but with significant fill toward the ends (so it's only a soft figure-8, not the dipole's sharp one).
- **Higher angle of radiation** than a flat-top dipole at the same center height, because the drooping legs add a vertical component. This actually **helps for short-skip / NVIS** work (talking to stations 100–500 km away) and slightly hurts for long-haul DX.
- **Some vertical polarization mixed in** — typically 10–20% by power. This makes it slightly more compatible with vertically polarized stations.

> ⚙️ **Advanced —** The current distribution along an inverted V is essentially the same as a dipole — sinusoidal, peaking at the feedpoint. What changes is the geometry of how that current vector projects into the far field. The drooping legs give a downward-pointing component that adds constructively in the zenith direction (good for NVIS) and destructively at low elevations (worse for DX). Modeling in NEC shows about a 0.5 dB elevation-pattern shift toward higher angles for a 120° apex, and 1.5 dB shift for a 90° apex.

## When to pick a V over a flat-top

- You only have one good support.
- You want to fit a full-size dipole into a small yard. The droop reduces the ground-projection footprint.
- You're doing **NVIS** — short-skip 80/40 m for regional work or emcomm. The higher takeoff angle is exactly what you want.
- You want a less directional antenna in the horizontal plane (the V is closer to omnidirectional than a flat-top).

## When to avoid

- **DX-only operation** at very low takeoff angles. A horizontally polarized antenna at high height beats a V every time for low-angle DX.
- **160 m and 80 m** in tight space. The high-voltage ends end up near the ground, which is lossy and slightly unsafe.

## Common mistakes

- **Apex angle too tight.** A 60° V looks dramatic and has the impedance of a tuna can. Keep the legs at 30° or less below horizontal.
- **Ends too close to the ground or to metal objects.** Both detune the antenna and waste power.
- **No choke balun.** Even worse on a V than on a flat-top.
- **Identical leg lengths but unequal heights at the ends.** This makes the antenna asymmetric — the two halves resonate slightly differently and you'll get common-mode current. If your two end supports are different heights, the wire still resonates fine, but the radiation pattern tilts toward the lower-end side.

## See also

- §05-01 — Dipole (the parent form)
- §05-12 — Choke balun (you really do need one)
- §05-13 — Ground-plane effects (height matters even more for a V)
- §05-15 — Reading the radiation pattern
- §08 — Antenna calculator
