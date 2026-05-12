#!/usr/bin/env python3
"""
Build arrl-sections.json from TIGER county shapefile + Natural Earth admin-1.

Output schema:
{
  "viewBox": [0, 0, W, H],
  "sections": {
    "<id>": {
      "svgPath": "M ... Z",
      "labelX":  float,
      "labelY":  float,
      "bbox":    [minX, minY, maxX, maxY],
      "country": "US" | "CA",
      "name":    "Eastern PA"  (optional, human-readable)
    },
    ...
  }
}

Strategy:
- US sections: dissolve TIGER counties using the section→county table.
- Canada: 1 polygon per RAC section. Ontario rendered as a single "ONN" polygon
  for V1 (no CD-level split yet). Maritimes split as NB/NS/PE (current 2023+
  RAC). Territories rendered as combined "TER".
- Project to North America LCC.
- Simplify aggressively (tolerance set per-section by polygon size).
- Drop AK/HI/PR/VI in V1 — they need composed insets and are deferred.

Usage:
  cd tools/arrl-map
  .venv/bin/python scripts/build_sections.py
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

import geopandas as gpd
from shapely.geometry import Polygon, MultiPolygon, box
from shapely.ops import unary_union

HERE = Path(__file__).resolve().parent.parent  # tools/arrl-map/
DATA = HERE / "data"

COUNTY_SHP   = DATA / "us_county" / "tl_2024_us_county.shp"
NE_ADMIN1    = DATA / "ne_admin1" / "ne_10m_admin_1_states_provinces.shp"
US_SECTIONS  = DATA / "section-to-counties.json"
CA_SECTIONS  = DATA / "section-to-csd-canada.json"
OUT_JSON     = HERE.parent.parent / "j-log" / "src" / "main" / "resources" / "com" / "jlog" / "maps" / "arrl-sections.json"

# US state postal → FIPS state code (used to filter TIGER counties).
STATE_FIPS = {
    "AL":"01","AK":"02","AZ":"04","AR":"05","CA":"06","CO":"08","CT":"09","DE":"10",
    "DC":"11","FL":"12","GA":"13","HI":"15","ID":"16","IL":"17","IN":"18","IA":"19",
    "KS":"20","KY":"21","LA":"22","ME":"23","MD":"24","MA":"25","MI":"26","MN":"27",
    "MS":"28","MO":"29","MT":"30","NE":"31","NV":"32","NH":"33","NJ":"34","NM":"35",
    "NY":"36","NC":"37","ND":"38","OH":"39","OK":"40","OR":"41","PA":"42","RI":"44",
    "SC":"45","SD":"46","TN":"47","TX":"48","UT":"49","VT":"50","VA":"51","WA":"53",
    "WV":"54","WI":"55","WY":"56","PR":"72","VI":"78",
}

# Sections deferred to V2 (need inset composition).
DEFER_V1 = {"AK", "PAC", "PR", "VI"}

# RAC Canadian sections: which Natural Earth province name(s) compose each.
CA_SECTION_PROVINCES = {
    "AB":  ["Alberta"],
    "BC":  ["British Columbia"],
    "MB":  ["Manitoba"],
    "NB":  ["New Brunswick"],
    "NL":  ["Newfoundland and Labrador"],
    "NS":  ["Nova Scotia"],
    "PE":  ["Prince Edward Island"],
    "QC":  ["Québec"],
    "SK":  ["Saskatchewan"],
    # TER officially covers Yukon + Northwest Territories + Nunavut, but
    # Nunavut is almost entirely Arctic archipelago and inflates the map
    # bbox dramatically while having essentially no QSO activity. Drop it
    # from the rendered polygon (the section still exists in the DB; clicks
    # on YT/NT still register as TER).
    "TER": ["Yukon", "Northwest Territories"],
    # V1: Ontario as single polygon, labelled "ONN". ONS/ONE/GH split needs
    # Census Division data — deferred.
    "ONN": ["Ontario"],
}

# Output canvas — chosen to give North America a comfortable aspect ratio.
VIEW_W = 1200
VIEW_H = 800
MARGIN = 20

# Simplification tolerance in projected metres (Lambert Conformal Conic).
# 10 km is plenty for an on-screen map; preserves coastlines and major
# administrative boundaries.
SIMPLIFY_TOL_M = 10_000

# Re-projection target: North America LCC (handles US contiguous + Canada).
NA_LCC = (
    "+proj=lcc +lat_1=20 +lat_2=60 +lat_0=40 +lon_0=-96 "
    "+ellps=GRS80 +datum=NAD83 +units=m +no_defs"
)

# Clip latitudes above this — strips the empty Arctic coastline of NT/YT/QC
# that otherwise inflates the map's bbox. 70°N keeps every named settlement
# in the territories (Inuvik = 68.4°N is the northernmost).
NORTHERN_CLIP_LAT = 70.0

# Northeast / Mid-Atlantic inset. Sections in this cluster are small and
# overlap visually on the main map; the inset renders zoomed copies in a
# spare corner so they're clickable. Section IDs included drive both the
# clip geometry on the inset and the duplicate SVGPath emission.
NE_INSET_SECTIONS = {
    "DE", "MDC", "SNJ", "NNJ", "CT", "RI",
    "WNY", "ENY", "WMA", "EMA", "VT", "NH",
}
# Inset viewBox on the main 1200×800 canvas (x, y, w, h).
# Lower-right; sized larger so the small NE sections are easy to click. The
# zoomed copies sit on top of where these same (tiny) sections render on the
# main map — fine, since the inset is the readable rendition of them.
NE_INSET_BOX = (600, 420, 580, 360)


# ----------------------------------------------------------------------------
# Data loading
# ----------------------------------------------------------------------------

def load_us_section_table() -> dict[str, dict]:
    with open(US_SECTIONS) as f:
        raw = json.load(f)
    out = {}
    for division, sections in raw.items():
        if division.startswith("_"):
            continue
        for sec_id, info in sections.items():
            out[sec_id] = info
    return out


def state_fips_for(section_state: str) -> list[str]:
    # Special case: "MD+DC" composite.
    if section_state == "MD+DC":
        return [STATE_FIPS["MD"], STATE_FIPS["DC"]]
    return [STATE_FIPS[section_state]]


def normalize_county_name(n: str) -> str:
    # TIGER NAME column has bare county names. Section tables use slightly
    # different forms ("St. Lawrence" vs "St. Lawrence", "Miami-Dade" vs
    # "Miami-Dade", etc.). Normalize: lowercase + strip "." and whitespace.
    return n.lower().replace(".", "").replace("-", "").strip()


# ----------------------------------------------------------------------------
# Build per-section geometries (WGS84 / EPSG:4269)
# ----------------------------------------------------------------------------

def build_us_sections(counties: gpd.GeoDataFrame) -> dict[str, dict]:
    table = load_us_section_table()
    sections = {}
    unmatched = []

    for sec_id, info in table.items():
        if sec_id in DEFER_V1:
            continue
        state = info["state"]
        wanted = info["counties"]
        fips_codes = state_fips_for(state)
        df = counties[counties["STATEFP"].isin(fips_codes)]

        if wanted == ["*"]:
            geom = unary_union(df.geometry.values)
        else:
            wanted_norm = {normalize_county_name(n) for n in wanted}
            df = df.assign(_norm=df["NAME"].map(normalize_county_name))
            sel = df[df["_norm"].isin(wanted_norm)]
            found_norm = set(sel["_norm"].tolist())
            missing = wanted_norm - found_norm
            if missing:
                unmatched.append((sec_id, sorted(missing)))
            geom = unary_union(sel.geometry.values)

        sections[sec_id] = {"geometry": geom, "country": "US"}

    if unmatched:
        print("WARNING: unmatched counties:")
        for sec_id, miss in unmatched:
            print(f"  {sec_id}: {miss}")
    return sections


def build_ca_sections(provinces: gpd.GeoDataFrame) -> dict[str, dict]:
    sections = {}
    for sec_id, prov_names in CA_SECTION_PROVINCES.items():
        df = provinces[provinces["name"].isin(prov_names)]
        if df.empty:
            print(f"WARNING: no Canadian provinces matched for {sec_id} -> {prov_names}")
            continue
        geom = unary_union(df.geometry.values)
        sections[sec_id] = {"geometry": geom, "country": "CA"}
    return sections


# ----------------------------------------------------------------------------
# Projection + simplification + SVG emission
# ----------------------------------------------------------------------------

def project_and_simplify(sections: dict[str, dict]) -> gpd.GeoDataFrame:
    gdf = gpd.GeoDataFrame(
        {"id": list(sections.keys()), "country": [s["country"] for s in sections.values()]},
        geometry=[s["geometry"] for s in sections.values()],
        crs="EPSG:4269",
    )
    gdf = gdf.to_crs(NA_LCC)
    gdf["geometry"] = gdf.geometry.simplify(SIMPLIFY_TOL_M, preserve_topology=True)
    return gdf


def compute_inset_geometries(gdf_lcc: gpd.GeoDataFrame, section_ids: set[str],
                              inset_box: tuple[int,int,int,int]) -> dict[str, object]:
    """
    Given an already-projected GeoDataFrame and the IDs to include in an
    inset, return {section_id: shapely Polygon in viewBox-space} sized to fit
    inside inset_box.

    The inset uses the same projection as the main map (already applied) and
    just rescales/translates the cluster to the inset rectangle.
    """
    from shapely.ops import transform as shp_transform

    vx, vy, vw, vh = inset_box
    margin = 6

    cluster = gdf_lcc[gdf_lcc["id"].isin(section_ids)]
    if cluster.empty:
        return {}
    minx, miny, maxx, maxy = cluster.total_bounds
    src_w = maxx - minx
    src_h = maxy - miny
    scale = min((vw - 2*margin) / src_w, (vh - 2*margin) / src_h)
    content_w = src_w * scale
    content_h = src_h * scale
    ox = vx + (vw - content_w) / 2.0
    oy_top = vy + (vh - content_h) / 2.0

    def to_inset(x, y):
        sx = (x - minx) * scale + ox
        # Flip Y, anchor within the inset's vertical span.
        sy = vy + vh - ((y - miny) * scale + (vh - content_h) / 2.0)
        return sx, sy

    out = {}
    for _, row in cluster.iterrows():
        g = shp_transform(lambda x, y, z=None: to_inset(x, y), row.geometry)
        out[row["id"]] = g
    return out


def fit_to_viewbox(gdf: gpd.GeoDataFrame) -> tuple[gpd.GeoDataFrame, float, float, float, float]:
    minx, miny, maxx, maxy = gdf.total_bounds
    src_w = maxx - minx
    src_h = maxy - miny
    scale = min((VIEW_W - 2 * MARGIN) / src_w, (VIEW_H - 2 * MARGIN) / src_h)
    # Center the projected content inside the viewBox so we don't anchor to
    # the top-left corner — when src aspect doesn't match the canvas, the
    # leftover space is split evenly on both sides.
    content_w = src_w * scale
    content_h = src_h * scale
    offset_x  = (VIEW_W - content_w) / 2
    offset_y  = (VIEW_H - content_h) / 2

    # SVG Y axis points down, so flip in the projected→screen transform.
    def transform_point(x, y):
        sx = (x - minx) * scale + offset_x
        sy = VIEW_H - ((y - miny) * scale + offset_y)
        return sx, sy

    from shapely.ops import transform as shp_transform
    gdf = gdf.copy()
    gdf["geometry"] = gdf.geometry.apply(
        lambda g: shp_transform(lambda x, y, z=None: transform_point(x, y), g)
    )
    return gdf, minx, miny, scale, offset_x


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


def clip_north(gdf: gpd.GeoDataFrame) -> gpd.GeoDataFrame:
    """Trim geometries above NORTHERN_CLIP_LAT (in degrees, lat/lon CRS)."""
    clip_box = box(-180.0, -90.0, 180.0, NORTHERN_CLIP_LAT)
    gdf = gdf.copy()
    gdf["geometry"] = gdf.geometry.intersection(clip_box)
    return gdf


def main():
    print("Loading TIGER counties…")
    counties = gpd.read_file(COUNTY_SHP)
    print(f"  {len(counties)} counties loaded")

    print("Loading Natural Earth admin-1…")
    ne = gpd.read_file(NE_ADMIN1)
    provinces = ne[ne["admin"] == "Canada"]
    # Reproject Canadian provinces to same lat/lon CRS as TIGER for the clip.
    provinces = provinces.to_crs("EPSG:4269")
    provinces = clip_north(provinces)
    print(f"  {len(provinces)} Canadian provinces loaded (clipped at {NORTHERN_CLIP_LAT}°N)")

    print("Building US sections (dissolving counties)…")
    us = build_us_sections(counties)
    print(f"  {len(us)} US sections built")

    print("Building Canadian sections…")
    ca = build_ca_sections(provinces)
    print(f"  {len(ca)} CA sections built")

    all_sec = {**us, **ca}
    print(f"Total sections: {len(all_sec)}")

    print("Projecting + simplifying…")
    gdf_lcc = project_and_simplify(all_sec)

    print("Computing Northeast inset…")
    inset_geoms = compute_inset_geometries(gdf_lcc, NE_INSET_SECTIONS, NE_INSET_BOX)
    print(f"  inset covers {len(inset_geoms)} sections")

    print("Fitting main map to viewBox…")
    gdf, _minx, _miny, _scale, _ox = fit_to_viewbox(gdf_lcc)

    print("Emitting SVG paths…")
    out = {
        "viewBox": [0, 0, VIEW_W, VIEW_H],
        "insets":  [{"name": "Northeast", "viewBox": list(NE_INSET_BOX)}],
        "sections": {},
    }
    for _, row in gdf.iterrows():
        path = polygon_to_svg(row.geometry)
        if not path:
            print(f"  empty geometry for {row['id']}, skipping")
            continue
        cx, cy = row.geometry.representative_point().coords[0]
        bx0, by0, bx1, by1 = row.geometry.bounds
        sec = {
            "svgPath": path,
            "labelX":  round(cx, 1),
            "labelY":  round(cy, 1),
            "bbox":    [round(bx0, 1), round(by0, 1), round(bx1, 1), round(by1, 1)],
            "country": row["country"],
        }
        # Attach inset geometry if this section is in the cluster.
        ig = inset_geoms.get(row["id"])
        if ig is not None:
            ipath = polygon_to_svg(ig)
            if ipath:
                icx, icy = ig.representative_point().coords[0]
                ibx0, iby0, ibx1, iby1 = ig.bounds
                sec["inset"] = {
                    "svgPath": ipath,
                    "labelX":  round(icx, 1),
                    "labelY":  round(icy, 1),
                    "bbox":    [round(ibx0, 1), round(iby0, 1), round(ibx1, 1), round(iby1, 1)],
                }
        out["sections"][row["id"]] = sec

    OUT_JSON.parent.mkdir(parents=True, exist_ok=True)
    OUT_JSON.write_text(json.dumps(out, separators=(",", ":")))
    size_kb = OUT_JSON.stat().st_size / 1024
    inset_count = sum(1 for s in out["sections"].values() if "inset" in s)
    print(f"Wrote {OUT_JSON} ({size_kb:.1f} KB, {len(out['sections'])} sections, "
          f"{inset_count} with inset)")


if __name__ == "__main__":
    main()
