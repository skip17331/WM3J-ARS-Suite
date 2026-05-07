---
id: 14-01
title: No Transmit
chapter: 14
section: 01
level: simple
status: draft
---

# No Transmit

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

You key the mic and nothing happens. No power output. No "TX" indicator on the radio. Or the radio shows TX but no power reaches the antenna.

This section walks the most likely causes in roughly the order they happen.

## Step 1 — Does the radio go into TX mode at all?

Watch the radio's TX/RX indicator (usually an LED labeled "TX" or a meter that switches function). When you key the mic:

- **Indicator stays in RX:** PTT signal isn't reaching the radio. Causes:
  - Mic not plugged in fully (a partially seated microphone connector).
  - Mic PTT switch broken (try a known-good mic).
  - PTT line in the mic cable broken (continuity check with a multimeter).
  - Foot switch / VOX / external PTT source not configured.
  - Software (Hamlib, FlRig, etc.) not connected; PTT-via-CAT is failing silently.

- **Indicator switches to TX but no audio:** PTT is fine; mic audio is the problem (see §14-03).

- **Indicator switches to TX and audio is present but no RF output:** PA stage or output network problem (continue to step 2).

## Step 2 — Is power reaching the PA?

If the radio enters TX but produces no RF:

- **Check the PA fuse.** Many radios have a separate PA fuse on the back, often blown by transient SWR or a bad antenna load. Replace.
- **Check the PA voltage rail.** With a multimeter on the PA voltage test point (consult the service manual), you should see the radio's nominal supply (12.0–13.8 V for most HF rigs).
- **Listen for relay click.** Modern radios use solid-state TX/RX switching; older ones use relays. A quiet relay = it's not switching. A dead relay (no click) means the relay coil failed or the driver transistor died.

> ⚙️ **Advanced —** Modern transceivers with solid-state TX/RX switching use PIN-diode pairs that need a small DC bias to switch. A failed bias supply can cause "TX mode but no RF output" with no obvious indicator. The service manual will document the bias voltage at each diode; ~+5 V or +12 V are typical depending on design.

## Step 3 — Is the ALC pegged?

The Automatic Level Control on most rigs prevents PA overdrive by reducing power if input drive is too high. If ALC is pegged at maximum:

- Power output may drop to 0 W or near it.
- The ALC meter will show full deflection.

Causes:
- Mic gain set too high (try lowering it 30%).
- Compressor or speech processor on with too much gain.
- External audio source (sound card) driving the radio at line level instead of mic level.

Lower the input drive until ALC reads in the normal range (radio-specific; usually a marked range on the meter), then increase carefully. Power output should rise as ALC reduces.

## Step 4 — Is the antenna selector right?

Most modern radios have multiple antenna ports. Some support automatic selection per band; some don't.

- Confirm the front-panel antenna selector matches the antenna actually connected.
- Look for "ANT 1" / "ANT 2" indicators.
- Some rigs require manual antenna selection per-memory or per-band.
- A dummy load on ANT 1 won't transmit through ANT 2 to your real antenna.

Also: **antenna tuners** sometimes need to be in TUNE or BYPASS depending on the moment. A tuner waiting for "TUNE" command may be sitting in a high-loss state.

## Step 5 — Is there a software interlock or transmit inhibit?

Software-controlled stations (Hamlib, FlDigi, JTDX, WSJT-X, J-Bridge) sometimes set transmit inhibits that the user forgets about:

- **WSJT-X "Disable Tx"** button — checks itself when a QSO completes; many beginners stay in disable mode unaware.
- **TUNE function** in the rig is enabled — radio is sending a tune carrier but not your audio.
- **CW key-jiggle** — the rig thinks the keyer is sending and refuses to TX from mic.
- **External CAT control** has set TX inhibit via Hamlib commands (rare, but possible).

Disconnect all software. Try TX from the mic alone. If TX works now, the software was the cause; reconnect one tool at a time.

## Step 6 — Is the band selected one your radio supports?

Some rigs (especially imports for the wrong region) won't transmit on bands you didn't expect. A radio sold in Europe may refuse to TX on the US 60 m channels; a US-region rig may refuse on 4 m.

Check the menu for "Region" / "Type" settings.

## Step 7 — Is the rig in receive-only mode?

A few possibilities:

- **General coverage receive** — most modern rigs receive 100 kHz to 30 MHz but only transmit on amateur bands. If you're on, say, 12.000 MHz (BBC shortwave), you'll receive but not transmit.
- **Receive-only memory** — some rigs let you mark memories as RX-only.
- **Out-of-band** — many rigs have an "out-of-band TX disable" that prevents transmitting outside ham allocations.

## Step 8 — Is something physically blocking the antenna line?

Rare but real:
- **Lightning arrestor** that fired and shorted; replace.
- **Manual coax switch** in the wrong position; check.
- **Broken UHF connector on a barrel** somewhere in the line.
- **Tuner in BYPASS** but with internal contacts welded open from a high-SWR event.

## When everything checks out and TX still doesn't work

If you've worked through all eight steps:

- Try the radio at a friend's station with their antenna and power supply. If it works there, the problem is in your station; if it doesn't, the radio has an internal issue.
- Bring the radio to a competent technician. PA-stage repairs (replacement of finals, alignment of bias networks, etc.) are not beginner work.

## Recovery checklist after diagnosis

When you fix the problem, document what it was. A simple shack notebook entry:

```
Date:       2026-03-12
Symptom:    No TX on any band
Cause:      Mic PTT switch broken (intermittent)
Fix:        Replaced mic
Prevention: None — mic switches wear out, this was 8 years old
```

Three years from now you'll thank yourself for these notes.

## See also

- §14-02 — no receive (often diagnosed with the same tools)
- §14-03 — distorted audio (related — TX works but sounds bad)
- §14-06 — power supply (the source of many "no TX" problems)
- §13 — high SWR (which can also produce "no TX" via fold-back protection)
