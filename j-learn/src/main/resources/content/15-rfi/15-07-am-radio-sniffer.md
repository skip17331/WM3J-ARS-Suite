---
id: 15-07
title: AM Radio Sniffer
chapter: 15
section: 07
level: simple
status: draft
---

# AM Radio Sniffer

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A cheap portable AM radio is the single most useful RFI direction-finding tool. It works on virtually any RFI source, costs $20, fits in a pocket, and runs on AA batteries. If you do any RFI hunting at all, get one.

## Why AM works

AM radios:

- Have an internal **ferrite-rod loop antenna** that's directional. Rotating the radio changes the signal level dramatically.
- Cover frequencies (530–1700 kHz, plus shortwave on better units) where most household RFI sources are loud — switching supplies, plasma displays, LED dimmers, etc., all radiate strongly across the AM and HF spectrum.
- Have very simple AGC and audio characteristics — the noise sounds the same regardless of which device makes it, so direction-finding is purely about signal strength.

You can use a higher-end shortwave receiver if you have one, but the cheap AM portable is the workhorse.

## What to buy

In rough order of usefulness for RFI work:

| Radio | Approximate price | Notes |
|-------|------------------:|-------|
| **Tecsun PL-380** | $40 | Includes shortwave bands too; great display; runs on rechargeables |
| **Tecsun PL-330** or **PL-660** | $80–120 | More features; includes SSB so you can hear amateur signals too |
| **Sangean ATS-405** | $90 | Another quality AM/SW portable |
| **Any cheap AM-only portable** | $15–25 | Works fine for sniffing; the ferrite loop antenna is what matters |

Avoid: pocket radios with no external antenna jack and a tiny ferrite rod — they're less directional. Avoid: anything advertised as "24-band" or with novelty features but cheap construction.

## How to use it

The basic technique:

1. **Turn on the AM radio.** Tune to a frequency between stations — somewhere quiet on the dial.
2. **Listen for the noise.** Should sound like the same noise you hear on the ham radio at much higher frequencies.
3. **Walk around with the radio**, holding it horizontally. Note where the noise gets louder.
4. **As the source gets close**, the noise becomes very loud. You'll find yourself standing within a few feet of a wall outlet or a specific device.
5. **Rotate the radio** as you walk. The ferrite loop antenna is most sensitive perpendicular to its axis. Find the orientation that maximizes the signal — that orientation aligns with the source.

Within a few minutes you can usually pinpoint a noisy device to a specific outlet or piece of equipment.

## Inside vs outside

For inside-the-house sources:

- Walk through every room of the house with the radio on.
- Listen for level changes near outlets, appliances, fixtures.
- Hold the radio close (within a foot) to suspect devices to confirm.

For outside-the-house sources:

- Walk along the street with the radio.
- Note where noise peaks. Power lines often have peaks near specific poles (the bad insulator).
- Drive slowly with the radio on the dashboard for longer-distance hunting.

## A few specific tips

### Use AM, not FM

FM has built-in noise rejection and won't show RFI well. Always use AM mode.

### Tune off-station

Tune to a frequency where there's no station, so you're listening to noise floor only. Stations override RFI and confuse the test.

### Compare to a known source

If you're not sure if what you hear is RFI or just AM-band noise: turn off all the breakers in your house. The AM radio's noise floor should drop to a baseline. Whatever raises it above that baseline is your culprit.

### Higher frequencies are less directional

Shortwave bands on a PL-380 or similar are received via a different antenna (a whip), which is less directional than the ferrite loop. For sub-30-MHz sniffing, the AM band is the most directional option.

## Direction-finding by null

The most precise technique. The ferrite loop antenna in an AM radio has two **null points** perpendicular to its axis. Rotate the radio:

1. Find the position where the signal is at minimum (a sharp dip).
2. The source is in the direction perpendicular to the radio's long axis at that point.

The null is much sharper than the peak, so you get better directional accuracy.

For long-distance source hunting, take two null bearings from different locations and triangulate.

## When to use a portable HF radio instead

A portable HF receiver (Tecsun PL-660, Eton Elite, Icom IC-705 if you have one) extends the same technique up into the actual frequencies you operate on.

Advantages:
- Direct comparison: the noise sounds exactly like what your station hears.
- Same propagation: noise that affects 14 MHz at your station also affects 14 MHz at your hunting location.

Disadvantages:
- Larger, more expensive.
- Whip antenna is less directional than a loop.

For really tough cases, a portable HF rig with a small loop antenna gives you both: the directionality of the loop and the frequency match of HF.

## A worked hunt

Suppose you have a constant hash that's worse on 80 m than 40 m. Your inside-the-house test confirms a single device is responsible. Hunt:

1. Turn on AM radio. Tune to 1100 kHz (no station). Hear the hash clearly.
2. Walk through the kitchen — hash gets louder. Loudest near the refrigerator.
3. Hold the radio at the back of the fridge. Very loud.
4. Open the fridge door. Hash continues — not the lighting.
5. Listen for a few minutes — hash is constant, not synced to the compressor cycle.
6. Conclusion: the **inverter compressor's variable-speed driver** is the source.
7. Solution: add a snap-on ferrite to the fridge's power cord.

The whole hunt takes 10 minutes.

## When the AM radio doesn't help

A few cases:

- **Pure RF source** that's frequency-specific (no broad spectrum). E.g., a single cleaning oscillator. Use a SDR with a waterfall (§15-08) to see exactly which frequencies are affected.
- **Very weak source** that the AM radio can't detect. Use a more sensitive HF receiver instead.
- **Source on a circuit you can't enter** (locked utility room, neighbor's house). Direction-find from outside.

For 95% of household RFI cases, the AM radio finds it.

> ⚙️ **Advanced —** The ferrite loop antenna in a portable AM radio has a directional response approximated by `cos(θ)` for the broadside angle θ. That cosine response gives the deep null at θ = 90° (perpendicular to the loop axis) and the peak at θ = 0° (along the loop axis). The depth of the null can exceed 30 dB in a well-designed antenna; the peak-to-null ratio is what gives the directional accuracy. For more precise direction-finding, professional equipment uses a narrow Adcock array or a phased loop pair, but a single loop in a $20 portable is enough for typical RFI hunting.

## See also

- §15-05 — isolation workflow (where the AM radio fits into the larger flow)
- §15-08 — SDR waterfall for visual diagnosis
- §17 — power-line noise (different hunting techniques apply)
