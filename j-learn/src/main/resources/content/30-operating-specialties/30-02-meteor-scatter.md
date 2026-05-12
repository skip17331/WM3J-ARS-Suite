---
id: 30-02
title: Meteor Scatter Operating
chapter: 30
section: 02
level: advanced
status: draft
---

# Meteor Scatter Operating

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What meteor scatter is

Every day, the Earth sweeps up several tons of dust-sized particles from space. Each one enters the upper atmosphere at 30–70 km/s, heats to incandescence, and leaves behind a brief trail of ionized gas at 80–120 km altitude. That trail is a momentary plasma reflector — it will bounce a VHF signal back to Earth for somewhere between 100 milliseconds and a few seconds before it dissipates.

A meteor-scatter contact is therefore not a *continuous* QSO. It's a series of brief reflections — "pings" — each one carrying a fraction of the QSO's information. Stitch enough pings together and you complete a callsign + grid + report exchange between stations 500–2200 km apart, on bands where direct path is impossible.

The propagation physics is covered in §01-09. This section is about how to actually *operate* meteor scatter — the rig setup, the timing protocol, the calling discipline, and what a typical session feels like.

## Bands and ranges

| Band | Why it works | Typical range |
|------|--------------|---------------|
| **6 m (50 MHz)** | Best ping rate; large trail reflectivity at low VHF | 800–2200 km |
| **2 m (144 MHz)** | The classic MS band; long Yagis and digital modes routine | 500–2000 km |
| **70 cm (432 MHz)** | Marginal; only the brightest meteors give usable reflections | 200–1500 km (uncommon) |

If you're starting, **2 m with MSK144** is the standard answer. Antennas are reasonably sized, digital decoders pull signals out of −8 dB SNR pings, and the activity exists year-round.

## Random vs scheduled

Meteor scatter has two operating styles:

### Random ("random hour")

You park on a calling frequency (144.150 USB in IARU Region 2, 50.260 USB on 6 m) and call CQ in the MSK144 format. Anybody listening in your potential coverage area who hears a ping with your call in it answers. No prior arrangement. This is the dominant style during major showers when the ping rate is high enough that random encounters work.

### Scheduled ("sked")

You and another station agree by email, web forum, or chat in advance: "Tomorrow 0600–0700 UTC, 144.142 MHz MSK144, you transmit first." Both stations point antennas at the optimum sky volume halfway between them, set up the 15-second alternating sequences, and grind out the contact ping by ping. This is how rare-grid contacts and DX-record contacts happen.

The chasing community lives on **on4kst.com** (the KST chat) where stations call for skeds in real time. A typical KST message:

```
WM3J → DL9XYZ: How about 144.142 MSK144 at 0530z? First TX you, I'll listen.
```

## MSK144 — the modern protocol

MSK144 (Minimum Shift Keying, 144 baud) lives in WSJT-X. It's purpose-built for meteor pings:

- **15-second sequences.** One station transmits seconds 00–15, the other 15–30, alternating.
- **72-bit messages.** Short enough that a single 100 ms ping can carry an entire callsign + grid + report.
- **Continuous re-transmission within each sequence.** Your message repeats every ~70 ms throughout the 15 seconds. Any ping anywhere in the sequence catches one complete copy.
- **Decoder threshold:** about −8 dB SNR. Pings as short as 80 ms are routinely decoded.

The on-air sequence looks the same as any WSJT-X QSO:

```
TX1: CQ WM3J FN20
TX2: WM3J DL9XYZ JO62
TX3: DL9XYZ WM3J -03
TX4: WM3J DL9XYZ R-05
TX5: DL9XYZ WM3J RRR
TX6: WM3J DL9XYZ 73
```

Each TX takes up to 15 s; the contact completes when both stations confirm RRR or 73. A *good* contact takes 4–8 minutes. A marginal one can take 30–60 minutes of grinding before all six exchanges complete.

## Equipment baseline

| Component | Minimum | Comfortable |
|-----------|---------|-------------|
| Rig | Any modern transceiver with 144 MHz SSB and a sound-card interface | IC-9700, FT-991A, K3 with VHF module |
| Antenna | Single 9-element 2 m Yagi at 30 ft | 17-element long Yagi or 2× stacked Yagis |
| Power | 50 W | 500 W (legal limit in many areas) |
| Rotator | Az only | Az + el for combined MS / EME |
| Computer | Anything running WSJT-X 2.x | Same |
| Preamp | Not strictly required | Mast-mounted LNA improves marginal pings |

A 9-element 2 m Yagi with 50 W gets you on the air. You'll work all your nearby grids during the Perseids and Geminids; you'll miss the marginal contacts and the long-haul DX. The upgrade to a 17-element long Yagi and 500 W is the difference between catching pings and *making contacts*.

## The shower calendar

Major showers concentrate activity into narrow windows. Operators plan their station upgrades and vacation days around these dates.

| Shower | Peak (UTC) | Typical ZHR | Notes |
|--------|------------|-------------|-------|
| Quadrantids | Jan 3–4 | 110 | Brief 6-hour peak; cold-weather op |
| Lyrids | Apr 22 | 18 | Modest filler |
| Eta Aquariids | May 5–6 | 50 | Best in southern hemisphere |
| **Perseids** | **Aug 12–13** | **100** | The classic. Consistent year-to-year. Warm weather. |
| Orionids | Oct 21–22 | 20 | Modest |
| **Leonids** | **Nov 17–18** | **15 (storm years 1000+)** | Storm cycle ~33 yr — next high in ~2032 |
| **Geminids** | **Dec 13–14** | **150** | Highest non-storm ZHR. Cold. |

Between showers, **sporadic background** still produces 2–10 useful pings per minute on 6 m and 1–4 on 2 m if you're listening during the early morning hours (0400–0900 local) when the Earth's leading face encounters incoming particles.

## A real Perseids night

Here's what a typical 2 m Perseids run looks like for a moderately equipped station (17-el Yagi, 500 W, IC-9700):

```
21:00 local — Set up. Beam SSW toward common-grid pile in EM50-area.
22:00       — First pings appearing on 144.150 MSK144 every 30-90 s.
              Brief calls from WM3J start drawing answers from EM62, EM84.
23:30       — Pings every 5-15 s, some lasting 1+ s.
              Contacts completing in 4-6 minutes each.
01:00       — Peak hour. Major-grid stations chasing. 8-10 contacts in queue.
03:00       — 16 contacts logged. Shower fading; pings drop to 1/minute.
05:00       — Back to scattered random pings. End session.
```

A *serious* run during a shower peak might log 25–40 grid squares in one night. That's why grid-collectors plan their year around Perseids and Geminids.

## Calling discipline

A few things meteor-scatter operators expect:

- **Keep your sequence transmits running.** Even if you haven't heard a ping yet, your TX-on sequence puts your call in the air for the next caller's decoder. Silence wastes the shower.
- **Don't change message types mid-QSO.** If the other station is sending TX2 (your call + their call + grid), don't jump them to TX5 (RRR). Confirm in order.
- **Watch the chat.** on4kst.com sees the marginal copy you're missing. Frequently the other station will type "I have your -03 report, please send R-XX" before either of you successfully decodes the next ping.
- **Don't transmit between sequences.** A 15-s alternating sequence is the protocol. SSB voice in between (some old-timers still do it) interferes with everyone else's decoding.

## When meteor scatter shines

- **2 m grid hunting** in the continental US — the easiest way to add inland grids that have no other VHF activity.
- **Cross-border / cross-region** contacts between, say, Pennsylvania and Texas on 144 MHz that no other propagation could carry.
- **Filling gaps in VUCC** (VHF/UHF Century Club) — most fillers in the 400–800 mile range come from MS.
- **Contests** — the ARRL January VHF, June VHF, and September VHF contests overlap the Quadrantid and minor showers; serious VHF contesters work MS during these windows.

## When it doesn't shine

- **Daytime ragchew.** Pings are too brief, too random. MS is a digital-mode-only specialty in modern practice.
- **Real-time emcomm.** You cannot rely on getting a message through in any given 15-second window.
- **Casual operating.** It's a planning-and-patience activity; not something you fire up "for an hour after dinner" and expect contacts.

> ⚙️ **Advanced —** The geometry of a meteor-scatter path matters as much as raw equipment. The optimum reflection volume is the patch of sky where the great-circle path between your two stations intersects the 100 km meteor zone — typically at the midpoint of the path, elevation 5–15°. WSJT-X has a "MSK144 sked planner" that displays this point given two grid squares. Both stations beam at the *common volume*, not at each other. A meteor trail oriented perpendicular to your station's bearing reflects strongly; one oriented along the path reflects poorly. The "hot rock" effect — a single bright meteor producing a 5+ second overdense trail — can carry an entire QSO in one ping; experienced operators chase these by listening more than transmitting during the few minutes after a fireball.

## See also

- [§01-09 — Weak-Signal VHF/UHF](../01-propagation/01-09-weak-signal.md) — meteor-scatter propagation physics
- [§03 — Digital Modes](../03-digital-modes/) — MSK144 in the WSJT-X family
- [§30-03 — EME Basics](30-03-eme-basics.md) — the related "moonbounce" specialty
- [§30-04 — Tropospheric Ducting](30-04-tropo-ducting.md) — the other weather-driven VHF mode
- [§06 — Antennas](../06-antennas/) — long-Yagi designs for 2 m
