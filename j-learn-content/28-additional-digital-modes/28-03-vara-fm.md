---
id: 28-03
title: VARA FM
chapter: 28
section: 03
level: simple
status: published
---

# VARA FM — High-Throughput Soundcard Modem for VHF/UHF FM

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What it is

VARA FM is the VHF/UHF FM-channel cousin of VARA HF (§28-02). Same author (Jose Alberto Nieto Ros, EA5HVK), same architecture, but tuned for the much wider passband of a typical FM voice channel. It typically delivers **10–25 kbps** over a single FM repeater channel or simplex link.

For local Winlink, VARA FM has largely replaced 1200-baud AFSK Packet (§03-06). A short email that took 90 seconds over 1200-baud Packet now takes 5 seconds over VARA FM, on the same radios.

The trade-off vs. Packet: VARA FM is **point-to-point** between two stations; it doesn't support digipeating, AX.25 callsign-based routing, or the cooperative multi-hop network behavior that classic Packet was built around. For email-into-the-RMS, that doesn't matter — you only ever talk to one gateway at a time.

## How it works

| Property | Value |
|----------|-------|
| Modulation family | OFDM over FM audio |
| Channel bandwidth | Standard FM voice (~12 kHz) |
| Throughput, narrow mode | ~7 kbps |
| Throughput, wide mode | up to 25 kbps |
| FEC | LDPC + interleaving |
| ARQ | Selective-repeat sliding window |
| Adaptive | Yes — modulation order and rate change with SNR |
| License model | Free up to ~7 kbps; paid unlock for wide mode |

The basic idea: FM audio between two radios provides a ~3 kHz analog channel of fairly high SNR (since FM is "captured" — either you have full quieting and a clean signal, or you have nothing). VARA FM packs OFDM into that channel.

Because FM either works or it doesn't, VARA FM doesn't need the deep weak-signal robustness of VARA HF. It can run faster modulation orders almost all the time, as long as the FM link is full-quieting. The moment the link starts hissing (mobile fade, hidden-transmitter), throughput collapses and VARA's ARQ retries fly.

> **Advanced —** Latency vs. throughput is a real consideration. VARA FM uses larger frames than 1200-baud Packet because each frame's protocol overhead is amortized across more data. Larger frames also mean **higher round-trip latency** — typically 1–3 seconds, vs. ~250 ms for 1200-baud Packet. For email this is invisible; for an interactive console session it'd feel sluggish. VARA FM is engineered for batch transfer, not keystroke latency.

## Why use it

- **Fast email over local repeaters.** Most ARES groups now run VARA FM Winlink gateways. A multi-attachment ICS-213 form moves in under 30 seconds.
- **Works through any FM repeater** with a clean audio path. No special repeater hardware needed.
- **No special radio.** Any VHF/UHF FM rig + sound-card interface (Signalink, RIM-Lite, or rig's built-in USB).
- **Free for typical use.** The unpaid tier covers most ARES needs.
- **Coexists with voice.** When the channel's idle, VARA FM uses it; when someone keys up for voice, VARA queues and resumes.

When to skip it: long-range HF paths (use VARA HF, §28-02), or any case where multi-hop AX.25 routing matters (Packet, §03-06).

## Operating

**Software:** **VARA FM modem** (vara-modem.com) + Winlink Express or Pat as the client. Setup parallels VARA HF — modem as a TCP socket, client connects to it.

**Hardware:**

- VHF/UHF FM rig (HT, mobile, or base).
- Sound-card interface, or rig with built-in USB audio.
- PTT via VOX, CAT, or hardware key.

**Calling channels:** there's no nationwide standard. ARES groups publish their local Winlink VARA FM gateway frequencies on the gateway list at winlink.org. Common patterns:

- **2m**: 145.030, 145.050, 145.070, 145.090 (data-segment simplex)
- **70cm**: 441.000–441.500 range
- **Local repeaters** — many ARES groups have a dedicated repeater for digital traffic

**A typical VARA FM session:**

1. Tune to gateway frequency.
2. Open Winlink Express; pick "VARA FM Winlink" session.
3. Click "Start." VARA FM transmits a short ID and listens for the gateway.
4. Gateway responds; link negotiation completes in ~3 seconds.
5. Outbox upload + inbox download begins. Watch the throughput indicator climb.
6. Session closes; review messages.

**Power level:** 5–25 W typical. With FM, more power doesn't help unless you're below full-quieting; once the gateway hears you cleanly, additional power just splatters into adjacent channels.

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| Connection but slow throughput | Audio levels wrong | Run VARA FM's tune routine; aim for −6 dBFS peaks |
| "No modem" | VARA FM app not running | Start VARA FM before opening Winlink session |
| Connect, then drop after 5 seconds | TX audio over-deviating the FM rig | Lower mic gain on the rig; check deviation with a service monitor if possible |
| Gateway busy | Single-user channel in use | Wait; or pick a different gateway |
| Repeater drops carrier mid-session | Repeater squelch tail too short | Use a simplex gateway, or a different repeater |
| Weak gateway signal (hiss) | Out of range, or wrong antenna | Move to higher ground, or pick a closer gateway |
| 70cm gateway works, 2m doesn't | Antenna mismatched at one band | Verify SWR on both bands |
| Throughput stuck at 1.2 kbps | Mode forced to narrow | Check VARA FM mode menu; should be "Wide" if both sides support it |

## Common mistakes

- **Over-deviating.** Most ham FM rigs ship with mic gain set for voice; VARA FM's tones can over-drive the modulator. Lower the rig's mic / drive gain until VARA reports a clean signal.
- **Treating it like Packet.** VARA FM has no digipeating and no callsign-based AX.25 — it's a point-to-point modem. If your local emcomm plan relies on a chain of digipeaters, VARA FM won't replace them.
- **Forgetting CTCSS / DCS.** If the repeater requires PL, set it on the rig — VARA FM doesn't manage CTCSS for you.

## See also

- §28-01 — Winlink (the consumer of VARA FM)
- §28-02 — VARA HF (same idea on HF)
- §03-06 — Packet (the AX.25 mode VARA FM mostly replaced for email)
- §04 — Repeaters & Band Plans (VHF/UHF data sub-bands)
- §21 — Emergency Comms
