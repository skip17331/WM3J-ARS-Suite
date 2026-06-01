---
id: 06-24
title: Random-Wire and Non-Resonant Long Wire
chapter: 06
section: 24
level: mixed
status: draft
---

# Random-Wire and Non-Resonant Long Wire

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A random-wire antenna is the simplest HF antenna that exists: one wire of convenient (non-resonant) length, fed at the end against a counterpoise through a **9:1 unun**, with a tuner finishing the match. It is the antenna you put up when nothing else fits — an attic, a tree, a balcony rail. It's not efficient or quiet, but it gets you on the air on every band a tuner will reach.

First, a terminology cleanup the rest of this section depends on:

- **Random wire** — a *short*, non-resonant end-fed wire (typically well under one wavelength on the bands used). Needs a tuner; radiates roughly like a vertical or sloped wire. This section.
- **EFHW** — a wire cut to a *resonant* half-wave, fed with a **49:1** transformer, no tuner. Different beast — see §06-13.
- **True long wire** — an end-fed wire **one wavelength or longer**, which develops multi-lobe gain that tilts toward the wire's axis as frequency rises. The rhombic and V-beam are its high-gain descendants — see §06-16.

A 40 ft wire is *not* a "long wire." It's a random wire. The distinction matters because only the genuine ≥1 λ wire has the gain lobes people imagine when they say "long wire."

## The 9:1 unun and why this length, not that one

A short end-fed wire presents a feed impedance that wanders over HF but tends to land in the **few-hundred to ~1500 Ω** range — except at lengths that are a half-wave multiple, where it spikes into the thousands. A **9:1 unun** divides that by nine, dropping it into a band most tuners can match, and gives the coax shield something defined to work against.

So you choose a length that **avoids half-wave resonance on the bands you want.** Lengths that dodge the spikes across most of HF:

| Recommended random-wire lengths (ft) |
|----------------------------------------|
| 29, 35.5, 41, 58, 71, 84, 107, 119, 148 |

These are chosen so the wire is never an exact ½ λ multiple on the ham bands. Pick one that fits your yard.

## The counterpoise is not optional

End-fed against "nothing" means the feedline shield and the shack become the return path — which is how a random wire earns its reputation for RFI. Give it a real return:

- At least **one counterpoise wire**, a quarter-wave on the lowest band (~33 ft on 40 m), laid on the ground or run opposite the antenna. More radials are better.
- A **common-mode choke** in the coax a few feet below the unun keeps the rest of the feedline out of the antenna.

> **Advanced —** The 9:1 ratio is a compromise, not a match — it's chosen empirically because the *geometric mean* of a short end-fed wire's impedance excursion across HF sits near 450 Ω (9 × 50). On any single band the actual transform is wrong, which is why a tuner is mandatory; the unun's real job is to keep the tuner's task inside its matching range and to present the coax with a stable-ish load. Counterpoise length and quantity shift the whole impedance picture, so adding radials changes what the tuner has to do — tune *after* the ground system is final.

## Realistic expectations

- **Always needs a tuner.** The 9:1 only brings the impedance into range.
- **Efficiency varies wildly by band** and by counterpoise quality — some bands are great, some are dogs.
- **Noisy on receive** relative to a resonant antenna; it picks up local QRN well.
- But: it's cheap, fast, hideable, and works on every band the tuner will load.

## When to pick a random wire

- Apartment, attic, HOA, or emergency — you need *an* antenna and resonance is a luxury.
- Portable/field where you'll throw a wire in a tree and run a tuner anyway.
- A first HF antenna to get on the air while you build something better.

## When to avoid

- You can fit a resonant antenna — an EFHW (§06-13) or dipole (§06-10) will be quieter and more efficient.
- You can't lay any counterpoise — the random wire's worst case (RF in the shack, poor efficiency) gets worse.

## Common mistakes

- **No counterpoise.** The single biggest cause of random-wire RFI and poor performance.
- **A half-wave-multiple length** on a wanted band — the 9:1 sees a few thousand ohms and the tuner gives up.
- **Calling it a "long wire."** Unless it's ≥1 λ, it has no gain lobes — it's a short fed wire.
- **Skipping the feedline choke** — the coax becomes part of the antenna.

## See also

- §06-13 — EFHW (the resonant, no-tuner end-fed — don't confuse them)
- §06-16 — Rhombic (where true long wires lead)
- §06-25 — Inverted-L (a random/resonant wire bent for low-band DX)
- §06-03 — Impedance transformation (the 9:1 and the tuner)
- §06-04 — Baluns and chokes
