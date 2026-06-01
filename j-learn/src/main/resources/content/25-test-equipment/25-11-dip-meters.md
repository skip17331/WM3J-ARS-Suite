---
id: 25-11
title: Dip Meters
chapter: 25
section: 11
level: mixed
status: published
---

# Dip Meters

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A **dip meter** — historically a **grid-dip oscillator (GDO)** in the tube era, now usually solid-state — measures the **resonant frequency of an unpowered LC circuit**. You don't connect it; you bring its tunable coil near the circuit under test. When the meter's oscillator frequency matches the circuit's resonance, the circuit absorbs energy from the meter, the oscillator's amplitude drops, and the meter needle **dips**. Read the dial: that's the resonant frequency.

It is one of the few instruments that measures circuits while they are powered off, with no electrical connection at all. That makes it uniquely good for things you can't easily probe with a VNA — coils inside a sealed trap, tank circuits inside an old transmitter, parasitic resonances in homebrew chassis wiring, the LC tank of a crystal oscillator.

Dip meters were everywhere in amateur shacks from the 1940s to the 1980s, then mostly fell out of fashion as VNAs and analyzers got cheap. They're making a quiet comeback as homebrewers rediscover that some measurements really are easier with a dip meter than anything else.

## What a dip meter is

A self-contained low-power RF oscillator with a **plug-in coil** at the front, a **tuning capacitor**, a **calibrated frequency dial**, and a **meter** displaying the oscillator's RF level.

```
                ┌───────────────────────────────┐
                │  ●  ●  ●  ●        ┌──────┐   │
                │   plug-in coil      │ DIAL │   │ ← frequency
   external ┐   │     ┃ ┃ ┃ ┃         │  ⊙   │   │
   LC tank  │   │     ┻━┻━┻━┻─┐       └──────┘   │
   under    └─►   ┌──┴───────┴─┐                  │
   test            │  oscillator  │   ┌────────┐   │
                  │  + det + AGC │──►│ METER  │   │ ← needle dips
                  └──────────────┘    │   ⊙   │     when LC tank
                                       └────────┘    resonates
                └───────────────────────────────┘
```

Twist the tuning knob to sweep the oscillator's frequency. When the oscillator's frequency matches the resonant frequency of the LC circuit you're holding near the coil, the circuit absorbs energy from the oscillator and the meter needle "dips" — drops sharply, then climbs back as you tune past resonance.

Different bands need different plug-in coils, just like a multi-band rig. A typical kit covers roughly 1.5 – 250 MHz with five or six coils.

## How resonance shows up on the meter

As you tune the dial, the meter reading looks like this:

```
   Meter reading
   ▲
   │   ●●●●●●●●●●           ●●●●●●●●●●●●●●●  ← normal oscillator level
   │              ●●●     ●●
   │                ●●● ●●
   │                  ●●●           ← THE DIP at LC tank's f_resonant
   │
   └────────────────────────────────────── tuning dial frequency
                       ↑
                f_resonant of tank under test
```

The depth of the dip depends on coupling. **Loose coupling** (coils far apart) gives a shallow but accurate dip; **tight coupling** gives a deep dip but pulls the indicated frequency slightly (the dip meter's coil becomes part of the tank circuit). Tune to find any dip with tight coupling, then back off to as loose a coupling as still gives a visible dip, and read the dial — that's the most accurate technique.

## What a dip meter tells you

**Only one thing:** the frequency at which an LC circuit resonates.

From that one fact you can derive many useful things, because resonance is `f = 1 / (2π · √(LC))`:

- If you know the capacitance, you can compute the inductance: `L = 1 / (4π² · f² · C)`.
- If you know the inductance, you can compute the capacitance: `C = 1 / (4π² · f² · L)`.
- If the device under test is a sealed trap, the dip directly tells you the trap's trap-frequency (the band it's suppressing) without disassembly.
- If a tank circuit in a transmitter is supposed to resonate at 14 MHz but the dip is at 13.4 MHz, something has drifted — bad capacitor, slugged coil, contamination.

It does **not** tell you the Q, the loss, the impedance at any other frequency, or anything about how the LC circuit will actually behave under power. For those, you need a VNA, oscilloscope, or signal generator.

> **Advanced —** A dip meter is a Colpitts or Hartley oscillator with an external coil. Its resonant frequency is set by the coil and a variable capacitor; the meter measures rectified RF voltage at the cathode (tube) or collector (transistor). When an external LC tank loosely couples to the oscillator coil, it presents a high impedance only at its resonant frequency — that impedance, transformed through the mutual inductance, increases the loading on the oscillator and damps its amplitude. The needle "dip" is literally the oscillator losing gain.

## Using a dip meter — the basic procedure

1. **Power off the device under test.** A dip meter measures resonance of *passive* LC tanks. If the circuit is powered, the dip meter will instead receive the signal generated by that circuit — useful in another way (see "Wavemeter mode" below) but not what you're doing here.
2. **Plug in the coil** for the band you expect resonance to fall in. Choose the next size up if uncertain — wide range first, then narrow down.
3. **Place the dip meter's coil near the LC tank under test.** Orient coils so their magnetic fields couple (axes parallel, coils close but not touching).
4. **Tune the dial slowly** while watching the meter. Look for any sudden, narrow dip in the needle position.
5. **If you see a dip:** back the coupling off (move the dip meter away) until the dip is barely visible, then read the dial. This is the resonant frequency.
6. **If you see no dip:** try a different coil to cover a wider/narrower frequency band, then sweep again. Multi-band sweeps catch parasitic resonances at unexpected frequencies.
7. **Verify with a receiver.** The dip meter is also a low-power transmitter. Tune a nearby receiver to the dial frequency — you should hear an unmodulated carrier. This confirms the dial calibration is right.

## Testing a coil

You have a hand-wound coil and want to know its inductance. A dip meter alone can't measure inductance, but **dip meter + a known capacitor = inductance meter**.

```
   ●─────●  ← clip leads from the test coil
   ┃     ┃
   ┃     ╪═════╪  ← KNOWN capacitor in parallel (e.g. 100 pF, NP0/C0G)
   ┃     ╪═════╪
   ┃     ┃                              ┌─────────────────┐
   ●─────●                              │  DIP METER      │
        coil under                      │   (loosely      │
        test                            │    coupled      │
                                         │    to the LC    │
   ◢ coupled loosely ◣ ←──────────────── │    pair)        │
                                         │                 │
                                         └─────────────────┘
```

Procedure:

1. **Solder or clip a known capacitor** — a known-good NP0/C0G ceramic, say 100 pF — across the coil's terminals. This makes a parallel LC tank.
2. **Bring the dip meter near** the new LC pair, oriented for magnetic coupling.
3. **Sweep and find the dip** as before. Suppose the dip is at 5.03 MHz.
4. **Compute inductance:** `L = 1 / (4π² · f² · C)`. Plugging in f = 5.03 × 10⁶ Hz and C = 100 × 10⁻¹² F: `L = 1 / (4 · π² · (5.03e6)² · 100e-12) ≈ 1.0 × 10⁻⁵ H = 10 μH`.

> **Advanced —** The capacitor must be much larger than the coil's self-capacitance and the dip meter's coupling capacitance, or the result is wrong. NP0/C0G is preferred because the capacitance is stable with temperature; X7R or worse will pull resonance several percent across a normal shack temperature swing. Use the Formulas calculator in J-Learn (§17) to cross-check the math.

## Testing a trap

An antenna trap is a parallel LC tank designed to resonate at one band — it presents a high impedance there, blocking that band from passing further out on the antenna. A 7 MHz trap, for example, makes a wire act as a 7 MHz dipole while the outer portion of the wire (past the trap) is electrically disconnected at 7 MHz; on other bands, the trap is mostly inductive and is transparent.

Traps go bad — moisture intrudes, capacitors shift, parasites collapse. The dip meter is the right instrument for "is this trap still tuned to 7 MHz?" because the trap is a **sealed LC tank with no electrical access**.

```
              ╲ wire to outer dipole leg
               ╲
                ●─────────●
                ┃         ┃    ← TRAP
                ┃    L    ┃       sealed case, plastic or aluminum,
                ┃   ┃ ┃   ┃       contains a coil L and a cap C
                ┃   ┃ ┃   ┃       resonant at the trap frequency
                ┃    L    ┃
                ┃   ┃ ┃   ┃
                ┃   ╪═C═╪┃
                ●─────────●
               ╱
              ╱ wire to inner dipole leg

              ◢ loosely coupled ◣

       ┌──────────────────────┐
       │ DIP METER coil hovers │   sweep the dial; needle dips at
       │ ~3 cm from trap body  │   the trap's resonant frequency
       └──────────────────────┘
```

Procedure:

1. **Disconnect the trap from the antenna** — both ends, so it's a floating LC tank. (Some operators do this with the antenna lowered and the trap accessible; ladder-line and wire dipoles let you reach the trap easily.)
2. **Plug in the dip meter coil** that covers the expected trap frequency. For a 7 MHz trap, the 5–10 MHz coil.
3. **Position the dip meter coil** near the trap. Aluminum-housing traps couple weakly — bring the coil within 2–3 cm of the trap body, alongside the coil portion of the trap (usually visible as a wound section, not a solid plate).
4. **Sweep the dial.** Look for a dip. For a healthy 7 MHz trap, expect the dip somewhere between 6.9 and 7.1 MHz; manufacturer specs sometimes intentionally tune slightly above the operating band.
5. **Read the dial at the dip.** If the dip is where it should be, the trap is fine. If the dip has drifted (say 6.4 MHz instead of 7.0), the trap's capacitance has shifted — moisture, corrosion, or a leaky cap. Some traps can be opened and re-tuned; others must be replaced.
6. **If no dip can be found** anywhere with any coil, the trap is dead — open coil, open capacitor, or the trap's mechanical assembly has lost continuity.

> **Advanced —** Some commercial traps (Hy-Gain, Mosley) have an aluminum housing that acts as a shorted turn. The shorted turn lowers Q and reduces the visible dip depth. Use the most-sensitive setting on the dip meter and bring the coil very close (1–2 cm). If still no dip, gently remove an end cap and probe the internal coil — that's where the LC tank lives. Always restore weatherproofing before re-installing on the antenna.

## Testing other tuned circuits

Dip meters work on any reasonably-Q'd LC tank. Useful applications:

- **Crystal oscillator tank circuits** — verify the plate tank in a tube transmitter resonates at the expected harmonic.
- **Resonant traps in low-pass filters** — the series-LC notches in a Chebyshev low-pass filter dip at their notch frequency.
- **RF chokes** — every RF choke has a self-resonant frequency. A 2.5 mH HF choke might self-resonate at 8 MHz — meaning above 8 MHz it acts like a capacitor, not a choke. Dipping the choke reveals this.
- **Antenna loading coils** — the helical coil at the base of a 2 m vertical, or the centre loading coil on a 40 m mobile whip, has a resonance you can measure.
- **Helical resonators** — copper-tube quarter-wave resonators in VHF/UHF cavities dip cleanly at their pass frequency.

## Wavemeter mode (passive listening)

Most dip meters can also work the other way: as a **wavemeter**. Power up the device under test (an oscillator, transmitter, signal generator), bring the dip meter near it without it oscillating, and tune the dial. Where the meter needle **peaks** (rather than dips), the dial reading is the frequency of the active signal.

This is a quick check on a homebrew oscillator: "is this thing oscillating at the design frequency, or is it on a parasitic somewhere else?" Dip meter in wavemeter mode answers in 30 seconds, no scope needed.

## Modern alternatives and replacements

- **NanoVNA S11 sweep.** Connect a small pickup coil (a loop of wire ~3 cm diameter, soldered to a UHF connector) to port 1 of a NanoVNA, sweep S11 magnitude, hover the loop near the LC tank under test. Resonance shows as a sharp dip in S11. This is the modern dip meter, in effect.
- **Tinkerable digital dip meter (TDDM)** kits. Inexpensive (~$30) microcontroller-based dip meters with LCD readout. Less elegant than a 1960s GDO, but accurate.
- **Used dip meters at hamfests.** Eico 710 series, Heathkit HD-1250, Measurements Corp model 59 — these turn up for $20–$80 at hamfests and work indefinitely after a quick recap.

## Common gotchas

- **The dip meter is a low-power transmitter.** It puts out a few milliwatts in the band where it's tuned. Don't sweep through aviation, military, or amateur sub-bands you don't have privileges on — you're technically transmitting. Most amateurs use them anyway during testing without ill effect, but be aware.
- **Coupling pulls frequency.** Tighter coupling = lower indicated frequency. Always confirm by backing the coupling off until the dip is barely visible, then read.
- **Multiple dips.** A given LC tank may dip at its fundamental and at the harmonic of an adjacent winding mode. The lowest-frequency dip is usually the desired resonance.
- **Dial calibration.** Old analog dip meters drift. Cross-check with a known-good signal generator or with a receiver of known calibration. A 1% dial error is common in 50-year-old instruments.
- **Body proximity matters.** Your hand near the coil shifts the resonance. Set the meter on a non-conductive bench surface and avoid putting hands inside the coil shroud.

## See also

- §06 — Antennas (overview of trap-loaded antennas)
- §17 — Formulas (LC resonance math: `f = 1 / (2π√LC)`)
- §25-01 — NanoVNA — Advanced Techniques (the modern equivalent of dip measurement)
- §25-10 — Antenna Analyzers (the other "what does this antenna want to do?" instrument)
- §26 — Homebrewing & RF Construction (where dip meters are most useful)
