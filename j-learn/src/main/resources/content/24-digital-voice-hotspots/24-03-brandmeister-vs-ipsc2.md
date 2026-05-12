---
id: 24-03
title: BrandMeister vs IPSC2
chapter: 24
section: 03
level: mixed
status: draft
---

# BrandMeister vs IPSC2

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What they are

DMR is a *protocol*. To actually route a DMR call from your hotspot in Maryland to a repeater in Spain, somebody has to run a **network** — a set of internet servers that accept DMR frames from connected endpoints (repeaters, hotspots) and forward them according to talkgroup subscriptions. The two networks that matter today are:

- **BrandMeister** — the largest amateur DMR network. Open-source, distributed, user-modifiable.
- **IPSC2** — a fork-style alternative. More controlled, regional, often closer in spirit to the original commercial DMR networking model.

A third network — **TGIF Network** — has grown in the last few years and is functionally similar to BrandMeister but with a smaller, friendlier user base. **DMR-MARC** still exists but is largely deprecated for everyday use.

A hotspot or repeater connects to *one* network at a time (technically a Pi-Star hotspot can run two MMDVM instances, but for our purposes treat it as one). You pick a network when you set up the hotspot.

## BrandMeister — the open big one

Launched in 2015 by a group of European hams. The name is from the German *Brandmeister* (fire chief), reflecting its origin in a German DMR cluster. Today it's the largest amateur DMR network in the world.

**Key properties:**

- **Open architecture.** Anyone can run a BrandMeister master server. There are 30+ regional masters worldwide (BM 3101 = US East, BM 3102 = US West, BM 2621 = Germany, BM 2351 = Italy, etc.). They all interconnect.
- **User-modifiable.** As a registered user (logged in at brandmeister.network), you can edit *your own* hotspot/repeater's talkgroup subscriptions, hangtimes, static talkgroup list, etc., live from a web dashboard.
- **Talkgroup-heavy.** ~6000 talkgroups in active use globally. New TGs are easy to create — fill out a web form.
- **High-traffic.** TG 91 (Worldwide) is busy almost 24/7.
- **Lots of bells and whistles.** APRS gating (your DMR position appears on aprs.fi), GPS data, SMS gateways, voice prompts, dashboard with live last-heard list.
- **Free.** No fees, no premium tier.

**The web dashboard at brandmeister.network** is part of what makes BM so usable. You log in with your callsign (verified against your DMR ID), and you can:

- See every hotspot/repeater registered to you.
- Add/remove static talkgroups on each.
- Set the dynamic-TG hangtime (default 15 min, can extend to 60 min).
- View a live last-heard log of every transmission worldwide.
- Subscribe to "self-care" features like auto-disconnect if idle.

## IPSC2 — the controlled alternative

IPSC2 originated as a continuation of the older IPSC ("IP Site Connect", a Motorola networking protocol) adapted for amateur use. It runs alongside BrandMeister but with a different philosophy.

**Key properties:**

- **Regional / hierarchical.** IPSC2 servers are organized by country / region. Operators of each server set their own rules.
- **Sysop-controlled talkgroup list.** New talkgroups require sysop approval. The TG list is much smaller and more stable than BrandMeister's.
- **Better-curated experience.** Fewer rogue talkgroups, less noise, more "official" feel. Popular in Europe especially.
- **Cross-mode bridging is common.** IPSC2 servers often bridge to YSF and D-STAR reflectors out of the box; BrandMeister does this too but less consistently.
- **Lower traffic on average.** TGs on IPSC2 are less busy than equivalents on BrandMeister — sometimes a feature, sometimes a frustration.

IPSC2 doesn't have one global "the dashboard" the way BrandMeister does — each server has its own web interface. Common ones: `ipsc2-eu.de` (Germany), `ipsc2-it.de` (Italy), `ipsc2-uk.de` (UK).

## TGIF Network — the third option

**TGIF Network** (named for the talkgroup-information format, not the Friday) started around 2019 as an "after-hours" DMR network — looser, more US-centric, fewer rules. It runs DMR but also bridges into YSF and D-STAR readily.

- Smaller than BrandMeister but growing.
- US-oriented but global reach.
- Friendly to new operators, especially on TG 31665 (TGIF Prime Time Net).
- Web dashboard at **tgif.network**.

## How they interconnect (or don't)

Here is the part that confuses everyone:

**Most BrandMeister talkgroups do NOT bridge to IPSC2, and vice versa.**

Each network is its own walled garden. TG 91 on BrandMeister and TG 91 on IPSC2 are *separate rooms* with *separate conversations*. Your DMR ID is the same on both networks (radioid.net is the source of truth), but the audio doesn't cross over unless someone has explicitly set up a bridge for that specific TG.

Some specific bridges exist:

- **TG 91 (Worldwide)** is often bridged between BM and IPSC2 — but not always, and the bridging direction can be one-way.
- **Some regional TGs** are bridged by gentlemen's agreement between sysops.
- **TGIF ↔ BrandMeister** has more cross-bridges than IPSC2 ↔ BrandMeister, especially for English-speaking TGs.

The practical effect: if you switch your hotspot from BrandMeister to IPSC2, **you are talking to a partly different audience** even with the same TG number. The user lists overlap (the same operator may have separate hotspots on each), but the live conversations don't.

```
   ┌────────────────────────┐         ┌────────────────────────┐
   │   BrandMeister         │  X  X   │   IPSC2                │
   │   (~5000 hotspots/rpt) │ ←─/─/──→│   (~1500 hotspots/rpt) │
   │   TG 91, 3100, 31480…  │   bridges  │   TG 91, 3100, 268…    │
   └────────────────────────┘  selective └────────────────────────┘
              │                                       │
              │  Both networks pull from same         │
              │  radioid.net user database            │
              └─────────────────────┬─────────────────┘
                                    │
                              All ~250,000
                              registered DMR IDs
```

> ⚙️ **Advanced —** Bridges between networks are usually implemented as a third entity — a "reflector bridge" — that connects to both networks simultaneously and forwards frames in both directions for specific talkgroup ID mappings. The XLX reflector software (originally for D-STAR — see [§24-05](24-05-dstar-routing.md)) supports DMR bridging too, which is how some BM↔IPSC2↔YSF cross-traffic works. The bridge introduces ~50–100 ms of latency on top of the underlying network hops and occasionally introduces transcoding artifacts if the bridge changes codecs.

## Picking a network for your hotspot

A reasonable decision tree:

**Pick BrandMeister if:**
- You're in the US and want the biggest pool of contacts.
- You want lots of talkgroups (regional, special interest, club nets).
- You like the modern web dashboard with live status, APRS, etc.
- Your local club uses BrandMeister (most US clubs do).

**Pick IPSC2 if:**
- You're in Europe and your country's IPSC2 server has good local traffic.
- You want a calmer, more curated environment.
- You specifically need bridges to YSF/D-STAR that your local IPSC2 server provides.
- A net you care about is hosted on IPSC2.

**Pick TGIF if:**
- You want a friendly small-network experience for learning DMR.
- The TGIF Prime Time Net (TG 31665, evenings US Central) interests you.
- You like experimenting and TGIF has a particular bridge or feature you want.

You can switch networks any time. Pi-Star and OpenSpot both have a network selector in their config UIs. On Pi-Star: *Configuration → DMR Configuration → DMR Master* (drop-down lists all the public BrandMeister, IPSC2, and TGIF servers).

> ⚙️ **Advanced —** Pi-Star supports running two MMDVMHost instances on a duplex hotspot, but the more common dual-network trick is to use the **DMR-XLX** option in Pi-Star — your hotspot connects to BrandMeister for primary DMR routing and *additionally* to an XLX reflector for cross-mode bridging. This effectively gives you both a DMR network connection and a YSF/D-STAR bridge on the same RF channel.

## See also

- [§24-01](24-01-dmr-overview.md) — DMR protocol fundamentals
- [§24-02](24-02-dmr-talkgroups.md) — Talkgroup routing (numbers differ across networks)
- [§24-07](24-07-hotspot-pistar.md) — Configuring the DMR Master on Pi-Star
- [§24-11](24-11-cross-mode-linking.md) — Bridging DMR ↔ D-STAR ↔ Fusion
