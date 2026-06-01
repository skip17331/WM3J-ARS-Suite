---
id: 22-04
title: Calling CQ and the Standard QSO Flow
chapter: 22
section: 04
level: simple
status: published
---

# Calling CQ and the Standard QSO Flow

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A **CQ** is amateur radio's "anyone want to talk?" — an open call to any listening station. It's how most QSOs start when you don't have a specific contact in mind. The format is conventional, the response is conventional, and the flow that follows is well-established. Once you've done a dozen CQ contacts you'll have it for life.

## The CQ format

### Voice (SSB)

```
"CQ CQ CQ this is WM3J Whiskey Mike Three Juliet
 calling CQ and standing by."
```

Three CQs followed by your call (twice — once plain, once phonetic) and a sign-off phrase. Variations:

```
"CQ DX CQ DX from Whiskey Mike Three Juliet, listening."
[targeting DX only]

"CQ 20 meters CQ 20 meters from WM3J, anyone for a chat."
[targeting on-band casual contacts]

"CQ contest CQ contest WM3J."
[contest CQ — short]
```

### Voice (FM repeater)

Don't say "CQ" on a repeater — that's HF/SSB convention. On FM:

```
"WM3J listening on 146.94"
[invites any listening station]

"WM3J monitoring"
[same intent, briefer]
```

Repeaters work as scheduled gathering points; "CQ" gets you puzzled looks. "Listening" or "monitoring" gets you contacts.

### CW

```
CQ CQ CQ DE WM3J WM3J WM3J K
```

The "DE" means "from" (it's "this is" in CW shorthand). Three CQs, your call three times, "K" (end of transmission, your turn).

For more specific calls:

```
CQ DX DE WM3J K               [calling DX only]
CQ DE WM3J WM3J K             [generic]
CQ TEST DE WM3J WM3J 5NN 003 K [contest]
```

### Digital (FT8 / FT4)

```
CQ WM3J FM18                  [your call + grid]
CQ NA WM3J FM18                [North America only]
CQ DX WM3J FM18                [DX-only]
```

Format is fixed by WSJT-X. "CQ NA" means "calling North America"; "CQ DX" means "anyone outside my country." Auto-formatted by the software.

## What happens after the CQ

The other station responds with their callsign:

### Voice (SSB)

```
You:     "CQ CQ CQ this is WM3J calling CQ"
Other:   "WM3J this is W1ABC, Whiskey One Alpha Bravo Charlie"
You:     "W1ABC this is WM3J, thanks for the call. You're 5-9
          here in Stafford, Virginia. Name is Steve. Over to
          you."
Other:   "Steve, copy 5-9. Name is Karen, QTH is Newport,
          Rhode Island. Pleasure to meet you."
You:     "Karen, pleasure to meet you too. Beautiful weather
          here, 65 degrees and clear. What's the rig on your
          end? Over."
[Conversation continues...]
You:     "73 Karen, see you down the log. WM3J clear."
Other:   "73 Steve, W1ABC clear."
```

### CW

```
You:     CQ CQ CQ DE WM3J WM3J WM3J K
Other:   WM3J DE W1ABC W1ABC K
You:     W1ABC DE WM3J UR 5NN 5NN STAFFORD VA STAFFORD VA NAME STEVE K
Other:   WM3J DE W1ABC UR 5NN NAME KAREN QTH NEWPORT RI K
You:     R FB KAREN TKS QSO 73 SK WM3J
Other:   73 ES SK W1ABC
```

CW abbreviates heavily: "5NN" = "599 (signal report)," "FB" = "fine business," "TKS" = "thanks," "QSO" = "contact," "ES" = "and," "SK" = "end of contact."

### Digital (FT8) — automated

```
TX: CQ WM3J FM18
RX: WM3J KK6XX -10        (his report of your signal)
TX: KK6XX WM3J R-08        (R + your report of his signal)
RX: WM3J KK6XX RR73        (acknowledgment + 73)
TX: KK6XX WM3J 73          (final 73)
```

WSJT-X automates the full sequence. You click their callsign in the decode list and the software handles transmissions. Total time: ~90 seconds for a complete contact.

## The "standard exchange"

Most casual QSOs include:

| Field | Why |
|-------|-----|
| **Signal report (5NN, RST)** | Tells the other operator how readable you are |
| **Your name** | Personal courtesy |
| **Your QTH (location)** | Geographic interest, geographic confirmation |
| **Your rig** (optional) | For amateur-radio talk; not required |
| **Your antenna** (optional) | Same |

Beyond the standard exchange, conversations can go anywhere — politics is rare (FCC rules discourage it), but weather, equipment, hobbies, travel, family, and current events are all fair game.

### Signal reports — RST and the "5-9 default"

The **RST** scale rates a signal:

| Letter | Meaning | Scale |
|--------|---------|-------|
| **R** (Readability) | How well you can copy | 1 (unreadable) to 5 (perfectly readable) |
| **S** (Strength) | How loud the signal is | 1 (faint) to 9 (extremely strong) |
| **T** (Tone) | CW-only — quality of the keyed tone | 1 (extremely rough) to 9 (perfect tone) |

A "5-9" signal is "perfectly readable, extremely strong" on voice. On CW it's "5-9-9" with the additional T component.

In practice, casual operators give "5-9" as a default and only deviate when something's notably different. Honest reports are better but less common than the cheerful default.

## When to call CQ vs answer one

**Call CQ** when:

- You have time and want to chat.
- You want to put a particular grid square on the air (DX from your QTH is interesting to the other end).
- You're operating a special event station or activity.
- You're trying out a new antenna or band.

**Answer a CQ** when:

- You hear someone calling and have a few minutes to chat.
- You need a contact for an award (DXCC, WAS, grid square chasers).
- You're testing your station and want a quick verify.

In a busy band (Field Day, contest, weekend afternoons), there are far more CQ-callers than CQ-answerers in normal practice — so a single CQ usually gets a response.

## Length of CQ vs response

A common mistake: the CQ is too short or too long.

- **Too short**: "CQ WM3J" once is not enough; a listener can miss it during their bandscan.
- **Too long**: 30 seconds of CQ is excessive — listeners grow impatient and move on.
- **Just right**: 5-15 seconds is typical. Three CQ-and-callsign cycles is the conventional length.

## After a CQ — how long to wait before re-calling

After a CQ, listen for **at least 5-10 seconds**. If no response:

- Move on to a different frequency, OR
- Re-call CQ on the same frequency.

Calling CQ on the same frequency repeatedly (more than 4-5 times) can sound desperate and irritate listeners. A few CQs, listen, move if nobody's biting.

## Special CQ targets

Some CQ formats target specific groups:

- **CQ DX** — only DX stations (outside your country). Don't call back if you're in the same country.
- **CQ NA / EU / VK** — only the named region.
- **CQ MM (Maritime Mobile)** / **CQ AM (Aeronautical Mobile)** — only that station type.
- **CQ POTA / SOTA** — Parks-on-the-Air or Summits-on-the-Air activities.
- **CQ contest** — contest operators only; expects a contest exchange.

If a specific call doesn't apply to you, don't answer. Calling back to "CQ DX" from inside the same country is rude and wastes both stations' time.

## QRZ — when someone calls back but you didn't catch their callsign

If someone responds to your CQ but you couldn't make out the callsign:

```
"WM3J QRZ?"     [Voice]
QRZ DE WM3J K   [CW]
```

QRZ asks "who is calling me?" The other station re-transmits their callsign. After 1-2 retries, if you still can't copy, "QSY 73" politely ends the attempt.

## Common CQ mistakes

- **Talking through your CQ.** "CQ this is WM3J also known as Steve operating from here in Stafford Virginia just trying out a new antenna and would love to chat with anyone listening" is too long. Keep it brief.
- **Mumbling phonetics.** "Whiskey Mike Three Juliet" — clear, slow, deliberate. Phonetics aren't a flourish; they're an accuracy tool.
- **Calling CQ on top of someone else.** Listen first; never CQ on a frequency that's already in use.
- **Repeating your call too few times.** Two callsigns is conventional in voice CQ, three in CW. One pass through is too short.
- **Responding to "CQ DX" from inside the DX station's country.** They're calling outside their country only.
- **Forgetting to listen.** After CQ, wait. Don't immediately call CQ again — give listeners time to grab the mic.
- **Inverted phonetics.** "WM3J" is "Whiskey Mike Three Juliet" — not "Whiskey Mike Three Joliet" (which is a city name, not a phonetic letter).

> **Advanced —** "CQ" is a Western Union telegraph operator's "all stations" call from the 1880s, predating amateur radio by several decades. The phonetic alphabet ("Alpha Bravo...") is the NATO/ICAO standard adopted in 1956; the older RAF and US military versions ("Able Baker...") are sometimes still heard from older operators. The "5NN" CW signal-report shorthand exists because typing "599" three times per contact is tedious — the cut numbers (T=0, A=1, U=2, V=3, 4=4, 5=5, 6=6, B=7, D=8, N=9) saved keystrokes; "5NN" reads as "599." Modern contesting and DX work uses 5NN exclusively for both readability and speed.

## See also

- §22-01 — "Is the frequency in use?" (the call before the CQ)
- §22-03 — Identifying (rules during the QSO)
- §22-05 — Pile-up etiquette (the special case for DX and contests)
- §19-04 — Phonetic alphabet
- §19-01 — Q-codes (QRZ, QSY, etc.)
