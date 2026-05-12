---
id: 23-10
title: Split Tactics
chapter: 23
section: 10
level: mixed
status: draft
---

# Split Tactics

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What this section is

This is the tactical side of split-frequency operation. The *mechanics* (what split is, how to set it up, the standard protocol) are in [§22-08 Split-Frequency Operation](../22-operating-practice/22-08-split-frequency.md). This section assumes you've read that and asks the next question: *given* that the DX is on split, what specifically should you do to be picked?

The answer involves reading the DX operator's behavior, staying ahead of the pile, exploiting your panadapter, and knowing when to abandon split and try something else.

## The fundamental question

When the DX says "5 to 15 up," they are *listening across 10 kHz of bandwidth*. Their ears (or their decoder, in digital) are sampling that range. The question that decides whether you get the contact is:

**Where, within that 10 kHz, will the DX's listening focus be in 5 seconds — and how do you put your signal there?**

The answer depends on the operator. Some operators tune linearly (bottom to top, then top to bottom). Some operators jump randomly. Some favor specific sub-ranges. Reading the pattern is the difference between waiting in line and being next.

## Reading the DX's listen pattern

The first task in any split situation is to figure out the operator's listening behavior. Listen to 5–10 QSOs and note where each picked caller was:

- **Linear scan (low to high):** Calls picked at 5 up, then 6 up, then 7 up, then 8 up, then 9 up… The operator is tuning the listen range smoothly. You can predict where they'll be in 5 seconds and place yourself just ahead.
- **Linear scan (high to low):** The reverse. Picks at 14 up, 13 up, 12 up… Position yourself just below the current pick frequency.
- **Bouncing / random:** Picks are scattered (5 up, 12 up, 7 up, 14 up, 6 up). The operator is jumping around or rotating in some non-linear pattern. Harder to predict; just pick a quieter spot and rely on your signal being clean.
- **Sub-range favoritism:** Picks are clustered at 8–10 up; the bottom and top are ignored. The operator has a preferred narrow window. Either fight for space in the favored sub-range (crowded) or wait for them to widen their listening.
- **Regional rotation:** Picks alternate by continent — one EU call, one NA call, one JA call. The listen range may be divided geographically (5–8 EU, 9–12 NA, 13–15 JA).

Most operators run *linear-scan* during the first hour and *bouncing* later as they get tired or the pile-up thins. The first hour is the predictable hour.

> ⚙️ **Advanced —** A useful trick: write down the kHz offset of each picked station for 5–10 QSOs. Patterns become obvious on paper that aren't obvious by ear. Top DX operators do this mentally; new operators benefit from physically writing it down. Some logging software auto-records pickup frequencies if you're using packet/RBN-augmented spotting.

## Staying ahead of the pile

If the DX is doing a linear scan (low to high) and just picked someone at 8 up, the next pick will be near 9 up. Stations who are sitting on 9 up *now* will be heard. Stations who are sitting at 5 up (where the previous pick was) are behind the focus and won't be heard.

To stay ahead:

1. **Note the current pick frequency.** This is where the DX's "ear" is now.
2. **Predict where the ear will be next.** Linear-up means +1 to +2 kHz; linear-down means -1 to -2 kHz.
3. **Position your TX there.** Tune your TX VFO to the predicted frequency.
4. **Call at the next QRZ.**

This is "lead-the-target" shooting applied to DX. It works when the DX is predictable; it fails when they're not.

## The panadapter advantage

Modern SDR-based rigs (IC-7300, IC-7610, FTDX-101D, FT-710, K4) and SDR-Console-driven setups have *visual* panadapter displays. The pile-up is visible as a cluster of carriers in the listen range. This is a huge advantage if you use it.

What you can see on the panadapter:

- **Where the dense parts of the pile are.** Avoid those.
- **Where the gaps are.** Transmit there.
- **Which kHz the DX just picked.** The picked station's carrier briefly appears in the listen range; the DX's response on their TX frequency tells you which one was selected.
- **The DX's listening focus.** Over time, the picked stations cluster in certain parts of the range — that's where to be.

A common workflow:

1. **Set the panadapter span to 10–20 kHz.** Wide enough to see the whole listen range; narrow enough that you can see individual carriers.
2. **Set the rig's main VFO to the DX's TX frequency** (so the rig audio gives you the DX's signal).
3. **Set the panadapter center to the listen range** (5–15 kHz away from the DX).
4. **Watch which carriers transmit and when**, correlating with who the DX picks.

In 1–2 minutes you have a mental model of the pile and can place yourself well.

## The CW skimmer advantage

For CW operations, the **CW Skimmer** software (or hardware equivalents like Skimmer Server) decodes every CW signal in the panadapter span simultaneously. Drop the skimmer on your panadapter, run it in parallel with your operating, and you'll see:

- **Every caller's full callsign in the pile** decoded and displayed.
- **Where each caller is in the listen range** (frequency tagged).
- **Who the DX picks at each cycle** (their signal in the picked frequency goes from "caller in pile" to "DX confirms").

This is essentially "computer-augmented hearing." On CW, a well-configured skimmer makes pile-up management 5–10x easier. The contest big-gun stations all use skimmers; many casual DXers do too.

> ⚙️ **Advanced —** Skimmers feed the **Reverse Beacon Network**, but they also serve as personal pile-up management tools. The trade-off: a skimmer's decoded list can have errors at low SNR, and relying on the decoded list instead of your own ears is risky in marginal conditions. Verify decoded callsigns against the audio before trusting them.

## When to abandon split

Split is not always the right choice. Reasons to switch to simplex (or try a different band):

- **The DX has gone simplex.** Sometimes a DXpedition opens split, then closes it after a few hours as the pile thins. Listen for the announcement: "no longer split, listening this frequency."
- **The DX's listen range is *too wide* to fit in your panadapter.** A 25+ kHz listen range with 500 callers is essentially noise; your odds drop. Try a different mode or band.
- **You're not getting partial copies.** If 30+ minutes of split calling produces zero "WM3? again?" partials, the DX simply isn't hearing you. Likely propagation; try later or a different band.
- **The DX is restricted to a different region and won't rotate to yours for hours.** Walk away.

## Mode-specific split tactics

### SSB

The standard. Listen for the operator's "up X" announcement; set split; call once cleanly. Phonetic clarity matters more than power — "Whiskey Mike Three Juliet" said cleanly at S5 beats "WM3J" said hastily at S9+10.

### CW

CW pile-ups are often denser per kHz because CW signals are narrower (200–500 Hz vs SSB's 2.4 kHz). The same listen range fits 5–10x more callers. Tactics:

- **Send your call at the DX's exact speed.** Don't slow them down.
- **Use 1–2 kHz of separation from the herd.** Most callers default to round-kHz offsets (5.000 up, not 5.300 up). Be off-grid.
- **Listen on your TX freq with QSK / full-break-in** if your rig supports it. You'll hear the DX between your dits and dahs, which catches partial copies instantly.

### FT8 / FT4

Digital split is handled by the software. In "Fox/Hound" mode (used by DXpeditions), the fox station occupies the standard FT8 calling frequency and the hounds transmit on different audio frequencies within the same kHz band. The software (WSJT-X with Hound mode enabled) places your call appropriately; you don't choose the audio frequency manually.

Tactics for FT8 fox/hound:

- **Set hound mode in WSJT-X.** "Settings → Advanced → Special Operating → Hound."
- **Click the fox's CQ in the decoded list.** WSJT-X responds with your call at a randomly-chosen audio frequency.
- **Let the software work the QSO.** Don't intervene; the fox/hound protocol handles the full exchange automatically.
- **If you're not picked in 5–10 cycles**, the band is crowded or your signal is too weak. Try a different band.

## When the DX changes the listen range

Mid-operation, the DX may announce a new listen range:

```
DX: "Now listening 10 to 20 up, was 5 to 15"
DX: "Listening only at 8 up"  (narrow window)
DX: "QSX 14215 simplex now"   (back to simplex)
```

Re-set your split immediately. Operators who keep transmitting on the old listen range are now QRMing whatever's there (often a different station running their own QSO). This is a fast way to be publicly named as a lid.

Cluster comments often relay these changes ("3Y0J now listening 10-20 up"). Watch the cluster as well as listening.

## A worked split tactical scenario

Scenario: VK0M (Macquarie Island DXpedition) on 40 m CW, working 5 to 15 up at 30 WPM. You're East Coast US with 100 W and a low dipole.

**Minute 0–2:** Listen. Note pick frequencies of 5 picked stations: 6 up, 8 up, 10 up, 7 up, 11 up. The operator is bouncing within 6–11 up; the bottom (5 up) and top (12–15 up) are mostly ignored.

**Decision:** Place your TX at 9 up (mid-range, in the favored sub-range, with offset 9.300 to be off-grid).

**Minute 2–5:** Call at 9.300 up. Three QRZ cycles; not picked. Watch the panadapter: 9.0–9.5 has 6 carriers; you're one of them. The DX has just picked someone at 10.2 up.

**Adjustment:** Move to 9.7 up — quieter spot, just above the current dense band.

**Minute 5–7:** Call at 9.7 up. Two cycles. The DX comes back: "WM3? AGN?" Partial copy.

Reply: "WM3J WM3J" — sent twice at the DX's speed.

DX: "WM3J 5NN."

You: "5NN TU 73."

DX: "TU QRZ."

Logged. Total: 7 minutes for a top-20 needed entity.

## See also

- [§22-08 — Split-Frequency Operation](../22-operating-practice/22-08-split-frequency.md) (the mechanics)
- [§22-05 — Pile-up Etiquette](../22-operating-practice/22-05-pile-up-etiquette.md)
- [§23-05 — Working Rare DX](23-05-working-rare-dx.md)
- [§23-09 — Pile-up Strategy](23-09-pile-up-strategy.md)
- [§03-01 — FT8 / FT4](../03-digital-modes/03-01-ft8-ft4.md)
