---
id: 26-06
title: Ferrite Mix Selection
chapter: 26
section: 06
level: mixed
status: published
---

# Ferrite Mix Selection

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## Ferrite is for **broadband** — that's the point

Powdered iron (§26-05) is **narrowband-tuned**: high Q, low loss, good in resonant circuits at one frequency. Ferrite is the opposite: **low Q, controlled loss**, broadband, designed to either *absorb* RF (chokes) or *transform* impedance over a wide range.

The two materials are **not interchangeable**. A ferrite mix's job is to look like a high impedance — partly inductive, partly resistive — over a *decade or more* of frequency. The lossy resistive component is a feature, not a bug: it's what converts common-mode RF current into heat instead of letting it propagate.

## Fair-Rite mix codes

The dominant ferrite manufacturer for amateur use is Fair-Rite. Each mix is a different ceramic formulation (manganese-zinc or nickel-zinc) with distinct frequency behavior:

| Mix | Material | µ_init | Frequency range (best Z) | Typical ham use |
|-----|----------|--------|--------------------------|------------------|
| **#31** | NiZn | 1500 | **1–50 MHz peak** | **Low-band HF chokes (160/80 m)** |
| #33 | MnZn | 800 | 0.1–1 MHz | Switching-supply chokes |
| **#43** | NiZn | 800 | **1–250 MHz** | **All-HF chokes; the workhorse** |
| #44 | NiZn | 800 | 1–30 MHz | Slightly higher µ vs 43; uncommon |
| **#52** | NiZn | 250 | **30–200 MHz** | **VHF transformers** |
| **#61** | NiZn | 125 | **50 MHz – 1 GHz** | **VHF/UHF chokes & transformers** |
| #67 | NiZn | 40 | 50 MHz – 5 GHz | UHF/microwave (low µ, high Q) |
| #73 | MnZn | 2500 | 100 kHz – 50 MHz | LF / lower HF emphasis |
| **#75** | MnZn | 5000 | **100 kHz – 10 MHz** | **LF chokes, AM band rejection** |
| **#77** | MnZn | 2000 | **100 kHz – 10 MHz** | **Similar to #75, more available** |
| #78 | MnZn | 2300 | 100 kHz – 8 MHz | Low-loss MnZn |

The two manganese-zinc (MnZn) families — #73, #75, #77, #78 — are for LF and audio. The nickel-zinc (NiZn) families — #31, #43, #52, #61, #67 — are for HF and up. The crossover is around 1 MHz; below it, MnZn has higher µ; above it, NiZn handles the frequency without losing all permeability to eddy currents.

## The decisive plot — impedance vs frequency

Fair-Rite's datasheets include a plot of **impedance vs frequency** for a "single-turn" loop through the bead. The plot has three regions:

1. **Inductive region** (low f): Z rises with frequency, mostly reactive (X_L). The core looks like a normal inductor.
2. **Transition region** (mid f): The reactive part flattens; the resistive (loss) component rises. Total |Z| keeps rising but the phase angle drops from 90° toward 0°.
3. **Resistive region** (high f): Mostly real loss; |Z| reaches a broad peak then slowly falls. This is where the core *absorbs* RF as heat — ideal for choking common-mode current.

For a common-mode choke, you want the offending frequency to fall in the **resistive region** of the mix. That's why #43 is the all-HF favorite: its resistive region spans 1–100 MHz, covering all HF ham bands plus 6 m and 2 m.

## Mix-by-band cheat sheet

| Band / use case | Recommended mix | Why |
|-----------------|-----------------|-----|
| 160 m common-mode choke | **#31** | µ stays high at 1.8 MHz; #43 is marginal |
| 80 m common-mode choke | **#31** preferred, **#43** OK | #31 has more loss at 3.5 MHz |
| 40 m – 10 m common-mode choke | **#43** | The universal HF choke material |
| 6 m common-mode choke | **#43** or **#52** | Both work; #43 simpler stock |
| 2 m common-mode choke | **#52** or **#61** | NiZn high-frequency family |
| 70 cm common-mode choke | **#61** | Or higher-µ variants |
| HF balun transformer | **#43** | Standard for 1:1, 4:1, 9:1 chokes |
| HF unun (EFHW 49:1) | **#43** | FT-240-43 the canonical core |
| VHF balun transformer | **#61** | 100–500 MHz transformers |
| Audio / mains hum filter | **#75** or **#77** | High-µ MnZn for audio |
| LF / AM-band RFI suppression | **#75** | High µ at 1 MHz |
| Switching supply input filter | **#31** or **#43** | Suppresses MHz-range supply hash |

## Reading the datasheet — a worked example

The Fair-Rite #43 datasheet for an FT240-43 toroid gives:

| f (MHz) | |Z| per turn | X | R |
|---------|-------------|---|---|
| 1 | 25 Ω | 20 | 15 |
| 10 | 200 Ω | 100 | 175 |
| 30 | 320 Ω | 50 | 315 |
| 100 | 280 Ω | -20 | 280 |
| 300 | 130 Ω | -80 | 100 |

So at 14 MHz, one wrap through an FT240-43 gives roughly 250 Ω of impedance. Ten wraps in series (the typical 1:1 current choke design) give roughly **2500 Ω** — comfortably above the "good" threshold (§18-05). The actual relationship between turn count and choking impedance isn't quite N (it's modified by self-capacitance), but for first-pass design, **multiply single-turn |Z| by N**.

For #31 mix on an FT240-31 at 1.8 MHz: single-turn |Z| ≈ 30 Ω. Ten turns → 300 Ω; not enough. **Use a stacked pair of FT240-31 with 14 turns each** to reach 1500 Ω+ on 160 m.

## Why #43 is the amateur default

Three reasons:

1. **Frequency span.** Useful from 1 to 250 MHz with one part number. No need to stock multiple mixes for different bands.
2. **Power handling.** FT240-43 handles ~500 W SSB / 250 W digital with no thermal issue. FT290-43 or stacked FT240-43 handles legal limit.
3. **Availability and cost.** Fair-Rite and clones flood the market. An FT240-43 is $7–10 from Mouser; bulk-pack pricing drops to $5.

The downside of #43 below 3 MHz: marginal common-mode rejection. For a dedicated 160 m station, switch to #31. For a multi-band 80–10 m station, stick with #43.

> **Advanced —** A **two-mix choke** stacks one FT240-31 (for low bands) with one FT240-43 (for high bands) on the same coax wind. The result is good choking from 1.8 to 50 MHz, where neither single mix excels alone. The trade is twice the cost and twice the length. K9YC and W1HIS have published detailed measurements; this approach is overkill for casual use but common in serious DX and contest stations where common-mode current must be killed across all bands without swapping chokes per band.

## Power and saturation

Ferrite saturates at a flux density typically around 2000–3000 gauss (varies by mix and temperature). Above saturation, µ collapses, the choke becomes invisible to RF, and the device behind it sees full common-mode current. This is bad on transmit — a saturated balun on a 1500 W key-down will instantly stop working and the rig's SWR may spike.

The flux-density check uses the same formula as powdered iron (§26-05). For a 1:1 current choke at 1500 W in 50 Ω on FT240-43 with 10 turns at 3.5 MHz:

```
E_peak = √(2 × 1500 × 50) = 387 V
A_e (FT240-43) = 1.58 cm²
B = 387 × 10⁸ / (4.44 × 1.58 × 10 × 3.5e6) = 158 gauss
```

158 gauss is well under saturation. The choke is fine on transmit; concern is heating, not saturation.

For **digital modes** (100 % duty cycle), the average power equals peak power. Halve the rated SSB capacity of any ferrite choke when calculating FT8/RTTY use.

## Buying — what's on the shelf

| Source | Strengths | Notes |
|--------|-----------|-------|
| **Mouser** | Genuine Fair-Rite, full mix range | Most reliable; bulk pricing good |
| **Kits And Parts (KitsAndParts.com)** | Amateur-focused; good values | Old amateur favorite |
| **Palomar Engineers** | Pre-built chokes too | Solid for not-DIY |
| **DX Engineering** | Pre-built and bulk cores | Higher prices, but reliable |
| **eBay** | Cheap surplus | **Verify mix** — counterfeit/mislabeled common |

A genuine FT240-43 has a slight matte gray finish; counterfeits are sometimes glossier or have visible mold seams. Test on the NanoVNA: a real FT240-43 with one turn should show ~250 Ω at 14 MHz. If yours shows 30 Ω, you have either Mix 75 (LF) or a fake.

## See also

- [§26-05 — Toroid Selection (Powdered Iron)](26-05-toroid-selection.md) — the resonant/tuned counterpart
- [§18-05 — Baluns & Chokes](../18-coax-connectors/18-05-baluns-chokes.md) — full balun spec reference
- [§14 — RFI](../14-rfi/) — why common-mode rejection matters
- [§26-07 — Linear vs Switching Power Supplies](26-07-linear-vs-switching-supplies.md) — ferrite chokes on supply leads
- [§17 — Formulas](../17-formulas/) — flux density / impedance math
