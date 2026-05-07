---
id: 13-09
title: Arcing Insulators
chapter: 13
section: 09
level: simple
status: draft
---

# Arcing Insulators

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The most common cause of power-line RFI. A high-voltage insulator on a pole develops a crack, becomes contaminated with airborne dirt, or has a hairline conductive path. The high voltage (typically 7,200 V phase-to-ground on residential distribution lines) finds a way to arc across the gap, generating broadband HF noise.

## How it sounds

Distinctive:

- **Sharp crackling** that pulses at the AC line frequency (60 Hz).
- Almost always **broadband across HF** — you'll hear it on every band.
- Often **worse in dry weather** when the surface conductivity is uneven (water from rain temporarily provides a smooth conductive path).
- **Drops or stops in heavy rain or snow** as the insulator gets cleaned and the gap shorts out.
- **Persists during dew or fog** when contamination is slightly conductive but not enough to fully short.

That weather pattern is the key fingerprint. If your noise is worse on dry summer days and quieter when it rains, you almost certainly have an insulator problem.

## What's physically happening

A porcelain or polymer insulator typically holds the energized line away from the grounded pole hardware. When that insulator is intact and clean:

- Surface resistance is essentially infinite.
- No current flows from line to pole.

When the insulator is cracked or contaminated:

- Tiny conductive paths form.
- The 7,200 V (or higher) source drives micro-arcs across these paths.
- Each arc is brief but produces broadband RF emissions reaching well into HF.

The arcs occur at AC voltage peaks (twice per cycle), giving the characteristic 120 Hz repetition rate (with each peak producing one or more arcs).

> ⚙️ **Advanced —** The HF spectrum content of arc emissions roughly follows a 1/f² pattern from the LF range to high VHF. The spectral peak energy is in the LF/MF range, but enough harmonic energy reaches HF to make insulator arcs noticeable from kilometers away. Modeling work (notably by W2VTM in QST) characterized typical insulator arcs as broadband emitters with a relatively flat spectrum from 2 to 30 MHz, dropping off in the VHF range. This explains why insulator arcs affect so many bands at once.

## How to find an arcing insulator

Walk your neighborhood with a portable AM or shortwave radio (see §13-13 for AM, §13-14 for SDR):

1. **Tune to a quiet AM frequency** (or a specific HF band where the noise is loudest).
2. **Walk along power line routes.** Stay safe — these are high-voltage lines.
3. **Note where the noise peaks.** Often at a specific pole.
4. **Look up at the pole** for visible damage to insulators. Look for:
   - Cracked porcelain.
   - Visible burn marks (carbonization).
   - Birds' nests built on insulators (the conductive material in the nest can short across).
   - Insulators tilted or damaged from impact (a vehicle that hit the pole).

A pair of binoculars helps. Do not climb or touch.

5. **Try at different times of day, weather conditions.** A bad insulator is usually loudest in dry weather; a temporarily-bad one (just wet) may be quiet at first then loud as it dries.

6. **Check both sides of the pole.** The arc may be on the back of the insulator, hard to see from one direction.

## What to do once you find it

You don't fix it yourself. Power lines are utility property; only utility crews work on them. Your job is to **document and report**.

For documentation:

1. **Note the pole number** (usually a metal tag on the pole, sometimes printed identifiers).
2. **Take photos** — multiple angles, including a close-up of the visible damage if possible.
3. **Note the GPS coordinates.**
4. **Note the date, time, weather conditions** when you observed the problem.
5. **Take an audio recording** of the noise from the pole and from your house.

Then file the complaint per §13-15.

## How utilities respond

Once a utility EMI/RFI engineer takes the complaint:

- **First visit (1–4 weeks)**: utility tech with a directional RFI sniffer drives or walks the route. They confirm or deny the problem and may identify a specific pole.
- **Repair scheduling (1–6 months)**: replacing an insulator requires a line crew, usually planned during normal maintenance cycles. Some utilities prioritize RFI; others fit it into the regular schedule.
- **Repair (a few hours)**: line crew swaps the insulator; sometimes they replace surrounding hardware too.
- **Verification (your job)**: confirm the noise is gone. Notify the utility if not. Sometimes a different insulator on the same pole is bad too.

A typical fix cycle: 3–9 months from first complaint to silence.

## What you can do in the meantime

- Use receive-antenna nulls. A Beverage receive antenna oriented away from the offending pole significantly reduces the noise pickup.
- Operate higher bands (10 m, 12 m). The arc spectrum still reaches them but is usually less intense.
- Operate during weather conditions that quiet the arc (heavy rain, snow). Counterintuitive but works.
- Add receive-side noise blanking (most modern radios have aggressive NB and DSP NR options).

## A worked example

A typical real-world report:

- **Symptom**: S6 broadband hash on 80 m and 40 m, only on dry days.
- **Diagnosis**: walked street with portable AM radio. Noise peaks at one specific pole 2 blocks away.
- **Visual**: pole has a cracked porcelain insulator on one phase.
- **Documentation**: photographed pole, captured audio recordings, marked GPS.
- **Filed complaint** with utility's RFI desk.
- **First utility visit (3 weeks)**: confirmed bad insulator with their sniffer.
- **Repair (4 months later)**: line crew replaced the insulator and adjacent hardware.
- **Verification**: noise gone within hours of repair. Total cycle: 5 months.

This is a common pattern. The cycle is long but the outcome is clean.

## What if it's not your utility?

Power-line noise can come from a pole that's served by a different utility than your house. In that case:

- The complaint goes to the utility that owns the pole, not your provider.
- This is sometimes harder to identify but the pole tag usually shows the owner.
- Coordinate with your provider; they often help route the complaint.

## See also

- §13-12 — corona discharge (similar but different mechanism)
- §13-13 — AM radio identification (the practical hunt)
- §13-15 — utility documentation procedure
