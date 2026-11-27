"""Build ars-fx's region layer for J-Map: the states and provinces of the US and Canada (na-states.bin.gz). Public domain
Natural Earth data. (This used to build a second layer, the ARRL / RAC Sweepstakes sections; that layer was removed with contest mode.)

Usage (from tools/basemap):
  python3 build_regions.py ne_50m_admin_1_states_provinces.geojson ne_50m_admin_0_countries.geojson ../../ars-fx/src/main/resources/com/ars/fx/map
Sources: https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/ne_50m_admin_1_states_provinces.geojson
         https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/ne_50m_admin_0_countries.geojson

Output is the format CountryShapes reads (big-endian, gzip'd): int regions; per region 3 bytes code, byte colour 1-7, int polygons;
per polygon int rings (outline first, then holes); per ring int points, then short lon, short lat in hundredths of a degree.
Colours are chosen so that regions that touch never share one (greedy DSATUR over shared boundary points).
"""
import gzip, json, math, os, struct, sys


def polys(g):
    return [g['coordinates']] if g['type'] == 'Polygon' else g['coordinates']


def bbox_lcc(geom):
    pts = [lcc(x, y) for poly in polys(geom) for ring in poly[:1] for x, y in ring]
    xs, ys = [p[0] for p in pts], [p[1] for p in pts]
    return min(xs), max(xs), min(ys), max(ys)


def from_geojson(geom):
    return [[[(x, y) for x, y in ring] for ring in poly] for poly in polys(geom)]


def colour(regions, tol):
    """regions: {code: [[ring...]]}. Greedy DSATUR over regions whose outlines come within tol degrees."""
    cell = {}
    def dens(ring):
        for i in range(len(ring)):
            (x0, y0), (x1, y1) = ring[i], ring[(i + 1) % len(ring)]
            n = max(1, int(math.hypot(x1 - x0, y1 - y0) / (tol / 2)))
            for s in range(n): yield x0 + (x1 - x0) * s / n, y0 + (y1 - y0) * s / n
    for code, pl in regions.items():
        for poly in pl:
            for ring in poly[:1]:
                for x, y in dens(ring): cell.setdefault((round(x / tol), round(y / tol)), set()).add(code)
    adj = {c: set() for c in regions}
    for codes in cell.values():
        for c in codes: adj[c] |= codes - {c}
    for (cx, cy), codes in cell.items():  # neighbouring cells too
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                o = cell.get((cx + dx, cy + dy))
                if o:
                    for c in codes: adj[c] |= o - {c}
    col = {}
    while len(col) < len(regions):
        c = max((c for c in regions if c not in col), key=lambda c: (len({col[n] for n in adj[c] if n in col}), len(adj[c])))
        used = {col[n] for n in adj[c] if n in col}
        col[c] = next(i for i in range(1, 20) if i not in used)
    assert max(col.values()) <= 7, 'needs %d colours' % max(col.values())
    return col


def write(path, regions, tol):
    col = colour(regions, tol)
    out = bytearray(struct.pack('>i', len(regions)))
    pts = 0
    for code in sorted(regions):
        out += code[:3].ljust(3).encode('ascii') + struct.pack('>bi', col[code], len(regions[code]))
        for poly in regions[code]:
            out += struct.pack('>i', len(poly))
            for ring in poly:
                q = []
                for lon, lat in ring:
                    v = (round(lon * 100), round(lat * 100))
                    if not q or q[-1] != v: q.append(v)
                out += struct.pack('>i', len(q))
                for lo, la in q: out += struct.pack('>hh', lo, la)
                pts += len(q)
    with gzip.open(path, 'wb', compresslevel=9) as z: z.write(bytes(out))
    print(path, len(regions), 'regions,', pts, 'points,', max(col.values()), 'colours,', os.path.getsize(path), 'bytes')


def main(admin1, admin0, outdir):
    a1 = json.load(open(admin1, encoding='utf-8'))['features']
    a0 = {f['properties']['ADM0_A3']: f for f in json.load(open(admin0, encoding='utf-8'))['features']}

    # ---- states and provinces of the US and Canada (Natural Earth's postal codes; PR and VI from the countries)
    na = {}
    for f in a1:
        p = f['properties']
        if p['admin'] in ('United States of America', 'Canada'): na[p['postal']] = from_geojson(f['geometry'])
    na['PR'] = from_geojson(a0['PRI']['geometry']); na['VI'] = from_geojson(a0['VIR']['geometry'])
    print(len(na), 'states and provinces')
    write(os.path.join(outdir, 'na-states.bin.gz'), na, 0.03)


if __name__ == '__main__':
    main(*sys.argv[1:4])
