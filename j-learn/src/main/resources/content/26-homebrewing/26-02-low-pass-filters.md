---
id: 26-02
title: Low-Pass Filters for Harmonic Suppression
chapter: 26
section: 02
level: mixed
status: published
---

# Low-Pass Filters for Harmonic Suppression

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## Why the LPF is mandatory

Every transmitter — commercial or homebrew — produces **harmonics**. A class-C or class-E final stage is essentially a square-wave generator at the fundamental frequency; without filtering, its second harmonic is typically –15 dB below the fundamental, and the third is around –20 dB. FCC §97.307 and equivalent national regulations require harmonic suppression of at least **–43 dBc** below 30 MHz and **–60 dBc** above. Without an LPF on your transmitter output, you cannot legally key up.

A homebrew class-E QRP rig running 5 W at 7 MHz, with no filter, would put about 150 mW out on 14 MHz (the second harmonic) and 50 mW on 21 MHz (third). That's enough to be heard worldwide on those bands while you think you're operating on 40 m. You will get an FCC inquiry, deservedly.

## Topology: Chebyshev vs Butterworth

Two filter families dominate amateur LPF designs:

| Family | Passband response | Stopband rolloff | Used when |
|--------|-------------------|------------------|-----------|
| **Butterworth** | Maximally flat (no ripple) | Gentle | Audiophile / lab use, where passband flatness matters |
| **Chebyshev** | Equal ripple in passband (0.1–1 dB) | Steeper | **Almost all amateur LPF** designs — better rejection per inductor |
| Elliptic / Cauer | Ripple in both passband + stopband | Steepest | Tightest cutoffs; rare in homebrew |

For harmonic suppression, **0.1 dB-ripple Chebyshev** is the universal amateur choice. The 0.1 dB ripple is invisible (you cannot hear it, your SWR meter won't catch it), and the harmonic rejection per filter section is the best you can get without going to elliptic complexity.

## Cutoff frequency selection

The cutoff f_c is set **10–20 % above the top of the operating band** — high enough that the passband loss is negligible, low enough that the second harmonic is well into the stopband.

| Band | Top of band | LPF f_c (typical) | Second harmonic | Stopband attenuation goal |
|------|-------------|-------------------|------------------|---------------------------|
| 160 m | 2.000 MHz | 2.2 MHz | 4.0 MHz | > 40 dB |
| 80 m | 4.000 MHz | 4.5 MHz | 8.0 MHz | > 40 dB |
| 40 m | 7.300 MHz | 8.0 MHz | 14.6 MHz | > 40 dB |
| 30 m | 10.150 MHz | 11.0 MHz | 20.3 MHz | > 40 dB |
| 20 m | 14.350 MHz | 15.5 MHz | 28.7 MHz | > 40 dB |
| 17 m | 18.168 MHz | 19.5 MHz | 36.3 MHz | > 50 dB |
| 15 m | 21.450 MHz | 23.5 MHz | 42.9 MHz | > 50 dB |
| 12 m | 24.990 MHz | 27.0 MHz | 50.0 MHz | > 60 dB |
| 10 m | 29.700 MHz | 32.0 MHz | 59.4 MHz | > 60 dB |

The 10 m filter is the toughest because the second harmonic (59 MHz) is at the edge of the FM broadcast band; the FCC limits there are aggressive.

## G3RJV's classic component tables

Rev. George Dobbs G3RJV published the most-copied amateur LPF tables in his *G-QRP Club* construction articles (republished in *SPRAT*). They are the de facto standard for QRP homebrew. A 5- or 7-element Chebyshev LPF with 0.1 dB ripple in 50 Ω:

### 5-element (3-inductor) LPF — typical QRP

```
         L1          L2          L3
  IN ─ ┃━━━━━┃ ─┬─ ┃━━━━━┃ ─┬─ ┃━━━━━┃ ─ OUT
                C1            C3
                ┴             ┴
                ═             ═
                ─             ─
                ┬             ┬
                gnd           gnd
```

| Band | L1 = L3 | L2 | C1 = C3 |
|------|---------|----|---------|
| 80 m | 2.40 µH | 2.74 µH | 1500 pF |
| 40 m | 1.32 µH | 1.50 µH | 820 pF |
| 30 m | 0.94 µH | 1.06 µH | 560 pF |
| 20 m | 0.66 µH | 0.75 µH | 390 pF |
| 17 m | 0.51 µH | 0.58 µH | 330 pF |
| 15 m | 0.43 µH | 0.49 µH | 270 pF |
| 12 m | 0.37 µH | 0.42 µH | 220 pF |
| 10 m | 0.31 µH | 0.36 µH | 180 pF |

A 5-element filter gives roughly **35–40 dB** of second-harmonic rejection. For 10 m and above, use a 7-element design (two more L-C sections) to reach 50–60 dB.

## Winding the inductors

Inductors dominate filter loss; getting the toroid right is the difference between a filter that works and a filter that overheats at 100 W.

| Band | Recommended core | Mix | Why |
|------|------------------|-----|-----|
| 160–80 m | T-50-2 or T-68-2 | red (Mix 2) | High permeability, low loss at LF |
| 40–30 m | T-50-6 or T-68-6 | yellow (Mix 6) | Best Q in this range |
| 20–15 m | T-50-6 or T-50-17 | yellow / blue-yellow | Mix 6 fine to ~20 MHz |
| 12–10 m | T-50-17 or T-50-10 | blue-yellow / black | Lower µ, better at higher f |
| 6 m | T-50-10 or T-37-10 | black (Mix 10) | Optimized for 30–100 MHz |

See [§26-05](26-05-toroid-selection.md) for the full toroid mix reference.

**Turns count** is computed from the A_L value:

```
N = 100 × √(L_µH / A_L_µH_per_100²)
```

Or, using the more common "A_L in nH per turn²":

```
N = √(L_nH / A_L)
```

A T-50-6 core has A_L = 4.0 nH/turn². For 1.5 µH:

```
N = √(1500 / 4.0) = √375 = 19.4 turns
→ wind 19 or 20 turns, measure with NanoVNA, trim or add as needed
```

Wire is typically #22 to #26 enameled magnet wire. Use #22 for 100 W, #24 for 10 W, #26 only for QRPp (< 1 W) or above-30-MHz designs where the wire diameter matters for self-resonance.

## Capacitors — voltage, type, tolerance

For QRP (5 W): standard 100 V silvered mica or NPO ceramic, 5 % tolerance. Mouser stocks the Cornell-Dubilier CDV series and Vishay NP0 in standard E12 values.

For 100 W: 500 V silvered mica or high-voltage NPO ceramic. **Do not** use Z5U / X7R / Y5V ceramics — their capacitance changes radically with voltage and temperature, ruining the filter response.

For legal-limit (1500 W): doorknob ceramics (CKE TD or Centralab type 850) rated 3 kV+. The peak voltage across the shunt caps at 1500 W in 50 Ω is roughly 866 V_rms = 1.2 kV peak, so 3 kV gives a safe margin.

## Verifying with a NanoVNA

Build the filter, sweep it on a NanoVNA from 100 kHz to 100 MHz, and check:

1. **Insertion loss in passband:** should be < 0.3 dB across the operating band
2. **Return loss in passband:** should be < –20 dB (SWR < 1.22)
3. **Second-harmonic rejection:** check at 2 × top-of-band
4. **Cutoff frequency:** the –3 dB point should match your design

If passband insertion loss is high (> 0.5 dB), the inductors are wound on the wrong mix (too lossy) or with too-thin wire. If the cutoff is too low, you have too many turns; trim and re-sweep.

> **Advanced —** A 7-element Chebyshev gives ~10 dB more harmonic rejection than a 5-element at the cost of one more inductor and one more cap. For combined filter/match networks (e.g., the output of a class-E PA that *needs* a specific load reactance), an elliptic-with-imaginary-zeros design places a notch at exactly 2× the fundamental, getting 60+ dB rejection there. The trade is filter sensitivity to component tolerance — 5 % caps may not be tight enough; you'll want 1 % or hand-selected.

## Common LPF construction mistakes

- **Wrong toroid mix.** A T-50-43 (ferrite) in a 14 MHz LPF burns up at 100 W. Use powdered-iron (T-prefix), not ferrite (FT-prefix), for filters.
- **Lead inductance.** At 30 MHz, 1 cm of capacitor lead is ~10 nH of stray inductance, which detunes the filter. Mount caps close to the board with short leads.
- **No shielding.** A bare LPF on perfboard couples to the rest of the circuit. Shield with a tin can or aluminum partition (see §26-08).
- **Inadequate cap voltage rating.** Z5U caps at 100 W go nonlinear and inject harmonics back into the supposedly-filtered output.
- **Skipping verification.** Always sweep with a NanoVNA after build. Filters are deterministic; if it doesn't measure right, it isn't right.

## See also

- [§26-05 — Toroid Selection](26-05-toroid-selection.md) — picking the right core
- [§26-03 — High-Pass Filters](26-03-high-pass-filters.md) — the dual
- [§26-04 — Bandpass & Notch Filters](26-04-bandpass-notch-filters.md) — selectivity uses
- [§17-05 — Resonant Frequency](../17-formulas/) — design math
- [§25 — Test Equipment](../25-test-equipment/) — NanoVNA usage
