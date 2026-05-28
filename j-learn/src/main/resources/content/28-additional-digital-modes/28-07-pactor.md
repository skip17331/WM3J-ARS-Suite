---
id: 28-07
title: Pactor
chapter: 28
section: 07
level: advanced
status: draft
---

# Pactor — Commercial-Grade HF Data

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What it is

Pactor (PACket Teleprinting Over Radio) is a family of HF data protocols developed by **SCS** (Spezielle Communications Systeme, Germany) starting in 1990. Pactor was *the* high-reliability HF data mode for two decades — used by sailors, missionaries, military, and commercial radio operators worldwide for moving email, weather data, and ICS forms over HF when no other path existed.

There are four versions: Pactor I (1991), II (1994), III (2002), IV (2011). Each generation increased throughput while maintaining backward compatibility. Pactor I is an open standard; Pactor II, III, and IV are proprietary and run only on SCS modem hardware.

Pactor's defining feature is **reliability** — its ARQ + FEC + adaptive modulation work together to deliver near-zero error rates over fading HF paths. Where soundcard modes like ARDOP or MT63 might drop a connection during deep fade, Pactor rides through.

The trade-off: SCS modems cost **$1,500–$3,000 USD**. For most hams that's prohibitive. With the rise of VARA HF (§28-02), which achieves comparable throughput in software, Pactor's amateur use has declined sharply since 2018. Pactor remains in heavy use in:

- **Maritime cruisers** crossing oceans where a dedicated modem in a harsh environment is preferred over a fiddly laptop+sound-card setup
- **Commercial / military** radio links (where amateur licensing doesn't apply)
- **Long-haul emergency comms** networks that pre-date VARA and still maintain Pactor infrastructure

## How it works

| Property | Pactor I | Pactor II | Pactor III | Pactor IV |
|----------|----------|-----------|------------|-----------|
| Modulation | 2-FSK + memory ARQ | DPSK, 4-tone | 8-PSK or 16-PSK, 18 tones | 64-QAM OFDM, 24 carriers |
| Bandwidth | 500 Hz | 500 Hz | 2400 Hz | 2400 Hz |
| Symbol rate | 100 baud | 100 baud | 100 baud (variable) | 1800 baud |
| Raw throughput | 100–200 bps | 100–800 bps | 100–3600 bps | 90–10500 bps |
| Typical throughput on HF | 100 bps | 300–600 bps | 800–2000 bps | 2000–5000 bps |
| FEC | Memory ARQ | Convolutional | Variable | Turbo / LDPC |
| ARQ | Stop-and-wait | Selective | Selective + Memory | Selective + Memory |
| Compression | None | Huffman | PMC (Pseudo-Markov) | PMC + LZ77 |
| Open standard | Yes | No | No | No |

Pactor's architecture is **half-duplex ARQ**: one station transmits a block, the other ACKs (or NAKs) it, and the sender either continues or repeats. Block lengths and modulation orders **adapt continuously** based on the link's measured SNR. On a strong link, Pactor IV blasts 64-QAM blocks at 1800 baud; as fading degrades the path, it steps down to 16-PSK, 8-PSK, 4-PSK, BPSK, and finally to Pactor II's robust DPSK before giving up.

**Memory ARQ** is Pactor's secret weapon: when a block fails to decode, the receiver doesn't throw it away. It stores the soft-decision symbol estimates and combines them with the retransmitted copy. After 2–3 retries, the soft-combined version often decodes even though no individual copy could.

> **Advanced —** Pactor IV's PMC (Pseudo-Markov Compression) achieves ~1.7× compression on English text and ~3× on typical email headers (which are highly structured). Combined with LZ77 dictionary compression for repeated phrases, an email with PMC sometimes transfers in *half* the air time of the same email uncompressed. On Pactor III, PMC alone was already a major throughput win — it's why a Pactor III link "feels" faster than its raw symbol rate suggests.

## Why use it

- **Reliability under bad HF conditions.** Pactor's combination of ARQ + memory + adaptive modulation makes it the most resilient HF data mode ever deployed.
- **Hardware modem.** Dedicated DSP hardware (the SCS PTC-IIIusb or DR-7800) doesn't compete with the OS for cycles — no audio dropouts from antivirus scans, no sound-card driver bugs.
- **Self-contained.** No PC needed for the modem itself; the modem can hold a small terminal and run autonomously. Useful for sailors and remote-station operators.
- **Mature.** Three decades of field deployment. Bugs are long-since worked out. Every Winlink CMS still supports Pactor.
- **Internationally licensed.** The Pactor protocol stack is recognized for commercial maritime use under ITU regulations.

When to skip it: **cost.** For most amateurs in 2025, VARA HF on a $50 USB sound-card interface delivers most of Pactor's performance at <5% of the cost. Unless you need Pactor's specific reliability under specific worst-case HF conditions, VARA HF wins on dollar-per-bit.

## Operating

**Hardware:** Only **SCS modems** support Pactor II/III/IV. Current model is the **DR-7800** (~$2,200 USD); older models (PTC-IIex, PTC-IIIusb) still appear on the used market. Pactor I can be transmitted with any TNC, but no one uses Pactor I alone — they always have a Pactor II+ capable modem.

**Software:** **Airmail** (legacy), **Winlink Express** (current), **RMS Express**, **Pat**. All connect to the SCS modem over a USB serial port and send AT-style commands.

**Connection:**

1. Cable the SCS modem to the rig (audio in/out, PTT) and to the PC (USB).
2. In Winlink Express, create a Pactor session, pointing at the modem's COM port.
3. Pick an RMS that supports Pactor.
4. Click "Start." The modem dials the gateway. Pactor IV handshake: ~3 seconds.
5. Outbox / inbox transfer. Session closes.

**Calling channels:** the gateway publishes a frequency. Pactor is **not** a band-share-friendly mode — it's a one-on-one ARQ link on a single channel, and other users on that channel will see it as steady noise.

**Calling frequencies (USB dial, common Winlink Pactor):**

| Band | Pactor RMS examples (varies regionally) |
|------|----------------------------------------|
| 80m | 3.591 MHz |
| 40m | 7.103 MHz |
| 30m | 10.145 MHz |
| 20m | 14.108 MHz |
| 17m | 18.117 MHz |
| 15m | 21.099 MHz |

These frequencies are shared with VARA HF gateways — the RMS may run either or both. Check winlink.org for current channel lists.

**Power level:** 50–100 W. Like VARA, more power doesn't help — the link will adapt down anyway, and over-driving causes IMD.

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| Modem not detected | Wrong COM port or driver | Verify USB driver in Device Manager; reseat USB cable |
| Connect, then drop | Audio level wrong | Run SCS modem's audio calibration (`@TX 1500` and adjust drive) |
| Throughput stuck at Pactor I | Other end is Pactor I-only, or SNR very poor | Try a different gateway; Pactor I → IV negotiation needs minimum SNR |
| "Lost link" mid-transfer | Deep selective fade beyond Pactor's adaptation range | Wait and reconnect; or switch bands |
| Modem firmware out of date | Newer Winlink features need newer firmware | Update via scs-ptc.com (PC tool) |
| Slow Pactor III speeds on Pactor IV capable modem | Other end is Pactor III only | Many RMS still support only up to Pactor III; check gateway capabilities |
| Modem keys TX but no signal heard | PTT or audio routing | Verify modem audio out → rig mic in; PTT line wired |
| Pactor IV mode "not licensed" | License keycode not entered | Each Pactor IV modem needs a license key from SCS |
| Connection appears strong, transfer slow | Wrong compression setting | Verify PMC is enabled in Winlink session config |

## Common mistakes

- **Buying Pactor in 2025 for amateur use.** VARA HF gets you 90% of the performance at <5% the cost. The remaining 10% (extreme fading reliability) matters for blue-water sailors and military ops, not most hams.
- **Running over-driven audio.** Pactor's adaptive modulation can't compensate for IMD splatter — the receiver sees a "garbage" channel and falls back to Pactor I.
- **Ignoring firmware updates.** SCS releases firmware updates periodically; older firmware may not negotiate with newer Pactor IV gateways.
- **Sharing the channel.** Pactor's ARQ doesn't share well — if two stations try to use the same RMS Pactor channel simultaneously, both fail.

## See also

- §28-01 — Winlink (Pactor's primary use case)
- §28-02 — VARA HF (the modern soundcard replacement)
- §28-10 — Digital messaging workflows (when Pactor is still the right tool)
- §21 — Emergency Comms (Pactor's traditional emcomm niche)
- §27 — Station Engineering (audio levels, ground, ALC)
