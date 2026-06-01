---
id: 03-00
title: Digital Modes — Overview
chapter: 03
section: 00
level: simple
status: draft
---

# Digital Modes — Overview

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A "digital mode" in amateur radio is any mode that encodes information as discrete symbols transmitted over RF, decoded by software at the other end. RTTY (1940s teletype), PSK31 (1990s rag-chew), FT8 (2017 weak-signal QSO mode), APRS (position reporting), and Packet (AX.25 data) are all digital modes — but they cover wildly different use cases, bandwidths, and decoders.

This chapter covers the six modes most commonly encountered in modern ham radio. Each section is a card with the same shape: **what it is**, **how it works** (modulation, baud, bandwidth, timing, FEC), **why use it**, **operating** (frequencies, software, calling conventions), and **troubleshooting**.

## What "digital mode" means in practice

Every digital QSO involves three layers:

```
Computer ─── audio ──── Radio ──── RF ──── Radio ──── audio ─── Computer
   │      (or USB)        │                  │       (or USB)      │
   │                      │                  │                     │
[encode]                  └── modulate       └── demodulate    [decode]
[FEC]                         (SSB or FM)        (SSB or FM)    [error
[interleave]                  carrier            carrier        check]
```

The radio is mostly just a transmitter / receiver of audio tones. The interesting work happens in software at both ends. This is why digital modes evolved so fast — better software gets you better performance with the same radio.

## The audio chain

Connecting your computer's audio to the rig is the single biggest source of "doesn't work" frustration. Three common interfaces:

| Interface | Examples | How it works |
|-----------|----------|--------------|
| **Built-in USB** | IC-7300, FTDX10, K3 (with KIO3) | Rig appears as a USB sound card + serial port. Cleanest setup. |
| **External sound interface** | SignaLink USB, RigBlaster, MFJ-1204 | USB sound card plus PTT keying via VOX or RTS line. Works with any rig. |
| **Computer's mic input + VOX** | Generic USB headset adapter | Audio goes via the rig's mic; VOX keys the rig. Cheap, finicky, more interference. |

Most operators settle on one of the first two within a year. The third is fine for occasional use but you'll fight audio levels and ground loops.

## Software ecosystem

| Mode | Primary software | Alternatives |
|------|------------------|--------------|
| FT8 / FT4 / WSPR | **WSJT-X** | JTDX, MSHV, Q65 (other WSJT family) |
| RTTY | **MMTTY** (Win) / **fldigi** | N1MM+ for contesting, gMFSK |
| PSK31 / Olivia / MFSK | **fldigi** | DM780 (HRD), MultiPSK |
| JS8Call | **JS8Call** | (its own ecosystem) |
| APRS | **APRSdroid** / **Xastir** / **YAAC** | Direwolf as TNC backend |
| Packet | **Direwolf** + terminal | UI-View, BPQ32, AGW Packet Engine |

Most of these are free and open-source. WSJT-X (Joe Taylor K1JT) and fldigi (Dave Freese W1HKJ) are the workhorses. Pick the software for the mode and learn it; switching modes usually means switching software.

## What this chapter is not

This chapter is the **operating reference** for digital modes. It is *not*:

- A buyers' guide for sound interfaces — see vendor reviews on QRZ / EHam.
- An exhaustive list of every digital mode — there are 40+ obscure ones (Olivia, MT63, Hellschreiber, etc.) that fldigi can decode but rarely appear on air.
- A WSJT-X / fldigi tutorial — those have excellent documentation; this chapter explains *what they're doing under the hood* so you know what to expect on the air.

## Bandwidth and band-plan etiquette

Digital modes occupy specific narrow segments of each band by convention. Operating outside the digital sub-band — even by a few hundred Hz — interferes with voice operators and is considered poor form. The frequency tables in each per-mode card list the standard "watering hole" frequency (where the activity congregates) and the broader sub-band (where occasional activity is acceptable).

| Band | HF Digital Sub-band (US) | Notes |
|------|--------------------------|-------|
| 80m | 3.570 – 3.600 MHz | RTTY/data |
| 40m | 7.040 – 7.125 MHz | RTTY/data |
| 30m | 10.130 – 10.150 MHz | All digital |
| 20m | 14.070 – 14.100 MHz | RTTY/data |
| 17m | 18.100 – 18.110 MHz | All digital |
| 15m | 21.070 – 21.110 MHz | RTTY/data |
| 12m | 24.920 – 24.930 MHz | All digital |
| 10m | 28.070 – 28.150 MHz | RTTY/data |
| 6m | 50.200 – 50.300 MHz | Various |

(Voluntary IARU plan; see §20-02 for the full HF voluntary band plan and §04 for VHF/UHF.)

## When clocks matter

A defining feature of *some* digital modes is that the receiver must know **when** a transmission starts to within a fraction of a symbol time. This drives the "set your computer clock with NTP" advice you'll hear constantly.

| Mode | Clock tolerance | Why |
|------|-----------------|-----|
| FT8 | ±1 second | 15-second slots, decoder needs to align |
| FT4 | ±0.5 second | 7.5-second slots |
| WSPR | ±2 seconds | 2-minute slots, more forgiving |
| JS8Call (Slow) | ±5 seconds | Long symbols, very forgiving |
| RTTY | none | Continuous; sync per character |
| PSK31 | none | Continuous; sync per phase change |
| APRS | none | Connectionless packets |
| Packet | none | TNC handles sync |

If you operate FT8 or FT4 and your clock drifts more than a second, your QSOs will fail. Set the OS to sync via NTP and verify before a session.

## Section index

| § | Mode | Bandwidth | When to use |
|---|------|----------:|-------------|
| 03-01 | FT8 / FT4 | 50 / 90 Hz | Weak-signal DXing, low-power, dense activity |
| 03-02 | RTTY | 250–500 Hz | Contests with RTTY events, traditional digital |
| 03-03 | PSK31 | 31 Hz | Keyboard-to-keyboard rag chew, very narrow |
| 03-04 | JS8Call | 50 Hz | Conversational + store-and-forward, emcomm |
| 03-05 | APRS | 2 kHz | Position reporting, weather telemetry, messaging |
| 03-06 | Packet | 6 / 16 kHz | BBS, DX cluster, KISS-to-IP, mostly historical |
| 03-07 | WSPR | 6 Hz | Propagation beacon / antenna testing (not a QSO mode) |

## See also

- §04 — Repeaters & Bandplans (where the digital sub-bands fit)
- §05 — Morse code (the original "digital mode")
- §18 — Coax & Connectors (audio cables matter for digital)
- §20 — Band Plans (full sub-band layout)
- §21 — Emergency & Public Service Comms (where JS8Call and APRS live)
