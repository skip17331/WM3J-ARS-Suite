---
id: 25-00
title: Test Equipment & Measurement — Overview
chapter: 25
section: 00
level: mixed
status: published
---

# Test Equipment & Measurement — Overview

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## Why a chapter on test equipment

You can operate amateur radio for years with nothing but the rig's own S-meter and SWR readout. You can also operate badly for years that way. A serious amateur eventually picks up a handful of instruments — not because every QSO needs them, but because **the questions you can answer with them are different from the questions you can answer without them**.

"Is my antenna actually resonant on 14.200, or is my tuner just hiding a problem?" → VNA answers in 30 seconds; without one, you guess.

"Why is my neighbor's TV interfering with my 2 m rig?" → A spectrum analyzer shows the second harmonic at 290 MHz that the SWR meter never sees.

"How much power am I really running on SSB?" → A peak-reading wattmeter shows 100 W PEP; an average-reading meter shows 20 W and convinces you the rig is sick.

This chapter goes instrument by instrument. What it measures, how to read it, the common ham use cases, the common mistakes, and the calibration discipline that keeps the readings honest.

## The instruments a serious amateur owns or borrows

A reasonable "shack instrument set" for a working HF/VHF station looks like this. Not every operator owns all of these — many are borrowed, shared at a club, or bought as projects arise.

| Instrument | What it does | First-pass cost | Covered in |
|------------|--------------|-----------------|------------|
| **VNA** (Vector Network Analyzer) | Complex impedance vs. frequency | $50 (NanoVNA) | §25-01 |
| **Oscilloscope** | Voltage vs. time, waveform shape | $300 (entry digital) | §25-02 |
| **Spectrum analyzer** | Signal amplitude vs. frequency | $130 (TinySA Ultra) | §25-03 |
| **Tracking generator** | SA companion — frequency response sweeps | bundled or $200+ add-on | §25-04 |
| **Field strength meter** | Relative radiated field at a point | $20 (diode-detector kit) | §25-05 |
| **Power / SWR meter** | Forward & reflected power | $50 (Daiwa CN-801) | §25-06 |
| **TDR** | Cable length, fault location | NanoVNA does this | §25-07 |
| **Antenna analyzer** | SWR / R / X at the antenna feedpoint | $130 (RigExpert AA-35) | §25-10 |
| **Dip meter** | Resonant frequency of any LC tank (incl. sealed traps) | $30 (kit) – $80 (used) | §25-11 |
| **Multimeter** | DC volts/amps/ohms | $30 (Fluke 101); $100+ (Fluke 117) | covered in §13 |

A NanoVNA + a TinySA + a Daiwa cross-needle + a 100 W dummy load + a decent multimeter covers 90% of amateur measurement work for under $400 total.

## Cost tiers

Instrumentation runs across three rough tiers.

### Cheap end — hobby instruments ($30–$300)

**NanoVNA**, **TinySA**, **RTL-SDR**, **Daiwa CN-801**, **MFJ-269** analyzer, diode-detector field strength meter. Built on consumer-grade silicon (SoCs, CMOS receivers), runs on small batteries or USB, "good enough" accuracy for amateur work (typically 1–3 dB calibration uncertainty).

These are revolutionary — instruments that cost $5,000 in 1990 are now $50 and fit in a pocket. The trade-off is accuracy: a NanoVNA at 1 GHz drifts more than an HP 8753; a TinySA can't separate two signals 100 Hz apart. For trimming an antenna, finding a harmonic, or measuring cable loss, that's fine.

### Mid tier — used lab gear and modern bench ($300–$3000)

**Rigol DS1054Z**, **Siglent SDS-1104X-E**, **Siglent SVA1015X** VNA/spectrum analyzer combo, **Bird 43** wattmeter with slugs, used **HP 8920** service monitors, used **Tektronix 2465** analog scopes. Chinese mid-range bench instruments and US/Japanese gear from the 1990s.

This is where most active amateur experimenters live. The bandwidth, dynamic range, and reliability are vastly better than the cheap tier, but you pay several hundred to a few thousand dollars per box.

### High end — professional instruments ($3000+)

**Keysight (formerly Agilent / HP)**, **Rohde & Schwarz**, **Tektronix MSO series**. New, these are $10k–$100k+ boxes used by RF design houses, calibration labs, and the FCC. Used HP 8753 VNAs, HP 8566 spectrum analyzers, and Tektronix 7000-series mainframes show up at hamfests for $500–$2000 and are still excellent.

For amateur work, the high end is rarely necessary — but a used HP 8753C VNA is a beautiful instrument, accurate to fractions of a dB at 6 GHz, and an absolute pleasure to use after a NanoVNA.

> **Advanced —** The accuracy spec to pay attention to when buying used lab gear is **calibration uncertainty** at the frequencies you care about. A used HP 8566B is excellent at 1 GHz but rolls off badly above 22 GHz; a Tek 7L13 is gorgeous at 100 MHz but flat-tops at high signal levels. Read the service manual's spec section — not the marketing copy — before buying.

## Borrow, share, build

You don't have to buy everything. Three other paths:

- **Club station.** Many radio clubs own a spectrum analyzer, network analyzer, and signal generator that members can use. Ask before you spend $1000.
- **Hamfest deals.** Older lab gear is regularly under $500 at hamfests. The trade-off: no warranty, possibly out-of-cal, possibly broken. Bring a portable signal source to check before paying.
- **Build kits.** The **EMRFD** book and the **ARRL Handbook** project section have plans for usable signal generators, return-loss bridges, and noise sources you can build for parts cost.

## What to measure, and when

A rough decision tree for "which instrument do I reach for?"

| Question | Instrument |
|----------|------------|
| Is the antenna resonant where I want it? | VNA |
| Is the cable open / shorted / damaged? | VNA in TDR mode |
| Am I making harmonics that violate Part 97? | Spectrum analyzer |
| Is my filter aligned? | Spectrum analyzer + tracking generator |
| Is the rig putting out the power it claims? | Wattmeter + dummy load |
| Is my SSB peak power correct? | Peak-reading wattmeter |
| Is the audio chain distorted? | Oscilloscope |
| What's my station noise floor on 40 m? | The rig itself + a dummy load (§25-08) |
| Where is this RFI coming from? | Field strength meter + directional antenna |
| Is my power supply ripple bad? | Oscilloscope |
| Is this sealed antenna trap still on frequency? | Dip meter |
| Is my hand-wound coil really the inductance I designed for? | Dip meter (with a known capacitor) |

This chapter walks each instrument in detail.

## Table of contents

| Section | Title | Level |
|---------|-------|-------|
| §25-00 | Overview (this section) | mixed |
| §25-01 | NanoVNA — Advanced Techniques | advanced |
| §25-02 | Oscilloscopes for RF Work | mixed |
| §25-03 | Spectrum Analyzers | mixed |
| §25-04 | Tracking Generators | mixed |
| §25-05 | Field Strength Meters | mixed |
| §25-06 | Power & SWR Meters | mixed |
| §25-07 | TDR — Time-Domain Reflectometry | advanced |
| §25-08 | Measuring Station Noise Floor | mixed |
| §25-09 | Calibration Workflows | mixed |
| §25-10 | Antenna Analyzers | mixed |
| §25-11 | Dip Meters | mixed |

## See also

- §09-15 — NanoVNA Trim Workflow (the basic NanoVNA workflow that §25-01 builds on)
- §10 — Feedline & SWR
- §13 — Station Troubleshooting (where measurements support fault-finding)
- §14 — RFI
- §15 — Noise Sources
- §17 — Formulas (Smith chart, SWR, dB — the math behind these readings)
