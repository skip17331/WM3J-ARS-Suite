---
id: 24-11
title: Cross-Mode Linking
chapter: 24
section: 11
level: mixed
status: draft
---

# Cross-Mode Linking

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## The problem

You have a DMR HT. Your friend has a D-STAR HT. The two protocols are completely incompatible at the RF layer — different modulation, different framing, different codec (DMR's AMBE+2 vs D-STAR's AMBE), different routing model (talkgroups vs reflectors), different networks (BrandMeister vs the D-STAR gateway federation).

Talking to each other directly over RF is impossible.

But: if both of you can reach the *internet*, the audio is just a stream of bits. A bridge can:

1. Decode your DMR AMBE+2 to PCM audio.
2. Re-encode the PCM as AMBE for D-STAR.
3. Reformat the framing (talkgroup ID → reflector module + URCALL).
4. Send the D-STAR-formatted version to a D-STAR network.
5. Your friend hears it from their local D-STAR repeater or hotspot.

That's cross-mode linking. The bridges that do this come in three flavors:

- **OpenSpot built-in** — does the transcoding inside the hotspot itself.
- **Pi-Star "MultiMode" / WPSD** — transcoding at the hotspot too, with more limitations.
- **Server-side reflectors (XLX, BM bridges)** — transcoding happens on a central server, your hotspot just speaks its native mode to the server.

## How transcoding works under the hood

The pipeline for cross-mode bridging:

```
   Mode A (e.g. DMR)              Bridge                 Mode B (e.g. D-STAR)
   ────────────────                ──────                ──────────────────────

   AMBE+2 bits (incoming)
      │
      ▼
   AMBE+2 decoder ──▶ PCM audio (8 kHz, 16-bit)
                          │
                          ▼
                   AMBE encoder ──▶ AMBE bits (outgoing)
                                       │
                                       ▼
                              D-STAR frame builder
                                       │
                                       ▼
                                  REF/XRF/DCS network
```

Two key facts:

1. **Audio goes through an intermediate PCM representation.** It's not bit-for-bit translation; it's full decode-then-re-encode.
2. **You pay a codec penalty.** Every codec is lossy. Decoding AMBE+2 to PCM loses some information. Re-encoding the PCM to AMBE loses more. The output is *audibly worse* than a direct same-codec QSO.

The audio quality penalty depends on:

- How different the source and destination codecs are.
- Whether the bridge does any audio cleanup (some apply noise gating, some don't).
- The quality of the AMBE chip / software implementation.

In practice, cross-mode bridged audio sounds like:

- Slightly tinnier than native.
- More obviously "digital" (artifacts on consonants).
- Quiet passages may have audible quantization noise.
- Background noise from the originating side is sometimes amplified by the second codec.

Listeners experienced with DV usually notice the difference but rate it as "tolerable." Listeners new to DV may not even hear the difference unless told to listen for it.

## AMBE ↔ AMBE+2 specifically

DMR and Fusion use **AMBE+2**. D-STAR uses original **AMBE**. They are *cousins* — same codec family, same parametric vocoder structure, similar bit rates — but not directly compatible.

Going from AMBE+2 to AMBE means:

- Decode AMBE+2 → 8 kHz / 16-bit PCM
- Re-encode PCM → AMBE (slightly more aggressive compression)

This is the most common cross-mode transcoding path and is fairly clean. Pi-Star handles it via the **DSD** (Digital Speech Decoder) library or hardware AMBE chips. OpenSpot does it natively.

Going from AMBE+2 (DMR) to AMBE+2 (Fusion) is theoretically just a re-framing operation — no codec re-encoding needed if the bit rates match. In practice, frame sizes differ (DMR is 30 ms slots, Fusion is 20 ms frames), so even "same codec" bridges require some re-packetization.

> ⚙️ **Advanced —** The patent expiration on AMBE in ~2017 enabled open-source decoder implementations (the **md380tools** project on the firmware side, and the **mbelib** library on the software side). Before that, all transcoding required a licensed DVSI hardware chip — which is why pre-2018 cross-mode bridging was rare and expensive. Today, MBE-family transcoding happens in cheap hardware (Raspberry Pi handles it at 5–10% CPU) and quality is comparable to the chip-based implementations. The actual perceptual codec algorithm is identical; only the chip-vs-software question changes.

## Bridge implementations

### OpenSpot — transcoding at the hotspot

The OpenSpot is the most user-friendly cross-mode bridge. You configure a **Connector** that defines:

- **Modem mode** (what RF mode the OpenSpot operates in — e.g., DMR)
- **Network type** (what network it bridges to — e.g., D-STAR REF030 C)

When you key your radio in DMR, the OpenSpot transcodes to D-STAR and forwards to REF030 C. To everyone on REF030 C, you appear as a D-STAR transmission with your callsign embedded. They reply on D-STAR; the OpenSpot transcodes back to DMR and emits it on your local RF.

The OpenSpot 4 does this in software; the 4 Pro has a hardware AMBE chip. Setup is a single Connector definition in the dashboard.

### Pi-Star — DMRGateway with cross-mode option

Pi-Star can do limited cross-mode bridging via the **DMRGateway** daemon configured with cross-mode targets. The most common setup:

- DMR talkgroup → forwarded to a YSF or D-STAR reflector
- The bridge is unidirectional (DMR-in to YSF-out, but the reverse may require separate config)

It's clunkier than OpenSpot but works. Configuration is in `/etc/dmrgateway` (Pi-Star dashboard has a *Cross Mode* page but it's incomplete).

### XLX reflectors — server-side transcoding

The cleanest cross-mode model is **XLX** — Extended XReflector. An XLX server is a single network entity that:

- Accepts D-STAR connections (REF/XRF/DCS protocols)
- Accepts DMR connections (BrandMeister / IPSC2 protocols)
- Accepts YSF connections (Fusion reflector protocol)
- **Bridges them all internally** — a transmission from any mode is transcoded and forwarded to all the others within the same reflector module.

Your hotspot connects to the XLX reflector in **its native mode** — no transcoding in your hotspot. Then the XLX server's central transcoder handles the cross-mode work, doing it once for all listeners on the reflector.

Famous XLX reflectors:

| XLX | Region | Modes bridged |
|------|--------|----------------|
| **XLX307 A** | New England | D-STAR + DMR + YSF |
| **XLX555 A** | Crossroads (US Multi-state) | All three |
| **XLX950 D** | Italian | EU-focused |
| **XLX076** | UK | UK-focused |

To use XLX from Pi-Star: configure D-STAR with reflector "XLX307 A". Audio you key on your hotspot goes to the XLX server, which forwards it to all the DMR / YSF / D-STAR participants in module A. Their audio comes back through your hotspot as D-STAR.

The advantage of XLX: server-side transcoding is *one* operation per transmission for everyone in the reflector module. Hotspot-side transcoding (OpenSpot) does it once per hotspot, which is fine but means each hotspot needs to be capable.

The disadvantage: XLX modules have limited capacity (a few hundred users max per module) and there are fewer of them than there are DMR talkgroups.

## MultiMode hotspots

A "multimode hotspot" colloquially means a hotspot that can switch RF modes on demand — sometimes within a single device, sometimes via separate channels. Most modern hotspots (Pi-Star, OpenSpot, WPSD) qualify because they support DMR, D-STAR, and Fusion at the firmware level.

A **multimode hotspot configured for cross-mode** means it's set up to receive in mode X and bridge to a network of mode Y simultaneously. This is the configuration that gives you "use my DMR HT, talk to D-STAR users" without ever programming a different radio.

OpenSpot defaults to multimode behavior. Pi-Star requires explicit cross-mode setup in the configuration pages.

## Practical gotchas

### Callsign metadata can be lost

DMR transmissions are tagged with a **DMR ID** (numeric). D-STAR transmissions are tagged with a **callsign** (8-char string). When bridging DMR → D-STAR, the bridge has to look up the DMR ID and produce a callsign — this requires the bridge to have a current copy of the DMR ID database. Most bridges do, but if your DMR ID isn't registered or the bridge's database is stale, your callsign on the D-STAR side may appear as `?????` or as your numeric DMR ID. This confuses recipients.

### Audio levels can mismatch

Each mode has its own assumptions about microphone gain and audio dynamic range. Bridges typically don't compensate. A loud DMR transmission may sound clipped after re-encoding as D-STAR; a quiet D-STAR transmission may sound buried after re-encoding as DMR. Cultural workarounds: speak slightly closer to the mic on the mode that's being transcoded *into*, or use a hotspot that allows audio gain adjustment on the bridge output.

### Some bridges are unidirectional

Not every bridge is two-way. Some YSF-to-DMR bridges (older configurations) only carry YSF traffic into DMR but not the reverse. If you can hear someone but they can't hear you, the bridge is half-broken — check the documentation for that specific reflector/talkgroup pairing.

### Bridge hopping multiplies the codec penalty

If a transmission goes DMR → bridge → YSF → bridge → D-STAR, the audio has been re-encoded three times. Each step adds quantization noise. The result sounds noticeably worse than any single-bridge configuration. Avoid chains of bridges when possible — pick a reflector that does all the mode-bridging in one server (XLX) rather than several daisy-chained ones.

## See also

- [§24-08](24-08-hotspot-openspot.md) — OpenSpot's built-in cross-mode bridging
- [§24-07](24-07-hotspot-pistar.md) — Pi-Star cross-mode configuration
- [§24-05](24-05-dstar-routing.md) — XLX as a D-STAR reflector
- [§24-04](24-04-dstar.md) — AMBE vs AMBE+2 codec differences
- [§24-10](24-10-ber-explained.md) — BER doesn't measure the codec-penalty side of audio quality
