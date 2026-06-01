---
id: 06-07
title: Radiation Patterns
chapter: 06
section: 07
level: mixed
status: published
---

# Radiation Patterns

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A radiation pattern is a **3D map of where your antenna sends and receives signal**. In two senses, your antenna *is* its pattern: the same dipole at two different heights produces two different patterns and therefore is, for practical purposes, two different antennas.

This section covers what patterns mean, how to read the two standard 2D plots (azimuth and elevation), and how to use pattern thinking to pick antennas, tune installations, and predict propagation paths.

## What a pattern shows

The radiation pattern is the **directional gain** in dBi as a function of direction. It's a 3D function: at every (azimuth, elevation) pair, you have a gain number. We almost never plot it in 3D; instead we use two 2D slices:

- **Azimuth pattern** — gain vs compass direction at a fixed elevation angle (usually the elevation of peak radiation, or "low elevation" like 10°).
- **Elevation pattern** — gain vs elevation angle at a fixed azimuth (usually the antenna's "main beam" azimuth).

Two patterns together describe the antenna for most operational purposes.

## Reading an azimuth pattern

Azimuth patterns are the famous-looking polar plots. Common shapes:

- **Omnidirectional (circle)** — a vertical antenna's azimuth pattern. Equal gain in all horizontal directions.
- **Figure-8** — a horizontal dipole or full-wave loop, broadside to the wire. Two peaks 180° apart, two nulls 90° away.
- **Cardioid (heart-shape)** — phased verticals, end-fire arrays. Peak in one direction, null toward the back.
- **Multi-lobe (cloverleaf)** — long wire antennas, harmonic operation. Multiple narrower beams in specific directions, with deep nulls between them.
- **Beam pattern** — Yagi or beam: one dominant forward lobe, smaller back lobe, near-zero sidelobes.

For DX work, the **front-to-back ratio** (forward gain minus backward gain in dB) and **3 dB beamwidth** (the angular range over which gain stays within 3 dB of peak) tell you most of what you need. A 4-element 20 m Yagi has roughly 6 dBi gain, 25 dB front-to-back, 65° beamwidth. A 5-element has 8 dBi, 22 dB F/B, 55° beamwidth.

## Reading an elevation pattern

Elevation patterns plot gain vs elevation angle from horizon (0°) to zenith (90°). For a horizontal antenna over real ground, the pattern shows:

- **A "main lobe" at some elevation** — the height-and-ground-determined peak.
- **Sometimes a second, third, fourth lobe** at higher elevations (interleaved ground reflections at higher harmonics of the height).
- **A null at the horizon** (0°) for any horizontal antenna over real ground.
- **A zenith null or zenith peak** depending on antenna type.

The single most useful number is the **takeoff angle**: the elevation of the lowest lobe's peak. For DX, you want this *low* (5–20°). For NVIS, you want it *high* (60–90°).

## Takeoff angles for common installations

| Antenna | Configuration | Takeoff angle (peak of lowest lobe) |
|---------|--------------|--------------------------------------|
| Horizontal dipole | 0.25 λ above ground | 90° (NVIS) |
| Horizontal dipole | 0.5 λ above ground | 30° |
| Horizontal dipole | 1.0 λ above ground | 14° |
| Horizontal dipole | 1.5 λ above ground | 9° |
| Inverted V (apex 0.5 λ) | 120° apex angle | ~32° (slightly higher than flat dipole) |
| Vertical (¼-wave with full radials) | Ground-mounted | 18–25° (depending on ground quality) |
| Vertical (½-wave or longer) | Ground-mounted | 15–25° (slightly lower than ¼-wave) |
| 3-element Yagi | 1.0 λ above ground | 14° |
| 5-element Yagi | 1.5 λ above ground | 9° |
| Skyloop | 0.4 λ above ground | 35–45° (high; with zenith null) |
| Magnetic loop (vertical plane) | Ground level | 25–35° (mostly horizontal polarization at peak) |
| EFHW (sloper, low end at 8 ft, high end at 40 ft) | — | 25° (mixed; some vertical polarization) |

What you actually want depends on what you're working:

- **Local QSO (under 200 km)**: high takeoff angle (60°+). NVIS or any low antenna.
- **Regional (200–1500 km)**: medium takeoff angle (25–45°).
- **Long-haul DX (5000+ km on HF)**: low takeoff angle (5–20°). Height and good ground both help.
- **Short-haul VHF/UHF**: usually any reasonable angle; line-of-sight and tropo-ducting dominate, not pattern shape.

## Polarization

The antenna's polarization is the orientation of the E-field of the radiated wave. In amateur practice:

- **Horizontal**: dipoles, beams, loops fed at the bottom. Most HF antennas. Quieter (less noise pickup from vertical-polarized power-line noise).
- **Vertical**: vertical antennas, mobile whips. Lower angle of radiation. More susceptible to vertical noise.
- **Mixed / circular**: some loops, helices, satellite antennas. The wave's E-field rotates as it propagates.

**Polarization mismatch is real**: a vertical and a horizontal antenna at the same site, talking to each other on a clear LOS path, will see ~20 dB cross-polarization loss. *In propagation through the ionosphere, polarization is generally scrambled* (Faraday rotation), so on HF DX paths the polarization mismatch issue mostly disappears. On VHF/UHF line-of-sight, it matters: you don't talk to a vertical-polarized FM repeater with a horizontal beam without paying a 20 dB price.

## Reading 3D patterns

Modern modeling tools (EZNEC, MMANA, 4nec2) produce 3D pattern visualizations. They look like distorted donuts, doughnuts on edge, or coral reefs. Three things to look at:

1. **The shape's main lobe direction** — does it point where you want?
2. **Pattern depth in unwanted directions** (back, sidelobes) — how much rejection?
3. **Lobe geometry near the horizon** — how steeply does gain fall off below the takeoff angle?

A pattern with a single sharp lobe pointed at the horizon is ideal for DX. A doughnut on its side (vertical antenna's shape) is ideal for omnidirectional medium-DX.

## Gain numbers

Patterns show gain in **dBi** (relative to an isotropic radiator) or **dBd** (relative to a half-wave dipole). The relation:

**dBi = dBd + 2.15**

Gain is a directional measurement: a "10 dBi antenna" has 10 dBi *in the direction of peak gain*, but probably less in other directions (or even negative — many beams have nulls). When comparing antennas, ask "gain in *what direction*?"

A few benchmark gains:

| Antenna | Gain (dBi, peak direction) |
|---------|----------------------------|
| Isotropic (theoretical) | 0 dBi |
| Half-wave dipole, free space | 2.15 dBi |
| Half-wave dipole, 0.5 λ above good ground | 6.5 dBi |
| Quarter-wave vertical with extensive radials | 1 dBi (low elevation peak; falls off fast) |
| 3-element Yagi | 6–7 dBi |
| 5-element Yagi | 8–9 dBi |
| 6-foot dish at 1296 MHz | 22 dBi |
| Long rhombic | 14–18 dBi |
| EME-class array (4 stacked Yagis) | 18–22 dBi |

Note that ground reflection adds 4–5 dB to the dipole's gain in the lowest-lobe direction. This is *real* gain (the signal is concentrated in a useful direction), but if you compared a "free-space dipole" model to your real antenna, you'd see this difference.

## Patterns at higher harmonics

When you operate an antenna on a harmonic (a 40 m dipole on 15 m, or an 80 m EFHW on 20 m), the pattern is **not the same as on the design band**. At 3× a dipole's design frequency, the pattern develops 4 lobes, not 2; the broadside lobe splits into a butterfly pattern with maxima at ~40° off-broadside.

This is why a multi-band antenna may have gain in different *directions* on different bands. A 40 m dipole hung east-west:

- 40 m: figure-8 broadside (maximum NSEW broadside, null east-west).
- 15 m: 4-lobe cloverleaf with maxima at ~50° off-broadside.

The implication: think of multi-band wire antennas as **band-specific patterns**, not "one omnidirectional antenna."

> **Advanced —** The current distribution at higher harmonics has multiple full-wave loops along the wire (current zeros at half-wavelength intervals). The far-field pattern is the Fourier transform of the current distribution, so multi-loop currents produce multi-lobe patterns. The lobe maxima occur where the path length differences between the various current peaks are integer multiples of a wavelength toward the observer. For a 3λ wire (3rd harmonic), peak gain is about 4.5 dBi at angles roughly 50° off the wire axis; for 5λ it's 6 dBi at 35° off. Operating a wire antenna on its harmonic can actually be *more directional* than its fundamental, but pointed in a different direction than the fundamental.

## Patterns and propagation

On HF, the path you actually use depends on which elevation angle the ionosphere supports for your distance. **MUF dictates angle** as much as antenna does:

- For 1500-km hops, the elevation angle is typically 15–25°. A horizontal dipole at 0.7 λ matches well.
- For 3000-km hops (one F2 hop), 5–15° angles. Higher antenna height is needed.
- For very-long DX (10,000+ km), 3–8°. Low-angle radiation is essential.

If your antenna has poor low-angle gain (a low dipole), distant DX becomes much harder than the band conditions alone would suggest. **Pattern matters more than ERP** for hard DX paths.

## Practical decisions from patterns

- **"Should I raise the antenna?"** Yes, if your DX-targeted band's takeoff angle drops as you raise it. Above about 1 λ, returns diminish.
- **"Should I switch from dipole to vertical?"** Look at the dipole's takeoff angle vs. the vertical's. If the vertical's lowest lobe is lower than the dipole's at your height, vertical wins for DX. (At 0.25 λ, the dipole is dead for DX and the vertical wins easily; at 1 λ, the dipole wins.)
- **"Why does my new antenna get weaker reports than the old one?"** Compare the two patterns. A higher-gain pattern in the wrong direction will lose to a lower-gain pattern in the right direction.
- **"Why don't I hear EU on 80 m at night?"** Because 80 m EU is a 5–10° elevation path, and your low 80 m dipole is shooting straight up. NVIS antenna for NVIS uses; high antenna for DX.

## See also

- §06-05 — Ground-plane effects (the major pattern-shaper at low heights)
- §06-06 — Modeling (tools to compute these patterns)
- §06-01 — Smith charts (matching is part of the system; pattern is the other)
- §11 — Power budget and ERP (ERP × pattern gain = effective field strength)
- §01-04 — Ionospheric layers (how angle becomes distance)
