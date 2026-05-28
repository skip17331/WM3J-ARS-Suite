---
id: 01-06
title: Sporadic E, TEP, Skip
chapter: 01
section: 06
level: mixed
status: draft
---

# Sporadic E, TEP, Skip

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The regular F2 propagation in §01-04 follows predictable rules. These three phenomena don't. They ignore the solar cycle, they show up out of nowhere, and they make the high bands and the low VHF do things they "shouldn't." Once you learn the patterns, you can chase them — but they will always have the last word.

## Sporadic E — the wildcard

**Sporadic E (Es)** is intense, patchy ionization in the E-layer that can reflect signals well into the VHF range. It opens **6 m, 10 m, 12 m**, and on rare occasions **2 m** to distances of 700–2200 km in a single hop, regardless of what F2 is doing.

### When it happens

| Hemisphere | Peak season | Secondary peak |
|------------|-------------|----------------|
| Northern   | May–early August | Late December (small) |
| Southern   | November–February | Late June (small) |

The summer peak is by far the dominant event — for two months, 6 m and the upper HF bands are the place to be at any latitude between about 25° and 65°.

Within the season, Es openings tend to:

- Last from minutes to hours.
- Happen most often **mid-morning and again late afternoon** (local time).
- Cluster in geographic patches — one operator hears Spain loud while a neighbor hears nothing.
- Not predict each other day-to-day. Yesterday's monster opening tells you nothing about today.

### Why it works

The exact mechanism is still debated, but the leading theory is **wind shear**: opposing tidal winds in the upper E region trap ionized particles (especially metals from meteor ablation — iron, magnesium, sodium) into thin layers. When these layers get dense enough, they reflect signals up to the VHF. The metallic ions recombine slowly enough to stay around for hours.

> **Advanced —** The wind-shear hypothesis (Whitehead 1961, refined by many since) explains the temporal patterns and the metallic-ion content seen in rocket data. The horizontal extent of an Es cloud is typically 50–500 km; vertical extent is just 1–2 km — these are thin sheets, not clouds. Reflection efficiency is very high; loss can be under 1 dB per hop. The MUF for a single Es hop is sometimes called the **Es-MUF** or **fEs**; values reach 50 MHz routinely in summer, 100 MHz on big days, and over 200 MHz on the absolute monster days. Multi-hop Es paths are common and add roughly geometrically.

### Practical operating

- Watch **6 m** in summer. When the band opens, the pattern is "weak, weak, weak, then everyone in Italy is loud." Be ready to call.
- Use **PSK Reporter** with the band filter set to 50 MHz to see Es openings forming in real time.
- Listen for the **beacons** — the IBP/NCDXF beacons on 14, 18, 21, 24, 28 MHz won't tell you much (they're CW on HF), but dedicated 6 m beacons give an instant Es indicator.
- **Multi-hop Es** can put EU-to-US contacts through on 6 m even though no F2 is supporting it. These are rare but real, mostly between 14:00 and 18:00 UTC during the summer Es peak.

## TEP — Trans-Equatorial Propagation

**TEP** is a special form of F2 propagation that links stations on opposite sides of the geomagnetic equator. It opens VHF paths — sometimes well above 144 MHz — between (e.g.) South America and the southern US, or southern Europe and southern Africa.

### When it happens

- **Late afternoon and evening hours** at the path midpoint (which is on or near the geomagnetic equator).
- **Equinox months** — March/April and September/October are the peak.
- Around solar maximum, where TEP can support **2 m** contacts; at solar minimum it's mostly 6 m.

### Why it works

The geomagnetic equator does odd things to the F2 layer. Ionization is forced upward by neutral winds and the vertical electric field, creating two **equatorial anomaly crests** north and south of the geomagnetic equator. Signals launched from one anomaly crest can refract along the field-aligned plasma between the crests, with surprisingly low loss, and return to ground at the matching crest in the opposite hemisphere.

This is **not** simple two-hop F2. It's a coupled-mode propagation that follows the magnetic field geometry, and it's why it works at frequencies that "shouldn't" propagate F2 at all.

> **Advanced —** Two TEP modes are distinguished: **afternoon TEP**, which is the slower-fading mode supporting MUFs up to ~50–60 MHz; and **evening TEP**, the spread-F mode that can punch up to 144 MHz and beyond at solar maximum. Evening TEP signals show characteristic flutter and echo because of the spread-F structure they pass through. Both modes require the path midpoint to lie roughly on the geomagnetic equator (not the geographic equator — the dip is near 0° magnetic latitude).

### Practical operating

- **6 m operators in southern Europe** routinely work southern Africa via TEP at solar max evening hours.
- **6 m and 2 m operators in the southern US (FL, TX) and Caribbean** work into Argentina, Brazil, Chile via afternoon and evening TEP.
- Watch for the characteristic "flutter" — TEP signals often have a fast multipath fade that's a tell.
- Plan around **22:00–02:00 UTC at the path midpoint** for evening TEP openings.

## Skip distance and the skip zone

**Skip** is the general term for ionospheric propagation that returns to Earth at a distance — as opposed to ground wave that hugs the surface. The geometry of skip creates a **skip zone**: a ring around your station where signals just don't reach.

### Why the skip zone exists

For each band, there is:

1. A **maximum ground-wave range** — typically a few tens of km on HF.
2. A **minimum skip distance** — the closest a one-hop F2 signal can land, set by the take-off-angle limit of your antenna.

Between these two ranges, you simply can't be heard. Or hear them.

Typical numbers (mid-cycle, mid-day, mid-latitude):

| Band | Max ground wave | Min skip | Skip zone |
|------|-----------------|---------:|----------:|
| 80 m | ~150 km | ~600 km | ~450 km |
| 40 m | ~100 km | ~800 km | ~700 km |
| 20 m | ~50 km | ~1000 km | ~950 km |
| 15 m | ~30 km | ~1500 km | ~1500 km |
| 10 m | ~20 km | ~2000 km | ~2000 km |

So if you live in Atlanta and you can easily work London on 10 m but cannot reach Charlotte (450 km) on the same band — that's the skip zone. Drop to 20 m and Charlotte is in range.

### How to bridge the skip zone

- **Use a lower band.** The skip zone shrinks dramatically as you go down in frequency.
- **Use a high-take-off-angle antenna** (NVIS — near-vertical incidence skywave) on a low band. NVIS on 40 m or 80 m can fill in your local zone for 100–500 km.
- **Wait for greyline.** D-layer disappearance puts low bands into play.
- **Use a high antenna for low angle DX, a low antenna for short distance** — these are not in conflict; you're filling different parts of the propagation map.

### Skip distance varies with everything

The numbers above are rough. Skip distance changes with:

- **Time of day** — daytime skip on 40 m is 800 km; nighttime can be 1500+ km
- **Solar activity** — high SFI raises skip distance because the F2 layer can support longer hops
- **Geomagnetic conditions** — disturbed ionosphere can produce wildly variable skip
- **Antenna take-off angle** — a low-angle antenna pushes minimum skip out further than a high-angle one

## How these three relate

| Phenomenon | What's reflecting | Bands affected | Predictable? |
|------------|-------------------|----------------|--------------|
| Sporadic E | Anomalous E-layer ion clouds | 10 m, 6 m, 2 m | Seasonally; daily no |
| TEP | Field-aligned equatorial F2 | 6 m, 2 m | Seasonally; time-of-day yes |
| Skip zone | Geometry of normal F2 | All HF | Yes — varies smoothly |

Sporadic E and TEP both produce VHF openings that "should be impossible." They have different signatures (Es is patchy and short-fading; TEP is fluttery and predictable). Operators on 6 m learn to tell them apart in seconds.

## See also

- §01-04 — the regular E and F2 layers; what "anomalous" means relative to them
- §01-05 — solar cycle, which sets how high TEP can punch (and not much else for Es)
- §01-08 — quick band choice when one of these is happening
