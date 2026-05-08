---
id: 04-03
title: Verticals
chapter: 04
section: 03
level: mixed
status: draft
---

# Verticals

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A vertical antenna is **omnidirectional in azimuth** and radiates at **low elevation angles** when properly installed. That two-sentence summary is why the vertical is the DX antenna of choice for operators who can't put up a tower, and the antenna of frustration for operators who don't understand its ground-system requirements.

The vertical can be the best HF antenna in your yard. It can also be a heater for the dirt under it. The difference comes down to one thing: the **ground system**.

## The vertical family

| Type | Length | Radials needed | Notes |
|------|--------|----------------|-------|
| Quarter-wave (¼ λ) | λ/4 | Many (16–60+) | Classic; fed at the base against radials |
| Half-wave (½ λ) | λ/2 | Few or none | High base impedance; fed via tuner or matching network |
| 5/8-wave | 5λ/8 | Many | Slight gain over ¼-wave; popular for VHF mobile |
| Ground-plane (GP) | λ/4 | 3–4 elevated | Same as ¼ wave but with elevated radials |
| End-fed verticals (no-radial) | λ/2 | None claimed | Marketing more than physics — see notes below |
| Loaded short verticals | < λ/4 | Many | Compromised efficiency; mobile and apartment use |

## Quarter-wave vertical: the canonical case

A vertical conductor exactly a quarter wavelength tall, fed at the base against ground, has:

- **Free-space-like feed impedance** of about **36 Ω** (over a perfect ground plane). That's a 1.4:1 SWR into 50 Ω coax — usable directly.
- **Pattern**: omnidirectional in azimuth; vertically polarized; low takeoff angle (peak around 18–25° elevation over real ground).
- **Length**: 234 / f(MHz) feet, or 71.4 / f(MHz) meters.

| Band | ¼-wave length |
|------|---------------|
| 80 m | 65 ft 7 in (20.0 m) |
| 40 m | 32 ft 9 in (9.99 m) |
| 30 m | 23 ft 1 in (7.05 m) |
| 20 m | 16 ft 6 in (5.03 m) |
| 17 m | 12 ft 11 in (3.94 m) |
| 15 m | 11 ft 0 in (3.36 m) |
| 12 m | 9 ft 5 in (2.86 m) |
| 10 m | 8 ft 2 in (2.50 m) |

## The radial system — the part everyone gets wrong

A vertical needs the **other half** of itself somewhere. That's what radials are. Without them, you don't have a quarter-wave radiator over a ground plane — you have a quarter-wave radiator with the ground itself acting as the return path, and the ground is a *terrible* conductor at HF.

**With no radials**, expect 3–6 dB loss compared to a properly-installed vertical. That's not a typo. Ground losses dump real power into warming the soil.

### Radial cookbook

- **Ground-mounted vertical**: lay 16–32 radials on the ground (or buried 1–2 inches deep) **a quarter-wavelength long each**. More is better up to 60 or so. Length is somewhat flexible — radials shorter than ¼ λ still help, just less. The classic ARRL study (Sevick, 1971) and N6LF's modern updates both find the **biggest improvement comes between 0 and 16 radials**; from there the curve flattens. **A vertical with 4 ground radials wastes maybe 40% of your power. With 16 radials, maybe 15%. With 60, under 5%.**
- **Elevated ground-plane**: just **3–4 radials** at ¼ λ each, lifted 8–10 ft off the ground, are enough. These act as a defined counterpoise; ground losses below them are dramatically reduced. **An elevated GP with 4 radials beats a ground-mounted vertical with 16 radials**, in most installs. This is one of the genuinely under-appreciated facts in amateur antennas.

### Wire size and material

Bare copper, #14 to #18 AWG, is fine. Insulated wire works too, with a slight detuning effect. **Bury it shallow** (1–2 inches) so the lawnmower doesn't find it, but don't bury it deep — surface conductivity is what matters, not depth.

> ⚙️ **Advanced —** A radial system replaces lossy earth in the *near field* of the vertical. The near-field zone roughly extends out to about 0.3–0.5 λ from the antenna; this is where displacement current density is high enough that earth losses dominate. Radials only need to cover that zone; there's no benefit to making them longer than ¼ λ for a single-band vertical, and there's diminishing return past 32 of them. The Brown/Lewis/Epstein 1937 paper is the foundational reference; 113 radials at 0.4 λ each is the canonical broadcast-quality number for an MF tower (530 kHz–1.6 MHz), but at HF and especially with elevated radials, the requirements are much more relaxed.

## Half-wave vertical: the no-radial trick

A half-wavelength vertical conductor fed at the base has a **very high feed impedance** (typically 1500–4500 Ω depending on conductor diameter and ground proximity). With an impedance-matching network at the base — typically a 49:1 or 64:1 unun — you can match it to 50 Ω coax.

The reason this is interesting: with the feedpoint at a high-impedance, **low-current** point, currents in the ground/radials are small, so radial losses are small. **A half-wave end-fed vertical genuinely doesn't need many radials.** A counterpoise (1 or 2 short wires, ~0.05 λ) is enough.

This is the "no-radial vertical" most commercial verticals (DX Commander, MyAntennas, Wolf River) use. They're not lying — but understand that the matching network is doing real work and has its own loss budget (typically 0.5–1 dB).

## 5/8-wave vertical

Slightly longer than a quarter wave: 0.625 λ. Provides ~3 dB gain over a ¼-wave at the *same* low takeoff angle, but is harder to feed (impedance is high and reactive). Mainly seen in VHF/UHF (the classic 5/8-wave whip on a mag-mount), where its physical size is manageable. Rarely used on HF except in short-shortened forms.

## Multiband verticals

Three approaches:

1. **Trap verticals** (Hustler 4/5/6BTV, Cushcraft R-series): one radiator with traps that act as electrical break-points per band. Convenient, slightly compromised efficiency. Each trap loses 0.3–1 dB.
2. **Parallel verticals on one ground system**: separate ¼-wave wires for each band, all sharing the same radial field, with separate or auto-coupled feeds (a "fan vertical"). Higher efficiency, more wires.
3. **Single radiator + tuner**: a long wire (e.g., 43 ft for the so-called "43-ft vertical") fed against a radial system with a remote tuner at the base. Multi-band but only as good as the tuner's match efficiency.

## Pattern characteristics

A vertical's vertical-plane pattern is a "doughnut on its side":

- **Maximum radiation at low elevation angles** — typically 18–28° over real ground. This is the big deal for DX: a vertical's typical radiation peak sits where DX paths usually arrive.
- **Null straight up** — verticals are bad at NVIS. If you want to talk to a guy 100 km away on 80 m, you want a horizontal antenna.
- **Omnidirectional in azimuth** — same gain in every horizontal direction.

Be aware: ground losses *flatten* the pattern in unhelpful ways at low angles. A vertical over poor (dry, sandy, rocky) ground can show 5–6 dB lower gain at 5° elevation than the same vertical over salt water.

## When to pick a vertical

- You have no tall trees / supports.
- You want one antenna, low height, omnidirectional, low-angle DX. (This is the textbook case.)
- You operate in HOA-restricted areas where a small ground-mounted vertical can pass as a flagpole.
- You're at the coast or near a lake — vertical efficiency improves dramatically over highly conductive ground.

## When to avoid

- You don't have space for radials. A vertical without a ground system is a heating element.
- Dry, rocky, or sandy soil. Conductivity matters more for verticals than horizontal antennas, and you cannot fully fix it with radials alone.
- You operate mostly short-skip / NVIS / regional. A horizontal dipole is better.
- High-noise environment. Verticals pick up vertically-polarized man-made noise (power lines, switching supplies) more readily than horizontals do. See §13.

## Common mistakes

- **One or two radials.** This is barely better than no radials. Lay 8 or more, and don't stop until you've got 16 if you can.
- **Radials of random lengths.** ¼-wave is the design number. Some shorter is OK; matching them all to the band of interest is best.
- **Treating "no-radial" advertising as physics.** End-fed half-wave verticals genuinely need fewer radials than ¼-wave verticals, but a single counterpoise wire is still a good idea.
- **Putting a ground-mounted vertical near a lossy object.** Buried metal pipes, chain-link fences, salt-treated soil with rebar, all change the ground impedance. Move 30 ft if you can.

## See also

- §04-13 — Ground effects (verticals depend on ground more than any other antenna)
- §04-15 — Reading the elevation pattern of a vertical
- §04-12 — Baluns and chokes (you still want one)
- §13 — Noise sources (verticals pick up noise differently)
- §07 — Antenna calculator
