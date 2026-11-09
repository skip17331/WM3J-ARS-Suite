# CLAUDE.md — ars-fx

Guidance for Claude Code when working in the ARS Suite application. `ars-fx` is the app itself; `j-log-common`,
`j-log-engine`, `j-digi` and `j-bridge` are libraries it embeds (none has an app of its own). There is no root `pom.xml`:
each module is built on its own. `../installer/` is a separate small installer app (`com.hamradio.ars.installer`); it
is not part of this jar.

## What it is

One JavaFX 21 / Java 21 application that runs the whole shack, in two ways from the **same jar**. It runs on the
classpath (no `module-info.java`).

- **Docked** (default) — one window, a dock on the left switches between surfaces. Entry: `com.ars.fx.Launcher`.
- **Loose** — each module in its own window, all attached to a background hub (`launch/run-loose.sh`, see
  `launch/README.md`).
- **Solo** — J-Map or J-Sat alone in a window, driven by a JSON launch file (`launch/run-solo.sh`, `--config`),
  optionally against a remote hub over the LAN. J-Map and J-Sat are surfaces of this app, not separate modules.

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
../build-release.sh --verify   # only re-check the jars already in ../dist
```

All scripts are bash: on Windows run them from Git Bash (`ars-fx.bat` / `install.bat` are the Windows launchers).

Libraries must be in `~/.m2` before `ars-fx` builds: `j-log-common`, `j-log-engine`, `j-digi`, `j-bridge`. Each
platform profile writes a distinct `ars-fx-<dist>.jar`; the build checks that a desktop jar carries only its own
platform's JavaFX natives.

## Package map (`com.ars.fx`)

| Package | Contents |
|---|---|
| `com.ars.fx` | `Launcher`/`Main` (entry points), `HubServer`, `Theme`/`Themes`, `AppIcons`, `AppVersion` (build version stamped from `version.properties`; version compare for the update check) |
| `surface/` | One class per screen: `JHubDashboard` (dashboard + the setup page builders), `InfoView` (Info tab: Upcoming contests / DXpeditions from iCal feeds, a Links tile of plain links; contests default to the WA7BNM RSS feed (permission granted, credit required, 12-hour cache), `data/UpcomingEvents` + `data/IcalFeed`; a News tile of headlines from RSS/Atom feeds, `data/NewsFeeds` + `data/FeedParser`; both fetch through `data/FeedFetcher`), `JHubSetup` (the one Setup screen: second left menu, section swapped on the right; open a section with `Shell.navigate("setup:<id>")`), `JLogCockpit` (the normal-mode QSO form; it is no longer a page of its own: `JHubDashboard` embeds `embeddedEntry()` and the collapsing LOG TOOLS column from `sideDrawers()`), `JLogContest` (the one-screen contest page: entry bar built from the plugin's fields, the SEND line from `SentExchange`, entry checks from `ContestValidation`, tab order from `ContestLayout`, dupes asked first, edit / erase, spot tags via `data/MultCheck` and `data/CallHistory`, and its own LOG TOOLS column from `toolsColumn()`; `contest/` holds the trackers, maps, `NetStrip`, `QtcPane`), `AwardsWindow` + `CustomAwardsView` (the awards screen and My awards), `LegalView` (the local licence / Code of Conduct / legal notes), `JMapView` (also the NCDXF beacon overlay and its QSY / Auto QSY), `JSatView`, `JDigiView`, `JBridgeView`, `JVaultView`, `JLearnView`, plus dialogs (`TranslatorWindow`, `ContestPicker`, ...) and `contest/` |
| `shell/` | Docking shell and shared widgets: `Shell`, `RigPanel`, `Tips`, `TxIndicator`, `FeedUi`, `AutoPanel`, `Glyphs`, `IdTimer` (station-ID countdown), `UpdateBanner` |
| `data/` | Non-UI services: `HubConfig`, `ModuleConfig`, `SoloConfig`, `ClusterClient`, `RigClient`, `RotorClient`, `AmpClient`, `DaemonManager`, `RemoteLink`/`RemoteServer`, `JLogDb`, `JVaultDb`, `LogImport`/`LogExport`/`LogUpload`, `MacroConfig`, `SatService`/`TleStore`, `DigiEngine`, `BridgeService`, `DxccInfo`, `CtyData` + `CtyUpdater` + `ZoneLookup` (the contest country files, their weekly refresh into `~/.j-hub/cty/`, and the CQ zone from a grid square), `Awards` / `AwardLevels` / `CustomAwards`, `CallHistory` (what each call usually sends; fills the exchange, tags spots), `MultCheck`, `SpotNeeds`, `N1mm` + `N1mmBroadcast`, `LegalDocs`, `LinkOpener`, `LearnContent`, `Bands`, `UpdateChecker`/`UpdateApplier`, `AntennaCalcs` + `AntennaRecommender` (Antenna Workshop maths, shown by `surface/AntennaWorkshopView` inside J-Learn) |
| `net/` | The contest network (`ContestNetwork`): one station hosts, others join over the LAN, every station keeps a full copy of the contest log |
| `sat/` | SGP4 propagation, pass prediction, Doppler, coordinate transforms |
| `morse/` | The Morse Trainer engine (audio, decoders, keyers, trainers) shown inside J-Learn |


Module surfaces: J-Hub, J-Log (normal + contest, with J-Log-Quick on the dashboard), J-Map, J-Sat, J-Digi, J-Bridge,
J-Vault, J-Learn. The Morse Trainer and Antenna Workshop also have dock entries of their own (`ModuleConfig` ids `morse` and
`workshop`); they open inside the J-Learn screen. Modules can be switched off in J-Hub ▸ Setup ▸ Modules.

## Embedded libraries

| Library | How ars-fx uses it |
|---|---|
| `j-digi` | `DigiEngine` runs `ModemService.startupEmbedded()` (audio capture only) and keeps the modem's PTT in step with the J-Hub rig settings (`rig.ptt`, `rig.rigctldHost`, `rig.rigctldPort`) and Transmit settings (`tx.cwWpm`, `tx.cwKeyer`) via `HubConfig.addChangeListener`, and switches the local CW skimmer from `digi.localSkimmer` (its spots go to the shared list through the hub: a window sends `spot.add`, `RemoteServer` calls `ClusterClient.addLocalSpot`), and passes it the rig's dial frequency and mode from `RigClient`; a library with no window or launcher of its own |
| `j-bridge` | `BridgeService` starts a `WsjtxUdpListener` on UDP 2237 (`-Dwsjtx.port`, or J-Hub ▸ Data ▸ WSJT-X UDP port, `data.wsjtxPort`) and enriches each decode (callsign, band/mode, DXCC country, distance and bearing via `DxccInfo`); a library with no standalone app |
| `j-log-engine` | SQLite DAOs, contest plugins/scoring, Cabrillo export, `DxccResolver` |
| `j-log-common` | Shared models and `BandplanLoader` |

## State on disk

| Path | Contents |
|---|---|
| `~/.j-hub/config.json` | All suite settings (`HubConfig`, flat key/value): station, rig, rotor, amp, antenna, cluster, transmit |
| `~/.j-hub/info/` | Cached feeds for the Info tab (`<key>.feed` + `<key>.stamp`; six-hour cache, `FeedFetcher`) |
| `~/.j-hub/updates/` | Update-check log and downloaded update files plus helper script (`UpdateChecker`/`UpdateApplier`) |
| `~/.j-log/` | `j-log.db`, `contest.db`, `config.db`, `translations.db`, `callhistory.db` (`CallHistory`: each call's usual state / section / zone / grid / name), `backups/`, `contest-logs/`, `exports/`; also `macros.json` (`MacroConfig`), `upload.json` (`UploadConfig`: upload logins), `lookup.json` (`LookupConfig`) and `awards/*.json` (`CustomAwards`: the operator's own award sheets, one file each) |
| `~/.j-vault/` | `inventory.db` (J-Vault), `backups/` |
| `~/.j-sat/` | `tles.txt` TLE cache, `settings.json` (J-Sat selection and settings) |
| `~/.j-map/cache/` | Cached sun image for J-Map |
| `~/.j-learn/content/` | J-Learn chapters, seeded on first open from the copy bundled in the jar (source: `../j-learn-content/`) |
| `~/.j-learn/morse/` | Morse Trainer settings and session logs |

The j-digi modem keeps its own settings in Java Preferences, not here (see `../j-digi/CLAUDE.md`).

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
- **Update check.** `UpdateChecker.start()` (from `Launcher`) checks once a day, anonymously; it can be turned off in
  J-Hub ▸ Station ▸ Updates. `UpdateBanner` shows a pending update; `UpdateApplier` downloads it and runs the helper
  script bundled in `resources/com/ars/fx/update/` (`update-unix.sh`, `update-windows.ps1`).
- **Transmit safety.** `Launcher` shuts down `TxController` on exit so the transmitter is never left keyed; Esc stops
  transmit (`TxIndicator.installEscStop`).
- Solo launch files: `launch/j-map-local.json`, `j-map-remote.json`, `j-sat-remote.json` (run with `launch/run-solo.sh`); `--write-sample map|sat [file]` writes one.
- **Never call `java.awt.Desktop` on the FX thread.** On Linux it can deadlock against JavaFX's GTK and freeze the desktop. Open
  links, files and folders through `data/LinkOpener` (`open`, `openFile`, `mail`: background thread, `xdg-open` on Linux).
- **DXCC prefix table.** `j-log-engine/.../scoring/dxcc-prefixes.json` is generated from `cty.dat` and the ADIF entity codes by
  `../tools/dxcc-prefixes/gen_dxcc_prefixes.py` (see its README); do not hand-edit it, edit `base-prefixes.json` there.
  `CtyDataTest.engineTableAgreesWithCountryFiles` checks the two agree.
- Themes: styles are in `src/main/resources/com/ars/fx/css/ars.css`; the Morse Trainer views use `.mt-root` rules.

## Testing

JUnit 5 under `src/test/java/com/ars/fx` (the Morse Trainer and its Arduino keyer parser, plus `data` tests for `HubConfig` and ADIF import). Run `mvn test`.
Libraries have their own tests in each module.
