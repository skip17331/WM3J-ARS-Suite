---
id: 06-19
title: Off-Center-Fed Dipole (OCFD / Windom)
chapter: 06
section: 19
level: mixed
status: draft
---

# Off-Center-Fed Dipole (OCFD / Windom)

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Move the feedpoint of a dipole away from the center and two things happen: the feed impedance rises, and a single feedpoint impedance starts to match on *several harmonically related bands at once*. That is the whole idea behind the off-center-fed dipole (OCFD) and its ancestor, the Windom. One wire, one feedline, four or five bands, no tuner — at the price of a feedline that wants to radiate if you don't choke it properly.

## The geometry

A half-wave (or longer) wire fed at roughly **one-third from one end** instead of the middle. The classic ratios:

| Design | Off-center point | Bands (from an 80 m wire) |
|--------|------------------|----------------------------|
| Classic OCFD ("Windom") | ~14% from center, i.e. about 1/3 from one end | 80 / 40 / 20 / 10 |
| 40 m OCFD | 1/3 from one end | 40 / 20 / 10 |

At that feedpoint the impedance sits near **200–300 Ω** on the fundamental and on the even-harmonic bands, instead of swinging from 70 Ω to several kΩ the way a center-fed wire does across its harmonics. A fixed transformer can therefore serve all of them.

## Feeding it

- **4:1 balun** matches ~200 Ω down to 50 Ω. The most common choice.
- **6:1 balun** for designs that center on ~300 Ω.
- Always a **current (choke) balun**, and usually a **second choke** a few feet down the coax. The OCFD is electrically lopsided, so it pushes common-mode current onto the feedline harder than a center-fed dipole does — the feedline *will* radiate and bring RF into the shack if you skip the choking. See §06-12.

> **Advanced —** Because the feed is off-center, the two halves carry unequal currents and the antenna is inherently unbalanced at the feedpoint. The 4:1 balun handles the impedance step, but only an additional common-mode choke restores feedline balance. This is why "my OCFD has RF in the shack" is the single most common complaint about the design — the cure is a good choke 0.05–0.1 λ down the coax, not a different balun ratio.

## The Carolina Windom variant

The **Carolina Windom** deliberately turns the feedline radiation into a feature. Below the 4:1 balun it inserts a measured length of coax as a **vertical radiator**, then a **line isolator** (choke) below that. The result is a horizontal OCFD with an intentional vertical radiating section — adding a lower-angle, more omnidirectional component that some operators find helps DX on the low bands. The line isolator stops the radiation from continuing down into the shack.

## Cookbook lengths

| Band group | Total wire | Long leg / short leg (≈) |
|------------|-----------|---------------------------|
| 80–10 OCFD | ~135 ft (41 m) | ~90 ft / ~45 ft |
| 40–10 OCFD | ~67 ft (20.5 m) | ~45 ft / ~22 ft |

Exact split varies by published design; cut a little long and trim the **long** leg for best SWR on the lowest band, then check the others.

## When to pick an OCFD

- You want **multi-band, no-tuner** operation on the harmonically related bands (80/40/20/10 or 40/20/10).
- You have room for a full-size flat-top and one center-ish support.
- You'll commit to proper choking.

## When to avoid

- You want 30 / 17 / 12 / 15 m — those aren't in the harmonic set and need a tuner anyway, at which point a doublet (§06-21) is the better tool.
- You can't tolerate any feedline radiation (the design fights you here).

## Common mistakes

- **No common-mode choke.** This is *the* OCFD failure mode — hot mics, RFI, SWR that changes when you touch the rig.
- **Wrong balun ratio** for the design's target impedance — gives high SWR that length tuning can't fix.
- **Trimming the short leg.** Tune the long leg; the short leg sets the harmonic relationships.
- **Confusing it with a G5RV.** Different matching scheme entirely — the OCFD uses a balun at the feedpoint, the G5RV uses a ladder-line matching section (§06-20).

## See also

- §06-01 — Dipoles (the center-fed baseline)
- §06-12 — Baluns and chokes (the 4:1 and the all-important second choke)
- §06-20 — G5RV / ZS6BKW (the other "one wire, several bands" approach)
- §06-21 — Doublet (when you'd rather have a tuner and all bands)
- §10-04 — Feedline and SWR
