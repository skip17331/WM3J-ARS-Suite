# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
mvn clean package -DskipTests
mvn javafx:run
# Optional flags (pass via Djavafx.args="..." or as program args):
#   --hub <host>          override J-Hub hostname (default localhost)
#   --launched-by-hub     J-Hub owns the lifecycle (no splash, etc.)
```

Java 21 + JavaFX 21.

## Architecture

JavaFX desktop app for amateur-satellite pass prediction, live tracking, Doppler correction, and rotor steering. **Does not own rig/rotor hardware directly** — every CAT/rotor command goes out as a J-Hub WebSocket message (J-Hub owns the serial-port singletons).

`ServiceRegistry` (in `com.hamradio.jsat.app`) wires the long-running services at startup; everything else fetches them by getter. Four scheduled tasks run on background threads:

| Cadence | Task |
|---|---|
| 1 s | UI tick — refresh dashboard panels |
| 5 min | Space weather pull (NOAA K-index, solar flux) |
| 10 min | Pass-prediction refresh for the visible satellite set |
| 12 h | TLE update from Celestrak |

J-Hub messages emitted: `SAT_DOPPLER` (per-tick frequency adjustment), `SAT_ROTOR_CMD` (target az/el during a tracked pass).

## Key services

| Service | Role |
|---|---|
| `TleManager` | Fetches TLEs from Celestrak (`celestrak.org/NORAD/elements/gp.php?GROUP=amateur\|stations`); falls back to AMSAT; caches to `~/.j-sat/tles.txt`; reloads on startup if cache is <1 day old |
| `SatelliteRegistry` | Loads bundled `satellite-registry.json` — transponder mode, uplink/downlink freqs, capabilities |
| `SatelliteTracker` | Orchestrates pass prediction across the visible set |
| `Sgp4Propagator` + `PassPredictor` | Standard SGP4 model; 15 s coarse step + 1 s binary-search refinement for AOS/LOS |
| `DopplerCalculator` | Per-second frequency correction for the active pass |
| `CoordTransform` | Geodetic ↔ topocentric math for az/el/range |
| `SpaceWeatherService` | NOAA K-index, SFI |
| `JHubClient` | WebSocket client (port 8080) |
| `JsatApiServer` | HTTP REST TLE endpoint (default port **8084**) — lets other apps pull TLEs without re-fetching from Celestrak |

## Packages

| Package | Responsibility |
|---|---|
| `com.hamradio.jsat.app` | `JSatApp` (Application entry), `ServiceRegistry`, `SplashScreen`, `CrashHandler` |
| `com.hamradio.jsat.service.tle` | `TleManager`, `CelestrakTleProvider` |
| `com.hamradio.jsat.service.orbital` | `Sgp4Propagator`, `PassPredictor`, `DopplerCalculator`, `CoordTransform` |
| `com.hamradio.jsat.service.tracking` | `SatelliteTracker` |
| `com.hamradio.jsat.service.registry` | `SatelliteRegistry` (loads bundled JSON) |
| `com.hamradio.jsat.service.config` | `JsatSettings`, `JsatSettingsLoader` |
| `com.hamradio.jsat.service.spaceweather` | NOAA pull + caching |
| `com.hamradio.jsat.hub` | `JHubClient` |
| `com.hamradio.jsat.api` | `JsatApiServer` REST endpoints |
| `com.hamradio.jsat.ui.main` | `MainWindow`, `DashboardLayout` |
| `com.hamradio.jsat.ui.panels` | `LivePassPanel`, `SpaceWeatherPanel`, `RigRotorPanel` |
| `com.hamradio.jsat.ui.canvas` | `SatTrackCanvas` map rendering, `WorldOutlines` |
| `com.hamradio.jsat.model` | `SatellitePass`, `TleSet`, `SatelliteDefinition`, `SatelliteState` |

## Data storage

- `~/.j-sat/config.json` — user station (lat/lon/grid), tracking prefs, API port
- `~/.j-sat/tles.txt` — plain-text TLE cache, one 3-line block per satellite

## External integrations

- HTTPS to `celestrak.org` (TLEs)
- HTTPS to NOAA (space weather)
- WebSocket to J-Hub (port 8080) for rig/rotor handoff
- HTTP server (port 8084) — outbound TLE distribution

## Resources

- `satellite-registry.json` — capability/freq table for every supported bird
- `world-map.jpg` — map background for the live-track canvas
- CSS, logback.xml

## What's NOT here

- No direct serial / Hamlib calls — all rig/rotor commands go through J-Hub.
- No JavaFX tests yet (TLE parsing and pass-prediction math are testable; UI is not).
