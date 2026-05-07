---
id: 22-04
title: JS8Call
chapter: 22
section: 04
level: simple
status: draft
---

# JS8Call

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What it is

JS8Call is a derivative of FT8 designed by Jordan Sherer (KN4CRD) and released in 2018. It uses **the same 8-FSK modulation as FT8**, but with two crucial differences: messages can carry arbitrary text (not just fixed-format QSO primitives), and stations can **relay messages** through other stations. This makes it useful for conversational keyboard chat at FT8-grade weak-signal sensitivity, and for emcomm scenarios where direct contact between two stations isn't possible.

JS8Call has filled a niche FT8 leaves open: weak-signal **conversation** rather than weak-signal QSO logging. In emergency communications it's increasingly popular for moving traffic at low power without relying on infrastructure.

## How it works

| Property | Slow | Normal | Fast | Turbo |
|----------|------|--------|------|-------|
| Modulation | 8-FSK | 8-FSK | 8-FSK | 8-FSK |
| Slot length | 30 s | 15 s | 10 s | 6 s |
| Symbol rate | 1.5 baud | 6.25 baud | 12.5 baud | 25.5 baud |
| Bandwidth | 8 Hz | 50 Hz | 80 Hz | 160 Hz |
| Decoder threshold | ~−28 dB SNR | ~−24 dB SNR | ~−20 dB SNR | ~−16 dB SNR |
| Clock tolerance | ±5 sec | ±2 sec | ±1 sec | ±0.5 sec |
| Typical use | NVIS night-time | General QSOs | Routine ops | Crowded stations |

Underneath, JS8Call uses **JS8** modulation — a customization of FT8's 8-FSK with **four selectable speeds**. The "Normal" speed has the same 50 Hz bandwidth and 15 s slot as FT8, just with a different message protocol that lets you send free-form text. "Slow" mode is dramatically more sensitive (−28 dB SNR!) but at 4× the slot time. "Turbo" trades sensitivity for slot speed when bands are clear.

The protocol uses **15-second slots that can chain** — a long message is broken into multiple slots automatically. The decoder buffers the full text and shows it as one message once complete.

**Store-and-forward** is the killer feature for emcomm. JS8Call lets you queue a message addressed to a callsign that isn't currently on the air; the message gets relayed by intermediate stations that hear both you and the destination. This works without infrastructure and without coordination — any JS8Call station with relay enabled will participate.

> ⚙️ **Advanced —** JS8Call's relay protocol is opportunistic: each station listens for messages addressed to callsigns it has heard recently and forwards them. Loop prevention uses a TTL field; duplicate-message suppression uses message hashes. The protocol is described in JS8Call's GitHub repository under `lib/js8/protocol.h`. It's not a full mesh, but it's enough to deliver a message hop-by-hop across a region without internet.

## Why use it

- **Conversational at FT8 sensitivity.** Same weak-signal copy as FT8, but you can actually have a chat. Decodes signals 24 dB below the noise floor.
- **Store and forward.** Send a message to a callsign that isn't on right now. The network forwards it.
- **Group addressing.** Send a message to a group ("/ARES") that everyone in that group sees.
- **Built-in NVIS-friendly.** Slow mode is well-suited to night-time, weak NVIS skywaves used in regional emcomm.
- **No internet needed.** Compared to digital voice or D-STAR, JS8Call works completely off-grid.

When to skip it: conversations where speed matters (PSK31 is much faster), DX hunting (FT8 has the activity), or contests (no JS8Call contesting tradition).

## Operating

**Software:** JS8Call (its own app — uses the WSJT-X audio infrastructure but a different decoder).

**US calling frequencies (USB dial):**

| Band | JS8Call calling freq |
|------|---------------------:|
| 80m | 3.578 |
| 40m | 7.078 |
| 30m | 10.130 |
| 20m | 14.078 |
| 17m | 18.104 |
| 15m | 21.078 |
| 12m | 24.922 |
| 10m | 28.078 |

**Calling and being called:**

```
You:    /CQ K1ABC FN42
Other:  K1ABC: W2DEF FN31 GE
You:    W2DEF: GA MIKE TNX FER CALL UR RST 599 ES NAME JOHN QTH BOSTON
Other:  K1ABC: GE JOHN UR RST 599 ALSO ES NAME MIKE QTH PHILA
... (free conversation continues, optionally many slots) ...
You:    W2DEF: 73 ES BCNU AGN
Other:  K1ABC: 73
```

The "/" before a command (CQ, RELAY, FORWARD) is JS8Call syntax for a directive. Messages addressed to specific callsigns use "callsign:" prefix; group messages use "@GROUPNAME:".

**Speed selection:** Normal (15-sec slot, 50 Hz BW) is the default. Use Slow only on quiet bands at QRP / NVIS distances. Use Turbo only when nobody else is on the band — turbo signals splatter when too many overlap.

**Power level:** 5–25 W matches the spirit of the mode. JS8Call is for moving information efficiently, not contesting.

**Group addressing for emcomm:**

| Group | Purpose |
|-------|---------|
| @AMRRON | American Redoubt amateur emcomm group |
| @ARES | Amateur Radio Emergency Service traffic |
| @ALL | Anyone listening |
| @SOTA | SOTA activations and chasers |

Groups are created by convention — anyone can listen for messages on any group; sending to a group means everyone in the group sees it.

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| No decodes at all | Computer clock not synced | Enable NTP; verify clock to ±1 second |
| Some decodes but rare | Audio level wrong | Check JS8Call's signal-strength meter; aim for 30–50 dB |
| Messages get sent but never reach the addressee | Direct path doesn't work; relay not enabled | Enable relay in Settings; ensure intermediate stations are on |
| Long messages decode partially then garble | Slot speed too fast for current SNR | Switch to Slower speed; expect longer message delivery |
| JS8Call window shows turbo signals as noise | Turbo mode requires precise clock sync | Verify clock is within ±0.5 sec; or switch to Normal |
| Group message goes nowhere | Nobody in that group is on, or the group spelling is wrong | Check group name capitalization; "@ARES" vs "@ARES " (trailing space) |
| Network announces are noisy | JS8Call beacons every N minutes by default | Increase beacon interval in Settings, or disable |
| Audio splatter on TX | Same as PSK31 / FT8 — ALC compression | Reduce audio level until ALC is off |
| Clock drifts but station was working yesterday | Timezone change or DST shift | Re-sync NTP; check timezone setting |

## See also

- §22-00 — Digital modes overview (audio chain)
- §22-01 — FT8 (the parent protocol)
- §22-03 — PSK31 (faster but no FEC)
- §23 — Emergency Communications (where JS8Call shines)
- §23-04 — Major emcomm nets (some have JS8Call sub-nets)
