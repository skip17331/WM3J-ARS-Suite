---
id: 05-03
title: Character Groups
chapter: 05
section: 03
level: simple
status: published
---

# Character Groups

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The Koch method (§05-01) introduces characters one at a time. The order matters: each new character should be sound-different from the ones already learned, and similar characters that might confuse a beginner should be spaced apart in the schedule.

## Koch's original 40-character order

This is the order published in Koch's 1936 paper and used by most rigorous trainers (G4FON, LCWO's Koch mode, CWops Beginner course):

```
Lesson:  1   2   3   4   5   6   7   8   9   10
Add:     K   M   U   R   E   S   N   A   P   T

Lesson:  11  12  13  14  15  16  17  18  19  20
Add:     L   W   I   .   J   Z   =   F   O   Y

Lesson:  21  22  23  24  25  26  27  28  29  30
Add:     ,   V   G   5   /   Q   9   2   H   3

Lesson:  31  32  33  34  35  36  37  38  39  40
Add:     8   B   ?   4   7   C   1   D   6   0

Lesson:  41
Add:     X
```

Forty-one lessons total, one new character per lesson.

> **Advanced —** Koch's specific ordering was empirically derived from his thesis work at TU Berlin. He tested several sequences with student groups and tracked time-to-proficiency. The "K M U R E S" opening was the consistent winner because (1) those characters span the full range of Morse rhythm patterns (short/long, alternating, regular), (2) they let the student build to common simple words quickly (US, ME, EUR, RUSE), and (3) they avoid two characters that differ by only a single dit/dah (like E vs I, or T vs M) early in the curriculum, which is where confusion is hardest to recover from.

## Why this order?

The order isn't random. It follows a few principles:

1. **Maximum sound contrast first.** K (`–·–`) and M (`– –`) are very different. Beginners can tell them apart trivially, which lets them focus on the *speed* of recognition rather than the *task* of recognition.

2. **No similar pairs introduced together.** E (`·`) and I (`··`) sound similar — they're not paired up. T (`–`) and M (`– –`) are spaced apart in the schedule too. This prevents early confusion.

3. **Build toward common words.** By lesson 8 you've got K M U R E S N A — enough for words like SEA, ARM, RUN, ARE, USE. Real text starts producing real words early, which is motivating.

4. **Punctuation early.** The period `.` arrives at lesson 14 because it's needed for callsigns and abbreviations. Comma arrives at 21 because it's less common.

5. **Numbers spread out.** Numbers are the hardest part of Morse for most students. Spacing them across the schedule (5 at lesson 24, 9 at 27, 2 at 28, ...) lets you absorb each one before the next.

## A friendlier alternative — LCWO default

Fabian DJ1YFK's LCWO trainer uses a slightly different order that introduces real-text frequency-common letters earlier:

```
K M R S U A P T L O W I . N J E F 0 Y , V G 5 / Q 9 Z H 3 8 B ? 4 2 7 C 1 D 6 X
```

The differences are minor and the result is comparable. The takeaway: **either order works**, just don't switch mid-stream.

## Character similarity table — what to watch for

Once you've learned a character, you may struggle to distinguish it from a similar one. Knowing which pairs are confusion candidates helps you drill them deliberately:

| Confusing pair | Why |
|----------------|-----|
| E (·) and I (··) | One vs two dits |
| T (–) and M (– –) | One vs two dahs |
| A (·–) and N (–·) | Same elements, opposite order |
| K (–·–) and R (·–·) | Same elements, opposite order |
| U (··–) and V (···–) | Differ by one dit |
| D (–··) and B (–···) | Differ by one dit |
| 5 (·····) and H (····) | Differ by one dit |
| 0 (–––––) and 9 (––––·) | Differ by one dit |

When you find yourself confusing a pair, drill them together for a few sessions. Most learners get past this in days.

## Character mnemonics — why they're a trap

You will see resources that teach Morse with mnemonics:

- E is "dit" (one short)
- T is "dah" (one long)
- A is "ah-LERT"
- B is "BIG bad bad bad"
- ...etc.

**These work for the first few hours and then become a wall.** Each mnemonic adds a translation step: hear the sound → remember the mnemonic → look up the letter. At 5 WPM that step fits in your head. At 12 WPM it doesn't, and you stall.

Koch trainees skip the mnemonics from day one, which is why they sail past 12 WPM where mnemonic learners hit a wall.

## What goes in the schedule

Most trainers stop at the 41 characters above. If you want to be a complete operator, you'll eventually need a few more:

| Character | Pattern | When you'll need it |
|-----------|---------|---------------------|
| Apostrophe `'` | `·––––·` | Some abbreviations |
| Hyphen `-` | `–····–` | URLs, fractions |
| Colon `:` | `–––···` | Time formats |
| Open/close paren `()` | `–·––·` / `–·––·–` | Some QSO formats |
| Plus `+` | `·–·–·` | The "AR" prosign |
| `=` | `–···–` | The "BT" prosign (paragraph break) |
| `~` (or `SK`) | `···–·–` | "Silent key" prosign — end of QSO |

Prosigns (signal-procedure characters) are covered in §19-02.

## Drill suggestions for individual characters

When a character is giving you trouble:

1. **Slow it down briefly** — drop the effective speed by 2 WPM for one session. Don't change character speed.
2. **Drill it with its confusing partner** — if E vs I, run a 5-minute session of just E's and I's randomly.
3. **Sing the rhythm** — say the dits and dahs out loud (`di-dah-dit` for R) while the trainer plays. Helps cement the pattern.
4. **Send it as well as copy it** — if you have a key/paddle, send a few minutes of the troublesome character. Production reinforces recognition.

## See also

- §05-01 — Koch method overview
- §05-02 — Farnsworth spacing for the gaps
- §05-04 — once you have all characters, moving to words and callsigns
- §19-02 — prosigns (procedural characters used in operating)
