---
id: 14-06
title: Step-by-Step Elimination
chapter: 14
section: 06
level: simple
status: published
---

# Step-by-Step Elimination

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A printable, executable checklist for systematically eliminating sources of RFI in your house. Use this when the isolation workflow (§14-05) has identified a noisy circuit but you need to pin down which device on it. Or when you're doing a baseline cleanup of your shack and want to systematically reduce the noise floor by 6+ dB.

## Before you start

Set up:

- Radio on, tuned to a quiet frequency on a band where the noise is clearly audible.
- AGC off (so the S-meter doesn't compress the readings).
- Headphones to hear small differences clearly.
- Notebook to log results.
- Phone or watch to time intervals.

Take a baseline reading.

## The protocol

For each device on the suspect circuit, do this exact sequence:

```
[ ] Note S-meter reading with all devices on (baseline).
[ ] Unplug device #1.
[ ] Wait 15 seconds.
[ ] Read S-meter.
[ ] Calculate change. Log it.
[ ] Reconnect device #1.
[ ] Move to device #2.
[ ] Repeat.
```

After all devices, list the changes from biggest to smallest:

```
Device 4 (USB charger):    -8 dB     ← worst offender
Device 7 (LED bulb):       -3 dB
Device 2 (computer):       -2 dB
Device 1, 3, 5, 6, 8:      0 dB      ← negligible
```

Now you have a priority list.

## Standard fix table

For each identified offender:

| Device class | First fix | Second fix | Last resort |
|--------------|-----------|------------|-------------|
| Wall-wart power supply | Replace with quality unit | Snap-on ferrite on output cable | Live with it |
| LED bulb | Replace with quality brand | Add inline filter | Use incandescent in that fixture |
| Plasma / old TV | Add chokes to power and HDMI | Move antenna farther | Replace TV |
| Switching laptop charger | Add ferrite to DC cable | Use a different brand | Battery-only operation |
| Network gear | Add chokes to power and Ethernet | Use shielded cables | Move farther from radio |
| HVAC controller | Add ferrite to thermostat wires | Have HVAC tech check connections | Coordinate cycle times with operating |
| Smart-home hub | Add ferrites; use wired Ethernet | Replace device | Disable unused features |
| Old refrigerator compressor | Add ferrite to power cord | Service or replace | Live with cycle times |
| Doorbell transformer | Replace transformer | Disconnect entirely | Replace doorbell |

For each fix, **re-test after applying** to confirm the improvement.

## How to apply ferrite to an existing device

The most common fix. Done correctly:

1. **Identify the cable.** Power cord is the usual target; sometimes audio/data cable.
2. **Locate the ferrite as close to the device as possible.** A snap-on at the device end of the cord is much more effective than at the wall end.
3. **Wrap the cable through the core multiple times.** 3–5 turns for a snap-on; more turns = more impedance up to the core's saturation point.
4. **Secure with cable ties** so the ferrite doesn't slide.
5. **Re-test.**

If 5 turns through one snap-on isn't enough, use a stack of 3 snap-ons in series — each adds independently.

## How to test a fix

Test fixes systematically:

1. Read S-meter without the fix (re-confirm the baseline).
2. Apply the fix.
3. Read S-meter with the fix.
4. Calculate change. **Less than 6 dB improvement** = the fix isn't enough; try stronger version (more turns, different mix).

A typical successful ferrite fix improves noise by 10–20 dB at the radio. Anything less and you have either the wrong mix, too few turns, or the source is radiating from somewhere other than the cable you're choking.

## When ferrite isn't enough

Some devices are so noisy that filtering can't tame them:

- **Plasma TVs** — internal high-voltage switching radiates from the entire display, not the power cord. Filter the cord first; if not enough, the TV needs to go.
- **Bad solar inverters** — same idea. Internal RF radiates from the unit's own enclosure.
- **Powerline networking** — by design uses HF spectrum; no filter recovers the spectrum. Stop using.

For these, replacement or operational changes are the answer.

## Reasonable goals

For most amateur stations:

- **Baseline noise floor on 80 m**: target S2 or below in a quiet rural setting; S5 or below in suburban; S6 or below in urban.
- **Improvement from systematic cleanup**: 6–12 dB typical; up to 20 dB if you started with a very noisy environment.
- **Time investment**: 4–8 hours of methodical work for a comprehensive cleanup.

Don't expect S0 noise in a modern household with 50 LED bulbs and 30 chargers. Some baseline noise is the cost of living in 2026.

## A worked example

A new operator complaining of S7 noise on 40 m and 80 m. Workflow:

| Day | Action | Result |
|-----|--------|--------|
| 1 | Inside-vs-outside test | Inside source (drops to S2 with all breakers off) |
| 2 | Found bad breaker = "lights" | Removes 4 dB when off |
| 3 | Tested 12 LED bulbs | Two cheap ones contributed 3 dB each |
| 3 | Replaced 2 bulbs with Cree LEDs | 80 m noise S7 → S5 |
| 4 | Found "garage" breaker contributes 3 dB | The new EV charger |
| 4 | Added 5-turn FT-240-31 to EV cable | Recovered 4 dB; noise S5 → S3 |
| 5 | Found "kitchen" 60 Hz buzz | Doorbell transformer hum |
| 5 | Replaced doorbell transformer | Quieter; subtle improvement |
| Total | 6 hours of work | Noise reduced from S7 to S2-S3 ≈ 18 dB improvement |

## What good logging looks like

```
2026-04-01: Baseline 80m noise = S7
            40m noise = S6
            20m noise = S5

2026-04-02: Identified inside source (no power = S2 on all bands)
            Tested all breakers; "lights" = +4 dB, "garage" = +3 dB

2026-04-03: Replaced 2 cheap LED bulbs with Cree
            New 80m baseline = S5 (-2 from before)

2026-04-04: Added FT-240-31 5-turn choke to EV J1772 cable
            New 80m baseline = S3 (-2 more)

2026-04-05: Replaced doorbell transformer (was buzzing)
            New 80m baseline = S2-S3 (subjective improvement)

Cumulative: 80m noise S7 → S2-S3, ~18 dB improvement
Cost:       $40 in 2 LEDs, $8 in ferrites, $25 transformer = $73
Time:       6 hours over 5 days
```

This is a real example pattern. Most operators get 12–18 dB of improvement from a few hours of systematic work.

## See also

- §14-04 — ferrite selection
- §14-05 — isolation workflow (the higher-level approach)
- §15, §15 — specific source categories
