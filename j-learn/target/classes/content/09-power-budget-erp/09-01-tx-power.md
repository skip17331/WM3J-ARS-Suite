---
id: 09-01
title: TX Power
chapter: 09
section: 01
level: simple
status: draft
---

# TX Power

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The transmitter's output power is the starting point of the power budget. It sounds simple — "100 W rig" — but several real-world factors mean the rated power isn't always what's actually leaving the rig. SWR fold-back, duty cycle effects, ALC compression, age-related output drop, and the difference between PEP and average power all matter for an honest budget.

This section covers what TX power actually is, how to measure it, and what to put into the rest of the power-budget calculation.

## Rated power vs. actual output

A rig labeled "100 W" typically means: **100 W peak output into a 50 Ω load, on the band where the rig was tested by the manufacturer.**

Real-world variations:

- **Across bands**: most rigs vary ±2–3 dB across the bands they cover. A 100 W rig might do 110 W on 20 m and 85 W on 6 m. Check the spec sheet.
- **At low temperatures**: cold transistors deliver slightly more power; hot transistors slightly less. ~5% variation typical.
- **With age**: PA transistors degrade slowly. A 10-year-old rig may deliver 5–15% less than its label.
- **At fold-back**: under high SWR, the rig automatically reduces power. A "100 W" rig at SWR 3:1 may actually be running at 50 W. (See §08-02.)
- **In ALC compression**: the rig's automatic level control can compress on voice peaks; average power drops below peak rated.

The honest baseline: **measure your rig's actual output** with a known-good wattmeter into a dummy load. Use that number in budgets, not the label.

## Measuring transmitter output

The right tool: an **average-reading wattmeter** with a directional coupler in the line, between the rig and a dummy load. Cross-needle types (Bird 43, Daiwa, Diamond) are common; many show forward and reflected power simultaneously.

Procedure:

1. Disconnect feedline from rig; insert wattmeter; connect dummy load to wattmeter output.
2. Tune to the band of interest; set rig to CW or AM mode (steady carrier, not SSB modulation).
3. Key the rig at full power for ~5 seconds.
4. Read forward power; reflected should be near zero (dummy load gives ~1.05:1 SWR).
5. Repeat across each band you care about.

Document. The output you measured at 14 MHz is the number to use in your 14 MHz power budget.

For SSB voice, the rig is typically modulated 30-50%, so average power is 30-50% of peak. For digital modes (FT8, RTTY), duty cycle is 100% and average = peak. See §06-04 for the duty-cycle factors that affect time-averaged power for RF safety calculations.

## Peak Envelope Power (PEP)

In SSB, the **peak envelope power (PEP)** is the rig's output at the instantaneous peak of a modulation cycle. The average power is much less. FCC defines amateur power limits in PEP — the legal limit is 1500 W PEP, which means peak-of-peak instantaneous output during voice transmission, not average.

For SSB voice:
- Peak envelope = ~rated peak.
- Average = 30–40% of PEP for typical voice without compression.
- Average = 50–70% of PEP with heavy compression.

For tone (dummy-load test signal):
- Both PEP and average = the rig's full power.

This is why the PEP rating on a transmitter is the same as what a wattmeter reads on dummy load with a steady tone. SSB just changes the average.

## Fold-back and SWR

Modern transceivers monitor reflected power and reduce output when SWR rises:

| SWR | Approximate output (% of rated) |
|-----|--------------------------------|
| 1.0:1 | 100% |
| 1.5:1 | 95–100% |
| 2.0:1 | 80–90% |
| 2.5:1 | 60–80% |
| 3.0:1 | 50–70% |
| 4.0:1 | 30–50% |
| 5.0:1 | 10–30% (or auto-cutoff) |

The exact fold-back curve varies by manufacturer. Some rigs are aggressive (Yaesu FT-991A folds steeply at 2:1); some are forgiving (older Kenwood designs allow more headroom). Check your rig's manual.

For accurate budgets at high SWR: **measure actual rig output at the operating SWR**, not assume rated. A "100 W" rig running 50 W due to fold-back has half the budget you assumed.

## Duty cycle considerations

For sustained operation, **average TX power matters more than peak**. A 1500 W FT8 station running 50% TX time has:

P_avg = 1500 × 1.0 (FT8 100% duty within burst) × 0.5 (TX time fraction) = **750 W average**.

For thermal budgets (rig PA heatsink, feedline heating, antenna components), use average. For RF exposure (§06-04), use time-averaged. For "what reaches the antenna in this moment", use peak (which equals rated if the rig isn't folded back).

## Fixed-output vs. variable-output rigs

Most modern transceivers have **continuously variable** output — turn a knob and the power changes from minimum to rated. Some older rigs, mobile rigs, and HT rigs have **fixed output levels** (e.g., 5 W high, 1 W low, 0.5 W TX power).

For variable-output rigs, **set the power for what you actually need**:
- Local QSO across town on 2 m: 5 W is plenty; full 50 W is wasteful.
- Working DX on 20 m: full 100 W gives best chance.
- Operating QRP for the challenge: 5 W or less; pick the value that fits the contest/event rules.

## Power supply considerations

The transmitter's output requires DC input power. A 100 W output rig typically needs 22 A at 13.8 V (304 W DC input) — efficiency is ~33% for HF SSB.

Implications:
- **Power supply current rating**: must handle TX peak (continuous if running digital).
- **Cable from supply to rig**: must handle the same current with low voltage drop.
- **Battery for portable**: 100 W from a 100 Ah battery = ~30 minutes of continuous TX before noticeable voltage sag.

For battery-portable, especially at higher TX powers, the power supply often becomes the limiting factor before the antenna or feedline.

## Output during heavy operating

A station that's transmitting hard for an extended period (contest, DX-pedition rate, FT8 sweep) has additional concerns:

- **Heat dissipation**: PA temperature rises during long key-downs; rigs auto-derate when too hot.
- **Power supply derate**: some supplies can't sustain rated output; full power for 10 seconds is fine, full power for 10 minutes is different.
- **Cable heating**: long high-power transmissions warm the feedline; if extreme, dielectric softens.

Plan budgets for typical operating conditions, not for theoretical peak.

> ⚙️ **Advanced —** Modern transceivers use **MOSFET or LDMOS power amplifiers** in HF stations — typically Class AB linearized for SSB, with class A or B for tone. Efficiency is set by Class AB design (~50% at full output) plus regulator overhead. The "rated 100 W output" is achievable into 50 Ω, with thermal protection circuits, automatic level control, ALC, and SWR fold-back all designed to keep the PA operating in a safe regime. The trade-off: aggressive protection (good for the PA) may cut output below the rated label under non-ideal conditions. Knowing your rig's protection thresholds tells you when its label-output is achievable vs. when fold-back is reducing it.

## Common TX-power budget mistakes

- **Using rated power instead of measured.** Your rig may deliver 92 W when labeled 100 W; that's the number to use.
- **Forgetting fold-back at high SWR.** A 100 W rig at SWR 3:1 may be at 60-70 W actual.
- **Mixing peak and average values.** Specifically: SSB peak = 100 W, average = 30-50 W. RF safety wants time-averaged; antenna heating wants average; "watts to neighbor's deck" wants peak.
- **Not measuring across bands.** Output at 14 MHz isn't the same as at 50 MHz. Check both.
- **Assuming the radio's display is accurate.** Some rigs' panel-display power is computed from voltage/current readings and can be inaccurate; an external wattmeter is more trustworthy.

## See also

- §09-00 — Chapter overview
- §09-02 — Feedline loss
- §09-04 — ERP / EIRP output
- §06-04 — Duty cycle (the time-averaging factor)
- §08-02 — SWR & reflected power (fold-back trigger)
- §17-01 — Battery maintenance (for portable power supply)
