# CLAUDE.md — J-Bridge

Guidance for Claude Code when working in this repository.

## Build & Run

```bash
# Build fat jar
./build.sh
# or
mvn clean package -DskipTests

# Run (JavaFX runtime in ./lib/javafx — same as j-log)
./run.sh
# or manually
java --module-path ./lib/javafx \
     --add-modules javafx.controls,javafx.fxml \
     -Dfile.encoding=UTF-8 \
     -jar target/j-bridge-1.0.0-shaded.jar
```

## Architecture Overview

J-Bridge is a **standalone** Java 21 + JavaFX 21 app in the WM3j ARS Suite.
It acts as a bridge between weak-signal digital-mode apps (WSJT-X **and JTDX**)
and j-hub.

The two upstream apps share the same UDP protocol family; J-Bridge auto-detects
which one is feeding it from the heartbeat `id` field and surfaces the source
in both the status panel ("App: JTDX") and the published `WSJTX_CONNECTION`
JSON message (`sourceApp` field).

MSHV is intentionally **not** supported — it is closed-source freeware with no
native macOS build, which fails the suite's "open source + Linux/Windows/macOS"
constraint.

```
WSJT-X / JTDX  ──UDP 2237──►  WsjtxUdpListener
                              │
                              ▼
                      WsjtxProtocolDecoder
                              │
                    ┌─────────┴──────────┐
                    │                    │
               CallsignParser     BandUtils.frequencyToBand()
                    │
                    ▼
              MessagePublisher  ──WebSocket──►  j-hub
                                                  │
                                         broadcastToAll()
                                                  │
                                    ┌─────────────┴──────────┐
                                    │                        │
                                 j-log                   HamClock
```

## Key Design Decisions

**No local DXCC enrichment** — j-hub's SpotEnricher does all geo enrichment
server-side from its `/dxcc/prefixes.json`. J-Bridge publishes a raw
WSJTX_DECODE and the hub rebroadcasts. Local enrichment was removed to avoid
duplicating j-hub's prefix table and to keep J-Bridge thin.

**Gson only for hub messages** — j-hub's wire protocol uses Gson (ConfigManager.gson()).
J-Bridge uses Gson for all JSON it sends/receives with the hub. This matches
j-log's HubEngine.java which also uses Jackson internally but Gson on the wire.

**Hub discovery** — listens UDP 9999 for HUB_BEACON from j-hub's HubDiscovery.
On beacon receipt, connects to ws://[sender-ip]:[wsPort]. Same mechanism as
j-log's HubDiscoveryListener.

**Bandplan caption** — `StatusPanels.setFrequency(hz)` consults
`com.jlog.bandplan.BandplanLoader` and appends the segment description
(e.g. `14.074 MHz   DATA — Digimodes / FT8 14074 (IARU-R2)`) so the
operator immediately sees what kind of activity belongs at that
frequency. Shared library lives in j-log-engine.

**Registration** — sends `{"type":"APP_CONNECTED","appName":"jBridge","version":"1.0.0"}`
immediately on WebSocket open. Required by HubServer.handleRegistration().

**j-hub shutdown behaviour** — HubServer.onClose() calls System.exit(0) when the
last registered app disconnects. J-Bridge must maintain its connection and
disconnect cleanly on window close.

## Package Structure

```
com.hamradio.jbridge/
  JBridgeMain.java             Application entry point
  ConfigManager.java           JSON config (j-bridge-config.json)
  LoggingConfigurator.java     Logback setup (logs to ~/.hamlog/logs/)
  BandUtils.java               Frequency→band, matches j-hub ClusterManager
  CallsignParser.java          WSJT-X message text → DX callsign
  WorkedListManager.java       Thread-safe worked callsign set
  HubClient.java               WebSocket client + hub discovery (UDP 9999)
  WsjtxUdpListener.java        WSJT-X UDP listener (port 2237)
  WsjtxProtocolDecoder.java    Binary protocol per NetworkMessage.hpp
  MessagePublisher.java        Gson JSON → j-hub wire messages
  model/
    WsjtxDecode.java
    WsjtxStatus.java
    WsjtxQsoLogged.java
    BandActivity.java
  ui/
    MainWindow.java             Primary BorderPane layout + callback wiring
    DecodeTableView.java        Colour-coded decode list (TableView)
    StatusPanels.java           BandActivityPanel, WsjtxStatusPanel, HubStatusPanel
    SettingsWindow.java         Modal settings dialog
```

## Logging

Logs go to `~/.hamlog/logs/j-bridge.log` (shared directory with j-log).
7-day daily rotation. Matches j-log LoggingConfigurator pattern exactly.

## Config File

`j-bridge-config.json` lives in the working directory alongside `hub.json`.
Created with defaults on first run.

## Dependencies

Versions locked to j-log pom.xml:
- Java 21 + JavaFX 21.0.2
- Java-WebSocket 1.5.4
- Gson 2.10.1 (j-hub wire protocol)
- SLF4J 2.0.12 + Logback 1.5.3
