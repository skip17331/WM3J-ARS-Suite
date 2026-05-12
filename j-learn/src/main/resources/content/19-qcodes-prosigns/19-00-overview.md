---
id: 19-00
title: Q-Codes & Prosigns — Overview
chapter: 19
section: 00
level: simple
status: draft
---

# Q-Codes & Prosigns — Overview

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Amateur radio inherited a vocabulary from the early 20th century when most communication was Morse code, transmissions were slow, and every saved keystroke mattered. Three-letter Q-codes ("QTH" for "my location"), CW prosigns ("AR" for "end of message"), abbreviations ("73" for "best regards"), and the phonetic alphabet ("Whiskey Mike Three Juliet") all came from this era.

Most amateur operators today learn just enough of this vocabulary to be on the air comfortably and never go deeper. This chapter is the reference for going deeper: the full Q-code list, prosigns you'll actually hear in CW QSOs, the abbreviations that show up in slow-CW practice, and the NATO/ICAO phonetic alphabet that's now the international standard.

## How the chapter is organized

| § | Topic | What you find |
|---|-------|---------------|
| 23-01 | Q-codes | The amateur-relevant subset of ITU Q-codes — alphabetical list with meanings |
| 23-02 | CW prosigns | Procedural signals (concatenated Morse characters with special meaning) |
| 23-03 | Abbreviations | Common amateur shorthand (CUL, OM, YL, ES, FB, etc.) |
| 23-04 | Phonetic alphabet | NATO/ICAO standard plus alternative civilian sets |

## Why this stuff still matters

Even on FT8, you'll see Q-codes in QSO summaries ("RR73" — "Roger Roger 73"). Even on FM voice, you'll hear "QSO" used as a noun ("we had a great QSO"). Q-codes have outlived their original CW context and become part of amateur English.

A few specific reasons to know this material:

- **CW operating** is impossible without prosigns and Q-codes. A typical CW QSO is 60-80% Q-codes, prosigns, abbreviations, and the rest is callsigns and signal reports.
- **Voice operating** uses Q-codes informally ("My QTH is Florida"), and the phonetic alphabet during weak-signal exchanges or to spell unusual words.
- **Digital modes** (FT8, RTTY, JS8Call) embed traditional shorthand in their predefined message templates.
- **Contest exchanges** use abbreviations heavily; knowing them avoids confusion at high speed.
- **Emergency/tactical communications** rely on phonetics for accuracy when conditions are poor.

## A short history

The **Q-code** system was created by the British Postmaster General in 1909 for shipboard wireless operators, then formalized by the International Radiotelegraph Convention in 1912 (the same year as the Titanic, which used Q-codes during its sinking). Each three-letter Q-code substitutes for an entire phrase: "QRT" instead of "Stop sending; cease all communications."

The same code can usually be **a question or a statement**, distinguished by context or by appending "?":

- "QTH" (statement): "My location is..."
- "QTH?" (question): "What is your location?"

The full Q-code dictionary contains hundreds of codes used by maritime, aeronautical, and military services. The amateur subset is much smaller — perhaps 30 codes you'll regularly see, of which 10 you'll use daily.

**CW prosigns** evolved from operator practice in the early 1900s — a way of compressing common procedural signals (start of transmission, end of message, error correction) into single-burst Morse equivalents. Many prosigns are *concatenated* characters: "AR" sent without the inter-letter gap, becoming a unique pattern.

**Phonetic alphabets** were standardized later. The current international standard, ICAO/NATO ("Alpha Bravo Charlie..."), was adopted in the 1950s after extensive testing for clarity in poor radio conditions. Earlier amateur use included "Adam Boy Charlie..." and various national variants; the ICAO version has now displaced these in international communication.

## A quick example

A simple CW QSO between two stations:

```
CQ CQ CQ DE WM3J WM3J K
```
"CQ — anyone calling — DE (this is) WM3J — K (over to you, anyone listening)"

```
WM3J DE W1AW K
```
"WM3J this is W1AW, over."

```
W1AW DE WM3J GE OM TNX FOR CALL UR RST 599 IN VA NAME MIKE BTU W1AW DE WM3J K
```
"W1AW this is WM3J. Good evening, old man. Thanks for the call. Your RST is 599 in Virginia. Name is Mike. Back to you. W1AW this is WM3J, over."

```
WM3J DE W1AW R FB MIKE UR 569 IN CT NAME JIM BTU WM3J DE W1AW K
```
"WM3J this is W1AW. Roger. Fine business, Mike. Your signal is 569 in Connecticut. Name is Jim. Back to you. WM3J this is W1AW, over."

```
W1AW DE WM3J R TNX QSO 73 ES GL DE WM3J SK
```
"W1AW this is WM3J. Roger. Thanks for the QSO. 73 (best regards) and GL (good luck). DE WM3J. SK (end of contact)."

Almost every word in that exchange is from one of this chapter's reference sections.

## What you will not learn here

- **Morse code itself** — see §05 for learning to send and copy CW.
- **Phonetic spelling of weird foreign DX prefixes** — sometimes contest stations spell their callsigns phonetically with creative variations; this chapter covers the standard.
- **Aviation, marine, or military Q-codes** — many beyond the amateur subset exist; this chapter is amateur-focused.
- **Digital-mode "shorthand"** — JS8Call's compressed messages, FT8's predefined fields, etc., are mode-specific (see §03).

## See also

- §05 — Morse (learning to send/copy)
- §03 — Digital modes (where some of this vocabulary appears in modern usage)
- §04 — Repeaters & bandplans (calling conventions on FM)
- §21 — EmComm (procedure-heavy use of phonetics)
