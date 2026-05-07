---
id: 04-17
title: Diversity
chapter: 04
section: 17
level: mixed
status: draft
---

# Diversity

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A single antenna captures one snapshot of a fading signal. **Diversity reception** uses two or more antennas (or two or more frequencies, or two polarizations, or two propagation paths) and combines or selects between them — recovering signal during fades that would kill a single-antenna receiver.

This is mostly an HF concept (where multipath fading is severe) and a VHF/UHF mobile concept (where motion through reflective environments creates rapid fading). It rarely matters for static VHF/UHF FM operation but is foundational for serious DX, weak-signal, and mobile work.

## Why diversity helps

When a signal arrives via multiple paths (skywave + ground wave on HF, direct + reflected on VHF mobile), the paths add or cancel depending on phase. Phase varies with time (ionosphere) and position (mobile motion), producing **fading**.

A second antenna a fraction of a wavelength away from the first sees a **different combination** of those same paths — because the path differences are different. When one antenna's signal fades, the other often doesn't. Selecting the stronger of the two, or combining them coherently, recovers a usable signal.

The **diversity gain** depends on how decorrelated the two antennas' received signals are:

| Correlation between antenna outputs | Diversity gain at 90% of fades |
|------------------------------------:|-------------------------------:|
| 1.0 (identical) | 0 dB (no help) |
| 0.7 | 1-2 dB |
| 0.5 | 3-4 dB |
| 0.3 | 5-7 dB |
| 0.0 (uncorrelated) | 8-10 dB |

So even moderate diversity can recover 3-7 dB of fading margin — often enough to turn an unreadable signal into a workable one.

## The four flavors

| Type | What it diversifies | Where it's used |
|------|---------------------|------------------|
| **Space diversity** | Two antennas separated in space | HF DXing, VHF mobile, MIMO Wi-Fi |
| **Polarization diversity** | One H + one V antenna at the same location | HF reception (Faraday rotation), satellites |
| **Frequency diversity** | Same content sent on two carriers | Repeater linking (rare in amateur), commercial / broadcast |
| **Time diversity** | Same data sent twice with delay between | FEC + interleaving in digital modes (FT8, JT65) |

For amateur work, **space diversity** and **polarization diversity** are the two useful ones for receive; the other two are mostly digital-mode features.

## Space diversity

Two physically separated antennas. To work, the two should be:

- **Far enough apart** that they see different multipath combinations. For HF this is typically **¼λ to ½λ** apart at the operating frequency. For VHF, even a few meters can give good decorrelation in a multipath-rich environment.
- **Connected to a diversity receiver** that selects or combines. Some HF receivers have two RF channels (Flex-6700, Icom IC-7851 in dual-receive mode); others use a separate auxiliary receiver and the operator switches manually.

Common implementations:

- **Two vertical antennas** spaced ½λ apart for 80m DXing.
- **Beverage receiving array** (long wire antennas at different angles) — selecting between two beverages with different headings is a form of space diversity.
- **Phased arrays** (4-square, 8-square verticals) — combining produces a directional pattern AND diversity gain in one design.

> ⚙️ **Advanced —** True space-diversity gain requires the receiver to combine the two signals correctly. Three combining methods:
> - **Selection combining**: pick the stronger antenna at any moment. Simplest; near-optimal for binary fading.
> - **Maximal-ratio combining (MRC)**: weight each antenna by its instantaneous SNR before summing. Mathematically optimal; requires phase-coherent reception. SDR-based receivers can do this in software.
> - **Equal-gain combining**: sum the two with equal weight after phase alignment. Simpler than MRC; ~1 dB worse.
>
> Most SDR transceivers offer MRC mode in their dual-RX configuration.

## Polarization diversity

One horizontal antenna + one vertical antenna at the same location. Each captures a different component of an arriving wave. If Faraday rotation flips the wave's polarization mid-path (HF), one antenna fades while the other peaks.

Common implementations:

- **Crossed dipoles fed independently** — receive on both, select stronger.
- **Quad antennas** — naturally bi-polarized.
- **Slant-45° antennas in dual-pol pairs** (broadcast / cellular).

Less effective for VHF/UHF where the path doesn't randomize polarization, but useful for any frequency where ionospheric or atmospheric depolarization is significant.

## When diversity isn't the right answer

- **Strong, stable LOS signals** (FM repeaters, line-of-sight VHF/UHF) don't fade much; diversity adds cost and complexity for marginal benefit.
- **Single-antenna installations** with sufficient SNR margin don't need diversity.
- **Most digital modes** (FT8, JT65, etc.) handle weak signals through FEC and time integration — they often outperform diversity at the same low SNR.

Diversity earns its keep in **HF DX, VHF/UHF weak-signal**, and **mobile** scenarios.

## A practical HF diversity setup

Two 80m verticals spaced ½λ (~40 m / 130 ft) apart, each fed by its own coax to a dual-RX SDR transceiver. On a fading 80m DX signal:

1. Tune the same frequency on both receivers.
2. Enable **dual-RX with diversity combining** (manufacturer name varies — Flex calls it "diversity receive", Icom calls it "dualwatch").
3. Listen — the combined signal should be more readable than either antenna alone during fades.

Expected gain: 3-6 dB on signals near the noise. On a clean signal, no benefit.

## In digital modes

Modern digital modes implement diversity-like features in software:

- **FT8 / FT4 / Q65** — long integration time + FEC effectively averages out short fades.
- **JS8Call (Slow mode)** — 30-second per-symbol time gives strong time-diversity behavior.
- **MIMO HF (experimental)** — two-antenna two-receiver setups with software combining are starting to appear in homebrew SDR projects.

These don't replace true antenna diversity but achieve similar end-results for the modes designed around them.

## See also

- §01 — Propagation (multipath fading mechanism)
- §04-15 — Radiation Patterns (pattern shaping, the basis for some diversity schemes)
- §04-16 — Polarization (the basis for polarization diversity)
- §21 — Digital modes (FT8, JS8Call use time/coding diversity in software)
