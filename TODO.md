# ARS Suite — To do

Kept in the repo so it carries over between sessions. Tick items off (`[x]`) as they are done, and delete them once they have shipped; add new ones at the top of a tier.
Ordered by priority: do the tiers top to bottom. Size is a rough guess: S = under a day, M = a few days, L = a week or more.

## 1. On hold — verify what shipped in v1.12.2
No label printers to test with at the moment, so these wait.
- [ ] **S** QSL label printing (beta): try on real hardware — Brother QL-810W, DYMO LabelWriter 550, Zebra ZD421 (ZPL and driver modes). Check size, margins, rotation on narrow strip labels. Fix what it finds before adding more printers.
- [ ] **S** Log browser: click through the Band / Mode filters and Delete + confirmation (only tested through a harness).
- [ ] **S** Confirm the Amateur Radio menu folder shows on Cinnamon after the installer fix.
- [ ] **S** Check the 2026-10-03 9K2IC (70cm DMR) entry in the log — looks like a mistyped date.

## 2. Next — award tracking
Built: real DXCC resolution, per-QSO confirmations, IOTA / SOTA / POTA fields, the awards screen (DXCC matrix, WAS, WAC, grids, IOTA, POTA, SOTA, zones) and your own award sheets (My awards).
- [ ] Try it on the work machine (Windows) — install, open the dock **Awards** button (or Log tools ▸ Awards ▸ Open awards), import a LoTW ADIF to see confirmations.
- [ ] **M** ITU zones by grid for the multi-zone countries. CQ zones now come from the worked station's grid square (`ZoneLookup`, using the J-Map CQ outlines) where the country spans several; ITU zones are still the country's default. Needs ITU zone outline data - none in the repo; find a licence-compatible source and add an `itu-zones.json` the same way.
- [ ] **S** Check the new prefix table in the field: the engine's DXCC prefix table was rebuilt from `cty.dat` (about 1,000 prefixes added or corrected: Hawaii, Guam, Asiatic Russia, Kaliningrad, Taiwan, Wales / Scotland, the Canaries and so on, and a dozen wrong entity numbers fixed). Watch the DXCC list and contest country multipliers for a surprise, and for the first week check that `~/.j-hub/cty/cty.dat` appears (weekly refresh).

- [ ] **S** Check **Get confirmations** against your real LoTW and eQSL accounts (tests use sample files only). LoTW uses the documented query; the eQSL inbox download is undocumented and follows what other loggers do, so it is the one most likely to need a fix. A password with a `#` is known to break eQSL downloads.
- [ ] **S** Confirmation sync follow-ups: also add confirmed QSOs missing from the log (today only matched ones are ticked and the rest are counted); show how many were newly confirmed; Club Log and QRZ have confirmation downloads too.
- [ ] **S** Reference lookups: the POTA lookup uses an unofficial interface and may change; the IOTA list is the public download from iota-world.org, kept a week; check their terms, and add a lookup button if typing and tabbing is not enough.

## 2b. Next — contest page (phases 2 and 3)
Phase 1 is built: one generic page driven by the 86 contest definitions in `j-log-engine/.../plugins/`; contest mode is entered from Log tools ▸ Log mode.
- [ ] **M** Phase 2, what is left: the layout block supports a note, tab order / hiding, exchange-field order and a default mode, with the operator's own file over it (the Layout... link). Not done: extra score tiles per contest, per-contest macro sets, pick-lists such as a county list, and layouts for the contests actually run - **tell me which contests to tailor first** (only Sweepstakes has one).
- [ ] **S** Try the call history (`CallHistory`; J-Hub ▸ Setup ▸ Data ▸ Call history): press **Build from my logs**, then in a Sweepstakes or state QSO party type a call you have worked before and check the exchange fills and the cluster spots tag **NEW MULT?** (the `?` marks a guess from history). Check the import against a real N1MM+ call history file - the column names are from memory of the format, not tested against one.
- [ ] **S** Call history follow-ups: learn from QSOs other stations log over the contest network and from ADIF imports as they happen (today: your own contest QSOs as logged, and a manual rebuild); county for in-state QSO parties (a county is not kept today, only a state); a way to fetch a published master call history file.
- [ ] **M** Phase 3, what is left: Sweepstakes is done as far as the flow goes (Space moves between fields, exchange memory). WAE QTC entry is built on the engine's QTC table - check its rules against the current WAEDC rules, and add delete/edit for a saved series. Field Day has a summary-sheet worksheet; the bonus values must be entered from the current year's rules (only emergency power and web submission are filled in). State QSO parties have the in-state mode and a bonus-stations tab; each party's special rules beyond what the engine scores are not covered.
- [ ] **S** Validation: sent-side values are not checked; add a callsign-exists check only if wanted (shape only today). Add state / province / county lists per QSO party so that warning becomes a hard error where the list is definitive.

## 2c. Next — gaps found in the competitor survey
From comparing the suite with N1MM+, DXLab, Log4OM, Wavelog, GridTracker and the portable loggers. We are working on these.
- [ ] **S** Super Check Partial and the band map are built (`ScpStore` / `ScpData` / `ScpBox`; `BandMapModel` / `BandMapView`). What is left of them: the DX and Sweepstakes call lists (`MASTERDX` / `MASTERSS`), a mark on calls already worked in the Super Check Partial strip and the same strip in the normal log; for the band map, the same map beside the contest entry rather than only in the cluster card (Map / List switch), click-to-tune on the ruler itself, mode-segment shading from the band plan, and spots from the local CW skimmer shown in their own colour.
- [ ] **L** **Phone-friendly web client** for the hub, for portable (POTA / SOTA) logging and shack monitoring without a native app: a responsive page served by the hub (the WebSocket link already exists) with QSO entry, spots, rig and rotor status; keep working offline in the field and sync when back on the network. Needs a login or token on the LAN and a decision on how it is reached from outside.
- [ ] **M** Our own propagation estimate, stage 1 is built (`Propagation`, `PathPropagationView`, in the Propagation drawer): from the solar flux, sunspot number and K index the app already pulls, a MUF and LUF for the path from your station to a call or prefix (the last spot you clicked), shown as a 24-hour by band grid; it says on the panel that it is an estimate. It is a simple model (secant-law MUF from a flux-based foF2, D-layer absorption for the LUF) and it is **not checked against measured data or VOACAP** - compare it with real openings or a VOACAP run before trusting it, especially on long multi-hop paths, which are its weakest part. Next stages: refine toward ITU-R P.533, and the foF2 / M3000 coefficient maps that need (static data files; check their licence), sporadic E, grey-line enhancement, the winter anomaly, polar paths, and the operator's power and antennas. Also: use it on the J-Map and spot cards ("open now" on a spot), and a best-time-to-work hint.
- [ ] **S** N1MM-compatible UDP broadcast is built (`N1mm` messages, `N1mmBroadcast` sender; Setup > Data, off by default): contacts added / edited / erased, lookups, radio info, app info and (optional) score, to the addresses given. Check it against a real receiver (GridTracker, Log4OM) and a real N1MM+ capture - the format was written from N1MM+'s document, whose own examples have typos. Still open: spots, a separate address per message type, the normal log's QSOs (only the contest log is broadcast), the "all computers" re-send, the inbound set-frequency command (port 13064) and rotator messages (12040), `countryprefix` (left blank), and run / S&P (`IsRunQSO`).
- [ ] **L** Network mode, stage 1 is built (`com.ars.fx.net`, Setup > Data > Contest network, a strip on the contest page): one station hosts and the others join; every station keeps a full copy of the contest log, so one that loses the host keeps logging and catches up on reconnecting. QSOs have a global id and a version; adds, edits and erases are sent at once and the later change wins; points and dupe flags are worked out at each station from the merged log; a shared key (HMAC handshake) keeps stray devices out; the strip shows who is joined and what each is on, and warns of a station in another contest or on the same band and mode. **Not tried on two real computers yet** - only on loopback in tests. Still open: chat and skeds; per-operator stats; multi-multi roles (one run station per band, multipliers station); serial numbers (two stations can take the same next number at the same instant); a clock-skew warning (the later change wins, so stations need NTP); encryption of the LAN traffic (only the key is protected); finding the host automatically (mDNS) instead of typing its address; a standby host if the host dies; QTCs and the normal log are not shared (only the contest log).
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
