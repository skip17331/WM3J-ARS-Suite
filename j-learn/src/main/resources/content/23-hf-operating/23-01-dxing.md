---
id: 23-01
title: DXing
chapter: 23
section: 01
level: mixed
status: published
---

# DXing

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What it is

**DX** is shorthand for *distance* — historically "long-distance," nowadays "outside your own country" on HF. **DXing** is the pursuit of contacts with as many far-away places as possible. It's the oldest competitive game in amateur radio, predating contesting by decades.

The structure is simple: the ARRL maintains a list of **DXCC entities** — roughly "countries" but with a more elaborate definition (separate islands, dependencies, and political fragments each count as their own entity). As of 2026 there are 340 current entities. Work all 340 and you're on top of the Honor Roll. Most active DXers spend years getting from 100 to 200 entities, then decades getting from 200 to 340.

## DXCC entities, briefly

An "entity" is a political or geographic area that ARRL has decided counts separately. It's not a 1:1 with UN member states. Examples of why the list is weird:

- The U.S. has 7 entities (CONUS, Alaska, Hawaii, plus 4 territories).
- The U.K. has 7 entities (England, Scotland, Wales, Northern Ireland, Isle of Man, Jersey, Guernsey).
- Antarctica has 3 entities; the Indian Ocean has 12.
- Some entities (Bouvet, Heard, Crozet) are uninhabited and only on-air during DXpeditions every 5–10 years.

The "rare" entities are the uninhabited ones and the politically restricted ones (P5 North Korea hasn't been worked since 2002; KH7K Kure has been off-limits for 15+ years). The "common" entities are populated places with active amateur radio communities — Germany, Japan, Australia, most of Europe and South America are easy contacts most days.

## The awards ladder

| Award | Requirement |
|-------|-------------|
| **DXCC** | 100 confirmed entities |
| **DXCC Honor Roll** | Within 9 of current total (currently 331+) |
| **DXCC #1 Honor Roll** | All current entities |
| **5BDXCC** | 100 entities each on 5 bands (80/40/20/15/10) |
| **DXCC Challenge** | Sum of unique band-entities across 160–6 m; max 3,420 |
| **WAS** | All 50 US states (separate, easier than DXCC) |
| **WAZ** | All 40 CQ zones (CQ Magazine's award) |
| **WPX** | Worked Prefixes (call sign prefixes, e.g. K1, W2, JA1, JA2…) |
| **IOTA** | Islands On The Air — RSGB-managed, ~1,200 island groups |

Most US DXers chase DXCC first and pick up the others along the way. The "Mixed" endorsement counts any mode; "Phone" "CW" "Digital" "RTTY" endorsements break out by mode. A serious DXer collects multiple endorsements.

## Confirmation

A contact doesn't count until both stations confirm it. Two methods:

- **Paper QSL card** — the traditional method. You print a card, mail it (often via a bureau to save postage), and wait months or years for the other station's card to arrive. Sentimental and slow.
- **Logbook of the World (LoTW)** — ARRL's electronic confirmation system. Both stations upload their logs; matching contacts are confirmed instantly. Now the dominant confirmation method; required for any serious DXing pace.

Other systems (eQSL, ClubLog, QRZ.com logbook) exist but don't count toward DXCC. LoTW or paper are the only official paths.

> **Advanced —** LoTW uses X.509 certificates issued by ARRL after a postal verification of your address. The protocol is `tqsl`, an open-source signing tool. Your log is signed locally, uploaded to ARRL, and matched against the other station's signed log. Forged confirmations are essentially impossible because both endpoints sign independently. The system has confirmed over 1.6 billion QSOs since 2003.

## The two mindsets

DXers fall roughly into two camps:

**100 entities in a year.** The "active DXer" mindset. You operate hard during contests, chase every spot on the cluster, run digital modes overnight, and rack up entities quickly. By the end of year 1 you have basic DXCC. By year 3 you're approaching Honor Roll. Requires good antennas, time, and persistence.

**100 entities in a lifetime.** The "casual DXer" mindset. You work DX when you happen to hear it. You don't chase pile-ups. Over 20 years of weekend operating, the entities accumulate. You enjoy each contact more because each one took longer.

Neither is wrong. The casual mindset is more common and more sustainable. The active mindset gets you to Honor Roll faster but burns out a non-trivial number of operators along the way.

## Basic DX discipline

The mechanical skills overlap heavily with pile-up etiquette (see [§22-05](../22-operating-practice/22-05-pile-up-etiquette.md)). The DX-specific habits:

- **Watch the cluster.** A DX spot is a 30-second window before the pile-up forms. Spotted, tuned, called within 30 seconds gets you the contact more often than not. See [§23-06](23-06-timing.md) on cluster-watching.
- **Know which entities you need.** A DXer keeps a "needed list" — entities they don't yet have, by band and mode. When a spot comes in for a needed entity, that's a priority. Logging software (N1MM, DXLab, Ham Radio Deluxe) maintains the list automatically.
- **Be patient with the rare ones.** A DXpedition to a top-10 needed entity may run for 10 days. You don't have to work them in the first hour; you have to work them in the first week. Stress is unnecessary.
- **Honor the operator's announcements.** "Listening only to NA" means North America only. If you're in Europe, don't call. The operator will move to your region in due time.
- **Use the right band at the right time.** Working VK from Maine at midnight local is hard; working VK from Maine at sunrise (grayline) is much easier. See [§23-07](23-07-grayline-exploitation.md).

## Why people chase

DXing offers something most modern hobbies don't: the *contact itself* is the reward. There's no game lobby, no leaderboard pinging you, no streamer audience. It's just you, your station, propagation, and a stranger on the other side of the planet — agreeing to swap a few seconds of radio waves.

For some people that's the whole appeal. For others, the awards are a structured way to organize the chase. Either way, the underlying activity hasn't changed since 1920: get a signal out, hear one come back, log it, do it again.

## See also

- [§22-05 — Pile-up Etiquette](../22-operating-practice/22-05-pile-up-etiquette.md)
- [§23-05 — Working Rare DX](23-05-working-rare-dx.md)
- [§23-07 — Grayline Exploitation](23-07-grayline-exploitation.md)
- [§01-08 — Band Choice Right Now](../01-propagation/01-08-band-choice-right-now.md)
- [§20-01 — HF Band Plan](../20-band-plans/20-01-hf-band-plan.md)
