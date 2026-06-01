---
id: 28-09
title: Robust Packet
chapter: 28
section: 09
level: mixed
status: draft
---

# Robust Packet — AX.25 Hardened for HF

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What it is

Robust Packet is a 2003 SCS-designed variant of AX.25 Packet (§03-06) reengineered for **HF** instead of VHF FM. The original 1200-baud AX.25 was built for clean FM voice channels — on HF, its AFSK tones get torn apart by fading, multipath, and selective propagation. Robust Packet keeps the **AX.25 frame format** (so callsign-based digipeating still works) but replaces the modulation with **DSP-based OFDM + FEC**.

The result: AX.25 frames that survive HF paths with selective fading and moderate noise. Slower than VHF Packet, but vastly more reliable than 300-baud HF Packet (the historical attempt to put AX.25 on HF, which works poorly because it uses the same FSK approach as VHF at a lower symbol rate).

Robust Packet is used by:

- **APRS-over-HF** networks (10.147 MHz and 14.103 MHz are the main APRS-HF channels)
- **Emcomm groups** that want callsign-routed packet for tactical messaging across regional distances where VHF FM doesn't reach
- **Maritime mobile** stations integrating with APRS while at sea

## How it works

| Property | Value |
|----------|-------|
| Modulation | OFDM, 8 sub-carriers |
| Bandwidth | 500 Hz |
| Symbol rate | 200 baud (Robust Packet 200) or 600 baud (RP 600) |
| Throughput | ~120 bps (RP 200) or ~360 bps (RP 600) |
| FEC | Convolutional rate 1/2 |
| ARQ | AX.25 connected-mode (selective with retries) |
| Frame format | AX.25 — same callsigns, same digipeater chain |
| Modem hardware | SCS Tracker / DSP TNC, or sound-card with software TNC |

The original AX.25 frame structure is unchanged: callsigns, destination/source/digipeater chain, payload, FCS. What's different is **everything below** the frame layer:

- **OFDM** replaces single-tone AFSK. Eight sub-carriers each carry a low-rate PSK signal. Selective fading that destroys one or two sub-carriers leaves the others intact.
- **Convolutional FEC (rate 1/2)** adds redundancy so the receiver can correct errors during fading.
- **Interleaving** spreads each AX.25 byte across many symbol times, so a brief noise burst destroys parts of many bytes rather than wiping out a whole byte (which FEC can recover).

The combination of OFDM + FEC + interleaving makes Robust Packet **10–20 dB more sensitive** than 300-baud HF AFSK Packet on a typical HF path. A signal that's unintelligible to old HF Packet decodes cleanly in Robust Packet.

> **Advanced —** Robust Packet's interleaver depth is ~10 symbol times (about 50 ms at RP 200). This is enough to spread byte information across the typical 1–10 ms multipath delays of HF, but short enough that ARQ latency doesn't suffer. Longer interleavers (used in some military HF modems) would offer more fade resilience but at the cost of much higher round-trip latency, which would break AX.25's connected-mode timeouts.

## Why use it

- **AX.25 routing.** Unlike VARA HF or Pactor, Robust Packet preserves AX.25 callsign-based routing — packets can be digipeated by callsign-aware nodes. This matters for APRS and for tactical packet networks.
- **Compatible with APRS.** RP is the standard for HF APRS gateways. iGates on 10.147 and 14.103 MHz forward HF APRS into the worldwide APRS-IS network.
- **Modest hardware requirements.** Works with SCS Tracker (~$300) or with software TNCs running on a sound card.
- **Open standard at the protocol layer.** AX.25 is documented; Robust Packet's lower layers are proprietary but widely supported in software TNCs.
- **Lower latency than VARA HF or Pactor.** Each AX.25 frame is ~50–250 ms; quick exchanges feel responsive.

When to skip it: large-file transfers (use VARA HF or Pactor), real-time conversation (PSK31 / JS8Call), or any case where APRS-style frame routing isn't a requirement.

## Operating

**Hardware:** **SCS Tracker** (~$300) is the dedicated DSP TNC. Or run Robust Packet in software via **direwolf** (open source, runs on PC or Raspberry Pi).

**Software clients:**

- **UI-View** (legacy, Windows) for APRS-HF.
- **YAAC** (cross-platform) for general AX.25.
- **APRSIS32** (Windows) for APRS work.
- **direwolf** itself includes a Telnet KISS interface for any AX.25 application.

**Calling frequencies:**

| Band | Use | Frequency (USB dial) |
|------|-----|---------------------|
| 30m | APRS-HF primary | 10.1473 MHz |
| 20m | APRS-HF secondary | 14.1027 MHz |
| 40m | Regional emcomm packet | 7.045 MHz (varies regionally) |
| 80m | Local emcomm packet | 3.585 MHz (varies regionally) |

**A typical Robust Packet APRS-HF exchange:**

```
K1ABC-9>APRS,WIDE2-1:!4216.32N/07103.45W>M Mobile from Maine
K3XYZ-10>APRS,WIDE2-1:!3852.10N/07717.65W>Solar-powered iGate
W2DEF-1>APRS,WIDE2-1:>QSY 144.39 NORMAL APRS HOURS
```

The frames look identical to VHF APRS — same format, same content. The only difference is the modulation underneath.

**Power level:** 25–50 W typical. Robust Packet's FEC and OFDM give enough margin that more power doesn't help much.

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| No decode despite audible signal | Wrong sub-mode (RP 200 vs RP 600) | Try both — most activity is RP 200 |
| Decoder lost frames intermittently | Sound-card sample rate drift | Lock to 48 kHz; use a hardware TNC for critical use |
| APRS-HF beacons disappear after digipeating | iGate is on wrong sub-mode | Verify iGate's Robust Packet config matches your TX |
| TX audio over-driving | ALC pumping | Reduce drive to barely-lit ALC |
| Connection drops after 60s | AX.25 keepalive timeout | Configure TNC for longer T1/T2 on HF (e.g. T1=20s, N2=10) |
| Direwolf shows "checksum error" | Frame corrupted beyond FEC | Likely a propagation event; wait and retry |
| Other AX.25 software doesn't see RP frames | KISS interface not connected | Verify Tracker's KISS port is exposed and TCP/IP works |
| Both ends RP but no connect | One side uses RP 600, other RP 200 | Match modes; RP is not auto-negotiating |

## Common mistakes

- **Using regular 300-baud HF Packet instead.** Old-style HF Packet is much worse than Robust Packet on the same path. There's no reason to use the slower, less-robust legacy mode in 2025.
- **Forgetting to set AX.25 timeouts for HF.** Default VHF AX.25 timeouts (T1 = 3 seconds) are too short for HF round-trip ARQ. Increase to 15–30 seconds.
- **Treating RP like real-time chat.** It's a packet mode — frames are short bursts with latency. Use VARA HF or PSK31 for conversation.
- **Running on a noisy sound-card with USB ground loops.** RFI directly into the sound-card destroys OFDM decode. Add a USB isolator or use a hardware TNC.

## See also

- §03-06 — Packet (the VHF AX.25 ancestor)
- §03-05 — APRS (the most common Robust Packet user)
- §28-07 — Pactor (also SCS, different design point)
- §21 — Emergency Comms (tactical packet networks)
- §27 — Station Engineering (sound-card RFI mitigation)
