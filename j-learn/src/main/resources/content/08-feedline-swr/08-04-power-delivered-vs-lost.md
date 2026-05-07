---
id: 08-04
title: Power Delivered vs Lost
chapter: 08
section: 04
level: mixed
status: draft
---

# Power Delivered vs Lost

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

This section ties the previous three together: knowing matched-line loss (§08-01), SWR (§08-02), and mismatch loss (§08-03), how much of your transmitter's power actually reaches the antenna? And where does the lost power go?

The math is straightforward — power in minus losses equals power out — but the bookkeeping has subtleties. Three different "powers" can be measured at different points in the system; getting them right is the difference between a useful prediction and a wrong one.

## The four measurement points

In a typical station, you might measure or care about power at four points:

| Point | What you'd see |
|-------|----------------|
| **PA output** | The transmitter's instantaneous output (rated 100 W or whatever) |
| **Forward power on feedline** | A wattmeter inserted between rig and antenna; reads forward only |
| **Reflected power on feedline** | Same wattmeter; reads reflected only |
| **At the antenna feedpoint** | Net power actually crossing into the antenna terminals |

The relationships:

- **PA output** = 100 W (assuming rig is delivering rated output; not folded back).
- **Forward power on feedline** = ~PA output (small loss in any internal switching/filtering).
- **Reflected power on feedline** = forward × |Γ|² = PA output × ((SWR − 1) / (SWR + 1))².
- **Power at antenna feedpoint** = forward minus reflected, minus matched-line loss, minus mismatch loss.

## The accounting equation

For a single feedline run of matched-line loss L_matched (dB) feeding an antenna at SWR S:

**Power at antenna = P_in × 10^(−L_total/10)**

Where L_total = L_matched + ΔL(SWR, L_matched) (per the table in §08-03).

Solving by example. Setup:
- 100 W PA output.
- 100 ft of LMR-400 at 14 MHz: matched loss = 0.45 dB.
- Antenna at SWR 1.5:1 → mismatch loss ~0.05 dB.
- L_total = 0.50 dB.

Power at antenna = 100 × 10^(−0.05) = 100 × 0.891 = **89 W**.

So 11 W is dissipated as heat — most of it in the cable, a small fraction in any matching network.

## Three breakdown scenarios

### Scenario A: Well-matched HF antenna, modern cable

100 W transmitter, 75 ft LMR-400 to a tuned dipole at 14 MHz, SWR 1.3:1.

- Matched loss: 0.45 × 75/100 = 0.34 dB.
- Mismatch loss: ~0.02 dB.
- Total: 0.36 dB.
- Power at antenna: 100 × 10^(−0.036) = **92 W**.
- Lost: 8 W.

### Scenario B: Same antenna, older cheap cable

100 W transmitter, 75 ft RG-58 at 14 MHz, SWR 1.3:1.

- Matched loss: 1.6 × 75/100 = 1.2 dB.
- Mismatch loss: ~0.06 dB.
- Total: 1.26 dB.
- Power at antenna: 100 × 10^(−0.126) = **75 W**.
- Lost: 25 W.

A 17 W difference — about an S-unit of TX/RX impact — just from feedline choice.

### Scenario C: Tough mismatch + lossy line

100 W transmitter, 100 ft RG-58 at 144 MHz, SWR 4:1.

- Matched loss: 6.6 × 100/100 = 6.6 dB.
- Mismatch loss: ~3 dB.
- Total: 9.6 dB.
- Power at antenna: 100 × 10^(−0.96) = **11 W**.
- Lost: 89 W.

A 1500 W amplifier into the same setup: only **165 W reaching the antenna** — and burning 1335 W in the cable as heat. (Don't actually do this; you'll cook the cable.)

## Where does the lost power go?

Three places:

1. **Conductor heating** in the coax — most of the loss for typical amateur cables. The center conductor and shield have I²R losses; current flowing through finite resistance dissipates heat.
2. **Dielectric heating** in the insulator — secondary contribution; dielectric loss tangent × frequency × E-field² = heat in the dielectric.
3. **Radiation from the cable** — small for sealed coax; significant for badly-installed cable with damaged shield (the cable becomes an antenna and radiates).

For a typical amateur installation, **heat in the cable** is by far the dominant loss mechanism. A 100 W loss over 100 ft of cable means the cable is dissipating 1 W per foot — enough to make it noticeably warm but not enough to damage modern PE-jacketed cable.

At 1500 W input through a marginal cable, dissipation can exceed cable thermal ratings. The dielectric softens, the cable's impedance changes, SWR worsens, more reflected power, more dissipation — runaway. **High-power transmissions through borderline cable can self-destruct in a few minutes.**

## Lost power and rig fold-back

Modern transceivers protect themselves by **reducing power** when SWR is high. The fold-back curve typically:

- 1.0–1.5:1 SWR: full rated power.
- 1.5–2.0:1: ~90% power.
- 2.0–3.0:1: 50-80% (folds rapidly).
- 3.0+:1: 10-40% or auto-cutoff.

So your 100 W rig at SWR 3:1 is actually transmitting maybe 50 W. The "delivered to antenna" calculation has to start from the actual TX output, not the rated.

Fold-back is a safety feature but it complicates the math: you can't just multiply rated power by transmission efficiency.

## Power reaching the antenna ≠ power radiated

A subtle point: power *delivered to the antenna feedpoint* is not the same as power *radiated into space*. The antenna also has internal losses:

- **Antenna conductor losses** (I²R in element wire/tubing).
- **Ground losses** (especially significant for verticals; see §04-13).
- **Match-network losses** in the antenna itself (some antennas have a built-in matching transformer).
- **Loading losses** in loaded antennas (mobile whips, magnetic loops, traps).

A vertical with 4 radials at 14 MHz might have 40% ground loss — meaning of the power reaching the feedpoint, only 60% is radiated. The rest heats the soil.

For a well-installed flat-top dipole at moderate height, antenna efficiency is usually >90% — the dipole's wire and matching network are nearly lossless. For verticals, magnetic loops, and traps, efficiency varies more.

## Round-trip loss for QSO budget

For working a station with marginal signal, **the relevant number is round-trip system loss**:

- TX side: feedline loss going from rig to your antenna.
- RX side: feedline loss going from antenna back to rig (same path, twice if you're working two-way).

A 6 dB total feedline loss = 12 dB round-trip = 2 S-units of received-signal-strength penalty.

This is why for marginal stations, **better feedline pays double**. Replacing a 6 dB feedline run with a 1 dB feedline run gains you 5 dB on TX *plus* 5 dB on RX = 10 dB QSO benefit. About 1.7 S-units in either direction.

## Practical guidance

- **For HF antennas of moderate length (under 100 ft)**: feedline loss is usually under 1 dB; mismatch loss under 0.5 dB. Total system loss ~1-1.5 dB. Acceptable.
- **For VHF antennas of moderate length (under 50 ft)**: same, slightly higher numbers (1-2 dB). Acceptable.
- **For UHF antennas of moderate length (under 30 ft)**: 1-3 dB total loss is typical. Tight.
- **For long runs (>200 ft)**: hardline becomes essential at VHF/UHF; LMR-400 minimum at HF.
- **For high SWR antennas**: get them matched. Mismatch loss is the most-fixable single contribution to system loss.

## Common power-bookkeeping mistakes

- **Reading "100 W" on the rig and assuming 100 W reaches the antenna.** Usually it's 70-90 W after feedline.
- **Ignoring fold-back when computing system efficiency.** A 1500 W rig at SWR 4:1 might actually be running at 200 W; the math has to use that, not the nameplate.
- **Treating dummy load tests as representative.** A dummy load presents 1:1 SWR; the rig sees 50 Ω. Real antennas don't. Test with the actual antenna.
- **Forgetting antenna efficiency.** Power reaching the feedpoint ≠ power radiated. Especially for verticals on average ground.
- **Neglecting connector losses.** Each connector adds ~0.05–0.1 dB. A run with 6 connectors total might add 0.5 dB beyond the cable losses.

## See also

- §08-00 — Chapter overview
- §08-01 — Coax loss by frequency
- §08-02 — SWR & reflected power
- §08-03 — Mismatch loss
- §09 — Power budget & ERP (the system-level extension of this)
- §04-13 — Ground-plane effects (antenna efficiency)
- §10 — High-SWR troubleshooting
- §18 — Coax & connectors reference (when picking a cable for this calc)
- §18-02 — Cable loss specifications
