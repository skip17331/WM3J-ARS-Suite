---
id: 18-01
title: Ohm's Law
chapter: 18
section: 01
level: simple
status: draft
---

# Ohm's Law

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The single most-cited equation in radio. Relates voltage, current, and resistance in any DC circuit (and any AC circuit dominated by resistance).

## Equation

```
V = I · R
```

Rearranged:

```
I = V / R
R = V / I
```

## Variables

| Symbol | Quantity | Units |
|--------|----------|-------|
| V | Voltage across the element | volts (V) |
| I | Current through the element | amperes (A) |
| R | Resistance of the element | ohms (Ω) |

## Worked example — fuse sizing for a 100 W mobile rig

A typical 100 W HF mobile rig draws about 22 A on transmit at 13.8 V. The DC power cable runs from the battery through a fuse, through 8 ft of #10 wire, to the rig.

What's the voltage drop across the wire?

```
#10 copper wire: 1.0 mΩ/ft
8 ft round trip (out + back) = 16 ft total
R = 16 ft × 0.001 Ω/ft = 0.016 Ω

V_drop = I · R = 22 A × 0.016 Ω = 0.35 V
```

So at full transmit, the rig sees 13.8 − 0.35 = **13.45 V** instead of 13.8 V. That's well within the rig's tolerance, but if you'd used #14 wire instead (~2.5 mΩ/ft):

```
R = 16 × 0.0025 = 0.040 Ω
V_drop = 22 × 0.040 = 0.88 V
```

Now the rig sees 12.92 V. Most rigs start cutting back power below 13.0 V, so the thinner wire actually robs you of TX output. **#10 minimum for any 100 W mobile install** is the rule of thumb that falls out of this calculation.

## Common mistakes

- **Using the wire's one-way length.** Current flows out *and* back through the cable; double the run before plugging in.
- **Ignoring connector and fuse-holder resistance.** A loose Anderson Powerpole or corroded ring terminal can add 0.05–0.1 Ω, which at 22 A drops another volt or two. Re-crimp suspect connectors.
- **Confusing AC and DC values.** Ohm's Law in this form applies cleanly to DC. For AC with reactance, use complex impedance instead (§18-04).

> ⚙️ **Advanced —** Ohm's Law is exact for ideal resistors. Real resistors have temperature coefficients (carbon-comp can drift ±5%/°C; metal film is ±50 ppm/°C) and skin-effect at RF (the effective AC resistance of a wire above ~1 MHz is higher than the DC resistance, because current crowds toward the surface). For RF power-handling design, use the AC resistance from the wire's datasheet.

## See also

- §09 — Power Budget & ERP (chains Ohm's Law into the full transmit-side calculation)
- §11 — Station Troubleshooting (voltage-drop diagnosis)
- §18-02 — Power Law (the P = V·I form, derived from this)
