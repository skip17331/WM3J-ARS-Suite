---
id: 06-04
title: Tracking Strategies
chapter: 06
section: 04
level: simple
status: draft
---

# Tracking Strategies

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A satellite is a fast-moving target in the sky. To work it, you have to do two things continuously during the pass: **point your antenna at it**, and **manage Doppler shift on the radio**. This section is about the antenna pointing — manual aiming, automated rotators, and the in-between practices.

For the radio side, see §06-02 (Doppler) and §06-04's twin, §06-07 (pass prediction). This section assumes you've predicted the pass and know azimuth/elevation; the question is how to keep the antenna there.

## The three strategies

| Strategy | Cost | Skill required | Best for |
|----------|------|----------------|----------|
| Hand-held aim | $0 (you already have hands) | Operator skill, dexterity | FM birds, casual operating |
| Look-and-shoot fixed antenna | $0 (omnidirectional or wide-beam fixed antenna) | Low | High-elevation passes only |
| Automated rotator + tracking software | $500–3000 | One-time setup | Linear transponder, serious operating |

Most operators use one approach across the board, but mixing is common — hand-held FM bird operations during commute time, automated rotator at home base.

## Hand-held aim (the Arrow approach)

A hand-held Yagi (the Arrow II is the canonical) lets you point the antenna manually as the satellite traverses the sky. You stand in your yard, watch the predicted azimuth/elevation on a phone app, and aim the antenna at the bird.

### How it works in practice

1. **Start before AOS** — pull up the pass prediction on your phone (AMSAT app, ISS Detector, etc.).
2. **Note the AOS azimuth and elevation curve** — typically AOS at one direction, peak elevation in the middle of the sky, LOS in the opposite direction.
3. **At AOS, aim the antenna toward the AOS azimuth at the horizon** — usually ~5° elevation.
4. **As the satellite climbs**, aim by following the prediction app's live map. The app shows current azimuth/elevation; you point the antenna there.
5. **Hold steady** — the antenna's beam (~30° wide on a 3-element Yagi at 144 MHz, narrower at 435 MHz) is forgiving. Small movements are fine.
6. **At LOS**, the satellite drops back below the horizon; the pass is over.

### Tips for hand-aiming

- **Listen to the downlink** as you aim. Strong signal = good aim. As you slowly move the antenna in azimuth or elevation, the signal peaks; that's where to hold.
- **Use a reference object** — point at a tree or a star at the predicted azimuth and use it as a reference. Easier than reading a compass mid-pass.
- **Hold the antenna steady, just sweep slowly** — avoid jerky motion; the beam is narrow at 70 cm and big movements lose signal.
- **For overhead passes, aim straight up** — you'll catch the satellite around closest approach even with rough aim.
- **Polarization matters** — most amateur satellites have linear (or circular) polarization. The Arrow has a polarization switch on each band; flipping it can dramatically change signal strength. Try both during a pass.

### Limitations

- **Can't hand-aim during storms, in cold weather, or while doing other things.**
- **Unsteady aim** at 70 cm gives unreadable signal during a fast-changing portion of the pass.
- **Hard to operate radio simultaneously** — typically one hand is on antenna, one on PTT, and your eyes flick between phone (predicted azimuth) and radio (Doppler-shifted frequency). It's a juggling act.

## Fixed antenna ("look and shoot")

If you have a fixed-direction antenna (omni for VHF, or a fixed beam pointing up), you can sometimes work satellites without aiming — particularly for **near-overhead passes**.

### Where it works

- **Omni 2 m antenna at home (e.g., a J-pole, a ground-plane vertical)** for receiving downlinks. Works for the closest part of a pass when the satellite is near zenith. Will have weaker signal at low elevations.
- **Fixed Yagi at moderate elevation** (45° or so) for receiving most of a pass at high elevation.
- **Fixed circular-polarized antennas** (a turnstile or quadrafilar helix) — designed for ~half-wavelength all-azimuth coverage; works passably for FM birds at moderate elevations.

### When it doesn't work

- **Low-elevation passes** — the satellite is far away (slant range high), signal is weak, fixed antenna gain in that direction is low.
- **Linear transponder satellites** — they need full duplex with strong RF on both bands; fixed gain isn't enough.
- **Very directional needs** — you can't aim at a satellite over the next county.

Look-and-shoot is fine for casual FM bird operations from a sedentary station. It's not the recommended strategy for serious satellite work.

## Automated rotator

The full-blown solution: a rotator that points your antenna automatically based on the satellite's position, computed by tracking software.

### Components

- **An az-el rotator** (azimuth + elevation): two axes. Yaesu G-5500 is the classic; M2 antennas RC2800 is a higher-end option; smaller rotators (Spid) are common at intermediate price points.
- **A directional antenna** appropriate for the band(s): typically dual-band cross-Yagis or two separate Yagis for V and U.
- **A computer** running tracking software (Gpredict, SatPC32, etc.).
- **Cabling and rotator interface** between computer and rotator.

### Setup

1. **Mount the rotator** on a mast, level base.
2. **Calibrate**: point the antenna at known references (north for azimuth-zero; horizon for elevation-zero). Update the rotator firmware's "home" position.
3. **Connect to tracking software** — most rotators use a serial / USB interface; software supports many protocols.
4. **Test by manually commanding** azimuth and elevation positions to verify control works.
5. **Run a satellite pass** in test mode to verify the rotator follows the predicted track without overshoot, oscillation, or limit-stops.

### How a tracked pass works

When the tracking software sees the satellite is approaching:

1. Software computes az/el for current satellite position.
2. Sends command to rotator: "go to az 145°, el 0°."
3. Rotator moves to that position.
4. Software updates az/el at ~0.5–1 second intervals as the satellite moves.
5. Rotator follows continuously.

The operator just watches the radio and works the QSO. The antenna stays on the satellite automatically.

### Calibration drift

Rotators drift slightly over months — bearings wear, gear backlash develops, mounting screws loosen. Re-calibrate annually:

- Point at a known landmark (a building corner you know is at azimuth 270° from your station).
- Verify the rotator agrees.
- Adjust the offset in tracking software if needed.

> ⚙️ **Advanced —** Az-el rotators have a problem at zenith: as the satellite passes directly overhead, the azimuth needs to flip 180° instantaneously. Most rotators can't do this fast enough; the antenna lags or overshoots. The classic fix is the **flip rotator** (a 0–540° azimuth range), which lets the antenna pass through the zenith point without azimuth flip. The Yaesu G-5500 supports this with its extended-range option. Without it, high-elevation passes are tricky — the rotator may "scan" frantically as the satellite crosses over.

## Polarization

Amateur satellites use various polarization schemes:

- **Linear horizontal** (most common downlinks): a linearly polarized antenna in the same orientation works best. Spinning the antenna may peak signal as orientation matches.
- **Linear vertical**: same, vertical polarization.
- **Right-hand circular polarization (RHCP)** or **Left-hand circular polarization (LHCP)**: mostly for higher-frequency satellites and the ISS. A circularly polarized antenna will give 3 dB less signal on the wrong handedness; a linearly polarized antenna receives both circulars equally (3 dB attenuation).

Practical:

- For terrestrial 2 m and 70 cm Yagis: vertical orientation typically works for FM birds; for crossed-Yagi setups, switching between V and H polarization mid-pass can recover lost signal as the satellite tumbles.
- **Polarization switching during a pass** — observed signal can fade when polarization mismatches develop. Switching may peak signal.

## Multi-mode tracking

Some satellites uplink in one band and downlink in another (V/U, U/V modes). With a single antenna for both, you'll use a duplexer or multi-band antenna. With separate antennas:

- A 2 m Yagi for one band, a 70 cm Yagi for the other.
- Both physically slaved to the same rotator (so one tracking signal moves both).
- Each antenna mast may have its own preamp and feedline.

Linear transponder operations on cross-band sats commonly use this configuration.

## When tracking goes wrong

| Symptom | Likely cause |
|---------|--------------|
| Antenna doesn't move | Cable disconnected, software not running, rotator power off |
| Antenna moves but ignores software | Software not actually controlling rotator (CAT/serial issue) |
| Antenna goes the wrong way | Inverted axis calibration |
| Antenna overshoots / oscillates | Rotator dynamics tuning needed |
| Antenna stops at a limit before reaching target | Software at limit-stop; needs flip rotator |
| Signal weak when antenna is "pointed at" satellite | TLE outdated, observer coords wrong, polarization mismatch |

## See also

- §06-00 — Chapter overview
- §06-02 — Doppler shift (the radio-side companion)
- §06-03 — Keplerian elements (where the prediction comes from)
- §06-07 — Pass prediction
- §05-15 — Radiation patterns (Yagi gain considerations)
