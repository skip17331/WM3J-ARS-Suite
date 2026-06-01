---
id: 04-11
title: AllStar Link
chapter: 04
section: 11
level: mixed
status: published
---

# AllStar Link

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

AllStar Link is the open-source heavyweight of **analog** linking. Built on **Asterisk** — the same open-source telephone PBX that runs countless business phone systems — plus the ham-specific `app_rpt` module (originated by Jim Dixon, WB6NIL), it carries near-telephone-quality audio that sounds like clean FM, not the compressed warble of EchoLink. It has largely replaced IRLP and is the platform of choice for node operators who want full control. (For the easy-on-ramp analog system see §04-10 EchoLink; for the comparison of all linkers, §04-05.)

## What a node is

Every AllStar node is a **small Linux computer** — almost always a Raspberry Pi — running the AllStar software, with a **USB radio interface** carrying audio and PTT to a radio:

- The radio can be a **repeater**, a **simplex link**, or a low-power "**private node**" you talk to with an HT like a hotspot.
- You register for a **node number** at allstarlink.org; that number is your address on the network.
- Popular interfaces and turnkey appliances: **DMK URI**, **RIM-Lite**, and all-in-one nodes like **ClearNode**, **SHARI**, and **NodeYNode**.

## Connecting — the DTMF command set

`app_rpt` is driven by DTMF. The defaults (every node owner can remap them) are:

| Command | Action |
|---------|--------|
| `*3<node>` | **Connect** (transceive — talk and listen) to a node |
| `*2<node>` | **Monitor** a node (listen only) |
| `*1<node>` | **Disconnect** from a node |
| `*70` | List current connections |
| `*81` | Speak the time |

So `*3 2 5 6 0` connects you to node 2560. Large **hub / conference nodes** (regional or interest-group reflectors) are just node numbers many stations connect to at once.

> **Advanced —** AllStar audio is **uncompressed-grade** (μ-law / G.711 internally, ~64 kbit/s) — the audio-quality reason to prefer it over EchoLink's GSM. Because it's Asterisk underneath, a node is fully **scriptable**: macros, scheduled connects, telemetry, autopatch, and web dashboards (**Allmon2 / Supermon**) for monitoring and control. Modern installs run **ASL3** (the current Debian-package-based AllStarLink 3); the older **HamVoIP** Pi image is still widely used on Pi 2/3. Bridging to the *digital* networks (DMR, D-STAR, Fusion) and to EchoLink is done with **DVSwitch**, which is how a single node can tie analog AllStar to a DMR talkgroup (§24-11).

## When AllStar shines

- You want the **best analog audio** and a fully open, controllable platform.
- A club building a **hub-and-spoke** linked system across several repeaters.
- A personal **private node** so an HT around the house reaches the world.
- Bridging analog RF into the digital networks via DVSwitch.

## When it doesn't help

- You just want the simplest possible on-ramp from a phone app → EchoLink (§04-10) is friendlier.
- No internet — like every linked system, only local RF survives an outage.

## Common mistakes

- **Camping connected to a busy hub** with an idle node — you're keying every transmission on that hub out of your local repeater. Disconnect when done.
- **Audio levels** — set deviation/levels with the built-in tools; over-deviation sounds bad and can desensitize the link.
- **Security** — an exposed node with default passwords gets abused. Lock down the Asterisk manager / IAX ports and use strong credentials.
- **DTMF leaking onto the link** — mute or trap command tones so they don't blast connected nodes.
- **Router/NAT not configured** — inbound connections (IAX2, UDP 4569) never arrive.

## See also

- §04-05 — Linked systems (the overview/comparison of all of them)
- §04-10 — EchoLink (the simpler analog system; AllStar can bridge to it)
- §24-11 — Cross-mode linking (DVSwitch bridging analog ↔ digital)
- §14 — RFI (Pi-based nodes and their feedline/RFI quirks)
