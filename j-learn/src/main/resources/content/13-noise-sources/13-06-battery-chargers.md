---
id: 13-06
title: Battery Chargers
chapter: 13
section: 06
level: simple
status: draft
---

# Battery Chargers

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advenced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Modern battery chargers — for power tools, e-bikes, vehicles, drones, robotic vacuums — are essentially dedicated switching power supplies optimized for cell-chemistry charging. The same RFI mechanisms apply (§13-01), but a few specifics make battery chargers their own category.

## Why they're sometimes worse than wall warts

A battery charger:

- Often has higher peak power than a phone charger (Brushless tool batteries: 100+ W. EV chargers: 7–11 kW).
- Operates intermittently — RFI spikes when charging, returns to baseline when finished.
- Has variable behavior across the charge cycle (constant current → constant voltage → trickle).
- May have cooling fans or thermal regulation that adds modulation.

The combination of high power and variable behavior makes battery chargers among the most distinctive RFI sources.

## What they sound like

- Hash that **starts when the battery is plugged in** and stops when charging completes.
- Often varies in level over the charge cycle (loud during constant current, quieter at the end).
- Some chargers add **modulated tones** — a 1 Hz beep, a 100 Hz hum, etc., from internal status indication.

## Charger types and their typical behavior

### Power tool chargers (DeWalt, Milwaukee, Makita, Ryobi, etc.)

- Typically 18 V battery, ~150 W charger.
- Switching at 50–250 kHz.
- RFI level: variable. Brand-name chargers tend to be moderate; off-brand can be loud.
- **Fix**: snap-on ferrite at the battery output cable.

### E-bike chargers

- 36 V or 48 V batteries, 200–500 W.
- Often noisier than power tool chargers because of higher power.
- **Fix**: snap-on ferrite at output; for severe cases, replace with a higher-end charger from a different brand.

### EV chargers (Level 1 and Level 2)

- 1.4–11 kW. Significantly higher power than other categories.
- The Level 2 chargers (240 V, 16–50 A) are essentially small inverters.
- RFI level: highly variable by brand. Tesla Wall Connector reportedly low; cheap chargers from EV importers can be very loud.
- **Fix**: ferrite chokes on the J1772 cable (5–8 turns through FT-240-31 toroids); add common-mode chokes on the AC mains feed.

For homeowners who use Level 2 EV charging and operate amateur radio, the EV charger is often a major source. Coordinate charge times to be away from operating times when possible.

### Lithium-ion battery balancers

Multi-cell lithium battery management systems include charge balancers that switch individual cells in and out at high frequency. RC (radio control) and drone batteries with internal BMS chips are particularly prone to this.

### Old battery chargers (lead-acid)

Old-school transformer-based chargers with simple full-wave rectification produce primarily 60/120 Hz hum, not HF hash. Less of a problem on the upper bands.

## Diagnosing

The pattern is distinctive:

1. Note when the noise starts. Usually when you plug in a device to charge.
2. Note when the noise stops. Usually when the charger finishes (LED changes color, fan stops, etc.).
3. Note the noise level varying with charge state.

This pattern — clearly tied to charging events — almost always points at a charger.

## Specific fixes

### Ferrite at the charge cable

Most universal. Snap-on ferrite at the cable's load end (the device that's being charged), with 3–5 turns through. Mix 31 or 43.

For larger chargers (EV, e-bike), use larger toroids (FT-240-31 with 5 turns) and check that they don't saturate at the high current.

### Replace the charger

Quality chargers are quieter. For tools, an OEM charger from the tool brand is usually better filtered than a third-party clone.

### Use a separate circuit

A charger plugged into a different breaker than your radio's circuit reduces the conducted-emission path. Especially helpful for EV chargers, which are typically on dedicated 240 V circuits anyway.

### Avoid charging during operating

For non-time-critical charging (overnight, when away), schedule charging to avoid your operating times.

### Battery isolation

For some applications, the noise is from the charger conducting back through the battery and into the device's own electronics during charging. Disconnect the device from the charger when not actively transferring power.

## A specific case: Tesla / EV at home

Tesla Wall Connectors are reportedly among the quietest EV chargers (Tesla's engineering includes RFI-aware design). Other brands (Wallbox, ChargePoint, Lectron) vary.

If you're shopping for an EV charger and you operate amateur radio:

- **Tesla** Wall Connector — generally quiet.
- **Enphase** EV chargers — quiet.
- **Wallbox** Pulsar Plus — moderate.
- **No-name imports** — variable; check reviews for RFI.

The cost difference between a $700 Tesla Wall Connector and a $300 generic imported one is sometimes the cost of your sanity at the radio.

## Charging at the shack

Some operators charge devices at the operating position. This is a bad idea unless you've verified the charger is silent — even a small phone charger sitting on the desk a foot from the radio is going to be much louder than the same charger in another room.

Move chargers as far from the antenna and the radio as practical.

## See also

- §13-01 — switching power supplies (the underlying mechanism)
- §12-04 — ferrite selection
- §12-06 — step-by-step elimination
