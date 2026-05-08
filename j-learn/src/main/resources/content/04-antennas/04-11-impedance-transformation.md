---
id: 04-11
title: Impedance Transformation
chapter: 04
section: 11
level: advanced
status: draft
---

# Impedance Transformation

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A radio's PA wants to see 50 Ω. A real antenna almost never *is* 50 Ω. Every antenna system in this chapter — from a perfect dipole to an end-fed wire to a magnetic loop — has a feedpoint impedance that needs to be transformed to 50 Ω before the radio is happy. **Matching networks** do that transformation.

This section covers the matching-network families: **transformers (ununs, baluns)**, **L-networks**, **PI- and T-networks** (the kind in your tuner), **quarter-wave matching sections**, **gamma matches**, and **stub matching**. Each has cases where it's the obvious choice.

## The matching problem, summarized

You have a load impedance Z_load (your antenna). You want to present 50 Ω to your radio. The mismatch is described by:

- The **resistance ratio** R_load / 50.
- The **reactance** jX_load (positive = inductive, negative = capacitive).

A matching network's job is to transform R_load to 50 *and* cancel jX_load, simultaneously, at the operating frequency. Any matching network is built from elements (capacitors, inductors, transmission-line sections) that do these two jobs in some combination.

## Transformer matching: ununs and baluns

**Unun** = unbalanced-to-unbalanced transformer (changes impedance, both sides single-ended).
**Balun** = balanced-to-unbalanced transformer (handles both impedance ratio and balance).

These are wound on ferrite toroids and use a turns ratio to step impedance:

| Turns ratio | Impedance ratio | Use case |
|-------------|----------------|----------|
| 1:1 | 1:1 | Pure choke balun (current balun); no impedance step |
| 2:1 | 4:1 | Folded dipole, OCFD (200 → 50 Ω) |
| 1:2 | 1:4 | Inverse — feeding a low-impedance load from 50 Ω |
| 3:1 | 9:1 | Random-wire / long-wire vertical (450 → 50 Ω); G5RV variants |
| 4:1 | 16:1 | High-impedance feed; T2FD; some commercial verticals |
| 5:1 | 25:1 | Less common but used in some EFHW designs |
| 7:1 | 49:1 | EFHW (2500 → 50 Ω) — see §04-04 |
| 8:1 | 64:1 | EFHW for higher-impedance installs |

**Transformers are inherently broadband.** A well-built FT240-43-cored transformer covers 80–10 m (or even 160–10) with reasonable efficiency. Their main limits are core saturation at high power and progressive loss at the band extremes.

## L-network: the simplest tunable match

An L-network has just **two reactive components**: one shunt and one series. It can match any complex load to 50 Ω at a single frequency, with the simplest possible component count. Two configurations:

- **High-pass L**: series capacitor + shunt inductor. Matches loads with R > 50 Ω.
- **Low-pass L**: series inductor + shunt capacitor. Matches loads with R < 50 Ω. Also acts as a low-pass filter (reduces harmonic radiation).

The "low-pass L" is what most amateur tuners use. The components are tunable (a roller inductor and a variable capacitor), so the same network covers a range of impedances and frequencies.

> ⚙️ **Advanced —** For a load of R + jX (where R < 50 Ω), the low-pass L solves: Q = sqrt((50/R) − 1); X_L = R × Q − X (series inductor, with the existing X cancelled); X_C = 50 / (Q × (1 + (1/Q²))) (shunt capacitor). For R > 50 Ω the formulation flips. The L-network's Q is fixed by the impedance ratio — Q ≈ sqrt((R_high / R_low) − 1). Higher Q means narrower bandwidth (good for filtering, bad if you wanted broadband).

## PI- and T-networks: the antenna tuner

A typical antenna tuner uses a **PI-network** (capacitor — inductor — capacitor, like the Greek letter Π) or a **T-network** (capacitor — inductor — capacitor, in series-shunt-series form, looking like a T).

| Network | Pros | Cons |
|---------|------|------|
| L-network | Simplest; lowest loss when properly designed | Fixed Q; can't always match very large impedance ratios in tight space |
| PI-network | Wide matching range; harmonic filtering; classic vacuum-tube PA output | Two capacitors and an inductor (more complex) |
| T-network | Most popular in commercial amateur tuners; widest range | Can produce pathologically high circulating currents in some matches; some "matches" radiate from the network itself |

A T-network tuner can present 1:1 SWR to the radio for *almost any* load — but the match might be lossy (10–30% loss is possible if you're matching a wildly bad load like a 5:1 SWR antenna over short feedline). **A 1:1 SWR shown on the rig does not mean efficient power transfer. It means the impedance presented to the rig is 50 Ω. The losses might be in the tuner.**

## Quarter-wave matching section

A quarter-wavelength of transmission line transforms impedances via:

**Z_in = Z₀² / Z_load**

Pick a coax with the right Z₀ to match a known antenna impedance. Common amateur examples:

- **Folded dipole + 75 Ω coax**: a folded dipole is ~280 Ω. A quarter wave of 75 Ω gives Z_in = 75² / 280 = 20 Ω. Doesn't match 50, so this isn't useful by itself.
- **Quad antenna + 75 Ω coax**: a quad is ~120 Ω. A quarter wave of 75 Ω gives Z_in = 75² / 120 = 47 Ω — almost perfect 50 Ω match!
- **75 Ω matching section for a delta loop** (similar to quad).

The classic "75 Ω matching section" for a quad or delta is exactly this — a quarter-wave of 75-Ω coax (using its actual VF for length calculation) at the antenna feedpoint, with 50 Ω coax from there back to the rig.

## Gamma match

A gamma match feeds an antenna *off-center* through a series capacitor and an exposed inner conductor that capacitively couples to the antenna element. Mostly used on Yagis (single driven element fed by a gamma rod parallel to the boom). It's not used on wire dipoles in amateur practice.

The gamma's appeal: it's mechanically simple on metal Yagi elements (no break in the element to install a balun) and adjustable. The downside: it's slightly asymmetrical (one side coupled, one side direct), so common-mode currents need management.

## Stub matching

A **shorted or open stub** of transmission line, attached at a specific point along the main feedline, presents a specific reactance that cancels the load reactance at that point. Result: 50 Ω at that point, propagating cleanly down the rest of the line.

Stub matching is design-intensive (you need to know the load impedance precisely) but uses no lumped components, which is useful at VHF and UHF where lumped components have parasitic problems. Most amateur stub matches use a quarter-wave shorted stub.

## Which one to use

| Scenario | Best matching scheme |
|----------|---------------------|
| Resonant dipole or vertical, near 50 Ω naturally | None — feed directly with coax + 1:1 choke balun |
| Quad / delta loop (~120 Ω) | Quarter-wave 75-Ω matching section |
| Folded dipole or OCFD (~200–300 Ω) | 4:1 balun |
| EFHW (~2500 Ω) | 49:1 unun |
| Random wire / long wire | 9:1 unun + counterpoise |
| Multiband antenna with non-resonant feedline | T- or PI-network tuner at the rig |
| Yagi with non-50-Ω driven element | Gamma match, hairpin match, or beta match |
| VHF/UHF where lumped elements are too small | Stub or transmission-line matching |

## Loss vs. SWR vs. matching efficiency

Three numbers, often confused:

- **SWR at the rig**: what your meter shows. Tells you whether the rig will deliver full power.
- **SWR at the antenna**: what's actually happening at the wire. Determines feedline mismatch loss.
- **Matching efficiency**: fraction of power that makes it through the matching network. A perfect match with high SWR through a high-loss tuner can be 70% efficient (1.5 dB loss).

A radio shows a "1:1 match" because the rig sees 50 Ω. That tells you nothing about how much power got to the antenna. **Always measure SWR at the antenna feedpoint at least once per installation** to verify the matching system is doing its job.

## Common mistakes

- **Using a tuner to "fix" a bad antenna.** A tuner makes the rig happy. It does not make the antenna better. If your antenna is dumping power into ground losses, the tuner can't help.
- **Wrong unun ratio.** Built a 49:1 EFHW unun for an installation that wants 64:1, or vice versa. Symptoms: high SWR you can't tune out by changing wire length.
- **Quarter-wave matching section using wrong velocity factor.** RG-59 and RG-11 75-Ω coax have different VFs; cut to the right physical length for the actual cable.
- **No common-mode choke even with a "balun."** Many "baluns" are actually just impedance transformers (voltage baluns) and don't break common-mode current. Add a current choke as well.
- **Matching network designed at the wrong frequency.** Resonant matching networks (quarter-wave stubs, fixed-tap autotransformers) only work at one frequency. For multi-band ops, use a tuner or a broadband transformer.

## See also

- §04-09 — Smith charts (the design tool for matching networks)
- §04-12 — Baluns and chokes (specific transformer types)
- §04-10 — Feedline effects (length affects observed Z)
- §04-04 — EFHW (the canonical unun-matched antenna)
- §15 — Formula appendix
