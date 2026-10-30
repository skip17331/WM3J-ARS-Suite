---
id: 04-06
title: Duplexers
chapter: 04
section: 06
level: advanced
status: published
---

# Duplexers

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A duplexer is the heavy steel-and-copper sculpture that lets a repeater **transmit and receive on the same antenna at the same time**. Without one, the repeater would either need two physically separated antennas (expensive, ugly, ground-hogging) or its receiver would be deafened by its own transmitter every time it keyed up.

This is one of the deepest pieces of repeater engineering. Most operators never need to know the details — but if you ever build a repeater, repair one, or wonder why old duplexers are sometimes worth more than the radio, this chapter is for you.

## The problem in numbers

Take a typical 2 m repeater:

- Transmit on 146.340 MHz at +20 W (43 dBm).
- Receive on 146.940 MHz, with a sensitivity of around –124 dBm (0.18 µV).
- Frequency separation: just 600 kHz.

If both went straight to the same antenna with no isolation, the receiver would see 43 dBm of its own transmitter at 146.340 — that's 167 dB above the receiver's noise floor. The receiver wouldn't just be desensitized — its front end would be **destroyed** within seconds.

The duplexer's job is to provide enough **isolation** between TX and RX paths to bring the TX signal down to where the receiver can ignore it (–95 dBm or below at the front end). For our 2 m repeater that means **138 dB or more of isolation between TX and RX, in only 600 kHz of frequency separation**.

That's an enormous engineering challenge. It's why duplexers look like sewer pipes welded together.

## How they work

A duplexer is a network of **resonant cavity filters** connected by precise lengths of coax cable. Each cavity is a hollow metal tube tuned to pass one frequency and reject another nearby frequency. By chaining several of them in the right configuration, the duplexer routes:

- **Antenna → Receiver** for signals on the receive frequency, blocking the transmit frequency
- **Transmitter → Antenna** for signals on the transmit frequency, blocking energy on the receive frequency

Two main designs:

### Bandpass cavity

A simple cavity that passes a narrow window of frequencies (a few hundred kHz wide) and attenuates everything else. Insertion loss is low (~1 dB) at the pass frequency; rejection at the reject frequency depends on Q.

### Bandpass-bandreject (notch) cavity

A bandpass cavity with an additional notch tuned 600 kHz (or whatever your offset is) above or below the pass frequency. The notch can provide 30–40 dB of rejection at the offset frequency in a single cavity.

A typical 2 m duplexer has **3 cavities per side, 6 cavities total**, all bandpass-notch, providing 90–100 dB of isolation TX-to-RX. Combined with the natural antenna-to-antenna isolation (which is essentially zero on a single shared antenna) and the cable lengths, the system gets to the 130+ dB needed.

## Why the cavities are so big

Resonant cavity Q (the sharpness of the filter) scales with the cavity's surface area. Bigger cavities = higher Q = sharper filters with less insertion loss. A 2 m duplexer cavity is roughly **6 inches in diameter and 18 inches tall**. UHF (70 cm) cavities are smaller. HF would require cavities the size of a small car — which is why HF doesn't use them; HF repeaters (rare) use frequency-separation duplexers tuned to far-apart frequencies.

> **Advanced —** Cavity Q scales as `Q ≈ (π/2) · (D/δ)`, where D is the diameter and δ is the skin depth at the operating frequency. At 146 MHz the skin depth in copper is about 5.5 µm, so a 6-inch cavity (~150 mm diameter) has Q ≈ 21,000 in theory, more like 8,000–10,000 in practice after the loading by the loop couplers. That's enough to give 60–80 dB rejection at 600 kHz offset with reasonable insertion loss.

## Tuning a duplexer

Done with a service monitor or a vector network analyzer (VNA). The procedure for each cavity:

1. Connect VNA's TX port to the cavity's input loop, RX port to the output loop.
2. Find the cavity's resonant peak — adjust the tuning rod (a threaded rod that changes the cavity's effective length) until the peak is at your pass frequency.
3. Adjust the notch tuning (a separate set screw on each cavity) to put a deep null at the reject frequency.
4. Move on to the next cavity in the chain.

After all cavities are tuned, verify TX-to-RX isolation across the antenna port with a signal generator on the TX side and a sensitive receiver on the RX side. You want at least 90 dB at the offset frequency.

**Don't fiddle with a duplexer's cavities without a VNA or service monitor.** A "tuned" duplexer with a thumbprint on the wrong screw can be 50 dB worse than untuned, and you won't know it from the radio side until the receiver is mysteriously deaf.

## Insertion loss and what it costs you

Even a well-tuned duplexer has **insertion loss**:

- Typical 2 m duplexer: 1.5 to 2.0 dB on each leg (TX and RX).
- Total system loss vs no duplexer: about 4 dB round-trip.

That's nontrivial — 4 dB is the difference between covering 30 miles and covering 18 miles. But it's the price of fitting on one tower with one antenna.

## When a duplexer goes bad

Common failures:

| Symptom | Likely cause |
|---------|--------------|
| Repeater suddenly has reduced range | Tuning drifted; recheck with VNA |
| Receiver desensed when TX is on | Notch frequency drifted off the TX frequency; or a coupling loop loosened |
| RX sensitivity good cold, bad hot | Thermal drift; common in cheap or aging cavities |
| One frequency works, the other doesn't | One side's tuning drifted further than the other |
| Repeater ID at full power but normal traffic ker-chunks | Not the duplexer — usually the controller's audio mute |

Tuning typically drifts ~10 kHz per year due to thermal cycling, vibration, and slow corrosion of the cavity contacts. A well-built duplexer in a well-controlled environment can hold tuning for 5+ years. A roof-mounted duplexer in a high-vibration environment may need annual retuning.

## What about other technologies?

A few alternatives exist:

- **Two-antenna systems** — separate TX and RX antennas with vertical separation (typically 30+ feet on the same tower). Provides isolation by physical distance plus directional antenna patterns. Works, but requires twice the antenna real estate.
- **Circulators** — solid-state ferrite devices that route signals one direction only (TX → antenna → RX). Used in cell-phone base stations. Bulky for VHF amateur use; common in commercial UHF microwave links.
- **Receive-only diversity sites** — separate receivers at multiple sites, no TX at the receive sites; transmit only at one main site. Common in commercial VHF systems; expensive for amateur.

For amateur repeaters, **bandpass-notch cavity duplexers remain the standard** because they work, they last, and used ones are cheap on the surplus market.

## Buying a used duplexer

Common brands to look for: **Telewave, EMR, Sinclair, TX RX Systems, Wacom, Decibel**. All build durable cavities. Avoid no-name imports; the tuning hardware on cheap copies is unreliable.

Used 2 m duplexers in working condition run $200–$600. UHF duplexers, $300–$800. Look for:

- Connectors not corroded
- Tuning rods threaded smoothly (not seized)
- All cavities the same model and size (mixing cavity types in one duplexer doesn't usually work)
- Good documentation of the last tuning frequencies

Plan to retune any used duplexer to **your** repeater's exact frequencies.

## See also

- §04-01 — the repeater the duplexer enables
- §18 (Coax & Connectors) — connectors used at the duplexer ports
- §17-formula-appendix — Q factor and resonance math
