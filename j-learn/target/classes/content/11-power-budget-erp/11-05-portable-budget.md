---
id: 11-05
title: Portable Budget
chapter: 11
section: 05
level: simple
status: draft
---

# Portable Budget

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

This section walks through complete power budgets for typical portable scenarios — SOTA (Summits On The Air), POTA (Parks On The Air), Field Day, mobile, and battery-powered base operations. Each example shows where the watts go and what the resulting EIRP is, so you can compare configurations before you pack the truck.

For the underlying math, see §11-01 through §11-04. For propagation predictions of what your portable EIRP will achieve, see §01-07.

## Common portable scenarios

| Scenario | Typical TX | Typical antenna | Typical feedline |
|----------|------------|-----------------|-------------------|
| QRP HF SOTA | 5 W | EFHW or wire dipole | 25 ft RG-174 or RG-58 |
| 100 W HF POTA | 100 W (HF mobile rig) | EFHW or vertical | 50 ft RG-58 or RG-8X |
| Field Day club | 100 W | Dipole or G5RV | 100 ft RG-8X |
| 2 m SOTA | 5 W (HT) | Hand-held J-pole or Yagi | 6 ft RG-58 or none |
| 2 m portable repeater | 25 W | 1/4-wave whip or vertical | 30 ft RG-8X |
| Solar-powered base | 100 W | Wire dipole | 75 ft RG-213 |

## Scenario A: 5 W QRP SOTA, 20 m EFHW

Setup:
- Rig: Elecraft KX2 or similar 5 W rig.
- Antenna: 65-ft EFHW for 20-10 m, with 49:1 unun.
- Feedline: 25 ft RG-174 (small/light SOTA cable).
- Frequency: 14 MHz.
- Antenna SWR at 14.060 MHz: 1.5:1 (typical EFHW on the design band).

Budget:

- P_tx: 5 W = 6.99 dBW.
- Matched-line loss (RG-174 at 14 MHz ≈ 2.4 dB/100 ft): 2.4 × 25/100 = 0.6 dB.
- Mismatch loss (~0.05 dB).
- Total feedline: 0.65 dB.
- Antenna gain (EFHW at moderate height, real ground): 4 dBi (peak broadside; some height gain).
- EIRP = 6.99 − 0.65 + 4 = 10.34 dBW = **10.8 W EIRP**.

So a 5 W QRP setup yields ~11 W EIRP — modest but workable for HF DX with good propagation.

For RX: a 5 W signal at the far end's receiver. With path loss to 5000 km at 14 MHz of ~129 dB, far-station receives at:
EIRP_dBm − path_loss + far_antenna_gain = 10.34 + 30 (dBW to dBm conversion) − 129 + 6 = −82.7 dBm = S6.

Marginal but workable. QRP works on 20 m.

## Scenario B: 100 W HF POTA, 30 ft vertical

Setup:
- Rig: Yaesu FT-991A or similar 100 W rig.
- Antenna: 25-ft adjustable vertical, with elevated radials.
- Feedline: 50 ft RG-8X.
- Frequency: 14.250 MHz (SSB).
- Antenna SWR: 1.7:1 (mid-band, untuned).

Budget:

- P_tx: 100 W = 20 dBW.
- Matched-line loss (RG-8X at 14 MHz, 1.0 dB/100 ft): 1.0 × 50/100 = 0.5 dB.
- Mismatch loss (~0.05 dB).
- Total feedline: 0.55 dB.
- Antenna gain (vertical with good radials, peak elevation 20°): 1.5 dBi at the takeoff angle.
- EIRP = 20 − 0.55 + 1.5 = 20.95 dBW = **125 W EIRP** at 20° elevation.

A vertical-fed POTA station has 125 W EIRP at the right takeoff angle. That's actually less than the 100 W TX power, because the vertical's gain at any one direction is similar to or below that of an isotropic source. The vertical's advantage is omnidirectional coverage and low takeoff angle — not gain.

## Scenario C: Field Day club station, 100 W, dipole, 100 ft cable

Setup:
- Rig: Icom IC-7610 at 100 W.
- Antenna: 80 m G5RV at 35 ft on 20 m.
- Feedline: 100 ft RG-8X at 14 MHz.
- Antenna SWR: 1.4:1 (G5RV resonates well on 20 m).

Budget:

- P_tx: 100 W = 20 dBW.
- Matched-line loss (RG-8X at 14 MHz, 1.0 dB/100 ft): 1.0 × 100/100 = 1.0 dB.
- Mismatch loss (1 dB matched, SWR 1.4:1): ~0.04 dB.
- Total feedline: 1.04 dB.
- Antenna gain (dipole at 0.5 λ above ground): 6 dBi peak broadside.
- EIRP = 20 − 1.04 + 6 = 24.96 dBW = **313 W EIRP**.

Solid Field Day station for 100 W. EIRP 313 W in the broadside direction.

## Scenario D: 2 m SOTA with HT and Arrow Yagi

Setup:
- Rig: Yaesu FT-3DR HT, 5 W.
- Antenna: Hand-held Arrow II Yagi, 3-element on 2 m.
- Feedline: 6 ft RG-58 (between HT and Arrow).
- Frequency: 144.300 MHz (SSB calling).
- Antenna SWR: 1.3:1.

Budget:

- P_tx: 5 W = 6.99 dBW.
- Matched-line loss (RG-58 at 144 MHz, 6.6 dB/100 ft): 6.6 × 0.06 = 0.4 dB.
- Mismatch loss (~0.05 dB).
- Total feedline: 0.45 dB.
- Antenna gain (Arrow II 3-element 2 m Yagi): 7 dBd = 9.15 dBi.
- EIRP = 6.99 − 0.45 + 9.15 = 15.69 dBW = **37 W EIRP**.

A 5 W HT into an Arrow Yagi has EIRP 37 W — about 7× the bare HT power. This is what makes SOTA on 2 m feasible.

## Scenario E: 25 W portable repeater (mountaintop install)

Setup:
- Rig: Yaesu FTM-7250 mobile rig at 25 W.
- Antenna: 1/4-wave whip on a 6-foot mast.
- Feedline: 30 ft LMR-400.
- Frequency: 146.940 (typical mountaintop repeater).
- Antenna SWR: 1.5:1.

Budget:

- P_tx: 25 W = 13.98 dBW.
- Matched-line loss (LMR-400 at 144 MHz, 1.5 dB/100 ft): 1.5 × 0.3 = 0.45 dB.
- Mismatch loss: ~0.05 dB.
- Total feedline: 0.50 dB.
- Antenna gain: 2 dBi (1/4-wave whip with mountain ground).
- EIRP = 13.98 − 0.50 + 2 = 15.48 dBW = **35 W EIRP**.

35 W EIRP on a mountaintop is enough to cover 30+ miles. Modest but functional for emcomm/portable use.

## Scenario F: Solar-powered home base

Setup:
- Rig: Icom IC-7300 at 100 W.
- Antenna: 80 m fan dipole at 35 ft.
- Feedline: 75 ft RG-213.
- Frequency: 14.220 (SSB phone).
- Power: solar panel + LiFePO₄ battery; PSU draws 22 A at 13.5 V (= 297 W DC) for full output.
- Antenna SWR: 1.6:1 on 20 m.

Budget (RF):

- P_tx: 100 W = 20 dBW.
- Matched-line loss (RG-213 at 14 MHz, 0.65 dB/100 ft): 0.65 × 0.75 = 0.49 dB.
- Mismatch loss: ~0.05 dB.
- Total feedline: 0.54 dB.
- Antenna gain: 6 dBi.
- EIRP = 20 − 0.54 + 6 = 25.46 dBW = **352 W EIRP**.

Budget (DC):

- Battery: 100 Ah LiFePO₄ = 1280 Wh nominal (12.8 V × 100 Ah).
- TX duty: 30% (typical SSB) at 297 W = 89 W average.
- RX power: ~5 W.
- Hourly draw: 89 + 5 = 94 W = 7.3 Ah.
- Battery runtime at 70% DoD: 70 / 7.3 = ~10 hours of operation.
- Recharge: 200 W solar panel → 16 A peak charging → 6 hours of charging in good sun returns the battery.

So a 100 Ah LiFePO₄ + 200 W solar can sustain a 100 W station for ~10 hours of mixed RX/TX before recharge needed. Field-deployable for a multi-day operation if you can get sun daily.

## Scaling for ERP

Compare the EIRP across scenarios:

| Scenario | Typical EIRP |
|----------|--------------|
| QRP SOTA (5 W EFHW) | 11 W |
| 100 W HF POTA (vertical) | 125 W |
| Field Day club (100 W dipole) | 313 W |
| 2 m SOTA (5 W Yagi) | 37 W |
| 25 W mountain repeater | 35 W |
| 100 W base + dipole | 350 W |

Higher EIRP gets through worse propagation, denser pile-ups, longer paths. An EFHW at 5 W and a dipole at 100 W have similar EIRPs to about a 30-100 W spread — not a dramatic difference for typical 20 m DX.

## When the budget breaks

Three failure modes that can blow up a portable budget:

1. **Long feedline at high frequency**: 100 ft RG-58 at 144 MHz = 6.5 dB matched loss, eats two-thirds of the power.
2. **Bad antenna SWR + lossy cable**: SWR 5:1 with 5 dB matched loss = ~10 dB total loss.
3. **Battery runs flat mid-operation**: budget the DC, not just the RF.

For emcomm/Field Day, plan all three margins.

## Common portable mistakes

- **Bringing inadequate cable.** RG-58 for HF portable is OK; RG-58 for VHF/UHF mountain repeater is questionable.
- **Forgetting to verify SWR after deployment.** New geometry, new ground = different SWR. Verify with analyzer at the site.
- **Underestimating battery draw.** A 100 W station continuous-TX is 22 A; a 100 Ah battery lasts 4-5 hours, not 10 hours.
- **Wrong antenna for the operating goal.** A vertical for NVIS is a poor choice; a low dipole for DX is a poor choice. Match the antenna to the propagation.
- **No backup.** SOTA: a stuck antenna, a tangled wire, a dead battery — bring spare gear.

## See also

- §11-00 — Chapter overview
- §11-01 through §11-04 — The components of this budget
- §05-04 — EFHW (the portable workhorse)
- §05-03 — Verticals (with portable radial system)
- §20-01 — Battery maintenance (for solar/portable operation)
- §07-05 — ERP for RF safety (verify EIRP doesn't exceed MPE distances)
