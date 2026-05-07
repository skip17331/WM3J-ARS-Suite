---
id: 02-08
title: Custom Offset Calculator
chapter: 02
section: 08
level: simple
status: draft
---

# Custom Offset Calculator

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Most repeaters use the **standard offsets** documented in §02-02 (–600 kHz on 2 m, +5 MHz on 70 cm, etc.). But every now and then you'll find one that doesn't — usually because of crowding, regional coordination decisions, or because the repeater was built before the conventions settled. Programming a custom offset by hand is straightforward; the math is grade-school subtraction.

## What "offset" means in your radio's menu

Every repeater memory needs three frequency-related fields:

| Field | What it controls |
|-------|------------------|
| **RX frequency** (or just "frequency") | What you hear — the repeater's output |
| **Offset / shift direction** | + or – — does the radio transmit above or below the RX? |
| **Offset amount** | How far in frequency to shift when transmitting |

When you press PTT, the radio computes:

```
TX frequency = RX frequency  ±  offset amount
```

Most radios let you enter the offset in MHz or kHz. Some menus call it "shift" or "duplex." All the same thing.

## Standard offsets recap

| Band  | Offset direction | Offset amount |
|-------|------------------|---------------|
| 6 m   | – (or +)         | 1.000 MHz     |
| 2 m (low) | + | 600 kHz |
| 2 m (mid–high) | – | 600 kHz |
| 2 m (147+) | + | 600 kHz |
| 1.25 m | – | 1.600 MHz |
| 70 cm | + (or –)         | 5.000 MHz     |
| 33 cm | – | 12.000 MHz |
| 23 cm | – | 20.000 MHz |

When the directory says "146.940 –" it means RX 146.940, offset direction –, **standard 600 kHz** for 2 m → TX 146.340.

## Programming a custom offset by hand

Step 1. Look up the repeater. Suppose you find a non-standard 2 m repeater listed as:

```
Output: 145.110
Input:  144.310
```

Step 2. Compute the offset:

```
145.110 - 144.310 = 0.800 MHz
```

Step 3. Note the direction. Input is **lower** than output, so the offset direction is **–**.

Step 4. Program your radio:

```
RX:        145.110
Offset:    –
Offset amount: 0.800 MHz   (or 800 kHz)
TX tone:   (whatever the directory specifies)
```

Step 5. Test by keying up briefly and listening for the squelch tail. If the repeater doesn't acknowledge, double-check: tone right? offset direction right? Many radios have a "show TX freq" function that displays exactly what frequency the radio will transmit on — use it.

## The math, written out

If you have:

- An **output (RX)** frequency `Fo`
- An **input (TX)** frequency `Fi`

Then the offset is:

```
Offset amount = | Fo − Fi |
Offset direction = '–' if Fi < Fo, else '+'
```

Numerical example for a 70 cm repeater:

```
Output: 442.300
Input:  447.300
Offset = | 442.300 − 447.300 | = 5.000 MHz   (standard for 70 cm)
Direction: + (because 447.300 > 442.300)
```

And a non-standard one:

```
Output: 444.075
Input:  449.075
Offset = 5.000 MHz, direction +   ← actually standard, just the shift sign is +5 not –5
```

This is the source of regular confusion: 70 cm in some areas uses **+5 MHz** (output low, input high), in others **–5 MHz** (output high, input low). Always check the directory.

## Cross-band repeaters

A few repeaters listen on one band and transmit on another:

```
Output: 146.940 (2 m)
Input:  442.500 (70 cm)
```

These are not "offset" repeaters in the standard sense — they're **cross-band**. Your radio has to be capable of receiving on one band while transmitting on another (most modern dual-band radios are). Programming differs:

- Some radios let you enter "different band" by setting offset direction to "split" and offset amount to the difference between bands (here, 296 MHz).
- Others use a separate "split mode" or "VFO A/B split" feature.
- A few cheap dual-banders simply don't support cross-band repeaters.

Read your radio's manual under "split frequency operation."

## Reverse-detect: which side am I on?

Sometimes you'll hear a strong signal on what you thought was the output frequency, but you can't tell if it's the repeater or another user transmitting on the input.

Most radios have a **reverse** function — temporarily swap RX and TX frequencies. Press it; if the signal disappears, you were hearing the actual output (and the other party was on input talking to the repeater). If the signal stays, you're hearing someone transmitting on what you thought was the output frequency, which means **either** they're using simplex on the repeater output (rude but legal) **or** the repeater is doing something unexpected (link traffic, weather alert, etc.).

## Common gotchas

- **Standard offset auto-detect** (ARS) on most modern radios picks the standard offset *direction* based on frequency, but assumes the standard *amount*. For non-standard offsets you'll need to set the offset amount manually.
- **Offset direction in some Yaesu menus** is shown as "DUP–" / "DUP+" / "OFF". On Icoms it's "DUP–" / "DUP+" / blank. On Kenwoods it's "+" / "–" / "off". All mean the same thing.
- **Negative offset on the wrong half of a band** — the radio may helpfully refuse to transmit because the resulting TX frequency is outside the band edge. Check what the radio is computing for TX before assuming the radio is broken.
- **Old repeaters with 1 MHz offsets on 2 m** — a few survive from before the standard. Check the directory.

## A useful sanity check

Write a one-line note in your radio's memory channel comment field:

```
Memory 12: W3MFW 145.11- T100  (RPT TX 144.31)
```

Saves the next person who programs a similar repeater (or your future self at 2 AM during an event) from having to re-do the math.

## See also

- §02-01 — repeater basics
- §02-02 — standard offsets and tones
- §02-07 — coordinated frequency assignments (which sometimes give non-standard offsets to fit a market)
