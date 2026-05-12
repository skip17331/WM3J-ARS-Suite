---
id: 05-05
title: QSO Simulation
chapter: 05
section: 05
level: simple
status: draft
---

# QSO Simulation

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

This chapter is a script and a method for simulating CW QSOs at home before you key up on the air. Practice with these scripts and your first real CW contact will feel familiar instead of terrifying.

## The standard CW QSO format

Every CW QSO has a beginning, a middle, and an end. The beginning is roll-call (who's transmitting, who's receiving). The middle is content (RST, name, QTH, rig, weather). The end is sign-off (thanks, 73, end-of-contact prosign).

A canonical exchange (between WM3J and K1ABC):

```
              ─────  Round 1: roll-call & opening greeting  ─────

WM3J:    CQ CQ CQ DE WM3J WM3J K
K1ABC:   WM3J DE K1ABC K1ABC K
WM3J:    K1ABC DE WM3J GE OM TNX FER CALL
         UR RST 589 589
         NAME MIKE MIKE
         QTH MD MD
         HW CPY? K1ABC DE WM3J KN

              ─────  Round 2: K1ABC's exchange  ─────

K1ABC:   WM3J DE K1ABC R FB MIKE TNX FER RPT
         UR RST 599 5NN
         NAME JOHN JOHN
         QTH NY NY
         BACK TO U OM WM3J DE K1ABC KN

              ─────  Round 3: extended info exchange  ─────

WM3J:    K1ABC DE WM3J FB JOHN ES PSE COPY
         RIG K3 K3 PWR 100 W ANT 40M DPL
         WX SUNNY 75 F HR
         BACK TO U DR JOHN K1ABC DE WM3J KN

K1ABC:   WM3J DE K1ABC R FB MIKE
         RIG IC-7300 PWR 50 W ANT VERTICAL
         WX RAIN 55 F HR
         GUD QSO ES TNX FER FB CHAT
         WM3J DE K1ABC KN

              ─────  Round 4: sign-off  ─────

WM3J:    K1ABC DE WM3J TNX FB QSO JOHN
         HOPE CUAGN 73 ES BCNU
         K1ABC DE WM3J SK

K1ABC:   WM3J DE K1ABC TNX MIKE 73 SK
```

That's it. About 90 seconds of QSO at 18 WPM. Notice the patterns:

- Every transmission begins with **his call DE my call** ("his call from my call")
- Most transmissions end with **K** (over to anyone) or **KN** (over to specific named station)
- Sign-off uses **SK** (silent key — end of QSO)

## What every CW operator knows

The CW community has agreed (over decades) on a small vocabulary that covers 95% of casual exchanges. Memorize these and you're operational:

| Abbrev | Meaning |
|--------|---------|
| GE / GM / GA | Good evening / morning / afternoon |
| OM | Old Man (term of endearment, all genders use it) |
| YL | Young lady (any female operator) |
| XYL | Wife/spouse |
| UR | Your |
| RST | Readability, Strength, Tone (599 = perfect, 339 = weak/scratchy) |
| FB | Fine Business (excellent / good) |
| TNX / TKS | Thanks |
| FER | For (always) |
| ES | And (from `&`) |
| HW CPY? | How copy? (How well are you receiving me?) |
| HR | Here |
| QTH | Location |
| WX | Weather |
| RIG | Transceiver |
| PWR | Power |
| ANT | Antenna |
| 73 | Best regards (sign-off) |
| 88 | Love and kisses (closing for spouse, family, romantic) |
| BCNU | Be seeing you |
| SK | End of contact |
| K | Over (to anyone) |
| KN | Over (to specific station only) |
| AR | End of message |
| BT | Pause / paragraph break |
| AS | Wait, please |
| QRZ? | Who's calling me? |
| QSY | Change frequency |
| QRP | Low power (5 W or less) |
| QRO | High power |
| QRM | Man-made interference |
| QRN | Natural interference (atmospheric, lightning) |
| QSL | Confirm receipt / send a QSL card |
| AGN | Again |
| PSE | Please |
| WID | With |
| OP | Operator |

The Q-codes are covered fully in §19-01. Most QSOs use only the dozen or so above.

## Practice scripts at three speeds

### Speed 1 — beginner (10–15 WPM)

Focus: just exchange callsigns, RST, name, QTH. Keep transmissions short.

```
CQ CQ DE WM3J WM3J K
WM3J DE K1ABC K1ABC K
K1ABC DE WM3J GE TNX UR RST 589
NAME MIKE QTH MD K1ABC DE WM3J K
WM3J DE K1ABC R FB MIKE UR 599
NAME JOHN QTH NY WM3J DE K1ABC K
K1ABC DE WM3J FB TNX 73 SK
WM3J DE K1ABC 73 SK
```

### Speed 2 — intermediate (15–20 WPM)

Same exchange but at higher speed and with rig/WX info.

### Speed 3 — full QSO (18–25 WPM)

The full script at the top of this chapter. By the time you can copy this comfortably, you're ready for real on-air contacts.

## How to practice with a partner

If you have a friend learning CW, practice by:

1. Both running a CW practice oscillator or two radios on a short cable (no antennas). Use a dummy load.
2. Sit in different rooms.
3. One person plays "CQ caller", the other plays "answerer."
4. Run a full QSO script from above.
5. Switch roles and run it again.
6. After 5 successful runs, change something — different speed, different RST, ad-libbed name and QTH, etc.

This is how CWops Academy classes work — pairs practice on Skype/Discord with a CW oscillator program, running scripted QSOs week after week.

## How to practice solo

If you don't have a partner:

- **MorseRunner** (free Windows app) — simulates an entire CW contest pile-up, you copy callsigns and exchanges.
- **RufzXP** — drills callsign reception only, no QSO context but excellent for the hardest part.
- **CWops's "On the Beach" recordings** — actual recorded QSOs from real operators, available on YouTube.
- **W1AW practice transmissions** — ARRL's daily code practice broadcasts, multiple speeds, scheduled times.
- **VBand** — `vband.app` — a web-based CW system where you can practice with other learners using a paddle interface in the browser.

## When you finally key up

Your first on-air QSO suggestions:

1. **Choose a moderately busy band — 40 m or 20 m** during daylight.
2. **Listen for slow CQ calls** — 15 WPM or less. They exist; CWops and SKCC operators send slow on purpose.
3. **Tune to a quiet spot near the SKCC frequencies** (3.550, 7.055, 14.050) and call your own slow CQ.
4. **Send your callsign three times** — `CQ CQ CQ DE WM3J WM3J WM3J K`.
5. **If someone answers** — relax. They expect you might be a beginner and will go slow. Use the script above.
6. **If nobody answers in 3 tries** — try a different frequency. Sometimes everyone's just not listening on that spot.

The CW community is unusually patient. People will slow down for you, repeat anything, and welcome you. Have fun.

## See also

- §05-04 — common words and operator vocabulary
- §05-06 — sending CW (paddle, straight key, electronic keyer)
- §19 — Q-codes and prosigns full reference
