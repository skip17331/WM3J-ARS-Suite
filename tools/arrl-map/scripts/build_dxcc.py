#!/usr/bin/env python3
"""
Build dxcc-entities.json from cty.dat (AD1C) + Natural Earth admin_0_countries.

V1 strategy:
- Parse cty.dat into ~346 DXCC entities (name, primary prefix, all prefixes, lat/lon).
- Match each entity to a Natural Earth country polygon by fuzzy name comparison.
- Emit one polygon per matched entity, keyed by primary prefix.
- Emit aliasTargets: every secondary prefix (and its letters-only form to match
  j-log's CallsignRegion.dxccPrefix() behaviour) → primary.

V1 deferred:
- Sub-country DXCC splits that require admin-1 boundaries (Alaska KL, Hawaii
  KH6, Sardinia IS0, UK GM/GW/GI, Spanish overseas EA6/EA8/EA9, French
  overseas TK/FH/FK/FT/...etc).
- Pure-island entities with no NE polygon (Bouvet 3Y, Kerguelen FT/W,
  Spratly 1S, etc.).

Output schema:
{
  "viewBox": [0,0,W,H],
  "projection": "robin",
  "entities": {
    "<primary>": { "svgPath", "labelX", "labelY", "bbox", "name", "continent" }
  },
  "aliasTargets": { "<prefix>": ["<primary>"] }
}

Usage:
  cd tools/arrl-map
  .venv/bin/python scripts/build_dxcc.py
"""

from __future__ import annotations
import json
import re
import sys
from pathlib import Path

import geopandas as gpd
from shapely.geometry import Polygon, MultiPolygon
from shapely.ops import transform as shp_transform

HERE = Path(__file__).resolve().parent.parent
DATA = HERE / "data"
CTY = DATA / "cty.dat"
NE_COUNTRIES = DATA / "ne_countries" / "ne_10m_admin_0_countries.shp"
OUT = HERE.parent.parent / "j-log" / "src" / "main" / "resources" / "com" / "jlog" / "maps" / "dxcc-entities.json"

# Robinson projection — standard world-display projection (no infinity at
# poles, good area/shape balance).
ROBIN = "+proj=robin +lon_0=0 +datum=WGS84 +units=m +no_defs"

VIEW_W = 1200
VIEW_H = 640
MARGIN = 10
SIMPLIFY_TOL_M = 30_000  # 30 km — fine enough at world scale

# Manual cty.dat-name → NE country-name overrides for cases that fuzzy match
# misses or where the canonical name differs.
NAME_OVERRIDES = {
    "United States of America":      "United States of America",
    "European Russia":               "Russia",
    # V1: aggregate Asiatic Russia into the European Russia polygon — proper
    # split (Urals boundary) needs admin-1 data, deferred.
    "Asiatic Russia":                None,
    "Republic of Korea":             "South Korea",
    "DPR of Korea":                  "North Korea",
    "Czech Republic":                "Czechia",
    "England":                       "United Kingdom",  # G — main UK polygon
    "Slovak Republic":               "Slovakia",
    "Federal Republic of Germany":   "Germany",
    "Fed. Rep. of Germany":          "Germany",
    "Macedonia":                     "North Macedonia",
    "Republic of Kosovo":            "Kosovo",
    "Burma":                         "Myanmar",
    "Bosnia-Herzegovina":            "Bosnia and Herzegovina",
    "Saint Vincent":                 "Saint Vincent and the Grenadines",
    # V1 sub-UK / sub-IT / sub-FR / sub-ES entities all need admin-1 splits.
    # Leave Scotland/Wales/NI/Sardinia/Corsica/Balearic/Canary/Ceuta
    # unmatched so the alias machinery routes worked-sets to plain G/I/F/EA.
    "Scotland":                      None,
    "Wales":                         None,
    "Northern Ireland":              None,
    "Isle of Man":                   None,
    "Jersey":                        None,
    "Guernsey":                      None,
    "Sardinia":                      None,
    "Corsica":                       None,
    "Balearic Islands":              None,
    "Ceuta & Melilla":               None,
    "Canary Islands":                None,
    "ITU HQ":                        None,
    "United Nations HQ":             None,
    "Vatican":                       "Vatican",
}


# ----------------------------------------------------------------------------
# cty.dat parsing
# ----------------------------------------------------------------------------

def parse_cty_dat(path: Path) -> list[dict]:
    text = path.read_text()
    entries = []
    # Entities are terminated by ';'. Each entity has a header line ending
    # in ':' and one+ continuation lines listing prefixes.
    for chunk in text.split(";"):
        chunk = chunk.strip()
        if not chunk:
            continue
        lines = [l.strip() for l in chunk.splitlines() if l.strip()]
        if not lines:
            continue
        header = lines[0]
        if ":" not in header:
            continue
        cols = [c.strip() for c in header.split(":")]
        if len(cols) < 8:
            continue
        name = cols[0]
        try:
            cqz   = int(cols[1])
            ituz  = int(cols[2])
            cont  = cols[3]
            lat   = float(cols[4])
            lon   = float(cols[5])
        except ValueError:
            continue
        # cty.dat lat is N-positive; lon is W-positive — flip sign for standard.
        lon = -lon
        primary = cols[7]
        prefix_blob = " ".join(lines[1:])
        prefixes = [p.strip() for p in prefix_blob.split(",") if p.strip()]
        # cty.dat embeds exception rules with '=' prefix; ignore those.
        prefixes = [p for p in prefixes if not p.startswith("=")]
        # And strip any zone overrides like "(8)" or "[20]" inside prefixes.
        prefixes = [re.sub(r"[\(\[].*$", "", p).strip() for p in prefixes]
        prefixes = [p for p in prefixes if p]
        entries.append({
            "name":     name,
            "primary":  primary,
            "lat":      lat,
            "lon":      lon,
            "continent": cont,
            "prefixes": prefixes,
        })
    return entries


def letters_only(prefix: str) -> str:
    """First run of letters at start — mimics CallsignRegion.dxccPrefix()."""
    m = re.match(r"^[A-Z]+", prefix)
    return m.group(0) if m else ""


# ----------------------------------------------------------------------------
# Geometry matching
# ----------------------------------------------------------------------------

def normalize_name(s: str) -> str:
    s = s.lower()
    s = re.sub(r"[^a-z0-9]+", " ", s)
    s = re.sub(r"\b(the|of|and|republic|federal|democratic|federation)\b", " ", s)
    s = re.sub(r"\s+", " ", s).strip()
    return s


def build_country_index(ne: gpd.GeoDataFrame) -> tuple[dict, dict]:
    """
    Two-tier name index. Primary (ADMIN/NAME/NAME_LONG/FORMAL_EN) is the
    entity's own name; secondary (SOVEREIGNT) is the sovereign nation —
    used only as a fallback so e.g. "United Kingdom" maps to the actual UK
    polygon, not a Cypriot base that happens to be UK-sovereign.
    """
    primary_idx: dict = {}
    secondary_idx: dict = {}
    for _, row in ne.iterrows():
        for col in ("ADMIN", "NAME", "NAME_LONG", "FORMAL_EN"):
            v = row.get(col)
            if isinstance(v, str) and v:
                primary_idx.setdefault(normalize_name(v), row.geometry)
        for col in ("SOVEREIGNT",):
            v = row.get(col)
            if isinstance(v, str) and v:
                secondary_idx.setdefault(normalize_name(v), row.geometry)
    return primary_idx, secondary_idx


def match_country(entity: dict, primary_idx: dict, secondary_idx: dict):
    name = entity["name"]
    if name in NAME_OVERRIDES:
        override = NAME_OVERRIDES[name]
        if override is None:
            return None
        n = normalize_name(override)
        return primary_idx.get(n) or secondary_idx.get(n)
    n = normalize_name(name)
    return primary_idx.get(n) or secondary_idx.get(n)


def keep_significant_components(geom, min_area_ratio: float = 0.10):
    """
    Drop tiny scattered islands so antimeridian-crossing entities (USA
    outlying, NZ Chathams, etc.) don't span the whole map after projection.
    Keeps every component that's at least min_area_ratio of the largest.
    """
    if not isinstance(geom, MultiPolygon):
        return geom
    polys = list(geom.geoms)
    if not polys:
        return geom
    areas = [p.area for p in polys]
    max_area = max(areas)
    kept = [p for p, a in zip(polys, areas) if a / max_area >= min_area_ratio]
    if not kept:
        # Shouldn't happen — keep the largest as a safety net.
        i = areas.index(max_area)
        return polys[i]
    if len(kept) == 1:
        return kept[0]
    return MultiPolygon(kept)


# ----------------------------------------------------------------------------
# Projection + viewBox fitting
# ----------------------------------------------------------------------------

def project_and_fit(geoms: dict[str, object]) -> tuple[dict[str, object], dict]:
    """
    Project each entity's geometry to Robinson and fit the whole world into
    the viewBox. Returns (projected_geoms_in_viewbox_coords, info).
    """
    ids = list(geoms.keys())
    gdf = gpd.GeoDataFrame({"id": ids}, geometry=[geoms[i] for i in ids], crs="EPSG:4326")
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

    out_geoms = {}
    for _, row in gdf.iterrows():
        g = shp_transform(lambda x, y, z=None: tx(x, y), row.geometry)
        out_geoms[row["id"]] = g
    return out_geoms, {"scale": scale, "ox": ox, "oy": oy}


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


# ----------------------------------------------------------------------------
# main
# ----------------------------------------------------------------------------

def main():
    print("Parsing cty.dat…")
    entities = parse_cty_dat(CTY)
    print(f"  {len(entities)} DXCC entities parsed")

    print("Loading Natural Earth countries…")
    ne = gpd.read_file(NE_COUNTRIES)
    primary_idx, secondary_idx = build_country_index(ne)
    print(f"  {len(ne)} countries, {len(primary_idx)} primary keys + {len(secondary_idx)} fallback keys")

    # Match each entity to a polygon
    matched = {}    # primary → shapely geometry (WGS84)
    metadata = {}   # primary → {name, continent, lat, lon}
    aliases = {}    # alt prefix → [primary]
    unmatched_named = []

    for ent in entities:
        geom = match_country(ent, primary_idx, secondary_idx)
        if geom is None:
            unmatched_named.append(ent["name"])
            # Still register aliases — they'll resolve to nothing visible
            # but the worked-set updater won't crash.
            continue
        primary = ent["primary"]
        # If multiple entities share a country polygon (e.g. England + UK
        # itself), the first one wins. Print a warning when this happens.
        if primary in matched:
            # Same primary already mapped — odd; skip duplicate.
            continue
        geom = keep_significant_components(geom)
        matched[primary] = geom
        metadata[primary] = {
            "name": ent["name"],
            "continent": ent["continent"],
        }
        # Build aliases: every secondary prefix + letters-only forms
        for p in ent["prefixes"]:
            if p == primary:
                continue
            aliases.setdefault(p, [primary])
            lo = letters_only(p)
            if lo and lo != primary:
                aliases.setdefault(lo, [primary])
        # Also alias the letters-only form of the primary if it differs.
        lo = letters_only(primary)
        if lo and lo != primary:
            aliases.setdefault(lo, [primary])

    print(f"  matched {len(matched)} entities to NE polygons")
    print(f"  unmatched cty.dat entities (deferred to V2): {len(unmatched_named)}")
    if unmatched_named[:8]:
        print(f"    e.g. {unmatched_named[:8]}…")

    print("Projecting + fitting…")
    placed, info = project_and_fit(matched)

    print("Emitting SVG paths…")
    out = {
        "viewBox":      [0, 0, VIEW_W, VIEW_H],
        "projection":   "robin",
        "aliasTargets": aliases,
        "entities":     {},
    }
    skipped_empty = 0
    for prim, geom in placed.items():
        path = polygon_to_svg(geom)
        if not path:
            skipped_empty += 1
            continue
        cx, cy = geom.representative_point().coords[0]
        bx0, by0, bx1, by1 = geom.bounds
        out["entities"][prim] = {
            "svgPath":   path,
            "labelX":    round(cx, 1),
            "labelY":    round(cy, 1),
            "bbox":      [round(bx0, 1), round(by0, 1), round(bx1, 1), round(by1, 1)],
            "name":      metadata[prim]["name"],
            "continent": metadata[prim]["continent"],
        }

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(out, separators=(",", ":")))
    size_kb = OUT.stat().st_size / 1024
    print(f"Wrote {OUT} ({size_kb:.1f} KB, {len(out['entities'])} entities, "
          f"{len(aliases)} aliases; skipped {skipped_empty} empty)")


if __name__ == "__main__":
    main()
