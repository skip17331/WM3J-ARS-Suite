---
id: 18-01
title: Coax Types
chapter: 18
section: 01
level: simple
status: draft
---

# Coax Types

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

This section is the type-by-type reference for the common coax cables in amateur use. Each entry covers physical construction, electrical specs, typical use cases, and notes on quality variation. Loss values per band are in §18-02; velocity factors are in §18-03.

## Quick comparison

| Cable | Outer dia | Z₀ | Dielectric | VF | Power @ 30 MHz | Cost / 100 ft | Best use |
|-------|-----------|------|------------|-----|----------------|---------------|----------|
| RG-58 | 0.195 in | 50 Ω | Solid PE | 0.66 | 700 W | $30 | Short patch (< 25 ft) |
| RG-58A/U | 0.195 in | 50 Ω | Solid PE | 0.66 | 1000 W | $40 | Same; mil-spec variant |
| RG-8X / Mini-8 | 0.242 in | 50 Ω | Foam PE | 0.78–0.84 | 1500 W | $40 | Patch cable, short antenna run |
| RG-8 / RG-213 | 0.405 in | 50 Ω | Solid PE | 0.66 | 5000 W | $90 | HF antenna run, classic |
| RG-214 | 0.425 in | 50 Ω | Solid PE | 0.66 | 5000 W | $250 | Mil-spec, double silver braid |
| 9913 (Belden) | 0.405 in | 50 Ω | Air-PE / foam | 0.84 | 4000 W | $120 | Lower-loss alternative to RG-213 |
| 9914 (Belden) | 0.405 in | 50 Ω | Foam | 0.82 | 4000 W | $110 | Improved 9913 |
| LMR-400 | 0.405 in | 50 Ω | Foam | 0.85 | 4000 W | $90 | Modern HF/VHF favorite |
| LMR-400-DB | 0.420 in | 50 Ω | Foam | 0.85 | 4000 W | $130 | Direct-burial-rated |
| LMR-400-UF | 0.405 in | 50 Ω | Foam | 0.85 | 4000 W | $115 | Ultra-flex stranded center |
| LMR-600 | 0.590 in | 50 Ω | Foam | 0.87 | 8000 W | $190 | Long VHF/UHF runs |
| LDF4-50A | 0.50 in | 50 Ω | Foam | 0.88 | 7500 W | $300 | Hardline; long & permanent |
| LDF5-50A | 0.875 in | 50 Ω | Foam | 0.89 | 18000 W | $750 | Big hardline; broadcast/EME |
| RG-6 (75 Ω) | 0.275 in | 75 Ω | Foam | 0.83 | n/a (low-power) | $20 | TV; sometimes amateur 75-Ω uses |
| 450 Ω ladder line | n/a | 450 Ω | Air | 0.91 | 5000 W | $40 | Balanced feedline |

(Power figures are approximate continuous SSB at sea level; high-altitude or full-duty digital reduces these significantly. Costs are typical 2026 retail for 100 ft.)

## RG-58

The smallest common 50 Ω coax. Solid polyethylene dielectric, single-braid copper shield, PVC jacket. Outside diameter about 0.2 inches.

**Use cases**: short interconnects, test cables, patch cables under 25 ft, low-power VHF/UHF mobile installs.

**Avoid**: anything over 25 ft at HF if you have RG-8X available; anything over 15 ft at VHF/UHF; any high-power application (>200 W); outdoor permanent runs (the small jacket cracks under UV faster than larger cables).

Variants:
- **RG-58/U**: standard commercial.
- **RG-58A/U**: stranded center conductor; slightly more flexible.
- **RG-58C/U**: polyethylene jacket (higher temp tolerance) instead of PVC.

## RG-8X / Mini-8

The "thin RG-8" — RG-8 form factor reduced to ~1/4 inch outer diameter. Foam polyethylene dielectric (so VF higher than solid PE), single shield (sometimes double in premium versions), PVC jacket.

**Use cases**: shack patch cables, antenna runs from desk to nearby antenna (under 50 ft), portable / field-day installs where flexibility and weight matter.

**Avoid**: long permanent runs (the loss adds up), VHF/UHF over 50 ft, anything that needs serious weatherproofing (jacket is thinner than RG-213).

Belden 9258 is a quality version; "RG-8X equivalent" from various sellers ranges widely in actual quality.

## RG-8 / RG-213

The classic. About 0.4-inch OD, solid PE dielectric, copper braid shield, PVC jacket. Available from every amateur supplier, every electrical wholesaler, every milsurp warehouse. Has been the standard amateur HF antenna feedline since the 1950s.

**Use cases**: HF antenna runs of any length, VHF runs up to ~100 ft, anything where you want a known-quantity cable.

**Variants**:
- **RG-8/U**: original Mil-C-17 type. Sometimes called "old RG-8."
- **RG-213/U**: the modern milspec successor. Same impedance, same diameter; better quality control on jacket and dielectric.
- **RG-214**: like RG-213 but with **double silver-plated braid**. About 1 dB/100ft lower loss at 144 MHz; ~3x the cost. Good when loss matters and budget allows.

For amateur use, **RG-213 is the answer** unless you've specifically bought RG-214 for low-loss.

## LMR-400 family (Times Microwave)

The modern foam-dielectric replacement for RG-213. Same outer diameter, foam-PE dielectric (so 0.85 VF instead of 0.66), copper-clad aluminum solid center conductor, foil + braid shield. About 30% lower matched loss than RG-213 at the same frequency.

**Use cases**: HF and VHF antenna runs of any length; the most common modern choice for amateur installs.

**Variants**:
- **LMR-400**: standard. PE jacket, suitable for outdoor exposure (UV-stable for ~10-15 years).
- **LMR-400-DB**: direct-burial. PE jacket with anti-water-tracking compound; foam stays dry when buried (standard LMR-400 absorbs over years if buried).
- **LMR-400-UF**: ultra-flex. Stranded center conductor, more flexible but slightly higher loss than solid-center.
- **LMR-400-LLPL**: low-loss low-power. Same loss numbers; reduced power rating; slightly lower cost.

There are many "LMR-400 equivalent" cables (Davis Buryflex, Times Microwave own variants, Talley Communications, Andrew/CommScope). Quality varies; stick with name-brand for buried or rooftop installs where re-doing the run is expensive.

## LMR-600

A bigger sibling of LMR-400 — 0.6-inch outer diameter. About 30% lower loss than LMR-400 at the same frequency. Stiffer; harder to route; more expensive.

**Use cases**: long VHF/UHF runs (>100 ft on 144 MHz, >50 ft on 432 MHz), high-power VHF/UHF stations, EME setups, contest stations with long tower-to-shack runs.

**Avoid**: short HF runs (overkill); applications needing flexibility (LMR-600 has a 6-inch minimum bend radius).

## Hardline (Andrew/CommScope LDF, FlexNet)

Semi-rigid coax with a corrugated solid copper or aluminum outer conductor (not braid) and foam dielectric. Outer diameters from 1/4 inch to 1.5+ inches.

**Common amateur sizes**:
- **LDF4-50A**: 1/2-inch hardline. ~3 dB/100 ft at 432 MHz.
- **LDF5-50A**: 7/8-inch hardline. ~1.7 dB/100 ft at 432 MHz.

**Use cases**: very long runs (>200 ft), high-power VHF/UHF, EME stations, satellite ground stations, broadcast facilities, professional installs.

**Avoid**: anything where you need flexibility (hardline has a 5-15 inch minimum bend radius depending on size); short runs (the cost rarely justifies the small additional gain over LMR-400 at HF).

**Connectors**: hardline uses specialized connectors (Type N hardline-specific, or Andrew's proprietary connectors). They cost $30-100 each and require specific tools to install. Plan for this in budget.

> ⚙️ **Advanced —** Hardline's lower loss comes from three factors: (1) larger outer conductor diameter reduces resistive loss in the shield; (2) foam dielectric has lower dielectric loss tangent than solid PE; (3) the closed solid outer conductor has lower leakage and stray inductance than braided shields. The cost is mechanical: hardline cannot be flexed, must be supported every few feet, and connectors are precision components. The result is broadcast-quality feedline applied to amateur use — the same cable that runs from a TV transmitter's PA to its antenna goes from your IC-7610 to your tower-top Yagi.

## Old / less-common types you might encounter

- **RG-9, RG-11**: 75-Ω cables, used in TV. RG-11 is occasionally used for amateur 75-Ω matching sections (quad antennas).
- **RG-59**: 75 Ω, smaller than RG-11. TV head-end use.
- **9914**: Belden's improved version of 9913, with foam dielectric (more water-resistant than 9913's air-spaced design).
- **RG-217, RG-218**: very large solid-dielectric high-power cables. Mostly displaced by hardline.
- **Heliax**: Andrew's brand name for hardline. Generic; "1/2-inch heliax" usually means LDF4-50A or equivalent.
- **CATV trunk cable** ("hardline cable TV"): 75-Ω hardline used by cable TV operators. Sometimes available used; rarely worth it for amateur 50-Ω applications.

## Buying coax

Three sources, each with tradeoffs:

| Source | Pros | Cons |
|--------|------|------|
| Amateur dealer (DX Engineering, Ham Radio Outlet, Universal Radio) | Quality known; correct connectors available; supports the hobby | Higher prices; some markup |
| Industrial wholesale (Anixter, Graybar, Mouser, Digi-Key) | Best prices on quantity; full Times Microwave or Belden product lines | Need to specify exactly; minimum quantities sometimes |
| Surplus / used / eBay | Lowest prices | Quality unknown; possibly old (rubber jackets brittle); possibly the wrong type relabeled |

For a single antenna run, amateur dealer is fine. For a multi-tower installation, wholesale is significantly cheaper.

**Test your coax** when it arrives. Verify the printed length matches the actual length (some sellers cut short). Sweep a section to confirm matched-line loss is in spec. Check the connector terminations on pre-made assemblies.

## Common mistakes

- **Using RG-58 for "everything."** Lossy on long runs; UV-fragile outdoors.
- **Using RG-8X for outdoor permanent runs.** The thinner jacket cracks faster than RG-213's.
- **Buying "RG-58 mil-spec" because it sounds better.** It's the same RG-58 with quality-controlled production. Unless you're meeting an actual milspec, the upgrade isn't worth 30%.
- **Mismatched cable between sections.** A 75-Ω section in an otherwise 50-Ω run creates SWR bumps. Use 75-Ω only as a deliberate matching transformer.
- **Old surplus coax.** Rubber-jacketed RG-58 from 1970 is brittle. PE-jacketed cables from the 90s are usually fine.
- **Ignoring direct-burial spec.** Standard LMR-400 buried in wet soil will degrade in 3-5 years; LMR-400-DB is rated for it.

## See also

- §18-00 — Overview
- §18-02 — Loss tables (the per-band loss numbers for each cable here)
- §18-03 — Velocity factor (electrical-length implications)
- §18-04 — Connectors (which connectors fit which cables)
- §04-10 — Feedline effects (the *why* of coax choice)
- §16-04 — Coax replacement (when to replace)
