---
id: 04-05
title: Linked Systems (AllStar / DMR / Fusion / D-STAR)
chapter: 04
section: 05
level: mixed
status: published
---

# Linked Systems (AllStar / DMR / Fusion / D-STAR)

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A traditional repeater serves a 30-mile circle. **Linked systems** connect repeaters together — over the internet, over RF, over both — so a station with a 5-watt handheld in Maryland can have a clean QSO with a station in New Zealand. There are several incompatible ecosystems doing this, each with its own technology, vocabulary, and culture.

## The 30-second map

| System    | Voice technology | Audio is | Linking is | Used where |
|-----------|------------------|----------|------------|------------|
| **AllStar Link** | Analog FM | Analog over the air | Internet | Worldwide; very popular for ad-hoc node operators |
| **EchoLink**    | Analog FM | Analog over the air | Internet | Older, simpler, computer-based clients common |
| **IRLP**        | Analog FM | Analog over the air | Internet | Older; mostly retired in favor of AllStar |
| **D-STAR**      | Digital (AMBE codec) | Digital over the air | Internet (or RF) | First mainstream digital voice; Icom-driven |
| **DMR**         | Digital (AMBE+2 codec) | Digital over the air | Internet (BrandMeister, TGIF, etc.) | Most popular digital today; ex-commercial gear cheap |
| **Yaesu System Fusion (C4FM)** | Digital | Digital over the air | Internet (Wires-X, YSF) | Yaesu-driven; nice analog/digital mode-mix support |
| **NXDN**        | Digital | Digital over the air | Limited | Niche; commercial overflow |
| **P25**         | Digital | Digital over the air | Limited | Public-safety overflow; uncommon on ham bands |

Two big distinctions matter:

- **Analog vs digital voice.** Analog systems (AllStar, EchoLink, IRLP) sound like FM. Digital systems (D-STAR, DMR, Fusion) sound robotic and "tinny" but are noise-immune at usable signal levels.
- **Reachability.** All of them can reach internet-connected nodes worldwide; what differs is the codec, the network protocol, and the radio you need.

## AllStar Link (analog, IP-based)

**The dominant analog linking system today.** Replaced IRLP in most operators' minds. Built on Asterisk (the open-source PBX) plus a thin layer of ham-specific glue.

**How it works:**
- Each node is a Linux computer (often a Raspberry Pi) connected to a radio.
- Nodes register with a central server and have a unique node number (e.g., 12345).
- A user on a node can DTMF-dial another node by its number, and the two nodes bridge audio in real time over the internet.
- Conferences (multiple nodes joined together) are common — `*3 41112` connects you to "the East Coast Reflector," for example.

**What you need to use it:**
- Find a local AllStar-equipped repeater, or run your own personal node (a Pi-based "hotspot").
- Once on the system, DTMF commands let you connect, disconnect, or join conferences.

**The strength:** open, free, audio quality is good (16 kHz analog), and an active node operator community.

The full treatment — the DTMF command set, node hardware/appliances, ASL3 vs HamVoIP, dashboards, and DVSwitch bridging — is in **§04-11**.

## EchoLink (analog, IP-based, computer-friendly)

Older protocol, still alive. Two ways to use it:

1. **Through a repeater** that has an EchoLink node attached. DTMF connect/disconnect commands.
2. **Through the EchoLink desktop or mobile app** — you talk into your laptop or phone microphone and the audio gets injected into the receiving repeater. Useful when traveling, but feels a bit like cheating.

EchoLink requires you to validate your callsign with the system once (proof you're a licensed amateur) before you can connect.

The full treatment — node types (`-L` / `-R` / app / conference), validation, connecting over RF and via the app, and latency etiquette — is in **§04-10**.

## IRLP (Internet Radio Linking Project)

The pioneer linking system, late 1990s. Mostly historical now — many IRLP nodes have been re-flashed as AllStar. The protocol is still alive in a few places. Same idea: numeric node IDs, DTMF dialing.

## D-STAR (digital — Icom-led)

The first widely-deployed digital voice mode for amateurs. Developed by JARL (Japan), commercialized by Icom.

**Codec:** AMBE (proprietary). Sounds digital — robotic, but clearly intelligible. Edges are sharp: signals either decode or don't.

**Network:** D-STAR repeaters connect to **gateways** on the internet, then to **reflectors** — virtual rooms where multiple repeaters meet. Reflector REF030 (US-East) and similar are popular.

**Routing:** Three flavors of how a call gets to its destination:
- **Local repeater** — just talking on the local D-STAR repeater
- **Gateway routing** — call goes to a specific other repeater (URCALL = repeater designator)
- **Callsign routing** — system tracks where the target callsign last appeared and routes to that repeater

**What you need:** an Icom (or Kenwood) D-STAR-capable radio. Cheaper now than at launch but still a noticeable price premium over plain FM gear.

> **Advanced —** D-STAR uses GMSK modulation at 4800 bits/sec for voice (3.6 kbit/s of AMBE codec + forward error correction). Channel spacing is 6.25 kHz, narrower than FM. The AMBE codec is patented and licensed only via the DV-Dongle/DV-AP hardware or radios with embedded AMBE chips — this licensing arrangement caused the open-source software-defined-radio community to develop the **Codec2** open codec as an alternative. Codec2-based digital voice (FreeDV) is rare on D-STAR but common on its own HF/VHF deployments.

## DMR (Digital Mobile Radio)

**The most popular amateur digital mode by user count.** DMR is a commercial standard (ETSI TS 102 361) that escaped into amateur radio because the gear is cheap (commercial-overflow Motorola, Hytera, BTECH, Anytone radios).

**Codec:** AMBE+2 (proprietary, an evolution of the same family used in D-STAR).

**Two-slot TDMA:** A single 12.5 kHz channel carries **two simultaneous conversations** by alternating in time. This is unusual for amateur radio and lets repeaters be twice as efficient.

**Network:** Most DMR repeaters connect to one of the big servers:
- **BrandMeister** — the largest network, completely amateur-driven.
- **TGIF Network** — popular alternative.
- **DMR-MARC** — older, more centralized.

**Talkgroups:** Instead of "reflectors" or "rooms", DMR uses **talkgroup IDs** — numeric IDs (3100 = North America, 91 = Worldwide, 31480 = Maryland Statewide, etc.). Your radio must be programmed with the talkgroups you want to monitor. To call a different talkgroup, you change the radio's TG selection.

**What you need:** a DMR radio (Anytone D878 is a popular ham choice; Motorola XPR series is commercial but works), a DMR ID (free, register at radioid.net), and patience to wade through the codeplug programming.

> **Advanced —** DMR's TDMA structure is precisely synchronized — slot 1 and slot 2 are 30 ms time slices. The radio always transmits on whichever slot is active for its talkgroup; this means two conversations happening on the same RF channel never interfere because they never transmit at the same time. The radio's TX is actually only on for half the time during a continuous transmission. This also means DMR transmitters are slightly more power-efficient than equivalent FM ones — a nice side effect.

## Yaesu System Fusion (C4FM)

Yaesu's digital-voice ecosystem. Uses C4FM (4-level FSK) modulation with the AMBE+2 codec.

**Distinguishing features:**
- **Auto-mode mix** — a Fusion repeater accepts both digital (C4FM) and analog (FM) signals on its input. Whatever you send in, the same modulation is broadcast on the output. Great for backwards compatibility.
- **Wires-X linking** — Yaesu's internet-link system, similar idea to AllStar but for digital. Linked rooms ("nodes") rather than reflectors or talkgroups.
- **YSF reflectors** — open-source alternative to Wires-X; many Fusion users prefer YSF for community ownership.

**What you need:** a Yaesu Fusion-capable radio (FT-70D, FT3DR, FTM-200/300/500, etc.).

## Hotspots — bringing the systems home

If you don't have a local D-STAR / DMR / Fusion repeater, you can run your own **personal hotspot** — a tiny low-power transceiver (often a Pi-based "MMDVM" hotspot, ~$100) that connects via the internet to the digital network and re-transmits audio in your shack at 10 mW so your handheld can talk to it.

Famous hotspot platforms:

- **Pi-Star** — the dominant open-source firmware. Supports D-STAR, DMR, Fusion, and more, all from one box.
- **WPSD** — fork of Pi-Star with additional features.
- **MMDVM-Cal** — calibration tool that comes with the firmware.

A hotspot lets one person talk to anyone on any of the digital networks without ever leaving the house. Doesn't really qualify as "amateur radio" in the over-the-RF sense — it's mostly internet — but it's how a lot of people stay on the air during commutes or in apartments.

## Picking a system

If you don't have a digital radio yet:

- **DMR** — cheapest gear, biggest network, steepest programming learning curve.
- **D-STAR** — Icom radios are pricier; programming is friendlier; network is smaller.
- **Fusion** — Yaesu radios; analog backwards compat is genuinely useful; network is somewhere between DMR and D-STAR in size.
- **AllStar / EchoLink** — analog only, works with any FM radio; learning curve is just DTMF commands.

Many local clubs only support one. Find out which **before** you buy gear.

## Common gotchas

- **Programming codeplugs** for DMR is tedious. Plan to spend an evening on it.
- **Talkgroup discipline** — DMR talkgroups have agreed quiet times and busy times. Listening before transmitting is harder than on analog because audio doesn't appear until decode succeeds.
- **Audio levels** — every digital mode is brutal about over-deviation. Hold the mic at the right distance or your audio will clip and decode badly.
- **Internet outages** kill linked-system QSOs instantly. Local RF still works; internet-routed contacts don't.

## See also

- §04-10 — EchoLink (the analog VoIP system in depth)
- §04-11 — AllStar Link (the open-source analog linker in depth)
- §04-01 — what plain-old repeaters do (linked systems are repeaters with more software)
- §14 — RFI (hotspots can interact with RFI in entertaining ways)
- §03 — Digital Modes (FT8, RTTY, etc.)
