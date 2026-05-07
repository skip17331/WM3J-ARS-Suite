---
id: 21-00
title: Formula Appendix — Overview
chapter: 21
section: 00
level: simple
status: draft
---

# Formula Appendix — Overview

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

This chapter is the math reference for the rest of J-Learn. Every formula that other chapters wave at — Ohm's Law, ERP, feedline loss, resonant frequency, Q factor, decibels, Smith chart math — has its own card here with the symbols, the equation, a worked example, and the gotchas that catch new operators.

The closing section (§21-14) bundles the **Formula Calculator** pattern — what the user-facing tool would look like — and demonstrates it with a full **RF exposure** worked example, since that calculation pulls together more variables (power, frequency, duty cycle, gain, distance) than any other in the appendix.

## How to use this appendix

The chapter is a reference, not a tutorial. Skim, bookmark, look things up.

Each formula card follows the same structure:

| Section | What goes there |
|---------|------------------|
| **Equation** | The formula in standard symbols, plus any common variants |
| **Variables** | Each symbol, its units, and what real-world quantity it represents |
| **Worked example** | A typical ham-radio scenario plugged into the formula |
| **Common mistakes** | The arithmetic / unit traps that bite people |
| **See also** | Links to the chapters that use this formula in context |

The cards are deliberately short. If you need the *why* behind a formula, the linked chapter has it. The card is for "I know what I want, I just need the numbers."

## Units conventions used throughout

| Quantity | Unit symbol | Notes |
|----------|-------------|-------|
| Frequency | Hz, kHz, MHz, GHz | 1 MHz = 10⁶ Hz; convert before plugging in |
| Wavelength | m | Free-space; multiply by velocity factor for cable |
| Voltage | V | RMS unless explicitly peak (V_p) or peak-to-peak (V_pp) |
| Current | A | RMS unless stated |
| Power | W | RMS power into a 50 Ω load unless stated |
| Resistance / reactance / impedance | Ω | "Pure" resistance: R; reactance: X; complex impedance: Z = R + jX |
| Capacitance | F (typically pF or µF) | 1 pF = 10⁻¹² F |
| Inductance | H (typically µH) | 1 µH = 10⁻⁶ H |
| Loss / gain | dB | Logarithmic; 3 dB ≈ 2× power, 10 dB = 10× power |
| Decibels referenced to 1 mW | dBm | 0 dBm = 1 mW; 30 dBm = 1 W |

## Constants you'll see repeatedly

| Constant | Value | Where it comes from |
|----------|-------|----------------------|
| Speed of light *c* | 2.998 × 10⁸ m/s | Vacuum / free space |
| Practical *c* for ham math | 300,000,000 m/s | Three-sig-fig version — close enough for everything below microwave |
| Free-space wavelength factor | 300 / f(MHz) = λ(m) | Direct consequence of *c*; memorize it |
| Half-wave dipole (in air) | 468 / f(MHz) = length(ft) | Empirical end-effect correction; see §21-06 |
| Quarter-wave vertical (in air) | 234 / f(MHz) = length(ft) | Same correction, halved |
| 1 dB | 10 × log₁₀(P₂/P₁) | Standard power-ratio definition |
| Free-space impedance | 377 Ω | Useful for E-field / H-field math (§21-14 RF exposure) |

> ⚙️ **Advanced —** The 468 and 234 numbers assume a thin wire well above ground at HF. They can be off by 5–10% for thick aluminum tubing, ground-plane antennas with sloped radials, antennas near other conductors, and at higher frequencies where the end-effect grows. Use them as a starting trim and verify with an analyzer (§22 references the NanoVNA workflow).

## Section index

| § | Title | Used by |
|---|-------|---------|
| 21-01 | Ohm's Law | §14 troubleshooting, §11 power budget |
| 21-02 | Power Law | §07 RF safety, §11 power budget, §13 SWR |
| 21-03 | Reactance (capacitive & inductive) | §05 antennas, §22 baluns |
| 21-04 | Impedance | §05 antennas, §09 feedline, §22 baluns |
| 21-05 | Resonant Frequency | §05 antennas, §13 high SWR |
| 21-06 | Wavelength | §05 antennas, §09 feedline |
| 21-07 | SWR | §09 feedline, §13 high SWR |
| 21-08 | ERP | §07 RF safety, §11 power budget, §24 band plans |
| 21-09 | Feedline Loss | §09 feedline, §22 coax |
| 21-10 | Decibels | Almost every chapter |
| 21-11 | Q Factor | §05 antennas (mag loops), §22 baluns |
| 21-12 | Bandwidth | §02 repeaters, §05 antennas, §22 |
| 21-13 | Smith Chart Basics | §05-09 Smith charts (chapter context) |
| 21-14 | Formula Calculator (RF exposure worked example) | §07 RF safety |

## See also

- §07 — RF Safety (where the RF-exposure formula card is used in context)
- §08 — Antenna Calculator (geometry-specific length formulas; this chapter is the underlying math)
- §11 — Power Budget & ERP (puts §21-08 together with §21-09 and §21-10)
- §13 — High SWR (uses §21-04, §21-05, §21-07)
- §22 — Coax & Connectors (uses §21-09 with measured loss tables)
