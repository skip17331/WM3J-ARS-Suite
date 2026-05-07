---
id: 03-02
title: Farnsworth Spacing
chapter: 03
section: 02
level: simple
status: draft
---

# Farnsworth Spacing

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Farnsworth spacing is the trick that makes the Koch method (§03-01) bearable: send each individual character at full target speed, but **stretch the gap between characters** so a beginner has time to think. As you improve, you shrink the gap until you're at the target effective speed too.

## Two speeds, not one

When a CW signal is sent, two timing things matter:

1. **Character speed** — how fast each individual letter is sent (the spacing of the dits and dahs *within* the letter).
2. **Effective speed** — how many words per minute the whole transmission averages out to (which depends on character speed *plus* the gaps between characters and words).

In standard CW, character speed and effective speed are equal. Send at 20 WPM, and the gaps between characters are at 20 WPM rates too.

In **Farnsworth spacing**, character speed is held high (say, 20 WPM) while the inter-character gap is stretched to slow the effective speed (say, to 10 WPM). The character "K" still sounds like a 20 WPM K — same dit-dah timing — but you have twice as long after each character before the next one arrives.

## The PARIS standard (where the math comes from)

The international convention defines:

- A **dit** = 1 unit time
- A **dah** = 3 units (3 × dit)
- Gap between dits/dahs **within** a character = 1 unit
- Gap **between characters** = 3 units
- Gap **between words** = 7 units

The reference word is **PARIS** — a five-letter word that takes exactly 50 units when sent at standard timing (including the trailing word gap). At any chosen WPM rate:

```
unit time (sec) = 60 / (WPM × 50) = 1.2 / WPM
```

So at 20 WPM:
- 1 unit = 60 ms
- A dit = 60 ms
- A dah = 180 ms
- Between characters = 180 ms
- Between words = 420 ms

> ⚙️ **Advanced —** The "PARIS" word is convention; some texts use "CODEX" (60 units) for slightly slower-rated speeds. Both are arbitrary; the WPM you read on a radio's display assumes PARIS unless documented otherwise. The mismatch between conventions causes the well-known phenomenon where a "20 WPM" recording from one source feels noticeably faster or slower than another.

## How Farnsworth changes things

In Farnsworth mode, you specify two numbers: a **character speed** (often 20 WPM) and an **effective speed** (whatever you can copy comfortably). The trainer keeps the dit/dah timing of each character at the high speed, but stretches the inter-character and inter-word gaps so the *average rate* of the message is your slow effective speed.

Numerical example, character speed 20 WPM, effective speed 10 WPM:

- Character internal timing: 60 ms per unit (20 WPM rate)
- A K is `dah-dit-dah` = 180-60-180 ms with 60 ms gaps inside = 540 ms total
- Inter-character gap stretched to **double** the standard (so the average comes out right): about 360 ms (vs. 180 ms standard)
- Word gap stretched proportionally

Result: each letter sounds fast and crisp, but you have a half-second between letters to write down the previous one and prepare for the next. As you progress, you reduce the inter-character stretch — eventually arriving at standard 20 WPM with no Farnsworth at all.

## Why this works for learning

The key research finding (verified many times since Koch's original work): **the brain learns auditory recognition at one speed**. If you train your ear to recognize the *sound of K at 20 WPM*, that recognition transfers to faster effective speeds without re-learning. If instead you train at 5 WPM character speed and slowly speed up, the brain locks in the *slow* recognition pattern and has to start over when you try to go faster.

Farnsworth spacing lets you train at a speed that's slow enough not to overwhelm a beginner, while exercising the right neural pattern (fast character recognition) from day one.

## How to use it in practice

When you start training:

- **Character speed**: 20 WPM (or 25 if you're ambitious).
- **Effective speed**: start at 5 WPM. Whatever you can copy at 90% accuracy.

As you improve:

- Bump effective speed by 1 WPM whenever you reliably copy at 90%+.
- **Don't change character speed.** Leave it at 20.
- When effective speed catches up to character speed (you're at 20/20), you've graduated from Farnsworth. Now you can either stay at 20 WPM standard or push character speed up to 25 and start over.

## Setting Farnsworth in trainers

| Trainer | Where to set it |
|---------|-----------------|
| **LCWO** | Settings tab → "Effective WPM" (separate from "Character WPM") |
| **G4FON** | Speed dialog → top slider is character, bottom is effective |
| **Morse Mania** | Settings → "Use Farnsworth spacing" toggle |
| **Hams' radios** | Most modern transceivers' built-in keyer support Farnsworth too — check your menu under "CW timing" |

## A practical day

Here's a typical week 4 practice session, using LCWO:

```
Character speed:   20 WPM
Effective speed:   8 WPM
Characters:        K M U R E S N A P T L W I .   (14 chars)
Session:           5 min × 6 with 1-min breaks
Goal:              90% accuracy on a 5-min run
```

After hitting 90% on this set for two days in a row:

1. Add character #15 (J). Drop effective speed back to 7 WPM for one session to absorb the new character.
2. Once 90% with J included, push effective speed back to 8.
3. Keep going.

## Common mistakes

- **Leaving Farnsworth on forever.** It's a learning aid, not a destination. Move past it once your effective speed is within 30% of your character speed.
- **Reducing character speed when you struggle.** Don't. Keep character speed pinned and stretch the gap instead.
- **Setting character speed too high too early.** 20 WPM is a fine target for most adults. Push to 25 only after you can copy 18–20 WPM effective.
- **Using effective-speed-only trainers.** Some old apps don't separate the two. Avoid them; they teach the slow-recognition pattern that's hard to undo.

## See also

- §03-01 — Koch method, the technique Farnsworth is built into
- §03-07 — tracking effective speed over time
