---
id: 06-01
title: Dipoles
chapter: 06
section: 01
level: mixed
status: draft
---

# Dipoles

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The half-wave dipole is the antenna everything else is compared to. When someone says "+3 dBd," they mean *3 dB better than a dipole at the same height*. When a manufacturer claims gain in dBi, you can mentally subtract about 2.15 to get dBd.

A dipole is two pieces of wire, total length about a half-wavelength on the band of interest, fed in the middle. That's it. With nothing more, you have an antenna that will work better than most operators expect.

## The headline numbers

| Property | Value |
|----------|-------|
| Total length (free space) | 468 / f(MHz) feet, or 143 / f(MHz) meters |
| Feed-point impedance (free space, infinite height) | 73 + j42 Ω |
| Feed-point impedance (½ λ above average ground) | ~70 Ω, near-zero reactance — feeds 50 Ω coax with SWR ~1.4:1 |
| Pattern | Figure-eight broadside to the wire (in free space) |
| Gain over isotropic | 2.15 dBi |
| Gain over a dipole | 0 dBd, by definition |
| Polarization | Same as the wire (horizontal if hung horizontal) |

The 468 figure already includes a velocity-factor correction for bare wire near earth. The "physics" answer is closer to 492/f(MHz), but you would always have to trim a 492 dipole down. 468 is a tested empirical starting length.

## The cookbook lengths

Cut a little long, then trim. Always.

| Band | Frequency for cut | Dipole length |
|------|-------------------|---------------|
| 80 m | 3.650 MHz | 128 ft 2 in (39.07 m) |
| 60 m | 5.350 MHz | 87 ft 6 in (26.69 m) |
| 40 m | 7.150 MHz | 65 ft 5 in (19.94 m) |
| 30 m | 10.125 MHz | 46 ft 3 in (14.10 m) |
| 20 m | 14.175 MHz | 33 ft 0 in (10.06 m) |
| 17 m | 18.118 MHz | 25 ft 10 in (7.87 m) |
| 15 m | 21.225 MHz | 22 ft 1 in (6.73 m) |
| 12 m | 24.940 MHz | 18 ft 9 in (5.71 m) |
| 10 m | 28.500 MHz | 16 ft 5 in (5.00 m) |
| 6 m | 50.150 MHz | 9 ft 4 in (2.85 m) |
| 2 m | 146.000 MHz | 3 ft 2 in (0.97 m) |

These are total lengths; cut each leg to half. Add 6 inches to each end for trimming. **Always trim down, never lengthen** — adding wire is a hassle you don't want.

The J-Hub Antenna tab does this calculation interactively.

## Cutting and tuning

1. Pick a center frequency. For 40 m DX, 7.150. For 40 m SSB phone, 7.225. For 40 m CW, 7.025.
2. Cut each leg to *half the table length plus 6 inches*.
3. Hang it at final height with final feedline routing. **Never tune at low height** — the resonant frequency shifts as you raise it.
4. Sweep with an analyzer. If resonance is *low* (e.g., wanted 7.150, got 7.080), trim equal lengths off both ends. **A rule of thumb: 1% length change → 1% frequency change.** So to move from 7.080 to 7.150 (about 1%), shorten by about 1% — that's about 8 inches total, 4 inches off each end.
5. Re-sweep, repeat. Two iterations usually gets you within 25 kHz.

## Feeding a dipole

Center-fed dipoles want **balanced feedline** because the antenna itself is balanced. Coax is unbalanced. If you feed a balanced antenna with unbalanced line, RF runs back down the *outside* of the coax shield as common-mode current. This is the most common cause of "weird stuff" — RF in the shack, distorted audio, computer interference, lights flickering when you key up.

The fix is a **1:1 current balun (choke balun)** at the feedpoint. See §06-12 for the details. In practice: every coax-fed dipole should have a choke balun at the center, period.

> ⚙️ **Advanced —** The 73-ohm free-space impedance comes from solving the integral for sinusoidal current distribution on a half-wave wire and is a textbook result. Real dipoles in real installations rarely sit at 73 ohms; height above ground, soil conductivity, and proximity to other conductors all shift it. At a half-wavelength above earth the impedance peaks near 90 Ω; at a quarter-wavelength it dips below 50 Ω; at very low heights (<0.15 λ) it falls below 20 Ω with significant ground loss. This is one reason the same dipole "tunes differently" at different heights.

## Multiband dipoles

A dipole cut for 40 m is also resonant on **15 m** (the 3rd harmonic; 7.150 × 3 ≈ 21.45 MHz). It is *not* resonant on the even harmonics. This trick gives you two bands free with one wire — the **40/15 dipole**.

Other useful coincidences:

- **80 m dipole** is roughly usable on **30 m** with a tuner, and resonant on **24 MHz** (12 m) as a third harmonic.
- **A 135-ft doublet** (close to a half-wave on 80 m) fed with ladder line and an antenna tuner will tune everywhere from 80 m through 10 m. This is the **G5RV's underlying idea** but cleaner.

For genuine multi-band coverage with separate resonance per band, see **fan dipoles** (multiple parallel dipoles, all at the center insulator, only one resonant per band) and **trap dipoles** (§06-08).

## Bandwidth

A wire dipole made of #14 AWG wire has a usable (SWR < 2:1) bandwidth of roughly **2% of center frequency**. On 40 m that's about 140 kHz. On 80 m it's about 70 kHz — which is *not* the whole band. You'll need either a tuner, two 80-m dipoles cut for different sub-bands, or a fatter conductor.

Bandwidth scales with conductor diameter. A dipole made of 1-inch aluminum tubing has roughly 4× the bandwidth of one made of #14 wire. **Cage dipoles** (multiple parallel wires spaced a few inches apart, joined at the ends) are an old-school way to fake fat-conductor bandwidth using just wire.

## Common variations

- **Inverted V** — center supported, ends drooping. See §06-02.
- **Sloper** — fed at the top, slanted toward the ground. Slightly directional.
- **Doublet** — same physical dipole, but fed with ladder line and a tuner instead of cut to a single resonance. Multi-band.
- **OCFD (Off-Center Fed Dipole)** — fed about 1/3 of the way from one end. Higher feed impedance (~200 Ω, used with a 4:1 balun), works on more harmonic bands than a center-fed dipole. See §06-12 for the balun.

## Common mistakes

- **Tuning at low height.** Resonance shifts 5–10% as you raise the antenna. Tune at final height or you'll re-tune three times.
- **No balun at the feedpoint.** RF in the shack, hot mics, weird patterns. Always use a 1:1 current balun.
- **Cut too short.** You can lengthen wire, but it's annoying. Cut long, trim down.
- **Bad insulators.** Plastic wire ties as end insulators last about a year in the sun. Use real ceramic or molded plastic insulators rated for outdoor use.
- **Sharp bends or kinks** at the feedpoint. The impedance is sensitive. Keep the wire straight at the feedpoint for at least a foot.

## See also

- §06-02 — Inverted V (the dipole most operators actually build)
- §06-12 — Baluns: which one and where to put it
- §06-13 — Why height changes everything
- §06-15 — Reading the dipole's radiation pattern
- §09 — Antenna calculator
- §10-04 — Feedline and SWR effects
