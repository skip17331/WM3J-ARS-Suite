---
id: 09-12
title: Loading for Shortened Antennas
chapter: 09
section: 12
level: mixed
status: draft
---

# Loading for Shortened Antennas

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What it is

A loading coil is an inductor inserted in series with a shortened antenna to make it electrically the right length for resonance. Mobile HF whips, attic dipoles, balcony verticals, and **any antenna shorter than its full physical resonant length** uses some form of loading.

The coil "fills in" the missing electrical length. A 33 ft vertical with a base loading coil can resonate at 7 MHz (where a full-size vertical would be 33 ft) — the coil adds reactance equivalent to the missing 32 feet of wire.

The trade-off: loading reduces efficiency. A heavily loaded antenna can be 3–6 dB worse than a full-size one. Position of the loading coil matters significantly — base loading is the easiest mechanically, top loading is the most efficient.

## How it works

A full-size half-wave dipole presents 73 Ω resistive at resonance. A **shortened** dipole presents capacitive reactance — the antenna looks like a (small) capacitor. To bring it back to resonance you add inductive reactance equal to the missing capacitive reactance.

For a horizontal shortened dipole:

```
X_required ≈ Z₀ × tan((π/2) × (1 − physLen/fullLen))
```

Where Z₀ ≈ 50 Ω for a typical dipole feed Z. The closer to full length, the smaller the required reactance.

**Position matters** because the antenna's current distribution is non-uniform. Most of the radiated power comes from the **high-current region near the center**. Putting the loading coil at the base means **all** the current flows through it (lossy); putting it near the tip means **less** current flows through it (low loss but less effective at adding electrical length).

| Position | Inductance needed | Efficiency | Mechanical |
|----------|-------------------|------------|------------|
| **Base loaded** | 1.0× (reference) | poor (3–5 dB loss typical) | easiest — coil is at the bottom |
| **Center loaded** | ~0.6× | best (1 dB loss typical) | moderate — coil between sections |
| **Top loaded** | ~0.3× | excellent (< 0.5 dB loss) | hardest — coil + end-mass at the top |

The calculator's "position factor" embeds these multipliers — base = 3.0×, center = 1.0× (best efficiency), top = 0.5×.

## Calculator inputs and outputs

The Antenna Workshop calculator (`Loading Coil`) takes:

- **Operating frequency** (MHz)
- **Antenna physical length** (ft)
- **Antenna type** — dipole or vertical
- **Coil position** — base, center, or top loaded

And returns:

- Full-size length (no loading needed)
- Reactance to add (Ω)
- Total loading inductance (µH)
- Per-side inductance (for dipoles, half goes in each leg)
- Suggested coil winding spec (turns × form diameter × wire gauge)
- Position-dependent efficiency / loss estimate
- Practical Q (air-core vs. iron-core)

## Worked example — 33 ft vertical loaded for 7 MHz

```
freq = 7.150 MHz
antenna physical length = 33 ft
type: vertical
position: center loaded

full-size length = 234 / 7.150 = 32.7 ft (essentially full size!)
shortening = 33/32.7 = 1.01 (no loading needed!)
```

OK that was a trick — for 7 MHz a 33 ft vertical is full-size. Let's try 40m on a 17 ft mast:

```
freq = 7.150 MHz
antenna physical length = 17 ft
type: vertical (¼λ)
position: center

full-size = 32.7 ft, physical = 17 ft → shortened to 52% of full
X_required = 36 × tan(π/2 × (1 − 0.52))
           = 36 × tan(0.754)
           = 36 × 0.95
           = 34 Ω at 1× position factor
inductance = 34 / (2π × 7.15e6) ≈ 0.76 µH

For a center-loaded vertical, 0.76 µH at the midpoint
Suggested coil: 4-5 turns of #14 on 2" form, close-wound
```

The actual reactance scales nonlinearly with shortening — a 50% shortened antenna needs much more loading than a 25% shortened one. The calculator handles this.

## Common mistakes

- **Base loading because it's easy.** It is — but with 3–5 dB of loss. If you can mechanically support a center-loaded coil (e.g., between two whip sections), the efficiency gain is huge.
- **Air-core coil with insufficient turns.** An air-core coil for HF loading needs many turns (10–30) on a substantial form. Skimpy coils have insufficient inductance and low Q.
- **Iron-core coil for high power.** Ferrite cores saturate at high RF current; for QRP it's fine, but legal-limit operation needs air-core or large-mass ferrite (high-µ #43 or #61 mix in 1+ inch toroids).
- **Forgetting Q.** A loading coil's Q matters — air-core ~150–300 vs. iron-core ~50–100. Higher Q = less loss. Aim for Q > 200 for best efficiency.
- **One coil for a dipole.** A horizontal shortened dipole needs **a coil in EACH leg** with half the inductance per side. Single coil at the feedpoint is wrong.
- **Loaded-antenna bandwidth is narrow.** A 50%-shortened antenna has roughly 1/4 the bandwidth of full-size. Plan to retune (or accept high SWR) at band edges.

> ⚙️ **Advanced —** The cot-approximation formula above is for shortened dipoles where the physical length is at most ~70–80% of full. For very short antennas (under 30% of full length), the formula breaks down because the antenna's input impedance becomes dominated by capacitive reactance and ohmic losses. NEC-2 modeling is essential for very-short HF mobile whips and similar designs.

## Build & trim notes

1. **Compute the required inductance** from the calculator.
2. **Wind the coil** on a 2" PVC form (or ceramic for high-power) with #14 enameled or solid copper wire, close-wound. Use the calculator's turns count as a starting estimate; expect to add or remove turns.
3. **Bench-test the coil** with an L-meter or NanoVNA Q-meter. Verify inductance and Q.
4. **Insert at the chosen position** — for dipoles, identical coil in each leg.
5. **Sweep with NanoVNA** — adjust turns by ±0.5 to fine-tune resonance.
6. **Weatherproof** if outdoors — water in a loading coil is a slow-acting failure mode.

## See also

- §06 — Antennas (theory)
- §09-00 — Antenna Workshop overview
- §09-13 — Trap Design (uses similar coil techniques)
- §17-03 — Reactance (where the coil's X_L comes from)
- §17-11 — Q Factor (why coil Q matters)
