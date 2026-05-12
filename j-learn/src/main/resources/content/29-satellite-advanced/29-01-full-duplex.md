---
id: 29-01
title: Full-Duplex Operation
chapter: 29
section: 01
level: mixed
status: draft
---

# Full-Duplex Operation

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

**Full-duplex** means your station transmits and receives simultaneously, on different frequencies. **Half-duplex** means the radio can only do one or the other at a time — push-to-talk transmits and mutes the receiver; release-to-receive listens and disables the transmitter.

A typical terrestrial FM contact is half-duplex even when the rig is technically capable of more, because both stations are on the same frequency and one TX deafens the other RX. Telephones are full-duplex (you can hear yourself talking and the other person interrupting). Satellite operating on FM birds or linear transponders is fundamentally a full-duplex problem — your TX is on one band, your RX is on another, and you need to hear what the satellite is doing to your signal at the moment you're transmitting it.

## Why full-duplex matters on linear transponders

Linear transponders are non-negotiable for full-duplex operation. Three reasons:

**1. Doppler tracking is impossible without it.** The downlink frequency shifts continuously through the pass. On SSB, a few hundred Hz of mis-tracking turns your voice into Donald Duck or Darth Vader. You correct by ear in real time — you hear your own voice come back through the bird and adjust the uplink (or the downlink) to keep yourself sounding natural. You cannot do this if you can't hear yourself.

**2. Verification that the satellite heard you.** On a linear transponder, the only confirmation that your uplink is making it through is hearing your own voice on the downlink. If you transmit blind and don't hear yourself, you have no way to know whether the satellite received nothing (uplink off-frequency, polarization mismatch, antenna mis-pointed, power too low) or whether you're working fine and just nobody happened to be listening.

**3. Power-level discipline.** A linear transponder's downlink power is shared across all users. If you overdrive your uplink — too much power, too aggressively pointed — your signal eats more of the transponder's output and crushes other QSOs. The only way to set power correctly is to listen to your own downlink and adjust until you're at about the same signal level as other operators. Without full-duplex, you have no feedback loop.

## Why full-duplex matters on FM birds

Strictly, you can work an FM bird half-duplex. Many operators do. But you give up several useful things:

- **Doppler correction on the downlink.** Same as above — without hearing the downlink while you transmit, you can't tell when your audio frequency-shifts out of the FM channel.
- **Knowing the bird is up.** Some FM birds have intermittent uplinks; you may be transmitting into a dead carrier. Hearing yourself confirms operational status.
- **Knowing you've captured the bird.** FM has the capture effect — the strongest signal wins. If somebody else is also transmitting, only one of you gets through. Hearing yourself confirms it's you.
- **Recognizing a courtesy tone, beacon, or end-of-QSO.** Some FM birds drop a courtesy beep at the end of each transmission. You only hear it if you're listening while not yourself transmitting — but full-duplex lets you hear when others stop.

The standard sat HTs (Yaesu FT-2DR, FT-3DR, FT-5DR; Kenwood TH-D74; the discontinued but excellent Icom IC-W2A) all support full-duplex. The cheap dual-band BaoFengs and most older HTs do not — they have cross-band capability but only half-duplex.

## Two ways to be full-duplex

### Two-radio setup

The traditional approach: one radio for the uplink, one for the downlink. The uplink rig transmits; the downlink rig receives continuously, on a different band, with its own antenna.

A common configuration for a serious portable satellite kit:

- **Uplink radio:** Yaesu FT-857, Icom IC-705, or any all-mode 2 m / 70 cm rig set to TX-only on the uplink band.
- **Downlink radio:** A second IC-705, a dedicated satellite-listening rig, or an SDR like the RSP1A on the downlink band.
- **Two antennas, ideally cross-polarized** — one on each band, or a dual-band Yagi like the Arrow with separate feedlines for each band so the uplink and downlink rigs don't interfere through the radio's own TX/RX switching.

The two rigs are completely independent; the uplink can transmit at any time while the downlink continues to listen. Cost is higher (two rigs), complexity is higher (two CAT cables, two audio paths if recording), but the result is true full-duplex with no compromises.

### Single-radio (sat-radio) setup

Some radios do full-duplex internally — they have a separate receiver chain for one band while the main TX chain handles the other. The current generation:

- **Icom IC-9700** — the canonical modern satellite rig. Native full-duplex on 2 m / 70 cm / 23 cm, with built-in Doppler tracking on the satellite memory channels. The dual VFOs are configured as MAIN (typically downlink) and SUB (typically uplink); MAIN receives continuously while SUB transmits.
- **Icom IC-905** — newer, adds 13 cm and 3 cm; same satellite-mode architecture as the IC-9700.
- **Kenwood TS-2000** — older but still in service; full-duplex on 2 m / 70 cm with a built-in satellite mode (SAT button).
- **Yaesu FT-991A** — partially. It can do satellite mode with full-duplex on some band pairs but is more limited than the IC-9700; check the manual for the specific uplink/downlink combinations supported.

The IC-9700 is the dominant choice as of 2026 for new satellite stations. The native Doppler tracking is a real convenience — point the rig at a satellite memory entry and it adjusts both VFOs continuously without external software.

## Headphone discipline

Whatever your hardware, full-duplex requires headphones. Speakers feedback the downlink audio into the microphone and the bird re-transmits your own voice with a delay, which is both annoying and unprofessional. Headphones — closed-back, comfortable — are not optional for serious satellite work.

> ⚙️ **Advanced —** Some operators run a noise-cancelling headset (e.g., Heil Pro 7) with a sidetone-and-monitor mix that lets you hear both the satellite downlink and your own outgoing audio at adjustable levels. This is the equivalent of a broadcast-engineer's cue mix and is the smoothest way to work a linear transponder pass while paying attention to Doppler in real time.

## See also

- §29-02 — Transponder etiquette (depends on full-duplex)
- §29-09 — Linear transponder strategy
- §07-01 — FM vs linear satellites
- §07-02 — Doppler shift
