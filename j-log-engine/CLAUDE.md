# CLAUDE.md — j-Log Engine

Shared logging engine for the WM3j ARS Suite. No JavaFX dependency.

## Build

```bash
./build.sh
# or
mvn clean install -DskipTests
```

This installs `j-log-engine-1.0.0.jar` to the local Maven repo. **Must be built before j-log, j-digi, or j-bridge.**

## What's Here

| Package | Responsibility |
|---------|---------------|
| `com.jlog.db` | DatabaseManager, QsoDao, ContestQsoDao, MacroDao — SQLite in `~/.j-log/` |
| `com.jlog.cluster` | HubEngine, HubDiscoveryListener — WebSocket client to j-hub |
| `com.jlog.plugin` | PluginLoader, ContestPlugin — JSON contest definitions |
| `com.jlog.scoring` | ContestScorer, DxccResolver, distance scoring |
| `com.jlog.export` | AdifExporter, CabrilloExporter, AdifImporter, ColoniesLogSheetPdf |
| `com.jlog.award` | award trackers (JSON-backed) |
| `com.jlog.util` | LoggingConfigurator, AppConfig, QrzLookup, MacroVariableEngine, Maidenhead |

## What's in j-log-common (not here)

The DB-free primitives were split into the sibling **`j-log-common`** module
(`com.jlog.common`) so non-logging consumers (j-bridge needs only the bandplan)
don't drag in SQLite/scoring/plugins/PDFBox. The engine `requires transitive
com.jlog.common`, so engine consumers still see these through us unchanged:

- `com.jlog.model` — DxSpot, QsoRecord, Macro, StationInfo
- `com.jlog.bandplan` — BandplanLoader + IARU R1/R2/R3 / per-country overlay
- `com.jlog.civ` — CivEngine, CivConfig (Icom CI-V over serial, JSSC)
- `com.jlog.i18n` — I18n resource-bundle loader
- `com.jlog.support` — IssueReporter (GitHub-issue browser launcher)

## What's NOT Here

- `com.jlog.app` — JavaFX Application entry point (j-log)
- `com.jlog.controller` — JavaFX UI controllers (j-log)
- `com.jlog.macro` — MacroEngine stays in j-log because it uses `AudioClip` and `Platform.runLater`

## Module

JPMS named module: `com.jlog.engine` (requires transitive `com.jlog.common`).

Consumers that use JPMS must `requires com.jlog.engine` in their `module-info.java`.
**Build order:** `j-log-common` must be installed before `j-log-engine`.
Consumers that don't use JPMS (j-digi, j-bridge) simply add the Maven dependency.

## Data Storage

All runtime data lives in `~/.j-log/` (shared with the j-log UI app):
- `j-log.db` — normal log QSOs
- `contest.db` — contest QSOs
- `config.db` — key/value config, macros, DX network profiles
