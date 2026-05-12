---
id: 22-08
title: Split-Frequency Operation
chapter: 22
section: 08
level: mixed
status: draft
---

# Split-Frequency Operation

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What "split" means

In normal operating you transmit and receive on the **same** frequency. The station you're talking to does too, and you take turns listening to each other on that one frequency. That's **simplex**.

**Split-frequency** operation (usually just "split") means the two stations are on **different** frequencies:

- One station **transmits on frequency A** and **listens on frequency B**.
- The other station **transmits on B** and **listens on A**.

The pair is still a normal two-way QSO; only the frequencies differ.

Split is a *technique*, not a mode. You can run split on CW, SSB, RTTY, even some FT8 contesting — anywhere the rig supports two VFOs.

## Why split exists

Three reasons hams run split:

1. **DXing — managing a pile-up.** This is by far the most common reason. A rare DX station calls CQ once, then "runs" — taking one contact after another with a steady cadence. Dozens or hundreds of stations want to call. If everyone calls on the DX's transmit frequency, it's a wall of noise and the DX can't pick out anyone. By listening on a different range (typically 5–15 kHz higher than their TX), the DX hears one or two callers at a time, cleanly.

2. **Cross-band repeaters and satellites.** A repeater transmits on one frequency and listens on another (e.g. a 2 m repeater might TX on 146.940 MHz and RX on 146.340 MHz, a 600 kHz offset). Working a linear satellite is also a form of split — uplink on 70 cm, downlink on 2 m.

3. **Avoiding QRM on a busy band.** Two stations agree to TX on different frequencies to dodge interference on one of them. Rare, but it happens.

For amateur HF voice/CW work, **#1 is what you'll deal with most**. The rest of this section is about DX-pile-up split.

## How the DX announces split

The DX station tells you where they're listening, in one of several shorthand forms. Listen for ten or fifteen seconds and you'll catch it.

| What the DX says | What it means |
|------------------|----------------|
| `"5 up"` or `"up 5"` | Listening 5 kHz higher than my TX |
| `"5 to 15 up"` or `"5-15 up"` | Listening across the range 5–15 kHz higher |
| `"listening 14210"` | Listening on 14.210 MHz specifically |
| `"QSX 14210"` (CW) | Same: listening on 14.210 MHz (`QSX` = "I am listening on…") |
| `"QSX 5 up"` (CW) | Listening 5 kHz up from my TX |
| `"split"` (alone) | Telling you to enable split; check the cluster or listen for their range |

If you hear the DX work a caller but never heard the caller, **the DX is running split** — you're hearing only one side of the conversation because the callers are on a different frequency. Tune up 5–15 kHz from the DX's TX and you'll hear the pileup.

## Setting up split on your rig

Every modern HF rig supports split via two **VFOs** (Variable Frequency Oscillators), called VFO A and VFO B. The idea:

- VFO A → DX's TX frequency (where you listen)
- VFO B → your TX frequency (in the DX's listen range)

Press the **SPLIT** button. The rig now **receives on A** and **transmits on B**.

### Step-by-step (typical modern rig)

1. **Tune VFO A to the DX's transmit frequency.** You're listening to them now.
2. **Press A=B** (or "VFO A → VFO B" — copies A's frequency to B). VFO B now matches A.
3. **Switch to VFO B.** Tune VFO B up by the DX's announced offset — usually 5 to 15 kHz higher. This will be your TX frequency.
4. **Switch back to VFO A** so your display and audio show the DX.
5. **Press SPLIT.** The SPLIT indicator lights up.
6. **Quick test:** key the rig briefly into a dummy load. Listen — the rig should TX on VFO B's frequency, not A. If you don't have a dummy load, *do this only on a clear frequency you've checked, not on the DX's TX freq.*

After your contact, **press SPLIT again to disable** before tuning around — otherwise you'll be transmitting on a stale frequency next time you key up.

### Common rig-specific notes

- **Icom:** SPLIT button toggles; XFC (Transmit Frequency Check) button lets you briefly listen on the TX VFO while held down.
- **Yaesu:** SPLIT button toggles; some models use `A>B` for one-direction copy. TX/A and TX/B LEDs show which VFO is transmitting.
- **Kenwood:** SPLIT button toggles; the rig display shows TX and RX frequencies separately when split.
- **Elecraft K3/KX-series:** TAP `B/VFO` to swap VFOs; tap `SPLIT` to enable.

> ⚙️ **Advanced —** Some rigs let you set a fixed split offset (e.g. always 5 kHz up) and toggle it on/off with a single button. This is convenient for habitual DXers but dangerous if the offset doesn't match the DX's stated listen range — you may transmit outside the listen window and never get called back.

## The split protocol — a worked exchange

Here's a complete split QSO with a DX station running on 14.205 MHz, listening 5–15 kHz up:

```
DX (TX on 14.205):  "QRZ 5 to 15 up from WM3J"
You (TX on 14.210): "Whiskey One Alpha Bravo Charlie"
                    [your call, once, brief]
DX (TX on 14.205):  "W1ABC 5-9, 5-9, QRZ"
                    [the DX picks you and gives signal report]
You (TX on 14.210): "5-9 thanks 73 W1ABC"
                    [you confirm + 73 + your call]
DX (TX on 14.205):  "5NN W1ABC, QRZ"
                    [the DX logs you and moves on]
```

Total time per contact: 5–15 seconds. Everything on your end happens on **your** TX frequency; everything on the DX's end happens on **their** TX frequency. You never transmit on 14.205.

## Picking your TX frequency in the listen range

If the DX says "5 to 15 up" they're listening across a 10 kHz range. Where in that range should you transmit?

- **Not at the very bottom (e.g. exactly 5 up).** That's where every newcomer picks. Crowded.
- **Not in the middle.** Same problem — common default.
- **Pick a spot 1–2 kHz from where you hear the most callers.** That's "off the pile." The DX is listening across the whole range; a slightly quieter spot makes you stand out.
- **Listen on your TX frequency** if you can (rig "dual watch" / XFC feature). If five other people are calling on the exact kHz you picked, move 1–2 kHz away.

The DX rotates their listening focus — they may listen at 5, then 8, then 12, then back to 5. Spreading callers across the range makes their job easier and gets more contacts logged. Bunching everyone at one frequency makes nobody happy.

## Pile-up calling discipline (in split)

These rules are the same as in any pile-up (see [§22-05](22-05-pile-up-etiquette.md) for the full discipline), but worth restating in the split context:

- **Never transmit on the DX's TX frequency.** This is the cardinal sin of DXing — called "transmitting in the pile" or "being a cop." The DX station and other operators will notice and call you out publicly.
- **Call once, briefly.** Your full callsign, once, phonetically (or once in CW). No "calling DX," no padding.
- **Wait for "QRZ" or the end of the previous contact** before calling again.
- **Stop calling if not picked.** Listen, wait, try the next round.
- **Don't repeat your call indefinitely** on the same frequency — move a kHz or two.

## Troubleshooting split

- **DX never comes back to me.** First, verify SPLIT is enabled. Then verify your TX frequency is in their listen range (use a panadapter or dual watch). Then check you're not transmitting in a crowded spot.
- **I hear the DX but never hear other callers.** That's normal in split — callers are on a different frequency. Tune up to the listen range to hear the pile.
- **Rig transmits on the wrong frequency.** SPLIT button isn't on, or VFO A vs B is swapped. Stop transmitting; reset; verify with a brief dummy-load key-down.
- **I forgot to turn split off and keyed up on a new band.** Common mistake. Always toggle SPLIT off after the contact.

> ⚙️ **Advanced —** DXpedition operators often use multiple-listener modes. "Listening 5-up for North America, 10-up for Europe" splits the geographic regions across the listen range. Some rigs and logging software automate split for known DXpeditions — the cluster spot includes the listen range and the logger configures both VFOs. Be cautious with automation; verify the announced split matches what your software set up.

## When NOT to use split

- **The DX is running simplex** ("listening this frequency" or no split announcement). Just call on their frequency.
- **You're calling CQ yourself.** No reason to run split unless you have a specific pile-up problem.
- **Casual ragchew.** Simplex is fine and expected.

## Cross-band repeaters and satellites (the other split)

Repeater and satellite operation is technically split, but the rig handles it for you:

- **Repeater offset** (e.g. ±600 kHz on 2 m, ±5 MHz on 70 cm) is preprogrammed in the rig's memory. Press PTT and the rig automatically TXes on the input frequency while displaying the output.
- **Satellites** use frequencies on different bands (uplink and downlink). Tracking software like SatPC32 or the J-Sat module adjusts both VFOs for Doppler shift continuously.

You don't manually set split for these — the rig or satellite tracking software does it. But under the hood, it's the same idea: TX on one frequency, RX on another.

## See also

- [§22-05 — Pile-up Etiquette](22-05-pile-up-etiquette.md) — calling discipline once you understand split
- [§22-04 — Calling CQ](22-04-calling-cq.md) — the standard simplex QSO flow
- [§22-01 — Is the Frequency in Use?](22-01-frequency-in-use.md) — listening before transmitting
- [§19-01 — Q-Codes](../19-qcodes-prosigns/19-01-q-codes.md) — `QSX` is the relevant Q-code for split
- [§20-04 — Satellite Band Plan](../20-band-plans/20-04-satellite.md) — uplink/downlink pairs
