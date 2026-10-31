# ARS Suite — design reference notes

## What this is
A set of **high-fidelity HTML/CSS/JS design references** for the ARS Suite (J-Hub + 7 modules): prototypes that show the intended
look and behaviour. They are **not the app** — the app is the JavaFX program in `../../ars-fx`, which was built from these and has
moved on since (see `../README.md`). Where the two differ, the app wins.

What is still worth using them for:
- the **design tokens** (`ars-tokens.css`) and the **card / chiclet / `--h` hue conventions** (`EDITING-STANDARD.md`) when adding or
  restyling a screen;
- the **screenshots** in `handoff-shots/` as a picture of the intended look;
- `Rig Control Drawer.html`, which `ars-fx`'s `RigPanel` is built 1:1 from.

The references are deliberately **JavaFX-safe**: solid fills, 1px borders, simple drop shadows, `oklch`/hex colours, no
`backdrop-filter`, no blur, no heavy CSS filters, so they translate directly to JavaFX CSS and scene nodes.

To preview one, open any `J-*.html` in a browser (they load via CDN React + Babel; no build step).

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

See `EDITING-STANDARD.md` for the card/chiclet conventions.
