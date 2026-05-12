---
id: 09-15
title: NanoVNA Trim Workflow
chapter: 09
section: 15
level: simple
status: draft
---

# NanoVNA Trim Workflow

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What it is

The NanoVNA Trim Workflow is the **systematic procedure** for taking an antenna from "calculator says 33.07 ft" to "actually resonant on the band you want." Every antenna in this chapter ends with the same instruction: "trim with an analyzer." This section is what that instruction means in practice.

A NanoVNA — or any vector network analyzer — measures the antenna's complex impedance across a frequency sweep. From that you get **resonant frequency**, **SWR**, **bandwidth**, and (for advanced cases) the **Smith chart trace**. With these, you can systematically trim wire, slide the feedpoint, adjust traps, etc. — instead of guessing.

This isn't a NanoVNA tutorial; it's the workflow that turns the analyzer's data into design changes.

## The hardware

- **NanoVNA** (any of the H4 / V2 / SAA-2 variants) — $50–$150
- **NanoVNA-Saver** software (free, runs on PC) — gives larger plots and CSV export
- **Calibration kit** (open / short / 50 Ω load) — usually included with the analyzer

A handheld NanoVNA at the antenna's feedpoint is the practical setup. Mark frequency, SWR, R, and X readings; export the sweep as CSV for later analysis.

## The standard workflow

### Step 1 — Bench check first

If the antenna has any **manufactured components** (traps, ununs, baluns, matching stubs), test them on the bench with the NanoVNA before installation:

- **Trap**: connect across the analyzer ports. Sweep — confirm resonant frequency (high-Z dip in S21 if using a transmission-line setup, or high-Z peak in reflection).
- **Unun**: connect a known load (e.g., 2200 Ω resistor for 49:1) on the secondary; verify the primary reads near 50 Ω.
- **Balun**: connect to a 50 Ω load; verify primary impedance and common-mode rejection.

This catches manufacturing errors **before** they're 30 ft up a tree.

### Step 2 — Install at low height for rough trim

Mount the antenna at **half the operating height** (or as low as practical) for the rough trim. Sweep the analyzer:

- **If resonance is below the target frequency** — antenna is too long. Trim equal amounts from both ends (dipole) or from the tip (vertical / EFHW).
- **If resonance is above the target** — antenna is too short. Add length (splice or use longer wire).

A typical 20m dipole drops resonance by ~10 kHz per cm of total length (5 mm per side). So 100 kHz off resonance = trim 5 cm per end.

### Step 3 — Raise to operating height

Pattern, gain, and **resonant frequency** all change with height. Sweep again at full operating height — typically the resonant frequency shifts up 50–200 kHz from the low-height check.

### Step 4 — Final trim

Iterate Step 2 at full height until the resonant frequency is at your target (usually band center; for FT8-only operations, the FT8 sub-band).

### Step 5 — Document the baseline sweep

Save the final sweep as a **baseline** for future comparison. Six months from now if SWR has shifted, you can compare current sweep to baseline and spot:

- **Drift down in frequency** → antenna lengthened (water absorption, balun saturated, etc.)
- **Drift up** → antenna shortened (corrosion at a connection, broken splice)
- **SWR worsened across-band** → connector or balun problem
- **Resonance disappeared** → element or feedline failure

Per **J-Vault** (Shack Inventory), store sweeps in `/shack-data/sweeps/A-001-2026-04.csv` (or wherever you organize them).

## Reading the sweep

A clean half-wave dipole sweep looks like a **deep V-shape**:

```
SWR
 │           min SWR  
 │      ╱─────╲────────
 │     ╱        ╲
1│────╱          ╲────
 │
 └────────────┬────────  freq
              f_resonance
```

Key features:

- **Minimum SWR** — the bottom of the V. Should be ≤ 1.5:1 for a properly-built dipole.
- **Resonant frequency** — the freq at minimum SWR (the V-bottom).
- **Bandwidth** — the freq range where SWR ≤ 2:1 (the V-width). Wider V = more usable bandwidth.

Common deviations:

| Pattern | Likely cause |
|---------|--------------|
| **Deep V at wrong frequency** | Length mismatch — trim or add wire |
| **Shallow V (high min SWR)** | Mismatch (wrong feed Z) — different balun or geometry |
| **Multiple SWR minima** | Multi-band antenna behaving correctly — verify expected resonances |
| **No clear V (random SWR)** | Damaged feedline, water in connector, broken element |
| **V at expected freq but high SWR away from band** | Narrow bandwidth — antenna is fine but only at center |

## Trim direction reference

| Antenna | Resonance too low (long)? | Resonance too high (short)? |
|---------|---------------------------|------------------------------|
| Flat dipole | Trim equal from both ends | Add wire (splice insulators) |
| Inverted-V | Trim equal from both ends | Add wire or steepen droop |
| EFHW | Trim from far end | Add wire |
| Vertical | Trim from top | Add length to top |
| Yagi DE | Trim equal from both ends | Add length |
| Trap | Adjust trap C value | Adjust trap C value |

For a dipole at 20m, **1 cm of wire (per side) shifts resonance ~10 kHz**. This scales linearly with band — at 80m it's ~3 kHz per cm; at 6m it's ~50 kHz per cm.

## Common mistakes

- **Trimming at low height.** Antenna's resonance shifts when raised. Always do final trim at operating height.
- **Cutting too much at once.** It's easier to take 2 cm off than to splice 2 cm back on. Trim small amounts (1–2 cm per pass).
- **Skipping the bench test.** Installing untested traps and ununs costs hours of climbing later. Bench verification is fast and prevents disappointment.
- **Using SWR meter only.** SWR alone can hide problems — a 1:1 reading at the rig with 6 dB feedline loss could mean an open at the antenna. NanoVNA at the **feedpoint** gives the true picture.
- **Not saving baselines.** Without a baseline sweep, you can't spot drift months / years later. Save every install's final sweep.
- **Trimming on a windy day.** Wind moves wire; readings shift. Trim in calm weather; sweep on calm days for baselines.

> ⚙️ **Advanced —** For matching-network design (Smith chart work), the NanoVNA + NanoVNA-Saver provides a real-time Smith chart. Verify your match-network's transformation graphically — saves time vs. closed-form calculation when iterating on a tricky design. The chart also reveals mismatches that a reflection-magnitude-only meter would miss (e.g., resistive loads at the wrong impedance vs. reactive loads at the same magnitude).

## See also

- §06-09 — Smith Charts (for matching-network analysis)
- §09-00 — Antenna Workshop overview
- §12 — High-SWR Troubleshooting (when the sweep is unexpected)
- **J-Vault** — Antennas (where to store baseline sweeps in inventory)
- §17-04 — Impedance (the math behind R + jX readings)
- §17-07 — SWR (the math behind SWR readings)
- §17-13 — Smith Chart Basics (interpreting the chart)
