---
id: 12-05
title: Faulty Balun
chapter: 12
section: 05
level: simple
status: draft
---

# Faulty Balun

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A balun (BALanced-to-UNbalanced transformer) sits between coax and an antenna that needs balanced feed. When it fails — most commonly from water, lightning, or core saturation — SWR rises and the radiation pattern goes weird. A balun is the second-most-likely external cause of SWR problems after coax/connectors.

## Where baluns live

- **At the feedpoint** of a center-fed dipole — converts the unbalanced coax to balanced antenna feed.
- **Inside a Yagi feedpoint** — same idea.
- **At the base of a vertical antenna** — usually an UNUN (unbalanced-to-unbalanced impedance transformer, similar concept).
- **At the base of an EFHW** — a 49:1 or 64:1 UNUN.
- **At a junction in an open-wire-line system** — converts coax-fed shack to ladderline-fed antenna.

If your antenna has a balun and SWR is wrong, the balun is suspect.

## Symptoms that point at the balun

- **High SWR on multiple bands** of a multi-band antenna (suggesting the wideband matching device is wrong).
- **SWR worse when wet** — water in the balun enclosure shorts internal components.
- **Burning smell from the antenna feedpoint** after a high-SWR transmission.
- **Visible damage** — cracked enclosure, melted potting compound, burnt-looking core.
- **Fuses or other accessories blown** in a recent lightning event near the antenna.

## Quick tests

1. **Visual inspection.** Look at the balun. Cracked? Melted? Discolored? Replace it.
2. **DC resistance check.** Disconnect the balun. With an ohmmeter, check coax-port-center to coax-port-shield (should be open or high resistance for current baluns; some may show a few ohms for choke baluns). Then check antenna-port-A to antenna-port-B (should match the spec sheet — often a few ohms for current balun, low resistance for voltage balun, etc. — consult the manufacturer).
3. **Substitution test.** Replace the balun with a known-good one (or temporarily wire the antenna directly to the coax, no balun, with a low-power test). If SWR fixes, the balun was bad.
4. **Smoke test (only if you must).** With a low-power test signal, look for arcing or smoke from the balun. Don't keep transmitting if you see either.

## Common failure modes

### Water ingress

The most common balun failure. Coax sealant fails, water gets into the enclosure, shorts internal components or saturates the core.

**Sign:** SWR worse after rain. Visible water inside the enclosure. Discoloration of the potting compound.

**Fix:** Open the enclosure. If components look fine and dry, dry it out (sun, hairdryer) and re-seal with weather-resistant sealant. If components are damaged (burnt windings, corroded leads), replace.

### Lightning damage

A nearby strike (doesn't have to be a direct hit) couples enormous current into the antenna and through the balun. Insulation breaks down; cores crack; windings fuse together.

**Sign:** Visible burns, cracked core, melted plastic. Often accompanied by other electrical damage (rotor controller, radio front end, station ground wires).

**Fix:** Replace the balun. Inspect everything else for damage too.

### Core saturation from over-power

Running 1500 W into a balun rated for 100 W will heat the core, eventually causing the ferrite to saturate (lose its magnetic properties) and possibly crack. Once saturated, the balun is essentially a piece of wire — no transformation, often a near-short circuit.

**Sign:** SWR rises after sustained high-power TX, even at the original operating frequency. Balun feels hot to the touch immediately after TX.

**Fix:** Replace with a balun rated for at least 1.5x your typical power, ideally 3x for digital modes (which run continuous at full duty cycle).

### Manufacturing defect

Rare, but happens. New balun shows wrong impedance ratio out of the box.

**Sign:** SWR doesn't improve from the moment of installation; sweep shows wrong impedance characteristics on the bench.

**Fix:** Return to manufacturer.

### Wrong balun type for the antenna

Common with hobbyist-built antennas. A 4:1 voltage balun used where a 1:1 current balun is needed (or vice versa) can produce reasonable SWR but bad common-mode rejection — and intermittent SWR problems as the common mode shifts with weather and antenna orientation.

**Sign:** SWR is OK, but the antenna behaves oddly — pattern shifts with band, RFI in the shack, S-meter changes when you touch the coax inside.

**Fix:** Use the correct balun type for the antenna. Most modern dipoles, EFHWs, and Yagis want a **1:1 current choke** at the feedpoint to suppress common mode; impedance transformation (4:1, 9:1, 49:1, 64:1) is a separate function done by an UNUN.

## Balun types — quick reference

| Type | Function | Common use |
|------|----------|------------|
| **1:1 current balun (choke)** | Forces equal currents into the two antenna sides; suppresses common-mode current on the coax shield | Center-fed dipole, Yagi, any antenna that needs balanced feed |
| **4:1 voltage balun** | 4:1 impedance step-up between coax and antenna | Off-center-fed dipoles (OCFDs), windom antennas, some folded dipoles |
| **4:1 current balun** | 4:1 impedance step-up + common-mode suppression | Modern preferred design over voltage balun for 4:1 ratios |
| **9:1 UNUN** | 9:1 impedance transformation, no balance | Random wire / longwire antennas |
| **49:1 UNUN** | 49:1 impedance transformation | End-fed half-wave (EFHW) antennas |
| **64:1 UNUN** | 64:1 impedance transformation | Some EFHW designs (Buddipole-style) |

## Building vs buying

Commercial baluns from reputable makers (Balun Designs, MyAntennas, Palomar, DX Engineering) are well-made and reasonably priced ($50–$150 for most amateur-power units). Build-your-own is satisfying but easy to get wrong — wrong core mix, wrong winding count, inadequate enclosure sealing.

If you do build:
- Use the right ferrite mix. For HF, mix #43 (FT-240-43) is the standard for choke baluns; mix #61 is preferred for wideband transformers.
- Wind enough turns. Too few = poor common-mode rejection at the lowest band; too many = bandwidth limited.
- Pot or seal the enclosure. Even a "dry" location like under an eave will see condensation; treat it as outdoor.

> **Advanced —** Choke balun effectiveness is measured as common-mode impedance, ideally at least 5,000 Ω across the operating bands. A typical 10-turn FT-240-43 choke gives ~3,000–10,000 Ω on 80 m through 10 m, with the peak around 14 MHz. For 160 m operation, a single FT-240-43 isn't enough; stack 2-3 cores or use a "ugly balun" (coiled coax) with many more turns. The MyAntennas RF-1 series datasheets show common-mode impedance vs frequency plots — useful reference for builders deciding whether their design is adequate.

## See also

- §06-12 — baluns and chokes (theory, in chapter 5)
- §18-05 — baluns and chokes (reference)
- §12-06 — feedline routing (related — common-mode current is a feedline issue too)
