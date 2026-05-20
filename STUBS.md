# ARS Suite — Stubs & Unfinished Features

Audit captured 2026-05-20 alongside the CI-V build-out. Each entry is what's
*claimed* by the UI or code structure vs. what actually runs today. Order is
rough priority: user-visible bugs first, internal stubs after.

## A. UI fields users fill in that no Java code reads

These are the "CI-V trap" — a setting is presented in the j-hub web UI, gets
saved to `j-hub.json`, but nothing on the backend ever consumes it. The user
thinks the feature works.

- [x] **Rig: CI-V Direct backend** — `RigSection.civPort` / `civBaud` /
      `civAddress`. UI: `#civ-block` in `index.html`, populated by `config.js`
      lines 981–983. Build in progress (this branch).
- [ ] **Rotor: Short-path offset** — `RotorSection.shortPathOffset`. UI:
      `#rot-short-offset` (`config.js:1005`). `HamlibRotorController.start()`
      reads only `tcpHost`/`tcpPort`; the offset feeds the rotor preset
      buttons users actually click.
- [ ] **Rotor: Custom preset** — `RotorSection.customPreset`. UI:
      `#rot-custom` (`config.js:1006`). Same bug as above.
- [ ] **Appearance: fontSize, waterfallColor** —
      `AppearanceSection.fontSize`, `AppearanceSection.waterfallColor`.
      Schema-only, no UI inputs and no readers. Either expose + apply, or
      delete from schema.

## B. Explicit "not implemented" markers in shipped code

User-facing first.

- [ ] **j-digi `{NAME}` macro substitution returns "OM" placeholder** —
      `j-digi/src/main/java/com/hamradio/modem/ui/MacroBar.java:23`. Every CW
      macro that uses `{NAME}` is silently wrong. Should read from the
      station-config `name` field broadcast in `STATION_CONFIG`.
- [ ] **j-digi: Transmit not implemented for certain modes** —
      `ModemService.java:527`. Identify which modes; document or finish.
- [ ] **j-digi: WAV export not implemented for certain modes** —
      `ModemService.java:603`. Same as above.
- [ ] **j-log: "not implemented" alert dialog** —
      `ContestLogController.java:3646`. Find which menu/button surfaces this
      and either build it or hide the entry point.
- [ ] **j-log: unknown-formula returns 0** — `ContestLogController.java:1958`.
      Silent scoring stub; any contest that hits this falls through to a
      zero score. Audit which contest plugins can trigger it.
- [ ] **j-learn: `<!-- TODO: content -->` placeholder** — `jlearn.js:393`.
      Per-page content gap; find which chapters/sections still hold the
      marker.

Internal stubs (no user impact today, but flagged for completeness):

- [ ] **j-digi LocalSkimmer Phase 2** — `dsp/LocalSkimmer.java:19`. Embedded
      CW skimmer foundation tier.
- [ ] **j-hub FCC ULS Phase 2** — `FccUlsImporter.java:83`. USI→operator
      class mapping from AM.dat.
- [ ] **j-hub antenna calculator Phase 2b** — `config.js:6380`.

Intentional / out of scope (do NOT build):

- **j-sat EME Phase 2** — `JsatSettings.java:38`. Matches the documented
  out-of-scope list (SO2R, web-remote, EME, voice-recog).

## C. Dead-quiet schema fields (config-file clutter, no UI exposure)

Not user-visible but they pollute `j-hub.json` and mislead anyone reading the
schema. Either wire them or delete them.

- [ ] `RotorSection.model`, `RotorSection.comPort` — "INTERNAL" backend
      placeholder that never shipped.
- [ ] `AmpSection.model`, `AmpSection.comPort`, `AmpSection.baud` — schema
      comment already admits they're informational.
- [ ] `LoggerSection.normalLog`, `LoggerSection.contests`,
      `LoggerSection.activeContest` — never read by anything in j-hub.
- [ ] All four `InfoScreenSection` fields (`mapStyle`, `showGreatCircle`,
      `spotTimeout`, `maxCachedSpots`).
