#!/usr/bin/env python3
"""Collect upcoming amateur radio hamfests from the ARRL Hamfests & Conventions calendar.

Writes JSON, CSV or an iCalendar (.ics) file. The .ics is what the app's Info tab reads:
J-Hub > Info > Sources > Hamfests takes an http(s) address, so either host the file somewhere
or run `--serve` and give the app http://127.0.0.1:8765/hamfests.ics.

Standard library only. Polite by design: ARRL's robots.txt asks for a 5 second crawl delay, which
this honours; results are cached for 24 hours so reruns do not hit the site; the User-Agent names
the tool. Check ARRL's terms of use before publishing the output anywhere.

Examples:
  hamfest_scraper.py                              # table of everything upcoming
  hamfest_scraper.py --state MD --state VA --days 120
  hamfest_scraper.py --format ics -o hamfests.ics
  hamfest_scraper.py --serve 8765 --state MD      # refreshes daily, serves /hamfests.ics
"""
import argparse, csv, datetime as dt, hashlib, html, http.cookiejar, http.server, io, json, re
import sys, threading, time, urllib.parse, urllib.request
from pathlib import Path

BASE = "https://www.arrl.org"
UA = "ARS-Suite-hamfest-scraper/1.0 (+https://github.com/skip17331/WM3J-ARS-Suite)"
DELAY = 5.0                       # seconds between requests: ARRL robots.txt Crawl-delay
MAX_PAGES = 40                    # safety stop; the calendar is ~14 pages of 8
CACHE = Path.home() / ".cache" / "ars-hamfests.json"
CACHE_AGE = 24 * 3600


class Fetcher:
    def __init__(self, delay=DELAY):
        self.delay, self.last = delay, 0.0
        self.op = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
        self.op.addheaders = [("User-Agent", UA)]

    def _wait(self):
        gap = self.delay - (time.time() - self.last)
        if gap > 0:
            time.sleep(gap)
        self.last = time.time()

    def get(self, path, data=None):
        self._wait()
        body = urllib.parse.urlencode(data).encode() if data is not None else None
        with self.op.open(urllib.request.Request(BASE + path, data=body), timeout=40) as r:
            return r.read().decode("utf-8", "replace")


def form_fields(page):
    """Hidden fields (CSRF token) of the hamfest search form, with every search box left empty."""
    form = re.search(r'<form[^>]*action="/hamfests/search".*?</form>', page, re.S)
    if not form:
        raise RuntimeError("search form not found - ARRL changed the page")
    d = {}
    for tag in re.findall(r"<(?:input|select)\b[^>]*>", form.group(0)):
        n = re.search(r'name="([^"]+)"', tag)
        if n and n.group(1).startswith("data["):
            v = re.search(r'value="([^"]*)"', tag)
            hidden = 'type="hidden"' in tag
            d[html.unescape(n.group(1))] = html.unescape(v.group(1)) if hidden and v else ""
    d["_method"] = "POST"
    return d


ITEM = re.compile(r"<li\b[^>]*>(?:(?!<li\b).)*?</li>", re.S)


def clean(s):
    return re.sub(r"\s+", " ", html.unescape(re.sub(r"<[^>]+>", " ", s))).strip()


def parse_page(page):
    out = []
    for li in ITEM.findall(page):
        m = re.search(r"<span>\s*(\d\d)/(\d\d)/(\d{4})\s*-?\s*</span>\s*(?:(\d\d)/(\d\d)/(\d{4})\s*<br\s*/?>\s*)?<a href=\"([^\"]+)\"[^>]*>(.*?)</a>", li, re.S)
        if not m:
            continue
        mo, da, yr, emo, eda, eyr, href, title = m.groups()

        def field(label):
            f = re.search(r"<strong>\s*%s:\s*</strong>(.*?)(?:<br|<a |</p>)" % label, li, re.S)
            return clean(f.group(1)) if f else ""

        site = re.search(r"Website:\s*</strong>\s*<a href=\"([^\"]+)\"", li)
        out.append({
            "date": f"{yr}-{mo}-{da}", "end": f"{eyr}-{emo}-{eda}" if eyr else f"{yr}-{mo}-{da}", "title": clean(title), "location": field("Location"),
            "type": field("Type"), "sponsor": field("Sponsor"),
            "website": html.unescape(site.group(1)) if site else "", "url": BASE + html.unescape(href),
        })
    return out


def scrape(delay=DELAY, log=lambda m: None):
    f = Fetcher(delay)
    form = form_fields(f.get("/hamfests"))
    page = f.get("/hamfests/search", form)
    events = parse_page(page)
    total = re.search(r"Results \d+ to \d+ of (\d+)", page)
    total = int(total.group(1)) if total else len(events)
    log(f"ARRL lists {total} events")
    n = 2
    while len(events) < total and n <= MAX_PAGES:
        got = parse_page(f.get(f"/hamfests/search/page:{n}/model:Event"))
        if not got:
            break
        events += got
        log(f"page {n}: {len(events)}/{total}")
        n += 1
    seen, uniq = set(), []
    for e in events:
        if e["url"] not in seen:
            seen.add(e["url"])
            uniq.append(e)
    return uniq


def load(refresh=False, delay=DELAY, log=lambda m: None):
    if not refresh and CACHE.exists() and time.time() - CACHE.stat().st_mtime < CACHE_AGE:
        return json.loads(CACHE.read_text())
    ev = scrape(delay, log)
    if ev:   # never overwrite a good cache with an empty scrape
        CACHE.parent.mkdir(parents=True, exist_ok=True)
        CACHE.write_text(json.dumps(ev))
    return ev


def select(events, states, days, today=None):
    today = today or dt.date.today()
    last = today + dt.timedelta(days=days) if days else None
    states = {s.upper() for s in states}
    out = []
    for e in events:
        d = dt.date.fromisoformat(e["date"])
        st = re.search(r",\s*([A-Z]{2})\s*$", e["location"])
        if d < today or (last and d > last):
            continue
        if states and (not st or st.group(1) not in states):
            continue
        out.append(e)
    return sorted(out, key=lambda e: (e["date"], e["title"]))


def ics_text(s):
    return s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n")


def to_ics(events):
    L = ["BEGIN:VCALENDAR", "VERSION:2.0", "PRODID:-//ARS Suite//hamfest scraper//EN",
         "X-WR-CALNAME:Hamfests (ARRL calendar)"]
    stamp = dt.datetime.now(dt.timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    for e in events:
        d = dt.date.fromisoformat(e["date"])
        desc = "\n".join(x for x in (e["type"], "Sponsor: " + e["sponsor"] if e["sponsor"] else "",
                                     e["website"], e["url"]) if x)
        L += ["BEGIN:VEVENT", "UID:" + hashlib.sha1(e["url"].encode()).hexdigest()[:20] + "@arrl-hamfests",
              "DTSTAMP:" + stamp, "DTSTART;VALUE=DATE:" + d.strftime("%Y%m%d"),
              "DTEND;VALUE=DATE:" + (max(d, dt.date.fromisoformat(e.get("end", e["date"]))) + dt.timedelta(days=1)).strftime("%Y%m%d"),
              "SUMMARY:" + ics_text(e["title"]), "LOCATION:" + ics_text(e["location"]),
              "DESCRIPTION:" + ics_text(desc), "URL:" + e["url"], "END:VEVENT"]
    L.append("END:VCALENDAR")
    return "\r\n".join(L) + "\r\n"


def render(events, fmt):
    if fmt == "json":
        return json.dumps(events, indent=2) + "\n"
    if fmt == "ics":
        return to_ics(events)
    if fmt == "csv":
        b = io.StringIO()
        w = csv.DictWriter(b, ["date", "end", "title", "location", "type", "sponsor", "website", "url"])
        w.writeheader()
        w.writerows(events)
        return b.getvalue()
    return "".join(f"{e['date']}  {e['location']:<24.24} {e['title']}\n" for e in events) or "No hamfests found.\n"


def serve(port, args):
    state = {"ics": b"", "ok": False}

    def refresh():
        while True:
            try:
                ev = select(load(True, args.delay), args.state, args.days)
                state["ics"], state["ok"] = to_ics(ev).encode(), True
                print(f"refreshed: {len(ev)} hamfests", file=sys.stderr)
            except Exception as ex:
                print("refresh failed:", ex, file=sys.stderr)
            time.sleep(CACHE_AGE)

    class H(http.server.BaseHTTPRequestHandler):
        def do_GET(self):
            if self.path.split("?")[0] != "/hamfests.ics" or not state["ok"]:
                self.send_error(404 if state["ok"] else 503)
                return
            self.send_response(200)
            self.send_header("Content-Type", "text/calendar; charset=utf-8")
            self.send_header("Content-Length", str(len(state["ics"])))
            self.end_headers()
            self.wfile.write(state["ics"])

        def log_message(self, *a):
            pass

    threading.Thread(target=refresh, daemon=True).start()
    print(f"serving http://127.0.0.1:{port}/hamfests.ics (first scrape takes about a minute)", file=sys.stderr)
    http.server.ThreadingHTTPServer(("127.0.0.1", port), H).serve_forever()


def main():
    p = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    p.add_argument("--state", action="append", default=[], metavar="ST", help="two-letter state/province, repeatable")
    p.add_argument("--days", type=int, default=0, help="only events within this many days (default: all)")
    p.add_argument("--format", choices=["table", "json", "csv", "ics"], default="table")
    p.add_argument("-o", "--output", help="write to a file instead of stdout")
    p.add_argument("--refresh", action="store_true", help="ignore the 24 hour cache")
    p.add_argument("--delay", type=float, default=DELAY, help="seconds between requests (default 5, ARRL's request)")
    p.add_argument("--serve", type=int, metavar="PORT", help="serve /hamfests.ics on localhost, refreshed daily")
    a = p.parse_args()
    if a.serve:
        serve(a.serve, a)
    ev = select(load(a.refresh, a.delay, lambda m: print(m, file=sys.stderr)), a.state, a.days)
    text = render(ev, a.format)
    if a.output:
        Path(a.output).write_text(text)
        print(f"wrote {len(ev)} hamfests to {a.output}", file=sys.stderr)
    else:
        sys.stdout.write(text)


if __name__ == "__main__":
    main()
