# WM3J ARS Suite — Roadmap

Where the suite is going, organised by phase. The roadmap is shaped by an
honest comparison against Ham Radio Deluxe and DXLab Suite — phases close
real gaps, with "off the list" items called out explicitly so contributors
don't sink time into directions the project has decided not to take.

> **How to read this file**
>
> - **Phase 1 — Quick wins.** Days to a couple of weeks each. Mostly docs
>   and small protocol extensions that reuse code that already exists.
> - **Phase 2 — Differentiators.** Multi-week features that give ARS Suite
>   capabilities no other free suite has.
> - **Phase 3 — Bigger projects.** Multi-month efforts that need a clear
>   contributor willing to own them.
> - **Phase 4 — Off the list.** Capabilities the project has consciously
>   chosen *not* to build. Documented so it's clear why, and so they don't
>   reappear as drive-by proposals.
> - **Continuous.** Polish, triage, accessibility — runs in parallel to
>   the phased work, never claims a "done" date.

---

## Phase 1 — Quick wins

- ~~Audio routing docs (Windows VB-Cable, macOS BlackHole, SignaLink, DigiRig
  walkthroughs in `HARDWARE_GUIDE.md`)~~ ✅ shipped 2026-05-12
- ~~CW Skimmer Server telnet ingest (second `ClusterManager` endpoint; tag
  spots `source:"SKIMMER"` so J-Map can colour them distinctly)~~ ✅ shipped 2026-05-12
- ~~Plug-in development guide (`docs/PLUGINS.md` — contest + award JSON
  schemas with worked examples)~~ ✅ shipped 2026-05-12
- ~~`Help → Report Issue` button in each module (auto-attaches diagnostics
  bundle to a pre-filled GitHub issue)~~ ✅ shipped 2026-05-12 — wired in
  j-log, j-digi, j-bridge, j-sat, j-vault, j-learn, morse-trainer, and
  the j-hub web UI (Dashboard → Configuration Backup & Export). j-map
  defers to the j-hub button since its UI is wallboard-style.
- ~~Live UI density preset (compact / comfortable / spacious) propagated
  over `CONFIG_UPDATE`~~ ✅ shipped 2026-05-12 — selector lives in
  j-hub's left-nav footer; broadcasts via `CONFIG_UPDATE`; j-hub web UI,
  j-log, and j-digi honor it live (multiplies root font size: compact
  0.92×, comfortable 1.0×, spacious 1.12×). j-bridge/j-map/j-sat/j-vault/
  j-learn/morse-trainer are follow-ups (their existing per-app sliders
  cover the same need today).

## Phase 2 — Differentiators

- ~~Embedded CW skimmer in J-Digi (multi-channel CW decode across the audio
  passband; publish `SPOT` messages with `source:"LOCAL_SKIMMER"`; optional
  outbound telnet so the operator becomes a public RBN node)~~ ✅ detector
  tier shipped 2026-05-12 — `LocalSkimmer` identifies up to 16
  simultaneous CW carriers across the audio passband via spectrum
  peak-picking (median smoothing + adaptive noise floor + parabolic
  sub-bin interpolation + bandwidth rejection). Publishes
  `LOCAL_SKIMMER_ACTIVITY` snapshots (1 Hz rate-limit). 8 tests cover
  detection / dedupe / wideband rejection / scan-band limits / cap.
  **Follow-up:** per-channel Goertzel band-pass + `CwMode` decoder + RBN
  outbound — promoted to Phase 3 as it's a substantially bigger DSP lift
  than the detector tier was.
- ~~EME-lite in J-Sat (moon Doppler correction, libration prediction,
  moon-window calendar between QTH and DX grid, frequency handoff to
  WSJT-X via j-bridge)~~ ✅ shipped 2026-05-12 — `LunarMath` (Meeus
  ch. 47 truncated ELP-2000 + ch. 53 libration); EME panel docks in
  the bottom bar when `showEmePanel` is enabled; 1 Hz tick refreshes
  az/el/range/Doppler/libration; common-window finder traverses 24h
  in 10-min steps. WSJT-X handoff covered already via the existing
  j-bridge UDP fan-out — operator dials the offset reported by the
  panel. Tests: 7 cases (distance bounds, Doppler scaling, libration
  bounds, common-window edge cases, motion-over-time).
- ~~Audio Setup Wizard in j-hub (probes audio interfaces, detects
  SignaLink/DigiRig/codec cards by name, suggests sample-rate + buffer
  defaults, runs a TX/RX loopback validation)~~ ✅ shipped 2026-05-12 —
  card on the J-Digi tab; `/api/audio/{devices,loopback-test,save}`
  endpoints; Goertzel-based SNR detection; saves to j-digi via
  `CONFIG_UPDATE` and surfaces matching WSJT-X device hints.
- ~~Community plug-in registry (GitHub-hosted JSON manifest at
  `wm3j.github.io/ars-plugins/index.json`; in-app browser in j-hub with
  one-click install into `~/.j-log/{contests,awards}/`)~~ ✅ shipped
  2026-05-12 — manifest at `docs/plugin-registry.json` (override via
  `-Djhub.plugin.registry=…`); 1-hour cache; in-app browser on the
  J-Log tab; install validates `contestId`/`awardId` and writes to
  `~/.j-log/{plugins,awards}/`. Override URL lets contributors stand
  up their own registry mirror.

## Phase 3 — Bigger projects

- Embedded skimmer per-channel decoder — **parked 2026-05-12.**
  Detector tier shipped in Phase 2 (`LocalSkimmer` peak-picker +
  `LOCAL_SKIMMER_ACTIVITY` broadcasts); the per-channel decoder
  (Goertzel band-pass per detected signal feeding a `CwMode`
  instance, callsign confidence scoring, real `SPOT` emission with
  `source:"LOCAL_SKIMMER"`, optional outbound RBN node) is a 2–3
  week DSP lift. Useful but not the highest-leverage next step;
  revisit when an operator wants to drive the work or when the
  detector tier accumulates real-world signal data we can decode
  against.
- ~~Priority-callsign spot alerts in J-Log~~ ✅ shipped 2026-05-12 —
  operator-curated watch list (POTA targets, friends, missing-DXCC
  needs) stored in `config.db.priority_callsigns`; inbound DX spots
  matched against the list fire a fading top-banner toast over the
  main log plus an in-process two-tone audible alert (800 → 600 Hz,
  350 ms, cosine-ramped); per-entry **muted / audible / banner**
  toggles + free-form note column; suffix-aware so W1AW/P, W1AW/M,
  W1AW/QRP all match W1AW; 60-second per-callsign debounce so a
  repeatedly-spotted target doesn't continuously re-alert. Tools →
  **Priority Callsigns…** opens the management window.
- ~~Dual-mode QSO phrase translator in J-Log~~ ✅ shipped 2026-05-12
  — `~/.j-log/translations.db` holds the phrase book (English /
  Spanish / German / Portuguese plus phonetic + category); seeded
  with 25 starter phrases on first run. Two viewers via Tools menu:
  **User Selected** (operator-picked checkboxes) and **DXCC Driven**
  (auto-picks columns from `DxccLanguageMap` based on the active
  callsign's DXCC entity, fallback to English-only). Both viewers
  share editable rows that persist immediately + a pin-to-top
  toggle; columns auto-hide when every cell is empty so partial
  translations stay tidy.
- Voice-control listener (`j-voice`) — **parked.** Offline Vosk model
  to parse *"tune to twenty meters"* / *"call CQ"* / *"log this QSO"*
  into `RIG_CONTROL` / `MODEM_TX` / `QSO_SAVE` WebSocket messages.
  Compelling differentiator but niche audience; revisit if a
  contributor wants to own it.

## Phase 4 — Off the list

These are deliberately not on the roadmap. Reasons noted so this stays
settled.

- **Full internet-remote operation (WAN audio).** Real internet remote
  needs sub-100 ms audio over WAN, push-to-talk over WebRTC, and a
  security model against rogue control. That's a separate product
  (RemoteHams, RCForb, FlexRadio Maestro). ARS Suite stays a station-side
  suite that pairs with a dedicated remote tool.
- **Full SO2R contest cockpit.** SO2R operators want N1MM+ /
  WriteLog / Win-Test. ARS Suite isn't going to outcompete N1MM+ there.
  Better integration story: keep ARS Suite logging / awards / maps,
  hand contest operation to N1MM+ via existing UDP broadcasts, the same
  way ARS Suite already defers to WSJT-X for FT8.
- **MAP65 / polarisation EME.** Niche-within-niche; the operators who
  need it already run WSJT-X + MAP65. The Phase 2 EME-lite work covers
  the ~80% of EME-curious operators who want Doppler + moon windows but
  aren't building a polarisation tracker.

## Active in Phase 3

- **Per-module UI i18n** — j-log already has 6-language coverage
  (en/de/es/fr/it/pt) and j-learn's chrome was localised 2026-05-12,
  but J-Digi, J-Bridge, J-Map, J-Sat, J-Vault, and Morse Trainer
  still ship English-only UI. The work is mechanical (extract
  strings, bundle them, swap labels for `I18n.get(...)` calls) but
  spans every module. Highest-leverage targets first: J-Digi
  (operator-facing during QSOs) and J-Map (operator-facing
  during DX chasing).

## Continuous (no phase, runs forever)

- **Per-release UI polish.** Each release picks one module and does a
  dedicated pass: keyboard nav, focus order, error-message wording,
  empty-state copy.
- **Issue triage.** Floor: close 3 issues per week. Diagnostics bundles
  attached to issues via the Phase 1 `Help → Report Issue` button feed
  this directly.
- **Quarterly accessibility audit.** One module per quarter gets a
  screen-reader pass.
- **Annual beta-tester standup.** Ship a release named after their
  top-voted fix.

---

## What this roadmap closes vs HRD / DXLab

After **Phase 1 + 2** the suite leads HRD on FOSS, cross-platform support,
single config surface, Antenna Workshop, J-Vault, J-Learn, Morse Trainer,
and now adds a local CW skimmer no free suite ships today.

Against DXLab the suite narrows the gap on plug-in ecosystem (Phase 1
plug-in guide + Phase 2 registry) and on local skimming. DXLab still leads
on SpotCollector's spot-filter rules and Pathfinder's lookup chain —
both are 20 years of single-author polish.

After **Phase 3** the suite picks up voice control and a LAN control
panel — capabilities neither HRD nor DXLab ships in their free product.
The SO2R + WAN-remote gap stays open *by choice*, see Phase 4.

---

*Last revised 2026-05-12. This document tracks intent; the
`MEMORY.md` index in the Claude memory directory tracks day-to-day
status of in-flight work.*
