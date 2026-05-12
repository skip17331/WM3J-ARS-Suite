---
id: 23-08
title: Regional Propagation Quirks
chapter: 23
section: 08
level: mixed
status: draft
---

# Regional Propagation Quirks

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## Why "quirks"

Standard HF propagation theory — F2 short-skip, MUF cycling with the sun, grayline at the terminator — covers most of what you'll see on the air. But the ionosphere isn't a uniform layer; it bulges, tilts, gets dented by the geomagnetic field, and has localized anomalies. The result is that certain *paths* have characteristic behaviors that don't show up in textbook propagation models.

This section catalogs the major regional quirks every active DXer eventually learns. Each is presented as a short paragraph with concrete tactical advice — when it happens, how to recognize it, what to do.

## Pacific long-path

The "short" great-circle path from the US East Coast to East Asia goes over the Arctic — but Arctic D-layer absorption often kills it during solar disturbances or in winter at high K-index. The "long" path goes the *other* way: out over the Pacific, across Asia, around the world. It's about 25,000 km versus 11,000 km for short-path, but on 20 m and lower it often beats the short path.

Recognition: signals from JA, BY, or HL are coming in from the *west* (toward the Pacific) instead of from the *northwest* (toward the pole) as your beam would normally suggest. The signal often has a peculiar slow flutter — the long path goes through more ionosphere, so multipath dispersion is more pronounced.

Tactically: beam west during morning grayline (09:00–11:00 UTC for US East Coast). Listen on 20 m, 30 m, or 40 m. Long-path JA on 20 m is a daily occurrence during peak solar conditions; on 40 m it's a grayline-only path.

## Polar-path absorption during SIDs

A **Sudden Ionospheric Disturbance** (SID) is an X-class solar flare that hits the day-side ionosphere with intense X-ray and UV radiation, momentarily ionizing the D layer to levels that absorb all HF below ~30 MHz for minutes to hours. The polar regions are hit hardest because the geomagnetic field funnels charged particles there.

After an SID, polar-crossing paths (US East Coast ↔ Asia, Europe ↔ Australia via the long-path over the pole) are dead for hours to days. The official term is **PCA — Polar Cap Absorption** — and it can last 2–7 days after a major flare.

Recognition: 20 m and 40 m suddenly go quiet on northern paths; signals from Europe or Asia disappear; the K-index spikes above 5. WWV at the top of the hour reports `geomagnetic field disturbed` or `severe storm in progress`.

Tactically: switch to non-polar paths. South America, Africa, Australia (via the equator) often stay open during PCA events. Or: take a day off and let the ionosphere recover.

## Caribbean openings on 6 m

The 6 m band (50 MHz) sees occasional spectacular openings to the Caribbean — Cuba, Puerto Rico, Dominican Republic, Venezuela, sometimes deep South America — via **sporadic-E** (Es) propagation. The mechanism is a thin, dense E-layer ionization cloud that refracts 6 m signals over single-hop distances of 1,000–1,500 miles.

Caribbean Es is most common in May–August, with secondary peaks in November–December. Openings can last from 15 minutes to several hours. The band typically opens from US Southeast first (FL, GA), then expands to TX, then to NC/VA, then sometimes as far north as Long Island and Boston.

Recognition: SSB and FT8 signals appear suddenly on 50.125 (SSB) and 50.313 (FT8) from CO, CT, PJ7, HI8, etc. The S-meter shows S9+ from stations that are normally inaudible. The opening can vanish in minutes.

Tactically: keep 6 m on a second VFO or auto-monitor mode during summer afternoons. When Es opens, drop everything else and operate hard — these openings are short. A modest 6 m antenna (3-element yagi or even a half-wave dipole) is enough.

## European auroral propagation

The aurora borealis is a stunning visual phenomenon and a useful HF reflector — though "useful" is generous. During geomagnetic storms (K≥5), auroral ionization at high latitudes can reflect HF and VHF signals, but with massive Doppler smearing and rapid fading.

European stations sometimes work each other via auroral backscatter on 144 MHz and 50 MHz — beaming *north* toward the aurora instead of directly at each other. The signals have a characteristic "growl" or harsh hissing sound from the Doppler smearing. SSB voice becomes unintelligible; CW becomes a buzz; only digital modes (FT8) survive consistently.

Recognition: bands above 28 MHz go dead in normal directions but suddenly come alive when beaming north. The signal sounds like an angry whisper.

Tactically: rare for North American operators since the aurora is usually too far north to reflect back to NA from anything useful. But for European and Scandinavian operators, auroral propagation during storms enables intra-EU contacts that wouldn't otherwise be possible.

## South American skew paths

The geomagnetic equator runs roughly along the Earth's geographic equator but tilts dramatically near South America — it dips far south near Brazil. The resulting **Equatorial Ionization Anomaly** (a peak in F-layer density either side of the magnetic equator) creates oddly steered propagation: signals from the US to South America often appear to be coming from a direction 20–30 degrees off the great-circle bearing.

Recognition: working PY (Brazil) or LU (Argentina) on 20 m, you find your signal peaks when beamed not at the great-circle bearing but slightly *south* of it. The path is "skewed" by the geomagnetic anomaly.

Tactically: if you have a directional antenna, sweep ±30 degrees off the GC bearing when working South America. The skew direction varies with time of day; afternoon openings often have the strongest skew.

> ⚙️ **Advanced —** The Equatorial Ionization Anomaly is two peaks of F-layer density at ±15° magnetic latitude with a "trough" at the magnetic equator. Signals crossing the equator are refracted by the gradient, not just the magnitude — so the apparent bearing changes. The effect is most pronounced near solar maximum on 20 m and 17 m. VOACAP and similar prediction tools model this; field experience often confirms.

## Trans-equatorial propagation (TEP)

A related phenomenon: **TEP** is enhanced propagation on 6 m (and occasionally 10 m) between stations both at the geomagnetic equator's *gradient*, typically around 15–30° geomagnetic latitude on either side. The mechanism involves field-aligned irregularities in the F2 region.

For US operators, TEP shows up as 6 m contacts to Argentina (LU), Chile (CE), and southern Brazil during evening hours in October–March. The path is essentially equator-jumping, with the signal traveling north-south rather than the usual east-west.

Recognition: 6 m suddenly opens from Florida or Texas to Argentina around 2200–0200 UTC. The signals are strong (S8+) and steady — different from Es, which is patchier.

Tactically: TEP is a "watch for it" mode rather than a "make it happen" mode. During evening hours in TEP season, keep 50.125 (SSB) and 50.313 (FT8) audible.

## African long-path

Africa is awkwardly placed for US operators — short-path goes nearly due east over the Atlantic, but the path angle (great-circle bearing of 60–110° from US East Coast) hits the auroral oval if you're far enough north. Many African paths from the NE US are easier *long-path* over the Pacific.

Recognition: ZS (South Africa), 5Z (Kenya), or 9J (Zambia) signals come in from the *west* on 20 m during local sunset. Long-path African openings are most reliable from the US East Coast in the 22:00–01:00 UTC window.

Tactically: beam *west* for African DX during evening grayline; beam *east* during pre-dawn grayline. Most US operators try short-path first; if it's silent, try long-path before giving up.

## Pacific Northwest to Europe

A specific tactical quirk: US Pacific Northwest (WA, OR, BC) operators trying to reach Europe face the worst-case geometry — short-path is nearly due north over the pole, long-path is nearly due south over Antarctica (effectively impossible due to high D-layer absorption over Antarctica's land mass). Even on 20 m, Europe is hard from Seattle.

Tactically: PNW operators do most of their European DXing via 20 m gray-line at PNW sunrise (15:00 UTC summer, 16:00 UTC winter), when the polar D-layer is at its thinnest. They also benefit from "off-the-side" propagation — beaming due east (toward Maine) and catching Europe via a non-great-circle skew path.

## Trans-polar paths during winter

Counter-intuitively, the polar paths (US East Coast ↔ Asia) are often *better in winter* than summer for the same geomagnetic conditions. Reasons: shorter daylight in the Arctic means less D-layer absorption; winter ionosphere structure favors high-latitude refraction.

Tactically: in mid-winter (Dec–Feb) try polar paths to JA, BY, HL on 20 m during US morning hours (12:00–15:00 UTC). The same paths in summer are often closed by D-layer absorption.

## A few short ones

A handful more, briefer:

- **20 m to South America at midnight US time** — often surprisingly open via low-angle F2 ducting; check 14.150–14.300 between 03:00–06:00 UTC.
- **80 m to Europe via grayline** — US East Coast to Europe on 80 m peaks at 22:00–23:00 UTC during EU sunrise. Often a quiet, surprisingly strong path.
- **17 m to anywhere** — the 18 MHz band is "the band nobody listens to," which means low QRM but also fewer DX stations. When 20 m is jammed, try 17 m for the same target.
- **Caribbean to Europe long-path on 40 m** — VP9 (Bermuda), 8P (Barbados), and other Caribbean stations regularly work Europe on 40 m via long-path. Strong signals can appear from the SW direction.
- **VK on 160 m at NA sunset** — Australia is reachable from US East Coast on 160 m during NA winter sunset windows; signals are very weak but copyable with patience.

## Where this knowledge comes from

None of these quirks is in the FCC test pool. They come from years of on-air experience and from the DX community's accumulated lore — DX Atlas documentation, NCDXF's beacons, the ARRL's *DXer's Handbook*, and the running commentary in chat rooms like the DX Cluster Mafia or the DX-World Slack. Active DXers calibrate their expectations on these quirks within their first few years of DXCC chasing.

The actionable takeaway: when a path that "should" work is silent, try the unexpected — different bearing, different time, different mode. Propagation is non-uniform, and the path that looks longer on the map sometimes works better than the path that looks shorter.

## See also

- [§01 — Propagation](../01-propagation/01-00-overview.md) (theory underneath all of these)
- [§01-03 — Greyline Propagation](../01-propagation/01-03-greyline.md)
- [§01-08 — Band Choice Right Now](../01-propagation/01-08-band-choice-right-now.md)
- [§23-07 — Grayline Exploitation](23-07-grayline-exploitation.md)
- [§23-01 — DXing](23-01-dxing.md)
