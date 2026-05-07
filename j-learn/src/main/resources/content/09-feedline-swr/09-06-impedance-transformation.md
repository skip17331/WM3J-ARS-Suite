---
id: 09-06
title: Impedance Transformation by Feedline
chapter: 09
section: 06
level: advanced
status: draft
---

# Impedance Transformation by Feedline

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A length of transmission line is not "transparent" to impedance — it transforms the load's impedance into something different at the source end. The transformation depends on the line's characteristic impedance Z₀, the load impedance Z_L at the far end, and the **electrical length** of the line at the operating frequency.

Two consequences for amateur operators:

1. **The SWR you read at the rig is not necessarily the SWR at the antenna.** Feedline transformation can make the rig-end SWR appear better (or, less commonly, worse) than the antenna's actual SWR.
2. **You can deliberately use feedline as a matching device** — quarter-wave matching sections, half-wave repeaters, stubs.

This section unpacks both. The general matching-network discussion is in §05-11; this section focuses on the feedline-specific behavior.

## The transmission-line equation

The closed-form result, for a lossless line of characteristic impedance Z₀ and electrical length βℓ (where β = 2π/λ_cable, ℓ is the physical length, and λ_cable accounts for VF):

**Z_in = Z₀ × (Z_L + jZ₀ × tan βℓ) / (Z₀ + jZ_L × tan βℓ)**

Where:
- Z_in is the impedance seen looking into the line (rig end).
- Z_L is the load impedance (antenna end).
- βℓ is the electrical length in radians (2π for one wavelength, π/2 for quarter-wave, etc.).

For lossy lines, replace tan with tanh and add the loss term; the structure is the same.

## Three special cases

### Case 1: Half-wave line (electrical length = λ/2, βℓ = π)

tan βℓ = 0, so:

**Z_in = Z₀ × (Z_L + 0) / (Z₀ + 0) = Z_L**

**A half-wave line preserves the load impedance.** What you see at the rig is what's at the antenna.

This is useful: cut your feedline to an electrical half-wavelength on the operating band, and the rig's SWR meter shows the antenna's actual SWR. A diagnostic tool.

### Case 2: Quarter-wave line (electrical length = λ/4, βℓ = π/2)

tan βℓ → ∞. Using L'Hôpital's rule:

**Z_in = Z₀² / Z_L**

**A quarter-wave line is an impedance inverter.** A high-Z load becomes low-Z at the source, and vice versa.

This is the basis of the quarter-wave matching transformer. Pick Z₀ such that Z₀² / Z_L = the desired Z_in (typically 50 Ω for amateur use).

For Z_L = 120 Ω, target 50 Ω: Z₀ = √(50 × 120) = √6000 ≈ 77 Ω. Use 75 Ω cable (standard); you get Z_in = 75² / 120 ≈ 47 Ω. Acceptable.

For Z_L = 25 Ω, target 50 Ω: Z₀ = √(50 × 25) = √1250 ≈ 35 Ω. Standard cables don't come in 35 Ω; you'd need to use two parallel 75 Ω cables (which gives 37.5 Ω) or accept some mismatch.

### Case 3: Other electrical lengths

In between λ/4 and λ/2, the line transforms impedance into a complex value (with both real and imaginary parts). On the Smith chart (§05-09), this looks like the impedance "spiraling" around the chart center as you move along the line.

The general behavior: as you move the line length from 0 to λ/2, the impedance traces a circle on the Smith chart, completing one full revolution.

## What this means for SWR meter readings

If your antenna is at SWR S at the antenna feedpoint, and there's a length of feedline between rig and antenna, **the SWR at the rig depends on the feedline's electrical length** (if the line is lossless). Specifically:

- **At electrical multiples of λ/2** (half-wave, full-wave, etc.): SWR_rig = SWR_antenna. (Because Z is preserved.)
- **At electrical multiples of λ/4 + odd half** (quarter-wave plus or minus integer half-waves): SWR_rig = SWR_antenna. (Yes, also — the inverted impedance has the same |Γ| and so the same SWR.)
- **At intermediate lengths**: SWR_rig depends on the impedance phase, but the magnitude is unchanged.

Wait — that's the **lossless** case. SWR is conserved on a lossless line at any length. **Real lines have loss**, and that's where things get interesting.

For a real lossy line:

- The reflected wave loses energy on each round trip.
- A round trip takes one full wavelength of "going down and coming back" in the line.
- After lots of cable loss, the reflected wave is weak, and the SWR appears lower.

Recall from §09-03: a 6 dB lossy line with shorted antenna shows ~2.6:1 SWR at the rig.

## Two diagnostic uses

### Diagnostic 1: Half-wave feedline shows true SWR

If you cut your feedline to a known electrical half-wave at your operating band, the rig's SWR meter directly reads the antenna's SWR (modulo small line loss). Useful for verifying that your antenna is at the SWR you think it is.

For 14 MHz with RG-213 (VF 0.66): half-wave physical length = 23.2 ft. Cut to 23 ft and the rig sees the antenna directly.

### Diagnostic 2: SWR sweep shape reveals problems

A NanoVNA's SWR-vs-frequency sweep at the rig end shows the antenna's SWR transformed by the cable. The transformation creates **periodic features** at frequencies where the cable is an integer multiple of half-wavelength.

A flat-top dipole on a 50-ft cable on 14 MHz would show:

- Dip at ~14 MHz (the antenna resonance).
- Pseudo-dip at ~10 MHz (where the cable is exactly λ/2 at that frequency).
- Pseudo-dip at ~21 MHz (cable is 3λ/4 there, also resonant).

These pseudo-dips are not the antenna; they're feedline artifacts. The antenna's true behavior is most apparent at frequencies where the cable is at multiples of λ/2.

A Smith chart trace makes this much clearer — the antenna's resonance is a sharp loop near the chart center; feedline transformation just rotates that loop around the center as frequency changes.

## Two design uses

### Design 1: Quarter-wave matching transformer

A coax of impedance Z₀ = √(Z_load × 50) cut to a quarter electrical wavelength matches a Z_load antenna to 50 Ω. Common amateur examples:

- Quad antenna (~120 Ω) → 75 Ω matching section → 47 Ω at rig (close enough).
- Folded dipole (~280 Ω) → 4:1 balun is more common but a 119 Ω cable would work (no standard cable at this Z; 4:1 balun simpler).

The matching section's length depends on the cable's VF (see §09-05).

### Design 2: Stub matching

A short open or shorted stub of transmission line, attached at the right point along the feedline, presents a specific reactance that cancels the load reactance. Combined with feedline length to the right point, a stub can match any complex load to 50 Ω at a single frequency.

This is more common at VHF/UHF where lumped components are difficult; at HF, lumped tuners are easier.

## Why the math is messy

The general transmission-line equation looks complicated because it's the steady-state result of forward and reflected waves bouncing on the line, with the boundary conditions at both ends determining the standing-wave pattern. The closed form captures all of that compactly.

Practical operators rarely need to solve the equation by hand:

- **For SWR concerns**: the lossless-case observation that SWR is conserved (in magnitude) along any line is what matters most.
- **For matching**: pick from the standard cases (quarter-wave, half-wave) or use a Smith chart graphical approach.
- **For tuner design**: either use a tuner (which solves for any impedance) or use modeling software.

> ⚙️ **Advanced —** The full lossy transmission-line equation: Z_in = Z₀ × (Z_L + Z₀ × tanh γℓ) / (Z₀ + Z_L × tanh γℓ), where γ = α + jβ is the complex propagation constant (α is loss per unit length in nepers, β is phase per unit length in radians). For a lossless line, α = 0 and γ = jβ, recovering the formula at the start of the section. The hyperbolic tangent gives the loss-induced damping that causes SWR to "decay" toward 1:1 as line length increases — the characteristic of "lossy line hides high SWR" (§09-03).

## Common transformation-related mistakes

- **Treating SWR at the rig as gospel.** Feedline transformation can make a real 5:1 antenna look like 2:1 at the rig. Verify at antenna.
- **Cutting matching sections without VF correction.** The quarter-wave physical length depends on the matching cable's VF. Multiply.
- **Using the wrong characteristic impedance for matching.** A 75 Ω matching section between 50 and 120 Ω antennas is approximately right; getting the impedance exactly right requires Z₀ = √(50 × 120) ≈ 77 Ω, not 75. Small mismatch from the approximation, accepted in practice.
- **Forgetting that transformation is band-specific.** A quarter-wave matching section at 14 MHz is half-wave at 7 MHz (no transformation) and 3λ/4 at 21 MHz (different transformation). Multi-band matching needs a different approach (broadband transformer, ATU).

## See also

- §09-00 — Chapter overview
- §09-02 — SWR & reflected power
- §09-05 — Velocity factor
- §05-09 — Smith charts (visualizing transformation)
- §05-10 — Feedline effects (the broader picture)
- §05-11 — Impedance transformation (matching-network view)
- §22-03 — Velocity factor reference
