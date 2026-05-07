---
id: 06-08
title: Doppler Correction Tables
chapter: 06
section: 08
level: advanced
status: draft
---

# Doppler Correction Tables

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

This section is the **lookup reference** for Doppler shift calculations on common amateur satellites. The tables below give the maximum Doppler shift (at AOS or LOS) and the typical correction needed at intermediate elevations during a pass. The conceptual treatment is in §06-02.

For an automated tracking setup these values aren't needed — the software handles correction continuously. For manual operation (hand-held antenna, manual frequency tuning) the tables let you pre-set tuning steps you'll dial through during a pass.

## Maximum Doppler shift by frequency

The fundamental relation: **Δf_max ≈ ±2.3 × 10⁻⁵ × f** for a typical LEO satellite at ~600 km altitude (which gives near-maximum radial velocity at horizon-crossing).

| Frequency | Δf_max (LEO horizon) |
|-----------|----------------------|
| 28 MHz | ±650 Hz |
| 50 MHz | ±1.15 kHz |
| 144 MHz | ±3.3 kHz |
| 220 MHz | ±5.1 kHz |
| 435 MHz | ±10 kHz |
| 902 MHz | ±21 kHz |
| 1296 MHz | ±30 kHz |
| 2400 MHz | ±55 kHz |
| 5760 MHz | ±130 kHz |
| 10368 MHz | ±240 kHz |

For other altitudes:
- ISS (400 km): roughly the same as 600 km LEO; shift varies ±5%.
- 800 km LEO (FO-29, RS-44): about 95% of the values above (slightly slower satellite).
- 1200 km MEO: about 80% of the values above.
- Geostationary (QO-100): essentially zero (~10 Hz from non-perfect orbit).

## Per-satellite reference: AO-91 (FM repeater)

**Uplink: 435.250 MHz (FM, 67.0 Hz CTCSS)**
**Downlink: 145.960 MHz (FM)**

For a representative high-elevation pass (~70° peak):

| Time from AOS | Elevation | Doppler uplink (Hz) | Doppler downlink (Hz) | Tune RX to (MHz) | Tune TX to (MHz) |
|---------------|-----------|----------------------|------------------------|--------------------|---------------------|
| 0:00 (AOS) | 0° | +10000 | +3300 | 145.9633 | 435.260 |
| 1:00 | 12° | +9100 | +3000 | 145.9630 | 435.259 |
| 2:00 | 25° | +7100 | +2350 | 145.9624 | 435.257 |
| 3:00 | 50° | +3700 | +1200 | 145.9612 | 435.254 |
| 4:30 | 70° (peak) | 0 | 0 | 145.960 | 435.250 |
| 6:00 | 50° | -3700 | -1200 | 145.9588 | 435.246 |
| 7:30 | 25° | -7100 | -2350 | 145.9576 | 435.243 |
| 9:00 | 12° | -9100 | -3000 | 145.957 | 435.241 |
| 10:00 (LOS) | 0° | -10000 | -3300 | 145.9567 | 435.240 |

(Sign convention here: positive Doppler means the satellite is approaching, so received frequency is higher than rest. Tune RX up to follow the apparent signal; tune TX up to compensate so the satellite's receiver sees the rest frequency.)

## Per-satellite reference: AO-92 (FM repeater)

**Uplink: 435.350 MHz (FM, 67.0 Hz CTCSS)**
**Downlink: 145.880 MHz (FM)**

Doppler magnitudes are essentially identical to AO-91 (same altitude class, same frequency band). Use the AO-91 table; just substitute the rest frequencies.

## Per-satellite reference: ISS cross-band repeater

**Uplink: 145.99 MHz (FM, 67.0 Hz CTCSS)**
**Downlink: 437.80 MHz (FM)**

ISS is at lower altitude (400 km) than AO-91 (600 km), so passes are shorter and the satellite spends less time near zenith. Doppler magnitudes are similar to AO-91.

| Time from AOS | Elevation | Doppler uplink (Hz) | Doppler downlink (Hz) | Tune RX to (MHz) | Tune TX to (MHz) |
|---------------|-----------|----------------------|------------------------|--------------------|---------------------|
| 0:00 (AOS) | 0° | +3300 | +10000 | 437.81 | 145.993 |
| 1:00 | 18° | +2700 | +8200 | 437.808 | 145.993 |
| 2:00 | 45° | +1500 | +4500 | 437.805 | 145.991 |
| 3:30 | 90° (overhead) | 0 | 0 | 437.80 | 145.99 |
| 5:00 | 45° | -1500 | -4500 | 437.795 | 145.989 |
| 6:00 | 18° | -2700 | -8200 | 437.792 | 145.987 |
| 7:00 (LOS) | 0° | -3300 | -10000 | 437.79 | 145.987 |

(For ISS uplink at 145.99, downlink at 437.80, the larger Doppler is on the 437 MHz downlink.)

## Per-satellite reference: FO-29 (linear transponder)

**Uplink: 145.900–146.000 MHz (LSB or CW)**
**Downlink: 435.800–435.900 MHz (USB or CW)** — *inverting transponder*

For a 60-minute pass (FO-29 is a higher orbit; longer passes):

| Pass position | Doppler (uplink + downlink combined) | Tune uplink up by (kHz) |
|---------------|---------------------------------------|--------------------------|
| AOS | +14 (max approach) | +14 |
| 25% | +10 | +10 |
| 50% | 0 | 0 |
| 75% | -10 | -10 |
| LOS | -14 (max recede) | -14 |

For an inverting transponder, the convention is to **tune the uplink** to compensate. The downlink will then appear at its rest frequency. (See §06-02 for the math.)

Specific operating: the operator picks a downlink slot (e.g., 435.870), works backward to compute the corresponding uplink at rest (uplink = 2 × inversion_center − downlink), then offsets the uplink by the Doppler value above.

## Per-satellite reference: RS-44 (linear transponder)

**Uplink: 145.935–145.995 MHz (LSB or CW)**
**Downlink: 435.610–435.670 MHz (USB or CW)** — *inverting transponder*

Similar pass-Doppler profile to FO-29. Use the FO-29 table; the magnitudes are within a few percent.

## Per-satellite reference: QO-100 (geostationary)

**Uplink: 2400.050–2410.000 MHz (S-band, USB)**
**Downlink: 10489.550–10490.000 MHz (X-band, USB)**

Doppler shift: **essentially zero** (~10 Hz drift over a day from non-perfect geostationary orbit; not observable in normal operation).

QO-100 is a "set frequency, work normally" satellite. No pass tracking, no Doppler correction, no antenna pointing. Just RF continuously to the same fixed dish aim.

## Approximate Doppler shift for arbitrary elevation

For a LEO satellite at altitude h ≈ 600 km, the Doppler shift varies with elevation as:

**Δf(elevation) = Δf_max × cos(elevation - direction-correction)**

where the direction correction accounts for whether the satellite is approaching or receding at that moment in the pass.

A rough approximation that works for "where am I right now":

| Elevation | Approach | Recede |
|-----------|----------|--------|
| 0° (horizon) | +Δf_max | -Δf_max |
| 15° | +0.93 Δf_max | -0.93 Δf_max |
| 30° | +0.83 Δf_max | -0.83 Δf_max |
| 45° | +0.69 Δf_max | -0.69 Δf_max |
| 60° | +0.50 Δf_max | -0.50 Δf_max |
| 75° | +0.27 Δf_max | -0.27 Δf_max |
| 90° (zenith) | 0 | 0 |

This is approximate; actual values depend on the satellite's exact altitude and the pass geometry. Use it for hand-tuning estimates; use tracking software for precision.

## Practical use of these tables

For manual operation:

1. **Predict the pass** (§06-07).
2. **Look up the satellite's max Doppler** for the band you're using.
3. **Pre-set your radio** to AOS-corrected frequency.
4. **At AOS, transmit the AOS-corrected frequency.**
5. **At intermediate elevations, dial through the values** from the table during the pass. Major correction every 60-90 seconds.
6. **At LOS, transmit the LOS-corrected frequency.**

For automated operation with tracking software, these tables aren't needed — the software does it continuously and accurately.

## Quick-reference: order of magnitude

For "how much do I need to tune?":

- **HF (10 m, 6 m)**: usually leave alone. Doppler is under SSB bandwidth.
- **2 m**: ±3 kHz total range. Manual tuning every minute or so during a pass works.
- **70 cm**: ±10 kHz. Manual tuning every 30 seconds gets clunky; automated is recommended.
- **23 cm**: ±30 kHz. Automated essentially required.
- **10 GHz**: ±240 kHz. Automated absolutely required; pre-correct based on prediction.

## See also

- §06-00 — Chapter overview
- §06-02 — Doppler shift (the conceptual treatment)
- §06-04 — Tracking strategies
- §06-07 — Pass prediction
