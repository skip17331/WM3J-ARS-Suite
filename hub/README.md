# Ham Radio Hub

Central WebSocket hub for the ham radio application suite.  
All connected apps share real-time rig state, DX cluster spots, and session data through this process.

---

## Quick Start

### Requirements
| Tool | Version |
|------|---------|
| Java | 11 or higher (17+ recommended) |
| Maven | 3.8+ |

### Build

```bash
cd hub
mvn package -q
```

This produces `target/hub-1.0.0.jar` (fat JAR with all dependencies).

### Run

```bash
java -jar target/hub-1.0.0.jar
```

On first run a default `hub.json` is created in the current directory and the JavaFX status window opens.

---

## Configuration

Edit `hub.json` directly **or** open the web UI at:

```
http://localhost:8081
```

Key sections:

| Section | Purpose |
|---------|---------|
| `hub` | WebSocket port (default 8080) and web UI port (default 8081) |
| `station` | Your callsign, latitude/longitude, grid square, timezone |
| `cluster` | DX cluster server, port, login callsign, band/mode filters |
| `logger` | Log mode (normal / contest) |
| `infoScreen` | Map style, spot timeout, max cached spots |

---

## WebSocket Protocol

Connect to `ws://localhost:8080`.  
Send `APP_CONNECTED` as your first message:

```json
{ "type": "APP_CONNECTED", "appName": "myapp", "version": "1.0" }
```

The hub replies with `HUB_WELCOME` and then replays the state cache (last rig status, recent spots, logger session).

### Message Types

| Type | Direction | Description |
|------|-----------|-------------|
| `APP_CONNECTED` | App → Hub | Registration |
| `HUB_WELCOME` | Hub → App | Acknowledgement |
| `APP_LIST` | Hub → All | Current connected-app list |
| `RIG_STATUS` | Logger → Hub → All | Transceiver state |
| `LOGGER_SESSION` | Logger → Hub → All | Operating mode/contest info |
| `SPOT_SELECTED` | Any → Hub → All | User clicked a spot |
| `SPOT` | Hub → All | Enriched DX cluster spot |
| `WSJTX_DECODE` | Bridge → Hub → All | WSJT-X decoded station |

---

## Project Layout

```
hub/
├── pom.xml
└── src/main/
    ├── java/com/hamradio/hub/
    │   ├── HubMain.java           Entry point / JavaFX bootstrap
    │   ├── HubServer.java         WebSocket server
    │   ├── WebConfigServer.java   Embedded Jetty HTTP server
    │   ├── ClusterManager.java    DX cluster telnet connection
    │   ├── SpotEnricher.java      DXCC lookup, bearing, distance
    │   ├── ConfigManager.java     hub.json read/write
    │   ├── StateCache.java        In-memory state ring buffer
    │   ├── MessageRouter.java     Message dispatch
    │   ├── DxccDatabase.java      Prefix table data-access layer
    │   ├── model/
    │   │   ├── HubConfig.java
    │   │   ├── Spot.java
    │   │   ├── RigStatus.java
    │   │   └── StationConfig.java
    │   └── ui/
    │       └── HubStatusWindow.java  JavaFX status window
    └── resources/
        ├── logback.xml
        ├── dxcc/prefixes.json     ~300 DXCC prefix entries
        └── web/
            ├── index.html         Config UI
            ├── config.js
            └── config.css
```

---

## Logging

Logs are written to:
- **Console** — colored output
- **`logs/hub.log`** — rolling daily file (30-day retention, 200 MB cap)

Adjust levels in `src/main/resources/logback.xml`.

---

## Adding a New Application

1. Open a WebSocket connection to `ws://localhost:<wsPort>`.
2. Send `APP_CONNECTED` with your app name and version.
3. Receive the state cache replay (rig status, recent spots, logger session).
4. Subscribe to any message types you care about.
5. Publish `RIG_STATUS`, `SPOT_SELECTED`, or `WSJTX_DECODE` as needed.

The hub handles all routing, enrichment, and persistence.

---

## License

MIT — free for personal ham radio use.
