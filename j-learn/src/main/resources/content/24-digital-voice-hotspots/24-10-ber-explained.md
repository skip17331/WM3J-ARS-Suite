---
id: 24-10
title: BER (Bit Error Rate) Explained
chapter: 24
section: 10
level: mixed
status: draft
---

# BER (Bit Error Rate) Explained

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What it is

**BER — Bit Error Rate** — is the percentage of received bits that the receiver got wrong before forward-error-correction had a chance to fix them. It's the standard digital signal-quality metric in DMR, D-STAR, Fusion, and every other DV mode in amateur use.

If you transmit a frame containing 1000 bits and the receiver receives 995 bits correctly and gets 5 wrong (before error correction), the BER for that frame is 0.5%.

BER is what you watch on a Pi-Star or OpenSpot dashboard to tell whether your hotspot is correctly receiving your radio. Low BER = clean reception = perfect audio. High BER = the network is fighting hard to recover your audio and the recipient hears it as robotic or dropouts.

```
   Pi-Star dashboard last-heard
   ──────────────────────────────
   Time    Mode    Callsign   Target          RSSI   BER
   14:32   DMR     WM3J       TG 3100         -42   0.1%   ← clean
   14:34   DMR     WM3J       TG 3100         -68   2.1%   ← marginal
   14:38   DMR     WM3J       TG 3100         -75   5.4%   ← falling apart
```

## What it measures

A bit error happens when the receiver's symbol-detection step (turning the analog waveform back into 0s and 1s) gets the wrong answer for a given symbol. Causes:

- **Low SNR** — signal close to the noise floor. Random noise nudges the symbol across the decision threshold.
- **Frequency offset** — TX and RX aren't on exactly the same frequency, so the modulation arrives skewed in frequency space.
- **Distortion** — amplifier nonlinearity or modulator imbalance creates symbols that don't sit cleanly at the expected positions.
- **Multipath** — signal arriving via two paths cancels or distorts at the receiver (rare on the very short ranges of a hotspot, common on real repeaters).
- **Receiver bandwidth mismatch** — radio's filters cutting off part of the signal spectrum.

BER counts these errors *before* forward error correction (FEC). FEC adds redundant bits to every frame; if the FEC has enough information to correct the errors that occurred, the audio gets through cleanly even though BER is non-zero. If BER exceeds what FEC can correct, audio is silently corrupted or framed-dropped.

## The "digital cliff" — why DV doesn't gracefully degrade

Analog FM has a property amateurs are accustomed to: as the signal weakens, the audio gradually becomes noisier, but you can still copy with effort. There's a slow degradation curve from "armchair copy" through "fading in and out" to "buried in noise but still 70% readable."

Digital voice has no such curve. Instead it has a **cliff**:

```
       Audio quality
       ↑
       │                                ╱─────── perfect audio ──────
       │                              ╱
       │                            ╱
       │                          ╱  ← cliff (very narrow signal range)
       │      ─────────────────╱
       │              silence / robotic / dropouts
       │
       └──────────────────────────────────────────→ Signal strength
                                     ↑ threshold
```

Below the threshold (typically around 1% BER post-FEC, or ~5–7% pre-FEC depending on mode), the codec gets enough corrupt frames that it produces:

- Silence (the frame was dropped entirely)
- Robotic "Dalek" sounds (the codec is trying to interpret corrupt parameters)
- Stuttering (some frames recovered, others lost)

There's almost no "I can copy with effort" in DV — either it decodes or it doesn't. This is the digital cliff and it's the single most criticized property of DV modes.

Hotspot operating mostly lives well above the cliff (the radio is across the room with a clear path), so the cliff rarely shows up. It does show up when:

- The hotspot is in another room with thick walls.
- The hotspot is in a metal-framed building with poor RF leakage.
- The hotspot's TX power is set too low.
- The hotspot is on a marginal frequency (close to another strong transmitter).

## Target BER values

Rough quality bands for hotspot operating:

| BER | Quality | What you hear |
|-----|---------|----------------|
| **0%** | Perfect | Cleanest audio the codec can produce |
| **<1%** | Excellent | Indistinguishable from 0% to most listeners |
| **1–3%** | Good | Audio is clean; occasional very brief artifact possible |
| **3–5%** | Marginal | Some listeners notice slight artifacts; occasional dropouts |
| **5–10%** | Poor | Frequent dropouts, robotic patches, stuttering |
| **>10%** | Unusable | Mostly silence; codec gives up; recipient hears garbage |

If your hotspot shows BER consistently >2%, something is wrong and worth investigating. <1% should be the normal operating state for a properly-tuned hotspot at conversational range.

## How to lower BER

In rough order of which is most likely to be the actual problem on a typical setup:

### 1. Frequency calibration

The most common cause of high BER on a fresh hotspot. The MMDVM HAT's TCXO is slightly off-frequency, and the radio is slightly off in the other direction. Net offset of even 200–500 Hz raises BER noticeably; >1 kHz makes things fall apart.

**Fix:** Run **MMDVMCal** (built into Pi-Star). Procedure:

```bash
ssh pi-star@<ip>
sudo pistar-mmdvmcal
```

Pi-Star's tool guides you through:

1. Generate a known calibration tone (a steady GMSK/4FSK signal).
2. Receive it with another radio that you trust the frequency of.
3. Measure the offset.
4. Enter the offset as **RXOffset** and **TXOffset** in Hz in the dashboard *Modem* page.

Typical corrections: ±300 to ±2000 Hz. Once set, BER usually drops dramatically (from 5%+ down to <1%).

### 2. RF level / power

A simplex hotspot whose TX power is too low won't be heard reliably by your handheld; if your handheld can't decode the hotspot's transmissions, the *reverse* direction (radio → hotspot) is fine but the dashboard shows the hotspot's RX direction only.

Conversely, *too much* RF power can also degrade BER by saturating the radio's front-end (intermod, ALC clamping, etc.) — though this is rare on a 10–20 mW hotspot.

Sweet spot for Pi-Star RF level: 50–80 (out of 255). Adjust and watch BER.

### 3. Antenna and placement

The stock antennas shipped with most cheap MMDVM HATs are minimal — sometimes just a stub. Improvements:

- Add a small SMA whip antenna ($5–$10). Usually halves BER.
- Move the hotspot away from your home Wi-Fi router (the 2.4 GHz and 5 GHz radios from a router can desense a nearby UHF receiver).
- Avoid putting the hotspot inside a metal enclosure unless you want short range.
- Distance between hotspot and handheld: 1 m is plenty; 10+ m starts to degrade.

### 4. Mode-specific tuning

**D-STAR (6.25 kHz channel):** Most sensitive to frequency offset. Calibrate carefully; tolerance is roughly ±200 Hz.

**DMR (12.5 kHz channel, TDMA):** Sensitive to **timing** as well as frequency. The DMR frame structure depends on accurate slot alignment. If your Pi's system clock is off, BER rises. Make sure NTP is working.

**Fusion (C4FM, 12.5 kHz channel):** Roughly between D-STAR and DMR in tolerance.

### 5. Hotspot hardware quality

The cheapest Jumbospots have crude TCXOs and may drift so much during a single QSO that you can watch BER climb over 5 minutes of operating. After 30+ minutes warm-up, drift stabilizes. If your BER varies with time-since-power-on, the TCXO is the culprit. Upgrade options:

- **Pi-Star duplex HAT with documented TCXO** — ~$120, stable from cold.
- **OpenSpot 4** — built-in quality oven-stabilized reference.
- **External heat-sinking** on the MMDVM chip can help with stability.

### 6. Codec-specific noise

Sometimes BER is fine, but audio still sounds bad. That's not a BER problem — that's a codec issue (clipped audio in your radio's microphone path, double-encoding from a cross-mode bridge, low-volume input). Check:

- Mic gain on your radio (most DV radios have a separate digital mic gain setting).
- Whether the network you're using is bridging through another codec (DMR → D-STAR transcoding adds artifacts even with perfect BER).

> **Advanced —** BER on a Pi-Star dashboard is technically *raw* BER — counted before FEC. Modern DV modes apply substantial FEC: DMR adds Reed-Solomon and Hamming codes that recover most single-bit errors. The "audio breakup threshold" is therefore not at the raw BER value shown but at the *uncorrected* BER after FEC, which depends on the burst-error structure of the channel. A signal with 4% raw BER from random Gaussian noise may decode perfectly; a signal with 1% raw BER but in bursts (e.g., from a brief multipath null) may produce more audible artifacts. Pi-Star's BER number is a useful proxy but not the full story.

## Reading BER on a Pi-Star dashboard

The Pi-Star dashboard shows two BER values:

- **Per-transmission BER** — averaged over the full duration of the last keyup.
- **Live BER** — updated in real time during a transmission (visible if you watch live).

The dashboard also color-codes:

- Green: BER < 1%
- Yellow: BER 1–3%
- Orange: BER 3–5%
- Red: BER > 5%

Watching the dashboard during your own test transmissions is the fastest way to tune a new hotspot.

## See also

- [§24-07](24-07-hotspot-pistar.md) — Pi-Star calibration via MMDVMCal
- [§24-08](24-08-hotspot-openspot.md) — OpenSpot's BER display
- [§24-09](24-09-duplex-vs-simplex-hotspots.md) — How simplex/duplex affects BER measurement
- [§24-11](24-11-cross-mode-linking.md) — BER doesn't tell the whole audio story when bridging modes
- [§01](../01-propagation/) — General signal-quality concepts (though propagation barely applies at hotspot ranges)
