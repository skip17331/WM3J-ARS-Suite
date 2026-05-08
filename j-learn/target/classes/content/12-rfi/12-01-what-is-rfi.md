---
id: 12-01
title: What is RFI
chapter: 12
section: 01
level: simple
status: draft
---

# What is RFI

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

RFI — Radio Frequency Interference — is unwanted RF energy that disrupts intended communication. Your radio receives it as noise that masks weak signals; or your transmissions cause unintended effects in nearby electronics; or both.

## Three categories of RFI

Three flavors, each with different fix paths:

### Conducted RFI

RF travels along wires (power, signal, antenna feedlines) instead of through the air. Examples:

- The hash from a switching supply travels backward through your shack power strip and into the radio.
- Common-mode current on coax shield (chapter §10-06) is conducted RFI.
- Noise from a neighbor's solar inverter travels along the AC mains into your house.

Conducted RFI is fixed by **breaking the conducted path** — chokes, isolation transformers, separate AC circuits, AC line filters.

### Radiated RFI

RF travels through the air as a normal radio wave. Examples:

- Your transmitter's RF energy reaching a poorly-shielded TV.
- A power-line corona discharge radiating across your antenna.
- A neighbor's plasma-screen TV radiating across the band.

Radiated RFI is fixed by **shielding the affected device**, **adding distance**, or **silencing the source**.

### Coupled RFI

Two circuits inductively or capacitively couple at audio or RF frequencies. Examples:

- An audio cable picking up RF from a nearby coax run.
- A transformer in one supply inducing 60 Hz hum into another supply's output.
- A laptop USB cable acting as a receiving antenna for a nearby switching supply.

Coupled RFI is fixed by **separating the circuits physically** or **shielding individual cables**.

Most real-world RFI involves all three at once. Diagnosis is about figuring out which path is dominant.

## What RFI sounds like

RFI sounds different from natural noise. A few classic patterns:

| Sound | Likely source |
|-------|---------------|
| Steady hash, no rhythm | Switching power supply, plasma display, dimmer at full |
| Buzz that varies with TV volume | Audio crosstalk via power line; TV-related |
| 60 Hz hum on every band | AC mains pickup; power line problem |
| Crackling that comes and goes | Bad insulator on a power pole; intermittent connection |
| Sequenced "tick tick tick" | LED light dimmer; refrigerator compressor; furnace blower; TV step-down regulator |
| Low-frequency throb | Pump motor; well pump; HVAC blower |
| High-pitched whine | Computer monitor; phone charger; LED driver |
| Modulated tone, like a car horn | AM broadcast station bleed-through; high-power local AM transmitter |
| Loud bursts at intervals | Dishwasher; washing machine motor; doorbell |
| Hash that fades during the day | Wirelessly-connected device; thermostat; security system |

§12-03 has audio examples and detailed identification methods.

## What you measure when you measure RFI

Several common metrics:

- **S-meter reading** of the noise floor on a quiet frequency. Most useful for "before vs after" comparisons.
- **dBm at the antenna terminals** — more precise than S-meter; requires a calibrated meter or a spectrum analyzer.
- **Signal-to-noise ratio** of a known weak signal — what really matters for operating.
- **Frequency range affected** — does the noise span 1.8–30 MHz, or is it just on 80 m? Width tells you about the source.
- **Time pattern** — constant, periodic, intermittent, weather-dependent.

## When RFI is "yours" vs "theirs"

Diagnostic question: **what changes when you turn off all the breakers in your house except the radio's?**

If the noise drops to nothing → your house is the source. Walk through the breakers turning them on one at a time to identify the affected circuit, then narrow to the device.

If the noise stays the same → an external source. Now you direction-find with a portable receiver.

This single test saves hours. Don't skip it.

## The "interference triangle"

For interference to occur, three things have to coexist:

1. **A source** that emits unintended RF energy.
2. **A receptor** that's affected by that energy.
3. **A coupling path** between them.

Removing or weakening any one of the three reduces the interference. Some examples:

| Approach | What it removes | Examples |
|----------|-----------------|----------|
| Eliminate the source | Source | Replace the offending switching supply with a quiet one |
| Harden the receptor | Receptor | Add ferrite to the affected device's power cable |
| Break the coupling path | Coupling | Move the source farther from the receptor; add shielding |

Often the easiest fix is to harden the receptor — most household electronics have very little RF immunity, and a $5 ferrite often helps. The hardest is eliminating the source, especially if it's a neighbor's device or a utility issue.

## Legal framework, briefly

In the US:

- **FCC Part 15** governs unintentional emitters (most consumer electronics). They must not cause "harmful interference" to licensed services and must accept any interference. This means: a TV that picks up your transmissions and starts buzzing is technically the TV's fault, not yours, by FCC definition.
- **FCC Part 18** covers ISM (industrial/scientific/medical) emitters.
- **Part 97** is amateur radio. You're allowed to transmit; your transmissions may or may not be the cause of a neighbor's problem.

In practice: the FCC won't intervene in most amateur-vs-neighbor interference disputes. You're expected to be a good neighbor, work with them to fix problems, and exhaust all reasonable avenues before escalating.

> ⚙️ **Advanced —** The "harmful interference" language in Part 15 has been tested in court repeatedly. The relevant case law (especially *In the Matter of Wayne Stam*) confirms that a Part 15 device that's susceptible to interference from a licensed transmitter is the device's problem, not the licensee's. But practical resolution rarely involves the FCC; the official process (filing a complaint, etc.) is slow and the FCC's enforcement budget is small. Most disputes are resolved informally between operators and neighbors.

## See also

- §12-02 — household RFI sources you might be making
- §12-03 — sound-based identification
- §12-05 — isolation workflow
- §13, §13 — specific source categories
