---
id: 26-07
title: Linear vs Switching Power Supplies
chapter: 26
section: 07
level: mixed
status: published
---

# Linear vs Switching Power Supplies

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## The fundamental trade

Every ham-station 13.8 V supply chooses between two architectures:

| | Linear | Switching |
|---|--------|-----------|
| Topology | 60 Hz transformer + rectifier + linear pass regulator | High-frequency switching regulator (20 kHz – 1 MHz) |
| Weight (25 A supply) | **25–35 lb** | **5–8 lb** |
| Efficiency | **40–60 %** | **85–94 %** |
| Heat dissipation at idle | **50–100 W** | **5–10 W** |
| RF noise | **Essentially none** | **Significant (must be designed/built well)** |
| Ripple | 100/120 Hz, easy to filter | 20 kHz – 1 MHz, harder to filter |
| Cost (25 A, name-brand) | $300–$450 | $200–$300 |
| Sound | Silent (except 60 Hz transformer hum) | Often has a small fan |
| Failure mode | Pass transistor short → output rises → blow fuse | Switching FET fault → wide range of outcomes |

There is no universal winner. The choice depends on the station's noise floor budget, the operator's budget, and where the supply lives relative to the receiver.

## Why linear supplies are RF-quiet

A linear supply's only RF-emitting component is the 60 Hz transformer (which radiates a 60 Hz magnetic field, irrelevant to HF). The rectifier and filter caps produce 120 Hz ripple, which the linear pass regulator (typically a 2N3055 or LM78xx-class device, sometimes a beefier MJ15003) cleans up to < 5 mV. The output is essentially DC with no RF content.

A linear supply sitting under the radio adds **zero** noise to the receiver's spectrum. That's why every serious DXer's bench has an Astron RS-35M, Daiwa PS-300, or similar linear — when you're trying to pull a –135 dBm signal out of the noise, the supply must contribute nothing.

The cost is weight (~30 lb for a 25 A supply, because the 60 Hz transformer is iron and copper), efficiency (the pass regulator dissipates the difference between unregulated DC and 13.8 V as heat), and bulk (you cannot fit a linear 25 A supply in a small Pelican case for portable ops).

## Why switching supplies are noisy

A switching supply chops DC at 20 kHz to 1 MHz, transforms it through a tiny high-frequency transformer, rectifies the chopped output, and regulates by adjusting the switching duty cycle. The chopping waveform is fundamentally a square wave, rich in harmonics.

A poorly-designed switching supply puts **noise at every multiple of the switching frequency from DC to >100 MHz**. A typical "cheap eBay switcher" running at 65 kHz radiates spurs you can find on every HF band, sometimes as high as S9. Even when the supply is well-shielded, common-mode current flows on its DC output leads back to the radio, where it couples into the receiver front end.

The frequencies you'll find these spurs:

| Switcher type | Fundamental | Common spur locations |
|---------------|-------------|------------------------|
| Cheap wall-wart | 50–80 kHz | Every 50–80 kHz across HF |
| MFJ-4230MV | 100 kHz | 100 kHz multiples |
| Astron SS-30M | 65–100 kHz adjustable | Shifts with knob; useful |
| Samlex SEC-1235M | 50 kHz | 50 kHz multiples |
| Computer ATX PSU | 60–200 kHz (variable) | Spread spectrum — worse, not better |

**Spread-spectrum** switchers (used in modern ATX computer supplies) deliberately dither their switching frequency to spread emissions across a wider band — better for FCC compliance but **worse for HF receivers**, because instead of one narrow spur you get a broad noise floor lift.

## RF-quiet switching supplies

The "RF-quiet" label is a designed feature, not an inherent property. The key elements:

1. **Shielded transformer with toroidal magnetics** — keeps the switching field contained.
2. **Common-mode chokes on both input (AC) and output (DC) leads** — typically ferrite mix #43 or #31.
3. **EMI input filter** — Y-caps and X-caps at the mains entry.
4. **Variable switching frequency** so the user can shift spurs out of their operating band.
5. **Linear post-regulator** — some "linear-finish" switchers add a 1 V linear stage after the main switcher for the last bit of ripple suppression.

Two RF-quiet switchers that have a good reputation in amateur use:

- **Astron SS-30M / SS-25M** — 25–30 A, switching, with selectable switching frequency. The user can tune the spurs out of the band they care about. Real-world: still some noise, but manageable. ~$220.
- **MFJ-4230MVP / 4230DMP** — 30 A switching, with built-in metering. Noisier than the Astron but cheaper. ~$180.
- **Powerwerx SS-30DV** — Similar class to the Astron; many positive reports.

Avoid: any unbranded eBay switcher, RIGOL/Mastech bench-supply switchers used for ham TX (designed for lab work, not RF-quiet), and ATX computer PSUs (spread-spectrum is bad for HF).

## Symptoms of a noisy supply

If you suspect your supply is the noise source:

1. **The "unplug test."** Power the radio from a 12 V battery. Listen for ~30 seconds, noting the noise floor. Power back from the supply. If the noise floor jumps 5+ dB, the supply is contributing.
2. **The "S-meter at no signal" test.** With antenna disconnected, look at the S-meter. It should bottom out (S0–S1). If it reads S3+ with the antenna off and the supply running, you have either supply noise or chassis-radiated noise.
3. **The "tune across the band" test.** Slow scan across 20 m. Note any signals at evenly-spaced kHz intervals (e.g., every 65 kHz or every 100 kHz). Those are supply harmonics.
4. **The "wave a ferrite over the leads" test.** Hold a clip-on ferrite (#43 mix, FT-240 or split bead) over the DC output leads. If the noise drops, you have common-mode current on the supply leads — add a permanent choke.

## Mitigating switching-supply noise

If you have a noisy switcher and don't want to replace it:

| Mitigation | Effect | Cost |
|------------|--------|------|
| Wind 10 turns of DC leads through an FT240-43 | 10–20 dB common-mode reduction | $10 |
| Add a 1000 µF cap across the supply terminals at the radio end | Reduces ripple by a few dB | $2 |
| Use larger-gauge DC leads (#10 or #8) | Reduces voltage sag and parasitic L | $15 |
| Re-route the supply far from the radio and antenna feed | 6–10 dB radiated noise reduction | $0 |
| Add a snap-on ferrite (#43) on the AC mains lead | Reduces conducted noise back into the house wiring | $5 |
| Replace with a linear supply | 100 % cure for switcher noise | $300+ |

A combination of #1 (output choke) and #5 (AC mains choke) on a $180 MFJ switcher usually gets you within 3 dB of a linear supply, at $20 of ferrite.

> **Advanced —** Some operators use **two supplies**: a small linear for the radio's receiver-side circuitry (a few amps) and a switcher for the high-current TX-amp side. This isolates the noise from the sensitive part. Hambrew designs are rare; KK7B's *Experimental Methods in RF Design* shows a few. For 99 % of stations, a single supply is fine.

## Battery operation — the cleanest of all

A 12 V lead-acid or LiFePO4 battery is the **quietest** "supply" you can use. No mains harmonics, no switching spurs, no transformer hum. Mobile and field stations often run from a single deep-cycle battery and a charger.

For QRP portable work, a LiFePO4 pack (12 V nominal, 3–8 Ah) weighs less than 2 lb and runs an FT-818 / IC-705 for a full day. The Bioenno BLF-1212A and the Talentcell YB1203000 are popular choices.

When the battery is charging from a switcher, the noise returns. Disconnect the charger when operating.

## Construction option — building a linear

A 13.8 V, 20 A linear supply is a classic homebrew project. Parts list:

- **Transformer**: 18 V CT secondary, 25 A. Antek AS-2218 ($90) or surplus.
- **Bridge rectifier**: 35 A, 100 V. MB3510 ($5).
- **Filter cap**: 22000 µF, 35 V. Cornell-Dubilier ($25).
- **Pass transistor**: 2N3055 ×4 in parallel, or MJ15003 ×2. ($10–20).
- **Driver**: 2N3055 single, BD139.
- **Regulator IC**: LM723 (the classic reference).
- **Heatsink**: 0.5 °C/W, large finned aluminum. ($30).
- **Bleeder resistor**: 1 kΩ 5 W across the filter cap. **Mandatory for safety.** (See §26-11.)
- **Fuse**: 25 A on the secondary side; 5 A on the primary.

Total parts cost: ~$200. The result: 25 lb of supply, dead-quiet on RF, indefinitely repairable, and a real homebrew accomplishment.

## Quick recommendation

| Station type | Recommendation |
|--------------|----------------|
| Home HF station, noise-sensitive | **Linear**: Astron RS-35M (~$340) |
| Home HF station, mid-budget | **RF-quiet switcher**: Astron SS-30M (~$220) |
| Field / portable | **LiFePO4 battery** + small charger |
| Mobile install | **Direct from vehicle 12 V** — usually quiet enough |
| QRP-only | **AA / AAA NiMH pack** or LiFePO4 small |

## See also

- [§26-08 — Enclosures & Shielding](26-08-enclosures-shielding.md) — containing supply emissions
- [§26-06 — Ferrite Mix Selection](26-06-ferrite-mix-selection.md) — chokes for DC leads
- [§14 — RFI](../14-rfi/) — tracking RFI sources including supplies
- [§15 — Noise Sources](../15-noise-sources/) — locating ambient noise
- [§25 — Test Equipment](../25-test-equipment/) — SDR for noise spur identification
