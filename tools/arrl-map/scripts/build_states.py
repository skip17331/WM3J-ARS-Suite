#!/usr/bin/env python3
"""
Build us-states.json — geographic US states + Canadian provinces map for
contests whose multiplier is states/provinces (ARRL DX DX-side, RTTY Roundup).

Reuses the same North-America LCC projection + Northern clip as the ARRL
section map for consistent visual style. Section IDs are 2-letter postal
codes (CT, NY, ON, BC, …). AK / HI / PR / VI rendered as special insets
like in the section map.

Output schema (mirrors arrl-sections.json):
{
  "viewBox":      [0,0,W,H],
  "insets":       [],
  "aliasTargets": {},
  "sections": {
    "<code>": { "svgPath", "labelX", "labelY", "bbox", "country" }
  }
}

Usage:
  cd tools/arrl-map
  .venv/bin/python scripts/build_states.py
"""

from __future__ import annotations

import json
from pathlib import Path

import geopandas as gpd
from shapely.geometry import Polygon, MultiPolygon, box
from shapely.ops import unary_union, transform as shp_transform

HERE = Path(__file__).resolve().parent.parent
DATA = HERE / "data"
COUNTY_SHP = DATA / "us_county" / "tl_2024_us_county.shp"
NE_ADMIN1  = DATA / "ne_admin1" / "ne_10m_admin_1_states_provinces.shp"
OUT_JSON   = HERE.parent.parent / "j-log" / "src" / "main" / "resources" / "com" / "jlog" / "maps" / "us-states.json"

# Same canvas + projection as the ARRL section map so the two look similar.
VIEW_W = 1200
VIEW_H = 800
MARGIN = 20
SIMPLIFY_TOL_M = 10_000
NORTHERN_CLIP_LAT = 70.0
NA_LCC = (
    "+proj=lcc +lat_1=20 +lat_2=60 +lat_0=40 +lon_0=-96 "
    "+ellps=GRS80 +datum=NAD83 +units=m +no_defs"
)

# US state FIPS → postal code (DC included).
FIPS_TO_POSTAL = {
    "01":"AL","02":"AK","04":"AZ","05":"AR","06":"CA","08":"CO","09":"CT","10":"DE",
    "11":"DC","12":"FL","13":"GA","15":"HI","16":"ID","17":"IL","18":"IN","19":"IA",
    "20":"KS","21":"KY","22":"LA","23":"ME","24":"MD","25":"MA","26":"MI","27":"MN",
    "28":"MS","29":"MO","30":"MT","31":"NE","32":"NV","33":"NH","34":"NJ","35":"NM",
    "36":"NY","37":"NC","38":"ND","39":"OH","40":"OK","41":"OR","42":"PA","44":"RI",
    "45":"SC","46":"SD","47":"TN","48":"TX","49":"UT","50":"VT","51":"VA","53":"WA",
    "54":"WV","55":"WI","56":"WY","72":"PR","78":"VI",
}

# Canadian province (NE 'name' column) → postal code.
PROVINCE_TO_POSTAL = {
    "Alberta": "AB", "British Columbia": "BC", "Manitoba": "MB",
    "New Brunswick": "NB", "Newfoundland and Labrador": "NL",
    "Northwest Territories": "NT", "Nova Scotia": "NS", "Nunavut": "NU",
    "Ontario": "ON", "Prince Edward Island": "PE", "Québec": "QC",
    "Saskatchewan": "SK", "Yukon": "YT",
}

# Sections drawn at fixed inset positions (same convention as
# build_sections.py). Skip AK in V1 (too large for the small free
# corner); skip NU because it's mostly Arctic islands.
SPECIAL_INSETS = {
    "HI": {"cx": 110,  "cy": 720, "size": 130, "country": "US"},
    "AK": {"cx": 110,  "cy": 130, "size": 170, "country": "US"},
    "PR": {"cx": 1090, "cy": 80,  "size": 90,  "country": "US"},
    "VI": {"cx": 1160, "cy": 100, "size": 22,  "country": "US"},
}

# Skip Nunavut for the same reason TER was clipped in the section map —
# the Arctic archipelago dominates the canvas with no QSO benefit.
SKIP_PROVINCES = {"NU"}


def clip_north(gdf: gpd.GeoDataFrame) -> gpd.GeoDataFrame:
    clip_box = box(-180.0, -90.0, 180.0, NORTHERN_CLIP_LAT)
    gdf = gdf.copy()
    gdf["geometry"] = gdf.geometry.intersection(clip_box)
    return gdf


def place_in_box(geom_lcc, cx: float, cy: float, size: float):
    minx, miny, maxx, maxy = geom_lcc.bounds
    src_w = maxx - minx
    src_h = maxy - miny
    if src_w <= 0 or src_h <= 0:
        return geom_lcc
    scale = size / max(src_w, src_h)
    cw = src_w * scale
    ch = src_h * scale
    ox = cx - cw / 2.0
    bottom_y = cy + ch / 2.0
    def tx(x, y):
        sx = (x - minx) * scale + ox
        sy = bottom_y - (y - miny) * scale
        return sx, sy
    return shp_transform(lambda x, y, z=None: tx(x, y), geom_lcc)


def fit_to_viewbox(gdf: gpd.GeoDataFrame):
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
    gdf = gdf.copy()
    gdf["geometry"] = gdf.geometry.apply(
        lambda g: shp_transform(lambda x, y, z=None: tx(x, y), g)
    )
    return gdf


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


def main():
    print("Loading TIGER counties (US)…")
    counties = gpd.read_file(COUNTY_SHP)
    print(f"  {len(counties)} counties")

    print("Dissolving US counties → states (51 entities)…")
    us = {}
    for fips, postal in FIPS_TO_POSTAL.items():
        df = counties[counties["STATEFP"] == fips]
        if df.empty:
            continue
        geom = unary_union(df.geometry.values)
        us[postal] = {"geometry": geom, "country": "US"}
    print(f"  {len(us)} US entities built")

    print("Loading Natural Earth admin-1 (Canada)…")
    ne = gpd.read_file(NE_ADMIN1)
    provinces = ne[ne["admin"] == "Canada"].to_crs("EPSG:4269")
    provinces = clip_north(provinces)
    ca = {}
    for _, row in provinces.iterrows():
        postal = PROVINCE_TO_POSTAL.get(row["name"])
        if not postal or postal in SKIP_PROVINCES:
            continue
        ca[postal] = {"geometry": row.geometry, "country": "CA"}
    print(f"  {len(ca)} CA entities built")

    all_entities = {**us, **ca}
    print(f"Total: {len(all_entities)} state/province polygons")

    # Project everything to NA LCC.
    print("Projecting + simplifying…")
    gdf = gpd.GeoDataFrame(
        {"id": list(all_entities.keys()),
         "country": [v["country"] for v in all_entities.values()]},
        geometry=[v["geometry"] for v in all_entities.values()],
        crs="EPSG:4269",
    ).to_crs(NA_LCC)
    gdf["geometry"] = gdf.geometry.simplify(SIMPLIFY_TOL_M, preserve_topology=True)

    # Split off special-position insets from the main fit.
    special_lcc = {row["id"]: row.geometry for _, row in gdf.iterrows()
                   if row["id"] in SPECIAL_INSETS}
    gdf_main = gdf[~gdf["id"].isin(SPECIAL_INSETS)].copy()

    print(f"Placing special insets ({len(special_lcc)})…")
    placed_specials = {}
    for sec_id, geom in special_lcc.items():
        cfg = SPECIAL_INSETS[sec_id]
        placed_specials[sec_id] = place_in_box(geom, cfg["cx"], cfg["cy"], cfg["size"])

    print("Fitting main map to viewBox…")
    gdf_fit = fit_to_viewbox(gdf_main)

    print("Emitting SVG paths…")
    out = {
        "viewBox":      [0, 0, VIEW_W, VIEW_H],
        "insets":       [],
        "aliasTargets": {},
        "sections":     {},
    }
    for _, row in gdf_fit.iterrows():
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
            "country": row["country"],
        }
    for sec_id, geom in placed_specials.items():
        path = polygon_to_svg(geom)
        if not path:
            continue
        cx, cy = geom.representative_point().coords[0]
        bx0, by0, bx1, by1 = geom.bounds
        out["sections"][sec_id] = {
            "svgPath": path,
            "labelX":  round(cx, 1),
            "labelY":  round(cy, 1),
            "bbox":    [round(bx0, 1), round(by0, 1), round(bx1, 1), round(by1, 1)],
            "country": SPECIAL_INSETS[sec_id]["country"],
        }

    OUT_JSON.parent.mkdir(parents=True, exist_ok=True)
    OUT_JSON.write_text(json.dumps(out, separators=(",", ":")))
    size_kb = OUT_JSON.stat().st_size / 1024
    print(f"Wrote {OUT_JSON} ({size_kb:.1f} KB, {len(out['sections'])} entities)")


if __name__ == "__main__":
    main()
