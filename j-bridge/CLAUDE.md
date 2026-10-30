# CLAUDE.md — J-Bridge

Guidance for Claude Code when working in this module.

## Role in the suite

J-Bridge is a **library**: the UDP listener and decoder for the feed of weak-signal digital-mode apps (WSJT-X **and
JTDX**). It has no window, launcher, config file or hub link of its own — the standalone app (`JBridgeMain`, the
window UI, `HubClient`, `MessagePublisher`) was removed; the only standalone module in the suite is J-Map.

`ars-fx` embeds it: `com.ars.fx.data.BridgeService` creates a `WsjtxUdpListener` (UDP port 2237, `-Dwsjtx.port` to
override) and the J-Bridge surface shows decodes, status and the connected source app.

MSHV is intentionally **not** supported — it is closed-source freeware with no native macOS build, which fails the
suite's "open source + Linux/Windows/macOS" constraint.

## Build

```bash
mvn install            # tests run; installs j-bridge-<version>.jar to ~/.m2
```

Java 21, Maven. `j-log-common` must be in `~/.m2` first (`../build-release.sh` installs the libraries).

## Architecture

```
WSJT-X / JTDX  ──UDP 2237──►  WsjtxUdpListener
                              │
                              ▼
                      WsjtxProtocolDecoder  (binary protocol per NetworkMessage.hpp)
                              │
                    ┌─────────┴──────────┐
                    │                    │
               CallsignParser     BandUtils.frequencyToBand()
                    │
                    ▼
        listener callbacks ──► ars-fx BridgeService ──► J-Bridge surface
```

The two upstream apps share the same UDP protocol family; J-Bridge detects which one is feeding it from the heartbeat
`id` field (`UpstreamAppConnectionListener`) and reports it as the source app.

## Key design decisions

- **No local DXCC enrichment.** The listener publishes raw decodes; enrichment happens in the consumer.
- **Band names** come from the shared band plan: `BandUtils.frequencyToBand(hz)` delegates to
  `com.jlog.bandplan.BandplanLoader` (j-log-common), so WSJT-X decodes get the same band names as the log and the
  cluster. Returns `"OOB"` outside every amateur band. Input is always Hz. The active region is set by `ars-fx`
  from the Station "IARU region" setting.

## Package structure

```
com.hamradio.jbridge/
  WsjtxUdpListener.java            WSJT-X / JTDX UDP listener  [used by ars-fx]
  WsjtxProtocolDecoder.java        Binary protocol decoder
  UpstreamAppConnectionListener.java
  CallsignParser.java              WSJT-X message text -> DX callsign
  BandUtils.java                   Frequency -> band via the shared band plan
  model/                           WsjtxDecode, WsjtxStatus, WsjtxQsoLogged  [used by ars-fx]
```

## Dependencies

Java 21, `j-log-common` (the band plan) and `slf4j-api`; JUnit 5 for tests. It is a thin jar (about 20 KB) with no
JavaFX, WebSocket, JSON or logging backend — `ars-fx` supplies those. `ars-fx` reads the decode `country` and
`distanceKm` fields, which this module leaves empty (see the model Javadoc), so those parts of its decode rows stay blank.
