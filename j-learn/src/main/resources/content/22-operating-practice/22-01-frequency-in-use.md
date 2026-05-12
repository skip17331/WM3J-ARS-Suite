---
id: 22-01
title: "Is the Frequency in Use?"
chapter: 22
section: 01
level: simple
status: draft
---

# "Is the Frequency in Use?"

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The first thing you say on a new HF frequency is **"is the frequency in use?"** — or its CW equivalent **QRL?**. The reason: someone may already be there, you just can't hear them. HF skip-zone propagation regularly puts two stations in QSO that neither of you can hear, and a third operator (you) plops down in the middle and walks all over them.

## The problem this solves

On VHF/UHF FM you can **hear** anyone using the channel within range — the carrier is loud, the squelch breaks. On HF SSB or CW that's not true. Three things can hide a QSO from you:

- **Skip distance.** Two stations at 1500 km can hear each other beautifully via F2; you sitting between them at 600 km hear nothing — they're in your skip zone.
- **One-way propagation.** Tropo, sporadic E, or auroral paths sometimes work in only one direction. A station 800 km north can be working a station 800 km south through you, and you hear neither.
- **Antenna nulls.** Your receive antenna may have a deep null exactly toward the station already on frequency.

If you transmit before checking, you're QRMing a contact that might be a 6 m grid square to Greenland. Asking once and listening solves the problem.

## The protocol

### Voice (SSB / FM)

```
"Is the frequency in use? This is WM3J."
[wait at least 5-10 seconds]
"Is the frequency in use? This is WM3J."
[wait again]
"This is WM3J QRZ; nothing heard, the frequency is clear."
```

Two queries with a pause between is the standard. The pause matters because the other end may be **finishing a transmission** when you call — they need a few seconds to start theirs and have it propagate back to you.

If someone responds:

```
Other: "The frequency is in use, please QSY."
You:   "Roger, WM3J QSY 73."
```

Move 3-5 kHz away and try again.

### CW

```
QRL?
[wait 5-10 seconds]
QRL?
[wait]
[no response] CQ CQ CQ DE WM3J WM3J WM3J K
```

The CW form is **QRL?** — "are you busy?" If someone is there:

```
Other: QRL DE W1ABC
You:   QSY 73 WM3J
```

Move and retry.

### Digital (FT8, FT4)

Digital modes occupy specific sub-bands and timeslots. The "frequency in use" check is different:

- **FT8 / FT4**: the entire 3 kHz audio passband is shared by hundreds of stations. The relevant question isn't "is the frequency in use" but "is this **audio frequency** (the slot you're transmitting on) in use." WSJT-X shows other stations' audio frequencies on the waterfall — pick a clear slot.
- **PSK31**: similar — pick a clear bin in the waterfall.
- **RTTY**: pick a clear spot, typically with at least 500 Hz of margin from neighboring signals.

For any digital mode, the protocol is **look at the waterfall first**. If a slot is occupied (current transmission visible), pick another slot. If a slot was used in the previous cycle but is empty now, it's likely available — the QSO has moved on.

## When to skip the check

There are situations where you don't need to ask:

- **You're calling a specific station** that just finished a contact ("WM3J, this is W1ABC who you just heard"). You've already heard the frequency, so the check is implicit.
- **You're answering a CQ.** The CQ-er has already established the frequency.
- **VHF/UHF FM** — the squelch tells you the channel is clear (or the carrier tells you it's not). No verbal check needed.
- **Calling on a busy contest.** During a contest, "is the frequency in use" is replaced by listening, then sending your callsign once. Long verbal checks waste time everyone is competing for.

But on routine HF SSB or CW, the QRL/frequency-in-use check is the universal first transmission.

## How long to wait

**At least 5 seconds, ideally 10.** Operators who get burned waiting too short:

- Other station is **finishing a transmission**. You hear silence for 2 seconds because they're in their PTT-release pause; you transmit; they transmit; collision.
- Other station is on a **slow CW frequency** (5-10 WPM). Their response to your QRL? takes a few seconds to send.
- Other station has **AGC recovery time** — high-power station hearing a strong local signal needs a few seconds before their RX is sensitive again.

Ten seconds feels like an eternity when you're eager to call, but it costs you nothing and avoids the worst-case interference.

## The check is also a test

Listening for 10 seconds doubles as a propagation check. You learn:

- **Background noise level** — your noise floor for this band/frequency.
- **Whether the band is open** — silence on 20 m at 0200Z UTC is meaningful; silence at 1400Z UTC is meaningless.
- **What's nearby** — adjacent QSOs, splatter from strong signals, etc.

Many seasoned operators sit on a frequency for a minute or two before transmitting, just listening. You learn the band by listening — and you'll never QRM a hidden QSO.

## Common mistakes

- **Not checking at all.** Just calling CQ on whatever frequency the VFO landed on. This is rude and frequently produces an irritated "the frequency is in use" response from someone whose QSO you just walked into.
- **Checking too briefly.** "QRL?... QRL? CQ CQ CQ" with no actual pause between the questions. The pauses matter; they're not formality.
- **Asking the question on the wrong band.** If you're using a non-resonant antenna and you don't realize you're radiating on 14 MHz when you think you're on 7 MHz, your QRL? goes out somewhere unexpected. Read the radio's display; verify you're on the right band before transmitting.
- **Being annoyed when told the frequency is in use.** It's not personal. The other station was there first. Move 3-5 kHz and try again — there's plenty of band.
- **Claiming the frequency.** "WM3J holding 14.205 for the next 30 minutes" is not how amateur radio works. You hold the frequency only as long as you're actively using it; if you go silent, someone else may legitimately take it.

> ⚙️ **Advanced —** Frequency reservation in amateur radio is informal and short-term. The IARU recommends a station that has been silent for **5 minutes** has effectively released the frequency. In contest operating this is much shorter — 30-60 seconds. The "QRZ" call (asking who's calling) is also a way to claim a frequency: by responding to your own CQ with "QRZ?" you signal that this frequency is yours and you're listening. In CW DX pile-ups, the DX station often "owns" a transmit frequency for hours by virtue of being the wanted signal that everyone is calling — but the same DX station has to occasionally re-call CQ to maintain the position.

## See also

- §22-04 — Calling CQ (after the frequency check passes)
- §22-05 — Pile-up etiquette (where the rules tighten)
- §19 — Q-codes (QRL, QRM, QSY)
- §01-06 — Sporadic E, TEP, Skip (why HF QSOs are sometimes hidden)
