<p align="center">
  <img src="splash.png" alt="WM3J ARS Suite Splash Image" width="600">
</p>

# WM3J‑ARS‑Suite

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://adoptium.net/temurin/releases/?version=21)
[![Platforms](https://img.shields.io/badge/platforms-Linux%20%7C%20Windows%20%7C%20macOS-success.svg)](#)

> **A modern, modular, operator‑first amateur radio suite.**
> Eight purpose‑built apps that share one config, one cluster connection,
> and one set of operator credentials — so your shack stops being seven
> programs glued together with sticky tape.

WM3J‑ARS‑Suite is a complete, integrated software ecosystem for the modern
ham. Logging, mapping, digital modes, WSJT‑X integration, satellite tracking,
inventory, an in‑app reference library, antenna design, and a brokered web
control hub — all written in Java, all GPLv3, all designed to feel like
**one application** instead of seven.

---

**Just want to install it?** → **[INSTALL.md](INSTALL.md)** — focused
step‑by‑step setup for Linux and Windows (git, Java, Maven, build, run).

**Already installed?** → **[USER_GUIDE.md](USER_GUIDE.md)** — web UI
walkthrough, per‑app setup notes, troubleshooting, and an architecture map.

**Wiring your radio, rotator, amp, or antenna switch?** →
**[docs/HARDWARE_GUIDE.md](docs/HARDWARE_GUIDE.md)** — beginner‑friendly
guide to USB‑to‑serial adapters, Hamlib daemons, USB hubs, stable port
names, and common gotchas.

---

## What's in the box

Eight Maven modules. Built once, work together.

| Module | What it does |
|---|---|
| **🎛 J‑Hub** | Central broker + browser‑based control surface (port 8081). Dashboard, station identity, callsign lookups, macros, DX cluster + RBN, log uploaders, antenna workshop, J‑Learn — all in one web UI. |
| **📋 J‑Log** | Dual‑purpose QSO logger: everyday log **and** full contest mode. Real‑time validation, multipliers, JSON contest plug‑ins. |
| **🗺 J‑Map** | Real‑time DX map with grayline, propagation overlays, weather, aurora, and great‑circle paths to spotted DX. |
| **〰 J‑Digi** | Classic keyboard‑to‑keyboard digital modem: RTTY, PSK31, Olivia, MFSK, Feld Hell. (FT8 lives in J‑Bridge.) |
| **⇄ J‑Bridge** | WSJT‑X integration. Forwards QSOs, spots, status, and frequency back into the suite over UDP. |
| **🛰 J‑Sat** | Satellite tracker with Doppler correction and rotor control through J‑Hub. |
| **📦 J‑Vault** *(new)* | Shack inventory + first‑call contacts + a one‑click **Estate Handoff PDF wizard** so your family knows who to call when you're SK. SQLite‑backed; runs in its own window on port 8083. |
| **📖 J‑Learn** *(new, bundled inside J‑Hub)* | An in‑app reference library: ~200 chapters covering propagation, antennas, RF safety, troubleshooting, formulas, operating practice, emcomm. Searchable; per‑chapter "Open in Workshop" deep‑links to the matching calculators. |

A shared **j‑log‑engine** library underpins logging across J‑Log, J‑Digi,
and J‑Bridge so everything writes to one set of databases under `~/.j‑log/`.

---

## Highlights

### 🧮 Antenna Workshop (inside J‑Hub)

A **questionnaire‑driven antenna recommender** + **13 live calculators**:

- **Recommender** — answers a handful of questions about your QTH, HOA
  status, bands, height, stealth needs, and budget, then ranks antennas
  that actually fit.
- **Calculators** — flat dipole · inverted‑V · fan dipole · trapped dipole ·
  OCF/Windom · EFHW (with and without traps) · J‑pole · Yagi‑Uda · vertical ·
  loading coils · trap design · magnetic loop · NanoVNA trim workflow.
- **Per‑formula calculators** for J‑Learn's Formulas chapter — Ohm's law,
  power, reactance, impedance, resonance, wavelength, SWR, ERP, feedline
  loss, decibels, Q factor, bandwidth, Smith chart, RF exposure.

Click a chapter card in J‑Learn → it opens the matching calculator
pre‑filled with sane defaults.

### 📦 J‑Vault — the SK problem, solved

When an operator becomes a Silent Key, families inherit a shack of expensive
gear they don't recognize. J‑Vault turns your inventory into a
**printable handoff document** — cover page, personal note, first‑call
contacts (priority‑sorted), full equipment inventory grouped by type,
sale‑recommendation matrix (HRO / DXE / R&L / Universal / GigaParts), an
11‑step plain‑language checklist, and a glossary for non‑hams. Real PDF
output via bundled jsPDF — no print dialog, no copy‑paste.

### 📖 J‑Learn — your reference library, in‑app

Twenty‑two chapters, ~200 sections, written for operators who already
have their license. Searchable, with an Advanced toggle for engineering
depth. Cross‑references jump straight to the calculator or sub‑section
you need. Bundled into J‑Hub — no separate install, no internet required.

### 🎙 Macros that share state

Every app uses the same macro engine. `{MYCALL}`, `{CALL}`, `{RST_S}`,
`{RST_R}`, `{NAME}`, `{EXCH}`, `{SERIAL}`, `{NR}`, `{FREQ}`, `{BAND}`,
`{MODE}` — the values come from the live rig + the QSO entry pane in
J‑Log/J‑Digi, **not** from per‑app duplicates.

### ☁ Log uploaders

Push QSOs from J‑Log to **eQSL.cc**, **Club Log**, **QRZ Logbook**, and
**HRDLog** with one click. Credentials are encrypted on disk
(AES‑GCM, key tied to this machine). Already‑uploaded QSOs are tracked
so subsequent runs only push the new ones.

### 🗼 J‑Map

Real‑time grayline + DX spots + ITU/CQ zones + Skywarn / aurora /
geomagnetic alerts + tropo + lightning + weather radar. Pulls spots from
the brokered J‑Hub feed (one telnet connection serves the whole suite).

### 🔭 J‑Sat

Doppler‑corrected satellite tracking with rotor control through J‑Hub.
ISS APRS, FM birds, linear birds. Pass prediction with editable
keplerian elements.

---

## Why This Project Exists

Ham radio operators deserve modern, ergonomic, integrated tools that
work consistently across all platforms.

Most existing amateur‑radio applications are:

- platform‑specific
- visually inconsistent
- difficult to integrate
- built on aging UI frameworks
- not designed for contest‑grade workflows

WM3J‑ARS‑Suite takes a different approach: **modular Java applications
with cockpit‑style UIs, unified configuration, and universal backend
support.** One DX cluster connection broadcast to every app. One station
identity. One macro engine. One inventory. One reference library.
Everything else is a tab.

---

## Quick install

**Full step‑by‑step instructions are in [INSTALL.md](INSTALL.md).**
The 30‑second version:

### Linux (Debian/Ubuntu)

```bash
sudo apt install -y git openjdk-21-jdk maven
git clone https://github.com/skip17331/WM3J-ARS-Suite.git ~/ARS_Suite
cd ~/ARS_Suite
mvn -q -DskipTests -f j-log-engine/pom.xml install
mvn -q -DskipTests -f j-learn/pom.xml install
mvn -q -DskipTests -f j-vault/pom.xml install
for m in j-hub j-log j-map j-digi j-bridge j-sat; do
    mvn -q -DskipTests -f "$m/pom.xml" package
done
./install.sh
./j-hub/start.sh   # then open http://localhost:8081/
```

Apps appear in your system menu under **Network / HamRadio**.

### Windows

```powershell
winget install --id Git.Git -e
winget install --id EclipseAdoptium.Temurin.21.JDK -e
winget install --id Apache.Maven -e
git clone https://github.com/skip17331/WM3J-ARS-Suite.git $HOME\ARS_Suite
cd $HOME\ARS_Suite
# (one-time: copy Windows JavaFX SDK into each module's lib\javafx — see INSTALL.md)
mvn -q -DskipTests -f j-log-engine\pom.xml install
mvn -q -DskipTests -f j-learn\pom.xml install
mvn -q -DskipTests -f j-vault\pom.xml install
foreach ($m in 'j-hub','j-log','j-map','j-digi','j-bridge','j-sat') {
    mvn -q -DskipTests -f "$m\pom.xml" package
}
.\install.bat
.\j-hub\start.bat
```

Shortcuts land in the Start Menu under **ARS Suite**.

> **macOS:** `brew install --cask temurin21 && brew install maven git`,
> then the same source/build flow as Linux. See INSTALL.md for the
> Gatekeeper note.

---

## Free and Open Source (GPLv3)

The entire suite is **free to use**, **free to modify**, and **free to
redistribute** under the **GNU General Public License v3 (GPLv3)**.

- the suite will always remain open and community‑driven
- improvements made by others stay open
- operators can customize for their own stations
- the codebase cannot be taken closed‑source

Same license as **Hamlib**, **WSJT‑X**, and **fldigi** — maximum
compatibility, long‑term openness.

---

## Technology Overview

- **Java 21+** — the suite is pure Java; no native code.
- **JavaFX 21** — clean, responsive, cross‑platform UI.
- **Maven** — modular builds with one shared engine library
  (`j-log-engine`).
- **Embedded Jetty** — J‑Hub serves the web control surface on
  port 8081; J‑Vault serves its own UI on port 8083.
- **Java‑WebSocket** — J‑Hub fans out a single broker connection to all
  the desktop apps so they stay in sync without polling.
- **SQLite** — local persistence for QSOs, contests, macros, inventory,
  callsign cache.
- **Hamlib** (`rigctld` / `rotctld`) — universal rig and rotor backend.
- **WSJT‑X** integration via **J‑Bridge** UDP listener.
- **JSON contest modules** — drop‑in plug‑ins for new contests.
- **jsPDF + jspdf‑autotable** — bundled inside J‑Vault for real,
  client‑side PDF generation.

Runs on:

- Linux (every flavor with Java 21+)
- Windows 10 / 11
- macOS (Apple Silicon and Intel)
- Raspberry Pi 4 / 5 (J‑Hub headless mode is great here)

---

## Project Goals

- Provide a **modern, unified** ham‑radio software suite.
- Maintain **cross‑platform compatibility** — same code, same UI,
  Linux / Windows / macOS / Raspberry Pi.
- Support **universal tools** like Hamlib and WSJT‑X — not vendor‑locked.
- Deliver **fast, ergonomic, contest‑ready** operator workflows.
- Keep the codebase **modular**, **maintainable**, and **open** for
  community contribution.
- Offer **a single logging solution** (J‑Log) for both everyday QSOs
  and high‑speed contest operation.
- Treat the **station as one system** — not a binder full of incompatible
  tools — through brokered J‑Hub state.
- Ensure the entire suite remains **free and open‑source** under GPLv3.

---

## Contributing

Pull requests, issue reports, contest plug‑ins, antenna calculators, and
documentation tweaks are all welcome. The codebase is intentionally
modular: pick a module, build it, hack on it independently. See
[CodeOfConduct.md](CodeOfConduct.md).

For broader context on what's planned next and what's in flight, the
roadmap lives in [USER_GUIDE.md § Architecture](USER_GUIDE.md#7-appendix-interconnection-map).

73 — **WM3J**
