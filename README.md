<p align="center">
  <img src="splash.png" alt="WM3J ARS Suite Splash Image" width="600">
</p>

# WM3J‑ARS‑Suite

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://adoptium.net/temurin/releases/?version=21)
[![Platforms](https://img.shields.io/badge/platforms-Linux%20%7C%20Windows%20%7C%20macOS-success.svg)](#)

> **A modern, modular, operator‑first amateur radio suite.**
> One JavaFX application that runs your whole shack — **docked** in a single
> window, or with each module **loose** in its own window sharing one
> background hub. One config, one cluster connection, one set of credentials.

WM3J‑ARS‑Suite is a complete, integrated software ecosystem for the modern
ham. Logging, mapping, digital modes, WSJT‑X integration, satellite tracking,
inventory, an in‑app reference library, and antenna design — all written in
Java, all GPLv3, all in **one application** instead of a dozen separate
programs.

---

**Just want to install it?** → **[INSTALL.md](INSTALL.md)** — focused
step‑by‑step setup for Linux and Windows (git, Java, Maven, build, run).

**Already installed?** → **[USER_GUIDE.md](USER_GUIDE.md)** — app
walkthrough, per‑module setup notes, troubleshooting, and an architecture map.

**Wiring your radio, rotator, amp, or antenna switch?** →
**[docs/HARDWARE_GUIDE.md](docs/HARDWARE_GUIDE.md)** — beginner‑friendly
guide to USB‑to‑serial adapters, Hamlib daemons, USB hubs, stable port
names, and common gotchas.

**Curious what's coming next?** → **[docs/ROADMAP.md](docs/ROADMAP.md)** —
phased plan of work in flight + features explicitly *not* on the
roadmap, with reasons.

**Want to write a contest or award plug-in?** →
**[docs/PLUGINS.md](docs/PLUGINS.md)** — JSON schemas with worked
examples for both surfaces. No code, no rebuild.

---

## What's in the box

One app, these modules — each a surface you can dock or float in its own window.

| Module | What it does |
|---|---|
| **J‑Hub** | The control surface: dashboard, station identity, callsign lookups, macros, DX cluster + RBN, log uploaders, antenna workshop, and all settings. Reached from the dock (docked) or the ⚙ gear (loose). |
| **J‑Log** | Dual‑purpose QSO logger: everyday log **and** full contest mode. Real‑time validation, multipliers, JSON contest plug‑ins. |
| **J‑Map** | Real‑time DX map with grayline, propagation overlays, weather, aurora, and great‑circle paths to spotted DX. |
| **〰 J‑Digi** | Classic keyboard‑to‑keyboard digital modem: RTTY, PSK31, Olivia, MFSK, Feld Hell. (FT8 lives in J‑Bridge.) |
| **⇄ J‑Bridge** | WSJT‑X integration. Forwards QSOs, spots, status, and frequency back into the suite over UDP. |
| **J‑Sat** | Satellite tracker with Doppler correction and rotor control. |
| **J‑Vault** | Shack inventory + first‑call contacts + a one‑click **Estate Handoff PDF wizard** so your family knows who to call when you're SK. SQLite‑backed. |
| **J‑Learn** | Embedded amateur‑radio reference library — ~200 sections covering propagation, antennas, RF safety, troubleshooting, formulas, operating practice, emcomm. Searchable; markdown source seeds to `~/.j-learn/content/` so you can edit content without rebuilding. |
| **Morse Trainer** | Learning and practicing CW — Koch‑method letter trainer, QSO simulator, sending trainer with real‑time decoding, analytics, and optional Arduino / Pi keyer hardware support. |

A shared **j‑log‑engine** library underpins logging across J‑Log, J‑Digi,
and J‑Bridge so everything writes to one set of databases under `~/.j‑log/`.
In **loose** mode the per‑module windows share live state (spots, rig, rotor,
station identity) through a small **background hub** in the system tray; in
**docked** mode they all share one process.

---

## Highlights

### Antenna Workshop (inside J‑Hub)

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

### J‑Vault — the SK problem, solved

When an operator becomes a Silent Key, families inherit a shack of expensive
gear they don't recognize. J‑Vault turns your inventory into a
**printable handoff document** — cover page, personal note, first‑call
contacts (priority‑sorted), full equipment inventory grouped by type,
sale‑recommendation matrix (HRO / DXE / R&L / Universal / GigaParts), an
11‑step plain‑language checklist, and a glossary for non‑hams — exported as a
printable handoff document.

### J‑Learn — your reference library, in‑app

Twenty‑two chapters, ~200 sections, written for operators who already
have their license. Searchable, with an Advanced toggle for engineering
depth. Cross‑references jump straight to the calculator or sub‑section
you need. Bundled into J‑Hub — no separate install, no internet required.

### One settings surface

Every operator preference lives in **J‑Hub inside the app** — callsign, grid,
rig backend, Hamlib host/port, IARU region + country (for bandplan captions),
PTT method, CW WPM, DX cluster, log uploaders, macros. Reach it from the dock
(docked) or the ⚙ gear in any window's top bar (loose). One place, no per‑app
duplicate config files; in loose mode the change reaches every window through
the background hub.

### Six languages, suite-wide

UI strings across every module are translatable — English, Spanish,
German, French, Italian, Portuguese. EN + ES ship embedded; the
other four ride as drop-in `.properties` packs under `i18n-packs/`
so native speakers can polish translations without rebuilding Java.
See `i18n-packs/README.md` for details.

### Macros that share state

Every app uses the same macro engine. `{MYCALL}`, `{CALL}`, `{RST_S}`,
`{RST_R}`, `{NAME}`, `{EXCH}`, `{SERIAL}`, `{NR}`, `{FREQ}`, `{BAND}`,
`{MODE}` — the values come from the live rig + the QSO entry pane in
J‑Log/J‑Digi, **not** from per‑app duplicates.

### Log uploaders

Push QSOs from J‑Log to **eQSL.cc**, **Club Log**, **QRZ Logbook**, and
**HRDLog** with one click. Credentials are encrypted on disk
(AES‑GCM, key tied to this machine). Already‑uploaded QSOs are tracked
so subsequent runs only push the new ones.

### J‑Map

Real‑time grayline + DX spots + ITU/CQ zones + Skywarn / aurora /
geomagnetic alerts + tropo + lightning + weather radar. Shares one DX‑cluster
connection with the rest of the suite (one telnet connection serves every
module). Runs loose on a second machine as a dedicated shack display.

### J‑Sat

Doppler‑corrected satellite tracking with rotor control. ISS APRS, FM birds,
linear birds. Pass prediction with editable keplerian elements.

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

WM3J‑ARS‑Suite takes a different approach: **one Java application with
cockpit‑style module surfaces, unified configuration, and universal backend
support.** One DX cluster connection shared by every module. One station
identity. One macro engine. One inventory. One reference library. Dock it all
in a single window, or float the modules you want — your choice.

---

## Quick install

**Full step‑by‑step instructions are in [INSTALL.md](INSTALL.md).**
The 30‑second version:

### Linux

```bash
# Debian family:  sudo apt update && sudo apt install -y git openjdk-21-jdk maven
# Fedora family:  sudo dnf install -y git java-21-openjdk-devel maven
git clone https://github.com/skip17331/WM3J-ARS-Suite.git ~/ARS_Suite
cd ~/ARS_Suite
./install.sh                       # builds the app + adds menu shortcuts
./ars-fx/ars-fx.sh                 # docked; or  --module log  (loose) / --hub (tray)
```

Shortcuts appear in your system menu under **Network / HamRadio** — **ARS
Suite** (docked) plus a window per module and **ARS Suite Hub**. Arch /
openSUSE / other distros — same flow, see [INSTALL.md](INSTALL.md) for the
package-manager line.

### Windows

**One command.** Install Git if you don't have it, clone, then run —
or just **double-click** — `install.bat`. It installs Java + Maven,
builds every module, and adds Start-Menu shortcuts.

```powershell
winget install --id Git.Git -e        # skip if you already have Git
git clone https://github.com/skip17331/WM3J-ARS-Suite.git $HOME\ARS_Suite
cd $HOME\ARS_Suite
.\install.bat                          # ~5–10 min first run; or double-click it
```

Then press the Windows key, type **ARS Suite**, and press Enter (or open a
single module's shortcut). Re-running `install.bat` after a `git pull` is the
upgrade path. JavaFX is bundled into the jar automatically — no SDK download.
If anything fails it writes `install-log.txt` — attach that to a GitHub issue.
Full walkthrough in [INSTALL.md](INSTALL.md).

### macOS

```bash
brew install --cask temurin@21
brew install maven git
git clone https://github.com/skip17331/WM3J-ARS-Suite.git ~/ARS_Suite
cd ~/ARS_Suite
./install.sh                              # detects Apple Silicon vs Intel
open ~/Applications/ARS\ Suite.app
```

`./install.sh` builds the matching jar for your Mac and plants a `.app` bundle
per shortcut in `~/Applications/` (**ARS Suite.app**, **J-Log.app**, …). First
launch may need a Gatekeeper unblock — see [INSTALL.md](INSTALL.md#macos).
Re-run after any `git pull` to upgrade. Per-window logs go to
`~/Library/Logs/ARS-Suite/`.

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
- **JavaFX 21** — clean, responsive, cross‑platform UI; one app, all modules.
- **Maven** — one shaded jar per OS, built from the `ars-fx` module over the
  shared engine library (`j-log-engine`).
- **Java‑WebSocket** — in **loose** mode a small background hub (port 8090)
  shares live state (spots, rig, rotor, station) with each module window, and
  with a J‑Map/J‑Sat display on a second machine.
- **SQLite** — local persistence for QSOs, contests, macros, inventory,
  callsign cache.
- **Hamlib** (`rigctld` / `rotctld` / `ampctld`) — universal rig / rotor / amp
  backend.
- **WSJT‑X** integration via **J‑Bridge** UDP listener.
- **JSON contest modules** — drop‑in plug‑ins for new contests.
- **Estate handoff** — J‑Vault produces a printable inventory + first‑call
  contacts document for your family.

Runs on:

- Linux (every flavor with Java 21+)
- Windows 10 / 11
- macOS (Apple Silicon and Intel)
- Raspberry Pi 4 / 5 (great as a loose J‑Map shack display)

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
  tools — through one shared configuration and live state.
- Ensure the entire suite remains **free and open‑source** under GPLv3.

---

## Contributing

Pull requests, issue reports, contest plug‑ins, antenna calculators, and
documentation tweaks are all welcome. The codebase is intentionally
modular: pick a module, build it, hack on it independently. See
[CodeOfConduct.md](CodeOfConduct.md).

For broader context on what's planned next and what's in flight, see
**[docs/ROADMAP.md](docs/ROADMAP.md)** — and the architecture appendix
in [USER_GUIDE.md § Interconnection Map](USER_GUIDE.md#7-appendix-interconnection-map).

73 — **WM3J**
