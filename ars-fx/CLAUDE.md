# CLAUDE.md — ars-fx

Guidance for Claude Code when working in the ARS Suite application. `ars-fx` is the app itself; the other Maven
modules are libraries it embeds.

## What it is

One JavaFX 21 / Java 21 application that runs the whole shack. Two ways to run it from the **same jar**. (The `j-digi` and `j-bridge` Maven modules are libraries with no
standalone app of their own.)

- **Docked** (default) — one window, a dock on the left switches between surfaces. Entry: `com.ars.fx.Launcher`.
- **Loose** — each module in its own window, all attached to a background hub (`launch/run-loose.sh`, see
  `launch/README.md`). Solo J-Map / J-Sat can also run from a JSON launch file, optionally against a remote hub.

The docked app is a *client* of a local background hub, `com.ars.fx.HubServer` (WebSocket, default `ws://127.0.0.1:8090`,
`remote.port` in the config). The hub owns the Hamlib daemons and live feeds (DX cluster, RBN, rig, rotor); the docked
app attaches through `data/RemoteLink` and the hub serves clients through `data/RemoteServer`. Clients stay passive
while a `RemoteLink` is active. `HubServer.ensureRunning()` brings the hub up if it is not already.

## Build & run

```bash
./run.sh              # mvn javafx:run, opens J-Hub; ./run.sh map|log|sat|digi|bridge|vault|learn|hubcfg opens a surface
mvn -DskipTests package                     # target/ars-fx-linux.jar
mvn -Pwin|-Pmac|-Pmac-aarch64|-Ppi package  # other platforms; the Pi jar has no JavaFX
mvn test              # JUnit 5 / surefire
../build-release.sh   # installs the libraries, builds all five platform jars into ../dist, checks natives
```

Libraries must be in `~/.m2` before `ars-fx` builds: `j-log-common`, `j-log-engine`, `j-digi`, `j-bridge`. Each
platform profile writes a distinct `ars-fx-<dist>.jar`; the build checks that a desktop jar carries only its own
platform's JavaFX natives.

## Package map (`com.ars.fx`)

| Package | Contents |
|---|---|
| `com.ars.fx` | `Launcher`/`Main` (entry points), `HubServer`, `Theme`/`Themes`, `AppIcons` |
| `surface/` | One class per screen: `JHubDashboard` (settings + data), `JLogCockpit`, `JLogContest`, `JMapView`, `JSatView`, `JDigiView`, `JBridgeView`, `JVaultView`, `JLearnView`, plus dialogs (`TranslatorWindow`, `ContestPicker`, ...) and `contest/` |
| `shell/` | Docking shell and shared widgets: `Shell`, `RigPanel`, `Tips`, `TxIndicator`, `FeedUi` |
| `data/` | Non-UI services: `HubConfig`, `ClusterClient`, `RigClient`, `RotorClient`, `AmpClient`, `DaemonManager`, `JLogDb`, `LogImport`/`LogExport`/`LogUpload`, `MacroConfig`, `SatService`/`TleStore`, `DigiEngine`, `BridgeService`, `LearnContent`, `Bands` |
| `sat/` | SGP4 propagation, pass prediction, Doppler, coordinate transforms |
| `morse/` | The Morse Trainer engine (audio, decoders, keyers, trainers) shown inside J-Learn |
| `mock/` | Sample data for screens |

Module surfaces: J-Hub, J-Log (normal + contest, with J-Log-Quick on the dashboard), J-Map, J-Sat, J-Digi, J-Bridge,
J-Vault, J-Learn. Modules can be switched off in J-Hub ▸ Modules.

## Embedded libraries

| Library | How ars-fx uses it |
|---|---|
| `j-digi` | `DigiEngine` runs `ModemService.startupEmbedded()` (audio capture only) and keeps the modem's PTT in step with the J-Hub rig settings (`rig.ptt`, `rig.rigctldHost`, `rig.rigctldPort`) and Transmit settings (`tx.cwWpm`, `tx.cwKeyer`) via `HubConfig.addChangeListener`, and switches the local CW skimmer from `digi.localSkimmer` (its spots go to the shared list through the hub: a window sends `spot.add`, `RemoteServer` calls `ClusterClient.addLocalSpot`), and passes it the rig's dial frequency and mode from `RigClient`; a library with no window or launcher of its own |
| `j-bridge` | `BridgeService` starts a `WsjtxUdpListener` on UDP 2237 (`-Dwsjtx.port`) and enriches each decode (callsign, band/mode, DXCC country, distance and bearing via `DxccInfo`); a library with no standalone app |
| `j-log-engine` | SQLite DAOs, contest plugins/scoring, Cabrillo export, `DxccResolver` |
| `j-log-common` | Shared models and `BandplanLoader` |

## State on disk

| Path | Contents |
|---|---|
| `~/.j-hub/config.json` | All suite settings (`HubConfig`, flat key/value): station, rig, cluster, macros, upload logins |
| `~/.j-log/` | `j-log.db`, `contest.db`, `config.db`, `translations.db`, `backups/`, `contest-logs/` |
| `~/.j-learn/content/` | J-Learn chapters, seeded on first open from the copy bundled in the jar (source: `../j-learn-content/`) |
| `~/.j-learn/morse/` | Morse Trainer settings and session logs |

## Conventions and gotchas

- **One band plan.** Use `data/Bands` (facade over `BandplanLoader`). Never add another band-edge table. The Station
  "IARU region" setting picks the plan (`Bands.applyRegion()` at start-up and on change).
- **Daemons** (`data/DaemonManager`) start rigctld / rotctld / ampctld when "Start … for me" is on. J-Hub's *PTT method* RTS or DTR
  adds `-P <type> -p <serial port>` to rigctld (CAT and VOX add nothing), and a daemon we started is restarted when its
  command line changes (the hub re-reads config on a `hub.reconfig` nudge). One we did not start is never touched.
- **Macros** are templates with `{TAGS}` (`MacroConfig.TAGS`, 42 of them) filled from the J-Hub settings; `{CALL}` is
  the worked station. A macro that needs an empty personal setting is refused with a message naming it.
- **Shared screens, one hub.** Data clients (`ClusterClient`, `RigClient` ...) must no-op their `start()` when a
  `RemoteLink` is active, so a docked or loose window never opens a second feed.
- **J-Learn content** lives in the repo root `../j-learn-content/` and is packaged into the jar; users update it from
  J-Hub ▸ Data ▸ J-Learn content.
- Solo launch files: `launch/j-map-local.json`, `j-map-remote.json`, `j-sat-remote.json` (run with `launch/run-solo.sh`); `--write-sample map|sat [file]` writes one.
- Themes: styles are in `ars.css`; the Morse Trainer views use `.mt-root` rules.

## Testing

JUnit 5 under `src/test/java/com/ars/fx` (the Morse Trainer and its Arduino keyer parser, plus `data` tests for `HubConfig` and ADIF import). Run `mvn test`.
Libraries have their own tests in each module.
