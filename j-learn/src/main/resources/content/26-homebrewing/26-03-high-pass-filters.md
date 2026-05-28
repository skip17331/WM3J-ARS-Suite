---
id: 26-03
title: High-Pass Filters
chapter: 26
section: 03
level: mixed
status: draft
---

# High-Pass Filters

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## When you actually need an HPF

In amateur work, low-pass filters dominate (every transmitter output needs one). High-pass filters are niche but irreplaceable in a few specific situations:

1. **Receiver front-end protection from the AM broadcast band (530–1700 kHz).** If you live within a few miles of a 50 kW AM transmitter, that station's strong field overloads your receiver's RF amp and mixer. An HPF with f_c ≈ 1.8 MHz blocks the BCB without affecting any ham band. This was a standard accessory through the 1980s — Drake TV-300LP and W6NL HPF designs were everywhere.
2. **Blocking mains hum on audio / data lines.** A 1 kHz HPF on an audio input rejects 50/60 Hz hum and its low harmonics while passing voice. Useful for noisy interface cables, especially on data modes (FT8, RTTY) where a clean audio path matters.
3. **TVI / cable-TV interface protection.** A 50 MHz HPF on the antenna lead-in to a cable TV receiver blocks HF amateur signals (especially the strong harmonics from a nearby 10 m PA) before they reach the TV's RF stages. Rare since cable went all-digital, but still useful for OTA TV antennas.
4. **VHF preamp protection from HF garbage.** A VHF (50 or 144 MHz) preamp sitting on a tower exposed to a strong HF signal can desensitize on receive. A 30 MHz HPF on its input blocks HF without affecting VHF.
5. **Crossover networks (audio).** Not RF, but the same math: tweeter networks use HPFs to keep bass out of the tweeter.

The vast majority of ham operations never need a discrete HPF — the receiver's built-in front end handles BCB rejection fine in suburban environments. HPFs become essential when you're within line-of-sight of a strong out-of-band emitter.

## Topology — the dual of an LPF

A Chebyshev or Butterworth HPF is structurally the **dual** of an LPF:

- Every series inductor in the LPF becomes a series **capacitor** in the HPF
- Every shunt capacitor in the LPF becomes a shunt **inductor** in the HPF

```
LPF:               HPF:
  L1   L2            C1   C2
IN ━━━━━━━━ OUT   IN ━┃┃━━━━━┃┃━ OUT
    │   │              │     │
   ═C1 ═C2            ┃L1   ┃L2
    │   │             ┃     ┃
   gnd gnd            gnd   gnd
```

5-element 0.1 dB Chebyshev HPF, 50 Ω, design example with f_c = 1.8 MHz (for BCB rejection):

| Component | Value |
|-----------|-------|
| C1 = C3 (series) | 1500 pF |
| C2 (series, middle) | 1200 pF |
| L1 = L2 (shunt) | 5.6 µH |

At 1 MHz (deep into the AM broadcast band), the attenuation is ~30 dB; at 530 kHz (bottom of BCB) it's ~50 dB. The 80 m band starts at 3.5 MHz, well into the passband, with negligible insertion loss.

## Inductor construction — high-mu cores for low frequencies

The HPF inductors are *shunt* elements, so they need to look high-impedance across the passband while looking low-impedance in the stopband (mains, BCB, etc.). At 530 kHz, 5.6 µH has X_L = 18.6 Ω — easily achieved, but you need a core that's still got high permeability at that frequency *and* low loss in the passband.

| Frequency range | Recommended core | Mix |
|-----------------|------------------|-----|
| Audio HPF (mains hum) | T-200-3 or laminated steel | Mix 3 (powdered iron) |
| 100 kHz – 2 MHz HPF | T-50-3 or T-50-15 | Mix 3 (gray) or Mix 15 (red/white) |
| 2 – 30 MHz HPF | T-50-2 or T-50-6 | Mix 2 (red) or Mix 6 (yellow) |
| 30 – 100 MHz HPF | T-50-10 or air-core | Mix 10 (black) or none |
| > 100 MHz HPF | air-core | — |

For ferrite-based audio chokes (e.g., a 1 kHz HPF for hum rejection in a data interface), use a **ferrite** core (#75 or #77 mix) — not powdered iron — because permeability matters more than Q below ~100 kHz. At low audio frequencies, a few-mH choke might need 100+ turns on a small toroid; manganese-zinc ferrite gets you there in a few turns.

> **Advanced —** The classic "BCB reject HPF" from the 1970s (e.g., the Drake TV-300LP and ARRL Handbook designs) used T-200-2 cores with 30+ turns. With modern NanoVNA verification, you can shrink to T-50-2 cores and 15–20 turns by accepting a steeper cutoff and slightly less attenuation deep in the stopband. The trade is component cost and self-resonance — small cores resonate higher and degrade attenuation above 50 MHz.

## Air-core for HF+ HPFs

Above ~30 MHz, powdered iron starts to lose Q. Above ~100 MHz, even Mix 10 is marginal. For HPFs cutting off in the VHF range (e.g., a 30 MHz HPF for a 2 m preamp), use **air-core inductors** wound on a plastic or ceramic form. The Wheeler formula (see §09-13) computes turns for a given inductance and form diameter.

A 30 MHz HPF for VHF preamp protection might use:

- L: 0.5 µH = 8 turns of #18 enamel on a 0.5" plastic form, close-wound
- C: 220 pF NPO ceramic

Total assembly fits in a tin-plate hobby box (see §26-08) the size of a pack of cards.

## Capacitor types — voltage and tolerance

For HF HPFs, the capacitors are in **series with the signal path**, which means they pass full RF current. At 100 W in 50 Ω, the RMS current is 1.4 A, and the voltage across a 1500 pF series cap at 4 MHz is:

```
V = I × X_C = 1.4 × (1 / (2π × 4×10⁶ × 1500×10⁻¹²)) = 1.4 × 26.5 = 37 V
```

Easy for a silvered mica or NPO ceramic. The cap voltage at lower frequencies (BCB) is higher, but the **current** is much lower because the BCB signal is attenuated upstream. In practice, 500 V NPO ceramics work for any HPF at the 100 W level.

For audio HPFs (data-line hum rejection), use plastic film caps (polypropylene or polyester) — they're stable, low-leakage, and the values are right (0.1 µF to 10 µF range).

## When NOT to use an HPF

- **You're hearing local strong signals from another ham.** That's adjacent-channel desense; an HPF won't help because the offender is *in* your band. Use a bandpass filter instead.
- **You have intermod from VHF/UHF nearby.** The intermod product is *in* your HF band; the source signals get filtered, but the mix product already exists in your front end. Use an attenuator pad first, then track down the front-end-mixer issue.
- **Mains hum on receive audio.** That's audio-path hum, not RF. Fix the audio cable / interface, don't add an RF HPF.

The diagnostic for "do I need an HPF" is: **measure the AM-BCB signal level at your antenna feedpoint with an SDR.** If you see BCB stations at S9+30, you need an HPF. If they're at S6, you don't.

## Construction recipe — 1.8 MHz HPF for BCB rejection

For a 100 W station, 50 Ω in/out, BNC connectors:

1. **Parts:**
   - 2× 1500 pF silvered mica, 500 V (Cornell-Dubilier CD15ED152J03)
   - 1× 1200 pF silvered mica, 500 V
   - 2× T-50-2 toroid (red)
   - 18" of #22 enameled wire
   - Hammond 1590B die-cast box, 2× BNC bulkhead connectors

2. **Wind L1 and L2:** 33 turns of #22 on T-50-2. Calculate: A_L (T-50-2) = 5.0 nH/turn². N = √(5600/5.0) = 33.5 turns. Wind 33, measure on NanoVNA, add 1 turn if low.

3. **Assemble:** Series caps connect input → L1-junction → L2-junction → output. Shunt inductors from each junction to chassis ground. Use the box wall as the ground plane.

4. **Verify on NanoVNA:** Sweep 100 kHz to 30 MHz. Insertion loss in 3.5–30 MHz should be < 0.3 dB; attenuation at 1 MHz should be > 25 dB; at 500 kHz, > 45 dB.

5. **Install:** Between antenna and receiver input. Leave it inline permanently — the QSO traffic on 160 m at 1.8 MHz is *just* inside the passband, so the HPF cuts off cleanly above the BCB.

## See also

- [§26-02 — Low-Pass Filters](26-02-low-pass-filters.md) — the dual; component-value relationships
- [§26-04 — Bandpass & Notch Filters](26-04-bandpass-notch-filters.md) — combining HPF + LPF = BPF
- [§13 — Station Troubleshooting](../13-station-troubleshooting/) — diagnosing BCB and other RX interference
- [§14 — RFI](../14-rfi/) — the broader interference picture
- [§25 — Test Equipment](../25-test-equipment/) — NanoVNA and SDR for HPF verification
