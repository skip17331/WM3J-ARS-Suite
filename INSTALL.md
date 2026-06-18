# WM3J ARS Suite — Install Guide

A focused, step-by-step install for **Linux**, **macOS**, and **Windows**.
Pick your platform, run the commands top-to-bottom. Most of the time is the
first Maven build (~10–20 min); the install itself is one command.

> Looking for the longer walk-through and per-app tips? See
> **[USER_GUIDE.md](USER_GUIDE.md)**. This file is just the install.

---

## What you'll end up with

The ARS Suite is **one JavaFX application** (`ars-fx`) that you can run two ways
off the **same** jar:

- **Docked** — J-Hub and every module in a single window with a left dock.
- **Loose** — each module in its own window, all sharing a small **background
  hub** that lives in the system tray.

The installer adds these menu shortcuts (all launching the one app different ways):

| Shortcut          | What it opens |
|-------------------|---------------|
| **ARS Suite**     | the docked app — J-Hub + all modules in one window |
| **J-Log / J-Map / J-Sat / J-Digi / J-Vault / J-Learn** | that module in its own window (loose) |
| **ARS Suite Hub** | the background hub only (system tray); owns the Hamlib daemons + live feeds |

There is **no web UI and no per-module ports** — everything is in the one app.
The only network port is **8090**, the hub's sharing socket (used by loose
windows on the same machine, and by a J-Map/J-Sat display on a second machine —
see [Second-machine display](#second-machine-display)).

---

## Prerequisites (all platforms)

- **Java 21 or newer** (Temurin / OpenJDK / Zulu / Microsoft Build all fine)
- **Maven 3.8 or newer**
- **Git**
- **~600 MB free disk space**
- **Optional:** Hamlib (`rigctld` / `rotctld` / `ampctld`) for rig / rotor / amp
  control; WSJT-X if you run FT8 / FT4 / MSK144 through J-Bridge.

> **No JavaFX SDK needed.** The Maven build bundles the correct JavaFX native
> runtime into the jar for your OS — Linux, Windows, macOS (Intel + Apple
> Silicon). Nothing extra to download. (64-bit ARM / Raspberry Pi is the one
> exception — it runs the JavaFX-less jar on a Liberica Full JDK; see
> [Raspberry Pi / ARM64](#raspberry-pi--arm64).)

---

## Linux

Tested on Ubuntu 22.04 / 24.04, Debian 12, Fedora 40, Arch.

### 1. Install git, Java, and Maven

**Debian / Ubuntu / Mint:**     `sudo apt update && sudo apt install -y git openjdk-21-jdk maven`
**Fedora / RHEL / Rocky:**      `sudo dnf install -y git java-21-openjdk-devel maven`
**Arch / Manjaro:**             `sudo pacman -S --needed git jdk21-openjdk maven`
**openSUSE:**                   `sudo zypper install -y git java-21-openjdk-devel maven`

Verify: `git --version`, `java --version` (must be ≥ 21), `mvn --version` (≥ 3.8).

### 2. Clone and install — one command

```bash
git clone https://github.com/skip17331/WM3J-ARS-Suite.git ~/ARS_Suite
cd ~/ARS_Suite
./install.sh
```

`./install.sh` builds the app (the shared libraries, then `ars-fx`), then runs
the installer, which writes `.desktop` entries to `~/.local/share/applications/`
and icons to `~/.local/share/icons/`. It's safe to re-run any time and never
touches your config or databases. (Already built the jars? `./install.sh
--skip-build`.)

You'll see eight shortcuts installed (ARS Suite + the six module windows + the
hub).

### 3. Launch

From your application menu, search for **ARS Suite** (docked) or any individual
module (loose). Or from the terminal:

```bash
./ars-fx/ars-fx.sh                # docked: J-Hub + all modules
./ars-fx/ars-fx.sh --module log   # one module, loose (log|map|sat|digi|vault|learn)
./ars-fx/ars-fx.sh --hub          # just the background hub (system tray)
```

The first loose window you open starts the background hub automatically; it
shows a tray icon and keeps running until you quit it there. Where there's no
usable system tray (e.g. GNOME/Wayland) the hub runs headless instead.

> **Settings live in the app.** Callsign, grid, IARU region, Hamlib endpoints,
> rig/rotor/amp backends, macros, uploaders — all in J-Hub inside the app
> (docked: the dock's gear; loose: the ⚙ in the window's top bar). No config
> files to hand-edit for normal operation.

---

## Windows

Tested on Windows 10 22H2 and Windows 11 23H2. **One command.**

```powershell
winget install --id Git.Git -e          # if you don't have Git
git clone https://github.com/skip17331/WM3J-ARS-Suite.git $HOME\ARS_Suite
cd $HOME\ARS_Suite
.\install.bat
```

Prefer no terminal? Open the `ARS_Suite` folder in Explorer and **double-click
`install.bat`**. It installs Java + Maven if missing, builds the Windows app
(`ars-fx -Pwin`), and adds Start-Menu shortcuts under **ARS Suite**. First run
~5–10 min; re-running later just upgrades. Flags: `-SkipDeps`, `-SkipBuild`.

When it finishes, press the Windows key, type **ARS Suite**, and press Enter —
or launch a single module window from its own Start-Menu entry.

> **Trouble?** `install.bat` saves a full transcript to **`install-log.txt`** in
> the `ARS_Suite` folder. Attach that one file to an issue at
> <https://github.com/skip17331/WM3J-ARS-Suite/issues>.

---

## macOS

Tested on macOS 12–14, Intel and Apple Silicon.

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"  # Homebrew, if needed
brew install --cask temurin@21
brew install maven git
git clone https://github.com/skip17331/WM3J-ARS-Suite.git ~/ARS_Suite
cd ~/ARS_Suite
./install.sh
```

`./install.sh` detects macOS and your CPU (Apple Silicon vs Intel), builds the
matching jar, and writes a `.app` bundle per shortcut into `~/Applications/`
(**ARS Suite.app**, **J-Log.app**, …, **ARS Suite Hub.app**). Each bundle's
wrapper just `exec`s `ars-fx/ars-fx.sh`, so a later `git pull` + `./install.sh`
re-propagates without a re-install. Per-window logs go to
`~/Library/Logs/ARS-Suite/`.

First launch may hit Gatekeeper ("cannot check it for malicious software").
Clear quarantine on all bundles at once:

```bash
xattr -dr com.apple.quarantine ~/Applications/ARS\ Suite*.app ~/Applications/J-*.app
```

Optional: `brew install hamlib` and `brew install --cask wsjtx`.

---

## Raspberry Pi / ARM64

The suite runs on 64-bit ARM (Pi 4/5, or any aarch64 Linux). OpenJFX ships no
linux-aarch64 native to Maven, so the ARM build leaves JavaFX out of the jar and
gets it from a **Liberica Full JDK 21** (BellSoft's JDK with JavaFX built in).

> **64-bit OS required.** `uname -m` must print `aarch64`. 32-bit Pi OS
> (`armv7l`) is not supported — reflash with the 64-bit image.

```bash
# Liberica Full JDK 21 (has JavaFX) — from bell-sw.com/pages/downloads, or:
sudo apt install bellsoft-java21-full     # if BellSoft's apt repo is configured
sudo apt install -y git maven

git clone https://github.com/skip17331/WM3J-ARS-Suite.git ~/ARS_Suite
cd ~/ARS_Suite
./install.sh                              # auto-detects aarch64 → builds the -Ppi (JavaFX-less) jar
./ars-fx/ars-fx.sh                        # runs on the Liberica Full JDK
```

A full build on a Pi 4/5 wants **4 GB+ RAM** and takes ~10–25 min. If you only
want a wall display, see [Second-machine display](#second-machine-display) — a
loose J-Map needs no station software locally.

---

## Building by hand

Only if `install.sh` / `install.bat` don't fit (proxy, custom JDK, watching each
step). Install the shared libraries to your local `~/.m2`, then build the app
with the profile for your platform:

```bash
for lib in j-log-common j-log-engine j-digi j-bridge; do
  mvn -q -DskipTests -f "$lib/pom.xml" install
done

mvn -q -DskipTests             -f ars-fx/pom.xml package   # Linux x86-64  → ars-fx-linux.jar
# mvn -q -DskipTests -Pwin         -f ars-fx/pom.xml package   # Windows       → ars-fx-windows.jar
# mvn -q -DskipTests -Pmac         -f ars-fx/pom.xml package   # macOS Intel   → ars-fx-mac.jar
# mvn -q -DskipTests -Pmac-aarch64 -f ars-fx/pom.xml package   # Apple Silicon → ars-fx-mac-aarch64.jar
# mvn -q -DskipTests -Ppi          -f ars-fx/pom.xml package   # 64-bit ARM    → ars-fx-pi.jar (Liberica Full JDK)

./install.sh --skip-build        # write shortcuts for the jar you just built
```

Build all platforms' jars at once into `dist/`: `./build-release.sh`.

---

## Second-machine display

J-Map (or J-Sat) makes a great dedicated shack display on a second monitor, a
laptop, or a Pi — pointed at your station's hub over the LAN. It's driven by a
small JSON file rather than the installer.

```
   ┌─────────────────────────┐   WebSocket push   ┌─────────────────┐
   │  Station PC             │  ───────────────►   │  Display box    │
   │  ARS Suite (hub on :8090)│  ws://station:8090  │  J-Map only     │
   └─────────────────────────┘                     └─────────────────┘
```

On the **station PC**, the hub serves on port 8090 automatically (whether you
run docked or via the tray hub). On the **display box**, copy a launch file and
point it at the station:

```jsonc
// j-map-remote.json   (see ars-fx/launch/ for samples)
{ "module": "map", "remote": "ws://192.168.1.42:8090",
  "station": { "call": "WM3J", "grid": "FM19" } }
```

```bash
./ars-fx/ars-fx.sh --config j-map-remote.json    # desktop
# on a Pi with the -Ppi jar + Liberica:  ars-fx/launch/run-solo.sh j-map-remote.json
```

Live spots, rig frequency, and rotor position push from the station to the
display; clicking a spot on the display tunes the rig back at the shack. If the
link drops, the display reconnects automatically. Open port **8090** on the
station's firewall for the LAN, e.g.
`sudo ufw allow from 192.168.0.0/24 to any port 8090`. See
[`ars-fx/launch/README.md`](ars-fx/launch/README.md) for all launch-file keys.

---

## Updating

```bash
cd ~/ARS_Suite && git pull && ./install.sh        # Linux / macOS (rebuilds + reinstalls)
```
```powershell
cd $HOME\ARS_Suite ; git pull ; .\install.bat     # Windows
```

Your config (`~/.j-hub/`, `~/.j-log/`, `~/.j-vault/`, …) and databases are never
touched by the installer or by `git pull`. The installer also prunes shortcuts
left by older installs.

---

## Where everything lives after install

| Path | What's there |
|------|--------------|
| `~/ARS_Suite/`              | Source + built jars (`ars-fx/target/ars-fx-*.jar`) |
| `~/.j-hub/`                 | Suite config (`j-hub.json`), credentials |
| `~/.j-log/`                 | Logs (normal + contest databases) + macros |
| `~/.j-vault/inventory.db`   | Inventory database |
| `~/.j-learn/content/`       | Reference-library markdown (seeded on first run, editable) |
| `~/.j-map/`, `~/.j-sat/`    | Cached map tiles / TLEs + settings |
| `~/.local/share/applications/ars-fx*.desktop` | Linux menu entries |
| `~/Applications/ARS Suite*.app`, `~/Applications/J-*.app` | macOS app bundles |
| `%APPDATA%\Microsoft\Windows\Start Menu\Programs\ARS Suite\` | Windows shortcuts |

---

## Common problems

**"command not found: mvn / git / java" (Windows, right after install)** — Open
a *new* PowerShell window; the bootstrap adds these to PATH but an open shell
keeps its old PATH.

**"JavaFX runtime components are missing" / "Could not find or load main
class"** — The jar is stale or built for the wrong OS. Rebuild for your platform
(`./install.sh`, or the matching `mvn -P… package` from
[Building by hand](#building-by-hand)) and relaunch.

**macOS "cannot check it for malicious software"** — Gatekeeper quarantine:
`xattr -dr com.apple.quarantine ~/Applications/ARS\ Suite*.app ~/Applications/J-*.app`.

**Installer prints "skipped (missing build: …)"** — The `ars-fx` jar wasn't
built for this platform. Run `./install.sh` (without `--skip-build`) or the
matching `mvn -P… package`.

**A loose window can't reach the hub / no spots** — The background hub isn't up.
Launch **ARS Suite Hub** (or `ars-fx.sh --hub`), or just open any module window
— the first one starts it. Check it's listening: `ss -ltn | grep 8090`.

**Maven build fails with `Could not resolve … j-log-engine` / `j-digi`** — The
shared libraries weren't installed first. Run the `mvn install` loop in
[Building by hand](#building-by-hand) before packaging `ars-fx`.

For deeper troubleshooting, see
**[USER_GUIDE.md § Troubleshooting](USER_GUIDE.md#6-troubleshooting)**.
