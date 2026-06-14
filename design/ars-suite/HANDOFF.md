# ARS Suite — Claude Code Handoff

## What this is
A complete set of **high-fidelity HTML/CSS/JS design references** for the ARS Suite (J-Hub + 7 modules). These are **prototypes that show intended look and behavior** — not production code to ship as-is. The task in your distro is to **recreate these designs using your real stack** (JavaFX scenes for the native windows, your embedded web UI framework for the J-Hub web surface), reusing the tokens and component conventions documented here.

Two render targets (as in the product):
- **Native JavaFX windows** — J-Hub Launcher, J-Log cockpit. Everything here is deliberately **JavaFX-safe**: solid fills, 1px borders, simple drop shadows, `oklch`/hex colors, no `backdrop-filter`, no blur, no heavy CSS filters. Translate 1:1 to JavaFX CSS + scene nodes.
- **Embedded web UI (localhost:8081)** — J-Hub config/dashboard and the module web surfaces. React-ready; recreate in your web framework.

Fidelity: **hi-fi.** Colors, type, spacing, and interactions are final-intent. Match them.

---

## How to bring this into Claude Code
1. Download the bundle (the `ars-suite/` folder) and drop it into your repo, e.g. `design/ars-suite/`.
2. Open the repo in Claude Code.
3. Point Claude at this file first:
   > "Read `design/ars-suite/HANDOFF.md` and `EDITING-STANDARD.md`. We're recreating these HTML design references in <our JavaFX app / our web UI>. Start with <screen>. Follow the editing standard exactly — especially the card and chiclet conventions and the `--h` hue system."
4. Implement screen-by-screen. Each module is self-contained (see File Map). Keep `ars-tokens.css` values as your single source of truth for color/spacing/radius.
5. For any card/chiclet changes your distro needs, use the prompt template in `EDITING-STANDARD.md` so edits stay consistent.

To preview a reference while building: open any `J-*.html` in a browser (they load via CDN React + Babel; no build step).

---

## Architecture of the references
- **`ars-tokens.css`** — the `-jl-` slate design system: all color/spacing/radius/shadow tokens as CSS custom properties, dark + light (`.ars-light`). **Single source of truth.**
- **`ars-shared.jsx`** — shared data + helpers: `ARS_MODULES` (the 7 modules + hue + running state), `ARSGlyph` (module icons), `ARSCompass`, `ARSBandScope`.
- **`suite-shell.jsx` / `suite-shell.css`** — the cross-module shell:
  - `SuiteDock` — left module launcher (collapsed icon rail → expands on hover; links between modules).
  - `SuiteDrawer` — the reusable collapsible drawer (accordion) used in every right rail.
  - `SuiteInstruments` — the standard instrument drawers (Antenna·Rotor, Propagation, Space weather, Weather).
  - `SuiteWaterfall` — canvas spectrogram (J-Digi/J-Bridge).
  - Shared top-bar (`.sx-top`), dock, drawer, and instrument-body styles.

Each module = one `J-*.html` loader + `<name>-app.jsx` + `<name>.css` (+ `<name>-data.jsx` for mock data).

---

## File map
| Module | Loader | App | CSS | Data |
|---|---|---|---|---|
| Launcher exploration (3 directions) | `J-Hub Launcher.html` | `dir-a.jsx` `dir-b.jsx` `dir-c.jsx` `dir-a-drawer.jsx` | inline | `ars-shared.jsx` |
| J-Hub Workspace (launcher+config+dashboard) | `J-Hub Workspace.html` | `ws-ui.jsx` `ws-dashboard.jsx` | `ws-*` inline | `ws-data.jsx` |
| J-Log cockpit | `J-Log Cockpit.html` | `jlog-app.jsx` | `jlog.css` | `jlog-data.jsx` |
| J-Map | `J-Map.html` | `jmap-app.jsx` | `jmap.css` | `jmap-data.jsx` (+ `bluemarble.jpg`, `image-slot.js`) |
| J-Digi | `J-Digi.html` | `jdigi-app.jsx` | `jdigi.css` | — |
| J-Bridge | `J-Bridge.html` | `jbridge-app.jsx` | `jbridge.css` | — |
| J-Sat | `J-Sat.html` | `jsat-app.jsx` | `jsat.css` | `jsat-data.jsx` |
| J-Vault | `J-Vault.html` | `jvault-app.jsx` | `jvault.css` | `jvault-data.jsx` |
| J-Learn | `J-Learn.html` | `jlearn-app.jsx` | `jlearn.css` | `jlearn-data.jsx` |
| Shared | — | `ars-shared.jsx` `suite-shell.jsx` | `ars-tokens.css` `suite-shell.css` | — |

---

## Design tokens (from `ars-tokens.css` — dark)
**Surfaces:** `--bg #0d1117` · `--surface-1 #141a22` · `--surface-2 #1a212b` · `--surface-3 #222b37` · `--surface-4 #2b3644`
**Borders:** `--border #28313d` · `--border-2 #38434f` · `--border-glow #455263`
**Text:** `--t1 #e8eef4` · `--t2 #97a4b2` · `--t3 #6a7684` · `--t4 #4d5663`
**Suite accent:** `--accent oklch(0.74 0.12 215)` (+ `--accent-dim`, `--accent-line`)
**Module hues** (shared chroma/lightness, varied hue — this is the J- family system):
`--log oklch(0.80 0.13 78)` amber · `--map oklch(0.74 0.12 205)` cyan · `--digi oklch(0.72 0.13 300)` violet · `--bridge oklch(0.72 0.12 255)` blue · `--sat oklch(0.76 0.13 150)` green · `--vault oklch(0.76 0.09 62)` bronze · `--learn oklch(0.72 0.14 18)` coral · `--hub oklch(0.80 0.04 220)`
**Status:** `--ok` green · `--idle` · `--warn` amber · `--err` red
**Radius:** `--radius 8px` · `--radius-sm 5px` · `--radius-lg 12px`
**Shadow:** `--shadow-1 0 1px 2px rgba(0,0,0,.4)` · `--shadow-2 0 6px 22px rgba(0,0,0,.45)`
**Type:** UI = IBM Plex Sans; **telemetry/numeric = JetBrains Mono** (`.ars-mono`, `font-feature-settings:'tnum'`). Keep mono for every freq/azimuth/SWR/score/clock readout.
Light theme = same tokens overridden under `.ars-root.ars-light`.

---

## Assets
- `bluemarble.jpg` — NASA Blue Marble (public domain, "Land Shallow Topo 2048", equirectangular). Default J-Map rectangular basemap. Safe to ship; re-host in your assets.
- `image-slot.js` — drop-in user image component (J-Map custom basemaps). Optional.
- No other external images; all icons are inline SVG (`ARSGlyph`) — reproduce as JavaFX SVGPath or web SVG.

See `EDITING-STANDARD.md` for the card/chiclet conventions and the edit prompt template.
