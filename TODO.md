# ARS Suite — To do

Kept in the repo so it carries over between sessions. Tick items off (`[x]`) as they are done; add new ones at the top of a section.

## Ideas to look at
- [ ] Contest calendar and other info — maybe a new **Info** tab (upcoming contests, plus other useful reference info). Decide where it lives and what feeds it.
- [ ] Maps: look at the J-Map options and see whether something better exists (tiles, projections, performance).
- [ ] J-Map: point-to-point tracks (ham to ham) — draw the path between two stations on the map.
- [ ] Beacon mode: look at it, get it working, and set up an auto-QSY mode.
- [ ] 3D globe as a map option — find out whether it is possible in JavaFX (and what it would cost).
- [ ] Get award tracking back into J-Log (DXCC, WAS, and so on; there is an `Awards` class in `data/` and an awards drawer to start from).

## Testing needed
- [ ] QSL label printing (beta, v1.12.2): try on real hardware — Brother QL-810W, DYMO LabelWriter 550, Zebra ZD421 (ZPL and driver modes). Check size, margins, rotation on narrow strip labels.
- [ ] Log browser: click through the Band / Mode filters, Delete + confirmation, and the **Browse full log ⤢** button inside J-Log (only tested through a harness).
- [ ] Confirm the Amateur Radio menu folder shows on Cinnamon after the installer fix.

## Features
- [ ] QSL labels: print from contest mode; optional "reprint" for a selected QSO (edits do not print today).
- [ ] QSL labels: more label stock / printer models if requested.
- [ ] Log browser: contest-log databases (it shows the normal log only); export the filtered view.

## Docs / housekeeping
- [ ] `USER_GUIDE.md` §3.3 still describes the old long J-Hub Data page; rewrite for the Setup screen and its menu.
- [ ] Check the 2026-10-03 9K2IC (70cm DMR) entry in the log — looks like a mistyped date.
