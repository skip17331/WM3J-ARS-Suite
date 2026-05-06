<p align="center">
  <img src="splash.png" alt="WM3J ARS Suite Splash Image" width="600">
</p>

# WM3J‑ARS‑Suite

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Build](https://github.com/YOUR-USERNAME/ARS_Suite/actions/workflows/build.yml/badge.svg)](https://github.com/YOUR-USERNAME/ARS_Suite/actions/workflows/build.yml)

WM3J‑ARS‑Suite is a modular, operator‑centric amateur radio software ecosystem designed to provide a unified, streamlined workflow for contesting, everyday logging, mapping, digital modes, and station integration. The goal of this project is to build a modern, reliable, cross‑platform toolkit that supports real operators in real conditions — without the clutter, fragmentation, or outdated interfaces common in legacy ham‑radio software.

---

**New here?** See **[USER_GUIDE.md](USER_GUIDE.md)** for installation
instructions (Linux / macOS / Windows), a web UI walkthrough, per-app setup
notes, troubleshooting, and an architecture map.

**Wiring your radio, rotator, amp, or antenna switch?** See
**[docs/HARDWARE_GUIDE.md](docs/HARDWARE_GUIDE.md)** — a beginner-friendly,
step-by-step guide to USB-to-serial adapters, Hamlib daemons, USB hubs,
stable port names, and common gotchas.

---

## Why This Project Exists

Ham radio operators deserve modern, ergonomic, integrated tools that work consistently across all platforms.

Most existing amateur‑radio applications are:

- platform‑specific  
- visually inconsistent  
- difficult to integrate  
- built on aging UI frameworks  
- not designed for contest‑grade workflows  

WM3J‑ARS‑Suite takes a different approach:  
**modular Java applications with cockpit‑style UIs, unified configuration, and universal backend support.**

---

## Free and Open Source (GPLv3)

The entire WM3J‑ARS‑Suite is **free to use**, **free to modify**, and **free to redistribute** under the  
**GNU General Public License v3 (GPLv3)**.

This ensures that:

- the suite will always remain open and community‑driven  
- improvements made by others stay open  
- operators can customize the software for their own stations  
- the codebase cannot be taken closed‑source  

This licensing choice aligns WM3J‑ARS‑Suite with other major ham‑radio projects such as **Hamlib**, **WSJT‑X**, and **fldigi**, ensuring maximum compatibility and long‑term openness.

---

## Technology Overview

The suite is written entirely in **Java**, using:

- **JavaFX** for a clean, responsive, cross‑platform UI  
- **Maven** for modular builds  
- **Hamlib** (rigctld/rotctld) for radio and rotator control  
- **WSJT‑X** integration via **J‑Bridge**  
- **JSON‑based contest modules** for flexible contest definitions  

Because Java and Hamlib are universally supported, the suite runs on:

- Linux  
- Windows  
- macOS  
- Raspberry Pi  
- Any system that supports Java 17+  

---

## Installation

**Prerequisites** (all platforms): **Java 21**. That's it. Hamlib and WSJT-X
are optional — the Dashboard tab in j-hub's web UI tells you if either is
missing and gives the install command for your OS.

### Linux

```bash
# Install Java (Debian/Ubuntu)
sudo apt install openjdk-21-jdk maven

# Get the suite
git clone https://github.com/YOUR-USERNAME/ARS_Suite.git ~/ARS_Suite
cd ~/ARS_Suite

# Build all modules
mvn -DskipTests -f j-log-engine/pom.xml install
for m in j-hub j-log j-map j-digi j-bridge j-sat; do
    mvn -DskipTests -f "$m/pom.xml" package
done

# Run the installer — writes .desktop entries and icons under
# ~/.local/share/applications and ~/.local/share/icons
./install.sh

# Launch j-hub (other apps auto-start once configured)
./j-hub/start.sh
```

Apps appear in your system menu under **Network / HamRadio**. Fedora / Arch /
openSUSE — same flow, just substitute `dnf install java-21-openjdk-devel maven`
or `pacman -S jdk21-openjdk maven` for the first line.

### Windows

1. Install **Temurin 21** from <https://adoptium.net/temurin/releases/?version=21>
   (tick "Set JAVA_HOME" and "Add to PATH" during setup).
2. Install **Maven** from <https://maven.apache.org/download.cgi> and add its
   `bin\` folder to `PATH`.
3. Unzip or clone the repo to `C:\ARS_Suite` (or anywhere — set
   `ARS_SUITE_HOME` in System Properties → Environment Variables if not there).
4. In a Command Prompt:

```cmd
cd C:\ARS_Suite
mvn -DskipTests -f j-log-engine\pom.xml install
for %m in (j-hub j-log j-map j-digi j-bridge j-sat) do mvn -DskipTests -f %m\pom.xml package
install.bat
j-hub\start.bat
```

`install.bat` writes Start-Menu shortcuts to
`%APPDATA%\Microsoft\Windows\Start Menu\Programs\ARS Suite\`. After that
you can launch everything from the Start Menu.

> **JavaFX:** each module expects `lib\javafx\` alongside its `target\` folder
> with the Windows JavaFX 21 SDK JARs. Grab them from
> <https://gluonhq.com/products/javafx/> if the release bundle didn't include them.

### macOS

```bash
brew install --cask temurin21
brew install maven
git clone https://github.com/YOUR-USERNAME/ARS_Suite.git ~/ARS_Suite
cd ~/ARS_Suite
# build + install as on Linux
mvn -DskipTests -f j-log-engine/pom.xml install
for m in j-hub j-log j-map j-digi j-bridge j-sat; do
    mvn -DskipTests -f "$m/pom.xml" package
done
./install.sh
./j-hub/start.sh
```

If Gatekeeper complains: `xattr -dr com.apple.quarantine ~/ARS_Suite`

### Installation notes

- Both `install.sh` and `install.bat` are **safe to re-run**. They never touch
  `j-hub.json`, databases, or logs — rerun after a rebuild to refresh shortcuts.
- All apps honor the **`ARS_SUITE_HOME`** environment variable if you install
  somewhere other than `~/ARS_Suite` (Linux/macOS) or `%USERPROFILE%\ARS_Suite`
  (Windows).
- Full walkthrough, web-UI tour, and architecture diagram live in
  [USER_GUIDE.md](USER_GUIDE.md).

---

## Core Applications

### **J‑Log**
A dual‑purpose logger designed for both:

- **everyday station logging**, and  
- **full contest logging**

J‑Log supports contest‑grade workflows, real‑time validation, operator‑centric ergonomics, and a clean everyday log mode for normal QSOs.

---

### **J‑Digi**
A digital‑mode engine supporting the most popular amateur radio digital modes, including:

- **RTTY**  
- **PSK31**  
- **Olivia**  
- **MFSK**  
- **Feld Hell**

J‑Digi does **not** interface with WSJT‑X.  
It is designed for classic keyboard‑to‑keyboard and legacy digital modes.

---

### **J‑Bridge**
A backend integration service that connects WM3J‑ARS‑Suite to **WSJT‑X**.

J‑Bridge provides:

- WSJT‑X UDP message handling  
- QSO forwarding  
- Spot forwarding  
- Status and frequency tracking  
- Integration and control by the ARS Suite  

This replaces the older **J‑Wrapper** concept.

---

### **J‑Map**
Real‑time mapping and grid intelligence for operators who need situational awareness during contests, portable operations, or everyday logging.

---

### **J‑Hub**
Backend service manager for:

- Hamlib  
- WSJT‑X (via J‑Bridge)  
- Internal module coordination  
- Single DX Spotter connection via telnet

---

## Project Goals

- Provide a **modern, unified** ham‑radio software suite  
- Maintain **cross‑platform compatibility**  
- Support **universal tools** like Hamlib and WSJT‑X  
- Deliver **fast, ergonomic, contest‑ready** operator workflows  
- Keep the codebase modular, maintainable, and open for expansion  
- Offer a **single logging solution** (J‑Log) for both everyday QSOs and high‑speed contest operation  
- Ensure the entire suite remains **free and open‑source** under GPLv3  

---
