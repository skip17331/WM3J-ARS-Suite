# WM3J ARS Suite — Install Guide

A focused, step-by-step install for **Linux** and **Windows**. Pick your
platform below and run the commands top-to-bottom. Should take about
15-25 minutes (most of that is the first Maven build).

> Looking for the longer walk-through, web-UI tour, and per-app tips?
> See **[USER_GUIDE.md](USER_GUIDE.md)**. This file is just the install.

---

## What you'll end up with

Eight Maven modules, one installer:

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

J-Learn (the in-app reference library) is **bundled inside J-Hub** — no
separate install. Open the J-Learn tab in J-Hub.

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
depend on it. Then install J-Learn (so J-Hub can bundle the content),
J-Vault, and finally package everything else.

```bash
# Shared engine first — must be installed (mvn install) so other modules find it
mvn -q -DskipTests -f j-log-engine/pom.xml install

# Install j-learn (its content is bundled into j-hub)
mvn -q -DskipTests -f j-learn/pom.xml install

# Install j-vault (so j-hub can launch it)
mvn -q -DskipTests -f j-vault/pom.xml install

# Package the apps
for m in j-hub j-log j-map j-digi j-bridge j-sat; do
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

You should see seven entries installed (j-hub, j-log, j-map, j-digi,
j-bridge, j-sat, j-vault).

### 5. Launch J-Hub

From your application menu, search for **WM3J J-Hub**. Or from the
terminal:

```bash
./j-hub/start.sh
```

Open <http://localhost:8081/> in your browser. That's the main UI.

To launch J-Vault separately, search for **WM3J J-Vault** in the menu,
or from J-Hub's left nav click **J-Vault → Launch**.

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

This step is one-time. The Linux flow doesn't need it because the
symlinks point at a working SDK already.

### 4. Build the suite

```powershell
# Shared engine first
mvn -q -DskipTests -f j-log-engine\pom.xml install

# J-Learn content (bundled into j-hub)
mvn -q -DskipTests -f j-learn\pom.xml install

# J-Vault (so j-hub can launch it)
mvn -q -DskipTests -f j-vault\pom.xml install

# Package the apps
foreach ($m in 'j-hub','j-log','j-map','j-digi','j-bridge','j-sat') {
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

You should see seven entries installed.

### 6. Launch J-Hub

Press the Windows key, type **WM3J J-Hub**, and press Enter. Or from
PowerShell:

```powershell
.\j-hub\start.bat
```

Open <http://localhost:8081/> in your browser.

J-Vault has its own Start-Menu entry (**WM3J J-Vault**); from inside
J-Hub you can also launch it via **J-Vault tab → Launch**.

---

## Updating

```bash
# Linux
cd ~/ARS_Suite
git pull
mvn -q -DskipTests -f j-log-engine/pom.xml install
mvn -q -DskipTests -f j-learn/pom.xml install
mvn -q -DskipTests -f j-vault/pom.xml install
for m in j-hub j-log j-map j-digi j-bridge j-sat; do
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
foreach ($m in 'j-hub','j-log','j-map','j-digi','j-bridge','j-sat') {
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

**Maven build fails on j-hub with `Could not resolve … j-learn` / `j-log-engine`** —
You forgot to `mvn install` (not just `mvn package`) the engine and j-learn first.
The build order matters: `j-log-engine → j-learn → j-vault → others`.

**Installer prints "skipped (missing build: …)"** — That module didn't build.
Re-run its specific `mvn package` line and look for the actual error.

**J-Vault launches but the J-Hub J-Vault tab shows "connection refused"** —
J-Vault is a separate process on port 8083. Click **J-Vault → Launch** in
J-Hub's left nav, wait 3-5 seconds for Jetty to start, then click **Reload**
on the embedded view.

For deeper troubleshooting, see **[USER_GUIDE.md § Troubleshooting](USER_GUIDE.md#6-troubleshooting)**.
