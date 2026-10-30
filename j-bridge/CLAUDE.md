# CLAUDE.md — J-Bridge

Guidance for Claude Code when working in this module.

## Role in the suite

J-Bridge reads the UDP feed of weak-signal digital-mode apps (WSJT-X **and JTDX**) and decodes it. `ars-fx` embeds
it: `com.ars.fx.data.BridgeService` creates a `WsjtxUdpListener` (port 2237, `-Dwsjtx.port` to override) and shows
decodes, status and the connected source app on the J-Bridge surface. `ars-fx` uses only `WsjtxUdpListener` and the
`model` classes from this module. The rest (`JBridgeMain`, `HubClient`, `MessagePublisher`, `ui/`) is the **standalone**
app, which publishes to a legacy hub (see below).

MSHV is intentionally **not** supported — it is closed-source freeware with no native macOS build, which fails the
suite's "open source + Linux/Windows/macOS" constraint.

## Build & Run

```bash
./build.sh             # mvn clean package -DskipTests -> target/j-bridge-<version>.jar (shaded fat jar)
./run.sh               # runs the newest target/j-bridge-*.jar, building first if there is none
```

Java 21, Maven. The jar carries JavaFX, so no `--module-path` is needed. `j-log-common` must be in `~/.m2`
(`../build-release.sh` installs the libraries).

## Architecture

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
        embedded:   BridgeService ──► ars-fx J-Bridge surface
        standalone: MessagePublisher ──WebSocket──► legacy hub
```

The two upstream apps share the same UDP protocol family; J-Bridge auto-detects which one is feeding it from the
heartbeat `id` field (`UpstreamAppConnectionListener`) and surfaces it as "App: JTDX" in the status panel.

## Standalone mode and the legacy hub

Standalone `JBridgeMain` registers with a WebSocket hub: `HubClient` listens for a UDP `HUB_BEACON` on port 9999,
connects to `ws://<sender>:<wsPort>` (default 8080), sends `{"type":"APP_CONNECTED","appName":"jBridge",...}` and
publishes `WSJTX_DECODE` / `WSJTX_CONNECTION` messages through `MessagePublisher` (Gson). That hub was the old
standalone J-Hub, which has been retired; `ars-fx` has its own hub (`HubServer`, port 8090) with a different
protocol that this module does not speak. Keep the standalone path compiling, but new work belongs in `ars-fx`.

## Key design decisions

- **No local DXCC enrichment.** J-Bridge publishes raw decodes; enrichment happens in the consumer.
- **Band names** come from the shared band plan: `BandUtils.frequencyToBand(hz)` delegates to
  `com.jlog.bandplan.BandplanLoader` (j-log-common), so WSJT-X decodes get the same band names as the log and the
  cluster. Returns `"OOB"` outside every amateur band. Input is always Hz.
- **Region / country** for the band plan come from the Station "IARU region" setting in the suite; standalone mirrors
  them in `j-bridge-config.json` under `"operator": {"region", "country"}` (defaults `IARU-R2` + `US`).
- **`StatusPanels.setFrequency(hz)`** appends the band-plan segment description
  (e.g. `14.074 MHz   DATA — Digimodes / FT8 14074 (IARU-R2)`).

## Package structure

```
com.hamradio.jbridge/
  JBridgeMain.java                 Standalone entry point
  Launcher.java                    Fat-jar main (starts JBridgeMain)
  ConfigManager.java               JSON config (j-bridge-config.json)
  LoggingConfigurator.java         Logback setup (logs to ~/.hamlog/logs/)
  CrashHandler.java
  BandUtils.java                   Frequency -> band via the shared band plan
  CallsignParser.java              WSJT-X message text -> DX callsign
  WorkedListManager.java           Thread-safe worked callsign set
  HubClient.java                   Legacy hub WebSocket client + UDP 9999 discovery
  WsjtxUdpListener.java            WSJT-X / JTDX UDP listener (port 2237)  [used by ars-fx]
  WsjtxProtocolDecoder.java        Binary protocol per NetworkMessage.hpp
  UpstreamAppConnectionListener.java
  MessagePublisher.java            Gson JSON -> hub wire messages
  i18n/I18n.java
  model/                           WsjtxDecode, WsjtxStatus, WsjtxQsoLogged, BandActivity  [used by ars-fx]
  ui/                              MainWindow, DecodeTableView, StatusPanels, SettingsWindow,
                                   JBridgeFontScaler, SplashJBridge
```

## Logging and config

Standalone logs go to `~/.hamlog/logs/j-bridge.log` (7-day daily rotation). `j-bridge-config.json` lives in the
working directory and is created with defaults on first run. The copy committed in this module is a sample.

## Dependencies

Java 21, JavaFX 21.0.2, Java-WebSocket 1.5.4, Gson 2.10.1, SLF4J 2.0.12 + Logback 1.5.3, and `j-log-common`.
