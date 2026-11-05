# ARS Suite — To do

Kept in the repo so it carries over between sessions. Tick items off (`[x]`) as they are done; add new ones at the top of a tier.
Ordered by priority: do the tiers top to bottom. Size is a rough guess: S = under a day, M = a few days, L = a week or more.

## 1. On hold — verify what shipped in v1.12.2
No label printers to test with at the moment, so these wait.
- [ ] **S** QSL label printing (beta): try on real hardware — Brother QL-810W, DYMO LabelWriter 550, Zebra ZD421 (ZPL and driver modes). Check size, margins, rotation on narrow strip labels. Fix what it finds before adding more printers.
- [ ] **S** Log browser: click through the Band / Mode filters and Delete + confirmation (only tested through a harness).
- [ ] **S** Confirm the Amateur Radio menu folder shows on Cinnamon after the installer fix.
- [ ] **S** Check the 2026-10-03 9K2IC (70cm DMR) entry in the log — looks like a mistyped date.

## 2. Next — award tracking (merged to main, PR #85; version not yet bumped)
Built: real DXCC resolution, per-QSO confirmations, IOTA / SOTA / POTA fields, and the awards screen (DXCC matrix, WAS, WAC, grids, IOTA, POTA, SOTA, zones).
- [x] Review and merge the PR (#85)
- [ ] Try it on the work machine (Windows) — install, open J-Log ▸ Awards, import a LoTW ADIF to see confirmations.
- [ ] **M** Audit the DXCC prefix table (`j-log-engine/.../dxcc-prefixes.json`) against the full ARRL list: about 270 prefixes were added, but whole blocks are still missing (much of Africa, Asia, Oceania, the Caribbean), and deleted entities are not tracked. Calls with no entity are counted in the Awards header.
- [ ] **M** Download confirmations straight from LoTW / eQSL (their APIs) instead of importing a file by hand.
- [ ] **S** Suggest the IOTA reference from the callsign (IOTA's public group list) and look up POTA parks / SOTA summits when typing a reference.
- [ ] **M** Work out CQ and ITU zones from the callsign / grid so WAZ does not depend on an ADIF import.
- [ ] **M** DXCC "needed" hints: flag cluster spots that are a new entity, band or mode (feeds the Info tab "Active now" panel).
- [ ] **S** Endorsement and award-level reporting: DXCC Challenge (entities x bands), 5-Band DXCC / WAS, mixed-mode; VUCC per band (50 MHz and up only).
- [ ] **S** Show IOTA / SOTA / POTA / confirmation boxes in J-Log-Quick and on the J-Map QSO card (only the full J-Log form has them).

## 3. Then — features operators will notice
- [ ] **M** J-Map: point-to-point tracks (ham to ham) — draw the path between two stations on the map.
- [ ] **M** Beacon mode: look at it, get it working, and set up an auto-QSY mode.
- [ ] **L** Contest calendar and other info — a new **Info** tab. Build first an "Upcoming" page (contests, DXpeditions, hamfests; one date-list view + a few feed fetchers) and an "Active now" panel (POTA / SOTA / DXpeditions); headlines (RSS) after that. Award hints ("ZL9 is on 17m CW — you still need it") need tier 2 first.
  - [ ] Calendars: contests (WA7BNM), hamfests / conventions (ARRL, HamCalendar — check for a usable feed), **DXpeditions** (NG3K, DX-World: entity, dates, bands, modes, QSL route), special event stations, net schedules, satellite news. Filter by distance from your grid.
  - [ ] Operating activity: POTA / SOTA / WWFF activators on air, RBN "who hears me" summary, award hints.
  - [ ] News feeds: ARRL / RSGB / CQ headlines, ham podcasts and video, rule and band-plan changes (FCC, IARU), software updates for WSJT-X / Hamlib.
  - [ ] Your stats: QSOs this month, new entities, QSL / LoTW confirmations, upload errors.
  - [ ] Reference: greyline / sunrise-sunset timetable and best bands by time of day, callsign of the day + quick lookup, licence renewal reminder, tip of the day (can surface new J-Learn chapters).
- [ ] **S** QSL labels: reprint for a selected QSO, and print from contest mode (edits do not print today).
- [ ] **S** Log browser: export the filtered view; show the contest-log databases too (it shows the normal log only).

## 4. Later — housekeeping and research
- [ ] **S** `USER_GUIDE.md` §3.3 still describes the old long J-Hub Data page; rewrite for the Setup screen and its menu.
- [ ] **S** QSL labels: more label stock / printer models if requested.
- [ ] **M** Maps: look at the J-Map options and see whether something better exists (tiles, projections, performance). Do this before the 3D globe.
- [ ] **L** 3D globe as a map option — find out whether it is possible in JavaFX (and what it would cost). Research only until the map review above is done.
