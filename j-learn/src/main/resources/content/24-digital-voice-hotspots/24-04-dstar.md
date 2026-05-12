---
id: 24-04
title: D-STAR
chapter: 24
section: 04
level: mixed
status: draft
---

# D-STAR

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What it is

**D-STAR** — Digital Smart Technologies for Amateur Radio — was the first widely-deployed amateur digital voice system. It was designed in the late 1990s and early 2000s by **JARL** (the Japan Amateur Radio League) as a clean-sheet ham-specific digital protocol — explicitly *not* a re-purposed commercial standard. Icom commercialized it starting around 2004 with the IC-2200H, and Kenwood joined later with the TH-D74 / TH-D75.

D-STAR predates DMR's amateur adoption by about a decade. By 2010 it was the dominant amateur digital mode worldwide; by 2018 DMR had overtaken it on user count, but D-STAR remains a strong second and is the system of choice in many regions (especially Japan and parts of Europe).

## How it works

D-STAR voice uses **GMSK** (Gaussian Minimum Shift Keying) modulation at 4800 bits/sec inside a 6.25 kHz channel. The bit budget:

| Component | Bits/sec |
|-----------|----------|
| AMBE voice codec | 2400 |
| Voice FEC | 1200 |
| Data channel ("slow data") | 1200 |
| **Total over the air** | **4800** |

A few things to notice:

- **Channel is narrower than DMR.** D-STAR fits in 6.25 kHz; DMR uses 12.5 kHz (but TDMA-shared between two QSOs).
- **No TDMA.** D-STAR is conventional FDMA — one QSO per channel, full-duplex on each side.
- **Slow data channel.** D-STAR allocates 1200 bps to a parallel data stream — for callsign metadata, GPS position (DPRS), short text messages, and the routing header.

### The codec — AMBE (first generation)

D-STAR uses the original **AMBE** (not AMBE+2). Same DVSI lineage, just an earlier generation. The codec runs at 2400 bps and produces audio that's somewhat thinner and more obviously "digital-sounding" than DMR or Fusion. Side-by-side, listeners usually rate D-STAR audio third of the three major DV modes — but it's still clearly intelligible.

The audio is the **biggest weakness** people cite about D-STAR. Whether it matters depends on your taste — many users adapt and never hear it as a problem.

> ⚙️ **Advanced —** D-STAR specifies AMBE 2400+1200 — 2400 bps of voice plus 1200 bps of FEC. AMBE+2 used in DMR/Fusion runs at 2450 bps voice + 1150 bps FEC, plus a smarter post-filter. The audible difference is mostly in the smoothness of mid-band consonants ("sh", "ch", "f" sounds). AMBE is proprietary to DVSI, and Icom radios contain a licensed AMBE chip; open-source D-STAR transceivers were not legally possible for years, which slowed homebrew adoption. The patent has since expired (~2017), which is why Pi-Star and OpenSpot can now transcode D-STAR audio without buying a chip from DVSI.

### The data side

Beyond the slow-data channel embedded in voice, D-STAR also defines a **digital data mode (DD)** running at 128 kbps on 23 cm (1.2 GHz) — used for amateur packet over IP, somewhat like a slow Wi-Fi link. DD mode is rare and only Icom's 23 cm radios (ID-1, ID-5100 in DD mode) support it. The voice-mode "DV" is what 99% of D-STAR traffic is.

## Why callsign routing instead of talkgroups

Here's the philosophical fork in the road that distinguishes D-STAR from DMR.

**DMR's model:** Tag every frame with a talkgroup number. Repeaters subscribe to TGs. Audio routes to whoever subscribed. *Callsigns are metadata, not routing.*

**D-STAR's model:** Tag every frame with the *destination callsign or repeater*. The network tracks where each callsign last appeared (which repeater it heard them on) and routes the call there. *Callsigns are routing.*

In D-STAR, every voice frame has four fields:

| Field | Purpose | Example |
|-------|---------|---------|
| **MYCALL** | Your callsign | WM3J |
| **URCALL** | Destination (a callsign, a CQ flag, or a reflector command) | CQCQCQ or W1ABC |
| **RPT1** | The repeater you're using (input side) | K3PDR B |
| **RPT2** | The repeater's gateway (or another repeater you want to route to) | K3PDR G or REF030CL |

The MYCALL field is what makes D-STAR culturally distinct: every transmission is labeled with the operator's callsign, automatically, by the radio. When you watch a D-STAR repeater dashboard, the last-heard list shows real callsigns, not numeric IDs that have to be looked up.

The URCALL field is where the routing happens:

- `CQCQCQ` — a general CQ on the local repeater.
- `W1ABC___` — try to route this transmission to whatever repeater W1ABC last appeared on.
- `REF030CL` — link this transmission to Reflector 030, module C (with linking commands).

The "callsign tracking" is done by the **D-STAR gateway** server — a network entity that watches every voice frame everywhere and updates a database: "WM3J was last heard via K3PDR B at 14:32 UTC". When someone elsewhere sends a transmission with URCALL=`WM3J____`, the gateway looks up that record and routes the audio to K3PDR.

This is genuinely useful when you have mobile operators — you can call your friend by their callsign without knowing which repeater they're sitting on right now.

## Strengths vs DMR

- **Callsign-aware everything.** Last-heard logs, position data, routing — all keyed to callsign, not numeric ID. Easier for newcomers to interpret.
- **Single radio, single setup.** D-STAR radios have one DV mode. DMR radios have codeplugs full of channels, color codes, slots, TGs. The complexity gap is real.
- **Narrower channel (6.25 kHz vs 12.5 kHz).** Better spectrum efficiency per QSO.
- **Stable, mature ecosystem.** No competing networks fragmenting the user base the way BrandMeister vs IPSC2 does for DMR.
- **Better DPRS / position integration.** Built-in to the protocol; not a bolt-on.

## Weaknesses vs DMR

- **Audio quality is the weakest of the three major DV modes.** Subjective, but consistent in listener tests.
- **Radio prices are higher.** Cheapest new D-STAR HT (Icom ID-52A) runs ~$580. Cheapest new DMR HT runs ~$120. The price gap reflects Icom's near-monopoly position.
- **Smaller user base globally.** Fewer repeaters, fewer reflectors active at any moment.
- **Slower to evolve.** Icom controls the radio side; JARL controls the protocol side. New features arrive in years, not months.
- **No two-slot TDMA.** A single channel hosts a single QSO; DMR's two-slot trick gives twice the capacity per kHz.

## Where D-STAR lives

Mostly **VHF (2 m) and UHF (70 cm)** repeaters in the US and Europe. Japan has D-STAR on 1.2 GHz too (the DV+DD-mode region). A typical D-STAR repeater in the US might be on 145.270 / 145.870 MHz (2 m) or 442.4625 / 447.4625 MHz (70 cm).

```
   D-STAR ecosystem
   ────────────────

   Radio (Icom IC-705, ID-52A, Kenwood TH-D75)
      │
      │  GMSK 4800 bps in 6.25 kHz channel
      ▼
   D-STAR repeater (RPT1)
      │
      │  Optional: connected to the gateway (RPT2)
      ▼
   D-STAR gateway
      │
      │  Looks up URCALL → routes to:
      │     - same repeater (local CQ)
      │     - another repeater (callsign routing)
      │     - a reflector (REFxxx, XRFxxx, DCSxxx)
      ▼
   Destination
```

The reflector / linking part of D-STAR is its own topic — see [§24-05](24-05-dstar-routing.md).

## What you need to get on D-STAR

1. **A D-STAR-capable radio.** Modern choices: Icom IC-705 (HF + VHF + UHF, D-STAR + everything), Icom ID-52A (HT), Icom ID-5100A (mobile), Kenwood TH-D75A (HT, dual-band).
2. **Registration with the D-STAR network.** Visit **regist.dstargateway.org** (or your country's equivalent) and register your callsign. Approval is usually within 24 hours and is free. Without registration your transmissions appear on local repeaters but don't route through gateways.
3. **A local D-STAR repeater** or a **hotspot** with D-STAR enabled. Pi-Star handles D-STAR natively; OpenSpot too.
4. **An understanding of MYCALL / URCALL / RPT1 / RPT2.** Modern radios let you set these once and forget; older ones need manual reconfiguration per QSO.

## See also

- [§24-05](24-05-dstar-routing.md) — Reflectors, linking, callsign routing in detail
- [§24-01](24-01-dmr-overview.md) — The DMR alternative
- [§24-06](24-06-fusion-wires-x.md) — Yaesu Fusion, the third major DV mode
- [§24-07](24-07-hotspot-pistar.md) — Running D-STAR on a Pi-Star hotspot
- [§04-05](../04-repeaters-bandplans/04-05-linked-systems.md) — Where D-STAR sits in the linked-systems map
