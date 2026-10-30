---
id: 08-04
title: Duty Cycle
chapter: 08
section: 04
level: mixed
status: published
---

# Duty Cycle

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The FCC's MPE limits are **time-averaged**, not instantaneous. This is a critical detail that determines whether a "non-compliant peak field" is actually a violation. A station whose peak field exceeds MPE for 30 seconds out of every 6 minutes may comply easily; a station whose field is constantly at the MPE level — even if peaks are lower — may not.

This section unpacks the two duty cycle factors: **mode duty cycle** (the fraction of time within a transmission that the carrier is actually present at full amplitude) and **TX time fraction** (the fraction of overall time the station is actually transmitting). Both multiply to give the time-averaged power for MPE evaluation.

## The two factors

When you compute MPE compliance, **time-averaged power** is what matters:

**P_time-avg = P_peak × (mode duty cycle) × (TX time fraction)**

Where:

- **P_peak** is your transmitter's peak output power (1500 W if running legal-limit; 100 W typical entry-level; etc.).
- **Mode duty cycle** is intrinsic to the modulation scheme — what fraction of time the carrier is at full amplitude during a transmission.
- **TX time fraction** is operational — what fraction of clock time you are actually transmitting (vs. receiving or unpowered).

Both factors are between 0 and 1.

## Mode duty cycles

| Mode | Duty cycle | Notes |
|------|-----------|-------|
| **CW** (steady tone, key down) | ~40% in actual QSO (text duty cycle ~40%); 100% during a long zero-beat key-down | Includes inter-character gaps |
| **CW (contest QSY ratchet)** | ~60% | Faster pace, less gap |
| **CW (continuous tune-up)** | 100% | Treat as full duty for evaluation |
| **FM** | 100% | Carrier always on during TX |
| **FT8, FT4** | 100% (during the 13-s or 6.4-s burst) | Continuous-carrier digital |
| **PSK31** | ~100% (during burst) | Continuous-carrier (variations exist) |
| **RTTY, MFSK** | 100% (during burst) | Continuous-carrier |
| **SSB voice (typical)** | ~30–40% | Highly modulation-dependent |
| **SSB voice (compressed/processed)** | ~40–60% | Compression raises average power |
| **AM** | ~50% | Peak envelope is 4× average power |
| **DV (DMR/D-STAR/Fusion)** | 100% during TX | Continuous-carrier digital voice |
| **APRS / packet** | 100% (during burst) | Bursts are short, often 1–3 seconds |

The FCC's bulletin OET-65 and the ARRL's RF Exposure Calculator both use these numbers. A few common misunderstandings:

- **CW is not 100% duty cycle in actual operating.** Inter-character spaces and word gaps reduce it to ~40%. Only a steady key-down "tune-up" carrier is 100%.
- **SSB voice is not 100% duty cycle.** Even maximally compressed SSB rarely exceeds 50% time-averaged. This is part of why SSB amateur stations rarely have MPE problems even at high peak power.
- **Digital modes (FT8, RTTY, etc.) ARE 100% duty cycle during the burst.** Your peak power = your burst-average power.
- **FM and DV are 100% duty cycle during TX.** A 50W mobile FM rig at full press-to-talk produces 50 W average power as long as the mic is keyed.

## TX time fractions

This is the operational factor. Common ranges:

| Operating activity | TX time fraction |
|-------------------|-----------------|
| Listening to nets, occasional check-in | 5–15% |
| Casual ragchew SSB QSO | 40–50% |
| Contest CQing (continuous transmission of CQ) | 60–80% |
| FT8 (15-second TX, 15-second RX, repeat) | 50% |
| Working a pile-up | 50–70% (you're talking when not listening) |
| Repeater PTT button held 30 s, off 5 minutes | <10% |
| Tune-up procedure | Brief but high: 100% × 5 seconds = 5/360 = 1.4% |

For MPE evaluation, **assume the worst-realistic TX time fraction** for your typical operating. If you contest, use 70%. If you mostly listen, use 15%.

## Putting them together

A 1500 W FT8 station, transmitting 50% of clock time, evaluates as if it were:

P_time-avg = 1500 × 1.0 × 0.5 = **750 W average**

Convert to MPE-compliant distances using 750 W (not 1500 W), and the math works out.

A 100 W SSB station with ~40% mode duty and 50% TX fraction:

P_time-avg = 100 × 0.4 × 0.5 = **20 W average**

This is why an SSB ragchewer with a 100 W station has an effectively 20-W exposure profile, while an FT8 contest station at 100 W has a 50-W exposure profile, and a FM repeater at 50 W transmitting briefly all day has a 5-W exposure profile.

## Time-averaging windows

The FCC's time-averaging is over:

- **6 minutes** for controlled environments.
- **30 minutes** for uncontrolled environments.

So averages are computed over those windows. A station that bursts 1500 W for 15 seconds out of every minute has, in the controlled window:

P_time-avg = 1500 × (1.0 mode duty cycle for digital) × (15/60 = 25%) = **375 W**

For uncontrolled (30-min window), if the activity is the same throughout: same answer. If you only transmit for 5 minutes during the 30-min window, P_time-avg drops further.

## Why it matters

Three real consequences:

1. **High-power digital modes are the harder MPE case.** 1500 W FT8 + 50% TX fraction = 750 W time-averaged, vs. 1500 W SSB + 40% mode + 50% TX = 300 W time-averaged. The same gear used in different modes has 2.5× different MPE-relevant exposure.
2. **CW operators get a friendly factor.** ~40% intrinsic duty + maybe 50% TX fraction = 20% time-averaged exposure relative to peak.
3. **SSB voice is "easy" for MPE compliance.** This is why most amateur SSB stations comply easily even at high peak power.

## Practical mitigation via duty cycle

If your station is borderline non-compliant on a particular band/antenna combination:

- **Switch from FT8 to SSB on that band**: you might gain 3× headroom in MPE.
- **Reduce TX time fraction**: if you ragchew instead of contest on that combination, time-averaged power drops.
- **Use the worst-mode-evaluation, not the average**: but then accept that you have to actually limit yourself to less-aggressive duty.
- **Don't try to argue mode duty cycle below the actual numbers**: the FCC will not accept "I only used 10% duty SSB" without measurement evidence.

## Tune-up: special case

Tuning your antenna using a steady carrier (CW with key down, or AM carrier) is **100% duty cycle**. A 5-second tune-up at 100 W is 100 W × 1.0 × (5/360) = 1.4 W time-averaged over a 6-minute window. Generally negligible for MPE.

But: **if you tune up at full 1500 W for 30 seconds**, you produced 1500 W × 1.0 × (30/360) = 125 W time-averaged. That's still small for the operator (controlled environment) but might matter if a bystander walks by during the tune-up.

Best practice: tune at reduced power (typically 50 W or less), and keep tune-up cycles short.

## What about peak field strength?

The FCC's rules use *time-averaged* MPE. But for **safety against acute injury** (RF burns, eye damage, immediate localized heating), **peak instantaneous field strength matters too**, separately from MPE compliance.

- A 1500 W signal at 6 inches from a magnetic loop's gap can produce kilovolts of E-field instantly. That's an injury risk regardless of duty cycle.
- A 1500 W signal at 50 ft from a Yagi cannot produce dangerous instantaneous fields, but the time-averaged exposure may still need evaluation.

In short: **MPE compliance covers chronic-exposure risks. Acute-injury risks need separate evaluation** (see §08-07 on RF burns).

> **Advanced —** The 6-minute and 30-minute time-averaging windows come from the IEEE C95.1 thermal-equilibrium model: at frequencies above 100 kHz, the dominant biological effect is tissue heating, and thermoregulation operates on timescales of minutes. Skin and superficial tissue equilibrate in ~6 minutes (hence the controlled limit; trained personnel can take corrective action within that window). Whole-body or deep-tissue heating equilibrates in ~30 minutes (hence the uncontrolled limit). Below 100 kHz, the dominant effect shifts from heating to nerve excitation, and time-averaging is less applicable; that's why the MPE rules treat sub-100-kHz exposures differently (out of scope for amateur except 137 kHz / 472 kHz).

## Common mistakes

- **Using "100% duty cycle" for SSB.** It's not. The conservative number is 40%; ARRL OET-65 calculations use 0.4.
- **Forgetting that FT8 IS 100% duty during burst.** People underestimate digital mode exposure.
- **Ignoring TX time fraction.** A station that transmits 5% of the time has 5% of the average exposure of one transmitting 100% of the time at the same peak power.
- **Confusing peak with average.** A 100 W peak SSB station has ~40 W time-averaged at 100% TX time fraction; the calculated MPE distance is for the 40 W number.

## See also

- §08-02 — MPE limits (the time-averaged values being compared against)
- §08-05 — ERP (the peak-power input)
- §08-01 — FCC rules (the time-averaging is in 47 CFR 1.1310)
- §17-14 — RF Exposure Calculator (handles duty cycle automatically)
