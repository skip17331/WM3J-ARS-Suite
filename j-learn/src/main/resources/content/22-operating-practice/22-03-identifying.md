---
id: 22-03
title: Identifying — the 10-Minute Rule and §97.119
chapter: 22
section: 03
level: simple
status: draft
---

# Identifying — the 10-Minute Rule and §97.119

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

US amateur radio has one nearly-inviolable identification rule: **transmit your FCC-assigned callsign at least every 10 minutes during a transmission, and at the end of the transmission**. The rule lives in 47 CFR §97.119. It's both a regulatory requirement and a practical courtesy — listeners need to know who's on the frequency.

## The actual regulation

The rule, in a nutshell:

- During a contact: identify **at least every 10 minutes**.
- At the end of the contact: identify **once at the end**.
- Identification must use your **FCC-assigned callsign** in clear audio (or CW, or RTTY, etc., depending on the mode).
- A "transmission" is a single keying-up of the transmitter. A QSO consisting of multiple transmissions still requires ID every 10 minutes regardless of how many transmissions you've made.

The full text of §97.119(a) is short:

> Each amateur station, except a space station or telecommand station, must transmit its assigned call sign on its transmitting channel at the end of each communication, and at least every 10 minutes during a communication, for the purpose of clearly making the source of the transmissions from the station known to those receiving the transmissions.

## What "every 10 minutes" actually means

**Not** "every 10 minutes from the start of the QSO," but **"at any point in any 10-minute window."** If you ID at minute 5, you don't need to ID again until minute 15.

In practice, most operators ID every 4-6 minutes during a casual QSO — easy to track, never violates the rule, and satisfies the "tell people who you are" goal more naturally.

A simple way to remember: **ID at the end of every transmission longer than a few minutes**, and you'll never violate the rule.

## How to ID

The format is just your callsign:

```
Voice:  "WM3J"  or  "Whiskey Mike Three Juliet"
CW:     DE WM3J  or  WM3J K
RTTY:   WM3J
FT8:    [the WSJT-X protocol auto-IDs in every transmission]
```

For voice, phonetic or non-phonetic both work — phonetics are clearer in poor conditions but slower. Use phonetics when the receiver is straining; use plain English when conditions are good.

### When you're using a tactical callsign

In emergency or public-service operating, tactical callsigns ("Net Control," "Shelter Bravo") are common. The FCC ID rule still applies — your tactical callsign **does not satisfy** §97.119. You must transmit your FCC callsign at least every 10 minutes.

The standard pattern:

```
"Net Control to Shelter Bravo, do you copy. WM3J."
                                              ^^^^
                                       FCC ID at end
```

### Multiple operators on one station

If two people share a microphone (e.g., field day, public service event), only one operator's callsign needs to be transmitted. The trustee/control operator is the responsible party. If the **second operator** is the trustee, they ID with their callsign; otherwise the first operator's callsign covers the station.

### Repeaters and special-event stations

A repeater identifies itself periodically (typically every 10 minutes in CW or voice, automatically). This satisfies §97.119 for the **repeater** but not for **users** of the repeater. Each user must still ID their own callsign.

Special-event stations (1×1 callsigns like W1A, K9R) ID with their special call.

## When you don't need to ID (almost never)

Two narrow exceptions in §97.119:

- **Space stations** (amateur satellites in orbit) — the operating organization handles their identification.
- **Telecommand stations** controlling space stations — typically don't ID on the uplink, but the satellite IDs on the downlink.

For everyone else, ID is required.

## Identifying for contests

Contests typically tighten the cycle:

```
Contest exchange: "WM3J 59 003" (callsign + signal report + sequence number)
Standard exchange every contact, repeated for each new QSO
```

Each contact identifies you, so contest operators rarely have to think about the 10-minute rule — they ID every 30-90 seconds. The end-of-QSO ID is automatic with every contact.

## Identifying in pile-ups

Calling in a pile-up is a sequence of brief transmissions:

```
"WM3J"     (your call once)
[wait]
"WM3J"     (your call once)
[wait]
[DX picks you]: "WM3J 59"
"5NN, you're 59 too, 73 WM3J"  (their report, your report, sign-off)
```

Each transmission contains your call. The 10-minute rule is automatically satisfied because every transmission has your call.

## Identifying on FT8 / FT4 / Q65

The WSJT-X protocols include callsigns in **every transmission** automatically. The standard FT8 sequence is:

```
"CQ WM3J FM18"           (CQ + your call + grid)
"WM3J KK6XX -10"         (their call, your call, signal report)
"KK6XX WM3J R-08"        (your call, their call, R-signal report)
"WM3J KK6XX RR73"        (their call, your call, end-acknowledgment)
"KK6XX WM3J 73"          (your call, their call, end-of-QSO greeting)
```

Every transmission has your callsign, so the 10-minute rule is satisfied automatically. You don't need to manually ID — the mode does it for you.

## Identifying when you're listening but not transmitting

You don't ID if you're not transmitting. The rule covers transmissions only. Receiving silently for 30 minutes doesn't violate anything.

The exception: if you're on a controlled net and the NCS asks "WM3J, you still on frequency?" — you respond with your callsign. That's a transmission, and it's its own identification.

## Common identification mistakes

- **Forgetting the end-of-QSO ID.** "73 thanks for the contact" without your callsign at the end is technically a §97.119 violation. End every QSO with your callsign even if you've already said it twice in the previous transmission.
- **Tactical-only ID.** "Net Control" or "Shelter Bravo" without the FCC callsign at the end is not enough. The FCC callsign must appear.
- **Phonetic-only ID for digital modes.** Some operators speak their callsign on FT8 — this is unnecessary; the mode handles ID. But be aware that on FM/SSB voice integrations, phonetic spelling for clarity in poor conditions is good practice.
- **Transmitting tune-up without ID.** A tune-up burst with no callsign before or after technically violates the rule. The 2-second cost of "WM3J tuning, please stand by" is worth it.
- **Transmitting on a club station without authorization.** Club station callsigns require the club's trustee to be the control operator (or to have authorized you). Transmitting on K1AA when you don't have authorization is unlicensed operation.
- **Contest "5NN K"** without callsign. The contest exchange is "[your call] 5NN [serial]" — you must transmit your call. "5NN K" alone is not enough.

## §97.119 specifics worth knowing

- **Identification must be on the same frequency** as the transmission. You can't ID on 14.205 by transmitting your callsign on 7.180.
- **Identification must be intelligible.** A garbled or extremely high-speed CW callsign that can't be reliably copied is not legal ID.
- **Foreign portable identification**: when operating from a foreign country (under reciprocal arrangements), you typically prefix your callsign with the host country's prefix (e.g., "VE3/WM3J" for operating in Canada). The host country's rules apply.
- **Mobile / portable indicators**: optional. "WM3J/M" (mobile) and "WM3J/P" (portable) are sometimes used; not legally required in the US.
- **Special event 1×1 callsigns**: legal for special events; assigned with specific dates and purposes.

> ⚙️ **Advanced —** §97.119(b) covers details of various identification modes — phone, image, RTTY, CW. The 1989 amendment adopted the 10-minute / end-of-transmission rule that's now standard. Earlier rules required identification at the start AND end of transmissions and every 10 minutes; the start-of-transmission rule was relaxed because automatic identifiers and digital modes made it impractical. Special-event 1×1 calls were authorized in 1985 with a coordinated-with-FCC issuance system. Operating under §97.107 reciprocal arrangements requires identification using the host country's prefix; the FCC publishes lists of reciprocal countries.

## See also

- §22-01 — "Is the frequency in use?" (where ID first appears in the QSO)
- §21 — Emergency comms (where tactical callsigns coexist with FCC IDs)
- §04 — Repeaters (repeater ID is automatic but yours is not)
