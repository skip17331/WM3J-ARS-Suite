---
id: 10-00
title: Feedline & SWR — Overview
chapter: 10
section: 00
level: simple
status: draft
---

# Feedline & SWR — Overview

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The feedline is the cable between your radio and your antenna. It looks passive — just a wire — but it's the single piece of your station most likely to silently steal performance. A great antenna with a poor feedline is mediocre. A mediocre antenna with a great feedline is mediocre. The whole *system* matters; getting the feedline right is half of the problem.

This chapter covers the relationship between SWR (Standing Wave Ratio), feedline loss, and what actually arrives at your antenna. The math here is the foundation of antenna-system design and is referenced by §06-10 (feedline effects on antenna behavior), §11 (power budget), §12 (high-SWR troubleshooting), and §18 (coax & connectors reference).

## How the chapter is organized

| § | Topic | What it covers |
|---|-------|----------------|
| 09-01 | Coax loss by frequency | How attenuation grows with frequency, and per-cable curves |
| 09-02 | SWR and reflected power | What SWR means, how it relates to forward and reflected power |
| 09-03 | Mismatch loss | Power lost specifically due to impedance mismatch |
| 09-04 | Power delivered vs. lost | Tracking watts through the entire feedline + matching system |
| 09-05 | Velocity factor | How fast the wave travels in coax; computing electrical lengths |
| 09-06 | Impedance transformation | How feedline length transforms the impedance you see at the rig end |

## The four numbers that describe a feedline system

| Number | Symbol | What it means |
|--------|--------|---------------|
| Matched-line loss | α | Loss when the line is terminated in its Z₀ — the "best case" |
| SWR at antenna | S | How well-matched the antenna is to the feedline at the antenna end |
| Total feedline loss | L_total | Matched loss plus extra loss caused by mismatch |
| Power delivered | P_load | Watts actually radiated by the antenna |

For a "perfect" system: matched-line loss is the only loss, SWR = 1:1, power delivered = transmitter power minus matched loss. This rarely happens in practice; understanding why and by how much you fall short of this ideal is what this chapter is for.

## A motivating example

You have a 100 W transmitter, 100 ft of RG-8X feeding a 20 m dipole. Look up matched-line loss in §18-02: RG-8X at 14 MHz is 1.0 dB/100 ft. So the dipole sees about 100 W − 1.0 dB = 79 W in the perfect case (1:1 SWR).

Now suppose the antenna is mismatched, presenting 3:1 SWR. Looking up §10-03's mismatch loss table: an additional ~0.5 dB. Total loss is 1.5 dB. The dipole sees 100 W − 1.5 dB = 71 W.

You went from 79 W to 71 W because of mismatch. **8 W lost** specifically to SWR-induced extra loss.

Now scale up: 1500 W transmitter, same setup. 1500 W − 1.5 dB = 1060 W at the antenna. The 0.5 dB mismatch loss costs you about 120 W of radiated power. Same proportion, bigger absolute number.

The chapter unpacks this: where the loss goes, how to measure it, and what to do about it.

## What's not in this chapter

- **Per-cable specs (loss/100 ft, VF, dimensions)** — see §18 for the complete reference table.
- **Connector types and how to install them** — see §18-04.
- **Baluns and chokes** — see §06-12 (full discussion) and §18-05 (reference).
- **Specific feedline failure modes** — see §12-01 (coax issues) and §12-07 (water ingress).
- **Antenna behavior** — see §06.

## Where the suite helps

- **J-Hub Antenna Tab** computes SWR-vs-frequency from a sample sweep and displays the matched-line loss for your selected feedline.
- **§09 (Antenna calculator)** uses these formulas to compute physical lengths.
- **NanoVNA / antenna analyzer** — the practical tool for measuring SWR at multiple frequencies.

## See also

- §06-10 — Feedline effects on antenna behavior
- §06-11 — Impedance transformation (companion to §10-06)
- §11 — Power budget and ERP (uses these numbers in system-level context)
- §12 — High-SWR troubleshooting (when something goes wrong)
- §18 — Coax & connectors reference
