---
id: 22-00
title: Voice Modes — Overview
chapter: 22
section: 00
level: simple
status: draft
---

# Voice Modes — Overview

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A **voice mode** in amateur radio is any mode that transmits human speech as an analog audio signal modulated onto an RF carrier. Three voice modes account for nearly all amateur voice activity: **AM** (amplitude modulation), **FM** (frequency modulation), and **SSB** (single sideband, a variant of AM).

This chapter covers each, in the same shape as the Digital Modes chapter (19): **what it is**, **how it works**, **why use it**, **operating** (frequencies, conventions, calling), and **troubleshooting**.

## At a glance

| Mode | Bandwidth | Power efficiency | Audio quality | Where used |
|------|-----------|------------------|---------------|------------|
| **AM** | ~6 kHz | Low (carrier + both sidebands) | Best (full audio fidelity) | 80 m / 40 m / 10 m AM nostalgia, aviation, broadcast |
| **FM** | 12–20 kHz (narrow) up to 25 kHz (wide) | High (constant carrier power) | Excellent above threshold; cliff-edges below | 10 m / 6 m / 2 m / 70 cm repeaters and simplex |
| **SSB** | ~2.4 kHz | Best (no carrier, one sideband) | Good (3 kHz audio) | All HF voice; 6 m / 2 m / 70 cm weak-signal |

If you're a new licensee picking your first voice mode:

- **FM on 2 m** if you want repeater contacts, local nets, easy gear. Most VHF/UHF voice is FM.
- **SSB on 20 m** if you want HF DX. Almost all HF voice is SSB.
- **AM** is a niche — fun, retro, but limited to a few AM-friendly windows and a small community.

## What "voice mode" means in practice

Every voice QSO involves these steps:

```
Microphone → audio → modulator → RF amplifier → antenna → RF
                       │
                  [AM, FM, or SSB]
                       │
RF → antenna → receiver front-end → demodulator → audio → speaker
                                       │
                                  [AM detector, FM discriminator, SSB BFO]
```

The modulation method determines the bandwidth, efficiency, and "feel" of the mode. SSB sounds tinny but punches through noise; FM sounds full but vanishes below threshold; AM sounds rich but eats spectrum.

## Mode vs. technique

Voice modes are *modulation types*. Operating *techniques* like **simplex**, **split**, **repeater duplex**, and **net operation** can be applied to any voice mode (and most digital modes too).

- For split-frequency operation see [§21-08](../21-operating-practice/21-08-split-frequency.md)
- For repeater operation see [§02 Repeaters & Band Plans](../02-repeaters-bandplans/02-00-overview.md)
- For calling discipline see [§21-04 Calling CQ](../21-operating-practice/21-04-calling-cq.md)

## Why three modes and not one?

The history is layered:

- **AM came first** (1920s amateur work). Simple, intuitive — your voice riding on a carrier. Eats spectrum because both sidebands are transmitted plus the carrier itself.
- **SSB emerged in the 1950s–60s** as a bandwidth-efficient AM. By suppressing the carrier and one sideband, SSB cuts bandwidth by 60% and pours all power into the remaining sideband. Dominated HF voice by the 1970s.
- **FM came to amateur use in the 1960s** via military and commercial surplus gear, then took over VHF/UHF in the 1970s with the repeater explosion. Constant power output is friendlier to repeater amplifiers, and audio quality above threshold is excellent.

Today: SSB owns HF, FM owns repeaters and VHF/UHF simplex, AM is a small but active enthusiast community.

> ⚙️ **Advanced —** Digital voice modes (D-STAR, DMR, Yaesu Fusion / C4FM, P25) are technically *digital* modes but operationally feel like FM voice — push-to-talk, codec encodes voice to digital bits, transmitted as 4-FSK or similar, decoded back to audio at the other end. They're covered separately in the digital-voice extension to chapter 19 (if/when added). The classic three voice modes — AM, FM, SSB — are all *analog*.

## What's covered

| § | Topic |
|---|-------|
| §22-01 | AM — Amplitude Modulation |
| §22-02 | FM — Frequency Modulation |
| §22-03 | SSB — Single Sideband |

## See also

- [§19 Digital Modes](../19-digital-modes/19-00-overview.md) — non-voice modes
- [§21 Operating Practice](../21-operating-practice/21-00-overview.md) — calling, pile-ups, split, etiquette
- [§02 Repeaters & Band Plans](../02-repeaters-bandplans/02-00-overview.md) — FM repeater operating
- [§18 Band Plans](../18-band-plans/18-00-overview.md) — where voice modes live by band
