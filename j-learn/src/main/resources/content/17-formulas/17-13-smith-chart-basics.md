---
id: 17-13
title: Smith Chart Basics
chapter: 17
section: 13
level: advanced
status: draft
---

# Smith Chart Basics

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The Smith chart is a polar plot of the reflection coefficient Γ. It maps every possible complex impedance onto a unit circle, with constant-resistance and constant-reactance contours superimposed. It's the universal tool for visualizing impedance, mismatch, and matching networks.

If §04-09 explains the picture and §04-11 explains the transformations, this section gives the underlying math.

## Equations

The Smith chart is parameterized by:

```
Γ = (Z_L − Z_0) / (Z_L + Z_0)
Γ = ρ × e^(jφ)         (polar form: magnitude ρ, phase φ)
```

Normalized impedance:

```
z = Z / Z_0   (typically Z_0 = 50 Ω)
z = r + jx    (real + reactive parts, normalized)
```

Inverse mapping (from Γ back to z):

```
z = (1 + Γ) / (1 − Γ)
```

The unit circle on the chart corresponds to all impedances with magnitude \|Γ\| = 1 (totally reflective: open, short, or pure reactance).

## Key chart features

| Chart feature | What it represents |
|---------------|---------------------|
| Center point | Perfect match (Γ = 0, Z = Z_0) |
| Outer circle | Pure reactance (or open / short) |
| Right-side axis | Pure resistance (Γ real, X = 0) |
| Top half | Inductive impedance (X > 0) |
| Bottom half | Capacitive impedance (X < 0) |
| Constant-r circles | Lines of constant resistance |
| Constant-x circles (arcs) | Lines of constant reactance |
| Constant-\|Γ\| circle | All impedances giving the same SWR |

A point on the right axis with Γ = +1 is an open (Z = ∞). On the left axis with Γ = −1 is a short (Z = 0). At the center (Γ = 0) is the perfect match (Z = Z_0).

## Worked example — plotting an antenna feed

Antenna at 7.150 MHz reads R = 75 Ω, X = +35 Ω on a 50 Ω line. Normalized:

```
z = 75/50 + j35/50 = 1.5 + j0.7
```

Plot at the intersection of the r = 1.5 circle and the x = 0.7 arc, in the upper half (positive reactance = inductive).

Computing Γ:

```
Γ = ((1.5 + j0.7) − 1) / ((1.5 + j0.7) + 1)
  = (0.5 + j0.7) / (2.5 + j0.7)
  
|0.5 + j0.7| = √(0.25 + 0.49) = √0.74 ≈ 0.860
|2.5 + j0.7| = √(6.25 + 0.49) = √6.74 ≈ 2.596
|Γ| = 0.860 / 2.596 ≈ 0.331

SWR = (1 + 0.331) / (1 − 0.331) ≈ 1.99
```

So this antenna gives ~2:1 SWR. On a Smith chart you'd see the impedance at radius \|Γ\| ≈ 0.33 from center.

## Movement along a transmission line

A length of lossless transmission line rotates the impedance point clockwise around a circle of constant \|Γ\|, by an angle:

```
Δφ = 2 × 2π × ℓ / λ_line   (radians, where ℓ is line length and λ is in the cable)
```

A half-wavelength rotation is 360° — the impedance returns to itself. A quarter-wavelength rotates 180° — equivalent to "the impedance you see at the far end is its mirror across the center."

This is *why* a quarter-wave matching stub of √(Z_a × Z_b) characteristic impedance transforms Z_a to Z_b: the rotation flips the impedance ratio across the chart center.

## Series component additions

Adding a series component to an existing impedance moves the point along a constant-resistance circle:

| Component | Direction on chart |
|-----------|---------------------|
| Series inductor (+jX_L) | Up (clockwise on r-circle, toward inductive top) |
| Series capacitor (−jX_C) | Down (counter-clockwise on r-circle, toward capacitive bottom) |

A series-component matching network is a sequence of jumps along constant-r circles.

## Parallel component additions

For parallel components, you work in admittance Y = 1/Z. The Smith chart has an admittance grid (often shown rotated 180°). Adding a parallel component moves along a constant-conductance circle:

| Component | Direction on admittance chart |
|-----------|---------------------|
| Parallel inductor | Down (toward inductive on Y chart) |
| Parallel capacitor | Up (toward capacitive on Y chart) |

L-network matching is a sequence of one series-component jump and one parallel-component jump.

## Common mistakes

- **Forgetting to normalize.** All Smith chart math assumes z = Z / Z_0. Plot z, not Z directly. Multiply by 50 at the end if you want absolute Z.
- **Confusing the impedance and admittance grids.** Most charts show one or the other in red/blue. Identify which you're using before reading values.
- **Treating line losses as zero.** A real lossy line slightly shrinks the constant-\|Γ\| circle as you move along it (the impedance spirals inward, not just rotates). For ham work the spiral is small and usually ignored.
- **Reading wavelength scales as fractions of frequency.** The "wavelengths toward generator/load" scales on a Smith chart are wavelengths *in the line*, not free-space wavelengths. Multiply by VF.

> ⚙️ **Advanced —** The Smith chart was patented by Phillip Smith at Bell Labs in 1939 as a graphical computational aid. With software (NanoVNA, simNEC, etc.) doing the math automatically, the chart is now mostly used as a visualization tool: "where is my impedance, how far from the center, what direction does this matching component move me?" The intuition is the value, not the slide-rule precision.

## See also

- §04-09 — Smith Charts (the picture-side explanation)
- §04-11 — Impedance Transformation (matching networks via Smith chart)
- §17-04 — Impedance (the Z = R + jX form)
- §17-07 — SWR (the radius-from-center reading)
