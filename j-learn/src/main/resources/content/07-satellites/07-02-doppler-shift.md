---
id: 07-02
title: Doppler Shift
chapter: 07
section: 02
level: mixed
status: draft
---

# Doppler Shift

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

When a satellite is approaching you, its transmitted frequency is shifted **higher** than its rest frequency. When it's receding, lower. The amount of shift depends on the satellite's speed along the line of sight (its **range rate**) and the operating frequency. For Low Earth Orbit (LEO) satellites moving at ~7.5 km/sec, the Doppler shift is significant — several kilohertz at VHF/UHF — and the shift changes throughout the pass as the geometry changes.

This is the **biggest practical difference** between satellite operating and terrestrial operating: you can't just set your radio to one frequency and leave it there. Either you continuously tune your radio (manually or automatically), or your QSO becomes unreadable.

## The math, briefly

Doppler shift in radio is the same physics as in audio (pitch change as a car passes you):

**Δf = f × v_radial / c**

Where:
- **f** is the rest-frame transmitted frequency.
- **v_radial** is the relative velocity along the line of sight (positive = approaching).
- **c** is the speed of light, 3 × 10⁸ m/sec.

For a LEO satellite at ~7500 m/sec orbital velocity, the maximum radial velocity (when the satellite is at the horizon, moving directly toward or away from you) is about 7000 m/sec — the geometry slightly reduces the projection of orbital velocity onto your line of sight.

| Frequency | Max Doppler shift (LEO, horizon-to-horizon) |
|-----------|----------------------------------------------|
| 28 MHz (10 m) | ±650 Hz |
| 50 MHz (6 m) | ±1.2 kHz |
| 144 MHz (2 m) | ±3.4 kHz |
| 435 MHz (70 cm) | ±10 kHz |
| 1.2 GHz (23 cm) | ±28 kHz |
| 2.4 GHz (13 cm) | ±55 kHz |
| 10 GHz (3 cm) | ±230 kHz |

The shift is **proportional to frequency** — twice the frequency means twice the Doppler shift. This is why 70 cm satellite operations need more aggressive tuning than 2 m operations on the same satellite.

## What you actually see during a pass

A typical LEO satellite pass:

- **AOS (just appearing on horizon, approaching)**: maximum positive shift.
- **As the satellite climbs**: the radial velocity decreases (the satellite is moving more "across" your view than toward you), so the shift decreases.
- **At closest approach (zenith for an overhead pass; closest point for a low-elevation pass)**: zero radial velocity, zero Doppler shift, frequency at rest.
- **As the satellite recedes**: radial velocity increases negatively, shift goes negative.
- **LOS (descending past horizon)**: maximum negative shift.

The transition through zero is **fast** — only the moments at and near closest approach. Most of the pass is spent with significant nonzero shift.

## Doppler shift on a typical FM bird (435 MHz uplink, 145 MHz downlink)

For AO-91 (uplink 435.250, downlink 145.960):

| Time in pass | Doppler 70 cm uplink | Doppler 2 m downlink |
|--------------|----------------------|----------------------|
| AOS (start, approaching) | +10 kHz | +3.4 kHz |
| 1 min in | +9 kHz | +3 kHz |
| 3 min | +5 kHz | +1.7 kHz |
| Closest approach | 0 | 0 |
| 7 min | -5 kHz | -1.7 kHz |
| 9 min | -9 kHz | -3 kHz |
| LOS | -10 kHz | -3.4 kHz |

**Both ends have Doppler.** The uplink shift is what the satellite sees on its receiver; if your transmit is too far off, the satellite's receiver squelch may not open. For an FM satellite, this means your effective uplink frequency on the satellite's receiver is your TX frequency MINUS the Doppler shift (signal arrives lower in frequency when you're approaching, by convention here positive shift = signal observed by approaching observer is higher).

## Compensating Doppler: three approaches

### 1. Manual: tune the downlink only

Simplest. As the pass progresses, you tune your downlink frequency to follow the moving satellite signal. On FM, you can usually leave the uplink alone — FM's bandwidth is forgiving (5 kHz channels typical), and the satellite's receiver bandwidth easily covers the Doppler-shifted uplink.

**On 70 cm uplink to AO-91**: you may want to manually adjust the uplink at AOS and LOS to keep the satellite's receiver locked — but for an overhead pass with strong signal, leaving uplink at rest frequency and just tuning downlink is fine.

### 2. Manual: tune both, with offset table

For linear transponder satellites with SSB or CW, you must adjust both ends. The convention is:

- **Tune the uplink** to compensate. The downlink will then appear at the satellite's rest-frame downlink frequency (no apparent change to you).
- This works because the inverting transponder's frequency relationship is fixed; if you adjust your uplink, the resulting downlink shift exactly cancels.

The math: if uplink rest is f_u and downlink rest is f_d (with inversion center f_c, so f_d = 2f_c − f_u), and the satellite is at radial velocity v:

- Your uplink TX appears to satellite at f_u × (1 - v/c).
- The satellite re-transmits at f_d shifted by the inversion: actual downlink TX is 2f_c − f_u_apparent = 2f_c − f_u(1 - v/c) = (2f_c − f_u) + f_u(v/c) = f_d + f_u × v/c.
- Doppler on the downlink (path back to you) is another v/c shift: f_apparent_at_you ≈ f_d + (f_u − f_d) × v/c.

For an inverting transponder (V/U mode: f_u ≈ 145, f_d ≈ 435), the (f_u − f_d) × v/c term gives a net shift on the downlink. To cancel it, tune the uplink by approximately Δf_u = f_d × v/c, which centers the downlink at its rest frequency.

This is what tracking software does automatically. Manual is doable but requires concentration during a pass.

### 3. Automated: tracking software + radio CAT control

The cleanest approach: tracking software (Gpredict, SatPC32, J-Sat) computes the live Doppler shift, sends it via CAT control to the radio, and the radio adjusts both uplink and downlink frequencies in real time.

Required:
- A radio with CAT control supporting frequency setting at high update rate (most modern radios).
- A computer connected to the radio.
- Tracking software with current Keplerian elements for the satellite.

Once set up, the operator just works the QSO; the software handles all Doppler.

> ⚙️ **Advanced —** The CAT update rate matters. A 1-second update gives smooth tuning but can leave the receiver up to 2-3 Hz off the actual signal momentarily during fast-changing geometry (closest approach for a high pass). Update rates of 100ms or faster are smoother. Some radios accept frequency commands faster than they can process them; the IC-9700 maxes at about 4 Hz CAT update rate before it starts dropping commands.

## "Doppler-correcting" practice

Different conventions for how to specify the corrected frequency:

- **Downlink-corrected (most common)**: the operator tunes their RX so the satellite's rest-frame downlink frequency is what they see on their dial. Doppler is fully compensated; the dial shows what would be transmitted if the satellite were stationary.
- **Uncorrected**: the operator's dial shows the actual received Doppler-shifted frequency. Less common; only used when communicating with someone using a different correction scheme.

For convention reasons:
- AMSAT-published frequencies are typically the **rest-frame** (uncorrected) frequencies.
- Your downlink dial should match these when properly Doppler-corrected.

## Doppler on different satellite types

**LEO satellites** (orbits < 1000 km): biggest Doppler. Full ±10 kHz on 70 cm. Pass durations 5-15 min. AO-91, FO-29, RS-44, ISS, etc.

**MEO satellites** (orbits ~10,000 km): smaller Doppler (the satellite's velocity is lower; ~3.6 km/sec). Pass durations 1-2 hours. Few amateur MEO sats (mostly research and military use this orbit).

**GEO satellites** (orbits 36,000 km): essentially zero Doppler (satellite is stationary relative to Earth). Es'hail-2 / QO-100 has no Doppler shift. Operating is much like a continuous local repeater — no tracking, no Doppler, no pass timing.

## A typical pass scenario

For an overhead pass of AO-91 (FM repeater, U/V mode, 435.250 uplink, 145.960 downlink):

| Time | Elevation | Slant range | Radial velocity | Doppler 70 cm uplink | Doppler 2 m downlink |
|------|-----------|-------------|------------------|----------------------|----------------------|
| 0:00 (AOS) | 0° | 2400 km | -7000 m/sec (away from you, since approaching from horizon side) | +10 kHz | +3.4 kHz |
| 1:00 | 10° | 2000 km | -6500 m/sec | +9.4 kHz | +3.1 kHz |
| 3:00 | 30° | 1500 km | -3500 m/sec | +5.1 kHz | +1.7 kHz |
| 5:00 (closest) | 60° | 950 km | 0 | 0 | 0 |
| 7:00 | 30° (other side) | 1500 km | +3500 m/sec | -5.1 kHz | -1.7 kHz |
| 9:00 | 10° | 2000 km | +6500 m/sec | -9.4 kHz | -3.1 kHz |
| 10:00 (LOS) | 0° | 2400 km | +7000 m/sec | -10 kHz | -3.4 kHz |

(Sign convention: approaching satellite has negative range rate; observer perceives signal at HIGHER frequency than rest. Some references invert the velocity sign; physics is identical. Verify your tracking software's convention.)

## Common Doppler mistakes

- **Setting and forgetting** uplink and downlink to rest frequencies for a 10-minute pass. The signal disappears as the geometry shifts; you'll be tuning frantically mid-QSO.
- **Tuning the wrong direction.** Approaching satellite → its signal appears HIGHER; you tune your RX UP, not down. Recede → signal LOWER; tune RX DOWN. Easy to flip in a hurry.
- **Forgetting Doppler on the uplink.** On FM, often gets away with it. On SSB linear transponder, the uplink shift is the dominant correction needed.
- **Not updating Keplerian elements.** Tracking software's frequency prediction depends on satellite position; old elements give wrong predictions. Update weekly.

## Quick reference: practical Doppler at common amateur satellite frequencies

| Frequency | Approx max Doppler (LEO) | Practical correction needed |
|-----------|---------------------------|------------------------------|
| 28 MHz | 650 Hz | Often leave alone (under SSB bandwidth) |
| 50 MHz | 1.2 kHz | Manual minor adjustment |
| 144 MHz | 3.4 kHz | Manual or automatic |
| 435 MHz | 10 kHz | Automatic strongly preferred |
| 1.2 GHz | 28 kHz | Automatic mandatory |
| 10 GHz | 230 kHz | Automatic mandatory; pre-correct based on prediction |

## See also

- §07-00 — Chapter overview
- §07-01 — FM vs linear (different Doppler treatment per type)
- §07-04 — Tracking strategies (where the Doppler correction lives)
- §07-07 — Pass prediction
- §07-08 — Doppler correction tables (lookup values per satellite)
