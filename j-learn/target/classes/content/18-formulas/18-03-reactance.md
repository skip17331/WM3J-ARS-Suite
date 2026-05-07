---
id: 18-03
title: Reactance (Capacitive & Inductive)
chapter: 18
section: 03
level: mixed
status: draft
---

# Reactance — Capacitive & Inductive

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Capacitors and inductors oppose AC current the way resistors oppose DC current — but the opposition is **frequency-dependent**, and instead of dissipating power, it stores and returns it. The frequency-dependent opposition is called **reactance** (symbol *X*), measured in ohms.

Reactance is the heart of every tuned circuit, every antenna's resonance, every feedline transformation, and every L/C filter in your station.

## Equations

```
Capacitive reactance:   X_C = 1 / (2π · f · C)
Inductive reactance:    X_L = 2π · f · L
```

## Variables

| Symbol | Quantity | Units |
|--------|----------|-------|
| X_C | Capacitive reactance | ohms (Ω) |
| X_L | Inductive reactance | ohms (Ω) |
| f | Frequency | hertz (Hz) — convert MHz to Hz before plugging in |
| C | Capacitance | farads (F) — typical pF and µF must be converted |
| L | Inductance | henries (H) — typical µH and mH must be converted |

## Behavior with frequency

| Component | At DC (f → 0) | At low f | At high f | At ∞ |
|-----------|---------------|----------|-----------|------|
| Capacitor | open (X_C = ∞) | high X_C | low X_C | short (X_C → 0) |
| Inductor | short (X_L = 0) | low X_L | high X_L | open (X_L → ∞) |

A capacitor blocks DC and passes RF; an inductor passes DC and blocks RF. This is why coupling capacitors carry the AC signal but block the DC bias on an amplifier stage, and why RF chokes are used to feed DC into an antenna's transmission line.

## Worked example — coupling capacitor on a 7 MHz buffer stage

A buffer amp's coupling cap should have reactance much smaller than the load it feeds. If the next-stage input is 1000 Ω at 7 MHz, you want X_C ≤ 100 Ω (about 1/10 of the load).

```
X_C = 1 / (2π · f · C)
100 = 1 / (2π · 7×10⁶ · C)
C = 1 / (2π · 7×10⁶ · 100)
C ≈ 227 pF
```

Round up to a standard 220 pF or 270 pF and you're set. Note that **at higher frequency the same capacitor presents lower reactance** — a 220 pF cap is X_C ≈ 100 Ω at 7 MHz, but only ~22 Ω at 30 MHz. That's why coupling caps in broadband amplifiers are sized for the *lowest* frequency in the operating band.

## Worked example — RF choke for 14 MHz

You're feeding a vertical antenna with a coax run, and you want to add a series RF choke to keep RF off the shield. You want the choke's reactance to be high compared to the feedline impedance — say, 10× of 50 Ω = 500 Ω at 14 MHz.

```
X_L = 2π · f · L
500 = 2π · 14×10⁶ · L
L = 500 / (2π · 14×10⁶)
L ≈ 5.7 µH
```

A common practical choke would be a #43 ferrite bead with sufficient turns to reach ~6 µH, or a quarter-wave coax choke at the band of interest (see §04-12 baluns and chokes for the physical implementations).

## Common mistakes

- **Forgetting to convert frequency to hertz.** Plugging "7" instead of "7,000,000" into the formula gives an answer 6 orders of magnitude wrong. Always convert MHz → Hz before substituting.
- **Forgetting to convert capacitance to farads.** Plugging "220" for "220 pF" gives an answer 10¹² off. Use 220×10⁻¹² instead.
- **Treating reactance like resistance for power dissipation.** Reactance does NOT dissipate power. A 100 Ω resistor and a 100 Ω reactance look the same to a meter, but only the resistor heats up.
- **Adding reactances arithmetically when they should subtract.** A series L and C cancel: X_total = X_L − X_C. At resonance they cancel completely (§18-05).

> ⚙️ **Advanced —** Reactance is signed. Capacitive reactance is conventionally written as −jX_C and inductive as +jX_L, where j = √(−1). The signs matter when computing complex impedance Z = R + jX, since they affect both magnitude and phase. See §18-04.

## See also

- §04-12 — Baluns and chokes (where these formulas size real chokes)
- §18-04 — Impedance (combines R with X to get full Z)
- §18-05 — Resonant Frequency (where X_L and X_C cancel exactly)
- §19-05 — Baluns reference (formulas applied to balun design)
