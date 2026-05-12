---
id: 22-02
title: Tune-up Etiquette
chapter: 22
section: 02
level: simple
status: draft
---

# Tune-up Etiquette

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

When you tune your antenna, you transmit a **steady carrier or noise** for several seconds. That's enormously disruptive on a shared band. Tuning into the wrong place at the wrong time is one of the most common forms of accidental QRM, and it has nothing to do with skill — even experienced operators do it.

This section covers when, where, and how to tune so you don't wipe out other people's contacts.

## What "tuning" actually transmits

Different tuning methods put different signals on the air:

| Method | What goes out |
|--------|---------------|
| **CW key down** | Continuous unmodulated carrier on your frequency |
| **AM tune mode** | Continuous unmodulated carrier (full power) |
| **SSB whistle** | A loud carrier-suppressed tone |
| **FM tune** | Continuous FM carrier (loud and squelch-busting) |
| **Digital tune (e.g., FT8 TX)** | A digital test tone |
| **Tuner auto-tune** | Pulses of carrier (typically 5-15 W) for a few seconds |
| **Antenna analyzer** | Low-power swept signal across a range of frequencies |

The first four are full-power continuous transmissions. They wipe out anything weak nearby — and on the upper bands "nearby" can mean ±5 kHz.

## The cardinal rule

**Never tune up on an active frequency.** Two ways to be sure:

1. **Tune into a dummy load.** A 50Ω dummy load absorbs your signal so nothing radiates. Most modern transceivers have an antenna selector with a dummy-load output. This is the right answer for any tune-up that doesn't strictly require radiating.
2. **Move to a clear frequency** that's well away from active QSOs, and tune briefly there.

Tuning on an active frequency is rude even if you're a kilowatt away from a marginal contact — the marginal contact is probably trying to copy a 3 dB signal that your tuning is now buried.

## When tune-up needs to be on the air

Some tuning genuinely requires a real antenna:

- **Antenna tuner adjustments** that need real-load feedback (autotuner with brief pulses).
- **Verifying SWR** before a contest or DX session, on the actual operating band.
- **Antenna analyzer sweeps** at low power, across a range of frequencies — these are typically <1 W and brief, and often acceptable.
- **Fine-tuning on a freshly built antenna** to verify resonant frequency.

For these, follow this protocol:

### The tune-up protocol

```
1. Listen to the frequency you intend to tune for at least 10 seconds.
2. If you hear anyone, move 3-5 kHz away to a different frequency.
3. Repeat step 1 on the new frequency.
4. Identify: "WM3J tuning, please stand by."
5. Tune at the lowest power that gives you readable feedback
   (typically 5-15 W is plenty for an autotuner).
6. Re-identify when done: "WM3J finished tuning, frequency
   clear."
```

Many operators skip the announce step. Don't — it's the difference between "polite tune-up" and "QRM in the clear."

The announcement matters because:

- Stations within range will hear your tune burst as a steady carrier and wonder if you're being intentionally jamming.
- Operators trying to copy a marginal signal can pause their RX gain controls.
- It's a useful courtesy and takes 2 seconds.

### Dummy-load is almost always the right answer

For 99% of tune-ups, you can use a dummy load:

- **Antenna tuner adjustments** can usually be done into a dummy load if you know the antenna's approximate impedance ahead of time and dial the tuner manually.
- **Power-output checks** (verifying your rig hits full power on a band) work fine into a dummy load.
- **AM modulation checks** can be done in dummy load.
- **Audio drive testing** for digital modes — dummy load.
- **Kit / homebrew testing** — definitely dummy load.

The cases where dummy load **doesn't** work are SWR verification (you need the actual antenna), antenna trim work (matching to the real antenna and feedline system), and certain tuner-feedback methods. These are the only cases where you should tune on the air.

## Tune-up power level

Use the **minimum power** that gives you the result you need. For an autotuner with relay-clicking auto-match, that's typically 5-15 W. For a manual tuner with cross-needle SWR display, you may need 25-50 W to get readable needles. Above that you're rarely getting better data, just causing more interference.

Most modern transceivers have a "tune" button that drops to a preset tune-power level (often 10 W) and outputs a steady CW carrier for the autotuner to respond to. Use this feature.

## Dedicated tune-up frequencies (don't really exist)

Old-school operating sometimes mentions "tune-up frequencies" — specific spots in each band where tuning is conventional. **These aren't real conventions today.** Any frequency in the band can be active; "tune-up frequencies" historically caused long bursts of carrier in the same place which was a different problem.

Better practice today: use the lowest unused part of the band for tune-up. On 20 m that's typically 14.000-14.030 if it's a CW-only segment; on 40 m it's 7.000-7.020. Check your bandplan (§20) for what's CW-only or "experimental" in your region.

## Tune-up during contests and pile-ups

Special cases:

- **During a contest**, tune in advance (before the contest starts), use a dummy load for any verification, and don't tune during peak hours. Even a brief tune on a contest QSY frequency is QRM.
- **During a pile-up for rare DX**, **never** tune in the pile-up. Move several kHz off and tune very briefly. Tuning at the DX listen frequency or split frequency is a quick way to be told to "stop tuning" by very angry operators.
- **In nets**, tune only when the NCS specifically opens the frequency for technical issues. NCS will sometimes say "Pre-net tune-up window from 1855 to 1900 UTC" before opening the net itself.

## Common tune-up mistakes

- **Long tune cycles.** A 30-second tune is excessive even on a clear frequency. Modern autotuners need 1-5 seconds; manual tuners need 5-15 seconds. Anything longer suggests a problem with the tuner, not a need to tune longer.
- **Tuning at full power.** 1500 W into the antenna for tune-up is unnecessary and dramatically more disruptive than 50 W into the antenna for tune-up.
- **Tuning before checking the band.** "Is the frequency in use?" applies to tuning, too. Listen first.
- **Tuning into a contest.** During a major contest, every 100 Hz of band is in use. Tune-up isn't possible in the active part of the band; do it elsewhere or use dummy load.
- **Forgetting to identify.** Continuous carrier without identification, even for tune-up, is technically a violation of FCC §97.119 (identification required during transmissions). A brief callsign before and after the tune satisfies the rule.
- **Tuning across a band edge.** If your antenna isn't trimmed for the band you're on, tuning forces the tuner to compromise. Trim the antenna for the operating range first; don't expect the tuner to make a 14 MHz dipole work on 7 MHz.

> ⚙️ **Advanced —** Modern autotuners use TX-side relay banks and a mismatch detector loop to find the impedance match. The detector typically operates at 5-10 W and converges in 1-3 seconds for a "memory" frequency (the tuner has cached the previous match for this band). For a fresh frequency the search may take 5-10 seconds. Older "T-network" tuners with manual capacitors and inductors require continuous TX during the adjustment, taking 10-30 seconds per band. For homebrew or kit tuners without the relay-bank technology, dummy load is even more important — you're going to be transmitting for a while.

## See also

- §22-01 — "Is the frequency in use?" (the listening protocol that applies before tuning too)
- §09-15 — NanoVNA Trim Workflow (low-power on-air alternative to tuning)
- §10-02 — SWR & Reflected Power (what tuning is actually adjusting)
- §13-04 — RF Feedback (what bad tune-ups cause)
- §17-07 — SWR formula
