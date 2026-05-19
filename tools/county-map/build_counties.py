#!/usr/bin/env python3
"""
Pure-stdlib county-map generator for state QSO-party / 7QP plugins.

Reimplements the (retired) tools/arrl-map/scripts/build_counties.py county
step WITHOUT geopandas/shapely/pyproj/GDAL — this host has no pip. Output is
byte-compatible with what CountyMap consumes:

  j-log/src/main/resources/com/jlog/maps/county-<state>.json
  { "viewBox":[0,0,900,700], "state":"WA", "aliasTargets":{},
    "sections": { "ADA": {"svgPath","labelX","labelY","bbox","name"}, ... } }

Geometry source: the public FIPS-keyed US county GeoJSON (Census cartographic
boundaries, mirrored by plotly/datasets) instead of the TIGER shapefile.
Code→county pairing reproduces the original positional-alphabetical method:
the ordered code list in j-log-engine/.../com/jlog/counties/<st>.json is
zipped against the state's county names sorted with spaces/dots stripped and
case-folded — identical ordering to the shipped maps (e.g. county-wa.json).

Usage:
  python3 tools/county-map/build_counties.py WA
  python3 tools/county-map/build_counties.py all      # every state w/ a code list
"""
from __future__ import annotations

import json
import math
import sys
import urllib.request
from pathlib import Path

HERE = Path(__file__).resolve().parent
DATA = HERE / "data"
GEOJSON = DATA / "us-counties-fips.json"
GEOJSON_URL = ("https://raw.githubusercontent.com/plotly/datasets/master/"
               "geojson-counties-fips.json")

ROOT = HERE.parent.parent
CODES_DIR = ROOT / "j-log-engine/src/main/resources/com/jlog/counties"
OUT_DIR = ROOT / "j-log/src/main/resources/com/jlog/maps"

# 2-digit state FIPS. 7QP 7th-area states first, then the other state QPs.
STATE_FIPS = {
    "AZ": "04", "ID": "16", "MT": "30", "NV": "32",
    "OR": "41", "UT": "49", "WA": "53", "WY": "56",
    "CA": "06", "TX": "48", "NC": "37", "FL": "12",
    "GA": "13", "MI": "26", "NY": "36", "OH": "39",
    "PA": "42", "SC": "45", "TN": "47", "ME": "23", "NH": "33", "VT": "50",
}

# Affects ONLY the positional-match sort order (geometry pulled by name).
SORT_NAME_OVERRIDES = {"FL": {"Miami-Dade": "Dade"}}

VIEW_W, VIEW_H, MARGIN = 900, 700, 30
SIMPLIFY_TOL_M = 2_000           # 2 km, matches the original pipeline
R = 6371008.8                    # authalic Earth radius (m)
# Albers Equal Area, CONUS standard parallels (same params as the old proj).
LAT1, LAT2, LAT0, LON0 = (math.radians(d) for d in (29.5, 45.5, 37.5, -96.0))
_AEA_N = (math.sin(LAT1) + math.sin(LAT2)) / 2.0
_AEA_C = math.cos(LAT1) ** 2 + 2 * _AEA_N * math.sin(LAT1)
_AEA_RHO0 = R / _AEA_N * math.sqrt(_AEA_C - 2 * _AEA_N * math.sin(LAT0))

# NOTE: do NOT strip " City"/" city" — independent cities are their own
# county-equivalents (e.g. Carson City NV) and 7QP keeps the full name.
_SUFFIXES = (" County", " Parish", " Borough", " Census Area",
             " Municipality")


def albers(lon_deg: float, lat_deg: float) -> tuple[float, float]:
    lon, lat = math.radians(lon_deg), math.radians(lat_deg)
    theta = _AEA_N * (lon - LON0)
    rho = R / _AEA_N * math.sqrt(max(0.0, _AEA_C - 2 * _AEA_N * math.sin(lat)))
    return rho * math.sin(theta), _AEA_RHO0 - rho * math.cos(theta)


def _perp(p, a, b) -> float:
    (x, y), (x1, y1), (x2, y2) = p, a, b
    dx, dy = x2 - x1, y2 - y1
    if dx == 0 and dy == 0:
        return math.hypot(x - x1, y - y1)
    return abs(dy * x - dx * y + x2 * y1 - y2 * x1) / math.hypot(dx, dy)


def douglas_peucker(pts, tol):
    if len(pts) < 3:
        return pts
    dmax, idx = 0.0, 0
    for i in range(1, len(pts) - 1):
        d = _perp(pts[i], pts[0], pts[-1])
        if d > dmax:
            dmax, idx = d, i
    if dmax > tol:
        left = douglas_peucker(pts[:idx + 1], tol)
        right = douglas_peucker(pts[idx:], tol)
        return left[:-1] + right
    return [pts[0], pts[-1]]


def ring_area_centroid(ring):
    a = cx = cy = 0.0
    for i in range(len(ring) - 1):
        x0, y0 = ring[i]
        x1, y1 = ring[i + 1]
        cross = x0 * y1 - x1 * y0
        a += cross
        cx += (x0 + x1) * cross
        cy += (y0 + y1) * cross
    if a == 0:
        xs = [p[0] for p in ring]
        ys = [p[1] for p in ring]
        return abs(0.0), sum(xs) / len(xs), sum(ys) / len(ys)
    a *= 0.5
    return abs(a), cx / (6 * a), cy / (6 * a)


def polygons_of(geom):
    """Yield lists-of-rings for each polygon (geographic lon/lat)."""
    t = geom["type"]
    if t == "Polygon":
        yield geom["coordinates"]
    elif t == "MultiPolygon":
        yield from geom["coordinates"]


def load_geojson():
    if not GEOJSON.exists():
        DATA.mkdir(parents=True, exist_ok=True)
        print(f"  downloading county GeoJSON -> {GEOJSON}")
        urllib.request.urlretrieve(GEOJSON_URL, GEOJSON)
    feats = json.loads(GEOJSON.read_text())["features"]
    by_state: dict[str, list] = {}
    for f in feats:
        p = f["properties"]
        nm = p["NAME"]
        for s in _SUFFIXES:
            if nm.endswith(s):
                nm = nm[: -len(s)]
                break
        by_state.setdefault(p["STATE"], []).append((nm, f["geometry"]))
    return by_state


def sort_key(name: str, overrides: dict) -> str:
    n = overrides.get(name, name).replace("&", "and")
    return n.replace(" ", "").replace(".", "").replace("'", "").lower()


def build_state(state: str, by_state: dict) -> None:
    fips = STATE_FIPS.get(state)
    if fips is None:
        raise SystemExit(f"{state}: no FIPS in STATE_FIPS")
    codes_path = CODES_DIR / f"{state.lower()}.json"
    if not codes_path.exists():
        raise SystemExit(f"{state}: missing code list {codes_path}")
    codes = json.loads(codes_path.read_text())
    counties = by_state.get(fips, [])
    overrides = SORT_NAME_OVERRIDES.get(state, {})
    counties.sort(key=lambda nc: sort_key(nc[0], overrides))
    if len(codes) != len(counties):
        raise SystemExit(
            f"{state}: {len(codes)} codes vs {len(counties)} counties — "
            "positional match impossible; check the code list.")

    # Project everything, collect per-county projected rings, track bounds.
    projected = []  # (code, name, [rings])
    minx = miny = float("inf")
    maxx = maxy = float("-inf")
    for (name, geom), code in zip(counties, codes):
        rings = []
        for poly in polygons_of(geom):
            for ring in poly:
                pr = [albers(lon, lat) for lon, lat in ring]
                pr = douglas_peucker(pr, SIMPLIFY_TOL_M)
                if len(pr) < 4:
                    continue
                if pr[0] != pr[-1]:
                    pr.append(pr[0])
                rings.append(pr)
                for x, y in pr:
                    minx, miny = min(minx, x), min(miny, y)
                    maxx, maxy = max(maxx, x), max(maxy, y)
        projected.append((code, name, rings))

    src_w, src_h = maxx - minx, maxy - miny
    scale = min((VIEW_W - 2 * MARGIN) / src_w, (VIEW_H - 2 * MARGIN) / src_h)
    cw, ch = src_w * scale, src_h * scale
    ox, oy = (VIEW_W - cw) / 2.0, (VIEW_H - ch) / 2.0

    def tx(x, y):
        return ((x - minx) * scale + ox,
                VIEW_H - ((y - miny) * scale + oy))

    sections = {}
    for code, name, rings in projected:
        fitted = [[tx(x, y) for x, y in r] for r in rings]
        if not fitted:
            continue
        path_parts, best_area, lx, ly = [], -1.0, 0.0, 0.0
        bx0 = by0 = float("inf")
        bx1 = by1 = float("-inf")
        for r in fitted:
            d = " ".join(f"{'M' if i == 0 else 'L'}{x:.1f},{y:.1f}"
                         for i, (x, y) in enumerate(r))
            path_parts.append(d + " Z")
            area, cx, cy = ring_area_centroid(r)
            if area > best_area:
                best_area, lx, ly = area, cx, cy
            for x, y in r:
                bx0, by0 = min(bx0, x), min(by0, y)
                bx1, by1 = max(bx1, x), max(by1, y)
        sections[code] = {
            "svgPath": " ".join(path_parts),
            "labelX": round(lx, 1),
            "labelY": round(ly, 1),
            "bbox": [round(bx0, 1), round(by0, 1), round(bx1, 1), round(by1, 1)],
            "name": name,
        }

    out = {"viewBox": [0, 0, VIEW_W, VIEW_H], "state": state,
           "aliasTargets": {}, "sections": sections}
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    out_path = OUT_DIR / f"county-{state.lower()}.json"
    out_path.write_text(json.dumps(out, separators=(",", ":")))
    print(f"Wrote {out_path} ({out_path.stat().st_size/1024:.1f} KB, "
          f"{len(sections)} counties)")


def build_7qp(by_state: dict) -> None:
    """Combined 8-state 7th-Call-Area map: ONE shared Albers projection so
    the states sit in correct relative geography. Section id = the 5-char
    7QP code (e.g. WACLL); reproducible from the committed 7qp.json."""
    src = json.loads((CODES_DIR / "7qp.json").read_text())
    geo_by_key = {}  # (fips, normname) -> geometry
    for st, fips in STATE_FIPS.items():
        for name, geom in by_state.get(fips, []):
            geo_by_key[(fips, sort_key(name, {}))] = geom

    projected, miss = [], []
    minx = miny = float("inf")
    maxx = maxy = float("-inf")
    for e in src:
        code, state, county = e["c"], e["s"], e["n"]
        fips = STATE_FIPS[state]
        geom = geo_by_key.get((fips, sort_key(county, {})))
        if geom is None:
            miss.append(code)
            continue
        rings = []
        for poly in polygons_of(geom):
            for ring in poly:
                pr = [albers(lon, lat) for lon, lat in ring]
                pr = douglas_peucker(pr, SIMPLIFY_TOL_M)
                if len(pr) < 4:
                    continue
                if pr[0] != pr[-1]:
                    pr.append(pr[0])
                rings.append(pr)
                for x, y in pr:
                    minx, miny = min(minx, x), min(miny, y)
                    maxx, maxy = max(maxx, x), max(maxy, y)
        projected.append((code, state, county, rings))
    if miss:
        raise SystemExit(f"7QP: {len(miss)} codes had no polygon: {miss[:10]}")

    src_w, src_h = maxx - minx, maxy - miny
    scale = min((VIEW_W - 2 * MARGIN) / src_w, (VIEW_H - 2 * MARGIN) / src_h)
    ox = (VIEW_W - src_w * scale) / 2.0
    oy = (VIEW_H - src_h * scale) / 2.0

    def tx(x, y):
        return ((x - minx) * scale + ox, VIEW_H - ((y - miny) * scale + oy))

    sections = {}
    for code, state, county, rings in projected:
        parts, best, lx, ly = [], -1.0, 0.0, 0.0
        bx0 = by0 = float("inf")
        bx1 = by1 = float("-inf")
        for r in rings:
            fr = [tx(x, y) for x, y in r]
            parts.append(" ".join(
                f"{'M' if i == 0 else 'L'}{x:.1f},{y:.1f}"
                for i, (x, y) in enumerate(fr)) + " Z")
            area, cx, cy = ring_area_centroid(fr)
            if area > best:
                best, lx, ly = area, cx, cy
            for x, y in fr:
                bx0, by0 = min(bx0, x), min(by0, y)
                bx1, by1 = max(bx1, x), max(by1, y)
        sections[code] = {
            "svgPath": " ".join(parts),
            "labelX": round(lx, 1), "labelY": round(ly, 1),
            "bbox": [round(bx0, 1), round(by0, 1), round(bx1, 1), round(by1, 1)],
            "name": county, "state": state,
        }
    out = {"viewBox": [0, 0, VIEW_W, VIEW_H], "state": "7QP",
           "aliasTargets": {}, "sections": sections}
    out_path = OUT_DIR / "county-7qp.json"
    out_path.write_text(json.dumps(out, separators=(",", ":")))
    print(f"Wrote {out_path} ({out_path.stat().st_size/1024:.1f} KB, "
          f"{len(sections)} counties, 8 states, shared projection)")


def main():
    if len(sys.argv) < 2:
        print("Usage: build_counties.py <STATE|all|7QP>", file=sys.stderr)
        sys.exit(2)
    by_state = load_geojson()
    arg = sys.argv[1]
    if arg.upper() == "7QP":
        print("Building 7QP combined map…")
        build_7qp(by_state)
        return
    if arg == "all":
        targets = sorted(s for s in STATE_FIPS
                         if (CODES_DIR / f"{s.lower()}.json").exists())
    else:
        targets = [arg.upper()]
    for st in targets:
        print(f"Building {st}…")
        build_state(st, by_state)


if __name__ == "__main__":
    main()
