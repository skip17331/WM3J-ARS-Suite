---
id: 06-06
title: Full-Wave Loops
chapter: 06
section: 06
level: mixed
status: draft
---

# Full-Wave Loops

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A **full-wave loop** is a closed wire about one wavelength in circumference, hung in any closed shape — square (quad), triangle (delta), or larger irregular shapes (skyloop). Unlike the small magnetic loop in §06-05, this loop is electrically *large*; its current and voltage distributions span a full wave around its perimeter. It radiates as a normal-sized antenna, not via near-field magnetic coupling.

Full-wave loops have two reputations among hams: **quiet on receive** and **slightly higher-gain than a dipole**. Both reputations are deserved, with caveats.

## The three common forms

| Form | Shape | Supports needed | Polarization (when fed at the bottom) |
|------|-------|-----------------|---------------------------------------|
| Quad | Square, hung corner-up or side-up | 2 (one each side) | Horizontal |
| Delta | Triangle, apex up or apex down | 1 (apex) + 1 ground anchor (or 2 ground anchors with apex up) | Mixed; depends on feed point |
| Skyloop | Large horizontal loop, supported at multiple corners | 3+ (one per corner) | Mostly horizontal, omnidirectional |

The quad and delta are usually single-band. The skyloop, fed with ladder line and a tuner, is a multi-band antenna.

## The headline numbers

| Property | Full-wave loop |
|----------|----------------|
| Total circumference | 1005 / f(MHz) feet (or 306 / f(MHz) meters) — slightly longer than a "physics" full wave because of velocity-factor effects |
| Feed impedance (free space) | ~120 Ω (square loop), 100 Ω (delta with apex up), 80 Ω (skyloop high above ground) |
| Match to 50 Ω coax | Use a 4:1 balun (120→30, then 1.4:1) — **or** use a quarter-wave 75 Ω matching section, **or** feed with ladder line and a tuner |
| Pattern | Bidirectional broadside to the plane of the loop (for quad/delta), omnidirectional + zenith null for skyloop |
| Gain over dipole | About 1.5–2 dBd at the same height (slightly more for quad than delta) |
| Bandwidth | About 4–6% (broader than a dipole because the loop is two parallel half-waves) |

## Cookbook lengths

For the full circumference (cut a few feet long, then trim down via SWR sweep at final height):

| Band | Loop circumference |
|------|--------------------|
| 80 m | 274 ft (83.6 m) |
| 40 m | 140 ft (42.7 m) |
| 20 m | 70 ft 7 in (21.5 m) |
| 15 m | 47 ft 4 in (14.4 m) |
| 10 m | 35 ft 3 in (10.8 m) |
| 6 m | 20 ft 0 in (6.10 m) |

For a square quad, divide circumference by 4 to get each side; for a delta, divide by 3.

## Quad loop

A square loop about 1 wavelength in circumference, hung in a vertical plane. A 20 m quad is about 17.6 ft on a side — manageable for two trees or a short mast.

- **Feed at the bottom corner** (or middle of the bottom side) for **horizontal polarization**.
- **Feed at one of the side midpoints** for **vertical polarization**.
- Pattern is broadside to the loop's plane (the directions you'd look *through* the loop).
- About **1.5 dBd gain** broadside, with a 6+ dB null edge-on.

The quad's reputation for being quieter than a dipole comes mostly from its **lower angle of takeoff at the same average height** and from the noise nulls being broadside to the antenna. Both are real effects.

## Delta loop

A triangular full-wave loop, popular because it only needs **one tall support and two ground anchors** (apex-up configuration) — a common backyard geometry. The 80 m delta is the canonical "stealth DX" antenna.

- **Apex up, fed at one of the lower corners**: mixed horizontal+vertical polarization, slight gain over a dipole on its primary band.
- **Apex up, fed in the middle of one of the sloping sides**: more vertical polarization, lower takeoff angle — better for DX.
- **Apex down, fed at the apex**: horizontal polarization, higher takeoff angle — better for short-skip.

The 80 m delta with 60-ft apex makes a respectable DX antenna where a full-size 80 m dipole won't fit; the diagonal layout makes better use of small lots.

## Skyloop

A horizontal loop, hung flat as a "ceiling" over your yard. Cut for the lowest band of interest (commonly 80 m: ~274 ft); fed with **ladder line and a tuner** for multi-band coverage.

- **Pattern**: omnidirectional in azimuth, **with a sharp null straight up**. This is the skyloop's signature: it's a poor NVIS antenna but a quiet long-haul antenna because the zenith null rejects the loud nearby stations and rain noise that come in at high angles.
- **Multi-band**: with ladder line and a balanced tuner, an 80 m skyloop tunes from 80 through 10 m. On harmonics it gets directional and develops sidelobes, but still works.
- **Quiet receive**: the noisy world is mostly above the antenna; the skyloop's null up there genuinely reduces noise levels by 1–3 S-units in many installs.

## Why loops are quieter on receive

Two real effects:

1. **Closed-loop antennas tend to reject low-impedance, near-field noise sources.** Power-line crackle, switching-supply hash, etc., couple into open-ended antennas (dipoles, verticals) more efficiently than into closed loops. A loop responds to a propagating wavefront; the local capacitive/inductive noise coupling is reduced.
2. **Pattern nulls.** A vertical-plane loop has nulls broadside to its edges; a skyloop has a zenith null. Your noise sources are *somewhere*, and the loop's null often happens to be aimed at them.

Both effects are real but not dramatic — typical loops are **1–3 dB quieter** than a same-band dipole on receive, sometimes more if there's a strong local noise source nicely placed in a null.

> **Advanced —** The "quiet loop" claim is sometimes mis-attributed. A loop's *radiation* characteristics vs. *near-field-coupling* characteristics are not the same. The reduced near-field noise pickup is a genuine consequence of the loop's input being at a low-impedance current node, while a dipole's feed point is at a higher-impedance current node — this changes how local-field noise voltages convert to terminal voltage. The pattern-null effect, by contrast, is just geometry. Both effects are typically a few dB; combined they can be quite useful, but a properly chosen Beverage receive antenna will outperform a TX loop for receive every time on 160/80 m.

## When to pick a full-wave loop

- You want slight gain over a dipole and have the space for two supports (quad or delta).
- You have noise problems and want a quieter receive antenna without dedicating a separate receive antenna.
- You have a yard you can ring with wire on three or four supports — skyloop time.
- You want one antenna that works on many bands (skyloop with tuner).

## When to avoid

- Tight space — a full-wave loop is *bigger* than a dipole in linear dimension.
- Single-tower install with no other supports.
- You want pure horizontal polarization at very low angles for DX — a dipole or beam at high height is better than a loop at moderate height for that mission.

## Common mistakes

- **Not putting the loop high enough.** A loop at 0.2 λ above ground works, but a loop at 0.5 λ works dramatically better — the takeoff-angle benefit needs height to develop.
- **Wrong impedance match.** 100–120 Ω feed impedance into 50 Ω coax = 2.5:1 SWR if you don't match it. Use a 4:1 balun, a 75 Ω matching section, or ladder line + tuner.
- **Skyloop with no balun on coax feed.** The skyloop is balanced; coax is not. Choke balun mandatory.
- **Forgetting the "tighter than a dipole" wire support requirement.** A delta loop's wire is under more tension than a dipole because gravity's pulling sideways on it; use stronger wire and stronger insulators.

## See also

- §06-01 — Dipoles (loops compared to)
- §06-12 — Baluns (the matching system)
- §06-15 — Pattern (loops have non-trivial elevation patterns)
- §15 — Noise (loops vs. other antennas)
- §06-13 — Height effects matter for loops too
