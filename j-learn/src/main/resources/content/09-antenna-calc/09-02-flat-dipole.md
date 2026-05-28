---
id: 09-02
title: Flat Dipole
chapter: 09
section: 02
level: simple
status: draft
---

# Flat Dipole

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What it is

The flat dipole is the simplest, cheapest, and most-built ham antenna: two equal lengths of wire fed in the middle, strung horizontally between two supports. Half-wave resonant on one band; usable feed impedance close to 50 Ω with a 1:1 balun.

Almost every operator builds one of these as a first antenna. It works.

## How it works

A half-wave dipole at its design frequency presents a current maximum at the center (where it's fed) and voltage maxima at the ends. The end-fed voltage is high enough that the wire's ends couple capacitively to surrounding objects — this slightly lowers the resonant frequency, requiring the antenna to be cut **about 5% shorter than free-space half-wavelength**.

The classical cut formula:

```
length(ft) = 468 / freq(MHz)
length(m)  = 142.5 / freq(MHz)
```

The "468" embeds the 5% end-effect correction for thin wire well above ground (see §17-06). For thick aluminum tubing or wire near other conductors, the constant shifts to 460–475.

Feed impedance at resonance is about **73 Ω in free space**, dropping toward 50 Ω as the antenna is mounted closer to ground. At λ/2 height, expect ~70 Ω; at λ/4 height, ~50 Ω; below λ/8 height, drops sharply.

## Calculator inputs and outputs

The Antenna Workshop calculator (`Flat Dipole`) takes:

- **Frequency** (MHz) — band center
- **Length factor (k)** — 468 default, adjustable for thick conductor / near-conductor cases
- **Display units** — feet or meters

And returns:

- Total length and per-leg length
- Free-space half-wavelength (no end effect)
- Expected feed impedance with height-correction note

## Worked example — 20m dipole at moderate height

```
freq = 14.150 MHz
k = 468

length = 468 / 14.150 = 33.07 ft (10.08 m)
each leg = 16.54 ft (each side from center insulator)
free-space λ/2 = 492 / 14.150 = 34.77 ft (no end effect)

end-effect correction = 1.70 ft (5%)
```

For a clean 50 Ω match, mount at least 33 ft (1 wavelength) above ground. For a typical backyard install at 20–30 ft height, expect ~50–60 Ω feed Z and a 1.5:1 SWR — entirely usable without a tuner.

## Common mistakes

- **Using the free-space formula** (492/f) and getting an antenna that resonates 5% too high in frequency. The 468 (or 142.5 metric) constant is empirical; use it.
- **Skipping the balun.** A 1:1 current balun at the feedpoint suppresses common-mode current on the coax shield. Without one, the coax becomes part of the antenna and the pattern distorts.
- **Mounting too low.** Below λ/8 (about 8 ft on 20m) the feed Z drops below 25 Ω, ground losses spike, and the antenna radiates mostly straight up.
- **Trimming the wrong end.** Both legs must remain equal length. Trim equal amounts from both ends or you'll skew the radiation pattern.
- **Ignoring nearby conductors.** A gutter or downspout 5 ft from the dipole can shift resonance by 100+ kHz.

> **Advanced —** A horizontal half-wave dipole at height *h* has an idealized radiation pattern with peak at zenith for *h < λ/4*, peak at horizon for *h ≈ λ/2*, and develops lobes at higher heights. The half-wave-over-ground feed impedance is ~73 Ω at infinite height, dropping to ~30 Ω at λ/4 height for a perfect ground; real ground (lossy) modifies this and adds a few ohms of loss resistance.

## Build & trim notes

1. **Cut the wire about 5% long.** It's easier to trim shorter than to splice longer.
2. **Solder a 1:1 current balun** to the center insulator; bring the coax straight down at right angles to the wire to minimize common-mode current.
3. **Sweep with an analyzer** (NanoVNA, RigExpert) before final installation. Rough trim outdoors at low height; final trim after raising to operating height.
4. **Trim equal amounts from both ends.** ~1 cm on each end shifts 20m resonance by ~10 kHz.
5. **Re-seal the balun** after each maintenance — water in the balun is the #1 dipole killer.

## See also

- §06 — Antennas (theory chapter for dipole physics)
- §09-00 — Antenna Workshop overview (what a calculator is)
- §09-15 — NanoVNA Trim Workflow (the systematic build-and-trim procedure)
- §17-06 — Wavelength (where 468 / 234 come from)
- §17-04 — Impedance (feed-Z math)
