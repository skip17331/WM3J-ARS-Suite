---
id: 13-03
title: Solar Inverters
chapter: 13
section: 03
level: simple
status: draft
---

# Solar Inverters

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Roof-mounted photovoltaic systems use **inverters** to convert the panel's DC output into AC for your house. Most consumer inverters are noisy on HF — sometimes catastrophically so. If you're considering solar and you operate amateur radio, this section is critical reading. If you already have solar and a noise problem, the fixes are limited but real.

## What's happening

A solar inverter takes 30–600 V DC from the panels and switches it through MOSFETs at high frequency to produce 60 Hz AC. The switching is at 10–50 kHz typically, with harmonics extending well into HF.

Three places noise comes from:

1. **The inverter itself** — radiates from its enclosure if shielding is inadequate.
2. **The DC wiring from panels to inverter** — long runs of unshielded DC cable carry the same switching hash as a giant antenna.
3. **The AC wiring back into the house** — hash conducts on the house mains.

Bigger inverters and longer wire runs both make the problem worse.

## What it sounds like

- Constant broadband hash, daytime only (no sun = no inverter activity = quiet).
- Slowly varies as cloud cover changes (peak load varies; switching frequency may modulate).
- Loudest on the lowest bands (160 m, 80 m, 40 m); usually less on 20 m and up.
- Disappears when the inverter is shut off (verify by tripping the AC disconnect).

A characteristic giveaway: the noise floor on lower bands rises a clear amount at sunrise, peaks around noon, then drops at sunset.

## Inverter brand variation

The variation in noise emission between brands is large:

| Brand | Typical noise level | Notes |
|-------|---------------------|-------|
| **SMA Sunny Boy** | Mid-low | German engineering; better-than-average filtering; popular among hams |
| **Enphase microinverters** | Low | Per-panel; small individual emitters; mostly quiet |
| **Solaredge with HD-Wave** | Mid | Mid-range; some installations quiet, others not |
| **Generac PWRcell** | Variable | Reports vary by installation |
| **Cheap Chinese imports** | High | Often heavily noisy; minimal filtering |

In broad terms: **microinverters (Enphase) are the quietest option** because each one is small and the low-frequency switching doesn't accumulate; **string inverters from established brands** are mid-range; **cheap string inverters** are usually the worst.

This matters at purchase time. Once installed, you can't easily change the inverter type.

## When buying solar

If amateur radio is part of your life:

- **Insist on Enphase or SMA Sunny Boy** in the contract. Pay the small premium.
- **Specify EMC compliance with FCC Part 15 Class B** (the residential limits, not the looser Class A industrial limits).
- **Request the inverter be installed away from the antenna** — opposite side of the house, in a shielded location if possible.
- **Use the shortest possible DC wire runs** — at most 30 ft if avoidable.
- **Insist on conduit for DC and AC runs** — the conduit acts as a partial shield.
- **Get the installer's commitment** to fix RFI issues if they arise. Get this in writing.

Pre-installation negotiation is much easier than post-installation troubleshooting.

## When you already have a noisy system

After installation, your options narrow:

### Common-mode chokes

Add chokes on:

- **AC output** of the inverter (where it ties into the house panel).
- **DC input** of the inverter (the lines from the panels).

Use heavy ferrite cores rated for the current. For a 5 kW residential inverter, you need cores that can pass 25 A AC and 50+ A DC without saturating. Mix 31 or 75 with multiple turns through stacked cores. This is industrial-grade work; budget $200–500 in parts and possibly an electrician's labor.

### Replace with quieter unit

If your inverter is one of the noisier types, replacing with an Enphase microinverter system (or quieter SMA Sunny Boy) can help. Cost: $2k–$5k for typical residential. Often the only real fix.

### Disable solar during operating

Some inverters can be remotely controlled. You can shut them off when you're operating critical contests or DXing. Wasteful but works.

### Change operating habits

Operate at night or early morning when the inverter is off. For DXing, the better DX is often during night hours anyway, so this may not be much of a sacrifice.

### Receive antenna improvements

A directional receive antenna with deep nulls (small loop, K9AY, Beverage) can null out the inverter direction. Effective if your antenna allows installing a separate RX antenna.

### Move the antenna

If your operating antenna is over the solar panels (e.g., a roof-mounted vertical), moving it to a separate ground-mounted location away from the panels and inverter can help dramatically.

## What you can't fix

Some installations are essentially unfixable:

- **Microinverter systems with poor connections** between modules.
- **String inverters with internal RF emission** that no external choke can address.
- **DC wiring runs of 100+ feet** that radiate as long-wire antennas.

In these cases, the choices are: replace the inverter, accept the noise, or move.

## Working with the installer

If your inverter caused RFI and you complained at installation:

- **Most installers don't know about ham radio.** Be patient.
- **Document the noise** before bringing it up. Recordings, S-meter readings, on/off comparisons.
- **Cite the FCC Part 15 standard.** A consumer device that emits more than the limits is non-compliant; the manufacturer is liable, the installer isn't.
- **Push for inverter swap** if the system was sold as RFI-compliant. Some manufacturers will swap inverters for known-quiet alternatives.
- **Be persistent.** It can take months.

## A note on grid-tied vs off-grid

- **Grid-tied inverters** sync to the AC line and inject power back. Most modern residential systems.
- **Off-grid inverters** (with batteries) generate their own AC. Some are quieter than grid-tie units; some are noisier.
- **Hybrid inverters** (Enphase IQ8, Tesla Powerwall paired with grid-tie) can switch between modes. RFI varies by mode.

## See also

- §13-01 — switching power supplies (the underlying noise mechanism)
- §12-04 — ferrite selection
- §14 — power-line noise (sometimes confused with solar)
