# WM3J ARS Suite — Plugin Format Reference

The authoritative schema for the suite's **contest** and **award** plugins, extracted
from the `j-log-engine` source. This is the contract a plugin-authoring tool generates
its forms and validators from, and the reference for hand-authoring a plugin.

> **Verify against source.** Every claim here was read out of `j-log-engine` (and, for
> contest *scoring*, `j-log`). Class/line cites are given so they can be re-checked.
> Parsing is **Jackson** with `@JsonIgnoreProperties(ignoreUnknown=true)` on every model
> — unknown keys are silently dropped and there is **no schema validation** beyond type
> binding and a non-blank-id check on import. The authoring tool *is* the validation layer.

---

## 0. Where plugins live & how they load

| | Contest | Award |
|---|---|---|
| Model class | `com.jlog.plugin.ContestPlugin` | `com.jlog.award.AwardPlugin` |
| Loader | `com.jlog.plugin.PluginLoader` | `com.jlog.award.AwardLoader` |
| Bundled resources | `j-log-engine` `/com/jlog/plugins/*.json` (~86) | `/com/jlog/awards/*.json` (5) |
| **User drop-in dir (auto-loaded)** | `~/.j-log/plugins/` | `~/.j-log/awards/` |
| Identity key | `contestId` (non-blank required on import) | `awardId` (non-blank required on import) |
| User overrides bundled? | Yes — same id replaces bundled | Yes — same id replaces bundled |

**Authoring-tool target:** write JSON into the **user drop-in dir**. The suite loads it on
next launch (contest) / Refresh (award) with **no source change**. The hard-coded
`PluginLoader.bundled[]` array only matters for plugins shipped *in the suite source* —
out of scope for an external authoring tool.

**Loader error behavior:** a malformed/unparseable plugin is `log.warn`-ed and skipped; it
does not abort startup and is not surfaced in the UI. So a bad plugin silently fails to
appear — another reason the tool must validate before export.

---

## 1. Engine vs. app split — critical for an external tool

| Capability | Lives in | Reusable from an engine-only dependency? |
|---|---|---|
| Parse/serialize `ContestPlugin` / `AwardPlugin` | `j-log-engine` | ✅ |
| `PluginLoader` / `AwardLoader` (load, import, dedup) | `j-log-engine` | ✅ |
| **Cabrillo export** (`CabrilloExporter`) incl. `CONTEST:` + exchange mapping | `j-log-engine` | ✅ |
| **Award progress** (`AwardService`, `AwardProgress`) | `j-log-engine` | ✅ |
| Dupe SQL primitives (`ContestQsoDao.isDuplicate*`) | `j-log-engine` | ✅ |
| **Contest scoring / multiplier-count / dupe dispatch** | **`j-log` `ContestLogController` + `com.jlog.scoring.*`** | ❌ not in engine |
| Map/section render panes, 13-Colonies call→state map | `j-log` UI | ❌ |

**Implication for the builder:** structural validation, Cabrillo preview, and *award*
score preview work off `j-log-engine` alone. A *contest* score/multiplier/dupe preview
needs either (a) a dependency on `j-log`, or (b) lifting the scoring dispatch out of
`ContestLogController` into the engine (the cleaner long-term move — it would also make the
scoring unit-testable in isolation). Decide this before promising "live score preview."

---

## 2. Contest plugin — top-level fields

`ContestPlugin.java`. Booleans default `false`, ints `0`, objects/lists `null`.

### Identity
| Key | Type | Notes |
|---|---|---|
| `contestId` | String | **Required** (non-blank on import). Machine id. Cabrillo `CONTEST:` fallback = `contestId.replace("_","-")`. |
| `contestName` | String | Display name. |
| `version` | String | Emitted as `X-JLOG-PLUGIN:` in Cabrillo. |
| `cabrilloContestName` | String | Overrides the `CONTEST:` header when set — use it whenever the sponsor token ≠ derived id (e.g. `ARRL-RR-PH`, `ARRL-DX-SSB`). |

### Exchange / entry
| Key | Type | Notes |
|---|---|---|
| `entryFields` | `FieldDef[]` | Row-1/2 entry bar — see §3. |
| `exchangeFormat` | String | Human hint shown under the entry bar; also broadcast to j-digi on `CONTEST_ACTIVE`. |
| `lockedBand` | String | Forces + read-onlys the band field. |
| `lockedMode` | String | Forces + read-onlys the mode field. |
| `stationClassifier` | String | Only `"callsignRegion"` is recognized; enables `conditionalFields`. |
| `conditionalFields` | `ConditionalField[]` | `{fieldId, showForRegions[], hideForRegions[]}` (regions e.g. `US`,`CA`,`DX`). |

### Multiplier
| Key | Type | Notes |
|---|---|---|
| `multiplierModel` | object | `{field, validValues[], perBand}` — `field` = the entry-field id holding the mult value; `perBand` counts it per band. |
| `multiplierList` | String | Classpath resource (JSON string-array) seeding the `worked_mults` pane universe. |
| `usStates` / `canadaProvinces` | String[] | Added to the multiplier universe. |
| `perModeMultipliers` | bool | Multipliers count once **per mode**; unfolds CW/Phone in claimed score; also reused as a per-mode dupe key (ARRL 10M). |
| `autoFillDxccPrefix` | bool | Auto-fills state/prov-rcvd with a DXCC-prefix proxy for DX. **Gotcha:** can corrupt Cabrillo when a column is dual-mapped — verify exchange output. |
| `autoFillWpxPrefix` | bool | Auto-fills `prefix_rcvd` with the WPX prefix (CQ WPX). |

### Scoring
| Key | Type | Notes |
|---|---|---|
| `scoringRules` | object | See §5. |

### Dupe (mutually-considered in this order)
| Key | Type | Dupe key |
|---|---|---|
| `contestWideDupe` | bool | callsign only (any band/mode) — Sweepstakes |
| `roverAwareDupe` | bool | `/R` calls: call+band+grid; else call+band |
| `perBandGridDupe` | bool | every call: call+band+grid — 10 GHz & Up |
| `fieldDayModeDupe` | bool | call+band+mode-CATEGORY (CW/PH/DG) |
| *(none of the above)* | — | call+band+mode (default) |

`multiplierType:"qso_party"` uses its own call+band+mode-class+county dupe.

### Cabrillo mapping
| Key | Type | Notes |
|---|---|---|
| `cabrilloSent` | String[] | Ordered tokens for the sent exchange (space-joined). |
| `cabrilloRcvd` | String[] | Ordered tokens for the rcvd exchange. |

Tokens are normally **entry-field ids**; special tokens resolve directly: `callsign`,
`serial_sent`, `serial_rcvd`, `rst_sent`, `rst_rcvd`, `mycall`, `band`, `mode`,
`field1`–`field5`, `prec_sent`, `check_sent`, `sect_sent`. A field marked `constant:true`
resolves from station config, not a per-QSO slot.

### UI / sections
| Key | Type | Notes |
|---|---|---|
| `row2Panes` | `PaneDef[]` | Trackers/maps/stats strip — see §4. |
| `sections` | String[] | Section labels for the flat section grid + `sweep_progress` total. |

---

## 3. `FieldDef` (entryFields[])

| Property | Type | Notes |
|---|---|---|
| `id` | String | Machine name. These ids consume **no** field1–5 DB slot: `callsign, serial_sent, serial_rcvd, band, mode, rst_sent, rst_rcvd, prec_sent, check_sent, sect_sent`. All other ids map to `field1..5` in declaration order (max 5). `callsign` is auto-uppercased/space-stripped. |
| `label` | String | Display label. |
| `type` | String | **Only `"combo"` changes behavior** (→ ComboBox, needs `options`). `text`/`number`/`checkbox` all render a plain TextField. |
| `width` | int | Pixel width (`>0`, else 100). |
| `options` | String[] | Combo items; also validates a text field (red ring on mismatch). |
| `entryRow` | int | `0` = received row, `1` = sent row (Tab order / layout / clear). |
| `autoIncrement` | bool | Serial fields: prefilled + non-editable. |
| `persistent` | bool | Value survives Clear. |
| `constant` | bool | Operator-constant sent exchange — no per-QSO slot; Cabrillo resolves from station config. |
| `validator` | String | One of `maidenhead`, `maidenhead6`, `numeric`, `fd_class`, `ss_check`. Unknown → ignored. |
| `required` | bool | **Vestigial** — getter exists, no enforcement. |

---

## 4. `PaneDef` (row2Panes[])

`{paneType, title, placement, config}` — `placement` `"column"` = tall right column, else
horizontal strip. `paneIndex` is **vestigial** (order = list order).

**Valid `paneType` values:** `dupe_checker`, `section_tracker`, `statistics`,
`us_state_map`, `canada_map`, `dxcc_list`, `dxcc_map`, `county_map`, `county_list`,
`per_mode_mult_grid`, `worked_before`, `grid_map`, `qtc`, `ss_section_map`,
`sweep_progress`, `worked_mults`. Unknown → bare placeholder label.

**Per-pane `config` keys:**
- `section_tracker` → `zoneGroups: [{name, sections[]}]` (else falls back to top-level `sections`).
- `county_map` → `dataset` (String, default `"7qp"` → `county-<dataset>.json`).
- `county_list` → `dataset` + `blockMap` (bool). All other panes read no config.

---

## 5. `ScoringRules`

| Key | Type | Notes |
|---|---|---|
| `pointsPerQso` | int | Default per-QSO points. |
| `modePoints` | Map<mode,int> | Per-mode override. |
| `pointsByRegionPair` | Map | Key `"{my}_{their}"` of `US`/`CA`/`DX`, e.g. `"US_DX":5`. |
| `pointsByBand` | Map<band,int> | Exact band; precedence over class. |
| `pointsByBandClass` | Map | `"HF"`/`"VHF"` (160m–10m = HF, 6m+ = VHF). |
| `scoreIsPointsOnly` | bool | Score = points, no multiplier (Field Day). |
| `rookieRoundupScoring` | bool | 2 pts if rookie (licence-year delta ≤3), else 1. |
| `perModeMultipliers` | *(top-level, see §2)* | — |
| `distanceScoring` | object | See below. |
| `multiplierType` | String | See §6. |
| `qsoParty` | object | Large sub-schema, active only when `multiplierType:"qso_party"` (§7). |
| `scoreFormula` | String | **Vestigial** — never read; score is always `points × mults`. |
| `allowDupes` | bool | **Vestigial.** |

**`distanceScoring`** (claimed/running score; sponsor re-adjudicates):
`{theirGridField (default grid_rcvd), ownGridField (else station grid), formula, minKm,
bandFactor: Map<band,int>, divisorKm, basePoints, minDistancePoints}`. `formula` ∈
`km_x_bandfactor` (`round(km)×bandFactor[band]`), `one_plus_ceil_km_div`
(`base + max(min, ceil(km/div))`, default div 500), `one_plus_floor_km_div` (default div
3000). Unknown formula → warns + falls back to `basePoints`.

---

## 6. `multiplierType` catalog

Dispatched by two `equals()` ladders in `ContestLogController` (**not** the engine). The
13 values with dedicated logic:

| Value | What it computes |
|---|---|
| `zone_country` | CQ WW CW/SSB — continent-pair pts; per-band zone + country dual mult |
| `zone_country_state` | CQ WW RTTY — adds W/VE state (triple mult) |
| `wpx_prefix` | CQ WPX — band-group continent-pair pts; contest-wide distinct WPX-prefix mult |
| `state_prov_country` | CQ 160M — asymmetric pts; per-band state/prov + DXCC mult |
| `grid_field` | Maidenhead grid-field mult (VHF / WW Digi) |
| `wae` | WAE-DC — band-weighted asymmetric mult + QTC subsystem |
| `qso_party` | State/province QSO-party engine — see §7 |
| `all_asian` | All Asian DX |
| `russian_dx` | Russian DX (SRR) |
| `sac` | Scandinavian Activity |
| `ari_dx` | ARI DX (Italy) |
| `wag` | Worked All Germany |
| `oceania_dx` | Oceania DX |

**Generic / documentary only** (`dxcc`, `sections`, `states`, `custom`, absent): no
dedicated branch — all use the default `points × distinct(multiplierModel.field)` path.
Differentiation comes from `multiplierModel`, `multiplierList`, `usStates`/`canadaProvinces`,
the autofill flags, and the panes — not the string.

---

## 7. `qsoParty` sub-schema (when `multiplierType:"qso_party"`)

~50 fields; consumed via the `com.jlog.scoring.QsoParty` helper. Grouped:

- **County/state detection:** `stateName`, `stateAbbr`, `inStateCounties[]`,
  `countyByExclusion`, `countyCodeLen`, `areaStatePrefixLen` (multi-state areas like 7QP).
- **Points:** `pointsByModeClass{PH,CW,RY,DG}` + `pointsByModeClassOut` (in/out asymmetry),
  `qsoPointCalls{call→pts}`, `pointsAllQsos`, `ptsInToIn`/`ptsInToOut`/`ptsOutToIn`,
  `ptsWorkedInState`/`ptsWorkedOutState`.
- **Mult scope/dimensions:** `multScope`/`multScopeOut` (`per_mode`|`per_band`|`once`),
  `inStateCountsStates`/`…CountsDxccEach`/`…CountsCounties`/`…OwnStateMult`/`…SelfStateMult`/`…NoDxMult`,
  `multsAllEntrants`, `clubMemberMult` + `clubMultCalls[]`, `multCap`.
- **Mode/grid:** `mergeRttyDigital`, `mergeCwDigital`, `inStateGrids[]`, `ft8GridDivisor`,
  `outStateGridCap`/`outStateGridUncapped`, `gridDivisorCeil`.
- **Bonus/sweep:** `bonusStations`/`bonusStationsOnce`/`bonusStationsPerMode`,
  `bonusPointCalls[]`, `rareCounties[]`+`rareQsoMultiplier`,
  `sweepBonusPoints`/`Threshold` (+ a second tier `…2`).

Score: `qpTotal × qpMults + bonusPts` (bonus added post-multiply).

---

## 8. Award plugin schema

`AwardPlugin.java` — much simpler than contest plugins; **fully in `j-log-engine`** (loader,
model, and progress computation are all reusable). Operates on the **normal log only**
(`j-log.db`); credits are de-duplicated into a set (one credit per distinct value — no
per-band/per-mode crediting).

| Key | Type | Notes |
|---|---|---|
| `awardId` | String | **Required** (non-blank on import). |
| `awardName` | String | Card title. |
| `description` | String | Details header. |
| `matchOn` | String | QSO field to extract. Recognized: `state`, `country`, `callsign`, `prefix` (WPX), `dxccPrefix`, `continent`, `grid` (⚠ reads the *notes* column — no real grid field). Unknown → null. |
| `targetLabel` | String | Count-axis label ("States"). |
| `targets` | `Target[]` | `{id, label}`. Presence makes it a **set-match** award (progress = covered targets). |
| `bonus` | `Target[]` | Extra targets counted on top. |
| `tiers` | `Tier[]` | `{threshold:int, name}`. `progressRatio` is against the **top** tier. |
| `window` | object | `{startUtc, endUtc}` ISO-8601 time gate (trailing `Z` stripped). |
| `options` | object | `{matchBaseCallsign (strip /P,/M…), confirmedOnly (require QSL received)}`. |

**Set-match vs count-match:** non-empty `targets` ⇒ set-match (total = targets+bonus). Empty
`targets` ⇒ count-match (e.g. WPX): progress = distinct worked values, total = top tier
threshold.

**Caveats to surface in a builder:** `continentOf()` is a hand-rolled country-name keyword
list (approximate, not a real DXCC table); `matchOn:grid` reads the notes column;
13-Colonies' callsign→state map and date window are hard-coded in j-log UI, not the JSON —
so that style of award isn't fully expressible in the schema today.

---

## 9. Fields a builder should NOT emit (vestigial / deprecated)

`ScoringRules.scoreFormula`, `ScoringRules.allowDupes`, `ContestPlugin.statistics`,
`PaneDef.paneIndex`, `FieldDef.required`. Present with getters but no consumer.

Also: `FieldDef.type` only branches on `"combo"`; emit `text`/`combo` and treat
`number`/`checkbox` as `text` (they render identically today). The
`ScoringRules.multiplierType` doc-comment in source is stale (lists 5; 13 are real).
