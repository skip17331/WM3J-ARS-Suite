---
id: 05-14
title: Modeling Concepts
chapter: 05
section: 14
level: advanced
status: draft
---

# Modeling Concepts

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Antenna modeling lets you predict, in advance, what an antenna will do. Pattern shape, elevation angle, feedpoint impedance, current distribution — all without putting wire in the air. Modern modeling tools are accurate enough that for any new wire antenna, **you should model it before you build it** if you possibly can. An hour with EZNEC, MMANA, or 4nec2 will save you days of trial-and-error in the yard.

This section covers what NEC-based modeling actually does, what tools are available, what they're good and bad at, and how to read the results without being deceived.

## What NEC is

**NEC** (Numerical Electromagnetics Code) is a method-of-moments solver for antennas made of thin wires. Originally developed at Lawrence Livermore in the late 1970s for military and government use, it became open-source in NEC-2 (1981), with a heavily-improved closed-source NEC-4 following. Most amateur tools wrap NEC-2.

NEC works by:

1. Taking your antenna geometry (a list of wires, each defined by start coords, end coords, and segmentation).
2. Discretizing each wire into many short segments.
3. Setting up a matrix equation for the currents in all segments such that the boundary conditions (driven by your transmitter, with no current at wire ends) are satisfied.
4. Solving the matrix.
5. From the resulting current distribution, computing the radiated field at any direction and the feedpoint impedance.

Output: the things you actually want — feedpoint Z, gain in any direction, elevation pattern, azimuth pattern, current distribution, near-field strengths.

## Common amateur modeling tools

| Tool | Cost | Strengths | Weaknesses |
|------|------|-----------|------------|
| **EZNEC** | Free Pro+ legacy version (recently released as freeware after Roy Lewallen's retirement) | The classic, highly polished UI; great for amateur use; the standard textbook tool | Windows only; NEC-2 underneath (limits to thin wires far from each other) |
| **MMANA-GAL** | Free | Excellent UI; easy geometry entry; nice patterns | NEC-2 limited; slightly more limited optimization tools |
| **4nec2** | Free | Most flexible; built-in optimizer; can handle complex geometries | UI is dense and intimidating; learning curve |
| **AutoEZ** | Inexpensive paid; companion to EZNEC | Adds parametric variation and optimization to EZNEC; valuable if doing serious design | Same engine as EZNEC + paid layer |
| **MININEC** | Free, very old | Light; works on older hardware | Limited segment count; less accurate than NEC-2 |

For most amateur work, **EZNEC or MMANA-GAL** is the right tool. 4nec2 if you want optimization or unusual geometries.

## What modeling is good at

- **Pattern shape and gain** for thin-wire antennas in free space or over typical ground. Accurate to ~0.5 dB for properly-segmented models.
- **Feedpoint impedance** for resonant and near-resonant antennas. Accurate to ~10% for typical installations.
- **Comparative ranking** of designs: "is the inverted V better than the dipole at this height?" — modeling answers this reliably even when absolute numbers are off.
- **Element-spacing studies** for Yagis, phased arrays, etc.
- **Predicting effects of nearby objects**, if you bother to include them in the model (e.g., adjacent wires, tower legs).
- **Optimization**: given a target (max gain at 14° elevation, say), the modeler can iterate element lengths/spacings to reach it.

## What modeling is bad at

- **Antennas with thick conductors close together** (cage dipoles, fat-element Yagis with element diameter > 0.005 λ). NEC-2 has known issues here; NEC-4 fixes most of them.
- **Antennas with traps and lumped reactances**, unless you correctly include the trap as a loaded segment. Done wrong, the model gives misleading results.
- **Magnetic loops** (very small loops where the model's "thin wire" assumption gets stretched).
- **Real ground losses for low antennas**, especially below 0.1 λ. The Sommerfeld solver helps; the Reflection-Coefficient method overestimates gain for low antennas.
- **Common-mode current effects on the feedline**. Most models assume an ideal feed and don't include the coax shield as a radiator. If your antenna's pattern depends on common-mode currents (and many real-world ones do), modeling won't catch it.
- **Exact resonance of complex antennas**. Models predict resonant frequency to within 1–2% typically; you still have to trim the real antenna.

## Model-building workflow

A reasonable first-time-modeler workflow:

1. **Start with the simplest topology**: a dipole, vertical, or whatever core element. Get the model working with reasonable numbers (you're aiming for 73 Ω feedpoint and a clean figure-8 for a free-space dipole — verify you got those before moving on).
2. **Choose your ground type** to match your QTH. "Real ground, average" is fine for most installations; salt water if you're coastal; very-poor for desert. Ground choice **dramatically** affects results for low antennas.
3. **Add complexity step by step.** Each added element or wire should produce a small, predictable change. If the change is large or unexpected, you have a model error (wrong segmentation, wires too close, geometry typo).
4. **Validate against known-good designs.** Build a model of a half-wave dipole at 0.5 λ above ground first. Verify pattern peak at ~30° elevation, gain ~6.5 dBi (free-space 2.15 + ground reflection 4–5 dB). If those numbers come out right, your modeling setup is sound.
5. **Run sensitivity studies.** Vary height, vary length, vary feed point — does the model behave consistently? If a small input change produces a wild output change, there's likely a model error or you're at the edge of NEC-2's validity.

## Segmentation — the most-broken thing in beginner models

A NEC model needs each wire divided into enough small segments that the current can be approximated as constant within each segment. Too few segments = wrong answer. Too many = slow, but no harm.

**Rule of thumb: ≥ 10 segments per half-wavelength of wire**, with shorter segments at high-current regions (feedpoints, ends of resonant elements). For a 33-ft 20 m dipole, that's 10 segments minimum, 21 better.

Common mistakes:

- **Too few segments** (3–5 for a half-wave dipole). Pattern looks vaguely correct, gain is inflated, impedance is wildly wrong.
- **Mismatched segments at junctions**. Where two wires meet at an angle, the segment lengths on each side need to be similar (within 2:1) or NEC produces artifacts at the junction.
- **Very short segments** (< 0.001 λ). NEC starts to fail numerically; impedance numbers go negative or unreasonable.
- **Segments near each other being similar lengths.** Wires that pass close to each other should have similar segmentation density to avoid cross-coupling errors.

## Reading model output

A typical model run produces:

- **Free-space pattern** (gain vs. azimuth at zero elevation): the antenna's "shape." Compare to expectations for the antenna type.
- **Real-ground pattern** (elevation pattern): the practical "what does it do at my QTH" curve. Look at the lowest lobe's elevation angle and its peak gain.
- **Feedpoint impedance** at the operating frequency. Real and imaginary parts. Verify it's reasonable (positive R, modest |X|) and matches expectations.
- **SWR vs frequency** sweep over the operating band(s). Tells you bandwidth.
- **Current distribution along the wires** — useful for spotting unexpected current minima/maxima that suggest the antenna is doing something unintended.

## Pitfalls and "lying models"

- **Trusting absolute gain numbers.** Models say "6.4 dBi forward gain" with five-digit precision; the *honest* number is "6.4 dBi ± 0.5 dB depending on ground assumptions." Trust the rank ordering, treat absolute gain as ±0.5 dB.
- **Ignoring the assumed feed.** NEC assumes an ideal voltage source at the feed segment — no transformer loss, no balun loss, no feedline. Your real antenna will have ~1 dB system loss from feedline + balun even with everything done right.
- **"My model says +3 dBd, my measurements show 0 dBd."** Most likely: model didn't include real losses (balun, feedline, ground) or omitted nearby objects (trees, walls, other antennas). The reality always includes those.
- **Using "perfect ground."** Useful for sanity-checking a model in free space but never representative of a real installation. Always switch to real ground before drawing conclusions about height vs. pattern.

> ⚙️ **Advanced —** The method of moments solves the Pocklington integral equation (or the more numerically tractable Hallén equation) for the current distribution on a wire conductor. The integral equation arises from requiring the tangential E-field at the conductor surface to equal zero (the perfect-conductor boundary condition). MoM expands the unknown current as a sum of basis functions (NEC uses sinusoidal basis functions per segment) and tests the residual against weighting functions (NEC uses pulse weighting), producing a Z-matrix equation Z·I = V to be solved for the segment currents. The "30 segments per wavelength" rule comes from the requirement that the basis functions resolve the current distribution adequately; resonances and rapid current changes need finer segmentation.

## Practical recommendations

- **Free-space modeling** (no ground) is fine for comparing two designs and seeing pattern shapes. Don't trust elevation patterns.
- **Real-ground modeling** is needed for any installation question (height, takeoff angle, ground losses).
- **Always run a sweep** across the band of interest, not just a single frequency. Bandwidth and SWR shape are revealing.
- **Model first, build second.** Even rough modeling reveals "this won't work" much faster than trying it.
- **When measurements disagree with the model**, find the missing component in the model — almost always feedline loss, ground losses, balun loss, or nearby objects you didn't model.

## See also

- §05-09 — Smith charts (modeling output → Smith chart visualization)
- §05-13 — Ground effects (the modeling input most people get wrong)
- §05-15 — Radiation patterns (interpreting the modeled output)
- §08 — Antenna calculator (J-Hub's simple length calculator for the cookbook cases)
