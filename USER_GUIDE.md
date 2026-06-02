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
4. [J-Hub control panel](#4-j-hub-control-panel)
5. [Per-app notes](#5-per-app-notes)
6. [Troubleshooting](#6-troubleshooting)
7. [Appendix: interconnection map](#7-appendix-interconnection-map)

---

## 1. Overview

The suite is not one program — it's **J-Hub plus a set of independent apps that connect to it.** Understanding that hub-and-spoke shape explains everything else, including the most common new-user question: *"why does J-Hub always have to be running?"*

### The shape: one hub, many spokes

```
                         ┌─────────────────────────────────────┐
                         │               J-HUB                  │
   browser ── :8081 ───▶ │  • Web control surface (config UI)   │
   (you configure here)  │  • WebSocket broker      :8080       │
                         │  • Shared StateCache (rig, spots,    │
                         │    logger session, station config)   │
                         │  • DX-cluster / RBN / skimmer feeds   │
                         │  • Launches the apps (child procs)    │
                         └───────▲───────────────▲──────────────┘
                                 │  WebSocket :8080 (one per app) │
        ┌──────────┬──────────┬──┴───┬──────────┬───────┴───┬──────────┐
     J-Log      J-Map     J-Digi   J-Bridge   J-Sat    morse-trainer  …
   (logger)   (DX map)  (modem)  (WSJT-X)  (sats)     (CW practice)

   Web apps J-Hub launches and iframes into its own UI (also reachable directly):
     J-Learn  :8082   (reference library)      J-Vault  :8083  (inventory / estate)
```

Every app — J-Log, J-Map, J-Digi, J-Bridge, J-Sat — is its **own process** and opens a WebSocket to J-Hub on port **8080**. They do **not** talk to each other directly; J-Hub is the switchboard in the middle.

### Why J-Hub is always running

J-Hub isn't "the launcher you can close once the apps are up." It is the thing the apps *depend on the whole time*. It plays four roles at once:

1. **Message broker.** All inter-app traffic flows through it. When J-Map gets a DX spot, J-Bridge gets a WSJT-X decode, or the rig changes frequency, that event goes to J-Hub and J-Hub re-broadcasts it to every other connected app. Close J-Hub and the apps go silent to each other.
2. **Configuration authority.** Your callsign, grid, IARU region/country, Hamlib (rig/rotor/amp) endpoints, PTT/keyer, DX cluster, log uploaders, macros — all live in **one file** (`~/.j-hub/j-hub.json`) and are edited in J-Hub's web UI. J-Hub pushes them to the apps live over the broker (`STATION_CONFIG`, and per-app messages like `JMAP_CONFIG`). There are no per-app config files to keep in sync.
3. **Shared state cache.** J-Hub remembers the last rig status, the active logging session, and a buffer of recent spots. An app that starts late (or reconnects) gets the current picture replayed to it immediately on connect — so opening J-Map mid-session shows the spots that already arrived.
4. **Service manager + control surface.** J-Hub launches the apps as child processes (the **Modules** tab), serves the whole web UI at `http://localhost:8081`, and hosts the **Antenna Workshop** (recommender + calculators). It also iframes the two web apps (J-Learn, J-Vault) so everything lives in one window.

> **The handshake.** An app starts → opens the WebSocket → sends `APP_CONNECTED` → J-Hub replies `JHUB_WELCOME` and replays cached state + the current station config → the app is now live on the shared bus. This is why apps can be opened in any order and still end up consistent.
>
> **If the hub goes away**, the apps don't crash — each caches the last config it received and keeps working stand-alone — but they stop seeing each other's events and stop getting config updates until J-Hub is back.

### The apps at a glance

| Name | Type | What it does | Ports |
|------|------|--------------|-------|
| `j-hub` | broker + web UI (JavaFX tray + Jetty) | Switchboard, config authority, state cache, app launcher, Antenna Workshop | **8080** WS, **8081** HTTP |
| `j-log` | JavaFX desktop | QSO logger — Normal + contest (68+ plugins) + awards | — (WS client) |
| `j-map` | JavaFX desktop | DX map: grayline, propagation, satellite / lunar / aurora overlays | — (WS client) |
| `j-digi` | JavaFX desktop | Native digital modem — CW, RTTY, PSK, Olivia, MFSK, Feld Hell | — (audio devices) |
| `j-bridge` | JavaFX desktop | Bridges WSJT-X into the suite | **2237** UDP (WSJT-X) |
| `j-sat` | JavaFX desktop | Satellite pass tracker; rig/rotor auto-tune during passes | 4540 (TLE API) |
| `morse-trainer` | JavaFX desktop | CW trainer — letter/group/QSO drills + sending practice | — |
| `j-learn` | **web app** (Jetty) | Reference library (300+ sections); iframed as the J-Learn tab, also at `:8082` | **8082** HTTP |
| `j-vault` | **web app** (Jetty + SQLite) | Shack inventory + Estate-Handoff PDF; iframed as J-Vault, also at `:8083` | **8083** HTTP |

The two **web apps** (J-Learn, J-Vault) are the only ones you can also open straight from a browser on the LAN — phone, tablet, shack laptop — at their ports. J-Vault keeps its own database (`~/.j-vault/inventory.db`) because inventory/estate data is sensitive enough to warrant a separate, isolated file.

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
   (serial port, baud, hex address) or Hamlib (pick your rig from the
   dropdown + serial port + baud — J-Hub spawns `rigctld` for you). Under
   **Rotor Control** do the same if you run a rotator (J-Hub spawns
   `rotctld`). Under **Amp Control** likewise (J-Hub spawns `ampctld`).
   If you already run the daemons yourself (e.g. on a separate machine),
   turn off the "Start &lt;daemon&gt; for me" toggle and just point Host/Port
   at them.
5. Under **DX Cluster**, pick a network (e.g. `dx.middlebrook.ca:8000`), enter
   your login callsign, tick **Auto-connect**, save.
6. Restart J-Hub. Auto-launched modules come up, register on the hub, and
   start sharing state. J-Vault appears in its own window on first launch
   (it's a JavaFX status window with an "Open in Browser" button).

---

## 4. J-Hub control panel

Every tab, card by card. J-Hub's web UI at **http://localhost:8081** is the single place you configure the whole suite. J-Hub must be running to reach it (see §1). The left-hand nav has **20 tabs**; each tab is built from **cards**, and most editable cards have their own **Save** button.

How saving works (verified in `config.js` / `WebConfigServer`):
- Editable settings are written to **`~/.j-hub/j-hub.json`** and pushed **live** to the running apps over the broker — no restart needed (the per-app config tabs note where a restart *is* required).
- A small status line next to each Save button confirms the write.
- Read-only tabs (Dashboard) only display state streamed from the apps; nothing to save.

> 📷 *Screenshots:* each tab below references an image under `docs/images/` (e.g. `jhub-station.png`). Capture each tab at ~1400 px wide with representative data.

---

### 4.1 Dashboard — live status (read-only)

![J-Hub — Dashboard](docs/images/jhub-dashboard.png)

The landing tab. Nothing here is editable; it's the at-a-glance state of the station, fed live over the broker.

- **Rig Status** — current **frequency**, **mode**, **power (W)**, **source**, **band**, and the rig **alias** (from Station). "Last update" timestamps the most recent `RIG_STATUS` message. Shows dashes until a rig backend is connected (Rig tab).
- **Rotor / Antenna** — an **azimuth compass** (heading needle) and an **elevation** dial, plus the rotor **backend** name. Driven by the Rotor tab / Hamlib `rotctld`.
- **DX Cluster** — connection dot + state, the **server** in use, **spots/min**, and **total spots** this session. Configured on the Cluster tab.
- **Module Connections** — one row per app (**J-Log, J-Digi, J-Bridge, J-Map, J-Sat, J-Vault, J-Learn**): a status dot, a meta line ("Not connected" / port), and **Launch** / **Stop** buttons that start or kill that app's process. This is the quickest way to bring an app up without a terminal.
- **Quick Actions** — four buttons: **Reconnect Rig**, **Restart Cluster**, **Reload Config** (re-reads `j-hub.json`), **Restart WS** (restarts the WebSocket broker).
- **Connected Apps** — a live WebSocket roster: which apps currently hold a connection to the broker, with a count. (Distinct from Module Connections, which is about the *process*; this is about the *live WS session*.)
- **System Dependencies** — auto-detection of **Hamlib** and **WSJT-X** with a **Re-check** button. Tells you whether rig control and the WSJT-X bridge have what they need on this machine.

---

### 4.2 Station — operator identity & location

![J-Hub — Station](docs/images/jhub-station.png)

The suite-wide identity. These values are broadcast to every app via `STATION_CONFIG`, and grid/region drive the band-plan captions shown on rig frequency and DX spots. Stored under `station` in `j-hub.json`. **Save Station** writes the whole tab.

**Operator** card:
| Field | Notes |
|-------|-------|
| **Callsign** | Your station callsign (forced uppercase). Used by every module and as the default cluster/RBN login. |
| **Operator Name** | Display name. |
| **Rig (Model)** | Actual transceiver model (e.g. `IC-746pro`, `FTDX-10`). Independent of the nickname. |
| **Alias / Friendly Name** | Short nickname shown wherever the rig is referenced ("Main", "Backup"). |
| **Rig Operator** | Person actually using this rig. Defaults to Operator Name; override for shared / club / multi-op stations. |

**Location** card:
| Field | Notes |
|-------|-------|
| **QTH** | Free-text location. |
| **Grid Square** | Maidenhead grid (uppercase). Feeds J-Map centering, J-Sat look angles, distance/bearing. |
| **Latitude / Longitude** | Decimal degrees (step 0.0001). |
| **CQ Zone** (1–40) · **ARRL Section** · **ITU Zone** (1–90) | Used for contest exchanges and award context. |

**Regional Settings** card:
| Field | Notes |
|-------|-------|
| **Timezone** | Drives local-time displays. |
| **Language** | UI language — English, German, Spanish, French, Italian, Portuguese. |
| **IARU Region** | R1 (Europe/Africa/ME), R2 (Americas), R3 (Asia/Oceania). Controls band-edge / segment captions on rig frequency and DX spots. |
| **Country Overlay** | National sub-band overlay on top of the IARU region — currently only **US (FCC §97.301)**, or "(region only)". |

---

### 4.3 Rig — transceiver backend

![J-Hub — Rig Control](docs/images/jhub-rig.png)

Sets how J-Hub talks to your radio. Stored under `rig` in `j-hub.json`; a connection badge by the card title shows Disabled / connected / error.

**Backend** (segmented): **None** · **CI-V Direct** (native Icom CI-V, no Hamlib) · **Hamlib rigctld**.

*CI-V Settings* (shown for CI-V Direct):
| Field | Notes |
|-------|-------|
| **Serial Port** | e.g. `/dev/ttyUSB0` or `COM3`. |
| **Baud Rate** | 4800–115200 (default 19200). |
| **CI-V Address (hex)** | Icom radio address, e.g. IC-7300 = `94`, IC-7700 = `74`. |

*Hamlib rigctld* (shown for Hamlib):
- **Start rigctld for me** (toggle) — J-Hub launches the `rigctld` daemon from the model/port below. Turn **off** if you run rigctld yourself (e.g. another machine).
- **Hamlib bin folder (override)** — leave blank to auto-detect; paste the folder containing `rigctld(.exe)` etc. if the System Dependencies card says Hamlib is missing (Windows ZIP extracts to a versioned folder like `C:\hamlib-w64-4.5.5\bin`).
- *Managed mode* fields: **Rig Model** (curated dropdown of common Yaesu / Icom / Kenwood / Elecraft / FlexRadio / SDR rigs, or **Custom** → enter the Hamlib model id from `rigctld --list`), **Serial Port**, **Baud Rate** (Default + 1200–115200).
- *External mode* fields: **Host** + **Port** (where your rigctld is listening; default `localhost:4532`).

**Common Settings:** **Poll Rate (ms)** (100–5000, default 500) · **Enable PTT** toggle (let J-Hub key the transmitter).

**Buttons:** **Save Rig Settings** · **Test Comm Port** (spawns a throwaway rigctld with the form values, queries frequency, tears down — verify COM/baud/model *before* saving) · **Test PTT** (enabled once PTT is on) · **Download Diagnostics** (zip of all module logs + rig/rotor/amp status + Hamlib probe, for bug reports).

**Live Rig Readout** card (read-only): Frequency (MHz) · Mode · Band · Power (W).

---

### 4.4 Rotor — antenna rotator

![J-Hub — Rotor Control](docs/images/jhub-rotor.png)

Backend + manual control for an az (or az/el) rotator. Stored under `rotor`.

**Backend** (segmented): **None** · **Internal** (model + COM/serial port) · **Hamlib rotctld**.

*Hamlib rotctld* mirrors the Rig tab:
- **Start rotctld for me** toggle (managed vs external).
- *Managed:* **Rotor Model** (dropdown: Yaesu/Kenpro GS-232 family, Hy-Gain/Idiom Press, Green Heron RT-21, SPID, M2, Prosistel, EA4TX, AMSAT LVB/IF-100, Heathkit — or **Custom** from `rotctld --list`), **Serial Port**, **Baud Rate**.
- *External:* **Host** + **Port** (default `localhost:4533`).
- **Short-Path Offset (°)** (0–359) and **Custom Preset (°)** (0–359). **Save Rotor Settings**.

**Live Position & Manual Control** card:
- **Azimuth** + **Elevation** gauges with live needles and big numeric readouts.
- **Azimuth** point-buttons: NW · N · NE · W · E · SW · S · SE.
- **Elevation** jog: ▲ +10° / +5° · ▼ −5° / −10° · **Park (0°)** · **■ Stop**.
- **Presets:** Short Path · Long Path · Custom (uses the offsets above).

---

### 4.5 Amp — amplifier (Hamlib ampctld)

![J-Hub — Amp Control](docs/images/jhub-amp.png)

Hamlib `ampctld` backend + live amp telemetry. Stored under `amp`.

**Backend** (segmented): **None** · **Hamlib ampctld**.

*Hamlib ampctld:*
- **Start ampctld for me** toggle (managed vs external).
- *Managed:* **Amp Model** (Elecraft KPA1500, Gemini DX1200 / HF-1K, or **Custom** from `ampctld --list`), **Serial Port**, **Baud Rate**.
- *External:* **Host** + **Port** (default `localhost:4531`).
- **Poll Rate (ms)** (200–10000, default 1000) · **SWR Fault Threshold** (1.0–10.0, default 3.0).
- **Follow rig band changes** toggle · **Show visual fault alerts** toggle. **Save Amp Settings**.

**Live Status** card (read-only): Power · SWR · Forward (W) · Frequency. Levels read "unsupported" on amps that don't expose them.

---

### 4.6 Antenna — automatic antenna switching

![J-Hub — Antenna Switch](docs/images/jhub-antenna.png)

Drives serial relay switches from band / mode / rotor heading. Stored under `antenna`.

**Connection** card: **Enable automatic switching** toggle · **Serial Port** · **Baud** · **Lockout switching while PTT is keyed** (recommended) toggle · **Save Connection**.

**Current Status** card (read-only): Band · Mode · Heading · Active Antenna · the matching rule (or "No matching rule").

**Switches** card: a list of switch definitions; **+ Add Switch**.

**Rules** card: ordered list (**first match wins**, reorder with ↑/↓); **+ Add Rule**; **Save Switches & Rules**. The per-switch command template substitutes `{switch}` and `{antenna}`, with `\r` / `\n` for line endings (e.g. `SW{switch}={antenna}\r`).

---

### 4.7 Macros — text & voice

![J-Hub — Macros](docs/images/jhub-macros.png)

Define the macros every module shares (the same `MacroVariableEngine` expands them at QSO time). A segmented control at the top switches between **Digital / CW** macros (text) and **Voice** macros (recorded WAV). Stored under `macros`.

- **Macros** card: an editable list (label + content); **+ Add Macro**; **Save Macros**.
- **Macro Variables Reference** card: a live table of every placeholder, its current value, and which tab/app owns it:

| Placeholder | Filled from |
|-------------|-------------|
| `{MYCALL}` | Station tab → Callsign |
| `{CALL}` · `{RST}`/`{RST_S}`/`{RST_R}` · `{NAME}` · `{EXCH}` | the per-QSO **entry pane** in J-Log / J-Digi (defaults: RST `599`, NAME `OM`) |
| `{SERIAL}` (zero-padded `007`) · `{NR}` (bare `7`) | J-Log contest module |
| `{FREQ}` (MHz, 3-dp) · `{MODE}` | Rig Control tab / rig CAT |
| `{BAND}` | derived from `{FREQ}` |

Values shown as "(entry pane)" / "(logger)" aren't held by J-Hub — they're filled by the app at send time. Unknown placeholders are left untouched, so new variables never break old macros.

---

### 4.8 Antenna Workshop — recommender + calculators

![J-Hub — Antenna Workshop](docs/images/jhub-antworkshop.png)

Two sub-panes (top sub-nav):
- **Calculators** — pick an antenna from the list and design it in the panel: flat / inverted-V / fan / trapped / OCF dipoles, EFHW (with and without traps), J-pole, Yagi-Uda, verticals, loading coils, and trap manufacturing.
- **Recommender** — a questionnaire wizard (Back / Next, progress) that scores and lists antennas suited to your space, bands, and goals; **Start Over** resets it.

Results and calculators deep-link into the matching J-Learn sections for the underlying math (antenna chapter + formula chapter).

---

### 4.9 J-Learn — reference library (embedded)

![J-Hub — J-Learn](docs/images/jhub-learn.png)

Embeds the J-Learn web app (separate process on **:8082**) as an iframe — 300+ sections on propagation, antennas, RF safety, troubleshooting, formulas, operating practice, emcomm. Lazy-loaded (no network hit until you open the tab).

- Buttons: **Launch & Attach** (start the j-learn process if needed and load it), **Reload** (refresh the iframe after content edits), **Open in New Tab** (`http://localhost:8082/` — also reachable from any LAN browser).
- This tab also hosts the **Morse Code Trainer** launch row — **▶ Launch Trainer** opens the standalone JavaFX trainer in its own window.

---

### 4.10 Cluster — DX cluster, RBN & skimmer

![J-Hub — DX Cluster](docs/images/jhub-cluster.png)

Telnet feeds that fan spots out to J-Map and J-Log over the broker (`SPOT`). Stored under `cluster` / `rbn` / `skimmer`.

**Connection** card:
- **Saved Networks** — pick a stored node (or "manual entry"); **Delete** removes the selected one.
- **Connection Details** — **Host**, **Port** (default 7373), **Login Callsign** (defaults to station call).
- **Save as Network Name** + **Save Network** to store the current details.
- **Auto-Reconnect** toggle (exponential backoff). Buttons: **Connect** · **Disconnect** · **Save Settings**.

**Status** card (read-only): Status dot · Server · Spots/min · Total spots.

**Spot Filters** card: **Bands** and **Modes** checkbox grids; **Save Filters**, **All Bands**, **Clear Bands**. These filter the operator-spotted cluster feed.

**Reverse Beacon Network** card: **Enable RBN feed** toggle · Server (`telnet.reversebeacon.net`) · Port (7000) · Login (falls back to station call) · **Min SNR (dB)** · Bands / Modes grids · **Save & Restart RBN**. RBN spots arrive tagged `source:"RBN"` and have their *own* band/mode filter, separate from the cluster filter.

**Local CW Skimmer Server** card: same shape as RBN for a LAN skimmer (CW Skimmer Server / AR-Cluster format), default `127.0.0.1:7300`, tagged `source:"SKIMMER"`. **Save & Restart Skimmer**.

**Recent Spots** card: a live table (DX · Freq · Mode · Country · Dist · Brg · Time); **Refresh**.

**Raw Telnet Feed** card: the live node stream, a **Send to Telnet** box for commands (e.g. `SH/DX 10`, `SH/WWV`), and **Clear**.

---

### 4.11 Logging & Data — uploaders, backup, databases, ports

![J-Hub — Logging & Data](docs/images/jhub-logging.png)

- **Log Uploaders** — push QSOs from `~/.j-log/j-log.db` to **eQSL.cc, Club Log, QRZ Logbook, HRDLog**. Per-service rows with **Upload pending**. Credentials are encrypted in `~/.j-hub/credentials.enc` (AES-GCM, key tied to this machine); already-uploaded QSOs are tracked in an `upload_state` table and skipped on re-run.
- **Cloud Backup** — **Enable scheduled cloud backup** toggle; mode **Folder** (any sync client — Dropbox/Drive/OneDrive/iCloud) or **WebDAV** (Nextcloud/ownCloud: URL + user + password + **Save WebDAV Credentials**). **Schedule (hours, 0 = manual)**, **Keep last N backups**, and **Include directories** checkboxes (`.j-hub`/`.j-log`/`.j-map`/`.j-sat`/`.j-digi`/`.j-bridge`). **Save Settings** · **Back up now** · status line.
- **Database Tools** — manage logs in `~/.j-log/`: **Select Database** + **Refresh**, shown **Active** name (*restart J-Log to apply a switch*); **+ Add Database** · **Set Active** · **Delete**. Import/Export: **Export ADIF** · **Export CSV** · **Import ADIF…**. **Backup Active DB** (timestamped copy next to the DB).
- **Configuration Backup & Export** — **Export Config (JSON)** · **Import Config** · **Export Diagnostics** (logs + status snapshot) · **🐛 Report an Issue…** (pre-filled GitHub issue; export diagnostics first and attach).
- **J-Hub Ports** — **J-Hub IP** (address modules use to reach the hub), **WebSocket Port** (default 8080), **Web Config Port** (default 8081). **Both port changes require a J-Hub restart.** **Save Ports**.

---

### 4.12 Modules — process & launch management

![J-Hub — Modules](docs/images/jhub-modules.png)

The full version of the dashboard's Module Connections.

- **Per-module cards** — for each app, its **launch command** (auto-filled per OS by `JHubConfig.applyDefaults`) and **auto-launch** setting, plus Launch/Stop. This is where you point J-Hub at a module's launcher if it lives somewhere non-standard.
- **Connected WebSocket Sessions** — a live table (App · Version · Connected time) of who currently holds a broker connection; **Refresh**.

---

### 4.13 J-Log — logger preferences

![J-Hub — J-Log settings](docs/images/jhub-jlog.png)

Per-app settings for J-Log (stored and pushed to J-Log; some apply live, some need a J-Log restart — noted per control).

- **Display** card:
  - **Show Space Weather** — SFI / A / K / SN / MUF in the J-Log clock bar (restart to apply).
  - **Show Rig Control Pane** — live freq/mode/band/power next to Data Entry with Use Freq / Use Mode buttons (live, no restart).
  - **ID Timer** — station-ID countdown (FCC §97.119) in the Normal clock bar; flashes red when due; resets on each logged QSO or the "ID'd" button (Normal Log only; live). **ID interval (minutes)** (1–60, default 10).
- **Global Base Font** — baseline size slider (10–22 px) for all unstyled elements.
- **Per-Pane Font Sizes** — overrides for Status/Clock Bar, Data Entry, QSO Log Table, Info/Bearing Pane, and DX + Heard-By panes (restart to apply).
- Buttons: **Save J-Log Settings** · **Save Data & Restart J-Log**.
- **Community Plug-in Registry** — browse contributed contest/award plug-ins; one click installs the JSON into `~/.j-log/plugins/` or `~/.j-log/awards/`. **🔄 Refresh Registry**; see `docs/PLUGINS.md` for the schema.

---

### 4.14 J-Map — map display, overlays & windows

![J-Hub — J-Map settings](docs/images/jhub-jmap.png)

Per-app settings for J-Map (mostly applied when J-Map starts; window fonts apply live). A blue note at top explains running J-Map on a **second machine** with `bash j-map.sh --hub <host> --hub-ws-port 8080 --hub-web-port 8081` (it subscribes to this hub — no second cluster connection).

- **API Keys** — **Use Mock Data** toggle (offline sample data); **NOAA API Key** (solar/geomagnetic); **OpenWeatherMap API Key** (weather overlays).
- **Map & Core Overlays** — toggles: World Map · Grayline · DX Spots on Map · DX Spot Paths (great-circle spotter→DX) · Sun Position; plus **Grayline Opacity** slider (0–1).
- **Space Weather Overlays** — Aurora · Geomagnetic Alerts · Satellite Tracking.
- **Terrestrial Weather Overlays** — Weather · Troposcatter · Radar · Lightning · Weather Fronts · Surface Conditions.
- **Amateur Radio Overlays** — CQ Zones · ITU Zones · Grid Squares · Rotor Map.
- **Visible Windows** — DE Window (My Station) · DX Window · Contest List.
- **DX Spot Filters** — Band Filter (All/160–2 m) · Max Spot Age (min) · Show Callsigns on map.
- **Time Display** — Show Local · Show UTC · optional **Secondary Timezone** (IANA).
- **Solar & Propagation** — Solar Data Panel · Sunspot Graphic · Propagation Data · Band Conditions.
- **Global Base Font** (10–22 px) and **Per-Window Font Sizes** (DE Info, DX Info, Contest List, Propagation, Lunar/Planetary; 0 = inherit, applied live).
- **Map Images** — **Map Style** (Blue Marble / Cloudless / Night Lights / Political / Custom), **Region**, and uploads for a **Custom World Map** (equirectangular JPG → `~/.j-map/world_map.jpg`) and **Great Circle Map** for the rotor pane (`~/.j-map/gcm.jpg`).
- **Map Source & Data** — **Tile Provider** (Flat / OSM / ESRI / CartoDB), **Default Zoom**, **Default Center Lat/Lon**, **Refresh Interval (sec)**. (TLE/satellite sources live on the J-Sat tab.)
- Buttons: **Save & Apply J-Map Settings** · **Save Data & Restart J-Map**.

---

### 4.15 J-Digi — modem keying, CW & audio

![J-Hub — J-Digi settings](docs/images/jhub-jdigi.png)

- **Audio Setup Wizard** — **🔍 Probe Audio Devices** lists the machine's sound cards; pick **Input (RX)** + **Output (TX)**, **🔁 Run Loopback Test** (a detected tone with SNR > 6 dB confirms wiring), **💾 Save to J-Digi**. It also tells you which device names to set in WSJT-X → Settings → Audio.
- **Local CW Skimmer** (experimental) — toggle a multi-channel CW detector that publishes `LOCAL_SKIMMER_ACTIVITY` snapshots as a band-activity gauge.
- **Transmit & CW** — **PTT Method** (VOX vs HAMLIB `rigctld T 1/T 0`), **CW Keyer** (AUDIO — synthesised Morse vs HAMLIB — rig's built-in keyer), **CW WPM** (5–60). Hamlib host/port are inherited from the **Rig Control** tab.
- **Global Base Font** (10–22) and **Per-Pane Font Sizes** (RX/TX text, Frequency/Callsign, Status Bar, Toolbar, Entry/Macros; 0 = inherit, live).
- Buttons: **Save J-Digi Settings** · **Save Data & Restart J-Digi**.

---

### 4.16 J-Bridge — WSJT-X integration

![J-Hub — J-Bridge settings](docs/images/jhub-jbridge.png)

- **WSJT-X Integration** — **WSJT-X Executable Path** (used when J-Bridge launches WSJT-X). J-Bridge listens for WSJT-X on UDP 2237.
- **Global Base Font** (10–22) and **Per-Pane Font Sizes** (Toolbar/Title, Sidebar/Status, Band Activity, Decode Table; restart to apply).
- Buttons: **Save J-Bridge Settings** · **Save Data & Restart J-Bridge**.

---

### 4.17 J-Vault — inventory (embedded)

![J-Hub — J-Vault](docs/images/jhub-jvault.png)

Embeds the J-Vault web app (separate process on **:8083**; data in `~/.j-vault/inventory.db`).

- **J-Vault Process** — status dot + **Launch & Attach** / **Stop** / **Open in New Tab** (`http://localhost:8083/`).
- **Embedded J-Vault** — the iframe (launch J-Vault first if it shows a connection error), a **Text Size** zoom slider (80–160 %), and **Reload**.

---

### 4.18 J-Sat — satellite & EME

![J-Hub — J-Sat settings](docs/images/jhub-jsat.png)

Per-app settings for J-Sat. J-Sat is the suite's **authoritative TLE source** (J-Map and others pull TLEs from it). Doppler/rotor automation is performed by **J-Hub** on messages from J-Sat, using the Rig/Rotor backends configured on those tabs.

- **J-Sat Connection** — status dot + Launch/Stop, with live Tracking / AZ-EL / Downlink readouts.
- **Station** — Callsign, Latitude °N, Longitude °E, Altitude km (J-Sat keeps its own copy for look-angle math).
- **Satellites to Track** — quick-select **All FM / All Linear / All APRS / Weather / Select All / Clear All**, then a scrollable per-sat checklist.
- **Pass Prediction** — **Min Elevation °** (0–30) · **Look-ahead Hours** (1–72).
- **Display** — Ground Track · Footprint · Space Weather Panel toggles.
- **Doppler Rig Control** — **Enable Doppler Correction** (J-Hub auto-tunes the rig via rigctld on each `SAT_DOPPLER`); shows live rig backend/status. Configure the rig on the **Rig** tab.
- **AZ/EL Rotor Tracking** — **Enable Rotor Tracking** (J-Hub auto-aims via rotctld on each `SAT_ROTOR_CMD`); shows rotor backend/position. Configure the rotor on the **Rotor** tab.
- **TLE Configuration** — **Stale Threshold (hours)** (default 48) · **TLE API Port** (default 4540) · **Refresh TLE Status**.
- **Global Base Font** + **Per-Pane Font Sizes** (Top Bar, Live Pass, Upcoming Passes, Space Weather, Rig/Rotor; 0 = inherit; restart to apply).
- **EME (Moon-bounce)** — **Enable EME panel**, **Reference RX frequency (MHz)** (Doppler scales with band: 50/144/222/432/1296/2304/3456/5760/10368), optional **DX QTH grid** (shows the mutual-Moon-window sked time).
- Buttons: **Save J-Sat Settings** · **Save Data & Restart J-Sat**.

---

### 4.19 Weather — space & local (read-only)

![J-Hub — Weather](docs/images/jhub-weather.png)

- **Space Weather** (NOAA SWPC) — tiles for **Kp Index**, **X-Ray Flux**, **IMF Bz / Bt**, **Solar Wind Speed / Density**, **Proton Flux**, with an HF-conditions summary line. **Refresh Now** + link to NOAA SWPC.
- **Local Weather** (OpenWeather) — Temperature, Conditions, Wind, Humidity, Visibility. Requires an **OpenWeatherMap API key** set on the **J-Map** tab; otherwise the card prompts for one.

---

### 4.20 Callsign — multi-provider lookup

![J-Hub — Callsign Lookup](docs/images/jhub-callsign.png)

Resolves callsigns from a local SQLite DB and online providers; modules use this for name/grid/class. Stored under `callsignLookup`.

- **Lookup** — type a callsign → result rows (Callsign, Name, Address, Grid, Class, Expires, Source).
- **Local Database** — shows Path / Records / Size / Last Updated, plus three import paths:
  - **FCC ULS Auto-Download** — one-click fetch + extract + import of the full FCC amateur DB (~220 MB, 700k+ US records) with a progress bar.
  - **Manual FCC Import** — point at an extracted `l_amat` folder (HD.dat / EN.dat / AM.dat).
  - **CSV Import** — HamCall/Buckmaster/any CSV with a callsign column.
- **Provider Settings** — **Lookup Provider** (Auto chain: Local DB → QRZ → HamQTH → HamDB → Callook; or force one) · **Local Database Path** · **Cache TTL (hours)** · **QRZ.com** user/pass (paid XML sub) · **HamQTH** user/pass (free) · **FCC Download URL Override** · **Enable Callsign Lookup** toggle · **Save Settings**.

---

### 4.21 The Operator Intel sidebar (always visible)

Independent of the tabs, a right-hand **Operator Intel** pane is always on screen: your **callsign / grid / rig / alias / operator** (operator is click-to-edit), the **live rig** readout (freq / mode / band / power), and **rotor** heading/backend — a persistent at-a-glance status that follows you across every tab.

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

#### Remote display (second machine)

J-Map doubles as a dedicated shack display — a wall monitor driven by a
cheap box (Raspberry Pi, mini-PC, spare laptop) showing grayline, spots,
and propagation while the station PC does the real work. The display box
runs **J-Map only** — there is no second J-Hub on it — and connects back
to the station's J-Hub over the network.

Build J-Map on the display box exactly as on the station, then launch it
pointed at the station's LAN address:

| Platform | Launch command |
|---|---|
| Linux / macOS | `./j-map/j-map.sh --hub 192.168.1.42` |
| Windows | `j-map\j-map.bat --hub 192.168.1.42` |

The station PC must allow two inbound ports: **8080** (WebSocket — live
data) and **8081** (HTTP — a one-time settings fetch at startup).

- **Resilient.** If the station is down when the display boots, or the
  link drops later, J-Map keeps running, shows a reconnecting banner,
  and retries automatically with exponential backoff (2 s → 60 s). It
  resyncs the moment the station returns — no manual restart.
- **Mostly read-only.** Settings and station identity flow hub →
  display; clicking a spot sends it back so the station's J-Log /
  J-Digi can tune the rig.
- **Configured from the station.** Edit J-Map's settings in the
  station's J-Hub web UI — changes broadcast live to the display.

For the full per-OS build recipe, boot auto-launch, and crash-recovery,
see **INSTALL.md → Standalone J-Map (second-machine display)**.

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
- **Estate Handoff PDF wizard** — click **Estate Document…** in the
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
  `> **Advanced —**` callouts (Extra-class / engineering-depth
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
