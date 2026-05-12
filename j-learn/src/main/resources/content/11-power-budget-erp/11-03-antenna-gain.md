---
id: 11-03
title: Antenna Gain
chapter: 11
section: 03
level: mixed
status: draft
---

# Antenna Gain

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Antenna gain is **directional** — it concentrates energy in some directions at the expense of others. A "10 dBi gain antenna" radiates ten times the power density of an isotropic radiator *in its peak direction*, but probably less in other directions, and possibly negative gain (= lower power density than isotropic) in nulls.

For power-budget purposes, what matters is the gain **in the direction you care about**: typically the direction toward your QSO partner. Antenna spec sheets advertise peak gain; real-world budgets need direction-specific gain.

This section unpacks gain for budget calculations.

## dBi vs. dBd

Two reference radiators:

- **Isotropic radiator (dBi)**: a theoretical point source that radiates equally in all directions. The unit is decibels referenced to isotropic.
- **Half-wave dipole (dBd)**: a real reference. A free-space half-wave dipole has 2.15 dBi gain in its broadside direction. The unit is decibels referenced to a dipole.

The relationship: **dBi = dBd + 2.15**.

For US amateur use, dBi is more common (matches FCC and most modeling software). For older specifications and antennas marketed by gain over a dipole, dBd is sometimes used. Always note which.

## Gain numbers for typical amateur antennas

Approximate peak gain in free space (no ground reflection):

| Antenna | dBd | dBi |
|---------|-----|-----|
| Isotropic | -2.15 | 0 |
| Half-wave dipole | 0 | 2.15 |
| Folded dipole | 0 | 2.15 |
| Quarter-wave vertical (no radials, perfect ground) | -1 | 1 |
| Inverted V (120° apex) | -0.5 | 1.6 |
| Full-wave loop | 1.5 | 3.6 |
| 2-element Yagi | 4 | 6 |
| 3-element Yagi | 5–6 | 7–8 |
| 5-element Yagi | 7–8 | 9–10 |
| 7-element Yagi | 9 | 11 |
| 5-foot dish at 1296 MHz | 17 | 19 |
| 6-foot dish at 1296 MHz | 18 | 20 |
| 4-stack 13-element 2 m Yagis (EME) | 16 | 18 |

Real-world over real ground: add 4–6 dB to the free-space value for a horizontal antenna at typical heights (this is the constructive ground-reflection gain). For verticals, the ground gain depends on radial system quality and ground type.

## Gain × pattern shape

A high-gain antenna has a **narrow** beam: more gain forward, less in other directions. The relationship:

**Beamwidth × gain = constant** (approximately; the exact factor depends on antenna type).

| Antenna | Forward gain (dBi) | Beamwidth (deg, broadside) |
|---------|---------------------|------------------------------|
| Isotropic | 0 | 180° (omnidirectional vertically) |
| Dipole | 2.15 | 80° |
| 3-element Yagi | 7 | 65° |
| 5-element Yagi | 9 | 55° |
| 7-element Yagi | 11 | 50° |
| 6-foot dish (1296 MHz) | 20 | 7° |

So a 7-element Yagi has 11 dBi forward but only ~50° wide. 30° off-axis is already at -3 dB; 90° off-axis is in the deep null (-15 dB or worse).

## Direction-dependent gain

For a power-budget calculation toward a specific QSO partner:

1. **Determine the direction from your QTH to theirs.** The bearing.
2. **Look at your antenna's pattern** in that direction.
3. **Read the gain at that direction** off the pattern.
4. **Use that gain in the budget**, not the peak gain.

Example: Your antenna has 11 dBi forward (toward Europe), 0 dBi off the back, -10 dBi to the south. If your QSO partner is south of you:

- Peak gain: 11 dBi
- Toward partner: -10 dBi
- **EIRP toward partner = TX power × −10 dBi gain = 0.1 × TX power.** A 100 W station provides 10 W effective EIRP toward the south.

This is dramatic. Direction-specific gain is the difference between "I'm running 100 W" and "I have 1000 W EIRP" toward where I want to talk. The peak gain advertises the beam's best direction; the actual gain in your QSO direction is what determines whether the contact is possible.

## How gain combines with feedline efficiency

For the EIRP calculation:

**EIRP_dBW = P_tx_dBW − feedline_loss_dB + antenna_gain_dBi**

If feedline loss is 1 dB and antenna gain is 10 dBi:

EIRP = P_tx + (10 − 1) = P_tx + 9 dB.

For a 100 W transmitter (20 dBW), EIRP = 29 dBW = **800 W EIRP**.

Feedline loss is **subtractive**; antenna gain is **additive**. Both have the same dB scale.

## Effective vs. peak gain

For the budget, *effective* (in-the-direction-of-the-partner) gain is what matters. **Peak** gain (advertised on spec sheets) is the maximum.

Three implications:

1. **A 12 dBi antenna pointed at the wrong direction is worse than a 6 dBi omni** at the same TX power. Direction matters more than gain alone.
2. **Rotor systems exist for this reason.** Pointing the high-gain beam at the right direction is what gives the high-gain advantage; without rotation, you only have it on whatever fixed direction you've chosen.
3. **For Multiple QSO partners (e.g., contesting),** an antenna with broader pattern (lower peak gain but more uniform) sometimes outperforms a sharp-beam antenna because *more partners* are reachable. The math is contest-specific; usually the high-gain beam with rotator wins.

## Real-world gain vs. modeled gain

Spec sheets show **modeled gain** (peak, free space, ideal). Real-world is typically:

- **Free-space modeled gain - 0.5 to 1 dB**: real losses (boom losses, connector losses, balun losses) shave a fraction of a dB from peak.
- **Plus 4-6 dB at the right height over real ground**: constructive ground reflection in the lobe direction.
- **Minus several dB in unwanted directions**: pattern nulls deepen with real ground.

The "antenna at 0.5 λ above good ground has gain X dBi" numbers in §06-15 already include the ground-reflection gain. Modeling software reports both free-space and over-real-ground, distinguishing the two.

For budget purposes: **use the modeled-over-real-ground gain in your peak direction, minus 0.5 dB for real losses.**

## Gain in receive

Antenna gain is **bidirectional** — the antenna receives with the same gain in the same direction it transmits. A 12 dBi gain Yagi pointed at a station's direction makes received signals appear 12 dB stronger compared to an isotropic antenna; equivalently, 12 dB more sensitivity.

This is why a high-gain beam is worth so much for DX: 12 dB gain on TX *and* 12 dB on RX = 24 dB total round-trip improvement vs. an isotropic system.

## Directional vs. omnidirectional tradeoffs

| Configuration | Pros | Cons |
|---------------|------|------|
| Omnidirectional (vertical, dipole) | Works in any direction simultaneously; no rotation needed | Lower peak gain |
| Single fixed beam | Higher peak gain in that direction | Doesn't work elsewhere; rotation required for general use |
| Rotatable beam | Highest gain in any direction (with rotation) | Requires mast, rotator, time to rotate |
| Multiple fixed beams | Multiple "best directions" simultaneously | Multiple feedlines; coupling/switching complexity |
| Phased array | Pattern shaping under control | Complex; expensive |

For amateur use, the typical progression is: dipole → vertical → 2-element Yagi (rotatable) → 3-element Yagi → 4–7 element Yagi → tower with stacked Yagis.

## Common antenna-gain mistakes

- **Trusting the manufacturer's "peak gain" claim.** Check whether it's modeled or measured, free-space or over-ground, peak or in a specific direction.
- **Adding ground-reflection gain to a free-space figure.** If the antenna spec says "5 dBi free space" and you put it 0.5 λ above ground, you don't get 5 + 6 = 11 dBi. The 5 dBi is the antenna's intrinsic gain; the ground reflection redistributes it (gain in the lobe direction is enhanced; gain in the null direction is reduced). The "over real ground" peak gain is typically 4-5 dB above the free-space peak gain for the lobe direction.
- **Using peak gain for a contact in a different direction.** A 12 dBi forward Yagi has -5 dBi to the side and -10 to the back. Toward those directions, your effective antenna gain is 17 dB or 22 dB lower than peak.
- **Ignoring elevation pattern.** A horizontal antenna at low height has its peak in the wrong direction (high elevation) for DX. The "peak gain" in the takeoff-angle direction is what matters for DX, not the absolute peak.

## See also

- §11-00 — Chapter overview
- §11-04 — ERP / EIRP output
- §06-15 — Radiation patterns (where these gain numbers come from)
- §06-13 — Ground-plane effects (how ground modifies free-space gain)
- §06-14 — Modeling concepts
- §08-05 — ERP for RF safety
