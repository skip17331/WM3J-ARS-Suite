---
id: 24-08
title: OpenSpot Hotspots
chapter: 24
section: 08
level: mixed
status: published
---

# OpenSpot Hotspots

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What it is

**OpenSpot** is a line of self-contained digital-voice hotspots manufactured by **SharkRF** in Estonia (originally Hungary). Unlike Pi-Star — which is open-source firmware running on commodity Raspberry Pi hardware — OpenSpot is a closed-source commercial product. You buy a finished box, plug it in, and configure it through a polished web UI.

The current generation is **OpenSpot 4** (~$280) and **OpenSpot 4 Pro** (~$330). Previous generations (OpenSpot 1, 2, 3) are still in use but discontinued.

OpenSpot occupies a specific niche: hams who want a hotspot that "just works" without SSH, microSD cards, or Linux fluency. The trade-off is cost and lack of community-driven extensibility.

## How it differs from Pi-Star

| Feature | Pi-Star | OpenSpot 4 / 4 Pro |
|---------|---------|---------------------|
| Cost | $50–$120 (DIY) | $280–$330 (boxed) |
| Hardware | Pi + MMDVM HAT (you assemble) | Single sealed unit |
| Firmware | Open-source | Closed-source |
| Configuration | Web dashboard, also SSH-able | Web dashboard only |
| Updates | `pistar-update`, manual or auto | One-click in dashboard |
| Cross-mode transcoding | Via XLX reflectors (somewhat clunky) | **Built-in, automatic** |
| AMBE chip | Not included (transcoding via software) | Included (hardware transcoding) |
| Internal battery | None (USB powered) | **Yes (4 Pro)** — ~5 hour runtime |
| Display | Optional Nextion screen (~$40 extra) | Built-in OLED |
| Setup time | 30–60 minutes if novice | 10 minutes |
| Bandwidth use | Higher (network frames flow always) | Optimized (idle-aware) |
| Community support | Massive forum / Reddit / Facebook | Smaller, vendor-led |

The headline feature of OpenSpot — and the main reason people pay 2-3x Pi-Star prices — is **automatic cross-mode bridging** built into the firmware.

## Auto cross-mode bridging

OpenSpot can take a transmission in *any* of its supported digital modes (DMR, D-STAR, Fusion C4FM, NXDN, P25, M17) and transcode it to a *different* mode on the network side, transparently.

For example:

```
   You: transmit DMR on TG 3100 from your Anytone HT
        │
        ▼
   OpenSpot RF receives DMR frames
        │
        │  AMBE+2 → PCM audio (decode)
        │  PCM audio → AMBE (encode for D-STAR)
        ▼
   OpenSpot sends D-STAR frames to REF030 C
        │
        ▼
   Audio appears on REF030 C as a D-STAR transmission
```

Your radio thinks it's doing DMR. The destination network thinks it's a D-STAR call. The OpenSpot bridges the two by:

1. Decoding AMBE+2 from the DMR frame to raw PCM audio.
2. Re-encoding the PCM audio with the AMBE codec for D-STAR.
3. Wrapping the AMBE bits in D-STAR framing and sending to the gateway.

This works for any combination — DMR ↔ D-STAR, DMR ↔ Fusion, Fusion ↔ D-STAR. The OpenSpot 4 Pro includes a hardware AMBE chip; the OpenSpot 4 does the transcoding in software (slightly slower; uses more CPU; still functional).

The audio quality penalty for cross-mode bridging is real (see [§24-11](24-11-cross-mode-linking.md)) — you're double-codec-encoding the audio, which always loses something. But for many users the convenience is worth the loss.

> **Advanced —** The OpenSpot's AMBE transcoding implementation differs slightly between the 4 and 4 Pro. The 4 uses an open-source AMBE software implementation (legal because the AMBE patent expired in ~2017), while the 4 Pro uses a licensed AMBE chip from DVSI. Listeners sometimes report the 4 Pro sounds slightly cleaner on cross-mode transcoded audio, particularly on quiet passages where the open-source decoder's quantization noise is more audible. The 4 also occasionally fails to transcode certain unusual AMBE frames (control packets, dual-mode signaling) that the 4 Pro handles natively.

## Key differences in operating philosophy

**Pi-Star is mode-mode:** You pick a mode (DMR, D-STAR, Fusion) per channel on your radio, and the hotspot operates in that mode. Cross-mode requires reflector-level bridges (XLX) and is somewhat fragile.

**OpenSpot is bridge-bridge:** You configure a "bridge" in the dashboard that says "incoming DMR on TG 3100 → outgoing D-STAR to REF030 C". The OpenSpot does this on every transmission automatically. You can have multiple bridges configured and switch between them with your radio's PTT.

This makes OpenSpot particularly attractive to operators who want to use one radio (say, a DMR HT) but participate in conversations happening on other-mode networks. The OpenSpot makes the multi-mode access transparent.

## The "Connector" system

OpenSpot's configuration model uses **Connectors** — each Connector is a defined endpoint on a network:

- "BrandMeister TG 3100" — a Connector for talking on USA Nationwide
- "REF030 C" — a Connector for the D-STAR reflector
- "YSF31226 America Link" — a Connector for the Fusion YSF reflector
- "M17 #M17-USA" — a Connector for the open-source M17 network

You define Connectors in the dashboard, then assign each to a "Modem Mode" — what RF mode the Connector listens on. Switching Connectors is done either:

1. By keying a specific TG or DTMF code on your radio.
2. By the dashboard (click a Connector to make it active).
3. By time-of-day rules (the OpenSpot Pro supports scheduled Connector changes).

## When OpenSpot makes more sense than Pi-Star

- **You want hardware cross-mode transcoding** without messing with XLX reflectors and double-bridge configurations.
- **You travel and want a battery-powered hotspot.** OpenSpot 4 Pro has ~5 hours of runtime; Pi-Star needs an external battery bank (~$25 extra).
- **You don't want to maintain a Pi.** Updates are one-click. No microSD corruption surprises. No SSH.
- **You want a clean OLED display showing live status** without buying and configuring a Nextion screen separately.
- **Vendor support matters to you.** SharkRF has email support; Pi-Star has community forums.

## When Pi-Star is the better choice

- **Budget.** $50 vs $280 is real money, especially for an experimental tool.
- **Extensibility.** Pi-Star supports custom hooks, log forwarding, APRS gating, weather widgets, Telegram bridges — anything you can SSH into a Linux box and add.
- **Mode support.** Pi-Star sometimes adds new modes (M17, recently) before OpenSpot firmware does, because community contributors move fast.
- **You learn by tinkering.** Setting up Pi-Star teaches you Linux, networking, MMDVM internals, and the DV protocols — knowledge OpenSpot's polished UI hides from you.
- **You operate multiple hotspots.** Pi-Star scales cheaply; multiple OpenSpots get expensive fast.

## A common middle path: WPSD

**WPSD** (formerly known as W0CHP-PiStar-Dash) is a fork of Pi-Star with enhanced features:

- Better dashboard UI
- More cross-mode bridging built in
- Improved live logs
- Modern PHP / Bootstrap
- Some OpenSpot-like polish without giving up open-source

Many former Pi-Star users have switched to WPSD as a middle ground between vanilla Pi-Star and OpenSpot. Same hardware as Pi-Star; better software. Free.

## What you need to get going with OpenSpot

1. **An OpenSpot 4 or 4 Pro.** ~$280–$330 from SharkRF.com or one of their dealers (HRO, Gigaparts, others).
2. **A DMR ID** (radioid.net), if doing DMR.
3. **A D-STAR registration** (regist.dstargateway.org), if doing D-STAR.
4. **A 2.4 GHz Wi-Fi network at home** (5 GHz works on Pro but not all models).
5. **A radio.** Any DMR / D-STAR / Fusion HT or mobile.
6. **About 10 minutes** for the initial setup.

## Initial setup workflow (OpenSpot 4)

1. Power on. The OpenSpot creates a Wi-Fi network called `OpenSpot-xxxxxx`.
2. Connect your phone or laptop. Browse to `http://openspot.local/` or `http://192.168.50.1/`.
3. **Configuration → Wi-Fi → Add network → enter your home SSID + password.**
4. **Configuration → Modem → set RF frequency.** Default is 433.0 MHz; change to something appropriate (in the US, 438.800 is a common choice for 70 cm hotspots; avoid 446.000 which is the GMRS Channel 1 simplex).
5. **Configuration → Callsign + DMR ID + APRS settings.**
6. **Configuration → Connectors → Add new.** Pick a network (e.g., BrandMeister), enter server (e.g., BM_3101), pick a TG.
7. **Activate the Connector.** Done.
8. Reconnect your phone/laptop to home Wi-Fi. Browse to the OpenSpot's IP on your LAN (the OLED display shows it).

## See also

- [§24-07](24-07-hotspot-pistar.md) — Pi-Star (the open-source alternative)
- [§24-11](24-11-cross-mode-linking.md) — Cross-mode transcoding in detail
- [§24-09](24-09-duplex-vs-simplex-hotspots.md) — Duplex vs simplex (both Pi-Star and OpenSpot come in both flavors)
- [§24-10](24-10-ber-explained.md) — Audio quality and BER on hotspots
