---
id: 15-04
title: Impedance
chapter: 15
section: 04
level: mixed
status: draft
---

# Impedance

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Impedance (*Z*) is the total opposition a circuit element offers to AC current. It combines **resistance** *R* (which dissipates power) and **reactance** *X* (which stores and returns it) into a single value with both magnitude and phase.

Every antenna, every feedline, every tuner, and every matching network in your station is described by impedance.

## Equation

```
Z = R + jX                  (complex form)
|Z| = √(R² + X²)            (magnitude)
φ = arctan(X / R)           (phase angle, in radians or degrees)
```

For a series combination of L and C and R:

```
X = X_L − X_C
|Z| = √(R² + (X_L − X_C)²)
```

## Variables

| Symbol | Quantity | Units |
|--------|----------|-------|
| Z | Complex impedance | ohms (Ω) |
| \|Z\| | Magnitude of impedance | ohms (Ω) |
| R | Resistance (real part) | ohms (Ω) |
| X | Reactance (imaginary part) | ohms (Ω) — positive = inductive, negative = capacitive |
| X_L | Inductive reactance | ohms (Ω) — see §15-03 |
| X_C | Capacitive reactance | ohms (Ω) — see §15-03 |
| φ | Phase angle | degrees or radians |

## Worked example — antenna feed at the operating frequency

You sweep a dipole with a NanoVNA and at 7.150 MHz it reads:

```
R = 65 Ω
X = +28 Ω (inductive)
```

The antenna is slightly long for the frequency (still inductive — see §15-05). Magnitude:

```
|Z| = √(65² + 28²)
    = √(4225 + 784)
    = √5009
    ≈ 70.8 Ω
```

Phase:

```
φ = arctan(28 / 65) = arctan(0.431) ≈ 23.3°
```

So the antenna is presenting **70.8 Ω at +23° phase** to the line. SWR on a 50 Ω line will be poor — that's the calculation in §15-07. The fix: shorten the dipole to bring X to ~0, or live with the SWR until the next ATU pass.

## Worked example — series L−C−R network

A tuned circuit has L = 5 µH, C = 50 pF, R = 10 Ω at 7 MHz. What's its impedance?

```
X_L = 2π · 7×10⁶ · 5×10⁻⁶ = 220 Ω
X_C = 1 / (2π · 7×10⁶ · 50×10⁻¹²) = 455 Ω
X = X_L − X_C = 220 − 455 = −235 Ω    (capacitive)

|Z| = √(10² + (−235)²) = √(100 + 55225) ≈ 235 Ω
φ = arctan(−235 / 10) ≈ −87.5°
```

The 10 Ω of resistance is essentially negligible — the impedance is dominated by the reactance. To make this circuit *resonant* at 7 MHz, you'd want X_L = X_C (see §15-05).

## Useful approximations

- **Pure resistance** (X = 0): Z = R, phase = 0. This is what feedlines try to see at the rig end.
- **Pure reactance** (R = 0): Z = X, phase = ±90°. An ideal capacitor or inductor.
- **\|X\| ≪ R**: Z ≈ R, phase ≈ 0°. The reactance is small enough to ignore.
- **\|X\| ≫ R**: Z ≈ X, phase ≈ ±90°. The resistance is small enough to ignore.

## Common mistakes

- **Adding reactance arithmetically with resistance.** Z is NOT R + X = (R + X). Use the Pythagorean form for magnitude: \|Z\| = √(R² + X²).
- **Forgetting reactance is signed.** Inductive (+) and capacitive (−) reactances cancel each other. Net reactance is X_L − X_C, not X_L + X_C.
- **Treating Z and R interchangeably.** A "50 Ω line" has 50 Ω **characteristic impedance**, which is purely resistive only at the matched frequency / when terminated in 50 Ω. Mismatched lines have complex Z that varies with position.
- **Ignoring the phase angle.** A 50 Ω resistive load and a 50 Ω reactive load both read "50 Ω" on a magnitude-only meter, but the rig sees them as totally different — one delivers power, one bounces it back.

> ⚙️ **Advanced —** Impedance can be written in several equivalent forms: rectangular Z = R + jX, polar Z = \|Z\|∠φ, and admittance Y = 1/Z = G + jB (conductance + susceptance). Smith charts (§15-13, §04-09) plot impedance in normalized form Z/Z_0, allowing graphical impedance manipulation. Most modern matching is done numerically (NanoVNA software), but the Smith-chart intuition is still essential for understanding *what* a matching network is doing.

## See also

- §04-09 — Smith Charts
- §04-11 — Impedance Transformation
- §08-02 — SWR & Reflected Power (uses Z to compute SWR)
- §15-03 — Reactance
- §15-13 — Smith Chart Basics
