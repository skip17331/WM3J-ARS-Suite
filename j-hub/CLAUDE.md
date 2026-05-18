# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
mvn package -q                          # compile + produce target/j-hub-1.0.0.jar (fat JAR)
java -jar target/j-hub-1.0.0.jar        # run (creates j-hub.json on first launch)
```

There are no tests in this project. There is no lint step.

## Architecture

J-Hub is the **central message broker** for the WM3J ARS Suite of ham radio desktop apps. It holds three long-running services wired together at startup in `JHubMain.bootstrap()`:

| Component | Role |
|---|---|
| `JHubServer` | Java-WebSocket server on port 8080; manages `AppSession` objects per connected client |
| `WebConfigServer` | Embedded Jetty HTTP server on port 8081; serves `src/main/resources/web/` and a config REST API |
| `ClusterManager` | Telnet client to a DX cluster; parses spot lines, enriches them, and publishes via `MessageRouter` |

**Message flow:**
1. An app connects via WebSocket and sends `APP_CONNECTED`.
2. `JHubServer` creates an `AppSession`, sends `JHUB_WELCOME`, and replays cached state from `StateCache`.
3. Subsequent messages are dispatched through `MessageRouter.route()`, which updates `StateCache` and rebroadcasts to connected apps.
4. Cluster spots arrive on a daemon thread in `ClusterManager`, are enriched by `SpotEnricher` (DXCC prefix lookup, bearing/distance), filtered by band/mode, then published via `MessageRouter.publishSpot()`.

**Key singletons** (all accessed via `getInstance()`): `ConfigManager`, `StateCache`, `SpotEnricher`, `MessageRouter`, `ClusterManager`.

**`StateCache`** holds three things: last `RigStatus`, last `LOGGER_SESSION` raw JSON, and a ring buffer of recent `Spot` objects (default 50). Late-joining apps receive a full replay of this cache immediately after registration.

**`ConfigManager`** reads/writes `j-hub.json` in the working directory and exposes a shared `Gson` instance (`ConfigManager.gson()`).

**`JHubStatusWindow`** is a JavaFX window launched by `JHubMain` (which extends `Application`); `JHubMain.main()` calls `Application.launch()` which bootstraps the FX toolkit before calling `start()`.

## Configuration

`j-hub.json` is created at first run. Key sections: `jHub` (ports), `station` (callsign, lat/lon, grid), `cluster` (server, port, loginCallsign, filters), `logger`, `infoScreen`. Edit directly or use the web UI at `http://localhost:8081`.

**J-Hub is the configuration UI for the JavaFX modules.** Port 8082 was once the old J-Map setup server (long retired) and is now used by the standalone J-Learn web app. J-Hub iframes J-Learn (8082) and J-Vault (8083) as tabs but doesn't own their UI. J-Map configuration lives in the J-Map tab at port 8081 and is delivered to J-Map via `JMAP_CONFIG` WebSocket messages.

## WebSocket Protocol

All messages are JSON with a `type` field. First message from any client must be `APP_CONNECTED`. Handled types: `APP_CONNECTED`, `JHUB_WELCOME`, `STATION_CONFIG`, `APP_LIST`, `RIG_STATUS`, `LOGGER_SESSION`, `SPOT_SELECTED`, `SPOT`, `WSJTX_DECODE`.

`JHUB_WELCOME` (sent on connect) and `STATION_CONFIG` (broadcast on `/api/config` or `/api/rig` save) both carry the full station section — including operator IARU region + country overlay used for bandplan captions — plus the rig's `rigHamlibHost` / `rigHamlibPort` so modules that key the rig (e.g. j-digi PTT/CW) reuse the station-level `rigctld` endpoint rather than holding duplicate prefs.

## Module Connections

J-Hub launches these modules as child processes via `AppLauncher`
(`cmd /c` on Windows, `bash -c` elsewhere). Each module's launch
command lives in `j-hub.json` at `apps.<key>.command` and is
auto-populated by `JHubConfig.applyDefaults()` on first run — OS-aware,
pointing at the module's native launcher under the detected ARS Suite
root (`<root>`):

| `apps` key | Module | Windows default | Linux / macOS default |
|---|---|---|---|
| `jMap`    | J-Map    | `<root>\j-map\j-map.bat`                    | `bash <root>/j-map/j-map.sh` |
| `jLog`    | J-Log    | `<root>\j-log\j-log.bat`                    | `bash <root>/j-log/j-log.sh` |
| `jBridge` | J-Bridge | `<root>\j-bridge\j-bridge.bat`              | `bash <root>/j-bridge/j-bridge.sh` |
| `jDigi`   | J-Digi   | `<root>\j-digi\j-digi.bat`                  | `bash <root>/j-digi/j-digi.sh` |
| `jSat`    | J-Sat    | `<root>\j-sat\j-sat.bat --launched-by-hub`  | `bash <root>/j-sat/j-sat.sh --launched-by-hub` |
| `jVault`  | J-Vault  | `<root>\j-vault\j-vault.bat`                | `bash <root>/j-vault/j-vault.sh` |
| `jLearn`  | J-Learn  | `<root>\j-learn\j-learn.bat`                | `bash <root>/j-learn/j-learn.sh` |

`applyDefaults()` fills blank commands **and normalizes** one carried
from another OS (e.g. a `bash …/run.sh` left in a config now opened on
Windows) to the correct per-OS launcher, leaving valid same-OS
customizations alone. The unified Windows installer (`install.bat`,
which runs `install.ps1`) generates the `.bat` wrappers and pre-empts
this on a fresh install. (morse-trainer is launched separately via
`WebConfigServer`, not from this table.)

## Adding a New Message Type

1. Add a case in `MessageRouter.route()`.
2. Add handler method that reads from `StateCache` or writes to it as appropriate.
3. Call `server.broadcastToAll()` or `broadcastExcept()` to forward.

## Logging

SLF4J + Logback. Console output is colored; rolling file logs go to `logs/j-hub.log`. Adjust levels in `src/main/resources/logback.xml`. The cluster reader logs non-spot lines at `TRACE` and parsed spots at `DEBUG`.
