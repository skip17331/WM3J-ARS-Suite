# Contest Map Roadmap

The first map (ARRL/RAC sections) shipped in V1 — 78 sections, one
`arrl-sections.json` resource, opened from **Contest menu → "Section Map…"**.

This document plans the other map types j-log contests would benefit from.
Tiers reflect engineering effort, not value.

## Tier 0 — already shipped

| Map | Resource | Wired into |
|---|---|---|
| **ARRL/RAC Sections** | `j-log/src/main/resources/com/jlog/maps/arrl-sections.json` (~50 KB, 83 sections, NE inset) | Menu item `menuSectionMap`; `ArrlSectionMap` class; refresh in `ContestLogController.updateStats()`. Aliases: `ONN/ONE/ONS/GH→ON`, `HI→PAC`, `NT/NU/YT→TER`, `FL→NFL/SFL/WCF` |
| **DXCC world map** | `j-log/src/main/resources/com/jlog/maps/dxcc-entities.json` (149 KB, 217 entities, 3096 prefix aliases) | Menu item `menuWorldMap`; `DxccMap` class; refresh in `ContestLogController.refreshMapsWorked()`. cty.dat-driven; ~130 remote-island entities + sub-country splits (AK/HI/Sardinia/UK regions/etc.) deferred to V2 |
| **States + Provinces** | `j-log/src/main/resources/com/jlog/maps/us-states.json` (52 KB, 65 entities — 50 states + DC + PR + VI + 12 CA provinces) | Menu item `menuStatesMap`; `StatesMap` class; refresh in both per-mode-mults and section-tracker branches of `updateStats()`. NU dropped (Arctic islands). Covers ARRL DX DX-side CW/SSB + RTTY Roundup |
| **CQ Zones** | `j-log/src/main/resources/com/jlog/maps/cq-zones.json` (31 KB, 40 zones, 9 numeric aliases) | Menu item `menuCqZonesMap`; `CqZoneMap` class. Parsed from MIT-licensed `go-cq-zone` Go source. Covers CQ WW CW/SSB/RTTY/Digital |
| **County maps (per state)** | `j-log/src/main/resources/com/jlog/maps/county-<state>.json` | Generic `CountyMap` class + dynamic menu `menuCountyMap`. Framework supports any state by adding a `CODE_TABLES` entry in `build_counties.py`. Shipped: CA (58 counties, 23 KB). TODO: TX, WA, NC, FL, GA, MI, NY, OH, PA, BC, ON, QC |

## Tier 1 — free reuse (no new data)

These contests use the **same ARRL sections** as Sweepstakes. Just enable the
menu item for them.

| Contest | Plugin | Notes |
|---|---|---|
| ARRL Field Day | `arrl_field_day.json` | Same sections, different received-section field (`sect_rcvd`) |
| ARRL Rookie Roundup (CW/SSB/RTTY) | `arrl_rookie_roundup_*.json` | Uses subset of ARRL sections |

**Status:** Field Day enabled in this work. Rookie Roundup variants only need
the same plugin tweak (drop in-cockpit pane) — left as a follow-up since
they're a low-traffic family of contests.

## Tier 2 — cheap (TIGER counties already on disk)

Multiplier = states + provinces. Polygons are a subset of what
`build_sections.py` already produces (just don't sub-divide).

| Contest | Plugin | Multipliers |
|---|---|---|
| ARRL DX (DX side, CW + SSB) | `arrl_dx_{cw,ssb}_dx.json` | 50 states + 13 provinces |
| ARRL RTTY Roundup | `arrl_rtty_ru.json` | Same |

**Path:** New script `build_states.py` reusing TIGER + Natural Earth, emits
`states.json` (~30 KB est.). New `StatesMap.java` class same API as
`ArrlSectionMap`. New menu item "States Map…" gated on
`multiplierType == "states"`.

## Tier 2b — cheap, per-state (also TIGER counties)

State QSO Parties — multiplier is *one state's counties*. Each is small.

| Contest | Plugin |
|---|---|
| California QSO Party | `ca_qso_party.json` (58 counties) |
| Texas QSO Party | `tx_qso_party.json` (254 — biggest, will need aggressive simplification) |
| Washington QSO Party | `wa_qso_party.json` (39) |
| North Carolina QSO Party | `nc_qso_party.json` |
| Pennsylvania QSO Party | `pa_qso_party.json` (existence TBD) |
| Maryland-DC QSO Party | `md_dc_qso_party.json` |
| Georgia QSO Party | `ga_qso_party.json` |
| British Columbia QSO Party | `bc_qso_party.json` (regional districts, not counties) |

**Path:** Generalise `build_sections.py` to take a state-FIPS arg and produce
`counties-<state>.json`. Single `CountyMap.java` class that loads
`counties-<state>.json` based on the active plugin's declared state.

## Tier 3 — moderate (new shapefile / zone data)

### CQ Zones (40 numbered zones, global)

Used by ~6 high-traffic plugins:

| Contest | Plugin |
|---|---|
| CQ WW DX (CW/SSB) | `cq_ww_{cw,ssb}.json` |
| CQ WW RTTY | `cq_ww_rtty.json` |
| CQ WW Digital | `cq_ww_digi.json` |
| CQ WPX (CW/SSB) | `cq_wpx_{cw,ssb,rtty}.json` (zones are secondary mult here) |
| All Asian DX (CW/SSB) | `all_asian_dx_*.json` |

**Path:** Source CQ zone polygons as KML (public-domain copy on many
contesting sites — verify license; or hand-derive from CQ Zone definitions
which are documented as bounding lines). Same pipeline: simplify, project to
world Mercator, emit `cq-zones.json` (~50 KB est., 40 polygons).

### DXCC countries (world map) — biggest payoff

Used by ~25 plugins (every `multiplierType: "dxcc"`).

| Contest family | Example plugin |
|---|---|
| ARRL DX (W/VE side) | `arrl_dx_{cw,ssb}_us.json` |
| Baltic Contest | `baltic.json` |
| Oceania DX | `oceania_dx_{cw,ssb}.json` |
| CQ Ironman | `cq_ironman.json` |
| EU HF Championship | `eu_hf_champ.json` |
| WAE (CW/SSB/RTTY) | `wae_{cw,ssb,rtty}.json` |
| (… ~15 more) | |

**Path:** Natural Earth 10m countries shapefile (already downloadable —
naciscdn.org, public domain). One wrinkle: DXCC entities don't match
countries 1:1. Examples:

- **Split**: USA → US (mainland) + AK + HI + KH0..KH9 (Pacific) + KG4 (Gitmo)
  + KP1..KP5 (Caribbean territories)
- **Split**: Italy → Italy proper + Sardinia (IS0)
- **Merged**: Several Caribbean micro-states each get one DXCC, easy
- **Phantom**: Bouvet, Heard Island, etc. — uninhabited, no separate polygon

Mitigation: ship a hand-curated `dxcc-overrides.json` that maps the dozen-or-so
problem cases. ~200 lines of work. World map output target ~150 KB.

## Tier 4 — different paradigm (procedural overlay)

### Maidenhead grid squares (ARRL VHF Contests)

Multiplier is the 4-character grid (FN20, FM19, etc.). There are 324
two-letter "fields" and 100 squares per field. For VHF contests, US/CA dx
typically lands in a few dozen relevant fields.

**Path:** No shapefile needed — procedurally generate the grid overlay in
JavaFX as a `GridPane` of rectangles positioned by Maidenhead math. Use the
same worked/click CSS classes. Background: a thin US/CA outline (reuse
`states.json` from Tier 2). Effort: one new class, no new data.

## Tier 5 — out of scope / not geographic

Many "custom" multiplier-type plugins don't have a useful map. Examples:

- **Russian DX** — oblast codes; could be a Russia oblast map (separate
  shapefile), but low traffic outside Russia.
- **Worked All Germany** — DOK codes; not geographic in any clean sense.
- **JIDX** — prefectures; could be a Japan map (~47 polygons).
- **Sprint contests, RSGB 80m, AP Sprint** — multipliers are calls/clubs.
  No useful map.

Defer indefinitely.

## Pipeline reuse plan

All future maps follow the same shape:

```
tools/arrl-map/
├─ data/                       # raw shapefiles + section/zone tables
├─ scripts/
│  ├─ build_sections.py        # done — Tier 0
│  ├─ build_states.py          # Tier 2
│  ├─ build_counties.py        # Tier 2b (parameterised by state)
│  ├─ build_cq_zones.py        # Tier 3
│  └─ build_dxcc.py            # Tier 3
└─ output → j-log/src/main/resources/com/jlog/maps/*.json
```

Each Java class (`ArrlSectionMap`, future `StatesMap`, `CountyMap`,
`CqZoneMap`, `DxccMap`) implements the same `setAllWorked / setCurrent /
setOnRegionClicked / setTooltipProvider` surface. Refactor into a
`MapPaneBase` once two of them exist.

Menu items: one per map type, each gated on the active plugin's
`multiplierType`. Future state: a single dynamic "Map…" menu item that
picks the right map for the loaded contest.
