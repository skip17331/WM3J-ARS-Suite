---
id: 09-07
title: EFHW (No Traps)
chapter: 09
section: 07
level: mixed
status: draft
---

# EFHW — End-Fed Half-Wave (No Traps)

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What it is

The end-fed half-wave (EFHW) is a half-wave wire fed at one end through a high-ratio impedance transformer ("unun" — unbalanced-to-unbalanced transformer). Because a half-wave wire's end-impedance is on the order of 2000–3000 Ω, a **49:1 or 64:1 unun** transforms it to 50 Ω.

The killer feature: an EFHW resonant on its fundamental is also resonant on its **2nd, 3rd, and 4th harmonics**. So a 40m EFHW (~67 ft) covers 40 / 20 / 15 / 10m without traps and without a tuner. Only one support is needed (the wire goes from the unun outward), making it the popular choice for backyards, attics, and POTA / SOTA.

## How it works

A half-wave dipole's voltage is high at the ends and low at the center. The end-impedance is voltage² / power = several thousand ohms. Fed at this point with a 49:1 unun, the **2450 Ω end-impedance maps to 50 Ω** at the coax connection.

Because the harmonic resonances of a half-wave wire (2×, 3×, 4× the fundamental) **also have voltage maxima at the ends**, the same unun match works on all the harmonic bands — though the impedance varies slightly band-to-band, the SWR stays acceptable.

The empirical length formula for an EFHW with a 49:1 unun is shorter than the standard half-wave formula (478 vs. 468) — the unun's parallel capacitance pulls the resonance slightly:

```
length(ft) = 478 / freq(MHz)
```

A **counterpoise wire** (typically 5% of the antenna's length) tied to the unun's ground side gives the antenna its other "half" of the radiation system. Without it, the coax shield carries unbalanced common-mode current, with all the consequent grief.

## Calculator inputs and outputs

The Antenna Workshop calculator (`EFHW (No Traps)`) takes:

- **Lowest band frequency** (MHz) — the antenna is half-wave at this freq
- **Unun ratio** — 49:1 (most common), 64:1, or 56:1

And returns:

- Total wire length, counterpoise length
- Expected end-impedance for the chosen unun
- Unun primary:secondary turns recommendation
- Harmonic resonance table (which bands you'll cover)

## Worked example — 40m EFHW for 40 / 20 / 15 / 10m coverage

```
freq = 7.150 MHz
unun: 49:1 (standard)

wire length: 478 / 7.150 = 66.85 ft  (~67 ft)
counterpoise: 67 × 0.05 = 3.35 ft  (~40 inches)
expected end Z: ~2450 Ω

Unun: 2 turns primary, 14 turns secondary on FT240-43 toroid
      (49:1 ratio = 7² turns ratio)

Harmonics covered:
  1× (7.15 MHz)  → 40m
  2× (14.30 MHz) → 20m
  3× (21.45 MHz) → 15m
  4× (28.60 MHz) → 10m
```

Each harmonic resonance is slightly off-center within its band, but SWR ≤ 2:1 across the standard amateur portions is typical. WARC bands (30/17/12 m) are odd harmonics and need a tuner.

## Common mistakes

- **No counterpoise.** The most common cause of "EFHW that doesn't work right." Without a counterpoise, the coax shield radiates and patterns become unpredictable.
- **Counterpoise too long.** A full-length quarter-wave counterpoise creates a different antenna — the harmonic structure breaks. Stick to ~5% of the wire length.
- **Wrong unun.** The 49:1 standard requires 7² (49) impedance ratio. For 64:1 use 8² turns ratio. Mismatched unun = high SWR or excessive heating.
- **Too few primary turns.** A 49:1 unun needs **at least 2 turns** on the primary; 3 turns is better for full power. Single-turn primaries saturate at high power and burn up.
- **Skipping the bench test.** Verify the unun's ratio with a known load (a 2200 Ω resistor across the secondary should read 50 Ω at the primary) before installing.
- **Bending the wire sharply.** A right-angle bend at ~30% from the unun (the high-current point) costs 1–2 dB. Keep the wire reasonably straight.

> **Advanced —** The "478" constant in the EFHW length formula is empirical and accounts for the unun's parallel capacitance plus typical end-effect on a fed-end wire. Different unun construction (turns count, core material, capacitance compensation cap across the primary) can shift this constant by ±2%. Always verify with an analyzer; trim shorter as needed.

## Build & trim notes

1. **Build the unun first.** Use FT240-43 toroid (43 mix is standard; 31 mix works for 80–40m, 52 mix for 40m+). Wind 2 turns primary (heavy wire #14), 14 turns secondary (typical #18 enameled).
2. **Bench-test** the unun: 2200 Ω resistor across secondary should read close to 50 Ω at the primary across 7–28 MHz.
3. **Cut wire to 478 / f formula** and add 3–5 ft for trim margin. Wind the counterpoise tight to the unun ground side (a length of stranded wire works fine).
4. **Install with one good support** — the high-impedance end (unun) is at the rig side; the low-impedance free end can be at modest height (8 ft is fine).
5. **Sweep all 4 harmonic bands** — adjust wire length for best compromise across all bands.
6. **Re-trim seasonally** — winter cold and summer heat shift resonance ~2–3% in some climates.

## See also

- §06-13 — EFHW (theory chapter — related single-band variant)
- §09-00 — Antenna Workshop overview
- §09-08 — EFHW (Trapped) — the multi-band variant with traps
- §09-15 — NanoVNA Trim Workflow
- §17-04 — Impedance (where the high end-Z comes from)
- §18 — Coax & Connectors (for the coax run)
