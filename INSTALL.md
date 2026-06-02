# WM3J ARS Suite — Install Guide

A focused, step-by-step install for **Linux**, **macOS**, and
**Windows**. Pick your platform below and run the commands top-to-bottom.
Should take about 15–25 minutes (most of that is the first Maven build).

> macOS users: there's a one-command bootstrap (`./bootstrap-mac.sh`)
> that collapses arch detection, the build loop, and Gatekeeper
> unblocking into a single script. See the **macOS**
> section below.

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

> **No JavaFX SDK needed.** The Maven build bundles the correct JavaFX
> native runtime into each module's jar automatically — Linux, Windows,
> and macOS, x64 and arm64. There is nothing extra to download or copy
> on any platform. (On 64-bit ARM / Raspberry Pi the build automatically
> uses JavaFX 23.0.1, since 21.0.3 ships no ARM native — see the
> [Raspberry Pi and ARM64](#raspberry-pi-and-arm64) section.)

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

The same `./install.sh` works for both Linux and macOS — it detects
the platform at runtime. On Linux it writes `.desktop` entries to
`~/.local/share/applications/` and copies icons to
`~/.local/share/icons/`. Safe to re-run any time; never touches your
config or databases.

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

Tested on Windows 10 22H2 and Windows 11 23H2. There is **one
command** — no copy-pasting a list of steps.

### Install

1. If you don't already have Git, install it (one line in PowerShell):

   ```powershell
   winget install --id Git.Git -e
   ```

2. Get the code and run the installer:

   ```powershell
   git clone https://github.com/skip17331/WM3J-ARS-Suite.git $HOME\ARS_Suite
   cd $HOME\ARS_Suite
   .\install.bat
   ```

   Prefer not to use a terminal at all? Open the `ARS_Suite` folder in
   File Explorer and **double-click `install.bat`**.

That's the whole install. `install.bat` installs Java and Maven for
you if they're missing, builds everything, and adds the Start-Menu
shortcuts. The first run takes about 5–10 minutes (it downloads
dependencies) — you can leave it running. Running it again later just
upgrades.

When it finishes, press the Windows key, type **WM3J J-Hub**, press
Enter, then open <http://localhost:8081/>.

> **If something goes wrong:** the installer saves a full transcript to
> **`install-log.txt`** in the `ARS_Suite` folder (and prints that
> path). Open an issue at
> <https://github.com/skip17331/WM3J-ARS-Suite/issues> and drag that
> one file in — that's all we need to help. No need to copy text out of
> the window.

You should get nine shortcuts (j-hub, j-log, j-map, j-digi, j-bridge,
j-sat, j-vault, j-learn, morse-trainer). J-Vault and J-Learn have their
own entries too; inside J-Hub each tab also has a **Launch** button.

> **One settings surface.** Operator preferences — callsign, grid, IARU
> region + country, Hamlib endpoint, J-Digi PTT/CW path — all live in
> J-Hub's web UI and ride to the modules over the broker. You should not
> need to hand-edit any config files for normal operation.

### What the one command does

`install.bat` is a small launcher for the real script (`install.ps1`);
it sets a PowerShell execution-policy bypass so a fresh machine needs
zero setup, and pauses at the end if you double-clicked it so you can
read the result. Both accept `-SkipDeps` (toolchain already present)
and `-SkipBuild` (jars already built). In four idempotent stages it:

1. **Toolchain** — ensures Git and Temurin 21 JDK via winget, and Maven
   from the genuine Apache zip (SHA-512 verified, unpacked under
   `%LOCALAPPDATA%\ARS-Suite`). Apache publishes no official winget
   package for Maven, so the bootstrap pulls it straight from Apache
   rather than trust a third-party manifest. Installs only what's
   missing; refreshes PATH in-process.
2. **Build** — `mvn install` j-log-engine, j-learn, j-vault; then
   `mvn package` j-hub, j-log, j-map, j-digi, j-bridge, j-sat,
   morse-trainer.
3. **Desktop integration** — runs the cross-platform installer, which
   writes per-module `.bat` launchers, derives a `.ico` from each
   module's icon, and creates Start-Menu shortcuts under
   `%APPDATA%\Microsoft\Windows\Start Menu\Programs\ARS Suite\`.
4. **Normalize config** — rewrites any stale Linux launch commands in
   `j-hub.json` to the Windows `.bat` wrappers.

### Doing it by hand

Only if the one command doesn't fit (corporate proxy, custom JDK
location, or you want to watch each step):

```powershell
winget install --id Git.Git -e
winget install --id EclipseAdoptium.Temurin.21.JDK -e
# Maven has no official winget package - install the genuine Apache zip:
$mvnVer = '3.9.16'
$dest   = "$env:LOCALAPPDATA\ARS-Suite"
$zip    = "$env:TEMP\apache-maven-$mvnVer-bin.zip"
Invoke-WebRequest "https://dlcdn.apache.org/maven/maven-3/$mvnVer/binaries/apache-maven-$mvnVer-bin.zip" -OutFile $zip
Expand-Archive $zip $dest -Force
$env:Path = "$dest\apache-maven-$mvnVer\bin;$env:Path"   # this session
# (the install.bat bootstrap also SHA-512-verifies the zip and persists PATH)
git clone https://github.com/skip17331/WM3J-ARS-Suite.git $HOME\ARS_Suite
cd $HOME\ARS_Suite
mvn -q -DskipTests -f j-log-engine\pom.xml install
mvn -q -DskipTests -f j-learn\pom.xml install
mvn -q -DskipTests -f j-vault\pom.xml install
foreach ($m in 'j-hub','j-log','j-map','j-digi','j-bridge','j-sat','morse-trainer') {
    mvn -q -DskipTests -f "$m\pom.xml" package
}
.\install.bat -SkipDeps -SkipBuild
.\j-hub\start.bat
```

If a module fails, fix it and re-run just that line — earlier
successes don't need redoing. Open <http://localhost:8081/> once J-Hub
is up.

---

## macOS

Tested on macOS 12 Monterey through 14 Sonoma, both Intel and
Apple Silicon. There's a one-command bootstrap that handles the
macOS-specific bits — arch detection, build, installer, and
Gatekeeper quarantine clearing — so the headline flow is short.

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
2. **Architecture detection.** `uname -m` picks `osx-aarch64`
   (Apple Silicon) or `osx-x64` (Intel) and still downloads the
   matching Gluon JavaFX 21.0.5 SDK to `~/.cache/ars-suite/`.
   *(As of the bundled-JavaFX change the Maven build already embeds
   the correct macOS JavaFX runtime into each jar, so this download
   is now redundant — harmless dead weight pending a
   `bootstrap-mac.sh` cleanup.)*
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

The second run is just build + install (faster). The installer is
safe to re-run — it overwrites the bundle contents and leaves config,
logs, and databases alone.

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
2. `mvn -DskipTests -f j-log-engine/pom.xml install`
3. `mvn -DskipTests -f j-learn/pom.xml install`
4. `mvn -DskipTests -f j-vault/pom.xml install`
5. `mvn -DskipTests package` in each of the seven user-facing modules
   (JavaFX is pulled automatically with the macOS classifier — no SDK
   download or copy needed).
6. `./install.sh`
7. `xattr -dr com.apple.quarantine ~/Applications/WM3J\ *.app` to skip
   the per-module Gatekeeper prompt on first launch.

---

## Raspberry Pi and ARM64

The suite runs on 64-bit ARM — Raspberry Pi 4 / 5, or any aarch64 Linux box
(Ampere, Apple-Silicon Linux VMs, ARM cloud instances). Two ways in: build
from source (identical to the Linux steps), or download the prebuilt ARM
jars from a release.

> **64-bit OS required.** You need a 64-bit (aarch64) OS — Raspberry Pi OS
> (64-bit), Ubuntu for Pi, or Debian arm64. Check with `uname -m`: it must
> print **`aarch64`**. If it says `armv7l` you're on 32-bit Pi OS, which is
> **not supported** — there is no upstream JavaFX desktop build for 32-bit
> ARM (armhf). Reflash with the 64-bit image.
>
> On ARM the build automatically uses **JavaFX 23.0.1** (JavaFX 21.0.3 has
> no linux-aarch64 native). This is detected from the CPU arch — there is
> nothing to set or pass.

### Option A — build from source (same as Linux)

The Linux instructions above work unchanged on a Pi; the build detects
`aarch64` and selects the ARM JavaFX runtime on its own:

```bash
sudo apt update
sudo apt install -y git openjdk-21-jdk maven

git clone https://github.com/skip17331/WM3J-ARS-Suite.git ~/ARS_Suite
cd ~/ARS_Suite

# Shared engine first, then the standalone web apps J-Hub launches
mvn -q -DskipTests -f j-log-engine/pom.xml install
mvn -q -DskipTests -f j-learn/pom.xml install
mvn -q -DskipTests -f j-vault/pom.xml install

# The JavaFX desktop apps
for m in j-hub j-log j-map j-digi j-bridge j-sat morse-trainer; do
  mvn -q -DskipTests -f "$m/pom.xml" package
done

./install.sh
```

Then launch `./j-hub/start.sh` and open <http://localhost:8081/>, exactly as
on desktop Linux.

> **Build time / RAM.** A full build on a Pi 4 / 5 takes longer than on a
> desktop (roughly 10–25 minutes) and wants **4 GB+ RAM**. If you only need
> the wall-display, build just J-Map — see *Standalone J-Map* below. The
> first run downloads JavaFX 23.0.1 (a few tens of MB extra) the first time.

### Option B — download prebuilt ARM jars (no build)

From the latest [release](https://github.com/skip17331/WM3J-ARS-Suite/releases)
grab the **`*-linux-aarch64.jar`** files for the desktop apps, plus the
platform-independent jars (which run on any CPU):

- ARM-native desktop: `j-hub-*-linux-aarch64.jar`, and likewise `j-log`,
  `j-map`, `j-digi`, `j-bridge`, `j-sat`, `morse-trainer`.
- Platform-independent: `j-learn-*.jar`, `j-vault-*.jar`, `j-installer-*.jar`.

JavaFX is bundled inside each jar, so just run them with Java 21 (J-Hub
first):

```bash
sudo apt install -y openjdk-21-jdk          # runtime only; no Maven needed
java -jar j-hub-1.5.0-linux-aarch64.jar     # then open http://localhost:8081/
```

Launch the other desktop apps the same way (`java -jar
<module>-…-linux-aarch64.jar`); J-Learn and J-Vault are the plain jars. The
prebuilt jars don't create menu shortcuts — if you want `.desktop`
integration, use Option A and `./install.sh`.

---

## Standalone J-Map (second-machine display)

J-Map is the one module worth dedicating a second machine to. It's a
read-only DX dashboard — grayline, propagation overlays, live spots,
rig position, EME panel — and the WebSocket protocol is push-only
from the broker side. That makes it the natural "shack display":
hang a monitor on the wall, point a cheap box at your station's
J-Hub, and walk away.

### Shape of the setup

```
   ┌─────────────────────────┐     WebSocket push      ┌─────────────────┐
   │  Station PC             │  ────────────────────►  │  Display box    │
   │  J-Hub + J-Log + …      │  ws://station:8080      │  J-Map only     │
   │  source of truth        │                         │                 │
   └─────────────────────────┘                         └─────────────────┘
```

There is **no second J-Hub** on the display box — two brokers would
have no sync between them. Spots, station identity, rig frequency,
and live config edits all originate on the station PC and ride out
to the display via the broker. If the link drops, J-Map shows a red
banner at the bottom and reconnects with exponential backoff (2 s →
60 s); no manual intervention needed.

The display box can also send a few things *back* — clicking a spot
on the map publishes a `SPOT_SELECTED` that the station's J-Log /
J-Digi consume to dial the rig. So "read-only" is mostly accurate
but not strictly true.

### Picking the display box

Any machine that can run Java 21 + JavaFX 21 with a graphics stack
works. Common picks:

| Box | Notes |
|---|---|
| Raspberry Pi 4 / 5 (4 GB+) | Most popular. Use **64-bit** Pi OS (`uname -m` must say `aarch64`). The build auto-detects `aarch64` and pulls JavaFX 23.0.1 — no SDK swap. See [Raspberry Pi and ARM64](#raspberry-pi-and-arm64). |
| Mac mini / spare Mac laptop | Same auto-detect handles Apple Silicon and Intel. |
| Generic x86 Linux SBC (Intel NUC, mini-PC) | Same as Linux above; no extra steps. |
| Windows mini-PC | Same as above — the build bundles the Windows JavaFX runtime automatically; no extra steps. |

### Recipe: from blank Pi to live dashboard

```bash
# 1. Prereqs (Pi OS / Debian / Ubuntu).
sudo apt update
sudo apt install -y git openjdk-21-jdk maven

# 2. Clone.
git clone https://github.com/skip17331/WM3J-ARS-Suite.git ~/ARS_Suite
cd ~/ARS_Suite

# 3. Build. J-Map depends on the shared logging engine for its
#    bandplan caption, so the engine has to land in your local
#    ~/.m2 first; then build j-map itself.
mvn -q -DskipTests -f j-log-engine/pom.xml install
mvn -q -DskipTests -f j-map/pom.xml package

# 4. Launch, pointing at your station's IP.
./j-map/j-map.sh --hub 192.168.1.42
```

That recipe also works on macOS (`brew install --cask temurin@21
maven git` instead of apt) and on a non-Pi Linux box (no apt
variation needed).

On Windows, install Java 21, Maven, and Git, then run the same clone
and `mvn` build commands. Only the launch line differs — use the
batch launcher instead of the shell script:

```bat
j-map\j-map.bat --hub 192.168.1.42
```

The build bundles the Windows JavaFX runtime automatically, so there
are no extra steps.

### Hub-side: open the firewall

J-Hub binds to all interfaces by default, but a host firewall on
your station PC may block external clients. From the display box:

```bash
nc -zv <station-ip> 8080    # WebSocket — required
nc -zv <station-ip> 8081    # HTTP — only for first-time settings fetch
```

If those refuse, open the ports on the station. Ubuntu/Debian with
ufw, restricting to your LAN:

```bash
sudo ufw allow from 192.168.0.0/24 to any port 8080
sudo ufw allow from 192.168.0.0/24 to any port 8081
```

Adjust the CIDR to match your network.

### Useful launch flags

| Flag | Effect |
|---|---|
| `--hub <host>` | IP or hostname of the station's J-Hub. Overrides `~/.j-map/settings.json`. |
| `--hub-ws-port N` | Override 8080 if your station runs J-Hub on a non-default port. |
| `--hub-web-port N` | Override 8081 likewise (used once at startup to fetch `/api/jmap`). |
| `--launched-by-hub` | Skip the splash; meant for when J-Hub itself spawns J-Map as a child process. |

### Making it a real shack display

The defaults already get you most of the way: F11 toggles fullscreen,
the cursor auto-hides over the map, and the splash overlay shows the
keybinds. To make it boot-and-forget:

- **Autologin to the desktop.** `sudo raspi-config` → System Options
  → Boot/Auto Login → Desktop Autologin.
- **Auto-launch J-Map at login.** Drop a `.desktop` file in
  `~/.config/autostart/` that runs `j-map.sh --hub <ip>`. Or use the
  systemd user unit below if you want crash-recovery too.
- **Disable screen blanking.** `sudo raspi-config` → Display Options →
  Screen Blanking → No. Or in `.xprofile`:
  `xset s off; xset -dpms; xset s noblank`.
- **Rotate the display** (vertical-mount shack screens are common):
  add `display_rotate=1` (90° CW) or `display_rotate=3` (90° CCW) to
  `/boot/firmware/config.txt`.

### Auto-start with crash recovery (optional)

If you want J-Map to relaunch after a crash or reboot, drop a
systemd user unit:

```bash
mkdir -p ~/.config/systemd/user
cat > ~/.config/systemd/user/j-map.service <<'EOF'
[Unit]
Description=J-Map shack display
After=graphical-session.target
PartOf=graphical-session.target

[Service]
ExecStart=%h/ARS_Suite/j-map/j-map.sh --hub 192.168.1.42
Restart=on-failure
RestartSec=10

[Install]
WantedBy=graphical-session.target
EOF

systemctl --user enable --now j-map
sudo loginctl enable-linger $USER   # run without an active login session
```

Replace the IP. Upgrade path:

```bash
cd ~/ARS_Suite && git pull
mvn -q -DskipTests -f j-log-engine/pom.xml install
mvn -q -DskipTests -f j-map/pom.xml package
systemctl --user restart j-map
```

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

```bash
# macOS
cd ~/ARS_Suite
git pull
./bootstrap-mac.sh         # re-runs the whole build + installer in one shot
```

```powershell
# Windows — re-runs build + installer in one shot
cd $HOME\ARS_Suite
git pull
.\install.bat
```

Your config (`~/.j-hub/`, `~/.j-vault/`, `~/.j-log/`, etc.) and
databases are never touched by the installer or by `git pull`.

### Optional follow-up: language packs

English and Spanish ship embedded in every module. To activate
**German, French, Italian, or Portuguese** suite-wide:

```bash
./install-lang-pack.sh de        # or fr / it / pt    (Linux, macOS)
install-lang-pack.bat de         #                    (Windows)
```

That copies `i18n-packs/<module>/messages_<lang>.properties` into
`~/.j-hub/lang/<module>/` for every module that has a pack. Then set
**Language: de** (or whichever) in J-Hub → Station → Regional Settings.
See `i18n-packs/README.md` for the full layout.

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
| `~/.j-hub/lang/<module>/`  | Installed external language packs (DE / FR / IT / PT) |
| `~/.local/share/applications/ars-*.desktop` | Linux menu entries |
| `~/Applications/WM3J *.app/`                | macOS app bundles (per module) |
| `~/Library/Logs/ARS-Suite/<module>.log`     | macOS module log output |
| `%APPDATA%\Microsoft\Windows\Start Menu\Programs\ARS Suite\` | Windows shortcuts |
| `%LOCALAPPDATA%\ARS-Suite\apache-maven-<ver>\` | Maven (Windows; only if it wasn't already on PATH) |

---

## Common problems

**"command not found: mvn" / "git" / "java" (Windows, after install)** —
Open a *new* PowerShell window. The bootstrap adds these to your user
PATH (winget for Git/JDK, the Apache Maven unpack for `mvn`), but an
already-open shell keeps its old PATH until you restart it.

**"java: command not found"** — Same: open a fresh terminal/PowerShell after install.

**"JavaFX runtime components are missing" or "Could not find or load
main class"** — The module's fat jar is stale or missing. JavaFX is
bundled into every jar by the Maven build, so this means
`target/<module>-<version>.jar` wasn't (re)built. Re-run that module's
`mvn -q -DskipTests -f <module>/pom.xml package` (use `\` on Windows)
and relaunch. (This also fixes it after a `git pull` that changed a
module — rebuild before relaunching.)

**"can't be opened because Apple cannot check it for malicious software"
on macOS** — Gatekeeper quarantine. `bootstrap-mac.sh` clears this for
you automatically, but if you ran `install.sh` directly without the
bootstrap, fix all bundles at once:

```bash
xattr -dr com.apple.quarantine ~/Applications/WM3J\ *.app
```

**No app bundles in `~/Applications/` after `./install.sh` on macOS** —
A module's jar wasn't built. The installer skips modules whose target
jar is missing; re-read the installer's output for the "Skipped"
lines, then re-run the matching `mvn package`.

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
