# ARRL Section Map Pipeline

Build a bundled SVG-path resource for the j-log Sweepstakes cockpit, replacing
the existing `RegionMapPane.ssSections()` tile-grid with real geographic
section polygons.

This directory holds intermediate data and scripts. **Only the final
`arrl-sections.json` (a few hundred KB) ships in the j-log jar** —
everything else stays here for reproducibility.

## Status (as of 2026-05-12)

Phase progress, matching tasks #9–#14:

| Phase | Task | Status |
|-------|------|--------|
| 1 | US section→county JSON | DONE — `data/section-to-counties.json` |
| 1b | Canada RAC section→CD JSON | DONE — `data/section-to-csd-canada.json` (Parry Sound split between ONN/ONS still needs CSD-level resolution) |
| 2 | TIGER county shapefile + StatsCan province shapefile | TODO |
| 3 | Dissolve + simplify + project (Python/GeoPandas) | TODO — see `scripts/build_sections.py` (not yet written) |
| 4 | Emit `arrl-sections.json` resource | TODO |
| 5 | Implement `ArrlSectionMap` JavaFX class | TODO |
| 6 | Swap into `ContestLogController.buildSectionPane` | TODO |

## Data Sources & Licensing

- **US section→county table:** Compiled from <https://www.arrl.org/section-boundaries>.
  Facts (which county is in which section) are not copyrightable; we list them
  ourselves in `data/section-to-counties.json` for the pipeline.
- **US county polygons:** US Census TIGER/Line shapefiles (public domain).
  Recommended file: `tl_YYYY_us_county.zip` from
  <https://www2.census.gov/geo/tiger/TIGER2024/COUNTY/>.
- **Canadian province polygons:** Statistics Canada GeoBase
  (public, attribution: Statistics Canada) —
  <https://www12.statcan.gc.ca/census-recensement/2021/geo/sip-pis/boundary-limites/index2021-eng.cfm>.
- **mapability.com reference image:** **DO NOT** redistribute. Visual target only.

## Coordinate System Plan

- Contiguous US: **Albers Equal-Area Conic** (EPSG:5070 / NAD83 CONUS Albers).
- Alaska, Hawaii, Puerto Rico, US Virgin Islands: render as **insets**
  (separate transforms, positioned bottom-left/under-FL like the d3.geoAlbersUsa convention).
- Canada: **Lambert Conformal Conic** (EPSG:3347 / NAD83 Canada LCC).
- Final 2D coordinates packed into a single output canvas (target: 1200×800).

## Output Schema (Phase 4)

`src/main/resources/com/jlog/maps/arrl-sections.json`

```json
{
  "viewBox": [0, 0, 1200, 800],
  "sections": {
    "EPA": {
      "svgPath": "M ... Z",
      "labelX":  812,
      "labelY":  336,
      "bbox":    [780, 318, 845, 360]
    },
    "...": { }
  }
}
```

The Java side reads this once at startup and constructs one `SVGPath` per
section. Coloring is purely CSS (`region-tile-worked` etc.), reusing the
existing pane styles.

## Known Gaps / Open Questions

- Canadian RAC section detail (ONN/ONE/ONS/GTA sub-divisions inside Ontario,
  MAR=Maritimes umbrella vs. NS/NB/PE individually) — need RAC reference,
  not in this file yet.
- `arrl_sweepstakes_cw.json` and `arrl_sweepstakes_ssb.json` are missing WV
  and NNY from their `sections` list. Either update those plugins or have
  the map silently skip sections not in the active plugin. Defer until phase 5.
- PAC = Hawaii + Pacific outlying. Bundled map will render HI only;
  Guam/AS/Wake are too rarely worked to matter visually.

## How to Resume (next session)

1. Read this README + `data/section-to-counties.json`.
2. Pick up Phase 1b (Canada) or skip directly to Phase 2 (download TIGER).
3. Scripts go in `scripts/`. Suggested deps: `geopandas`, `shapely`,
   `pyproj`. Use a venv in `tools/arrl-map/.venv/`.
4. Tasks #9–14 in the task list track end-to-end progress.
