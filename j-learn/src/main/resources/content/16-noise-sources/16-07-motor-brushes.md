---
id: 16-07
title: Motor Brushes
chapter: 16
section: 07
level: simple
status: draft
---

# Motor Brushes

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A "brushed" motor uses sliding carbon contacts (brushes) against a rotating commutator to deliver power to the rotor windings. The act of brush-on-commutator contact creates **constant micro-arcs**, which radiate broadband HF noise. Vacuum cleaners, hair dryers, drills, mixers, blenders, and old ceiling fan motors are common offenders.

## How it works

When a motor rotates, the brush slides across one commutator segment, then onto the next. At the transition:

- The current in one winding has to abruptly stop.
- The current in the next winding has to abruptly start.
- The brush briefly bridges both segments.
- The inductance of the windings causes a voltage spike (the back-EMF) at each commutation.

That voltage spike is hundreds of volts in a fraction of a millisecond — a textbook generator of HF noise. The spike radiates across the entire HF spectrum.

In a healthy motor, the noise is contained somewhat by the motor's case and the AC mains filter (if any). In a worn motor, the brushes have eroded, the commutator has grooves, the spring tension is wrong, and the arcing is much worse.

## Common offenders

| Device | Type | Typical noise level |
|--------|------|---------------------|
| **Vacuum cleaner** | Universal motor (high RPM brushed) | Loud |
| **Hand mixer / blender** | Universal motor | Loud, intermittent |
| **Hair dryer** | Universal motor | Moderate |
| **Power drill (corded)** | Universal motor | Loud during use |
| **Bench grinder, table saw** | Universal motor | Loud |
| **Old ceiling fan** | Capacitor-start AC motor (no brushes — but may have other issues) | Generally quiet now |
| **Furnace blower (older)** | Brushed AC motor | Constant when running |
| **Garbage disposal** | Universal motor | Loud, brief |
| **Treadmill / exercise equipment** | DC motor with brushes + speed control | Variable |

## Sound character

- Harsh, scratchy hash that varies with motor RPM.
- Often modulated by the AC line frequency (60 Hz) — listen for a 60 Hz beat.
- **Worse with a worn motor.** A new vacuum cleaner is annoying; a 10-year-old vacuum cleaner is much worse.
- Stops completely when the motor stops.

## Distinguishing from switching supply noise

A switching supply produces continuous, steady hash that's the same level whether the device is "active" or in standby. A motor's brush noise is **intermittent**, only present when the motor runs, and usually correlates with motor RPM (audible humming pitch).

## Fixes

### Replace with a brushless motor

The fundamental fix. Modern brushless DC motors (BLDC) and electronically commutated motors (ECM) don't have brushes and don't generate this kind of noise. Common in:

- Modern cordless power tools (Milwaukee Fuel, DeWalt XR Brushless, Makita LXT Brushless).
- Newer vacuum cleaners (Dyson, Shark — most modern brands).
- Modern HVAC blowers (ECM).
- Many newer appliance motors.

When replacing brushed equipment, prefer brushless versions. Cost difference is usually small now (the technology has matured).

### Replace worn brushes

For an older motor that's gotten louder, the brushes may be worn down and arcing more. Symptoms:
- Visible spark inside the motor when running.
- Brushes shorter than spec (compare to spare).
- Commutator surface scored or pitted.

Fix: replace brushes (about $10–20 in parts) and have a tech "true" the commutator surface. Worth it for a $500 vacuum, not worth it for a $30 mixer.

### Add a snap-on ferrite

For an existing motor that's noisy but you're not replacing it: snap-on ferrite at the motor's power cord, as close to the motor as possible. Use mix 31 or 43, with as many turns as fit through the snap-on (typically 3–5).

This typically reduces the radiated noise by 6–10 dB. It doesn't fix the underlying arcing but reduces what gets out the cord into the house wiring.

### Add a capacitor across the brushes

Some brushed motors come from the factory with small capacitors (0.01 µF, AC-rated) wired across the brushes specifically to suppress arcing noise. If yours doesn't, you can add them. Two caveats:

- Has to be done with the motor disconnected (electrical work).
- The capacitor must be rated for the AC voltage and the motor's surge environment.

This is a manufacturer-level fix; some manufacturers omit it to save cost.

### Use the device when not operating

A vacuum cleaner that's used 30 minutes a week is a tolerable RFI source if it never runs during your operating times. Schedule cleaning before/after operating sessions.

## A note on cordless tools

Modern cordless tools (lithium-ion battery powered) are typically brushless and quiet, BUT:

- The battery charger (when charging) is its own SMPS source — see §16-06.
- Some older lithium-ion tools used brushed motors with battery-powered drives that could still be noisy.

Replacement of corded tools with cordless brushless is generally a step-improvement in RFI.

## Specific case: vacuum cleaners

Standard household vacuums use universal motors at 15,000–30,000 RPM. They're loud both acoustically AND on the air. Approaches:

- **Dyson V-series, Shark Apex** — brushless. Generally quiet on air.
- **Old shop-vac with universal motor** — loud. Add ferrite or operate during non-operating hours.
- **Robot vacuums (Roomba, etc.)** — generally brushless and quiet, but have built-in switching supplies (chargers and battery balancers).

## When the motor is not the source

Sometimes "vacuum cleaner noise" turns out to be the vacuum's electronic motor controller, not the motor brushes. Modern variable-suction vacuums use SCR or triac speed controls that chop the AC waveform. The chopped waveform has its own characteristic noise — different from brush arcing.

If the noise persists at one specific suction setting but disappears at others, suspect the speed control rather than the brushes.

## See also

- §16-05 — HVAC (which includes motor-related noise too)
- §16-01 — switching supplies
- §15-04 — ferrite selection
