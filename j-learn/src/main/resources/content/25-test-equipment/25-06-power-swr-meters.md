---
id: 25-06
title: Power & SWR Meters
chapter: 25
section: 06
level: mixed
status: draft
---

# Power & SWR Meters

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A power/SWR meter is the most-used piece of ham test gear — most stations have one inline between rig and antenna 24/7. It tells you two things at once: **how much power is going forward** to the antenna and **how much is reflecting back**. From those two you compute SWR.

Despite being so common, power/SWR meters are full of subtle ways to get a wrong number. The biggest single mistake is reading **average power** when the signal is **SSB voice** — the resulting reading is 3–6× lower than the actual peak power, which leads to chasing nonexistent power problems for hours.

## What a power meter actually measures

Every directional power meter has the same core: a **directional coupler** that samples a tiny fraction (say -30 dB) of the forward-traveling wave, and separately samples the same fraction of the reverse-traveling wave. Those samples are rectified and displayed.

```
   rig ──┬─── (forward sample) ── diode ── meter ── FWD reading
         │
         ├─── (main line through, ~50 Ω) ───────── to antenna
         │
         └─── (reverse sample) ── diode ── meter ── REF reading
```

What gets displayed depends on the meter design.

## Meter types

### 1. In-line directional couplers — separate FWD and REF meters

The classic design. Two needles, two scales. The operator reads forward, reads reflected, and computes (or reads from a chart) the SWR. Examples: Bird 43, MFJ-815, MFJ-862, Diamond SX-200.

Accuracy is best in this design because each direction has its own dedicated meter and the coupler can be designed for high directivity. The downside: requires reading two values and a chart lookup.

### 2. Cross-needle meters

Two needles share a single dial face. They're mechanically linked so the **intersection point** of the two needles reads SWR directly on a curved scale, while the individual needles read forward and reflected power. Examples: Daiwa CN-101, CN-801, Diamond SX-100, Comet CMX-200.

Highly intuitive. The intersection moves around the dial as you change frequency and antenna — the position immediately tells you "match is good" (intersection in the green) or "match is bad" (intersection above the red SWR line).

```
                              FWD scale
                              ╱
            ╱┄┄┄┄┄┄┄┄┄┄┄ /
           ╱            ●  ← intersection: power AND SWR at this point
          ╱            ╱
         ╱            ╱
        ─────────────────── REF scale
                            ╲
                             ╲
```

### 3. Digital wattmeters

LCD or LED display showing forward, reflected, and computed SWR as numbers. Examples: LP-100A, Telepost Inc. LP-100, Array Solutions PowerMaster III, Surecom SW-102.

Pros: precise numerical reading, often multiple modes (PEP/average/peak hold), often calibration-traceable.
Cons: needs power to operate, less intuitive than analog needles for watching a quick trend.

## Peak vs. average reading — the biggest mistake

This is the most common source of confusion in ham power measurement. **The mode you select on the meter determines what you read, and SSB voice is different from CW or digital.**

### Average reading

The meter shows the **average power** over a 0.1–1 second window. For a continuous-carrier mode (CW key down, FM, AM carrier, FT8, RTTY), average power IS the power output — readings match the rig's spec.

For SSB voice, average power is *much* lower than peak power because speech has long silences between syllables. A 100 W PEP SSB transmitter measured average reads **~15–30 W** depending on speech compression. Many newcomers see this and conclude "my rig is sick" — it isn't.

### Peak reading (PEP)

The meter holds the highest power seen in the last ~5 seconds (a "peak detector"). For SSB voice, this is your actual PEP and matches the rig's spec.

For CW and digital modes, peak reading equals average reading (because the signal is constant during key-down).

### Quick rule

| Mode | Use this meter setting |
|------|------------------------|
| CW | Average (or peak — same value) |
| SSB voice | **Peak / PEP** |
| FM | Average |
| AM | Average (carrier power; peak shows ~4× carrier on 100% modulation) |
| FT8 / FT4 | Average |
| RTTY | Average |
| Digital voice (DMR / D-STAR / Fusion) | Average |

If your meter doesn't have a PEP mode and you're running SSB, **assume the actual PEP is roughly 5× the displayed average power** as a rough sanity check. Better yet, key the rig with a steady carrier (CW or RTTY at full power) to verify the maximum is what you expect.

> ⚙️ **Advanced —** "PEP" in FCC parlance is the average power over the single RF cycle of the peak envelope. Meters that report PEP typically use a peak-hold detector with a fast attack (a few µs) and a slow release (seconds). The peak-hold release time matters — too fast and the needle bounces around chasing each syllable; too slow and you lose the ability to see if peaks are increasing during a long transmission. 200 ms attack / 2 s release is typical.

## Reading SWR

SWR is **always** computed from the ratio of forward to reflected voltage:

```
SWR = (1 + sqrt(P_ref / P_fwd)) / (1 - sqrt(P_ref / P_fwd))
```

Most meters either compute this for you (cross-needle, digital) or you read it from a chart on the back panel.

Common SWR readings and what they mean:

| SWR | Interpretation | Action |
|-----|---------------|--------|
| 1.0:1 | Perfect match | Possible — dummy load reads this. Real antennas rarely. |
| 1.1–1.5:1 | Excellent | No action needed. |
| 1.5–2.0:1 | Good | OK to operate. Most antenna systems live here. |
| 2.0–3.0:1 | Marginal | Tuner can usually clean it up. Rigs above 100 W may fold back. |
| 3.0:1+ | Mismatch | Investigate — wrong band, antenna problem, water in connector. |
| ∞ (full reflection) | Open or short | Antenna disconnected, or feedline open / short. |

A **rising SWR over time** while the antenna hasn't been touched is the classic symptom of water intrusion in a connector — see §13 for diagnosis.

## Calibration and accuracy

Power meters are calibrated for specific frequency ranges. A meter calibrated for HF (1.8–30 MHz) will read incorrectly at 2 m. A meter rated for 1.8 MHz – 500 MHz handles most amateur bands but typically has 1–2 dB accuracy variation across that range.

Common calibration mistakes:

- **Bird 43 slug installed wrong band.** The Bird 43 uses interchangeable "slugs" — each calibrated for a specific frequency × power range. A 50 MHz, 100 W slug used at 14 MHz reads **wrong** (the slug coupler's directivity isn't optimized for 14 MHz). Bird sells dozens of slugs; you need one per band per power range.
- **Old diodes degrading.** Schottky and germanium diodes in cheap meters age and drift. A meter that read accurately 10 years ago may read 20% high or low today.
- **Forgetting the meter has a cal pot.** Many analog meters have an internal calibration potentiometer that adjusts the full-scale deflection. If you took it apart and bumped the pot, recalibrate against a known reference before trusting readings.

For occasional verification, a **commercial dummy load + a calibrated reference source** like a signal generator at known power is the standard. See §25-09.

## What NOT to do

- **Don't put the meter in line backward.** Some meters are directional and will read zero (or completely wrong) if reversed. The arrow on the case should point from rig to antenna.
- **Don't exceed the meter's power rating.** A meter rated 200 W is destroyed by a 1 kW key-down. Many meters have a power range switch — set it correctly.
- **Don't trust SWR with an antenna tuner between rig and meter.** The tuner makes everything look like 1:1 at the rig side, but the antenna side is unchanged. **Put the SWR meter between tuner and antenna** for the real SWR; put it between rig and tuner only if you need to confirm the rig sees a good match.
- **Don't ignore reflected power readings.** Even 1.5:1 SWR with high TX power produces several watts of reflected power — survivable for the rig but a sign the antenna isn't optimum.

## Bird 43 wattmeter — a quick word

The **Bird 43** is the industry-standard analog power meter, found in commercial radio shops, broadcast stations, and many ham shacks for decades. The 43 itself is the meter; **slugs** plug into it and determine the frequency range and power range. A "100C" slug is 50–125 MHz, 100 W full scale. A "250H" slug is 2–30 MHz, 250 W full scale.

The 43 has earned its reputation: rugged, reliable, repeatable. The slugs hold calibration for decades. Used 43s with a handful of slugs cost $200–500 at hamfests and are worth every dollar if you're serious.

The trade-off: you read forward power, **swap slug directionality**, read reflected power, then compute SWR. Cross-needle and digital meters do this in one operation.

> ⚙️ **Advanced —** The Bird 43's slugs are directional couplers built around a coaxial transmission-line section with a precision sampling loop. The directivity of a good slug is 25–30 dB — meaning a perfectly absorbed forward wave produces a "reflected" reading 25–30 dB below the forward, even with no actual reflection. This is the noise floor of any directional-coupler-based SWR measurement; it's why you can't measure SWR below ~1.05:1 accurately with a Bird, or below ~1.10:1 accurately with most ham meters.

## Common mistakes

- Reading average on SSB and concluding "low power" — covered above, the dominant mistake.
- Meter installed backward.
- Wrong slug or wrong range for the band/power.
- Trusting SWR reading at the rig side of an antenna tuner.
- Not zeroing the meter (analog) before reading.
- Reading SWR at the rig when there's significant feedline loss between rig and antenna — the rig-side SWR will read better than the actual antenna match because losses absorb reflected energy.

## See also

- §10 — Feedline & SWR
- §25-10 — Dummy Loads & Power Sensors
- §25-09 — Calibration Workflows
- §13 — Station Troubleshooting (SWR problems)
- §17-07 — SWR (the math)
