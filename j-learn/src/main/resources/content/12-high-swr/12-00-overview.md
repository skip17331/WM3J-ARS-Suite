---
id: 12-00
title: High SWR — Overview
chapter: 12
section: 00
level: simple
status: draft
---

# High SWR — Overview

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

You key up to call CQ and the SWR meter pegs. The radio's protection circuit kicks in. Maybe a SWR alarm beeps. Maybe the rig folds back to 5 W and you sound terrible. Now what?

This chapter is the field guide. It walks the seven most common causes of high SWR — what each looks like, how to confirm it, and how to fix it. Work the list in order; about 80% of high-SWR problems turn out to be the first three causes.

## What "high" means

| SWR | What's happening | What to do |
|-----|------------------|------------|
| 1.0–1.5:1 | Excellent. Antenna well-matched to feedline. | Operate normally. |
| 1.5–2.0:1 | Acceptable. Most rigs operate at full power without folding back. | OK. Consider tuning if you're picky. |
| 2.0–3.0:1 | High. Most modern rigs start folding back here. Mismatch loss starts to bite. | Investigate. Often fixable. |
| 3.0–5.0:1 | Very high. Rig may refuse to TX. Real power loss to feedline heating. | Stop transmitting. Find the problem. |
| > 5.0:1 | Probably broken. Open or short somewhere. | Stop transmitting. Test before TX again. |

Numbers above 3:1 are the action threshold for most operators.

## The diagnostic tree (read this first)

When SWR is high:

1. **Is the SWR high on every band, or just one?** All bands → feedline / connector / coax problem. One band only → antenna issue (length, environment, balun on a single-band antenna).
2. **Did it work before?** Yes, recently → something physical changed (water, ice, wind damage, a critter). No, brand new install → length / matching / balun issue.
3. **Is the SWR the same at the rig and at the antenna feed point?** (You need a portable analyzer like a NanoVNA to check both ends.) Same → antenna is the cause. Different → feedline / connector / matching component is the cause.
4. **What does the SWR-vs-frequency curve look like?** Sharp dip at one frequency → resonant antenna, just not on the band you wanted. Flat-high everywhere → feedline open or short. Wandering high → connector or balun problem.

This tree leads you to one of the seven sections in this chapter.

## How this chapter is organized

| § | Cause | When to suspect it |
|---|-------|--------------------|
| 13-01 | Coax issues | Old or weather-damaged cable, kinks, water in the dielectric |
| 13-02 | Connector issues | Recent re-termination, water-exposed connector, gradually worsening |
| 13-03 | Incorrect length | Brand new antenna, not yet tuned, or moved/shortened |
| 13-04 | Nearby metal | New gutter, new tower section, a tree branch grew, building added |
| 13-05 | Faulty balun | Balun got rained on, internal core saturated, or hit by lightning |
| 13-06 | Feedline routing | Coax wrapped around the antenna, common-mode current issue |
| 13-07 | Water ingress | Water in coax dielectric, in connectors, or in feedpoint matching network |

## Tools you need

The minimum:

- **The radio's built-in SWR meter** — fine for a yes/no answer, not great for diagnosis.
- **An external SWR meter** between rig and feedline — slightly better, can see what the rig sees.

Useful additions:

- **A NanoVNA** (~$50) — measures SWR vs frequency over a range, vector data, can be carried up to the antenna. The single most useful piece of HF test gear an operator can own.
- **An antenna analyzer** (RigExpert, MFJ-269, MFJ-259B) — like a NanoVNA but with a more rugged form factor. Plug into a feedline and sweep.
- **A return-loss bridge or directional coupler** — for serious work; lets you separate forward and reflected wave at any point in the system.

## What to do right now

If you're staring at a high-SWR situation:

1. **Don't keep transmitting.** Most rigs survive a few seconds of high SWR thanks to fold-back protection, but extended high-SWR TX can damage the PA. Take it off the air.
2. **Reduce power to QRP** (5 W or less) for any further testing — protects the rig if SWR stays high.
3. **Walk the chain physically.** Look at every cable, every connector, the antenna itself. A surprising number of problems are visible (loose connector, broken wire, fallen-down balun).
4. **Then go to the appropriate section** in this chapter.

## See also

- §18 (Coax & Connectors reference) — coax types and connector specs
- §12-01 onward — work the cause-by-cause sections in order
