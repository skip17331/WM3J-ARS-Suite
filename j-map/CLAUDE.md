# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Build (skip tests for speed)
mvn clean package -DskipTests

# Run via Maven (recommended during development)
mvn javafx:run

# Run fat JAR directly
java -jar target/j-map-1.5.0-fat.jar

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=SolarPositionServiceTest

# Run a single test method
mvn test -Dtest=SolarPositionServiceTest#testSummerSolsticeDeclination
```

Java 21 and Maven 3.8+ required. JavaFX is bundled via Maven — no separate installation needed. The pom.xml auto-selects the correct JavaFX native classifier for linux/mac/windows.

## Architecture Overview

**Entry point:** `JMapApp` (JavaFX `Application`) detects local vs. remote mode, loads initial settings, wires `ServiceRegistry`, then hands off to `MainWindow`. J-Map is **headless** — it binds to no HTTP port.

**Mode detection (JMapApp.init):**
- *Local mode* — J-Hub WS (port 8080) reachable at startup → initial settings via `GET /api/jmap`, live updates via `JMAP_CONFIG` WebSocket messages.
- *Remote mode* — J-Hub not reachable → initial settings from `/config/jmap_config.json`, then `~/.j-map/settings.json` as fallback.

**Configuration authority:** J-Hub (port 8081) is the sole UI for all J-Map settings. Changes saved in J-Hub are broadcast as `JMAP_CONFIG` WebSocket messages. `DxClusterClient` receives them and calls `ServiceRegistry`'s `jMapConfigListener`, which applies the new settings live.

**Service layer pattern:** All external data sources implement `DataProvider<T>`, extend `AbstractDataProvider<T>` (which provides thread-safe caching via `AtomicReference`), and override `doFetch()`. Every domain has a pair: a live provider (e.g., `NoaaSolarDataProvider`) and a `Mock*` provider. `ServiceRegistry` selects between them based on `Settings.isUseMockData()` and whether API keys are set. Providers are held as `volatile` fields so hot-swapping on settings change is safe.

**Refresh scheduling:** `ServiceRegistry.start()` creates a 4-thread `ScheduledExecutorService` and schedules each provider at its own cadence (solar: 15 min, propagation: 10 min, DX spots: 2 min, aurora: 30 min, rotor: 1 sec). The UI reads from `getCached()` — it never blocks on network.

**Render loop:** `MainWindow` uses a JavaFX `AnimationTimer` to drive all UI updates. Full UI (time + grayline + panels) refreshes every second; rotor map refreshes every 500ms. All UI mutations happen on the JavaFX Application Thread via `Platform.runLater`.

**Settings flow:** Initial settings loaded by `SettingsLoader.loadOrDefaults(hubReachable)`. Local settings also persisted at `~/.j-map/settings.json` as a fallback. Live changes come exclusively via `JMAP_CONFIG` WebSocket messages from J-Hub.

**UI layout:** `DashboardLayout` builds a `BorderPane`: `TimePanel` top, `WorldMapCanvas` (wrapped in a `StackPane` with `RotorMapPane` overlaid bottom-right) center, right sidebar (`SolarDataPanel` / `PropagationPanel` / `BandConditionsPanel`). `WorldMapCanvas` is a JavaFX `Canvas` that composites the world map image, grayline night mask, DX spot dots, and aurora/weather/tropo overlay images on every `redraw()` call.

**Astronomy:** `SolarPositionService` implements the NOAA SPA algorithm (pure math, no external calls). `GraylineService` uses it to produce a `NightMask` pixel array. `SunriseSunsetService` derives rise/set times for the QTH.

## Bandplan caption on DX spots

`DXInfoWindow` consults `com.jlog.bandplan.BandplanLoader` (j-log-engine) and appends a segment caption (e.g. `20m  14074.0 kHz   DATA — Digimodes / FT8 14074`) so the operator can see what activity belongs at the spot's frequency without consulting an external chart. j-log-engine is the only ARS-Suite dependency j-map carries — the shade plugin filters drop its unused transitive parts (SQLite, JSSC, civ) from the fat jar.

Region/country come from j-hub's Station settings (Regional Settings card) — delivered via `JHUB_WELCOME` / `STATION_CONFIG` and mirrored into the `Settings` fields `bandplanRegion` (default `IARU-R2`) and `bandplanCountry` (default `US`). Stand-alone overrides still possible in `~/.j-map/settings.json`.

## Adding a New Data Source

1. Create a data record (e.g., `MyData.java`) in the appropriate `service/` subpackage.
2. Define an interface extending `DataProvider<MyData>`.
3. Implement a live provider extending `AbstractDataProvider<MyData>` with `doFetch()`.
4. Implement a `Mock*` provider (also extending `AbstractDataProvider<MyData>`).
5. Add a `volatile` field to `ServiceRegistry` and wire it in `rebuildProviders()`.
6. Add a `scheduleAtFixedRate` call in `ServiceRegistry.start()`.
7. Expose a getter from `ServiceRegistry` and consume `getCached()` in the UI.

## Key Files

| File | Purpose |
|---|---|
| `app/ServiceRegistry.java` | DI container; provider lifecycle, scheduling, hot-swap |
| `app/JMapApp.java` | JavaFX entry point; mode detection; no web server |
| `service/AbstractDataProvider.java` | Caching base class for all providers |
| `service/config/Settings.java` | All configuration fields; Jackson-serialized |
| `service/config/SettingsLoader.java` | Initial load (local: J-Hub HTTP; remote: /config/jmap_config.json) |
| `service/dx/DxClusterClient.java` | J-Hub WebSocket client; handles JMAP_CONFIG messages |
| `ui/main/DashboardLayout.java` | Layout orchestrator; update dispatch |
| `ui/overlays/WorldMapCanvas.java` | Map compositing (grayline, DX, overlays) |
