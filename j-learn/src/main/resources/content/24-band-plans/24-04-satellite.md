---
id: 24-04
title: Satellite Sub-bands
chapter: 24
section: 04
level: simple
status: draft
---

# Satellite Sub-bands

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Specific sub-bands of several amateur allocations are reserved by international convention for **satellite uplinks and downlinks**. These reservations come from the IARU's coordination work with AMSAT and other satellite organizations. They are voluntary band-plan conventions, not legal restrictions — but operating non-satellite traffic on these segments interferes directly with people working orbiting satellites and is poorly tolerated.

This section catalogs the satellite sub-bands and what's typical in each. For *operating* satellites — Doppler tuning, mode selection, satellite scheduling — see §06.

## The "OSCAR" segments

OSCAR = Orbiting Satellite Carrying Amateur Radio. The first amateur satellites in the 1960s were named OSCAR-1, OSCAR-2, etc. The name has stuck for the segments reserved for satellite work, even though most modern amateur satellites are AMSAT-funded with names like AO-91, AO-92, FO-99, etc.

## VHF satellite sub-bands

| Band | Sub-band (MHz) | Use |
|------|---------------|-----|
| 2 m | 144.300-144.400 | Satellite uplinks |
| 2 m | 145.800-146.000 | Satellite downlinks |

Satellites with **U/V** mode (UHF uplink, VHF downlink) downlink in 145.800-146.000. Satellites with **V/U** mode (VHF uplink, UHF downlink) uplink in 144.300-144.400.

Most modern linear-transponder satellites use the V/U mode (because UHF antennas are smaller and easier to put on the satellite). FM repeater satellites also use V/U.

## UHF satellite sub-bands

| Band | Sub-band (MHz) | Use |
|------|---------------|-----|
| 70 cm | 432.300-432.400 | Satellite uplinks (less common) |
| 70 cm | 435.000-438.000 | Satellite uplinks AND downlinks |

The 435.000-438.000 range is the heaviest satellite band:

- **U/V satellites** uplink in 435-438, downlink at 145.800-146.000.
- **U/U satellites** (rare) use both 435-438 segments.
- Most amateur satellites' downlinks land somewhere in 435.000-438.000.

## Specific frequencies on common amateur satellites

Most popular FM repeater satellites and their frequencies (these change as satellites are launched and decommissioned; values below are current as of 2026):

| Satellite | Mode | Uplink | Downlink | Notes |
|-----------|------|--------|----------|-------|
| AO-91 (Fox-1B) | FM repeater | 435.250 (CTCSS 67.0) | 145.960 | LEO, 14-min passes |
| AO-92 (Fox-1D) | FM repeater | 435.350 (CTCSS 67.0) | 145.880 | LEO |
| ISS (Repeater) | FM cross-band | 145.990 (CTCSS 67.0) | 437.800 | When packet repeater is active |
| ISS (Voice) | FM | 145.200 | 145.800 | When astronaut voice is active |
| HO-113 (Hope Sat) | FM repeater | 435.350 | 145.825 | Chinese amateur sat |
| FO-29 | Linear transponder | 145.900-146.000 | 435.800-435.900 | Inverting transponder |
| RS-44 | Linear transponder | 145.935-145.995 | 435.610-435.670 | Inverting; Russian |

Linear-transponder satellites pass a slice of spectrum, not just an FM voice signal — multiple QSOs can run simultaneously within the transponder's bandwidth.

## HF satellite sub-bands

A few historic amateur satellites have used HF:

- **AO-13** (decommissioned): used 145 MHz uplink, 28 MHz downlink (V/A mode). The 28 MHz downlink was 29.510-29.590.
- Some current/historical satellites have V/A modes still.

In modern practice, HF satellite operation is rare. The 29 MHz "satellite downlink" sub-band (29.510-29.590) is sometimes still mentioned in band plans but rarely active.

## 23 cm satellite sub-band

| Band | Sub-band | Use |
|------|----------|-----|
| 23 cm | 1260.000-1270.000 | Satellite uplinks |

The 1260-1270 MHz segment is reserved for **L-band uplinks** to satellites. Some satellites (FO-99, EsHail-2 / Es'hail) have L-band uplinks; the segment supports them.

## Es'hail-2 / QO-100 (geostationary satellite)

Launched 2018, **the only currently operational amateur geostationary satellite** (over the equator at 26°E longitude — visible from Africa, Europe, parts of Asia, eastern parts of South America). Not visible from continental US.

- **Uplink**: 2400.050-2410.000 MHz (S-band)
- **Downlink**: 10489.550-10490.000 MHz (X-band)

Specific dish, transverter, and feedline equipment are required. North American amateurs cannot directly hear or transmit to QO-100 (no line-of-sight); they can listen via internet-streamed receivers at observable locations.

## Satellite "no-go" zones within satellite sub-bands

Within the satellite sub-bands, additional convention: do not transmit on:

- **The exact center of a satellite's transponder** during a pass — this jams the entire transponder for the orbit.
- **The TLM (telemetry) frequencies** of operational satellites — typically marked with "BCN" or "TLM" in satellite info pages.
- **Other satellites' active passes** — be aware of which satellites are currently in your view.

The J-Sat module shows which satellites are over the horizon; check it before transmitting in a satellite sub-band.

## Working satellites: a quick procedure

For an FM repeater satellite (e.g., AO-91):

1. **Predict the pass** — use J-Sat or a similar tracker; note acquisition of signal (AOS) and loss of signal (LOS) times.
2. **Set the satellite's uplink and downlink frequencies** — many radios let you set both as a "satellite memory" with cross-band split.
3. **Set CTCSS subaudible tone** if required (most sats use 67.0 Hz).
4. **Doppler-correct manually** — as the satellite approaches, downlink is high; as it recedes, downlink is low. For FM, +/- 5 kHz from the rest frequency typically covers a pass.
5. **Listen first** before transmitting. Satellites are shared resources; don't talk over an active QSO.
6. **Identify with grid square** — common practice for satellite QSOs.
7. **Be brief** — the entire pass is typically 10-15 minutes; some operators limit themselves to 30-60 second exchanges to make room for others.

For linear transponder satellites, the procedure is similar but you tune the radio's continuous spectrum within the transponder's passband rather than picking a fixed FM frequency.

## See also

- §24-00 — Chapter overview
- §24-02 — VHF (where the 145.8-146.0 segment lives)
- §24-03 — UHF (where 435-438 lives)
- §06 — Satellites (operating-procedure detail)
- §06-01 — FM vs. linear satellites
- §06-02 — Doppler shift correction
- §06-04 — Tracking strategies
