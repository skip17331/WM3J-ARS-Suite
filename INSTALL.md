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

## Prerequisites (both platforms)

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
