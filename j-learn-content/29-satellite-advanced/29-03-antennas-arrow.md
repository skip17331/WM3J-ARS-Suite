---
id: 29-03
title: Arrow Handheld Yagi
chapter: 29
section: 03
level: mixed
status: published
---

# Arrow Handheld Yagi

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The **Arrow Antennas 146/437-10WBP** ("Wide Band Portable") is the canonical handheld satellite Yagi. If a hundred amateurs are working AO-91 or SO-50 from a park bench somewhere on a Saturday afternoon, ninety of them have one of these — or its slightly-different sibling, the 146/437-10BP (without the wideband 70 cm matching network). Arrow Antennas of Cheyenne, Wyoming has been making these since the late 1990s, and the design has barely changed because it doesn't need to.

## What it is

A cross-polarized dual-band Yagi assembled from aluminum tubing:

- **3 elements on 2 m (146 MHz):** reflector, driven element, 1 director — about 7.5 dBi gain, 60° forward beamwidth.
- **7 elements on 70 cm (437 MHz):** reflector, driven element, 5 directors — about 11.5 dBi gain, 35° forward beamwidth.
- The two Yagis are mounted **perpendicular to each other** on a common boom. The 2 m driven element is horizontal; the 70 cm driven element is vertical (or vice versa, depending on assembly choice).
- Two separate feedlines (one per band) terminate in BNC connectors at the back of the boom. A foam-rubber handgrip lets you carry it like a TV antenna — boom horizontal, you holding the back, the front pointed at the sky.
- Assembled length about 34 inches; disassembles into a small bundle that fits in a backpack.
- Weight about 1.5 lbs.

The "wideband" version uses a matching network on the 70 cm element that flattens the SWR across a wider range than the basic BP — useful if you're working multiple satellites with different 70 cm frequencies (435 MHz region) in one session.

## Why it works for satellites

Three things make this antenna almost perfectly matched to LEO satellite operating:

1. **Gain enough, not too much.** 7.5 dBi on 2 m and 11.5 dBi on 70 cm is the right tradeoff. More gain narrows the beamwidth, and a too-narrow beam is hard to hold on a moving satellite by hand. The Arrow's beams are wide enough that small pointing errors don't drop you out of the pass.

2. **Cross-polarized elements give you linear-to-circular flexibility.** Satellites can transmit at any polarization (and many slowly rotate). By rotating the Arrow physically around its boom axis as you work the pass, you can find the polarization that maximizes your downlink signal at any moment. You can't do this with a single-polarization Yagi.

3. **Genuinely portable.** Disassembles in 30 seconds, fits in a backpack, sets up in 60 seconds. You can work a satellite from a hiking trail, a hotel parking lot, a SOTA summit, or your back yard.

## Operating technique

The Arrow is a manual-aim antenna. There's no rotator, no automated tracking — you point it by eye, with the satellite's predicted az/el path in your other hand (or on your phone).

### Setting up

Before AOS:

1. **Look up the pass on your phone.** A pass prediction app (ISS Detector, GoSatWatch, AMSAT's pass predictor) gives you AOS time, AOS azimuth, max elevation, and LOS azimuth.
2. **Stand facing AOS azimuth.** Hold the Arrow with the boom horizontal, pointed at the AOS heading and tilted up at about 5-10° elevation.
3. **Configure your radio.** Set uplink, downlink, CTCSS tone (for FM birds), and verify full-duplex is enabled (§29-01).
4. **Headphones on.** You'll need to hear the downlink while you transmit.

### During the pass — the 30° up arc

This is the standard hand-aiming technique:

1. **At AOS:** point at the AOS azimuth, low elevation (about 10° up). Start listening for the satellite's downlink. You should hear it within 30 seconds of predicted AOS.
2. **Track the path:** sweep the antenna across the sky following the predicted ground track. For a typical 60°-max-elevation pass, the antenna will arc from low-east through high-overhead to low-west.
3. **Point AHEAD of where you think the satellite is.** LEO satellites move at about 4°/second across the sky at closest approach. Your hand-aiming lag (perception to muscle response to where the antenna actually is) is typically 1-2 seconds — so the antenna should lead the satellite by 4-8°. If you point exactly where the satellite is, by the time the antenna gets there, the satellite has moved on. This is the single biggest mistake new Arrow operators make.
4. **At maximum elevation:** the antenna is high overhead. The hardest physical position to hold and the hardest to point — but also the strongest signal segment of the pass, so it's worth the effort. Some operators kneel or lie on their back during max elevation for stability.
5. **At LOS:** the antenna is low on the descending azimuth. Signal drops rapidly in the last 30 seconds; you may need to take a step or two as the satellite goes behind a horizon obstruction.

### Polarization rotation

LEO satellites tumble. Their downlink polarization rotates relative to your antenna throughout the pass — sometimes by tens of degrees per minute, sometimes more slowly. The Arrow's perpendicular elements let you compensate by physically rotating the antenna around its boom axis.

If the downlink suddenly goes weak (deeper than expected for the geometry), **rotate the antenna 45° to 90° around the boom axis** while continuing to point at the satellite. Usually within a quarter turn you'll find the polarization that gives you a clean signal again. Repeat as needed throughout the pass.

> **Advanced —** The 2 m and 70 cm elements are perpendicular, so when one band is at optimum linear polarization match, the other is at worst (cross-polarized → 20-30 dB loss). In practice this matters less than you'd think because the satellite is slowly rotating its polarization too — over a 10-minute pass, you'll cycle through several optima for each band, and 70 cm being the higher-gain link tends to be the one you optimize first. For FM birds where the uplink-receive bandwidth at the satellite is wide and forgiving, you optimize for downlink (70 cm); for linear birds with V/U mode, you may optimize for the harder of the two (typically downlink again, since the satellite's transmitter is power-limited and your TX has spare margin).

## Common Arrow mistakes

- **Pointing where the satellite is, not where it's going.** Lead it.
- **Not rotating polarization.** Fixed orientation throughout the pass leaves you in a deep fade for parts of every pass.
- **Holding it too tight, too high, for too long.** Wear out your arm at max elevation and you're going to bobble through LOS. Brace your elbows on your body if you can.
- **Forgetting to check coax routing.** The 2 m and 70 cm feedlines come off the back of the boom. If they snag on your wrist or radio strap when you turn through the pass, you'll yank the connector — usually right when you need the signal most.
- **Working in a place with metal obstructions.** Metal roofs, fences, vehicles within a few meters distort the antenna pattern. Open ground is best.

## Variants and accessories

- **Arrow II 146/437-14:** longer boom, more elements, more gain. Used by serious portable operators but harder to aim and heavier to hold.
- **Diplexer for single-feedline operation:** combines the two band feedlines into one. Useful if your rig has only one antenna port (like a satellite HT). Adds about 0.5 dB loss but simplifies the setup.
- **Arrow tripod adapter:** the OSJ portable mount that lets you put the Arrow on a camera tripod. Frees your hands but requires you to physically reposition between pointing changes — not ideal for active tracking, fine for stationary work like ISS school contacts.

## See also

- §29-04 — Eggbeater omni (the no-pointing alternative)
- §29-06 — Polarization switching (the fixed-station equivalent of physical rotation)
- §07-01 — FM vs linear satellites
- §06 — Antennas (Yagi theory)
