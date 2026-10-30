---
id: 17-12
title: Bandwidth
chapter: 17
section: 12
level: mixed
status: published
---

# Bandwidth

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Bandwidth is the frequency range over which a circuit, antenna, or signal operates. It's defined by where some quantity drops to a specified threshold — typically the 3-dB-down points (half-power points) of a filter or antenna's response.

The same word covers radically different things: signal bandwidth (an SSB signal is ~2.4 kHz wide), receiver IF bandwidth (a CW filter is ~500 Hz), antenna bandwidth (a dipole's "2:1 SWR" range), and crystal-filter bandwidth (a 9 MHz IF filter is 2.7 kHz). The formulas below pin down each one.

## Equations

For an L−C resonant circuit (and for a single-tuned antenna):

```
BW = f_r / Q
```

For a Gaussian filter (approximate; useful for receiver passbands):

```
BW_3dB = 0.94 × σ × √(8 × ln(2))
       ≈ 2.35 × σ
```

For SSB modulation:

```
Channel BW ≈ 2.4 kHz (audio bandwidth from ~300 Hz to ~2700 Hz)
```

For a digital mode at symbol rate R:

```
Channel BW ≈ R × (1 + α)    (α ≈ 0.2..0.35 for common pulse shapes)
```

## Variables

| Symbol | Quantity | Units |
|--------|----------|-------|
| BW | Bandwidth (3 dB unless stated) | hertz (Hz, kHz, MHz) |
| f_r | Center / resonant frequency | hertz |
| Q | Quality factor (§17-11) | dimensionless |
| σ | Standard deviation (Gaussian filter) | hertz |
| R | Symbol rate (digital signal) | symbols / second |
| α | Roll-off factor | dimensionless (0..1) |

## Antenna bandwidth (the SWR-2:1 rule)

Most amateurs measure antenna bandwidth as the frequency range over which SWR ≤ 2:1 (or ≤ 1.5:1 for tighter installs). For a single-tuned antenna:

```
BW_2:1 ≈ f_r / Q × √2   (approximate)
BW_2:1 ≈ f_r × 1.41 / Q
```

A higher-Q antenna has narrower bandwidth — that's the tradeoff. A wire dipole on 80m might have Q ~10, giving 80m BW ~280 kHz (covers most of the band). A loaded mobile whip on 80m might have Q ~50, giving 80m BW ~70 kHz — much narrower.

## Worked example — receiver filter bandwidths

| Mode | Typical IF filter | Notes |
|------|------:|-------|
| AM broadcast | 6 kHz | Wide enough for HiFi audio |
| FM repeater | 12.5 kHz (narrow) / 25 kHz (wide) | Per regulatory standard |
| SSB voice | 2.4–3.0 kHz | Modern SSB filters; "wider" sounds better |
| CW | 250 / 500 Hz | Most modern rigs offer 500 Hz; 250 Hz for crowded contests |
| Digital (FT8) | 2.5 kHz | Software handles individual signals within the audio passband |
| RTTY | 250–500 Hz | Mark + space tones inside the filter |

A 500 Hz CW filter is appropriate for chasing weak signals; a 2.4 kHz SSB filter is appropriate for casual conversation. Wider filters let in more noise, so use the narrowest filter that admits your wanted signal.

## Worked example — crystal filter Q

A 9 MHz SSB crystal filter has a 2.4 kHz bandwidth. What's its Q?

```
Q = f / BW = 9×10⁶ / 2400 = 3,750
```

That's a very high Q for a manufactured filter. Quartz crystals achieve Qs in the tens of thousands. Mechanical filters (Collins, Heath, Yaesu) get Qs in the thousands. L/C filters cap out around Q = 100–200 for practical reasons.

## Worked example — channel bandwidth for FT8

FT8 uses 8-FSK at 6.25 baud (15 second slots, 75 tones per symbol). The 8-tone alphabet at 6.25 baud:

```
Tone spacing = 6.25 Hz
Total tones = 8
BW ≈ 8 × 6.25 = 50 Hz per signal
```

A receiver with a 2.5 kHz audio passband can hold about 2500 / 50 ≈ 50 simultaneous FT8 signals — which is why FT8 operates so densely. (See §03 for digital-modes bandwidth math.)

## Common mistakes

- **Calling it "−6 dB bandwidth" when you mean "−3 dB bandwidth."** Both are valid measures; the −6 dB BW is wider. Always state which.
- **Confusing audio bandwidth and RF bandwidth.** SSB transmits a 2.4 kHz audio passband as a 2.4 kHz RF channel. Both are 2.4 kHz, but they're different signals (one is at audio, one at the carrier ± 2.4 kHz).
- **Reading antenna bandwidth without a defined SWR threshold.** "The dipole has 200 kHz bandwidth" is meaningless without "at SWR ≤ 2:1." Measure with an analyzer and pick a threshold.
- **Forgetting that bandwidth is symmetric only for symmetric filters.** Some antennas have asymmetric SWR sweeps (more responsive on one side of resonance than the other); state both edges.

## See also

- §04 — Repeaters & Bandplans (channel-spacing rules)
- §06 — Antennas (antenna bandwidth specs)
- §17-05 — Resonant frequency
- §17-11 — Q Factor (relates Q to BW)
- §03 — Digital modes (where signal bandwidth is computed per protocol)
