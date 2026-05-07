---
id: 18-09
title: Feedline Loss
chapter: 18
section: 09
level: mixed
status: draft
---

# Feedline Loss

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Coaxial cable attenuates the signal as it travels. The loss is specified per unit length and per frequency in the cable's datasheet, then scaled to the actual run length. Loss when the line is matched (1:1 SWR) is the **matched loss**; loss when the line is mismatched is higher because reflections traverse the cable more than once.

## Equations

Matched loss (dB) for a given length and frequency:

```
L_matched(dB) = ℓ_per_100ft(dB) × length(ft) / 100
```

Or in metric:

```
L_matched(dB) = ℓ_per_100m(dB) × length(m) / 100
```

Total loss with mismatch (the line + the SWR penalty):

```
L_total(dB) = L_matched + L_SWR_penalty
```

The SWR penalty depends on both matched loss and SWR; see §08-03 (mismatch loss) for the closed-form.

## Variables

| Symbol | Quantity | Units |
|--------|----------|-------|
| L_matched | Matched (1:1 SWR) loss | dB |
| ℓ_per_100ft | Cable's per-100-ft loss spec | dB / 100 ft (frequency-dependent) |
| L_SWR_penalty | Additional loss from reflections | dB |
| length | Cable run length | ft or m |

## Cable loss reference (matched, dB per 100 ft)

Approximate values from manufacturer datasheets at common ham frequencies:

| Cable | 1.8 MHz | 7 MHz | 14 MHz | 28 MHz | 50 MHz | 144 MHz | 432 MHz | 1.3 GHz |
|-------|--------:|------:|-------:|-------:|-------:|--------:|--------:|--------:|
| RG-58 (foam) | 0.4 | 0.9 | 1.3 | 1.9 | 2.5 | 4.6 | 8.4 | 16.5 |
| RG-58A (PVC) | 0.5 | 1.1 | 1.6 | 2.4 | 3.3 | 6.0 | 11.4 | 22.0 |
| RG-8X | 0.3 | 0.6 | 0.9 | 1.3 | 1.7 | 3.1 | 5.7 | 11.0 |
| RG-213 (foam) | 0.2 | 0.4 | 0.6 | 0.8 | 1.1 | 1.9 | 3.4 | 6.8 |
| LMR-400 | 0.1 | 0.3 | 0.4 | 0.6 | 0.8 | 1.4 | 2.6 | 4.8 |
| LMR-600 | 0.1 | 0.2 | 0.3 | 0.4 | 0.5 | 0.9 | 1.7 | 3.2 |
| 7/8" Heliax (LDF5-50A) | 0.05 | 0.1 | 0.15 | 0.2 | 0.3 | 0.5 | 1.0 | 1.9 |

(Approximate; consult Belden/Times Microwave/Andrew datasheets for the exact figure for your cable.)

## Worked example — 100 ft of LMR-400 at 14 MHz

```
ℓ = 0.4 dB / 100 ft (at 14 MHz)
length = 100 ft
L_matched = 0.4 × 100 / 100 = 0.4 dB
```

Convert to power factor: 10^(−0.4/10) = 0.912 → **91% of TX power reaches the antenna**.

For a 100 W rig, 91 W is delivered. 9 W is lost as cable heat (and pattern distortion).

## Worked example — 100 ft of RG-58 at 144 MHz

```
ℓ = 4.6 dB / 100 ft
length = 100 ft  
L_matched = 4.6 dB
```

Convert: 10^(−4.6/10) = 0.347 → **35% reaches the antenna**.

A 50 W mobile radio with 100 ft of RG-58 to a 2m antenna delivers only **17 W** to the antenna. This is why VHF/UHF runs use LMR-400, RG-213, or hardline.

## Worked example — RG-8X at 14 MHz, 50 ft

```
ℓ = 0.9 dB / 100 ft
length = 50 ft
L_matched = 0.9 × 50 / 100 = 0.45 dB
```

Power factor: 10^(−0.45/10) = 0.901 → **90% delivered**.

For 100 W in: 90 W out at the antenna. Effectively the same as LMR-400 for this short run on HF. RG-8X is fine for 50 ft of HF; LMR-400 only really matters on long runs or VHF/UHF.

## Approximate scaling rules

- **Loss roughly doubles when frequency quadruples.** A cable with 1 dB / 100 ft at 14 MHz is about 2 dB / 100 ft at 50 MHz, 4 dB / 100 ft at 144 MHz.
- **Loss scales linearly with length.** 50 ft has half the loss of 100 ft. 200 ft has double.
- **Larger cable diameter → less loss.** Going from RG-58 to RG-213 to LMR-400 to LDF5-50A roughly halves loss each step.

## Common mistakes

- **Using the wrong cable's spec.** "RG-8" without further qualification could mean 1980s lossy stuff or modern foam-core. Read the actual cable's printed legend or measure with a NanoVNA.
- **Ignoring frequency.** A cable with 0.4 dB / 100 ft on 80m has 4× that loss on 6m. Always look up the loss at the operating frequency.
- **Treating SWR as a separate loss to add.** Matched loss and SWR-mismatch loss interact: a high-loss cable hides SWR (because the reflected wave is attenuated). See §08-03.
- **Counting only one-way length.** Loss is *one-way attenuation* per the cable's spec. Don't multiply by 2 — you're already measuring loss from rig to antenna.

> ⚙️ **Advanced —** Loss in coax has two components: I²R loss in the conductor (proportional to √f) and dielectric loss in the insulator (proportional to f). At HF the conductor loss dominates; at UHF and microwave the dielectric loss dominates. This is why dielectric-quality matters more at higher frequencies (Teflon vs. polyethylene vs. foam vs. air-spaced).

## See also

- §19 — Coax & Connectors (full chapter on cable selection)
- §19-02 — Loss tables (more cable types)
- §08-03 — Mismatch loss (the SWR penalty)
- §09-02 — Feedline loss in the budget chain
- §18-10 — Decibels (the dB ↔ power-factor conversion)
