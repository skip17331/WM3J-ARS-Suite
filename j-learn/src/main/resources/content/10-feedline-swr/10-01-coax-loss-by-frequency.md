---
id: 10-01
title: Coax Loss by Frequency
chapter: 10
section: 01
level: mixed
status: draft
---

# Coax Loss by Frequency

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Coax loss is **frequency-dependent**. A cable that's fine on 80 m may be marginal on 6 m and unusable on 2 m. The relationship is governed by physics: the **skin effect** that confines AC current to the surface layer of the conductors gets stronger at higher frequencies, increasing resistance and therefore loss; **dielectric loss** in the insulator also grows with frequency.

This section covers how loss scales with frequency, why each cable type has the slope it does, and how to compute total loss for any cable at any frequency.

## The basic frequency dependence

For a typical coax dominated by conductor (skin-effect) loss:

**α(f) ≈ α(f_ref) × √(f / f_ref)**

That is, loss scales as the **square root of frequency**. Doubling frequency increases loss by √2 ≈ 41%. Increasing by 10× increases loss by ~3.16× (some).

For dielectric loss alone, the relation is **linear with frequency** (α ∝ f). Most amateur cables in the HF/VHF/UHF range are dominated by conductor loss, so the √f rule is a good first approximation. Above ~1 GHz, dielectric loss starts to matter more for typical PE-dielectric coax.

## Loss curves: a few common cables

Per 100 ft, all values nominal at 25 °C:

| Cable | 1.8 MHz | 7 MHz | 14 MHz | 28 MHz | 50 MHz | 144 MHz | 432 MHz | 1296 MHz |
|-------|---------|-------|--------|--------|--------|---------|---------|----------|
| RG-58 | 0.4 | 0.9 | 1.6 | 2.5 | 3.5 | 6.6 | 12.0 | 22.5 |
| RG-8X | 0.3 | 0.6 | 1.0 | 1.6 | 2.4 | 4.5 | 8.0 | 15.5 |
| RG-213 | 0.2 | 0.4 | 0.65 | 1.0 | 1.5 | 2.8 | 5.0 | 9.5 |
| LMR-400 | 0.13 | 0.27 | 0.45 | 0.7 | 1.0 | 1.5 | 2.7 | 4.8 |
| LMR-600 | 0.07 | 0.15 | 0.27 | 0.4 | 0.6 | 0.95 | 1.7 | 3.0 |
| LDF4-50A (1/2" hardline) | 0.05 | 0.1 | 0.18 | 0.27 | 0.4 | 0.7 | 1.3 | 2.4 |

Visualizing the trend:

- **RG-58**: starts cheap at HF, gets very lossy at UHF. Slope is steep.
- **RG-213**: moderate at HF, manageable at VHF, getting lossy at UHF. Classic balanced choice.
- **LMR-400**: low loss at HF, low at VHF, manageable at UHF. The modern HF/VHF/UHF favorite.
- **Hardline (LDF4-50A)**: lowest loss across the board. Big, expensive, hard to install. Only worth it for very long runs or critical applications.

## Why each cable is what it is

Three factors set a cable's loss:

1. **Conductor diameter and material.** Larger center conductor = lower resistance = lower loss. RG-213 (~0.7 mm center) has lower loss than RG-58 (~0.5 mm center). Silver-plated conductors reduce loss further (silver has slightly lower resistivity than copper, more importantly higher conductivity at the surface where skin-effect current flows).
2. **Shield design.** Single braid (RG-58, RG-8X) has higher resistance than double braid (RG-213, RG-214) which has higher resistance than solid copper tube (hardline). Each step down halves the shield's contribution to loss.
3. **Dielectric.** Foam dielectrics (LMR-400, 9913, 9914) have lower dielectric loss than solid PE (RG-213) and lower stored capacitance per unit length, which slightly reduces conductor current density. Air dielectric (some hardline) is even better.

The cumulative effect: hardline can be 3-4× lower loss than RG-213 at the same frequency, and RG-213 is ~2× lower loss than RG-58.

## Computing total loss

For a cable run of length L feet at frequency f:

**Total matched loss (dB) = (loss per 100 ft from table) × L / 100**

Examples:

- **75 ft RG-213 at 14 MHz**: 0.65 × 75/100 = **0.49 dB**.
- **150 ft LMR-400 at 432 MHz**: 2.7 × 150/100 = **4.05 dB**.
- **100 ft RG-58 at 1296 MHz**: 22.5 × 100/100 = **22.5 dB**. (Don't actually do this.)

## Translating dB to power loss

Useful conversions:

| dB loss | Power retained | Power lost |
|---------|----------------|------------|
| 0.5 | 89% | 11% |
| 1 | 79% | 21% |
| 2 | 63% | 37% |
| 3 | 50% | 50% |
| 4 | 40% | 60% |
| 6 | 25% | 75% |
| 10 | 10% | 90% |
| 20 | 1% | 99% |

So 3 dB is "halve the power"; 6 dB is "quarter the power"; 10 dB is "tenth the power."

## Loss vs. frequency: the slope

For most amateur cables, the increase from 1.8 MHz to 1296 MHz is roughly:

- **RG-58**: 56× (0.4 → 22.5)
- **RG-213**: 47× (0.2 → 9.5)
- **LMR-400**: 37× (0.13 → 4.8)
- **LDF4-50A**: 48× (0.05 → 2.4)

Roughly √(1296/1.8) ≈ 27× would be pure skin-effect; the larger increases reflect dielectric loss starting to matter at high frequencies.

> ⚙️ **Advanced —** Coax loss has two main components: conductor loss (Rₛ × f^0.5 × geometry term) and dielectric loss (tan δ × f). Conductor loss dominates below the "loss-tangent crossover" frequency (typically 1-10 GHz for amateur cables). The conductor loss has a frequency-dependent skin depth: δ = 1/(√(πfμσ)), where σ is the conductor's conductivity and μ is permeability. Skin depth at 1 MHz in copper is 65 μm; at 1 GHz, 2 μm; at 10 GHz, 0.65 μm. The current is confined to this thin surface layer, increasing effective resistance with frequency. Real coax loss formulas integrate the resistance contributions of the inner conductor and outer braid, weighted by their respective current densities; this is the Maxwell-Boltzmann derivation that produces the closed-form coax loss equation.

## Loss vs. SWR: Mismatch makes it worse

The "matched-line loss" values in the table are the *minimum* — the loss you get when the antenna is perfectly matched. If SWR is elevated, the wave travels the lossy line multiple times (forward, reflected, back-reflected), and total loss exceeds the matched-line value.

Section 09-03 covers this. In summary: at SWR 2:1 you get ~10-30% extra loss; at SWR 5:1 you get ~50-100% extra loss; at SWR 10:1 the extra loss can exceed the matched-line value itself.

## How loss matters operationally

For each S-unit (~6 dB) of feedline loss, you lose:

- 6 dB of transmitted signal at the antenna (vs. no loss case)
- 6 dB of receive sensitivity (the antenna's 6 dB-down RX signal is what reaches the rig)
- **Total round-trip impact: 12 dB** ≈ 2 S-units of "what you'd hear vs. what you'd hear with no feedline."

A 200 ft RG-58 run at 144 MHz has 13 dB matched loss = ~2 S-units RX impact, ~2 S-units TX impact = 4 S-units total (the difference between hearing a station S9 and hearing them S5).

This is real signal. Match the cable to the run length and frequency.

## Buying good cable for the run

Decision matrix:

| Run length | Frequency | Recommended cable |
|------------|-----------|-------------------|
| Under 25 ft | Any HF | RG-58 OK; RG-8X better |
| Under 25 ft | VHF | RG-8X |
| Under 25 ft | UHF | RG-213 |
| 25-50 ft | HF | RG-213 |
| 25-50 ft | VHF | LMR-400 |
| 25-50 ft | UHF | LMR-400 |
| 50-100 ft | HF | RG-213 or LMR-400 |
| 50-100 ft | VHF | LMR-400 |
| 50-100 ft | UHF | LMR-400 minimum |
| 100-200 ft | HF | LMR-400 or hardline |
| 100-200 ft | VHF | Hardline preferred |
| 100-200 ft | UHF | Hardline |
| Over 200 ft | Any | Hardline |

These are starting points; specific situations may differ.

## Common loss-related mistakes

- **Reading "1.6 dB/100 ft" and not multiplying by length.** A 200 ft run has 3.2 dB loss, not 1.6 dB.
- **Forgetting the loss is per band.** Same cable has different loss at 14, 28, 50, 144, 432 MHz. Compute each.
- **Treating "low-loss" relative to RG-58 as "lossless."** LMR-400 is much better than RG-58, but at 432 MHz it still has 2.7 dB/100 ft. Not zero.
- **Buying cheap "name-brand-equivalent" cable without testing.** Quality varies enormously. Test critical runs.
- **Old cable assumed to have spec performance.** A 20-year-old cable's loss has likely degraded 30-50% from age (water ingress, jacket UV damage). Re-measure or replace.

## See also

- §10-00 — Chapter overview
- §10-02 — SWR and reflected power
- §10-03 — Mismatch loss (the SWR penalty)
- §10-04 — Power delivered vs lost
- §18 — Coax & connectors (full reference)
- §06-10 — Feedline effects on antenna behavior
- §12-01 — Coax issues troubleshooting
