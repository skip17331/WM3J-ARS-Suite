#!/usr/bin/env python3
"""
Build per-state county maps for state QSO party plugins.

Each state QSO party uses its own 3- or 4-letter county code scheme
(CA: ALAM, ALPI, AMAD…; TX: AND, ANG, ARM…). We hand-curate the
code→county-name mapping per state, then dissolve the matching TIGER
county polygons and emit one map JSON per state.

Output: j-log/src/main/resources/com/jlog/maps/county-<state>.json
Schema mirrors arrl-sections.json (viewBox / sections / aliasTargets).

Usage:
  cd tools/arrl-map
  .venv/bin/python scripts/build_counties.py CA
  .venv/bin/python scripts/build_counties.py TX
  .venv/bin/python scripts/build_counties.py all
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

import geopandas as gpd
from shapely.geometry import Polygon, MultiPolygon
from shapely.ops import transform as shp_transform

HERE = Path(__file__).resolve().parent.parent
DATA = HERE / "data"
COUNTY_SHP = DATA / "us_county" / "tl_2024_us_county.shp"
OUT_DIR = HERE.parent.parent / "j-log" / "src" / "main" / "resources" / "com" / "jlog" / "maps"

# State FIPS codes (subset relevant to QSO parties we know about).
STATE_FIPS = {
    "CA": "06", "TX": "48", "WA": "53", "NC": "37", "FL": "12",
    "GA": "13", "MI": "26", "NY": "36", "OH": "39", "PA": "42",
    "BC": None, "ON": None, "QC": None,  # Canadian — handled separately
}

# Single-state US Albers projection — keeps each state's geometry compact
# and undistorted at the canvas scale.
ALBERS_US = (
    "+proj=aea +lat_1=29.5 +lat_2=45.5 +lat_0=37.5 +lon_0=-96 "
    "+ellps=GRS80 +datum=NAD83 +units=m +no_defs"
)

VIEW_W = 900
VIEW_H = 700
MARGIN = 30
SIMPLIFY_TOL_M = 2_000  # 2 km — fine detail at county scale


# Per-state code → TIGER county name mapping. Keys are the 3-/4-letter codes
# in the contest's `multiplierList`; values are the TIGER NAME column value.
# Add a new entry per state by hand once and the polygon work is automatic.

CA_CODE_TO_NAME = {
    "ALAM": "Alameda", "ALPI": "Alpine", "AMAD": "Amador", "BUTT": "Butte",
    "CALA": "Calaveras", "COLU": "Colusa", "CONT": "Contra Costa",
    "DELN": "Del Norte", "ELDO": "El Dorado", "FRES": "Fresno",
    "GLEN": "Glenn", "HUMB": "Humboldt", "IMPE": "Imperial", "INYO": "Inyo",
    "KERN": "Kern", "KING": "Kings", "LAKE": "Lake", "LASS": "Lassen",
    "LOSA": "Los Angeles", "MADE": "Madera", "MARI": "Marin",
    "MARP": "Mariposa", "MEND": "Mendocino", "MERC": "Merced",
    "MODO": "Modoc", "MONO": "Mono", "MONT": "Monterey", "NAPA": "Napa",
    "NEVA": "Nevada", "ORAN": "Orange", "PLAC": "Placer", "PLUM": "Plumas",
    "RIVE": "Riverside", "SACR": "Sacramento", "SBEN": "San Benito",
    "SBER": "San Bernardino", "SDIE": "San Diego", "SFRA": "San Francisco",
    "SJOA": "San Joaquin", "SLUI": "San Luis Obispo", "SMAT": "San Mateo",
    "SBAR": "Santa Barbara", "SCLA": "Santa Clara", "SCRU": "Santa Cruz",
    "SHAS": "Shasta", "SIER": "Sierra", "SISK": "Siskiyou", "SOLA": "Solano",
    "SONO": "Sonoma", "STAN": "Stanislaus", "SUTT": "Sutter",
    "TEHA": "Tehama", "TRIN": "Trinity", "TULA": "Tulare", "TUOL": "Tuolumne",
    "VENT": "Ventura", "YOLO": "Yolo", "YUBA": "Yuba",
}

CODE_TABLES = {
    "CA": CA_CODE_TO_NAME,
    # TX, WA, NC, etc. — these state QSO parties happen to use codes that
    # are listed in pure alphabetical order matching the TIGER county
    # alphabetical order, so positional matching (see fall-through in
    # build_state) covers them without a hand-curated table.
}

# Per-state TIGER-name rewrites that affect ONLY the sort order used by the
# positional-matching fallback. Use when the contest authors sorted under a
# different name than TIGER's canonical one (e.g., Florida's contest still
# uses "Dade" while TIGER renamed it to "Miami-Dade" in 1997). Geometry is
# still pulled by the TIGER name.
SORT_NAME_OVERRIDES = {
    "FL": {"Miami-Dade": "Dade"},
}

# Path to the contest-specific code list shipped in j-log-engine. The build
# loads this when the state has no explicit CODE_TABLES entry and pairs
# codes to TIGER counties positionally (both sorted alphabetically).
ENGINE_COUNTIES_DIR = (
    HERE.parent.parent / "j-log-engine" / "src" / "main" / "resources"
    / "com" / "jlog" / "counties"
)


def polygon_to_svg(geom) -> str:
    parts = []
    polys = []
    if isinstance(geom, Polygon):
        polys = [geom]
    elif isinstance(geom, MultiPolygon):
        polys = list(geom.geoms)
    else:
        return ""
    for poly in polys:
        rings = [poly.exterior] + list(poly.interiors)
        for ring in rings:
            coords = list(ring.coords)
            if not coords:
                continue
            d = " ".join(
                f"{'M' if i == 0 else 'L'}{x:.1f},{y:.1f}"
                for i, (x, y) in enumerate(coords)
            )
            parts.append(d + " Z")
    return " ".join(parts)


def build_state(state_code: str, counties_gdf: gpd.GeoDataFrame) -> Path:
    fips = STATE_FIPS.get(state_code)
    if fips is None:
        raise SystemExit(f"State {state_code!r} not in STATE_FIPS or is Canadian (TBD)")

    table = CODE_TABLES.get(state_code)
    if table is None:
        # Fall back to positional-alphabetical matching. Works when the
        # contest's code list is ordered alphabetically by county and has
        # the same count as TIGER (true for CA, TX, and most US state QPs).
        codes_path = ENGINE_COUNTIES_DIR / f"{state_code.lower()}.json"
        if not codes_path.exists():
            raise SystemExit(
                f"No code table for {state_code!r}, and no contest codes file at {codes_path}"
            )
        codes = json.loads(codes_path.read_text())
        overrides = SORT_NAME_OVERRIDES.get(state_code, {})
        # Sort TIGER names with spaces stripped + case-folded so multi-word
        # counties (El Paso, San Jacinto, Red River, …) interleave naturally
        # with single-word neighbours — the same ordering contest authors
        # use when assigning codes. Per-state overrides handle renames
        # (e.g. Miami-Dade ↔ Dade).
        def sort_key(s: str) -> str:
            return overrides.get(s, s).replace(" ", "").replace(".", "").lower()
        tiger_names = sorted(
            counties_gdf[counties_gdf["STATEFP"] == fips]["NAME"].tolist(),
            key=sort_key,
        )
        if len(codes) != len(tiger_names):
            raise SystemExit(
                f"{state_code}: {len(codes)} codes vs {len(tiger_names)} TIGER counties — "
                "positional match impossible; add a CODE_TABLES entry."
            )
        table = dict(zip(codes, tiger_names))
        print(f"  using positional-alphabetical mapping ({len(table)} codes)")

    state_counties = counties_gdf[counties_gdf["STATEFP"] == fips].copy()
    name_to_geom = {row["NAME"]: row.geometry for _, row in state_counties.iterrows()}

    # Build the (code, geometry) list, warning on misses.
    rows = []
    missing = []
    for code, county_name in table.items():
        g = name_to_geom.get(county_name)
        if g is None:
            missing.append((code, county_name))
            continue
        rows.append({"id": code, "name": county_name, "geometry": g})
    if missing:
        print(f"  WARNING: {len(missing)} codes have no TIGER match:")
        for c, n in missing[:10]:
            print(f"    {c} -> {n}")

    gdf = gpd.GeoDataFrame(rows, crs=counties_gdf.crs).to_crs(ALBERS_US)
    gdf["geometry"] = gdf.geometry.simplify(SIMPLIFY_TOL_M, preserve_topology=True)

    minx, miny, maxx, maxy = gdf.total_bounds
    src_w = maxx - minx
    src_h = maxy - miny
    scale = min((VIEW_W - 2 * MARGIN) / src_w, (VIEW_H - 2 * MARGIN) / src_h)
    cw = src_w * scale
    ch = src_h * scale
    ox = (VIEW_W - cw) / 2.0
    oy = (VIEW_H - ch) / 2.0
    def tx(x, y):
        sx = (x - minx) * scale + ox
        sy = VIEW_H - ((y - miny) * scale + oy)
        return sx, sy
    gdf["geometry"] = gdf.geometry.apply(
        lambda g: shp_transform(lambda x, y, z=None: tx(x, y), g)
    )

    out = {
        "viewBox":      [0, 0, VIEW_W, VIEW_H],
        "state":        state_code,
        "aliasTargets": {},
        "sections":     {},
    }
    for _, row in gdf.iterrows():
        path = polygon_to_svg(row.geometry)
        if not path:
            continue
        cx, cy = row.geometry.representative_point().coords[0]
        bx0, by0, bx1, by1 = row.geometry.bounds
        out["sections"][row["id"]] = {
            "svgPath": path,
            "labelX":  round(cx, 1),
            "labelY":  round(cy, 1),
            "bbox":    [round(bx0, 1), round(by0, 1), round(bx1, 1), round(by1, 1)],
            "name":    row["name"],
        }

    out_path = OUT_DIR / f"county-{state_code.lower()}.json"
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(json.dumps(out, separators=(",", ":")))
    print(f"Wrote {out_path} ({out_path.stat().st_size/1024:.1f} KB, "
          f"{len(out['sections'])} counties)")
    return out_path


def main():
    if len(sys.argv) < 2:
        print("Usage: build_counties.py <STATE_CODE | all>", file=sys.stderr)
        sys.exit(2)

    if sys.argv[1] == "all":
        # All US states that have both a TIGER FIPS code and a contest
        # codes file shipped in j-log-engine.
        targets = [s for s, f in STATE_FIPS.items()
                   if f is not None
                   and (ENGINE_COUNTIES_DIR / f"{s.lower()}.json").exists()]
    else:
        targets = [sys.argv[1].upper()]

    print("Loading TIGER counties…")
    counties = gpd.read_file(COUNTY_SHP)

    for st in targets:
        print(f"Building {st}…")
        build_state(st, counties)


if __name__ == "__main__":
    main()
