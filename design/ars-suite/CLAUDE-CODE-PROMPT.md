# Claude Code — BUILD PROMPT (read this first, do not skip)

## 0. READ `PROJECT-OVERVIEW.md` FIRST
Before anything, read **`PROJECT-OVERVIEW.md`** — it describes the WHOLE product and the three architecture decisions you previously got wrong (J-Hub is unified, the J-Hub cockpit is a full dashboard not chiclets, every module shares the shell). This prompt is the per-screen build guide; the overview is the map.

You are implementing the **ARS Suite** UI in **JavaFX**. A complete, approved set of design references already exists in this folder. **Your job is to reproduce them faithfully in JavaFX — not to design anything, not to simplify, not to use an older layout you may have seen.**

## ⛔ STOP — what went wrong last time
A previous run ignored these references and shipped an early "Cockpit A" prototype: **no drawers, wrong layout, wrong rail.** That is WRONG and must not happen again. The early prototypes (`dir-a.jsx`, `dir-b.jsx`, `dir-c.jsx`, `J-Hub Launcher.html`) are **historical exploration only — DO NOT BUILD FROM THEM.**

## ✅ The ONLY sources of truth
Build each screen from **(a) its screenshot** and **(b) its current HTML/CSS/JS**, listed below. If your output doesn't match the screenshot, your output is wrong — fix it to match.

| Screen | Screenshot (ground truth) | Code to translate |
|---|---|---|
| J-Hub dashboard/cockpit | `handoff-shots/01-jhub-dashboard.png` | `J-Hub Workspace.html` + `ws-ui.jsx` + `ws-dashboard.jsx` + `ws-data.jsx` |
| J-Hub **in-app config page** (proves config is NOT a separate web UI) | `handoff-shots/09-jhub-config.png` | same as above (`ws-ui.jsx` router + config pages) |
| J-Log cockpit | `handoff-shots/02-jlog-cockpit.png` | `J-Log Cockpit.html` + `jlog-app.jsx` + `jlog.css` + `jlog-data.jsx` |
| J-Map | `handoff-shots/03-jmap.png` | `J-Map.html` + `jmap-app.jsx` + `jmap.css` |
| J-Digi | `handoff-shots/04-jdigi.png` | `J-Digi.html` + `jdigi-app.jsx` + `jdigi.css` |
| J-Bridge | `handoff-shots/05-jbridge.png` | `J-Bridge.html` + `jbridge-app.jsx` + `jbridge.css` |
| J-Sat | `handoff-shots/06-jsat.png` | `J-Sat.html` + `jsat-app.jsx` + `jsat.css` |
| J-Vault | `handoff-shots/07-jvault.png` | `J-Vault.html` + `jvault-app.jsx` + `jvault.css` |
| J-Learn | `handoff-shots/08-jlearn.png` | `J-Learn.html` + `jlearn-app.jsx` + `jlearn.css` |

Shared across all: `ars-tokens.css` (colors/spacing — **single source of truth**), `suite-shell.jsx`+`suite-shell.css` (the **left module dock** + the **collapsible right-rail drawers** + shared top bar), `ars-shared.jsx` (module list, icons).

## Required structure — EVERY module screen has all three
Look at any screenshot: they share one shell. You MUST reproduce all three regions:
1. **Left module dock** (`SuiteDock`) — narrow icon rail, expands on hover, lists J-Hub + the 7 modules + Station settings, highlights the active one, navigates between modules.
2. **Center work area** — the module's main content (per screenshot).
3. **Right rail of collapsible DRAWERS** (`SuiteDrawer` / `SuiteInstruments`) — accordion panels with a header + glance summary that expand/collapse. The standard instrument drawers are **Antenna·Rotor, Propagation, Space weather, Weather**, plus module-specific ones. **If your screen has no drawer rail, it is wrong.**

Plus the **top bar** (`.sx-top`): brand + live telemetry stats + UTC clock.

## How to translate to JavaFX (this is a REWRITE, not a port)
- Recreate the **layout, components, hierarchy, spacing, and states** from the screenshot/code using JavaFX nodes (`BorderPane`/`HBox`/`VBox`/`GridPane`, `Region`, `Canvas` for waterfalls/compass/sky-plot).
- Put all colors/spacing/radii into a **JavaFX CSS stylesheet** mapping 1:1 to `ars-tokens.css` custom properties (JavaFX has no CSS vars — use `-fx-` looked-up colors, e.g. `.root { -ars-surface-2: #1a212b; }` and reference with `-fx-background-color: -ars-surface-2;`). Dark + light variants.
- These references are already **JavaFX-safe**: no blur/backdrop-filter/heavy filters. Keep it that way. Solid fills, ≤1px borders, the two defined drop shadows only.
- **Monospace (JetBrains Mono) on every telemetry readout** — frequency, azimuth, SWR, dB, score, clock. UI text = IBM Plex Sans.
- The **`--h` hue system**: each module/card sets one hue and derives tints from it. In JavaFX, pass the module color and use `derive()`/`ladder()` or precomputed mixes to reproduce the `color-mix(in oklch, var(--h) N%, surface)` tints.
- Reproduce **interaction states**: hover (border → `--border-glow`, tiles lift), running/active (hue tint + left accent bar + label), drawer expand/collapse, dock hover-expand.

## Editing cards & chiclets later
Follow **`EDITING-STANDARD.md`** — card/chiclet anatomy, the `--h` convention, the "is this on-system?" checklist, and a paste-in edit prompt. Use it for every card/chiclet adjustment so the distro stays coherent.

## Definition of done (per screen)
- [ ] Left dock present, hover-expands, navigates, highlights active module
- [ ] Center matches the screenshot layout & content
- [ ] Right rail has the collapsible drawers shown in the screenshot (incl. instrument drawers)
- [ ] Top bar with telemetry stats + UTC clock
- [ ] All colors/spacing from the token stylesheet (no stray hex), dark + light
- [ ] Mono on every numeric readout
- [ ] Side-by-side with the screenshot, a reviewer can't tell which is which

## Suggested order
1. **Shared shell first** (dock + drawer + top bar + token stylesheet) — everything else depends on it.
2. J-Log cockpit (densest, proves the system) → J-Hub dashboard → J-Map → J-Digi/J-Bridge → J-Sat → J-Vault → J-Learn.

If anything is ambiguous, **open the HTML in a browser and inspect it**, or ask — do **not** invent or fall back to an older layout.
