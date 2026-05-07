---
id: 19-00
title: Coax & Connectors — Overview
chapter: 19
section: 00
level: simple
status: draft
---

# Coax & Connectors — Overview

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

This chapter is the **reference appendix** for transmission lines and the connectors that go on them. It complements §04-10 (feedline effects on antenna behavior), §08 (feedline & SWR), and §17-04 (coax replacement) by being the place you look up "what is RG-213's loss at 50 MHz" or "what's the difference between PL-259 and N."

The content is organized for fast lookup, with full tables rather than long prose. If you want to *understand* feedline behavior, read §04-10. If you want to *check* a number, you're in the right chapter.

## How the chapter is organized

| § | Topic |
|---|-------|
| 22-01 | Coax types — what each common type is, what it's good for, when to pick it |
| 22-02 | Loss tables — matched-line loss per 100 ft for every common cable, every band |
| 22-03 | Velocity factor — VF values per cable; how to compute electrical lengths |
| 22-04 | Connectors — PL-259, N, BNC, SMA, F, etc. — specs and comparisons |
| 22-05 | Baluns & chokes (reference) — quick-lookup table; full discussion in §04-12 |

## What's a "coax"?

**Coaxial cable** is a transmission line with two conductors sharing a common axis: an inner conductor (a wire or solid rod) surrounded by an insulating dielectric, surrounded by an outer conductor (braid, foil, or solid tube), surrounded by a protective jacket. The geometry constrains the electromagnetic field to the space between the inner conductor and the outer shield, so the signal stays inside the cable rather than radiating.

The four parameters that define a coax's electrical behavior:

- **Characteristic impedance Z₀** — set by the ratio of inner to outer conductor diameters and the dielectric constant. Amateur radio uses **50 Ω** almost exclusively. Cable TV uses 75 Ω.
- **Velocity factor (VF)** — the speed of an RF wave in the cable, as a fraction of the speed of light in vacuum. Solid PE dielectric: ~0.66. Foam PE: 0.78–0.85. Air-spaced (with PE spacers): 0.92.
- **Matched-line loss** — the dB/length attenuation when terminated in its own Z₀. Increases with frequency (skin effect dominates above ~100 kHz).
- **Power handling** — the maximum continuous power before insulation breakdown or thermal damage. Decreases with frequency and with elevated SWR.

Detailed values for each common type appear in §19-01 and §19-02.

## Coax in amateur radio: the common types in one paragraph

The cable types you'll see referenced repeatedly: **RG-58** (small, lossy, fine for short low-power runs), **RG-8X / Mini-8** (slightly bigger, slightly less lossy, common patch cable), **RG-213 / RG-8** (the classic 1/2-inch HF cable, low loss, available everywhere), **LMR-400** (low-loss replacement for RG-213, foam dielectric for VF 0.85, the modern HF/VHF favorite), **LMR-600** (bigger and lower-loss for long VHF/UHF runs), **9913 / 9914** (Belden's foam-dielectric LMR-400 competitors), and **hardline** (semi-rigid 1/2-inch or larger cable with copper or aluminum tube outer, used by broadcast and amateur stations for very long very-low-loss runs).

For most amateur installations, you'll have **RG-213 or LMR-400 for outdoor antenna runs** and **RG-8X for shack patch cables**.

## "Good cable" myths and realities

A few claims you'll see in catalogs or eBay listings, with reality:

- **"Mil-spec coax"** — sometimes meaningful (e.g., RG-214 is genuinely milspec with a double silver-plated braid, real spec 5.0 dB/100ft at 1 GHz). Often marketing — "RG-58 mil-spec" might just mean "complies with the original RG-58 specification, which was milspec when written in the 1940s."
- **"Silver-plated center conductor"** — real, can reduce loss by 5-10% at VHF/UHF. Mostly marginal at HF (skin depth at HF includes most of the conductor, not just the surface).
- **"Solid copper shield"** — usually means a thin solid copper tube, like hardline. Real benefit: lower loss, better shielding. Typically only available in semi-rigid / hardline construction.
- **"100% shield coverage"** — usually a foil + braid combination. Honest — the foil provides full coverage, the braid carries the high-frequency current. Better shielding than basic single-braid.
- **"Direct-burial-rated"** — has a polyethylene or polyurethane jacket designed not to absorb water and to resist soil chemistry. Real distinction; matters if you're burying cable.

## Connector summary

The amateur coax-connector world is dominated by:

- **PL-259 / SO-239** ("UHF" connector — a misnomer; it's HF-VHF only, falls apart electrically above 200 MHz). The default amateur HF connector. Cheap, widespread, easy to install. Not actually 50 Ω, not actually weather-resistant without sealing.
- **N** — true 50 Ω (or 75 Ω variants) constant-impedance connector. Weather-resistant when properly torqued. Used VHF and above; preferred for any high-power or critical match application.
- **BNC** — bayonet quick-connect, 50 Ω (also 75 Ω in TV use). Common for test equipment, instrument coax, scope probes.
- **SMA** — small threaded RF connector. Used on HTs, NanoVNAs, antenna analyzers. 50 Ω; reliable through 18 GHz.
- **F-type** — 75 Ω TV/satellite TV connector. You'll occasionally find it in amateur use, mostly with TV-coax (RG-6) repurposed antennas.
- **TNC** — threaded version of BNC. Used some in commercial / military.

Section 22-04 covers each in detail.

## "If you only remember three things"

1. **For HF antenna runs of typical length (50-100 ft), use RG-213 or LMR-400** with PL-259 connectors and a 1:1 current balun at the antenna feed.
2. **For VHF/UHF, upgrade to LMR-400 minimum**; consider hardline for runs over 100 ft.
3. **Weatherproof every outdoor connector**, every spring, with self-fusing tape + Scotch 33 outer. Water in coax is the silent killer of amateur antennas.

## See also

- §04-10 — Feedline effects (the *why* behind the numbers in this chapter)
- §04-11 — Impedance transformation
- §04-12 — Baluns and chokes (full discussion)
- §08 — Feedline & SWR
- §10-01 — Coax issues (troubleshooting view)
- §17-04 — Coax replacement
