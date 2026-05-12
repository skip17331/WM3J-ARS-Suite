---
id: 25-03
title: Spectrum Analyzers
chapter: 25
section: 03
level: mixed
status: draft
---

# Spectrum Analyzers

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A spectrum analyzer (SA) is the **frequency-domain twin** of an oscilloscope. Where a scope shows you voltage vs. time, an SA shows you signal power vs. frequency. Two signals at the same frequency add up on a scope but are indistinguishable; the same two signals on an SA appear as two separate spectral lines you can measure individually.

For amateur work this matters in three big areas:

- **Harmonic and spurious emissions** — am I clean enough to meet Part 97 limits?
- **Receiver noise floor / interference identification** — what's *that* signal at 7.165 MHz?
- **Filter and amplifier characterization** — does this band-pass filter actually reject what I want it to?

## What an SA shows that a scope can't

An SA's display is a 2-D plot: **frequency on X, signal amplitude on Y**. A 100 W transmitter feeding a dummy load looks like a tall thin spike at the carrier frequency, with shorter spikes at 2×, 3×, 4× the carrier (harmonics), and a noise-floor "grass" along the bottom of the display.

```
    amplitude (dBm)
       │
       │  ████  carrier (+50 dBm, 100 W)
       │  ████
       │  ████
       │  ████
       │  ████          ▒    2nd harmonic (-10 dBm-ish if PA is good)
       │  ████          ▒
       │  ████          ▒    ▒  3rd harmonic
       │  ████          ▒    ▒
       │  ████ ▒▒▒░░▒▒▒ ▒▒▒░░▒▒░▒░░▒▒░░▒▒░▒░░░  noise floor (-110 dBm)
       └──────────────────────────────────────► frequency
          14 MHz       28 MHz   42 MHz
```

A scope can't separate the carrier from the harmonics — they all add up to one composite waveform whose distortion suggests harmonics exist but can't measure them. The SA shows each one as a discrete peak with a measurable amplitude in dBm.

## The three controls that matter most

An SA has more knobs than a scope, but three dominate:

### Center frequency and span

You're looking at a range of frequencies centered on `center_freq`, with width `span`. E.g. center = 14.200 MHz, span = 100 kHz → showing 14.150 to 14.250 MHz.

For looking at harmonics of a 14 MHz transmitter, you want a much wider span — say center = 50 MHz, span = 100 MHz → showing 0 to 100 MHz, which catches the fundamental, 2nd, 3rd, 4th, 5th, 6th, 7th harmonics in one screen.

### Resolution bandwidth (RBW)

The SA works by sweeping a narrowband filter across the spectrum and measuring the power in that filter at each frequency. The filter's bandwidth is the **RBW**. Two effects:

- **Narrower RBW = better frequency resolution.** Two signals 1 kHz apart are visible as separate peaks only if RBW < 1 kHz.
- **Narrower RBW = lower noise floor displayed.** The noise floor is `kTB × NF`, so reducing B by 10× drops the displayed noise floor by 10 dB.
- **Narrower RBW = much slower sweep.** Sweep time goes roughly as `1 / RBW²`. A 1 Hz RBW sweep across 100 MHz takes hours.

Rule of thumb: pick the **widest RBW that still resolves what you need to see.** For harmonics on a 100 MHz span, 100 kHz RBW is fine. For separating two CW signals 500 Hz apart, you need 100 Hz RBW or narrower.

### Video bandwidth (VBW)

After the RBW filter detects the signal, a video filter smooths the display. **Lower VBW = smoother trace, hides noise but also slows the response to real signals.**

Default: VBW = RBW / 10 or so. For high-precision amplitude readings, VBW = RBW / 100 gives an averaged, clean noise floor. For watching transient signals (a fast-keyed CW transmitter, a hopping digital signal), VBW = RBW so you see the real-time peak.

## Reading harmonic content

The classic ham use of an SA: **am I making harmonics within FCC limits?**

Part 97.307(d) requires harmonics on HF be **at least 43 dB below the carrier** for transmitters above 30 W (more strict at higher power), measured into a dummy load. To measure:

1. Connect the rig → directional coupler / attenuator → SA. **Never** plug full transmit power into an SA directly. A 30 dB attenuator + the coupler's coupling factor should bring the signal to around -10 dBm at the SA's input.
2. Set SA: center = (fundamental + 2× to 5× harmonic range), span = wide enough to capture out to the 7th harmonic.
3. Key the rig into a dummy load.
4. **Reference level**: set so the carrier peak is near the top of the display.
5. **Measure**: marker on carrier, peak → reference. Marker delta → each harmonic. The delta is the rejection in dB.

A clean modern HF rig typically shows -55 to -65 dBc on harmonics — well inside Part 97 limits.

> ⚙️ **Advanced —** The SA's own RF front-end produces spurious responses if overdriven. **Always check by inserting a 10 dB attenuator and confirming all the peaks drop by exactly 10 dB.** If a "harmonic" drops by 20 dB or more, it was generated inside the SA — your DUT is cleaner than the SA was telling you. Calibrated lab SAs (HP 8566, Keysight N9000) have very high input intercept points; cheap SAs (TinySA) saturate easily and produce internal spurs above -20 dBm input.

## Looking at spurious emissions

"Spurious" = anything that's not the wanted signal or its expected harmonics. Examples:

- **LO leakage** — local oscillator frequency leaking through into the transmit output. Common in cheap kit transmitters.
- **Image frequency** — for a transmitter that mixes two oscillators, the unwanted image is at `LO ± wanted` (the one you didn't intend).
- **Intermodulation products** in a multi-tone signal — see next section.
- **Power supply spurs** — switching frequency sidebands at carrier ± switching frequency.

A 100 MHz span around the carrier, with the RBW set to ~1 kHz, reveals close-in spurs. A wider span out to a few hundred MHz reveals far-away spurs.

## IMD (intermodulation distortion) products

When two tones go into a nonlinear amplifier, you get not just the two tones at output but also **mix products**: `2f1 - f2`, `2f2 - f1`, `3f1 - 2f2`, etc. The 3rd-order products `2f1 - f2` and `2f2 - f1` are close to the original tones (and inside the SSB passband if the tones were modulating audio), so they don't get filtered out — they show up as distortion / splatter.

The standard **two-tone test**:
1. Inject two equal-amplitude tones at f1 = 700 Hz and f2 = 1900 Hz audio into the rig's mic input (the spacing is chosen so the IMD products land at audible frequencies).
2. Look at the rig's RF output on the SA.
3. You should see two peaks at the carrier ± 700 Hz and ± 1900 Hz. The IMD products appear as smaller peaks at carrier ± (2×1900 - 700) = ± 3100 Hz and similar.
4. Measure the dB difference between the wanted tones and the IMD products. **30 dB below PEP** is "decent"; **35 dB below** is "very good"; **20 dB below** is "splatter monster."

A modern rig running into a dummy load should hit ~-30 dBc 3rd-order IMD. The same rig run too hard into a marginal PA will show -15 dBc and you're causing QRM 5 kHz away from your carrier.

## Cheap-end SA options

You don't need a Keysight to get useful SA work done.

| Option | Frequency range | Dynamic range | Cost | Notes |
|--------|-----------------|---------------|------|-------|
| **TinySA Ultra** | 100 kHz – 6 GHz | ~70 dB | $130 | Excellent value; has built-in signal generator |
| **TinySA (original)** | 100 kHz – 350 MHz (extended to 960 MHz) | ~70 dB | $60 | Good HF + 2 m / 70 cm; slow sweep |
| **RTL-SDR + GQRX or SDR# panadapter** | 24 MHz – 1.7 GHz | ~50 dB | $30 | Software-defined; needs PC; great for survey |
| **HackRF One** | 1 MHz – 6 GHz | ~50 dB | $300 | Similar to RTL-SDR but wider; can also TX |
| **Siglent SVA1015X** | 9 kHz – 1.5 GHz | ~100 dB | $1700 | Mid-tier bench SA + VNA combo |
| **Used HP 8566B** | 100 Hz – 22 GHz | ~120 dB | $1500–3000 used | Magnificent but heavy and old |

For ham work checking harmonics and looking at interference, **TinySA Ultra** is the recommended starting point. Below $200 and adequate for everything below "do I really meet Part 97?" (where you'd want a calibrated instrument).

## Calibration and accuracy

SAs need calibration to read absolute power correctly. A few rules:

- **Trust relative readings (dBc) more than absolute readings (dBm).** The carrier vs. harmonic ratio depends only on the SA's linearity, which is usually good. The absolute amplitude depends on calibration that may have drifted.
- **Use a known reference signal** for absolute calibration. A signal generator at exactly -20 dBm is the standard reference. After calibration, the SA should read -20 ± 1 dBm.
- **Re-cal annually** or after travel. Cheap SAs may drift more.

See **§25-09 Calibration Workflows** for the full procedure.

## Common mistakes

- **Overdriving the input.** Damages the front end or creates fake spurs. Always use an external attenuator for transmitter measurements.
- **Reading RBW-dependent noise floor as if it were a fixed number.** Noise floor moves 10 dB when you change RBW by 10×. The signal-to-noise ratio is what matters.
- **Mistaking SA-internal spurs for real signals.** Insert an attenuator and verify all peaks drop proportionally. Internal spurs drop faster than 1:1.
- **Span too narrow.** Showing only 14.200 ± 100 kHz won't reveal the harmonic at 28.4 MHz. Use wide span when looking for far-away spurs.
- **Span too wide for fine work.** Showing 0–6 GHz won't resolve a -50 dBc IMD product 3 kHz from the carrier. Use narrow span and narrow RBW for close-in.

> ⚙️ **Advanced —** Modern SAs offer **FFT-based** analysis as an alternative to swept-tuned. FFT mode captures a wide span instantaneously (no sweep), but at the cost of dynamic range and image rejection. For transient signals (a hopping digital mode, a pulsed radar) FFT mode is the only way to see them. For steady-state harmonics, swept-tuned is more accurate. The Siglent SSA3000X-Plus and similar have both modes.

## See also

- §25-04 — Tracking Generators (SA companion for sweeping filter response)
- §25-02 — Oscilloscopes (the time-domain twin)
- §25-09 — Calibration Workflows
- §14 — RFI (finding interference with an SA)
- §15 — Noise Sources
- §17 — Formulas (dB, dBm, dBc math)
