---
id: 17-14
title: RF Exposure
chapter: 17
section: 14
level: mixed
status: draft
---

# RF Exposure

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

This card walks through the **RF exposure** calculation end-to-end. It pulls together more variables (power, frequency, duty cycle, gain, distance) than any other formula in this chapter — it's the canonical use of the formula-calculator pattern.

The regulatory side (FCC §1.1310 limits, controlled vs. uncontrolled environments, evaluation requirements) lives in **§06 RF Safety**. This section is the math.

## What's being calculated

The FCC requires every amateur station to evaluate whether the RF field at occupied locations exceeds the **Maximum Permissible Exposure (MPE)** for that frequency. The chain of math is:

```
Average power at antenna  →  EIRP in main lobe  →  Power density at distance d  →  Compare to MPE
```

## Inputs

| Input | Symbol | Typical range | Units | Comes from |
|-------|--------|---------------|-------|-----------|
| Transmitter PEP | P_PEP | 5 W (HT) … 1500 W (legal limit) | W | Rig spec |
| Average power factor (mode) | k_avg | 0.20 (SSB) … 1.0 (FT8/FM) | dimensionless | Mode-specific |
| Operating duty cycle | D | 0.0 … 1.0 | dimensionless | TX/RX time ratio |
| Feedline loss | L | 0.5 dB … 10 dB | dB | §17-09 |
| Antenna gain | G | 0 dBd (dipole) … 15 dBd (Yagi) | dBd or dBi | Antenna spec |
| Distance to evaluation point | d | 0.3 m (HT touching head) … 30 m | m | Site survey |
| Frequency | f | 1.8 MHz … 1.3 GHz | MHz | Operating freq |

## Step 1 — Average power into the feedline

For RF exposure, we use *average* power (not PEP), because the body integrates exposure over the **30-minute averaging window** for controlled environments and **6-minute window** for uncontrolled.

```
P_avg = P_PEP × k_avg × D
```

| Mode | k_avg | Notes |
|------|------|-------|
| SSB voice | 0.20 | Average voice waveform |
| AM voice | 0.50 | Carrier present continuously |
| FM voice | 1.0 | Constant envelope |
| CW (manual) | 0.40 | Average dits-and-dahs vs. carrier |
| FT8 / FT4 | 1.0 | Constant envelope, full duty within slot |
| RTTY | 1.0 | FSK, constant envelope |
| Digital (PSK31) | 0.5 | Compact tone clusters |

D is your transmit duty cycle: if you transmit 1 minute out of 6, D = 0.17. If you're rag-chewing, expect D ~ 0.5. Contest transmit-receive ratio is closer to D = 0.7 for the operator at the key.

> ⚙️ **Advanced —** The 30 / 6 minute averaging windows come from FCC §1.1310. Controlled environment means you (the operator) and your family at the station; uncontrolled means anyone walking by — neighbors, kids, the public. Use the 6-min window when computing exposure to the public.

## Step 2 — Power at the antenna (after feedline loss)

```
P_ant = P_avg × 10^(−L_dB / 10)
```

(Linear feedline-loss factor; see §17-09 for the conversion.)

## Step 3 — EIRP in the main lobe

```
EIRP = P_ant × 10^(G_dBi / 10)
G_dBi = G_dBd + 2.15
```

(See §17-08 for the dBi/dBd conversion.)

## Step 4 — Power density at distance d

For the far-field (which is *generally* d > 2λ for HF, d > 10λ for VHF/UHF), the power density at distance d in free space is:

```
S = EIRP / (4π · d²)
S = P_avg × G_lin × L_lin / (4π · d²)
```

Where S is in watts per square meter, EIRP in watts, d in meters.

For ham work, especially indoors / near the antenna, you also add a **ground-reflection enhancement factor** of up to 4× (i.e., 6 dB) to be conservative. The FCC's MPE evaluation worksheet uses this. So the conservative version:

```
S_worst = 4 × EIRP / (4π · d²) = EIRP / (π · d²)
```

## Step 5 — Compare to MPE

The MPE limit S_MPE depends on frequency (the body is more sensitive in some bands). From FCC §1.1310:

| Frequency | S_MPE (W/m²), uncontrolled |
|-----------|---------------------------:|
| 0.3–3 MHz | 100 / f² (where f in MHz; capped at 100) |
| 3–30 MHz | 180 / f² |
| 30–300 MHz | 0.2 |
| 300 MHz – 1.5 GHz | 0.0067 × f (where f in MHz) |
| 1.5 GHz – 100 GHz | 1.0 |

| Frequency | S_MPE (W/m²), controlled |
|-----------|---------------------------:|
| 0.3–3 MHz | 100 (constant, no derate) |
| 3–30 MHz | 900 / f² |
| 30–300 MHz | 1.0 |
| 300 MHz – 1.5 GHz | 0.0335 × f |
| 1.5 GHz – 100 GHz | 5.0 |

The "S_MPE" value is in watts per square meter — and yes, the formula switches form across frequency ranges because the body's RF absorption profile isn't flat.

If S_worst ≤ S_MPE, you're compliant. If not, you must either reduce power, increase distance, reduce duty cycle, or change antenna placement (§06).

## Worked example — 100 W FT8 on a 6 dBd Yagi at 14.150 MHz, 5 m to neighbor's window

Inputs:

```
P_PEP = 100 W
k_avg = 1.0           (FT8 is constant envelope)
D = 0.5               (15-second slot, 30 sec total cycle = 50% duty)
L = 1 dB              (LMR-400, 100 ft, 14 MHz — see §17-09)
G = 6 dBd → 8.15 dBi
d = 5 m               (neighbor's bedroom window)
f = 14.150 MHz        (uncontrolled)
```

Step 1: Average power into feedline:

```
P_avg = 100 × 1.0 × 0.5 = 50 W
```

Step 2: Power at antenna:

```
P_ant = 50 × 10^(−1/10) = 50 × 0.794 ≈ 39.7 W
```

Step 3: EIRP:

```
EIRP = 39.7 × 10^(8.15/10) = 39.7 × 6.53 ≈ 259 W
```

Step 4: Power density (worst case with ground reflection):

```
S_worst = EIRP / (π · d²)
        = 259 / (π · 5²)
        = 259 / 78.54
        ≈ 3.30 W/m²
```

Step 5: MPE for uncontrolled at 14.150 MHz (3–30 MHz range):

```
S_MPE = 180 / f² = 180 / 14.15² = 180 / 200.2 ≈ 0.90 W/m²
```

**S_worst (3.30) > S_MPE (0.90)** — this scenario fails. You would need to either:

- Reduce power (use 25 W instead of 100 → S_worst = 0.83, just under)
- Increase distance (move the Yagi farther away or aim it differently)
- Reduce duty cycle (lower D to 0.13 → S_worst ≈ 0.86)
- Use the controlled MPE for the operator (S_MPE_controlled = 4.50 → passes for the operator, but neighbors are uncontrolled)

## Worked example — 5 W HT held to head at 446.000 MHz

Inputs:

```
P_PEP = 5 W
k_avg = 1.0           (FM)
D = 0.3               (30% TX during active QSO)
L = 0 dB              (HT direct to antenna)
G = 0 dBd → 2.15 dBi
d = 0.05 m            (5 cm from head)
f = 446.000 MHz       (uncontrolled if at a hamfest, etc.)
```

```
P_avg = 5 × 1.0 × 0.3 = 1.5 W
EIRP = 1.5 × 10^(2.15/10) ≈ 1.5 × 1.64 ≈ 2.46 W
S_worst = 2.46 / (π · 0.05²) ≈ 2.46 / 0.00785 ≈ 313 W/m²

S_MPE (300 MHz–1.5 GHz uncontrolled) = 0.0067 × 446 ≈ 2.99 W/m²
```

**S_worst (313) >> S_MPE (3.0)** — way over.

This is why HT manuals say "do not transmit with the antenna touching your face" — at 5 W on UHF, the field 5 cm from the antenna exceeds MPE by 100×. In practice the body isn't a flat absorber and the field falls off faster than 1/d² in the *near field* (within ~2λ ≈ 1.3 m for 446 MHz), so actual exposure is lower than the simple formula predicts. But the formula is conservative, which is what the FCC wants.

The practical takeaway: **hold the HT a few inches off your head, not pressed against it**. Above 30 cm distance, even worst-case math passes for typical HT scenarios.

## Common mistakes

- **Using PEP instead of average power.** PEP is the peak; MPE is averaged over 6 / 30 minutes. SSB at 100 W PEP averages closer to 20 W.
- **Forgetting the ground-reflection enhancement.** The 4× factor is the FCC's recommended worst case for ground-mounted or near-ground antennas. Skip it only with strong justification.
- **Comparing average to peak MPE.** MPE is averaged; compare averaged S_worst to S_MPE. Don't compare PEP S to averaged MPE.
- **Forgetting to convert dBd → dBi.** The MPE formulas use EIRP (referenced to isotropic), not ERP (referenced to dipole). Always convert antenna gain to dBi before plugging in.
- **Mixing controlled and uncontrolled tables.** A neighbor's window is uncontrolled. The shack itself (with you and your family present) is controlled. Use the right column.

> ⚙️ **Advanced —** This walk-through assumes the far-field 1/d² law. In the *near field* (within ~2λ for HF, much less for VHF/UHF), the field is dominated by the antenna's reactive near-field structure and falls off faster than 1/d². For ham work near 80m antennas (λ = 80 m → near-field extends to ~160 m!), this means the simple formula *over-estimates* exposure within tens of meters of the antenna, giving conservative compliance numbers. The FCC accepts the simple formula as adequate for most ham installations.

## See also

- §06 — RF Safety (the regulatory side; the *why*)
- §06-02 — MPE Limits (the table this section uses)
- §06-04 — Duty Cycle (how to estimate D for your operating style)
- §06-06 — Safe Antenna Placement (how to fix a failing scenario)
- §09 — Power Budget & ERP (related calculation chain)
- §17-08 — ERP / EIRP (the formula card this builds on)
- §17-09 — Feedline Loss (where L comes from)
- §17-10 — Decibels (the dB ↔ linear conversions used throughout)
