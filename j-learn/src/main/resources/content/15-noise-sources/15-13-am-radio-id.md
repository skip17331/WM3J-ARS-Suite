---
id: 15-13
title: AM Radio ID
chapter: 15
section: 13
level: simple
status: draft
---

# AM Radio ID

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The AM radio sniffer technique from §14-07 works for power-line noise too — with some adaptations. Power-line sources are usually stronger than household sources and at a greater distance, which changes the practical procedure.

## Why AM works for power-line hunting

Same reasons as for household RFI:

- AM portable receivers have a directional ferrite-rod antenna.
- They respond well to broadband impulse noise (the kind that arcing insulators and corona produce).
- They're cheap and portable.

Power-line sources tend to be louder than household sources but at distances of 100–1000 ft instead of 5–50 ft. The technique scales fine.

## What you need

- **A portable AM/SW radio** — Tecsun PL-380 ($40) is ideal. The shortwave bands let you also confirm the noise on amateur frequencies as you walk.
- **Hat / sun protection / good walking shoes** — you may be outside for an hour or two.
- **A notebook or phone for logging** what you find.
- **A camera** for photographing pole numbers and any visible damage.
- **Optional: binoculars** for inspecting tall poles without climbing.

## The procedure

1. **Tune to a quiet AM frequency** (say 1100 kHz) where the noise is clearly audible at your station.
2. **Confirm the noise pattern** with the AM radio at your station. Now you know what you're listening for.
3. **Walk away from your station along the most likely power line route.**
4. **As the noise gets louder**, you're getting closer. As it gets quieter, you're moving away. The path of strongest noise traces back to the source.
5. **At the source pole**, the noise is unmistakably loud. Hold the AM radio in different orientations to find the deep null — this confirms the direction of arrival and rules out coincidental "loud" spots.

The procedure is the same regardless of which power-line failure mode (insulator, transformer, hardware, corona). What you find at the pole tells you the diagnosis.

## Walking efficiently

A few tips to save time:

- **Walk along the side of the road** the power lines run on. Listen as you walk.
- **Pause every 100 ft** to read the radio carefully (your footsteps mask weak changes in noise level).
- **Note any pole where the noise is significantly louder than the average.** Mark those for closer inspection.
- **Cover both sides of the line.** Some installations have a phase on the right and a phase on the left of the road; the bad one might be on the side away from your walking path.
- **Drive slowly** if the area is too large to walk practically (suburban distribution lines can run for miles).

## Distinguishing pole sources from in-pole sources

When you reach a pole where the noise is loud:

- **Hold the radio at the pole's base.** Note the level.
- **Walk away from the pole 50 ft.** Level should drop noticeably.
- **Walk 100 ft.** Level should drop more.
- **If it doesn't drop**, the source isn't at this pole specifically — it might be the entire line, suggesting corona.
- **If level stays high near the pole and drops away**, this pole is the source.

The directional null with the ferrite antenna is more precise than just signal level — rotate the radio to find the deep dip; the source direction is perpendicular to the radio's long axis.

## What to look for at the suspect pole

Once you've identified a pole as the source:

- **Cracked porcelain insulators** (white discs at the top of the pole). Look for visible cracks, brown spots, or burn marks.
- **Loose hardware** — a cross-arm bolt that looks slightly out, a guy wire that doesn't appear taut, a transformer that looks asymmetric.
- **Visible burn marks or carbonization** on the pole or hardware near the line connection points.
- **Bird damage** — nests built on insulators are a classic cause.
- **Recent storm damage** — broken branches resting on lines, ice damage, pole damage from a vehicle.

Photograph everything. The pole number is on a metal tag (usually 4–8 ft up); photograph that too.

## When the source is a transmission line

If your noise traces back to a major transmission line (tall steel structures, multi-conductor lines), the AM-radio approach still works but at greater distances. Transmission lines can carry the noise back up to your location for miles.

For transmission lines:

- The actual fault may be far away (multiple miles).
- Walking is impractical; drive along the route.
- Multiple inspectors may be needed to identify the exact fault location.
- Reporting goes to the transmission-line owner (often different from your local distribution utility).

## Logging your hunt

Useful template:

```
Date / time:
Weather:                  (dry, fog, drizzle, etc.)
Starting noise level:     S6 on 80 m (your station)
Walked toward:            (street name, direction)
Loudest pole found:       (pole #)
Owner:                    (utility name from pole tag)
Visible problem:          (insulator crack, etc.)
GPS coordinates:
Photos taken:             yes/no
Notes:
```

Save these notes. They become the basis of your utility complaint (§15-15).

## See also

- §14-07 — AM radio sniffer for household RFI (similar technique)
- §15-14 — SDR identification (alternative for harder cases)
- §15-15 — utility documentation procedure
