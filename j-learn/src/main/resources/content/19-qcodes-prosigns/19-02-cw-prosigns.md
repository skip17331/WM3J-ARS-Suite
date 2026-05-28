---
id: 19-02
title: CW Prosigns
chapter: 19
section: 02
level: simple
status: draft
---

# CW Prosigns

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A **prosign** (procedural signal) is a single Morse pattern that has a procedural meaning beyond the simple letters that compose it. Many prosigns are formed by **concatenating two letters without the inter-letter space** — sending them as one continuous pattern. They function like punctuation marks for a CW transmission: signaling start, end, separation, error, and other procedural cues.

This section lists the prosigns you'll actually encounter in amateur CW operation. Each entry shows the Morse pattern, the conventional written representation (often as overscore-joined letters in formal text, written `KA` here for "K and A run together"), and the meaning.

## Notation conventions

When writing about CW, prosigns are typically marked with an overscore — `\overline{KA}` or `<KA>` — to distinguish them from the two separate letters K and A. In plain text, `<KA>` or just `KA` (with the convention "this is concatenated") is the common form. This document uses `<KA>` for the concatenated form.

## The most-used prosigns

These show up in essentially every CW QSO.

### `<AR>` — End of message

- **Morse**: `· — · — ·` (di-dah-di-dah-dit; the two letters A and R run together).
- **Use**: Signals the end of a transmission, when no specific reply is required from any specific station. Often used to end CQ calls and general transmissions.

Example: `CQ CQ CQ DE WM3J <AR>` — "I am calling CQ, end of transmission."

Sometimes written `AR` or `+`.

### `<SK>` — End of contact (silent key)

- **Morse**: `· · · — · —` (dit-dit-dit-dah-dit-dah; S and K run together).
- **Use**: Signals the end of a complete contact. Both stations acknowledge SK to formally close the QSO.

Example: `73 ES GL DE WM3J <SK>` — "73 and good luck, this is WM3J, end of contact."

This prosign also acquired a sad second meaning in amateur usage: a deceased ham is a "Silent Key" or "SK." The phrase "I heard W1AAA went SK last month" means "I heard W1AAA passed away last month." The two uses don't conflict because context makes them clear.

### `<BT>` — Separator (paragraph break)

- **Morse**: `— · · · —` (dah-dit-dit-dit-dah; B and T run together).
- **Use**: Separates sections of a transmission, like a paragraph break or sentence break. Used to organize information.

Example: `UR RST 599 <BT> NAME MIKE <BT> QTH VA <BT> RIG IS IC-7610 BTU` — "Your RST is 599. (separator) Name is Mike. (separator) Location is Virginia. (separator) Rig is IC-7610. Back to you."

Often written `=` (since the Morse pattern is identical to the equals sign).

### `<KN>` — Specific station only respond

- **Morse**: `— · — — ·` (dah-di-dah-dah-dit; K and N run together).
- **Use**: Signals "Over to YOU specifically; don't let anyone else jump in." Used at the end of a transmission to a specific station, to discourage tail-ending or interruption.

Example: `WM3J DE W1AW <KN>` — "WM3J, this is W1AW; only you respond."

Distinguishes from a plain `K`, which means "any station may respond."

### `K` — Over to any station

- **Morse**: `— · —` (dah-di-dah; the letter K).
- **Use**: At the end of a transmission, signals "over to anyone." More open than `<KN>`.

Example: `CQ CQ DE WM3J K` — "Anyone, please respond."

### `<AS>` — Wait

- **Morse**: `· — · · ·` (di-dah-di-di-dit; A and S run together).
- **Use**: "Stand by; wait." Sent when you need to pause briefly mid-transmission.

Example: `CALL AGN PSE <AS>` — "Call again please; wait." (Followed by a brief pause while you check something.)

### `<BK>` — Break

- **Morse**: `— · · · — · —` (dah-dit-dit-dit-dah-di-dah; B and K run together).
- **Use**: "I am breaking into your transmission." Used to interrupt without taking over the conversation. Often used in nets or QSOs with three or more stations.

Example: Op A is sending a long monologue; Op B sends `<BK>` `BK W1XYZ <BK>` to politely break in.

### `R` — Roger / received

- **Morse**: `· — ·` (di-dah-dit; the letter R).
- **Use**: Confirms receipt. The CW equivalent of saying "Roger" on voice.

Example: `R FB OM TNX FOR CALL` — "Roger, fine business old man, thanks for the call."

### `<HH>` (or `<EEEEEEEE>`) — Error

- **Morse**: Eight dots in a row (`· · · · · · · ·`).
- **Use**: "I made an error; what comes next is the correction." When you mis-key something, send 8 dits and continue with the correct version.

Example: `MY NAME IS MARK 88888888 MY NAME IS MIKE` — corrected mid-transmission.

Less common in modern fast-speed CW (operators just continue and let context fix it), but classic procedure.

## Less-common but useful

### `<CT>` — Start of message (formal)

- **Morse**: `— · — · —` (dah-di-dah-di-dah; C and T run together).
- **Use**: Signals the start of a formal message. Used in NTS (National Traffic System) and similar formal traffic. In casual QSOs, just sending the other station's callsign serves the same purpose.

### `<SN>` — Understood

- **Morse**: `· · · — ·` (dit-dit-dit-dah-dit; S and N run together).
- **Use**: "I understand." Sometimes used in formal traffic handling to confirm understanding without a full Roger.

### `<NJ>` — Shift to wabun (Japanese kana)

- **Morse**: `— · — · — — —` (dah-di-dah-di-dah-dah-dah)
- **Use**: Historical; signals a switch to Japanese Wabun code. Rarely encountered today.

### `<DT>` — Decimal point

- **Morse**: `— · · — ` (dah-dit-dit-dah)
- **Use**: A decimal point or period mid-message. In informal CW, the standard "AAA" prosign or a dot is used; this is more formal.

### `<XR>` — Cross-band

- Used in formal traffic to indicate a cross-band reference.

## Punctuation in CW

Many punctuation marks have standard Morse equivalents that function as prosigns:

| Punctuation | Morse | Notes |
|-------------|-------|-------|
| `.` (period) | `· — · — · —` | Same as `<AAA>` |
| `,` (comma) | `— — · · — —` | Same as `<MIM>` |
| `?` (question mark) | `· · — — · ·` | Same as `<IMI>` |
| `=` (equals / separator) | `— · · · —` | Same as `<BT>` (separator) |
| `+` (plus / end of message) | `· — · — ·` | Same as `<AR>` |
| `/` (slash) | `— · · — ·` | Same as `<DN>` — used for portable callsign suffixes |
| `(` `)` (parentheses) | `— · — — · —` (open and close) | Used in formal text only |
| `:` (colon) | `— — — · · ·` | Used in time/score notation |
| `;` (semicolon) | `— · — · — ·` | Less common |
| `'` (apostrophe) | `· — — — — ·` | "DON'T" → DONT in casual CW |
| `"` (quote) | `· — · · — ·` | Rare |

The amateur convention is to **omit punctuation** for casual conversation (uses `<BT>` for any pause; spells words out). Formal traffic uses the full punctuation set.

## Punctuation prosigns most-used

Of the punctuation set, three appear constantly:

- **`<BT>` / `=`** — paragraph separator. Every CW QSO uses this between thoughts.
- **`?`** — question mark. Used at the end of any question for clarity.
- **`/`** — used in portable callsigns (`WM3J/P` = "WM3J portable"; `WM3J/M` = "WM3J mobile"). Sent as the slash prosign.

## Combining prosigns

Some common combinations:

- `<KN>` followed by waiting silence — full "over to you specifically, I'm done speaking now."
- `<BK>` `<BK>` — emphatic break ("I really need to interrupt").
- `<AR>` `<SK>` — message and contact end together (transmitting a final message that's also the contact-close).

## Prosigns vs. Q-codes

Prosigns and Q-codes serve overlapping purposes:

- **`<KN>`** = end my transmission, specific station respond.
- **"QRZ?"** = "who is calling me?"
- **`<AR>` `K`** = end of transmission, over to anyone.

Prosigns are *procedural* (about the protocol of the transmission); Q-codes are *content* (about the meaning of the message). In an actual QSO, you'll mix them naturally.

> **Advanced —** The "concatenated letter" prosigns are fundamentally Morse trigrams (or longer) that don't decode as any single letter. The receiving operator's brain learns to recognize them as patterns, the same way it learns single letters — but as longer-than-letter recognized units. This is part of why high-speed CW operators read by sound-pattern rather than letter-by-letter; prosigns are just one example of the broader pattern-recognition skill.

## Common mistakes

- **Sending letters with proper inter-letter spacing when meant as a prosign.** Sending K-N (as separate letters) instead of `<KN>` (concatenated) sounds different and means different things.
- **Confusing `<KN>` and `K`.** They're different procedural calls. `<KN>` says "only the specific station;" `K` says "anyone."
- **Skipping `<BT>` and running thoughts together.** A 3-minute monologue without separators is hard to follow; insert `<BT>` between sentences.
- **Using prosigns on FM voice.** "Whiskey-Mike-Three-Juliet, Kilo-November" doesn't make sense on phone — the procedural cue has been replaced by voice convention. Don't translate prosigns to voice.

## See also

- §19-00 — Chapter overview
- §19-01 — Q-codes
- §19-03 — Abbreviations
- §05 — Morse (where prosigns get used)
- §05-07 — Sending practice (drill on prosign sending)
