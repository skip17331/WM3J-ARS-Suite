---
id: 15-02
title: LED Lights
chapter: 15
section: 02
level: simple
status: published
---

# LED Lights

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

LED bulbs replaced incandescents almost universally over the last 15 years. They use less power and last longer — but most contain a small switching driver that emits HF noise. A house with 30+ LED bulbs adds measurably to the noise floor; a house with 30 cheap bulbs adds significantly.

## Why LEDs are noisy

A "white" LED needs about 3 volts to forward-bias. To run from 120 VAC, the bulb needs a power supply that converts AC to a regulated low-voltage DC. Cheap bulbs use a tiny switching driver, sometimes with no input filter at all. The same dynamics as wall-wart chargers (see §15-01) — the switching action generates HF harmonics that conduct out the bulb's threads back into the house wiring.

Worse: many LED drivers are designed to dim, which means they vary their switching frequency or duty cycle in response to a dimmer's chopped waveform. A dimmer at 50% creates more switching activity per cycle, often making noise much worse.

## Sound and pattern

- Constant hash, like a switching supply.
- Worse on lower bands (most LED drivers operate at 50–500 kHz; harmonics bleed into HF).
- Worse when dimmed.
- Often **goes away** when the lights are off — which makes diagnosis easy.

## Quick test

The easiest of all RFI tests:

1. Turn off all the lights in your house.
2. Note the change in S-meter.

If the noise drops several dB, LED bulbs are a major contributor. Now narrow down by turning lights back on a circuit at a time, then a fixture at a time, then a bulb at a time.

## Quality variations

The variation is enormous:

| Brand | Typical noise level |
|-------|---------------------|
| **Cree** (when available) | Very quiet |
| **Philips Soft White (proper LED)** | Quiet |
| **GE Lighting** | Quiet to mid |
| **Sylvania** | Quiet to mid |
| **Hardware-store house brand** | Variable; some are fine, many are noisy |
| **Cheapest Amazon multipack** | Often very noisy |

The price tag is a rough indicator. A $5 bulb is usually quieter than a $1 bulb because the higher cost includes proper input filtering. A $25 bulb (high-CRI commercial type) is almost always quiet.

## Fixes

### Replace the offending bulbs

The most reliable fix. Replace cheap bulbs with quality ones (Cree, GE Lighting, Philips Soft White). Cost: $4–10 per bulb. For a house with 30 bulbs, even spot-replacing the worst 5 can recover 6–10 dB.

### Use less light

Sometimes "10 LED bulbs in this fixture" is just too many. Replacing a fixture's complement with fewer, brighter bulbs can reduce both noise and ambient light to better levels.

### Use better dimmers

If you must dim, use dimmers and bulbs designed for each other:

- **Trailing-edge dimmers** with LED-rated bulbs are quieter than leading-edge dimmers.
- **Lutron Diva LED+** dimmers are widely regarded as quieter than the cheapest house-brand units.
- Match the dimmer wattage to the bulb load (don't drive a 5 W LED off a 250 W incandescent dimmer).

### Replace the fixture's transformer (12 V LED systems)

Some kitchen-undercabinet, landscape, and recessed lights use a separate 12 V transformer. Replacing a noisy magnetic transformer with a quiet electronic one (or vice versa, depending on which you have and which is the problem) can help.

### Live with it

Sometimes you accept the noise. A bedroom lamp that's only on at night when you're not operating, isn't worth replacing.

## Specific high-impact replacements

Some bulb types are notorious:

### Recessed (can light) LED retrofits

Cheap retrofit kits use bare-minimum drivers. Replace with quality alternatives or use older incandescent reflectors.

### Smart bulbs (Wi-Fi, Zigbee)

Add Wi-Fi or Zigbee radios to the LED driver. Often noisier than non-smart equivalents at HF. Quality varies widely; some Hue and LIFX bulbs are quiet.

### Christmas / decorative LED strings

Many strings have very poor input filtering. The seasonal use limits the impact, but a 100-light string can be loud during the season. Battery-operated versions are often quieter than line-powered.

### Aquarium / grow lights

LED aquarium and grow lights can be loud and run continuously. Quality units (Kessil, AI Hydra) are quiet; cheap units can be very noisy.

## Adding ferrite to LED bulbs

A snap-on ferrite at the base of a noisy LED bulb won't fit the typical fixture. The fix has to be at the panel/circuit level:

- Snap-on ferrite at the wall switch (on the load wires).
- Snap-on ferrite at the panel (on the breaker output).

Both reduce conducted emissions on that circuit. Effectiveness depends on the offending bulbs' impedance characteristics.

## Coordinating with the household

Lighting is shared infrastructure. The household has opinions about light color, brightness, and quality. Approach the problem with that in mind:

- Quality LED bulbs look as good as (or better than) cheap ones.
- Replacement is invisible to non-radio housemates.
- The "cleanup" is easy to sell as "the new bulbs are warmer / brighter / last longer."

## See also

- §15-01 — switching supplies (LEDs are essentially tiny SMPS)
- §14-02 — household sources overview
- §14-04 — ferrite selection
