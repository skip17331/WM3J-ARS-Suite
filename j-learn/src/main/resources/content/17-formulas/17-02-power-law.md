---
id: 17-02
title: Power Law
chapter: 17
section: 02
level: simple
status: draft
---

# Power Law

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The relationship between voltage, current, resistance, and power. Combines Ohm's Law (§17-01) with the definition of power. Comes up every time you size a power supply, fuse, dummy load, resistor, or PA stage.

## Equation

The base form:

```
P = V · I
```

Combined with Ohm's Law:

```
P = I² · R
P = V² / R
```

These are the three forms you'll use. Pick whichever form matches what you already know.

## Variables

| Symbol | Quantity | Units |
|--------|----------|-------|
| P | Power dissipated or delivered | watts (W) |
| V | Voltage (RMS, for AC) | volts (V) |
| I | Current (RMS, for AC) | amperes (A) |
| R | Resistance (or pure resistance part of impedance) | ohms (Ω) |

## Quick conversions

| Have | Want | Formula |
|------|------|---------|
| V and I | P | P = V × I |
| I and R | P | P = I² × R |
| V and R | P | P = V² / R |
| P and R | V | V = √(P × R) |
| P and R | I | I = √(P / R) |
| P and V | I | I = P / V |

## Worked example — sizing a dummy load

You want to test a 100 W HF rig at full transmit into a dummy load. What current flows in the dummy load, and what voltage appears across it?

A standard dummy load is 50 Ω. Using P = V² / R rearranged:

```
V = √(P · R) = √(100 · 50) = √5000 ≈ 70.7 V (RMS)
I = √(P / R) = √(100 / 50) = √2    ≈ 1.41 A (RMS)
```

So a 100 W signal into 50 Ω is 70.7 V RMS, 1.41 A RMS. Peak voltage is √2 × RMS ≈ 100 V; for SSB envelope peaks (PEP), double it to ~200 V peak-to-peak.

This drives several practical decisions:
- **Resistor wattage.** A dummy load made from carbon resistors needs to dissipate 100 W continuously without burning. A 100 W resistor running at its rated dissipation is **already failing** — derate to 50% (so use a 200 W resistor for 100 W RF).
- **Cooling.** Anything dissipating 100 W needs heatsinking or oil immersion. The Cantenna and Heath HN-31 dummy loads are oil-filled because of this.
- **Insulation.** Components in the load need to handle ~200 V peak-to-peak without arcing. Most 1/2 W resistors do.

## Worked example — battery runtime

Your portable HF rig draws 0.5 A on receive and 18 A on transmit at 12 V. With a 30 Ah LiFePO₄ pack and an estimated 25% transmit duty cycle:

```
Average current = 0.75 × 0.5 + 0.25 × 18
                = 0.375 + 4.5
                = 4.875 A

Runtime = 30 Ah / 4.875 A ≈ 6.2 hours
```

For a SOTA-style activation where you actually transmit only 10% of the time:

```
Average current = 0.9 × 0.5 + 0.1 × 18
                = 0.45 + 1.8 = 2.25 A
Runtime = 30 / 2.25 ≈ 13.3 hours
```

## Common mistakes

- **Using peak voltage where RMS is needed.** Always convert peak-to-peak or peak measurements to RMS before plugging into P = V²/R. RMS = peak / √2 for a sine wave.
- **Forgetting PEP vs. average power for SSB.** A "100 W" SSB rig produces 100 W *peak envelope power*, but the average power averaged over a few seconds of voice is closer to 25–40 W. Dummy loads can be sized for the *average* dissipation (lower); transmitter components must be sized for the *peak* (higher).
- **Mixing AC and DC Ohms.** A 50 Ω dummy load is 50 Ω resistive at RF; its DC resistance is the same to within fractions of an ohm. But a 50 Ω antenna's *DC* resistance is essentially zero — it's the *RF* impedance that matches the line. Don't confuse them.

> ⚙️ **Advanced —** For AC across complex impedance, only the in-phase (real) component of voltage and current dissipates real power; the reactive component circulates without dissipation. Real power is then P = V·I·cos(φ), where φ is the phase angle between V and I. For ham work the simple forms above almost always suffice (50 Ω matched lines, resistive dummy loads); the cos(φ) factor matters when you're doing antenna or amplifier matching network analysis.

## See also

- §09 — Power Budget & ERP (the chain from PA output through feedline to ERP)
- §10 — High SWR (uses P = I²R to derive heating in mismatched lines)
- §17-01 — Ohm's Law (Ohm's Law combined with P = V·I gives the full set)
- §17-08 — ERP (uses the same dB-style power math)
- §17-10 — Decibels (the log-domain way to manipulate power ratios)
