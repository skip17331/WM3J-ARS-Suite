# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Build fat jar (skips tests — there are none)
./build.sh
# or
mvn clean package -DskipTests

# Run (expects JavaFX runtime in ./lib/javafx)
./run.sh
# or manually
java --module-path ./lib/javafx \
     --add-modules javafx.controls,javafx.fxml,javafx.media \
     -Dfile.encoding=UTF-8 \
     -jar target/j-log-1.0.0-shaded.jar
```

The shaded jar (`target/j-log-1.0.0-shaded.jar`) bundles all dependencies except JavaFX, which must be on the module path at runtime.

## Architecture Overview

J-Log is a Java 21 + JavaFX 21 desktop app using a standard MVC layout with a named module (`com.jlog.app`).

**Startup sequence** (`JLogApp.init` → `start`):
1. `AppConfig.load()` — reads Java `Preferences` (theme, language, mode)
2. `LoggingConfigurator` — sets Logback level (INFO or DEBUG)
3. `I18n.load()` — loads the appropriate `messages_<lang>.properties`
4. `DatabaseManager.initAll()` — opens/creates three SQLite DBs in `~/.j-log/`
5. `SplashScreen` — mode chooser (Normal vs Contest); launches the selected window

**Two operating modes share no UI but share the same singleton services:**
- *Normal mode*: `NormalLogController` + `NormalLog.fxml` → `QsoDao` → `j-log.db`
- *Contest mode*: `ContestLogController` + `ContestLog.fxml` → `ContestQsoDao` → `contest.db`; contest definition loaded from a `ContestPlugin` JSON

**Key singletons** (all use `getInstance()`):
| Singleton | Responsibility |
|-----------|---------------|
| `DatabaseManager` | Opens/closes the three SQLite connections; provides `getConfig`/`setConfig` for key-value config stored in `config.db` |
| `AppConfig` | Thin façade — small prefs (theme, lang, window geometry) via Java `Preferences`; station/CI-V config delegated to `DatabaseManager.getConfig` |
| `CivEngine` | Bidirectional Icom CI-V over serial (JSSC); 500 ms poll + unsolicited frame handling |
| `HubEngine` | WebSocket hub connection; delivers enriched `DxSpot` objects via listener |

**Contest plugin system**: JSON files define the entire contest UI (fields, scoring, multiplier model, Cabrillo mapping). `PluginLoader` discovers them from the JAR resources first, then `~/.j-log/plugins/`. `ContestPlugin` is a Jackson-mapped POJO — adding fields to the JSON schema requires updating `ContestPlugin.java` and its inner classes.

### Group / multi-region QSO party pattern

Multi-state/province QSO parties (7QP, NEQP, CPQP) get a county/region
**list side-column** (worked-colored), an optional **combined geographic
map** (`Contest → County Map`), and an optional **block tile map** under
the list. The cockpit infra is generic — **adding a new one is data +
plugin JSON only, zero Java**:

1. **Bake the source**: `j-log-engine/src/main/resources/com/jlog/counties/<name>.json`
   = `[{"c":<code>,"s":<state/prov>,"n":<name>}, …]`, from an
   authoritative source — `~/.fldigi/data/<X>.txt` where it exists
   (`7QP.txt`, `NEQP.txt`, …; CSV `State,ST,County,CC`), else
   operator-supplied. **Verify, never fabricate** these (contest-scoring
   critical; the operator will catch errors). `<code>` MUST equal what
   the operator logs in `state_prov_rcvd`.
2. **(Optional) geographic map**: add `<name>` to `COMBINED` in
   `tools/county-map/build_counties.py` (pure-stdlib; replaces the
   retired geopandas pipeline; `tools/county-map/data/` is a gitignored
   reproducible download), then `python3 tools/county-map/build_counties.py
   <NAME>` → `j-log/src/main/resources/com/jlog/maps/county-<name>.json`.
   Fully verify (0 name/state mismatches, per-state counts).
3. **Wire the plugin**: add to `row2Panes`
   `{"paneType":"county_list","placement":"column","config":{"dataset":"<name>","blockMap":<true|false>}}`.

Generic infra (no per-party Java): `ContestLogController.buildCountyListPane`,
`countyDataset()`, `menuCountyMap()`, `countyBlockMap`. List labels
register in `sectionLabels` so the worked-mult refresh greens
list + map + block grid together (`sectionLabels` is cleared once in
`buildRow2Panes()`; `buildSectionPane`/`buildCountyListPane` only
*add*, so a plugin may safely declare both a `section_tracker` and a
`county_list`). `blockMap:true` appends a
`RegionMapPane` tile grid under the list — use when vector polygons
aren't obtainable (CPQP).

**Code-key gotchas (verify per party):** 7QP = state-first 5-char
(`ORDES`); 7QP's WA codes differ from the WA Salmon Run codes — keep
7QP's namespace isolated, never reuse the per-state `county-XX.json`.
NEQP's plugin exchange is state-first (`MAWOR`) though `NEQP.txt`'s CC
is county-first (`FAICT`) — build keys as `ST+county3`. CPQP = bare
3-letter FED code, no province prefix.

**Not every "group" party fits**: the ARC (Amateur Radio Club) QSO
Party has no geographic/finite multiplier (mult = distinct worked
club-member callsigns) — leave it on `worked_mults`; do not add a
hollow list pane.

**Macro system**: `Macro` objects (stored in `config.db` as JSON action arrays) are executed by `MacroEngine`, which dispatches each `MacroAction` type (PTT, CW, VOICE_PLAY, etc.) against `CivEngine` or JavaFX `AudioClip`. Macros are bound to F1–F12.

**Theme/CSS**: `JLogApp.applyTheme(scene)` applies `base.css` + either `light.css` or `dark.css` to any `Scene`. Call this whenever a new window is created.

**i18n**: All user-visible strings go through `I18n.get("key")`. Keys live in `src/main/resources/com/jlog/i18n/messages*.properties`.

## Data Storage

All runtime data lives in `~/.j-log/`:
- `j-log.db` — normal log QSOs
- `contest.db` — contest QSOs (includes `contest_id`, `is_dupe`, `field1`–`field5`)
- `config.db` — key/value app config, macros (as JSON), DX network profiles
- `plugins/` — user-installed contest plugin JSON files
- `logs/j-log.log` — daily-rotating log (7 days)

## Module System Notes

The project uses the JPMS named module `com.jlog.app`. The `module-info.java` `opens` directives are required for JavaFX FXML injection and Jackson deserialization — do not remove them when adding new packages that need reflection.
