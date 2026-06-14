# ARS Suite — PROJECT OVERVIEW (read this before any code)

> Claude Code: this describes the **entire approved product**. Earlier you built fragments from outdated prototypes — J-Hub as a separate web UI, the J-Hub cockpit as "just chiclets." **Both are wrong and obsolete.** This document is the whole picture. Build the suite as described here, reproducing the 9 screenshots in `handoff-shots/`, rewritten in JavaFX.

---

## The big picture
ARS Suite is **one coherent desktop product**: a central **J-Hub** plus **7 modules** (J-Log, J-Map, J-Digi, J-Bridge, J-Sat, J-Vault, J-Learn). Every surface shares ONE shell — a left module dock, a center work area, and a right rail of collapsible drawers — so moving between modules feels like one app, not seven.

**This is the entire suite. There are 8 application surfaces, all already designed:**
| # | Surface | File | Screenshot |
|---|---|---|---|
| 1 | **J-Hub Workspace** (launcher + unified config + cockpit dashboard) | `J-Hub Workspace.html` | `01-jhub-dashboard.png`, `09-jhub-config.png` |
| 2 | J-Log cockpit | `J-Log Cockpit.html` | `02-jlog-cockpit.png` |
| 3 | J-Map | `J-Map.html` | `03-jmap.png` |
| 4 | J-Digi | `J-Digi.html` | `04-jdigi.png` |
| 5 | J-Bridge | `J-Bridge.html` | `05-jbridge.png` |
| 6 | J-Sat | `J-Sat.html` | `06-jsat.png` |
| 7 | J-Vault | `J-Vault.html` | `07-jvault.png` |
| 8 | J-Learn | `J-Learn.html` | `08-jlearn.png` |

---

## ‼️ THREE architecture decisions you MUST honor (this is where you went wrong)

### 1. J-Hub is UNIFIED — there is no separate "web config UI" anymore
The original product had a *separate* J-Hub web config UI (sidebar of 20 flat items) **plus** a launcher. **We merged them.** J-Hub is now ONE workspace window with:
- A **left nav** split into **OPERATE** (the 7 modules — launch/stop/focus) and **J-HUB** (config: Dashboard, Station, Hardware, Data, Intel — grouped, hierarchical, NOT 20 flat items).
- A **center router**: selecting Dashboard shows the cockpit; selecting any config item (e.g. Hardware → Rig control) swaps in that settings page with a breadcrumb. See `09-jhub-config.png` — that Rig-control page lives **inside J-Hub**, it is NOT a separate browser app.
- Do **not** build a standalone web config UI. Do **not** keep the flat 20-item sidebar. The grouped IA replaces it.

### 2. The J-Hub cockpit is a FULL DASHBOARD — not "just chiclets"
See `01-jhub-dashboard.png`. The dashboard centerpiece is a **working mini J-Log** (quick QSO entry: callsign/RST/freq/mode + recent-QSO table) and a **live DX Cluster**, with a **right rail of collapsible instrument drawers** (Rig control, Rotor control, Space weather, Propagation, Weather). The module **chiclets are NOT the dashboard** — they became the **left dock** (the OPERATE list). If your J-Hub is a grid of chiclets, you built the obsolete prototype. Delete it and build the dashboard in the screenshot.

### 3. Every module shares the SAME shell — all three regions, every screen
Open any of the 8 screenshots: same skeleton.
- **Left module dock** — narrow icon rail, hover-expands to names, lists J-Hub + 7 modules + Station settings, highlights the active module, navigates between them.
- **Center** — that module's work area.
- **Right rail of collapsible DRAWERS** — accordion panels (header + glance summary, expand/collapse). Standard instrument drawers **Antenna·Rotor, Propagation, Space weather, Weather** appear across modules, plus module-specific ones.
- **Top bar** — brand + live telemetry stats + UTC clock.
**A module screen with no drawer rail, or no dock, is wrong.**

---

## What each surface contains (so you don't simplify them away)
- **J-Hub Workspace** — left OPERATE/J-HUB nav; center = cockpit dashboard (mini J-Log + DX cluster + setup profiles) OR a routed config page (Station/Hardware/Data/Intel); config pages are real forms (selects, segmented toggles, sliders, switches) — see `09`.
- **J-Log** — contest cockpit: exchange entry (Run/S&P), Full-QSO-details disclosure, contest log table, band map, Zones/Rate/DX-Cluster + instrument drawers, F-key macro bar, bottom status bar with freq/mode/band. Contest-selectable (CQ WW, WPX, etc.).
- **J-Map** — propagation map; **azimuthal** (polar, QTH-centered) and **rectangular** (NASA Blue Marble basemap `bluemarble.jpg`) projections; spots overlaid with great-circle/gray-line; drawers: DX Spots, DX station info, Antenna·Rotor, Propagation, Space wx, Weather.
- **J-Digi** — CW/RTTY/PSK decode; waterfall (Canvas) + tuning cursor, mode tabs, live decode window, Tx macros; Decoder + instrument drawers.
- **J-Bridge** — WSJT-X/FT8: waterfall, 15s sequence bar + Even/Odd + Enable-Tx, Band Activity / Rx Frequency decode tables, Tx1–6 message sequence; WSJT-X link + instrument drawers.
- **J-Sat** — polar sky plot (zenith center) with pass track + live position, satellite pass list (AOS/next/LOS), az/el/range telemetry, Doppler uplink/downlink VFOs; Next-passes + instrument drawers.
- **J-Vault** — inventory table (category/value/condition/**estate disposition**), value summary, item detail, estate-planning documents, disposition-by-heir; drawers.
- **J-Learn** — topic browser, study-deck grid with progress + cards-due, reference reader, quick reference, exam prep; drawers.

---

## Build instructions (JavaFX rewrite)
1. **Read** `CLAUDE-CODE-PROMPT.md` (per-screen source-of-truth table + JavaFX translation rules) and `EDITING-STANDARD.md` (card/chiclet conventions).
2. **Build the shared shell FIRST** — token stylesheet (from `ars-tokens.css`), the dock, the drawer component, the instrument drawers, the top bar. Everything depends on it.
3. **Then build each surface** to match its screenshot + code. Order: J-Log → J-Hub Workspace (dashboard + a config page) → J-Map → J-Digi/J-Bridge → J-Sat → J-Vault → J-Learn.
4. These references are **JavaFX-safe** (no blur/filters). Solid fills, ≤1px borders, two defined shadows. Monospace on every numeric readout. Dark + light themes via the token stylesheet.
5. **Match the screenshots.** If yours differs, yours is wrong — fix it. Don't invent, don't simplify, don't revert to an older layout. When unsure, open the HTML in a browser and inspect.

## Acceptance for the whole project
- [ ] J-Hub is ONE workspace (nav + router): cockpit dashboard AND in-app config pages — no separate web UI, no flat 20-item sidebar
- [ ] J-Hub dashboard = mini J-Log + DX cluster + instrument drawer rail (NOT a chiclet grid)
- [ ] All 8 surfaces share dock + center + drawer rail + top bar
- [ ] Standard instrument drawers present where shown
- [ ] Tokens only (dark+light), mono telemetry, JavaFX-safe
- [ ] Each screen indistinguishable from its screenshot side-by-side
