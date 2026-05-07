---
id: 08-02
title: SWR & Reflected Power
chapter: 08
section: 02
level: mixed
status: draft
---

# SWR & Reflected Power

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

When a wave traveling down a transmission line meets an impedance mismatch at the far end, **part of the wave reflects back** toward the source. The forward and reflected waves combine in the line, producing standing waves of voltage and current — a pattern that's stationary in space, oscillating in time. The **Standing Wave Ratio (SWR)** measures the depth of this pattern.

This section unpacks what SWR is, what it tells you, and how it relates to forward and reflected power. The companion sections cover the *consequences* of high SWR — extra loss (§08-03), reduced delivered power (§08-04), and feedline transformation (§08-06).

## SWR, defined

The basic definition:

**SWR = V_max / V_min**

Where V_max and V_min are the peak and trough of the standing wave pattern in voltage along the line. Equivalently:

**SWR = (1 + |Γ|) / (1 − |Γ|)**

Where |Γ| is the magnitude of the **reflection coefficient** at the load. |Γ| = 0 means no reflection (perfect match), so SWR = 1. |Γ| = 1 means total reflection (open or short circuit), so SWR = ∞.

A few common conversions:

| SWR | \|Γ\| | Reflected power (% of forward) | Return loss (dB) |
|-----|------|--------------------------------|------------------|
| 1.0 | 0.000 | 0% | ∞ |
| 1.1 | 0.048 | 0.2% | 26 |
| 1.5 | 0.200 | 4% | 14 |
| 2.0 | 0.333 | 11% | 9.5 |
| 3.0 | 0.500 | 25% | 6 |
| 5.0 | 0.667 | 44% | 3.5 |
| 10.0 | 0.818 | 67% | 1.7 |
| 20.0 | 0.905 | 82% | 0.9 |
| ∞ | 1.000 | 100% | 0 |

So a 2:1 SWR means about 11% of forward power is reflected; 5:1 means 44%; 10:1 means 67%. **The bigger the SWR, the more power bouncing around in the feedline.**

## Forward, reflected, and net power

When a transmitter pushes power into a transmission line, three things happen:

1. **Forward wave** P_fwd travels toward the antenna.
2. **Reflected wave** P_ref returns toward the transmitter (if antenna impedance ≠ line Z₀).
3. **Net delivered power** P_load = P_fwd − P_ref reaches the antenna.

The relationship to SWR:

**P_ref / P_fwd = |Γ|² = ((SWR − 1) / (SWR + 1))²**

A 100 W transmitter into a perfectly matched antenna (SWR 1:1): P_fwd = 100, P_ref = 0, P_load = 100.

Same transmitter into 3:1 SWR antenna: P_ref = 100 × ((3−1)/(3+1))² = 100 × 0.25 = **25 W reflected**. P_load = 100 − 25 = **75 W**.

Note: this assumes a lossless feedline. With real feedline loss, the picture is more complicated — see §08-03.

## What SWR meters actually measure

A typical SWR meter (in the rig, in an external bridge, or built into a tuner) inserts a directional coupler in the feedline. The coupler samples the forward and reflected wave separately, then displays:

- **Forward power** (P_fwd in watts, often as a peak-reading meter for SSB).
- **Reflected power** (P_ref).
- **SWR** computed from the ratio.

What a meter cannot tell you directly:

- **Where** in the system the mismatch is (antenna? Connector? Coax? Tuner?). The meter just sees the result at its own location in the line.
- **How much loss** is occurring in the feedline. SWR meters don't measure loss.
- **What the SWR is at the antenna.** With matched-line loss in between, the SWR at the rig end is *lower* than the SWR at the antenna end. SWR meter at the rig hides high antenna SWR if feedline loss is significant.

## Why SWR is "1.5:1, 2:1, 3:1"

The colon notation just means "ratio of voltage peaks." 1.5:1 = peaks are 1.5x the troughs. 1:1 = no peaks at all (uniform voltage). The convention of ":1" reminds you it's a ratio and the second number is normalized.

Some references write it as "1.5" without the colon. Same thing.

## Common SWR values and what they mean operationally

| SWR | What it means | What to do |
|-----|---------------|------------|
| 1.0–1.3:1 | Excellent match. Antenna is well-tuned. | Operate normally. |
| 1.3–1.7:1 | Good match. Most rigs at full power. | OK. Maybe tune for perfection if you're picky. |
| 1.7–2.0:1 | Acceptable. Most rigs at full power; small extra feedline loss. | Operate; consider tuning. |
| 2.0–3.0:1 | High. Most modern rigs start folding back power. Real mismatch loss. | Investigate. Likely fixable. |
| 3.0–5.0:1 | Very high. Significant mismatch loss; rig may refuse TX. | Stop transmitting. Diagnose. |
| > 5.0:1 | Probably broken. Open or short somewhere. | Stop. Test before TX again. |

Modern transceivers have built-in SWR foldback that automatically reduces transmit power as SWR rises, protecting the PA. By the time the rig folds back to 5 W, you're at SWR 5:1 or worse.

## SWR is frequency-dependent

A given antenna is matched at one or a few specific frequencies (its resonances) and mismatched elsewhere. A typical dipole at its design frequency: 1.3:1. At ±2% (±150 kHz on 40 m): 1.5:1. At ±5%: 2.5:1. Outside the 2:1 SWR bandwidth, the antenna is "out of band" for that resonance.

This is why an antenna analyzer's **SWR-vs-frequency sweep** is so useful — you see the entire response curve, not just one point. The dip in the curve shows where the antenna is best-matched; the width of the dip is the bandwidth.

## SWR and antenna efficiency

SWR doesn't directly tell you anything about antenna efficiency. **A perfect dummy load has SWR 1:1 and is 0% efficient as an antenna.** A 50 Ω resistor presents perfect SWR; it dissipates all power as heat, radiates nothing.

For an antenna to work, it must:

1. **Have low SWR** (so power gets to it through the feedline).
2. **Have high radiation resistance** relative to its loss resistance (so power that reaches it is radiated, not dissipated as heat in the antenna's matching components).

The two conditions are independent. SWR tells you about the first; modeling and pattern measurement tell you about the second. See §04-13 (ground effects on efficiency) and §04-14 (modeling).

## Relationship to return loss

**Return loss (RL)** is another way of expressing SWR, in dB:

**RL = −20 log₁₀ |Γ|**

Higher RL = better match. RL = 14 dB ≈ SWR 1.5:1; RL = 6 dB ≈ SWR 3:1; RL = 0 dB = SWR ∞.

NanoVNAs, network analyzers, and other vector instruments often report return loss directly. The two are equivalent; just different scales.

> ⚙️ **Advanced —** SWR is a single-frequency measurement of the standing-wave amplitude on the transmission line. The full vector reflection coefficient Γ = Γ_real + jΓ_imag carries more information — both magnitude and phase. A Smith chart plots Γ in the complex plane, showing every aspect of the line's impedance behavior at once. SWR is just |Γ| converted to ratio form. Return loss is just |Γ| in dB. All three describe the same physics.

## What rigs do with high SWR

Modern transceivers have **SWR protection circuitry**:

- **Foldback at SWR 1.5–2:1**: rig reduces output power proportionally; full-power operation requires SWR < ~1.5:1.
- **Hard cutoff at SWR 3–5:1**: rig refuses to transmit; alarm / display warning.
- **Failsafe**: rig disconnects PA bias entirely if reflected power exceeds protection threshold.

Older rigs (vacuum-tube finals, no protection) could be damaged by sustained high-SWR transmission — the PA tube or transistors saw out-of-spec voltage during reflected-wave peaks and overheated. Modern transistor rigs usually survive thanks to foldback, but extended high-SWR operation will still age the PA.

## Common SWR mistakes

- **Treating any 1.5:1 as "needs fixing."** 1.5:1 is fine; modern rigs operate at full power. Don't waste time tuning to 1.0.
- **Ignoring high SWR because "the rig still puts out signal."** Foldback is hiding the problem; the antenna is broken or mismatched. Diagnose.
- **Comparing SWR readings between different SWR meters.** Different meters can show ±10-15% different readings due to calibration. For absolute accuracy, use a calibrated reference; for trend monitoring, stick with one meter.
- **Reading SWR at the rig and assuming it's the antenna's SWR.** Feedline loss masks high antenna SWR. Measure at the antenna feedpoint with a portable analyzer for the truth.
- **Treating SWR = 1:1 as "perfect station."** Dummy load gives 1:1 SWR. The match alone tells you nothing about radiation efficiency.

## See also

- §08-00 — Chapter overview
- §08-03 — Mismatch loss (what high SWR costs you)
- §08-04 — Power delivered vs lost
- §04-10 — Feedline effects (impedance transformation hides true SWR)
- §10 — High-SWR troubleshooting
- §04-09 — Smith charts (Γ in complex plane)
- §18 — Coax & connectors
