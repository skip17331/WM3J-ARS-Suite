# Claude Code task — Rig Control drawer (canonical, with sub-drawers)

Build the **Rig Control drawer** in JavaFX to match the reference exactly. This is the single canonical rig panel for the whole suite and **replaces the older simple "Rig control" drawer** everywhere it appears.

## Source of truth
- **Layout (open & interact):** `Rig Control Drawer.html` (standalone, static, built on `ars-tokens.css`).
- **Screenshots** in `handoff-shots/`:
  - `10-rig-control-drawer.png` — the whole drawer, default state.
  - `10b-rig-band-mode.png` — **Sub-drawer 1: Band & Mode** expanded.
  - `10c-rig-filter-functions.png` — **Sub-drawer 2: Filter & Functions** expanded.
  - `10d-rig-dsp-levels.png` — **Sub-drawer 3: DSP & Levels** expanded.
- Build THIS layout; do not reuse the previous rig drawer markup.

## Where it goes
- The drawer **drops down from the top-right RIG button** and expands to **≈1/4 of the screen width** (≈26%, clamp 348–392px), full height.
- It **replaces the old simple rig drawer** in: J-Hub Workspace dashboard rail, J-Log instrument rail, any module rail that shows a rig panel.

## ‼️ Structure: ONE drawer containing THREE sub-drawers
Read this carefully — there are two nesting levels.

### The DRAWER (outer, fixed; always visible)
Top → bottom, **pinned/non-collapsing**:
1. **Header** — rig name (IC-7610 · CI-V), CAT status, collapse chevron.
2. **Meters strip** (above the frequency) — **S-meter** (segmented) on top; then a 3-up mini-bar row **SWR · PWR · ALC**.
3. **Frequency pane** —
   - **Direct-entry frequency** in the center (editable; type to set, e.g. `14.074.000`).
   - **Stacked arrow pair on the LEFT = FAST** (▲/▼, accent-colored, large step e.g. 1 kHz).
   - **Stacked arrow pair on the RIGHT = SLOW** (▲/▼, small step e.g. 10 Hz).
   - VFO A/B toggle + SPLIT tag above; VFO-B line + step legend ("Fast step 1 kHz · Slow step 10 Hz") below.
4. **Footer action bar** (pinned to bottom) — **ATU · TUNE · PTT**.

### The SUB-DRAWERS (collapsible accordions, between freq pane and footer)
Each is a collapsible section with a header (label + glance summary + chevron) that expands/collapses (use a max-height transition — NOT grid-template-rows). Default: **Sub-drawer 1 open, 2 & 3 collapsed.**
- **Sub-drawer 1 — “Band & Mode”** (`10b`): Band grid (160→6 + GEN) + Mode grid (USB/LSB/CW/CW-R/RTTY/FT8/AM/FM). Summary e.g. `20m · USB`.
- **Sub-drawer 2 — “Filter & Functions”** (`10c`): passband visualizer + BW/Shift/NR/Notch tiles, and 8 function toggles (SPLIT, RIT, XIT, A=B, A/B, VOX, SPOT, LOCK). Summary e.g. `2.4k · SPLIT`.
- **Sub-drawer 3 — “DSP & Levels”** (`10d`): DSP grid (AGC, PRE, ATT, NB, NR, NOTCH) + level sliders (PWR, MIC, RF-G, AF). Summary e.g. `AGC-M · 92W`.

> The collapsed sub-drawer header shows a one-line summary so the panel stays compact — open only what you need.

## Style rules
- Tokens only from `ars-tokens.css` (dark + light); no raw hex.
- **JavaFX-safe**: solid fills, ≤1px borders, simple shadows, no blur/filters.
- **Monospace (JetBrains Mono) on every readout** — frequency, S-meter scale, SWR/PWR/ALC, BW, levels, VFO-B.
- Frequency digits use the color treatment in the reference (MHz neutral, kHz accent, Hz dimmed).
- Follow `EDITING-STANDARD.md` for card/chiclet conventions.

## Wiring (layout only here — you implement the logic)
- **Fast/slow arrows** → ±step CAT tune (support press-and-hold to repeat). Steps configurable (fast≈1 kHz, slow≈10 Hz, or tie fast to band-step / slow to tuning-step).
- **Direct-entry field** → parse & set frequency on Enter/blur.
- **VFO A/B, SPLIT** → VFO/split control. Band & Mode grids → set band/mode.
- **Filter** (BW/shift/NR/notch), **Functions** (SPLIT/RIT/XIT/A=B/A/B/VOX/SPOT/LOCK), **DSP** (AGC/PRE/ATT/NB/NR/NOTCH) → rig commands.
- **Level sliders** (PWR/MIC/RF-G/AF) → rig set-levels.
- **Meters** (S/SWR/PWR/ALC) → live rig telemetry.
- **ATU / TUNE / PTT** → ATU cycle / tune carrier / transmit.
- Keep frequency/VFO state shared app-wide (top-bar RIG readout, J-Log entry, J-Map tune-to-spot all reflect the same rig state).
- Persist each sub-drawer's open/closed state (optionally per-operator).

## Done when
- [ ] Outer drawer matches `10-rig-control-drawer.png`; ≈1/4 width; header + meters strip + freq pane pinned, footer pinned.
- [ ] S-meter + SWR sit ABOVE the frequency pane.
- [ ] Frequency = direct entry, with FAST arrows left / SLOW arrows right (stacked ▲▼).
- [ ] Three sub-drawers (Band & Mode / Filter & Functions / DSP & Levels) collapse independently and match `10b/10c/10d`.
- [ ] Tokens only, mono telemetry, dark + light, JavaFX-safe.
