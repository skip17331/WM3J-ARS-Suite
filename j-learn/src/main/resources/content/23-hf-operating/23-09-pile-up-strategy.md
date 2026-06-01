---
id: 23-09
title: Pile-up Strategy
chapter: 23
section: 09
level: mixed
status: published
---

# Pile-up Strategy

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What this section is (and isn't)

This is the *tactical* side of pile-ups: how to choose your TX frequency, how to time your call within the pile-up cycle, how to read the operator's behavior, and when to give up and come back later. It assumes you've already read [§22-05 Pile-up Etiquette](../22-operating-practice/22-05-pile-up-etiquette.md) and know the *rules* — don't call on TX freq, don't repeat-call, listen first. That's the etiquette. This is the strategy.

If you skipped §22-05, go back. Strategy without etiquette is just being a loud lid.

## The picking process — what's actually happening on the DX side

Understanding what the DX operator is doing tells you what to do to be picked. They are:

1. **Tuning slowly across the listen range** (or jumping to specific spots within it).
2. **Hearing whatever pile-up signals are in their current narrow passband** (typically 200–800 Hz on CW, 2.4 kHz on SSB).
3. **Selecting one callsign that's intelligible**, either by waiting until other callers stop or by picking through a thinner part of the pile.
4. **Sending that callsign + signal report.**

They are *not* picking based on signal strength alone. They pick based on *intelligibility*. A clean S5 signal in a quiet spot beats an S9 signal in a wall of S9s. The skill is not "get louder"; it's "get into a position where the operator can resolve your call."

## Picking your TX frequency

When the DX announces "5 to 15 up," they're listening across a 10 kHz range. Most callers do one of two things:

- **Default to the bottom of the range** (5 up exactly). Newbie default; very crowded.
- **Default to the middle** (10 up). Also crowded.

Both are bad. The DX is rotating their listening focus, but the *density* of callers is concentrated at the obvious spots. To stand out, you want a spot where there are *fewer* callers.

Tactical options:

1. **Listen across the listen range** before transmitting. Use the rig's "dual watch" or "split-listen" function to sample the pile-up. Find a spot where only 1–3 callers are active at any moment.
2. **Pick a non-round-number kHz.** 14.207.5 (off-grid) often has fewer callers than 14.208 (on-grid). Most rigs auto-tune to whole-kHz steps; running half-kHz off-grid breaks that herd behavior.
3. **Avoid the spot the previous picked station was on.** If the last QSO worked someone at 5 up exactly, every newcomer will pile onto 5 up to mimic. Move to 8 up or 12 up.
4. **Watch the panadapter.** If you have an SDR display (modern rigs all do), the pile-up shows up as a visible cluster of carriers. Look for gaps. Transmit in a gap.

> **Advanced —** A common DX operator habit is to **walk** their listening focus — start at the bottom of the range, sweep up over 30 seconds, sweep back down. If you can detect this rhythm (by listening for the previous picked callsigns and where they were in the range), you can predict where the operator's "ear" will be in 30 seconds and pre-position your TX there. This is the elite skill of pile-up reading; very few operators do it consciously, but the best ones do.

## Timing within the QRZ window

The pile-up cycle is described in [§23-06](23-06-timing.md). For pile-up strategy specifically:

- **Drop your call within 0.5–1 second of "QRZ."** Late callers (3+ seconds late) hit a closed window.
- **Send your full callsign once, cleanly.** Phonetics on SSB; one transmission only.
- **Don't add padding.** No "calling DX," no "ZL5 portable," no extra characters. The DX has 10–20 callers in their ears; the shortest, cleanest call wins.
- **Listen on your TX frequency** during the half-second after your call. If the DX comes back with a partial that matches you, send your full call again immediately. If they come back with someone else, stop transmitting.

The math on call length: a 4-character callsign at SSB cadence takes about 2 seconds; at CW 25 WPM it takes 1.2 seconds. The DX's listening window is 2–4 seconds wide. You have time for one call, not two.

## Reading the operator's cadence

Within 30 seconds of listening, you should know:

- **Average QSO duration** (8s = fast; 15s = moderate; 25s = slow).
- **Does the operator take tail-enders or insist on full QRZ cycles?**
- **Are they working any restriction** (by numbers, by region)?
- **Are they tolerant of partial copies or do they demand full callsigns?**

Each of these changes your tactics. A fast operator (8s/QSO, no tail-enders, accepts partials) wants speed and brevity above all — you must drop a clean call within 1 second of QRZ. A slow operator (25s/QSO, demands full repeats, polite) gives you more room to call cleanly but requires more patience.

## When the DX gets partial copy

A frequent moment: the DX hears part of your call and asks for the rest.

```
DX: "the W-M-3 station, again?"
```

If your call is WM3J, that's you — respond:

```
You: "Whiskey Mike Three Juliet, WM3J"
```

If the partial doesn't match you (e.g., they said "W3" but you're WM3J — "W3" matches W3 prefixes, not WM3J), *don't call*. The DX wants a specific station; jumping in adds QRM and tells the operator you don't listen.

Common partial-copy strategies:

- **Partial matches you exactly:** call back twice, slowly, phonetically.
- **Partial overlaps but isn't you:** stay quiet. The intended station will respond.
- **Partial is ambiguous:** wait one cycle. If the DX repeats with more detail ("Whiskey Mike, again?"), respond.

## Callsign-fragment listening

Conversely, when the DX picks someone, *listen* to whose callsign they say. If it matches your prefix or suffix in a region restriction, your turn is approaching. If it matches stations in a different region, the DX is working that region; wait.

A subtle skill: the DX may pick a station whose callsign sounds *very similar* to yours. You then need to *not* call (it's not your turn) but be ready when QRZ comes again. Operators sometimes confuse near-matches; the DX may say your call by accident — don't acknowledge unless they confirm.

## When to move TX frequency

Three signs to retune your TX:

- **You've called 4–6 times on the same kHz with no response.** The DX is hearing other callers more clearly. Move 1–2 kHz.
- **A bigger station has landed on your kHz** (say, a 1500 W EU station has parked at your 14.210 spot). You'll lose the comparison; move away.
- **The DX has audibly worked several stations on different kHz spots than yours.** Their listening is now away from your spot. Move toward where they're picking.

Each move resets your "newcomer" status — the DX hears a fresh signal in a fresh spot, which can break the cycle of being passed over.

## When to give up and come back later

DXing rewards patience but also rewards knowing when to stop. Signs to walk away:

- **You've called for 30+ minutes with no contact and no partial copy.** Propagation may be against you (different path angle than the DX is favoring) or your spot is wrong.
- **The DX is restricting by region and won't be in yours for a while.** No point burning energy; come back at the right rotation.
- **You're tired and starting to make mistakes** (calling out-of-turn, transmitting in the wrong VFO, mistuning split). Walk away for 30 minutes; come back fresh.
- **The band is closing** (signals fading, S-meter dropping). The DX may be QSYing or going QRT.
- **You don't actually need this entity on this band/mode.** Check your needed list. If it's not new for you, let other operators work them.

DXpeditions run for days. Casual pile-ups thin out within hours. The "now or never" feeling is almost always wrong. Come back in an hour.

## A worked example

Scenario: 3Y0J on 20 m SSB, listening 5–15 up, working stations rapidly. You're in the US East Coast with 100 W and a dipole.

**Minute 0–2:** Listen. Hear them work K1ABC (5 up), W4DEF (8 up), VE3GHI (5 up, again — bottom is crowded), JA1JKL (12 up — long-path JA!).

Pattern: the DX is rotating across the range; they're not biased to bottom or top; they're tolerant of long-path JA. The range is open across the full 10 kHz.

**Minute 2:** Notice the bottom of the range is dense with NA callers. The middle (8–10 up) has fewer. The top (12–15 up) is mostly long-path Asia. Pick 9 up — quieter spot, mid-range, off the dense bottom.

**Minute 2–5:** Call at 9 up, once per QRZ cycle. Three calls; not picked. Listen to who's being picked: the DX is now working 6 up, 7 up, 8 up — the listening focus has moved up toward you.

**Minute 5:** Call at 9 up at QRZ. The DX comes back: "the WM3 station, again?" Partial copy. Reply: "Whiskey Mike Three Juliet, WM3J." DX: "WM3J 5-9, QRZ." You're logged.

**Minute 5:30:** Reply briefly. "5-9 thanks 73 WM3J." Done. Leave the frequency.

Total: 5 minutes of active calling for one of the rarest entities in the world. Patience, position, timing — not power.

## Some things that don't work

- **More power.** A 1500 W signal at S9+30 doesn't beat a 100 W signal at S7 in a different kHz spot. The bottleneck is intelligibility, not loudness.
- **Calling repeatedly within one QRZ window.** "WM3J WM3J WM3J WM3J" doesn't work better than "WM3J" — it just sounds more desperate and uses more time.
- **Calling out-of-turn during region restrictions.** The DX has heard your call but won't work you. Wait.
- **Spamming the cluster.** Posting "I worked them, what a thrill!" annoys other operators. Just log the QSO and move on.

## See also

- [§22-05 — Pile-up Etiquette](../22-operating-practice/22-05-pile-up-etiquette.md) (the rules)
- [§22-08 — Split-Frequency Operation](../22-operating-practice/22-08-split-frequency.md) (the mechanics)
- [§23-05 — Working Rare DX](23-05-working-rare-dx.md)
- [§23-06 — Timing](23-06-timing.md)
- [§23-10 — Split Tactics](23-10-split-tactics.md)
