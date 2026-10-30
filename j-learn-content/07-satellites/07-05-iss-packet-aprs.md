---
id: 07-05
title: ISS Packet & APRS
chapter: 07
section: 05
level: simple
status: published
---

# ISS Packet & APRS

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The International Space Station carries amateur radio equipment maintained by the **ARISS** program (Amateur Radio on the International Space Station). Several radios in the Columbus, Service Module, and other modules support amateur operations on multiple modes — voice, packet, SSTV, and an FM cross-band repeater. From your back yard, with a $200 setup, you can work the ISS.

This section covers the modes you can work *without an astronaut at the microphone* — the always-on packet digipeater and the cross-band FM repeater. Voice contacts with crew members (the famous "school contacts") are scheduled events; this section briefly notes them but focuses on the everyday ISS packet/repeater use.

## What's on the ISS amateur radio gear

Multiple radios over time. As of 2026, the active configuration generally includes:

- **A 2 m / 70 cm transceiver** in the Columbus module (Kenwood TM-D710GA or similar).
- **An APRS digipeater** that retransmits packets sent up to it on 145.825 MHz (worldwide APRS frequency for satellite gateway).
- **An FM cross-band repeater** (uplink 145.99 MHz, downlink 437.80 MHz; or sometimes the reverse depending on configuration).
- **Voice/SSTV gear** used during scheduled events.

The configuration changes with crew time and mission priorities. Check the ARISS status page (ariss.org) before each session for current operating mode.

## ISS packet (APRS)

The ISS APRS digipeater is the easiest amateur space contact you can make. It's **always on** when no other amateur mode is active, and it digipeats any APRS packet sent on 145.825 MHz worldwide.

### What "digipeat" means

The ISS receives your packet on 145.825, decodes it, and re-transmits it on the same frequency with its own callsign added. Anyone on Earth with a receiver tuned to 145.825 in line-of-sight of the ISS at that moment hears the digipeated packet.

In practical terms: you send a packet → ISS hears it → ISS rebroadcasts it → other ground stations (and APRS-IS via internet gateways) record the receipt. Your packet has officially "gone through space."

### Equipment needed

- **A 2 m radio** capable of transmitting AFSK (audio frequency-shift keying) at 1200 baud — most amateur 2 m radios.
- **A TNC or sound-card software** to encode the APRS packet (Direwolf, UISS, Xastir, APRS+, etc.).
- **A directional antenna** with some gain — handheld Arrow II or fixed Yagi.
- **A pass prediction** for ISS — Heavens-Above and most satellite trackers list the ISS prominently.

### Procedure

1. **Predict the next ISS pass over your station.**
2. **Tune your radio to 145.825 MHz** simplex.
3. **Set up your APRS software** with your callsign, position, and a brief comment.
4. **At AOS, transmit a single APRS position packet.** Don't spam; one transmission is plenty.
5. **Listen for the digipeated copy** on 145.825 — you'll hear your own callsign played back from space within seconds.
6. **Confirm via APRS-IS** (aprs.fi or similar) — your packet should appear on the worldwide map with the satellite gateway path showing it went through ISS.

### Why people do it

- **Easy first space contact** — no QSO partner needed; just send and verify.
- **Educational.** Demonstrates packet radio, satellite operations, and APRS in one short pass.
- **Achievement.** Some hams collect "worked through ISS" as a bragging right.

## ISS cross-band FM repeater

When activated by the crew, the ISS hosts a **cross-band FM repeater** — uplink on one band, downlink on the other. This is genuine voice repeater operation through space, like a giant FM bird.

### Frequencies (typical recent configuration)

- **Uplink**: 145.99 MHz (FM, 67.0 Hz CTCSS)
- **Downlink**: 437.80 MHz (FM)

These can change. Verify current configuration via ARISS status page or AMSAT before transmitting.

### Procedure

Like working any FM bird (see §07-01):

1. Set radio for full duplex, uplink and downlink as above.
2. CTCSS 67.0 Hz on uplink.
3. Listen for the repeater carrier and any active QSOs.
4. Brief, polite QSOs (callsign + grid + sign-off; ~30 seconds total).
5. Manage Doppler — the 437 MHz downlink shifts about ±10 kHz over the pass.

### Etiquette

The ISS repeater is the most-watched amateur satellite in the world. Pile-ups are intense. Practical guidance:

- **Don't dominate.** One QSO per pass.
- **Brief exchanges only.** Callsign + grid + 73, done.
- **Don't tail-end.** Wait for the previous QSO to fully end; respect operator pacing.
- **Don't call CQ unless the bird is quiet.** If you can hear other QSOs, just respond.
- **Don't transmit during voice contacts.** When astronauts are on the microphone (scheduled school contacts and special events), the cross-band repeater is OFF; transmitting on 145.99 just adds noise on the ground.

## ISS voice (scheduled events)

Astronauts occasionally operate the ISS amateur radio for **voice contacts**, typically in two modes:

### Casual operation

When crew time permits, an astronaut may turn on the radio and call CQ. These are unannounced; you find out by hearing them or by being lucky enough to be tuned in. Frequencies vary; common ones are 145.80 (downlink) for voice.

### School contacts (ARISS-supported)

Pre-scheduled contacts between astronauts and school groups. The school sends questions; the astronaut answers in real time. These last 10-15 minutes during a pass.

Anyone with a 2 m radio can listen to school contacts — the downlink is on a publicly known frequency (typically 145.80) and can be heard worldwide via WebSDR receivers near the school's location.

The school groups have priority; **non-school stations should not transmit** on the uplink during a scheduled school contact. Listen only.

### SSTV (Slow-Scan TV)

Periodically, the ISS transmits SSTV image data — sometimes for educational events, sometimes as just a planned amateur activity. The transmission is on 145.80 in PD120 or other SSTV mode; receiving requires a 2 m FM radio and SSTV decoding software (MMSSTV, RX-SSTV, Robot36 for Android).

A successful SSTV decode produces a picture. ARISS sends certificates to operators who report decoded images.

## ISS APRS via the world

Beyond the ISS-as-digipeater pattern above, **the ISS itself reports its position via APRS** when its packet system is in beacon mode. You can receive these beacons on 145.825 just by listening; no transmission required.

Combined with **APRS-IS gateways**, the ISS's beacon is forwarded to the global APRS-IS network. You can see the ISS on aprs.fi from anywhere — it's the icon shaped like a satellite, moving across the world map.

## Common ISS packet/repeater mistakes

- **Wrong frequency.** Verify the current ARISS configuration before each session — frequencies do change.
- **No CTCSS on the cross-band repeater uplink.** No tone = receiver doesn't open. Verify 67.0 Hz set.
- **Transmitting on 145.825 during a pass with no APRS data.** That's just noise; you're blocking real APRS for the duration of your transmission. Send a packet and stop.
- **Camping on the cross-band repeater frequency.** Multiple QSOs are queued behind you; keep it brief.
- **Trying to make a voice contact when no astronaut is on the radio.** The cross-band repeater is automatic; voice is human-driven and only occurs in scheduled events.

## Also worth knowing

- **The ISS speeds across the sky at 7.66 km/sec** — even a high-elevation pass is over in 10 minutes.
- **Maximum slant range**: ~2300 km at horizon, ~415 km at zenith. Slant range determines path loss.
- **Pass frequency** from your QTH: typically 5-7 visible passes per day, of which 2-3 are decent (>10° peak elevation).
- **Best passes** for working voice or cross-band repeater are the high-elevation ones; for APRS, low-elevation passes work fine.

## See also

- §07-00 — Chapter overview
- §07-01 — FM vs linear (the cross-band repeater is FM)
- §07-02 — Doppler shift
- §07-04 — Tracking strategies
- §07-07 — Pass prediction
- §03-05 — APRS (the protocol)
