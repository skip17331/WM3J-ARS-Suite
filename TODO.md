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
- [ ] **M** Audit the DXCC prefix table (`j-log-engine/.../dxcc-prefixes.json`) against the full ARRL list: about 270 prefixes were added, but whole blocks are still missing (much of Africa, Asia, Oceania, the Caribbean), and deleted entities are not tracked. Calls with no entity are counted in the Awards header.
- [ ] **M** Download confirmations straight from LoTW / eQSL (their APIs) instead of importing a file by hand.
- [ ] **S** Suggest the IOTA reference from the callsign (IOTA's public group list) and look up POTA parks / SOTA summits when typing a reference.
- [ ] **M** Work out CQ and ITU zones from the callsign / grid so WAZ does not depend on an ADIF import.
- [ ] **M** DXCC "needed" hints: flag cluster spots that are a new entity, band or mode (feeds the Info tab "Active now" panel).
- [ ] **S** Endorsement and award-level reporting: DXCC Challenge (entities x bands), 5-Band DXCC / WAS, mixed-mode; VUCC per band (50 MHz and up only).
- [ ] **S** Show IOTA / SOTA / POTA / confirmation boxes on the J-Map QSO card (the dashboard has the full form; the map card does not).
- [ ] **S** My awards: show custom awards in the dashboard Awards drawer; per-band tracking within one award; a printable sheet (CSV export only today).

## 2b. Next — contest page (phases 2 and 3)
Phase 1 is built: one generic page driven by the 86 contest definitions in `j-log-engine/.../plugins/`; contest mode is entered from Log tools ▸ Log mode.
- [ ] **M** Phase 2 — per-contest layout: an optional `layout` block in the contest JSON (which panels, order, first fields, macro set, extra score tiles, pick-lists such as the county list), plus a user override file `~/.j-log/contests/<id>.json` layered over the built-in one. Tailor the contests actually run first (ask which).
- [ ] **M** Needs: new-multiplier detection (the NEW MULT banner) and a bandmap that shows only spots that are a new multiplier or new on this band; "check partial" from the log history.
- [ ] **S** Contest start / end times in the definitions, and a timer (elapsed, remaining, off-time).
- [ ] **M** Phase 3 — special panels: Sweepstakes (precedence / check / section flow), Field Day (class + section, bonus points), WAE **QTC entry** (not implemented), QSO-party in-state vs out-of-state exchange and counties.
- [ ] **S** QSO-party "mults left" counts the counties in the multiplier list; for an operator inside the state it should be states and provinces instead.
- [ ] **S** Mixed-mode contests (QSO parties, Field Day, VHF, IARU, EU HF...) open in the rig's mode, else SSB — confirm that default, or make it a setting.
- [ ] **S** Contest macro keyer on the contest page: confirm CW / voice F-keys work there as they did on the old page.
- [ ] **S** Validation: sent-side values are not checked; add a callsign-exists check only if wanted (shape only today). Add state / province / county lists per QSO party so that warning becomes a hard error where the list is definitive.
- [ ] **S** Contest QSO edit: changing the callsign of an old QSO re-checks dupes for the whole log; add a test for the re-flag logic (needs a test database).

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
