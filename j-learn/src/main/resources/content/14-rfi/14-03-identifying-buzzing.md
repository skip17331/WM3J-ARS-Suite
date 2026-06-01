---
id: 14-03
title: Identifying Buzzing
chapter: 14
section: 03
level: simple
status: published
---

# Identifying Buzzing

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Half of RFI diagnosis is just **listening carefully**. Different sources have characteristic sounds, time patterns, and frequency footprints. With practice you can identify many sources from a few seconds of audio without leaving the radio.

## A taxonomy of noise sounds

### Steady hash / "frying bacon"

A continuous, even, broadband hash. No rhythm. Sounds like static but doesn't change with band noise.

**Likely sources:**
- Switching power supply (most common)
- Plasma TV
- Solar inverter (if daytime-only)
- LED light driver

**Distinguishing feature:** it's the *same* sound across many frequencies, fades in and out as you tune across resonances of the source's harmonics.

### 60 Hz buzz

A clearly-pitched buzz at the 60 Hz line frequency. Often with harmonics at 120, 180, 240 Hz.

**Likely sources:**
- AC power line problem
- Transformer with loose laminations
- Bad AC contact (corroded outlet, loose wire)

**Distinguishing feature:** the pitch is exactly 60 Hz (or 50 Hz outside the Americas).

### 120 Hz buzz

Similar to 60 Hz but at twice the frequency.

**Likely sources:**
- Doorbell transformer
- Old fluorescent ballast
- Half-wave rectifier somewhere

**Distinguishing feature:** specifically 120 Hz, no 60 Hz fundamental.

### Crackling / popping

Random, brief crackles, sometimes with audible "pop" character.

**Likely sources:**
- Bad insulator on a power pole (intermittent arcing)
- Loose connection somewhere on the AC mains
- Static buildup on the antenna in dry weather
- Lightning at long distance (single, very brief crashes)

**Distinguishing feature:** the time pattern is irregular. Lightning is one brief crash; arcing is a sustained crackle that may pulse.

### Modulated tone / "warbling"

A musical-pitched tone that varies in frequency or amplitude.

**Likely sources:**
- Nearby AM broadcast station leaking through the receiver front end
- Crystal oscillator in a digital device (often near specific frequencies)
- Computer monitor's switching converter

**Distinguishing feature:** has a clear tonal character; not broadband.

### "Tick tick tick" sequenced

Regular, even ticks at 1–10 Hz rate. Often quite loud, then silent for a moment.

**Likely sources:**
- LED driver in dimmed state
- Some HVAC controls
- Some smart-home devices syncing
- Ignition system (if you're near a road and a car drives by)

**Distinguishing feature:** the rhythm is very regular. Time the ticks per second.

### Bursty interference

Random or periodic bursts of hash, each lasting a fraction of a second to a few seconds.

**Likely sources:**
- Powerline networking (Ethernet over power)
- Wi-Fi mesh node syncing
- Cellphone tower paging burst
- Computer hard drive seeking

**Distinguishing feature:** intermittent. Often correlates with specific user actions in the house.

### Whoosh / fluctuating noise

A noise that comes and goes on a slow timescale (seconds to minutes).

**Likely sources:**
- Atmospheric noise (lightning storm at distance — natural, not RFI)
- Pulsing motor (HVAC blower with PWM control)
- Charger that adapts its switching rate

**Distinguishing feature:** correlates with weather (atmospheric) or with operating cycle (motor).

### Whining at a specific pitch

A continuous tone at one specific pitch (e.g., 1.2 kHz, 2.7 kHz, 8 kHz).

**Likely sources:**
- Computer monitor
- LED driver
- USB charger
- Cheap audio equipment with switching supply

**Distinguishing feature:** pitch is constant; localized to specific bands where the source's fundamental frequency falls.

## Time-pattern analysis

Watch the noise for a minute and note when it changes:

| Pattern | What it suggests |
|---------|------------------|
| Constant 24/7 | Always-on device (router, fridge, smart speaker) |
| Day only | Solar PV inverter |
| Night only | Furnace; pool pump; well pump |
| When dishwasher runs | Dishwasher motor |
| When heating activates | Furnace blower or HVAC controller |
| Periodic, every 30 min | Sync interval of a smart-home device |
| Random bursts | Network device transmitting |
| Strong, then absent | Charger plugged in vs unplugged |
| Worse after rain | Power-line insulator wet (corona) |
| Worse in cold | Loose connection contracting |

A good operator keeps a notebook: date, time, observed noise, weather, what was happening in the house.

## Frequency-pattern analysis

Sweep the bands and note where the noise is strong:

- **Strong on 80 m, weaker as you go up** = typical of switching supply hash. Most switching supplies have fundamental switching at 50 kHz to 1 MHz; the harmonics get weaker at higher frequencies.
- **Strong only on 20 m and 15 m** = something resonant in those bands; could be a wire of specific length acting as an unintentional radiator.
- **Strong in narrow ranges, quiet between** = harmonics of an oscillator. Note the exact frequencies; the spacing tells you the fundamental.
- **Strong everywhere uniformly** = broadband source like a plasma display or a powerline arcing problem.

## Tools for sound identification

### Headphones

Use headphones for serious diagnosis. Speaker audio rolls off bass and changes the tonal character of the noise. A pair of decent over-ear headphones makes the noise's character much more apparent.

### A spectrum analyzer or SDR

A waterfall display shows you the time and frequency pattern at once. Bursty sources are obvious; constant sources show as horizontal lines; tonal sources show as bright vertical lines. §14-08 covers this in detail.

### A recorder

Record the noise and play it back later, or send it to someone for diagnosis. The amateur RFI subreddit and several mailing lists have communities that can identify noise from a 10-second clip.

### An AM radio

A cheap portable AM radio held near suspect devices is the oldest and still one of the best direction-finding tools. Detail in §14-07.

## Practice

If you don't have an RFI problem right now, train your ear by deliberately turning on suspect devices and listening to what they sound like:

- Plug a cheap USB charger into the wall, turn on the radio, listen to the resulting hash on 80 m.
- Turn on a TV, then turn it off — note the change.
- Run a dishwasher; listen to the noise pattern as the motor cycles.

After a few weeks of paying attention, you'll recognize household device noise reflexively when you encounter it on the air.

> **Advanced —** Source identification by sound is sometimes formalized in commercial RFI investigation. The military uses standardized recordings of known emitters as reference; some utility companies use "EMI fingerprints" derived from time-frequency analysis of known equipment. For amateur use, building your own reference library of household noise sounds is a worthwhile exercise — name them in your notes, listen for patterns, and the on-air noise becomes much less mysterious.

## See also

- §14-02 — household sources by category
- §14-07 — AM radio sniffer for direction-finding
- §14-08 — SDR waterfall for visual diagnosis
- §15 — detailed source-by-source guides
