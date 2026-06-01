---
id: 06-01
title: Smith Charts
chapter: 06
section: 01
level: advanced
status: published
---

# Smith Charts

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The Smith chart is the antenna engineer's most-used graphical tool. It looks like circles drawn inside circles, but it is in fact a *one-glance display of every relevant property of a transmission-line/antenna system at a chosen frequency*: impedance, reflection coefficient, SWR, return loss, electrical length, and what a matching network needs to look like to fix any of those.

If you read NanoVNA traces or use modern antenna analyzers, you are already reading a Smith chart whether you know it or not. This section turns that reading into understanding.

## What the chart represents

A Smith chart is a **2D plot of the complex reflection coefficient**, Γ (gamma). Γ has a real part and an imaginary part; both range from −1 to +1; and |Γ| ≤ 1 for any passive network.

The chart maps:

- The **center** of the chart = perfect match. Γ = 0. Your impedance equals Z₀ (50 Ω in amateur use). SWR = 1:1.
- The **right edge** of the chart = open circuit. Γ = +1. Infinite Z. SWR = ∞.
- The **left edge** = short circuit. Γ = −1. Z = 0. SWR = ∞.
- **Anywhere on the horizontal centerline** = pure resistance. To the right of center, R > 50 Ω. To the left, R < 50 Ω.
- **Above the centerline** = inductive reactance (positive jX).
- **Below the centerline** = capacitive reactance (negative jX).

The whole thing is normalized — every impedance is expressed as a multiple of Z₀ (50 Ω). A point at "1.0" is 50 Ω. A point at "2.0" is 100 Ω. A point at "0.5" is 25 Ω.

## The two grids

A Smith chart has two overlaid sets of curves:

1. **Constant-resistance circles**: circles tangent to the right edge, each labeled with a specific R/Z₀ value. The big circle through the center is r = 1 (50 Ω). To the right of it: r > 1. Inside it: r < 1.
2. **Constant-reactance arcs**: arcs that pass through the right edge of the chart, each labeled with a specific X/Z₀ value. Above the centerline are positive (inductive); below, negative (capacitive); the centerline itself is X = 0.

To read an impedance: find where the constant-r circle and constant-x arc cross. The point is your impedance.

## What you read off it directly

| Quantity | Where to look |
|----------|---------------|
| **Impedance** at the measurement point | Read R and X off the grid lines |
| **SWR** | Distance from center, along the horizontal axis. The SWR circle passes through center-to-edge ratio = SWR. There are also concentric scales printed at the bottom of most printed charts. |
| **Reflection coefficient magnitude \|Γ\|** | Distance from center, normalized so center = 0 and edge = 1 |
| **Return loss** | Same scale as \|Γ\|, in dB: RL = −20 log₁₀\|Γ\| |
| **Phase angle of Γ** | Angular position around the chart |

A NanoVNA's "Smith chart" trace is the locus of these complex Γ values across the swept frequency. A short antenna under-resonance will show a trace that starts in the lower-left (capacitive, low R), passes through the centerline (resonance, real impedance), and continues into the upper-left (inductive). Where the trace passes nearest the center is your best-match frequency.

## Reading SWR from a trace

Every concentric circle around the chart center is **a constant-SWR circle**. The chart center is SWR = 1:1; the edge is SWR = ∞.

Common SWR circle landmarks:

| SWR | Distance from center |
|-----|---------------------|
| 1.5:1 | About 0.2 of full radius |
| 2.0:1 | About 0.33 of full radius |
| 3.0:1 | About 0.5 of full radius |
| 5.0:1 | About 0.67 of full radius |
| 10:1 | About 0.82 of full radius |

A modern analyzer draws these reference circles for you. The 2:1 SWR circle is the most-used.

## Why we use it

Three reasons the Smith chart has not been replaced in a hundred years:

1. **A series-reactance change moves you along an arc**, not in a confusing way. Adding a series capacitor moves you down an arc; adding a series inductor moves you up. **A shunt-element change moves you along an arc on the *admittance* (mirror) chart**, which has the same shape mirrored left-right.
2. **An impedance transformation through a length of transmission line is just a rotation around the chart center**, at a rate of 360° per half-wavelength. Walking down 1/8 of a wavelength of feedline rotates you 90° clockwise around the chart. This makes feedline analysis purely geometric.
3. **Matching network design becomes a path-planning problem**: start at the antenna's complex Γ, end at the chart center, moving by feedline rotation and component-jumps along the right kinds of arcs. Once you know which arcs each kind of component traces, you design L-networks and PI-networks by drawing.

## Reading a NanoVNA trace

Practical NanoVNA reading workflow:

1. **Where on the chart is the trace?** Center = good. Right side = high R, possibly open. Left side = low R, possibly short. Upper half = inductive. Lower half = capacitive.
2. **Does the trace pass through the centerline (X = 0)?** That's a *resonant* frequency. Note the frequency and the R value at that point.
3. **Does it pass near the center?** That's a *matched* frequency (R near Z₀ *and* X near 0). The closest-to-center point in your sweep is your best match.
4. **What's the trace shape?** A small loop near center = narrow resonance. A long sweep across the chart = broad antenna. A trace that wanders without passing center = a non-resonant or compromised antenna.

## Common patterns to recognize

| Trace shape | Antenna behavior |
|-------------|------------------|
| Tight loop near center, crossing centerline | Properly resonant, correct impedance — well-matched antenna |
| Loop crossing centerline but offset to right or left | Resonant but wrong impedance — needs matching transformer |
| Trace stays in upper half | Inductive — antenna is electrically too long |
| Trace stays in lower half | Capacitive — antenna is electrically too short |
| Trace traces a near-perfect circle around the chart, full size | Mostly feedline transformation — the antenna is mismatched and the feedline is rotating the result |
| Trace is a small bunch on the right edge | Open or near-open — antenna conductor is broken or disconnected |
| Trace is a small bunch on the left edge | Short — coax shield touching center conductor, or shorted balun |

> **Advanced —** The Smith chart's mathematical magic is the conformal mapping z → (z−1)/(z+1) from the right half of the complex impedance plane onto the unit disk in the Γ plane. Resistance circles in z-space map to circles tangent to the right edge in Γ-space; reactance lines map to arcs passing through the right edge. The fact that a length of transmission line transforms impedance via Γ_new = Γ_old × e^(−j2βℓ) is what makes feedline transformation a clean rotation. Phillip Smith published the chart in 1939 (*Electronics* magazine); the pre-Smith approach involved nomographs and was vastly more painful.

## When you don't need the Smith chart

For most amateur antenna work, the **SWR vs frequency trace** alone gives you everything you need: where's the dip, how deep is it, how broad is it. The Smith chart adds value when:

- You're designing a matching network and need to know what kind of element (series cap, shunt inductor) to add.
- You're troubleshooting an antenna with confusing SWR that doesn't dip cleanly anywhere — the Smith chart shape tells you the failure mode (open, short, severely mistuned, severely mis-impedance-d).
- You're computing the impedance presented to your tuner from a not-50-ohm antenna across a length of feedline.

## See also

- §06-02 — Feedline effects (Smith chart shows them as rotations)
- §06-03 — Impedance transformation (Smith chart designs the network)
- §12 — High-SWR troubleshooting (Smith chart traces help diagnose)
- §17-13 — Smith chart formulas (the math, in the formula appendix)
