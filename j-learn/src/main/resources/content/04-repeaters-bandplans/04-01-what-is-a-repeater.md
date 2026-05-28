---
id: 04-01
title: What is a Repeater
chapter: 04
section: 01
level: simple
status: draft
---

# What is a Repeater

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A repeater is a radio that **listens on one frequency and transmits everything it hears on another frequency, simultaneously**. It's installed somewhere high — a hilltop, a tall building, a tower — and it lets two handheld radios that couldn't reach each other directly talk through it.

## Why repeaters exist

VHF and UHF signals mostly travel line-of-sight. Two handhelds at street level have a working range of maybe a mile or two — buildings, trees, and the curvature of the Earth get in the way. Put a 50-watt repeater 500 feet up on a tower and that same handheld can suddenly reach 30+ miles, and so can every other handheld in the area.

The repeater is just a relay. It doesn't know what it's relaying — it doesn't know who you are, doesn't care what you say. It hears RF on its input, transmits on its output, and stops transmitting a few seconds after the input goes silent.

## Input vs output

Every repeater has **two frequencies**:

- **Output frequency** — what the repeater transmits. This is what you tune your radio to receive.
- **Input frequency** — what the repeater listens on. This is what your radio transmits when you key up.

The two are separated by a fixed amount called the **offset**. On 2 m the offset is conventionally 600 kHz; on 70 cm it's 5 MHz. Other bands use other conventions (covered in §04-02).

Your radio handles this for you: when you tune to a repeater output frequency and tell the radio "this is a repeater, the offset is –600 kHz", the radio listens on the displayed frequency and transmits 600 kHz lower automatically. You never have to think about it during a QSO.

## Simplex vs duplex

- **Simplex** = both stations transmit and receive on the same frequency. They take turns. This is what direct radio-to-radio works as. Covered in §04-04.
- **Duplex** = transmit on one frequency, receive on another, simultaneously. The repeater operates in duplex.
- **Half-duplex** = your radio still takes turns (you don't transmit while listening), but it uses two different frequencies. This is what your handheld does when it's "in repeater mode."
- **Full-duplex** = transmit and receive at the same time, on two frequencies. The repeater itself does this. Cell phones do it. Most ham radios do not.

## What's inside a repeater

At a minimum:

1. A **receiver** tuned to the input frequency.
2. A **transmitter** tuned to the output frequency.
3. A **controller** — a small computer that decides when to transmit, what announcements to play (the periodic ID), what tones to require, and what to do during emergencies.
4. A **duplexer** — the box that lets the receiver and transmitter share one antenna without the transmitter desensing the receiver. Important enough to get its own section (§04-06).
5. **Antennas** — usually a single high-gain vertical at the top of a tower.
6. **Power** — usually mains AC with battery backup, sometimes solar.

Bigger repeater systems add **EchoLink / IRLP / AllStar** internet linking (§04-05), **voting receivers** for better coverage from low-power handhelds, and **multi-site linking** for wide-area coverage.

## Coverage

Repeater coverage depends on:

- **Antenna height** — the single biggest factor. Doubling the antenna height roughly doubles the radius.
- **Antenna gain** — a 6 dBi gain antenna roughly doubles the effective range vs a 0 dBi one.
- **Transmit power** — important but with diminishing returns. Going from 25 W to 100 W only adds about 2 dB of range.
- **Receiver sensitivity** — the repeater hearing your weak handheld is usually the limiting factor, not your radio hearing the repeater.
- **Local terrain** — a hill blocks line of sight even at modest distance.

A typical 50 W 2 m repeater on a 100-foot tower covers roughly a 30-mile radius for handheld users, much further (60+ miles) for mobiles with rooftop antennas.

## Etiquette

A repeater is shared infrastructure. The basics:

- **Listen first.** Make sure nothing's already in progress.
- **Identify** with your callsign at the start of a transmission, every 10 minutes during a long QSO, and at the end. (The FCC rules say "every 10 minutes and at the end"; most operators ID at the start as a courtesy too.)
- **Pause between transmissions.** Leave at least a second after the squelch tail before keying back. This lets emergency traffic break in, and it lets the controller reset.
- **Keep it clean.** Repeaters are public — kids' scanners, news media, and the FCC can all hear you.
- **Use simplex when you can.** If you and the other station are close enough to talk simplex, drop off the repeater (move to 146.520 — the national 2 m calling frequency) and free it up.

## What can break

Common reasons a repeater "doesn't work" for you:

| Symptom | Likely cause |
|---------|--------------|
| You key up but the repeater doesn't repeat your audio | Wrong tone / no tone (see §04-02) |
| Repeater repeats but nobody answers | Quiet repeater, or you're the only one on it |
| You hear the repeater but it doesn't hear you | Your TX power too low for the path |
| You sound distorted on the repeater | Mic gain set wrong, or you're holding the radio too close |
| Repeater suddenly goes off the air for a few seconds | Time-out timer — most repeaters drop after 3 minutes of continuous transmission |

## See also

- §04-02 — the tone / offset settings that make your radio actually work the repeater
- §04-04 — simplex frequencies for when a repeater isn't needed
- §04-05 — repeaters that link across the internet
- §04-06 — the duplexer that lets a repeater run on one antenna
