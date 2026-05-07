---
id: 02-09
title: Where to Find Repeater Frequencies
chapter: 02
section: 09
level: simple
status: draft
---

# Where to Find Repeater Frequencies

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The 2m and 70cm bands have hundreds of thousands of repeaters worldwide. Knowing **where to find them** is half the battle — once you have the frequency, offset, and tone, your radio's memory does the rest.

This section lists the major directories — online, printed, and on-radio — without hyperlinks. Type the URLs into your browser; bibliography entries are formatted for searching at your library or favorite bookseller.

## Online directories (general)

```
RepeaterBook              repeaterbook.com
   The de facto standard for North American repeaters. User-maintained,
   covers US, Canada, Mexico, plus international entries. Free; iOS and
   Android apps mirror the database. Filter by state, county, mode,
   power, network. Export to CHIRP-compatible CSV.

ARTSCi (RFinder)          rfinder.net
   Subscription-based ($10/year typical). Worldwide repeater database
   with weekly updates. Integrates with several cloning programs.

RadioReference            radioreference.com
   Frequency database with repeater listings, scanner frequencies,
   trunked-system maps. Premium subscription unlocks deep search;
   free tier is searchable but limited.

DMR-MARC                  dmr-marc.net
   DMR repeater registry. The authoritative directory for talkgroup
   listings, color codes, and network mapping. Free.

Brandmeister              brandmeister.network
   DMR network with worldwide talkgroup routing. Real-time map of
   active repeaters and connections. Free.

YSF Reflector Directory   register.ysfreflector.de
   Yaesu System Fusion / WIRES-X reflector listings. Free.

D-STAR Repeater Status    dstarinfo.com
   D-STAR repeater listings + reflector connections. Free.

QRZ Repeater Database     qrz.com/repeaters
   Repeaters by callsign or grid square. Free with QRZ account.

RepeaterDB                repeaterdb.com
   Crowd-sourced; smaller user base than RepeaterBook but useful
   for cross-checking.
```

## Online directories (regional / specialty)

```
US — ARRL Repeater Directory (online portal)
   arrl.org/repeater-directory
   ARRL membership required for full access.

US — Frequency Coordinators (regional)
   wia.org.au/                                         (Australia)
   ukrepeater.net                                       (UK / RSGB)
   nffa.de                                              (Germany)
   ncpra.com (North Carolina)
   tasrc.org (Texas)
   txasrc.org (Texas — separate from TASRC)
   ncia-arc.com (Northern California)
   nerac-org (Nevada)
   ... (each state has a frequency-coordination council;
        repeaterbook.com lists the coordinator per region)

Worldwide — IARU Region 1/2/3 repeater listings:
   iaru-r1.org   (Europe, Africa, Middle East)
   iaru-r2.org   (Americas)
   iaru-r3.org   (Asia-Pacific)

POTA / SOTA — for portable activations near you:
   pota.app                                              (POTA)
   sota.org.uk                                           (SOTA)
```

## On-radio (over-the-air) directories

Some repeater systems beacon their identity automatically:

- **APRS-equipped repeaters** beacon their callsign and frequency on 144.39 MHz (US) periodically. Decoded by APRS receivers / aprs.fi.
- **D-STAR / DMR / YSF** announce on each connection — your radio shows the gateway and reflector.
- **Many analog repeaters** identify with CW callsign every 10 minutes (FCC requirement).

## Printed sources (bibliography)

```
ARRL Repeater Directory
   ARRL — published annually since the 1970s.
   ISBN: latest editions; purchase from arrl.org/shop or any radio bookseller.
   Format: pocket-sized softcover; ~600 pages; lists US, Canada, Mexico
   repeaters with frequency, offset, tone, location, and net schedules.
   Strengths: portable, no internet needed, good index.
   Weaknesses: out of date by a few months at publication; updates require
   buying the next edition.

ARRL Travel Plus for Repeaters (CD-ROM, historical)
   ARRL — last published ~2017. Searchable database on disc;
   superseded by online tools. Useful only for archival references.

Radio Today / Radio Today Magazine "Repeater Roundup" features
   Print magazine; periodic features by region.
   ISSN-style serial; check arrl.org or used-magazine sellers.

Various Regional Frequency Council Directories
   State-specific or council-specific publications.
   Examples:
     - "Northern California Repeater Directory" (NARCC)
     - "Texas Frequency Coordination Directory"
     - "New England Repeater Council Directory"
   Publication frequency: usually annual.
   Sources: regional frequency coordinator websites or club newsletters.
```

## Club newsletters and websites

Local clubs are the best source for **active** repeaters in your area:

- The repeaters listed in directories may be silent (ID'ing but unused) or **off the air** despite still showing up.
- A local club's "what's on tonight" net list tells you which machines are actually used.
- Search "[your county/region] amateur radio club" to find them.

> ⚙️ **Advanced —** Repeater "activity index" is a fuzzy concept. RepeaterBook crowd-sources usage-frequency reports, but they're sparse. The most reliable signal is the club's net schedule: a repeater that hosts a weekly net is by definition active. Repeaters with no net listing in the last 12 months are probably silent. RepeaterBook flags some as `OFF-AIR`; trust those flags.

## How to use this information

When you arrive in a new area:

1. **Check RepeaterBook** for nearby repeaters — typically 2m and 70cm.
2. **Filter by your mode** — analog FM, DMR, D-STAR, Fusion, or P25 depending on your radio.
3. **Look for active nets** — a repeater hosting a net is reliably manned.
4. **Note the tone (CTCSS/DCS) and offset** — these often vary by region.
5. **Listen first** before transmitting. Ten minutes of silence isn't necessarily a dead repeater; it might just be quiet.
6. **Identify yourself** correctly per FCC rules: callsign at the start, every 10 minutes during, and at the end.

## See also

- §02-00 — Chapter overview
- §02-01 — What is a repeater
- §02-02 — Offsets, tones, CTCSS, DCS (the technical fields each directory lists)
- §02-04 — Simplex calling frequencies (when no repeater is appropriate)
- §02-05 — Linked systems (DMR, Fusion, D-STAR, AllStar networks)
- §02-07 — Frequency coordination (who maintains the lists)
- §23-04 — Message forms (NTS / ARES use repeater nets)
