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

## WebSocket Protocol

All messages are JSON with a `type` field. First message from any client must be `APP_CONNECTED`. Handled types: `APP_CONNECTED`, `JHUB_WELCOME`, `APP_LIST`, `RIG_STATUS`, `LOGGER_SESSION`, `SPOT_SELECTED`, `SPOT`, `WSJTX_DECODE`.

## Module Connections

J-Hub manages the following ARS Suite modules:

| Key | Module | Default command |
|---|---|---|
| `jMap` | J-Map | `cd /home/mike/ARS_Suite/j-map && mvn javafx:run -q` |
| `j-log` | J-Log | `bash /home/mike/ARS_Suite/j-log/run.sh` |
| `j-bridge` | J-Bridge | `bash /home/mike/ARS_Suite/j-bridge/run.sh` |
| `j-digi` | J-Digi | `bash /home/mike/ARS_Suite/j-digi/run.sh` |
| `j-sat` | J-Sat | `bash /home/mike/ARS_Suite/j-sat/run.sh --launched-by-hub` |

## Adding a New Message Type

1. Add a case in `MessageRouter.route()`.
2. Add handler method that reads from `StateCache` or writes to it as appropriate.
3. Call `server.broadcastToAll()` or `broadcastExcept()` to forward.

## Logging

SLF4J + Logback. Console output is colored; rolling file logs go to `logs/j-hub.log`. Adjust levels in `src/main/resources/logback.xml`. The cluster reader logs non-spot lines at `TRACE` and parsed spots at `DEBUG`.
