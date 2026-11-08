"""Offline tests: fixtures and a throwaway local web server. Run: python3 -m unittest test_finder -v  (from this folder)"""
import datetime as dt, http.server, json, tempfile, threading, unittest
from pathlib import Path
import finder as f

TODAY = dt.date(2026, 11, 8)
JSONLD = '''<html><head><script type="application/ld+json">{"@context":"https://schema.org","@type":"Event","name":"Frostfest Hamfest 2027",
"startDate":"2027-02-06T08:00","endDate":"2027-02-06","description":"Swap tables","url":"https://club.example/frostfest",
"location":{"@type":"Place","address":{"addressLocality":"Gloucester","addressRegion":"VA"}}}</script></head><body>x</body></html>'''
TEXT = '''<html><head><title>Club events 2027</title></head><body><h2>Upcoming</h2><ul>
<li>March 13-14 - Spring Hamfest (Clinton, NJ)</li><li>Board meeting March 2</li>
<li>Sat Nov 14: Fall Tailgate Swap Meet, Marana, AZ</li><li>August 1 - Old Hamfest 2019 (Dover, DE)</li></ul>
<h3>Winter Hamfest</h3><p>January 9, 2027</p><p>Harrisburg, PA</p></body></html>'''
ICS = "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\nSUMMARY:Vienna Winterfest Hamfest\r\nDTSTART;VALUE=DATE:20270314\r\nDTEND;VALUE=DATE:20270315\r\nLOCATION:Vienna\\, VA\r\nEND:VEVENT\r\nBEGIN:VEVENT\r\nSUMMARY:Club picnic\r\nDTSTART;VALUE=DATE:20270320\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n"


class Extract(unittest.TestCase):
    def test_jsonld(self):
        e = f.extract(JSONLD, "text/html", "https://club.example/", TODAY)
        self.assertEqual(1, len(e)); self.assertEqual(("2027-02-06", "VA", "Gloucester"), (e[0]["start"], e[0]["state"], e[0]["city"]))

    def test_text_finds_hamfests_only(self):
        e = {x["title"]: x for x in f.extract(TEXT, "text/html", "https://club.example/", TODAY)}
        self.assertEqual("2027-03-13", e["Spring Hamfest"]["start"]); self.assertEqual("2027-03-14", e["Spring Hamfest"]["end"])
        self.assertEqual("NJ", e["Spring Hamfest"]["state"])
        self.assertTrue(any(t.startswith("Fall Tailgate") for t in e))
        self.assertFalse(any("Board" in t for t in e), "a meeting is not a hamfest")
        self.assertFalse(any("Old Hamfest" in t for t in e), "past events are dropped")
        w = e["Winter Hamfest"]; self.assertEqual(("2027-01-09", "PA"), (w["start"], w["state"]))   # heading, date, place on separate lines

    def test_ics_allday_end_is_inclusive_and_keyword_gated(self):
        e = f.extract(ICS, "text/calendar", "https://club.example/c.ics", TODAY)
        self.assertEqual(1, len(e)); self.assertEqual(("2027-03-14", "2027-03-14", "VA"), (e[0]["start"], e[0]["end"], e[0]["state"]))

    def test_year_rolls_forward_without_a_year(self):
        d = f.find_dates("Hamfest Jan 9", TODAY)[0][0]; self.assertEqual(dt.date(2027, 1, 9), d)

    def test_cross_month_range(self):
        s, e, _ = f.find_dates("Aug 29 - Sep 1, 2027", TODAY)[0]; self.assertEqual((dt.date(2027, 8, 29), dt.date(2027, 9, 1)), (s, e))


class Merge(unittest.TestCase):
    def ev(self, title, url, conf=0.6, state="MD"):
        return f.mk(title, dt.date(2027, 3, 13), dt.date(2027, 3, 13), "", state, url, conf)

    def test_duplicates_merge_and_confidence_rises(self):
        m = f.merge([self.ev("Annual Spring Hamfest", "https://a/"), self.ev("Spring Hamfest 2027", "https://b/"), self.ev("Other Swapfest", "https://c/")])
        self.assertEqual(2, len(m)); sp = [x for x in m if "Spring" in x["title"]][0]
        self.assertEqual(2, len(sp["sources"])); self.assertEqual(0.7, sp["confidence"])

    def test_publish_hides_low_confidence_and_refuses_a_collapse(self):
        with tempfile.TemporaryDirectory() as d:
            out = Path(d) / "events.json"
            evs = [f.mk(f"Fest {i} Hamfest", dt.date(2027, 3, 1) + dt.timedelta(days=i), dt.date(2027, 3, 1) + dt.timedelta(days=i), "", "MD", "https://a/", 0.6) for i in range(12)]
            evs.append(f.mk("Maybe Hamfest", dt.date(2027, 5, 1), dt.date(2027, 5, 1), "", "", "https://a/", 0.45))
            self.assertEqual(12, f.publish(evs, out))
            doc = json.loads(out.read_text()); self.assertNotIn("url", doc["events"][0]); self.assertIn("sources", doc["events"][0])
            with self.assertRaises(SystemExit): f.publish(evs[:3], out)


class Site(http.server.BaseHTTPRequestHandler):
    hits = []
    pages = {"/robots.txt": ("text/plain", "User-agent: *\nDisallow: /private\n"),
             "/": ("text/html", '<a href="/events">Events calendar</a> <a href="/private/hamfests">Hamfest list</a> <a href="/about">About us</a>'),
             "/events": ("text/html", TEXT), "/private/hamfests": ("text/html", TEXT), "/about": ("text/html", "<p>Hamfest March 20, 2027 Dover, DE</p>")}

    def do_GET(self):
        Site.hits.append(self.path); ct, body = self.pages.get(self.path, (None, None))
        if ct is None: self.send_error(404); return
        if self.headers.get("If-None-Match") == '"v1"' and self.path != "/robots.txt":
            self.send_response(304); self.end_headers(); return
        self.send_response(200); self.send_header("Content-Type", ct); self.send_header("ETag", '"v1"')
        self.end_headers(); self.wfile.write(body.encode())

    def log_message(self, *a): pass


class Crawl(unittest.TestCase):
    def test_follows_hinted_links_obeys_robots_and_caches(self):
        srv = http.server.ThreadingHTTPServer(("127.0.0.1", 0), Site); threading.Thread(target=srv.serve_forever, daemon=True).start()
        base = f"http://127.0.0.1:{srv.server_port}"; Site.hits.clear(); state = {}
        try:
            c = f.Crawler(delay=0, state=state, allow_local=True); c.today = TODAY
            ev = c.run([base + "/"])
            self.assertIn("/events", Site.hits); self.assertNotIn("/private/hamfests", Site.hits, "robots.txt disallows it")
            self.assertTrue(any(e["title"] == "Spring Hamfest" for e in ev))
            Site.hits.clear(); c2 = f.Crawler(delay=0, state=state, allow_local=True); c2.today = TODAY
            ev2 = c2.run([base + "/"]); self.assertTrue(any(e["title"] == "Spring Hamfest" for e in ev2), "304: events come from the saved results")
            c3 = f.Crawler(delay=0); c3.today = TODAY
            self.assertEqual([], c3.run([base + "/"]), "loopback hosts are refused unless allowed")
        finally:
            srv.shutdown()


if __name__ == "__main__":
    unittest.main()
