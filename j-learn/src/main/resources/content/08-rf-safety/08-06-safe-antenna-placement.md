---
id: 08-06
title: Safe Antenna Placement
chapter: 08
section: 06
level: simple
status: draft
---

# Safe Antenna Placement

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

This section translates the rules and limits into **practical antenna-placement guidance**. Where should the antenna go relative to your house, your yard, your neighbors? How high? How far from the nearest people-occupied space? What constitutes a "safe distance" at typical amateur power levels?

The numbers below are not legal substitutes for an actual MPE evaluation — every installation differs, and the J-Hub RF Exposure Calculator (§12) does the case-specific math. But these guidelines will keep typical amateur installs out of trouble before you start trimming dimensions.

## The basic safe-distance numbers

For typical amateur antennas with 100 W and 1500 W on the most common bands, **assuming uncontrolled environment limits** (the bystander case):

| Antenna + power | 14 MHz (20 m) | 28 MHz (10 m) | 50 MHz (6 m) | 144 MHz (2 m) | 432 MHz (70 cm) |
|-----------------|---------------|---------------|---------------|----------------|-----------------|
| 100 W into dipole (5 dBi) | 4 ft | 9 ft | 24 ft | 24 ft | 14 ft |
| 1500 W into dipole (5 dBi) | 14 ft | 35 ft | 90 ft | 90 ft | 55 ft |
| 100 W into vertical (1 dBi peak) | 3 ft | 6 ft | 16 ft | 16 ft | 9 ft |
| 1500 W into vertical (1 dBi) | 8 ft | 22 ft | 60 ft | 60 ft | 35 ft |
| 100 W into 6-element Yagi (12 dBi) | 9 ft | 19 ft | 50 ft | 50 ft | 30 ft |
| 1500 W into 6-element Yagi (12 dBi) | 36 ft | 76 ft | 200 ft | 200 ft | 120 ft |

These are **uncontrolled-environment** distances (5× tighter than controlled). Halve them for the operator-position case (you, who can move out of the field).

## Guidelines for typical installs

### HF antennas (1.8–28 MHz)

- **A 30+ ft horizontal antenna at 100 W is essentially never an MPE problem** at any band. The MPE distances are well within the antenna's natural separation from people.
- **A vertical antenna 20+ ft from the nearest occupied space at 100 W is safe** on all HF bands.
- **An attic antenna or low antenna (< 15 ft above operator areas)** at 100 W needs explicit evaluation — the operator and family below the antenna may be in the near field.
- **Power-amp-fed (1500 W) installations** need explicit evaluation on every band, especially 6 m, 2 m, and 70 cm if multi-band capable.

### VHF/UHF antennas (50 MHz and up)

The 30–300 MHz range is where MPE limits are tightest. Practical implications:

- **A 2 m FM mobile vertical** (5 dBi gain) at 50 W on the roof of a car: MPE distance to the driver is ~4 ft. Most car installs put the antenna 2-3 ft from the driver's head, which is ~1 ft below the formula safe distance, BUT the driver is in a controlled environment (knows TX is happening, can react). Practical guidance: keep mobile VHF/UHF antennas ≥ 2 ft from the driver.
- **A 2 m base-station vertical (5–8 dBi) at 50–100 W**: 15–25 ft to bystander areas is typically fine.
- **A 6-element 2 m Yagi** at 100 W on a tower: the forward-direction MPE distance is around 50 ft. If you have neighbors within 50 ft of the *forward direction*, evaluate carefully (or aim the beam away from them as default).

### Magnetic loops and indoor antennas

These are the genuinely problematic cases:

- **A 1-meter magnetic loop at 100 W**: H-field at the operating chair (typically 3-5 ft from the loop) is borderline-uncontrolled-MPE on 80–40 m. Evaluate carefully. Best practice: position yourself 2+ meters from the loop during transmit if at all possible.
- **An attic dipole at 100 W** above a bedroom: the bedroom occupants 6-10 ft below the dipole are in the near field at MF/HF frequencies. Compliance is usually achievable but not automatic; evaluate.
- **A balcony vertical at 100 W on 20 m, with a neighbor's window 8 ft away**: you may be over MPE limits at the neighbor's window. Evaluate. Likely need to mitigate (lower power on this band, or move antenna).
- **A 1500 W mobile install** in a vehicle: the vehicle occupants are within ~3-5 ft of the antenna. Even with the body of the car as some shielding, this needs explicit evaluation. The OET-65 supplemental document covers vehicle MPE in detail.

## Antenna height: the distance-equivalent

Height above ground typically gives you horizontal-distance equivalence to nearby spaces, but more usefully it changes the elevation pattern in ways that benefit safety:

- **A high horizontal antenna** (30+ ft for 100 W on 20 m) radiates predominantly to low elevation angles. The downward radiation toward your yard is much weaker than the broadside radiation.
- **A low horizontal antenna** (8-12 ft) has a near-zenith pattern, putting more radiation directly downward — *worse* for the people below.
- **Vertical antennas** have a fan-shaped pattern with main lobes 18-25° above the horizon. Ground-level radiation directly under a vertical is relatively low.

The implication: **going up generally helps safety**, both by adding distance to nearby people and by changing the pattern.

## Beam direction and pattern control

For directional antennas (Yagis, log periodics, beams), the **direction of the beam during high-power TX** matters. Your forward gain might be 12 dBi, but your **back gain** is typically -10 dBi or worse (i.e., 22 dB front-to-back). A 1500 W station on a 12 dBi forward-gain antenna has only 75 W effective backward EIRP — much friendlier for whatever sits behind the tower.

Practical guidance:

- **Default the beam direction** when operating high power so the lobe avoids your house, your neighbor's house, and the public sidewalk.
- **If possible, use the rotator's home position** to set "default away" pointing for high-power tune-up.
- **Document the beam-direction rules** in your station setup; if you run a multi-op contest, brief operators.

## Property lines and the public

The FCC rules don't care about property lines per se — they care about where humans might be. But practically, MPE compliance often determines where on your lot you can install antennas:

- **Antennas at the back fence** with neighbor proximity: evaluate at the neighbor's nearest occupied space (deck, window, yard play area).
- **Antennas near sidewalks, alleys, or rights-of-way**: pedestrians passing by are bystanders. Evaluate.
- **Setback rules** in your CC&R / HOA / municipal code may mandate antenna distances from property lines for non-RF reasons. Comply with those too — but they're typically more restrictive than RF safety, so meeting them usually helps with MPE compliance.

## "Family room" check

For typical residential installs, the most overlooked check is **the family room** (living room, kitchen, bedrooms). In particular:

- An attic antenna directly above the master bedroom: the bedroom occupants spend hours below the antenna. Evaluate against time-averaged exposure.
- A roof-mounted vertical 12 ft above an upstairs bedroom: similar issue.
- A wire dipole strung from house corner to a tree, passing over the deck where the family eats dinner: evaluate at the deck.

These cases are usually compliant at 100 W; they may not be at 1500 W.

## Specific scenarios

### Apartment or HOA balcony (common case)

- Install: small portable vertical or magnetic loop on the balcony.
- Concerns: neighbor windows within ~6-15 ft, possibly directly above or below.
- Likely status: **MPE compliance can be tight**, especially for downstairs neighbors below a vertical antenna. Evaluate specifically. Use ≤ 100 W. Position the antenna to maximize distance from neighbor occupied spaces.

### Mobile install with high power (e.g., 1 kW into car-mounted vertical)

- Concerns: occupants are 3-6 ft from the antenna.
- Status: requires careful evaluation. The body of the car provides some shielding for sidewall-mounted antennas. Roof-mounted antennas have less shielding. The OET-65 mobile RF exposure rules are explicit; consult them.
- Typical mitigation: 100% time-averaged power = peak × duty × TX-time fraction; the formula often saves you because mobile users have low TX-time fractions.

### Field day / portable contest setup

- Install: temporary antennas, possibly close to people.
- Status: same MPE rules apply. The "field day exception" some hams talk about does not exist; you are responsible for evaluation even at temporary sites.
- Practical: keep antennas at proper distances from operating areas and bystanders. Use operating-position discipline; don't have non-operators (visitors, family) close to the high-power antenna.

### EME / high-gain narrow-beam stations

- Install: very high-gain antennas (15-25 dBi) at high power.
- Concerns: forward-direction EIRP is enormous (> 100 kW common); MPE distances in the main beam can be hundreds of feet.
- Status: the main beam is pointed at the moon, which means it's pointed *up* most of the time. Ground-level exposure is dominated by sidelobes, which are 20-30 dB below the forward gain. Usually compliant at the station; evaluate for any structure that might be in the main beam direction (other towers, buildings).

> **Advanced —** Near-field exposure for low antennas requires the modified-image-theory formulas: the antenna's image in lossy ground re-radiates with reduced amplitude and shifted phase. For very low antennas (< 0.1 λ above ground), the field at ground level has both reactive and radiating components, with the reactive component frequently dominant within 0.05 λ. The OET-65 calculator and ARRL's tool both account for this; manual hand-calculation of low-antenna near-fields is error-prone and typically conservative by a factor of 2-3.

## Common mistakes

- **Putting an antenna "as close as legal" to the property line.** RF safety isn't bound by property lines. Evaluate the neighbor's exposure.
- **Skipping the family-room evaluation.** Your kids in the bedroom below the attic antenna are bystanders.
- **Assuming antenna height alone is enough.** Height helps; it doesn't substitute for evaluation.
- **Using directional antennas with full power without thinking about beam direction.** A 12 dBi gain Yagi pointed at the neighbor's house is a 12 dB worse exposure than the same antenna pointed away.
- **Forgetting that vertical antennas put strong fields close to ground at moderate distances.** A "tall" vertical's MPE distance is at ground level, where children may be.

## See also

- §08-02 — MPE limits
- §08-03 — Controlled vs uncontrolled
- §08-04 — Duty cycle
- §08-05 — ERP / EIRP
- §12 — RF Exposure Calculator
- §06-05 — Ground-plane effects (where the pattern goes)
- §06-07 — Radiation patterns (where the gain is high)
