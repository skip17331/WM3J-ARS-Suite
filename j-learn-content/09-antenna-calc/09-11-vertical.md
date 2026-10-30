---
id: 09-11
title: Vertical Antennas
chapter: 09
section: 11
level: mixed
status: published
---

# Vertical Antennas

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What it is

A vertical antenna is a single vertical wire or tube fed against a **ground system** of buried or elevated radials. The simplest is the **quarter-wave (¼λ) vertical**: a wire one-fourth of a wavelength tall, with the rig connected at the bottom and the ground side connected to the radial system.

Verticals offer:

- **Low takeoff angle** — good for DX (the radial-system efficiency permitting)
- **Omnidirectional** azimuth pattern
- **Vertical polarization** — useful for matching repeaters; less suited to most ham HF work (which is horizontally polarized)
- **Smaller footprint** than a horizontal dipole — but the radial field still needs space

Variants include the **half-wave (½λ)** vertical (no radials needed, end-fed), the **5/8-wave (⅝λ)** vertical (slightly more gain, low takeoff), and the **trapped vertical** (multi-band, like Hustler / Hy-Gain commercial verticals).

## How it works

A quarter-wave vertical exploits the **image-plane** principle: a horizontal ground plane below the vertical creates an "image" of the antenna below ground, electrically. The vertical-plus-image acts as a half-wave dipole, but mounted vertically. Without the ground plane (or radial substitute), there's no image — the antenna doesn't work.

The vertical's length is empirically **234 / f(MHz)** in feet — same end-effect correction as a half-dipole leg. The radials (or a metal ground plane) provide the "other half" of the antenna's RF current path.

A **buried radial system** acts as a low-loss virtual ground. As a rule of thumb:

| Radials | Estimated ground-system loss |
|--------:|-----------------------------:|
| 4 | ~5 dB |
| 8 | ~3 dB |
| 16 | ~2 dB |
| 32 | ~1 dB |
| 64+ | < 0.5 dB |

So 4 radials over poor soil = vertical is **5 dB worse** than a horizontal dipole at typical heights. 32 radials over decent soil = comparable performance, plus the vertical's lower-angle pattern.

Feed impedance over a perfect ground is ~36 Ω. Real ground losses raise it; raised radials lower it back to ~50 Ω.

## Calculator inputs and outputs

The Antenna Workshop calculator (`Vertical Antenna (¼λ)`) takes:

- **Frequency** (MHz)
- **Number of radials** installed

And returns:

- Vertical element length
- Each radial length (¼λ)
- Total radial wire needed
- Estimated ground-system loss for that radial count
- Expected feed Z and match recommendation

## Worked example — 20m vertical with 16 radials

```
freq = 14.150 MHz
radials = 16

vertical length: 234 / 14.150 = 16.54 ft
radial length: ¼λ = 16.54 ft each

total wire: 16 × 16.54 = 264.6 ft of radial wire
estimated loss: ~1 dB

feed Z: ~40 Ω over 16 radials → 1:1 balun directly to 50 Ω coax
```

A 20m vertical is **16.5 ft tall** plus a **16 × 16.5 = 264 ft buried radial field** spreading from its base. This is why verticals "need a yard" even though they're short.

## Common mistakes

- **Too few radials.** Below 16 radials, ground-system loss dominates. A vertical with 4 radials performs **6 dB worse** than the same vertical with 32 radials — a real signal-strength difference.
- **Feeding without a balun.** Direct coax-to-vertical-base feed lets common-mode current flow on the coax shield. A 1:1 balun at the base prevents this.
- **Radial trimming.** Radials don't need to be exactly ¼λ — any length helps. Cut to fit your yard. **Longer is always better** up to ~½λ.
- **Mounting too close to a metallic structure** (chimney, gutter, gas line). Couples energy and detunes.
- **Using insulated wire for radials.** Bare or stranded uninsulated wire is preferred — insulation slightly degrades radiation but mostly it complicates termination at the base.
- **Electrolytic decay.** Buried radials in salty / wet soil corrode in 5–10 years; check resistance to ground annually.

> **Advanced —** A 5/8-wave (⅝λ) vertical has slightly more gain than a ¼λ — about 3 dB at low takeoff angles — at the cost of a more complex matching network (since 5/8-wave is not naturally 50 Ω). The 5/8 is popular for VHF/UHF mobile installs because it's easier to mount than a ½λ vertical at 144 MHz. The half-wave (½λ) **center-fed** vertical (sometimes called "Coaxial vertical" or "Sleeve dipole") is a fully-radial-free design that uses the coax shield as the lower half — a different tradeoff than the ¼λ with radials.

## Build & trim notes

1. **Lay radials first.** Plan the radial field layout before raising the vertical — easier to bury wires when there's no antenna in the way.
2. **Anchor the radials at the base** to a brass ring or copper bus-bar; bond the bus-bar to the coax shield.
3. **Raise the vertical**, install a 1:1 balun at the base, connect to coax.
4. **Sweep with a NanoVNA**; trim the vertical for resonance at the operating frequency.
5. **Verify lowest possible SWR** — 1.2–1.5:1 with a real ground-system; under 2:1 with marginal radial fields.
6. **Re-check resonance seasonally** — wet/dry soil shifts ground losses and resonance subtly.

## See also

- §06-12 — Verticals (theory chapter)
- §09-00 — Antenna Workshop overview
- §09-15 — NanoVNA Trim Workflow
- §17-06 — Wavelength
- §17-09 — Feedline Loss
