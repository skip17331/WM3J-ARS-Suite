---
id: 24-01
title: DMR Overview
chapter: 24
section: 01
level: mixed
status: published
---

# DMR Overview

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What it is

**DMR — Digital Mobile Radio** — is a digital voice standard published by ETSI (the European Telecommunications Standards Institute) as **ETSI TS 102 361**. It was designed for *commercial* land-mobile use: taxi fleets, security guards, factories, utilities. Amateur radio adopted it about a decade after commercial deployment, mostly because the commercial gear got cheap and the surplus market filled with capable radios.

By active-user count, DMR is the largest amateur digital-voice ecosystem worldwide. There are over 250,000 registered DMR IDs (registered at radioid.net), and the BrandMeister network alone routinely sees 5,000+ simultaneously connected hotspots and repeaters.

## How it works

DMR uses **2-slot TDMA** (Time-Division Multiple Access) on a single 12.5 kHz RF channel. That's the headline feature:

```
   One 12.5 kHz RF channel
   ────────────────────────
   Slot 1: │ TS1 │ TS1 │ TS1 │ TS1 │ ...
   Slot 2: │ TS2 │ TS2 │ TS2 │ TS2 │ ...
           │     │     │     │     │
           ←─30ms→ each
```

Each 60 ms frame is split into two 30 ms time slots. Two completely separate conversations can run on the same RF channel at the same time — Slot 1 might carry "North America Tac 1" while Slot 2 simultaneously carries "Worldwide English". A single 12.5 kHz repeater pair therefore behaves like two repeaters.

> **Advanced —** TDMA also means a continuously transmitting DMR radio's RF stage is actually only keyed on for 30 ms out of every 60 ms. The radio's PA duty cycle is ~50%, which improves battery efficiency, lowers thermal stress, and explains why DMR HTs can run higher peak power in the same chassis as an equivalent FM radio. The "off" half of each frame is also where the radio's receiver gets a chance to listen for the other slot, talkgroup hangtime, and network sync.

### The codec — AMBE+2

DMR uses **AMBE+2** (Advanced Multi-Band Excitation, 2nd-gen) for voice compression. AMBE+2 takes 20 ms of speech and encodes it to roughly 49 bits + FEC, fitting comfortably inside one TDMA slot with bandwidth to spare.

AMBE+2 is **proprietary** — owned by DVSI Inc. — which is why every commercial DMR radio contains a small AMBE+2 chip and why open-source DMR transceivers are very rare. It's the same lineage as the original AMBE used in D-STAR, but tuned for slightly better quality at slightly higher bit rates.

The audio is intelligible, slightly thin, and tolerates background noise reasonably well. Most listeners describe it as "phone-call quality" — clear but not warm.

### The modulation

DMR uses **4FSK** (4-level frequency shift keying) at 4800 symbols/sec — so 9600 bits/sec total over the RF channel. With FEC overhead, each slot carries about 2.45 kbit/sec of usable voice data.

| Property | Value |
|----------|-------|
| Channel spacing | 12.5 kHz |
| Modulation | 4FSK |
| Symbol rate | 4800 / sec |
| Gross bit rate | 9600 bps |
| Slots per channel | 2 (TDMA) |
| Voice codec | AMBE+2 |
| Frame length | 60 ms (30 ms × 2 slots) |
| Color codes | 16 (CC 0–15; like CTCSS for digital) |

## The Tiers — I, II, III

DMR is defined in three tiers, but only two matter for amateur use.

**Tier I** — license-free, low-power, **simplex only**. Used by some consumer DMR walkie-talkies (the FRS-equivalent in some countries). Effectively never on ham bands.

**Tier II** — licensed, conventional (non-trunked), repeaters allowed. **This is what amateurs use.** Every BrandMeister / IPSC2 repeater and every ham DMR hotspot is Tier II.

**Tier III** — licensed, **trunked**. Used by commercial fleets that want a pool of channels managed by a control channel. Trunking is incompatible with the amateur cultural model (no centralized fleet manager), so Tier III is absent from ham radio.

## Why DMR dominates commercial radio

When ETSI ratified DMR around 2005, every commercial radio manufacturer had a path to compliance. Motorola (MOTOTRBO), Hytera, Kenwood, Icom (commercial line), Vertex Standard, and a dozen Chinese OEMs all shipped Tier II radios in volume. The two-slot TDMA was a big draw for license holders because it doubled their per-channel capacity overnight. By 2015, DMR had displaced most of the older proprietary digital trunking systems in commercial use.

That commercial mass production is what made amateur DMR cheap. Anytone D878UV at $250, BTECH DMR-6X2 at $200, used MotoTRBO XPR portables under $100 — that pricing exists because the *commercial* side amortized R&D, and amateurs ride the wave.

## Ham adoption history — the short version

- **2008–2010:** First amateur experiments with surplus MotoTRBO gear. Operators figured out how to register IDs and link individual repeaters.
- **2012:** **DMR-MARC** (Motorola Amateur Radio Club) launched the first amateur DMR network — centralized, US-focused, with a curated talkgroup list.
- **2015:** **BrandMeister** launched (open-source, distributed, user-modifiable). Quickly eclipsed DMR-MARC in user count.
- **2017:** Anytone D868UV released — first widely available *amateur-marketed* DMR HT with a sane codeplug GUI. Mass adoption follows.
- **2019:** **IPSC2** spreads as a BrandMeister alternative, especially in Europe.
- **2020s:** TGIF Network appears as a third major option. Hotspots (Pi-Star, OpenSpot) become the dominant access method.

## Where amateur DMR lives

Almost entirely **UHF (70 cm / 440 MHz)**. A few VHF DMR repeaters exist but they are uncommon. Reasons:

- The commercial DMR equipment market is UHF-heavy (more commercial UHF licenses than VHF).
- 70 cm has more spectrum to spare for digital channels — most US ham 70 cm band plans set aside 441–445 MHz for digital.
- Penetration into buildings (where DMR fleets historically operated) favors UHF.

Typical amateur DMR repeater frequencies in the US: **441–445 MHz input, +5 MHz offset** (e.g., 441.1875 in / 446.1875 out — though by tradition the listed frequency is the *output*).

> **Advanced —** The DMR "color code" (CC) is a digital equivalent of CTCSS — a 4-bit field (0–15) embedded in every voice frame's sync pattern. Repeaters reject frames with the wrong color code. Adjacent repeaters on the same frequency use different CCs to keep cross-talk out. Most US amateur DMR repeaters default to **CC1**; some choose CC2 or CC11 to differentiate networks (e.g., BrandMeister vs IPSC2 in the same metro area).

## What you need to get on DMR

1. **A DMR radio.** Anytone AT-D878UVII Plus is the current favorite (~$300). Cheaper: TYT MD-UV380 (~$130). For mobiles: Anytone AT-D578UVIII or Motorola XPR4350.
2. **A DMR ID.** Free registration at **radioid.net** — they verify your callsign against the FCC/national database and issue a 7-digit DMR ID (mine is `3157331` for example). The ID is embedded in every transmission so the network knows who's talking.
3. **A codeplug.** This is the radio's configuration file — channels, talkgroups, scan lists, contact list, color codes, etc. You'll spend an evening or two on this. Most clubs publish a starter codeplug for their region.
4. **An access point** — either a local DMR repeater on a network you can reach, or a personal hotspot (see [§24-07](24-07-hotspot-pistar.md)).

## See also

- [§24-02](24-02-dmr-talkgroups.md) — Talkgroups (how DMR routes calls)
- [§24-03](24-03-brandmeister-vs-ipsc2.md) — Picking a DMR network
- [§24-07](24-07-hotspot-pistar.md) — Setting up a Pi-Star hotspot for DMR
- [§04-05](../04-repeaters-bandplans/04-05-linked-systems.md) — DMR in the broader linked-systems landscape
- [§24-10](24-10-ber-explained.md) — BER as the DMR signal-quality metric
