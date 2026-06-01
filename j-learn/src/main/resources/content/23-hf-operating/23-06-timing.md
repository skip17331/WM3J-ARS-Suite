---
id: 23-06
title: Timing
chapter: 23
section: 06
level: mixed
status: published
---

# Timing

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## Why timing matters more than power

In a 100-caller pile-up, the difference between getting the contact and not getting it is rarely RF power. Adding 6 dB (going from 100 W to 400 W) shifts your S-meter reading by one S-unit — useful, but not decisive when 100 other stations are also at S9.

The decisive factors are usually *temporal*: did you transmit during the 1-second window when the DX was actively listening? Did your call begin and end before the next "QRZ"? Did you spot the new opening 30 seconds before the rest of the cluster?

This section is about the timing instincts that separate effective DX operators from frustrated ones.

## The QRZ window

A pile-up has a rhythm. The DX works one station, says "QRZ," listens 1–3 seconds, hears callers, picks one, says that call plus 5-9, the picked station says 5-9 back, the DX says QRZ again. The cycle is typically 8–15 seconds per QSO.

The *window to be heard* is the 1–3 seconds right after "QRZ":

```
Timeline of one DX QSO cycle:

t=0     "K1ABC 5-9"        ← DX gives report to the previous caller
t=2     "5-9 TNX 73"       ← K1ABC confirms
t=4     "QRZ"              ← DX opens the listening window
t=4-7   *callers transmit* ← YOUR WINDOW: 3 seconds
t=7     "WM3J 5-9"         ← DX picks the next caller
t=9     "5-9 TNX"          ← cycle continues
```

If your call starts at t=4 and ends at t=6, you're in the window. If your call starts at t=5.5 and ends at t=8, half your call is on top of the DX's response to the next picked station — you're QRMing the new contact. If your call starts at t=7, the DX has already picked someone; you're calling into a closed window.

**The skill:** start your call within 0.5–1 second of "QRZ," and have it finish within 2 seconds total.

Practice on non-rare DX first. The cadence is the same; the pressure is lower.

## Reading the cadence

Each operator has their own pile-up rhythm. The first 30 seconds of listening tells you:

- **Fast and clipped** (8 seconds per QSO): the operator is in "rate mode." Calls must be brief and immediate. No padding.
- **Slow and steady** (15+ seconds per QSO): the operator is being deliberate, often because pile-up control is hard or they're confirming carefully. You have more time to start your call.
- **Variable** (5–20 seconds): the operator is dealing with QRM, partial copies, or "stand by" moments. Watch each cycle.
- **Pauses every 5–10 QSOs**: the operator drinks water, logs catches up, or they're switching antennas. Don't fill the pause; wait.

A common newbie mistake is to *over-fill* the pause. The DX takes a 3-second breath, and 50 stations transmit into the silence — creating their own QRM as a result. Don't be one of the 50. If the DX hasn't said "QRZ," wait.

## The 30-second listen rule

Before you transmit on a new frequency for the first time:

1. **Listen for 30 seconds minimum** to understand the situation.
2. Confirm the DX is who you think they are (callsign verified).
3. Confirm whether they're running simplex or split, and where they're listening.
4. Confirm whether they're working a region restriction ("EU only," "by numbers").
5. Confirm the rough rate (how many seconds per QSO).
6. Only then call.

30 seconds feels like an eternity in a pile-up — you want the contact *now*. Resist. The cost of 30 seconds of listening is small; the cost of a botched first call (calling on TX frequency, calling out-of-region, missing the QRZ window) is much worse.

In contests, the rule shortens to 10–15 seconds: just confirm the exchange format and the running station's call. Contests have less ambiguity.

## Cluster-watching

The DX cluster is a real-time stream of DX spots posted by other operators. Modern cluster software (DX Spider, AR-Cluster, RBN's auto-spotting) can deliver a spot within 5–30 seconds of the DX appearing on the band. Logging software (N1MM Logger+, DXLab, Ham Radio Deluxe) consumes the cluster stream and overlays spots on a band map.

The 30–60-second window after a *brand-new* spot is the golden moment:

- The spot has only just been published.
- A small fraction of cluster-watchers have seen it yet.
- The DX may have called CQ for the first time.
- The pile-up hasn't formed yet.

If you can spot, tune, and call within 30 seconds, you might be one of 2–5 stations calling, not 200. Working a needed entity in those first 30 seconds is one of the most satisfying things in DXing.

How to be ready:

- **Keep the cluster open** during prime DX hours. N1MM's bandmap, DXLab's SpotCollector, or DXKeeper all display new spots prominently.
- **Filter the cluster** to your needed entities. Most cluster software lets you flag spots for entities you don't have on a given band-mode. Those spots highlight or beep.
- **Have macros ready.** "TX call once" should be one key. Pre-typed your CQ-reply message, the exchange, the thank-you. Don't be hunting for keys when the spot lands.
- **Auto-QSY.** Many loggers can auto-tune your rig to a clicked spot's frequency. One click → rig tuned → ready to call.

> **Advanced —** The **Reverse Beacon Network (RBN)** is a global network of automated CW/FT8/RTTY skimmers that decode signals and post them to the cluster within 5–15 seconds of each CQ. RBN spots flood the cluster during contests but are invaluable in normal operation — they detect openings before human operators do. Filter your cluster feed to "RBN only" for the freshest spots; filter to "human spots only" for verified DX without false positives.

## Don't call into a quiet pause

A pile-up that suddenly goes quiet is *suspicious*, not an opening. Possible explanations for sudden silence:

- The DX is dealing with a QRM problem ("stations on my TX frequency, off please").
- The DX is moving to a different band or going QRT.
- The DX is logging a catch and will resume in a few seconds.
- The previous QSO went unconfirmed and the DX is asking for repeat ("again, the call please?").

In any of these, transmitting into the silence makes things worse. The DX hasn't said "QRZ"; they're not listening for new callers. Wait until the rhythm resumes.

A useful test: if the DX hasn't spoken in 10+ seconds, they may be QRT or QSYing. Listen another 30 seconds; if no resumption, they've left. Tune to the next spot.

## Calling speed and timing on CW

CW pile-ups have similar timing but on a different scale. The DX sends at 25–35 WPM; calling stations send at similar speed. The QRZ window is 1–2 seconds in characters (a 4-character callsign at 25 WPM takes about 1.2 seconds).

CW timing tips:

- **Match the DX's speed.** If they're sending at 30 WPM, send back at 30 WPM. Don't slow them down by replying at 18.
- **Don't insert "DE" or "K".** Just send your call. "WM3J" is enough. No "DE WM3J K" — that's wasted dits.
- **Send your call once, cleanly.** Listen. If not picked, send once more after the next QRZ. Don't repeat-call across multiple cycles.
- **If the DX sends only your suffix** ("3J?"), they got partial copy. Send your full callsign twice, slowly: "WM3J WM3J".

## Calling speed and timing on FT8

FT8 has a fixed cadence — 15-second slots, even-numbered slots for one side, odd-numbered for the other. Your "timing" is whether you click the right station in the right slot:

- **Click the station's CQ during a *receive* slot** so your transmit happens in the *next* slot. WSJT-X's "Enable TX" toggles whether the next slot is yours.
- **Don't double-click after sending TX1.** The software will keep transmitting through the standard QSO sequence on its own.
- **If a fox/hound DXpedition is running**, set "Hound mode" in WSJT-X. The fox station can work 100+ hounds in one 15-second slot using multiple simultaneous frequencies. You don't pick the frequency; the software does.

Cluster integration matters in FT8 too — when a spot for a new band appears, you can QSY in 5 seconds rather than 30.

## When to slow down

Sometimes the right answer is *not* to call. Recognize these moments:

- **The DX is dealing with QRM.** They'll resume in a moment; calling during their fix attempt makes the QRM worse.
- **The DX is restricted to a region you're not in.** Wait for your rotation.
- **The pile-up is so dense that your call won't be heard.** Pause 5–10 minutes; let the casual callers complete; try again when the pile has thinned.
- **You're tired and making mistakes.** A botched call (transmitting on TX frequency, calling out-of-turn) costs more than the QSO is worth. Walk away for 30 minutes and come back fresh.

Patience compounds in DXing. The operators who work every DXpedition are not the loudest or the fastest; they're the ones who time their calls well and don't waste energy on bad cycles.

## See also

- [§22-05 — Pile-up Etiquette](../22-operating-practice/22-05-pile-up-etiquette.md)
- [§22-08 — Split-Frequency Operation](../22-operating-practice/22-08-split-frequency.md)
- [§23-05 — Working Rare DX](23-05-working-rare-dx.md)
- [§23-09 — Pile-up Strategy](23-09-pile-up-strategy.md)
- [§03-01 — FT8 / FT4](../03-digital-modes/03-01-ft8-ft4.md)
