---
id: 29-07
title: Mast-Mounted Preamps
chapter: 29
section: 07
level: mixed
status: published
---

# Mast-Mounted Preamps

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A satellite signal from LEO is weak when it gets to the ground. A 1-watt downlink from a CubeSat 1500 km away delivers roughly **-130 dBm at your antenna's feedpoint** — about 10 dB above thermal noise in a 2.5 kHz SSB bandwidth on a quiet receiver. There's no real margin. Anything that adds loss or noise between the antenna and the receiver eats into that 10 dB and pushes the signal toward unintelligible.

The single biggest improvement most amateur satellite stations can make is **moving the receive preamp from the shack to the antenna feedpoint**. A 0.5 dB noise figure preamp mounted at the antenna beats a 0.5 dB NF preamp mounted at the rig — every time, by a lot. This section is about why, and how to actually implement it.

## Why coax loss matters so much

Coax has loss that increases with frequency and with length. Some representative numbers for common amateur feedlines, at 100 feet:

| Coax type | Loss at 144 MHz | Loss at 435 MHz | Loss at 1.2 GHz |
|-----------|-----------------|-----------------|-----------------|
| RG-58 | 4.0 dB | 7.4 dB | 14.5 dB |
| RG-8X | 2.9 dB | 5.2 dB | 9.0 dB |
| RG-213 | 1.5 dB | 2.8 dB | 5.0 dB |
| LMR-400 | 1.5 dB | 2.7 dB | 4.7 dB |
| LMR-600 | 0.9 dB | 1.7 dB | 3.0 dB |
| 7/8" hardline | 0.5 dB | 0.9 dB | 1.6 dB |

A typical home installation might have 80 feet of LMR-400 from a tower to the shack. At 435 MHz that's about 2.2 dB of loss — which doesn't sound bad until you realize **that 2.2 dB is loss on a signal that's already 1 dB above noise**, and it's also 2.2 dB of additional noise figure on the receive system regardless of what preamp you have in the shack.

The Friis cascaded noise figure formula explains the problem:

**F_total = F_1 + (F_2 - 1)/G_1 + (F_3 - 1)/(G_1 × G_2) + ...**

Where F_n is the noise factor (linear) of each stage and G_n is the gain (linear) of each stage. **The first stage's noise figure dominates.** A high-gain low-noise first stage swamps everything that comes after it.

If the first stage of your receive chain is the coax (a passive lossy element with noise figure = loss in dB and gain = -loss in dB), the second stage (the rig's front end) noise figure is divided by the coax gain — which is a fraction less than 1, so you're *multiplying* the rig's noise figure contribution. Bad.

If the first stage is a **preamp at the antenna feedpoint** with 20 dB gain and 0.5 dB noise figure, the coax (and everything after it) is divided by 100x in noise contribution. The coax becomes effectively invisible.

## Worked example

Compare two configurations for 435 MHz reception with 80 feet of LMR-400:

**Configuration A: preamp at the rig (shack-mounted)**

- Antenna → 80 ft LMR-400 (2.2 dB loss) → shack preamp (NF 0.5 dB, gain 20 dB) → rig (NF 6 dB).
- Cascade: F_coax = 2.2 dB (loss). F_preamp = 0.5 dB. F_rig = 6 dB.
- F_total ≈ 2.2 + (0.5 effective) + (6 - 1)/(20 dB gain ÷ 2.2 dB loss) = first stage is the coax, so F_total starts at 2.2 dB and adds tiny amounts.
- **System NF ≈ 2.7 dB.**

**Configuration B: preamp at the antenna (mast-mounted)**

- Antenna → mast preamp (NF 0.5 dB, gain 20 dB) → 80 ft LMR-400 (2.2 dB loss) → rig (NF 6 dB).
- F_preamp = 0.5 dB. F_coax = 2.2 dB. F_rig = 6 dB.
- F_total ≈ 0.5 + (2.2 - 1)/100 + (6 - 1)/100 ≈ 0.5 + 0.012 + 0.05 ≈ **0.56 dB.**

Configuration B is **2.1 dB better** than A. That's a doubling of received SNR — the difference between a marginal signal and a clean one.

## Where to put the preamp physically

The preamp needs to be **before the coax run**, as close to the antenna feedpoint as possible. In practice:

- **At the antenna feedpoint:** the ideal. Some commercial antennas (M2 EB-series with optional LNA, certain crossed Yagi packages) include integral preamp space.
- **In a weatherproof box on the boom or mast, within a foot of the feedpoint:** equivalent in performance, easier to install and maintain.
- **At the bottom of the tower, before the cable run to the shack:** acceptable for short tower-to-feedpoint cables (under 20 ft), but inferior to true mast-mounting.

The preamp box needs to be:

- **Weatherproof:** sealed against rain, with appropriate gland fittings on the cables.
- **Lightning-protected:** an arrestor on the input side, grounded to the mast.
- **Powered:** typically 12 V via the coax itself (bias-T arrangement; see below).
- **Bypass-able during transmit:** the preamp must switch out of circuit during your TX, or it will be destroyed. This is non-negotiable.

## TX/RX switching for full-duplex satellite use

A satellite preamp must handle the case where the rig may **transmit on the same band the preamp is amplifying** (e.g., for a single-antenna 70 cm uplink + 70 cm downlink CubeSat operating on a single antenna).

For dual-band satellite work (V/U or U/V mode), this is usually not an issue — you have separate 2 m and 70 cm antennas and feedlines, with a preamp on each downlink line that doesn't see the other band's TX. The standard arrangement:

- **2 m preamp** mounted on the 2 m Yagi/eggbeater feedpoint, amplifies 144 MHz downlink, doesn't see the 70 cm TX (which is on a separate antenna and feedline).
- **70 cm preamp** mounted on the 70 cm Yagi feedpoint, amplifies 437 MHz downlink, doesn't see the 2 m TX.

For single-band operation (or when uplink and downlink share an antenna), the preamp needs a **TX-bypass relay** triggered by the rig's PTT or by RF-sense circuitry. The relay routes the antenna directly to the rig during TX, bypassing the preamp.

Reliable PTT-switched bypass is the safer approach — it eliminates any window where TX RF could hit the preamp's sensitive input. RF-sense bypass works most of the time but can fail under specific conditions (slow keying, high VSWR transients).

## SAW filters for image rejection

A high-gain preamp without filtering will amplify **everything in the band** — including strong nearby signals on adjacent frequencies, paging transmitters, commercial UHF, etc. Strong out-of-band signals can desensitize the preamp through gain compression or third-order intermodulation, raising the system noise floor.

The standard fix is a **SAW (Surface Acoustic Wave) filter** in front of the preamp's input, passing only the satellite band (e.g., 435-440 MHz on 70 cm) and rejecting everything else by 40-60 dB. Most commercial mast-mounted preamps include a SAW filter as standard.

Tradeoff: the SAW filter adds about 1.5-2 dB of insertion loss, which adds the same amount to the system noise figure. That's the cost of immunity to strong adjacent-channel signals — and on most amateur band-edges (where commercial users sit right at 440 MHz, or where cell towers blast nearby), it's worth the cost.

## Power feed via bias-T

The preamp needs DC power, typically 12 V at 100-200 mA. Running a separate power cable up the tower is ugly; the standard solution is **bias-T power feed via the coax itself**.

A bias-T is a passive three-port network:

- **RF in/out:** the signal port, AC-coupled.
- **DC in:** the power port, DC-coupled with a series choke to block RF.
- **RF + DC:** the combined port, which carries both the RF signal and the DC power.

At the shack end, a bias-T injects 12 V onto the coax shield (or center conductor, depending on design) using a series choke; at the antenna end, another bias-T extracts the DC power for the preamp while passing the RF onward to the rig. The two bias-Ts are sold as matched pairs.

Common amateur products:

- **DEMI** (Down East Microwave) mast-mounted preamps with paired bias-T injectors.
- **SSB Electronics** preamps — German brand, popular in European amateur satellite installations.
- **Microwave Modules** in the UK — similar product line.
- **MFJ-1130 series** — economical entry-level mast preamps.

Cost is typically $150-400 per preamp + bias-T pair, depending on noise figure and gain spec.

## Common installation mistakes

- **Forgetting to invert the preamp's relay polarity** for the rig's PTT line. The bypass relay may stay energized in the wrong state.
- **Inadequate weatherproofing.** Water in the preamp box destroys it within months. Use proper gland fittings, drain holes at the bottom, and silicone sealant around all penetrations.
- **No lightning arrestor.** A mast-mounted preamp is at the top of the tower; lightning will find it. An arrestor at the bottom of the tower (where the coax enters the building) plus a fuse on the bias-T power line is the minimum protection.
- **Putting a preamp in front of an already-good-NF rig that's seeing zero coax loss.** If your rig is a Kenwood TS-2000 with the integrated 2 m / 70 cm preamps and you have 10 feet of LMR-400, the mast preamp adds almost nothing. The math has to favor mast-mounting; it doesn't always.
- **Too much gain.** A 30+ dB mast preamp can overload the rig's front-end, especially if you have nearby paging transmitters or cell sites. 15-20 dB gain is typically the sweet spot.

> **Advanced —** For 23 cm and microwave satellite work (1.2 GHz and up), the LNA's noise figure becomes the dominant system spec. State-of-the-art HEMT-based LNAs at 1.2 GHz reach 0.3 dB NF, and at 10 GHz can reach 0.6 dB NF with cryogenic cooling (not amateur-typical but possible). At these frequencies the cable loss penalty is so severe that the preamp **must** be at the antenna — there's no compromise that works. Most 23 cm satellite ground stations include a feedpoint LNA as part of the antenna assembly, with the bias-T integrated into the dish or helix mount.

## See also

- §29-05 — Helical antennas (where the LNA is usually integrated with the helix feed)
- §29-06 — Polarization switching (mast-mounted preamps integrate naturally with switching networks)
- §29-08 — Doppler automation
- §06 — Antennas (cable loss, system noise figure)
