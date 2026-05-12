#!/usr/bin/env python3
"""
Build maidenhead-grids.json — 4-character Maidenhead grid square map for
ARRL VHF contests (Jan/June/Sep).

Covers a bounding box around continental North America:
  longitude  -130°W to  -55°W   (BC to Newfoundland; 75° wide → 38 grids of 2°)
  latitude    22°N  to  55°N    (Mexico border to mid-Canada; 33° tall → 33 grids of 1°)

Total: 1254 grid squares. Each is keyed by its 4-char locator ("EM48",
"FN20", …) so clicks/worked-set updates map straight from the contest's
`grid_rcvd` field.

A thin states+provinces outline is included as a non-clickable background
so the abstract grid has geographic context.

Schema mirrors arrl-sections.json (`sections` dict, viewBox, aliasTargets).

Usage:
  cd tools/arrl-map
  .venv/bin/python scripts/build_grids.py
"""

from __future__ import annotations
import json
from pathlib import Path

import geopandas as gpd
from shapely.geometry import Polygon, MultiPolygon, box
from shapely.ops import transform as shp_transform, unary_union

HERE = Path(__file__).resolve().parent.parent
DATA = HERE / "data"
COUNTY_SHP = DATA / "us_county" / "tl_2024_us_county.shp"
NE_ADMIN1  = DATA / "ne_admin1" / "ne_10m_admin_1_states_provinces.shp"
OUT = HERE.parent.parent / "j-log" / "src" / "main" / "resources" / "com" / "jlog" / "maps" / "maidenhead-grids.json"

# Same NA LCC projection as the other NA maps for visual consistency.
NA_LCC = (
    "+proj=lcc +lat_1=20 +lat_2=60 +lat_0=40 +lon_0=-96 "
    "+ellps=GRS80 +datum=NAD83 +units=m +no_defs"
)

VIEW_W = 1200
VIEW_H = 700
MARGIN = 20

# Coverage bbox — generous around continental NA VHF activity.
LON_MIN, LON_MAX = -130.0, -55.0
LAT_MIN, LAT_MAX =   22.0,  55.0


def grid_id(lon: float, lat: float) -> str:
    """Return 4-char Maidenhead locator for the bottom-left corner of a cell."""
    L = lon + 180.0
    A = lat + 90.0
    f_lon = int(L // 20)
    f_lat = int(A // 10)
    s_lon = int((L % 20) // 2)
    s_lat = int(A % 10)
    return f"{chr(ord('A') + f_lon)}{chr(ord('A') + f_lat)}{s_lon}{s_lat}"


def build_grid_polygons():
    """Yield (grid_id, Polygon in lat/lon) for each 2°×1° cell in the bbox."""
    cells = []
    lon = LON_MIN
    while lon < LON_MAX:
        lat = LAT_MIN
        while lat < LAT_MAX:
            gid = grid_id(lon, lat)
            # Rectangle corners in lon/lat
            poly = Polygon([
                (lon,       lat),
                (lon + 2.0, lat),
                (lon + 2.0, lat + 1.0),
                (lon,       lat + 1.0),
            ])
            cells.append((gid, poly))
            lat += 1.0
        lon += 2.0
    return cells


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
    print("Generating grid squares…")
    cells = build_grid_polygons()
    print(f"  {len(cells)} cells")

    print("Loading state/province outlines for background…")
    counties = gpd.read_file(COUNTY_SHP)
    us_outline = unary_union(counties.geometry.values)
    ne = gpd.read_file(NE_ADMIN1)
    ca = ne[(ne["admin"] == "Canada") & (ne["name"] != "Nunavut")].to_crs("EPSG:4269")
    clip_box = box(-180, -90, 180, 70)
    ca["geometry"] = ca.geometry.intersection(clip_box)
    ca_outline = unary_union(ca.geometry.values)
    background = unary_union([us_outline, ca_outline])

    # Project everything together so transforms align.
    print("Projecting…")
    gdf = gpd.GeoDataFrame(
        {"id": [c[0] for c in cells] + ["__bg__"]},
        geometry=[c[1] for c in cells] + [background],
        crs="EPSG:4326",
    ).to_crs(NA_LCC)
    # Simplify the background outline aggressively; grids are already simple.
    bg_row = gdf[gdf["id"] == "__bg__"].iloc[0]
    bg_simpler = bg_row.geometry.simplify(20_000, preserve_topology=True)

    # Fit to viewBox using the union of all geometries' bounds.
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
    def apply(g):
        return shp_transform(lambda x, y, z=None: tx(x, y), g)

    print("Emitting SVG…")
    out = {
        "viewBox":      [0, 0, VIEW_W, VIEW_H],
        "background":   polygon_to_svg(apply(bg_simpler)),
        "aliasTargets": {},
        "sections":     {},
    }
    for _, row in gdf.iterrows():
        if row["id"] == "__bg__":
            continue
        g = apply(row.geometry)
        path = polygon_to_svg(g)
        if not path:
            continue
        cx, cy = g.representative_point().coords[0]
        bx0, by0, bx1, by1 = g.bounds
        out["sections"][row["id"]] = {
            "svgPath": path,
            "labelX":  round(cx, 1),
            "labelY":  round(cy, 1),
            "bbox":    [round(bx0, 1), round(by0, 1), round(bx1, 1), round(by1, 1)],
        }

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(out, separators=(",", ":")))
    size_kb = OUT.stat().st_size / 1024
    print(f"Wrote {OUT} ({size_kb:.1f} KB, {len(out['sections'])} grids + background)")


if __name__ == "__main__":
    main()
