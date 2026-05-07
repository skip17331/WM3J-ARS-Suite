---
id: 22-06
title: Packet Radio
chapter: 22
section: 06
level: mixed
status: draft
---

# Packet Radio

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What it is

Packet radio is a connection-oriented digital protocol — **AX.25** — running over RF. Stations *connect* to each other in a TCP-like handshake, exchange acknowledged frames, and disconnect. Beneath that, AX.25 is **the same protocol** that APRS (§22-05) uses; the difference is that APRS is connectionless broadcasts, while packet sets up explicit point-to-point sessions.

Packet had its heyday in the late 1980s and early 1990s when ham BBSes (bulletin boards), DX clusters, mailbox systems, and TheNet / NET/ROM mesh networks ran on every band. Most of that has migrated to the internet, but packet still has loyal users and active networks — particularly in remote regions, emcomm circles, and as the underlying technology for APRS.

## How it works

| Property | 1200 baud | 9600 baud |
|----------|-----------|-----------|
| Modulation | Bell 202 AFSK (1200 / 2200 Hz) | Direct FSK |
| Bandwidth | ~3 kHz | ~16 kHz |
| Data rate | 1200 bps | 9600 bps |
| Typical band | VHF FM (144.39, 145.05, 145.07, etc.) | VHF / UHF FM |
| Radio requirement | Any FM radio | Radio with "9k6" / "data" port (flat audio) |
| Maximum frame size | 256 bytes (informational) | 256 bytes |
| Sliding window | 1–7 frames in flight | 1–7 frames in flight |
| Sync | Per-frame preamble | Per-frame preamble |

**AX.25 frame structure:**

```
┌──────────┬──────────┬──────────┬──────────┬─────────┬──────────┬──────────┐
│   Flag   │   Addr   │ Control  │   PID    │  Info   │   FCS    │   Flag   │
│ 0x7E     │ src+dst  │ frame    │ protocol │ payload │  (CRC)   │ 0x7E     │
│  (1B)    │ (14-70B) │  type    │   id     │ (0-256) │  (2B)    │  (1B)    │
│          │          │  (1-2B)  │  (1B)    │         │          │          │
└──────────┴──────────┴──────────┴──────────┴─────────┴──────────┴──────────┘
```

The Address field carries up to 8 callsigns: 1 source, 1 destination, and up to 6 digipeater hops. The Control field marks the frame type (information, supervisory, unnumbered) and sequence number. The FCS is a 16-bit CRC that detects but doesn't correct errors.

**Connection-oriented vs. connectionless:**

| Mode | Used for | Example |
|------|----------|---------|
| **UI (Unnumbered Information)** | Beacons, broadcasts | APRS, Beacon stations |
| **I-frame (Information)** | Connected QSO with ACKs | BBS, DX cluster sessions |
| **S-frame (Supervisory)** | Flow control, ACKs, RNR | Sliding-window management |

When you "connect" to a BBS at `K1ABC-1`, your TNC sends an SABM frame ("Set Asynchronous Balanced Mode" — start a connection); the BBS sends UA ("Unnumbered Acknowledge"). After that, every I-frame is acknowledged, retried if lost, and delivered in order. Disconnect with a DISC frame.

> ⚙️ **Advanced —** AX.25's sliding window allows up to 7 outstanding frames. Combined with the FCS retry behavior, throughput on 1200 baud can approach 1200 bps when the channel is clean. On a noisy channel, throughput collapses to under 100 bps because every dropped frame triggers retransmission. 9600 baud has the same protocol but with 8x the raw rate, giving usable interactive throughput when the channel is good.

## Why use it

- **Reliable text messaging.** AX.25 is connection-oriented — frames are acknowledged. You know what was delivered.
- **BBS, DX cluster, mail.** Networks of WinLink / BPQ / RMS BBS stations still operate; some are reached only via packet, not internet.
- **TCP/IP over packet.** Older ham networks ran TCP/IP at amprnet.org via 1200 / 9600 baud packet links. Largely historical now but the protocol stack still works.
- **Emcomm.** WinLink uses packet (and other modes) to deliver email-style traffic when the internet is down. ARES uses packet on 145.05 MHz nationally.
- **No internet dependency.** Like APRS but with confirmed delivery.

When to skip it: if you want chat (use PSK31 or JS8Call), real-time tracking (APRS), or anything that benefits from internet connectivity (most operators).

## Operating

**Common frequencies (US):**

| Band | Frequency | Use |
|------|-----------|-----|
| 80m | 3.585 MHz LSB | HF packet (300 baud) |
| 40m | 7.0625 MHz LSB | HF packet (300 baud) |
| 20m | 14.103 MHz LSB | HF packet (300 baud) |
| 2m | 145.010, 145.030, 145.050, 145.070, 145.090 | VHF 1200 baud |
| 2m | 144.910 / 144.930 | VHF 9600 baud |
| 70cm | 441.000–441.350 | UHF 1200 / 9600 |
| 220 MHz | 223.620–223.660 | Some BBSes |

In the 1990s, most cities had at least 5 active packet frequencies. Today, expect 1–2 active per metro area, and many regions have none.

**Software:**

- **Direwolf** (modern, free, cross-platform). Software TNC; pairs with any sound card.
- **UI-View** (Win, classic). Connection-oriented packet client.
- **BPQ32** (Win). Full BBS / node software.
- **AGW Packet Engine**. Connects multiple applications to one TNC.
- **Hardware TNCs:** Kantronics KAM-XL, KPC-3+ (still in production), MFJ-1270B, TNC-X.

**A typical BBS session:**

```
cmd: c K1ABC-1                                   ← connect to K1ABC-1
*** CONNECTED to K1ABC-1
WELCOME TO K1ABC PACKET BBS
LOGIN PROMPT >
> W2DEF                                          ← your callsign
> PASSWORD                                       ← (optional)
* MAIN MENU *
1. List bulletins
2. Send mail
3. List active users
4. Disconnect
5. Help
> 4                                              ← disconnect
*** DISCONNECTED
```

**Path settings:** for connected sessions, you specify the digipeaters explicitly:

```
c K1WX-3 via WIDE1-1, WIDE2-2
```

This connects to K1WX-3 through one local digi and two wide-area digis. Modern packet networks use NET/ROM nodes which automatically route — you connect to a nearby node, then ask it to forward you to the destination.

**Power level:** 25–50 W on VHF FM is plenty; the channel works at low power because it's localized. Don't run high power on packet — it doesn't help and creates more interference.

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| TNC says "no connect" but station is heard | Path wrong, or station's TNC overloaded | Try direct (no via); shorten path; wait and retry |
| Connection established but no response from BBS | BBS is hung; or you typed a wrong character | Disconnect (Ctrl+D in some clients) and retry |
| Lots of retries, slow throughput | Channel congestion; weak signal | Wait for the channel to clear; increase power; or move to a different frequency |
| 9600 baud doesn't connect with a 1200-baud TNC | Different modulation — 9600 needs flat audio, direct FSK | Use a 9600-capable TNC; or operate at 1200 |
| TX deviation wrong (others can't decode) | Audio level needs adjustment | Check TX deviation: 3 kHz for 1200 baud; 5 kHz for 9600 baud |
| Connection drops mid-session | Other side's TNC reset or path failed | Reconnect; check intermediate digis are still active |
| Frames sent but no ACK | Other side not receiving | Verify radio is on RX; verify frequency; verify squelch isn't blocking weak signals |
| Direwolf shows decoded packets but cannot transmit | PTT not connecting | Check Direwolf's PTT settings (RTS/DTR/GPIO); test with `direwolf -p` |
| Latency very high | TNC's "PERSIST" / "SLOTTIME" parameters too aggressive | Default Persist=63, Slottime=10 is usually fine; check TNC config |
| Mode says 1200 baud but I'm hearing FT8-like signals | Tuned to wrong frequency | 1200 baud APRS is on 144.390 — Bell 202 AFSK sounds like a high-pitched chirp, not FT8 |

## See also

- §22-00 — Digital modes overview
- §22-05 — APRS (uses AX.25, same protocol, different application)
- §02 — Repeaters & Bandplans (where the packet sub-bands are)
- §23 — Emergency communications (where packet still earns its keep, especially WinLink)
