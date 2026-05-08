---
id: 15-05
title: Resonant Frequency
chapter: 15
section: 05
level: mixed
status: draft
---

# Resonant Frequency

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A series L−C circuit is **resonant** at the frequency where the inductive and capacitive reactances are equal and cancel: X_L = X_C. At resonance the impedance is purely resistive (just the loss resistance), and the circuit appears as a short for series resonance or an open for parallel resonance.

Antennas, IF filters, traps, and tuned amplifier stages are all resonant at design frequency. Knowing the resonance formula lets you compute trap values, lengthen/shorten antennas, and predict where filters peak.

## Equation

```
f_r = 1 / (2π · √(L · C))
```

Rearranged:

```
L = 1 / (4π² · f_r² · C)
C = 1 / (4π² · f_r² · L)
```

## Variables

| Symbol | Quantity | Units |
|--------|----------|-------|
| f_r | Resonant frequency | hertz (Hz) |
| L | Inductance | henries (H) |
| C | Capacitance | farads (F) |

## Quick form (for ham bands)

A useful shortcut when frequency is in MHz, L in µH, C in pF:

```
f_r(MHz) = 159.15 / √(L(µH) · C(pF))
```

This avoids the unit conversions every time you size an antenna trap.

## Worked example — antenna trap for 14 MHz

You're building a multi-band trap dipole that needs a 14 MHz trap. You have a 100 pF doorknob capacitor on hand. What inductance do you need?

```
f_r(MHz) = 159.15 / √(L · C)
14 = 159.15 / √(L · 100)
√(L · 100) = 159.15 / 14 ≈ 11.37
L · 100 = 129.2
L ≈ 1.29 µH
```

Wind a coil with that inductance (about 12 turns of #14 wire on a 1" diameter, 1.5" long form, per typical wire-coil tables) and you have a trap that's open at 14 MHz and a short below it.

## Worked example — figuring out where a dipole is *actually* resonant

A 40-meter dipole reads R = 50 Ω, X = +35 Ω at 7.000 MHz. The antenna is *not* resonant at 7.000 — it's slightly long, so its self-resonant frequency is *below* 7 MHz. To get to resonance you'd need to shorten it.

Estimating: the inductive reactance is dominated by the dipole length being slightly long for the operating frequency. Each percentage point of length reduces resonant frequency by about 1%. So if you shorten the dipole by ~1% (about 8 inches on a 66 ft dipole), it will resonate near 7 MHz.

A NanoVNA sweep before and after the trim confirms the actual resonance — no math required. The formula here is just for reasoning about *why* the trim works.

## Common mistakes

- **Mixing units.** Use the (Hz, H, F) form OR the (MHz, µH, pF) shortcut. Don't mix them.
- **Forgetting the 2π factor.** It's there because reactances X_L = 2πfL and X_C = 1/(2πfC) both have it. Setting them equal yields the formula.
- **Treating series and parallel resonance as the same.** A series LC at resonance is a *short* (low impedance, current flows freely); a parallel LC at resonance is an *open* (high impedance, current is blocked). Same formula, opposite behavior.
- **Ignoring component tolerance.** A 5% capacitor and 10% inductor combined give resonance accuracy of about ±7%. For tight filters use 1% C and 2% L, or trim experimentally.

> ⚙️ **Advanced —** The Q of a resonant circuit (§15-11) determines how sharp the resonance is. Q = 2π · stored energy / energy lost per cycle = X_L / R = X_C / R at resonance. A high-Q antenna trap (Q > 50) is sharply tuned and stays "open" only over a narrow band; a low-Q trap (Q ~ 10) tunes broadly but with more loss.

## See also

- §04-08 — Traps (uses this formula for multi-band antennas)
- §10-04 — High-SWR diagnosis (off-resonance is one cause)
- §15-03 — Reactance (the X_L = X_C condition that defines resonance)
- §15-11 — Q Factor (sharpness of the resonance)
- §15-12 — Bandwidth (how Q and resonance interact)
