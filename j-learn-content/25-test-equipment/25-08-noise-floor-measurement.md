---
id: 25-08
title: Measuring Station Noise Floor
chapter: 25
section: 08
level: mixed
status: published
---

# Measuring Station Noise Floor

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The **noise floor** of your station is the minimum signal level you can usefully receive. Below it, signals disappear into "grass" on the band. The noise floor on every HF station is dominated by **ambient noise picked up by the antenna** — not by the receiver's internal noise — and is *not* something you control by tuning the rig.

Knowing your noise floor matters for three reasons:

- **Detection threshold.** A signal 10 dB above your noise floor is workable; 5 dB above is marginal; below the floor is invisible.
- **Diagnosis.** A higher-than-typical noise floor is the symptom of nearby RFI. You can't fix what you can't measure.
- **Comparison.** Knowing your S-meter reading at the floor lets you compare bands, antennas, and times of day quantitatively.

## Reference values — what's "normal"?

Your noise floor depends on **band**, **location type**, and **time of day**. Approximate values for the **40 m band** in the evening:

| Location | Typical noise floor (S-units) | Typical (dBm) |
|----------|-------------------------------|----------------|
| Remote / rural (no power lines for miles) | S2 – S3 | -113 to -119 dBm |
| Suburban (typical neighborhoods) | S5 – S6 | -97 to -107 dBm |
| Urban / dense city | S7 – S9 | -85 to -97 dBm |
| Very bad: under power lines, near industrial | S9+10 to S9+30 | -65 to -85 dBm |

These are receive **with antenna connected, in your normal band, in the rig's narrow CW or SSB bandwidth**.

By band, noise generally falls with frequency:

- 160 m / 80 m: highest (atmospheric noise dominates)
- 40 m / 30 m: high (both atmospheric and man-made)
- 20 m: moderate
- 15 m / 10 m: lower (less atmospheric noise, but more man-made if you're in a city)
- 6 m / 2 m / 70 cm: lowest (the receiver's own noise figure becomes the floor on a quiet day)

> **Advanced —** The fundamental noise floor of the universe at amateur HF frequencies is about **-160 dBm/Hz** of cosmic background and galactic noise. In a 2.5 kHz SSB bandwidth that's about -126 dBm, equivalent to S1. Anything above that is *additional* noise from atmospheric, man-made, or receiver sources. Your antenna captures all of it; the rig amplifies it but can't reduce it. The only path to lower noise floor is a better antenna location, a directional antenna that nulls the noise source, or moving the noise source.

## S-units and dBm

The S-meter on most modern rigs reads in **S-units**, with one S-unit nominally equal to **6 dB** (though many rigs are nonlinear and don't actually deliver 6 dB per S-unit, especially below S9).

The IARU reference: **S9 = -73 dBm**, with -6 dB per S-unit going down:

| S-unit | dBm | μV (across 50 Ω) |
|--------|-----|-------------------|
| S9+30 dB | -43 | 1.6 mV |
| S9+20 dB | -53 | 500 μV |
| S9+10 dB | -63 | 160 μV |
| **S9** | **-73** | **50 μV** |
| S8 | -79 | 25 μV |
| S7 | -85 | 13 μV |
| S6 | -91 | 6 μV |
| S5 | -97 | 3 μV |
| S4 | -103 | 1.6 μV |
| S3 | -109 | 0.8 μV |
| S2 | -115 | 0.4 μV |
| S1 | -121 | 0.2 μV |

Above 30 MHz, the convention shifts to S9 = -93 dBm (20 dB less). Your rig's S-meter may follow either convention; check the manual.

## The standard noise-floor measurement procedure

The point of the procedure is to **distinguish receiver noise from antenna noise**. If the noise floor stays the same when you disconnect the antenna and replace with a dummy load, the rig's internal noise is dominant — there's no point hunting external noise sources. If the noise floor drops significantly with a dummy load, your antenna is picking up real ambient noise.

Procedure:

1. **Pick a band and a clear frequency** — one with no signals nearby. The middle of a CW segment in mid-afternoon usually works.
2. **Set the rig**: CW mode, 500 Hz filter (or whatever your narrowest filter is), AGC off (so the S-meter reads correctly), preamp off, attenuator off, RF gain at maximum.
3. **Read the S-meter** with the antenna connected. Write down the value. Call this `S_antenna`.
4. **Swap to a 50 Ω dummy load** at the antenna jack. Read again. Call this `S_dummy`.
5. **Compare:**
   - If `S_antenna - S_dummy ≥ 10 dB`: antenna noise dominates. Most stations.
   - If `S_antenna - S_dummy ≤ 3 dB`: receiver noise dominates. Your antenna is so quiet (or so deaf) that the rig's NF is the limit.

The typical HF station shows `S_antenna - S_dummy` in the range of 15–30 dB — confirmation that external noise is the limiter and that pursuing RFI mitigation will improve receive sensitivity.

> **Advanced —** Receiver-noise-limited operation is normal on VHF/UHF where atmospheric noise is low. On 2 m, a station with a 0.5 dB NF preamp and a clean antenna mounting site may have receiver noise = antenna noise within ~3 dB. This is the regime where preamp noise figure improvements actually buy you better detection threshold. On HF, even a perfect noise-free receiver wouldn't help because the antenna delivers atmospheric and man-made noise far above the rig's noise floor.

## Procedure for identifying band-specific noise sources

Once you've measured the noise floor on each band, you can localize a problem.

1. **Tune across each amateur band.** Note the S-meter reading on a clear frequency in each.
2. **Plot** the readings vs. band:

```
   S-units
     9 │             X     X (broadband noise — power line)
     8 │       X    X X    X X
     7 │   X  X X  X  X    X X X  X
     6 │   X X X X  X X    X X X  X X
     5 │X  X X X X  X X    X X X  X X X
       │
       └────────────────────────────────────► band
       160 80  40  30 20 17 15 12 10  6  2  70
```

A flat-across-all-bands noise floor suggests broadband man-made noise (power-line, switching supplies, BPL).

A noise floor peaking on specific bands suggests a tuned source:

- **Peaks at 80 m and 40 m**: switching-mode power supplies (50–100 kHz fundamental, harmonics in HF)
- **Peaks at 14 MHz only**: a single device emitting at that frequency (LED light driver, charger)
- **Peaks at every multiple of 60 Hz**: power-line related (look for "buzz" tone too)
- **Peaks at 14.2 MHz, 14.4 MHz, 14.6 MHz**: a switching supply with switching frequency = 200 kHz

3. **Find the source by direction-finding.** Use a portable FSM or hand-held HF receiver with a loop antenna; walk around your property and into neighbor's yards to locate.

See §14 RFI for the full source-hunting procedure.

## A worked example

Hypothetical rural station, 40 m, 9 PM local:

- Antenna noise reading: **S2** (about -115 dBm)
- Dummy load reading: **S0** (below S1 — the rig's own noise floor at this filter setting)
- Difference: ~15 dB → antenna noise dominates

So this station has an excellent noise floor (S2 on 40 m is rural-quiet). A weak DX signal at S3 is 6 dB above the noise — detectable in CW; marginal in SSB. A signal at S5 is 18 dB above noise — solid copy in any mode.

Now suburban station, 40 m, 9 PM local:

- Antenna: **S7** (-85 dBm) — typical suburban
- Dummy load: **S0** (-121 dBm)
- Difference: 36 dB → strongly antenna-noise-dominated

Same DX signal at S3 (-109 dBm) is now 24 dB **below** the suburban noise floor and **completely inaudible**.

This is the noise-floor problem in a nutshell: the rural station can hear signals the suburban station can't, even though both rigs are identical.

## Measuring noise floor in dBm directly (advanced)

For more rigor, use the rig's actual measured noise floor in dBm rather than S-units. Most modern rigs are linear within ~3 dB across a wide range; some (Icom IC-7300, Kenwood TS-590, Yaesu FT-DX10) actually deliver close to 6 dB per S-unit.

Calibration: feed a calibrated signal generator at known dBm into the rig's antenna port. Adjust to make the S-meter read S9. The reading should be -73 dBm (for HF). If it's not, note the offset.

Then for noise floor: read S-units, convert to dBm using the calibration, and report dBm. Or use a software spectrum-display tool (PowerSDR, SDR-Console, Quisk) that reads directly in dBm.

> **Advanced —** "Noise floor" in dBm depends on bandwidth. A 2.5 kHz SSB filter delivers 10×log10(2500/500) = 7 dB more noise than a 500 Hz CW filter from the same external source. Specifying noise floor without specifying bandwidth is ambiguous; always say "S2 in 500 Hz CW" or "-110 dBm in 2.5 kHz SSB."

## Common mistakes

- **Reading S-meter with AGC on.** AGC compresses the S-meter reading at high levels. Disable AGC or use the rig's "noise floor measurement" mode if it has one.
- **Reading with preamp on, then noting noise floor.** Preamp adds 10–20 dB to displayed S-meter for both noise and signal. The ratio (signal-to-noise) doesn't change but the displayed dBm does. Always note preamp state when reporting noise floor.
- **Confusing band noise with antenna noise.** A long-wire vs. a small loop will have different noise floor *because the antennas have different patterns*, not because the local noise level changed.
- **Measuring during a thunderstorm.** Lightning impulses jack up noise floor by 10s of dB. Wait for quiet conditions.
- **Forgetting to compare to dummy load.** Without that comparison, you don't know whether you're rig-limited or antenna-noise-limited.

## See also

- §14 — RFI (sources and mitigation)
- §15 — Noise Sources
- §25-05 — Field Strength Meters (for direction-finding noise)
- §25-03 — Spectrum Analyzers
- §17 — Formulas (dBm, kTB, NF math)
