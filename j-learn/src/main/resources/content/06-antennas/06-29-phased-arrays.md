---
id: 06-29
title: Phased Arrays & Directivity
chapter: 06
section: 29
level: mixed
status: draft
---

# Phased Arrays & Directivity

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

§06-27 combined antennas **in phase** for gain. Feed them **out of phase** instead and something more useful happens: the pattern develops a *direction*. Two verticals and a phasing network give you a steerable, unidirectional antenna with a deep null you can point at an interfering station — the basis of the two-element cardioid and the four-square, the workhorses of serious low-band DX.

This section is about **driven** arrays, where every element is fed. Getting directivity from *parasitic* (undriven) elements — the Yagi and quad — is a different mechanism, covered in §06-30 (Beams).

## How two elements make a direction

Each element radiates; in some directions the two waves arrive in step and add, in others they arrive opposed and cancel. Two knobs set where: the **physical spacing** (a propagation delay) and the **feed phase** (an electrical delay). Line them up so they reinforce one way and oppose the other, and you have a beam.

| Spacing | Feed phase | Pattern | Gain over one element |
|---------|-----------|---------|------------------------|
| ¼ λ (90°) | 90° out of phase | **Cardioid** — unidirectional, one deep back null | ~3 dB, F/B 20–30 dB |
| ½ λ (180°) | in phase (0°) | **Broadside** — bidirectional, max perpendicular to the line | ~4 dB |
| ½ λ (180°) | 180° out of phase | **End-fire** — bidirectional along the line | ~4 dB |

The cardioid is the prize: with ¼λ spacing and a 90° feed phase, the fields reinforce off one end and cancel off the other, putting a **null tens of dB deep** behind the array. Swap which element leads (a relay) and the pattern flips 180° — a switchable two-direction antenna.

## The four-square

Four verticals on the corners of a ¼λ-side square, fed through a phasing network that drives one element leading, two in quadrature, and one lagging, produces a **cardioid aimed at one corner**. Switch the phasing with relays and you steer it to any of four directions (often eight with the in-between combinations). Expect **~5 dB gain over a single vertical and 20–30 dB front-to-back** — which on 160/80 m receive is the difference between copying the DX and not. It's the standard competitive low-band array.

## The catch: mutual coupling

Here is why you can't just cut a 90° length of coax as a "phasing line" and call it done.

When you drive closely-spaced elements, the current in each one induces a voltage in its neighbors. So an element's **feedpoint impedance is not its self-impedance** — it shifts, the elements end up at *different* impedances, and a strongly-coupled element can even show **negative resistance** (it absorbs power from the array rather than radiating its share). A naive equal-length phasing line feeds current that depends on these shifted, unequal impedances, so the actual element currents — and thus the pattern — are nothing like the design intent. The famous symptom is a four-square with a mediocre 8 dB front-to-back instead of 25 dB.

> **Advanced —** The robust fix is **current forcing**. A feedline that is an odd multiple of a quarter-wavelength is an impedance inverter: it makes the element **current** proportional to the **voltage at the line's input**, independent of the element's (coupling-shifted) feedpoint impedance. Feed every element through equal-length odd-¼λ lines and you control the current ratio and phase by controlling the input voltages — exactly what the design assumes. This is the foundation under the practical feed systems below.

## Feed methods (the practical menu)

- **Simple / "Wilson" λ/4 phasing line** — the naive single-quarter-wave-of-coax approach. Cheap, and *wrong* in the presence of mutual coupling; gives degraded F/B. Fine only when you understand its limits.
- **Christman method** — choose feedline lengths so the elements' transformed impedances come out *equal* at the junction, then simply parallel them. No network, low loss; the phase is fixed by the geometry. A favorite for fixed two-element arrays.
- **Lewallen (W7EL) method** — current-forcing odd-¼λ lines plus an **L-network** to set the exact phase. Robust and adjustable; the standard homebrew approach.
- **Collins / hybrid-coupler (Comtek)** — a **90° quadrature hybrid** splits power into precise equal amplitudes and a clean 90° phase *regardless of load mismatch*, dumping the imbalance into a resistor. Easiest to get right and the basis of most commercial four-square controllers; the cost is some power lost in the dump resistor.

## Practical reality

- **It's an array of verticals** — every element needs its own radial field (§06-12, §06-05). Skimp on radials and the pattern collapses; the array is only as good as its grounds.
- **Narrowband.** The deep F/B null is sharp and frequency-sensitive — designed for a slice of a band, not the whole thing.
- **Element matching matters.** Elements must be identical and identically installed; mismatched feedpoint impedances ruin the current balance.
- **Receiving arrays** (small phased verticals, receive four-squares) use the same phasing ideas at low power for a quiet directional RX pattern. Dedicated receive antennas — Beverages, loops-on-ground, flags/pennants — are covered in §06-31.

## When to use

- Low-band (160/80/40 m) DX where you need gain **and** a steerable null on interference/noise.
- You have the real estate for multiple verticals and their radial fields.
- Contest or DX station wanting switchable directions without a rotator.

## When to avoid

- No room for multiple elements + radials → a single vertical or an inverted-L (§06-25) is the realistic choice.
- You want broadband, casual operation — the narrowband, install-sensitive array is overkill.
- You're after a rotatable beam on the high bands — that's a Yagi/parasitic array (§06-30), not a driven phased array.

## Common mistakes

- **A bare λ/4 coax "phasing line"** with no regard for mutual coupling — the classic cause of disappointing front-to-back.
- **Unequal or non-current-forcing feedlines** — element currents drift from design and the null fills in.
- **Under-built radial systems** — it's still verticals; ground losses dominate and unequal grounds unbalance the array.
- **No common-mode chokes** on the feedlines — stray current re-radiates and wrecks the pattern (§06-04).
- **Expecting the textbook null across a whole band** — retune/redesign per target frequency slice.

## See also

- §06-27 — Phasing harnesses & stacking (the in-phase case; same hardware, no offset)
- §06-30 — Beams (parasitic directivity — the undriven-element alternative)
- §06-31 — Receive antennas (RX-side directivity and null-steering)
- §06-12 — Verticals (the usual array element; radials apply per element)
- §06-07 — Radiation patterns (reading the cardioid, the null, the F/B)
- §06-03 — Impedance transformation (the ¼λ line as the current-forcing inverter)
- §06-06 — Modeling concepts (model the coupled array before you build it)
