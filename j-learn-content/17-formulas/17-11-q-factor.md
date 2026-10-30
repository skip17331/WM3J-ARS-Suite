---
id: 17-11
title: Q Factor
chapter: 17
section: 11
level: advanced
status: published
---

# Q Factor

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The Q factor (quality factor) of a resonant circuit measures how sharp its resonance is. High Q = narrow bandwidth, high efficiency, sharp tuning. Low Q = wide bandwidth, more loss, broadly tuned.

Antennas, traps, IF filters, magnetic loops, and tuned amplifier stages all have a Q. Magnetic loops in particular live and die by their Q (often 200–500 for practical loops).

## Equations

The defining relationship:

```
Q = 2π × (energy stored per cycle) / (energy lost per cycle)
```

Practical forms:

```
At resonance:
  Q = X_L / R    (or equivalently X_C / R)

Bandwidth and Q:
  Q = f_r / Δf_3dB

Loop antenna Q:
  Q = R_radiation_+_loss / X
  (where R is the total series resistance and X the loop's reactance at resonance)
```

## Variables

| Symbol | Quantity | Units |
|--------|----------|-------|
| Q | Quality factor (dimensionless) | — |
| X_L, X_C | Reactance at resonance | ohms (Ω) |
| R | Series resistance (loss) | ohms (Ω) |
| f_r | Resonant frequency | hertz (Hz) |
| Δf_3dB | Bandwidth at 3 dB down points | hertz (Hz) |

## Quick interpretation

| Q | Bandwidth on 7 MHz | Comment |
|---|---:|---------|
| 10 | 700 kHz | Very broad; antenna trap, broadband filter |
| 50 | 140 kHz | Moderate; typical L/C tank circuit |
| 100 | 70 kHz | Sharp; quartz crystal filter |
| 500 | 14 kHz | Very sharp; magnetic loop antenna |
| 1000 | 7 kHz | Quartz crystal in a single-frequency oscillator |
| 10000 | 700 Hz | Mechanical filter, expensive crystal |

## Worked example — Q of an L−C resonant circuit

A 5 µH inductor with 1 Ω series resistance, tuned with a 50 pF capacitor (resonant at ~10 MHz):

```
X_L at 10 MHz = 2π · 10×10⁶ · 5×10⁻⁶ = 314 Ω
R = 1 Ω

Q = X_L / R = 314 / 1 = 314
```

The 3-dB bandwidth would be:

```
Δf = f_r / Q = 10×10⁶ / 314 ≈ 31.8 kHz
```

So this filter is about 32 kHz wide at the −3 dB points — sharper than an SSB filter, less sharp than a CW filter.

## Worked example — magnetic loop Q

A 1m diameter magnetic loop antenna at 14 MHz has X_L ≈ 200 Ω at the desired tuning. The total loss resistance (radiation + ohmic + capacitor + connections) is about 0.5 Ω. So:

```
Q = 200 / 0.5 = 400
```

The 3-dB bandwidth is:

```
Δf = 14×10⁶ / 400 = 35 kHz
```

The mag loop is sharply tuned — usable across only ~35 kHz before SWR rises sharply. This is why mag loops require a remote-controlled motor on the tuning capacitor: you re-tune as you change frequency. Higher Q = more efficient (less loss into the resistance), but narrower usable bandwidth.

## The Q tradeoff

For a fixed inductor with fixed loss resistance, raising Q simultaneously:
- **Reduces** the 3-dB bandwidth (sharper tuning).
- **Increases** the voltage and current at resonance (Q × the input voltage, in fact).
- **Decreases** the losses as a fraction of stored energy.

The "current and voltage rise at resonance by Q" is what makes high-Q magnetic loops dangerous — a 50 W transmitter into Q = 400 produces voltages of `√(50 × 200) × 400 ≈ 40 kV` at the tuning capacitor. The capacitor must be rated accordingly (vacuum capacitors are common in mag loops for this reason).

## Common mistakes

- **Confusing loaded vs. unloaded Q.** The Q in isolation (loop alone) is the unloaded Q. Connect it to a feedline and the feedline's loading reduces Q: this is loaded Q. The two can differ by 5× or more.
- **Forgetting the capacitor / tuning-element loss.** A 0.1 Ω capacitor ESR at 1 A circulating current dissipates 0.1 W on top of any RF loss. In a high-Q circuit this matters — capacitor selection makes or breaks the design.
- **Treating Q as a single property of the antenna.** Q changes with frequency for the same physical antenna. A loop at 14 MHz has a different Q than the same loop at 7 MHz.
- **Reading Q off a sweep without correcting for analyzer load.** A NanoVNA's input impedance presents a load on the resonant circuit that reduces Q. Use a high-impedance probe or compute the unloaded Q from the loaded measurement.

> **Advanced —** Q is a property of the *resonance*, not the components. The same inductor in a different circuit (different frequency, different damping) has a different effective Q. The "Q of an inductor" stated by manufacturers is at a specific frequency and assuming a specific test setup; check the datasheet's measurement conditions.

## See also

- §06-14 — Magnetic loops (where Q is a defining specification)
- §06-17 — Traps (high-Q traps stay sharp; low-Q traps tune broadly)
- §17-03 — Reactance (X_L and X_C from which Q is computed)
- §17-05 — Resonant frequency (where Q applies)
- §17-12 — Bandwidth (the f_r / Q relationship)
