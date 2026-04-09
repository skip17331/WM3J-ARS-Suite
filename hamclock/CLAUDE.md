# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Build (skip tests for speed)
mvn clean package -DskipTests

# Run via Maven (recommended during development)
mvn javafx:run

# Run fat JAR directly
java -jar target/hamclock-clone-1.0.0-jar-with-dependencies.jar

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=SolarPositionServiceTest

# Run a single test method
mvn test -Dtest=SolarPositionServiceTest#testSummerSolsticeDeclination
```

Java 21 and Maven 3.8+ required. JavaFX is bundled via Maven — no separate installation needed. The pom.xml auto-selects the correct JavaFX native classifier for linux/mac/windows.

## Architecture Overview

**Entry point:** `HamClockApp` (JavaFX `Application`) loads settings, wires `ServiceRegistry`, starts `SetupWebServer`, then hands off to `MainWindow`.

**Service layer pattern:** All external data sources implement `DataProvider<T>`, extend `AbstractDataProvider<T>` (which provides thread-safe caching via `AtomicReference`), and override `doFetch()`. Every domain has a pair: a live provider (e.g., `NoaaSolarDataProvider`) and a `Mock*` provider. `ServiceRegistry` selects between them based on `Settings.isUseMockData()` and whether API keys are set. Providers are held as `volatile` fields so hot-swapping on settings change is safe.

**Refresh scheduling:** `ServiceRegistry.start()` creates a 4-thread `ScheduledExecutorService` and schedules each provider at its own cadence (solar: 15 min, propagation: 10 min, DX spots: 2 min, aurora: 30 min, rotor: 1 sec). The UI reads from `getCached()` — it never blocks on network.

**Render loop:** `MainWindow` uses a JavaFX `AnimationTimer` to drive all UI updates. Full UI (time + grayline + panels) refreshes every second; rotor map refreshes every 500ms. All UI mutations happen on the JavaFX Application Thread via `Platform.runLater`.

**Settings flow:** Settings are persisted at `~/.hamclock/settings.json` (Jackson). The `SetupWebServer` (NanoHTTPD on port 8080) is the only way to change settings — the main display has no settings UI. When the web page POSTs a change, `ServiceRegistry.onSettingsChanged()` rebuilds providers and fires `settingsChangedCallback`, which calls `DashboardLayout.applySettings()` via `Platform.runLater`.

**UI layout:** `DashboardLayout` builds a `BorderPane`: `TimePanel` top, `WorldMapCanvas` (wrapped in a `StackPane` with `RotorMapPane` overlaid bottom-right) center, right sidebar (`SolarDataPanel` / `PropagationPanel` / `BandConditionsPanel`). `WorldMapCanvas` is a JavaFX `Canvas` that composites the world map image, grayline night mask, DX spot dots, and aurora/weather/tropo overlay images on every `redraw()` call.

**Astronomy:** `SolarPositionService` implements the NOAA SPA algorithm (pure math, no external calls). `GraylineService` uses it to produce a `NightMask` pixel array. `SunriseSunsetService` derives rise/set times for the QTH.

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
| `app/HamClockApp.java` | JavaFX entry point; startup/shutdown sequence |
| `service/AbstractDataProvider.java` | Caching base class for all providers |
| `service/config/Settings.java` | All configuration fields; Jackson-serialized |
| `ui/main/DashboardLayout.java` | Layout orchestrator; update dispatch |
| `ui/overlays/WorldMapCanvas.java` | Map compositing (grayline, DX, overlays) |
| `web/SetupWebServer.java` | NanoHTTPD embedded server; settings POST handler |
