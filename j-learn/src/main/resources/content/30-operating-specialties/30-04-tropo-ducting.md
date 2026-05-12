---
id: 30-04
title: Tropospheric Ducting
chapter: 30
section: 04
level: mixed
status: draft
---

# Tropospheric Ducting

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What tropo ducting is

The bottom 10–15 km of the atmosphere — the troposphere — normally attenuates VHF and UHF signals beyond line-of-sight. You can sometimes squeeze 50–100 miles out of a 2 m signal with a tall antenna and patience, but past that the curvature of the Earth and ordinary atmospheric refraction bend the signal into the ground.

Sometimes the atmosphere acts differently. A **temperature inversion** — warm dry air sitting on top of cool moist air — forms a sharp boundary in refractive index. Signals entering that boundary at low angles can be **trapped** inside the inversion layer, refracted along its length for hundreds or thousands of kilometers before they leak back out. That trapping volume is a **tropospheric duct**.

A ducted 2 m signal that would normally die at 100 miles instead carries 800 miles or more. The band that an hour ago seemed empty is suddenly alive with distant grid squares, repeaters, and SSB activity that has no business being audible.

## When it happens

Ducting needs a stable inversion to form. Three weather patterns reliably produce it:

| Pattern | Geography | When |
|---------|-----------|------|
| **Continental high pressure** with subsidence inversion | Inland continental US, central/western Europe | Summer / early autumn, multi-day stagnant highs |
| **Coastal / marine ducting** at sea-air boundary | Mediterranean, Caribbean, Gulf coast, California coast, Sea of Japan | Spring through fall; persistent in summer |
| **Frontal boundaries** with sharp warm-air-aloft layer | Anywhere with strong synoptic-scale weather | Spring and fall transition seasons |

The classic continental opening: a stagnant high-pressure system sits over a region for 3–5 days. Air aloft warms by adiabatic compression while the surface air retains overnight moisture. By day three, an inversion has formed at 1500–3000 ft, and stations 800–2000 km apart suddenly hear each other on 2 m SSB. This pattern is most reliable in **late summer and early autumn** when high pressure dominates.

The marine duct along the Atlantic Coast in summer is a permanent fixture — VK1WIA bulletins regularly note that east-coast US stations and Caribbean stations work each other almost daily on 2 m via the duct that hugs the warm-sea / cool-air boundary.

## Bands and distances

| Band | Typical ducted range | Notes |
|------|---------------------|-------|
| **6 m** | 800–2000 km | Less duct-dependent (Es is often stronger here) |
| **2 m** | 500–1500 km | The classic tropo band; SSB activity strong |
| **70 cm** | 500–1500 km | Sharper opening / closing; deep openings work it |
| **23 cm** | 300–1200 km | Best in coastal / marine ducts |
| **9 cm / 5 cm** | 300–1000 km | Microwave bands love a strong duct |

The general rule: **the higher the frequency, the better confined the signal in a thin duct, but the easier the duct collapses** when the inversion weakens. 2 m is the best balance and is where most casual tropo activity happens.

## Predicting tropo — Hepburn maps and surface analysis

Two tools dominate tropo prediction:

### Hepburn tropospheric ducting forecast maps

William Hepburn's site (dxinfocentre.com) publishes daily global maps showing predicted tropo opening intensity, color-coded:

| Intensity | What it looks like on air |
|-----------|---------------------------|
| 1 — slight | Marginal openings, beacons audible |
| 2 — moderate | Repeater hearing 500 km, SSB DX possible |
| 3 — good | Routine 1000 km contacts, big-station fun |
| 4 — strong | Continental-scale openings, 1500+ km |
| 5 — extreme | Once-a-year, multi-day events, transoceanic |

Operators in tropo-prone regions check Hepburn's map every morning. A predicted 3+ event over a path is reason to spend the evening on 2 m SSB.

### Surface weather analysis

The Hepburn maps come from numerical weather models. You can read the same signal yourself:

- **Surface high pressure** stagnant over the area: a ridge of 1020+ mb that hasn't moved in 2+ days is suspect.
- **Temperature inversion soundings** from regional weather offices: a sharp positive temperature gradient at 1000–3000 ft AGL means a duct.
- **Cloud structure**: shallow stratus or fog layer with clear sky above is a tell.
- **Lack of convection**: clear, calm, stagnant air is a tropo-friendly state.

The weather-aware operator notices these things and is *already on the air* before the spotting networks fill up.

## Operating tropo — modes and frequencies

Tropo paths are shallow-fade, low-noise channels — much friendlier than meteor scatter or EME. You can run any mode that works on line-of-sight VHF:

- **SSB and CW** are dominant. Calling frequencies: **50.125 MHz** (6 m), **144.200 MHz** (2 m), **432.100 MHz** (70 cm), **1296.100 MHz** (23 cm) in IARU Region 2.
- **FT8** works well — the digital sub-band frequencies (50.313, 144.174, 432.174 MHz) light up during openings.
- **FM** is usable but suboptimal — the 12 dB SINAD threshold of FM means marginal duct conditions that would be readable on SSB are unintelligible on FM. FM repeaters from distant grids are a *signature* of an opening but not the ideal QSO mode.

A typical opening on 2 m:

```
Local 19:00 — Band sounds normal. K1ABC beacon (EM75) audible, weak.
20:00       — K1ABC suddenly S7. K4XYZ (EL98) appears on 144.200, working you direct.
21:30       — N5MS (EM10, ~1500 km away) calls CQ on 144.200 SSB. You answer.
                Both signals 5x5. Five-minute QSO.
23:00       — Duct fades. Distant signals drop back to noise.
```

## Antennas — beam horizontal

Tropo ducts run **horizontally** in the atmosphere — you want your antenna's main lobe **pointing along the horizon**, not up into the sky.

- **Long single Yagis** (10–14 elements on 2 m, 12–16 on 70 cm) are the standard tropo antennas.
- **Stacking** two Yagis vertically narrows the elevation pattern further into the duct — useful at the high end of tropo intensity.
- **Antenna height** matters: getting the antenna above local clutter (treelines, buildings) into the open atmosphere is the difference between catching a duct and not. Even 20 ft more height can change a station's tropo reach by 30%.
- **Az rotator only** is fine; no elevation needed for tropo.

A single 10-element 2 m Yagi at 50 ft and 100 W will work nearly every continental-class tropo opening. The same antenna at 20 ft over a clutter-rich suburban yard will catch maybe one in three.

## Why SSB and CW beat FM

FM's 12 dB threshold effect is fatal to marginal tropo. If a signal is 11 dB above the FM threshold you get full quieting; 11 dB below and you get full noise. SSB and CW degrade gracefully: a signal 6 dB into the noise is still copyable on CW, perhaps weakly intelligible on SSB. Tropo openings spend most of their time in the marginal zone — 0 to +6 dB SNR — which means SSB and CW catch contacts FM operators miss entirely.

This is the same reason DXers prefer SSB and CW over FM for any kind of weak-signal work, but it's particularly true for tropo, where the fade margin is constantly shifting.

## Specific tropo regions and DX

Some regions have famous tropo paths:

| Path | Distance | Notes |
|------|----------|-------|
| **California ↔ Hawaii** | ~3900 km | Documented multiple times via Pacific marine duct |
| **Mediterranean Spain ↔ Italy** | 1000–1500 km | Routine in summer |
| **US Gulf coast** along New Orleans–Tampa | 800 km | Regular summer activity |
| **UK ↔ Scandinavia** | 800–1500 km | North Sea ducting in autumn |
| **Australia east coast ↔ New Zealand** | 2000+ km | Tasman Sea duct |

## When tropo shines

- **Multi-state VHF contests.** The June and September ARRL VHF contests catch summer tropo seasons; ops who beam down-coast during the right hours work 10× the grids they would on a closed band.
- **Hunting beacons** on 2 m and 70 cm — a routine tool for monitoring conditions.
- **Adding grids for VUCC** on the higher bands (70 cm, 23 cm) where there's no other propagation.
- **Marine-coast operating** where the duct is nearly always present.

## When tropo doesn't help

- **Long-distance DX on 2 m to other continents** — except for a few documented marine paths, transoceanic tropo is rare. EME or satellite is the answer.
- **Inland operation in winter** — continental high pressure inversions are weaker; activity tails off.
- **Inside a city's RF noise floor** — tropo signals are weak; you need a quiet receive site.

> ⚙️ **Advanced —** Tropo ducting requires the modified refractive index gradient dM/dh to go negative — equivalently, dN/dh < −157 N-units per km where N is the radio refractive index N = 77.6(P/T) + 3.73×10⁵(e/T²) (P = pressure mb, T = temperature K, e = water vapor partial pressure mb). When dM/dh is negative across some atmospheric slab, signals are *guided* within that slab. The duct height and thickness determine the minimum trapped frequency: a thin (50 m) duct only traps signals above ~5 GHz; a thick (500 m) duct can trap 50 MHz signals. Surface ducts (where the inversion is right at ground level) carry the strongest signals because no leakage occurs at the bottom; elevated ducts (inversion aloft) leak energy out the bottom and are noisier. ITU-R P.452 is the standard for predicting tropo path loss with duct enhancement.

## See also

- [§01-09 — Weak-Signal VHF/UHF](../01-propagation/01-09-weak-signal.md) — tropo in propagation context
- [§01-01 — Solar Indices](../01-propagation/01-01-solar-indices.md) — what tropo is *not* (it's weather, not solar)
- [§30-02 — Meteor Scatter](30-02-meteor-scatter.md) — the other VHF weak-signal mode
- [§30-05 — Aircraft Scatter](30-05-aircraft-scatter.md) — yet another VHF specialty
- [§06 — Antennas](../06-antennas/) — long Yagis for VHF
- [§20 — Band Plans](../20-band-plans/) — VHF/UHF calling frequencies
