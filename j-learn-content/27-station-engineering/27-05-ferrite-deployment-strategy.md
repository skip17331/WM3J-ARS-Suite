---
id: 27-05
title: Ferrite Deployment Strategy
chapter: 27
section: 05
level: mixed
status: published
---

# Ferrite Deployment Strategy

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Ferrite chokes are the single most cost-effective RFI mitigation tool an amateur owns. A $5 toroid with five turns of cable through it can kill a noise problem that would otherwise require hundreds of dollars of testing. The catch: **placement and mix selection matter more than quantity**. A pile of random ferrites scattered across the shack does very little; a few chokes placed thoughtfully at the right impedance discontinuities can transform a noisy station into a quiet one.

This section is the deployment strategy — where to put chokes and why. Mix selection is covered in §14-04 (RFI ferrite selection) and §26-06 (the engineering view).

## The four common locations

In a typical station, there are four classes of cable that benefit from ferrites:

1. **Coax feedline** at the entry to the shack, and again at the antenna feedpoint — common-mode current on the shield.
2. **AC mains lead** at each piece of equipment — both for blocking noise *coming in* from the mains and stopping RF *going out* of the chassis into the mains.
3. **USB and Ethernet** between radio and computer — common-mode current on data lines.
4. **Audio cables** between rig, soundcard, and computer — both common-mode and ground-loop currents.

A station with chokes at all four locations is usually 10–20 dB quieter on receive and immune to RF-in-the-shack feedback on transmit.

## Location 1: Coax feedline

The most important single choke in the station. Goes at the **shack entry point** — where the coax transitions from the outdoor world to the indoor world.

```
   Antenna feedpoint
   │
   │  (coax run)
   │
   ▼
   ┌──[ Choke 1: 5–10 turns through FT-240-31 or FT-240-43 ]──┐
   │                                                          │
   │   (entry panel, arrestor)                                │
   │                                                          │
   ▼                                                          │
   To radio                                                   │
```

The purpose: kill any common-mode current that's traveled down the *outside* of the coax shield from the antenna. Without the choke, that current arrives at the rig and either:

- Bites the operator on the mic.
- Couples into the audio and produces "RF in the shack."
- Modulates the receive signal as the operator's hand moves on the chassis.

A typical kitchen-table HF station benefits hugely from this one choke. Cost: $15 (one FT-240-43 toroid, $8; a few feet of coax for the turns; cable ties to hold it together).

For HF: **FT-240-43** is the workhorse. Best impedance from about 5 MHz to 30 MHz.
For low-band emphasis (160 m / 80 m): **FT-240-31**. Better below 5 MHz, slightly worse above 14 MHz.
For wide-coverage HF including 6 m: **stack of two FT-240-31** in series.

Number of turns through the toroid:

- 5 turns: minimum useful for HF.
- 7–9 turns: typical for serious HF work.
- 10+ turns: difficult on RG-213; OK on smaller coax (RG-8X, RG-58).

**A second choke at the antenna feedpoint** is also valuable — it stops the antenna from radiating differently when the operator touches the rig. Often called the "1:1 choke balun" and standard practice on every wire dipole installation.

See [§18-05](../18-coax-connectors/18-05-baluns-chokes.md) for the design detail.

## Location 2: AC mains leads

Each piece of switching-power-supply-equipped equipment (modern rigs, computers, monitors, PSUs) can be both a *source* of conducted RFI on the AC line and a *receiver* of mains-coupled noise from other sources in the house.

The fix: **clamp a snap-on ferrite around the AC cord, close to the equipment**. The closer to the equipment, the better — RF hash from the gear doesn't get back out onto the house wiring.

Common-mode chokes for AC line:

- **Mix 31 snap-on** (Fair-Rite 0431167281, 1.4" ID) — covers 1 MHz to ~150 MHz. Useful for medium-frequency RFI.
- **Mix 73 snap-on** (Fair-Rite 0473167281) — covers below 1 MHz. Useful for AM-broadcast and lower-MF RFI.
- **Mix 43 snap-on** — covers 30 MHz to 300 MHz. Useful for VHF/UHF RFI from switching supplies.

A standard kit: one mix-31 and one mix-73 snap-on per AC cord. Cost: $4–$8 per snap-on. A typical 4-piece station with four AC cords needs 8 snap-ons — ~$50 total.

> **Advanced —** For chronic AC-mains noise, a *whole-station mains filter* is better than per-cord chokes. The Tripp-Lite ISOBAR series, MFJ-1164, and Corcom EMI filters (industrial) all provide better attenuation than snap-ons because they include capacitors-to-ground in addition to common-mode inductance. A 15 A Corcom EMI filter is ~$40 and handles the entire shack's AC.

## Location 3: USB and Ethernet

USB cables are notorious common-mode antennas. Every long USB cable behaves like a small dipole at HF — both radiating CAT-control noise *out* and picking up RFI *in*. The symptoms: CAT disconnects on transmit, USB audio glitches, soundcard noise floor that rises when the rig keys.

The fix: **a snap-on ferrite at each end of the USB cable, close to each connector**.

- **Mix 43 snap-on** is the right pick (USB hash is in the 10–200 MHz range).
- **2–5 turns** through the snap-on if possible; just clamping over the cable also helps but turns are dramatically better.

Pre-built USB cables with molded-in ferrites (the lumps on either end of many commercial cables) work but are typically Mix 43 with only one pass — equivalent to a 1-turn snap-on. Adding more turns through a separate ferrite gives 6–10 dB better attenuation.

Ethernet is similar. **Cat 6 with snap-on ferrites at each end** keeps switched-Ethernet noise from contaminating the receiver. Mix 31 or Mix 43; 2–3 turns.

## Location 4: Audio cables

Audio cables — the line-out from rig to soundcard, the line-in from rig mic-jack to computer — are short, low-impedance, and prone to ground loops. Common-mode current on the audio cables manifests as hum, buzz, or coupling between rig and computer.

The fix differs from the data-cable case:

- **Audio isolation transformer** (1:1 transformer, MFJ-1224 style, or a small commercial unit) breaks the DC ground path while passing audio.
- **Snap-on ferrite** at the rig end of each audio cable — Mix 31, 2–3 turns. Knocks down common-mode currents at HF.
- **Use shielded cable** (most consumer audio cables are shielded; the cheap ones aren't).
- **Keep audio cables away from coax and AC lines** in the cable bundle.

A station with FT8 hum is almost always an audio-cable common-mode problem. Add an isolation transformer and a ferrite and the hum drops 20+ dB.

## Snap-ons vs threaded toroids

Two physical formats:

- **Snap-on (clamp-on)**: hinges open; cable lays in; clamps closed. Easy to install/remove without disconnecting the cable. Slightly less effective per turn (~70% of a threaded equivalent) due to the air gap.
- **Threaded (closed) toroid**: the cable must be passed through the hole multiple times. Better attenuation per turn. Requires disconnecting one end of the cable to install.

For *temporary* RFI hunting (§14-05), snap-ons. For *permanent* station installation, threaded toroids where practical, snap-ons where the cable can't be disconnected (USB on a sealed device, AC power cord on a moulded plug).

## A complete station ferrite inventory

For a typical HF station:

| Location | Ferrite | Cost |
|----------|---------|------|
| Coax at entry panel | 1 × FT-240-43, 7 turns | $15 |
| Coax at antenna feedpoint | 1 × FT-240-43, 5 turns | $15 |
| Each AC cord (× 4) | Mix-31 snap-on | $24 |
| AC at noisy switching PSU | Mix-73 snap-on | $8 |
| USB cable to rig | Mix-43 snap-on × 2, 3 turns each | $10 |
| Ethernet to computer | Mix-31 snap-on × 2 | $8 |
| Audio rig→PC | Mix-31 snap-on, 3 turns | $5 |
| Audio PC→rig | Mix-31 snap-on, 3 turns | $5 |
| Rotor control cable | Mix-43 snap-on at entry | $5 |
| **Total** | | **~$95** |

For under $100, the entire station is choke-protected. The improvement on a typical noisy QTH is dramatic — 6–15 dB lower noise floor, no RF-in-the-shack symptoms, no CAT disconnects on transmit.

## When ferrites don't work

Some failures of ferrite RFI mitigation:

- **Wrong mix for the frequency.** Mix 43 on 80 m does almost nothing; Mix 31 or Mix 75 is needed for low bands. (See [§26-06](../26-homebrewing/26-06-ferrite-mixes.md) for mix vs frequency.)
- **Too few turns.** One pass through a snap-on is barely measurable; 5+ turns is the useful regime.
- **Wrong location.** A choke in the middle of a cable does nothing for common-mode currents that have already coupled into the equipment.
- **The noise is differential, not common-mode.** Ferrites only suppress common-mode (where the two conductors carry currents in the same direction). Differential noise (signal vs return) passes straight through.

If a placed ferrite doesn't fix the problem, work through the isolation workflow ([§14-05](../14-rfi/14-05-isolation-workflow.md)) instead of adding more ferrites.

## Cross-references

- [§14-04 — Ferrite Selection (RFI view)](../14-rfi/14-04-ferrite-selection.md) — the consumer-side selection guide
- [§14-05 — Isolation Workflow](../14-rfi/14-05-isolation-workflow.md) — finding what to choke
- [§26-06 — Ferrite Mixes (engineering view)](../26-homebrewing/26-06-ferrite-mixes.md) — material-by-material reference
- [§18-05 — Baluns & Chokes](../18-coax-connectors/18-05-baluns-chokes.md) — the antenna-side common-mode choke
