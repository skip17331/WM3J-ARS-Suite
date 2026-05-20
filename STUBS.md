# ARS Suite — Stubs & Unfinished Features

Audit captured 2026-05-20; closeout pass same day. Order is rough user-impact
priority. Items closed in this pass are noted with their commit hash where
already pushed.

## A. UI fields users fill in that no Java code reads

- [x] **Rig: CI-V Direct backend** — `RigSection.civPort`/`civBaud`/`civAddress`.
      Closed 2026-05-20, commit `f227aa1`.

  ~~Rotor: shortPathOffset / customPreset~~ — false positive. Consumed
  client-side at `config.js:rotorPreset()`.

- [x] **Appearance: fontSize / waterfallColor** — schema-only, no readers.
      Deleted from `AppearanceSection` + JS default. Closed in this pass.

- [ ] **Logger: dbPath / mode** — `LoggerSection.normalLog.dbPath` and
      `LoggerSection.mode` are exposed in the j-hub UI (Logging tab,
      `#log-db-path` + `#log-mode`) and posted via `saveLogging()`, but no
      Java code reads them and j-log uses its own hardcoded `~/.j-log/`
      path. Same shape as the CI-V trap. **Closeout** (deferred): either
      (a) wire the dbPath through `STATION_CONFIG` so j-log honors it, or
      (b) remove the Logging tab. Decide later.

## B. Explicit "not implemented" markers

User-facing — closed:

- [x] **j-digi `{NAME}` macro = "OM" placeholder** — closed.
      `LogEntryPane.getName()` exposes the typed DX-op name field;
      `MacroBar` constructor gained a `nameFn` Supplier and wires
      `ctx.name`. `MacroVariableEngine` already substitutes "OM" when
      the supplier returns blank, so existing macros keep working. Also
      added `ModemService.getMyName()` reading the station operator name
      from `STATION_CONFIG.name`, for any future caller that wants the
      local op (not the worked op).

- [x] **j-log "not implemented" alert** — `menuNewDatabase()` closed.
      Now shows a confirmation dialog and deletes every QSO for the
      current contest (plus joined `contest_qtc` rows) via
      `ContestQsoDao.deleteAllForContest()`. Reloads table + stats on
      success. Operator is told to use `File → Backup Database` first
      since there's no undo.

- [x] **j-log unknown distance-formula returns 0** —
      `ContestLogController.java:1958` closed. Now logs a warn (once per
      plugin+formula, via `warnUnknownDistanceFormula`) and falls back
      to `basePoints` so contests don't silently score zero.

User-facing — still open:

- [ ] **j-digi AX.25 transmit + WAV export not implemented** —
      `ModemService.java:524` (TX) and `:600` (WAV). `transmitterForMode`
      returns `null` for `AX25`; every other mode has a transmitter.

      **Closeout (~1-2 days):** Implement `Ax25Transmitter` (HDLC framing,
      NRZI bit-stuff, AFSK 1200/2200 Hz tones at 1200 baud) or gate AX25
      out of the mode dropdown.

- [ ] **j-learn `<!-- TODO: content -->` placeholder** —
      `jlearn.js:393`. Per-chapter content gap. Content-writing task.

Internal stubs (no user impact today):

- [ ] **j-digi LocalSkimmer Phase 2** — `dsp/LocalSkimmer.java:19`.
      Foundation tier scaffolding only; real feature, not a polish fix.
      Defer.

- [x] ~~**j-hub FCC ULS Phase 2** — operator class from AM.dat~~ — false
      positive in the audit. `FccUlsImporter.readOperatorClasses()`
      already parses AM.dat, expands the code via
      `HamDbProvider.classExpand`, and writes it to
      `callsigns.license_class`. The "Phase 2" label in line 83 is a
      section header, not a TODO.

- [ ] **j-hub antenna calculator Phase 2b** — `config.js:6380`.

Intentional / out of scope:

- **j-sat EME Phase 2** — matches the documented out-of-scope list.

## C. Dead-quiet schema fields

- [x] `RotorSection.model`, `RotorSection.comPort` — deleted.
- [x] `AmpSection.model`, `AmpSection.comPort`, `AmpSection.baud` —
      deleted (schema comment already labeled them informational).
- [x] `LoggerSection.contests`, `LoggerSection.activeContest` — deleted.
- [x] `InfoScreenSection` whole class + top-level field + dead
      `ConfigManager.getInfoScreen()` accessor — deleted.

Gson silently ignores unknown JSON fields, so existing `j-hub.json` files
with these fields still load cleanly — they just drop out on the next
save.
