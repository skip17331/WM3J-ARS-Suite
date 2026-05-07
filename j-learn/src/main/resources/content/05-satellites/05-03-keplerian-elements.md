---
id: 05-03
title: Keplerian Elements
chapter: 05
section: 03
level: advanced
status: draft
---

# Keplerian Elements

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A satellite's orbit can be fully described by **six numbers**: the Keplerian elements (sometimes called orbital elements). Plug those six numbers into a propagator, add a time, and you get the satellite's position and velocity in space. Tracking software does this thousands of times per minute to draw the satellite's path on a map and predict when it will be over your station.

This section covers what the six elements are, how to read them, and how to keep them current. You don't need to do the math — software does that — but understanding the elements helps you spot when something's wrong (a satellite "disappearing" from predictions, a pass time that's clearly off, a drift in the predicted track).

## The six elements

Each element captures one geometric property of the orbit:

| Element | Symbol | Describes | Typical units |
|---------|--------|-----------|---------------|
| Semi-major axis | a | Size of the orbit (average distance from center of Earth) | km |
| Eccentricity | e | Shape (0 = circle; 0–1 = ellipse; 1 = parabola) | unitless, 0–1 |
| Inclination | i | Tilt of the orbit plane relative to Earth's equator | degrees, 0–180° |
| Right ascension of ascending node | Ω (RAAN) | Where the orbit crosses the equator going north | degrees, 0–360° |
| Argument of perigee | ω | Where in the orbit the satellite is closest to Earth | degrees, 0–360° |
| Mean anomaly | M | Where the satellite is on the orbit at the epoch time | degrees, 0–360° |

Plus the **epoch**: the moment in time at which all the above values were measured.

That's seven things. With these, you can compute the satellite's position at any past or future time — within the limits of how accurate the elements are.

## Why orbits drift (and elements expire)

Real orbits are not Keplerian. Earth isn't a point mass:

- **Earth's oblateness** (the equatorial bulge) tugs slightly on the satellite, causing the ascending node and argument of perigee to **regress** over time.
- **Atmospheric drag** (for LEO satellites) gradually decreases the orbit's energy, lowering the semi-major axis and shortening the orbital period.
- **Solar radiation pressure** (for higher orbits) imparts a tiny but noticeable acceleration.
- **Lunar and solar gravitation** perturb high orbits.
- **Solar activity** changes upper-atmospheric density, accelerating drag during high SFI periods.

Tracking software uses an **SGP4 propagator** that includes corrections for the dominant of these (oblateness via the J2 term) but doesn't model drag rate evolution well. Over weeks, the propagated position drifts from reality. A LEO satellite tracked from week-old elements may be predicted ~5–10 km off its actual position; over a month, ~50–100 km. For pass-time prediction, this means timing errors of seconds to minutes; for antenna pointing, it means antenna aim could be a beam-width or two off.

**Elements need to be updated weekly** for accurate tracking. Older = more error.

## Where to get current elements

The standard format is **Two-Line Element Set (TLE)** — two lines of 69 characters each, encoding all the Keplerian elements plus drag information and the epoch. NORAD/NASA generate TLEs from radar tracking and post them publicly:

- **CelesTrak** (celestrak.com) — the primary public source; no registration needed, free.
- **Space-Track** (space-track.org) — registered access; current and historical TLEs.
- **AMSAT** — amsat.org publishes amateur-satellite-specific TLE bundles.
- **Pass-prediction apps** typically auto-fetch from one of these sources.

A typical TLE looks like (line breaks shown for readability; the actual format is two lines exactly):

```
ISS (ZARYA)
1 25544U 98067A   26127.51234567  .00012345  00000-0  23456-3 0  9991
2 25544  51.6420  72.3456 0007890 123.4567 234.5678 15.49567890123456
```

The two-line block contains:

**Line 1:**
- Satellite name (line 0, optional title line)
- Catalog number (25544 for ISS)
- International designator (1998-067A = the ISS module)
- Epoch (2026, day 127.5123 = May 7 around noon UTC)
- First time derivative of mean motion (drag indicator)
- Various dragging-related fields
- Element set number, checksum

**Line 2:**
- Catalog number again (sanity check)
- Inclination (51.64°)
- RAAN (72.35°)
- Eccentricity (0.0007890 — very nearly circular)
- Argument of perigee (123.46°)
- Mean anomaly (234.57°)
- Mean motion (15.4957 revolutions per day) — implies period of ~93 minutes
- Revolution number at epoch

> ⚙️ **Advanced —** The TLE format dates from the 1960s and uses a specific quirk: the mean motion is given as revs/day (not radians/sec), the eccentricity is implicit-decimal (0.0007890 written as 0007890), and the drag terms are in scientific notation with a non-standard ASCII encoding (12345-3 means 1.2345e-3). The exact TLE columnal layout is defined by NORAD; parsers must respect every character position. SGP4 is the standard propagator that consumes TLEs; it includes J2 oblateness, atmospheric drag (with simplified models), and a few other perturbations. Higher-fidelity propagators (SGP4XP, HPOP) exist but require more orbital data than a TLE provides.

## How a TLE relates to the six elements

The TLE encodes the six Keplerian elements with three additional drag-related parameters and an epoch:

| TLE field | Keplerian element |
|-----------|-------------------|
| Mean motion | derived from semi-major axis (a = ⁻³√(GM × T² / 4π²)) |
| Eccentricity | e |
| Inclination | i |
| RAAN | Ω |
| Argument of perigee | ω |
| Mean anomaly | M |
| Epoch | reference time for all above |

So the TLE is just the six elements packaged with metadata. The "ndot/2" and "nddot/6" fields capture drag's effect on mean motion (rate of change and acceleration of mean motion).

## Sample TLE for common amateur satellites (illustrative — actual values change)

| Satellite | Catalog # | Inclination | Period |
|-----------|-----------|-------------|--------|
| ISS | 25544 | 51.6° | 93 min |
| AO-91 (Fox-1B) | 43017 | 97.4° | 96 min |
| AO-92 (Fox-1D) | 43137 | 97.6° | 96 min |
| FO-29 | 24278 | 98.6° | 105 min |
| RS-44 | 44909 | 82.5° | 120 min |
| Es'hail-2 / QO-100 | 43700 | 0.1° (geosync) | 1436 min (geostationary) |

For up-to-the-day actual TLEs, fetch from CelesTrak or AMSAT.

## Updating tracking software

Most tracking software has an "auto-update TLEs" feature that pulls fresh elements from CelesTrak or AMSAT on a schedule (daily or weekly). Recommended: enable auto-update.

If you operate offline (field portable, off-grid), download a TLE bundle before going. The bundle is usually a single text file with TLEs for all amateur satellites; load it once and your tracking software is good for the week.

## "My pass times look wrong"

If your tracking software is predicting pass times that are clearly off (the satellite appears 30 seconds early or late), the likely causes:

1. **Outdated TLE**. Refresh from CelesTrak. This fixes 80% of "wrong pass time" complaints.
2. **Wrong satellite TLE applied**. Some satellites share names or designations close together; verify you're using the right TLE.
3. **Wrong observer location**. Tracking software needs your station's latitude, longitude, and altitude. A 1° lat/lon error = 100 km observer position error = noticeable timing changes.
4. **Wrong system clock**. If your computer's clock is off by 30 seconds, predictions are off by 30 seconds. Use NTP.
5. **Atmospheric drag not modeled**. For LEO satellites in solar maximum, drag is high and SGP4's drag model is approximate. Even fresh TLEs can be 5–10 seconds off.

## Software that tracks satellites

| Software | Platform | Notes |
|----------|----------|-------|
| Gpredict | Linux, Mac, Windows | Free, open-source; widely used in amateur community |
| SatPC32 | Windows | Long-standing favorite; auto-tunes radios via CAT |
| Orbitron | Windows | Older free Windows tool; still functional |
| Heavens-Above | Web | Free; great for human-readable pass info |
| AMSAT pass predictor | Web | Free; AMSAT-published |
| ISS Detector | Mobile (iOS, Android) | Free; nice phone-screen tracking |
| J-Sat | Mac/Linux/Windows | Built into the suite (this project's tracker) |

All of these consume TLEs in the standard NORAD format. Set the satellite list, let the software fetch fresh elements, and the predictions should be accurate to a few seconds.

## Common Keplerian-element pitfalls

- **Stale elements.** Refresh weekly. A TLE 2 months old can be 10+ km off.
- **Wrong satellite catalog number.** Some satellites have multiple identifiers; AMSAT-published lists are the most curated for amateur use.
- **Manual TLE entry typos.** Easy to mistype a digit. Use auto-update or copy from a clean source.
- **Time zone confusion.** TLE epoch is in UTC. Your tracking software's display may be in local time. Verify which.
- **Trusting predictions too far out.** Even fresh TLEs degrade after ~7 days for LEO; predictions a month out are speculative.

## See also

- §05-00 — Chapter overview
- §05-02 — Doppler shift (depends on knowing position from elements)
- §05-04 — Tracking strategies (the orbital position output of element propagation)
- §05-07 — Pass prediction (the practical output of all this)
