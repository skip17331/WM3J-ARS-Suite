---
id: 30-05
title: Aircraft Scatter
chapter: 30
section: 05
level: mixed
status: published
---

# Aircraft Scatter

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What aircraft scatter is

A commercial airliner cruising at 35,000 ft has a fuselage roughly the size of a 60-meter cylinder of polished aluminum. To a VHF or UHF signal, it's an excellent reflector. If two stations on the ground both have line-of-sight to the same aircraft simultaneously, that aircraft can bounce signals between them — a phenomenon called **aircraft scatter** (AS).

Unlike tropo ducting (weather-driven) or meteor scatter (random natural events), aircraft scatter is **predictable**. The aircraft are tracked in real time on ADS-B, their flight plans are public, and the geometry is straightforward — you can plan a contact for *exactly* when a known plane will cross the path midpoint.

This makes aircraft scatter the "scheduled-precision" VHF specialty: very short signal duration per aircraft, but very predictable timing.

## The propagation mechanism

The signal path looks like this:

```
You (FN20)  →  airliner @ 35,000 ft  →  Other station (EM84)
              ↑ midpoint of path ↑
```

The airliner has to be **above the radio horizon** as seen from both stations, which means within a few hundred miles of the path midpoint at typical cruise altitude. The reflection geometry is forward-scatter — the signal grazes the fuselage at a low angle and bounces forward toward the other station. Reflection coefficient depends on the angle (better at near-perpendicular incidence) and the aircraft's orientation relative to the path.

Practical numbers:

| Band | Aircraft scatter distance | Reflection strength |
|------|-----------------------------|----------------------|
| 2 m | 200–500 mi (320–800 km) | Moderate |
| 70 cm | 200–500 mi | Good |
| 23 cm | 200–600 mi | Best (smaller wavelength, fuselage looks larger) |
| 6 m | 300–700 mi | Marginal (longer wavelength, fuselage smaller relative) |
| 13 cm and up | 200–600 mi | Excellent in microwave AS |

The aircraft has to be in the air during your contact attempt. Each transit through the optimum-reflection volume lasts 30–120 seconds — long enough for a digital-mode QSO, often long enough for an SSB exchange.

## Using ADS-B for scheduling

The game-changer for aircraft scatter is **ADS-B (Automatic Dependent Surveillance-Broadcast)**: every commercial airliner continuously transmits its position, altitude, and call. Anyone with an inexpensive ADS-B receiver (an RTL-SDR dongle and a small antenna will do) can see every aircraft within ~200 mi in real time.

Tools that combine ADS-B with great-circle path geometry let you predict aircraft scatter contacts:

- **AirScout** (DL2ALF) — Windows application that takes two grid squares and tracks all aircraft transiting the midpoint volume, showing predicted signal strength and time window for each.
- **HRPT / Virtual Radar Server / dump1090** — raw ADS-B feeds you can integrate into custom scripts.
- **FlightAware / FlightRadar24** — web-based, less amateur-radio-specific, but handy for casual planning.

A typical AirScout workflow:

```
1. Enter your grid (FN20) and the other station's grid (EM84).
2. AirScout shows all aircraft currently in the midpoint volume.
3. Pick a flight: "DAL2407, 36000 ft, midpoint cross at 14:32 UTC, predicted SNR +3 dB"
4. Both stations schedule 144.174 MHz FT8 starting 14:31 UTC.
5. Aircraft crosses midpoint, signal peaks for 90 seconds, contact completes.
```

This level of precision is unique among the weak-signal modes.

## Bands and modes

| Band | Typical mode | Why |
|------|--------------|-----|
| 2 m | SSB, CW, FT8, MSK144 | Most common AS band; sustained ping useful for digital |
| 70 cm | SSB, CW, FT8 | Better reflectivity; tighter beam helps aim |
| 23 cm | CW, FT8, JT65 | Best reflectivity; very narrow beam essential |
| 6 m | SSB | Used during meteor showers when meteor-scatter is competing |
| Microwave 13/9/5/3 cm | CW, SSB, JT65 | The premier AS bands for active microwave operators |

FT8 with auto-sequencing works particularly well for AS because the 15-second sequences fit comfortably inside the 60–90 second aircraft transit window. MSK144 can also work but the brief duration doesn't match its meteor-ping design.

## Contests and events that emphasize AS

Several VHF / UHF / microwave contests have become aircraft-scatter playgrounds:

| Contest | Where AS plays a role |
|---------|------------------------|
| **ARRL January VHF** | Inland US 2 m / 70 cm AS during quiet propagation |
| **ARRL June / September VHF** | Mixed AS + Es + tropo; AS for marginal grids |
| **CQ WW VHF** | Microwave AS-heavy in EU contests |
| **ARRL 10 GHz & Up** | AS dominates link budgets on microwave |
| **EU IARU Region 1 contests** | AS is a recognized propagation mode in scoring |

The 10 GHz & Up contest in particular is built around AS — at 10 GHz, the aircraft fuselage is enormous relative to the wavelength, reflection is excellent, and the beam narrowness makes precision pointing pay off. Active operators schedule specific flights and run microwave SSB contacts during the 60-90 second predicted-peak windows.

## Equipment

A serious aircraft-scatter station is similar to a serious tropo station with one addition (the ADS-B receiver):

| Component | Spec |
|-----------|------|
| Antenna | Long Yagi or dish, az rotator, horizontal polarization |
| Rig | Any modern VHF/UHF transceiver |
| Power | 100–500 W (legal limit benefits when reflection is marginal) |
| Preamp | Mast-mounted LNA helpful at 23 cm and above |
| Software | WSJT-X for FT8, AirScout for prediction, mapping software |
| ADS-B receiver | RTL-SDR + 1090 MHz antenna; feeds AirScout |
| Internet | For ADS-B aggregator data and KST chat |

A station that's already equipped for tropo and meteor scatter on 2 m needs only AirScout and an ADS-B feed to start doing aircraft scatter.

## A typical AS contact

```
14:30 UTC — You (FN20) and W4XYZ (EM84) confirm on KST chat:
             "144.174 FT8, aircraft DAL2407 midpoint at 14:32"
14:31      — Both stations start auto-CQ; antennas pointed at midpoint bearing
14:32:00   — Aircraft enters predicted reflection volume
14:32:15   — First FT8 decode: "W4XYZ WM3J FN20"  +1 dB
14:32:30   — Response: "WM3J W4XYZ EM84"  −2 dB
14:32:45   — Report exchange: "W4XYZ WM3J +01"
14:33:00   — Confirmation: "WM3J W4XYZ R-02"
14:33:15   — RRR + 73 — contact complete
14:33:30   — Aircraft past midpoint; signal drops back to noise
```

A complete FT8 contact in 90 seconds, with both stations knowing exactly when to be ready.

## Calling discipline

- **Don't transmit blind.** Without an aircraft in the reflection volume, you're calling for nothing. Use AirScout or equivalent.
- **Coordinate by chat.** KST.com VHF rooms or email confirm the flight and frequency before either station transmits.
- **Both stations point at the midpoint**, not at each other. The signal is going up to the aircraft and back down, not direct.
- **Use SSB/CW/FT8 — not FM.** Same threshold-effect argument as tropo. FM works only when reflection is unusually strong.
- **Run short messages.** Each transit window is limited; pre-planned exchanges complete faster.

## When AS shines

- **Filling marginal mid-distance grids on 2 m / 70 cm / microwave** that don't have meteor scatter or tropo activity.
- **Microwave operating in general** — at 10 GHz, AS may be the *only* path between two distant stations.
- **Contest grid hunting** with known scheduled flight times.
- **Mid-latitude paths in winter** when other VHF propagation is dead.

## When AS doesn't help

- **Transoceanic distances.** Aircraft scatter is 200–500 mile range; oceanic paths need EME or satellite.
- **No aircraft in the sky.** AS shuts down between 0200 and 0500 local in most regions when commercial traffic is light.
- **Surface clutter near both stations** — you need clear line-of-sight upward to the aircraft volume.
- **Quick casual operation.** AS rewards planning; it doesn't reward random calling.

> **Advanced —** The radar-equation analog for aircraft scatter uses bistatic geometry with the aircraft as a non-cooperative target. The reflection coefficient depends on the aircraft's radar cross-section (RCS) at the operating frequency, the bistatic angle, and the aspect angle (which way the aircraft is pointing relative to the path). A B737 fuselage has an RCS of roughly 100 m² at 2 GHz and 1 m² at 100 MHz; the much larger relative reflector at microwave is why microwave AS is so much stronger than 2 m AS. The 60–90 second peak window is the time during which the aircraft's bistatic angle is within ~10° of forward-scatter maximum. Multi-aircraft AS — using two or more aircraft in the volume simultaneously — produces fading patterns from interference between the multiple reflections, observable on a waterfall and useful for diagnosing whether you're seeing AS or some other mechanism.

## See also

- [§01-09 — Weak-Signal VHF/UHF](../01-propagation/01-09-weak-signal.md) — VHF propagation modes
- [§30-02 — Meteor Scatter Operating](30-02-meteor-scatter.md) — the other "scheduled brief reflection" mode
- [§30-04 — Tropospheric Ducting](30-04-tropo-ducting.md) — weather-driven VHF
- [§30-03 — EME Basics](30-03-eme-basics.md) — longer-haul VHF specialty
- [§03 — Digital Modes](../03-digital-modes/) — FT8 / MSK144 for AS
- [§06 — Antennas](../06-antennas/) — long Yagis and dishes for VHF/UHF/microwave
