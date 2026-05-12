---
id: 26-09
title: Grounding for Homebrew
chapter: 26
section: 09
level: mixed
status: draft
---

# Grounding for Homebrew

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## Why grounding is the silent killer

A circuit with a perfect schematic and quality parts can still hum, oscillate, or pick up RFI — and the cause is almost always **bad grounding**. The schematic shows a clean triangle pointing at "GND" everywhere. In hardware, GND is a piece of copper or steel with finite resistance and finite inductance, and **current flowing through it creates voltage drops that appear as noise everywhere**.

This section is about *small-chassis* grounding — the wires and traces inside a homebrew box. Whole-station grounding (the wire from the chassis to the ground rod outside) belongs to §27.

## Star grounding — the one rule that matters

The single most important rule in homebrew chassis grounding: **every ground returns to ONE point**. That point is the "star," and the wires from each subcircuit are the points of the star.

```
       Subcircuit 1
          │
          │
Subcircuit 4 ──── ★ ──── Subcircuit 2
          │
          │
       Subcircuit 3

★ = the single ground point
```

The star is typically a brass screw post or a bolt through the chassis where all ground wires meet. Each subcircuit (oscillator, mixer, power amp, audio, DC input) has *its own dedicated ground wire* to the star. No subcircuit shares its ground wire with another.

### Why star grounding works

Consider what happens *without* star grounding:

```
DC supply ──┬── Audio amp ──┬── RF amp ── Antenna
            │               │
            └─── GND ───────┘
                  ↑
            Audio current (1 A) and RF current (1 A) both flow
            through this SHARED ground wire. The voltage drop
            across the wire's resistance + inductance appears in
            BOTH subcircuits' grounds.
```

A 6-inch piece of 22-gauge wire has ~50 mΩ of resistance and ~150 nH of inductance. At audio frequencies, 1 A of audio current creates 50 mV of voltage drop. At 14 MHz, 1 A of RF current creates V = I × X_L = 1 × 13.2 Ω = 13.2 V of RF voltage drop — that's not "ground" at the far end of the wire; that's *14 MHz noise* coupled into whatever else uses that wire.

The fix: **separate the return paths**. Audio current flows on the audio's own wire back to the star. RF current flows on the RF circuit's own wire back to the star. The two never share, so the audio circuit never sees RF on its ground, and vice versa.

## What to ground to the star

A typical homebrew transmitter or receiver has these "ground" subcircuits, each getting its own star wire:

- **DC input ground** — the return from the power supply
- **Audio ground** — input mic / output speaker returns
- **Low-level RF ground** — receiver front end, mixer, IF
- **High-level RF ground** — final amplifier, output filter
- **Digital ground** — keying / control circuits if any
- **Chassis ground** — the metal box itself

The star is often *not* the chassis itself — it's a separate single point inside the box, with one final short wire from the star to the chassis. This way the chassis "ground" is one branch of the star, not the meeting point of everything.

> ⚙️ **Advanced —** Some designs (Doug DeMaw W1FB's *QRP Notebook*; Wes Hayward W7ZOI's *Solid State Design*) use **multiple stars** in cascade — a "tree" structure. The transmitter front end has its own local star; the final amplifier has another; the audio section has a third. Each local star connects to a master star via a single wire. This works for complex designs but adds discipline; if you're a beginner, stick with a single star.

## Ground loops — the disease star grounding cures

A **ground loop** is a closed-circuit ground path that forms when two ground points are connected by two different wires (or one wire and the chassis). The loop has finite inductance; magnetic fields from nearby AC mains or RF cables induce a small voltage around the loop, driving current through the loop wires. That current creates voltage drops, which appear as noise.

The classic ground-loop symptom: **60 Hz / 120 Hz hum on every audio path** that gets worse when you touch certain wires. The fix is to break the loop — disconnect one of the redundant ground paths so current can no longer circulate.

Star grounding prevents loops by construction: with all grounds meeting at one point, there's no closed loop to circulate current through.

## Bonding the ground plane to the chassis

In a PCB-based homebrew design, you have a ground plane on the PCB **and** the metal chassis. These need to be bonded to each other to maintain a unified ground reference. But — and this is the subtlety — the bond must be **multiple short straps**, not a single wire.

Why multiple straps? A single wire has inductance, which becomes significant at HF. A 4-inch wire is ~100 nH; at 14 MHz, X_L = 9 Ω. That's not a "short" between the PCB ground plane and the chassis — that's a *significant impedance*. Currents trying to return through this single wire create voltage drops, and the PCB ground "floats" with respect to the chassis at RF.

Two or four short straps (1/4" wide braid, less than 1" long) in parallel give:

- Lower DC resistance
- Lower inductance (parallel inductors)
- Multiple distributed bond points across the PCB

For a small board (under 4"), two diagonal bonds are typically enough. For a larger board, four corner bonds.

## Separating signal ground from chassis ground at I/O

A subtle but important detail: at the connectors (BNC, SO-239, RCA, etc.), the connector shell is *chassis ground*. The PCB ground plane is *signal ground*. Should the two be tied together at the connector, or kept separate?

**For RF connectors (BNC, SO-239)**: **tie them together at the connector**. The signal coming out of the radio expects shield-grounded coax; the PCB's signal ground and the chassis's chassis ground must meet at the connector to avoid common-mode current on the connecting cable.

**For audio / line-level connectors (RCA, 1/4" TRS)**: there's a school of thought (the "differential ground" approach) that says keep them separate, run a separate ground from the signal source's reference back to the receiving end's reference, and only bond at the receiving end. This is the "telescoping ground" or "Pin 1 problem" fix that audio engineers know well. For amateur audio interfaces between a radio and a computer (data modes), this is often the cure for a stubborn hum problem.

**For DC power**: ground the negative lead to the chassis at the *power-input feedthrough* and tie the PCB DC ground to the chassis there. This is one of the star's branches.

## Common chassis-ground mistakes

- **Daisy-chained grounds.** Subcircuit A's ground goes through subcircuit B's ground to reach the star. This *is not* a star; it's a serial ground.
- **Long ground wires.** A 6-inch ground wire isn't "ground" at RF — it's an inductor. Keep ground returns under 2".
- **Paint on the chassis** under a ground lug. The paint is an insulator; tighten the lug, get continuity check, and it might be 5 Ω instead of 0.05 Ω. Scrape paint to bare metal where ground bonds.
- **A single bolt through cardboard.** A "ground" lug on a fiberboard chassis is not ground. The chassis material has to be metal end-to-end.
- **The "earth ground" misconception.** The 8-foot copper rod outside is *for lightning and safety*. Inside the radio, "ground" is the chassis. The two are bonded together for safety but they are *not* the same node for signal-integrity purposes.
- **Forgetting to bond the lid.** If you have a metal lid on a metal box, the lid is part of the ground only if the screws make contact. Painted or anodized lids may not bond unless threads are scraped bare.

## Quick checklist for a new build

When you finish wiring a homebrew project and before powering on:

1. **One star point identified.** Sharpie a small dot on the inside of the chassis.
2. **Every ground wire returns to the star.** Trace each subcircuit's ground; verify it doesn't go through another subcircuit's ground.
3. **PCB ground plane bonded to chassis** with at least two short straps.
4. **All RF connector shells bonded to chassis** at the connector.
5. **DC input has a feedthrough cap to chassis** at the entry point.
6. **No ground loop** — visually trace each ground; confirm no closed loop exists.
7. **Continuity check**: 0 Ω from any chassis point to any other chassis point.
8. **Continuity check**: 0 Ω from star to chassis (one short bond wire).

A 10-minute pre-power-on check saves hours of "where is this hum coming from" troubleshooting.

## See also

- [§26-08 — Enclosures & Shielding](26-08-enclosures-shielding.md) — chassis bonding details
- [§26-10 — PCB Layout Basics](26-10-pcb-layout-basics.md) — ground plane via stitching
- [§27 — Station Engineering](../27-station-engineering/) — whole-station grounding and bonding
- [§14 — RFI](../14-rfi/) — common-mode current diagnosis
- [§13 — Station Troubleshooting](../13-station-troubleshooting/) — finding hum and noise
