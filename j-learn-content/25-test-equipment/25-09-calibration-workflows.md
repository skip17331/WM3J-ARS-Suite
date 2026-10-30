---
id: 25-09
title: Calibration Workflows
chapter: 25
section: 09
level: mixed
status: published
---

# Calibration Workflows

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A test instrument is only as good as its calibration. An uncalibrated NanoVNA reports nonsense; a year-out-of-cal spectrum analyzer can be off by 3 dB; a dropped wattmeter can read 20% high or low forever after. Calibration is the discipline of **tracing every reading back to a known standard** so you can trust the numbers.

Amateur calibration isn't lab-grade and doesn't have to be — but the basic moves matter, and the cost of skipping them is reading bogus numbers and chasing nonexistent problems.

## What "calibration" actually means

Calibration has two related meanings depending on context:

### Field calibration

What you do **every session** with an instrument before measuring. Examples:

- SOL-calibrating a NanoVNA before sweeping an antenna
- Zeroing an analog wattmeter's meter needle
- Normalizing an SA + TG by capturing a "thru" reference
- Compensating an oscilloscope probe against the calibration pulse output

This is a quick procedure (30 seconds to 2 minutes) that removes session-to-session variability — different cables, different temperatures, different reference planes. It doesn't fix a broken instrument; it removes drift around the working point.

### Reference calibration

What a lab does **periodically** to your instrument to verify it agrees with traceable national standards. Examples:

- Sending a Bird 43 wattmeter to Bird Calibration Services for annual re-cal against NIST-traceable references
- Running an internal "self-test" on a Keysight VNA against built-in standards
- Adjusting an SA's amplitude calibration against a measured -20 dBm reference source

This is the deeper question of "is the instrument actually telling the truth?" — and is more expensive (lab cal fees of $200–1000 per instrument per year are common for commercial lab gear).

Amateur instruments rarely get formal reference cal. Instead, we use **cross-checking** against multiple instruments and known standards to build informal confidence.

## When to re-cal

Rules of thumb:

| Trigger | Action |
|---------|--------|
| **Annually** | Quick verification of every active instrument |
| **After travel** | Re-cal anything bumped in transit (NanoVNA, SA tip-of-probe especially) |
| **After dropping** | Anything with a moving meter (analog wattmeter, FSM) — verify zero and gain |
| **After tip / connector change** | NanoVNA after swapping cables; scope after swapping probes; SA after changing input attenuator |
| **Before a critical measurement** | Always verify before publishing or using the result in a permit application |
| **After temperature changes** (cold storage to warm shack) | Let instrument acclimate 30 min, then re-cal if reading changes |

A practical schedule for a working ham station:

- **NanoVNA**: SOL re-cal before every sweep session, save to a cal slot
- **Wattmeter**: yearly comparison against a calibrated reference
- **Scope**: monthly probe-cal check; annual self-cal sequence
- **Spectrum analyzer**: annual amplitude-cal check against a known signal source
- **Multimeter**: every 2 years for a Fluke; every 6 months for cheap meters

## Calibration procedures by instrument

### NanoVNA — OSL kit

The standard procedure, in detail:

1. **Connect calibration kit** to the analyzer.
2. **Set frequency range** wider than you need — calibrating at 1 MHz–1 GHz works fine even if you're only sweeping 14–14.5 MHz, because the analyzer interpolates between calibrated points.
3. **Set number of points** to 101 or more (more = slower cal but better interpolation).
4. **Press CAL → RESET** (clears any stored cal).
5. **Connect "Open" standard.** Press OPEN.
6. **Connect "Short" standard.** Press SHORT.
7. **Connect "Load" standard.** Press LOAD.
8. **For S21 measurements**: connect both ports together with a male-male barrel. Press ISOLN (isolation) and THRU.
9. **Press DONE.**
10. **Save to a cal slot** (CAL → SAVE → choose slot 0–4). Document which slot has what range — `slot 0: HF antenna sweeps (1-30 MHz)`, `slot 1: VHF antennas (50-500 MHz)`, etc.

Save baselines with each cal. A baseline sweep of a known-good antenna stored alongside the cal lets you detect cal drift later.

> **Advanced —** The included NanoVNA OSL kit assumes ideal standards. Real opens have ~30 fF of fringing capacitance; real shorts have ~30 pH of inductance. For HF amateur work this is negligible. For 6 GHz or 23 cm work, the assumption introduces ~1° of phase error and ~0.1 dB of amplitude error — too much for serious filter / matching work. Calibration kits with measured "polynomial coefficients" (Keysight 85033E, Maury) compensate for these standards' real values; the NanoVNA software typically doesn't load such corrections, which is why cheap NanoVNAs are limited above ~1.5 GHz.

### Spectrum analyzer — self-test and amplitude cal

Most bench SAs have a built-in calibration sequence:

1. **Power on, let warm up** for at least 30 min (longer for HP / Keysight gear; the front-end gain drifts with temperature).
2. **Run self-cal sequence** (varies by model — Siglent: CAL → ALL; Keysight: System → Alignments → All).
3. **Verify against an external reference.** Connect a calibrated signal generator at -20 dBm to the SA input. SA should read -20 ± 1 dBm.

If you don't own a calibrated signal generator, you can use a **commercial CW radio's published output power** at a known frequency, fed through a known-loss attenuator. A 100 W rig keyed into a 50 Ω dummy load, with a 60 dB attenuator (40 dB + 20 dB pad), gives -10 dBm at the SA — useful as a single-point check. Accuracy of this approach is ±2 dB because rig output power isn't precisely 100 W.

### Oscilloscope — probe cal and self-cal

Every digital scope has a small square-wave output on the front panel labeled "PROBE COMP" or similar, usually 1 kHz at 0.5 V. Procedure:

1. Connect the 10× probe to channel 1.
2. Clip the probe tip to the PROBE COMP terminal; ground clip to the chassis ground.
3. Display the 1 kHz square wave on screen.
4. **Adjust the probe's compensation trimmer** (small screwdriver-adjustable capacitor near the BNC end) until the square wave has perfectly flat tops:

```
   Overshooting probe:        Drooping probe:           Correctly compensated:
       ┌─╲                       ┌╲                          ┌────┐
       │ ╲___                    │ ╲___                      │    │
       │     ___                 │     ───                   │    │
                                                              ╲___╱
       (too much C)              (too little C)               (perfect)
```

Self-cal: most modern digital scopes have an internal calibration routine that takes 5–10 minutes. Run it after a temperature swing or before precision measurements.

### Power meter — comparison against a reference

The standard ham power-meter cal:

1. Connect rig → calibrated reference wattmeter → unit-under-test (UUT) → dummy load.
2. TX a clean carrier (CW, 50% of UUT's range — e.g. 50 W if UUT is 100 W rated).
3. Read both meters. UUT should agree with reference within UUT's spec'd accuracy (typically ±5% or ±10%).
4. If they don't agree, either (a) adjust the UUT's internal cal pot if accessible, or (b) note the offset for compensation.

Without a reference wattmeter, you can use a **commercial rig's spec** — a modern HF rig set to "100 W output" usually delivers 95–105 W actual. Not lab-grade but a single useful sanity point.

### Multimeter — comparison against a reference

Annual procedure: read a known reference voltage and current.

- **Voltage reference**: a 9 V alkaline battery is ~9.0 V fresh; a 1.5 V alkaline is ~1.55 V fresh. A precision reference (LM4040, AD584) gives 4.096 V or 5.000 V or 10.000 V at ±0.1% accuracy for $5 in parts.
- **Resistance**: a 0.1% precision resistor (any value 100 Ω to 100 kΩ) gives a reference point.

If your multimeter reads ±0.5% of the references, it's working.

## Cross-checking — the amateur substitute for traceability

You probably don't own NIST-traceable standards. The amateur substitute is **cross-checking**: measure the same thing with multiple instruments and look for agreement.

Example: measuring the output power of your rig.

1. **Method A:** Rig → Bird wattmeter → dummy load. Read 95 W.
2. **Method B:** Rig → 30 dB attenuator → SA. Compute SA reading (-15 dBm) back to source = +15 dBm + 30 dB = +45 dBm = ~32 W. **Wait — these don't agree.** Recheck attenuator values; the 30 dB pad is actually a 35 dB pad mislabeled. Now reading +50 dBm = 100 W. ✓
3. **Method C:** Calorimetric — submerge a 50 Ω water-cooled dummy load, measure water temperature rise vs. time, compute average power. 100 W ± 5 W. ✓

Three methods agreeing to within 5% is strong informal evidence that all three are calibrated correctly. One disagreement points to the bad instrument.

> **Advanced —** True NIST traceability requires an unbroken chain from your instrument to a national standard. Each link in the chain has an uncertainty budget, and uncertainties combine in quadrature. A professional lab's traceability documentation shows the entire chain explicitly. For amateur work this is overkill, but the *concept* is useful: when you measure something, ask "what's my chain back to a known reference?" If the chain is broken or unknown, you don't actually know the true value — just the consistent reading.

## Documenting calibration

Keep a simple log per instrument:

```
NanoVNA-H4 #2:
  2025-10-15: SOL-cal verified against 50 Ω precision load (Maury).
              Showed 50.1 Ω ± 0.2 Ω across 1-100 MHz. ✓
  2026-04-10: Re-cal after travel. SOL with included kit. Saved to slot 0.
  2026-05-01: SOL re-cal for J-Map antenna baseline. Slot 1.
```

For a club station with multiple users, a calibration sticker or card with "next cal due: <date>" is the standard.

## Common mistakes

- **Trusting a never-calibrated instrument.** Cheap meters drift with temperature and age. A reading without recent cal is a guess.
- **Cal-then-bump-the-probe.** Cal is invalidated as soon as the probe or cable changes. Re-cal after any physical change in the signal path.
- **Mixing reference planes.** SOL-cal at port A, then measuring at port B's reference plane: the difference between A and B is uncorrected. Either re-cal at B or use port extension.
- **Skipping warm-up.** SA, signal gen, frequency counter all drift cold-to-warm. 30 min warm-up minimum for precision work.
- **Not documenting the cal.** "I think I calibrated this a few months ago" is not a cal log. Write it down.

## See also

- §25-01 — NanoVNA Advanced (SOL kit detail)
- §25-03 — Spectrum Analyzers (calibration of)
- §25-06 — Power & SWR Meters (calibration of)
- §25-02 — Oscilloscopes (probe cal)
- §13 — Station Troubleshooting (where calibrated readings matter)
