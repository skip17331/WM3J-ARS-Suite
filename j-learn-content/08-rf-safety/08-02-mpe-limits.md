---
id: 08-02
title: MPE Limits
chapter: 08
section: 02
level: mixed
status: published
---

# MPE Limits

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The **Maximum Permissible Exposure (MPE)** limits are the actual numbers used to evaluate compliance. They specify the maximum E-field strength, H-field strength, or far-field power density allowed at any place humans might be exposed, as a function of frequency and exposure category (controlled vs. uncontrolled).

This section is the lookup reference. It also explains the near-field vs. far-field distinction that's responsible for many beginner errors.

## The MPE table (47 CFR 1.1310)

| Frequency range | E-field (V/m) — controlled | H-field (A/m) — controlled | Power density (mW/cm²) — controlled | E-field (V/m) — uncontrolled | H-field (A/m) — uncontrolled | Power density (mW/cm²) — uncontrolled |
|----------------|----------------------------|----------------------------|--------------------------------------|------------------------------|------------------------------|----------------------------------------|
| 0.3–3.0 MHz | 614 | 1.63 | 100 | 614 | 1.63 | 100 |
| 3–30 MHz | 1842 / f | 4.89 / f | 900 / f² | 824 / f | 2.19 / f | 180 / f² |
| 30–300 MHz | 61.4 | 0.163 | 1.0 | 27.5 | 0.073 | 0.2 |
| 300–1500 MHz | — | — | f / 300 | — | — | f / 1500 |
| 1500–100,000 MHz | — | — | 5.0 | — | — | 1.0 |

(f = frequency in MHz)

Three things to notice:

1. **The 30–300 MHz range is the worst** — that's where the human body absorbs RF most efficiently. MPE limits are lowest there.
2. **Below 30 MHz, the limit scales with 1/f or 1/f²** — at lower frequencies, more field is allowed because absorption is less efficient. (Magnetic-field heating becomes the limiting factor instead.)
3. **Uncontrolled power-density limit is 5× lower than controlled** in the worst-case range (30–300 MHz). Hence the importance of identifying which category each location falls under.

## Translating to amateur bands

Power density limits, computed for each amateur band, in mW/cm²:

| Band | Frequency | Controlled | Uncontrolled |
|------|-----------|-----------|--------------|
| 160 m | 1.8 MHz | 100 | 100 |
| 80 m | 3.5 MHz | 73.5 | 14.7 |
| 40 m | 7 MHz | 18.4 | 3.7 |
| 30 m | 10 MHz | 9.0 | 1.8 |
| 20 m | 14 MHz | 4.6 | 0.92 |
| 17 m | 18 MHz | 2.78 | 0.56 |
| 15 m | 21 MHz | 2.04 | 0.41 |
| 12 m | 24 MHz | 1.56 | 0.31 |
| 10 m | 28 MHz | 1.15 | 0.23 |
| 6 m | 50 MHz | 1.0 | 0.2 |
| 2 m | 144 MHz | 1.0 | 0.2 |
| 1.25 m | 222 MHz | 1.0 | 0.2 |
| 70 cm | 432 MHz | 1.44 | 0.288 |
| 33 cm | 902 MHz | 3.0 | 0.6 |
| 23 cm | 1296 MHz | 4.32 | 0.86 |
| 13 cm | 2304 MHz | 5.0 | 1.0 |

So: **at 14 MHz, the uncontrolled MPE is 0.92 mW/cm².** Anywhere bystanders might be must see a power density less than this when time-averaged.

## Computing the field at a distance

For a far-field calculation (distance > 2 D²/λ from antenna):

**Power density (mW/cm²) = 0.000064 × ERP(W) / d²(ft)**

Or equivalently:

**Power density (mW/cm²) = 0.0000884 × ERP(W) × G(numeric) / d²(ft)**

where G is the antenna's gain over an isotropic radiator (G_dBi → numeric is 10^(G_dBi/10)).

A **simpler form using ERP in watts**:

**Power density (mW/cm²) ≈ 0.064 × ERP(W) / 1000 / d² (m²)**

### Example

Station: 100 W to a 5 dBi gain dipole (ERP_isotropic ≈ 100 × 3.16 = 316 W EIRP). Frequency: 14 MHz. Distance to bystander: 30 ft (9.1 m).

Power density = 0.000064 × 316 / 30² = 0.0000225 mW/cm² × 1000 = 0.0225 mW/cm².

MPE uncontrolled at 14 MHz: 0.92 mW/cm². You're at 2.4% of the limit. Compliant.

### Example with high power and close approach

Station: 1500 W to a 6 dBi vertical (EIRP ≈ 1500 × 4 = 6000 W). Frequency: 14 MHz. Distance to neighbor's deck: 12 ft.

Power density = 0.000064 × 6000 / 12² = 2.67 mW/cm².

MPE uncontrolled at 14 MHz: 0.92 mW/cm². You are at **2.9× the limit**. Non-compliant. This installation has to mitigate (lower power, move antenna, restrict access, or beam direction control).

## Near-field vs. far-field

The standard MPE formulas assume **far-field conditions**, which require:

**distance > 2 D² / λ**

where D is the largest dimension of the antenna and λ is the wavelength. For a 33-ft 20 m dipole at 14 MHz (λ = 21 m), the far-field distance is 2 × (33 × 0.305)² / 21 = 9.7 m or about 32 feet. **Closer than that, you're in the near field**, where:

- The simple "1/d²" power-density formula doesn't apply.
- Field strength can be locally higher than the far-field formula predicts.
- E-field and H-field are not simply related (at far field, H = E/377 for a propagating wave; at near field, the local impedance can be very different from 377 Ω).

For amateur work near antennas (the operator inside their own near field, common for indoor antennas, low antennas, and especially magnetic loops), the calculation gets more complicated. Conservative approach: **assume the worst-case near-field intensity is 2–3× the far-field formula's prediction**, and check whether you're still compliant under that assumption. Or use NEC modeling, which can compute near-field strength directly.

> **Advanced —** The 2D²/λ "far-field distance" comes from the requirement that the path-length difference from the antenna's edges to a point at distance d, vs. the path-length to the antenna center, is less than λ/16 — keeping phase coherence within an acceptable tolerance for the radiating-far-field approximation. Inside this distance, you can have either a "reactive near field" (very close, where the antenna's stored field dominates) or a "radiating near field" (transitional region). MPE evaluations in the near field properly use OET-65's near-field correction methods, or NEC's near-field output mode. For a simple low dipole or vertical, an upper-bound near-field estimate is **3× the far-field prediction at the same distance** — conservative but workable.

## Magnetic loops: special case

A magnetic loop is unusual because it radiates primarily through its magnetic field, and the H-field falls off slowly (1/r³ in the very near field, transitioning to 1/r² and finally 1/r in the far field). At the loop's gap and at distances within ~λ/10 of the loop, the H-field can be several A/m at moderate transmit power.

The MPE table sets H-field uncontrolled limits at 0.073 A/m above 30 MHz, and 2.19/f A/m below 30 MHz. At 14 MHz that's 0.16 A/m — and a 100 W magnetic loop at its gap may produce H = 1 A/m or more.

**Magnetic loops require explicit near-field MPE evaluation**, not just power-density calculations. NEC modeling is recommended; failing that, **assume the operator and any bystander must stay at least 1 meter from the loop during transmit at 100 W or more**, and 2–3 meters at higher powers.

## How time-averaging changes the math

The MPE limits are **time-averaged over a 6-minute window for controlled environments and 30-minute window for uncontrolled environments**. So if your peak field in some location is 2× the MPE limit, but you only transmit 1/4 of the time, time-averaged exposure = 0.5 × MPE = compliant.

The averaging window matters: a station that transmits a 5-minute call followed by a 1-minute pause (during a contest) might exceed the controlled-environment time-averaged limit in some moment, while a station transmitting 1 minute then 30 minutes of receive easily complies.

For amateur ops, the typical computation is:

**Time-averaged power = peak power × (mode duty cycle) × (TX time fraction)**

Common mode duty cycles:

| Mode | Duty cycle |
|------|-----------|
| CW (steady tone, key down) | 100% |
| FT8, RTTY, MFSK, other digital | 100% |
| FM | 100% (carrier always on) |
| SSB voice | ~30–40% (modulation envelope dependent) |
| AM | ~50% |

A 1500 W FT8 station transmitting in 15-second bursts every minute is exposing bystanders to **(1500 × 1.0 × 0.25) = 375 W average**, not 1500 W. That matters.

## Practical takeaways

1. **Below 30 MHz, MPE limits are usually generous** — a typical horizontal antenna 30+ ft up easily complies on most HF bands at most power levels.
2. **30–300 MHz is the strict range** — VHF/UHF beam stations need to think hard about pattern direction during high-power TX.
3. **Magnetic loops, attic dipoles, mobile installs with amps** are the most likely problem cases. Evaluate carefully.
4. **Always evaluate uncontrolled MPE limits** for spaces bystanders might occupy. The 5× tighter limit is usually the binding constraint.
5. **Time-averaging is real** — a station transmitting 5% of the time has its exposure reduced 20× in time-averaged terms.

## See also

- §08-01 — FCC rules (the regulatory framework)
- §08-03 — Controlled vs uncontrolled (which limit applies where)
- §08-04 — Duty cycle (the time-averaging factor)
- §08-05 — ERP (the input to power-density calculations)
- §08-06 — Safe antenna placement (using these limits in installation design)
- §17-14 — RF Exposure Calculator
- §11 — Power budget / ERP
