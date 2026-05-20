# ARS Suite — Stubs & Unfinished Features

Audit captured 2026-05-20; closeout passes same day. Order is rough user-impact
priority. Items closed are noted with the relevant commit / change.

## A. UI fields users fill in that no Java code reads

- [x] **Rig: CI-V Direct backend** — closed in `f227aa1`.
  ~~Rotor: shortPathOffset / customPreset~~ — false positive (consumed
  client-side at `config.js:rotorPreset()`).
- [x] **Appearance: fontSize / waterfallColor** — schema & default deleted
      (`172f6a3`).
- [x] **Logger: dbPath / mode** — dead "Log Database" card removed from
      Logging tab; `saveLogging()`, populate code, and `LoggerSection`
      schema all deleted. Log Uploaders and Cloud Backup cards stay.
      Closed in this pass.

## B. Explicit "not implemented" markers

Closed:

- [x] **j-digi `{NAME}` macro = "OM" placeholder** — wired to
      `LogEntryPane.getName()` (`172f6a3`).
- [x] **j-log "not implemented" alert** — `menuNewDatabase()` now wipes
      the current contest's QSOs after confirmation (`172f6a3`).
- [x] **j-log unknown distance-formula returns 0** — now logs warn +
      falls back to `basePoints` (`172f6a3`).
- [x] **j-digi AX.25 TX + WAV export** — **permanently parked**, not
      built. Real packet TX needs a TNC / direwolf between soundcard
      and radio, out of j-digi's scope. Messages now read "AX.25 is
      decode-only in j-digi" instead of "not implemented yet";
      `transmitterForMode` comment explains the decision so future
      audits don't re-flag it. See memory entry
      `feedback_ax25_tx_parked`.
- [x] **j-hub antenna calculator Phase 2b** — false positive. Every
      entry in `AW_ANTENNAS` (11) already has a matching `AW_CALCS`
      entry, so the "Coming soon" branch and the "Phase 2b" message
      are unreachable. Replaced the misleading "coming in a follow-up
      commit" string with a defensive "calculator not defined"
      fallback.
- [x] ~~**j-hub FCC ULS Phase 2** — operator class from AM.dat~~ — false
      positive (already implemented).
- [x] ~~**j-learn `<!-- TODO: content -->` placeholder**~~ — false positive.
      `jlearn.js:392` is a defensive renderer for a marker that no
      shipping chapter uses (verified: 0 markers across 297 markdown
      files in 31 chapters). Comment updated to explain.
- [x] ~~**j-digi LocalSkimmer Phase 2**~~ — false positive. The full
      multi-carrier-decode + callsign-scoring + spot-publish pipeline
      shipped (`MultiCarrierDecoder`, `CallsignScorer`,
      `SkimmerSpotPublisher`). Stale Javadoc updated.

Intentional / out of scope:

- **j-sat EME Phase 1+2** — plan approved 2026-05-20, implementation
  on hold. Phases 3+ (Doppler tuning, WSJT-X UDP, sky temperature,
  sked board) still on the roadmap but not planned in detail.
- **j-digi AX.25 TX** — permanently parked, see
  `feedback_ax25_tx_parked` memory.

## C. Dead-quiet schema fields

All closed in `172f6a3` + this pass:

- [x] `RotorSection.model`, `RotorSection.comPort`
- [x] `AmpSection.model`, `AmpSection.comPort`, `AmpSection.baud`
- [x] `LoggerSection` entire class (+ `ConfigManager.getLogger()`)
- [x] `InfoScreenSection` entire class (+ `ConfigManager.getInfoScreen()`)
- [x] `AppearanceSection.fontSize`, `AppearanceSection.waterfallColor`

Gson silently ignores unknown JSON fields, so existing `j-hub.json` files
with these fields still load cleanly — they just drop out on the next save.
