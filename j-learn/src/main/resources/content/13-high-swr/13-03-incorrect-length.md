---
id: 13-03
title: Incorrect Length
chapter: 13
section: 03
level: simple
status: draft
---

# Incorrect Length

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A resonant antenna is **resonant at one frequency** — set by its physical length. If the length is wrong, the resonance is on the wrong frequency, and the SWR on your operating frequency will be high. This is the most common cause of high SWR on a freshly-built antenna and the easiest to fix.

## Symptoms that point at length

- **High SWR on one band only** — the band you want — and acceptable SWR somewhere nearby.
- **The minimum-SWR point is shifted** — instead of dipping at 14.200 MHz like you expected, it dips at 14.700 (antenna too short) or 13.500 (antenna too long).
- **A new antenna fresh out of the box** — most factory antennas are deliberately built slightly long so you can trim them to your specific environment.
- **An antenna that worked then stopped** — possible if a wire stretched, a section was bent, or someone shortened a leg.

## The diagnostic

Sweep the antenna with an analyzer (NanoVNA, MFJ, etc.) over a wider frequency range than just the band you want. You'll see one (or several) **resonance dips**:

- **Dip below the band** — antenna is too long.
- **Dip above the band** — antenna is too short.
- **No clear dip in the expected range** — antenna isn't resonant at all (broken element, missing trap, missing balun).

The size of the shift tells you how far off you are.

## The trim-or-extend rule of thumb

For a half-wave dipole or quarter-wave vertical, length and frequency are inversely proportional:

```
new_length / old_length = old_freq / new_freq
```

Example: dipole resonant at 14.700 MHz, you want it at 14.150.

```
new_length / old_length = 14.700 / 14.150 = 1.039
```

So lengthen by about 4%. A 33-foot dipole becomes about 34.4 feet (each leg from 16.5 ft to 17.2 ft).

The classic shortcut for a half-wave dipole in free space is:

```
length (ft)  = 468 / freq (MHz)
length (m)   = 142.5 / freq (MHz)
```

Reality is always a bit shorter — the **velocity factor** of the wire and the proximity of the ground reduce the resonant length to about 0.95 × that. So a more realistic starting length:

```
length (ft) ≈ 444 / freq (MHz)
```

Plan to build a few percent long, then trim.

## How to trim safely

1. **Calculate** the difference between current resonant frequency and target frequency.
2. **Compute the trim amount** using the proportion above.
3. **Trim half the calculated amount, evenly from both legs** (for a center-fed dipole).
4. **Re-measure SWR.**
5. **Repeat** in smaller increments until you hit your target.

Always trim small. You can always cut more off; you can never put it back. (Well, you can splice it, but the splice changes the impedance slightly and looks ugly.)

For a vertical, trim only the radiator, not the radials.

## Common situations and solutions

### "Brand new antenna, SWR is 4:1 on the whole band"

Most likely the antenna is set up for a different ham band than you intended, or the manufacturer's instructions called for a tune step you skipped. Re-read the instructions. If it really is the wrong band, check the resonance frequency and either trim/extend, or use the antenna on the correct band.

### "Antenna was 1.2:1 yesterday, now it's 3.5:1"

Possibilities:
- An element broke or fell loose. Visual inspection.
- Weather: rain or ice changed the surrounding environment. Check after weather clears.
- Something was added nearby (new cable, new gutter, vegetation grew). Walk the area.
- A connection inside the matching network failed (PL-259 at the feedpoint, balun, etc.). Check connectors.

### "SWR is 1.0:1 at the antenna feedpoint but 3:1 at the radio"

Coax / connector / common-mode current issue. The antenna is fine; the feedline isn't. See §13-01, §13-02, §13-06.

### "SWR dip is in the right place but the dip isn't deep enough"

The antenna is resonant on frequency, but the impedance at resonance isn't 50 Ω. Either:
- Antenna is at the wrong height (height affects feedpoint impedance dramatically — a dipole at λ/4 over ground has very different impedance from one at λ/2).
- Wrong type of feedline (e.g., 75 Ω feedline on a 50 Ω antenna).
- Matching network out of adjustment.

A NanoVNA Smith chart shows you both resistance and reactance at resonance — if the antenna is resonant (reactance = 0) but resistance is, say, 25 Ω instead of 50 Ω, you'd see SWR of 2:1 even though the length is correct.

> ⚙️ **Advanced —** Trimming an end-fed antenna doesn't follow the same rules as a center-fed one. EFHWs use a 49:1 (or 64:1) UNUN at the feed point; resonance shifts about half as much per unit of trimmed length because the feedpoint impedance also changes with length, partially compensating. Plan trimming-by-experiment for EFHWs, not pure calculation. Multi-band antennas with traps shift very little on the lowest band when trimmed at the tip (the lowest-band element passes through the traps), but high-band resonance shifts a lot. Trim a multi-band antenna iteratively, starting from the lowest band.

## Antennas where length isn't the answer

Some antennas don't have a single "right length":

- **Magnetic loops** are tuned with a variable capacitor — the loop circumference is fixed.
- **Mobile screwdriver antennas** are tuned with a sliding coil tap.
- **Yagi/quad** — tuning is done by adjusting element spacing and director/reflector lengths together; consult the manual.
- **EFHWs** — see Advanced note above.
- **Random wire + tuner** — by design, tuner handles the matching, so length is mostly about avoiding resonant lengths that are too far off.

For these antennas, "incorrect length" maps to "tuning element out of adjustment" — different procedure but same idea.

## See also

- §05-antennas (chapter 05) — antenna design fundamentals
- §08-antenna-calc — calculators for antenna lengths
- §13-04 — nearby metal effects (often confused with length issues)
