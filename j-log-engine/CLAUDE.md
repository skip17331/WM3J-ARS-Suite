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
| `com.jlog.civ` | CivEngine, CivConfig — Icom CI-V over serial (JSSC) |
| `com.jlog.cluster` | HubEngine, HubDiscoveryListener — WebSocket client to j-hub |
| `com.jlog.model` | DxSpot, QsoRecord, Macro, StationInfo — shared data objects |
| `com.jlog.plugin` | PluginLoader, ContestPlugin — JSON contest definitions |
| `com.jlog.export` | AdifExporter, CabrilloExporter |
| `com.jlog.util` | LoggingConfigurator, AppConfig, QrzLookup |
| `com.jlog.i18n` | I18n — resource bundle loader |

## What's NOT Here

- `com.jlog.app` — JavaFX Application entry point (j-log)
- `com.jlog.controller` — JavaFX UI controllers (j-log)
- `com.jlog.macro` — MacroEngine stays in j-log because it uses `AudioClip` and `Platform.runLater`

## Module

JPMS named module: `com.jlog.engine`

Consumers that use JPMS must `requires com.jlog.engine` in their `module-info.java`.
Consumers that don't use JPMS (j-digi, j-bridge) simply add the Maven dependency.

## Data Storage

All runtime data lives in `~/.j-log/` (shared with the j-log UI app):
- `j-log.db` — normal log QSOs
- `contest.db` — contest QSOs
- `config.db` — key/value config, macros, DX network profiles
