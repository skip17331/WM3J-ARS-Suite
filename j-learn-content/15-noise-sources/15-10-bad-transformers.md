---
id: 15-10
title: Bad Transformers
chapter: 15
section: 10
level: simple
status: published
---

# Bad Transformers

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Distribution transformers (the cylindrical or rectangular cans on poles, or the green pad-mount boxes in newer subdivisions) step down 7,200 V or higher distribution voltage to 240/120 V for residential use. When their internal core or windings degrade, they generate distinctive RFI — usually 60 Hz buzz with HF harmonics.

## How they fail

Several mechanisms produce RFI:

- **Saturated core**: the laminated steel core driven beyond its design flux density distorts the magnetic field, generating harmonics into the audio and HF range.
- **Loose laminations**: the core's steel sheets vibrate against each other at 60/120 Hz, producing audible buzz and electrical noise.
- **Cracked insulation**: internal arcing between windings or between winding and core.
- **Bad bushings**: the insulator that carries the high-voltage line into the transformer can crack and arc, similar to §15-09.
- **Aging oil**: the insulating oil inside an older transformer (PCB or mineral oil) breaks down chemically, increasing internal arcing.

## How it sounds

- Continuous **60 Hz buzz** with strong harmonics at 120, 180, 240, etc., extending up into HF.
- Often coexists with broadband hash from internal arcing.
- May be **persistent** (always present) rather than weather-related.
- A failing transformer often gets noisier over weeks or months as the underlying problem progresses.

The 60 Hz character is the giveaway — listen for the tonal hum overlaid on the broadband hash.

## How to find a bad transformer

Same direction-finding technique as for arcing insulators (§15-13, §15-14):

1. Walk with a portable AM radio along power line routes.
2. Note where noise peaks. Transformers are often at street corners, at the start of a service drop, or in pad-mount cabinets.
3. **Look at the transformer**:
   - Pole-mount: visible from the ground; check for oil leaks, deformation, or visible smoke.
   - Pad-mount: green box at street level; usually closed, harder to inspect.

If you can hear an audible 60 Hz hum from the transformer (audible to your ears, not just the radio), the loose-lamination failure mode is confirmed.

## Reporting

Same procedure as bad insulators (§15-15). Add to the documentation:

- Transformer location and type (pole-mount cylinder, pole-mount rectangular, pad-mount).
- Any visible signs of distress: oil leaks, deformation, audible hum, smoke.
- Whether the transformer serves your house or a different one.

## Repair vs replacement

Utilities generally don't repair transformers in the field. The bad unit gets swapped out for a new one. Cost to the utility is significant ($1k–$10k per transformer plus crew time), so they sometimes try to verify the diagnosis carefully before replacing.

If your initial complaint is about RFI and the utility tech is uncertain, they may:

- Schedule for monitoring (return when conditions are bad).
- Ask other operators in the area if they hear similar noise (corroboration).
- Bring more sophisticated test equipment (ultrasonic detector, partial-discharge analyzer).

Be patient. A transformer replacement takes longer than an insulator swap because of the cost.

## After replacement

Verify silence. If the noise is gone, document the resolution and thank the utility (a quick email or letter goes a long way for future cooperation).

If the noise is still present after replacement, the original diagnosis was wrong (it's actually an insulator on the same pole, or a different pole nearby). Re-do the direction-finding with the now-known information.

## See also

- §15-09 — arcing insulators (similar diagnostic approach)
- §15-12 — corona discharge
- §15-15 — utility documentation
