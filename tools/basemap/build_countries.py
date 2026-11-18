"""Build ars-fx's country shapes (countries50.bin.gz) from Natural Earth's 1:50m admin-0 countries (public domain).

Usage: python3 build_countries.py ne_50m_admin_0_countries.geojson ../../ars-fx/src/main/resources/com/ars/fx/map/countries50.bin.gz
Source: https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/ne_50m_admin_0_countries.geojson

Format (big-endian, gzip'd), read by com.ars.fx.data.CountryShapes:
  int countries
  per country: 3 bytes ADM0_A3 (ASCII), byte MAPCOLOR7 (1-7; neighbours never share one), int polygons
    per polygon: int rings (the first is the outline, the rest holes)
      per ring: int points, then per point short lon, short lat in hundredths of a degree
Consecutive points that round to the same hundredth are dropped.
"""
import gzip, json, struct, sys

def main(src, dst):
    feats = json.load(open(src, encoding='utf-8'))['features']
    out = bytearray(struct.pack('>i', len(feats)))
    pts = 0
    for f in feats:
        p, g = f['properties'], f['geometry']
        code = (p.get('ADM0_A3') or '---')[:3].ljust(3, '-').encode('ascii')
        colour = int(p.get('MAPCOLOR7') or 1)
        polys = [g['coordinates']] if g['type'] == 'Polygon' else g['coordinates']
        out += code + struct.pack('>bi', colour, len(polys))
        for poly in polys:
            out += struct.pack('>i', len(poly))
            for ring in poly:
                q = []
                for lon, lat in ring:
                    v = (round(lon * 100), round(lat * 100))
                    if not q or q[-1] != v: q.append(v)
                out += struct.pack('>i', len(q))
                for lo, la in q: out += struct.pack('>hh', lo, la)
                pts += len(q)
    with gzip.open(dst, 'wb', compresslevel=9) as z: z.write(bytes(out))
    print(len(feats), 'countries,', pts, 'points')

if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])
