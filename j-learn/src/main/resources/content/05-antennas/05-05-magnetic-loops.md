---
id: 05-05
title: Magnetic Loops
chapter: 05
section: 05
level: mixed
status: draft
---

# Magnetic Loops

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A small transmitting magnetic loop (STL) is an antenna shaped like a closed circle, **roughly 1/10 of a wavelength in circumference**, tuned to resonance with a high-Q variable capacitor. It looks ridiculous — a 3-ft hula hoop — but it works.

This is the antenna that lets HOA-restricted hams, apartment dwellers, and operators with a single small balcony actually get on HF. It's also the antenna that requires the most precise mechanical construction of any in this chapter, and the one most likely to bite (literally) if mis-handled.

## How it differs from a normal antenna

A "normal" half-wave antenna radiates primarily through its **electric** field. The current is sinusoidal, peaking at the feedpoint, falling to zero at the ends; voltage peaks at the ends.

A magnetic loop, by contrast, has **near-uniform current around the entire loop** (because the loop is electrically small, the current cannot vary much). It radiates primarily through its **magnetic** field. The voltage at the gap (where the tuning capacitor sits) is enormous — *thousands of volts at moderate power* — while the radiated field is dominated by the H-field at distances less than a wavelength.

This has consequences:

1. **High Q (narrow bandwidth)**. SWR < 2:1 may only span 5–20 kHz on 40 m. You retune for every QSY of more than a few kHz.
2. **High voltages at the capacitor gap**. You absolutely cannot touch the capacitor terminals during transmit, even at QRP power. At 100 W, the gap can be 3+ kV.
3. **Lower environmental coupling**. The loop is less sensitive to nearby objects than a dipole or vertical of similar size — because it's responding to magnetic field, not electric. This is why mag loops actually work indoors.
4. **Quieter receive**. The loop's null pattern (broadside to the loop's plane) is sharp and the directional axis can be rotated. Mag loops are famously good at nulling local noise.

## The typical numbers

| Property | Typical small mag loop on HF |
|----------|------------------------------|
| Loop diameter | 3 ft (1 m) for 7–28 MHz coverage; 5 ft for 3.5–14 MHz |
| Loop conductor | Copper tube, 3/4 to 1 inch diameter |
| Tuning capacitor | Vacuum-variable or wide-spaced air, 10–500 pF |
| Voltage at capacitor (100 W, 40 m) | 3–6 kV peak |
| Bandwidth (SWR < 2:1) | 5 kHz on 80 m, 20 kHz on 20 m, 50 kHz on 10 m |
| Q | 200–800 (band-dependent) |
| Efficiency | 10–60% (band-dependent; 80 m worse than 20 m worse than 10 m) |
| Gain vs. dipole at low height | Often comparable; sometimes *better* in restricted indoor space |

The "comparable to a dipole" claim deserves the asterisk: a mag loop in an apartment beats a *low* dipole in the same apartment, because the dipole at 8 ft over a concrete slab is hugely lossy. A mag loop at 8 ft is much less ground-coupled (it's responding to H-field, and ground losses are smaller for H-fields at small antenna heights). Compare instead to a *good* outdoor dipole at 60 ft and the loop loses by 6+ dB on most bands.

## Construction outline

A working STL needs four components:

1. **The main loop.** Copper tube or large-diameter copper wire, formed into a circle. Soldered/clamped joins must be very low-resistance — at 100 W, even a 0.05 Ω contact is dissipating 5 W as heat in a tiny area. **The conductor losses dominate small-loop efficiency**; thicker conductor = more efficient antenna. 1-inch copper tube is the sweet spot for HF mag loops.
2. **The tuning capacitor.** Either an air-variable with very wide plate spacing (for QRP use) or a vacuum-variable capacitor (for legal-limit use). The capacitor must hold off the full peak voltage at the gap — typically rated 5–10 kV for 100 W operation. **A standard receiving air-variable will arc and fail.**
3. **The coupling loop or gamma match.** Energy is coupled into the main loop via a small **coupling loop** (typically 1/5 the diameter of the main loop) at the bottom, opposite the capacitor, fed with coax. Adjust the coupling-loop size for 50 Ω match.
4. **Mounting and tuning mechanism.** Many designs use a stepper motor on the capacitor for remote tuning — manually retuning every 5 kHz becomes tedious quickly.

## Bandwidth and tuning

Q is intentionally high (that's how a small loop radiates efficiently). The narrow bandwidth means:

- **Every QSY is a retune.** A 5 kHz move on 40 m may take you from 1.2:1 SWR to 4:1.
- **Temperature affects resonance.** A 5°C change can shift the resonance enough to need a touch-up. Sun on a rooftop loop is a particular challenge.
- **Motorized tuning is almost mandatory** for any serious operating beyond a single net frequency.

> ⚙️ **Advanced —** Loop efficiency η = R_rad / (R_rad + R_loss). For a small loop, R_rad scales as (circumference / λ)⁴ — fourth power. So a loop 0.1 λ in circumference has *one ten-thousandth* the radiation resistance of a half-wave loop. Conductor and capacitor losses don't shrink as fast, so efficiency falls precipitously as you go lower in frequency for any given physical loop. This is why an 80 m mag loop (with the same 1-m diameter that works fine on 20 m) is on the order of 10% efficient: R_rad has fallen to milliohms while loss resistance is still tens of milliohms. The fix is a bigger loop, lower-resistance copper, and silver-plated or vacuum-variable capacitors.

## Power handling

Loop efficiency *and* capacitor voltage both scale with power. At 100 W:

- A typical 1-m HF mag loop runs **3–4 kV at the capacitor gap on 40 m**.
- The same loop on 80 m can run **6–8 kV** because Q is higher there.
- At 1500 W, those numbers triple. Vacuum capacitors are mandatory.

**Key safety principle**: never touch the capacitor or any part of the loop during transmit. RF burns from a 5 kV gap are immediate and severe. See §07-07.

## Pattern and polarization

A mag loop's pattern depends on its orientation:

- **Vertical loop (loop in a vertical plane)**: bidirectional figure-8, broadside through the plane of the loop; nulls at the edges (perpendicular to the loop face). Polarization is mostly horizontal at the broadside maximum.
- **Horizontal loop (loop in a horizontal plane, like a ceiling fan)**: omnidirectional in azimuth, mostly horizontal polarization, takeoff angle high.

The vertical-loop figure-8 pattern is sharp — typical front-to-side ratio is 20+ dB. **You can rotate a mag loop to null a local noise source**, which is a major operational advantage.

## When to pick a magnetic loop

- HOA / apartment / restricted space — the only way to be on HF.
- You need a directional antenna with deep nulls in a small space.
- You want a quiet receive antenna (the magnetic-coupling characteristic also helps with noise rejection).
- Stealth — a 3-ft circle on a balcony reads as a "decoration."

## When to avoid

- You have *any* outdoor space for a wire — a wire dipole or EFHW will outperform a comparable-cost mag loop.
- You want simple multi-band quick QSY operation. Loops are slow.
- You operate at high power without a vacuum capacitor.
- 80 m or 160 m as a primary band — loop efficiency falls off a cliff at low frequencies for any reasonable physical size.

## Common mistakes

- **Cheap capacitor.** Receive-grade air-variables arc and fail. Spend the money on a vacuum-variable or a wide-spaced transmitting air-variable.
- **Lossy joints.** Bolted joints on the main loop tube. Either solder them or use multiple parallel low-resistance bolts. A 0.1 Ω joint at 5 A of loop current dissipates 2.5 W per joint.
- **Capacitor too close to the loop.** The capacitor's stator and rotor act as part of the antenna. Add length and you change resonance unpredictably.
- **Touching the loop during TX.** RF burns and capacitive coupling distort the antenna. Don't even put your hand near the capacitor gap.
- **Trying to make a single loop cover too many bands.** Below about a 4:1 frequency ratio you're fighting physics. A 1-m loop is sensible for 14–30 MHz; for 7–14 MHz you want 1.5 m; for 80 m you want 2.5 m or more.

## See also

- §05-15 — Pattern (the figure-8 of a vertical mag loop)
- §07-07 — RF burns (mag loop voltages are no joke)
- §17 — Noise sources (loops null directionally)
- §05-14 — Modeling (NEC loops are tricky; prefer specialized loop calculators)
