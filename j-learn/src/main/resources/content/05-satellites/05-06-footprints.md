---
id: 05-06
title: Footprints
chapter: 05
section: 06
level: simple
status: draft
---

# Footprints

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A satellite's **footprint** is the region of the Earth's surface that has line-of-sight to the satellite at a given moment. Inside the footprint, you can potentially work the satellite (subject to your antenna gain, the satellite's coverage at that elevation, and noise). Outside, you can't — the radio horizon blocks the path entirely.

Understanding footprints tells you: who else can be on the bird right now, what the maximum DX distance is for a satellite QSO, and where to expect a contact's location to be on the world map.

## What sets the footprint size

The footprint is determined by the **satellite's altitude** and a small contribution from its antenna pattern. Geometrically:

- The satellite is at altitude **h** above Earth's surface.
- A point on Earth's surface has line-of-sight to the satellite if the angle from the satellite to that point, measured from the line between satellite and Earth's center, is less than the angle to the horizon.
- The maximum great-circle distance from the **subsatellite point** (the point directly under the satellite) to the edge of the footprint is:

**d = R × arccos(R / (R + h))**

where R is Earth's radius (6378 km) and h is the satellite altitude.

Plugging in numbers:

| Altitude (h) | Footprint radius (km) | Footprint diameter (km) | Approx area covered (% of Earth's surface) |
|--------------|------------------------|--------------------------|---------------------------------------------|
| 200 km (very low LEO) | 1500 | 3000 | 0.6% |
| 400 km (ISS, ~93 min orbit) | 2200 | 4400 | 1.2% |
| 600 km (typical FM bird) | 2700 | 5400 | 1.8% |
| 800 km (FO-29, RS-44) | 3100 | 6200 | 2.4% |
| 1200 km (some MEO research sats) | 3800 | 7600 | 3.6% |
| 20,200 km (GPS orbit) | 7600 | 15,200 | 14% |
| 35,786 km (geostationary, e.g., QO-100) | 9000 | 18,000 | 42% (but with elevation limits at extremes) |

For the ISS, that's a **footprint diameter of 4400 km** — about the distance from Boston to San Francisco. Anyone within that circle (centered on the ISS's subsatellite point) has potential line-of-sight.

## Why "potential"?

Three things modify the practical footprint:

1. **Minimum useful elevation.** At the very edge of the geometric footprint, the satellite is at 0° elevation — at the horizon, with maximum slant range and weakest signal. Trees, buildings, and atmospheric attenuation can block low-elevation contacts. Practical footprint usually assumes ≥5° elevation, which slightly shrinks the radius.
2. **Satellite's transmit power and antenna pattern.** Most LEO amateur satellites have low gain antennas (1–3 dB gain) — roughly hemispherical coverage. So at 0° elevation (8° elevation from satellite's view), antenna gain is approximately constant. The footprint is mostly geometry, not antenna patterning.
3. **Receiver/transmitter sensitivity at long slant range.** A QRP station with 5W and a small antenna won't be heard from the footprint edge by anyone — the path loss is too high. Bigger stations get more usable footprint.

## DX distance through a satellite

The maximum distance between two stations both within a satellite's footprint is approximately **2 × footprint radius** — across the whole footprint diameter. A station near one edge can theoretically work a station near the opposite edge if both have good line-of-sight to the satellite simultaneously.

| Satellite altitude | Approx max footprint diameter |
|---------------------|--------------------------------|
| ISS (400 km) | ~4400 km |
| AO-91 (600 km) | ~5400 km |
| FO-29 (800 km) | ~6200 km |
| QO-100 (geostationary) | ~18,000 km within main beam (Africa+Europe) |

So an FM bird pass over the eastern US could let you work someone on the western US (3000+ km), but probably not someone on the west coast (4500+ km) unless the geometry is exactly right and both stations have strong gear.

## Reading footprint maps

Most satellite tracking software displays the satellite's current footprint as a circle on a world map. The circle moves as the satellite moves. Inside the circle = line-of-sight to the satellite right now.

The circle is **not actually a circle** on a real-world map — it's a circle on the spherical Earth, which projects to an oval on a flat map (Mercator) or a near-circle on a globe view. Software handles the projection.

## Footprint and pass prediction

The pass prediction for your station tells you when the satellite enters and leaves your *station's* horizon. Equivalently, you're asking when the satellite's footprint includes your location.

For a typical LEO satellite making a single pass over you:

- **Footprint sweeps in from the west** (or east, depending on orbital direction).
- **Your station enters the footprint at AOS** (the moment the satellite crests your horizon).
- **The footprint moves over you** — center of footprint at the satellite's closest approach.
- **Your station leaves the footprint at LOS.**

For higher orbits (longer pass durations, larger footprints), you stay in the footprint for hours. For LEO, only 5–15 minutes.

## Where this matters operationally

- **Predicting QSO partners**: who else might be on the bird? Anyone in the footprint. For a US-east station, a typical LEO pass might have other stations in the footprint from southern Canada to South America.
- **Coordinating contacts**: "Let's meet on this bird at 18:35 UTC" — verify both your stations will be in the footprint at that time.
- **DX-pedition coordination**: rare-grid sat operators publish exact pass times so chasers know when their footprint will cover the desired grid.
- **Satellite-only activity**: groups like AMSAT-NA have "satellite contests" where the goal is to work as many other satellite stations as possible in a weekend; understanding footprints tells you who's reachable when.

## Geostationary special case (QO-100)

A geostationary satellite has an essentially **fixed footprint** — it doesn't move (relative to Earth). Es'hail-2 / QO-100, at 26°E equatorial, has a footprint covering:

- Africa
- Europe (except far north)
- Middle East
- Western parts of central Asia
- Eastern South America

North America has no line-of-sight to QO-100 (the satellite is below the western horizon for North American stations). North American amateurs cannot use QO-100 directly; some receive its downlink via internet-streamed receivers in covered regions.

The next-generation amateur GEO satellite (CAMSAT-X, AMSAT-DL planning, others) may include North American coverage by being positioned over the Atlantic.

## Multi-satellite footprint considerations

When working through linear transponder satellites (multiple QSOs simultaneously), the **shared footprint** of the satellite is the limiting factor — every operator on that satellite right now is somewhere in its footprint. Pile-up dynamics on linear sats are partly a function of how many active operators are within the footprint at any moment.

For FM birds (single QSO at a time), the footprint determines who *could* be calling — important because pile-ups during prime passes can have hundreds of stations queued.

## Common footprint misconceptions

- **"The satellite's signal is everywhere on Earth."** No — only inside the footprint. From outside, the radio horizon completely blocks line-of-sight.
- **"Higher altitude means better signal."** Means **larger footprint** but also **larger slant range** (path loss). The two trade off; LEO satellites have high path loss at footprint edges due to long slant range.
- **"GEO satellites cover the whole hemisphere."** They cover about a third of Earth's surface, not a hemisphere — limited by the geometry of viewing the equator at 35,786 km altitude. Edge-of-footprint elevation is just over 0°, which is poor in practice.
- **"My station is in the footprint, so I can work the satellite."** Footprint is necessary but not sufficient. You also need adequate antenna gain, low noise, and good orientation.

## See also

- §05-00 — Chapter overview
- §05-04 — Tracking strategies (where in the footprint to point)
- §05-07 — Pass prediction (when your station is inside the footprint)
- §09-05 — Portable budget (path loss to satellite calculation)
