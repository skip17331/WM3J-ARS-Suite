---
id: 09-04
title: ERP / EIRP Output
chapter: 09
section: 04
level: mixed
status: draft
---

# ERP / EIRP Output

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The output of the power budget — putting the previous three sections together — is **ERP** (Effective Radiated Power) or **EIRP** (Equivalent Isotropic Radiated Power). This is the radiated power in the direction of interest, accounting for transmitter output, feedline loss, and antenna gain.

This is the single number that summarizes "how loud is your station, in the direction you care about." It's also the number used for FCC RF exposure compliance (§06-05), DX path-loss calculations, and antenna-system comparisons.

## ERP vs. EIRP, again

Just to be clear:

- **ERP** = transmitter power × feedline efficiency × antenna gain over a **dipole**.
- **EIRP** = transmitter power × feedline efficiency × antenna gain over an **isotropic** radiator.

Conversion: **EIRP = ERP × 1.64** (a dipole is 2.15 dBi over isotropic, = 1.64× linear).
Equivalently: **EIRP_dBW = ERP_dBW + 2.15 dB**.

US FCC and most modern usage prefers EIRP. Some older references and contest-radio specs use ERP. Always note which.

## The master formula

In dB:

**EIRP (dBW) = P_tx (dBW) − feedline_loss (dB) + antenna_gain (dBi)**

Where:
- P_tx_dBW = 10 × log₁₀(P_tx in watts).
- feedline_loss_dB = matched-line loss + mismatch loss.
- antenna_gain_dBi = peak gain in the relevant direction.

Convert dBW back to watts: **P_W = 10^(P_dBW/10)**.

Using the dB form keeps the math easy: just add and subtract.

## Worked examples

### Example 1: Modest station

Setup: 100 W TX, 75 ft RG-213 at 14 MHz, 6 dBi dipole at SWR 1.5:1.

- P_tx: 100 W = 20 dBW.
- Matched feedline loss: 0.65 × 0.75 = 0.49 dB.
- Mismatch loss (~SWR 1.5:1, low matched loss): 0.04 dB.
- Total feedline loss: 0.53 dB.
- Antenna gain: 6 dBi.
- EIRP = 20 − 0.53 + 6 = 25.47 dBW = **352 W EIRP**.

The 100 W transmitter delivers 88 W to the antenna, then the antenna concentrates it into a beam-shaped pattern with 6 dBi peak gain — yielding 352 W EIRP in the broadside direction.

### Example 2: High-gain station

Setup: 1500 W TX, 100 ft LMR-400 at 14 MHz, 12 dBi gain 4-element Yagi at SWR 1.3:1.

- P_tx: 1500 W = 31.76 dBW.
- Matched loss: 0.45 × 1.0 = 0.45 dB.
- Mismatch loss: ~0.02 dB.
- Total feedline loss: ~0.47 dB.
- Antenna gain: 12 dBi.
- EIRP = 31.76 − 0.47 + 12 = 43.29 dBW = **21.3 kW EIRP**.

A 1500 W rig with a Yagi has the same EIRP as a 100 W rig into an isotropic source about 60× more powerful — 21.3 kW vs. 100 W. This is what serious DX stations achieve.

### Example 3: Marginal portable

Setup: 5 W QRP, 30 ft RG-58 at 14 MHz, 0 dBd dipole at SWR 1.7:1.

- P_tx: 5 W = 6.99 dBW.
- Matched loss: 1.6 × 0.3 = 0.48 dB.
- Mismatch loss: ~0.04 dB.
- Total: 0.52 dB.
- Antenna gain: 2.15 dBi (dipole = 0 dBd = 2.15 dBi).
- EIRP = 6.99 − 0.52 + 2.15 = 8.62 dBW = **7.3 W EIRP**.

A QRP station's EIRP is modest by big-station standards but enough to make trans-continental contacts on 20 m with right propagation.

### Example 4: VHF FM mobile

Setup: 50 W mobile rig, 10 ft RG-8X to a 1/4-wave whip at 144 MHz, SWR 1.5:1.

- P_tx: 50 W = 16.99 dBW.
- Matched loss: 4.5 × 0.1 = 0.45 dB.
- Mismatch loss: ~0.05 dB.
- Total: 0.50 dB.
- Antenna gain: 2 dBi (1/4-wave whip; mobile installs typically slightly better than ideal due to ground plane).
- EIRP = 16.99 − 0.50 + 2 = 18.49 dBW = **70 W EIRP**.

So a 50 W mobile FM station puts out about 70 W EIRP — modest, fine for working a 10-mile-range repeater.

### Example 5: 1500 W into a Yagi pointed wrong

Setup: 1500 W, 100 ft LMR-400, 12 dBi Yagi at SWR 1.3:1, but the QSO partner is in the back lobe (gain ~−2 dBi at that angle).

- P_tx: 31.76 dBW.
- Total feedline loss: ~0.47 dB.
- Antenna gain (in partner's direction): -2 dBi.
- EIRP toward partner = 31.76 − 0.47 − 2 = 29.29 dBW = **850 W EIRP**.

So even with 1500 W and a Yagi, if the partner is not in the antenna's lobe, you have only 850 W EIRP. Compare to peak 21.3 kW — 14 dB difference. That's why beam direction matters.

## Cumulative power-budget table

For quick reference, an "all-good" budget at typical amateur conditions:

| Station class | TX (W) | Cable | Cable loss (dB) | Antenna | Gain (dBi) | EIRP (peak) |
|---------------|---------|-------|-------------------|---------|-------------|--------------|
| QRP portable | 5 | 30' RG-58 | 0.5 | Dipole | 2.15 | 7 W |
| Beginner HF | 100 | 100' RG-58 | 1.6 | Dipole | 6 (over ground) | 270 W |
| Modest HF | 100 | 75' RG-213 | 0.5 | Dipole | 6 | 350 W |
| Better HF | 100 | 75' LMR-400 | 0.35 | Dipole | 6 | 365 W |
| Mid-tier HF | 100 | 100' LMR-400 | 0.45 | 3-elem Yagi | 9 | 720 W |
| Serious HF | 1500 | 100' LMR-400 | 0.45 | 4-elem Yagi | 12 | 21 kW |
| Top-tier HF | 1500 | 200' hardline | 0.36 | Stacked Yagis | 18 | 76 kW |
| 2 m FM mobile | 50 | 10' RG-8X | 0.5 | 1/4-wave whip | 2 | 70 W |
| 2 m base | 100 | 50' LMR-400 | 0.75 | 6-element Yagi | 12 | 1.4 kW |
| 70 cm UHF base | 50 | 50' LMR-400 | 1.4 | 7-element Yagi | 13 | 720 W |

These are typical numbers; actual installations vary.

## Using EIRP for RF safety

For 47 CFR §97.13 RF exposure compliance:

1. Compute EIRP (this section's output) for each band/antenna combination.
2. Identify locations where humans might be exposed.
3. Use the §06-02 MPE limits for the operating frequency.
4. Compute power density at each bystander location: PD = (0.0796 × EIRP_W / d²_m²) for far-field; corrections for near-field.
5. Compare to MPE; document compliance.

The §10 (RF Exposure Calculator) in J-Hub does this; you input EIRP from this chapter's output.

## EIRP and DX path budget

For working DX, the path-loss equation gives received signal at distance d:

**PR (dBm) = PT (dBm) + GT (dBi) − Loss(d, f) + GR (dBi) − Lcable_RX (dB)**

Where Loss(d, f) is the free-space path loss (FSPL):

**FSPL (dB) = 20 × log₁₀(d_km) + 20 × log₁₀(f_MHz) + 32.45**

For 14 MHz, 5000 km: FSPL = 20 × log(5000) + 20 × log(14) + 32.45 = 73.97 + 22.92 + 32.45 = **129.4 dB**.

So your EIRP minus 129.4 dB = received signal level at the far station's antenna.

A 1500 W into 12 dBi Yagi (43 dBW EIRP) at 14 MHz to a station 5000 km away with a 6 dBi dipole and 1 dB feedline loss:

PR = 43 + 0 (units conversion to dBW) − 129.4 + 6 − 1 = **−81.4 dBW = −51.4 dBm = S9 + 22 dB**.

That's a strong signal; easily worked. Compare to a 5W QRP setup: P = 6.99 + 0 − 129.4 + 6 − 1 = **−117.4 dBW = -87.4 dBm = S5**. Marginal but workable on 20 m.

> ⚙️ **Advanced —** The free-space path loss formula assumes line-of-sight propagation. For HF, the path is not free-space — it goes through the ionosphere. Real HF propagation involves: (a) ionospheric refraction with associated absorption (often dominant at HF), (b) multipath from multi-hop paths, (c) polarization rotation through the ionosphere (Faraday rotation at HF), (d) signal "QSB" from changing path geometry. VOACAP and similar prediction tools model all of these, producing a probability-of-success curve for each path/time/frequency combination — which is what propagation forecasts you see online are computing.

## Common EIRP mistakes

- **Using TX wattage instead of EIRP.** Some online calculators want EIRP; they assume gain = 0 dBi if you give them transmitter wattage. Result: underestimate compliance distance for any directional antenna.
- **Using peak gain for a non-peak-direction QSO.** Effective gain in the partner's direction is what matters. Sidelobe direction has 10-15 dB less gain.
- **Mixing up dBd and dBi.** EIRP uses dBi.
- **Ignoring tuner loss.** A tuner matching a high-SWR antenna may lose 1–3 dB. Subtract from EIRP.
- **Forgetting ALC compression.** A 100 W rig running with heavy SSB voice may average 30 W; that's the EIRP-relevant number for sustained operating.

## See also

- §09-00 — Chapter overview
- §09-01 — TX power
- §09-02 — Feedline loss
- §09-03 — Antenna gain
- §09-05 — Portable budget (worked example)
- §06-05 — ERP for RF safety
- §01-07 — Prediction models (where path loss meets EIRP)
- §10 — RF Exposure Calculator
