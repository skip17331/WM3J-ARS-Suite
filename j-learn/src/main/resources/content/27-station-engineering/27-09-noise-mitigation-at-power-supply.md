---
id: 27-09
title: Noise Mitigation at the Power Supply
chapter: 27
section: 09
level: mixed
status: draft
---

# Noise Mitigation at the Power Supply

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A modern switching power supply (SMPS) — the lightweight, cheap, efficient unit that powers most amateur rigs and almost all consumer electronics — operates by chopping DC at 30–100 kHz and rectifying. The chopping produces broadband electromagnetic energy that, without careful filtering at the supply's output, leaks into:

- **The 12 V DC rail** as conducted noise (a "switching birdy" at the chopping frequency and its harmonics).
- **The chassis** as common-mode noise (radiated by the case and the output leads).
- **The AC mains** as both conducted and radiated noise (carried back to neighboring outlets).

A noisy switching PSU can raise the receiver noise floor by 10–20 dB on HF bands. This section is about identifying and fixing PSU-borne noise — and when to give up and replace the PSU.

## How PSU noise manifests

Symptoms of PSU-borne noise on the receiver:

- **Discrete birdies** at fixed frequencies — usually at the PSU's switching frequency (30 kHz, 50 kHz, 65 kHz, 100 kHz are common) and integer multiples up through HF. A 65 kHz PSU produces birdies at 65, 130, 195, 260... kHz, with strong harmonics through 30 MHz.
- **Broadband hash** — a continuous noise floor 5–15 dB above the natural band noise floor. Doesn't move when the rig is tuned across the band. Disappears when the PSU is disconnected.
- **Hum at 120 Hz** — the rectified mains harmonic. Indicates failing filter capacitors or a poorly designed supply.
- **Hash that varies with rig load** — couples through shared DC supply on transmit/receive transitions.

Confirm the PSU is the source by:

1. Switching the rig to battery power (LFP pack from §27-07). If the noise vanishes, the PSU is implicated.
2. Listening with a portable AM radio held near the PSU. PSU switching is audible as a buzz or rasp.
3. Disconnecting the PSU from AC mains entirely. If the noise floor drops with the PSU off, the supply is conducting noise back through the mains and into something else.

## Adding common-mode chokes to the DC output

The first and cheapest fix: **wind the 12 V DC output leads through a ferrite toroid**, both the positive and negative leads together, multiple turns.

```
                    PSU output lugs (+ and -)
                       │
                       │
                       ▼
              ┌──────────────────┐
              │                  │
              │   FT-240-31 or   │  ← 5–7 turns of the DC twisted pair
              │   FT-240-43      │     through the toroid
              │                  │
              └──────────────────┘
                       │
                       ▼
                 To RIGrunner / rig
```

This **kills common-mode noise** on the DC supply (where both conductors carry noise in the same direction relative to chassis). It doesn't help differential noise (where the noise is between the + and - conductors), but most PSU noise that reaches the rig is common-mode.

**Choice of mix:**

- **Mix 31** — best from 1 MHz to 30 MHz. The HF workhorse.
- **Mix 43** — best from 30 MHz to 300 MHz. For VHF/UHF radios.
- **Mix 75** — best below 1 MHz. For low-band stations or PSUs with low switching frequencies.

A stacked pair (one Mix 31 and one Mix 43) gives coverage across the entire amateur range. Cost: ~$20 for two toroids.

## RF feedthrough capacitors at the PSU

For a more permanent fix, install **RF feedthrough capacitors** on the PSU output. These are small (1000 pF to 10 nF) capacitors connected between each DC output lead and the chassis, located *physically at the output terminal*. They shunt high-frequency noise to chassis where it can be returned to AC ground.

```
       PSU chassis
       ─────────────────
              │     │
              ▼     ▼
         === 1nF  === 1nF
            ▼     ▼
       +12V ●─────●─── DC negative output
            ↓     ↓
            (to load)
```

The capacitors must be **rated for the rail voltage** (50 V is fine for 12 V supplies) and **physically close** to the output terminal — long leads turn them into inductors.

Suitable parts:

- **Murata DSS6NZ82A102Q55B** — surface-mount feedthrough, 1 nF, 50 V.
- **Amidon 1000pF disc ceramic** — through-hole, easier for retrofit.
- **Cornell-Dubilier KP series** — high-quality film, good for higher voltage.

Adding feedthrough caps requires opening the PSU. **Always unplug from AC and discharge the input filter caps** (a 1 MΩ resistor across the input filter for 30 seconds) before working inside.

> **Advanced —** A well-designed switching PSU already has feedthrough capacitors at the output, plus a common-mode choke on the AC input. Manufacturers cheap out on these for cost reasons. Adding external compensation can recover what should have been there from the factory. For a *really noisy* PSU, the best approach is to add: (1) external chokes on DC output, (2) external chokes on AC input, (3) feedthrough caps if accessible, (4) a metal shield over the entire PSU bonded to station ground. Cumulative improvement can be 20–30 dB. At some point, the cost of all this exceeds the cost of just replacing the PSU with a better one.

## Replacing a noisy switcher with a linear

The nuclear option: replace the switching PSU with a **linear power supply** that uses a 60 Hz transformer, rectifier bridge, and series-pass regulator. No switching, no birdies, just a single fundamental at 120 Hz that's trivially filtered.

**Linear PSU advantages:**

- Essentially zero RF noise output.
- Robust under transient loads.
- Long lifetime (the failure mode is usually a single capacitor that's easy to replace).

**Linear PSU disadvantages:**

- Heavy. A 30 A linear weighs 25–40 lb; a 30 A switcher weighs 4–8 lb.
- Less efficient (60–75% vs 85–92%) — produces more heat.
- More expensive at the same current rating ($300–$500 vs $100–$200).

**Recommended linear units:**

- **Astron RS-35M** — 25 A continuous, 35 A peak. ~$300. The legendary amateur standard since the 1970s.
- **Astron VS-50M** — 37 A continuous, 50 A peak, with adjustable voltage. ~$400.
- **Daiwa SS-330W** — 30 A continuous, variable voltage. ~$350.
- **Diamond GZV-4000** — Japanese-market unit, 40 A, very clean. ~$500 used.

For an amateur who has chased PSU noise through every available switcher fix and still isn't satisfied, a linear is the cure. Astron RS-35 service life routinely exceeds 30 years; the only reason to retire one is mechanical failure of the case.

**When is the swap worth it?**

- Receiver noise floor is consistently 6+ dB above the natural band noise.
- After installing chokes and feedthrough caps, the floor is still elevated.
- The station is in a quiet RF environment where noise floor really matters (rural DX site, weak-signal VHF, EME).
- The operator can tolerate the weight and heat.

For a typical urban station already swimming in S5 power-line noise, the PSU contribution may not be the dominant source. Linear PSU spending should be deferred until the *other* sources are mitigated.

## Ground-loop hum

A subtle PSU-related issue: **120 Hz hum** from a ground loop between PSU, rig, and computer/audio interface.

Mechanism: the PSU's DC output ground is bonded to AC safety ground via its internal grounding. The rig's chassis is bonded to AC safety ground via its own AC cord. The two bond points are at slightly different potentials (a few mV at most), but at 60 Hz this drives a tiny current through the audio interconnect. The current modulates the audio signal — audible as hum.

**Fixes:**

- **Audio isolation transformer** on the rig-to-computer audio cable (MFJ-1224, SignaLink, or generic 600:600 Ω 1:1 transformer). Breaks the DC ground path while passing audio.
- **USB isolator** between rig and computer (Adafruit USB isolator, ~$30). Breaks the DC ground path of the USB shield.
- **Verify the AC outlets** are wired correctly. A miswired outlet (hot/neutral reversed, missing ground) elevates the ground-potential difference.

## A diagnostic procedure

If the receiver is noisier than it should be and the PSU is suspected:

1. **Run the rig on battery** for 30 minutes. Note the noise floor.
2. **Plug in the PSU** and listen. Did the noise floor rise? By how many dB?
3. **Add a Mix 31 toroid** on the DC output, 5 turns. Did it improve?
4. **Add a Mix 31 snap-on** on the AC input. Did it improve?
5. **Try a different outlet** on a different breaker. Did it improve? (Indicates conducted return through AC mains.)
6. **Open the PSU and add feedthrough caps** on the DC output (only if comfortable with mains-voltage work). Did it improve?
7. **Replace the PSU with a different unit** — borrow from a friend, use a different model — and re-measure.

Each step either rules out a class of fix or proves a hypothesis. Most stations are quieted by step 3 or 4. Steps 5–7 are for the stubborn cases.

## Cross-references

- [§14-02 — Household Noise Sources](../14-rfi/14-02-household-sources.md) — diagnosing noise more broadly
- [§14-05 — Isolation Workflow](../14-rfi/14-05-isolation-workflow.md) — the systematic isolation procedure
- [§27-05 — Ferrite Deployment](27-05-ferrite-deployment-strategy.md) — choke placement strategy
- [§27-06 — Power Distribution](27-06-power-distribution.md) — separation of digital and RF supplies
