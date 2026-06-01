---
id: 22-08
title: Split-Frequency Operation
chapter: 22
section: 08
level: mixed
status: published
---

# Split-Frequency Operation

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## The problem split solves

Picture a rare DX station — say, a DXpedition to a Pacific atoll that's active for one week every four years. The instant they call CQ on, say, 14.205 MHz, **two to three hundred stations** worldwide want to be the one they pick. Without split, here's what each side experiences:

**The DX operator's experience (simplex):** They call "QRZ" and instantly hear a *wall of overlapping voices* on 14.205 — 200 callsigns shouted simultaneously into the same 3 kHz of audio. To human ears it's an undifferentiated roar. They can't pull a single complete callsign out of it. They guess at fragments ("Whiskey... was that Whiskey One? Whiskey One Alpha somebody?") and the QSO rate collapses.

**Your experience as a caller (simplex):** You key up the moment they say "QRZ" — but so does everyone else, and your 100 W signal is competing with 199 others all transmitting on the exact same kHz. The DX will probably never hear you specifically; you'll hear them work somebody else and have to start over.

The wall-of-noise problem isn't the band's fault; it's geometric. Two hundred signals on the same frequency overlap in time and cancel each other in the DX's receiver. The fix is to **spread the callers out across a range of frequencies** so the DX only hears one or two at a time. That's split.

## How split fixes it

The DX transmits on one frequency (their TX) and *listens on a different range* — usually 5 to 15 kHz higher. Callers transmit somewhere in that range, not on the DX's TX freq. Now:

**The DX's experience:** They tune across the listen range and hear individual stations cleanly — one caller at 5 kHz up, another at 7 kHz up, another at 12 kHz up. Each is alone on its own kHz, so each is intelligible. The DX can pick a single callsign and work it. The next caller is on a different kHz, so picking them out is just a tuning adjustment.

**Your experience as a caller:** You're on a specific kHz in the listen range — say 14.210 MHz. Maybe four or five other people picked the same kHz, but most of the pileup is spread across the 5-to-15-up range. You can hear the DX cleanly on 14.205 and you transmit on 14.210. When the DX names a callsign, you hear it on their TX frequency.

Visually, on a panadapter/waterfall, it looks like this:

```
       DX (single signal)         |---- pileup spread across 10 kHz ----|
       ▼                          ▼  ▼   ▼     ▼   ▼  ▼  ▼    ▼   ▼     ▼
       ████                       █  █   █     █   ██ █  █    █   █     █
       ────┴────┴────┴────┴────┴────┴────┴────┴────┴────┴────┴────
      14.205                   14.210                              14.215
       ←──── 5 kHz gap ────→
```

The DX's TX frequency is empty of caller signals (because nobody is supposed to transmit there). The pileup is a cluster of signals 5 to 15 kHz higher.

## What "split" actually is, technically

**Split-frequency operation** means a station transmits on one frequency and receives on a different one. Each side still has a single radio with a single antenna — what differs is the rig's TX and RX VFOs.

- The DX station: **TX on A, RX on B**
- A caller: **TX on B, RX on A**

The pair is still a normal two-way QSO; only the frequency pairing differs. There's nothing inherently exotic about it — every FM repeater on every band uses split (the repeater transmits on one frequency and listens on another). The HF DX case is just split done manually with two VFOs instead of automatically with a repeater memory.

Split is a *technique*, not a mode. You can run split on CW, SSB, RTTY, even some FT8 contesting — anywhere the rig supports two VFOs.

## Three contexts where split shows up

1. **DXing — managing a pile-up.** This is the dominant amateur use of split and the focus of the rest of this section. Heavy traffic, rare entity, hundreds of callers, DX runs split to keep the QSO rate up.
2. **Cross-band repeaters and satellites.** A 2 m FM repeater transmits on 146.94 MHz and listens on 146.34 MHz (a 600 kHz offset); the rig handles this automatically when you select the repeater from memory. Working a linear satellite is the same idea on a bigger scale — uplink on 70 cm, downlink on 2 m, with Doppler shift continuously adjusted by tracking software.
3. **Avoiding QRM.** Two stations agree to TX on different frequencies to dodge interference on one of them. Rare in modern amateur practice but it happens, especially on net frequencies that get covered by sudden contest activity.

The rest of this section is about case #1.

## Recognizing split is happening

If you tune across the band and find a rare callsign that you can hear clearly but **nobody seems to be calling them on their frequency**, that's the telltale sign: the DX is running split, and the callers are bunched up on a different frequency. Three giveaways:

1. **You hear the DX, but not the responses.** A normal QSO has both sides on the same frequency, so you hear both. Split means you only hear the DX (because the callers are 5–15 kHz away).
2. **The DX announces "up" or "QSX" before going back into "QRZ".** Listen for 30 seconds — they'll repeat the announcement every few contacts.
3. **A cluster spot or the panadapter shows the split.** DX cluster comments include the listen range (`14205.0  KH1/KH7Z   UP 5-15`). On a panadapter, the pileup cluster of signals is visible above the DX's TX frequency.

If you hear the DX work a caller but the caller's audio never came through your speaker, you've just had a split-mode encounter — the caller was on a different kHz.

## How the DX announces split

The DX station tells you where they're listening, in one of several shorthand forms.

| What the DX says | What it means |
|------------------|----------------|
| `"5 up"` or `"up 5"` | Listening 5 kHz higher than my TX |
| `"5 to 15 up"` or `"5-15 up"` | Listening across the range 5–15 kHz higher |
| `"down 2"` or `"2 down"` | Listening 2 kHz **lower** (rare but common on 40 m where the lowest end of the phone segment is popular DX territory and "up" would put callers outside the phone band) |
| `"listening 14210"` | Listening on 14.210 MHz specifically (a single kHz, not a range) |
| `"QSX 14210"` (CW) | Same: listening on 14.210 MHz (`QSX` = "I am listening on…") |
| `"QSX 5 up"` (CW) | Listening 5 kHz up from my TX |
| `"split"` (alone) | Telling you to enable split; check the cluster, listen for the range, or look at the panadapter |

"Up" is dominant on 20 m / 15 m / 10 m / 17 m. "Down" appears on 40 m and 80 m where the DX may be transmitting at the upper edge of their band's DX window and listens lower. Read what the DX says — don't assume "always up."

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

> **Advanced —** Some rigs let you set a fixed split offset (e.g. always 5 kHz up) and toggle it on/off with a single button. This is convenient for habitual DXers but dangerous if the offset doesn't match the DX's stated listen range — you may transmit outside the listen window and never get called back.

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

> **Advanced —** DXpedition operators often use multiple-listener modes. "Listening 5-up for North America, 10-up for Europe" splits the geographic regions across the listen range. Some rigs and logging software automate split for known DXpeditions — the cluster spot includes the listen range and the logger configures both VFOs. Be cautious with automation; verify the announced split matches what your software set up.

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
