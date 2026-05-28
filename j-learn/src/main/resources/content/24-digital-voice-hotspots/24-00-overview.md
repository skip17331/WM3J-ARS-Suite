---
id: 24-00
title: Digital Voice & Hotspot Systems — Overview
chapter: 24
section: 00
level: simple
status: draft
---

# Digital Voice & Hotspot Systems — Overview

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What digital voice is

**Digital voice (DV)** is two-way radio that turns your microphone audio into a stream of bits, transmits the bits over the air, and turns them back into audio at the other end. Instead of the analog FM hiss-and-modulation you grew up with, the radio sends a structured digital packet — call it a frame — that carries a few dozen milliseconds of compressed speech plus some metadata (your callsign, your talkgroup, error-correction bits).

Three properties fall out of doing it that way:

- **Audio either decodes clean or it doesn't.** No graceful degradation; you hear perfect audio or robotic R2-D2 noises (or silence) once the signal drops below the decode threshold. This is the "digital cliff."
- **Metadata travels with the audio.** Your callsign, the destination, position data, even short text — all of it rides inside the same frame the voice does.
- **Routing is software, not RF.** Because every frame is labeled, a repeater can choose to forward your audio to a specific other repeater, a reflector, a talkgroup, or the internet at large.

That last property is what makes the modern DV world look the way it does: globally interconnected, room-based, and almost always backed by the public internet.

## The three dominant systems

| System | Sponsor | Codec | Routing model | Typical radio cost |
|--------|---------|-------|----------------|--------------------|
| **DMR** | ETSI (commercial standard, ham-adopted) | AMBE+2 | Talkgroups (numeric IDs) | $80–$300 (Anytone, Retevis, BTECH, Motorola surplus) |
| **D-STAR** | JARL / Icom | AMBE (original) | Callsign + reflectors (REF / XRF / DCS) | $300–$700 (Icom IC-705, ID-52, Kenwood TH-D74) |
| **Fusion (System Fusion / C4FM)** | Yaesu | AMBE+2 | Rooms / nodes (WIRES-X, YSF) | $200–$500 (FT-70D, FT3DR, FTM-300/500) |

A fourth — **NXDN**, **P25**, **TETRA**, etc. — exists in the public-safety / commercial world but barely registers in amateur use.

The three ham systems are mutually incompatible at the RF layer (different modulation, different framing, different error-correction). At the *internet* layer they're increasingly bridged via cross-mode reflectors and modern hotspots (see [§24-11](24-11-cross-mode-linking.md)).

## What a hotspot is

A **hotspot** is a tiny low-power transceiver (typically 10–20 mW) that:

1. Has a small SDR-ish radio front end — usually an MMDVM board on top of a Raspberry Pi, or a self-contained box like an OpenSpot or a SharkRF gear.
2. Connects to your home internet via Wi-Fi or Ethernet.
3. Bridges between RF (so your handheld talks to it) and an internet network (BrandMeister, REFxxx, YSF, etc.).

The handheld talks to the hotspot at across-the-room distances; the hotspot does the heavy lifting of joining the global network. The net effect: a 5 W DMR HT in your kitchen can have a clean QSO with VK in Australia, even if there is no DMR repeater within 500 miles of you.

Hotspots are why digital voice "won" amongst apartment-dwelling and HOA-constrained hams — you don't need a tower, you don't need a coordinated repeater, you just need a USB-C power brick and home Wi-Fi.

## When digital voice beats analog FM

- **Long internet-linked QSOs.** Calling Japan from the parking lot of a Walmart is an everyday thing on DMR. On analog FM-via-AllStar it works too, but DV's audio holds up cleaner over multiple internet hops.
- **Weak local signal.** A DV signal at -116 dBm decodes; an analog FM signal at the same level is mostly noise.
- **You want callsign-aware routing.** D-STAR's callsign routing finds a roaming friend without you needing to know which repeater they're using right now.
- **Spectrum efficiency.** DMR carries two simultaneous QSOs in 12.5 kHz that FM would need 25 kHz to do.

## When analog FM beats digital voice

- **Audio fidelity for ragchew.** Even the best codec (AMBE+2 at full rate) sounds compressed and a touch robotic. Analog FM with a decent mic is warmer.
- **Emergency / interop.** Public-service nets, ARES drills, and most simplex emergencies still use analog FM because anyone can join with any radio. Your DMR HT cannot talk to my D-STAR HT directly.
- **Simplicity.** No codeplug. No DMR ID. No reflector to remember. Pick a frequency, press PTT.
- **Battery life.** DV codec processing draws more current; analog FM HT batteries last longer on RX.
- **No internet.** When the cable modem goes down (or the power grid does), every internet-linked DV QSO dies instantly. Analog FM simplex keeps working.

## What this chapter covers

| Section | Topic |
|---------|-------|
| [§24-01](24-01-dmr-overview.md) | DMR overview — TDMA, AMBE+2, Tiers |
| [§24-02](24-02-dmr-talkgroups.md) | DMR talkgroups — routing by number |
| [§24-03](24-03-brandmeister-vs-ipsc2.md) | BrandMeister vs IPSC2 networks |
| [§24-04](24-04-dstar.md) | D-STAR — codec, history, callsign routing |
| [§24-05](24-05-dstar-routing.md) | D-STAR reflectors and routing (REF/XRF/DCS) |
| [§24-06](24-06-fusion-wires-x.md) | Yaesu System Fusion / WIRES-X |
| [§24-07](24-07-hotspot-pistar.md) | Pi-Star hotspot setup |
| [§24-08](24-08-hotspot-openspot.md) | OpenSpot — commercial alternative |
| [§24-09](24-09-duplex-vs-simplex-hotspots.md) | Duplex vs simplex hotspots |
| [§24-10](24-10-ber-explained.md) | BER (Bit Error Rate) explained |
| [§24-11](24-11-cross-mode-linking.md) | Cross-mode linking (DMR ↔ D-STAR ↔ Fusion) |
| [§24-12](24-12-digital-voice-etiquette.md) | Digital voice etiquette |

## See also

- [§04-05 — Linked Systems](../04-repeaters-bandplans/04-05-linked-systems.md) — the broader linked-systems landscape including AllStar and EchoLink
- [§03 — Digital Modes](../03-digital-modes/) — the *data* equivalents (FT8, RTTY, JS8Call), not voice
- [§22-08 — Split-Frequency Operation](../22-operating-practice/22-08-split-frequency.md) — hotspots are essentially permanent split, automated
