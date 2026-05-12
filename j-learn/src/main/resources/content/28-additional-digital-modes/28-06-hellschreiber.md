---
id: 28-06
title: Hellschreiber
chapter: 28
section: 06
level: mixed
status: draft
---

# Hellschreiber — Painted-Text Mode

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What it is

Hellschreiber (often just "Hell") is a fax-style text mode invented by Rudolf Hell in 1929 and used by the German military through World War II. It's the oldest mode still in regular amateur use. Instead of encoding characters as bits, Hell transmits each character as a **pixel image** — the receiver "paints" the dots across a strip on the screen.

What makes Hell unique: there is **no character set, no decoding, and no error correction**. The receiver just paints what it hears. If the signal is fuzzy, the printed letters look fuzzy but are still readable by eye. A human reading Hell needs no software beyond a strip-chart display — and historically, Hell receivers were mechanical strip printers.

Modern hams use **Feld-Hell** (the original Rudolf Hell mode), **PSK-Hell**, and **MFSK-Hell** variants — all running in Fldigi. The community is small but enthusiastic, with a "Feld Hell Club" maintaining activity awards.

## How it works

| Property | Value |
|----------|-------|
| Modulation | OOK (on-off keying) — Feld-Hell |
| Symbol rate | 122.5 baud (Feld-Hell) |
| Bandwidth | ~245 Hz (Feld-Hell) |
| Character resolution | 7 columns × 14 rows of pixels |
| Pixels per character | Up to 49 active pixels |
| FEC | None — each character is sent twice (double-print) |
| Throughput | ~25 WPM (Feld-Hell) |
| Decoder | Visual — operator's eye |

Each character is a **7-wide-by-14-tall pixel matrix**. The transmitter sends one column at a time (top to bottom, 14 pixels per column) at 122.5 dots per second. Each pixel is either "on" (transmit a tone burst, ~9 ms) or "off" (silence).

The receiver displays a **strip waterfall** — a horizontal scroll where each new column from the radio is painted at the right edge, and old columns slide left. After 7 columns, a character has been painted; the operator reads the strip like a fax printout.

```
   ██  █     █    █    █
   █ █ █    █     █    █
   █  ██   █      █    █
   █   █  █████  ████ █████
                         (character "K" painted column by column)
```

> ⚙️ **Advanced —** Hell's resilience against fading is statistical, not algorithmic. Each pixel is independent — a fade that knocks out one column leaves the other 6 columns of the same character intact. The human visual system fills in missing pixels by pattern-matching to known letter shapes. This is similar to how we read handwriting through smudges or coffee stains. Computer OCR can match human performance on clean Hell, but humans win on degraded copy because the brain's letter-recognition engine is more forgiving of partial information. This is the one mode where the operator's eyes outperform the digital decoder.

## Why use it

- **Visually readable.** No decoding required. You watch text appear on a waterfall strip and read it.
- **Surprisingly robust against non-fading noise.** Static crashes blur individual pixels; the letter shape survives.
- **Historical / educational.** Demonstrates pre-digital era thinking — a mechanical-strip-printer mode that worked across continents.
- **Niche community.** The Feld Hell Club runs monthly sprints and award programs. Friendly, slower-paced operation.
- **Compatible with any rig.** Standard SSB + audio interface. Same hardware as PSK31.

When to skip it: any time you need **accurate** text copy (FEC modes beat Hell), or contesting, or DX hunting at the noise floor (Olivia / FT8 win). Hell is for the love of it, not for performance.

## Operating

**Software:** **Fldigi** is the standard. Open Fldigi → mode → Hell → Feld Hell (or other variant).

**Calling frequencies (USB dial):**

| Band | Hell activity |
|------|--------------|
| 80m | 3.583 MHz |
| 40m | 7.063 MHz |
| 30m | 10.143 MHz |
| 20m | 14.063 MHz (main activity) |
| 17m | 18.103 MHz |
| 15m | 21.063 MHz |
| 10m | 28.063 MHz |

The "063" pattern across bands is conventional. Activity is light — most days only on scheduled sprints — but 14.063 occasionally has casual QSOs.

**A typical Feld Hell QSO** is no different in content from PSK31 or MFSK16 — keyboard chat. The visual experience is what's different: instead of decoded text appearing in a window, painted characters scroll across a strip.

The "double-print" convention: most operators send each character **twice** — once at the top half of the strip, once at the bottom. This is a built-in form of redundancy: a fade that destroys the top half leaves the bottom half intact, and the operator's eye reads from whichever half is cleaner.

**Power level:** 25–100 W typical. Hell's on-off keying has a 50% duty cycle (the average between "all-on" and "all-off"), so it's gentler on the rig than PSK31. But over-driving still causes splatter.

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| Strip shows gibberish patterns | Wrong sideband | Switch to USB |
| Characters appear offset / drifting | Sample-rate mismatch | Ensure sound card and Fldigi are at 48 kHz |
| Top half clear, bottom half missing | Asymmetric fade — that's the point of double-print | Read from the clean half |
| Strip too dark / bright to read | Display contrast | Adjust Fldigi's Hell display brightness slider |
| Long horizontal streaks | Static crashes during pixel-on | Normal — read through them |
| Character spacing wrong | Software timing off | Restart Fldigi; verify CPU isn't pegged |
| Sent text appears OK locally but others can't read | Audio level wrong or sideband flipped | Verify on a remote SDR; check ALC light |
| Can't find activity | Hell is sparse — check schedule | feldhellclub.org has sprints calendar |

## Common mistakes

- **Expecting computer-style decoded text.** Hell paints; you read. There's no decoded text log unless you copy by hand.
- **Running with double-print disabled.** Double-print costs nothing in air-time (each character is twice as tall, same column count) and dramatically improves readability.
- **Sending at QRO power.** Hell is on-off keying — power peaks aren't the average. Over-driving spreads splatter, which others see as horizontal lines through their own paint strip.
- **Mistaking PSK-Hell for Feld-Hell.** Both are "Hell" variants but use different keying. RsID announces which is which.

## See also

- §28-04 — Olivia (the modern weak-signal alternative)
- §28-05 — MFSK16 / MFSK32 (also Fldigi-supported, FEC-protected)
- §03-03 — PSK31 (uses similar bandwidth; very different modulation)
- §22 — Operating Practice (joining niche-mode communities)
- §05 — Morse code (another human-decodable mode)
