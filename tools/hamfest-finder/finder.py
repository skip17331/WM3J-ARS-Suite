#!/usr/bin/env python3
"""Hamfest finder: crawl club and event pages politely and compile a list of upcoming hamfests.

Starts from the pages in seeds.txt, follows links that look like hamfest / calendar / event pages, pulls events out of
each page (schema.org Event data, .ics files, then dated text next to hamfest words), merges duplicates and writes
events.json. Only facts are kept: name, dates, place and a link back to the page that announced the event. No page
text is stored or republished.

Standard library only. Polite: robots.txt and its Crawl-delay are obeyed, noindex / nofollow are obeyed, one request
per host every 5 seconds, small page limits, conditional requests (ETag / Last-Modified), and a User-Agent that names
the tool and gives a contact address. See README.md for the rules about what may be a seed.
"""
import argparse, datetime as dt, difflib, hashlib, html, ipaddress, json, re, socket, sys, time
import urllib.error, urllib.parse, urllib.request, urllib.robotparser
from pathlib import Path

CONTACT = "wm3jmike@gmail.com"
UA = f"WM3J-ARS-Suite-HamfestFinder/1.0 (+https://github.com/skip17331/WM3J-ARS-Suite; contact {CONTACT})"
MAX_BYTES = 2_000_000
MAX_PER_PAGE = 30            # a page that "has" more events than this is a directory, not an announcement: keep the best
MIN_CONFIDENCE = 0.5
KEYWORDS = re.compile(r"hamfest|ham\s*fest|hamcon\b|swap\s*-?\s*fest|swap\s*-?\s*meet|tailgate|flea\s*market|hamvention|"
                      r"ham\s*expo|ham\s*fair|radio\s*(?:fest|festival|convention)|amateur\s*radio\s*(?:fest|festival|convention|expo)|"
                      r"ham\s*radio\s*(?:convention|festival|swap)|division\s*convention|state\s*convention|hamarama", re.I)
LINK_HINT = re.compile(r"hamfest|swap|tailgate|flea|convention|hamcon|expo|events?\b|calendar|\.ics\b", re.I)
STATES = set("AL AK AZ AR CA CO CT DE DC FL GA HI ID IL IN IA KS KY LA ME MD MA MI MN MS MO MT NE NV NH NJ NM NY NC ND OH OK OR PA RI SC SD TN TX UT VT VA WA WV WI WY AB BC MB NB NL NS ON PE QC SK".split())
MONTHS = {m[:3]: i for i, m in enumerate("january february march april may june july august september october november december".split(), 1)}
def mon(w):
    return MONTHS[w.lower()[:3]]
MON = r"(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|june?|july?|aug(?:ust)?|sept?(?:ember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\.?"
DASH = r"\s*(?:-|–|—|&|and|to|through|thru)\s*"
RE_MD = re.compile(rf"\b{MON}\s+(\d{{1,2}})(?:st|nd|rd|th)?(?:{DASH}(?:{MON}\s+)?(\d{{1,2}})(?:st|nd|rd|th)?)?(?:,?\s*(20\d\d))?\b", re.I)
RE_NUM = re.compile(r"\b(\d{1,2})/(\d{1,2})/(\d{4}|\d{2})\b")
RE_PLACE = re.compile(r"\(?\b([A-Z][A-Za-z.'’ -]{2,30}),\s*([A-Z]{2})\b\)?")


# ---------------------------------------------------------------- dates and text
def to_date(y, m, d):
    try:
        return dt.date(y, m, d)
    except ValueError:
        return None


def infer_year(m, d, today, hint=None):
    """No year in the text: the page's year if it names one, else the next time that date comes round."""
    if hint:
        return hint
    y = today.year
    c = to_date(y, m, d)
    return y + 1 if c and c < today - dt.timedelta(days=60) else y


def find_dates(text, today, hint=None):
    """[(start, end, pos)] for every date or date range in the text."""
    out = []
    for m in RE_MD.finditer(text):
        mo = mon(m.group(1)); d1 = int(m.group(2)); mo2 = mon(m.group(3)) if m.group(3) else mo
        d2 = int(m.group(4)) if m.group(4) else d1
        y = int(m.group(5)) if m.group(5) else infer_year(mo, d1, today, hint)
        s, e = to_date(y, mo, d1), to_date(y + (1 if mo2 < mo else 0), mo2, d2)
        if s and e and s <= e and (e - s).days <= 10:
            out.append((s, e, m.start()))
    for m in RE_NUM.finditer(text):
        y = int(m.group(3)); y += 2000 if y < 100 else 0
        s = to_date(y, int(m.group(1)), int(m.group(2)))
        if s:
            out.append((s, s, m.start()))
    return out


def page_text(src):
    """HTML to text with a line break at each block tag, so one listing stays on one line."""
    src = re.sub(r"(?is)<(script|style|noscript|svg|head)\b.*?</\1>", " ", src)
    src = re.sub(r"(?i)<\s*(br|/p|/div|/li|/tr|/h\d|/ul|/table|/section|/article)\b[^>]*>", "\n", src)
    src = re.sub(r"(?s)<[^>]+>", " ", src)
    return "\n".join(re.sub(r"[ \t\r\f\v ]+", " ", l).strip() for l in html.unescape(src).split("\n") if l.strip())


def year_hint(text, today):
    ys = [int(y) for y in re.findall(r"\b(20\d\d)\b", text[:3000])]
    ys = [y for y in ys if today.year - 1 <= y <= today.year + 2]
    return max(set(ys), key=ys.count) if ys else None


def place_of(s):
    for m in RE_PLACE.finditer(s):
        if m.group(2) in STATES:
            return m.group(1).strip(" -.,("), m.group(2)
    return "", ""


def tidy_title(s):
    s = RE_MD.sub(" ", s); s = RE_NUM.sub(" ", s); s = RE_PLACE.sub(" ", s)
    s = re.sub(r"(?i)\b(?:mon|tue|tues|wed|thu|thur|thurs|fri|sat|sun)(?:day|sday|nesday|rsday|urday)?\b\.?", " ", s)
    s = re.sub(r"[\s\-–—:|·•@,()\[\]]+", " ", s).strip(" -–—:|·•@,.")
    return s[:90]


def mk(title, s, e, city, state, url, conf):
    return {"title": title, "start": s.isoformat(), "end": e.isoformat(), "city": city, "state": state, "url": url, "confidence": conf}


# ---------------------------------------------------------------- extraction
def from_jsonld(src, url, today):
    out = []

    def walk(o):
        if isinstance(o, list):
            for x in o: walk(x)
        elif isinstance(o, dict):
            t = o.get("@type"); t = " ".join(t) if isinstance(t, list) else str(t or "")
            if "Event" in t and o.get("startDate"):
                name = str(o.get("name", "")); blob = name + " " + str(o.get("description", ""))
                s = parse_iso(o.get("startDate")); e = parse_iso(o.get("endDate")) or s
                if s and e and KEYWORDS.search(blob):
                    loc = o.get("location") if isinstance(o.get("location"), dict) else {}
                    adr = loc.get("address") if isinstance(loc.get("address"), dict) else {}
                    city, st = str(adr.get("addressLocality", "")), str(adr.get("addressRegion", "")).upper()[:2]
                    out.append(mk(tidy_title(name) or name[:90], s, max(s, e), city, st if st in STATES else "", str(o.get("url") or url), 0.9))
            for v in o.values():
                if isinstance(v, (dict, list)): walk(v)
    for blob in re.findall(r'(?is)<script[^>]+application/ld\+json[^>]*>(.*?)</script>', src):
        try: walk(json.loads(html.unescape(blob.strip())))
        except ValueError: pass
    return out


def parse_iso(v):
    m = re.match(r"(\d{4})-(\d\d)-(\d\d)", str(v or ""))
    return to_date(int(m.group(1)), int(m.group(2)), int(m.group(3))) if m else None


def from_ics(text, url):
    out = []
    text = re.sub(r"\r?\n[ \t]", "", text)
    for block in re.findall(r"(?s)BEGIN:VEVENT(.*?)END:VEVENT", text):
        def f(k):
            m = re.search(rf"(?m)^{k}[^:\n]*:(.*)$", block)
            return m.group(1).strip().replace("\\,", ",").replace("\;", ";").replace("\\n", " ") if m else ""
        d = lambda v: (to_date(int(v[:4]), int(v[4:6]), int(v[6:8])) if re.match(r"\d{8}", v) else None)
        s, e = d(f("DTSTART")), d(f("DTEND"))
        if not s or not KEYWORDS.search(f("SUMMARY") + " " + f("DESCRIPTION")):
            continue
        if e and "VALUE=DATE" in block and e > s: e -= dt.timedelta(days=1)   # all-day DTEND is exclusive
        city, st = place_of(f("LOCATION"))
        out.append(mk(tidy_title(f("SUMMARY")) or f("SUMMARY")[:90], s, e or s, city, st, f("URL") or url, 0.9))
    return out


def from_text(src, url, today):
    text = page_text(src); hint = year_hint(text, today)
    lines = text.split("\n"); out = []
    for i, line in enumerate(lines):
        for s, e, pos in find_dates(line, today, hint):
            blk = line if KEYWORDS.search(line) else ""
            if not blk and i > 0 and KEYWORDS.search(lines[i - 1]) and len(lines[i - 1]) < 100 and not find_dates(lines[i - 1], today, hint):
                blk = lines[i - 1] + " " + line          # a heading with the date on the next line
            if not blk or len(line) > 400 or any(int(y) < today.year for y in re.findall(r"\b(20\d\d)\b", blk)):
                continue                                  # not a listing, or it names an earlier year
            city, st = place_of(blk)
            if not st and i + 1 < len(lines) and len(lines[i + 1]) < 120:
                city, st = place_of(lines[i + 1])
            title = tidy_title(blk)
            if len(title) < 4:
                continue
            out.append(mk(title, s, e, city, st, url, 0.6 if st else 0.45))
    return out


def extract(body, ctype, url, today):
    if "calendar" in ctype or url.lower().split("?")[0].endswith(".ics"):
        found = from_ics(body, url)
    else:
        found = from_jsonld(body, url, today) or from_text(body, url, today)
    found = [x for x in found if dt.date.fromisoformat(x["end"]) >= today
             and dt.date.fromisoformat(x["start"]) <= today + dt.timedelta(days=550)]
    found.sort(key=lambda x: -x["confidence"])
    return found[:MAX_PER_PAGE]


# ---------------------------------------------------------------- merging
def norm(t):
    return " ".join(w for w in re.sub(r"[^a-z0-9 ]", " ", t.lower()).split() if w not in {"the", "annual", "of", "and", "20", "arrl"} and not re.fullmatch(r"20\d\d|\d+(st|nd|rd|th)", w))


def merge(events):
    """Same date (or overlapping) and state with a similar name is one event; keep the most confident, count the sources."""
    out = []
    for e in sorted(events, key=lambda x: -x["confidence"]):
        for k in out:
            if k["state"] == e["state"] or not k["state"] or not e["state"]:
                if k["start"] == e["start"] and difflib.SequenceMatcher(None, norm(k["title"]), norm(e["title"])).ratio() >= 0.75:
                    if e["url"] not in k["sources"]:      # a second page announcing it raises our confidence
                        k["sources"] = sorted(k["sources"] + [e["url"]]); k["confidence"] = round(min(0.95, k["confidence"] + 0.1), 2)
                    if not k["state"]: k["city"], k["state"] = e["city"], e["state"]
                    break
        else:
            out.append({**e, "sources": [e["url"]]})
    for k in out: k["id"] = hashlib.sha1((k["start"] + norm(k["title"]) + k["state"]).encode()).hexdigest()[:12]
    return sorted(out, key=lambda x: (x["start"], x["title"]))


# ---------------------------------------------------------------- crawling
class Crawler:
    def __init__(self, delay=5.0, max_pages=300, per_host=15, allow_local=False, state=None, log=lambda m: None):
        self.delay, self.max_pages, self.per_host, self.allow_local, self.log = delay, max_pages, per_host, allow_local, log
        self.robots, self.last, self.count, self.state = {}, {}, {}, state if state is not None else {}
        self.today = dt.date.today()

    def _open(self, url, headers=None, limit=MAX_BYTES):
        rq = urllib.request.Request(url, headers={"User-Agent": UA, **(headers or {})})
        with urllib.request.urlopen(rq, timeout=30) as r:
            return r.status, dict(r.headers), r.read(limit + 1)[:limit]

    def _safe_host(self, host):
        if self.allow_local: return True
        try:
            return not any((lambda a: a.is_private or a.is_loopback or a.is_link_local or a.is_reserved)(ipaddress.ip_address(i[4][0]))
                           for i in socket.getaddrinfo(host, None))
        except OSError:
            return False

    def robots_for(self, url):
        p = urllib.parse.urlsplit(url); root = f"{p.scheme}://{p.netloc}"
        if root not in self.robots:
            rp = urllib.robotparser.RobotFileParser()
            try:
                self.wait(p.netloc); st, _, body = self._open(root + "/robots.txt", limit=500_000); rp.parse(body.decode("utf-8", "replace").splitlines())
            except urllib.error.HTTPError as ex:      # RFC 9309: a 4xx means there is no robots.txt; a 5xx means assume no
                rp.parse([] if 400 <= ex.code < 500 else ["User-agent: *", "Disallow: /"])
            except Exception:
                rp.parse(["User-agent: *", "Disallow: /"])        # cannot read it: assume no
            self.robots[root] = rp
        return self.robots[root]

    def wait(self, host, extra=0.0):
        gap = max(self.delay, extra) - (time.time() - self.last.get(host, 0))
        if gap > 0: time.sleep(gap)
        self.last[host] = time.time()

    def fetch(self, url):
        """(body, content type) or None. Obeys robots.txt (and its Crawl-delay) and keeps to the host limits."""
        p = urllib.parse.urlsplit(url)
        if p.scheme not in ("http", "https") or not p.netloc or not self._safe_host(p.hostname or ""): return None
        if self.count.get(p.netloc, 0) >= self.per_host: return None
        rp = self.robots_for(url)
        if not rp.can_fetch(UA, url): self.log(f"robots.txt says no: {url}"); return None
        cd = rp.crawl_delay(UA) or 0
        old = self.state.get(url, {})
        hdr = {k: v for k, v in (("If-None-Match", old.get("etag")), ("If-Modified-Since", old.get("modified"))) if v}
        self.wait(p.netloc, cd); self.count[p.netloc] = self.count.get(p.netloc, 0) + 1
        try:
            st, h, raw = self._open(url, hdr)
        except urllib.error.HTTPError as ex:
            if ex.code == 304: return ("", "not-modified")
            self.log(f"{ex.code}: {url}"); return None
        except Exception as ex:
            self.log(f"failed ({ex}): {url}"); return None
        ct = h.get("Content-Type", "").lower()
        if "html" not in ct and "calendar" not in ct and not url.lower().split("?")[0].endswith(".ics"): return None
        if "noindex" in h.get("X-Robots-Tag", "").lower(): return None
        self.state[url] = {"etag": h.get("ETag"), "modified": h.get("Last-Modified"), "events": [], "links": []}
        return raw.decode("utf-8", "replace"), ct

    def links(self, body, base):
        if re.search(r'(?i)<meta[^>]+name=["\']robots["\'][^>]+content=["\'][^"\']*(nofollow|none)', body): return []
        out = []
        for m in re.finditer(r'(?is)<a\b[^>]*href=["\']([^"\'#]+)[^>]*>(.*?)</a>', body):
            u = urllib.parse.urljoin(base, html.unescape(m.group(1))).split("#")[0]
            if re.search(r'(?i)rel=["\'][^"\']*nofollow', m.group(0)[:m.group(0).find(">")]): continue
            if LINK_HINT.search(re.sub(r"<[^>]+>", " ", m.group(2)) + " " + urllib.parse.urlsplit(u).path): out.append(u)
        return out

    def run(self, seeds):
        queue = [(u, 0) for u in seeds]; seen = set(seeds); events = []; pages = 0
        while queue and pages < self.max_pages:
            url, depth = queue.pop(0)
            got = self.fetch(url)
            if not got: continue
            pages += 1; body, ct = got
            if ct == "not-modified":                      # unchanged: reuse what we found and where it led
                found, links = self.state[url]["events"], self.state[url].get("links", [])
            else:
                found = extract(body, ct, url, self.today); links = self.links(body, url)
                self.state[url].update(events=found, links=links)
            if depth < 2:
                host = urllib.parse.urlsplit(url).netloc
                for u in links:
                    same = urllib.parse.urlsplit(u).netloc == host
                    if u not in seen and (depth == 0 or same): seen.add(u); queue.append((u, depth + 1))
            events += [e for e in found if dt.date.fromisoformat(e["end"]) >= self.today]
            self.log(f"{pages}: {len(found)} events  {url}")
        return events


# ---------------------------------------------------------------- output
def publish(events, out_path, previous_floor=0.5, now=None):
    """Write the file. Refuse to replace a good list with one that lost half its events (a broken crawl)."""
    keep = [e for e in merge(events) if e["confidence"] >= MIN_CONFIDENCE]
    p = Path(out_path)
    if p.exists():
        try:
            old = len(json.loads(p.read_text())["events"])
            if old >= 10 and len(keep) < old * previous_floor:
                raise SystemExit(f"refusing to publish: {len(keep)} events, last time {old}")
        except (ValueError, KeyError):
            pass
    doc = {"generated": (now or dt.datetime.now(dt.timezone.utc)).strftime("%Y-%m-%dT%H:%M:%SZ"), "count": len(keep),
           "notice": f"Facts only (name, dates, place) gathered from pages that allow crawling; each event links to its source. "
                     f"To have a page left out, write to {CONTACT}.",
           "events": [{k: v for k, v in e.items() if k != "url"} for e in keep]}
    p.write_text(json.dumps(doc, indent=1) + "\n")
    return len(keep)


def read_seeds(path):
    return [l.split("#")[0].strip() for l in Path(path).read_text().splitlines() if l.split("#")[0].strip()]


def main():
    here = Path(__file__).parent
    a = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    a.add_argument("--seeds", default=str(here / "seeds.txt")); a.add_argument("--out", default="events.json")
    a.add_argument("--state", default=str(here / ".finder-state.json"), help="cache of ETags and per-page results")
    a.add_argument("--max-pages", type=int, default=300); a.add_argument("--delay", type=float, default=5.0)
    a.add_argument("--allow-local", action="store_true", help="allow private / loopback hosts (testing only)")
    a = a.parse_args()
    seeds = read_seeds(a.seeds)
    if not seeds: sys.exit("no seeds: add pages whose owners allow this to seeds.txt (see README.md)")
    sp = Path(a.state); st = json.loads(sp.read_text()) if sp.exists() else {}
    c = Crawler(a.delay, a.max_pages, allow_local=a.allow_local, state=st, log=lambda m: print(m, file=sys.stderr))
    ev = c.run(seeds)
    sp.write_text(json.dumps(st))
    print(f"wrote {publish(ev, a.out)} events to {a.out}", file=sys.stderr)


if __name__ == "__main__":
    main()
