---
id: 11-00
title: Station Troubleshooting — Overview
chapter: 11
section: 00
level: simple
status: draft
---

# Station Troubleshooting — Overview

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The radio works. The antenna works. The coax works. So why isn't anything happening on the air? This chapter is about the **station-level** problems — the ones that aren't pure SWR (which has its own chapter, 13), aren't RFI (chapter 15), but are the station-as-a-system not behaving.

## How this chapter is organized

The sections are sorted by "what is failing":

| § | Failure | Common cause |
|---|---------|--------------|
| 14-01 | No transmit | PTT, power supply, sequencer, fuse, ALC misconfigured |
| 14-02 | No receive | Antenna selector, attenuator, preamp dead, audio chain, squelch |
| 14-03 | Distorted audio | Mic gain, compressor, ALC, mic placement, processed monitor |
| 14-04 | RF feedback | Common-mode current, ground loop, audio cable shielding |
| 14-05 | Grounding | Single-point ground, station ground bus, pipe ground |
| 14-06 | Power supply | Voltage drop under load, ripple, ground noise |

Walk the chapter that matches your symptom. If you don't know what's failing, work this overview's diagnostic tree first.

## The diagnostic tree

```
Is the radio powered on and receiving normal background noise?
├── No → power supply / fuse / connection issue. See §11-06.
└── Yes:
    Can you hear local stations / weather / shortwave broadcasts?
    ├── No → antenna or RX chain. See §11-02.
    └── Yes:
        Can you transmit (does PTT close, is there RF output)?
        ├── No → PTT / fuse / interlock. See §11-01.
        └── Yes:
            Does the audio sound good (have someone listen)?
            ├── No → audio chain. See §11-03.
            └── Yes:
                Are you getting reports of being heard?
                ├── No → antenna or feedline (chapter 13)
                └── Yes:
                    Is there RF in the shack (tingles, RFI)?
                    ├── Yes → §11-04 (RF feedback) or §11-05 (grounding)
                    └── No → check propagation, you're operating
                              in a poor band condition
```

## A general philosophy

Station problems are best diagnosed by **isolation** — disconnect things until the problem goes away, then add them back one at a time. The first thing that brings the problem back is the cause.

Common isolation steps:

1. **Substitute a known-good antenna** (a dummy load is fine for testing). If the problem goes away, the original antenna is the issue.
2. **Substitute a known-good cable.**
3. **Move the radio to a different power source.**
4. **Disconnect everything from the radio except antenna and microphone.**
5. **Use the radio in low-power mode** to rule out PA-stage issues.
6. **Try the radio at a friend's station** (or borrow theirs).

Each substitution either rules out one suspect or confirms it. This is much more reliable than guessing.

## Tools you'll want

For station-level troubleshooting:

- **A dummy load** — 50 Ω, rated for at least your transmitter power. A small one for low-power testing ($25), a bigger one for full-power tuning ($75–$150).
- **A multimeter** — ideally with a 10 A current setting too.
- **An oscilloscope** — useful for audio chain debugging; entry-level USB scopes ($50) are adequate.
- **A second receiver** — even a cheap SDR dongle ($25) lets you listen to your own transmissions and confirm what you sound like.
- **A handheld radio** — for QSY tests and as a "monitor" of the local environment.
- **An RF current probe** — for common-mode testing (covered in §11-04).

## When to call for help

Some problems are best handed to someone with more experience or a more equipped shop:

- **PA stage failure** in a transceiver — risk of further damage if you try to fix it without test equipment.
- **Display or controller failure** — typically requires manufacturer service.
- **Recurring intermittent problems** that resist isolation — sometimes a fresh pair of eyes is faster than another evening of guessing.
- **Modifications gone wrong** — if you opened the radio and now it doesn't work, stop and consult.

The local club, an Elmer, or a manufacturer's authorized repair shop are all reasonable next steps.

## See also

- §10 — high SWR (sometimes mistaken for station problems)
- §12 — RFI (often comorbid with station grounding issues)
- §13, §14 — noise sources that look like station problems
