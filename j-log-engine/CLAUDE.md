# CLAUDE.md — j-Log Engine

Shared logging engine for the WM3J ARS Suite. No JavaFX dependency. Consumed by `ars-fx`, `j-digi` and `j-bridge`.

## Build

```bash
./build.sh
# or
mvn clean install -DskipTests
```

This installs `j-log-engine-<version>.jar` to the local Maven repo. **`j-log-common` must be installed first, and the
engine must be built before `ars-fx` and `j-digi`.** `../build-release.sh` does the whole sequence.

## What's Here

| Package | Responsibility |
|---------|---------------|
| `com.jlog.db` | DatabaseManager, QsoDao, ContestQsoDao, ContestQtcDao, TranslationDao, BackupService — SQLite in `~/.j-log/` |
| `com.jlog.plugin` | PluginLoader, ContestPlugin, MultiplierLists — JSON contest definitions |
| `com.jlog.scoring` | ContestScorer, DxccResolver and the per-contest multiplier helpers (AriDx, Wag, WaeMultiplier ...) |
| `com.jlog.export` | CabrilloExporter (ADIF import/export live in `ars-fx`) |
| `com.jlog.util` | AppConfig, CallsignRegion, DxccLanguageMap, Maidenhead |

## What's in j-log-common (not here)

The DB-free primitives live in the sibling **`j-log-common`** module (`com.jlog.common`) so non-logging consumers
(j-bridge needs only the bandplan) don't drag in SQLite/scoring/plugins. The engine `requires transitive
com.jlog.common`, so engine consumers still see these through us unchanged:

- `com.jlog.model` — QsoRecord, QtcRecord, Macro, Translation
- `com.jlog.bandplan` — BandplanLoader + IARU R1/R2/R3 / per-country overlay (the suite's single band plan)
- `com.jlog.support` — IssueReporter (GitHub-issue browser launcher)

## What's NOT Here

- The UI, DX cluster, rig/rotor control, ADIF, LoTW/QRZ and awards — these live in `ars-fx`.
- Unused hub, award, CI-V and QRZ classes were removed (see the cleanup commits).

## Module

JPMS named module: `com.jlog.engine` (requires transitive `com.jlog.common`).

Consumers that use JPMS must `requires com.jlog.engine` in their `module-info.java`.
**Build order:** `j-log-common` must be installed before `j-log-engine`.
Consumers that don't use JPMS (j-digi) simply add the Maven dependency.

## Data Storage

All runtime data lives in `~/.j-log/` (shared with `ars-fx`):
- `j-log.db` — normal log QSOs
- `contest.db` — contest QSOs
- `config.db` — key/value config, macros, DX network profiles
- `translations.db` — QSO phrase translator
