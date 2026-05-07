---
id: 02-04
title: Simplex Calling Frequencies
chapter: 02
section: 04
level: simple
status: draft
---

# Simplex Calling Frequencies

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

**Simplex** = direct, radio-to-radio, no repeater in the middle. You and the other station both transmit and receive on the same frequency, taking turns. It's the simplest way to use a VHF/UHF radio, and on a busy night it's often more pleasant than a crowded repeater.

## Why simplex matters

- **Privacy** — you and the other station are the only ones tuned to that exact frequency. No third party listening unless they happen to scan the right spot.
- **No infrastructure dependency** — no repeater means no controller failures, no power outages at the hilltop, no internet dropouts on the linked node.
- **Frees up repeaters** — every QSO done simplex is one less QSO clogging the local machine.
- **Skill builder** — without a 50 W repeater 500 feet up doing the heavy lifting, you learn what your gear can really do.
- **Emergency-ready** — when a hurricane takes out the repeater tower, simplex is what you have left.

## The national calling frequencies

These are the frequencies you tune to find people calling CQ when there's no specific repeater in mind:

| Band     | Calling frequency | Mode |
|----------|------------------:|------|
| 6 m      | 50.125 MHz | SSB |
| 6 m      | 52.525 MHz | FM |
| 2 m      | 144.200 MHz | SSB |
| 2 m      | 146.520 MHz | FM (the "five two") |
| 1.25 m   | 223.500 MHz | FM |
| 70 cm    | 432.100 MHz | SSB |
| 70 cm    | 446.000 MHz | FM |
| 33 cm    | 927.500 MHz | FM (regional) |
| 23 cm    | 1296.100 MHz | SSB |

The most-used by far is **146.520 MHz FM** — the "**five two**" of countless casual contacts. If you're just driving around with a 2 m radio, monitoring 146.520 is how you find other hams.

## How to call CQ on simplex

The polite procedure:

1. **Listen for at least 30 seconds.** If anything's already in progress, don't step on it.
2. **Ask "is this frequency in use?"** Wait 5 seconds. Some QSOs have long pauses; you might hear someone respond.
3. **Call**: `CQ CQ CQ this is [your callsign] [your callsign] CQ [your callsign] standing by`. Use phonetics for unusual call letters.
4. **Listen for replies.** If someone answers, **immediately move off the calling frequency** to a clear frequency nearby (e.g., 146.500, 146.540, 146.560 — leave 146.520 alone for the next caller).
5. **If no one answers** after a couple of tries, give up and listen on other simplex spots or call elsewhere.

## Working FM simplex around 146.520

Memorize the simplex sub-band:

| Frequency | Use |
|-----------|-----|
| 146.400–146.580 | Simplex |
| 146.520 | National FM calling — call here, then move |
| 146.540, 146.550, 146.560, 146.580 | Common QSY targets after a 146.520 call |
| 147.420–147.570 | Additional simplex |

The full simplex range is on 15 kHz steps. Stay on those — don't program random odd frequencies.

## Working SSB on 144.200

VHF SSB on 2 m is its own community — generally weak-signal operators with horizontal antennas (yagis) chasing tropospheric ducting and aurora. The procedure is similar to HF SSB:

- Call CQ on 144.200, **then QSY** to a nearby clear frequency.
- Use phonetics — signals are often weak.
- Most activity is in the evenings and during contests.

## Range expectations

Two stations, both running 50 W mobile, both with quarter-wave whips on the roof, in moderately rolling terrain:

- **2 m simplex**: 10–25 miles
- **70 cm simplex**: 5–15 miles (UHF is more terrain-sensitive)
- **6 m simplex**: 25–60 miles (the longest range of the trio in flat conditions)

Two handhelds in the same situations:

- **2 m**: 1–3 miles in town, 5–10 in open terrain
- **70 cm**: similar but slightly less

A handheld talking to a mobile or fixed station: somewhere in between.

## Getting more range out of simplex

- **Get higher.** A handheld at the top of a parking garage will reach 5x further than the same handheld at street level.
- **Use a better antenna.** Replacing the rubber duck on a handheld with even a mediocre roll-up J-pole picks up 3–6 dB.
- **Use 5 W, not 1 W.** That's 7 dB and often the difference between making the contact and not.
- **Try a directional antenna.** A 3-element 2 m yagi gives ~6 dB gain in the pointed direction; you can often double simplex range with one.
- **Try SSB instead of FM.** SSB pulls signals out of noise that FM squelches off entirely. Range-wise, SSB at 25 W is comparable to FM at 250 W on the same path.

## Simplex etiquette

- **Don't camp on calling frequencies.** Move after making contact.
- **Don't use simplex on a repeater frequency.** It's not illegal but it confuses everyone — your simplex contact will appear to be "the repeater" to anyone listening with their radio set up for that repeater pair.
- **Identify just like on a repeater** — every 10 minutes and at the end.
- **If you're having a long QSO, agree on the next frequency** rather than always returning to 146.520.

## When repeaters are off the air

Real-world example: a local repeater takes a lightning hit and is down for two weeks. The local club moves to 146.580 simplex for nightly nets. The takeaway: **know what frequency your local club uses for "no repeater" backup**, and have it programmed before you need it.

## See also

- §02-01 — repeaters, the not-simplex alternative
- §02-03 — full band plans showing where simplex is allowed
- §22 (EmComm chapter) — emergency operating practices that lean on simplex
