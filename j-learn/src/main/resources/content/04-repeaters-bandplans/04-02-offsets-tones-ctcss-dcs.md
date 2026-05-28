---
id: 04-02
title: Offsets, Tones, CTCSS, DCS
chapter: 04
section: 02
level: simple
status: draft
---

# Offsets, Tones, CTCSS, DCS

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

To talk through a repeater, your radio needs three settings right: the **frequency** (output), the **offset** (which tells it the input frequency), and a **tone** (which proves to the repeater that you're a real ham and not interference). Get any one of these wrong and the repeater either won't hear you, or won't repeat for you.

## Standard offsets

By long convention, every band has a default repeater offset:

| Band     | Standard offset | Example output | Example input |
|----------|----------------:|----------------|---------------|
| 6 m      | –1 MHz          | 53.030         | 52.030        |
| 2 m      | –600 kHz (low half) / +600 kHz (high half) | 146.940 → 146.340 | 147.300 → 147.900 |
| 1.25 m   | –1.6 MHz        | 224.500        | 222.900       |
| 70 cm    | +5 MHz (or –5)  | 442.500        | 447.500       |
| 33 cm    | –12 MHz         | 927.000        | 915.000       |
| 23 cm    | –20 MHz         | 1287.000       | 1267.000      |

For 2 m specifically, the convention is:
- Outputs **144.5–145.5 MHz**: input is +600 kHz higher.
- Outputs **145.5–146.0 MHz**: input is –600 kHz lower.
- Outputs **146.0–147.0 MHz**: input is –600 kHz lower.
- Outputs **147.0–148.0 MHz**: input is +600 kHz higher.

Most modern radios have an "auto offset" or "ARS" (Automatic Repeater Shift) feature that picks the right direction based on the frequency. Trust it for normal repeater frequencies; check it manually for unusual ones.

## What "tone" actually means

The tone is a sub-audible signal your radio transmits along with your voice. The repeater listens for this tone before it activates the transmitter. If the tone is missing or wrong, the repeater hears your audio but won't repeat it — your signal gets ignored as if you weren't there.

Why? Two reasons:

1. **To stop accidental keyups** — pagers, baby monitors, weather noise, distant repeaters on the same input frequency. Without tone, any random RF on the input would key the repeater and make it un-usable.
2. **To select the right repeater** when two repeaters share the same frequency in different geographic areas (you'd be amazed how often this happens).

There are two flavors of tones in common use:

### CTCSS (Continuous Tone-Coded Squelch System)

A single low-frequency audio tone, transmitted continuously alongside your voice. The audible range of speech is ~300 Hz upward, and CTCSS tones live in the 67–254 Hz range — low enough that your ear rarely notices but the repeater can detect.

Also called **PL** ("Private Line", a Motorola trademark) or just **tone**. They all mean the same thing.

The standard CTCSS tones are a finite list (you don't pick any random frequency):

```
67.0   71.9   74.4   77.0   79.7   82.5   85.4   88.5
91.5   94.8   97.4  100.0  103.5  107.2  110.9  114.8
118.8  123.0  127.3  131.8  136.5  141.3  146.2  151.4
156.7  159.8  162.2  165.5  167.9  171.3  173.8  177.3
179.9  183.5  186.2  189.9  192.8  196.6  199.5  203.5
206.5  210.7  218.1  225.7  229.1  233.6  241.8  250.3
254.1
```

If a repeater directory says "Tone: 100.0", you set CTCSS encode to 100.0 Hz. Done.

### DCS (Digital Coded Squelch)

A continuous low-rate digital code (134.4 bits per second) instead of an audio tone. Same idea, different mechanism. DCS codes are three-digit numbers like 023, 047, 125. Less common than CTCSS; some repeaters use DCS to be different.

If a directory says "DCS: 023", you set the digital squelch encode to 023.

## Encode vs decode

Your radio has separate settings for "tone encode" (TX) and "tone decode" (RX):

- **Encode (TX)**: your radio sends the tone whenever you transmit. Required by the repeater.
- **Decode (RX)**: your radio's speaker stays muted unless it hears the tone. Useful for filtering out interference.

For most repeater work you only need **encode**. Set encode to the repeater's tone, leave decode off. If decode is on with the wrong tone, you won't hear the repeater either.

Some clubs run repeaters that **transmit a tone too** so members can use decode to mute the speaker for everything except this repeater. The tone the repeater transmits is usually (but not always) the same as the input tone.

## Putting it all together

A typical repeater entry in a radio's memory:

```
Memory:    1
Name:      W3MFW
Frequency: 146.940 MHz       (← what you receive)
Offset:    -0.6 MHz          (transmit on 146.340 MHz)
TX Tone:   100.0 Hz CTCSS    (encode while transmitting)
RX Tone:   off               (don't filter received audio)
Mode:      FM
```

When you key up:
1. Radio switches to TX, transmits on 146.340 MHz with 100.0 Hz CTCSS overlaid.
2. Repeater on the hilltop hears the carrier with the right tone, decides "this is a real user", and starts transmitting on 146.940 MHz.
3. After you let go, repeater keeps transmitting for ~2 more seconds (the "tail"), then drops.

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| Repeater never repeats your voice | Wrong tone, or tone off | Check encode setting matches the directory |
| Some repeaters work, others don't | Different tones, you only programmed one | Each repeater needs its own tone in memory |
| You hear the repeater fine but it never repeats you | Your TX is too weak — high antenna height needed | Try from a different location; you may be in a shadow |
| You hear nothing on RX even though there's traffic | Decode tone is on with wrong value | Turn decode off |
| Your radio chirps when you press PTT but never transmits | Battery low, or PTT timeout | Check battery, recycle the PTT |
| You "double" — your audio comes through chopped up | Two stations transmitting at once on the input | Standard etiquette; one of you back off |

## What if I don't know the tone?

The repeater won't tell you — it'll just sit there ignoring you. Resources:

- **RepeaterBook** (`repeaterbook.com`) — crowd-sourced repeater database, tones included, by area.
- **ARRL Repeater Directory** — paid annual book, very thorough.
- **Local club website** — ask Google "ham repeater [town]"; almost every active repeater has a club page.
- **Listen for the ID** — if you hear the repeater's CW ID it's probably active, and someone on it can tell you the tone.

> **Advanced —** Tone-Squelch Reverse-Burst (TSRB) is a feature some commercial radios use to suppress the squelch tail when releasing PTT. It briefly reverses the CTCSS tone phase before unkeying so the repeater controller drops out cleanly. Most ham repeaters don't bother with this, but if you're hearing an annoying "ker-chunk" tail, your repeater might benefit from a controller upgrade. Also: some Motorola controllers support **two-tone paging** in addition to CTCSS — a sequential tone burst that wakes a specific receiver. This is unusual on amateur repeaters but appears on commercial-converted gear.

## See also

- §04-01 — what a repeater actually does
- §04-08 — programming non-standard offsets
- §04-07 — frequency coordination (why the same tone on the same frequency in two cities is OK)
