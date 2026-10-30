---
id: 25-05
title: Field Strength Meters
chapter: 25
section: 05
level: mixed
status: published
---

# Field Strength Meters

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A **field strength meter (FSM)** is a portable RF receiver with a known antenna and a meter showing **relative signal strength** at the operator's location. You walk around with it; the meter rises and falls. With it you can answer questions a fixed-station setup can't:

- **Where is my antenna actually radiating?** (Walk around it at 100 ft; map the pattern.)
- **Where is this interference coming from?** (Walk toward it with a directional antenna; the meter peaks at the source.)
- **Is the transmitter at the other end actually getting out?** (Stand near it; the meter goes up when keyed.)

The FSM is one of the oldest pieces of ham test gear (homebrew designs from the 1930s still work) and one of the simplest. A diode, a meter, and a piece of wire is enough for a working FSM.

## How it works

The simplest FSM is a **diode-detector** circuit:

```
   antenna ─┐
            ├── tuned circuit (optional) ── diode ──┬── DC microamp meter ─── ground
            │                                       │
           (RF ground / counterpoise)               C (RF bypass)
```

The antenna picks up RF voltage. The diode rectifies it. The resulting DC current flows through the microamp meter. **More RF → more DC → meter rises.** It's not calibrated to absolute V/m, but the *relative* reading is good enough to tell you "closer is bigger."

For more sensitivity, add an op-amp or a tuned circuit. Commercial FSMs typically have:

- A short whip or telescoping antenna (calibrated, sometimes)
- Tuned input (selects amateur bands)
- Amplifier with adjustable gain
- Analog or LCD meter

Cheap kits from the 1970s (Heathkit HD-1422, MFJ-802) still circulate at hamfests for $20–$40. Modern equivalents (MFJ-852, Diamond GSV-3000) are $80–$150.

## Three classic use cases

### 1. Antenna pattern measurement

Put your transmitter into a clean low-power CW carrier (5 W max — you'll be walking near the antenna). Walk a circle around the antenna at a fixed radius (say 100 ft on a dipole, 50 ft on a vertical) and write down the FSM reading every 10° of compass bearing.

Plot the readings on polar paper. You'll see the antenna's actual **azimuth pattern** — usually different from the textbook prediction because of ground, nearby trees, structures, the feedline, etc.

What this reveals:

- **A dipole's null at the ends.** Confirmed if the FSM reading drops 15–20 dB compared to broadside.
- **A vertical's omnidirectional pattern.** Confirmed if all 36 readings are within ±2 dB of each other.
- **An asymmetry.** Usually means a nearby conductor (gutter, fence, power line) is coupling into the antenna and distorting the pattern.
- **Reflections from buildings.** Look for unexpected peaks that don't fit the model.

> **Advanced —** A full pattern measurement should be at multiple radii. Near the antenna (within ~λ/4) you're measuring near-field, which has reactive components that don't represent the radiated pattern. Far enough out (~10× λ) you're in the far field and the pattern stabilizes. For HF this means measurements at 100s of feet, which is rarely practical — most ham pattern measurements are in the transition region and need to be interpreted with that in mind.

### 2. Finding a transmitter (or RFI source)

The FSM with a **directional antenna** (a small Yagi, a loop, even a folded dipole) is a direction-finding tool. Two scenarios:

**Direction-finding a transmitter on purpose.** Foxhunts use exactly this gear: a 2 m or 70 cm receiver with attenuator + Yagi + FSM-style readout, used to triangulate a hidden transmitter. Several rounds of bearing-from-different-locations narrow the location to within a few feet.

**Hunting RFI** is the same technique applied to an unintentional radiator. You hear a buzz on 40 m and want to find the source:

1. Tune the FSM to 40 m if possible (or use the rig's S-meter as your FSM).
2. Pick a directional antenna and walk toward where you think the noise is.
3. Watch the meter rise as you get closer.
4. At each location, rotate the antenna 360° and note the bearing of peak reading.
5. Triangulate from 2–3 locations.

A 30 dB attenuator helps when you get close — without it you saturate the receiver and lose the ability to tell "closer" from "even closer." See **§14 RFI** for the full RFI-hunting workflow.

### 3. Verifying your transmitter is actually radiating

Plug a 50 Ω dummy load into your rig. Key the rig. Stand 10 ft away with an FSM. The meter should read **very low** — a dummy load doesn't radiate much. Now swap to a real antenna. Key again. The meter should jump dramatically — typically 30 dB or more, depending on the antenna and distance.

This is a quick sanity check that:

- The rig is putting out power (vs. transmitting into nothing)
- The antenna is actually connected and not loose at a connector
- The antenna is actually radiating (not "tuner is hiding a dead element")

The dummy-load-vs-antenna comparison is the single most useful check the FSM enables — it takes 30 seconds and catches "I've been transmitting into a broken feedline for an hour" mistakes that you'd otherwise miss until the QSO partner doesn't reply.

## Cheap diode-detector designs

A homebrew FSM you can build in an hour:

```
Components:
- 1N5711 Schottky diode (good RF detector to ~1 GHz)
- 1 nF ceramic bypass capacitor
- 100 µA panel meter (any cheap moving-coil)
- ~1 m telescoping whip antenna
- A box

Layout:
    whip ─── 1N5711 ─┬─── meter ── chassis ground
                     │
                    1 nF
                     │
                     └── chassis ground
```

Total parts cost: ~$15. Sensitivity is enough to register 5 W into a 20 m dipole at 100 ft. For better sensitivity, add a 2N3819 JFET front-end or a single op-amp DC amplifier.

> **Advanced —** A diode detector is **square-law** at low signal levels and **linear** at high signal levels. This means the meter reading is proportional to *power* at low signals and to *voltage* at high signals. For relative pattern measurements at moderate power, square-law region is fine — equal-level peaks really are equal power. For accurate field strength in V/m, use a calibrated FSM with a known antenna factor.

## Pattern-measurement procedure (the proper way)

For a serious antenna pattern measurement:

1. **Set up.** Pick a calm day. Pick a location with minimal nearby conductors. Use lowest practical TX power.
2. **Mark the circle.** Drive small stakes every 10° around the antenna at the chosen radius.
3. **TX a steady carrier.** CW, no modulation, into the antenna.
4. **Walk the circle.** At each stake, hold the FSM at the same height, point the antenna the same way relative to the source, and record the meter reading.
5. **Repeat at multiple frequencies** if the antenna is multi-band (each band's pattern differs).
6. **Plot.** Polar plot software (or graph paper) gives you the pattern. Don't forget the noise floor — readings near the noise floor are dominated by ambient signals, not your antenna.

Common error: the operator walking with the FSM acts as a partial reflector. **Hold the FSM at arm's length on the same side at every reading**, and consider using a tripod-mounted FSM with binocular reading at a distance.

## Calibration (or the lack thereof)

Most FSMs are **uncalibrated** — the meter reading is in arbitrary units. That's fine for relative work (this position is louder than that position) but no good for absolute V/m or dBμV/m.

To calibrate, you need:

- A known signal source at a known distance and orientation
- A known antenna gain on your FSM
- A way to convert RF voltage at the FSM antenna to V/m in the field

A typical procedure: drive a known-good signal generator into a half-wave dipole. Measure the FSM reading at a known distance. Use the Friis formula to compute the field strength at that distance. Now you have one calibration point — extrapolate by assuming linearity (good for moderate signal levels).

In practice ham FSMs are almost always used relatively, not absolutely.

## Common mistakes

- **Walking too close to a high-power antenna.** RF burns and exposure limits matter. Stay clear and use lowest practical power for pattern work.
- **Acting as a reflector.** Your body bends the field. Use a tripod or arm's-length consistent placement.
- **Forgetting attenuator when close.** Saturates the FSM; loses sensitivity to small changes.
- **Measuring in the near-field and treating it as far-field.** Near-field readings don't represent radiation pattern.
- **Hunting RFI with a fixed antenna.** Direction-finding needs a directional antenna; an omni gives no useful bearing.

> **Advanced —** For exposure compliance under FCC rules, you need calibrated V/m readings, not relative FSM readings. The standard tool is a **broadband isotropic probe** (Narda, Holaday, EMR-300) which has multiple orthogonal sensors and reads peak vector field strength in V/m. These cost thousands of dollars; clubs sometimes own one for member use. See §14 (RFI / RF Exposure) for compliance workflow.

## See also

- §14 — RFI (hunting interference with an FSM)
- §15 — Noise Sources
- §25-03 — Spectrum Analyzers (a more sophisticated relative-strength tool)
- §06-07 — Radiation Patterns (antenna patterns)
- §17 — Formulas (V/m, dBμV/m, Friis)
