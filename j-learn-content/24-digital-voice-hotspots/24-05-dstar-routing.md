---
id: 24-05
title: D-STAR Routing — Reflectors and Callsign Routing
chapter: 24
section: 05
level: mixed
status: published
---

# D-STAR Routing — Reflectors and Callsign Routing

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## The two routing models D-STAR supports

D-STAR routes a call in one of two fundamentally different ways:

1. **Reflector linking** — a repeater (or hotspot) attaches itself to a *room* (the reflector) and everything heard locally gets retransmitted to the room. Cousins of DMR talkgroups, but with stateful linking.
2. **Callsign routing** — a single transmission is targeted at a specific callsign. The network looks up where that callsign last appeared and routes the audio there. No need for the destination to "join" anything first.

You will use both — they solve different problems.

## Reflectors

A **reflector** is a virtual room hosted on a server somewhere on the internet. Repeaters and hotspots can *link* to a reflector, and once linked, audio flows in both directions between the local RF and the reflector.

Three families of reflectors exist, each with its own server software and namespace:

| Family | Naming | Server software | History |
|--------|--------|------------------|---------|
| **REFxxx** | REF001 through REF999 (8-char, like REF030AL) | dstar_chat | The original. Operated by Robin AA4RC originally; many still active. |
| **XRFxxx** | XRF001 through XRF999 | dextra (open-source) | Community alternative to REFxxx. Largely supplanted by DCS. |
| **DCSxxx** | DCS001 through DCS999 | DCS (DigitalCallServer) | Modern, more reliable, the default for new reflectors in the 2020s. |
| **XLXxxx** | XLX001 through XLX999 | XLX reflector (multi-protocol) | Bridges D-STAR + DMR + YSF in a single reflector — see [§24-11](24-11-cross-mode-linking.md). |

A reflector is divided into **modules** — letters A through Z, though only A through F are commonly used. Each module is an independent room within the reflector. So `REF030 module C` and `REF030 module A` are separate conversations.

In transmissions, modules are encoded as the **8th character** of the URCALL field. Example URCALLs:

- `REF030CL` = link to REF030 module C (the L suffix means "link")
- `REF030AU` = unlink from REF030 module A (U suffix = unlink)
- `       I` = an info command (space-padded, I suffix returns the current link state)
- `       U` = a disconnect command (unlink from whatever's currently linked)

Famous reflectors that you'll encounter:

| Reflector | Owner / region | Note |
|-----------|----------------|------|
| **REF001 C** | UK | Long-running, English ragchew |
| **REF030 C** | US Multi-State | One of the most active US reflectors |
| **REF038 A** | West Coast US | |
| **DCS005 D** | Germany / EU | Heavy traffic, EU-wide |
| **XLX307 A** | New England | Multi-mode (cross-bridges DMR/YSF) |
| **XRF002 A** | New England (older) | Some clubs still maintain |

The convention is to write the reflector name with the module letter separated by a space: "REF030 C" or "DCS005 D". On the air, the suffix is spoken: "I'm on Reflector Thirty Charlie."

> **Advanced —** The three reflector protocols (REFxxx using dstar_chat, XRFxxx using dextra, DCSxxx using DCS) are wire-protocol incompatible — a repeater linked to REF030 cannot also be linked to DCS005 simultaneously, and audio from one does not flow to the other unless someone has set up a server-level bridge. XLX reflectors solve this by speaking all three protocols at once and bridging them transparently. The trade-off is server complexity — an XLX reflector is a larger, more brittle thing to operate.

## Linking a repeater to a reflector

When you transmit on a D-STAR repeater (or hotspot) with the right URCALL, the repeater itself sends a command to the reflector server: "link me to your module C." Once linked:

```
   Your radio
      │
      │  GMSK 4800 bps
      ▼
   D-STAR repeater (local RF)
      │
      │  D-Plus / DCS protocol over internet
      ▼
   Reflector (REF030 C)
      │
      │  Audio echoed to:
      ▼
   Every other repeater / hotspot linked to REF030 C worldwide
```

The repeater stays linked until:

- Someone (you, or another operator) sends an *unlink* command (URCALL `       U`).
- The reflector's idle-timeout fires (often 15 min of no audio).
- The internet hiccups and the link drops.

**Linking is a stateful, per-repeater operation.** A repeater is linked to exactly one reflector module at a time. If you link a repeater that already had an active link to a different reflector, the previous link drops. This causes the most common D-STAR cultural friction: someone joins their hotspot to REF030 C, gets a QSO going, and then someone else on the same repeater jumps to REF001 C and yanks the connection out from under them. See [§24-12](24-12-digital-voice-etiquette.md).

For **hotspots**, linking is just for you — you change reflectors freely without affecting anyone else. For **repeaters**, you're affecting every local user. Always announce before you link or unlink a repeater.

## Linking from your radio

The traditional way (and still the only way on older Icom radios): set URCALL to the appropriate 8-character link command and key the radio briefly.

| URCALL | What it does |
|--------|---------------|
| `REF030CL` | Link this repeater to REF030 module C |
| `REF030CU` | Unlink (specifically) from REF030 C |
| `       U` | Generic unlink — drop whatever link is active |
| `       I` | Info — radio reports current link state (voice prompt back) |
| `CQCQCQ` | Reset to plain CQ-on-this-reflector mode |

Modern radios (Icom ID-52A, ID-5100, IC-705) automate this with a reflector menu — pick "REF030 C" from a list and the radio sets URCALL for you. Pi-Star and OpenSpot dashboards have a click-to-link button.

## Callsign routing — D-STAR's signature feature

Reflector linking is great for *rooms* — group conversations. But D-STAR's original idea was point-to-point routing by callsign. This is what makes D-STAR feel different from DMR.

**How it works:** Every time you transmit, the D-STAR gateway notes "Callsign X was last heard at Repeater Y at timestamp Z." This is the **callsign tracking database**.

When someone sets URCALL to your callsign (left-justified, space-padded to 8 chars: `WM3J____`) and keys up, their gateway:

1. Looks up WM3J in the tracking database.
2. Finds the most recent entry (say, "WM3J last heard at K3PDR B, 4 minutes ago").
3. Forwards the audio frames to K3PDR.
4. K3PDR transmits the audio on its local RF.

```
   Caller: VK4XYZ (Australia)
   URCALL = WM3J____

         │
         │  audio + URCALL
         ▼
   Caller's gateway
         │
         │  "Where is WM3J?"
         ▼
   Callsign tracking DB ──▶ "K3PDR B, recent"
         │
         │  Route audio to K3PDR
         ▼
   K3PDR B (Maryland)
         │
         │  Local RF
         ▼
   WM3J's radio
```

You don't have to "subscribe" to anything. You don't have to be on the same reflector. You just have to have *recently transmitted* (within the database's memory window, usually ~30 minutes to a few hours depending on the gateway).

This is genuinely magical when it works. The catch: if you haven't transmitted in a while, the tracking entry is stale, and callsign-routed audio goes to the wrong repeater or nowhere at all. Hams who use callsign routing keep up a "ping" habit — keying briefly on their local repeater every so often to keep the tracking entry fresh.

> **Advanced —** The callsign-tracking database is *not* centralized for the whole D-STAR network — each gateway maintains its own view and periodically synchronizes with the upstream "trust server" (formerly run by Icom, now distributed). Sync lag is typically <60 seconds but can extend to several minutes during heavy traffic. If a callsign appears on two repeaters simultaneously (mobile operator driving between coverage zones), the tracking is non-deterministic — whichever gateway last updated wins.

## DPRS / D-PRS — position data over D-STAR

D-STAR's 1200 bps slow-data channel can carry **position reports** in a format called **DPRS** (D-STAR PRS, also written D-PRS). DPRS is essentially APRS-format position packets piggybacked on the D-STAR voice frame.

When you key up:
- Your radio embeds its GPS coordinates (and optionally symbol, course, speed, comment) in the slow-data channel.
- The gateway extracts the DPRS data and forwards it to **aprs.fi** via an APRS-IS gate.
- Your position appears on the global APRS map — without ever transmitting on the APRS frequency (144.39 MHz in the US).

This means a D-STAR HT with built-in GPS (ID-52A, TH-D75A) acts as a free APRS tracker any time you key up — no extra hardware. Set DPRS in the radio's menu, key up once, and check aprs.fi for your callsign.

> **Advanced —** DPRS packets are formatted in the slow-data channel as a NMEA-style sentence, e.g., `$$CRC4D08,WM3J,FN19,144.39,K3PDR B,*RST*`. The gateway parses this and constructs a standard APRS packet for forwarding. Some gateways add SSIDs based on the D-STAR routing context (e.g., `-7` for handheld, `-9` for mobile), others pass through whatever the radio sends. The lag from RF key-up to aprs.fi appearance is typically 5–30 seconds.

## Putting it together — a typical session

```
   Power on the radio. URCALL = CQCQCQ. Listen on local repeater.

   Want to talk to friends in California? Set URCALL = REF030CL, key briefly.
      Voice prompt: "Linked to REF030 C."

   Listen for 30 seconds. QSO in progress between W6 and K6 stations.

   Call CQ when clear. URCALL stays as CQCQCQ (the linking already happened).

   QSO ends. URCALL = "       U", key briefly. "Unlinked."

   Want to call a specific friend? URCALL = "W1ABC___", key up briefly.
      Audio routes to wherever W1ABC was last heard. No reflector involved.

   Done. URCALL back to CQCQCQ for local QSOs.
```

## See also

- [§24-04](24-04-dstar.md) — D-STAR protocol fundamentals
- [§24-02](24-02-dmr-talkgroups.md) — Contrast with DMR's talkgroup routing
- [§24-07](24-07-hotspot-pistar.md) — D-STAR setup on Pi-Star (which automates URCALL fiddling)
- [§24-11](24-11-cross-mode-linking.md) — XLX reflectors and cross-mode bridging
