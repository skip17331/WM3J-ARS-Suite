# ARS Suite — To do

Kept in the repo so it carries over between sessions. Tick items off (`[x]`) as they are done, and delete them once they have shipped; add new ones at the top of a tier.
Ordered by priority: do the tiers top to bottom. Size is a rough guess: S = under a day, M = a few days, L = a week or more.

## 0. Built — try it and report back
Merged or ready since v1.12.2. The code is written and the tests pass, but nobody has clicked through these in the running app yet. Tick one when you have used it; fix what it finds.
- [ ] **S** J-Hub dashboard as the normal log: full QSO form beside the cluster, the collapsing **Log tools** column (Log mode, RTTY/CW, Heard By, Awards, Skimmer, Translator, Database tools), J-Log gone from the dock. Check widths on a small monitor.
- [ ] **S** One-screen contest page: try a Sweepstakes, a CQ WW and a QSO party. Check the SEND line, mode default, "mults left" tile, rates and the tabs.
- [ ] **S** Contest validation: a bogus section / precedence / zone / callsign is refused; an unrecognised state or county in a QSO party warns and needs a second Enter.
- [ ] **S** Dupes ask permission; Edit / Erase on the contest log rows and the All QSOs window; dupe flags re-decided after an erase.
- [ ] **S** Awards: the dock **Awards** button, the drawer's Customize link, and the **My awards** tab with the 13 Colonies template — check the K2A–K2M + WM3GPO list against the year you operate.
- [ ] **S** Info page: the Contests / DXpeditions / Hamfests tiles, the News tile, **Feeds…**, **Sources…**, **Request a change**, and the links row at the bottom. Check the layout at your window size.
- [ ] **S** License, Code of Conduct and legal notes open from local copies (bottom of the Info page). After the next real update, confirm `~/.j-hub/legal/installed.properties` moved to the new version.
- [ ] **S** Beta banners on J-Digi and J-Bridge (and "(beta)" in their dock tooltips).
- [ ] **S** Nightly release workflow: confirm a night with new code produces a release, and a night with none does not.
- [ ] **S** Read `LEGAL.md` and correct anything about the project's licence, privacy or contact details that is not right.

## 1. On hold — verify what shipped in v1.12.2
No label printers to test with at the moment, so these wait.
- [ ] **S** QSL label printing (beta): try on real hardware — Brother QL-810W, DYMO LabelWriter 550, Zebra ZD421 (ZPL and driver modes). Check size, margins, rotation on narrow strip labels. Fix what it finds before adding more printers.
- [ ] **S** Log browser: click through the Band / Mode filters and Delete + confirmation (only tested through a harness).
- [ ] **S** Confirm the Amateur Radio menu folder shows on Cinnamon after the installer fix.
- [ ] **S** Check the 2026-10-03 9K2IC (70cm DMR) entry in the log — looks like a mistyped date.

## 2. Next — award tracking
Built: real DXCC resolution, per-QSO confirmations, IOTA / SOTA / POTA fields, the awards screen (DXCC matrix, WAS, WAC, grids, IOTA, POTA, SOTA, zones) and your own award sheets (My awards).
- [ ] Try it on the work machine (Windows) — install, open the dock **Awards** button (or Log tools ▸ Awards ▸ Open awards), import a LoTW ADIF to see confirmations.
- [ ] **S** Close the remaining ~155 prefixes the engine's DXCC table lacks (the cty.dat fallback covers them for the awards and spot flags, but scoring and contest multipliers still use the table): mostly Russia (about 38 prefixes), France overseas (TH, TO, TP, TQ, TV, TX), Hawaii AH6 / AH7, Guam AH2, China XS, Botswana 8O, Gibraltar ZG, Greenland XP, Costa Rica TE, Colombia HJ. `CtyDataTest.auditAgainstTheEnginePrefixTable` writes the full list to `target/cty-audit.txt`.
- [ ] **S** Keep `cty.dat` current: the copy in the app is from release 3634 (2 Nov 2026); the files change a few times a month. Needs a stable download address (the zip name carries the release number) or reading it off the site's front page; until then it is refreshed with each app release.
- [ ] **M** Refine CQ / ITU zones by grid for the multi-zone countries (US 3-5, Canada 1-5, Russia, China, Australia, Brazil...). Zones now come from the callsign through cty.dat, which gives the entity default and a few per-call overrides, so a W6 station reads zone 5 where it is really 3. The J-Map zone outlines (`cq-zones.json`) could turn a grid square into a zone.

- [ ] **S** Check **Get confirmations** against your real LoTW and eQSL accounts (tests use sample files only). LoTW uses the documented query; the eQSL inbox download is undocumented and follows what other loggers do, so it is the one most likely to need a fix. A password with a `#` is known to break eQSL downloads.
- [ ] **S** Confirmation sync follow-ups: also add confirmed QSOs missing from the log (today only matched ones are ticked and the rest are counted); show how many were newly confirmed; Club Log and QRZ have confirmation downloads too.
- [ ] **S** Reference lookups: the POTA lookup uses an unofficial interface and may change; the IOTA list is the public download from iota-world.org, kept a week; check their terms, and add a lookup button if typing and tabbing is not enough.

## 2b. Next — contest page (phases 2 and 3)
Phase 1 is built: one generic page driven by the 86 contest definitions in `j-log-engine/.../plugins/`; contest mode is entered from Log tools ▸ Log mode.
- [ ] **M** Phase 2 — per-contest layout: an optional `layout` block in the contest JSON (which panels, order, first fields, macro set, extra score tiles, pick-lists such as the county list), plus a user override file `~/.j-log/contests/<id>.json` layered over the built-in one. Tailor the contests actually run first (ask which).
- [ ] **M** New-multiplier detection (the NEW MULT banner) and a spot filter that shows only spots that would be a new multiplier or new on this band. Builds on the band map and Super Check Partial below.
- [ ] **S** Contest start / end times in the definitions, and a timer (elapsed, remaining, off-time).
- [ ] **M** Phase 3 — special panels: Sweepstakes (precedence / check / section flow), Field Day (class + section, bonus points), WAE **QTC entry** (not implemented), QSO-party in-state vs out-of-state exchange and counties.
- [ ] **S** QSO-party "mults left" counts the counties in the multiplier list; for an operator inside the state it should be states and provinces instead.
- [ ] **S** Mixed-mode contests (QSO parties, Field Day, VHF, IARU, EU HF...) open in the rig's mode, else SSB — confirm that default, or make it a setting.
- [ ] **S** Contest macro keyer on the contest page: confirm CW / voice F-keys work there as they did on the old page.
- [ ] **S** Validation: sent-side values are not checked; add a callsign-exists check only if wanted (shape only today). Add state / province / county lists per QSO party so that warning becomes a hard error where the list is definitive.
- [ ] **S** Contest QSO edit: changing the callsign of an old QSO re-checks dupes for the whole log; add a test for the re-flag logic (needs a test database).

## 2c. Next — gaps found in the competitor survey
From comparing the suite with N1MM+, DXLab, Log4OM, Wavelog, GridTracker and the portable loggers. We are working on these.
- [ ] **M** **Super Check Partial** and a graphical **band map**. Super Check Partial: type part of a call and see the matching known contest calls (the `MASTER.SCP` list; check where it may be fetched from and its terms, and keep it updated like the other feeds), plus calls from your own log. Band map: spots laid out along the band by frequency, aged and colour-coded (worked / needed / new multiplier), click to tune, on the contest page and the dashboard.
- [ ] **L** **Phone-friendly web client** for the hub, for portable (POTA / SOTA) logging and shack monitoring without a native app: a responsive page served by the hub (the WebSocket link already exists) with QSO entry, spots, rig and rotor status; keep working offline in the field and sync when back on the network. Needs a login or token on the LAN and a decision on how it is reached from outside.
- [ ] **M** **Our own propagation predictions** (what DXLab PropView and Log4OM get from VOACAP). Decision: compute them ourselves in Java from the space-weather data the app already pulls (solar flux / sunspot number, A and K index) plus the path (great circle, distance, time, season, grid). Do it in stages: first a simple MUF / band-open estimate for the path between the station and a spot or entity, shown as band-opening graphs; then refine toward the ITU-R P.533 method. Note that a VOACAP-grade answer also needs static ionospheric coefficient maps (foF2 / M3000), which are data files to ship, not something the live feeds provide; check their licence. Say plainly in the app that it is an estimate.
- [ ] **S** **N1MM-compatible UDP broadcast** of contacts (and lookups, radio info, scoring) so GridTracker, Log4OM and other tools can follow our log live. Host / port in Setup; keep the message format identical to N1MM+'s.
- [ ] **L** **Network mode**: multi-operator and multi-computer contest logging over the LAN. A shared contest log with stations and operators, band / mode conflict checks, per-operator stats, sked and chat; also club-station use. Build on the hub (decide whether the hub is the server). Reference points: N1MM+ networking and Wavelog's multi-operator logging.
- [ ] **M** **Emergency communications** — PARKED, do not start. Bring GoKit (APRS, Winlink, AX.25 packet; alpha, separate repo) into the suite — decide whether to embed it or link / launch it — with shared station identity and settings, and ICS forms.

## 3. Then — features operators will notice
- [ ] **M** J-Map: point-to-point tracks (ham to ham) — draw the path between two stations on the map.
- [ ] **M** Beacon mode: look at it, get it working, and set up an auto-QSY mode.
- [ ] **L** Info tab, what is left. The Upcoming tiles and the News tile are built; still to do:
  - [ ] **S** **Send the permission email to WA7BNM** (contest calendar). Their terms bar automated access without written permission; until it is granted the app ships no built-in contest feed (the operator pastes their own .ics link under Info ▸ Sources). When it arrives, set the default in `UpcomingEvents.sources()` and keep the "WA7BNM Contest Calendar" label with a link back. Consider asking ARRL too about using their feed's headlines.
  - [ ] **S** Hamfests: no usable public feed found yet (ARRL, HamCalendar) — same "paste a link" path for now.
  - [ ] **M** "Active now" panel: POTA / SOTA / WWFF activators on air, DXpeditions on the air now, RBN "who hears me" summary, award hints ("ZL9 is on 17m CW — you still need it"; needs tier 2 first).
  - [ ] **M** News sources the catalog lacks: CQ (the newsroom feed stopped in 2024), IARU / band-plan changes (their feed is stale), FCC rule changes, WSJT-X / Hamlib release feeds, ham podcasts and video.
  - [ ] **S** AMSAT's feed takes minutes to download from some networks, so it fills in on the second refresh; look at a lighter feed or a longer timeout.
  - [ ] **M** More calendars: special event stations, net schedules, satellite news; filter by distance from your grid.
  - [ ] **M** Your stats: QSOs this month, new entities, QSL / LoTW confirmations, upload errors.
  - [ ] **M** Reference: greyline / sunrise-sunset timetable and best bands by time of day, callsign of the day + quick lookup, licence renewal reminder, tip of the day (can surface new J-Learn chapters).
- [ ] **S** QSL labels: reprint for a selected QSO, and print from contest mode (edits do not print today).
- [ ] **S** Retire the old standalone J-Log page (`JLogCockpit.build()`): nothing opens it any more, but the dashboard embeds its form. Move the form into its own class and delete the page.
- [ ] **S** Log browser: export the filtered view; show the contest-log databases too (it shows the normal log only).

## 4. Later — housekeeping and research
- [ ] **S** `USER_GUIDE.md` §3.3 still describes the old long J-Hub Data page; rewrite for the Setup screen and its menu.
- [ ] **S** `USER_GUIDE.md`: the dashboard now holds normal logging (no J-Log in the dock), the Log tools column, the one-screen contest page (entry, SEND line, validation, dupes, edit / erase), My awards, the Info page, and the beta status of J-Digi and J-Bridge.
- [ ] **S** `ars-fx/CLAUDE.md`: the surfaces list still needs the contest page and the classes behind it (`SentExchange`, `ContestValidation`), `CustomAwards`, `LegalDocs` / `LegalView`, and that J-Log normal mode now lives on the dashboard.
- [ ] **S** QSL labels: more label stock / printer models if requested.
- [ ] **M** Maps: look at the J-Map options and see whether something better exists (tiles, projections, performance). Do this before the 3D globe.
- [ ] **L** 3D globe as a map option — find out whether it is possible in JavaFX (and what it would cost). Research only until the map review above is done.

## 5. Decided against
- A WSJT-X activity layer in J-Bridge (call roster, wanted-grid highlights, PSK Reporter map, like GridTracker) — GridTracker already does it, so we leave it to them.
