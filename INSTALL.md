# WM3J ARS Suite — Install Guide

A focused, step-by-step install for **Linux** and **Windows**. Pick your
platform below and run the commands top-to-bottom. Should take about
15-25 minutes (most of that is the first Maven build).

> Looking for the longer walk-through, web-UI tour, and per-app tips?
> See **[USER_GUIDE.md](USER_GUIDE.md)**. This file is just the install.

---

## What you'll end up with

Ten Maven modules, one installer:

| Module      | What it is                                             | Port  |
|-------------|--------------------------------------------------------|-------|
| **j-hub**       | Central broker + web UI (Dashboard, Macros, Modules…) | 8080 / 8081 |
| **j-log-engine**| Shared logging library (built first; no UI)           | —     |
| **j-log**       | QSO logger and contest module                         | —     |
| **j-map**       | DX map with propagation overlays                      | —     |
| **j-digi**      | Digital modem (RTTY / PSK / Olivia)                   | —     |
| **j-bridge**    | WSJT-X / FT8 integration                              | —     |
| **j-sat**       | Satellite tracker                                     | —     |
| **j-vault**     | Shack inventory + estate-handoff PDF wizard           | 8083  |
| **j-learn**     | Amateur-radio reference library (web app)             | 8082  |
| **morse-trainer** | Morse code learning + sending practice + analytics  | —     |

J-Hub iframes J-Learn so it shows up as a tab inside the main UI; you can
also open it directly at <http://localhost:8082/> from any browser on the
LAN.

---

## Prerequisites (all platforms)

- **Java 21 or newer** (Temurin / OpenJDK / Zulu / Microsoft Build all fine)
- **Maven 3.8 or newer**
- **Git**
- **~500 MB free disk space**
- **Optional:** Hamlib (`rigctl`/`rotctld`) for rig / rotor control;
  WSJT-X if you run FT8 / FT4 / MSK144 through the bridge.

---

## Linux

Tested on Ubuntu 22.04 / 24.04, Debian 12, Fedora 40, Arch.

### 1. Install git, Java, and Maven

**Debian / Ubuntu / Mint:**

```bash
sudo apt update
sudo apt install -y git openjdk-21-jdk maven
```

**Fedora / RHEL / Rocky:**

```bash
sudo dnf install -y git java-21-openjdk-devel maven
```

**Arch / Manjaro:**

```bash
sudo pacman -S --needed git jdk21-openjdk maven
```

**openSUSE:**

```bash
sudo zypper install -y git java-21-openjdk-devel maven
```

Verify:

```bash
git --version    # any 2.x is fine
java --version   # must be 21 or newer
mvn --version    # must be 3.8 or newer
```

### 2. Clone the repository

```bash
cd ~
git clone https://github.com/skip17331/WM3J-ARS-Suite.git ARS_Suite
cd ARS_Suite
```

### 3. Build the suite

J-Log-Engine is a shared library — install it first so the others can
depend on it. J-Learn and J-Vault are standalone modules but get
`mvn install`-ed so J-Hub can launch them by name.

```bash
cd ~/ARS_Suite

# Shared engine first — must be installed so log/digi can find it
mvn -q -DskipTests -f j-log-engine/pom.xml install

# Standalone web apps J-Hub launches — install for the .m2 cache
mvn -q -DskipTests -f j-learn/pom.xml install
mvn -q -DskipTests -f j-vault/pom.xml install

# Package the apps
for m in j-hub j-log j-map j-digi j-bridge j-sat morse-trainer; do
  mvn -q -DskipTests -f "$m/pom.xml" package
done
```

If any module fails, fix that module's error and re-run **just** that
line — earlier successes don't need to be redone.

### 4. Run the installer

This writes `.desktop` entries and icons under `~/.local/share/`. Safe
to re-run any time; never touches your config or databases.

```bash
./install.sh
```

You should see nine entries installed (j-hub, j-log, j-map, j-digi,
j-bridge, j-sat, j-vault, j-learn, morse-trainer).

### 5. Launch J-Hub

From your application menu, search for **WM3J J-Hub**. Or from the
terminal:

```bash
./j-hub/start.sh
```

Open <http://localhost:8081/> in your browser. That's the main UI.

J-Vault and J-Learn are separate processes (ports 8083 and 8082). They
have their own Start-Menu / app-menu entries, and J-Hub's left nav has a
**Launch** button on each tab so you can start them from the UI.

> **One settings surface.** Operator-level preferences — callsign, grid,
> IARU region + country, Hamlib endpoint, J-Digi PTT/CW path — all live
> in J-Hub's web UI and ride to the modules over the broker. You should
> not need to hand-edit Java Preferences or per-module JSON config files
> for normal operation. Edit in the browser, save, done.

---

## Windows

Tested on Windows 10 22H2 and Windows 11 23H2.

### 1. Install git, Java, and Maven

**Easiest (one command, requires winget — built into Windows 10/11):**

Open PowerShell as Administrator and run:

```powershell
winget install --id Git.Git -e
winget install --id EclipseAdoptium.Temurin.21.JDK -e
winget install --id Apache.Maven -e
```

**Manual download** (use this if winget isn't available):

1. **Git** — <https://git-scm.com/download/win>
   Run the installer; accept defaults. (When asked about the default
   editor, pick whatever you're comfortable with — it doesn't affect
   the suite.)
2. **Java 21 (Temurin)** — <https://adoptium.net/temurin/releases/?version=21>
   Pick **Windows x64 → JDK → .msi**. During install, tick
   "Set JAVA_HOME variable" and "Add to PATH".
3. **Maven** — <https://maven.apache.org/download.cgi>
   Download the **Binary zip archive** (e.g. `apache-maven-3.9.x-bin.zip`),
   extract to `C:\Maven`, then add `C:\Maven\apache-maven-3.9.x\bin`
   to your PATH (System Properties → Environment Variables).

**Open a fresh PowerShell** (so PATH changes take effect) and verify:

```powershell
git --version
java --version
mvn --version
```

All three should print versions. If any "is not recognized," restart
PowerShell or check PATH.

### 2. Clone the repository

```powershell
cd $HOME
git clone https://github.com/skip17331/WM3J-ARS-Suite.git ARS_Suite
cd ARS_Suite
```

### 3. Install the JavaFX Windows runtime

The repo's `lib/javafx` symlinks point to a Linux SDK, which doesn't
work on Windows. Each module needs its own Windows JavaFX libs:

1. Download **JavaFX 21 SDK for Windows x64** from
   <https://gluonhq.com/products/javafx/>
   (look for "JavaFX Windows x64 SDK", e.g. `openjfx-21.0.x_windows-x64_bin-sdk.zip`).
2. Extract to `C:\javafx-sdk-21`.
3. For each module that has a `lib\javafx` link or directory, replace it
   with a copy of the JavaFX `lib` folder. From PowerShell:

   ```powershell
   foreach ($m in 'j-hub','j-log','j-map','j-digi','j-bridge','j-sat','j-vault') {
       $dest = "$HOME\ARS_Suite\$m\lib\javafx"
       if (Test-Path $dest) { Remove-Item -Recurse -Force $dest }
       New-Item -ItemType Directory -Path $dest -Force | Out-Null
       Copy-Item -Recurse "C:\javafx-sdk-21\lib\*" $dest
   }
   ```

   J-Learn is a pure web app and doesn't use JavaFX, so it isn't in the
   list above. Morse-trainer pulls JavaFX directly from Maven Central, so
   it doesn't need the SDK swap either.

This step is one-time. The Linux flow doesn't need it because the
symlinks point at a working SDK already.

### 4. Build the suite

```powershell
cd $HOME\ARS_Suite

# Shared engine first
mvn -q -DskipTests -f j-log-engine\pom.xml install

# Standalone web apps J-Hub launches — install for the .m2 cache
mvn -q -DskipTests -f j-learn\pom.xml install
mvn -q -DskipTests -f j-vault\pom.xml install

# Package the apps
foreach ($m in 'j-hub','j-log','j-map','j-digi','j-bridge','j-sat','morse-trainer') {
    mvn -q -DskipTests -f "$m\pom.xml" package
}
```

If any module fails, re-run just that line after fixing.

### 5. Run the installer

This generates per-module `.bat` launchers (when missing) and creates
**Start Menu shortcuts** under `ARS Suite`. Safe to re-run.

```powershell
.\install.bat
```

You should see nine entries installed.

### 6. Launch J-Hub

Press the Windows key, type **WM3J J-Hub**, and press Enter. Or from
PowerShell:

```powershell
.\j-hub\start.bat
```

Open <http://localhost:8081/> in your browser.

J-Vault and J-Learn each have their own Start-Menu entries
(**WM3J J-Vault**, **WM3J J-Learn**); from inside J-Hub you can also
launch them via the corresponding tab's **Launch** button.

---

## macOS

Tested on macOS 12 Monterey through 14 Sonoma, both Intel and
Apple Silicon. There's a one-command bootstrap that handles the
macOS-specific bits — JavaFX SDK download, arch detection, build,
installer, Gatekeeper quarantine clearing — so the headline flow
is short.

### Quick path (recommended)

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"  # Homebrew, if needed
brew install --cask temurin@21
brew install maven git
git clone https://github.com/skip17331/WM3J-ARS-Suite.git ~/ARS_Suite
cd ~/ARS_Suite
./bootstrap-mac.sh
open ~/Applications/WM3J\ J-Hub.app
```

Then open <http://localhost:8081/> in your browser.

### What `bootstrap-mac.sh` does

Six steps, all idempotent — re-running after a `git pull` is the
upgrade path.

1. **Prerequisites check.** Verifies `java` (≥ 21), `mvn`, `git`,
   `curl`, `unzip` are on PATH. Tells you the brew command to
   install whichever is missing.
2. **Architecture detection + JavaFX SDK.** `uname -m` picks
   `osx-aarch64` (Apple Silicon) or `osx-x64` (Intel). Downloads
   the matching Gluon JavaFX 21.0.5 SDK to `~/.cache/ars-suite/`
   (so subsequent runs skip the download), then copies its `lib/`
   into every module that hard-codes `--module-path lib/javafx`
   in its launcher: j-hub, j-log, j-map, j-digi, j-bridge, j-sat.
3. **Engine + web-app install** to your local `~/.m2/` repo —
   j-log-engine first, then j-learn and j-vault (which j-hub
   launches as child processes).
4. **Module package.** Builds j-hub, j-log, j-map, j-digi,
   j-bridge, j-sat, morse-trainer with `mvn package -DskipTests`.
5. **Installer.** Calls `./install.sh`, which detects macOS and
   writes a `.app` bundle per module into `~/Applications/` (not
   `/Applications/`, which needs `sudo`). Each bundle's
   `Contents/MacOS/<name>` shell wrapper just `exec`s the
   module's existing `*.sh` launcher, so future `git pull`
   updates need no re-install.
6. **Gatekeeper quarantine clear.** Runs `xattr -dr com.apple.quarantine`
   on every installed bundle so first-time double-click works
   without the right-click → Open dance.

Bundle layout when it's done:

```
~/Applications/
├── WM3J J-Hub.app/
├── WM3J J-Log.app/
├── WM3J J-Map.app/
├── WM3J J-Digi.app/
├── WM3J J-Bridge.app/
├── WM3J J-Sat.app/
├── WM3J J-Vault.app/
├── WM3J J-Learn.app/
└── WM3J Morse Trainer.app/
```

Per-module log output: `~/Library/Logs/ARS-Suite/<name>.log`. (Finder
swallows stdout from `.app` bundles, so the launcher redirects there.)

### Updating

```bash
cd ~/ARS_Suite
git pull
./bootstrap-mac.sh
```

The JavaFX SDK is cached so the second run is just build + install
(faster). The installer is safe to re-run — it overwrites the bundle
contents and leaves config, logs, and databases alone.

### Optional: Hamlib and WSJT-X

```bash
brew install hamlib
brew install --cask wsjtx
```

Both are optional. The suite runs without them; J-Hub's Dashboard
has a re-check button after you install them.

### Doing it by hand

If `bootstrap-mac.sh` doesn't fit your needs (corporate proxy, mirror
of Gluon, custom JDK location), the manual path is what the script
does in pieces:

1. `brew install --cask temurin@21 && brew install maven git`
2. Download `openjfx-21.0.5_osx-aarch64_bin-sdk.zip` (Apple Silicon)
   or `…_osx-x64_bin-sdk.zip` (Intel) from
   <https://gluonhq.com/products/javafx/>; unzip and copy its `lib/`
   into `j-hub/lib/javafx/`, `j-log/lib/javafx/`, `j-map/lib/javafx/`,
   `j-digi/lib/javafx/`, `j-bridge/lib/javafx/`, `j-sat/lib/javafx/`.
3. `mvn -DskipTests -f j-log-engine/pom.xml install`
4. `mvn -DskipTests -f j-learn/pom.xml install`
5. `mvn -DskipTests -f j-vault/pom.xml install`
6. `mvn -DskipTests package` in each of the seven user-facing modules.
7. `./install.sh`
8. `xattr -dr com.apple.quarantine ~/Applications/WM3J\ *.app` to skip
   the per-module Gatekeeper prompt on first launch.

---

## Pi Display Mode (j-map only, second-machine setup)

Use case: a Raspberry Pi 4/5 wired to a large monitor in the shack, showing
J-Map full-screen so you can see propagation / spots / grayline at a glance
from across the room. The Pi is *just a display* — no logging, no rig
control, no inputs. The real station PC runs the full suite and is the
single source of truth.

### How it works

J-Map runs on the Pi and connects over the LAN to the J-Hub WebSocket on
your main station PC. J-Hub binds to all interfaces by default
(`0.0.0.0:8080` / `:8081`), so any host on the same LAN can subscribe.
Spots, station info, and config changes are pushed to J-Map automatically.
If the link drops the Pi shows a red "Disconnected from j-hub" banner at
the bottom of the map and reconnects with exponential backoff (2s → 60s).

There is **no second J-Hub** on the Pi — that would be a second broker
with no sync to the main one. One J-Hub, many displays.

### On the main station PC (already done if the suite is running)

Verify J-Hub is reachable from the LAN:

```bash
# From any other machine on the same network:
nc -zv <main-pc-ip> 8080  # WebSocket
nc -zv <main-pc-ip> 8081  # web config (optional, only for /api/jmap fetch)
```

If those fail, open ports 8080 (and optionally 8081) on the main PC's
firewall. Ubuntu/Debian:

```bash
sudo ufw allow from 192.168.0.0/16 to any port 8080
sudo ufw allow from 192.168.0.0/16 to any port 8081
```

### On the Pi

```bash
sudo apt update
sudo apt install -y git openjdk-21-jdk maven

cd ~
git clone https://github.com/skip17331/WM3J-ARS-Suite.git ARS_Suite
cd ARS_Suite

# Build *only* j-map — no engine, no other modules.
mvn -q -DskipTests -f j-map/pom.xml package

# Run, pointing at the main station's IP. Replace with yours.
./j-map/run.sh --hub 192.168.1.42
```

Useful flags (j-map):

| Flag                     | What it does                                                |
|--------------------------|-------------------------------------------------------------|
| `--hub <host>`           | IP / hostname of the main station's J-Hub                   |
| `--hub-ws-port <NNNN>`   | Override 8080 if J-Hub is configured to a different port    |
| `--hub-web-port <NNNN>`  | Override 8081 for the HTTP config endpoint                  |
| `--launched-by-hub`      | Skip the splash; intended for when J-Hub itself launches it |

### Auto-start on boot (optional)

Drop a systemd user unit so j-map relaunches after reboots and crashes:

```bash
mkdir -p ~/.config/systemd/user
cat > ~/.config/systemd/user/j-map.service <<'EOF'
[Unit]
Description=J-Map remote display
After=graphical-session.target
PartOf=graphical-session.target

[Service]
ExecStart=%h/ARS_Suite/j-map/run.sh --hub 192.168.1.42
Restart=on-failure
RestartSec=10

[Install]
WantedBy=graphical-session.target
EOF

systemctl --user enable --now j-map
sudo loginctl enable-linger $USER   # so the unit runs without an active login
```

Edit the `--hub` IP to match your station. Update with `git pull && mvn -q
-DskipTests -f j-map/pom.xml package && systemctl --user restart j-map`.

---

## Updating

```bash
# Linux
cd ~/ARS_Suite
git pull
mvn -q -DskipTests -f j-log-engine/pom.xml install
mvn -q -DskipTests -f j-learn/pom.xml install
mvn -q -DskipTests -f j-vault/pom.xml install
for m in j-hub j-log j-map j-digi j-bridge j-sat morse-trainer; do
  mvn -q -DskipTests -f "$m/pom.xml" package
done
./install.sh
```

```powershell
# Windows
cd $HOME\ARS_Suite
git pull
mvn -q -DskipTests -f j-log-engine\pom.xml install
mvn -q -DskipTests -f j-learn\pom.xml install
mvn -q -DskipTests -f j-vault\pom.xml install
foreach ($m in 'j-hub','j-log','j-map','j-digi','j-bridge','j-sat','morse-trainer') {
    mvn -q -DskipTests -f "$m\pom.xml" package
}
.\install.bat
```

Your config (`~/.j-hub/`, `~/.j-vault/`, `~/.j-log/`, etc.) and
databases are never touched by the installer or by `git pull`.

---

## Where everything lives after install

| Path | What's there |
|------|--------------|
| `~/ARS_Suite/`             | Source + built jars |
| `~/.j-hub/`                | J-Hub config (`j-hub.json`), credentials, logs |
| `~/.j-vault/inventory.db`  | J-Vault inventory database |
| `~/.j-learn/content/`      | J-Learn markdown (seeded on first run, editable) |
| `~/.j-learn/settings.json` | J-Learn port override |
| `~/.j-log/`                | J-Log databases (normal log + contests) + macros |
| `~/.j-map/`                | J-Map cached map images, settings |
| `~/.j-sat/`                | J-Sat TLE cache + settings |
| `~/.local/share/applications/ars-*.desktop` | Linux menu entries |
| `%APPDATA%\Microsoft\Windows\Start Menu\Programs\ARS Suite\` | Windows shortcuts |

---

## Common problems

**"command not found: mvn" (after winget install on Windows)** —
Open a *new* PowerShell window. winget updates PATH but only for new sessions.

**"java: command not found"** — Same: open a fresh terminal/PowerShell after install.

**"Could not find or load main class" or "JavaFX runtime components are missing"
on Windows** — You skipped step 3 (Windows JavaFX SDK). The Linux symlinks
in `lib/javafx` don't work on Windows.

**Maven build fails with `Could not resolve … j-log-engine`** —
You forgot to `mvn install` (not just `mvn package`) the engine first.
J-Hub no longer depends on j-learn at build time; the standalone web apps
(j-learn, j-vault) just need to be installed so J-Hub can launch them.

**Installer prints "skipped (missing build: …)"** — That module didn't build.
Re-run its specific `mvn package` line and look for the actual error.

**J-Vault / J-Learn launches but the J-Hub tab shows "connection refused"** —
J-Vault and J-Learn are separate processes on ports 8083 and 8082. Click
**Launch** at the top of the corresponding tab in J-Hub, wait 3-5 seconds
for Jetty to start, then click **Reload** on the embedded view.

For deeper troubleshooting, see **[USER_GUIDE.md § Troubleshooting](USER_GUIDE.md#6-troubleshooting)**.
