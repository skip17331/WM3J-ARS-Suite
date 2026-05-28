---
id: 24-02
title: DMR Talkgroups
chapter: 24
section: 02
level: mixed
status: draft
---

# DMR Talkgroups

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What a talkgroup is

A **talkgroup (TG)** is a numeric ID that names a virtual "room" on a DMR network. Every voice frame on DMR is tagged with its talkgroup. Repeaters and hotspots subscribe to one or more talkgroups; when a frame arrives tagged with a subscribed TG, the repeater retransmits it. Frames tagged with TGs the repeater isn't subscribed to are silently dropped.

That's the whole routing model. DMR has **no callsign-based routing** the way D-STAR does (see [§24-04](24-04-dstar.md)). If you want to talk to a specific person, you both have to be on the same talkgroup at the same time.

Talkgroups are *numbers* — `91`, `3100`, `31480` — not names. The names you see on your radio's display are just labels you assigned in your codeplug. The number is what travels in the frame.

## How a call routes through talkgroups

A worked example. You're in Maryland, on a hotspot connected to BrandMeister, with your radio set to TG 3100 ("North America"):

```
   Your radio  ──[RF]──▶  Your hotspot  ──[internet/MMDVM]──▶  BrandMeister master server
                                                                       │
                                                                       ▼
                                                              Looks up: "TG 3100 — who subscribes?"
                                                                       │
                                       ┌───────────────────────────────┼───────────────────────────────┐
                                       ▼                               ▼                               ▼
                                Hotspot in CA                  Repeater in TX                  Hotspot in ON
                                (subscribed to TG 3100)        (subscribed to TG 3100)        (subscribed to TG 3100)
                                       │                               │                               │
                                       ▼                               ▼                               ▼
                                   Audio out                       Audio out                       Audio out
```

Every subscribed endpoint hears you. Nobody else does — a hotspot subscribed only to TG 91 won't relay your TG 3100 frame.

## Common worldwide talkgroups

These are the BrandMeister assignments — they are *almost* the same on IPSC2 / TGIF but check the network's wiki before assuming.

| TG | Name | Notes |
|----|------|-------|
| **91** | Worldwide | The big one. Always-on on most hotspots. Heavy traffic, English-dominant. |
| **92** | Europe | Region-wide EU TG |
| **93** | North America | (Slightly different from 3100 — 93 is the older "NA Calling" TG) |
| **123** | UK Cluster | UK-wide gateway |
| **3100** | USA Nationwide | The most-monitored US TG |
| **3101** | New England | US regional |
| **3102** | Mid-Atlantic | (DC/MD/VA/DE) |
| **3103** | South East | |
| **3104** | Great Lakes | |
| **3105** | Mountain | |
| **3106** | Western | |
| **3107** | Northwest | |
| **3108** | Pacific | (Hawaii, etc.) |
| **31[XX]** | US state | 31480 = Maryland, 31036 = Colorado, 31069 = New York, etc. (the last digits are the FIPS state code) |
| **310[XXX]** | US local / club | Many clubs run their own |
| **9** | Local | Per-repeater local-only TG (does not bridge to network) |
| **4000** | Disconnect | Special: drops your hotspot from any dynamic talkgroup |
| **9990** | Echo / Parrot | Records 30 sec and plays it back — useful for audio-quality testing |
| **5000** | Network status (info) | Voice prompts about server status |

The number range is structured: 1–9 is special (local, disconnect), 1–9999 is global/regional, 31XXX is US state, 310XXX is US local, and other countries have their own prefixes (3026 = Canada, 235 = UK, 268 = Ireland, 505 = Australia, 511 = New Zealand, etc. — the ITU country code stripped of the leading "2" or with the leading "+" stripped depending on convention).

> **Advanced —** BrandMeister assigns talkgroup numbers using a loose hierarchy: 1-digit = network primitives, 2-3 digit = continental/national, 4-5 digit = regional / state, 6-7 digit = club / local interest. Anyone can request a new talkgroup via the BrandMeister web portal — it's a free-form namespace. IPSC2 is more curated; talkgroup creation requires a sysop. Cross-network talkgroup numbers do not always match — TG 3100 on BrandMeister is "USA Nationwide", but the same number on TGIF Network has different semantics. Always check the network's TG list, not just the number.

## Static vs dynamic talkgroups

This distinction trips up newcomers more than anything else in DMR.

**Static talkgroups** are configured on the repeater/hotspot itself by the sysop (or by you, for your own hotspot). They are **always subscribed** — every frame for that TG gets relayed, whether anyone's listening or not. Audio for static TGs comes through your speaker constantly, all day, because the hotspot is always pulling them down from the network.

**Dynamic talkgroups** are subscribed *on demand*. You key up your radio on a TG that isn't static on the hotspot — your hotspot says "hey BrandMeister, please send me TG X frames too." BrandMeister adds your hotspot to that TG's subscriber list. Frames flow for **15 minutes** (the standard timeout). If nobody keys up on that TG for 15 minutes, BrandMeister un-subscribes you and the audio stops.

To explicitly drop a dynamic TG before the timeout: key up briefly on **TG 4000**. This sends a "disconnect all dynamic TGs" command to BrandMeister.

```
   Hotspot configuration              What you hear

   Static: 91, 3100, 31480           Always: WW, USA, Maryland talk
                                     Plus: anything you key up = added for 15 min
                                     After 15 min idle: dynamic TGs drop off
```

The practical effect:

- **Static TGs** = your "default playlist" of conversations.
- **Dynamic TGs** = what you actively join.

Most hotspot owners set 1–3 talkgroups static (their region + maybe Worldwide) and use dynamic for everything else. If you set 20 TGs static, your hotspot is constantly receiving audio and you'll hear overlapping conversations — chaotic.

## PTT-keyup vs always-on

Two ways to actually call on a talkgroup, depending on hotspot/radio configuration:

**PTT-keyup (dynamic-style):** Tune your radio to the TG. Hit PTT. Speak. Your hotspot dynamically subscribes (if not static), routes your audio to the network, and the TG is now live on your hotspot. Subsequent traffic on that TG flows to you for 15 minutes. This is the dominant model.

**Always-on (static):** Configure the TG as static in your hotspot dashboard. Audio flows continuously. No keying needed to "join". Useful for TGs you always want to monitor (your home state, club net).

## Talkgroup discipline

A few cultural norms — see [§24-12](24-12-digital-voice-etiquette.md) for the full version:

- **Listen first.** On DMR you can't "hear the band" the way you can on FM. Listen for 30+ seconds on a TG before keying up — there may be a QSO in progress that your hotspot isn't yet receiving because you just dynamically joined.
- **Don't park on TG 91 for a regional ragchew.** Worldwide is for short DX-style contacts, not 45-minute rambles. Move to a regional TG.
- **Identify with your full callsign every transmission.** Not just at the start — every PTT. The network tags your transmission with your DMR ID, but other listeners may not have that mapped to a callsign on their display.
- **TG 9990 (Parrot) for audio testing only.** Don't ragchew with Parrot — it just echoes back.

## See also

- [§24-01](24-01-dmr-overview.md) — DMR fundamentals
- [§24-03](24-03-brandmeister-vs-ipsc2.md) — Which network's TG list applies
- [§24-07](24-07-hotspot-pistar.md) — Configuring static TGs on Pi-Star
- [§24-12](24-12-digital-voice-etiquette.md) — Talkgroup etiquette in detail
