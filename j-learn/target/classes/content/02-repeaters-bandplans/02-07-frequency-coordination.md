---
id: 02-07
title: Frequency Coordination
chapter: 02
section: 07
level: simple
status: draft
---

# Frequency Coordination

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

In every region of the country there's a volunteer organization called a **frequency coordinator**. Their job: keep two repeaters from being put on the same frequency in the same area. Their tool is a **coordinated repeater list** that you must consult — and respect — before lighting up a new repeater.

## Why coordination exists

VHF and UHF repeater frequencies are limited. The 2 m band has 116 possible repeater pairs (146.610–147.390 plus the high half of 147); 70 cm has more but the sub-bands vary by region. Every market has more clubs and operators than there are coordinated channels, especially in dense metropolitan areas.

Without coordination, anyone could buy a kit, point a yagi at a hilltop, and crash a frequency that another repeater 50 miles away has used for 20 years. The result would be mutual interference, broken QSOs, and angry email. Coordination prevents that by **assigning each new repeater a frequency that won't interfere with existing ones**, considering propagation, antenna height, ERP, and direction.

## Who the coordinators are

Each region of the US has its own coordinator. They are volunteer organizations recognized by the FCC under Part 97.205 — the rule that gives coordinated repeaters a regulatory advantage in any interference dispute.

Examples:

- **TASMA** — Two-Meter Area Spectrum Management Association (Southern California 2 m)
- **NARCC** — Northern Amateur Relay Council of California (Northern California)
- **MARC** — Mid-Atlantic Repeater Council (PA, MD, DE, parts of NJ)
- **FRC** — Florida Repeater Council
- **IRA** — Indiana Repeater Council
- And many more — every state has at least one, sometimes split by band or geography.

A complete list is maintained by the **NFCC** (National Frequency Coordinators' Council) at `nfcc.us`.

## What "coordination" gives you

When your repeater is coordinated:

1. **You have priority** in interference disputes under FCC Part 97.205. If an uncoordinated repeater in your coverage area causes interference, the FCC will side with you.
2. **You're listed** in regional repeater directories, on RepeaterBook, on the ARRL Repeater Directory.
3. **Other coordinators consult your assignment** when planning their own — your frequency stays clear in adjacent regions.
4. **You get a courtesy heads-up** when a new system comes up nearby that might interfere.

## What coordination doesn't do

- It doesn't grant you exclusive use — anyone can transmit on the input frequency from anywhere; only the *output* belongs to your repeater.
- It doesn't give you legal protection for technical problems with your own equipment (deviation, harmonics, spurious emissions).
- It doesn't relieve you of FCC-required identification.
- It doesn't override Part 97 — the FCC's rules supersede the coordinator's.

## How the coordination process works

Typical workflow when a club decides to put up a new repeater:

1. **Decide what you want** — band, mode, intended coverage area, ERP, antenna height.
2. **Apply to your regional coordinator.** A simple form: club name, technical contact, equipment specs, antenna location lat/lon and height, ERP.
3. **Coordinator does interference modeling** — checks the existing assignments in the area for any conflicts.
4. **They assign you a frequency pair and a tone.** This may take weeks or months in busy markets. The assigned tone reduces the chance of mutual interference between geographically adjacent repeaters that share a frequency pair.
5. **Build it. Test it. Get it on the air.**
6. **Stay in touch with the coordinator** if anything changes — moving the antenna, raising power, going off the air, etc.

Some markets are **closed** — there are no available frequencies, period. New systems wait for old ones to retire. Some clubs have been on a 70 cm waiting list for years.

## "Uncoordinated" repeaters

There's nothing illegal about an uncoordinated repeater (assuming you're following Part 97), but you give up your priority. If a coordinated user complains about interference from your uncoordinated system, you'll be asked to fix it or shut down — and the FCC will be on the coordinated user's side.

A few legitimate reasons a system might stay uncoordinated:

- **Personal/test repeaters at low power** that don't reach beyond a property line.
- **Temporary event repeaters** for parades, marathons, etc. — usually negotiated informally with the coordinator.
- **Hotspots and digital nodes** under 1 W — coordination doesn't really apply.

A few illegitimate reasons:

- "I don't want to wait."
- "The coordinator wouldn't give me my preferred frequency."
- "Coordination is just bureaucracy."

These show up regularly and create the interference problems that make coordinators necessary in the first place.

## Tones and coordination

Two coordinated repeaters can share an output frequency if they're geographically far enough apart that their signals don't overlap meaningfully. To distinguish them at the boundary where coverage might overlap, **each is assigned a different CTCSS tone**. Operators in the boundary area program both repeaters into memory with the matching tones. The coordinator decides which tone gets which repeater.

This is also why "**transmit decode tone too**" is a useful repeater feature — it lets users in the overlap region set their RX squelch to the local tone and not hear the distant repeater that happens to be on the same frequency.

## When coordination breaks down

Despite best efforts, conflicts happen:

- **Across coordinator boundaries** — two repeaters near a state line, each coordinated by a different organization. Coordinators usually talk and resolve these.
- **Long-range tropospheric ducting** — coastal repeaters occasionally hear each other through ducts that propagate over hundreds of miles. Tones save the day.
- **A coordinator who's stopped responding** — happens when a long-time volunteer steps down. The NFCC helps regions transition.
- **Disputes over coordination decisions** — typically resolved by the regional coordinator's grievance process; rarely escalates to the FCC.

## What you should do as an end user

1. **Use coordinated repeaters preferentially.** They're the well-engineered ones with the long-term operational discipline.
2. **Listen to the local repeater council net** if there is one — many councils run nets monthly, and they're a great way to learn what's happening in your area.
3. **Don't dox uncoordinated repeaters in public** — if you discover one, contact the coordinator privately first. Sometimes there's a story.
4. **Support your coordinator** — many run on donations from clubs whose repeaters they list.

## Where to look

- **NFCC** — `nfcc.us` — directory of regional coordinators by state.
- **RepeaterBook** — `repeaterbook.com` — public crowd-sourced list (which mostly mirrors coordinated databases).
- **Your state ARRL section page** — usually links to the local coordinator and any active repeater councils.

## See also

- §02-01 — what a repeater is
- §02-02 — tones and how they distinguish co-channel repeaters
- §07-rf-safety — the FCC RF exposure rules that affect repeater coordinators' job
