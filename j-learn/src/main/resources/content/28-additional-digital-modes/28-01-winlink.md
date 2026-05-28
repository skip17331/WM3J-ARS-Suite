---
id: 28-01
title: Winlink
chapter: 28
section: 01
level: mixed
status: draft
---

# Winlink — Radio Email for Emergency Comms

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What it is

Winlink is a worldwide network that moves **email-format messages** over amateur radio. A user at a laptop runs **Winlink Express**, composes an email like in any other email client, connects to a nearby **RMS gateway** by radio, and the message is forwarded into the Winlink CMS (Common Message Server) and out to its destination — either another Winlink address or a regular internet email address.

When the internet is up, Winlink runs entirely over the internet (Telnet sessions to the CMS). The interesting case is when the internet is *down*: the radio gateways keep working, and email flows over HF or VHF instead. This is what makes Winlink the de facto standard for amateur emergency communications in the US, Canada, and many other countries.

Winlink is not a "real-time chat" mode. Each session opens a link, exchanges queued messages in both directions, and closes. Typical session length is 30 seconds to 5 minutes depending on traffic and band conditions.

## How it works

| Property | Value |
|----------|-------|
| Architecture | Client → RMS gateway → CMS → recipient |
| Client software | Winlink Express (Win), Pat (cross-platform), RMS Express (legacy) |
| Connection modes | Telnet (internet), Packet (VHF FM), VARA FM, VARA HF, ARDOP, Pactor |
| Message format | RFC-822 email with optional attachments |
| Maximum message size | Practically: 50–120 kB per session (mode-dependent) |
| FEC | Mode-dependent (ARDOP, VARA, Pactor each provide their own) |
| Encryption | None on amateur frequencies (FCC §97.113) |
| Routing | Automatic via CMS; recipient pulls from any RMS |

The user's client doesn't pick a route across the network — it just connects to *one* RMS, and the CMS handles everything afterward. The CMS is mirrored across multiple servers (US, EU, AU) for redundancy; if one CMS is down, the RMS automatically uses another.

**RMS gateways** are radio-attached servers run by volunteers worldwide. As of 2025 there are roughly 1,500 RMS stations, with denser coverage in the US, Canada, Western Europe, and Australia. An operator picks the nearest gateway with the right mode (VARA HF, Pactor, etc.) and propagation.

> **Advanced —** The Winlink protocol stack is mode-agnostic. The RMS gateway speaks a protocol called **B2F** (Forwarding Protocol 2, Binary) with the client, regardless of whether the underlying transport is Pactor, ARDOP, VARA HF, VARA FM, or Telnet. B2F handles the message queue, compression (LZHUF), and acknowledgments. Each radio mode is just a "pipe" that B2F runs through. This is why the same .b2f files transfer cleanly across all transports.

## Why use it

- **Email when the internet is gone.** Hurricane Maria (Puerto Rico, 2017): when the island lost both cell and internet for weeks, ARRL-coordinated ham volunteers moved roughly 18,000 Winlink messages in / out. This is the defining Winlink use case.
- **ICS form transmission.** Winlink Express ships with all standard ICS forms (ICS-213, ICS-309, etc.) as built-in templates. Operators fill out the form on screen and send; recipient gets a fillable PDF or formatted text. Served agencies (Red Cross, county EOCs, state EMA) prefer this over voice.
- **Background operation.** A Winlink station can sit unattended, polling for messages periodically. Useful for shelter operators who can't dedicate a body to radio duty.
- **No internet at *either* end.** Messages can be sent and received entirely by radio if both stations use radio paths to RMS gateways.
- **Email gateway.** Recipients without a Winlink account see a normal email; senders can reply by normal email and it routes back to the radio operator.

When to skip it: Winlink is **not** a real-time mode. For tactical voice traffic, voice nets are still faster. For a casual digital chat, FT8 or PSK31 are simpler.

## Operating

**Software:** Winlink Express (free, Windows-only, official). **Pat** (open source, runs on Linux / macOS / Windows / Raspberry Pi, command-line + web UI). Both interoperate fully.

**Modes by band and use case:**

| Band class | Preferred mode | Range | Throughput |
|------------|---------------|-------|-----------|
| Local (VHF/UHF FM) | Packet (1200 bps) | 15–50 km | 1.2 kbps |
| Local high-speed | VARA FM | 15–50 km | 10–25 kbps |
| Regional / state | VARA HF, ARDOP | 100–2000 km | 0.5–4 kbps |
| Long-haul HF | VARA HF, Pactor III/IV | 500–5000 km | 0.5–4 kbps |
| Internet (testing / drills) | Telnet | unlimited | 100+ kbps |

**A typical Winlink session:**

1. Open Winlink Express. Compose message (or fill an ICS form).
2. Hit "Post to Outbox."
3. Open a new session — pick mode (e.g. VARA HF Winlink).
4. Pick an RMS gateway from the channel list (sorted by predicted propagation).
5. Tune rig, click "Start." VARA HF connects in ~10 seconds.
6. Outbox is uploaded; inbox is downloaded.
7. Session closes. View any received mail.

**RMS gateway lists** are kept current on winlink.org. Winlink Express also has a built-in "Propagation" tool that uses VOACAP-style HF propagation prediction to rank gateways by expected SNR for your current path and time.

**Power level:** 25–100 W on HF is typical. Excess power doesn't help — VARA's link adaptation will pick the appropriate symbol rate, and over-driving just splatters.

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| "No CMS available" on Telnet test | Internet not reachable | Verify network; try `ping cms.winlink.org` |
| VARA HF connects but throughput is 100 bps | Path too weak for higher rates | Move to a closer RMS, try a different band, or increase TX power |
| Session opens, then disconnects mid-transfer | RFI killing the modem, or audio levels wrong | Add a common-mode choke at the rig; check ALC light off during TX |
| "User not authorized" error | Account not yet validated | First-time accounts must send a message from any RMS within 30 days; or pay the $24 system donation |
| Can transmit but cannot receive | Audio routing wrong | Verify rig's audio in/out is bound to VARA's audio device |
| ICS form rendering broken on recipient end | Recipient's Winlink Express is out of date | Templates change; both ends should be on current Winlink Express |
| Gateway shows "BUSY" persistently | Channel in use by another station | VARA HF and Pactor allow one user at a time — wait or pick another RMS |
| Pactor not connecting despite good signal | TNC firmware out of date | SCS modems need periodic firmware updates from scs-ptc.com |
| Session keeps retrying ARQ blocks | Marginal SNR | Reduce TX power slightly (over-driving hurts) or move band |

## Common mistakes

- **Treating Winlink as instant messaging.** It's not — expect minutes, not seconds.
- **Skipping the activation message.** New accounts must send a message from a real RMS within 30 days or the account is locked.
- **Encrypting attachments** to "protect" sensitive info. FCC §97.113 forbids encryption on amateur frequencies (except limited specific exceptions for control of a satellite or model craft). Winlink has been carefully designed to comply; don't break that.
- **Running TX power flat-out.** VARA's link adaptation handles weak signals well. 100 W is the typical ceiling; 25 W often works equally well to a nearby RMS.
- **Not practicing.** Winlink is muscle memory under stress. Drill regularly — monthly SETs, weekly net check-ins.

## See also

- §21 — Emergency Comms (the principal use case)
- §28-02 — VARA HF (most common HF transport for Winlink today)
- §28-03 — VARA FM (most common VHF transport for Winlink today)
- §28-07 — Pactor (legacy commercial-grade transport)
- §28-10 — Digital messaging workflows (how this fits in a real incident)
