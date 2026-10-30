---
id: 15-14
title: SDR ID
chapter: 15
section: 14
level: simple
status: published
---

# SDR ID

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A software-defined radio with a waterfall display gives you a visual fingerprint of power-line noise that can confirm the diagnosis and (sometimes) help with direction-finding. Pair this with the AM sniffer (§15-13) for the most complete tools.

## Why SDR helps for power-line problems

The waterfall shows:

- **The 60 Hz / 120 Hz pulse pattern** characteristic of arcing.
- **The broadband nature** spanning from LF up into HF.
- **Time variation** as the arcing changes intensity.
- **Distinctive shape** that's easy to differentiate from household RFI.

You can confirm "this is power line noise" rather than "this is something else" with much higher confidence than from sound alone.

## Setting up

Same hardware as for household RFI hunting:

- An RTL-SDR Blog v3 ($35) or similar.
- A laptop with SDR software (SDR#, GQRX, CubicSDR).
- A short antenna (whip or telescoping) for portable use; or your station antenna if you're at the radio.

## Recognizing power-line patterns

### Insulator arc signature

```
Frequency:   ─────────────────────────────────
                     bright haze across band

Time ↓       Brightness pulses at 60/120 Hz
             (visible as horizontal stripes)
```

Look for:
- Broadband brightness across the entire visible spectrum.
- Pulsation at the AC line rate (60 Hz US, 50 Hz elsewhere).
- Slight temporal variation but generally constant intensity.

### Transformer signature

Similar to insulator arc but with:
- Stronger 60/120 Hz pulse (more periodic).
- Often less broadband — energy concentrated in lower bands.
- More constant in time.

### Loose hardware signature

- Often more irregular than insulator arcs.
- May come and go more, especially with wind.
- Same broadband character.

### Corona signature

- More even across the spectrum than arcing.
- Less pulsation (corona is more continuous than arcing).
- Strong weather correlation.

## Direction-finding with SDR

For portable hunting, an SDR + laptop + small directional antenna combination is more powerful than the AM sniffer alone:

1. **Open the waterfall** at your laptop.
2. **Walk to a pole.** Watch the waterfall.
3. **Rotate the antenna.** Note the orientation that maximizes the noise level.
4. **The peak orientation** points at the source.

The SDR's waterfall lets you see weak changes that the AM radio's audio output would mask. You can also see the spectral character of the noise to confirm it's really power line noise rather than coincidental household RFI.

## Recording for utility complaints

The SDR's waterfall is invaluable evidence in a utility complaint. Some utilities have started accepting:

- **Screenshot waterfalls** showing the noise pattern.
- **IQ recordings** of the noise (some utilities have analysts who can look at this).

When you file the complaint (§15-15), include screenshots:

- Before walking to the pole — showing the noise at your station.
- At the suspect pole — showing the noise level there.
- Away from the pole — showing the noise drops.

This visual evidence is much harder for a utility to dismiss than "I hear a buzz."

## A specific use case

For an intermittent source you can't catch in real time:

1. Set up the SDR at your station, recording continuously to disk.
2. Wait for the noise to recur.
3. Note the exact time.
4. Review the recording to confirm pattern and intensity.

This works well for sources that are weather-dependent — record over several rainy/dry/foggy days and correlate the intensity to weather.

## Alternative: handheld RFI direction-finder

For serious RFI hunting (utility employees doing this for a living), there are commercial tools:

- **Radar Engineers Model 240** and similar — a calibrated portable receiver with built-in directional antenna and logarithmic display. Cost: $5k+.
- **TI Industries Radar Engineers** RFI sniffers — purpose-built for utility EMI work.

For amateurs, a portable SDR with a small loop antenna is a fraction of the cost and works fine for the types of noise we encounter.

## See also

- §14-08 — SDR waterfall for general RFI hunting
- §15-13 — AM radio identification (companion technique)
- §15-15 — utility documentation procedure
