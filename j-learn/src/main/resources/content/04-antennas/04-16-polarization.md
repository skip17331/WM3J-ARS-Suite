---
id: 04-16
title: Polarization
chapter: 04
section: 16
level: mixed
status: draft
---

# Polarization

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The orientation of the **electric field** in a propagating radio wave is its polarization. It's set by the geometry of the transmit antenna and (for a clean signal path) preserved as the wave travels. The receiving antenna must be oriented to match — a mismatch costs signal strength.

## The three flavors

| Polarization | Antenna geometry | Where it dominates |
|--------------|------------------|---------------------|
| **Vertical** | Wire / element vertical (perpendicular to ground) | FM repeaters, mobile, marine, public safety, most commercial |
| **Horizontal** | Wire / element horizontal (parallel to ground) | HF SSB / CW long-distance, weak-signal VHF/UHF, EME, meteor scatter |
| **Circular (RHCP / LHCP)** | Two perpendicular elements fed 90° out of phase | Satellites, EME, some weak-signal VHF |

A vertical antenna radiates **vertically polarized** waves; a horizontal dipole radiates **horizontally polarized** waves; a turnstile or cross-Yagi with proper phasing radiates circular.

## Why polarization matters

### Cross-polarization loss

If the transmit antenna is horizontally polarized and the receive antenna is vertically polarized (or vice versa) — a **cross-polarization mismatch** — the signal can drop by **20+ dB** on a clean line-of-sight path. That's about 4 S-units. It's a big deal for VHF/UHF where multipath doesn't help.

For HF skywave paths, the **ionosphere randomizes polarization** as the wave traverses it (Faraday rotation). The signal arrives elliptically polarized at unpredictable angles, so the polarization mismatch loss averages out to about 3 dB regardless of what you transmit and what you receive. This is why HF SSB QSOs work fine between vertical and horizontal stations.

### Mode-by-mode polarization conventions

| Mode / band | Convention | Reason |
|-------------|-----------|---------|
| HF SSB / CW | Horizontal (most stations) | Better long-distance pattern, lower ground losses |
| HF mobile | Vertical (forced by car geometry) | Roof / fender / hatch mounts |
| HF NVIS (regional 80m / 40m) | Horizontal low (~⅛ λ) | High-angle radiation; ground reflection |
| 6m | Mixed (horizontal for SSB/CW, vertical for FM) | SSB inherits HF convention; FM inherits VHF |
| 2m / 70cm FM repeaters | Vertical | Mobile-friendly; vertical antennas omnidirectional |
| 2m / 70cm SSB / CW (weak signal) | Horizontal | Lower noise, better pattern |
| Satellite VHF / UHF (FM birds) | Vertical or whip (HT) | Forgiving; signal margin is large |
| Satellite VHF / UHF (linear birds) | Circular preferred | Spacecraft tumbles — circular avoids fading |
| EME (1296 MHz, 432, 144) | Circular preferred | Faraday rotation through ionosphere + spacecraft attitude |
| Meteor scatter | Horizontal | Trail polarization aligned with horizontal wave |

### When mismatch is a feature, not a bug

Some systems exploit cross-polarization to share spectrum:

- **Broadcast TV / FM** can stack horizontally + vertically polarized antennas at the same site to double effective bandwidth (but this is mostly historical; modern FM uses circular).
- **Cellular** uses ±45° slant polarization in many sites for similar capacity reasons.
- **Repeater receive antennas** sometimes use slant or circular to capture both H and V mobile signals.

For amateur work this rarely matters; we use whatever polarization the band convention says.

## Computing cross-polarization loss

For two linear polarizations differing by angle θ:

```
Loss(dB) = 20 · log₁₀(cos θ)
```

| Angle θ | Loss |
|--------:|-----:|
| 0° (matched) | 0 dB |
| 30° | 1.2 dB |
| 45° | 3 dB |
| 60° | 6 dB |
| 80° | 15 dB |
| 90° (cross-pol) | ∞ (in theory; ~20-30 dB in practice) |

A horizontal antenna receiving a vertical signal, in free space, theoretically gets nothing. In practice:
- Multipath reflections off ground / objects re-randomize polarization.
- Real antennas aren't perfectly linearly polarized — they have a small "cross-pol" component.
- Wave-front depolarization through atmospheric turbulence adds 5-15 dB of "spillover".

So 20-30 dB is the realistic worst case for a clean LOS path; on a multipath-rich path it might be only 3-10 dB.

## Circular polarization

A circularly polarized wave has its E-field rotating around the propagation axis at the wave's frequency. Two flavors:

- **RHCP (Right-Hand Circular)** — E-field rotates clockwise viewed in the direction of propagation
- **LHCP (Left-Hand Circular)** — counterclockwise

A circular antenna receiving a same-sense circular wave gives full signal. **Opposite-sense circular** gives a deep null (the same 20+ dB loss as cross-linear). Linear-to-circular gives a fixed **3 dB loss** regardless of the linear angle — circular receives "half" of the linear wave.

Generated by phasing two perpendicular linear elements 90° apart in time; common implementations:
- **Turnstile** — two crossed dipoles fed 90° out of phase
- **Crossed Yagi** — two Yagis at right angles fed in phase quadrature
- **Quadrifilar helix** — four helically-wound elements (used in many GPS antennas)
- **Helical antenna** — single wire wound as a helix; pitch and circumference set polarization

> ⚙️ **Advanced —** A pure circular wave is the sum of two perpendicular linear waves of equal amplitude with 90° phase difference. Imperfect circularity is described by **axial ratio** (AR = max electric-field amplitude / min). AR = 1 (0 dB) is perfect circular; AR > 6 dB is essentially linear at some angle. Quality satellite antennas achieve AR ≤ 3 dB on-axis.

## In practice for your station

- **HF**: don't worry about polarization. Ionosphere takes care of it. Vertical or horizontal — pick what fits your space.
- **VHF/UHF FM repeaters**: install vertical. Period.
- **VHF/UHF weak signal (SSB / CW)**: install horizontal Yagis. Match the local convention.
- **Satellites**: a circular antenna helps but a linear yagi works for FM birds. For linear birds, expect deep fades from polarization mismatch — switching the polarization sense (RHCP ↔ LHCP) recovers the signal during a fade.
- **Mobile**: vertical (forced by geometry).

## See also

- §01 — Propagation (where Faraday rotation comes in)
- §04-15 — Radiation Patterns (pattern + polarization together)
- §04-17 — Diversity (using polarization as one diversity dimension)
- §05 — Satellites (circular polarization for sat work)
- §15-08 — ERP (when polarization-mismatch losses enter the link budget)
