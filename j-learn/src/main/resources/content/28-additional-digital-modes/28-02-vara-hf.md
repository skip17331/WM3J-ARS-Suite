---
id: 28-02
title: VARA HF
chapter: 28
section: 02
level: mixed
status: draft
---

# VARA HF — High-Throughput Soundcard Modem

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What it is

VARA HF is a high-performance **soundcard modem** for HF data transport. Written by Jose Alberto Nieto Ros (EA5HVK), it replaced Pactor as the standard high-throughput Winlink mode on HF — without requiring the $1,500+ Pactor hardware. VARA HF runs on any PC with a sound card and an SSB rig; the software is **proprietary but free for amateur use** in its base form.

VARA HF achieves 2,000+ bps under good conditions in a 2.3 kHz audio channel. It adapts the modulation in real time based on link quality — strong link → 16-QAM at high symbol rate; weak link → BPSK at low symbol rate. The same connection can shift from 100 bps to 7,000 bps without dropping.

Outside Winlink, VARA HF also works as a peer-to-peer chat / file-transfer tool (VARAC application). Most operators meet VARA HF through Winlink first.

## How it works

| Property | Value |
|----------|-------|
| Modulation family | OFDM, multiple QAM/PSK orders |
| Channel bandwidth options | 500 Hz, 2.3 kHz, 2.75 kHz (extra-wide) |
| Symbol rate | 25 baud (narrow) to 1000+ baud (wide) |
| Throughput, 500 Hz channel | ~250 bps best case |
| Throughput, 2.3 kHz channel | 200 bps (weak) to ~5,000 bps (strong) |
| Throughput, 2.75 kHz channel | up to 7,000 bps (rare conditions) |
| FEC | LDPC + interleaving (mode-dependent) |
| ARQ | Selective-repeat with sliding window |
| Adaptive | Yes — link layer chooses best mode each frame |
| License model | Proprietary; free up to ~3,000 bps; paid unlock above |

VARA HF uses **OFDM** (orthogonal frequency-division multiplexing): the audio passband is split into many narrow sub-carriers (e.g. ~52 carriers at 47 Hz spacing in 2.3 kHz mode), each carrying a low-rate QAM signal. OFDM is robust against frequency-selective fading — if one sub-carrier fades, the others survive, and FEC recovers the missing bits.

The modem continuously measures channel quality (via per-carrier SNR estimates) and negotiates an appropriate **modulation level** (BPSK, QPSK, 8-PSK, 16-QAM) and **frame size** (number of OFDM symbols per data block). On a weak link it sends slow, robust BPSK blocks; on a strong link it sends fast 16-QAM blocks. This switching happens every few seconds.

> **Advanced —** VARA's ARQ uses a sliding window with selective repeat. The receiver acknowledges each correctly-decoded block by sequence number; failed blocks are retransmitted while later blocks continue forward. This is more efficient than stop-and-wait ARQ (used by older modes like Pactor I) and is why VARA HF gets much higher *usable* throughput than its raw symbol-rate would suggest. Under typical CONUS HF conditions, you'll see 1,000–2,500 bps sustained — about 3–5× a Pactor III link on the same path.

## Why use it

- **Cheap.** No special hardware. Any sound card + SSB rig works.
- **Fast for HF.** 5–10× the throughput of older soundcard modes like ARDOP or MT63.
- **Adaptive.** No manual mode-switching as conditions change.
- **Free for personal Winlink use.** The base license (up to ~3,000 bps) is free; faster modes require a one-time donation/license fee.
- **Quietest soundcard modem in its class.** OFDM uses available bandwidth efficiently — no idling carriers.

When to skip it: if you need **real-time interactive chat** (Olivia / PSK31 / JS8Call are designed for that), or if you're on a **VHF FM** link (use VARA FM instead, §28-03), or if you have **Pactor IV hardware already** and need maximum reliability for critical traffic.

## Operating

**Software:** **VARA HF modem** (vara-modem.com) — runs as a separate process, exposing a TCP socket. Winlink Express, Pat, and other clients connect to that socket. The modem owns the sound card; the client owns the message layer.

**Setup chain:**

1. Install VARA HF modem.
2. Install Winlink Express (or Pat).
3. In Winlink, add a VARA HF Winlink session; point it at `127.0.0.1:8300` (default VARA TCP port).
4. Configure VARA's audio device (rig's USB sound interface).
5. Configure VARA's PTT (typically VOX or rig CAT).
6. Run the **VARA Audio Tune** test — adjust sound levels so the green LED stays around 75% peak with no red.
7. Open a Winlink VARA HF session; pick an RMS; connect.

**Audio level setup (the #1 cause of problems):**

- **TX audio**: drive level set so the rig's **ALC just barely lights** — no more. Over-driving is the most common VARA failure mode; it causes IMD and the link drops.
- **RX audio**: VARA's monitor should peak around **−6 dBFS** (about 75% on most level meters). If clipped (red), reduce sound-card input gain.
- **No audio processing**: turn off the rig's noise reduction, noise blanker, and audio equalizer in TX. These mangle OFDM.

**Calling frequencies:** VARA HF has no fixed channels — it's a peer-to-peer link. For Winlink, the RMS gateway list shows the gateway's frequency. Common Winlink VARA HF dial frequencies (USB, in MHz): 3.589, 7.064, 10.140, 14.106, 18.108, 21.108, 28.120 (varies regionally).

**Power level:** 25–50 W is plenty for most paths. Running over 100 W invites IMD problems.

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| VARA "connects" then drops in seconds | TX audio too hot (ALC pumping) | Lower drive until ALC is dark / barely lit |
| Link stays at 100 bps even on strong signal | Audio clipping in RX path | Lower sound-card input gain; aim for −6 dBFS peaks |
| "No modem detected" in Winlink | VARA modem app not running | Start VARA HF before opening a Winlink session |
| Slow throughput on a known-good path | Other interfering signals in passband | Move ±200 Hz; widen rig filter to 2.7 kHz; ensure rig DSP doesn't notch in band |
| Connection sound is right but no decode | Sample-rate mismatch between rig and sound card | Set both to 48 kHz; restart VARA |
| PTT keys but no audio | TX audio device wrong | Verify VARA's TX device matches rig's USB codec |
| Dropouts every few seconds | RX path RFI or USB cable | Add ferrite to USB cable; check ground loop |
| VARA shows "P4" mode unavailable | Free license tier | Donate to unlock high-speed modes; for casual Winlink, free tier is enough |
| Throughput collapses at sundown | Real propagation fade | Try another band; VARA adapts but can't beat physics |
| Connect to RMS, but RMS doesn't respond | RMS is busy with another user | VARA RMS is single-user; wait or pick another gateway |

## Common mistakes

- **Driving too hot.** "Louder is better" is wrong for OFDM. ALC must be off / barely lit.
- **Leaving rig's NR or NB on.** These add nonlinearity. Turn them off for digital modes.
- **Using rig's built-in narrow filters.** VARA wants the full 2.4–2.7 kHz SSB passband. A 500 Hz filter destroys all but the slowest VARA modes.
- **Running on a noisy bus-powered USB hub.** Buy an externally-powered hub or plug the rig direct to the PC.

## See also

- §28-01 — Winlink (VARA HF's primary use case)
- §28-03 — VARA FM (same idea for VHF)
- §28-07 — Pactor (what VARA HF mostly replaced)
- §27 — Station Engineering (audio levels, ALC, grounding)
- §14 — RFI (common-mode chokes for clean digital ops)
