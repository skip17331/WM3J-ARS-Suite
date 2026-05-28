---
id: 05-00
title: Morse — Overview
chapter: 05
section: 00
level: simple
status: draft
---

# Morse — Overview

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## Why Morse?

Morse code (CW — Continuous Wave) is no longer required for any amateur license. So why learn it?

- **It works when nothing else does.** A CW signal can be readable when an SSB signal on the same path is buried in noise. Operators routinely complete CW QSOs at signal-to-noise ratios that would make voice impossible.
- **The bandwidth is tiny.** A CW signal is about 150 Hz wide vs. 2,400 Hz for SSB. Sixteen CW conversations fit in the bandwidth of one SSB conversation.
- **The equipment can be simple.** A homemade transmitter with one tube, no microphone, no audio chain — that's a working CW station. Some people enjoy that constraint.
- **The community is welcoming.** CW operators are friendly, help newcomers, and run dedicated practice nets at slow speeds.
- **It's portable.** A 5-watt CW QRP rig and a wire antenna fit in a backpack and work the world.
- **The chase.** The CW DX windows on every band are where the rare DX hangs out. If you want DXCC awards quickly, CW is the fastest path.

You don't have to operate exclusively on CW. Most operators who learn it do voice and digital too. But once you can copy 15 WPM, every band on every radio gets richer.

## How long does it take to learn?

The standard answer: **about 100 hours of focused practice** to reach 15 words per minute (WPM) — the speed at which you can comfortably hold a casual QSO. Some people get there in 50 hours; some in 200. Plan on **3 months at 30 minutes a day** as a reasonable target.

The first three weeks are the hardest. After that, your brain switches from "decode each letter" to "recognize the rhythm," and progress accelerates dramatically.

## How this chapter is organized

| § | Topic | What you get out of it |
|---|-------|------------------------|
| 03-01 | Koch method | The training method that actually works (vs. the ones that don't) |
| 03-02 | Farnsworth spacing | The trick that lets you learn at high character speed without panic |
| 03-03 | Character groups | The order Koch teaches characters in, and why |
| 03-04 | Words and callsigns | Moving from random characters to actual content |
| 03-05 | QSO simulation | The standard CW QSO format you can practice against |
| 03-06 | Send practice | Learning to transmit, paddle vs straight key, sending well |
| 03-07 | Speed tracking | How to measure progress and when to bump the speed |
| 03-08 | Mini tests | Self-administered tests you can use to check yourself |
| 03-09 | Hardware keyer builds | Two reference DIY keyer designs (Arduino USB serial, Pi Zero W wireless) |

## What gear do I need?

To **learn**: nothing. There are excellent free apps (LCWO, Morse Mania, G4FON) that train you with audio in your headphones. No radio required for the first month or two.

To **operate** once you've learned:

- A radio that can do CW (any HF transceiver can; check that yours has a "CW" mode setting).
- A **key** — either a **straight key** (the classic up-down lever) or a **paddle** (a horizontal lever that produces dits and dahs automatically). Most modern operators learn on a paddle because the timing is enforced by the keyer in the radio.
- An **electronic keyer** — built into every modern radio; converts paddle clicks into properly-timed dits and dahs.

A reasonable starter setup: any HF rig + a $30 paddle + a wire antenna. Total under $400 used.

## The bundled Morse Code Trainer

The ARS Suite ships with a standalone **Morse Code Trainer** (JavaFX desktop app). Click **▶ Launch Trainer** at the top of any §05 page to start it, or run `morse-trainer/run.sh` directly.

What's in it:

- **Letter Trainer** — Koch-method progression with per-character feedback.
- **Letter Group Trainer** — random 2–5 character groups with per-character accuracy stats.
- **QSO Simulator** — realistic CW exchanges across three difficulty levels (Training / Casual / Contest).
- **Sending Trainer** — decodes your keying in real time, scores it, and reports per-character timing diagnostics.
- **Hardware-keyer support** — works with the keyboard out of the box; optional Arduino USB keyer or Raspberry Pi Zero wireless keyer (full BOM and wiring diagram in `morse-trainer/hardware/README.md`).
- **Session export** — every session writes a JSON snapshot under `~/morse-trainer/logs/` for tracking progress over time.

Audio is generated locally (Java Sound), so it works completely offline.

### Other trainers worth knowing about

The bundled trainer covers the day-to-day practice loop. These external resources still complement it:

- **LCWO** — `lcwo.net` — Fabian DJ1YFK's web-based trainer. Tracks your progress, gradually introduces characters via Koch method, runs realistic QSOs.
- **G4FON Koch trainer** — Windows desktop app. Long the standard for serious CW students.
- **Morse Mania** — iOS/Android. Convenient for short practice sessions on a phone.
- **CW Academy** — `cwops.org` — free instructor-led classes from CWops, the international CW operators' club. Highly recommended.

## See also

- §05-01 — start here if you've never learned CW before
- §05-08 — try one of these mini-tests to see where you stand if you've already started
