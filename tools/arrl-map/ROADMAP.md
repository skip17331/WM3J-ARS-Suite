# Contest Map Roadmap

All six planned contest-map types are shipped. The active roadmap is empty;
this document is now a reference for what's deployed and what's parked.

## Shipped

| Map | Resource | Wired into |
|---|---|---|
| **ARRL/RAC Sections** | `j-log/src/main/resources/com/jlog/maps/arrl-sections.json` (~50 KB, 83 sections, NE inset) | Menu item `menuSectionMap`; `ArrlSectionMap` class; refresh in `ContestLogController.updateStats()`. Aliases: `ONN/ONE/ONS/GH→ON`, `HI→PAC`, `NT/NU/YT→TER`, `FL→NFL/SFL/WCF` |
| **DXCC world map** | `j-log/src/main/resources/com/jlog/maps/dxcc-entities.json` (149 KB, 217 entities, 3096 prefix aliases) | Menu item `menuWorldMap`; `DxccMap` class; refresh in `ContestLogController.refreshMapsWorked()`. cty.dat-driven |
| **States + Provinces** | `j-log/src/main/resources/com/jlog/maps/us-states.json` (52 KB, 65 entities — 50 states + DC + PR + VI + 12 CA provinces) | Menu item `menuStatesMap`; `StatesMap` class. NU dropped. Covers ARRL DX DX-side CW/SSB + RTTY Roundup |
| **CQ Zones** | `j-log/src/main/resources/com/jlog/maps/cq-zones.json` (31 KB, 40 zones, 9 numeric aliases) | Menu item `menuCqZonesMap`; `CqZoneMap` class. Parsed from MIT-licensed `go-cq-zone` Go source. Covers CQ WW CW/SSB/RTTY/Digital |
| **County maps (per state)** | `j-log/src/main/resources/com/jlog/maps/county-<state>.json` | Generic `CountyMap` class + dynamic menu `menuCountyMap`. Shipped: CA (58), TX (254), WA (39), NC (100), FL (67), GA (159), MI (83), NY (62), OH (88), PA (67) |
| **Maidenhead grid squares** | `j-log/src/main/resources/com/jlog/maps/maidenhead-grids.json` (221 KB, 1254 cells covering -130°W to -55°W, 22°N to 55°N) | Menu item `menuGridMap`; `MaidenheadGridMap` class. Procedurally generated cells + states/provinces outline background. Worked-set accepts 6-char subsquares. Covers ARRL Jan/June/Sep VHF |

**Plugin coverage:** ~53 of 68 j-log contest plugins now have at least one
geographic map. Each opens from the Contest menu and updates live as QSOs
are logged.

## Parked

| Item | Status |
|---|---|
| Canadian QSO Party county maps (BC/ON/QC) | Blocked — Statistics Canada portal blocks direct downloads of Census Division shapefiles |
| DXCC sub-country splits (AK/HI from USA, Scotland/Wales/NI from UK, Sardinia, Spanish overseas, etc.) | V2 — needs admin-1 boundaries + hand-curated `dxcc-overrides.json` |
| 130 remote-island DXCC entities (Bouvet, Kerguelen, Spratly, etc.) | V2 — no NE country polygon exists |
| Rookie Roundup cockpit-pane cleanup | Cosmetic — map menu already works; just an unused pane reference |
| Russia oblast map, JIDX prefecture map | Out of scope — low traffic outside home regions |

## Pipeline reuse

```
tools/arrl-map/
├─ data/                       raw shapefiles + section/zone tables
├─ scripts/
│  ├─ build_sections.py        ARRL/RAC sections
│  ├─ build_states.py          US states + CA provinces
│  ├─ build_counties.py        per-state counties (parameterised; `all` mode)
│  ├─ build_cq_zones.py        CQ zones (from go-cq-zone data)
│  ├─ build_dxcc.py            DXCC entities (cty.dat + Natural Earth)
│  └─ build_grids.py           Maidenhead grid squares (procedural)
└─ output → j-log/src/main/resources/com/jlog/maps/*.json
```

Each Java class (`ArrlSectionMap`, `StatesMap`, `CountyMap`, `CqZoneMap`,
`DxccMap`, `MaidenheadGridMap`) implements the same `setAllWorked /
setCurrent / setOnRegionClicked / setTooltipProvider` surface. Now that six
exist, a refactor into a `MapPaneBase` shared class would remove ~600 lines
of duplication — not on the active roadmap, but easy follow-up.
