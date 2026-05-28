---
id: 13-06
title: Power Supply
chapter: 13
section: 06
level: simple
status: draft
---

# Power Supply

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A bad or undersized power supply is the cause of more "the radio's broken" complaints than the radios themselves. Symptoms include power output dropping under heavy duty cycle, audio distortion, hum on the carrier, the radio resetting mid-transmission, or display brownouts. Most of these resolve when you address the supply.

## What HF radios need

Typical 100 W HF transceivers want:

- **13.8 V DC** ± 15% under all operating conditions
- **22–25 A** peak current draw at full TX
- **Quiet DC** — no AC ripple in audio band, low RF noise

Some lower-power QRP radios are happier on 12.0 V; some can handle 14.5 V. Consult the manual.

## Common power supply problems

### Voltage drops under load

The supply puts out 13.8 V at idle (a few hundred mA), but sags to 11.5 V when the radio TXes. The radio's PA can't develop full power and may fold back protectively.

Symptoms: full power on receive (irrelevant — it's quiet then), reduced power on TX, sometimes intermittent operation.

Causes:
- Supply rated too low (15 A supply with a 25 A radio).
- Supply old, capacitors degraded.
- Cable from supply to radio too thin or too long, dropping voltage in the cable resistance.
- Loose connection at one of the terminals.

Fix:
- Use a properly-sized supply. 25 A continuous duty for a 100 W HF radio.
- Use thick cable: at minimum 10 AWG for runs under 6 ft. Thicker for longer runs.
- Crimp connections firmly; don't trust just twisted wire under a screw.

### AC ripple in the audio

The supply produces clean DC at idle, but heavy load creates AC ripple at 60 Hz (and harmonics). This ripple modulates the audio chain, producing audible hum on TX.

Symptoms: 60 Hz buzz on transmitted SSB; audible hum in CW carrier; varies with mic gain.

Causes:
- Failing electrolytic capacitors in the supply.
- Cheap supply with inadequate filtering.

Fix:
- Add a large electrolytic capacitor (10,000 µF, 25 V or higher) across the supply output. This is a band-aid; a proper supply has this built in.
- Replace the supply with a quality unit (Astron, Powerwerx, Samlex). Linear supplies are quietest; switching supplies (modern, smaller, cheaper) need careful design to be quiet.

### Switching noise on RF

A switching supply that wasn't designed with HF amateur use in mind injects harmonics into the receiver, raising the noise floor and creating birdies (false signals) at specific frequencies.

Symptoms: unusually high noise floor, especially on lower bands; signals at exact frequencies that don't go away.

Causes:
- Cheap switching supply with inadequate RF filtering.
- Switching supply too close to the radio (inductive coupling).

Fix:
- Use a supply with documented low RFI (Astron SS-30M, Samlex SEC-1235M, MFJ-4225). Many of these include extra RF filtering on the output.
- Add common-mode chokes on the DC cable.
- Move the supply 6+ feet from the radio.
- For HF, consider a linear supply — Astron RS-35M is the classic, slightly heavier and more expensive but RF-quiet.

### Radio resets mid-transmission

The supply's overload protection kicks in mid-TX, dropping voltage to zero briefly. The radio resets.

Causes:
- Supply rating exceeded by transient peak current.
- Supply has poor transient response — can't supply a quick high-current pulse.

Fix:
- Larger supply.
- Add capacitance at the radio end of the DC cable (10,000+ µF) to handle transients.

### Battery operation problems

For battery-powered stations:

- **Voltage starts dropping during long TX sessions.** Need bigger battery or higher capacity.
- **Internal resistance of an aging battery rises**, causing brown-outs at high current draw. Replace the battery (lead-acid is typically 3–5 year service life).
- **Wrong battery chemistry** — Lithium iron phosphate (LiFePO4) at 12.8 V is fine for HF radios; lithium-ion at 14.4 V can be too high; AGM lead-acid at 12.6 V is fine but heavy.

## Diagnosing a supply problem

The minimum tools:

- **A multimeter on DC volts** — measure right at the radio's DC input terminals during TX. Voltage should not sag more than 5% from idle to full power.
- **An oscilloscope on AC volts** — connect across the supply output, switch the scope to AC coupling. Ripple should be under 50 mV peak-to-peak. Anything above 200 mV is degraded.

A test:

1. Note the voltage at the radio's DC input with the radio in receive (should be 13.6–13.8 V).
2. Key down a steady carrier at 100 W into a dummy load.
3. Note the voltage now (should still be 13.0+ V).
4. Hold for 30 seconds.
5. Note the voltage at the end (should still be 13.0+ V if the supply is sized correctly).

If voltage dropped below 12.5 V at any point, the supply or the cable is undersized.

## Recommended supplies for HF stations

Reasonable choices in 2025–2026:

| Supply | Type | Rating | Notes |
|--------|------|--------|-------|
| Astron RS-35M | Linear | 25 A continuous, 35 A peak | Classic; bombproof; RF-quiet; heavy |
| Astron SS-30M | Switching | 25 A continuous | Lighter, smaller, RF-filtered |
| Powerwerx SS-30DV | Switching | 30 A continuous | Adjustable voltage; common in mobile installs |
| Samlex SEC-1235M | Switching | 23 A continuous | Quiet, popular for QRP and mid-power |
| MFJ-4230MV | Switching | 30 A continuous | Adjustable voltage; affordable |

Avoid:
- Computer ATX supplies as primary radio power (cheap, but inconsistent voltage and noisy under load).
- Cheap "13.8 V" wall-wart supplies (rated for 5 A or less; can't handle TX spikes).

## Cable sizing — a lookup table

For a 13.8 V supply feeding a radio drawing 25 A peak, with a target voltage drop of < 0.5 V (about 4%):

| Cable length | Wire size needed |
|--------------|------------------|
| 3 ft | 12 AWG |
| 6 ft | 10 AWG |
| 10 ft | 8 AWG |
| 15 ft | 6 AWG |
| 25 ft | 4 AWG |

Round up if in doubt. Thicker is always safer.

## Connectors

For DC connections at 25+ A:

- **Anderson Powerpoles** are the modern standard. Color-coded, polarized, easy to make up.
- **Screw terminals** at the supply itself. Tighten properly.
- **Soldered ring terminals** are reliable but inflexible; OK for permanent installs.
- **Cigar-lighter plugs** are the worst. They corrode, lose contact, and overheat. Don't use them for ham gear.

## See also

- §13-01 — no transmit (often a power supply issue)
- §13-03 — distorted audio (60 Hz hum from supply ripple)
- §15-01 — switching power supply noise (related to RFI)
