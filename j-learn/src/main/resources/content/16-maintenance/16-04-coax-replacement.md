---
id: 16-04
title: Coax Replacement
chapter: 16
section: 04
level: simple
status: draft
---

# Coax Replacement

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Coax doesn't last forever. Every coax run is on a slow timer — UV degrades the jacket, water creeps under the connectors, repeated flexing fatigues the shield, and dielectric absorbs moisture over years. Eventually performance drops measurably: the SWR sweep starts looking different, or 10 m signals are quieter than they should be, or the same antenna performs noticeably worse than it did last year.

This section covers when to replace coax, how to replace it without making the new run worse than the old one, and what to do with the run you're pulling out.

## When to replace coax

### Symptom-driven triggers

Replace a coax run *now* if any of these appear:

- **Visible water in the dielectric.** Cut the connector off; if the white foam or solid PE is darkened, gray, or wet, water has reached it. The run is compromised. (See §10-07.)
- **Visible jacket cracking.** UV exposure has reached the dielectric or the shield. The shield will corrode through, often faster than the visible damage suggests.
- **Visible kink** that creased the dielectric. The cable's characteristic impedance is altered at the kink; SWR sweep will show a localized bump.
- **Connector pulled off in your hand** during inspection. The strain relief was inadequate, the cable has been flexing for years; replace the connector minimum, the run if there's any doubt.
- **Animal damage** — rodent chewing, bird pecking. Even if "still works," the breach is a future failure point.
- **Burn marks** at a connector or along the jacket. Indicates either a bad TX-time match (high circulating currents) or a connector arc-over. Replace the section.
- **Measured SWR sweep** has shifted significantly from baseline. Compare today's NanoVNA trace to your spring trace; if the resonance is in a different place, the loss has changed, or the shape is different — the cable has changed, not (just) the antenna.

### Calendar-driven preventive replacement

Even without symptoms, plan replacement based on cable type and exposure:

| Cable type | Outdoor lifetime | Indoor / sheltered lifetime |
|------------|-------------------|------------------------------|
| RG-58, RG-8X | 5–10 years | 15+ years |
| RG-213, RG-8 | 8–15 years | 20+ years |
| LMR-400 (and equivalents — Times Microwave) | 10–20 years | 25+ years |
| Hardline (Andrew/CommScope) | 25+ years | 40+ years |
| Direct-burial coax | 10–15 years | n/a |

These are general ranges from manufacturer field data and amateur experience; humid coastal climates shorten the upper end, dry desert installations can extend it.

The **biggest single factor** is whether the run sits in direct sunlight. A black-jacketed coax in full sun in Texas can show jacket cracking in 5 years; the same cable in a shaded run on the north side of a house in Wisconsin may go 20.

## How to assess remaining life

Three diagnostic measurements help you decide whether a run is still healthy:

### 1. SWR sweep comparison

The cleanest test. If you have a baseline sweep from when the cable was installed (which you should, per §16-03), compare today's sweep:

- **Resonance frequency** unchanged, but **broader / shallower dip**: cable loss has increased; matched-line loss is up.
- **New, sharp dip** in the middle of the sweep: a localized impedance bump in the cable (kink, water bubble, bad connector mid-run).
- **Wandering / messy SWR**: shield corrosion has advanced; common-mode currents now matter.

### 2. Cable loss measurement

With a NanoVNA and a known load (a 50 Ω termination at the far end), you can measure the cable's matched-line loss directly:

1. Connect the NanoVNA's port 1 to one end of the coax.
2. Terminate the far end with a precision 50 Ω load.
3. Sweep the NanoVNA in S11 (return loss) mode.
4. The return loss reading is **2× the cable's one-way loss** (the signal goes down, reflects off the imperfect termination — about 35 dB return — and comes back).

A new coax should show very high return loss (35+ dB) at all frequencies of interest. As the coax ages, return loss decreases — the cable is dissipating more energy, less is reflecting back.

If today's measured loss exceeds the manufacturer's spec by ~30%, the cable is at end of life. (Spec values: RG-213 at 14 MHz is 0.65 dB/100ft; an in-service piece reading 0.9 dB/100ft is ~40% over.)

### 3. TDR sweep

A NanoVNA can act as a low-resolution Time Domain Reflectometer. The trace shows reflections at every impedance discontinuity along the cable, with distance scaled by the cable's velocity factor. Look for:

- **Spike near the connector end**: connector itself (expected, small).
- **Spike in the middle**: a kink, water bubble, or junction problem.
- **Gradual rise** down the cable: distributed loss (normal aging).

> ⚙️ **Advanced —** A TDR's distance resolution is about half the wavelength of the highest frequency in the sweep. A NanoVNA sweeping to 1.5 GHz has ~10 cm resolution in coax (with VF 0.66); finer than that requires faster sampling. For locating water-ingress points or bad mid-run connectors, this resolution is plenty. The "TDR" mode in NanoVNA software (NanoVNA-Saver, etc.) does the inverse-FFT internally; the cable distance scale is automatic if you set the velocity factor correctly.

## Choosing replacement coax

When replacing, match or upgrade — never downgrade. Considerations:

| Consideration | Guidance |
|---------------|----------|
| Length of run | < 50 ft: RG-8X or LMR-240 plenty. 50–150 ft: RG-213 or LMR-400. 150+ ft: LMR-400 minimum, hardline ideal. |
| Highest frequency | HF only: RG-8X is fine. VHF/UHF: LMR-400 or better. Above 432 MHz at length: hardline. |
| Power level | Up to 200 W: most coax handles fine. Above 500 W or contesting heavily: stick with RG-213, LMR-400, or hardline. |
| Outdoor exposure | UV-resistant jacket essential. Belden 9913F7 has a UV-stable jacket; LMR-400-DB is direct-burial-rated. |
| Buried | Direct-burial-rated only. LMR-400-DB or LMR-400-UF. Standard LMR-400 will absorb water through the foam dielectric over a few years if buried. |
| Cost | LMR-400-equivalent cable from "Davis RF" or other reputable sellers is significantly cheaper than name-brand and works well for most amateur uses. |

## How to replace coax: the workflow

A typical re-coax of an antenna run:

1. **Plan the route**. Will the new run go where the old one did? Or is there a better path (less sun, less proximity to power lines)? Now is the time to improve.
2. **Buy materials in advance**:
   - The new coax (cut to length + 5 ft for trim margin)
   - New connectors (PL-259 for HF, N for UHF/VHF; one for each end)
   - Weatherproofing supplies (3M 130C self-fusing tape, Scotch 33 outer tape, dielectric grease, optional Coax-Seal)
   - Heat-shrink for strain relief
3. **Pull the new coax**, ideally before disconnecting the old one — overlap operation. Use the old coax as a pulling string if your route is constrained.
4. **Terminate one end** at the radio. Test by reading the connector's continuity (center conductor and shield should not short).
5. **Terminate the antenna end** (this often requires going up). Heat-shrink for strain relief; tape; seal.
6. **Sweep the new run** with the antenna. Compare to baseline. If significantly different from your historical baseline, you have a connector or termination problem on the new run — fix it before moving on.
7. **Disconnect the old coax** and remove it. Coil and label for disposal.
8. **Document**: log the date, the cable type, the length, the connectors used. This becomes baseline for future inspections.

## Connector workflow notes

- **Don't reuse connectors** unless you know they're known-good. New connectors are cheap; the time you save with new ones outweighs the dollar.
- **PL-259**: solder properly. The shield-soldering holes need actual heat — a 25 W iron will not do it on RG-213. Use a 60–100 W iron and clean rosin-core solder.
- **N connectors**: crimp or solder; either works. Don't mix crimp tools and crimp connectors from different manufacturers.
- **Crimp ferrules**: use the correctly-sized ferrule for the cable. RG-58 ferrules don't fit RG-213; LMR-400 has its own ferrule sizes.
- **Heat-shrink boot**: install before the connector. Slide on, terminate connector, slide boot over the connector body and shrink in place.

## What to do with the old coax

A coax run that's been replaced because it failed isn't useless:

- **Strip it for the copper.** A 100-ft run of RG-213 has roughly 5 lbs of copper in the shield, plus a solid copper center conductor. Local scrap recyclers take it.
- **Cut into short patch cables.** A 6-ft length salvaged from a failed run can be a perfectly good shack patch cable, at QRP power, for years.
- **Test pieces**: short sections (< 10 ft) make great test cables for the bench.
- **Educational**: cut sections lengthwise to show students the construction (center, dielectric, shield, jacket).

Don't reuse it for a real antenna run. The reason you replaced it was that it failed; it'll fail again.

## Common mistakes

- **Replacing only the failing piece.** A 50-ft run with a bad connector at year 12 — you replace the connector, not the run. But the rest of the cable is also at year 12. Plan the full replacement now, not after the next failure.
- **Cheap coax for a long run.** RG-58 at 100 ft on 144 MHz has 6.5 dB matched loss — your 100-watt rig is now a 22-watt rig at the antenna. Use cable matched to your length and frequency.
- **Skimping on weatherproofing.** Five minutes of tape applied carefully will give you 10 years; five minutes of "good enough" tape gives you 18 months. The cost is identical.
- **Buying coax to "store for later."** Coax has a shelf life too — UV exposure, jacket plasticizer migration, dielectric absorption over time. Buy what you need; don't stockpile.
- **Skipping the baseline sweep on the new install.** Without it, the next inspection has nothing to compare against.

## See also

- §16-00 — Overview
- §16-03 — Scheduled inspections (where you'll find the symptoms)
- §10-01 — Coax issues (the troubleshooting view)
- §10-07 — Water ingress
- §18 — Coax & connectors reference
- §04-10 — Feedline effects (why loss matters)
