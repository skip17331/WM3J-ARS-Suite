---
id: 18-05
title: Baluns & Chokes (reference)
chapter: 18
section: 05
level: mixed
status: draft
---

# Baluns & Chokes (reference)

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

This is the **lookup section** for balun and choke specs — sizes, ratios, ferrite mixes, power ratings. The full conceptual treatment lives in §06-12 (when to use each, what common-mode current is, why current baluns matter). Come here when you need to remember "what mix is FT240-43" or "what's the power rating of a typical 4:1 voltage balun."

## Quick selection: what to put where

| Antenna / situation | Recommended balun |
|---------------------|-------------------|
| Coax-fed dipole | 1:1 current (choke) at feedpoint |
| Inverted V | 1:1 current at feedpoint (more critical than flat-top dipole) |
| Off-center-fed dipole (OCFD) | 4:1 current balun at feedpoint |
| Folded dipole | 4:1 voltage or current balun |
| Full-wave loop (quad, delta) | 4:1 current balun (or 75-Ω quarter-wave matching section) |
| End-fed half-wave (EFHW) | 49:1 unun + 1:1 current choke 5 ft down feedline |
| Random wire | 9:1 unun + counterpoise + 1:1 choke |
| Vertical (ground radials) | 1:1 current choke at base (good practice) |
| Vertical (elevated radials) | 1:1 current choke at base (mandatory) |
| Yagi (gamma match) | 1:1 current choke on coax |
| Yagi (folded driven element) | 4:1 current balun |
| Magnetic loop | 1:1 current choke on coax (the loop itself is the matching network) |

## Ferrite material reference

The mix matters as much as the size. For amateur HF/VHF choking and transformer baluns:

| Mix | Frequency range (best Z) | Use case |
|-----|--------------------------|----------|
| Type 31 | 1–50 MHz peak | Best for HF current chokes; high permeability and lossy at HF |
| Type 43 | 1–250 MHz | The all-band HF/lower-VHF go-to; balanced loss + reactance |
| Type 61 | 50–1000 MHz | VHF/UHF chokes |
| Type 73 | 100 kHz–50 MHz | Lower-frequency emphasis (LF/MF/lower HF) |
| Type 77 | 100 kHz–10 MHz | LF/MF — same family as 73 |
| Type 67, 68 | High-Q, RF transformer cores | Inductors, not chokes |
| Iron powder (Mix-2, Mix-6) | LF/HF tuned circuits | Higher Q, less loss — *not* for common-mode choking |

The **rule**: for choking common-mode current, use a **lossy** ferrite (31 or 43). For tuned circuits, use a **high-Q** core (67, 68, or iron powder). They are not interchangeable.

## Common ferrite core sizes

The "FT-N-mix" naming convention: FT = ferrite toroid, N = OD in 1/100 inch (so FT240 is 2.4 in OD), mix = material code.

| Core | OD | ID | Height | A_L (Type 43) | A_L (Type 31) | Typical use |
|------|-----|-----|--------|----------------|----------------|-------------|
| FT82-43 | 0.825 in | 0.520 in | 0.250 in | 470 | 1400 | Small RF transformers |
| FT114-43 | 1.142 in | 0.748 in | 0.295 in | 800 | 2400 | Medium RF transformers |
| FT140-43 | 1.4 in | 0.9 in | 0.5 in | 950 | 2950 | Common 100W EFHW unun |
| FT240-43 | 2.4 in | 1.4 in | 0.5 in | 1075 | 3000 | Large baluns, high-power |
| FT290-43 | 2.9 in | 1.5 in | 0.75 in | 1300 | 3500 | Legal-limit baluns |

A_L is in **nH per turn²** — used to calculate primary-side inductance for transformer baluns.

## Sizing for power

Single-core size limits, approximate continuous SSB:

| Core | Frequency | Approx max power (matched, SSB) |
|------|-----------|----------------------------------|
| FT140-43 | 1.8–30 MHz | 100–200 W |
| FT240-43 | 1.8–30 MHz | 500–1000 W |
| FT240-43 (stacked pair) | 1.8–30 MHz | 1500 W+ |
| FT290-43 | 1.8–30 MHz | 1500 W+ |

For **digital modes (100% duty cycle)**, halve these numbers. A 100 W FT8 station should use FT240-class minimum.

For **legal-limit (1500 W) operation** at high duty cycles, stack two FT240 cores or use FT290 single.

## Common balun designs (build references)

### 1:1 current choke (W2DU-style bead choke)

Slide ~50 ferrite beads (Type 43, "Maxwell beads," typically FT240-43-equivalent or smaller bead variants) over a 6-foot length of small coax (RG-303, RG-400, or PTFE-jacketed RG-58). Heat shrink the assembly into a single weatherproof bundle.

**Choking impedance**: ~3000 Ω across HF.

**Power**: limited by coax (RG-303 fine to 200W; RG-400 fine to 500W; jacketed coax to legal limit).

### 1:1 current choke (coax-on-toroid)

Wind 8–12 turns of coax through a single FT240-43 toroid. The coax is treated as a single transmission line; the magnetic flux through the toroid is from the common-mode current only.

**Choking impedance**: 1000–2500 Ω across 80–10 m.

**Power**: limited by coax (RG-58 fine to ~200W; RG-213 fine to legal limit).

### 4:1 current balun

Two windings on FT240-43 (or FT290-43 for higher power). Bifilar wound, ~14 turns per winding for HF coverage.

**Impedance ratio**: 200 Ω : 50 Ω (or input/output depending on orientation).

**Power**: ~500 W on single FT240; 1500 W on stacked FT240s.

### 9:1 unun

Trifilar wound on FT240-43, 12 turns. Used for random-wire / long-wire antennas with a counterpoise.

**Impedance ratio**: 450 Ω : 50 Ω.

**Power**: ~500 W single core.

### 49:1 unun (EFHW)

Two windings on FT240-43 (or stacked FT240-43 for full power). Primary 2 turns, secondary 14 turns, ratio 1:7 (impedance 1:49). Sometimes wound autotransformer-style with a single tap.

**Impedance ratio**: 2450 Ω : 50 Ω.

**Power**: ~200W single FT140-43; ~500W single FT240-43; 1500W stacked FT240-43.

## Voltage balun vs. current balun (reminder)

| Type | Forces | Common-mode current | Use case |
|------|--------|---------------------|----------|
| Voltage balun | Equal & opposite voltages | Allows it (not suppressed) | OK for symmetric loads only; rare in amateur use |
| Current balun (choke balun) | Equal & opposite currents | Suppressed by high common-mode Z | Default; what you almost always want |

**Almost every amateur installation should use a current balun.** A "balun" sold without specifying current vs. voltage is usually a current balun in modern amateur dealer products, but verify before buying.

## Choking-impedance targets

The minimum target for adequate common-mode suppression:

| Choke quality | Common-mode Z (ohms) | Real-world result |
|---------------|----------------------|-------------------|
| < 500 | Bad | Common-mode current still flows |
| 500–1000 | Marginal | Helps in low-power, symmetric installs |
| 1000–3000 | Good | Solves problems for typical installations |
| 3000–10000 | Excellent | Solid for high-power, asymmetric installs |
| > 10000 | Boutique | Diminishing returns |

A measurement note: choking impedance is **frequency-dependent**. A choke with 5000 Ω at 14 MHz might have 1500 Ω at 1.8 MHz and 800 Ω at 50 MHz. The good news: most amateurs only need adequate choking on the bands they actually use, and a Type 31 or 43 choke is typically broad-band enough to cover all of HF.

## Where to buy / what to use

For typical amateur use:

- **Pre-built quality**: Balun Designs, Palomar, DX Engineering, MFJ (some models). All are competent for legal-limit HF.
- **Kits**: KM4ACK, MyAntennas, various eBay sellers — varying quality. Stick to known brands.
- **Cores in bulk**: Mouser, Amidon, KitsAndParts.com (now Box Vault Industries). FT240-43 in bulk is ~$8 each; the wire and box are $5. A homebrew 1:1 current balun costs $13 in materials.

For VHF/UHF:

- Type 61 ferrite cores; smaller wire; smaller cores. Most amateur VHF/UHF doesn't need explicit choking on coax (the antennas are usually quasi-balanced or fed through gamma matches).

## Common balun problems

- **Saturation at high power**: core gets hot, flux density exceeds the material's saturation point, balun loses transformation. Fix: bigger core or stacked cores.
- **Wrong mix**: a Type 73 core in a 14 MHz application is barely a choke. Always verify mix.
- **Wrong winding count**: a 49:1 with 4 turns instead of 14 has 1/9 of the choking impedance. Count wraps carefully.
- **Common-mode current bypassing the balun**: water in the balun box, the balun grounded to a bonding network that becomes a return path, or feedline running parallel and capacitively coupled. Symptom: choke seems OK but common-mode current still measured downstream.
- **Inadequate weatherproofing**: water in the balun box ruins the ferrite (changes mix properties; can crack with freeze cycles). Drain holes face down; gasket seals; silicone-injected if the user wants belt-and-suspenders.

## See also

- §06-12 — Baluns and chokes (full discussion: when to use each, common-mode theory)
- §18-00 — Coax & connectors overview
- §12-05 — Faulty balun (troubleshooting view)
- §14 — RFI (the problem common-mode current causes)
- §16-03 — Inspections (where balun health gets checked)
