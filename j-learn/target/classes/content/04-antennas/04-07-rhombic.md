---
id: 04-07
title: Rhombic (Terminated & Unterminated)
chapter: 04
section: 07
level: advanced
status: draft
---

# Rhombic (Terminated & Unterminated)

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The rhombic was, for a generation, the highest-gain wire antenna a serious shortwave operator could build. Four wires laid out in a long diamond shape, each leg several wavelengths long, supported on four masts. Properly built, it produces 12–16 dBi of gain in a sharp forward-pointing pattern with deep nulls behind. Commercial shortwave broadcasters, transoceanic point-to-point HF links, and military operators ran rhombics for decades.

Today, almost no amateur builds one — the antennas are huge, single-direction, and modest-bandwidth at best. But understanding the rhombic teaches you how high-gain wire arrays work in general, and a few amateur stations on large rural lots still operate them.

## The geometry

A rhombic is a **horizontally-laid diamond**: four wires forming a rhombus shape, fed at one end (the "feed end" or "apex") and either terminated at the opposite end ("terminated rhombic") or left open ("resonant" / "unterminated rhombic"). The diagonals of the rhombus are aligned with the desired direction of communication; the **long axis of the diamond points where you want to talk**.

Key parameters:

- **Leg length (L)**: typically 2 to 10 wavelengths per leg. Longer legs = more gain, narrower beam.
- **Tilt angle (the angle each leg makes with the long axis)**: typically 60–75°. The diamond is *long and narrow*, not square.
- **Height above ground**: typically 0.5 to 1 wavelength (and as a result, rhombics are usually multi-band when supported at one optimized height; they work on harmonically related higher bands too).

A typical "small" amateur rhombic on 14 MHz has 4-wavelength legs (about 280 ft each) and a tilt angle of 70°. That makes the long diagonal about 540 ft and the short diagonal about 200 ft. **The footprint is a city block.** This is why amateur rhombics largely vanished after WWII.

## Terminated vs. unterminated

A terminated rhombic has a **non-inductive resistor** (typically 600–800 Ω) at the far end of the diamond. The feed end has a feed transformer or transmission-line section to match.

- **Terminated**: about half the power that travels down the legs is absorbed by the terminating resistor — meaning **3 dB lower gain** than a pure radiating antenna. In return, you get **~2:1 SWR or better across a 4:1 frequency range** (HF-broadband performance) and a clean unidirectional pattern.
- **Unterminated** (also called "resonant rhombic" or "inverted-V rhombic" depending on orientation): no terminator. The reflected wave from the open end produces standing waves that increase gain by ~3 dB (you keep the power that the terminator would have dumped). The trade-off: narrow bandwidth (resonates only on specific frequencies) and a slightly less clean pattern with a backwards lobe.

Commercial HF broadcasters used **terminated rhombics** because broad bandwidth was worth the 3 dB loss. Amateur "rhombics" on small lots were sometimes the unterminated form, optimized for a single band.

## Gain and pattern

For a rhombic with 4-wavelength legs, optimal tilt, and 0.5 λ height:

- **Forward gain**: 12–14 dBi (terminated), 15–16 dBi (unterminated)
- **Beamwidth (3 dB)**: ~12° azimuth, ~20° elevation
- **Front-to-back**: 25 dB (terminated, very clean) or 12 dB (unterminated)
- **Takeoff angle**: 8–12° (very low — excellent for DX)

For longer legs (8 λ), gain rises to 17–19 dBi, beamwidth narrows to ~6°. The antenna becomes essentially a **point-to-point** antenna; you could communicate New York → London but not New York → Paris, because Paris falls outside the main lobe.

## How it actually works (intuition)

Think of each leg of the rhombus as a long traveling-wave antenna. A wave launched onto the feedpoint travels down the leg toward the far end. Because the leg is several wavelengths long, the radiation it produces along its length adds constructively in **a cone** centered on the wire axis (the "endfire" direction).

Now arrange four such legs in a diamond, with the right tilt angle, so all four cones overlap *in the same direction* — the long axis of the diamond. The contributions from all four legs combine coherently in that direction and cancel in most others. Result: a sharply directional antenna with very low takeoff angle.

The terminator's job is to absorb the wave at the end of the system before it reflects back. Without termination, the reflected wave produces a **rearward-pointing lobe** and standing-wave-induced bandwidth narrowing.

> ⚙️ **Advanced —** Rhombic design is a closed-form optimization problem: for given leg length L (in wavelengths), the optimum tilt angle θ satisfies sin(θ) = sqrt(1 − 1/(2L) − ... ); for L = 4 λ that's about 70°. The optimum height balances ground-reflection enhancement at the desired elevation angle. Bruce et al. (1931, AT&T) is the foundational paper; Harper's 1941 *Rhombic Antenna Design* is the classic engineering treatment. The unterminated version is sometimes called a "Bruce rhombic" after the 1931 paper.

## Why almost no one builds one anymore

Three reasons:

1. **Space.** A 14 MHz rhombic with usable gain wants 500+ feet of long-axis length and four supports. Almost no amateur lot supports this.
2. **Single-direction.** A rhombic only points one way (and the back direction at half gain, with the unterminated form). You'd need a *system* of rhombics to cover the major DX paths from your QTH — and that's how commercial ops did it. They had a rhombic field with a switch matrix.
3. **Yagis got better.** A modern 5-element 20 m Yagi on a 60-ft tower gives 11 dBi forward gain with rotation and a 1/4-acre footprint. The rhombic gets you 13 dBi and you can't rotate it. The trade-off no longer favors the rhombic for amateur use.

## When you might still see (or build) one

- Very large rural lots (10+ acres) with one specific DX path the operator cares about (e.g., a Pacific NW station with a 4-wavelength rhombic aimed at JA).
- Very low frequency (160 m, 80 m) where Yagis are physically impractical and a long wire antenna's space is the only way to get directional gain.
- HF receive-only setups for SWLing — terminated rhombics make excellent receive antennas because the directional pattern rejects off-axis noise.
- Historical / restoration interest at hams clubs with the land for it.

## Common mistakes (by the few who try)

- **Tilt angle wrong for the leg length.** The optimum tilt is leg-length dependent; a "rhombic" with 90° tilt (a square) is not a rhombic — it's a small loop, with completely different pattern.
- **Height too low.** Below ~0.4 λ above ground, the takeoff-angle gain falls apart.
- **Cheap terminator.** The terminator at the far end dissipates about half the transmit power. At 1500 W input, that's 750 W into the resistor, *continuously during a long key-down*. A wirewound resistor will fail. Use a non-inductive carbon-composition or thick-film resistor rated for the dissipation.
- **Forgetting that the terminator end is hot.** RF voltages at the open ends (or even the terminated end) are substantial; keep the area clear.

## See also

- §04-15 — Reading directional patterns
- §04-13 — Height effects on radiation patterns
- §04-12 — Matching transformers (rhombic feed systems)
- (planned §27 — Yagis: the modern alternative)
