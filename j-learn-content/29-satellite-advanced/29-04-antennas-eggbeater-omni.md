---
id: 29-04
title: Eggbeater Omnidirectional Antennas
chapter: 29
section: 04
level: mixed
status: published
---

# Eggbeater Omnidirectional Antennas

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

An **eggbeater** is an omnidirectional satellite antenna — no pointing, no rotor, no tracking. It's named for its shape: two perpendicular loops arranged so they look like the whisks of a kitchen eggbeater. The radiation pattern is roughly hemispherical, with circular polarization (RHCP for most amateur sat designs), and gain in the 2-4 dBic range over a wide elevation arc.

This is the antenna for the operator who wants to work FM birds from home without dedicating a corner of the yard to a rotor and Yagi. It mounts on a vertical mast, points straight up, and the rest is up to the satellite.

## The basic design

A classic eggbeater consists of:

- **Two full-wavelength loops** at the operating frequency, oriented vertically and perpendicular to each other (forming a "+" when viewed from above).
- **A 90° phasing harness** that drives one loop with a quarter-wavelength delay relative to the other, producing circular polarization.
- **A horizontal reflector** (a wire screen or a single loop) about a quarter wavelength below the driven loops, which tilts the pattern upward and improves gain at higher elevations.
- **A vertical mast mount** at the bottom, supporting the assembly.

The vertical perpendicular-loop arrangement gives near-omnidirectional coverage in azimuth — the antenna doesn't have a "front" or "back." Coverage in elevation is broad: usable from about 10° to 80° elevation, peaking somewhere around 30-45°. There's a null at the zenith (directly overhead) because of how the loops interact, and a soft rolloff toward the horizon where the reflector and ground effects degrade the pattern.

## The M2 EB-144 and EB-432

The dominant commercial eggbeater is the **M2 Antenna Systems** family — the **EB-144** for 2 m (144-148 MHz) and the **EB-432** for 70 cm (430-440 MHz). M2 is a well-known antenna manufacturer in California, primarily for the weak-signal VHF/UHF community.

Specifications (typical):

| Model | Band | Gain (zenith) | Gain (30° elev) | Power | Connector |
|-------|------|---------------|------------------|-------|-----------|
| EB-144 | 2 m | -3 dBic (null) | +4 dBic | 200 W | N female |
| EB-432 | 70 cm | -2 dBic (null) | +4 dBic | 200 W | N female |

The "dBic" is dB relative to isotropic circular — meaning gain compared to an ideal isotropic radiator with matched circular polarization. The gain figures are best taken as approximate; the real benefit of the eggbeater is the pattern *shape* (broad hemispherical coverage), not the peak gain.

Physically, an EB-144 is about 24 inches square (the reflector screen) with the loops projecting up another 24 inches. The EB-432 is roughly half-size everywhere because of the higher frequency. Both mount on a 1.25-1.5 inch vertical mast at the bottom.

A stacked-band installation pairs the EB-144 and EB-432 on the same mast, one above the other, with about 4 feet of separation to minimize cross-band coupling. Two feedlines (one per band) go down the mast to the shack.

## When omni is enough

The eggbeater works for:

- **FM birds.** Most FM satellites have enough downlink power that even a few-dB-gain omni antenna gets you through clean, especially for the high-elevation portion of the pass. SO-50, AO-91, AO-92, the various TEVEL CubeSats — all workable on an eggbeater.
- **ISS voice and packet.** The ISS downlink is loud (relatively — it's still 5-25 W from low orbit), and an eggbeater gets you usable signal across the entire pass.
- **Listening on linear birds.** You can monitor RS-44 or FO-29 on an eggbeater — you won't have a great signal, but you'll hear the beacon and louder QSOs across the passband. For transmitting, you'll struggle (see below).
- **Set-and-forget operating.** You're at your desk, the bird passes overhead, you make a contact in 2 minutes, you go back to whatever else you were doing. No setup time, no aiming, no tracking software required.

The eggbeater does NOT work for:

- **Working linear birds at low TX power.** The lack of forward gain means you need more uplink power to overcome the path loss, and that's exactly what you shouldn't do on a linear bird (§29-02). An eggbeater + linear bird is a setup for becoming an alligator unintentionally.
- **Weak DX signals near the horizon.** The eggbeater's gain drops fast below 10° elevation; you'll lose AOS and LOS portions of passes that a Yagi would catch.
- **EME or any deep-space work.** Far too little gain.
- **23 cm / 13 cm satellite work.** No equivalent omni exists with usable gain at those frequencies; weak-signal microwave needs a dish or helical.

## Installation tips

The eggbeater is electrically sensitive to nearby metal — more so than most antennas because the symmetry of the loop pattern depends on a clean radiation environment.

- **Mount it above the roofline if at all possible.** A roof-mounted mast 6-10 feet above the peak gets the antenna clear of structural metal.
- **Keep coax runs short.** The omni's low gain means coax loss matters more than for a Yagi — a 100 ft run of RG-8X at 435 MHz is over 4 dB loss, which eats most of the antenna's gain. LMR-400 or hardline is preferable.
- **Don't stack other antennas right next to it.** A vertical 2 m / 70 cm antenna within 6 feet of the eggbeater will detune and distort the pattern. Give it space.
- **Ground the mast properly.** Standard lightning protection (8 AWG copper to a ground rod, lightning arrestor on the feedline). The eggbeater itself has a low DC resistance to ground through the loop, which is mildly helpful but not a substitute for proper lightning protection.

## Operating an eggbeater installation

The operating workflow is almost lazy compared to Arrow-and-track work:

1. **Look at the pass prediction** — usually just to verify the bird is up.
2. **Tune the rig** to the satellite's downlink and uplink.
3. **At AOS:** key the mic and call CQ when you hear the bird or a CQ from another station.
4. **Doppler-correct** as the pass progresses (manual on most rigs; automatic if the IC-9700 is configured for the satellite).
5. **Sign off, QSY** as the pass ends.

No aiming, no tracking, no body contortions to point an antenna overhead. The tradeoff is you miss the very-low-elevation portions of the pass and you can't compete with a high-gain station on a busy pass — but for casual FM-bird work from home, the eggbeater is the right antenna.

> **Advanced —** Some eggbeater designs (notably the **Diamond X-Quad** family and some homebrew variants) use crossed Yagis with a phasing harness to produce a "high-gain omni-like" pattern — they're not truly omni but cover a wider arc than a steerable Yagi. These are a middle ground between the eggbeater and the rotor-mounted Yagi: a few dB more gain than the M2 EB designs in exchange for some directional sensitivity that you can ignore for casual passes.

## DIY eggbeater builds

Several published homebrew designs exist:

- **W6PQL's eggbeater** — popular among microwave builders, uses #14 hard-drawn copper wire for the loops with a printed-circuit-board phasing network.
- **AMSAT-DL's eggbeater plans** — published in DUBUS magazine, used by European satellite ground stations.
- **Various ham-radio club builds** — typically built around 1/4-inch copper tubing, soldered at the loop junctions, with a homebrew quarter-wave phasing line.

A homebrew eggbeater costs about $30 in materials (mostly the copper tubing or wire, BNC/N connector, plywood for the reflector frame). Performance is comparable to the M2 commercial product if construction is careful. Plans typically take a Saturday afternoon to execute.

## See also

- §29-03 — Arrow handheld (the pointed alternative)
- §29-05 — Helical antennas (the high-gain circular-polarization alternative)
- §07-01 — FM vs linear satellites
- §06 — Antennas (loop and Yagi theory)
