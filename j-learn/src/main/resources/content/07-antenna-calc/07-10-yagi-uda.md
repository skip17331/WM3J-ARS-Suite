---
id: 07-10
title: Yagi-Uda
chapter: 07
section: 10
level: mixed
status: draft
---

# Yagi-Uda

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What it is

The Yagi-Uda (commonly just "Yagi") is a directional beam antenna invented by Hidetsugu Yagi and Shintaro Uda at Tohoku University in 1926. It uses a **driven element** (the only element actually fed) plus **passive parasitic elements** — typically one **reflector** behind the driven element and one or more **directors** in front — to produce a focused beam pointed in the direction of the directors.

Yagis dominate amateur DX and contesting because they offer **substantial gain** (5–14 dBi) and **high front-to-back ratio** (12–25 dB) in a relatively compact, mast-mountable design.

## How it works

Each parasitic element is a wire of approximately half-wave length, but slightly **longer** (reflector) or **shorter** (director) than the driven element. The slight length offset causes the parasitic to **re-radiate** with a phase shift relative to the driven element. With the right spacing, the re-radiated waves add constructively in one direction and destructively in the opposite — producing the directional pattern.

| Element | Length vs. Driven | Effect |
|---------|-------------------|--------|
| Reflector | +5% (longer) | Inductive — phase shift gives forward gain |
| Driven (DE) | reference | Fed by coax; resonant at design freq |
| Director 1 | −5% (shorter) | Capacitive — focuses energy forward |
| Director 2+ | progressively shorter | Each adds a bit more gain (with diminishing returns) |

Spacing matters as much as element length. Typical optimal spacings (in wavelengths):

| Spacing | Optimal value |
|---------|---------------|
| Reflector → Driven | 0.15–0.20 λ |
| Driven → Director 1 | 0.10–0.15 λ |
| Director N → Director N+1 | 0.20 λ |

## Calculator inputs and outputs

The Antenna Workshop calculator (`Yagi-Uda`) takes:

- **Center frequency** (MHz)
- **Total elements** (2 to 6) — including reflector + DE + directors

And returns:

- Reflector, Driven, and per-director element lengths
- Spacing between each element pair
- Total boom length
- Expected forward gain (dBi and dBd)
- Expected front-to-back ratio
- Match recommendation (gamma, hairpin, or 4:1 transformer to 50 Ω)

## Worked example — 3-element 20m Yagi

```
freq = 14.175 MHz
elements = 3 (reflector + DE + 1 director)

λ = 984 / 14.175 = 69.42 ft
DE length = 468 / 14.175 × 0.97 = 32.04 ft (~3% shortening for gamma match)
reflector length = 32.04 × 1.05 = 33.64 ft
director 1 length = 32.04 × 0.95 = 30.44 ft

reflector → DE spacing: 69.42 × 0.18 = 12.50 ft
DE → director 1 spacing: 69.42 × 0.12 = 8.33 ft
boom length: 12.50 + 8.33 = 20.83 ft

Expected gain: ~7.5 dBi (5.4 dBd)
Expected F/B: ~20 dB
Expected feed Z: ~25-35 Ω → gamma match or hairpin to 50 Ω
```

A 3-element 20m Yagi is **20+ ft long on a substantial boom**, weighs ~30 lbs, and turns a 12 ft turning radius — this is why Yagis live on towers, not lawn poles.

## Expected gain by element count

| Elements | Gain (dBi) | Gain (dBd) | F/B (dB) | Boom length (λ) |
|---------:|-----------:|-----------:|---------:|----------------:|
| 2 | 5.5 | 3.4 | 12 | 0.18 |
| 3 | 7.5 | 5.4 | 20 | 0.30 |
| 4 | 9.0 | 6.9 | 22 | 0.50 |
| 5 | 10.5 | 8.4 | 25 | 0.70 |
| 6 | 11.5 | 9.4 | 25 | 0.90 |

Beyond 6 elements gain increases ~0.5–1 dB per added director, with diminishing returns and exploding boom length.

## Common mistakes

- **Element lengths too literal.** The empirical numbers in the calculator are starting points. Your specific build (element thickness, boom material, mounting hardware, height above ground) will shift resonance ±2%. Plan to trim.
- **Wrong feed Z assumption.** Yagis present 25–35 Ω at the driven element. A 4:1 hairpin or gamma match brings it to 50 Ω. Direct 50 Ω feed gives 1.5–2:1 SWR.
- **Skipping the gamma match.** A "T-match" or "hairpin" is also acceptable; the gamma is most common because it's external and adjustable.
- **Insulators between elements and boom.** All-aluminum elements through an aluminum boom (no insulator) is fine — RF current is on the surface, not the interior. Insulators are needed when the boom is metal AND elements are insulated wire (rare).
- **Inadequate boom rigidity.** A 20-ft Yagi boom flexes significantly in wind; add boom support cables (truss) above 18 ft.
- **Neglecting wind survival.** A 5-element 20m Yagi presents ~25 ft² of wind area at full broadside. Ensure tower can handle the load.

> ⚙️ **Advanced —** Modern Yagi designs from W2PV, K1FO, DL6WU, and especially **YU7EF / OK1DFC optimized series** outperform classical "tapered" lengths by 1–2 dB through careful element-length and spacing optimization. For serious builds, run the antenna through **NEC-2 / 4nec2 / EZNEC** modeling rather than relying on closed-form formulas. The calculator's outputs are good for quick estimation; modeling gives precise lengths for your specific element diameters and boom material.

## Build & trim notes

1. **Build to the calculator's dimensions** plus 1 inch trim margin on each element end.
2. **Assemble on the ground** — trim driven element first, then directors (front-to-back), then reflector.
3. **Use a NanoVNA on the gamma match** to find the SWR minimum; adjust gamma capacitor and shorting bar for 50 Ω at center freq.
4. **Lift to operating height before final trim** — close-to-ground patterns and feed Z are different from operating height.
5. **Verify F/B by rotating** the antenna and noting signal-strength differences front vs. back. 18+ dB confirms the design is working.
6. **Add a bird-deterrent** — Yagis attract perching birds, which detune the pattern and create maintenance headaches.

## See also

- §04 — Antennas (theory chapter for parasitic elements)
- §07-00 — Antenna Workshop overview
- §07-15 — NanoVNA Trim Workflow
- §18-04 — Impedance (gamma / hairpin matching math)
- §18-08 — ERP (Yagi gain into the budget)
