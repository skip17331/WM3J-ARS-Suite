---
id: 07-13
title: Trap Design & Manufacturing
chapter: 07
section: 13
level: advanced
status: draft
---

# Trap Design & Manufacturing

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What it is

A **trap** is a parallel L-C resonant circuit installed in series with an antenna wire to isolate sections of the antenna at specific frequencies. At its design (resonant) frequency, a trap looks like a high-impedance open — disconnecting the rest of the wire. Below resonance, it acts as a loading inductor — physically shortening the antenna's electrical length.

Traps are the building block of trapped dipoles (§07-05) and trapped EFHWs (§07-08), and also appear in some commercial multi-band verticals.

## How it works

A parallel L-C circuit at its resonant frequency:

```
f_r = 1 / (2π · √(L · C))
```

(See §15-05.) At f_r, the inductor and capacitor's reactances cancel — the parallel combination presents a very high impedance. Above f_r the trap looks capacitive; below f_r it looks inductive (and adds physical-length-shortening loading inductance to the antenna).

Three parameters define a trap:

| Parameter | Typical value | Notes |
|-----------|----------|-------|
| **L** | 1–10 µH | Coil — air-core for low loss |
| **C** | 50–250 pF | Capacitor — high voltage rating critical |
| **Q** | 50–150 | Trap quality factor; higher = sharper, less loss |

The choice of L and C is **a tradeoff**. For a fixed f_r, smaller C means larger L (more turns of wire, larger coil). Smaller L means larger C (more capacitor, higher voltage rating). 100 pF is a popular sweet spot for HF traps.

## Calculator inputs and outputs

The Antenna Workshop calculator (`Trap Design`) takes:

- **Resonant frequency** (MHz) — the band the trap should isolate
- **Capacitance choice** (pF) — pick a doorknob/vacuum cap on hand
- **Operating power** (W PEP) — for voltage rating
- **Coil form diameter** (in)
- **Wire gauge** — #12 / #14 / #16

And returns:

- Required inductance L
- Reactance at resonance (X_L = X_C)
- Required capacitor voltage rating (with safety margin)
- Capacitor type recommendation (mica / doorknob / vacuum)
- Coil turns, length, and total wire length
- Estimated trap Q

## Worked example — 14 MHz trap for an 80/40/20 trapped dipole

```
freq = 14.150 MHz
capacitance = 100 pF
power = 100 W PEP
form = 1.0 in
wire = #14 AWG

L_required = 1 / (4π² × (14.15×10⁶)² × 100×10⁻¹²)
           = 1.27 µH

X_L = 2π × 14.15×10⁶ × 1.27×10⁻⁶ = 113 Ω
V_cap = √(100 W × Q=100 × 113 Ω) = 336 V peak
Voltage rating needed: ≥ 504 V (1.5× safety) → mica or ceramic disc OK

Coil: ~6 turns of #14 on 1" PVC form, close-wound
Coil length: ~0.4 inches (close-wound)
Wire needed: ~1.5 ft (plus 6" leads)
Trap Q: ~100 (typical for air-core)
```

For legal-limit (1500 W) operation:

```
V_cap = √(1500 × 100 × 113) = 1300 V peak  → vacuum capacitor (5 kV+)
```

The voltage requirement is the gating factor for high-power traps.

## Capacitor type guide

| Voltage requirement | Capacitor type | Cost |
|---------------------|----------------|------|
| < 1 kV | Silvered mica or NPO ceramic | $1–5 |
| 1–3 kV | Doorknob ceramic (e.g., Centralab type 850) | $10–30 |
| 3–10 kV | Vacuum capacitor (Jennings, Comet, Russian surplus) | $50–300 |
| > 10 kV | Custom vacuum or oil-filled | $300+ |

For most ham work (100 W PEP, low-Q trap), a **mica or NPO ceramic** is sufficient. For 500 W PEP you'll want **doorknob ceramic**. For legal limit, **vacuum capacitor**.

## Coil construction

The Wheeler formula (single-layer air-core inductor):

```
L (µH) = (r² × N²) / (9r + 10ℓ)

where:
  r = coil radius (inches)
  N = number of turns
  ℓ = coil length (inches)
```

For a close-wound coil, ℓ = N × wire_diameter, so this becomes an iterative solve (the calculator does this for you).

Typical values:

| Form | Wire | Inductance per turn |
|------|------|---------------------|
| 1" PVC, close-wound #14 | 0.064" wire dia | ~0.20 µH/turn at 5 turns |
| 2" PVC, close-wound #14 | 0.064" wire dia | ~0.40 µH/turn at 5 turns |
| 3" PVC, close-wound #12 | 0.081" wire dia | ~0.85 µH/turn at 5 turns |

## Common mistakes

- **Underrated capacitor.** Number-one trap failure: capacitor arcs internally at full power, trap detunes / shorts. Always size with 1.5× safety margin on voltage.
- **Ferrite-core coils.** Ferrite saturates at high RF current; trap Q drops; trap loss increases. **Air-core only** for HF antenna traps.
- **Insufficient bench-testing.** Build the trap, verify resonance with a NanoVNA or dip meter on the bench. Don't install untested traps.
- **Wrong wire gauge.** #16 is too thin for legal limit; #12 is excessive for QRP. #14 is the standard for 100 W work.
- **Under-counting turns.** The Wheeler formula slightly underestimates inductance (~5%); plan to wind 1 extra turn and remove if needed.
- **Skimping on weatherproofing.** Trap drift over a season due to water ingress is the #2 trap failure mode. Pot the trap in epoxy or seal it in heat-shrink with sealing compound.

> ⚙️ **Advanced —** Coaxial-cable traps (using a section of coax wound on a form) are a specialized variant where the inner conductor is the L and the inter-conductor capacitance is the C. Ratings are more predictable than discrete L+C, but the design space is more constrained. RG-58 has ~30 pF/ft of capacitance — that drives the design.

## Designing traps for shorty antennas

A "shorty" trapped dipole (§07-05) or trapped EFHW (§07-08) uses a trap with **higher inductance and lower capacitance** than the conventional design, while keeping the **resonant frequency unchanged**. The extra inductance gives the trap more loading reactance on bands below resonance, which compensates for the physically shorter outer segments.

The Trapped Dipole and Trapped EFHW calculators compute the needed L and C directly. Plug those into this calculator (or skip straight to the construction recipe — L and C are already specified for you).

### What changes for a shorty trap

| Parameter        | Standard 14 MHz trap | 30% shorty 14 MHz trap |
|------------------|----------------------|------------------------|
| L                | 1.27 µH              | 2.2–2.5 µH             |
| C                | 100 pF               | 50–60 pF               |
| X_L at f_r       | 113 Ω                | 200–220 Ω              |
| V_cap @ 100 W    | ~340 V               | ~1100 V (dipole), ~2200 V (EFHW) |
| Coil form        | 1" × 6 turns         | 1.5" × ~10 turns       |
| Cap type         | silvered mica        | doorknob or vacuum     |
| Q sensitivity    | moderate             | **high** — Q losses scale with X_L |

### Three things to watch on shorty traps

1. **Capacitor voltage rating.** A shorty trap's reactance at resonance is 1.7–2× higher than a conventional trap. V_cap_peak = √(P · Q · X_L), so a 30% shorty trap sees roughly 2× the voltage. Don't reuse the dipole cap inventory for a shorty EFHW — the voltage budget is fundamentally different.

2. **Q matters more.** A shorty trap is carrying more reactive energy. Trap loss is proportional to X_L / Q. A standard trap with Q=100 loses ~0.5 dB on the lower band; a shorty trap with the same Q loses ~1 dB. To keep losses down, **wind on a larger-diameter form** (boosts Q) and use thicker wire (#12 instead of #14).

3. **Thermal drift is worse.** Larger coils on larger forms have more thermal mass but also more temperature-sensitive geometry. A shorty trap can drift ±0.5% across a season vs. ±0.1% for a conventional trap. Re-check resonance after the first few weeks outdoors.

### Recommended coil form sizes for shorty traps

| Trap f_r | L (µH) | Form Ø | Wire | Approx turns |
|----------|--------|--------|------|--------------|
| 21 MHz   | ~1.5   | 1.0"   | #14  | 7            |
| 14 MHz   | ~2.5   | 1.5"   | #14  | 10           |
| 7 MHz    | ~6.5   | 2.0"   | #12  | 14           |
| 3.5 MHz  | ~22    | 3.0"   | #12  | 25           |

The Wheeler formula (above) gives the starting point; final tune on a NanoVNA.

## Construction recipe

For a typical 14 MHz, 100 W trap:

1. **Get parts:**
   - 100 pF mica capacitor, 1 kV (Cornell-Dubilier CD15FD101J04 or equivalent)
   - 1" PVC pipe, 2" length (the form)
   - 18" of #14 enameled solid copper wire
   - Heat-shrink tubing, weatherproof sealant

2. **Wind the coil:** Drill two small holes 0.5" apart for the wire ends. Tightly wind 6 turns of #14, close-wound. Anchor the ends through the holes.

3. **Solder the capacitor across the coil ends** — make sure the C is in **parallel** with the L.

4. **Bench-test:** Connect to a NanoVNA, sweep — verify resonance is at 14.150 MHz ± 50 kHz. Adjust turns if needed.

5. **Weatherproof:** Heat-shrink over the assembly with marine-grade sealant. Mount in the antenna with a non-conductive bracket.

## See also

- §04-08 — Traps (theory chapter)
- §07-00 — Antenna Workshop overview
- §07-05 — Trapped Dipole (uses these traps)
- §07-08 — EFHW (Trapped) (also uses these traps)
- §15-05 — Resonant Frequency
- §15-11 — Q Factor
- §15-03 — Reactance
