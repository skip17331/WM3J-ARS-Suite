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
     -jar target/hamlog-1.0.0-shaded.jar
```

The shaded jar (`target/hamlog-1.0.0-shaded.jar`) bundles all dependencies except JavaFX, which must be on the module path at runtime.

## Architecture Overview

HamLog is a Java 21 + JavaFX 21 desktop app using a standard MVC layout with a named module (`com.hamlog.app`).

**Startup sequence** (`HamLogApp.init` → `start`):
1. `AppConfig.load()` — reads Java `Preferences` (theme, language, mode)
2. `LoggingConfigurator` — sets Logback level (INFO or DEBUG)
3. `I18n.load()` — loads the appropriate `messages_<lang>.properties`
4. `DatabaseManager.initAll()` — opens/creates three SQLite DBs in `~/.hamlog/`
5. `SplashScreen` — mode chooser (Normal vs Contest); launches the selected window

**Two operating modes share no UI but share the same singleton services:**
- *Normal mode*: `NormalLogController` + `NormalLog.fxml` → `QsoDao` → `hamlog.db`
- *Contest mode*: `ContestLogController` + `ContestLog.fxml` → `ContestQsoDao` → `contest.db`; contest definition loaded from a `ContestPlugin` JSON

**Key singletons** (all use `getInstance()`):
| Singleton | Responsibility |
|-----------|---------------|
| `DatabaseManager` | Opens/closes the three SQLite connections; provides `getConfig`/`setConfig` for key-value config stored in `config.db` |
| `AppConfig` | Thin façade — small prefs (theme, lang, window geometry) via Java `Preferences`; station/CI-V config delegated to `DatabaseManager.getConfig` |
| `CivEngine` | Bidirectional Icom CI-V over serial (JSSC); 500 ms poll + unsolicited frame handling |
| `DxClusterEngine` | Telnet DX cluster connection; delivers parsed `DxSpot` objects via listener |

**Contest plugin system**: JSON files define the entire contest UI (fields, scoring, multiplier model, Cabrillo mapping). `PluginLoader` discovers them from the JAR resources first, then `~/.hamlog/plugins/`. `ContestPlugin` is a Jackson-mapped POJO — adding fields to the JSON schema requires updating `ContestPlugin.java` and its inner classes.

**Macro system**: `Macro` objects (stored in `config.db` as JSON action arrays) are executed by `MacroEngine`, which dispatches each `MacroAction` type (PTT, CW, VOICE_PLAY, etc.) against `CivEngine` or JavaFX `AudioClip`. Macros are bound to F1–F12.

**Theme/CSS**: `HamLogApp.applyTheme(scene)` applies `base.css` + either `light.css` or `dark.css` to any `Scene`. Call this whenever a new window is created.

**i18n**: All user-visible strings go through `I18n.get("key")`. Keys live in `src/main/resources/com/hamlog/i18n/messages*.properties`.

## Data Storage

All runtime data lives in `~/.hamlog/`:
- `hamlog.db` — normal log QSOs
- `contest.db` — contest QSOs (includes `contest_id`, `is_dupe`, `field1`–`field5`)
- `config.db` — key/value app config, macros (as JSON), DX network profiles
- `plugins/` — user-installed contest plugin JSON files
- `logs/hamlog.log` — daily-rotating log (7 days)

## Module System Notes

The project uses the JPMS named module `com.hamlog.app`. The `module-info.java` `opens` directives are required for JavaFX FXML injection and Jackson deserialization — do not remove them when adding new packages that need reflection.
