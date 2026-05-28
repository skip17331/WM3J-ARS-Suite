---
id: 14-04
title: Ferrite Selection
chapter: 14
section: 04
level: mixed
status: draft
---

# Ferrite Selection

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A ferrite "bead" or "core" wrapped around a cable adds **lossy impedance** to common-mode currents on the cable, dampening RFI without affecting the differential signal that's supposed to be there. Picking the right ferrite is the difference between a cure and a placebo.

## What ferrite is

Ferrite = a mixture of iron oxide and other metal oxides (zinc, manganese, nickel) sintered into ceramic-like cores. Different formulations ("mixes") have different magnetic properties optimized for different frequency ranges.

The key property is the **complex permeability** μ = μ' − jμ''. The real part (μ') is the inductive contribution; the imaginary part (μ'') is the lossy contribution. For RFI suppression, you want **high μ''** in the frequency range of the noise you're trying to suppress.

## The mixes that matter for amateur radio

| Mix | μ' typical | Best frequency range | Use for |
|-----|-----------|----------------------|---------|
| **#31** | ~1500 | 1–300 MHz | General HF/VHF; the most common HF choke material |
| **#43** | ~800 | 1–250 MHz | HF chokes, baluns; broader range than #31 |
| **#61** | ~125 | 200 MHz – 2 GHz | VHF/UHF |
| **#73** | ~2500 | 1–50 MHz | High-loss HF; popular for power-cord chokes |
| **#75** | ~5000 | 0.1–10 MHz | Audio-frequency chokes; AC mains filtering |
| **#77** | ~2000 | 0.5–50 MHz | LF/HF |
| **#J** | ~5000 | LF audio | Audio chokes |

For most ham work:
- **HF chokes** (1.8–30 MHz) — Mix 31 or 43, or 73 for maximum loss.
- **VHF/UHF chokes** (50–500 MHz) — Mix 61 or 43.
- **Wideband mains filtering** — Mix 75 or 31.

## Forms of ferrite

Same mix, different shapes for different jobs:

### Toroidal cores

Donut-shaped, you wind the cable through the hole. Highest possible inductance per turn. Used for:
- Building 1:1 current baluns and chokes (FT-240-31 or FT-240-43 are the workhorses).
- Wide-range mains chokes.

Sizes: FT-50, FT-114, FT-140, FT-240 (numbers are outer diameter in inches/100). FT-240 is about 2.4" OD.

### Snap-on cores

Hinged plastic case with two ferrite halves inside. Snaps over a cable without disconnecting it. Used for:
- Quick fixes on power cords, USB cables, audio cables.
- Field repair.

Sizes: matched to common cable diameters (5 mm, 10 mm, 13 mm, etc.). Sold cheaply on Amazon in mixed-size boxes (~$15 for 50).

### Beads

Solid ferrite cylinder with a small hole for a single wire. Used for:
- Bypassing single wires inside equipment.
- Component-level filtering on a PCB.

Less common in amateur RFI work; more common in commercial PCB design.

### Tubes

Larger ferrite tubes, often 0.5–2 inches inside diameter. Cable passes through several times. Used for:
- High-current power cables.
- Mains line filtering.

## How many turns?

Common-mode impedance of a ferrite choke scales roughly as:

```
Z ≈ μ × N²
```

where N is the number of turns through the core. Doubling turns roughly quadruples impedance.

Practical numbers for an FT-240-31 toroid at 14 MHz:
- 1 turn: ~500 Ω
- 5 turns: ~5000 Ω
- 10 turns: ~15,000 Ω
- 15 turns: starts to self-capacitance limit; not much more impedance gained

Typical aim: **3000+ Ω of common-mode impedance** at the lowest band of interest.

For HF chokes on coax: 8–10 turns through an FT-240-31 or -43 covers 1.8–30 MHz reasonably.

> **Advanced —** The square-of-turns relationship breaks down at high turn counts because the parasitic capacitance between turns creates a parallel resonance, after which impedance starts to drop. The "self-resonant frequency" of a winding limits how much choking you can get from a single core. For HF, the self-resonant frequency of a 10-turn FT-240-31 is around 30 MHz; above that, choking degrades. To choke higher frequencies, use fewer turns (1-3 for VHF) and a different mix (61 or 43).

## Reading the spec sheets

Manufacturer spec sheets (Fair-Rite, FerroxCube) provide several useful curves:

- **μ' and μ'' vs frequency** — tells you in what range the mix is most lossy.
- **Impedance vs turns** — tells you how many turns to use.
- **Saturation flux density** — important for high-current applications (mains chokes); not usually a concern for signal cables.
- **Curie temperature** — temperature above which the mix loses its magnetic properties (Mix 31 is ~140 °C, well above any typical environment).

For most RFI work, the impedance-vs-frequency curves are what you read.

## Common application examples

### Choking the rig's coax

10 turns of the coax through an FT-240-31 right at the antenna feedpoint. Same again at the rig end of the coax for extra suppression in tough cases.

### Choking a power supply DC cable

5–8 turns through an FT-240-43 or FT-240-31 at the radio's DC input. Helps prevent supply hash from getting into the radio AND prevents transmitted RF from getting into the supply.

### Choking a USB or audio cable

Snap-on ferrite at each end of the cable. For USB, mix 31 or 43. Skip the cheap "Type 31?" snap-ons of unknown mix sold in bulk on eBay; their mix is often unspecified and may not be effective at HF.

### Choking the AC mains entering the shack

Stack of two or three large toroids (FT-240-75 or similar mix) at the mains entry. Each "hot" wire passes through the cores 3–5 times. Doesn't help with AC fundamental (it'd saturate the core); does help with HF hash riding on the mains.

### Choking a long Ethernet cable

For Ethernet over Cat5/6, snap-on ferrites at each end. Use mix 31 or 43. Won't help with shielded Cat6a STP that's already RF-tight — only useful on unshielded twisted pair.

## Mistakes to avoid

- **Wrong mix for the frequency.** A mix-61 ferrite is optimized for VHF/UHF; using it at HF gives weak choking. A mix-31 is optimized for HF; using it at UHF gives weak choking.
- **Too few turns.** A single pass through a snap-on isn't much choking. 3+ turns is the minimum for meaningful impedance.
- **No-name ferrite.** Some imported snap-ons are reportedly low-permeability material masquerading as ferrite. Stick to known brands (Fair-Rite, Würth Elektronik, Mix 31 / 43 / 75 / 77 / 31S sold by Amidon, DX Engineering, Mouser).
- **Using a ferrite as a balun without measurement.** A choke that works at 14 MHz may be useless at 1.8 MHz. Verify with a NanoVNA before declaring victory.

## Where to buy

For US/Canada amateurs:

- **Amidon** — `amidoncorp.com` — the canonical ham source for cores by part number.
- **DX Engineering** — `dxengineering.com` — pre-built choke kits and individual cores.
- **MFJ** — basic cores and snap-ons.
- **Mouser, Digi-Key** — for the obsessive who want exact part numbers from Fair-Rite or Würth.
- **Amazon** — variety packs of snap-ons; check reviews carefully (counterfeit and wrong-mix products are common).

A good starter kit for a new operator:
- 4 × FT-240-31 toroids (~$8 each)
- 4 × FT-240-43 toroids (~$8 each)
- 50-piece snap-on assortment (~$15)

Total: about $80, enough for years of choke building.

## See also

- §12-05 — baluns (use the same ferrite mixes)
- §12-06 — feedline routing (where chokes belong)
- §18-05 — baluns and chokes reference
