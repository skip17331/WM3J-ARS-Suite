---
id: 23-03
title: Run vs. Search-and-Pounce
chapter: 23
section: 03
level: mixed
status: draft
---

# Run vs. Search-and-Pounce

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## The two postures

In a contest (and in non-contest pile-up DXing too), there are exactly two ways to make QSOs:

**Running.** You pick a frequency, call CQ, and stay there. Other stations find you and call you. You work them one after another from a fixed spot on the band.

**Search-and-Pounce (S&P).** You tune across the band, find someone else calling CQ, call them, work them, then keep tuning. You move from frequency to frequency, hunting for stations.

Every contester does both. The skill is knowing which to do when, and being good at switching between them.

## What running looks like

You find an open spot on the band (more on that in a moment), key up, and call:

```
"CQ Contest, Whiskey Mike Three Juliet, CQ"
```

If you have a good signal and a good frequency, callers start arriving within seconds. You work them in sequence:

```
You:  "CQ Contest WM3J"
K1A:  "Kilo One Alpha"
You:  "K1A 5-9 PA"
K1A:  "5-9 MA"
You:  "QRZ WM3J"
K2B:  "Kilo Two Bravo"
You:  "K2B 5-9 PA"
K2B:  "5-9 NJ"
You:  "QRZ WM3J"
W4C:  "Whiskey Four Charlie"
...
```

Each QSO is ~8 seconds. If callers are stacked you're doing 60–120 QSOs/hour from one frequency, hands on the keyboard, never touching the VFO.

**Pros of running:**
- High rate when conditions are good.
- You log the contact and the caller goes away — no fighting other callers.
- You're a "spot magnet" — once spotted, your rate goes up further.

**Cons of running:**
- You're tied to a frequency. If the band shifts, you may be on the wrong band.
- You don't get multipliers — you can't choose who calls you.
- A weak signal can't hold a run frequency; bigger stations will be heard over you.

## What S&P looks like

You tune across the band, listening for CQs. When you hear one, you call back, work them, then keep tuning.

```
[Tuning] *static* *cq* "...CQ Contest K3DEF Kilo Three Delta..."
You:    "WM3J"
K3DEF:  "WM3J 5-9 03"
You:    "5-9 05"
K3DEF:  "TU QRZ K3DEF"
[Tuning] *static* "...CQ Contest W7XYZ..."
You:    "WM3J"
...
```

Pace is slower — you spend half your time tuning, finding signals, and waiting for a CQ. Rate is typically 30–60 QSOs/hour for S&P versus 60–120 for running.

**Pros of S&P:**
- You can chase multipliers — if the band has a rare section/zone, you tune to find it.
- You don't need a powerful signal to be heard — you're calling a station that's listening for callers.
- You can work the whole band, not just one frequency.

**Cons of S&P:**
- Lower rate.
- More mental effort — you're constantly tuning, listening, calling, working.
- Tuning fatigue is real after several hours.

## Which to use when

A general guide:

| Situation | Posture |
|-----------|---------|
| You're spotted on the cluster and callers are coming | Run |
| You have a clean frequency and a good signal on a popular band | Run |
| You're a smaller station competing with big runs | S&P |
| The band is starting to close (rate drops) | Switch to S&P, look for openings on other bands |
| You're chasing multipliers (new sections, zones, etc.) | S&P |
| You're new to the contest and not sure how to run | S&P first, learn the exchange, then try to run |
| The first 30 min and last 30 min of a contest | S&P (everyone is calling CQ; nobody is searching) |
| The middle of the night on 80 m / 40 m | S&P (low rate either way; mults matter more) |

The best contesters do both, switching every 10–30 minutes based on rate. If a run rate drops below ~30/hour, it's time to S&P for a while. If S&P finds you a clean frequency near a big-signal multiplier station, set up a run there.

## The Run/S&P button

All modern logging software has a **Run / S&P** mode toggle (Ctrl+Tab in N1MM Logger+, similar in others). The toggle changes how the software behaves:

**Run mode:**
- Hitting Enter sends your CQ message.
- The current frequency is "your" run frequency.
- F-key macros are tuned for running: F1 = CQ, F2 = exchange, F3 = thank-you-QRZ, etc.
- Bandmap shows who's calling you (decoded from spotting and skimmer data).

**S&P mode:**
- Hitting Enter calls the station you tuned to (sends your callsign).
- F-key macros change: F1 = "his call" sends his call back to him, F2 = your exchange, F3 = thanks.
- Software tracks frequencies you've already worked so you don't waste time recalling them.

Toggling correctly between modes is a basic contesting skill. In N1MM, the entry window shows `RUN` or `S&P` in the title bar; in WinTest the indicator is in the status bar. If your macros are sending the wrong message, you're probably in the wrong mode.

> **Advanced —** Top contesters using **SO2R** (Single Operator, Two Radios) literally do both at once — one radio runs on a clean frequency, the other S&Ps the rest of the band for multipliers. Coordinating between the two without missing exchanges is the elite skill of modern contesting. SO2V (Two VFOs on one radio) is a poor-man's SO2R using one rig's two receivers. N1MM's "Run+S&P" workflow assumes one of these setups.

## Multiplier hunting via S&P

In multiplier-rich contests (CQ WW, Sweepstakes), a single new mult can be worth 30+ QSOs. The discipline:

1. **Keep your logging software's multiplier window open.** It lists every needed mult by band — sections, zones, countries, prefixes.
2. **When you see a needed mult on the cluster**, S&P to it immediately. Stop your run, tune to the spot, get the QSO. Resume your run afterward.
3. **Tune the band's multiplier-rich areas.** The low end of the band on CW (rare DX hangs out at the band edges), the upper end on SSB (where DX expeditions often park).
4. **Plan a 30-minute "mult sweep" mid-contest.** Set down the run, do a full band-pass S&P for 30 minutes, grab everything you don't have, then go back to running.

The math works because score multiplies. Adding a 5th mult takes you from 4×100 = 400 to 5×100 = 500 (a 100-point gain from one QSO). Adding a 50th mult takes you from 50×500 = 25,000 to 51×500 = 25,500 (a 500-point gain from one QSO). Late-contest mults are disproportionately valuable.

## Sustaining a run

A good run frequency is hard to find and easy to lose. Some habits:

- **Listen first.** "Is the frequency in use?" — even in a contest, you must check before claiming the spot. 10–15 seconds of listening, then a brief "QRL?" Some contesters skip this; that's rude even on contest weekend.
- **Stake your claim with a CQ immediately.** If nobody answers, CQ again in 5 seconds. If you don't establish yourself, someone else will.
- **Don't move unless rate drops.** Moving a run frequency costs you — you lose the cluster spot, the cumulative listeners, the band-conditions optimum. Stay until the rate is clearly dead.
- **Send your callsign every QSO.** No abbreviations. The next caller may not have heard your previous QSO; they need your full call to spot you on the cluster.
- **Use a logging-software macro for "QRZ + my call,"** not just "QRZ." Other stations need to hear your call repeatedly to identify you.

## When to give up a run frequency

Three signs:

- **Rate has dropped below 30 QSOs/hour for 10+ minutes.** The band has shifted; your run frequency is no longer productive.
- **A bigger station has settled near you (within 1 kHz).** Their signal is QRMing yours. You'll lose this fight; move 2–3 kHz away or to another band.
- **A pileup of callers is too thick to manage.** Counter-intuitive but real — if 40 stations are calling at once and you can't extract single callsigns, your rate drops. Consider going split (transmit on the run freq, listen ±2 kHz), or just slow down briefly to clear the pile.

After abandoning a run, S&P for 15–30 minutes to find the new "hot" part of the band, then try a new run frequency.

## See also

- [§23-02 — Contesting Basics](23-02-contesting-basics.md)
- [§23-06 — Timing](23-06-timing.md)
- [§22-05 — Pile-up Etiquette](../22-operating-practice/22-05-pile-up-etiquette.md)
- [§22-08 — Split-Frequency Operation](../22-operating-practice/22-08-split-frequency.md)
