---
id: 28-04
title: Olivia
chapter: 28
section: 04
level: mixed
status: draft
---

# Olivia — Sub-Noise-Floor Manual Chat

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What it is

Olivia is a **multi-tone MFSK** mode designed by Pawel Jalocha (SP9VRC) in 2005 for reliable manual chat under extremely weak signal conditions. Its design goal: copy 100% of typed text at signal levels where the human ear can't hear the carrier at all. It achieves this by combining many parallel low-rate tones with forward error correction.

Unlike FT8 — which is fast but locked to a tiny fixed message vocabulary — Olivia is **free-form keyboard chat**. You type, the other operator reads. It's slow (10–25 WPM, depending on configuration), but as long as the band is open at all, it gets through.

Olivia is the favorite mode of operators who want **conversation under marginal conditions**: low-power QRP, indoor antennas, low solar flux, polar paths. It also has a small but loyal community ("the Olivia chat group") that runs daily nets in English and Spanish on 14.107 MHz.

## How it works

| Property | Value |
|----------|-------|
| Modulation | Multi-tone FSK (MFSK) |
| Number of tones | 8, 16, 32, 64, 128, or 256 |
| Bandwidth | 125, 250, 500, 1000, or 2000 Hz |
| Typical setting | "Olivia 8/250" (8 tones, 250 Hz wide) |
| Other common settings | "16/500", "32/1000" |
| Symbol rate | Bandwidth ÷ tones (e.g. 8/250 = 31.25 baud) |
| FEC | Walsh function block code |
| Decoder threshold | ~−14 dB SNR (8/250); ~−12 dB (32/1000) |
| Throughput | 10–60 WPM depending on configuration |
| Character set | 7-bit ASCII (full keyboard) |

The naming convention is **`tones/bandwidth`** — so "Olivia 16/500" means 16 tones across 500 Hz. Both ends must use the same setting; mismatched configurations don't decode.

The trade-offs:

- **More tones** = slower per character, but each tone has more time-energy and the FEC has more redundancy → **deeper into the noise**.
- **Wider bandwidth** = faster, but more vulnerable to selective fading and QRM.

For weak-signal CW-replacement: **8/250** is the canonical choice. For ragchew under moderate conditions: **16/500** balances speed and robustness. For DX nets where everyone has a strong signal: **32/1000** runs near typing speed.

The mode uses **Walsh-Hadamard FEC**: each character is spread across 64 bits transmitted as 64 separate symbols. Even with half the symbols destroyed by fading or QRM, the decoder reconstructs the character.

> ⚙️ **Advanced —** Olivia's selectivity in the time domain comes from its **64-tone-time block**: each ASCII character occupies a 64-symbol-by-N-tone block, and the decoder correlates the received audio against all possible character codes. A character is reported only if its correlation exceeds the next-best candidate by a configurable margin (typically 1.4×). This is why Olivia "either copies or doesn't" — you rarely see partially-garbled text. The decoder either has enough signal energy to commit, or it stays silent.

## Why use it

- **Reliability below the noise floor.** Olivia copies signals that are inaudible.
- **Free-form text.** Unlike FT8, you can talk about anything.
- **No clock requirement.** Olivia, unlike FT8, doesn't need NTP-synced computer time.
- **Forgiving of poor antennas.** A wet noodle in the attic still copies Olivia from across the continent at solar minimum.
- **Multilingual.** Olivia uses 7-bit ASCII — works fine for English, but the community also operates in Spanish, Portuguese, German, and others.

When to skip it: contests (too slow), DX hunting where everyone uses FT8 (you'll be alone), and any case where structured messaging beats keyboard chat (Winlink, JS8Call).

## Operating

**Software:** **Fldigi** (cross-platform, open source) is the standard. It supports all Olivia configurations and includes mode auto-detection ("RsID"). MultiPSK (Windows, paid) also supports Olivia.

**Calling frequencies (USB dial + audio offset):**

| Band | Center frequency | Typical config |
|------|------------------|----------------|
| 80m | 3.580 MHz | 8/250 |
| 40m | 7.073 MHz | 8/250 |
| 30m | 10.142 MHz | 16/500 |
| 20m | 14.107 MHz | 8/250 (main "Olivia chat" net) |
| 17m | 18.103 MHz | 8/250 |
| 15m | 21.130 MHz | 16/500 |
| 10m | 28.120 MHz | 8/250 |

The audio tones land in the standard SSB passband; tune the rig 1–2 kHz below the activity and place the signal around 1500 Hz audio.

**RsID (Reed-Solomon Identifier):** a short header burst that announces the mode and frequency. Fldigi can be set to send RsID with every TX. Receivers monitoring the band see "Olivia 8/250 detected at 1500 Hz" and the decoder auto-configures. RsID is what makes Olivia practical when configurations vary.

**A typical Olivia QSO:**

```
TX: CQ CQ CQ DE K1ABC K1ABC K1ABC PSE K
RX: K1ABC DE W2DEF W2DEF K
TX: W2DEF DE K1ABC GM ES TNX FOR CALL = UR RST 569 569 = NAME JOHN JOHN = QTH BOSTON BOSTON = HW? K
RX: ... (similar back)
```

The "=" character (BT prosign) separates fields; "K" invites the other station to transmit. The pacing is leisurely — at 8/250, each line takes 30–60 seconds. A complete QSO might run 10 minutes.

**Power level:** 5–25 W. Olivia's weak-signal performance makes more power pointless for casual chat.

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| No decode despite visible signal | Wrong tones/bandwidth setting | Try common ones (8/250, 16/500, 32/1000); enable RsID auto-detect |
| Decode produces gibberish | Wrong sideband (LSB vs USB) | Switch to USB |
| Partial decode, then stops | Drift between rig and other station | Use a rig with stable oscillator; warm up 15 min before TX |
| Audio waterfall shows signal but Fldigi shows blank | Squelch / margin too high | Lower the Fldigi squelch threshold in Configure → IDs |
| Other stations copy you but you can't copy them | RX audio level wrong | Aim for Fldigi RX level at ~50% of meter |
| Decoder produces "OOOOOO" or similar repeating | Decoder locked on a carrier or birdie | Adjust frequency; check rig's noise blanker is off |
| TX sounds clean but never gets answered | Configuration mismatch | Send RsID; or call "CQ OLIVIA 8/250" so others know what to set |
| Slow stations on a busy band | Olivia is just slow | Switch to 32/1000 for higher-rate paths; or accept the pace |

## Common mistakes

- **Forgetting RsID.** Without it, other operators don't know your settings. Always enable RsID for TX.
- **Setting too narrow a config on a busy band.** 8/250 is one-tenth the width of a typical SSB signal — perfect for QRM avoidance, but slow. Pick the width that matches conditions and patience.
- **Treating it like FT8.** Olivia is conversation, not exchange. Enjoy the slow tempo.
- **Calling on the wrong frequency.** 14.107 MHz is *the* Olivia frequency. Hang out there and you'll find QSOs.

## See also

- §28-05 — MFSK16 / MFSK32 (faster cousins, less weak-signal-robust)
- §03-03 — PSK31 (the other "keyboard chat" mode)
- §03-04 — JS8Call (FT8-derived, also conversational, with FEC and store-and-forward)
- §11 — Power Budget (QRP / Olivia synergy)
- §22 — Operating Practice (rag-chew etiquette)
