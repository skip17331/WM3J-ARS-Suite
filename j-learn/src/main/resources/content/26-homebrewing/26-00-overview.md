---
id: 26-00
title: Homebrewing & RF Construction — Overview
chapter: 26
section: 00
level: simple
status: draft
---

# Homebrewing & RF Construction — Overview

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## The tradition

Amateur radio was a *homebrew* hobby for its first fifty years. Until WWII, there was almost no commercial market for ham gear — every transmitter was built on a piece of pine board with hand-wound coils, tube sockets, and a few mica capacitors. Even into the 1970s the kit lineage (Heathkit, Knight, Allied) was strong enough that "I built my rig" was the default story, not the exception.

That world is gone. Today's HF transceiver has a 32-bit DSP, two thousand SMT parts, and a firmware load that took an engineering team a year to write. You will not homebrew an IC-7300. You can, however, homebrew almost everything that goes around the radio — and doing so teaches more about RF than any book.

## What's worth building today

| Category | Worth building? | Why |
|----------|-----------------|-----|
| **HF transceiver** | No | 2000+ SMT parts, DSP firmware, multi-band switching. Buy used. |
| **VHF/UHF FM rig** | No | Same. Chinese HTs are $30. |
| **RF power amplifier** (tube or solid-state) | **Yes** | Few parts, lots of learning, real cost savings vs. commercial |
| **Low-pass / band-pass filters** | **Yes** | Trivial parts, big impact on harmonic suppression and RX quality |
| **Antenna tuner** | **Yes** | Inductors, caps, switches — bench-friendly |
| **Antennas** (dipoles, verticals, Yagis, loops) | **Yes** | The most common homebrew project; results often beat commercial |
| **Baluns / chokes / ununs** | **Yes** | See §18-05 — a $13 part is just wire and a ferrite |
| **QRP CW transmitters** (Pixie, Forty-9er, BITX) | **Yes** | Classic learning projects; many kits available |
| **Class-E QRP transmitters** | **Yes** | Modern, high-efficiency, simple |
| **Audio accessories** (CW keyers, headphone amps, audio filters) | **Yes** | Easy entry; useful daily |
| **Power supplies** | Maybe | Linear builds are educational; switchers are a packaging exercise |
| **Test gear** (dummy loads, attenuators, signal generators) | **Yes** | Cheaper than commercial and you learn calibration |

The general rule: anything **mostly passive** (filters, antennas, baluns, dummy loads) is high-value homebrew. Anything that is **mostly software** (modern transceiver, digital decoder) is not.

## Tools you need to start

A modest bench will support 90% of amateur construction:

**Soldering / assembly:**
- Temperature-controlled soldering iron (Hakko FX-888D, Weller WE1010, or Pinecil USB-C portable)
- 60/40 leaded solder, 0.5 mm and 1.0 mm spools (leaded melts cleaner; lead-free is required only if you're selling commercially)
- Solder wick, flux pen, isopropyl alcohol for cleanup
- Helping-hands magnifier with LED ring
- Side cutters, needle-nose pliers, wire strippers

**Measurement:**
- Digital multimeter (Fluke 87V if you can afford it; UNI-T UT61E or a $30 Aneng for getting started)
- Oscilloscope — Rigol DS1054Z (100 MHz, 4-channel) or Siglent SDS1104X-E
- NanoVNA-H4 or SAA-2N for antenna and filter sweeps (under $100, paradigm-changing)
- Dummy load (50 Ω, 100 W minimum — see §25)
- Frequency counter (cheap PLJ-1601C or built into the scope)

**Parts storage:**
- Component organizer drawers (Akro-Mils, HARBOR FREIGHT or equivalent)
- Resistor/capacitor assortment kits (1 % metal-film 0805 and through-hole, 50 V ceramic + film cap kits)
- Toroid assortment: a few each of T-50-2, T-50-6, T-50-17, FT37-43, FT82-43, FT140-43, FT240-43

**Stock supplies:**
- Enameled magnet wire (#22, #24, #26 AWG)
- Hookup wire (22 AWG stranded, multiple colors)
- PCB blanks (single- and double-sided copper-clad, perfboard, Manhattan-style pads)
- Hammond aluminum boxes (1590B, 1590BB, 1590D — the Eddystone-style die-cast classics)

Total bench cost for a competent station: **$500–$1,200** new; **$200–$400** if you buy used and patient.

## Table of contents (chapter 26)

| § | Topic | Level |
|---|-------|-------|
| 26-01 | RF Amplifier Topologies (Class A through F) | Advanced |
| 26-02 | Low-Pass Filters (harmonic suppression) | Mixed |
| 26-03 | High-Pass Filters (RX protection, hum blocking) | Mixed |
| 26-04 | Bandpass & Notch Filters (contest RX, QRM kill) | Mixed |
| 26-05 | Toroid Selection (powdered iron) | Mixed |
| 26-06 | Ferrite Mix Selection (#31, #43, #61…) | Mixed |
| 26-07 | Linear vs Switching Power Supplies | Mixed |
| 26-08 | Enclosures & Shielding (boxes, feedthroughs) | Mixed |
| 26-09 | Grounding for Homebrew (star ground) | Mixed |
| 26-10 | PCB Layout Basics (RF traces, ground planes) | Mixed |
| 26-11 | RF Safety in Homebrew (HV, bleeders, burns) | Mixed |

## See also

- [§09-13 — Trap Design](../09-antenna-calc/09-13-trap-design.md) — winding HF trap coils
- [§18-05 — Baluns & Chokes](../18-coax-connectors/18-05-baluns-chokes.md) — ferrite balun reference
- [§17 — Formulas](../17-formulas/) — Ohm's law, reactance, Q, resonance
- [§25 — Test Equipment](../25-test-equipment/) — what to verify with and how
- [§27 — Station Engineering](../27-station-engineering/) — grounding, bonding, lightning
