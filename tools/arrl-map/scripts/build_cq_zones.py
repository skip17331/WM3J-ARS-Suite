#!/usr/bin/env python3
"""
Build cq-zones.json from github.com/logocomune/go-cq-zone data.go (MIT,
attributed to Mark Beech 2015-2018). The Go source embeds 40 CQ zone
polygons; we parse them with regex into a list of (zone_number, name,
center_lat, center_lng, [(lat,lng), …]).

Output schema mirrors dxcc-entities.json — same Robinson projection so the
DXCC map and CQ Zone map have consistent visual style.

Usage:
  cd tools/arrl-map
  .venv/bin/python scripts/build_cq_zones.py
"""

from __future__ import annotations
import json
import re
from pathlib import Path

import geopandas as gpd
from shapely.geometry import Polygon, MultiPolygon
from shapely.ops import transform as shp_transform

HERE = Path(__file__).resolve().parent.parent
DATA = HERE / "data"
SRC = DATA / "cqzone_data.go"
OUT = HERE.parent.parent / "j-log" / "src" / "main" / "resources" / "com" / "jlog" / "maps" / "cq-zones.json"

ROBIN = "+proj=robin +lon_0=0 +datum=WGS84 +units=m +no_defs"
VIEW_W = 1200
VIEW_H = 640
MARGIN = 10
SIMPLIFY_TOL_M = 25_000   # 25 km — zones are continent-scale, generous simplification fine


# Match one zone struct on a single line:
#   {name: `Zone Name`, number: N, center: Coordinate{Lat: a, Lng: b}, polygon: []Coordinate{{Lat: x, Lng: y}, …}},
ZONE_RE = re.compile(
    r"\{name:\s*`(?P<name>[^`]+)`,\s*number:\s*(?P<num>\d+),"
    r"\s*center:\s*Coordinate\{Lat:\s*(?P<clat>-?\d+(?:\.\d+)?),\s*Lng:\s*(?P<clng>-?\d+(?:\.\d+)?)\},"
    r"\s*polygon:\s*\[\]Coordinate\{(?P<poly>.*?)\}\}",
    re.DOTALL,
)

COORD_RE = re.compile(r"\{Lat:\s*(-?\d+(?:\.\d+)?),\s*Lng:\s*(-?\d+(?:\.\d+)?)\}")


def parse_data_go(path: Path) -> list[dict]:
    text = path.read_text()
    zones = []
    for m in ZONE_RE.finditer(text):
        coords = []
        for cm in COORD_RE.finditer(m.group("poly")):
            lat = float(cm.group(1))
            lng = float(cm.group(2))
            coords.append((lng, lat))   # shapely uses (x=lng, y=lat)
        if len(coords) < 3:
            continue
        # Close the ring if needed.
        if coords[0] != coords[-1]:
            coords.append(coords[0])
        zones.append({
            "number": int(m.group("num")),
            "name":   m.group("name"),
            "center": (float(m.group("clng")), float(m.group("clat"))),
            "polygon": coords,
        })
    return zones


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
    print("Parsing cqzone_data.go…")
    zones = parse_data_go(SRC)
    print(f"  {len(zones)} zones parsed")

    if len(zones) < 40:
        print("WARNING: expected 40 zones; got", len(zones))

    # Build a GeoDataFrame so we can project everything together.
    print("Projecting + simplifying…")
    polys = [Polygon(z["polygon"]) for z in zones]
    gdf = gpd.GeoDataFrame(
        {"id": [str(z["number"]).zfill(2) for z in zones],
         "name": [z["name"] for z in zones]},
        geometry=polys, crs="EPSG:4326",
    )
    gdf = gdf.to_crs(ROBIN)
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

    print("Emitting SVG paths…")
    out = {
        "viewBox": [0, 0, VIEW_W, VIEW_H],
        "projection": "robin",
        "aliasTargets": {},
        "attribution": "CQ zone polygons from github.com/logocomune/go-cq-zone (MIT, © Mark Beech 2015-2018)",
        "entities": {},
    }
    for _, row in gdf.iterrows():
        path = polygon_to_svg(row.geometry)
        if not path:
            continue
        cx, cy = row.geometry.representative_point().coords[0]
        bx0, by0, bx1, by1 = row.geometry.bounds
        out["entities"][row["id"]] = {
            "svgPath": path,
            "labelX":  round(cx, 1),
            "labelY":  round(cy, 1),
            "bbox":    [round(bx0, 1), round(by0, 1), round(bx1, 1), round(by1, 1)],
            "name":    row["name"],
        }
        # Numeric alias: "1" → "01" so worked-set updates accept either form.
        n = int(row["id"])
        if str(n) != row["id"]:
            out["aliasTargets"][str(n)] = [row["id"]]

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(out, separators=(",", ":")))
    size_kb = OUT.stat().st_size / 1024
    print(f"Wrote {OUT} ({size_kb:.1f} KB, {len(out['entities'])} zones, "
          f"{len(out['aliasTargets'])} aliases)")


if __name__ == "__main__":
    main()
