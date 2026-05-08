---
id: 13-01
title: Switching Power Supplies
chapter: 13
section: 01
level: simple
status: draft
---

# Switching Power Supplies

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The most common single source of HF noise in modern households. Every wall wart, phone charger, laptop brick, USB charger, LED driver, and most appliance controllers contain one. They're cheap, light, and efficient — and many are noisy on the air.

## What it is

A switching power supply (SMPS) takes AC mains and converts it to a regulated DC output by **chopping the input at high frequency** (50 kHz to several MHz), then filtering the result. The high-frequency chopping is what makes them small and efficient — but also what makes them noisy.

Inside a typical SMPS:

```
AC mains ──→ rectifier ──→ DC bulk cap ──→ MOSFET switching at 50–500 kHz
                                                ↓
                                            transformer
                                                ↓
                                            secondary rectifier ──→ DC out
```

The switching MOSFET turns on and off at the switching frequency. This generates square-wave currents in the transformer primary, with **harmonic content extending hundreds of MHz**. Without proper filtering, those harmonics conduct back through the AC mains (where your antenna might pick them up via the house wiring) and radiate from the supply itself and its output cables.

## What it sounds like

- Continuous broadband hash, no obvious rhythm.
- Loudest on bands closest to the switching frequency's harmonics.
- Doesn't change with audio output level (since the SMPS is doing its thing regardless of what the device's actual function is).
- Often reduces if you turn the device off (proves the SMPS is the source).

## Visual signature on a waterfall

Broad horizontal stripes at intervals corresponding to the switching frequency. A 100 kHz switcher produces stripes every 100 kHz. A 250 kHz switcher produces stripes every 250 kHz. The stripes get fainter at higher frequencies (the harmonics weaken) but extend well past 30 MHz on a poor supply.

## Quality variations

Not all SMPS are noisy. The variation is huge:

| Quality tier | Noise level | Typical examples |
|--------------|-------------|------------------|
| **Top tier** | Negligible RFI | Apple charger, MacBook brick, Anker premium, OEM laptop bricks from major brands |
| **Mid tier** | Mild RFI on closest bands | Most generic-brand bricks |
| **Bottom tier** | Heavy RFI across HF | $5 chargers from random sellers, no-name USB hubs, very cheap LED drivers |

Quality correlates roughly with price and brand reputation. A $5 USB charger is almost always noisier than a $25 one of the same wattage. The difference is in the input filter, the snubber on the switching MOSFET, the shielding of the transformer.

## How to test

Before working on the chargers themselves:

1. Note your station noise floor on a quiet 80 m or 40 m frequency.
2. **Unplug each charger** in turn. Note any device that produces a > 2 dB drop when removed.
3. Compile a list of offenders. Replace or filter the worst.

A typical modern household has 10–30 chargers plugged in at any time. Many will be silent (or nearly so); a few will be loud. Replacing or filtering 2–3 of the worst can reduce noise by 6–10 dB.

## How to fix

In rough order of difficulty and effectiveness:

### Replace with a quality unit

For phone and USB chargers, the easiest move. Buy:

- **Anker** — Powerport series, USB-C PD chargers; consistently quiet.
- **Apple** — well-engineered for both noise and safety.
- **Aukey, RAVPower** — generally good.
- **OEM laptop bricks** from the laptop manufacturer.

Avoid:
- $5–10 chargers from no-name sellers on Amazon.
- Old chargers that came free with budget devices.

Cost: $20–40 per charger. Often the right answer.

### Add ferrite to the output cable

Snap-on ferrite at the device end of the cable, with 3–5 turns. Mix 31 or 43 ferrite core. This breaks the common-mode current path on the cable, reducing radiation.

Cost: $1 per snap-on. Cheap and quick.

### Add ferrite to the input (mains side)

If the SMPS itself is radiating directly (not just via its output cable), you may need to add filtering on the mains side. Some options:

- Snap-on ferrite on the AC plug end of the SMPS cord.
- Plug-in mains filter (Tripp Lite / APC line conditioner).
- Wholesale mains filter at the panel (this affects everything in the house).

### Replace the entire device

If the device's whole electronics are noisy, not just its power supply, replacement is the move. This is common with cheap LED bulbs (where the driver IS the device) and cheap USB hubs.

### Power the device from a battery

For devices that don't need to be on continuously — phone chargers when the phone is fully charged, accessories used briefly — just unplug them when not in use. A surprisingly effective fix.

## Special cases

### Cheap "fast" chargers

USB-PD and Quick Charge 3.0+ chargers run at higher switching frequencies (sometimes 1+ MHz) and are often less filtered than older chargers. Some are quiet; others spit hash across all of HF.

If you have a fast charger and an HF noise problem, test that charger specifically. Replacement may help.

### LED drivers in fixtures

A cheap LED replacement bulb has the SMPS *inside the bulb*. Filtering it externally is hard. The fix is to replace the bulb with a quality one (Cree, Philips, GE Lighting). See §13-02.

### Variable-speed drives

Industrial gear (HVAC, well pumps, some commercial freezers) uses variable-frequency drives that are essentially big SMPS. These can produce massive HF noise, often spanning the entire spectrum.

Filtering these is difficult and expensive (industrial line filters, often $200+). For amateur operators, the practical option is operational — coordinate operating times with the equipment cycle, or improve receive antenna nulls.

## What you can't fix

- **A neighbor's noisy charger.** They're under no obligation to fix it. Approach diplomatically; many will swap out a $5 cable for a $20 one if it stops their TV from buzzing too.
- **Powerline noise from outside your property.** That's §13-08 territory; it's the utility's problem.
- **Pure broadband sources (plasma TVs, solar inverters).** Filtering helps somewhat; replacement is often the only real fix.

## A typical cleanup result

Real-world report from a US suburban household:

- Baseline: S6 noise on 80 m
- Identified 4 noisy USB chargers (cheap Amazon units)
- Replaced with quality Anker units
- Result: S4 noise on 80 m (~12 dB improvement)
- Total cost: $80 in new chargers
- Time: 1 evening

This is a typical pattern. Switching supplies are usually the biggest single contributor and the easiest to fix.

> ⚙️ **Advanced —** EMC (Electromagnetic Compatibility) regulations require that switching supplies meet conducted-emission limits at the AC mains terminals. The CISPR 22 / FCC Part 15 limits are roughly 60 dBµV from 150 kHz to 500 kHz, declining to 50 dBµV at higher frequencies. Many cheap supplies don't actually meet these limits in practice — the certification is paid for by the importer who tests one sample, and the production units may differ. Knockoff and counterfeit chargers (especially USB-C PD) frequently fail safety AND EMC tests; this is one good reason to avoid the cheapest options.

## See also

- §12-04 — ferrite selection
- §12-06 — step-by-step elimination
- §13-02 — LED lights (which are essentially small SMPS)
