"""Build ars-fx's two region layers for J-Map: the ARRL / RAC Sweepstakes sections (ss-sections.bin.gz) and the states and
provinces of the US and Canada (na-states.bin.gz). Public domain Natural Earth data plus the section outlines already shipped in
ars-fx/src/main/resources/com/ars/fx/maps/arrl-sections.json.

Usage (from tools/basemap):
  python3 build_regions.py ne_50m_admin_1_states_provinces.geojson ne_50m_admin_0_countries.geojson \
      ../../ars-fx/src/main/resources/com/ars/fx/maps/arrl-sections.json ../../ars-fx/src/main/resources/com/ars/fx/map
Sources: https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/ne_50m_admin_1_states_provinces.geojson
         https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/ne_50m_admin_0_countries.geojson

The section map (arrl-sections.json) is SVG in a Lambert conformal conic projection (the old build_sections.py: lat_1 20, lat_2 60,
lat_0 40, lon_0 -96, GRS80, scaled into 1200 x 800). That is undone here: the scale and offset are fitted to the sections that are
whole states, then every outline is turned back into longitude / latitude. AK, PAC (Hawaii), PR and VI were drawn as insets in
arbitrary places, so those four come from Natural Earth where they really are. MAR (NB + NS + PE) is dropped: it only duplicated three
sections that are drawn on their own. That leaves the 85 sections.

Output is the format CountryShapes reads (big-endian, gzip'd): int regions; per region 3 bytes code, byte colour 1-7, int polygons;
per polygon int rings (outline first, then holes); per ring int points, then short lon, short lat in hundredths of a degree.
Colours are chosen so that regions that touch never share one (greedy DSATUR over shared boundary points).
"""
import gzip, json, math, os, re, struct, sys

A, F = 6378137.0, 1 / 298.257222101
E = math.sqrt(2 * F - F * F)


def _m(p): s = math.sin(p); return math.cos(p) / math.sqrt(1 - E * E * s * s)
def _t(p): s = math.sin(p); return math.tan(math.pi / 4 - p / 2) / (((1 - E * s) / (1 + E * s)) ** (E / 2))

_P1, _P2, _P0, _L0 = map(math.radians, (20, 60, 40, -96))
_N = (math.log(_m(_P1)) - math.log(_m(_P2))) / (math.log(_t(_P1)) - math.log(_t(_P2)))
_FF = _m(_P1) / (_N * _t(_P1) ** _N)
_R0 = A * _FF * _t(_P0) ** _N


def lcc(lon, lat):
    rho = A * _FF * _t(math.radians(lat)) ** _N
    th = _N * (math.radians(lon) - _L0)
    return rho * math.sin(th), _R0 - rho * math.cos(th)


def lcc_inv(x, y):
    rho = math.copysign(math.hypot(x, _R0 - y), _N)
    t = (rho / (A * _FF)) ** (1 / _N)
    th = math.atan2(x, _R0 - y)
    p = math.pi / 2 - 2 * math.atan(t)
    for _ in range(8):
        s = math.sin(p)
        p = math.pi / 2 - 2 * math.atan(t * ((1 - E * s) / (1 + E * s)) ** (E / 2))
    return math.degrees(th / _N + _L0), math.degrees(p)


def polys(g):
    return [g['coordinates']] if g['type'] == 'Polygon' else g['coordinates']


def bbox_lcc(geom):
    pts = [lcc(x, y) for poly in polys(geom) for ring in poly[:1] for x, y in ring]
    xs, ys = [p[0] for p in pts], [p[1] for p in pts]
    return min(xs), max(xs), min(ys), max(ys)


def fit(sections, states):
    """Least squares for sx = k*X + ox, sy = oy - k*Y from the sections that are exactly one state."""
    rows = []  # (coefficient on k, on ox/oy selector, target)
    A_ = []; b_ = []
    for sid, name in (('WY', 'Wyoming'), ('CO', 'Colorado'), ('ND', 'North Dakota'), ('SD', 'South Dakota'), ('NE', 'Nebraska'),
                      ('KS', 'Kansas'), ('UT', 'Utah'), ('NM', 'New Mexico'), ('AZ', 'Arizona'), ('NV', 'Nevada'), ('ID', 'Idaho'),
                      ('MT', 'Montana'), ('OK', 'Oklahoma'), ('IA', 'Iowa'), ('MN', 'Minnesota'), ('MO', 'Missouri'),
                      ('AR', 'Arkansas'), ('IL', 'Illinois'), ('WI', 'Wisconsin'), ('KY', 'Kentucky'), ('TN', 'Tennessee')):
        g = states[name]
        x0, x1, y0, y1 = bbox_lcc(g)
        bx0, by0, bx1, by1 = sections[sid]['bbox']
        # unknowns [k, ox, oy]
        for X, t in ((x0, bx0), (x1, bx1)): A_.append([X, 1, 0]); b_.append(t)
        for Y, t in ((y1, by0), (y0, by1)): A_.append([-Y, 0, 1]); b_.append(t)
    # normal equations
    n = 3
    ata = [[sum(r[i] * r[j] for r in A_) for j in range(n)] for i in range(n)]
    atb = [sum(r[i] * b for r, b in zip(A_, b_)) for i in range(n)]
    for i in range(n):  # gaussian elimination
        p = max(range(i, n), key=lambda r: abs(ata[r][i])); ata[i], ata[p] = ata[p], ata[i]; atb[i], atb[p] = atb[p], atb[i]
        for r in range(i + 1, n):
            f = ata[r][i] / ata[i][i]
            for c in range(i, n): ata[r][c] -= f * ata[i][c]
            atb[r] -= f * atb[i]
    sol = [0] * n
    for i in reversed(range(n)):
        sol[i] = (atb[i] - sum(ata[i][j] * sol[j] for j in range(i + 1, n))) / ata[i][i]
    k, ox, oy = sol
    worst = max(abs(sum(r[j] * sol[j] for j in range(n)) - b) for r, b in zip(A_, b_))
    print('fit: k=%.6e ox=%.2f oy=%.2f, worst bbox residual %.1f px' % (k, ox, oy, worst))
    return k, ox, oy


def svg_rings(d):
    out = []
    for sub in re.findall(r'M[^M]*', d):
        pts = [tuple(map(float, m)) for m in re.findall(r'(-?\d+\.?\d*),(-?\d+\.?\d*)', sub)]
        if len(pts) > 1 and pts[0] == pts[-1]: pts = pts[:-1]
        if len(pts) >= 3: out.append(pts)
    return out


def area(r):
    return 0.5 * sum(r[i][0] * r[(i + 1) % len(r)][1] - r[(i + 1) % len(r)][0] * r[i][1] for i in range(len(r)))


def inside(pt, ring):
    x, y = pt; c = False
    for i in range(len(ring)):
        xi, yi = ring[i]; xj, yj = ring[i - 1]
        if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi) + xi: c = not c
    return c


def group(rings):
    """Rings -> polygons [outline, holes...]: a ring inside an odd number of others is a hole of the smallest one holding it."""
    rs = sorted(rings, key=lambda r: -abs(area(r)))
    depth, parent = [], []
    for i, r in enumerate(rs):
        holders = [j for j in range(i) if inside(r[0], rs[j])]
        depth.append(len(holders)); parent.append(holders[-1] if holders else None)
    polys_ = {}
    for i, r in enumerate(rs):
        if depth[i] % 2 == 0: polys_[i] = [r]
    for i, r in enumerate(rs):
        if depth[i] % 2 == 1 and parent[i] in polys_: polys_[parent[i]].append(r)
    return list(polys_.values())


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


def main(admin1, admin0, sections_json, outdir):
    a1 = json.load(open(admin1, encoding='utf-8'))['features']
    a0 = {f['properties']['ADM0_A3']: f for f in json.load(open(admin0, encoding='utf-8'))['features']}
    states = {f['properties']['name']: f['geometry'] for f in a1}
    secs = json.load(open(sections_json))['sections']
    k, ox, oy = fit(secs, states)

    # ---- sweepstakes sections
    ss = {}
    ne_for = {'AK': states['Alaska'], 'PAC': states['Hawaii'], 'PR': a0['PRI']['geometry'], 'VI': a0['VIR']['geometry']}
    for sid, s in secs.items():
        if sid == 'MAR': continue
        if sid in ne_for:
            ss[sid] = from_geojson(ne_for[sid]); continue
        rings = [[lcc_inv((x - ox) / k, (oy - y) / k) for x, y in r] for r in svg_rings(s['svgPath'])]
        ss[sid] = group(rings)
    print(len(ss), 'sections')
    write(os.path.join(outdir, 'ss-sections.bin.gz'), ss, 0.3)

    # ---- states and provinces of the US and Canada (Natural Earth's postal codes; PR and VI from the countries)
    na = {}
    for f in a1:
        p = f['properties']
        if p['admin'] in ('United States of America', 'Canada'): na[p['postal']] = from_geojson(f['geometry'])
    na['PR'] = from_geojson(a0['PRI']['geometry']); na['VI'] = from_geojson(a0['VIR']['geometry'])
    print(len(na), 'states and provinces')
    write(os.path.join(outdir, 'na-states.bin.gz'), na, 0.03)


if __name__ == '__main__':
    main(*sys.argv[1:5])
