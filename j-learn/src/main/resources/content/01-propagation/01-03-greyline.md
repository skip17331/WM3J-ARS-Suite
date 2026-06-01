---
id: 01-03
title: Greyline Propagation
chapter: 01
section: 03
level: simple
status: published
---

# Greyline Propagation

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The **greyline** — also called the **terminator** — is the moving line on Earth where day meets night. For about 30 to 90 minutes around your local sunrise and sunset, this line gives you a propagation gift: long, low-angle DX paths on bands that normally won't go anywhere near that distance.

## Why greyline works

Two things have to happen at the same time for greyline magic:

1. **The D-layer disappears.** D-layer ionization (chapter §01-04) needs sunlight. As soon as the sun drops below the horizon at any point along your path, that part of the D-layer starts to vanish. With no D-layer, the strong daytime absorption on 80 m, 40 m, and 160 m vanishes too.
2. **The F-layer is still ionized.** F-layer ionization persists for hours after sunset because the upper atmosphere is too thin for ions to recombine quickly. So the F-layer is still up there, ready to bend signals back to Earth.

Combine "no D-layer absorption" with "F-layer still working" along a path that **runs along the greyline itself** (not crossing in or out of darkness), and you get a low-loss, long-distance corridor for the lower bands.

## When and how long

Greyline windows last roughly:

- **Tropics**: 15–25 minutes — sun rises and sets fast.
- **Mid-latitudes** (US, Europe): 30–60 minutes.
- **High latitudes** (UK, Scandinavia, southern Australia): 1–2 hours, sometimes more in summer.
- **Polar circles in summer**: hours; the greyline sits on the horizon and barely moves.

There are **two openings per day** — one at sunrise and one at sunset. Sunrise greyline often outperforms sunset because the F-layer is still freshly ionized from yesterday and has had a long quiet night to settle into a smooth gradient.

## Best bands for greyline

| Band | When greyline helps | Why |
|------|---------------------|-----|
| **160 m** | Always | Daytime absorption is brutal; the only reason 160 m DX is ever possible |
| **80 m** | Always | Same reason as 160 m; 80 m DX often goes from impossible to easy at sunrise |
| **40 m** | Often | Even mid-bands open dramatically as the D-layer fades |
| **30 m** | Sometimes | Helps especially in winter when the day window is short |
| **20 m and up** | Rarely the dominant effect | These bands work fine in daylight; greyline doesn't add as much |

## The geometry

The propagation gift only exists when the **path runs along the greyline**, not perpendicular to it. If you're in Florida and you want Italy at sunrise, that path runs roughly east-west — along the greyline — and works beautifully. If you want to work New Zealand from Florida at sunrise, that path runs north-south and you get no greyline boost.

A simple test: imagine the terminator drawn on a globe. If the great-circle path between you and the DX station follows or runs close to that line, greyline is your friend. If the path crosses the terminator at a steep angle, it's a normal day or night path.

## How to actually use it

1. **Know your local sunrise/sunset times.** The radio doesn't care about civil twilight; it cares about geometric sunrise — when the sun's center crosses the horizon. Most prediction tools and rotator software show the terminator on a world map.
2. **Be on the radio 30 minutes before sunrise** if you're chasing 80 m or 160 m DX. The opening can start before you'd expect.
3. **Look at the world map and ask "who else is in greyline right now?"** If you're at sunrise and Western Europe is at sunset, the path between you can be wide open even though it'd be hopeless an hour later.
4. **Try CW or FT8** — voice modes need more signal, and greyline windows can be marginal. Digital modes can pull a contact through where SSB cannot.
5. **Listen first.** Greyline often produces signals that sound surprisingly weak at one moment and surprisingly loud the next. The peak is brief; don't waste it calling CQ blind.

## Common targets by region

If you live on the US East Coast:

- **Sunrise greyline:** Africa, Middle East, Indian Ocean — usually 11–13 UTC depending on season. Loud signals from VU, A4, S2, Z3.
- **Sunset greyline:** Pacific — VK, ZL, KH6 on 80 m and 40 m around 22–24 UTC in summer.

If you live in Western Europe:

- **Sunrise:** Pacific including KH6, JA on 80 m.
- **Sunset:** West Coast US, sometimes deep into the Pacific.

If you live on the US West Coast:

- **Sunrise:** Europe, Africa, sometimes deep Asia.
- **Sunset:** Indian Ocean, Africa via long path.

## Tools

- **DR2W terminator map** — `dr2w.de/sun.html` — clean visual of where the greyline is right now.
- **PSK Reporter at sunrise** — set the band to 80 m or 160 m and watch a "wave" of spots from stations entering greyline.
- **J-Map's grayline overlay** — same idea, integrated with the suite. Shows the day/night line with a 6° twilight band.

## A few realistic expectations

- Greyline is not a guarantee. Bad solar conditions can kill the opening before it forms.
- Greyline windows on 160 m can be as short as 5 minutes during summer.
- The very best 160 m DX of the year happens in winter, on greyline, when the geomagnetic field is quiet.
- The best signals in the window are often the ones who heard the band quieting and got there early.

## See also

- §01-04 — what the D-layer is doing during the day, and why it shuts down at sunset
- §01-08 — quick band-choice cheat sheet that includes greyline windows
