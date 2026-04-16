# CLAUDE.md — Digital Bridge

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
     -jar target/digital-bridge-1.0.0-shaded.jar
```

## Architecture Overview

Digital Bridge is a **standalone** Java 21 + JavaFX 21 app in the WM3j ARS Suite.
It acts as a bridge between WSJT-X (weak-signal digital modes) and j-hub.

```
WSJT-X  ──UDP 2237──►  WsjtxUdpListener
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
server-side from its `/dxcc/prefixes.json`. Digital Bridge publishes a raw
WSJTX_DECODE and the hub rebroadcasts. Local enrichment was removed to avoid
duplicating j-hub's prefix table and to keep Digital Bridge thin.

**Gson only for hub messages** — j-hub's wire protocol uses Gson (ConfigManager.gson()).
Digital Bridge uses Gson for all JSON it sends/receives with the hub. This matches
j-log's HubEngine.java which also uses Jackson internally but Gson on the wire.

**Hub discovery** — listens UDP 9999 for HUB_BEACON from j-hub's HubDiscovery.
On beacon receipt, connects to ws://[sender-ip]:[wsPort]. Same mechanism as
j-log's HubDiscoveryListener.

**Registration** — sends `{"type":"APP_CONNECTED","appName":"digitalBridge","version":"1.0.0"}`
immediately on WebSocket open. Required by HubServer.handleRegistration().

**j-hub shutdown behaviour** — HubServer.onClose() calls System.exit(0) when the
last registered app disconnects. Digital Bridge must maintain its connection and
disconnect cleanly on window close.

## Package Structure

```
com.hamradio.digitalbridge/
  DigitalBridgeMain.java       Application entry point
  ConfigManager.java           JSON config (digitalbridge-config.json)
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

Logs go to `~/.hamlog/logs/digitalbridge.log` (shared directory with j-log).
7-day daily rotation. Matches j-log LoggingConfigurator pattern exactly.

## Config File

`digitalbridge-config.json` lives in the working directory alongside `hub.json`.
Created with defaults on first run.

## Dependencies

Versions locked to j-log pom.xml:
- Java 21 + JavaFX 21.0.2
- Java-WebSocket 1.5.4
- Gson 2.10.1 (j-hub wire protocol)
- SLF4J 2.0.12 + Logback 1.5.3
