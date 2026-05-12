---
id: 29-05
title: Helical Antennas
chapter: 29
section: 05
level: mixed
status: draft
---

# Helical Antennas

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A **helical antenna** is a wire wound into a helix — a 3D spring shape — fed at one end against a ground-plane reflector at the other. The geometry produces **circular polarization** natively, with gain proportional to the helix's length in wavelengths. Helicals are the standard for high-gain circular-polarization work: amateur satellite uplinks and downlinks at UHF and above, telemetry stations for spacecraft, and the original feed for the deep-space dish at Goldstone before phased arrays took over.

For amateur satellite work, helicals show up in two contexts: as the dish-mount feed for a parabolic dish ground station (microwave bands, especially 13 cm and 3 cm), and as a standalone "helix on a boom" antenna for 70 cm or 23 cm direct use. This section covers both.

## Circular polarization and the helix's mode

A helix transmits and receives circularly polarized waves. The handedness (RHCP — right-hand circular polarization — vs LHCP — left-hand) depends on the winding direction:

- **Right-hand winding (looking from the feedpoint outward, the helix spirals clockwise away from you):** transmits and receives **RHCP**.
- **Left-hand winding:** **LHCP**.

Most amateur satellites use **RHCP** by convention (matching the dominant ground-station preference and the natural handedness produced by typical satellite antenna designs). But not all — a few have specifically chosen LHCP, and some satellites tumble so the polarization is effectively unknown and the operator switches between RHCP and LHCP looking for the better signal.

A helix in its **axial mode** (the mode used for satellite work) radiates along the helix's axis — so it's a directional antenna, like a Yagi, with a forward beam pattern. The helix has to be **pointed at the satellite** for best gain, the same as a Yagi.

The axial-mode condition: helix diameter ~ 0.32 wavelength, helix turn-to-turn spacing ~ 0.25 wavelength, total length determines gain (more turns = more gain = narrower beam).

## Gain vs. length

A rough formula for axial-mode helix gain:

**Gain (dBi) ≈ 11.8 + 10·log₁₀(N·S/λ)**

where N is the number of turns and S/λ is the turn spacing in wavelengths. For a typical S/λ = 0.25 helix:

| Turns | Length (wavelengths) | Approximate gain | 70 cm length | 23 cm length |
|-------|----------------------|------------------|--------------|--------------|
| 4 | 1.0 | 11.8 dBi | 27 in | 10 in |
| 8 | 2.0 | 14.8 dBi | 54 in | 19 in |
| 12 | 3.0 | 16.6 dBi | 81 in | 28 in |
| 16 | 4.0 | 17.8 dBi | 108 in | 37 in |
| 20 | 5.0 | 18.8 dBi | 135 in | 47 in |

Gain grows logarithmically with length — doubling the length adds only 3 dB. Past about 20 turns, the practical gain levels off because of manufacturing tolerance and dispersion effects.

## The "helix on a boom" home build

This is the standalone-antenna application: a helix as the satellite ground-station antenna, mounted on a boom and pointed by a rotor.

A typical 70 cm helix-on-a-boom for satellite downlink:

- **8-12 turns** of #10 or #12 copper wire, wound on a fiberglass or PVC center support.
- **Diameter:** about 8.5 inches (0.32 wavelength at 437 MHz).
- **Turn spacing:** about 6.7 inches (0.25 wavelength).
- **Total length:** 54 to 81 inches.
- **Reflector:** a flat square plate or wire-mesh screen, about 0.75 wavelength on a side (~20 inches square), at the feedpoint end.
- **Feedpoint:** the wire's start is soldered (or clamped) at the center of the reflector to a coax connector; the coax shield grounds to the reflector. Often a quarter-wavelength impedance-matching section (a flat strip of copper at the feedpoint) reduces the natural ~140 ohm helix impedance to 50 ohms.
- **Boom mount:** the reflector mounts to a horizontal boom; the helix projects forward; the whole assembly mounts on a rotor.

Construction takes a weekend. Plans are widely published — try AMSAT-DL's DUBUS articles, W1GHZ's microwave-antenna book, or any of several VHF/UHF antenna handbooks.

A 12-turn 70 cm helix gives ~16 dBi gain and a beamwidth of about 30°. That's significantly more gain than the Arrow's 70 cm side (11.5 dBi) at the cost of a narrower beam and a much bigger physical antenna.

## Helicals as dish feeds

The other major application: a **short helix** (typically 3-5 turns) at the focal point of a parabolic dish, serving as the dish's feed. This is the standard setup for:

- **13 cm satellite work** (2.4 GHz S-band downlink — used by some older AMSAT satellites and now QO-100 via S-band uplink).
- **3 cm satellite work** (10 GHz X-band — QO-100 downlink, and the standard amateur microwave-DX band).
- **Earth-Moon-Earth (EME)** on 23 cm, 13 cm, 6 cm, and 3 cm.

The dish's gain is determined by its diameter; the feed's job is to illuminate the dish efficiently. A short helix illuminates a dish well, with circular polarization that matches what most satellites transmit. A 1.2 m dish at 10 GHz with a 4-turn helix feed gives ~35 dBi gain — that's the kind of number you need for QO-100 reception from a North-American latitude (when you can see it).

A typical helix-fed-dish for QO-100 transmit/receive in Europe:

- **0.6 m to 1.2 m offset-fed dish** (the kind used for European satellite TV).
- **2.4 GHz uplink helix feed** (a 3-turn helix at the dish's focus, RHCP).
- **10 GHz downlink with an LNB** (the same offset dish; the LNB replaces the helix feed for downlink reception; some setups use a separate dish for uplink and downlink).

This is more complicated than 2 m / 70 cm satellite work — but it's the entry-point setup for 13 cm and 3 cm amateur satellite operation, and the gear is largely repurposed from the consumer satellite-TV market.

## Tower-mounted helicals for serious operators

A fixed helix installation on a tower is the high-end ground station — the kind of setup you see at AMSAT-NA member shacks doing serious linear-bird and AO-7 work.

Typical configuration:

- **Dual helicals:** one for 2 m and one for 70 cm, on a common az/el rotor at the top of a 30-50 ft tower.
- **Each helix:** 14-20 turns, 16-19 dBi gain, beamwidth 20-25°.
- **Polarization choice:** Most are wound for RHCP, but some operators install dual RHCP/LHCP helicals side-by-side (or a single switchable design) to handle both handedness depending on the satellite.
- **Az/el rotor:** Yaesu G-5500 or equivalent, with computer control for satellite tracking.
- **Mast-mounted preamps** (§29-07) at the feedpoint, before the long coax run down the tower.
- **Hardline or LMR-600 coax** for the run from the tower base to the shack.

Cost is in the $3000-5000 range for the antennas and rotor (helicals are not commercially common; most are homebrewed or commissioned from M2 or similar builders), plus tower and installation costs.

The payoff is the ability to work weak linear birds throughout their passes with margin to spare, and the option to chase deep-space amateur transponders (AO-7 from a long way off, EME if you add another rotor and high-frequency setup).

> ⚙️ **Advanced —** Helicals have a known impedance issue: the feedpoint Z is around 140 ohms naturally, which mismatches 50-ohm coax by 2.8:1 SWR. The quarter-wave matching section at the feedpoint (a flat copper strip ~λ/4 long, transforming 50→140 ohms via Z₀ = √(50 × 140) = 84 ohms characteristic impedance) is the standard fix. Some commercial helicals use a 100-ohm balun or a tapered matching transformer instead. Verify SWR on a vector network analyzer before locking in the design — small geometry errors translate to significant SWR shifts at 70 cm and above.

## Comparing helical to crossed Yagi

For amateur satellite work, the main alternative to a helix is a **crossed Yagi** (§29-06): two Yagis arranged 90° apart with a phasing harness to produce circular polarization.

| Factor | Helical | Crossed Yagi |
|--------|---------|--------------|
| Polarization | Inherently circular (fixed handedness) | Circular via phasing harness; can switch RHCP ↔ LHCP |
| Gain per length | Lower (about 12 dBi for a 27" 70 cm helix) | Higher (about 13-14 dBi for a comparable Yagi) |
| Bandwidth | Wide (10-20% of center freq usable) | Narrow (5-10%) |
| Construction | Requires helix winding; harder to homebrew well | Easier (two off-the-shelf Yagis + phasing) |
| Physical fragility | The exposed helix is vulnerable to icing | More robust |
| Polarization switching | Requires separate RHCP and LHCP antennas | Built into the harness |

For most amateur satellite operators, the crossed Yagi with switchable polarization is the better choice. Helicals win when bandwidth or pattern symmetry matters — or when the application is microwave dish feed, where the helix is unmatched as a focal-point illuminator.

## See also

- §29-06 — Polarization switching (crossed Yagis with phasing harness)
- §29-04 — Eggbeater omni (low-gain circular polarization alternative)
- §29-07 — Mast-mounted preamps
- §06 — Antennas (gain, beamwidth, polarization theory)
