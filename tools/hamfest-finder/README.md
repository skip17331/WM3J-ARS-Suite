# Hamfest finder

Crawls club and event pages politely and compiles its own list of upcoming hamfests into `events.json`: facts only
(name, dates, city and state, a link to each source page). No page text is stored or republished. Standard library only.

```
python3 finder.py                      # crawl seeds.txt, write events.json (a full run takes many minutes: 5 s per host)
python3 finder.py --max-pages 20 --out test.json
python3 -m unittest test_finder -v     # offline tests (fixtures + a local test server)
```

## How it works
1. **Seeds** (`seeds.txt`): the pages it starts from. Following links from a seed reaches hamfest, calendar and event
   pages (to other sites one hop, then only within the same site, two hops deep at most).
2. **Polite fetching:** robots.txt and its `Crawl-delay` obeyed (a 4xx on robots.txt means none; a 5xx or timeout means
   skip the site for this run), `noindex` / `nofollow` obeyed, one request per host every 5 s, at most 15 pages per
   host and 300 per run, conditional requests (ETag / Last-Modified) so unchanged pages cost one tiny request, private
   and loopback addresses refused, a User-Agent that names the tool and gives a contact address.
3. **Extraction**, best first: schema.org `Event` data, then `.ics` files, then dated text beside hamfest words
   (hamfest, swapfest, tailgate, convention, ...). Meetings and past years are ignored. Confidence: 0.9 structured data,
   0.6 text with a place, 0.45 text without; a second page announcing the same event adds 0.1.
4. **Merge:** same start date, same state and a similar name are one event. Only confidence 0.5 or more is published.
5. **Safety:** `events.json` is not replaced if the new list has less than half the old one (a broken crawl).

## What may be a seed
A seed is a human decision. List a page only if its owner's terms allow it or the owner said yes; note the date and who
agreed in `seeds.txt`. Pages reached by following links are gated by robots.txt only, which is what search engines do;
the output is facts and links, and the `notice` in the file says how to ask for a page to be left out. Checked and
**not allowed**: arrl.org (terms bar robots and republishing), contestcalendar.com (terms bar copying), QRZ forum posts
(members' own text).

## Publishing (GitHub Pages)
`hamfest-finder.workflow.yml` is a ready weekly GitHub Action. It belongs in a **separate repo** with Pages switched on,
so a bot never commits to this one: copy `finder.py`, `seeds.txt` and the workflow there. The app would download
`https://<user>.github.io/<repo>/events.json` once every 12 hours (not built yet).

## Limits
Heuristics miss events and mis-read some lines, so expect to tune them on real pages. A page that lists a year-less
date near "Hamfest" gets the next occurrence of that date. Needs seeds to find anything.
