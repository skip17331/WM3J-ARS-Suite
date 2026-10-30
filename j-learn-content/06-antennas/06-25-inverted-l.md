---
id: 06-25
title: Inverted-L
chapter: 06
section: 25
level: mixed
status: published
---

# Inverted-L

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The inverted-L is the classic answer to "how do I work DX on 160 and 80 m without a 130-foot tower?" It's an end-fed wire that goes **up** for as much height as you can manage, then bends and runs **horizontal** to a far support — the shape of an upside-down L. The vertical section does the low-angle DX radiating; the horizontal section is top-loading that brings the whole thing to resonance without needing a full quarter-wave of vertical height. It is, in effect, a bent and top-loaded vertical, and it lives or dies on its ground system.

## The geometry

| Section | Job |
|---------|-----|
| Vertical run | The real radiator — radiates at low angles for DX. **More vertical height = more DX.** |
| Horizontal top | Top-loading — replaces the vertical height you couldn't get, sets resonance |
| Ground / radials | The return path — this is a monopole, so the ground system *is* half the antenna |

Two common electrical lengths:

- **Quarter-wave (resonant)** — total wire ≈ 234 / f(MHz) feet. Feed impedance ~30–40 Ω against a good radial field; feed directly (SWR ~1.3:1) or trim with a small L-network. A 160 m quarter-wave L is ~125–135 ft total — e.g. 50–60 ft vertical, the rest horizontal.
- **Half-wave-ish (high-Z end-fed)** — total wire ≈ 3/8 to 1/2 λ. The feedpoint impedance is high, so it's less sensitive to ground losses and gives even better low-angle gain — popular as a budget DX wire and as a "poor man's vertical array element."

## Cookbook (quarter-wave, resonant)

| Band | Total wire (≈) | Example split |
|------|----------------|----------------|
| 160 m (1.83 MHz) | 128 ft (39 m) | 55 ft up / 73 ft across |
| 80 m (3.6 MHz) | 65 ft (20 m) | 35 ft up / 30 ft across |
| 40 m (7.1 MHz) | 33 ft (10 m) | 20 ft up / 13 ft across |

Maximize the **vertical** portion within your support height; let the horizontal portion make up the rest. Trim the horizontal end for resonance — it's the easiest part to reach.

## The ground system is the antenna

This is a vertical monopole, so the same rule as §06-12 applies, only more so on the low bands where losses dominate:

- **Radials, lots of them.** 16 is a working minimum; 32–60 is better on 160 m. They don't need to be resonant — many shorter radials beat a few long ones.
- On-ground or buried radials are fine; elevated radials (a few, tuned) also work and can cut the count.
- A poor ground turns DX power into dirt-heating. The difference between 4 radials and 32 on 160 m can be several S-units to the DX station.

> **Advanced —** The inverted-L's low takeoff angle comes from the vertical section's current, which peaks at the feed (current maximum) for the quarter-wave case. The horizontal top carries mostly the high-voltage / low-current end, so it contributes little radiation and largely cancels its own horizontal component above good ground — that's *why* the L stays a low-angle radiator instead of turning into a high-angle horizontal wire. Push the current maximum higher (go to the 3/8–1/2 λ version, which moves the current node up the vertical section) and the low-angle gain improves further, at the cost of needing a matching network for the high feed impedance. See §06-05 and §06-07.

## When to pick an inverted-L

- **160 / 80 m DX** with one tall-ish support (a tree, a tower you can hang off the side of).
- You can lay a radial field but can't put up a full vertical or a low-band dipole at useful height.
- Field/contest low-band antenna that goes up fast.

## When to avoid

- You can't lay radials (rooftop, rock, tiny lot) — its efficiency collapses without a ground system.
- You only care about high bands — a dipole or vertical is simpler there.

## Common mistakes

- **Too few radials.** The number-one inverted-L disappointment — it's a vertical, and verticals need ground.
- **Too little vertical height.** A short vertical section with a long horizontal top becomes a high-angle cloud-warmer, not a DX antenna. Get the wire *up* first.
- **Confusing it with an EFHW mounted as an "inverted-L."** That's a resonant half-wave with a 49:1 unun and minimal ground (§06-13); this is a ground-dependent quarter-wave monopole. Different feed, different ground needs.
- **No feedline choke** — common-mode current on an end-fed-against-ground wire is real (§06-04).

## See also

- §06-12 — Verticals (the inverted-L is a bent, top-loaded one)
- §06-05 — Ground-plane effects (the radial field decides performance)
- §06-13 — EFHW (the resonant end-fed cousin — different feed/ground)
- §06-23 — Sloper / half-sloper (the other one-support low-band wire)
- §06-07 — Radiation patterns (seeing the low-angle lobe)
