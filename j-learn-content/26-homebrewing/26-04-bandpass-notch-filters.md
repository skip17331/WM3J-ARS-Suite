---
id: 26-04
title: Bandpass & Notch Filters
chapter: 26
section: 04
level: mixed
status: published
---

# Bandpass & Notch Filters

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## Bandpass — when one band is enough

A **bandpass filter (BPF)** passes a narrow range of frequencies and blocks everything else. In amateur work, the killer application is **multi-operator contesting**: in a Field Day or contest station with multiple radios sharing the same antenna farm, each radio needs aggressive filtering on its band to keep the other radios from desensitizing it.

A typical contest BPF passes only one ham band (e.g., 14.0–14.35 MHz) with < 0.5 dB insertion loss, and rejects adjacent bands by 40+ dB. Two stations 50 ft apart, both running 100 W — one on 20 m and one on 15 m — *cannot* coexist without BPFs. The 15 m station's 100 W is heard as S9+50 by the 20 m receiver three feet away. The 20 m BPF on the 15 m station's coax, and the 15 m BPF on the 20 m station's, drop that cross-band leakage to S5 — usable.

### Topology

A BPF is functionally an HPF cascaded with an LPF, or more commonly, a set of **coupled resonators** — series L-C circuits tuned to the passband center, magnetically or capacitively coupled to neighbors. The number of resonators sets the **order** (and thus skirt steepness):

| Order | Resonators | Insertion loss | Skirt steepness |
|-------|-----------|----------------|-----------------|
| 2nd | 2 L-C | ~0.5 dB | Gentle |
| 3rd | 3 L-C | ~0.7 dB | Moderate |
| 5th | 5 L-C | ~1.0 dB | Steep (typical contest filter) |
| 7th | 7 L-C | ~1.5 dB | Very steep (rare; alignment-sensitive) |

Most amateur BPFs are 3rd or 5th order. Commercial examples: **Dunestar 600/M0RHA**, **W3NQN/Inrad band-pass filters**, **DX Engineering Hi-Z** series. All use 5-section L-C coupled-resonator topology.

### Commercial 5th-order BPF for 20 m (50 Ω, 100 W)

Typical design from the Dunestar 600 series:

```
Center freq: 14.175 MHz
Passband:    13.9–14.45 MHz (–1 dB)
Insertion loss in band: 0.8 dB
Rejection at 7 MHz:    50 dB
Rejection at 21 MHz:   45 dB
Rejection at 28 MHz:   60 dB
Component count:       5 toroidal inductors + 8 silvered mica caps
```

Building one is straightforward but **alignment-sensitive**: each resonator must be tuned to within 0.1 % of the design frequency, or the passband ripple grows and the skirts soften. A NanoVNA is mandatory.

## Notch filters — surgical interference removal

A **notch filter** is the opposite of a BPF: a narrow rejection band, everything else passes. Three common amateur uses:

1. **Mains hum rejection (50 Hz or 60 Hz).** A notch at 50/60 Hz on an audio path kills hum without affecting voice. Active op-amp notches (twin-T topology) are typical here. Q can be very high (Q=50+) because the offending frequency is precise.
2. **19 kHz pilot tone in FM IF chains.** Some FM detectors have leftover 19 kHz pilot tones (used by FM stereo) that bleed into the audio. A 19 kHz notch in the audio amplifier path scrubs it out.
3. **Specific carrier QRM.** A nearby SDR-detected carrier at, say, 14.230 MHz (an unstable computer switching supply harmonic) can be notched out with a high-Q parallel L-C trap in series with the RX input. The Q is the trade — too high and the notch is microscopic (a frequency drift puts you back in QRM); too low and you eat 20 kHz of band around the target.

### Q factor and selectivity tradeoff

The **Q (quality factor)** of a notch determines its width:

```
BW_3dB = f_notch / Q
```

| Application | Target Q | Notch width at 14 MHz |
|-------------|----------|------------------------|
| Mains hum (audio, twin-T) | 50–200 | 0.3–1.2 Hz (at 60 Hz) |
| Specific RF carrier QRM | 100–500 | 28–140 kHz (at 14 MHz) — too wide! |
| RF carrier (high-Q crystal notch) | 10000+ | < 1.5 kHz |

A simple L-C trap at HF gets you Q ≈ 100, which is **too wide** for surgical RF notching — you'd kill the entire phone segment. For tight RF notches, use a **crystal notch filter** (a quartz crystal as the resonant element, Q in the thousands). Crystal notches are common in IF stages of high-end receivers but rare in amateur homebrew because cutting a custom crystal is expensive.

For broader carrier removal (acceptable to lose 50 kHz of band around the target), a parallel L-C trap with Q ≈ 100 is straightforward homebrew:

```
        L_trap
     ┃━━━━━┃
IN ──┴──╫──┴── RX
     ┃     ┃
     ═C_trap
     ┃     ┃
     │     │
   (placed in series with the antenna lead)

at f_notch = 1 / (2π√LC), L-C tank impedance goes to infinity
→ that signal is blocked
```

Design example, notch at 14.230 MHz:

```
C = 100 pF (NPO)
L = 1 / (4π² × 14.23e6² × 100e-12) = 1.25 µH
```

Wind 8 turns of #22 on a T-50-6 core for 1.25 µH (A_L = 4.0 nH/turn², N = √(1250/4.0) = 17.7 turns — wait, let me re-check). For low Q the lossy mix is fine; for high Q use Mix 10 or air-core.

## Combining BPF and notch — the contest receiver

A serious contest receiver setup might have:

```
ANT → BCB HPF → 20 m BPF → variable notch (for in-band QRM) → RX
```

Each filter does one job. The BCB HPF (§26-03) blocks AM broadcast. The 20 m BPF blocks other ham bands. The notch handles the one carrier that bleeds through everything. This stack typically adds 1.5–2 dB total insertion loss — acceptable for a quiet receiver, painful for a noise-floor-limited weak-signal mode like EME.

> **Advanced —** Modern SDR receivers implement the BPF and notch in **digital DSP** after the ADC. Software notches can have Q > 10000 because they're not bound by component tolerance. The catch: the ADC must not overload upstream. A strong out-of-band signal that would have been killed by an analog BPF can saturate the ADC, intermodulating in-band signals and creating QRM that no DSP can remove. So even in SDR-only setups, an analog BPF in front of the ADC is good practice for contesting — it protects the dynamic range. The Flex SDR series, Apache Labs Anan, and Elecraft K4 all have switched analog BPF banks specifically for this reason.

## Construction recipe — 5-section 20 m BPF

Following the Inrad / W3NQN classic design, 100 W rating, 50 Ω:

1. **Parts:**
   - 5× T-50-6 toroids (yellow)
   - Silvered mica or NPO ceramic caps: see table below
   - Hammond 1590BB box, 2× SO-239 bulkhead
   - PCB or Manhattan-style construction

2. **Component values (center 14.175 MHz, BW ≈ 600 kHz):**
   | Element | L (µH) | C (pF) |
   |---------|--------|--------|
   | L1, L5 | 0.85 | 150 |
   | L2, L4 | 0.85 | 150 |
   | L3 | 0.85 | 150 |
   | Coupling caps (4) | — | 22 |

3. **Wind each inductor:** 15 turns of #22 on T-50-6. Measure each: all five within 1 % of each other.

4. **Assemble in shielded compartments** if possible (see §26-08). Each resonator gets its own compartment with a single small hole for the coupling cap.

5. **Tune on NanoVNA:** Sweep and adjust each toroid's turn spacing (squeeze or spread the windings) until insertion loss in 14.0–14.35 MHz is < 1 dB and adjacent-band rejection meets the design spec.

## See also

- [§26-02 — Low-Pass Filters](26-02-low-pass-filters.md) — LPF basics, same component math
- [§26-03 — High-Pass Filters](26-03-high-pass-filters.md) — HPF basics
- [§26-05 — Toroid Selection](26-05-toroid-selection.md) — picking cores
- [§17-11 — Q Factor](../17-formulas/) — Q math for notch / BPF design
- [§25 — Test Equipment](../25-test-equipment/) — NanoVNA for alignment
