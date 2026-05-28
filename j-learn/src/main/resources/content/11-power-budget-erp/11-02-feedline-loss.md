---
id: 11-02
title: Feedline Loss
chapter: 11
section: 02
level: mixed
status: draft
---

# Feedline Loss

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The feedline component of the power budget is the cable run between transmitter and antenna. It has two contributions: **matched-line loss** (always present, set by cable type and frequency) and **mismatch-induced extra loss** (depends on antenna SWR). Both turn watts into heat in the cable.

This section is the budget-perspective treatment — how to compute the loss and apply it to the budget. The conceptual treatment is in §10 (Feedline & SWR) and the per-cable reference values are in §18-02.

## The two contributions

For a cable run of length L feet at frequency f, with antenna SWR S:

**Total feedline loss (dB) = Matched-line loss + Mismatch loss**

| Term | What it is | Source |
|------|-----------|--------|
| Matched-line loss | Loss when SWR = 1:1; pure conductor + dielectric heating | §18-02 (per-cable table) × L/100 |
| Mismatch loss | Extra loss from reflected wave bouncing through lossy line | §10-03 (table by matched-loss × SWR) |

For typical amateur installs (SWR ≤ 2:1, modern cable, length ≤ 100 ft), mismatch loss is small (~5-15% of matched-line loss). For high-SWR antennas with long lossy cable, mismatch loss can exceed the matched-line loss itself.

## Quick-lookup table for power-budget use

For the most common amateur scenarios, total feedline loss in dB:

| Cable | Length | 14 MHz | 50 MHz | 144 MHz | 432 MHz |
|-------|--------|--------|--------|---------|---------|
| RG-58 | 50 ft | 0.8 | 1.8 | 3.3 | 6.0 |
| RG-58 | 100 ft | 1.6 | 3.5 | 6.6 | 12.0 |
| RG-8X | 50 ft | 0.5 | 1.2 | 2.3 | 4.0 |
| RG-8X | 100 ft | 1.0 | 2.4 | 4.5 | 8.0 |
| RG-213 | 50 ft | 0.33 | 0.75 | 1.4 | 2.5 |
| RG-213 | 100 ft | 0.65 | 1.5 | 2.8 | 5.0 |
| LMR-400 | 50 ft | 0.23 | 0.5 | 0.75 | 1.4 |
| LMR-400 | 100 ft | 0.45 | 1.0 | 1.5 | 2.7 |
| LMR-400 | 150 ft | 0.68 | 1.5 | 2.25 | 4.05 |
| LMR-400 | 200 ft | 0.9 | 2.0 | 3.0 | 5.4 |

(All matched-line loss; assumes SWR 1:1. For higher SWR, add mismatch loss from §10-03's table.)

## Worked examples

### Example 1: Modest HF station

Setup: 100 W HF station, 75 ft RG-213 to a tuned 20 m dipole, SWR 1.5:1.

- Matched-line loss: 0.65 × 0.75 = **0.49 dB**.
- Mismatch loss (from §10-03 lookup, 0.5 dB matched + SWR 1.5:1): **~0.05 dB**.
- Total feedline loss: **0.54 dB** ≈ ~12% of TX power.
- 100 W → **88 W at antenna feedpoint**.

### Example 2: Long VHF run

Setup: 100 W FM station, 150 ft LMR-400 to a 2 m vertical at 144 MHz, SWR 1.3:1.

- Matched-line loss: 1.5 × 1.5 = **2.25 dB**.
- Mismatch loss: ~0.1 dB.
- Total: 2.35 dB ≈ ~42% of TX power lost in cable.
- 100 W → **58 W at antenna**.

### Example 3: Marginal UHF setup

Setup: 50 W FM, 100 ft RG-8X to a 70 cm Yagi at 432 MHz, SWR 2:1.

- Matched-line loss: 8.0 × 1.0 = **8.0 dB**.
- Mismatch loss (8 dB matched + SWR 2:1): ~1.6 dB.
- Total: 9.6 dB.
- 50 W → **5.5 W at antenna**.

The cable just ate 90% of the transmit power. Budget consequence: this is a 5 W station, not a 50 W station, from the antenna's perspective.

### Example 4: High-power HF with quality cable

Setup: 1500 W on 14 MHz, 200 ft LMR-400, dipole at SWR 1.7:1.

- Matched-line loss: 0.45 × 2.0 = **0.9 dB**.
- Mismatch loss (~0.05 dB).
- Total: ~1.0 dB.
- 1500 W → **1200 W at antenna feedpoint**.

300 W heating the cable. At 200 ft, that's 1.5 W/foot — warm but within LMR-400's rating.

### Example 5: Bad cable + bad SWR

Setup: 100 W on 432 MHz, 50 ft RG-58, antenna at SWR 4:1.

- Matched-line loss: 12.0 × 0.5 = **6.0 dB**.
- Mismatch loss: from §10-03 lookup, 6 dB matched + SWR 4:1 → ~3 dB extra.
- Total: 9 dB.
- 100 W → **12.6 W at antenna**.

Most of the TX power is heat in the cable. **This is what bad cabling does to your budget.**

## Translating dB to watts

Quick reference for converting dB loss to power retained:

| Loss (dB) | Power retained | Power lost |
|-----------|----------------|------------|
| 0.5 | 89% | 11% |
| 1 | 79% | 21% |
| 1.5 | 71% | 29% |
| 2 | 63% | 37% |
| 3 | 50% | 50% |
| 4 | 40% | 60% |
| 6 | 25% | 75% |
| 9 | 12.6% | 87.4% |
| 10 | 10% | 90% |
| 12 | 6.3% | 93.7% |

So 3 dB total loss = half the watts; 6 dB = quarter; 10 dB = tenth.

## Choosing cable for your budget

Reverse engineering: if you want to deliver a specific power to the antenna, work backward.

**Goal**: 95% efficiency at 14 MHz with 100 ft cable.
- Need <0.22 dB total loss.
- LMR-400 at 14 MHz: 0.45 dB/100 ft = 0.45 dB total. Doesn't meet 95%.
- LMR-600: 0.27 dB/100 ft = 0.27 dB. Still doesn't meet.
- LDF4-50A hardline: 0.18 dB/100 ft = 0.18 dB. **Meets 95%.**

So if 95% efficiency really matters at 14 MHz over 100 ft, you need hardline. For most amateur use, 90% (= 0.46 dB loss) is acceptable; LMR-400 hits this just barely.

For VHF/UHF, similar exercise — but the matched losses are higher, so meeting an efficiency target requires more aggressive cable choices or shorter runs.

## Adapting for non-standard scenarios

**Variable-length feedline runs** (mobile install with retractable cable, portable with deployed cable):
- Compute loss for the *actual deployed length* on each operating session.

**Non-standard SWR** (running an antenna outside its design band with a tuner):
- The tuner presents 1:1 to the rig, but SWR on the feedline (between tuner and antenna) is *unchanged from raw*.
- For budget purposes: use raw antenna SWR, not "1:1 at the rig."

**Multi-antenna installations** (switchable antenna systems):
- Compute budget separately for each antenna; the budget changes as you switch.

**Stacked antennas** (two Yagis fed from one feed harness):
- Add the harness's loss to the feedline loss budget.
- The shared feedpoint sees the antenna pair as the combined antenna; gain is higher but feed efficiency is set by the harness.

## Common feedline-budget mistakes

- **Treating "low-loss" as "lossless".** LMR-400 at 432 MHz still has 2.7 dB/100 ft. Compute it, don't assume zero.
- **Ignoring mismatch loss for "good enough" SWR.** Even SWR 2:1 contributes ~5-15% extra loss. Add it.
- **Using one cable type's loss for a different cable.** "It's about the same as RG-213" — sometimes yes, sometimes off by 30%. Use the actual cable's number.
- **Not measuring total loss after install.** A new install should have loss within 10% of computed; if larger, you have a connector problem or cable fault.
- **Forgetting connector losses.** Each connector adds ~0.05–0.1 dB; an outdoor run with 4 connectors adds ~0.3 dB to the budget. For long runs at low loss, this matters.

## See also

- §11-00 — Chapter overview
- §11-01 — TX power
- §11-04 — ERP / EIRP output (where the feedline loss feeds into the radiated power)
- §18-02 — Coax loss tables (full reference)
- §10-01 — Coax loss by frequency (the conceptual treatment)
- §10-03 — Mismatch loss (the SWR penalty)
- §10-04 — Power delivered vs. lost
