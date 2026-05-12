---
id: 17-00
title: Formulas — Overview
chapter: 17
section: 00
level: simple
status: draft
---

# Formulas — Overview

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

This chapter is the math reference for the rest of J-Learn. Every formula that other chapters wave at — Ohm's Law, ERP, feedline loss, resonant frequency, Q factor, decibels, Smith chart math — has its own card here with the symbols, the equation, a worked example, and the gotchas that catch new operators.

Each formula card has a matching **interactive calculator** in the J-Hub Antenna Workshop tab. The card explains the math; the calculator lets you plug in numbers and read off answers. A "▶ Open in Workshop" button at the top of every section in this chapter takes you straight to the matching panel.

§17-14 (RF Exposure) is the longest card because that calculation pulls together more variables (power, frequency, duty cycle, gain, distance) than any other formula in the chapter — it's the canonical example of the formula-calculator pattern.

## How to use this chapter

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
| Half-wave dipole (in air) | 468 / f(MHz) = length(ft) | Empirical end-effect correction; see §17-06 |
| Quarter-wave vertical (in air) | 234 / f(MHz) = length(ft) | Same correction, halved |
| 1 dB | 10 × log₁₀(P₂/P₁) | Standard power-ratio definition |
| Free-space impedance | 377 Ω | Useful for E-field / H-field math (§17-14 RF exposure) |

> ⚙️ **Advanced —** The 468 and 234 numbers assume a thin wire well above ground at HF. They can be off by 5–10% for thick aluminum tubing, ground-plane antennas with sloped radials, antennas near other conductors, and at higher frequencies where the end-effect grows. Use them as a starting trim and verify with an analyzer (§18 references the NanoVNA workflow).

## Section index

| § | Title | Used by |
|---|-------|---------|
| 18-01 | Ohm's Law | §13 troubleshooting, §11 power budget |
| 18-02 | Power Law | §08 RF safety, §11 power budget, §12 SWR |
| 18-03 | Reactance (capacitive & inductive) | §06 antennas, §18 baluns |
| 18-04 | Impedance | §06 antennas, §10 feedline, §18 baluns |
| 18-05 | Resonant Frequency | §06 antennas, §12 high SWR |
| 18-06 | Wavelength | §06 antennas, §10 feedline |
| 18-07 | SWR | §10 feedline, §12 high SWR |
| 18-08 | ERP | §08 RF safety, §11 power budget, §20 band plans |
| 18-09 | Feedline Loss | §10 feedline, §18 coax |
| 18-10 | Decibels | Almost every chapter |
| 18-11 | Q Factor | §06 antennas (mag loops), §18 baluns |
| 18-12 | Bandwidth | §04 repeaters, §06 antennas, §18 |
| 18-13 | Smith Chart Basics | §06-09 Smith charts (chapter context) |
| 18-14 | RF Exposure | §08 RF safety |

## See also

- §08 — RF Safety (where the RF-exposure formula card is used in context)
- §09 — Antenna Calculator (geometry-specific length formulas; this chapter is the underlying math)
- §11 — Power Budget & ERP (puts §17-08 together with §17-09 and §17-10)
- §12 — High SWR (uses §17-04, §17-05, §17-07)
- §18 — Coax & Connectors (uses §17-09 with measured loss tables)
