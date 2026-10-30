# WM3J ARS Suite — Install Guide

How to install the suite on **Linux**, **macOS**, **Windows** and **Raspberry Pi**.
There are two routes:

- **[Download a release](#option-a--download-a-release-no-build)** — grab one jar and run it. Needs only Java 21.
- **[Build from source](#option-b--build-from-source)** — one command (`install.sh` / `install.bat`) that also adds
  menu shortcuts. Needs Java 21, Maven and Git; the first build takes 10–20 minutes.

> After installing, read **[USER_GUIDE.md](USER_GUIDE.md)** for first-run setup and a tour of the app.

---

## What you get

The suite is **one JavaFX application** (`ars-fx`). You can run the same jar two ways:

- **Docked** — J-Hub and every module in a single window, with a dock on the left to switch between them.
- **Loose** — each module in its own window. They share a small **background hub** that runs in the system tray and
  owns the radio connections and live feeds.

The source installer adds these shortcuts, all of which start the same jar:

| Shortcut | Opens |
|----------|-------|
| **ARS Suite** | The docked app: J-Hub plus every module in one window |
| **J-Log, J-Map, J-Sat, J-Digi, J-Vault, J-Learn** | That module in its own window |
| **ARS Suite Hub** | The background hub only (system tray) |

There is no web interface. The only network port the suite opens is **8090** — the hub's sharing port, used by loose
windows on the same machine and by a second-machine display ([see below](#second-machine-display)).

---

## Prerequisites

| | Release jar | Build from source |
|---|:---:|:---:|
| **Java 21 or newer** (Temurin, OpenJDK, Zulu, Microsoft Build…) | ✔ | ✔ |
| **Maven 3.8+** | | ✔ |
| **Git** | | ✔ |
| **Disk space** | ~100 MB | ~600 MB |

Optional, for the matching features:

- **Hamlib** (`rigctld`, `rotctld`, `ampctld`) — rig, rotor and amplifier control.
- **WSJT-X** — if you run FT8 / FT4 / MSK144 through J-Bridge.

**No JavaFX download is needed.** Each jar carries the JavaFX runtime for its own platform. The one exception is the
64-bit ARM (Raspberry Pi) jar, which needs a JDK that includes JavaFX — see [Raspberry Pi](#raspberry-pi--arm64).

**Install Java 21**

| System | Command |
|--------|---------|
| Debian / Ubuntu / Mint | `sudo apt update && sudo apt install -y openjdk-21-jdk` |
| Fedora / RHEL / Rocky | `sudo dnf install -y java-21-openjdk-devel` |
| Arch / Manjaro | `sudo pacman -S --needed jdk21-openjdk` |
| macOS | `brew install --cask temurin@21` |
| Windows | `winget install EclipseAdoptium.Temurin.21.JDK` |

Check with `java --version` — it must say 21 or higher.

---

## Option A — download a release (no build)

1. Open the [Releases page](https://github.com/skip17331/WM3J-ARS-Suite/releases) and download the jar for your system
   from the newest release:

   | Your system | File |
   |-------------|------|
   | Linux, 64-bit Intel/AMD | `ars-fx-<version>-linux-x86_64.jar` |
   | Windows 10 / 11 | `ars-fx-<version>-windows-x86_64.jar` |
   | macOS, Intel | `ars-fx-<version>-macos-x86_64.jar` |
   | macOS, Apple Silicon (M1 and later) | `ars-fx-<version>-macos-aarch64.jar` |
   | Raspberry Pi 4/5, 64-bit OS | `ars-fx-<version>-linux-aarch64.jar` (see [Raspberry Pi](#raspberry-pi--arm64)) |

2. Run it:

   ```bash
   java -jar ars-fx-<version>-linux-x86_64.jar                      # docked app
   java -jar ars-fx-<version>-linux-x86_64.jar --module log         # one module in its own window
   java -cp   ars-fx-<version>-linux-x86_64.jar com.ars.fx.HubServer   # background hub only
   ```

   On Windows you can usually double-click the jar. Module ids for `--module` are
   `log`, `logc`, `map`, `sat`, `digi`, `bridge`, `vault` and `learn`.

A jar run this way gives you the full app but no menu shortcuts. For shortcuts, use Option B.

---

## Option B — build from source

### Linux

Tested on Ubuntu 22.04 / 24.04, Debian 12, Fedora 40 and Arch.

```bash
# 1. Git, Java 21 and Maven
sudo apt update && sudo apt install -y git openjdk-21-jdk maven      # Debian / Ubuntu / Mint
#   sudo dnf install -y git java-21-openjdk-devel maven               # Fedora / RHEL / Rocky
#   sudo pacman -S --needed git jdk21-openjdk maven                   # Arch / Manjaro
#   sudo zypper install -y git java-21-openjdk-devel maven            # openSUSE

# 2. Get the code and install
git clone https://github.com/skip17331/WM3J-ARS-Suite.git ~/ARS_Suite
cd ~/ARS_Suite
./install.sh
```

`./install.sh` builds the shared libraries and the `ars-fx` jar, then writes `.desktop` entries to
`~/.local/share/applications/` and icons to `~/.local/share/icons/`. You get eight shortcuts: ARS Suite, six module
windows and the hub. It is safe to re-run at any time and never touches your settings or logs. If the jars are already
built, use `./install.sh --skip-build`.

Open your application menu and search for **ARS Suite**, or run it from a terminal:

```bash
./ars-fx/ars-fx.sh                  # docked: J-Hub + all modules
./ars-fx/ars-fx.sh --module log     # one module in its own window
./ars-fx/ars-fx.sh --hub            # the background hub only (system tray)
```

### Windows

Tested on Windows 10 22H2 and Windows 11 23H2.

```powershell
winget install --id Git.Git -e          # if you don't have Git
git clone https://github.com/skip17331/WM3J-ARS-Suite.git $HOME\ARS_Suite
cd $HOME\ARS_Suite
.\install.bat
```

Prefer no terminal? Open the `ARS_Suite` folder in Explorer and double-click **`install.bat`**. It installs Java and
Maven if they are missing, builds the Windows app and adds Start-menu shortcuts under **ARS Suite**. The first run takes
about 5–10 minutes. Flags: `-SkipDeps`, `-SkipBuild`.

When it finishes, press the Windows key, type **ARS Suite** and press Enter. If anything goes wrong, `install.bat` saves
a full transcript to `install-log.txt` in the `ARS_Suite` folder; attach that file to an
[issue](https://github.com/skip17331/WM3J-ARS-Suite/issues).

### macOS

Tested on macOS 12–14, Intel and Apple Silicon.

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"   # Homebrew, if needed
brew install --cask temurin@21
brew install maven git
git clone https://github.com/skip17331/WM3J-ARS-Suite.git ~/ARS_Suite
cd ~/ARS_Suite
./install.sh
```

`./install.sh` detects Intel or Apple Silicon, builds the matching jar and writes one `.app` bundle per shortcut into
`~/Applications/` (**ARS Suite.app**, **J-Log.app** … **ARS Suite Hub.app**). Each bundle simply runs
`ars-fx/ars-fx.sh`, so a later `git pull` followed by `./install.sh` updates them. Logs go to
`~/Library/Logs/ARS-Suite/`.

There is also `./bootstrap-mac.sh`, which checks your tools, builds, installs and clears the quarantine flag in one go.

The first launch may be blocked by Gatekeeper ("cannot check it for malicious software"). Clear it for all bundles:

```bash
xattr -dr com.apple.quarantine ~/Applications/ARS\ Suite*.app ~/Applications/J-*.app
```

### Raspberry Pi / ARM64

The suite runs on 64-bit ARM (Pi 4 or 5, or any aarch64 Linux). OpenJFX publishes no linux-aarch64 build to Maven, so
the ARM jar leaves JavaFX out and takes it from a **Liberica Full JDK 21** (BellSoft's JDK with JavaFX built in).

> **A 64-bit OS is required.** `uname -m` must print `aarch64`. 32-bit Pi OS (`armv7l`) is not supported.

```bash
# Liberica Full JDK 21 — from bell-sw.com/pages/downloads, or:
sudo apt install bellsoft-java21-full        # if BellSoft's apt repository is configured
sudo apt install -y git maven

git clone https://github.com/skip17331/WM3J-ARS-Suite.git ~/ARS_Suite
cd ~/ARS_Suite
./install.sh                                 # detects aarch64 and builds the JavaFX-less jar
./ars-fx/ars-fx.sh
```

A full build wants 4 GB or more of RAM and takes 10–25 minutes. If all you want is a wall display, a Pi running only
J-Map needs no station software — see [Second-machine display](#second-machine-display). To use a downloaded ARM jar
instead of building, run it with the Liberica JDK's `java`.

### Building by hand

Use this if the installer scripts don't suit you (a proxy, a custom JDK, or you want to watch each step). Install the
four shared libraries to your local Maven repository first, then build the app for your platform:

```bash
for lib in j-log-common j-log-engine j-digi j-bridge; do
  mvn -q -DskipTests -f "$lib/pom.xml" install
done

mvn -q -DskipTests                 -f ars-fx/pom.xml package   # Linux x86-64   → ars-fx-linux.jar
# mvn -q -DskipTests -Pwin         -f ars-fx/pom.xml package   # Windows        → ars-fx-windows.jar
# mvn -q -DskipTests -Pmac         -f ars-fx/pom.xml package   # macOS Intel    → ars-fx-mac.jar
# mvn -q -DskipTests -Pmac-aarch64 -f ars-fx/pom.xml package   # Apple Silicon  → ars-fx-mac-aarch64.jar
# mvn -q -DskipTests -Ppi          -f ars-fx/pom.xml package   # 64-bit ARM     → ars-fx-pi.jar

./install.sh --skip-build          # write shortcuts for the jar you just built
```

`./build-release.sh` builds all five platform jars into `dist/` and checks that none carries another platform's JavaFX
natives.

---

## First launch

1. Start **ARS Suite**. A splash screen appears, then the app opens on the **J-Hub** dashboard.
2. Go to **J-Hub ▸ Station** and enter your callsign and grid square. Everything else can wait.
3. **J-Learn** copies its reference library to `~/.j-learn/content/` the first time you open it.

The rest of the setup — radio, cluster, log databases, uploads — is in **[USER_GUIDE.md](USER_GUIDE.md)**. All settings
are made inside the app; there are no configuration files to edit by hand.

If your desktop has no system tray (GNOME on Wayland, for example), the background hub runs without a tray icon. The
docked app doesn't need the tray at all.

---

## Second-machine display

J-Map or J-Sat makes a good dedicated display on a second monitor, a laptop or a Pi, fed by your station over the LAN.

```
   ┌──────────────────────────┐   WebSocket     ┌──────────────────┐
   │  Station PC              │  ───────────►   │  Display machine │
   │  ARS Suite (hub, :8090)  │  ws://station:8090  │  J-Map or J-Sat  │
   └──────────────────────────┘                 └──────────────────┘
```

1. **On the station PC**, open **J-Hub ▸ Data ▸ Remote sharing** and switch on *Share with solo modules on the LAN*
   (the port is 8090 unless you change it). Allow the port through the firewall for your LAN only, for example
   `sudo ufw allow from 192.168.0.0/24 to any port 8090`.
2. **On the display machine**, install the app (either option) and write a small launch file pointing at the station:

   ```jsonc
   // j-map-remote.json   (more samples in ars-fx/launch/)
   { "module": "map", "remote": "ws://192.168.1.42:8090",
     "station": { "call": "WM3J", "grid": "FM19" } }
   ```

   ```bash
   ./ars-fx/ars-fx.sh --config j-map-remote.json                # from a source install
   java -jar ars-fx-<version>-linux-x86_64.jar --config j-map-remote.json   # from a release jar
   # Pi with the ARM jar and Liberica:  ars-fx/launch/run-solo.sh j-map-remote.json
   ```

Spots, rig frequency and rotor position flow from the station to the display, and clicking a spot on the display tunes
the rig at the shack. If the link drops, the display reconnects by itself. The launch-file keys are listed in
[`ars-fx/launch/README.md`](ars-fx/launch/README.md).

---

## Updating

**Source install**

```bash
cd ~/ARS_Suite && git pull && ./install.sh          # Linux / macOS
```
```powershell
cd $HOME\ARS_Suite ; git pull ; .\install.bat       # Windows
```

**Release jar** — download the newer jar and run that instead.

Updating never touches your settings, logs or databases. The reference library in `~/.j-learn/content/` is also left
alone; to bring it up to date, use **J-Hub ▸ Data ▸ J-Learn content ▸ Update files**.

---

## Where things live

| Path | What's there |
|------|--------------|
| `~/ARS_Suite/` | Source and built jars (`ars-fx/target/ars-fx-*.jar`) — source installs only |
| `~/.j-hub/config.json` | All suite settings: station, rig, cluster, macros, upload logins |
| `~/.j-log/j-log.db` | The normal log |
| `~/.j-log/contest-logs/` | Contest logs, one database per contest and year (e.g. `NovSwpSSB2026.db`) |
| `~/.j-log/archive/<Contest>_<Year>/` | Contest databases you have archived |
| `~/.j-log/backups/` | ADIF / CSV exports made from J-Hub ▸ Data |
| `~/.j-vault/inventory.db` | J-Vault inventory |
| `~/.j-learn/content/` | J-Learn chapters (editable; `content.backup` holds the previous copy after an update) |
| `~/.j-sat/`, `~/.j-map/` | Orbital data and cached map data |
| `~/.local/share/applications/ars-fx*.desktop` | Linux menu entries |
| `~/Applications/ARS Suite*.app`, `~/Applications/J-*.app` | macOS app bundles |
| `%APPDATA%\Microsoft\Windows\Start Menu\Programs\ARS Suite\` | Windows shortcuts |

Back up `~/.j-log/` and `~/.j-hub/` and you have your whole station.

---

## Removing the suite

Delete the `ARS_Suite` folder (or the jar), then the shortcuts listed above. Your data in the `~/.j-*` folders stays
until you delete it yourself.

---

## Common problems

**"command not found: mvn / git / java" on Windows, straight after installing** — Open a *new* PowerShell window. The
installer updates PATH, but a window that was already open keeps its old one.

**"JavaFX runtime components are missing" or "Could not find or load main class"** — The jar is stale or built for the
wrong system. Rebuild with `./install.sh`, or use the matching `-P…` profile from [Building by hand](#building-by-hand).
With a downloaded jar, check you picked the file for your OS and CPU.

**"Unsupported class file major version"** — Your Java is older than 21. Run `java --version` and install Java 21.

**macOS "cannot check it for malicious software"** — Gatekeeper quarantine; run the `xattr` command under
[macOS](#macos).

**Installer prints "skipped (missing build: …)"** — The jar for this platform hasn't been built. Run `./install.sh`
without `--skip-build`.

**`Could not resolve … j-log-engine` / `j-digi` during the build** — The shared libraries weren't installed first. Run
the `mvn install` loop under [Building by hand](#building-by-hand).

**A loose window shows no spots, or can't reach the hub** — The background hub isn't running. Open **ARS Suite Hub**, run
`ars-fx.sh --hub`, or open any module window — the first one starts it. Check it is listening with
`ss -ltn | grep 8090`.

**No tray icon** — Some desktops (GNOME on Wayland) have no system tray. The hub then runs without one; stop it with
`Ctrl-C` in its terminal or `pkill -f com.ars.fx.HubServer`.

More in **[USER_GUIDE.md § Troubleshooting](USER_GUIDE.md#7-troubleshooting)**.
