#!/usr/bin/env python3
"""
Rebuild the engine's DXCC prefix table (j-log-engine/.../scoring/dxcc-prefixes.json) from the contest country files.

    tools/dxcc-prefixes/gen_dxcc_prefixes.py [--cty FILE] [--out FILE] [--refresh-adif] [--check]

How it works
  1. Start from base-prefixes.json (the hand-kept table: one row per prefix) and apply the FIXES below (entity numbers
     that were wrong, and 'FT' which is split by cty.dat instead).
  2. Number each cty.dat entity with its ADIF DXCC entity code: by name against adif-entities.json (deleted codes are never
     used), with the OVERRIDES below for names that do not match.
  3. For every prefix cty.dat gives an entity, add or replace a table row when the table does not already resolve that
     prefix to the same number and continent. Shorter prefixes go first so a longer one sees what the shorter changed.
     Exact-callsign entries (=W1AW) are ignored: the engine matches prefixes only.
  4. Write the table (one row per line, same layout as before).

  --cty           country file to read (default: the copy the app ships, ars-fx/src/main/resources/com/ars/fx/cty/cty.dat)
  --refresh-adif  download the current ADIF entity list from adif.org into adif-entities.json first
  --check         write nothing; exit 1 if the table on disk differs from what would be generated

A prefix the table gets wrong but cty.dat does not mention is not touched: fix base-prefixes.json or FIXES.
After running, `mvn test` in ars-fx (CtyDataTest.engineTableAgreesWithCountryFiles) checks the result against cty.dat.
"""
import argparse, difflib, html, json, re, sys, urllib.request
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
OUT_DEFAULT = ROOT / 'j-log-engine/src/main/resources/com/jlog/scoring/dxcc-prefixes.json'
CTY_DEFAULT = ROOT / 'ars-fx/src/main/resources/com/ars/fx/cty/cty.dat'
ADIF_URL = 'https://adif.org/314/ADIF_314.htm'

# (country, wrong number in base-prefixes.json) -> the ADIF entity code
RENUMBER = {('Gambia', 35): 422, ('Liberia', 37): 434, ('Chagos Islands', 38): 33, ('Grenada', 75): 77,
            ('E. Kiribati', 123): 48, ('Ogasawara', 177): 192, ('Thailand', 268): 387, ('Vanuatu', 297): 158,
            ('Belarus', 370): 27, ('Western Sahara', 432): 302, ('Reunion', 483): 453}
RENAME = {'Netherlands Antilles': 'Bonaire'}         # its number, 520, is Bonaire in ADIF
DROP_PREFIXES = {'FT'}                               # was 'French Southern' under 508 (Austral I.); cty.dat splits it properly
# cty.dat entity name -> ADIF entity code (None: not a DXCC entity). Everything else is matched by name.
OVERRIDES = {'Vienna Intl Ctr': None, 'Kingdom of Eswatini': 468, 'Dem. Rep. of the Congo': 414, 'DPR of Korea': 344,
             'Banaba Island': 490, 'Western Kiribati': 301, 'Central Kiribati': 31, 'Eastern Kiribati': 48,
             'Asiatic Turkey': 390, 'European Turkey': 390, 'Republic of South Sudan': 521, 'Shetland Islands': 279,
             'African Italy': 248, 'Sicily': 248, 'Sov Mil Order of Malta': 246, 'Fed. Rep. of Germany': 230,
             'Vatican City': 295, 'North Macedonia': 502, 'United States': 291, 'UK Base Areas on Cyprus': 283,
             'Sint Maarten': 518, 'US Virgin Islands': 285, 'Cabo Verde': 409, 'Central African Republic': 408,
             'Palestine': 510, 'Comoros': 411}

def refresh_adif():
    t = urllib.request.urlopen(ADIF_URL, timeout=60).read().decode('utf8', 'ignore')
    j = t.find('id="Enumeration_DXCC_Entity_Code"')
    if j < 0: sys.exit('adif.org page layout changed: DXCC enumeration not found')
    seg = t[j:t.find('</table>', j)]
    out = {}
    for r in re.findall(r'<tr[^>]*>(.*?)</tr>', seg, re.S):
        c = [html.unescape(re.sub(r'<[^>]+>', '', x)).strip() for x in re.findall(r'<td[^>]*>(.*?)</td>', r, re.S)]
        if len(c) >= 2 and c[0].isdigit() and int(c[0]) > 0:
            out[int(c[0])] = {'name': c[1], 'deleted': c[2] if len(c) > 2 else ''}
    if len(out) < 300: sys.exit('only %d ADIF entities parsed; not writing' % len(out))
    (HERE / 'adif-entities.json').write_text('{\n' + ',\n'.join(f'  "{k}": {json.dumps(v)}' for k, v in sorted(out.items())) + '\n}\n')
    print('adif-entities.json: %d entities' % len(out))

def parse_cty(path):
    ents, cur, body = [], None, ''
    for line in Path(path).read_text(encoding='latin1').splitlines():
        l = line.strip()
        if not l: continue
        if cur is None:
            f = l.split(':')
            if len(f) < 8: continue
            cur = dict(name=f[0].strip(), cont=f[3].strip(), lat=float(f[4]), lon=-float(f[5])); body = ''
        else:
            body += l
            if l.endswith(';'):
                cur['aliases'] = [t.strip() for t in body.rstrip(';').split(',') if t.strip()]
                ents.append(cur); cur = None
    return ents

def norm(s):
    return re.sub(r'[^a-z0-9 ]', '', s.lower().replace('&', 'and').replace('st.', 'saint').replace('is.', 'islands').replace('i.', 'island')).strip()

def build(cty_path):
    adif = {int(k): v for k, v in json.loads((HERE / 'adif-entities.json').read_text()).items()}
    live = {k: norm(v['name']) for k, v in adif.items() if v['deleted'] != 'Y'}
    ents = parse_cty(cty_path)
    number = {}
    for e in ents:
        if e['name'] in OVERRIDES: number[e['name']] = OVERRIDES[e['name']]; continue
        best = difflib.get_close_matches(norm(e['name']), list(live.values()), 1, 0.0)[0]
        number[e['name']] = [k for k, v in live.items() if v == best][0]

    base = json.loads((HERE / 'base-prefixes.json').read_text())
    for r in base:
        r['dxcc'] = RENUMBER.get((r['country'], r['dxcc']), r['dxcc'])
        r['country'] = RENAME.get(r['country'], r['country'])
    base = [r for r in base if r['prefix'].upper() not in DROP_PREFIXES]
    names = {}
    for r in base: names.setdefault(r['dxcc'], r['country'])      # the table's own name for an entity wins
    table = {}
    for r in base: table.setdefault(r['prefix'].upper(), r)       # the loader keeps the first of a duplicate

    CONT = re.compile(r'\{([A-Za-z]+)\}')
    def split(a):
        m = CONT.search(a)
        return re.sub(r'[\(\[<{~].*', '', a).upper(), (m.group(1).upper() if m else None)
    cty_prefix, cty_exact = {}, {}
    for e in ents:
        for a in e['aliases']:
            ex = a.startswith('=')
            b, c = split(a[1:] if ex else a)
            (cty_exact if ex else cty_prefix)[b] = (e, c or e['cont'])
    def cty_res(call):
        if call in cty_exact: return cty_exact[call]
        for n in range(len(call), 0, -1):
            if call[:n] in cty_prefix: return cty_prefix[call[:n]]
    def res(call):
        for n in range(len(call), 0, -1):
            if call[:n] in table: return table[call[:n]]

    cands = []
    for e in ents:
        if number[e['name']] is None: continue
        for a in e['aliases']:
            if a.startswith('='): continue
            b, c = split(a)
            cands.append((len(b), b, e, number[e['name']], c or e['cont']))
    cands.sort(key=lambda x: (x[0], x[1]))
    added = {}
    for _, b, e, n, cont in cands:
        probe = b + ('AA' if b[-1].isdigit() else '1AA')
        hit = cty_res(probe)
        if not hit or hit[0] is not e: continue               # a longer cty prefix owns this call: that one is tested instead
        cur = res(probe)
        if cur is not None and cur['dxcc'] == n and cur['continent'] == cont: continue
        if b in added: continue
        row = dict(prefix=b, country=names.get(n) or e['name'], continent=cont, dxcc=n, lat=round(e['lat'], 2), lon=round(e['lon'], 2))
        added[b] = row; table[b] = row
    keep = [r for r in base if r['prefix'].upper() not in added]
    return keep + list(added.values()), len(base), len(added)

def render(rows):
    def line(e):
        d = dict(prefix=e['prefix'], country=e['country'], continent=e['continent'], dxcc=e['dxcc'], lat=e['lat'], lon=e['lon'])
        if 'timezone' in e: d['timezone'] = e['timezone']
        return '  ' + json.dumps(d, separators=(',', ':'), ensure_ascii=False)
    return '[\n' + ',\n'.join(line(e) for e in rows) + '\n]\n'

def main():
    ap = argparse.ArgumentParser(description=__doc__.split('\n\n')[0])
    ap.add_argument('--cty', default=str(CTY_DEFAULT)); ap.add_argument('--out', default=str(OUT_DEFAULT))
    ap.add_argument('--refresh-adif', action='store_true'); ap.add_argument('--check', action='store_true')
    a = ap.parse_args()
    if a.refresh_adif: refresh_adif()
    rows, nbase, nadded = build(a.cty)
    text = render(rows)
    out = Path(a.out)
    if a.check:
        same = out.exists() and out.read_text(encoding='utf8') == text
        print('dxcc-prefixes.json is %s' % ('up to date' if same else 'OUT OF DATE: run tools/dxcc-prefixes/gen_dxcc_prefixes.py'))
        sys.exit(0 if same else 1)
    out.write_text(text, encoding='utf8')
    print('%s: %d rows (%d from the base table, %d added or replaced from cty.dat)' % (out, len(rows), nbase, nadded))

if __name__ == '__main__':
    main()
