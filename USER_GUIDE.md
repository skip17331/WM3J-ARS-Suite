# WM3J ARS Suite — User Guide

A practical guide for installing, configuring, and operating the suite.
See [README.md](README.md) for the project's purpose and license.

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
   - [Linux](#22-linux)
   - [macOS](#23-macos)
   - [Windows](#24-windows)
   - [Optional dependencies — Hamlib and WSJT-X](#25-optional-dependencies--hamlib-and-wsjt-x)
3. [First-run setup](#3-first-run-setup)
4. [Web UI walkthrough](#4-web-ui-walkthrough)
5. [Per-app notes](#5-per-app-notes)
6. [Troubleshooting](#6-troubleshooting)
7. [Appendix: interconnection map](#7-appendix-interconnection-map)

---

## 1. Overview

The suite is six JavaFX applications plus one broker:

| Name       | What it does                                                           | Default ports                    |
|------------|------------------------------------------------------------------------|----------------------------------|
| `j-hub`    | Central WebSocket broker + web config UI + service manager             | 8080 (WS), 8081 (HTTP)           |
| `j-log`    | QSO logger: casual (Normal) + contest (68+ plugins) + awards           | none (connects to j-hub)         |
| `j-map`    | DX map, grayline, propagation model, satellite/lunar overlays          | none                             |
| `j-digi`   | Native digital modem — CW, RTTY, PSK31/63/125, FT8/4, Olivia, MFSK     | none (uses audio devices)        |
| `j-bridge` | Bridges WSJT-X to the suite via UDP 2237                               | 2237 (WSJT-X UDP)                |
| `j-sat`    | Satellite pass tracker, rig/rotor auto-tune during passes              | 4540 (TLE API)                   |

**Traffic flow:** every app opens a WebSocket to `j-hub` on port 8080, sends
`APP_CONNECTED`, and joins a shared event stream. When a spot arrives, a
callsign is looked up, or the rig changes frequency, every connected app
sees it.

**Config lives in one place:** `~/ARS_Suite/j-hub/j-hub.json`. Edit via the
web UI at `http://localhost:8081` — or by hand if you prefer.

---

## 2. Installation

### 2.1 All platforms — prerequisites

- **Java 21 or newer.** Any JDK works (Temurin / OpenJDK / Zulu / Oracle
  / Microsoft Build of OpenJDK). JavaFX is bundled into each module's
  shaded JAR, so you do **not** need a separate JavaFX install.
- **Disk:** ~250 MB for the installed suite, plus ~50 MB per active
  log database.
- **RAM:** 512 MB for j-hub alone; ~1 GB if all modules are running.
- **Optional:**
  - **Hamlib** (`rigctl` / `rotctld`) — for rig or rotor control.
  - **WSJT-X** — only if you run FT8/FT4/MSK144 through the integration.

Check Java is installed:

```bash
java --version
# Expected: openjdk 21.x.x ...   or similar
```

### 2.2 Linux

Debian/Ubuntu:

```bash
sudo apt install openjdk-21-jdk
```

Fedora/RHEL:

```bash
sudo dnf install java-21-openjdk-devel
```

Arch:

```bash
sudo pacman -S jdk21-openjdk
```

Install the suite — pick **A** (release bundle) or **B** (build from source):

**A. Release bundle:**

```bash
mkdir -p ~/ARS_Suite
tar -xzf ars-suite-<version>.tar.gz -C ~/ARS_Suite --strip-components=1
cd ~/ARS_Suite
./install.sh      # writes .desktop entries + icons under ~/.local/share/
./j-hub/start.sh  # launch j-hub
```

**B. Build from source (requires Maven 3.8+):**

```bash
sudo apt install maven       # or dnf/pacman equivalent
git clone https://github.com/YOUR-USERNAME/ARS_Suite.git ~/ARS_Suite
cd ~/ARS_Suite

# j-log-engine is the shared library — install to local .m2 first
mvn -DskipTests -f j-log-engine/pom.xml install

# Build each module
for m in j-hub j-log j-map j-digi j-bridge j-sat; do
  mvn -DskipTests -f "$m/pom.xml" package
done

./install.sh       # writes menu entries
./j-hub/start.sh   # launch j-hub
```

**Install to a non-default location?** Set the env var:

```bash
export ARS_SUITE_HOME=/opt/ars-suite   # or wherever you put it
```

All apps honor this — it's the reference root when j-log / j-map / j-digi /
j-bridge / j-sat need to find j-hub's `start.sh`.

### 2.3 macOS

Install Java 21 via Homebrew:

```bash
brew install --cask temurin21
```

Then the release-bundle or source flow is the same as Linux:

```bash
mkdir -p ~/ARS_Suite
tar -xzf ars-suite-<version>.tar.gz -C ~/ARS_Suite --strip-components=1
cd ~/ARS_Suite/j-hub
./start.sh
```

If Gatekeeper complains about an unsigned JAR, run once with:

```bash
xattr -dr com.apple.quarantine ~/ARS_Suite
```

### 2.4 Windows

1. **Install Java 21.** Download Temurin from
   <https://adoptium.net/temurin/releases/?version=21>. Pick the `.msi`
   installer, tick "Set JAVA_HOME" and "Add to PATH" during setup.
2. **Unzip the release bundle** to `C:\ARS_Suite` (or any path you like;
   set `ARS_SUITE_HOME` if it's not `%USERPROFILE%\ARS_Suite\`).
3. **Build the modules** (if you cloned from source — release bundles skip
   this). Each module also has a convenience `.bat` in its root:
   ```cmd
   cd C:\ARS_Suite
   mvn -DskipTests -f j-log-engine\pom.xml install
   for %m in (j-hub j-log j-map j-digi j-bridge j-sat) do mvn -DskipTests -f %m\pom.xml package
   ```
4. **Run the installer** — creates Start-Menu shortcuts under
   `%APPDATA%\Microsoft\Windows\Start Menu\Programs\ARS Suite\`:
   ```cmd
   cd C:\ARS_Suite
   install.bat
   ```
5. **Launch j-hub** from the Start Menu, or directly:
   ```cmd
   C:\ARS_Suite\j-hub\start.bat
   ```

> **Note on paths:** apps default to looking for j-hub at
> `%USERPROFILE%\ARS_Suite\j-hub`. For any other location set the
> `ARS_SUITE_HOME` system environment variable (System Properties →
> Environment Variables) to the install root.

> **JavaFX on Windows:** each module expects a `lib\javafx\` folder next
> to its `target\` folder with the Windows JavaFX 21 JARs. If the release
> bundle didn't include them, download the Windows SDK zip from
> <https://gluonhq.com/products/javafx/>, extract the JARs into each
> module's `lib\javafx\`, and you're set.

### 2.5 Optional dependencies — Hamlib and WSJT-X

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
   A small status window opens showing uptime, ports, connected apps, and
   external dependency status. Click **Open Web Config UI** to launch the
   browser-based configurator.
2. In the web UI, go to the **Station** tab and enter your callsign,
   name, QTH, grid square, latitude/longitude, timezone, and language.
   Save.
3. Go to the **Modules** tab. For each app you want to auto-launch when
   j-hub starts, paste the launch command and tick **Auto-launch**. Example
   commands (Linux, default layout):
   - `j-log`: `bash /home/$USER/ARS_Suite/j-log/j-log.sh`
   - `j-map`: `cd /home/$USER/ARS_Suite/j-map && mvn javafx:run -q`
   - `j-bridge`: `bash /home/$USER/ARS_Suite/j-bridge/j-bridge.sh`
   - `j-digi`: `bash /home/$USER/ARS_Suite/j-digi/j-digi.sh`
   - `j-sat`: `bash /home/$USER/ARS_Suite/j-sat/j-sat.sh --launched-by-hub`
4. Optional but recommended: under **Rig Control**, configure either CI-V
   (serial port, baud, hex address) or Hamlib (rigctld host/port). Under
   **Rotor Control** do the same if you run a rotator.
5. Under **DX Cluster**, pick a network (e.g. `dxc.ve7cc.net:7300`), enter
   your login callsign, tick **Auto-connect**, save.
6. Restart j-hub. Auto-launched modules come up, register on the hub, and
   start sharing state.

---

## 4. Web UI walkthrough

The web UI lives at `http://localhost:8081`. Navigation is a left sidebar
with tabs; each tab saves independently via its own **Save** button.

### Dashboard

Live cockpit. Shows:

- **Rig Status**: current frequency, mode, power, source (CI-V / Hamlib / WSJT-X).
- **Rotor / Antenna**: azimuth, target, connection state.
- **Quick Actions**: reconnect rig, restart cluster, reload config, restart WS.
- **Connected Apps**: each registered WebSocket client with `up` / `msg` / `hb`
  age counters. Red dot = stale (no traffic in ~60 s and no heartbeat in ~45 s).
- **System Dependencies**: Hamlib and WSJT-X presence + version; **Re-check**
  button.

### Station

Operator identity: callsign, name, QTH, grid, lat/lon, timezone, ARRL section,
CQ/ITU zones, display language (en / de / es / fr / it). These propagate to
every module — change them once, everyone picks up the update on next start
(or live if the module supports it).

### Rig Control / Rotor Control

Choose a backend (`CI_V`, `HAMLIB`, or `NONE`) and configure the relevant
parameters. Supports hot-swap: change the backend and save; j-hub reconnects
without a full restart.

### DX Cluster

Pick from a list of networks or add your own, set filters (bands, modes),
auto-connect toggle. Raw telnet stream and parsed spots both flow to every
connected app — j-log shows them in the DX Spotting pane, j-map plots them
on the world map.

### Logging & Data

Database management for j-log:

- **Database selection** — switch between multiple `.db` files stored in
  `~/.j-log/`. Useful for multiple contest entries or keeping a personal
  log separate from club operations.
- **Add / Set Active / Delete** — create and manage databases without
  touching j-log.
- **Import / Export** — ADIF import (routes through a running j-log), ADIF
  and CSV export.
- **Backup** — `Backup Active DB` writes a timestamped copy next to the
  original in `~/.j-log/`.

### Modules

Launch configuration for each managed app. Per app:

- **Auto-launch** toggle — start when j-hub starts.
- **Command** — shell command that launches the app (platform-specific).
- **IP** — reserved for future remote-launch support; leave `localhost`.

### J-Log

J-Log-specific settings:

- **Display** — show/hide the Space Weather pane.
- **Global Base Font** — baseline UI font size.
- **Per-Pane Font Sizes** — individual overrides for status bar, data entry,
  QSO log table, info/bearing pane, DX + Heard-By panes. `0 = inherit`.
- **Save J-Log Settings** / **Save Data & Restart J-Log** — the latter flushes
  + restarts j-log so pane overrides take effect.

### J-Map / J-Digi / J-Bridge / J-Sat

Same pattern as the J-Log tab — each has font controls and a **Save Data &
Restart** button. J-Map adds API keys (NOAA, OpenWeatherMap) and map-image
uploads. J-Sat adds satellite selection, elevation thresholds, TLE source.

### Weather

Live Space Weather tiles (Kp, X-ray flux, IMF Bz, solar wind, proton flux)
plus local weather when an OpenWeatherMap key is configured in J-Map.

### Callsign

Configure callsign lookup providers: QRZ.com XML, HamQTH, HamDB, Callook,
local FCC ULS database. Set a priority chain (`auto`) or pin to a single
provider. Import the FCC ULS data locally for offline lookups.

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

### J-Digi

- Requires working audio input/output. Pick devices in **Audio** menu.
- Doesn't interface with WSJT-X — use j-bridge for that. J-Digi is for
  keyboard-to-keyboard and legacy digital modes (RTTY, PSK31, Olivia,
  MFSK, Feld Hell).
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

### J-Sat

- Loads TLE data on first run (a few hundred KB from Celestrak). Configure
  TLE source and staleness threshold in the **J-Sat** tab.
- Rig and rotor control are independent toggles — both require their
  respective backend in j-hub's Rig/Rotor Control tabs.
- Select active satellites on the J-Sat tab's satellite list; only selected
  birds appear in the upcoming-passes list.

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
table in [section 2.5](#25-optional-dependencies--hamlib-and-wsjt-x). If it
runs but j-hub still can't talk to the rig, verify the serial port is
readable (`ls -l /dev/ttyUSB*`) and your user is in the `dialout` group on
Linux.

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
                         ┌──────────────────────┐
                         │    j-hub  (broker)   │
                         │                      │
                         │  WS :8080   HTTP :8081 │
                         │                      │
                         │  ├─ MessageRouter    │
                         │  ├─ ConfigManager    │
                         │  ├─ ClusterManager   │──(telnet)──▶ DX Cluster
                         │  ├─ StateCache       │               (VE7CC / AR / etc.)
                         │  ├─ WeatherService   │──(https)──▶ NOAA SWPC
                         │  └─ AppLauncher      │──(https)──▶ hamqsl.com
                         │                      │
                         └──┬────┬────┬────┬────┘
                            │    │    │    │
               ┌────────────┘    │    │    └────────────────┐
               │                 │    │                      │
               ▼                 ▼    ▼                      ▼
         ┌─────────┐      ┌──────────┐   ┌──────────┐   ┌──────────┐
         │  j-log  │      │  j-map   │   │ j-digi   │   │ j-bridge │   ┌─────────┐
         │         │      │          │   │          │   │          │   │  j-sat  │
         │ ┌─────┐ │      │  world   │   │  DSP     │   │  UDP     │   │  TLE    │
         │ │ DAO │ │      │  map +   │   │  decoder │   │  :2237 ──┼──▶│  pass   │
         │ │     │ │      │  overlays│   │  + wave- │   │    │     │   │  predict│
         │ └──┬──┘ │      │          │   │  form    │   │    ▼     │   │         │
         └────┼────┘      └──────────┘   └────┬─────┘   │  WSJT-X  │   └────┬────┘
              │                               │         └──────────┘        │
              │     ┌──────────────────────┐  │                             │
              └────▶│ j-log-engine (lib)   │◀─┘                             │
                    │  shared SQLite DAO,  │                                │
                    │  ContestPlugin +     │                                │
                    │  HubEngine client    │                                │
                    └──────────┬───────────┘                                │
                               │                                            │
                               ▼                                            │
                         ┌──────────┐                                       │
                         │ ~/.j-log │                                       │
                         │   /*.db  │                                       │
                         └──────────┘                                       │
                                                                            ▼
                                                               ┌────────────────────┐
                                                               │  Rig / Rotor       │
                                                               │  (Hamlib rigctld / │
                                                               │   CI-V serial)     │
                                                               └────────────────────┘
```

**Key wires:**

- **WebSocket (j-hub:8080)** — every module's primary link. Carries
  `APP_CONNECTED`, `JHUB_WELCOME`, `RIG_STATUS`, `SPOT`, `LOGGER_SESSION`,
  `CONTEST_ACTIVE`, `SOLAR_FLUX`, `HEARD_BY_SPOT`, `QSO_SAVED`,
  `IMPORT_ADIF`, `MODEM_DECODE`, `MODEM_TX`, `CONFIG_UPDATE`, `HEARTBEAT`,
  `SHUTDOWN`, etc. Human-readable JSON with a `"type"` field.
- **HTTP (j-hub:8081)** — web config UI + REST: `/api/config`, `/api/status`,
  `/api/sessions`, `/api/deps`, `/api/diagnostics/bundle`, `/api/jlog`,
  `/api/jmap`, `/api/jdigi`, `/api/jbridge`, `/api/jsat`, `/api/db/*`,
  `/api/apps/*`, `/api/weather`, `/api/callsign/*`.
- **UDP 2237 (WSJT-X ↔ j-bridge)** — WSJT-X's reporting protocol. j-bridge
  listens; WSJT-X broadcasts heartbeat, status, decode, QSO-logged packets.
- **Telnet (ClusterManager ↔ DX cluster)** — one connection per j-hub
  instance. Parsed spots flow to all modules via WebSocket.
- **j-log-engine** — Maven artifact embedded in j-log, j-digi, j-wae. Holds
  the shared SQLite DAO (`QsoDao`, `ContestQsoDao`, `QtcDao`, `MacroDao`),
  contest/award plugin loaders, and the `HubEngine` WebSocket client. This
  is why j-digi can log RTTY contest QSOs directly to the shared DB without
  routing through j-log.
- **SQLite databases** — all under `~/.j-log/`: `j-log.db` (default normal
  log), `contest.db` (contest QSOs), `config.db` (preferences), and any
  user-created databases. Read-only cross-process access is fine; j-hub's
  DB Browser uses it.
- **Rig control** — either CI-V serial (jSerialComm) or Hamlib (`rigctld`
  over TCP). j-hub's `HamlibRigController` polls and broadcasts `RIG_STATUS`
  every ~500 ms.

**Dependency chain for building from source:**

```
j-log-engine  (shared library — no deps within suite)
    ├── j-log
    ├── j-digi
    └── j-wae

j-hub         (standalone — does not depend on j-log-engine)
j-map         (standalone)
j-bridge      (standalone)
j-sat         (standalone)
```

Build `j-log-engine` first and `mvn install` it to your local `.m2` cache,
then any order works for the others.

---

*Beta users: file issues with a diagnostics zip attached. Thanks for helping
shake the suite out.*
