---
id: 24-06
title: Yaesu System Fusion / WIRES-X
chapter: 24
section: 06
level: mixed
status: draft
---

# Yaesu System Fusion / WIRES-X

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What it is

**System Fusion** is Yaesu's digital voice ecosystem, launched in 2013 as a direct competitor to D-STAR (which was Icom's territory) and DMR (which was ETSI's). It uses a modulation Yaesu calls **C4FM** (4-level FM, sometimes called Continuous 4-level FM) and the AMBE+2 codec.

Fusion sits as the third leg of the digital-voice tripod alongside DMR and D-STAR. By user count it's the smallest of the three globally, but it has a passionate user base — particularly in the US — and a few distinct technical advantages.

The internet linking side of Fusion is **WIRES-X** (Wide-coverage Internet Repeater Enhancement System version X), Yaesu's proprietary linking network, plus the open-source **YSF reflector** system as a community alternative.

## How it works

C4FM is a 4-level frequency-shift modulation: the carrier shifts among four discrete frequencies (corresponding to bit pairs 00/01/11/10) at a 4800 symbol/sec rate. Total bit rate is 9600 bps, same as DMR.

Where Fusion differs technically:

| Property | Fusion (C4FM) | DMR | D-STAR |
|----------|---------------|------|---------|
| Channel width | 12.5 kHz | 12.5 kHz | 6.25 kHz |
| Modulation | 4FSK (C4FM) | 4FSK | GMSK |
| Bit rate | 9600 bps | 9600 bps | 4800 bps |
| Multiplexing | FDMA (single QSO) | TDMA (2 slots) | FDMA |
| Codec | AMBE+2 | AMBE+2 | AMBE |
| Auto-mode mix | **Yes** | No | No |

The most distinctive Fusion property: **a single Fusion repeater can carry both digital and analog FM signals on the same input frequency, and re-broadcast each in its original modulation**. The repeater's controller detects the incoming modulation type and adapts. This is called **AMS (Automatic Mode Select)** in Yaesu terminology.

That's a big deal for clubs migrating from analog to digital — they can install a Fusion repeater and keep their analog FM users happy on the same machine.

## Narrow vs wide modes

Fusion offers **two voice modes**, selectable per transmission:

| Mode | Bits for voice | Bits for FEC/data | Audio quality |
|------|-----------------|---------------------|----------------|
| **DN (Digital Narrow / V/D mode)** | 3600 bps voice + 1200 bps data | Heavier FEC | "Standard" — robust to weak signals |
| **VW (Voice Wide / Voice FR)** | 9600 bps voice, no data | No FEC | "High-fidelity" — sounds notably better |

**DN mode** is the default and what most users run. It's a balance of audio quality, weak-signal robustness, and parallel data carriage (callsign, GPS, etc.). Sounds slightly better than D-STAR, roughly equal to DMR.

**VW mode** dedicates the entire 9600 bps stream to voice — no FEC, no data. The audio is much cleaner — listeners describe it as "FM-quality" or "near broadcast" — but it has **no error correction**, so as the signal weakens it degrades sharply with no graceful recovery. Most operators use VW only on strong simplex contacts or when working someone direct on the same repeater. It's not network-routable in most cases because internet links assume DN-mode framing.

> **Advanced —** Fusion also defines a **DW (Data Wide)** mode for 9600 bps pure data — used for image transmission and some experimental data applications. And **FR (Full Rate)** is the technical name sometimes used for VW. The mode encoding lives in the first frame's sync pattern, so a receiver can switch modes mid-QSO if the transmitter changes. Most repeaters are configured to operate in DN-only or AMS modes; pure VW links require both endpoints to be VW-capable, which limits VW's practical use to direct simplex.

## WIRES-X — Yaesu's linking network

WIRES-X is to Fusion what BrandMeister is to DMR — except it's run by **Yaesu**, the manufacturer, rather than by the community.

**Key properties:**

- **Centralized.** Yaesu operates the master server. There is exactly one WIRES-X network, run from Yaesu's HQ in Japan with regional mirrors.
- **Closed source.** The protocol is not publicly documented. The WIRES-X server software is not available to operators.
- **Room-based.** Conversations happen in named **rooms** — "America Link", "Texas Connection", "JR1WAA-ROOM", etc. — not numeric talkgroups.
- **Nodes** are individual repeaters or hotspots connected to WIRES-X. Each has a 5-digit node ID.
- **Rooms** are virtual conferences. Multiple nodes can join the same room; each room has its own 5-digit room ID (with the high digits typically distinguishing rooms from nodes).
- **Free to use**, but requires Yaesu hardware (or HRI-200 interface for a homebrew node).

WIRES-X grew from Yaesu's earlier WIRES-II analog linking system, hence the name. WIRES-X started as analog-only and added digital (Fusion) support around 2015.

### A typical WIRES-X room

Famous rooms:

| Room | ID (example) | Description |
|------|--------------|-------------|
| **America Link** | 21080 | Long-running US-wide room, English |
| **AmericaLink** | 27793 | (Slightly different — there are several Americas-themed rooms) |
| **TX-LINK** | 11203 | Texas regional |
| **WIRES-X DX Hub** | 28054 | International contacts |
| **JR1WAA-ROOM** | 27075 | Japan, Yaesu HQ's house room |

You connect a Fusion repeater to a room by DTMF or by the WIRES-X protocol from a connected node. From an FT3DR or FT-70D HT, you can:

1. Press the **DX** (or X) key to enter WIRES-X mode.
2. Search for rooms by ID or name.
3. Select and connect; the local repeater (if WIRES-X-equipped) joins that room.

## YSF — the open-source alternative

Many Fusion users prefer the **YSF reflector** system — an open-source community-run alternative to WIRES-X. YSF reflectors:

- Run on open-source software (the **MMDVMHost** and **YSFGateway** packages).
- Are operated by individual hams, not Yaesu.
- Use a numeric ID system independent of WIRES-X.
- Easily bridge to DMR talkgroups and D-STAR reflectors (via XLX or custom bridges).
- Are the preferred Fusion network for hotspots — Pi-Star and OpenSpot connect natively.

Famous YSF reflectors:

- **YSF 31226** — America Link (community version)
- **YSF 30277** — DODROPIN
- **YSF 21080** — (intentionally matches the WIRES-X America Link number for bridging clarity)

The relationship between WIRES-X and YSF is uneasy — Yaesu does not officially support YSF, but the protocol-level compatibility is fine because both use the same C4FM modulation. Most repeater owners run one or the other, occasionally both via a clever software setup.

## Fusion vs DMR — when each is preferable

**Fusion is better when:**
- You want a single repeater that serves analog FM and digital users (AMS / auto-mode mix).
- You want slightly better audio than DMR or D-STAR (especially in DN mode with strong signal).
- You prefer room names over numeric talkgroups (more like "AOL chatroom" than "fleet code").
- Your local club already runs Fusion.

**DMR is better when:**
- You want maximum spectrum efficiency (TDMA = 2 QSOs per channel).
- You want the largest network and the most active TGs worldwide.
- You're cost-conscious on the radio side.
- You want a mature, documented, multi-vendor protocol.

## Where Fusion lives

Mostly **VHF (2 m) and UHF (70 cm)** repeaters in the US. Heavy concentration in regions where Yaesu has strong dealer presence — Texas, southeast US, parts of California. The Northeast US leans more toward D-STAR and DMR.

Typical US Fusion repeater frequencies: 145.1xx / 145.7xx (2 m, -600 kHz offset) or 443.xxx (70 cm, +5 MHz offset).

## What you need to get on Fusion

1. **A Yaesu Fusion radio.** Options:
   - **FT-70DR** (HT, dual-band, basic Fusion) — ~$180
   - **FT-3DR** (HT, dual-band, GPS, touchscreen) — ~$420
   - **FT-5DR** (HT, dual-receive, color screen) — ~$500
   - **FTM-200DR / FTM-300DR / FTM-500DR** (mobile, various feature levels) — $300–$700
   - **FT-991A** (HF/VHF/UHF base, with Fusion) — ~$1200
2. **A WIRES-X node ID** (only if you're running a node — most users don't need one).
3. **Access to a Fusion repeater** or a hotspot with Fusion enabled (Pi-Star, OpenSpot).

No registration with a central network is required to *use* Fusion (unlike DMR's radioid.net requirement). Your callsign is embedded in the digital signal automatically.

## See also

- [§24-04](24-04-dstar.md) — D-STAR comparison
- [§24-01](24-01-dmr-overview.md) — DMR comparison
- [§24-07](24-07-hotspot-pistar.md) — Pi-Star setup for Fusion / YSF
- [§24-11](24-11-cross-mode-linking.md) — Bridging Fusion to DMR/D-STAR
- [§04-05](../04-repeaters-bandplans/04-05-linked-systems.md) — Linked-systems landscape
