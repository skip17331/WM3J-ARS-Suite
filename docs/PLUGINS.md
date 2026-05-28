# Writing Plug-ins for J-Log

J-Log has two plug-in surfaces — **contests** and **awards** — both
JSON-driven. No code, no rebuild. Drop a file in the right directory,
click *Refresh* in J-Log, and your contest or award shows up.

This guide covers both surfaces with worked examples copied from
bundled plug-ins. The canonical examples ship inside `j-log-engine.jar`
under `com/jlog/plugins/` and `com/jlog/awards/`.

---

## Where plug-ins live

| Plug-in type | User directory (yours) | Bundled fallback (read-only) |
|---|---|---|
| Contests | `~/.j-log/plugins/` | `com/jlog/plugins/` in `j-log-engine.jar` |
| Awards | `~/.j-log/awards/` | `com/jlog/awards/` in `j-log-engine.jar` |

J-Log loads bundled plug-ins first, then overlays anything in your user
directory. If your file has the same `contestId` (or `awardId`) as a
bundled one, **yours wins** — that's how you customise a bundled
contest without forking the suite.

Any `.json` file in the user directory is picked up. Name them whatever
you like; the loader keys on the `contestId` / `awardId` field, not the
filename.

---

## Part 1 — Contest plug-ins

### Quick anatomy

A contest plug-in is one JSON object with five required sections:

```jsonc
{
  "contestId":   "MACHINE_ID",      // unique key
  "contestName": "Display Name",    // shown in the contest chooser
  "exchangeFormat": "Send 599 STATE | Rcv 599 POWER",   // human-readable

  "entryFields":     [ … ],   // exchange entry bar
  "scoringRules":    { … },   // points + dupe rules
  "multiplierModel": { … },   // what counts as a multiplier
  "row2Panes":       [ … ]    // 3–4 helper panes shown below the entry bar
}
```

Optional sections cover things like Cabrillo export mapping, section
lists for Sweepstakes-style contests, locked band/mode, multi-region
station classification, and per-mode multiplier counting.

### Field reference

**Top level**

| Field | Type | Req | Purpose |
|---|---|---|---|
| `contestId` | string | | Unique key. Caching + dedup happens on this. |
| `contestName` | string | | Display name in the contest chooser. |
| `version` | string |   | Informational ("1.0.0"). |
| `exchangeFormat` | string |   | Human-readable hint shown in the UI. |
| `entryFields` | array | | The exchange entry bar — see below. |
| `scoringRules` | object | | Points + dupes + multiplier source. |
| `multiplierModel` | object | | What field holds the multiplier value. |
| `row2Panes` | array | | 3–4 helper panes. See `paneType` list below. |
| `statistics` | array |   | Statistic IDs to compute. |
| `cabrilloMapping` | object |   | Field → Cabrillo column for the exporter. |
| `sections` | array |   | Valid section list (Sweepstakes-style). |

**`entryFields[]` — exchange bar**

| Field | Values | Purpose |
|---|---|---|
| `id` | string | Machine name (`callsign`, `rst_sent`, `cq_zone`, etc.) |
| `label` | string | Display label on the entry bar |
| `type` | `text` \| `number` \| `combo` \| `checkbox` | Input widget |
| `width` | int | Suggested pixel width |
| `required` | bool | Validation: row won't save if blank |
| `autoIncrement` | bool | Auto-incrementing serial numbers |
| `options` | string[] | For `combo`: valid choices |
| `entryRow` | `0` \| `1` | 0 = received (default), 1 = sent |
| `persistent` | bool | Value survives between QSOs (band, mode, state_sent) |
| `validator` | `maidenhead` \| `numeric` \| null | Extra validation |

**`scoringRules`**

| Field | Type | Purpose |
|---|---|---|
| `pointsPerQso` | int | Base points per QSO |
| `modePoints` | map | Per-mode override, e.g. `{"CW": 3, "SSB": 1}` |
| `pointsByRegionPair` | map | `"US_DX": 5` — needs `stationClassifier` set |
| `pointsByBandClass` | map | `{"HF": 1, "VHF": 2}` |
| `pointsByBand` | map | `{"6m": 1, "70cm": 2}` |
| `scoreIsPointsOnly` | bool | No multiplier multiplication (Field Day) |
| `rookieRoundupScoring` | bool | 2 pts if ≤2 years licensed, else 1 pt |
| `multiplierType` | `sections`\|`dxcc`\|`states`\|`custom` | Multiplier source |
| `scoreFormula` | string | Expression: `qsoPoints * dxccMults`, etc. |
| `allowDupes` | bool | Dupes count for points (default false) |

**`multiplierModel`**

| Field | Type | Purpose |
|---|---|---|
| `field` | string | Entry-field `id` whose value is the multiplier |
| `validValues` | string[] | Known valid multipliers (e.g., CQ zones 01–40). Empty = accept any value. |
| `perBand` | bool | Count multipliers per band (CQ WW) or once for the contest (Sweepstakes) |

**`row2Panes[]` — supported `paneType` values**

| `paneType` | What it does |
|---|---|
| `dupe_checker` | Live dupe lookup as you type the callsign |
| `statistics` | QSO count, points, rate, multipliers |
| `dxcc_list` | DXCC entities worked, grouped by continent |
| `worked_mults` | Multipliers worked vs needed |
| `per_mode_mults` | Multipliers per mode |
| `worked_grids` | Maidenhead grids worked |
| `sweep_progress` | Sections / states grid for Sweepstakes-style |
| `us_state_map` | Real-geographic US state map |
| `canada_map` | Real-geographic Canadian provinces |
| `custom` | Free-form text card (set `config.text`) |

### Advanced fields

Most contests don't need these. Add them only when the rules call for it.

| Field | Purpose |
|---|---|
| `lockedBand` | Force band to one value (ARRL 10M) |
| `lockedMode` | Force mode to one value |
| `perModeMultipliers` | Multipliers count once per mode |
| `stationClassifier` | `"callsignRegion"` etc. — drives `conditionalFields` and `pointsByRegionPair` |
| `conditionalFields` | Show/hide fields by classifier output |
| `autoFillDxccPrefix` | Auto-fill state_rcvd with the DX prefix |
| `autoFillWpxPrefix` | CQ WPX prefix auto-fill |
| `contestWideDupe` | Any band/mode counts as a dupe (Sweepstakes) |
| `roverAwareDupe` | `/R` rovers can be re-worked per grid (VHF) |
| `usStates`, `canadaProvinces` | Multiplier lists used by `us_state_map` / `canada_map` panes |
| `multiplierList` | Classpath path to a JSON array (counties, sections, etc.) |

### Worked example — ARRL DX (CW) W/VE side

This is a working bundled plug-in, lightly trimmed:

```jsonc
{
  "contestId":   "ARRL_DX_CW_US",
  "contestName": "ARRL International DX (CW) — W/VE",
  "version":     "1.0.0",
  "exchangeFormat": "Send 599 STATE | Rcv 599 POWER",

  "entryFields": [
    { "id":"callsign",   "label":"Callsign", "type":"text",  "width":130, "required":true,  "entryRow":0 },
    { "id":"band",       "label":"Band",     "type":"combo", "width":80,  "entryRow":0, "persistent":true,
      "options":["160m","80m","40m","20m","15m","10m"] },
    { "id":"rst_sent",   "label":"RST S",    "type":"text", "width":50, "entryRow":1 },
    { "id":"rst_rcvd",   "label":"RST R",    "type":"text", "width":50, "entryRow":0, "persistent":true },
    { "id":"dxcc",       "label":"DXCC",     "type":"text", "width":70, "required":true, "entryRow":0 },
    { "id":"power_rcvd", "label":"Pwr R",    "type":"text", "width":60, "entryRow":0 },
    { "id":"state_sent", "label":"State S",  "type":"text", "width":55, "entryRow":1, "persistent":true },
    { "id":"mode",       "label":"Mode",     "type":"combo","width":55, "entryRow":1, "persistent":true,
      "options":["CW"] }
  ],

  "scoringRules": {
    "pointsPerQso":     3,
    "multiplierType":   "dxcc",
    "scoreFormula":     "qsoPoints * dxccMults",
    "allowDupes":       false
  },
  "multiplierModel": {
    "field":       "dxcc",
    "perBand":     true,
    "validValues": []
  },

  "row2Panes": [
    { "paneIndex":1, "paneType":"dupe_checker", "title":"Dupe Checker" },
    { "paneIndex":2, "paneType":"statistics",   "title":"Statistics"   }
  ],

  "statistics": ["qso_count","total_score","multipliers","qso_per_hour","dxcc_count"],

  "cabrilloMapping": {
    "rst_sent": "sent_1_rst",
    "field3":   "sent_2_state",
    "rst_rcvd": "rcvd_1_rst",
    "field2":   "rcvd_2_power"
  }
}
```

### Where to find more examples

Browse `j-log-engine/src/main/resources/com/jlog/plugins/`. Notable
reference contests:

- `cq_ww_cw.json` — zone + DXCC, per-band multipliers
- `arrl_dx_cw_us.json` — DXCC mult, region-pair points (above)
- `iaru_hf.json` — zone/HQ custom multiplier, auto-fill DXCC prefix
- `arrl_sweepstakes_cw.json` — sections, contest-wide dupes
- `arrl_field_day.json` — `scoreIsPointsOnly`, station classifier

### Testing your contest plug-in

1. Drop the file in `~/.j-log/plugins/`.
2. Restart J-Log (or use **File → Import Plugin…** from inside J-Log).
3. Switch to **Contest** mode at the splash screen; your contest should
   appear in the chooser keyed by `contestName`.
4. Score a couple of dummy QSOs and watch the statistics pane —
   `scoreFormula` errors show in the bottom status bar.

---

## Part 2 — Award plug-ins

Awards are simpler than contests. A plug-in declares **what to match
against in each QSO** (state, callsign, DXCC prefix, …) and either a
**target list** (set-match award like WAS) or no list at all
(count-match award like WPX).

### Quick anatomy

```jsonc
{
  "awardId":     "MACHINE_ID",
  "awardName":   "Display Name",
  "description": "Longer description shown on the awards dashboard.",
  "matchOn":     "state",
  "targetLabel": "States",

  "targets": [ { "id": "AL", "label": "Alabama" }, … ],
  "tiers":   [ { "threshold": 10, "name": "10 states" }, … ]
}
```

### Field reference

**Top level**

| Field | Type | Req | Purpose |
|---|---|---|---|
| `awardId` | string | | Unique key. |
| `awardName` | string | | Display name. |
| `description` | string |   | Detailed description shown in the dashboard's Details view. |
| `matchOn` | string | | Which QSO field to match: `state` \| `country` \| `callsign` \| `prefix` \| `dxccPrefix` \| `continent` |
| `targetLabel` | string |   | Display label for the progress axis (`"States"`, `"Prefixes"`, …) |
| `targets` | array | conditional | Predeclared target list (set-match). Omit for count-match awards. |
| `bonus` | array |   | Optional bonus targets that appear separately (13 Colonies). |
| `tiers` | array |   | Achievement thresholds (e.g. DXCC 25 / 50 / 75 / 100). |
| `window` | object |   | Time window for special-event awards (13 Colonies). |
| `options` | object |   | Matching behaviour switches. |

**`targets[]` / `bonus[]`**

```jsonc
{ "id": "AL", "label": "Alabama" }
```

- `id` — the actual value compared against `matchOn` (state code,
  callsign, DXCC prefix, …)
- `label` — human-readable name shown in the dashboard

**`tiers[]`**

```jsonc
{ "threshold": 100, "name": "DXCC (100)" }
```

- `threshold` — number of distinct targets needed
- `name` — display name for the tier

**`window`** — for special-event awards only:

```jsonc
"window": {
  "startUtc": "2026-07-01T13:00:00Z",
  "endUtc":   "2026-07-08T05:00:00Z"
}
```

Only QSOs inside the window count toward the award.

**`options`**

| Field | Purpose |
|---|---|
| `matchBaseCallsign` | Strip `/P`, `/M`, `/R`, `/QRP` etc. before matching |
| `confirmedOnly` | Only count QSL-confirmed QSOs |

### Award types

- **Set-match award** — `targets` is populated. Progress = number of
  declared targets covered. (WAS, DXCC, 13 Colonies, IOTA.)
- **Count-match award** — `targets` is empty. Progress = number of
  distinct values found. (WPX = unique prefixes; Maidenhead grid awards.)

### Worked example — Worked All States

```jsonc
{
  "awardId":     "WAS",
  "awardName":   "Worked All States",
  "description": "Work and confirm QSOs with amateurs in all 50 United States.",
  "matchOn":     "state",
  "targetLabel": "States",

  "targets": [
    {"id":"AL","label":"Alabama"}, {"id":"AK","label":"Alaska"},
    {"id":"AZ","label":"Arizona"}, /* … 47 more … */
    {"id":"WY","label":"Wyoming"}
  ],

  "tiers": [
    {"threshold":10, "name":"10 states"},
    {"threshold":25, "name":"25 states"},
    {"threshold":40, "name":"40 states"},
    {"threshold":50, "name":"WAS (50 states)"}
  ]
}
```

### Worked example — 13 Colonies (special-event, with bonus + window)

```jsonc
{
  "awardId":     "13_COLONIES",
  "awardName":   "13 Colonies Special Event",
  "description": "Work each of the 13 Colony stations K2A–K2M during Independence Day week.",
  "matchOn":     "callsign",
  "targetLabel": "Colony stations",

  "window": {
    "startUtc": "2026-07-01T13:00:00",
    "endUtc":   "2026-07-08T05:00:00"
  },

  "targets": [
    {"id":"K2A","label":"New York"}, {"id":"K2B","label":"Virginia"},
    /* … through K2M … */
  ],

  "bonus": [
    {"id":"WM3PEN", "label":"Pennsylvania bonus"},
    {"id":"GB13COL","label":"UK bonus"},
    {"id":"TM13COL","label":"France bonus"}
  ]
}
```

### Where to find more examples

Browse `j-log-engine/src/main/resources/com/jlog/awards/`. Notable
reference awards:

- `dxcc.json` — 217 entities, `matchOn: dxccPrefix`, tiered
- `was.json` — 50 states, `matchOn: state` (above)
- `colonies_13.json` — set-match with bonus + window (above)
- `wpx.json` — count-match (no `targets`), `matchOn: prefix`

### Testing your award plug-in

1. Drop the file in `~/.j-log/awards/`.
2. In J-Log Normal mode, open **Awards Dashboard** and click **Refresh**.
3. Your award appears with progress counts derived from the current log
   database.

---

## Versioning and sharing

- Bump `version` whenever you change scoring rules or targets — j-log
  doesn't enforce it, but other operators downloading your plug-in
  appreciate it.
- Plug-ins are plain JSON; share via gist, GitHub, the reflector, or
  the upcoming **community plug-in registry** (Phase 2 of the
  [roadmap](ROADMAP.md)).
- If you write a plug-in for a contest or award that ships globally and
  isn't bundled yet, open a PR against `j-log-engine` to add it to the
  bundled set — that way every operator gets it for free.

## When to drop down to code

Plug-ins handle every contest and award the suite has shipped to date.
You only need to write Java if you're adding:

- A new `paneType` for `row2Panes` (e.g., a new specialty multiplier
  view)
- A new `matchOn` source for awards (e.g., satellite-pass matching)
- A new statistic computation
- A new `scoreFormula` operator

That's a `j-log-engine` change, not a plug-in. Open an issue first to
talk through the design.
