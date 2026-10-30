---
id: 06-31
title: Receive Antennas — Beverages, Loops & Flags
chapter: 06
section: 31
level: mixed
status: published
---

# Receive Antennas — Beverages, Loops & Flags

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

On the low bands the problem usually isn't getting heard — it's *hearing*. A big 160 m vertical radiates fine but also delivers a faceful of atmospheric noise and local QRM. A dedicated **receive antenna** throws away signal strength you don't need in exchange for the one thing that matters on receive: a better **signal-to-noise ratio**, whether by aiming a directional pattern at the DX or by steering a **null** onto the interference. Almost all of these have low or negative gain — that's fine; a preamp makes up the level, and what you're buying is *quiet* and *directional*.

## The small loop — sharp, steerable nulls

A small (magnetic) loop has a figure-eight pattern with **two deep, sharp nulls broadside to the plane of the loop** (perpendicular to its face). The nulls are far sharper than the broad maxima, which is the whole trick: **put the loop on a rotator and you can steer a null straight onto an interfering station or a local noise source and drop it 20–40 dB** — you null the unwanted signal rather than peaking the wanted one. This is exactly the mechanism behind radio direction finding and fox-hunting.

- It's the same antenna family as the transmit magnetic loop (§06-14) — here used at low power on receive purely for its nulls.
- A **shielded loop** rejects electric-field (local) noise and gives cleaner, deeper nulls.
- **180° ambiguity:** the figure-eight has *two* nulls 180° apart, so a bare loop can't tell front from back (a "sense" antenna resolves it for DF). For nulling QRM that ambiguity doesn't matter — you just rotate for minimum.

## Terminated loops — flags, pennants, EWE, K9AY

Add a **terminating resistor** to a small vertical loop and the bidirectional figure-eight collapses into a **cardioid** — unidirectional, with a deep null off the back. Flags, pennants, the EWE, and the **K9AY** are all variations: compact (a few metres), modest output, excellent front-to-back for their size. The K9AY and similar use two switchable loops to select among four directions from one small footprint — the small-lot answer to a Beverage.

## The Beverage

A **Beverage** is a long wire — one to several wavelengths — strung low (8–10 ft) and **terminated at the far end** into ground through ~400–600 Ω. The termination makes it a unidirectional traveling-wave antenna that listens *toward the terminated end*, with superb directivity and very low noise. It has large negative gain (a preamp is mandatory), but on 160/80 m its directivity and quiet are unmatched.

- Needs **space** — hundreds of feet per wire — and people run several in different compass directions, switched at the shack.
- A **Beverage-on-the-ground (BOG)** lays the wire on the dirt: shorter, lower output, dead simple, surprisingly effective for a small lot.
- Fed through a matching transformer (~9:1, the ~450 Ω wire to 50 Ω coax) at the feed end.

## Loop-on-ground (LoG)

A small loop lying flat **on the ground** — roughly omnidirectional, extremely quiet, and about the simplest low-noise RX antenna there is. Negligible output (needs a preamp), but a popular first RX antenna where there's no room for a Beverage.

## What makes or breaks every RX antenna

> **Advanced —** The feedline is the trap. A receive antenna's whole value is its low noise and clean pattern, and **common-mode current picked up on the coax shield** will dump shack/line noise straight into the receiver and fill in the nulls — the feedline becomes the dominant "antenna." Every RX antenna needs a good **isolation transformer / common-mode choke** at the feedpoint (§06-04), and benefits from a choke at the shack end too. This matters far more here than on a transmit antenna.

Other essentials:

- **Separate RX input or a T/R relay** — never transmit into a Beverage or preamp.
- **Decouple from the transmit antenna** — a resonant TX vertical nearby couples into the RX antenna and fills its nulls; detune or relay it out on receive.
- **Preamp, with care** — make up the low output, but watch for overload/IMD on a crowded low band.

## When to use

- 160/80/40 m DXing where you can hear noise but not the DX.
- A noisy QTH where steering a null onto the noise source is the only practical cure.

## When to avoid

- The high bands, where the TX antenna already hears well enough and S/N isn't the limit.
- No way to isolate the feedline — without it, an RX antenna underperforms a plain dipole.

## Common mistakes

- **No feedline isolation** — the number-one reason a receive antenna "doesn't work": the coax brings the noise back.
- **Skipping the preamp** then concluding the antenna is deaf — low output is by design.
- **Letting the TX antenna couple in** — fills the nulls you built the thing for.
- **Forgetting the Beverage termination** — an unterminated Beverage is bidirectional and loses its main advantage.

## See also

- §06-14 — Magnetic loops (the transmit cousin of the null-steering RX loop)
- §06-29 — Phased arrays & directivity (RX phased verticals / receive four-squares)
- §06-04 — Baluns and chokes (the all-important feedline isolation)
- §06-12 — Verticals (the transmit antenna these complement on the low bands)
- §15 — Noise sources (what you're trying to null out)
