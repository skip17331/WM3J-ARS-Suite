---
id: 01-08
title: Band Choice Right Now
chapter: 01
section: 08
level: simple
status: draft
---

# Band Choice Right Now

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

You sat down. The radio is on. Where do you tune? This page is a 60-second decision tree — no theory, just answers.

## The 30-second cheat sheet

| It's | Try first | Try next | Don't bother with |
|------|-----------|----------|---------------------|
| Daytime, summer | 20 m, 17 m | 15 m, 10 m | 80 m, 160 m |
| Daytime, winter | 20 m | 17 m, 15 m | 80 m, 160 m |
| Sunrise / sunset | 40 m, 80 m, 160 m | 30 m | 12 m, 10 m (usually) |
| Night, after dinner | 40 m, 30 m, 80 m | 160 m | 17 m, 15 m, 12 m, 10 m |
| Late night | 80 m, 160 m | 40 m | (most others) |

That's it. Read the rest only if you want the *why* or the special cases.

## The decision tree

### Step 1 — what time is it where I am?

- **Daytime** (sun up, more than two hours after sunrise and before sunset) → Step 2
- **Twilight** (within 90 min of sunrise or sunset) → Step 4 (greyline)
- **Night** → Step 3

### Step 2 — daytime path planning

The high bands are open. Pick by distance to your target:

- **Local / regional (under 500 km)** → 80 m or 40 m. NVIS antennas help.
- **Continental (500–3000 km)** → 20 m or 40 m.
- **DX (3000+ km)** → 20 m, 17 m, 15 m. Try 10 m if SFI > 100.

Then check what the solar conditions allow:

- SFI under 90 → stay on 20 m and 40 m. Higher bands are sparse.
- SFI 90–150 → 17 m and 15 m work most days; 10 m has openings.
- SFI 150+ → 10 m is daily reality. 12 m is excellent. 6 m F2 is possible.

### Step 3 — nighttime path planning

The low bands take over. Pick by distance:

- **Local (under 300 km)** → 80 m at high angle, or 40 m.
- **Continental** → 40 m, 30 m, sometimes 20 m if late evening.
- **DX** → 40 m, 80 m greyline edges, 160 m for the brave.

Check K-index:

- K = 0–2: noise floor is low. 160 m DX is on.
- K = 3–4: noticeable noise. 40 m fine; 160 m painful.
- K = 5+: storm. Stay on the lowest bands and don't expect polar paths.

### Step 4 — greyline / twilight

Both 80 m and 40 m wake up here, often dramatically. Your sunrise greyline = paths to whoever is at sunset (and vice versa).

- **Sunrise**: chase paths to the **east of you** (where the sun is already up). Best 5–30 minutes before your local sunrise.
- **Sunset**: chase paths to the **west of you** (where the sun is going down behind you).
- 160 m greyline windows are short — be ready before they open.

See §01-03 for greyline detail.

## The "what's actually open" approach

Theory is great. The fastest way to know what works **right now** is to look at what's already being heard:

1. Open **PSK Reporter** (`pskreporter.info`).
2. Set the filter to "received here from anywhere", last 30 minutes, your QTH grid.
3. Switch through the bands. You'll see exactly which paths are currently producing signals from where.
4. **The bands with active spots from interesting places are open. The bands with nothing are not.**

That's the empirical answer. If 17 m is showing 50 spots from EU and 10 m is showing zero, you know where to point your antenna.

## Special situations

### "There's a contest on this weekend"

Contest activity changes the answer. During CQ WW or ARRL DX, every band that *could* work *will* work because everyone is out there hunting. Pick the band with the most contest activity — the spotting density itself is a propagation indicator.

### "I want to work a specific country"

1. Note their local time (you can find this in J-Map or any world clock).
2. If they're in the daytime portion of the cycle and you are too, use the high bands.
3. If they're night and you're day (or vice versa), the path crosses the terminator — try 40 m or 30 m, or 20 m at the edges.
4. Both in night → 40 m, 80 m.
5. Both in day → 20 m, 17 m, 15 m, 10 m if conditions allow.

### "There's a flare warning"

A solar flare can blackout HF (especially the lower bands) for 30 minutes to a few hours on the sunlit side of Earth. If NOAA says X-class flare in progress:

- Lowest bands first (160 m is least affected since it's already nearly unusable in daylight).
- The dark side of Earth is unaffected — 80 m and 40 m at night are fine.
- Wait it out; recovery is quick after the flare ends.

### "There's a geomagnetic storm in progress"

A CME hitting Earth (K ≥ 5) suppresses MUF and adds noise to all bands, with the heaviest impact on polar paths.

- Avoid paths that cross 60°+ latitude.
- Equatorial paths still work.
- Drop a band from your normal choice.
- After about 24–48 hours conditions usually return to normal.

### "Sporadic E season"

May–August in the Northern Hemisphere. Always check 6 m and 10 m even if you're at solar minimum — Es opens these bands without help from the sun.

## A weekly rhythm

If you operate at the same time every day:

- **Mornings** (06:00–09:00 local): 40 m local + DX greyline windows
- **Mid-morning** (09:00–11:00): 30 m, 20 m for medium DX
- **Mid-day** (11:00–14:00): 20 m, 17 m, 15 m for DX
- **Afternoon** (14:00–17:00): high bands continue; 10 m peaks
- **Evening** (17:00–21:00): 40 m takes over for short paths; 20 m fades for long DX
- **Night** (21:00–24:00): 40 m, 30 m, 80 m
- **Late night** (00:00–03:00): 80 m DX peak, 160 m possible
- **Pre-dawn** (03:00–06:00): 160 m best, 80 m for shorter paths

This is for moderate solar activity (SFI ~150). High SFI shifts everything one band higher (10 m active longer, 6 m possible during the day). Low SFI shifts everything one band lower (20 m may be the highest band that opens at all).

## When in doubt

If none of the above seems to be working:

1. **Check the cluster.** If everyone else is hearing things you're not, your antenna or your noise floor is the issue, not propagation.
2. **Try FT8.** It's the lowest-SNR mode in common use; if anything is going to come through, FT8 will hear it.
3. **Drop a band.** When the band you expected to be open isn't, the next-lower one usually is.
4. **Wait 30 minutes.** Conditions can change fast around the transitions (greyline, sunset, sunrise).
5. **Walk away.** Some days the bands are just bad. Read §01-05 — where in the solar cycle are we? Maybe today's not a DX day.

## See also

- §01-01 — what SFI / K / A are telling you
- §01-03 — greyline windows
- §01-06 — sporadic E seasons
- §01-07 — prediction models for serious planning
