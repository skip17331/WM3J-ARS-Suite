# Hamfest scraper

Collects upcoming hamfests from the [ARRL Hamfests & Conventions calendar](https://www.arrl.org/hamfests)
and writes a table, JSON, CSV or an iCalendar file. Standard library only (Python 3.8+).

```
python3 hamfest_scraper.py                                  # table of everything upcoming
python3 hamfest_scraper.py --state MD --state VA --days 120
python3 hamfest_scraper.py --format ics -o hamfests.ics
python3 hamfest_scraper.py --serve 8765 --state MD          # serves http://127.0.0.1:8765/hamfests.ics
```

**Use in the app:** J-Hub > Info > Sources > Hamfests takes an http(s) address (never a file path),
so run `--serve` and paste `http://127.0.0.1:8765/hamfests.ics`, or host the `.ics` yourself.

**Do not run this against ARRL, and never publish its output, without written permission.** ARRL's Terms of Use
(https://www.arrl.org/privacy-policy, read 2026-11-08) say you must not "use any robot, spider, or other automatic device,
process, or means to access the Website for any purpose, including monitoring or copying", nor republish its material, and
allow personal non-commercial use only. The tool is kept as the starting point for sources that do allow it. Ask ARRL
(and read each new site's terms and robots.txt) before using it for real.

**Manners:** one request every 5 seconds (ARRL's robots.txt `Crawl-delay`), so a full pull of ~14 pages takes
about a minute; the result is cached in `~/.cache/ars-hamfests.json` for 24 hours (`--refresh` to force).
Only ARRL-listed events are covered, and the list gives a start date (and an end date for multi-day events)
but no times. If ARRL changes the page, the tool stops with
"search form not found" or returns nothing rather than writing a bad file.
