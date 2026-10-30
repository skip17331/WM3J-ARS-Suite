---
id: 08-00
title: RF Safety — Overview
chapter: 08
section: 00
level: simple
status: published
---

# RF Safety — Overview

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Every amateur radio licensee in the United States is **legally required** to evaluate their station for RF exposure compliance. This isn't a paperwork ritual — at amateur power levels (especially 100 W and up) and at typical antenna distances, real RF exposure limits do get exceeded under bad geometry. The FCC's rules exist because the underlying physics is real.

This chapter is the practical guide. It covers what the rules require, how to compute the numbers, how to design your station so the numbers come out OK, and what happens when they don't (RF burns, bystander exposure, induced currents in nearby objects).

The good news: **the vast majority of typical amateur installations comply easily**, especially horizontal antennas at reasonable height. The bad news: **a few common scenarios — magnetic loops, attic dipoles, mobile installations with high-power amps, indoor antennas — can be genuinely problematic**, and you have to evaluate them, not just assume.

## Why this chapter matters

Three reasons RF safety is more than legal box-checking:

1. **High-frequency RF (10s of MHz–GHz) absorbs in tissue.** At MPE-exceeding intensities, this produces real heating — especially in the eyes (low blood flow, poor heat dissipation) and gonads. Effects are dose-dependent: brief exposure to mildly elevated fields is harmless; sustained exposure to strongly elevated fields can cause damage.
2. **Direct RF burns from contact** with high-voltage points on antennas (especially at QRO) are immediate and severe. A magnetic loop's tuning capacitor at 100 W can put 5+ kV across a small gap. A dipole's end at 1500 W can produce visible plasma if you arc to it.
3. **Bystander exposure is the FCC's real concern.** You as the operator know what you're doing; the kid on the swing set 15 ft from your vertical doesn't. The "uncontrolled environment" rules apply to anyone who isn't aware of the RF.

## How the chapter is organized

| § | Topic | Why it's here |
|---|-------|---------------|
| 08-01 | FCC rules | What 47 CFR 1.1307 / 97.13 actually say; the legal structure |
| 08-02 | MPE limits | The exposure numbers themselves (E-field, H-field, power density) by frequency |
| 08-03 | Controlled vs uncontrolled environments | The two MPE thresholds; why "uncontrolled" is usually the binding one |
| 08-04 | Duty cycle | How TX time fraction and mode duty cycle affect time-averaged exposure |
| 08-05 | ERP & EIRP | Effective radiated power as the input to all MPE calculations |
| 08-06 | Safe antenna placement | Practical distances; antenna height, fences, neighbor proximity |
| 08-07 | RF burns | The contact-injury side; how burns happen and how to prevent them |

## What changed in 2021

The FCC's 2019 rule changes (effective May 2021) **eliminated the categorical exemption that exempted most amateur installs from formal RF exposure evaluation**. Before 2021, hams under 100 W on most bands didn't have to formally evaluate. After 2021, **every licensee evaluates every station, period**.

The evaluation can be informal — the FCC doesn't ask for the paperwork up front — but **you must perform it and keep records of it**. If a complaint or inspection arises, the FCC will ask to see your evaluation.

## What you actually have to do

The minimum, for typical amateur stations:

1. **Know your ERP** on each band. (See §08-05.)
2. **Know your antenna's distance** to the nearest place where humans (or pets) might be — your operating position, family living areas, neighbors' yards, sidewalks.
3. **Compute or look up the MPE distance** for that ERP and that frequency. (Tables in §08-02; the ARRL has a free online evaluator. The J-Hub RF Exposure Calculator at §12 does this interactively.)
4. **If actual distance > MPE distance**, you're compliant. Document the evaluation and file it.
5. **If actual distance < MPE distance**, mitigate: reduce power, raise antenna, change beam direction during high-power TX, install warning signs, or restrict access to the area.

For most installations, this whole process takes 30 minutes. Once.

## The threshold that surprises people

| Power level + antenna scenario | Likely compliant? |
|--------------------------------|-------------------|
| 100 W into a 30-ft-up dipole, 100 ft from the house | Easily |
| 100 W into a vertical 20 ft from the patio | Probably; check the numbers |
| 1500 W into a Yagi 50 ft up, beam pointed at neighbor's yard | Maybe not; check carefully |
| 100 W into an attic dipole 8 ft above where you sleep | Marginal; evaluate carefully |
| 100 W into a magnetic loop on your desk | Re-evaluate; loops have very high near-field strengths |
| 1500 W into a dipole at the back property line, 6 ft over a public sidewalk | No. Move the antenna. |

The "magnetic loop on your desk" case is one of the genuinely problematic amateur configurations, even at low power. See §08-06.

## What you will not learn here

- **Detailed bioeffects research** — the underlying biology of microwave absorption is its own field.
- **Lightning protection** — covered separately (planned ch. 29).
- **Tower climbing safety, mechanical hazards** — covered separately (planned ch. 31).
- **Battery and electrical safety** — covered separately (planned ch. 33).

## Where the suite helps

- **§17-14 (RF Exposure Calculator)** — interactive calculator. Enter band, ERP, antenna distance; get controlled/uncontrolled compliance status.
- **§11 (Power Budget / ERP)** — work out your ERP given TX power, feedline loss, and antenna gain.
- **§08 (this chapter)** — the conceptual background for both calculators.

## See also

- §17-14 — RF Exposure Calculator (the practical tool)
- §11 — ERP / Power Budget
- §06-05 — Ground-plane effects (matters for near-field calculation)
- §06-07 — Patterns (gain at the relevant elevation matters for exposure direction)
