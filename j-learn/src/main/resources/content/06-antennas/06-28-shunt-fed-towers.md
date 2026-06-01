---
id: 06-28
title: Shunt-Fed Towers
chapter: 06
section: 28
level: mixed
status: published
---

# Shunt-Fed Towers

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

If you already have a guyed tower with a beam on top, you own most of a serious low-band vertical and don't know it. The tower is a tall conductor; the beam and rotator on top act as a capacity hat. Feed the whole structure as a vertical monopole and a 50–70 ft tower becomes a real 160 / 80 / 40 m antenna for the cost of some wire, a weatherproof box, and a radial field. The catch is *how* you feed it — the tower is grounded, so you can't just hook coax to the base.

## Why shunt feed, not base feed

Your tower is **DC-grounded** on purpose: lightning protection, structural bonding, and code. To base-feed it as a vertical you'd have to sit it on a base insulator and carry the whole guyed, beam-loaded structure on that insulator — expensive and mechanically nasty.

A **shunt feed** sidesteps all of that. It keeps the tower grounded at DC and couples RF onto it through a wire run alongside the tower. The grounded tower then radiates as a (top-loaded) quarter-wave vertical against a radial field — no base insulator, no structural change.

## The gamma / omega shunt

- A vertical or slightly slanted **shunt wire** (or aluminum rod) runs parallel to the tower, a foot or two off it, **bonded to the tower at a tap point** partway up, and brought down to a feedpoint at the base.
- The shunt-plus-tower forms a matching section. The feedpoint looks like a resistance in series with an inductive reactance; a **series variable capacitor** at the base tunes out the reactance and lands you on 50 Ω. That arrangement is a **gamma match** applied to the entire tower.
- When the gamma alone can't reach 50 Ω, an **omega match** adds a second (shunt) capacitor for more range.
- **Tap height** and **shunt-to-tower spacing** set the resistive part; the **capacitor** cancels the reactance. You adjust tap and cap iteratively for 1:1.

## The folded unipole

The modern, more forgiving version replaces the single gamma wire with a **skirt of several wires** spaced around the tower, all bonded to it **at the top** and brought down to a common feedpoint ring at the base, fed through a transformer. The folded unipole is **broader-banded** than a bare gamma, often needs no series capacitor, and is what most commercial "tower-feed" kits implement.

## The beam on top is a feature

The Yagi/tribander and rotator at the top act as a **capacity hat** — top loading that lowers the tower's resonant frequency. A 60 ft tower is only about half of a 160 m quarter-wave (~130 ft), so without loading it would be far too short; the beam on top does a lot of that work for free. The beam keeps working normally on its own bands — the shunt feed is a separate low-band RF path on the same structure.

## Radials are not optional

A shunt-fed tower is a **ground-mounted monopole**, so the rules of §06-12 (Verticals) and §06-05 (Ground-plane effects) apply in full: ground losses dominate low-band efficiency, and the answer is a **radial field**. Lay as many radials as you can. The tower's ground rods and guy anchors are for lightning and bonding — they are *not* an RF ground substitute.

> **Advanced —** On 160 m a typical 50–70 ft tower sits well below a quarter-wave, so the beam's top loading and the shunt's series reactance carry the resonance — expect a **narrow 2:1 bandwidth** (often only tens of kHz on 160). Practical starting points: tap roughly ¼ to ⅓ of the way up, shunt spacing 12–30 in. Set tap and spacing for ~50 Ω resistive at resonance, then null the reactance with the capacitor. The guys, the coax running down the tower, and the beam all couple into the system, so the final numbers are install-specific — model it in EZNEC (§06-06) or tune empirically with an analyzer in the *final* configuration, the same way the half-sloper (§06-23) is an empirical antenna.

## When to use

- You have a grounded, guyed tower with a beam and want 160 / 80 / 40 m without putting up a separate vertical.
- You can lay a radial field at the base.

## When to avoid

- **No radial space** — efficiency collapses, as with any vertical.
- **Crank-up or tilt-over towers**, or towers with non-conductive sections, complicate the RF path.
- You need wide 160 m coverage — a short top-loaded tower is inherently narrow; plan to retune across the band (or use a folded unipole for a little more bandwidth).

## Common mistakes

- **No radials.** The number-one shunt-fed-tower disappointment — it's a vertical, and the ground system *is* the antenna.
- **Trying to insulate and base-feed it.** The tower is grounded; the shunt is the entire point. Leave the base bonded.
- **Tuning before everything's in place.** The beam, rotator, guys, and tower coax all couple in — match in the final configuration or you'll redo it.
- **No common-mode choke** at the feedpoint — station coax becomes part of the antenna (§06-04).
- **Resonant guy wires.** Break long guys with insulators so they don't detune or absorb.

## See also

- §06-12 — Verticals (the tower is one, top-loaded by the beam)
- §06-23 — Sloper / half-sloper (the other grounded-tower low-band antenna)
- §06-05 — Ground-plane effects (radials decide efficiency)
- §06-03 — Impedance transformation (the gamma / omega match)
- §06-06 — Modeling concepts (model the loaded tower before you climb)
