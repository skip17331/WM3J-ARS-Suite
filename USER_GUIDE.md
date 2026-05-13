# WM3J ARS Suite — User Guide

A practical guide for installing, configuring, and operating the suite.
See [README.md](README.md) for the project's purpose and license, and
[docs/ROADMAP.md](docs/ROADMAP.md) for what's planned next.

> **Wiring up actual radio gear?** Read [docs/HARDWARE_GUIDE.md](docs/HARDWARE_GUIDE.md)
> first — it's a beginner-friendly walk-through of cables, USB-to-serial
> adapters, USB hubs, and stable port names. This guide assumes the
> hardware side is already plugged in and that you can see your devices
> as serial ports on the PC.

---

## Table of contents

1. [Overview](#1-overview)
2. [Installation](#2-installation)
   - [All platforms — prerequisites](#21-all-platforms--prerequisites)
   - [Build order matters](#22-build-order-matters)
   - [Linux quick build](#23-linux-quick-build)
   - [Install to a non-default location](#24-install-to-a-non-default-location)
   - [Windows JavaFX](#25-windows-javafx)
   - [macOS](#26-macos)
   - [Optional dependencies — Hamlib and WSJT-X](#27-optional-dependencies--hamlib-and-wsjt-x)
3. [First-run setup](#3-first-run-setup)
4. [Web UI walkthrough](#4-web-ui-walkthrough)
5. [Per-app notes](#5-per-app-notes)
6. [Troubleshooting](#6-troubleshooting)
7. [Appendix: interconnection map](#7-appendix-interconnection-map)

---

## 1. Overview

The suite is **one broker** (J-Hub), **six JavaFX desktop apps** (J-Log,
J-Map, J-Digi, J-Bridge, J-Sat, Morse Trainer), and **two web apps**
(J-Vault, J-Learn) that J-Hub iframes for a single-pane experience:

| Name        | What it does                                                          | Default ports                |
|-------------|-----------------------------------------------------------------------|------------------------------|
| `j-hub`     | Central WebSocket broker + web control surface + service manager. Hosts the **Antenna Workshop** (recommender + 27 calculators); iframes J-Vault and J-Learn for a unified UI. | 8080 (WS), 8081 (HTTP)       |
| `j-log`     | QSO logger: casual (Normal) + contest (68+ plugins) + awards          | none (connects to j-hub)     |
| `j-map`     | DX map, grayline, propagation model, satellite/lunar/aurora overlays  | none                         |
| `j-digi`    | Native digital modem — CW, RTTY, PSK31/63/125, Olivia, MFSK, Feld Hell | none (uses audio devices)   |
| `j-bridge`  | Bridges WSJT-X to the suite via UDP 2237                              | 2237 (WSJT-X UDP)            |
| `j-sat`     | Satellite pass tracker, rig/rotor auto-tune during passes             | 4540 (TLE API)               |
| `j-vault`   | Shack inventory + first-call contacts + Estate Handoff PDF wizard. Standalone JavaFX app with its own embedded Jetty + SQLite. | 8083 (HTTP) |
| `j-learn`   | Amateur-radio reference library web app. Pure Jetty + HTML; markdown content seeds to `~/.j-learn/content/` so users can edit without rebuilding. | 8082 (HTTP) |
| `morse-trainer` | Standalone JavaFX Morse code trainer — letter / group / QSO simulator + sending practice with real-time decoding. Launchable from J-Hub or directly. | none                  |

**J-Learn** is its own process. The J-Hub web UI iframes it as the J-Learn
tab, but you can also visit `http://localhost:8082/` directly from any
browser on the LAN — phone, tablet, the shack laptop. ~200 sections
covering propagation, antennas, RF safety, troubleshooting, formulas,
operating practice, and emcomm; with click-through deep-links to the
matching calculators in J-Hub's Antenna Workshop.

**Traffic flow:** every app opens a WebSocket to `j-hub` on port 8080, sends
`APP_CONNECTED`, and joins a shared event stream. When a spot arrives, a
callsign is looked up, or the rig changes frequency, every connected app
sees it. J-Vault is the exception — it's a self-contained service that
keeps its own state in `~/.j-vault/inventory.db` and serves its own UI
on port 8083; J-Hub launches it as a child process and embeds its UI in
the J-Vault tab via iframe.

**Config lives in one place:** `~/.j-hub/j-hub.json`. Edit via the
web UI at `http://localhost:8081` — or by hand if you prefer. J-Vault
keeps a separate, isolated database under `~/.j-vault/` since estate /
inventory data is sensitive enough to warrant its own file.

**One settings surface.** J-Hub's web UI is the source of truth for
every operator preference that affects the broader suite — callsign +
grid, IARU region + country (used for bandplan captions across modules),
Hamlib endpoint, PTT method + CW keyer for J-Digi, DX cluster, log
uploaders, macros. Changes propagate live over the broker via
`STATION_CONFIG` / `CONFIG_UPDATE` messages — no per-app restart, no
duplicate JSON files to keep in sync. Modules cache the last-known
values so they can still run stand-alone after a hub disconnect.

---

## 2. Installation

> **The canonical install guide is [INSTALL.md](INSTALL.md)** — focused
> step-by-step setup for Linux and Windows with distro-specific package
> manager lines, the Windows JavaFX SDK swap, and an updating section.
> What follows here is a brief recap and the cross-platform notes that
> apply once you're past the basic install.

### 2.1 All platforms — prerequisites

- **Java 21 or newer.** Any JDK works (Temurin / OpenJDK / Zulu / Oracle
  / Microsoft Build of OpenJDK).
- **Maven 3.8+** for building from source.
- **Git** for cloning the repository.
- **Disk:** ~500 MB for source + built jars; plus ~50 MB per active
  log database and per-app data dirs (`~/.j-hub/`, `~/.j-vault/`,
  `~/.j-log/`, `~/.j-map/`, `~/.j-sat/`).
- **RAM:** 512 MB for J-Hub alone; ~1.5 GB if all modules are running.
- **Optional:**
  - **Hamlib** (`rigctl` / `rotctld` / `ampctld`) — rig / rotor / amp control.
  - **WSJT-X** — only if you run FT8/FT4/MSK144 through the J-Bridge integration.

Check Java is installed:

```bash
java --version
# Expected: openjdk 21.x.x ...   or similar
```

### 2.2 Build order matters

The suite has dependencies between modules. Build in this order:

```
j-log-engine    (shared library — install first)
j-learn         (standalone web app — install so J-Hub can launch it)
j-vault         (standalone web app — install so J-Hub can launch it)
j-hub, j-log, j-map, j-digi, j-bridge, j-sat, morse-trainer   (any order)
```

The `j-log-engine`, `j-learn`, and `j-vault` modules need `mvn install`
(not just `package`) so their jars land in your local `.m2` cache. The
others just need `mvn package`.

### 2.3 Linux quick build

```bash
git clone https://github.com/skip17331/WM3J-ARS-Suite.git ~/ARS_Suite
cd ~/ARS_Suite
mvn -q -DskipTests -f j-log-engine/pom.xml install
mvn -q -DskipTests -f j-learn/pom.xml install
mvn -q -DskipTests -f j-vault/pom.xml install
for m in j-hub j-log j-map j-digi j-bridge j-sat morse-trainer; do
  mvn -q -DskipTests -f "$m/pom.xml" package
done
./install.sh
./j-hub/start.sh
```

### 2.4 Install to a non-default location?

```bash
export ARS_SUITE_HOME=/opt/ars-suite   # Linux/macOS
setx ARS_SUITE_HOME C:\opt\ars-suite   # Windows (System Properties → Env Vars)
```

All apps honor this when launched from a child process by J-Hub.

### 2.5 Windows JavaFX

The repo's `lib/javafx` symlinks point at a Linux SDK. On Windows, copy
the Windows JavaFX 21 SDK into each module's `lib\javafx\` after cloning.
Step-by-step PowerShell loop in **[INSTALL.md § Step 3](INSTALL.md)**.

### 2.6 macOS

```bash
brew install --cask temurin21
brew install maven git
# then the same source/build flow as Linux
```

If Gatekeeper complains about an unsigned JAR, run once:

```bash
xattr -dr com.apple.quarantine ~/ARS_Suite
```

### 2.7 Optional dependencies — Hamlib and WSJT-X

The suite does not auto-install these. Open j-hub's web UI after startup —
the Dashboard has a **System Dependencies** card that detects both and shows
the correct install command for your OS if either is missing. The **Re-check**
button re-probes after you install them.

**Hamlib quick install:**

| OS        | Command                                              |
|-----------|------------------------------------------------------|
| Debian/Ubuntu | `sudo apt install libhamlib-utils`              |
| Fedora    | `sudo dnf install hamlib`                            |
| Arch      | `sudo pacman -S hamlib`                              |
| macOS     | `brew install hamlib`                                |
| Windows   | Download from <https://hamlib.github.io/>            |

**WSJT-X quick install:**

| OS        | Command                                              |
|-----------|------------------------------------------------------|
| Debian/Ubuntu | `sudo apt install wsjtx`                        |
| Fedora    | Download `.rpm` from <https://wsjt.sourceforge.io/wsjtx.html> |
| Arch      | `yay -S wsjtx` (AUR)                                 |
| macOS     | `brew install --cask wsjtx`                          |
| Windows   | Download installer from <https://wsjt.sourceforge.io/wsjtx.html> |

---

## 3. First-run setup

1. Launch `j-hub` (`./start.sh` on Linux/macOS, `start.bat` on Windows).
   A small status window (the **Mini UI**) opens showing uptime, ports,
   connected apps, and Launch/Stop buttons for every managed module.
   Click **Open Web Config UI** to launch the browser-based configurator.
2. In the web UI, go to the **Station** tab and fill in:
   - **Callsign** and **Operator Name** (top row)
   - **Rig (Model)** and **Alias / Friendly Name** — these are independent.
     Model is the actual transceiver (e.g. `IC-746pro`); alias is the short
     nickname shown wherever a rig label appears (e.g. `Main`, `Backup`).
   - **Rig Operator** — leave blank to default to the operator name above;
     override for shared / club / multi-op stations.
   - **QTH**, **Grid Square**, **lat/lon**, **timezone**, **language**.
   - **IARU Region** (R1 / R2 / R3) and **Country Overlay** (default
     US FCC; leave blank for region-only). Drives the bandplan caption
     shown in J-Digi, J-Bridge, and J-Map's DX Info window.
   Save.
3. Go to the **Modules** tab. For each app you want to auto-launch when
   J-Hub starts, paste the launch command and tick **Auto-launch**. Example
   commands (Linux, default layout):
   - `j-log`:    `bash /home/$USER/ARS_Suite/j-log/j-log.sh`
   - `j-map`:    `bash /home/$USER/ARS_Suite/j-map/j-map.sh`
   - `j-bridge`: `bash /home/$USER/ARS_Suite/j-bridge/j-bridge.sh`
   - `j-digi`:   `bash /home/$USER/ARS_Suite/j-digi/j-digi.sh`
   - `j-sat`:    `bash /home/$USER/ARS_Suite/j-sat/j-sat.sh --launched-by-hub`
   - `j-vault`:  `bash /home/$USER/ARS_Suite/j-vault/j-vault.sh`

   J-Vault is launched the same way as the others but lives on its own port
   (8083) and keeps its own data directory. It does **not** join the
   broker WebSocket; J-Hub just spawns the process and embeds its UI in
   the J-Vault tab.
4. Optional but recommended: under **Rig Control**, configure either CI-V
   (serial port, baud, hex address) or Hamlib (rigctld host/port). Under
   **Rotor Control** do the same if you run a rotator. Under **Amp Control**
   if you run an amplifier with `ampctld`.
5. Under **DX Cluster**, pick a network (e.g. `dx.middlebrook.ca:8000`), enter
   your login callsign, tick **Auto-connect**, save.
6. Restart J-Hub. Auto-launched modules come up, register on the hub, and
   start sharing state. J-Vault appears in its own window on first launch
   (it's a JavaFX status window with an "Open in Browser" button).

---

## 4. Web UI walkthrough

The web UI lives at `http://localhost:8081`. Navigation is a left sidebar
with tabs in this order:

```
Dashboard · Station · Callsign · J-Log · J-Map · J-Digi · J-Bridge · J-Sat ·
J-Vault · J-Learn · Modules · Logging & Data · Macros · Rig Control ·
Rotor Control · Amp Control · Antenna Switch · DX Cluster · Antenna Workshop · Weather
```

Each tab saves independently via its own **Save** button.

### Dashboard

Live cockpit. Shows:

- **Rig Status**: current frequency, mode, power, source (CI-V / Hamlib / WSJT-X).
- **Rotor / Antenna**: azimuth, target, connection state.
- **Module Connections**: J-Log, J-Digi, J-Bridge, J-Map, J-Sat, J-Vault, and
  J-Learn each with a status dot and Launch / Stop buttons. J-Vault and
  J-Learn also have an Open button that switches to their tab (which iframes
  the standalone process).
- **Quick Actions**: reconnect rig, restart cluster, reload config, restart WS.
- **Connected Apps**: each registered WebSocket client with `up` / `msg` / `hb`
  age counters. Red dot = stale (no traffic in ~60 s and no heartbeat in ~45 s).
- **System Dependencies**: Hamlib and WSJT-X presence + version; **Re-check**
  button.

### Station

Operator identity. Two rows:

- **Callsign** + **Operator Name**.
- **Rig (Model)** (e.g. `IC-746pro`) + **Alias / Friendly Name**
  (e.g. `Backup`) — independent fields, both can be set freely.
- **Rig Operator** — overrides operator name for this rig (shared / club /
  multi-op stations); leave blank to default to operator name.

Plus QTH, grid, lat/lon, timezone, ARRL section, CQ/ITU zones, display
language (en / de / es / fr / it / pt). All of these propagate to every
module on next start (or live if the module supports it).

> **What "Language" covers today.**
>
> - **English + Spanish ship embedded** in every module's jar (or
>   `i18n.js` for web modules). Selecting `en` or `es` works out of
>   the box on a fresh install.
> - **German, French, Italian, Portuguese** ship as drop-in language
>   packs at `i18n-packs/<module>/messages_<lang>.properties` in the
>   repo. Activate by copying the relevant pack to
>   `~/.j-hub/lang/<module>/` (e.g. `~/.j-hub/lang/j-digi/messages_de.properties`).
>   The module will pick it up on the next launch — or live if it's
>   listening for `JHUB_WELCOME` / `STATION_CONFIG`.
> - **J-Log** has the deepest coverage (every menu, label, button, and
>   status string — 6 bundles, ~163 keys each).
> - **J-Digi, J-Bridge, J-Map, J-Sat, Morse Trainer** translate the
>   top-level UI surfaces (menus, buttons, panel headings, status
>   messages, dialog titles). Strings deep inside dialogs may still
>   appear in English until reported and added to the bundles.
> - **J-Learn** translates its UI chrome (search box, theme picker,
>   top-bar buttons); the ~200 chapters of body content remain
>   English-only — translating that is a content-team effort.
> - **J-Vault** translates its top header, equipment-card heading,
>   and filter dropdowns via `web/i18n.js`. Operator-written equipment
>   names and notes stay in whatever language they were typed in.
>
> All translations except English are machine-translated and would
> benefit from a native-speaker pass. Corrections only require editing
> the relevant `.properties` file — no Java rebuild needed for the
> external packs.

Under **Regional Settings**, set your **IARU Region** (R1 Europe/Africa,
R2 Americas, R3 Asia/Oceania) and optionally a **Country Overlay** (US
FCC sub-bands today; other countries can be added later). These drive
the bandplan caption shown in J-Digi (rig status bar), J-Bridge (status
panel) and J-Map (DX info window) — e.g. `20m   14074 kHz   DATA —
Digimodes / FT8`. Defaults are `IARU-R2` + `US`.

### Callsign

Configure callsign lookup providers: QRZ.com XML, HamQTH, HamDB, Callook,
local FCC ULS database. Set a priority chain (`auto`) or pin to a single
provider. Import the FCC ULS data locally for offline lookups.

### J-Log / J-Map / J-Digi / J-Bridge / J-Sat

Per-app font / display settings, plus app-specific extras:

- **J-Log** — global base font + per-pane overrides (status bar, data entry,
  QSO log table, info/bearing pane, DX + Heard-By panes). `0 = inherit`.
- **J-Map** — API keys (NOAA, OpenWeatherMap), map-image uploads
  (custom equirectangular world map, custom great-circle map).
- **J-Digi** — **Transmit & CW** card (PTT method `VOX` / `HAMLIB`,
  CW keyer `AUDIO` / `HAMLIB`, CW WPM) plus font sizes for waterfall,
  decode pane, status. Hamlib host/port come from the **Rig Control**
  tab — no duplicate field here. When `cw.keyer = HAMLIB`, J-Digi hands
  the text to `rigctld` (`b <text>`) so the rig's own keyer plays the
  Morse at its configured WPM. Changes apply live on the next transmit;
  no J-Digi restart needed.
- **J-Bridge** — font sizes; restart-on-save flushes WSJT-X UDP listener.
- **J-Sat** — satellite selection, elevation thresholds, TLE source URL +
  staleness threshold.

Each tab has **Save** and **Save Data & Restart** buttons. The latter
flushes + restarts the app so pane overrides take effect.

### J-Vault

Embedded view of the J-Vault web UI (loaded from `http://localhost:8083/`
in an iframe). Two cards:

- **J-Vault Process** — Launch / Stop / Open in Browser. Launch first if
  the iframe shows a connection error.
- **Embedded J-Vault** — text-size slider (80-160%) zooms the iframe;
  Reload button refetches.

The full J-Vault feature set (inventory CRUD, first-call contacts, Estate
Handoff PDF wizard) lives inside the iframe — see [§5 Per-app notes](#5-per-app-notes).

### J-Learn

Iframes the standalone J-Learn web app at `http://localhost:8082/`. Lazy-
loaded — the iframe is created the first time the tab opens, so no network
hit if J-Learn isn't running. Top of tab:

- **Launch / Reload / Open in Browser** — same lifecycle controls as J-Vault.
- Status text shows whether `:8082/api/health` answered.

The actual UI lives inside the iframe (the same one you'd get visiting
`http://localhost:8082/` directly): TOC sidebar with filter + Advanced
toggle, rendered markdown viewer, and a text-size slider.

Some chapters surface a banner that deep-links into J-Hub features. When
the iframe is embedded inside J-Hub, clicking the button posts a message
to the parent which then dispatches the action; standalone, the buttons
fall back to opening the corresponding J-Hub URL in a new tab.

| Chapter | Banner | Action |
|---------|--------|--------|
| §03 Morse | 🎧 Morse Code Trainer | Launches the standalone JavaFX trainer app |
| §07 Antenna Workshop | 📡 Antenna Workshop | Opens the matching antenna calculator |
| §15 Formulas | 📐 Formula Calculator | Opens the matching per-formula calculator |

### Modules

Launch configuration for each managed app — J-Log, J-Digi, J-Bridge, J-Map,
J-Sat, J-Vault, J-Learn. Per app:

- **Launch Command** — shell command that launches the app (platform-specific).
- **IP** — reserved for future remote-launch support; leave `localhost`.
- **Auto-Launch** toggle — start automatically when J-Hub starts.
- **Launch / Stop / Save** buttons + a status message.

### Logging & Data

Three things in one tab:

- **Log Uploaders** — push QSOs from `~/.j-log/j-log.db` to **eQSL.cc**,
  **Club Log**, **QRZ Logbook**, and **HRDLog**. Credentials encrypted
  on disk in `~/.j-hub/credentials.enc` (AES-GCM, key tied to this
  machine). Already-uploaded QSOs tracked in an `upload_state` table so
  subsequent runs only push new ones.
- **Log Database** — switch between multiple `.db` files in `~/.j-log/`,
  add / set active / delete, ADIF import + ADIF/CSV export, **Backup
  Active DB** for a timestamped sidecar copy.
- **Configuration Backup & Export** — back up `~/.j-hub/`, `~/.j-log/`,
  `~/.j-map/`, `~/.j-sat/`, `~/.j-digi/`, `~/.j-bridge/` as a folder or
  to a WebDAV target, with rotation. Plus **Export Diagnostics** which
  bundles every app's logs + config + dep-check results into a zip.

### Macros

Two-mode (Digital/CW vs Voice) macro editor with a **Macro Variables
Reference** card listing every `{VAR}` placeholder supported by the
shared `MacroVariableEngine`:

| Placeholder | Source       | Description |
|-------------|--------------|-------------|
| `{MYCALL}`  | Station config | Operator's own callsign |
| `{CALL}`    | QSO entry    | DX (worked) callsign |
| `{RST}`, `{RST_S}`, `{RST_R}` | QSO entry | Sent / received signal report |
| `{NAME}`    | QSO entry    | DX operator name |
| `{EXCH}`    | QSO entry    | Contest exchange / notes |
| `{SERIAL}` / `{NR}` | Logger | Zero-padded / bare serial number |
| `{FREQ}`    | Live rig     | Rig frequency in MHz, three decimals |
| `{BAND}`    | Derived      | Band tag derived from `{FREQ}` |
| `{MODE}`    | Live rig     | Current operating mode |

Voice macros store **WAV recordings** alongside text macros — record
once, replay during a contest exchange. Unknown placeholders pass
through untouched so future variables don't break old macros.

### Rig Control / Rotor Control / Amp Control

Choose a backend (`CI_V`, `HAMLIB`, or `NONE`) and configure the relevant
parameters (serial port + baud + CI-V address; or rigctld/rotctld/ampctld
host + port). Supports hot-swap: change the backend and save; J-Hub
reconnects without a full restart.

The **Rig Control → Hamlib** host/port set here is the single
station-level `rigctld` endpoint. When J-Digi's PTT method or CW keyer
is set to `HAMLIB`, J-Digi reuses *this* endpoint — no duplicate config
in the J-Digi tab.

### Antenna Switch

Configure a serial-controlled antenna switch with a per-switch command
template. Optional **lockout on PTT** prevents switching while
transmitting. Rules let you tie specific bands or rigs to specific
antennas automatically.

### DX Cluster

Pick from a list of networks or add your own, set filters (bands, modes),
auto-connect toggle. Plus an opt-in **Reverse Beacon Network** feed on
parallel telnet that streams skimmer-decoded spots into the same broadcast
(tagged `source:"RBN"` for distinct rendering downstream). Raw telnet
stream and parsed spots both flow to every connected app — J-Log shows
them in the DX Spotting pane, J-Map plots them on the world map.

### Antenna Workshop

Two sub-tabs:

- **Calculators** (default) — 13 antenna calculators (flat dipole,
  inverted-V, fan dipole, trapped dipole, OCF/Windom, EFHW with/without
  traps, J-pole, Yagi-Uda, vertical, loading coils, trap design,
  magnetic loop, NanoVNA trim workflow) plus 14 per-formula calculators
  for J-Learn's Formulas chapter (Ohm's law, power, reactance,
  impedance, resonance, wavelength, SWR, ERP, feedline loss, decibels,
  Q factor, bandwidth, Smith chart, RF exposure).
- **Recommender** — questionnaire-driven antenna picker. Doesn't render
  until you click the Recommender sub-tab (saves a few hundred ms on
  page load). Asks a handful of questions about your QTH, HOA status,
  bands, height, stealth needs, and budget, then ranks antennas that
  actually fit.

### Weather

Live Space Weather tiles (Kp, X-ray flux, IMF Bz, solar wind, proton flux)
plus local weather when an OpenWeatherMap key is configured in the J-Map
tab.

---

## 5. Per-app notes

Most apps need no special setup beyond what's in j-hub's web UI. The exceptions:

### J-Log

- **Modes** — Normal Log (casual QSOs) or Contest Log. Choose at startup from
  the splash screen; the active choice drives which pane layout loads.
- **Contest plugins** live in `~/.j-log/contests/` (user-installed) or bundled
  inside `j-log-engine.jar`. Import a community plugin via **File → Import
  Plugin…** from inside j-log.
- **Awards** live in `~/.j-log/awards/`. Drop a JSON plugin into that folder
  and click **Refresh** on the Awards dashboard.
- **POTA/SOTA activation mode** — launch j-log with `--activation` to reveal
  the activation bar (SIG + SIG_INFO fields that stamp every QSO).
- **CW/Digital keyer** — toggle via **Tools → CW / Digital Keyer**. The
  embedded pane drops into the main window; right-click RX text to send
  selection to callsign/name/state fields.
- **QSL printing** — **File → Print QSL Cards…** opens a 4-up layout that
  prints either the currently-selected rows or all unsent cards. Option to
  auto-mark QSL sent after print succeeds.

### J-Map

- First run asks for Station lat/lon; without it, grayline and bearings are
  useless. Set them in the **Station** tab before starting j-map.
- Default world map is a basic blue-marble. Upload any equirectangular jpg
  under **J-Map → Map Images** to replace it.
- Floating windows (DX Info, Contest List, Countdown, Propagation, Lunar)
  can be dragged, toggled individually, and their positions persist.
- **DX Info bandplan caption** (`14074 kHz   DATA — Digimodes / FT8`)
  follows the **IARU Region + Country Overlay** set under J-Hub's
  Station → Regional Settings. Updates live without a J-Map restart.

### J-Digi

- Requires working audio input/output. Pick devices in **Audio** menu.
- Doesn't interface with WSJT-X — use j-bridge for that. J-Digi is for
  keyboard-to-keyboard and legacy digital modes (RTTY, PSK31, Olivia,
  MFSK, Feld Hell).
- **Transmit & PTT** — pick **VOX** (audio-sensing — works without CAT,
  required if you use SignaLink/DigiRig audio-PTT) or **HAMLIB** (J-Digi
  sends `T 1`/`T 0` to the station's `rigctld`). HAMLIB is required for
  pure CW since there's no audio for VOX to sense. Set in **J-Hub →
  J-Digi → Transmit & CW**.
- **CW keying paths** — set `cw.keyer = AUDIO` (default) for synthesized
  sidetone through the soundcard, or `cw.keyer = HAMLIB` to hand the
  text to the rig's built-in keyer over CAT (`b <text>`). Both paths
  share the **CW WPM** setting on the same card. AUDIO works with VOX;
  HAMLIB is cleanest if you have CAT but no audio path to the rig.
- **Bandplan caption** in the status bar follows the **IARU Region** +
  **Country** set on J-Hub's Station tab.
- **Contest mode** — when j-log enters a contest, j-digi gets a
  `CONTEST_ACTIVE` message and rebuilds its entry form to match the
  contest's exchange schema. QSOs logged from j-digi go directly to the
  shared contest DB via j-log-engine.

### J-Bridge

- Listens for WSJT-X UDP broadcasts on port 2237 by default.
- Configure WSJT-X: **File → Settings → Reporting → UDP Server**:
  - Server: `127.0.0.1`
  - Port: `2237`
  - **Accept UDP requests**: checked
- The WSJT-X sidebar panel in j-bridge shows connection status (green dot +
  version string when connected). If WSJT-X was already running before
  j-bridge started, click the **Reconnect** button in that panel.
- **Frequency bandplan caption** in the status panel follows the
  **IARU Region + Country Overlay** set under J-Hub's Station tab.

### J-Sat

- Loads TLE data on first run (a few hundred KB from Celestrak). Configure
  TLE source and staleness threshold in the **J-Sat** tab.
- Rig and rotor control are independent toggles — both require their
  respective backend in j-hub's Rig/Rotor Control tabs.
- Select active satellites on the J-Sat tab's satellite list; only selected
  birds appear in the upcoming-passes list.

### J-Vault

- **Standalone process on port 8083.** Launch from J-Hub's Modules panel
  or the Mini UI, or directly via `bash j-vault/j-vault.sh`. Opens its
  own JavaFX status window with **Open in Browser** / **Hide** / **Quit**
  buttons.
- **Data lives in `~/.j-vault/inventory.db`** (separate from `~/.j-hub/`).
  On first launch, J-Vault checks for a legacy `~/.j-hub/inventory.db`
  (from before the J-Vault split) and copies it forward — no data lost.
- **Estate Handoff PDF wizard** — click **📄 Estate Document…** in the
  Inventory toolbar. Pick which sections to include via Include/Exclude
  radio pairs (first-call contacts, equipment inventory, value summary,
  sale recommendations, step-by-step instructions, glossary). Click
  **Download PDF** — produces a real `.pdf` file in your browser's
  Downloads folder via bundled jsPDF + jspdf-autotable. No print
  dialog, no copy-paste.
- **Filter by disposition** in the wizard — All / Working only /
  Working + Repairable (sellable).
- **Personal note** field on the wizard prints on the cover page.
- **Type-specific hints** in the Add Item modal change as you switch
  the Type dropdown — radios get hints about firmware versions, coax
  runs get hints about model = type+length, towers get hints about
  guy material, etc.

### J-Learn

- **Standalone web app** on port 8082. J-Hub iframes it as the J-Learn
  tab; the same URL works directly in any browser on the LAN.
- **Editable content** — on first run, the bundled markdown is seeded to
  `~/.j-learn/content/`. Edit a file there, hit Reload in the J-Learn tab,
  and the change shows up immediately. No rebuild required.
- **Settings** — `~/.j-learn/settings.json` lets you change the listen
  port. Override per-launch with `-Djlearn.port=NNNN`.
- **Search box** filters the TOC by title or section ID. Typing `15-`
  narrows to chapter 15 (Formulas); typing `emcomm` jumps to chapter 20.
- **Advanced toggle** above the TOC shows / hides
  `> ⚙️ **Advanced —**` callouts (Extra-class / engineering-depth
  paragraphs). Default is hidden.
- **Text-size slider** at the top of the page scales the rendered viewer
  between 80-180%. Persists per browser via `localStorage`.
- **Direct deep-links** — `http://localhost:8082/?section=04-03` opens
  J-Learn straight to that section. Used by the iframe inside J-Hub for
  cross-module navigation.
- **Cross-references** look like `§NN-NN` in prose. Most chapters end
  with a "See also" section linking to related sections.
- **Per-chapter banners** (§03 Morse, §07 Antenna Workshop, §15 Formulas)
  inject a Launch / Open button at the top of every section in that
  chapter — see the table in the Web UI walkthrough.

---

## 6. Troubleshooting

### Nothing connects to j-hub

- Check `~/ARS_Suite/j-hub/logs/j-hub.log` for `WebSocket server ready on :8080`.
- Port conflict? `ss -tlnp | grep 8080` should show only the j-hub Java process.
- Firewall: allow TCP 8080 and 8081 locally.

### WSJT-X doesn't show as connected in j-bridge

Full diagnostic in the [README](README.md) — but the typical issues:

1. **Accept UDP requests** not ticked in WSJT-X's Reporting settings.
2. Another tool (JTAlert, GridTracker, JS8Call) already bound to port 2237.
   Unicast UDP only delivers to one receiver — either shut the conflicting
   tool down or switch WSJT-X to multicast (`239.255.0.0`) and have each
   tool join the group.
3. Start-order — if j-bridge was started *after* WSJT-X, WSJT-X's socket
   may be stale. Click **Reconnect** in j-bridge's WSJT-X sidebar panel.

### Hamlib commands fail

Run `rigctl --version` at the command line. If it's missing, install per the
table in [section 2.7](#27-optional-dependencies--hamlib-and-wsjt-x). If it
runs but j-hub still can't talk to the rig, verify the serial port is
readable (`ls -l /dev/ttyUSB*`) and your user is in the `dialout` group on
Linux.

### J-Vault tab shows "connection refused" / blank iframe

J-Vault is a separate process listening on **port 8083**, not part of j-hub.
The J-Hub web UI's J-Vault tab is just an iframe to `http://localhost:8083/`.

1. Click **J-Vault → Launch** in J-Hub's left nav (or run `./j-vault/start.sh`
   directly), wait 3–5 seconds for Jetty to come up.
2. Click **Reload** on the embedded view (top-right of the J-Vault tab).
3. If Launch does nothing, check that `j-vault/target/j-vault-1.0.0.jar`
   exists. If not, build it: `mvn -DskipTests -f j-vault/pom.xml install`.
4. Port conflict on 8083? `ss -tlnp | grep 8083` — kill anything else
   bound there, or change `webPort` in `~/.j-vault/settings.json`.

### J-Learn tab shows "connection refused" or empty content

J-Learn is a separate process on port 8082. The J-Hub web UI's J-Learn tab
is just an iframe to `http://localhost:8082/`.

1. Click **Launch** at the top of the tab (or run `./j-learn/start.sh`),
   wait 3-5 seconds for Jetty to come up.
2. Click **Reload** on the embedded view.
3. If Launch does nothing, check that `j-learn/target/j-learn-1.1.0.jar`
   exists. If not, build it:
   `mvn -DskipTests -f j-learn/pom.xml install`.
4. Port conflict on 8082? `ss -tlnp | grep 8082` — kill anything else
   bound there, or change `port` in `~/.j-learn/settings.json`.

### J-Learn isn't picking up edited content

Markdown lives at `~/.j-learn/content/`. Edits show up on Reload — but
remember J-Learn falls back to the bundled jar copy *only* for files
missing from disk. If you renamed something on disk and it still shows
the old version, you're seeing the jar fallback; restore the file or
update the manifest.

### Awards Dashboard errors

The dashboard wraps load errors in a visible message. Most common cause: the
`~/.j-log/awards/` directory doesn't exist yet. Create it, drop an award JSON
into it, click **Refresh**.

### QSO won't save

- **Callsign** is always required.
- **Band** and **Mode**, if typed, must be recognized values — empty is OK.
  Check the lowered-case value against the supported list.
- The **Save** button is disabled when validation fails. Hover to see what's
  wrong in the status bar at the bottom of the window.

### Collecting a support bundle for bug reports

Open j-hub web UI → **Logging & Data** → **Configuration Backup & Export** →
**Export Diagnostics**. You get `ars-diag-<timestamp>.zip` containing every
module's logs, the current config snapshot, the live session list, dep-check
results, and OS/Java environment info. Attach it to your bug report.

### A crash dialog popped up

Every app has a global crash handler. An uncaught exception:

- Writes the full stack trace to `logs/<app>.log`.
- Pops a dialog with the stack trace and a **Copy to Clipboard** button.
- Does **not** exit the app — you can often keep working after saving.

Copy the stack trace into the bug report along with the diagnostics zip.

---

## 7. Appendix: interconnection map

```
                          ┌────────────────────────┐
                          │     j-hub  (broker)    │
                          │                        │
                          │  WS :8080   HTTP :8081 │
                          │                        │
                          │  ├─ MessageRouter      │
                          │  ├─ ConfigManager      │
                          │  ├─ ClusterManager     │──(telnet)──▶ DX Cluster
                          │  ├─ StateCache         │               (VE7CC / AR / etc.)
                          │  ├─ SpotEnricher       │──(https)──▶ HamQTH / QRZ
                          │  ├─ WeatherService     │──(https)──▶ NOAA SWPC
                          │  ├─ AppLauncher        │──(https)──▶ hamqsl.com
                          │  └─ AntennaWorkshop    │
                          │                        │
                          └─┬──┬──┬──┬──┬─┬──────┬───────┬──────┘
                            │  │  │  │  │ │      │       │
                            │  │  │  │  │ │      │  iframe
              ┌─────────────┘  │  │  │  │ │      │       │
              │     ┌──────────┘  │  │  │ │      │       └──────┐
              ▼     ▼             ▼  ▼  ▼ ▼      ▼              ▼
        ┌─────────┐ ┌─────────┐ ┌─────┐ ┌──────┐ ┌──────┐ ┌──────────┐ ┌──────────┐
        │  j-log  │ │  j-map  │ │j-digi│ │j-bridge│ │j-sat│ │ j-vault  │ │ j-learn  │
        │         │ │         │ │      │ │       │ │      │ │          │ │          │
        │ ┌─────┐ │ │ world   │ │ DSP  │ │ UDP   │ │ TLE  │ │ Inventory│ │ Markdown │
        │ │ DAO │ │ │ map +   │ │ +    │ │ :2237 │ │ pass │ │   DB     │ │ + manif. │
        │ │     │ │ │ overlays│ │ wave │ │  ↓    │ │ pred │ │ + Estate │ │ Pure web │
        │ └──┬──┘ │ │         │ │      │ │ WSJT-X│ │      │ │   PDF    │ │ (Jetty)  │
        └────┼────┘ └─────────┘ └──┬───┘ └───────┘ └──┬───┘ │          │ │          │
             │                     │                  │     │ HTTP:8083│ │ HTTP:8082│
             │   ┌──────────────┐  │                  │     └────┬─────┘ └────┬─────┘
             └──▶│ j-log-engine │◀─┘                  │          │            │
                 │   (shared)   │                     │          ▼            ▼
                 │ SQLite DAO + │                     │    ┌──────────┐ ┌──────────┐
                 │ ContestPlugin│                     │    │~/.j-vault│ │~/.j-learn│
                 │ HubEngine WS │                     │    │ /inv.db  │ │ /content │
                 └──────┬───────┘                     │    └──────────┘ └──────────┘
                        ▼                             ▼
                  ┌──────────┐               ┌────────────────────┐
                  │ ~/.j-log │               │  Rig / Rotor       │
                  │   /*.db  │               │  (Hamlib rigctld / │
                  └──────────┘               │   CI-V serial)     │
                                             └────────────────────┘
```

**Key wires:**

- **WebSocket (j-hub:8080)** — every JavaFX module's primary link. Carries
  `APP_CONNECTED`, `JHUB_WELCOME`, `STATION_CONFIG`, `RIG_STATUS`, `SPOT`,
  `LOGGER_SESSION`, `CONTEST_ACTIVE`, `SOLAR_FLUX`, `HEARD_BY_SPOT`,
  `QSO_SAVED`, `IMPORT_ADIF`, `MODEM_DECODE`, `MODEM_TX`, `CONFIG_UPDATE`,
  `HEARTBEAT`, `SHUTDOWN`, etc. Human-readable JSON with a `"type"`
  field. `JHUB_WELCOME` (sent on connect) and `STATION_CONFIG`
  (broadcast on save) carry the full station section — IARU region,
  country, callsign, grid, timezone, ARRL section, CQ/ITU zones — plus
  the station's Hamlib `rigctld` endpoint so modules that key the rig
  (J-Digi PTT / CW) reuse it rather than holding duplicate prefs.
- **HTTP (j-hub:8081)** — web config UI + REST: `/api/config`, `/api/status`,
  `/api/sessions`, `/api/deps`, `/api/diagnostics/bundle`, `/api/jlog`,
  `/api/jmap`, `/api/jdigi`, `/api/jbridge`, `/api/jsat`, `/api/db/*`,
  `/api/apps/*`, `/api/weather`, `/api/callsign/*`, `/api/antenna/*`.
- **HTTP (j-vault:8083)** — standalone Jetty for inventory CRUD, photo
  upload, and the Estate-Handoff PDF generator. The J-Hub web UI's
  **J-Vault** tab is just an iframe pointing here; J-Vault is otherwise
  decoupled from the broker.
- **HTTP (j-learn:8082)** — standalone Jetty serving the markdown reference
  library at `/` plus `/api/jlearn/manifest` and `/api/jlearn/content?id=`.
  The J-Hub web UI's **J-Learn** tab iframes it; cross-module banners
  (Workshop / Morse / Formulas) postMessage actions back to the parent.
- **UDP 2237 (WSJT-X ↔ j-bridge)** — WSJT-X's reporting protocol. j-bridge
  listens; WSJT-X broadcasts heartbeat, status, decode, QSO-logged packets.
- **Telnet (ClusterManager ↔ DX cluster)** — one connection per j-hub
  instance. Parsed and enriched spots flow to all modules via WebSocket.
- **j-log-engine** — Maven artifact embedded in j-log and j-digi. Holds the
  shared SQLite DAO (`QsoDao`, `ContestQsoDao`, `MacroDao`),
  contest/award plugin loaders, the `MacroVariableEngine` placeholder
  substitution layer, and the `HubEngine` WebSocket client. This is why
  j-digi can log RTTY contest QSOs directly to the shared DB without
  routing through j-log.
- **j-learn** — standalone web app on port 8082. Ships ~200 markdown
  sections, seeded to `~/.j-learn/content/` on first run. Decoupled from
  the broker; J-Hub launches it as a child process and iframes the UI.
- **SQLite databases** — log/contest data lives under `~/.j-log/`
  (`j-log.db`, `contest.db`, `config.db`, plus any user-created DBs).
  Inventory and estate data live under `~/.j-vault/inventory.db`. J-Hub
  config is a JSON file at `~/.j-hub/j-hub.json`. Read-only cross-process
  access is fine; j-hub's DB Browser uses it.
- **Rig control** — either CI-V serial (jSerialComm) or Hamlib (`rigctld`
  over TCP). j-hub's `HamlibRigController` polls and broadcasts `RIG_STATUS`
  every ~500 ms.

**Dependency chain for building from source:**

```
j-log-engine  (shared library — no deps within suite)
    ├── j-log
    └── j-digi

j-learn       (standalone web app — installed so j-hub can launch it)
j-vault       (standalone web app — installed so j-hub can launch it)

j-hub         (no Maven dep on j-learn or j-vault — discovers them at run-time)
j-map         (standalone)
j-bridge      (standalone)
j-sat         (standalone)
```

Build order: `j-log-engine` first (mvn install), then `j-learn` and
`j-vault` (mvn install), then any order for the rest with `mvn package`.

---

*Beta users: file issues with a diagnostics zip attached. Thanks for helping
shake the suite out.*
