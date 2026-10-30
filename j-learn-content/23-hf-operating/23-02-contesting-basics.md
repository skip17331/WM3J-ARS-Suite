---
id: 23-02
title: Contesting Basics
chapter: 23
section: 02
level: simple
status: published
---

# Contesting Basics

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What it is

A **contest** is a timed competition where stations score points for making contacts. The clock starts at a fixed UTC moment, runs for a fixed duration (a few hours, a weekend, or a full week), and ends. During that window every contact is a point — sometimes plus a multiplier — and the highest-scoring station in each entry category wins.

Contests are the high-rate, high-intensity side of amateur radio. Where DXing is patient (you might call one station for 20 minutes), contesting is impatient (20 minutes should produce 20 to 60 contacts). The skill set overlaps but is distinct.

## The exchange

Every contest defines a required **exchange** — the data both sides must transmit and log for the contact to count. The exchange is what makes contesting work: it's short, structured, and machine-checkable.

Common exchange formats:

| Contest | Exchange | Example |
|---------|----------|---------|
| **ARRL Sweepstakes** | RST + serial + precedence + check + section | `WM3J 59 047 A 89 EPA` |
| **ARRL DX (US side)** | RST + state | `WM3J 59 PA` |
| **ARRL DX (DX side)** | RST + power | `JA1ABC 59 100` |
| **CQ WW DX** | RST + CQ zone | `WM3J 59 05` |
| **CQ WPX** | RST + serial | `WM3J 59 0123` |
| **IARU HF Championship** | RST + ITU zone | `WM3J 59 08` |
| **NAQP** | Name + state | `WM3J Mike PA` |
| **Field Day** | Class + section | `2A EPA` |

Notice the pattern: every exchange is 2–5 short fields, all numeric or short alphabetic, all easy to send by voice or CW. There's no name, no QTH, no rig — just what the contest sponsor needs to score.

In SSB contests, signal report is almost always "59" regardless of actual signal. It's not lying — it's convention. The real interesting field is the second piece (serial number, zone, section). On CW it's "5NN" (where N is the cut number for 9).

## The QSO rate mindset

The unit of contesting is **rate** — contacts per hour. A casual entry might run 30/hour; a serious entry runs 100–200/hour for hours at a stretch; the top-five all-band stations sustain 250–400/hour during peak openings. Rate is everything because:

- **Score is proportional to QSO count.** Doubling rate doubles score, all else equal.
- **The clock is fixed.** A 48-hour contest gives you 48 hours; an hour you spend ragchewing is an hour you've thrown away.
- **Multipliers come with volume.** You're more likely to stumble on a needed zone/section/state if you've worked 800 stations than if you've worked 80.

A 60-second QSO is unacceptable in a serious contest. The target is 10–15 seconds per QSO on SSB, 5–8 seconds on CW, 15 seconds per slot on FT4 / RTTY.

A typical SSB exchange at full rate sounds like this:

```
You:  "CQ Contest, Whiskey Mike Three Juliet"
Them: "Kilo One Alpha Bravo Charlie"
You:  "K1ABC 5-9 PA"
Them: "5-9 MA"
You:  "QRZ Whiskey Mike Three Juliet"
```

Total elapsed: 8 seconds. No "thanks," no "73," no "hello Bob." Just the data and the next CQ.

## Sprint vs. marathon

Contests come in two duration flavors:

**Sprints** — 4 to 8 hours. High intensity, no sleep involved, no fatigue management. Examples: NAQP (10 hours, technically), NA Sprint (4 hours), CW Sprint, RTTY Sprint. You go hard for the whole window.

**Marathons** — 24 to 48 hours, sometimes longer. Sleep management is part of the game. The world-class operators sleep 4–6 hours total during a 48-hour weekend. Examples: CQ WW DX (48 h), ARRL DX (48 h), Sweepstakes (24 h within a 30 h window), CQ WPX (48 h), IARU (24 h), WAE (48 h).

> **Advanced —** Multi-operator categories (Multi-Single, Multi-Two, Multi-Multi) split the operating across a team and allow continuous operation. Single-operator categories have rest-period rules: in CQ WW DX, the rule is 30 minutes minimum rest blocks, and you can't operate more than 36 hours out of 48. Logging software enforces this. Single-Op Unlimited allows packet/cluster spots; Single-Op Classic forbids them.

## Common contests

The big ones on the US calendar:

| Month | Contest | What's special |
|-------|---------|----------------|
| Jan | ARRL RTTY Roundup | First contest of year; RTTY/digital only |
| Feb | ARRL DX CW (Feb), then SSB (Mar) | US works DX, DX works US |
| Mar | CQ WPX SSB | Prefixes are mults; rewards rare callsigns |
| May | CQ WPX CW | Same as above but on CW |
| Jun | ARRL VHF | 6m and up; Es openings are the show |
| Jun | ARRL Field Day | Half contest, half community event |
| Jul | IARU HF | Short (24 h); fun warmup for fall season |
| Aug | NAQP CW/SSB | Domestic, casual, 10 hours |
| Sep | CQ WW RTTY | First of the fall contest weekends |
| Oct | CQ WW SSB | The big one; ~30,000 logs worldwide |
| Nov | ARRL Sweepstakes CW (then SSB) | US/Canada only; the most US-specific |
| Nov | CQ WW CW | The other big one; ~25,000 logs |
| Dec | ARRL 10 m | 10 m only; great when the band is open |

The two CQ WW weekends (SSB in October, CW in November) are the largest contests in amateur radio — by far the most participation, the most DX, the most rate. If you've never operated a major contest, CQ WW SSB is the one to start with.

## How a contest QSO is scored

Score = (sum of QSO points) × (sum of multipliers)

QSO points are usually small (1 point for same-continent, 3 for different-continent, 5 for different-country, etc.). Multipliers are bigger — they're sections, zones, prefixes, or countries you've worked. Working a rare multiplier counts 5–10x as much as another duplicate-country QSO.

This is why contesters chase multipliers — a single new mult might be worth 30 QSOs of score.

## How to start

The first contest is just *get on the air during one*. Don't worry about score. Goals for a first-time entry:

1. **Pick a small or moderate contest.** NAQP, IARU, or 10 m contest are good starting points. CQ WW SSB is fun but overwhelming.
2. **Use logging software.** N1MM Logger+ (Windows), WinTest (Windows/Linux), MacLoggerDX (Mac), or fldigi for digital. The software enforces the exchange, prevents duplicates, and calculates score live.
3. **Listen for 15 minutes first.** Hear the cadence, the exchange, what other stations are saying.
4. **Search-and-Pounce first.** Tune the band, find someone calling CQ, answer them. Don't try to "run" your first contest. See [§23-03](23-03-run-vs-sp.md).
5. **Submit your log.** Even if you only made 50 contacts, submit. It costs nothing, helps log-checkers, and qualifies you for participation certificates.

The big-picture skill — knowing what's open, where to listen, when to switch bands — is the same as DXing. The difference is the pace.

## See also

- [§23-03 — Run vs. Search-and-Pounce](23-03-run-vs-sp.md)
- [§23-06 — Timing](23-06-timing.md)
- [§22-05 — Pile-up Etiquette](../22-operating-practice/22-05-pile-up-etiquette.md) (contest pile-ups)
- [§20-01 — HF Band Plan](../20-band-plans/20-01-hf-band-plan.md) (where contests live in each band)
