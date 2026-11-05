# WM3J ARS Suite — User Guide

How to set up and operate the suite. To install it, see **[INSTALL.md](INSTALL.md)**. The project's purpose and
licence are in [README.md](README.md).

> **Wiring up real radio gear?** Read [docs/HARDWARE_GUIDE.md](docs/HARDWARE_GUIDE.md) first. It covers cables,
> USB-to-serial adapters, USB hubs and stable port names. This guide assumes your radio is already plugged in and shows
> up as a serial port on the computer.

## Contents

1. [Overview](#1-overview)
2. [First-run setup](#2-first-run-setup)
3. [J-Hub — settings and data](#3-j-hub--settings-and-data)
4. [The modules](#4-the-modules)
5. [Macros and transmitting](#5-macros-and-transmitting)
6. [Running loose, and a second-machine display](#6-running-loose-and-a-second-machine-display)
7. [Troubleshooting](#7-troubleshooting)
8. [Appendix: where your data lives](#8-appendix-where-your-data-lives)

---

## 1. Overview

The suite is **one JavaFX application**. A dock down the left edge switches between its screens (the modules).
Hover over almost anything — buttons, drawers, switches, the dock — for a tooltip.

Side panels fold to a thin strip and slide open while the mouse is over them. **Pin** keeps a panel open, and the suite
remembers that; **Unpin** lets it collapse again.

| Module | What it is for |
|--------|----------------|
| **J-Hub** | The dashboard and every setting: station, radio hardware, cluster, log databases, uploads, macros, satellites |
| **J-Log** | Logging, in *normal* mode and *contest* mode; also the small **J-Log-Quick** form on the J-Hub dashboard |
| **J-Map** | DX map with grayline, propagation, spots and beam headings |
| **J-Digi** | Built-in digital-mode decoder: CW, RTTY, PSK31, Olivia, MFSK16, DominoEX, AX.25 |
| **J-Bridge** | Brings WSJT-X decodes (FT8, FT4 and others) into the suite |
| **J-Sat** | Satellite passes, rig and rotor tracking, Doppler |
| **J-Vault** | Shack inventory and first-call contacts |
| **J-Learn** | Reference library, with the Morse Trainer and a QSO simulator |

You can turn modules off in **J-Hub ▸ Modules**; a disabled module disappears from the dock.

### Docked or loose

- **Docked** (the **ARS Suite** shortcut): everything in one window, one program. Live data is shared directly.
- **Loose** (the per-module shortcuts, or `--module <id>`): each module in its own window. They share data through a
  small **background hub** in the system tray, which owns the radio connections and feeds. The first loose window
  you open starts it. A loose window has no dock; the **⚙ gear** in its top bar opens J-Hub settings, with a
  "← Back" link to return.

The hub's only network port is **8090**. See [§6](#6-running-loose-and-a-second-machine-display).

### Starting up

A splash screen shows while the app loads. The tray icon is the suite's red **J**. If your desktop has no tray, the
hub simply runs without one.

---

## 2. First-run setup

1. **Open J-Hub ▸ Station.** Enter your **callsign**, **operator name**, **grid square**, latitude and longitude, and
   elevation. Pick your **IARU region**. These drive band-plan captions and every
   bearing, grayline and satellite pass in the suite — set your location before using J-Map or J-Sat.
2. **Choose units.** *Station ▸ Units* switches the whole app between **Metric** (km, °C, km/h, hPa, m) and
   **Standard (US)** (mi, °F, mph, inHg, ft).
3. **Connect your radio** under **J-Hub ▸ Rig control** (see [§3.2](#32-hardware-rig-rotor-amplifier-antenna-switch)).
4. **Pick a DX cluster** under **J-Hub ▸ Data ▸ DX cluster** and press **Connect** on the cluster panel
   (see [§3.3](#33-data)).
5. **Open J-Log** and log a contact. Your normal log is created automatically at `~/.j-log/j-log.db`.

Nothing else is needed to start. Settings are saved as you change them to `~/.j-hub/config.json` and take effect
straight away — in loose mode the change reaches every open window.

### Optional software

| | Linux (Debian/Ubuntu) | Fedora | Arch | macOS | Windows |
|---|---|---|---|---|---|
| **Hamlib** | `sudo apt install libhamlib-utils` | `sudo dnf install hamlib` | `sudo pacman -S hamlib` | `brew install hamlib` | <https://hamlib.github.io/> |
| **WSJT-X** | `sudo apt install wsjtx` | download from <https://wsjt.sourceforge.io/wsjtx.html> | AUR `wsjtx` | `brew install --cask wsjtx` | download from the same page |

Hamlib is needed only to control a rig, rotor or amplifier. WSJT-X is needed only for J-Bridge.

---

## 3. J-Hub — settings and data

J-Hub is the first screen. Its left menu has these pages: **Dashboard**, **Station**, **Hardware** (Rig control,
Rotor control, Amplifier, Antenna switch), **Data**, **Satellites** and **Modules**.

### 3.1 Dashboard and Station

**Dashboard** is a live operating view. It holds the quick **New QSO** form (J-Log-Quick) with its F1–F8 macro bar, a
live **DX cluster** list with band, mode and "needed only" filters, a **Saved QSOs** strip, and a right-hand rail with
the **rig**, **rotor**, **amplifier**, propagation, solar and space-weather, and band conditions. A button opens the
full J-Log.

**Station** holds your identity (callsign, operator name), location (grid, latitude, longitude, elevation), ITU and CQ
zones, IARU region, default TX power and the **Units** switch.

### 3.2 Hardware: rig, rotor, amplifier, antenna switch

Each page has the same shape: a **model**, the **Hamlib model number**, **interface**, **serial port** and **baud**
(or **host/port** for a network device), and a switch **Start rigctld / rotctld / ampctld for me**.

- **Switch on** and the suite starts the Hamlib daemon with the model and port you gave it.
- **Switch off** and it connects to a daemon you run yourself, at the host and port shown.

Extra settings:

- **Rig control** — poll interval, auto band-follow, and **PTT & keying**: PTT method, CW keyer and the **PTT delay**
  used when sending macros (see [§5](#5-macros-and-transmitting)).
- **Rotor control** — overlap/wrap, park position, dead-band, **auto-track satellites (J-Sat)** and the beam presets
  (Europe, Africa, S. America, Caribbean, Asia, Oceania).
- **Amplifier** — follow-rig-band, maximum power, standby on TX inhibit.
- **Antenna switch** — model, host and port, follow-rig-band, and a six-port map of antenna names and bands.

On Linux, make sure you are in the `dialout` group to reach serial ports: `ls -l /dev/ttyUSB*`.

### 3.3 Data

One long page of everything that carries data in or out. It is organised into sections:

| Section | What it does |
|---------|--------------|
| **Log database** | Shows exactly which database and table J-Log is using, with its QSO count and size. **Open folder** and **Open J-Log** shortcuts. |
| **Contest databases** | Lists every contest log with its contest, start date and QSO count. **Archive** moves one into storage; **Restore** brings it back. |
| **Archived contest databases** | The archived logs, kept in `~/.j-log/archive/<Contest>_<Year>/`. |
| **J-Learn content** | Where J-Learn's chapters come from and how to refresh them — see [§4.8](#48-j-learn). |
| **Backup, export & import** | **Export ADIF** or **Export CSV** to a timestamped file in the backup folder, and **Import ADIF…** into the normal log (QSOs already in the log are skipped). |
| **Online log upload** | **LoTW** (signed with TrustedQSL), **eQSL.cc**, **QRZ Logbook** and **Club Log**, each with its login, an **Upload** button and an auto-upload tick. |
| **Label printers (beta)** | Printer model (Brother QL, DYMO LabelWriter, Zebra, Rollo or any system printer), label stock, printer or network address, copies and a **Test print** button. Used by **Print QSL label** in J-Log (§4.1). |
| **Logbook & export** | Cabrillo / CSV character set and LoTW / eQSL auto-upload. |
| **Contest / Cabrillo** | The categories printed in your Cabrillo file (operator, power, assisted, band, mode, station, club) plus Sweepstakes precedence, check and section. |
| **DX cluster** | A **pick list of clusters** (or type your own `host:port`; one runs at a time), login callsign, auto-connect, band and mode filters, and **connection tries** (the retry limit; 0 keeps trying). |
| **RBN** | Auto-connect on start, and its own connection-tries limit. |
| **WSJT-X / digital** | UDP port (2237), auto-log FT8/FT4 contacts, PSK Reporter upload. |
| **Callsign lookup** | Callook.info (free, US), HamQTH, QRZ.com XML, QRZCQ, HamCall, and a local CSV callbook file. Tried top to bottom; the first answer fills name, QTH, grid, state and county. |
| **Remote sharing** | Share live data with solo J-Map / J-Sat windows on other machines over the LAN (port 8090). |
| **Transmit** | CW speed, the voice output device and the **PTT delay**. |
| **Station macros** | The F1–F8 keys — see [§5](#5-macros-and-transmitting). |

### 3.4 Satellites

**J-Hub ▸ Satellites** is where you choose what J-Sat tracks.

- **Orbital data.** *Refresh now* downloads fresh elements from Celestrak and AMSAT; *Refresh automatically* does it
  daily. Without a download the suite uses a bundled snapshot.
- **Selection.** Search and filter everything the data covers, tick the satellites you want, and use *Select shown*,
  *Clear shown* or *Reset to defaults*. "Only satellites with orbital data" and "Has a pass in the next 24 h above N°"
  narrow the list. A pick that has no data is looked up on Celestrak by its NORAD number.

### 3.5 Modules

Switch each module on or off. J-Hub itself is always on.

---

## 4. The modules

### 4.1 J-Log

J-Log is a single screen. A bar across the top always tells you which database you are writing to, with **Switch
database** and **Database tools ›** (which opens J-Hub ▸ Data).

**Normal mode**

- **New QSO** — callsign, band, mode, frequency, RST sent and received, name, QTH, state, county, grid, power and
  comment, then **Log QSO ⏎**. Typing a call looks it up (see *Callsign lookup* above) and shows its DXCC entity,
  beam heading and your previous contacts with it. **Print QSL label** prints a label for each new QSO when you save it; pick the printer in Setup ▸ Logbook ▸ Label printers. *(Beta.)*
- **Recent log** — your latest QSOs; select one to edit or delete it.
- **DX Cluster** — live spots with a **Console** button that opens a raw-feed pop-up with a command line (history, and
  shortcuts such as `SH/DX 20` and `SET/FT8`). The status is always **Disconnected**, **Connecting** or **Connected**,
  and **Stop** cancels a connection that is not coming up.
- **Heard By** and **Skimmer** — who has heard you on the Reverse Beacon Network, and CW skimmer decodes.
- **RTTY / CW · TX & RX** — a text pane for the modem.
- **Rig and band controls**, propagation and space weather in the drawers beside the form.
- **Translator** — opens the QSO phrase translator (below).

**Contest mode** — press **⇄ Contest mode**.

1. **Choose a contest** from the list of about 87 supported contests. J-Log offers the last one you used first.
2. **Choose a database.** Either **continue an existing one** (each is shown with its contest and start date), or
   **start a new one**. A new log gets a suggested name such as `NovSwpSSB2026.db`, which you can change.
3. Log in the contest layout: an entry bar built from the contest's exchange, a live score line, and the dupe and
   multiplier panels for that contest. **Change contest**, **Switch database** and **← Normal log** are on the screen.
4. **Export Cabrillo ⤓** writes the file for submission, using the categories set in J-Hub ▸ Data.

Contest databases live in `~/.j-log/contest-logs/`. When you finish with one, archive it from J-Hub ▸ Data.

**Translator.** Pick a language and a phrase; it shows the phrase in that language with a pronunciation guide to
say it with. It covers Spanish, German, French, Italian and Portuguese, and, set to *By country*, it follows the DXCC country of the
call you are logging. The German and Portuguese pronunciation guides are approximations and have not been reviewed by
native speakers.

### 4.2 J-Map

A full-screen map with a controls panel:

- **Projection** — rectangular, or azimuthal centred on your grid.
- **Overlays** — grayline and the other map layers, switched on and off.
- **DX Spots** — filter by **band**, **mode**, **spot age** and **need** (ATNO / band-fill). Select a spot to see its
  DXCC entity, beam heading and distance in **DX station info**.
- **Who's hearing me · RBN** — plots stations that have heard you.
- **NCDXF beacons** — show the beacon cycle.
- **Beam heading** shows where the rotor is pointing, on both map views. With no rotor answering there is no beam and no
  heading.

Clicking a spot on the map or in the list tunes the rig and pops up the station's details. **Log this** opens the
**New QSO** form, filled in from the spot. Press **Enter** to log it to your **normal log**, or **Esc** or **Clear** to
drop it. Drag the form by its top row. **Pin** moves it into a drawer in the right column, where it stays and empties after each QSO; while it is docked, a
spot click fills it directly and the pop-up has no **Log this** button. **+
QSO** at the top left of the map opens a blank card. Contest logging stays in J-Log.

Set your latitude and longitude first. Distances follow your Units setting.

### 4.3 J-Digi

The built-in modem decodes from your sound card. Pick a mode tab — **CW, RTTY, PSK31, Olivia, MFSK16, DominoEX** or
**AX.25** — and watch the **waterfall** and the **decoder** text. The audio spectrum is live. J-Digi starts its own
audio when the app starts; no hub link is needed.

### 4.4 J-Bridge

Receives WSJT-X on UDP port **2237**. The **WSJT-X link** panel shows whether WSJT-X is talking; decodes arrive in a
table (**UTC**, **dB**, **DT** and **message**) with the 15-second period shown. Double-click a station in WSJT-X to set
the DX call. With *Auto-log FT8/FT4 QSOs* on (J-Hub ▸ Data), completed contacts go to your log.

In WSJT-X, open *File ▸ Settings ▸ Reporting* and tick **Accept UDP requests**.

### 4.5 J-Sat

Shows the satellites you chose in J-Hub ▸ Satellites: the selected satellite's position, **Next passes**, and a
**Radio · Doppler** panel. A satellite with no orbital data is flagged with a warning; satellites with no radio data
still get position and passes. A link at the top takes you back to the picker. With *Auto-track satellites* on, the
rotor follows the pass; Doppler-corrected downlink frequencies can be sent to the rig.

### 4.6 J-Vault

An inventory of your equipment, plus an **Estate · first-call contacts** drawer: the people to call first, their
relationship to you and which items they should be offered.
**+ Add item** opens a form; select an item to see or edit it; **Delete** asks you to click twice. **Export CSV** writes a
spreadsheet file. The data is a private database, kept apart from your log.

### 4.7 Morse Trainer and QSO simulator

Inside **J-Learn**, the **Morse Trainer** entry at the top of the chapter list opens a set of CW practice screens:
letters (Koch method, with your level), groups, a **QSO simulator** that sends a whole contact for you to copy, and
**sending practice** with a keyer. Settings include the Koch order. A Raspberry Pi or Arduino keyer can be used for
sending; the build guides are in [docs/morse-hardware](docs/morse-hardware/README.md).

### 4.8 J-Learn

The reference library: propagation, antennas, RF safety, repeaters and band plans, satellites, formulas, feedline,
troubleshooting and more — about 30 chapters and 300 sections. Open a chapter in the left drawer and choose a section.

The text is kept **apart from the program**, in `~/.j-learn/content/`, so it can be updated and edited without a new
release.

- It is copied there from the app automatically the first time you open J-Learn.
- **J-Hub ▸ Data ▸ J-Learn content** refreshes it. Type or paste a **source** — a `.zip` address (the default is the
  project's GitHub archive), a local `.zip`, or a folder — and press **Update files**. The new files are checked
  first, and the old ones are kept as `content.backup`.
- **Restore bundled copy** puts back the chapters that shipped with your version, and **Open folder** shows the files.

You may edit the markdown files directly; **Update files** and **Restore** will replace your edits (the backup keeps
them).

---

## 5. Macros and transmitting

The F1–F8 keys are set in **J-Hub ▸ Data ▸ Station macros**. Each key has a label, a **CW / RTTY text** and an **SSB
voice file**. Macros are built from your station data and can use **tags** — `{MYCALL}`, `{CALL}`, `{NAME}` and about
forty more, listed under *Macro tags* on that page. Fields you have not filled in simply stay empty. **Rebuild from
station data** regenerates the defaults.

**What a key sends** is chosen next to the macro buttons in J-Log, J-Log-Quick and contest mode, with the
**Macros send as** tick boxes: **CW / data** or **Voice**. With neither ticked, the key follows the mode field (CW, RTTY
and digital modes send the text; SSB and the other phone modes play the voice file).

**How it transmits**

- **CW** goes to the rig's own keyer through Hamlib.
- **Voice** keys PTT, plays the audio file through the output device set in *Transmit*, then releases PTT.
- **RTTY and PSK31** go to the J-Digi modem.
- A **PTT delay** (one number, in J-Hub ▸ Data ▸ Transmit) is applied both between keying and the start of audio, and
  between the end of audio and un-keying.

The **ID** button in every module's top bar counts down 10 minutes. It turns amber in the last minute and shows
**ID NOW** when time is up; click it after you identify to start again.

While transmitting, a flashing red **TRANSMITTING** light shows in every module's top bar. **Esc**, or clicking the
light, stops sending and drops PTT. Nothing is sent unless the rig is connected, and PTT is also released on an error,
when a window closes, and by a watchdog.

> **Try it on a dummy load first, with your hand near Esc.** Macro keying has been tested in software but not yet on a
> range of real radios.

---

## 6. Running loose, and a second-machine display

**Loose windows.** Start a module on its own with its shortcut, or from a terminal:

```bash
./ars-fx/ars-fx.sh --module map      # log | logc | map | sat | digi | bridge | vault | learn
./ars-fx/ars-fx.sh --hub             # the background hub only
```

The hub owns the Hamlib daemons and the live feeds (DX cluster, RBN, rig, rotor) and publishes them on
`ws://127.0.0.1:8090`. Windows show the last known values and reconnect on their own if the hub restarts.

**A second machine.** J-Map or J-Sat can run on a laptop or Raspberry Pi and follow your station over the LAN.
Switch on *Share with solo modules on the LAN* in **J-Hub ▸ Data ▸ Remote sharing**, open port **8090** to your LAN, and
start the display with a launch file — the steps and a sample are in
[INSTALL.md § Second-machine display](INSTALL.md#second-machine-display).

---

## 7. Troubleshooting

**A loose window shows no spots or rig data.** The background hub isn't running. Open **ARS Suite Hub**, run
`ars-fx.sh --hub`, or open any module; the first one starts it. Check it is listening with `ss -ltn | grep 8090`. For a
display on another machine, allow inbound TCP 8090 on the station's firewall, LAN only.

**The DX cluster won't connect.** Look at the status: **Connecting** means the server hasn't answered after login
yet. Press **Stop** to cancel, then check the server and your login callsign in J-Hub ▸ Data ▸ DX cluster. The
**Console** pop-up shows the raw conversation. After the *connection tries* limit (default 5) it gives up; **Connect**
starts a fresh count.

**Hamlib commands fail.** Run `rigctl --version` in a terminal. If it is missing, install Hamlib (see
[§2](#optional-software)). If it runs but the suite can't talk to the rig, check the port is readable
(`ls -l /dev/ttyUSB*`), that your user is in the `dialout` group on Linux, and that the model number and baud match the
radio. A device that another program already holds open can't be opened twice.

**WSJT-X doesn't show as connected in J-Bridge.**
1. In WSJT-X's Reporting settings, tick **Accept UDP requests** and use port 2237.
2. Another program (JTAlert, GridTracker, JS8Call) may already own port 2237. Unicast UDP reaches only one
   listener; close the other tool, or switch WSJT-X to multicast (`239.255.0.0`) and have each tool join it.
3. If J-Bridge started after WSJT-X, restart the link from the **WSJT-X link** panel.

**J-Learn shows "This section's text isn't available".** Open J-Hub ▸ Data ▸ J-Learn content and press **Update
files** or **Restore bundled copy**.

**J-Learn shows old text after I edited a file.** If a file is missing on disk J-Learn falls back to the copy bundled
in the app. Restore the file, or press **Restore bundled copy**.

**A module is missing from the dock.** Turn it on in J-Hub ▸ Modules.

**A QSO won't save.** A callsign is required. Band and mode may be left empty, but anything typed must be a known
value.

**Log import skipped some QSOs.** *Import ADIF…* skips a QSO whose call, minute, band and mode are already in the
log, and reports records it could not read (no call or no date).

**Switching to contest mode shows no database.** Each contest keeps its own database in `~/.j-log/contest-logs/`;
choose **start a new one**, or **Restore** an archived log in J-Hub ▸ Data.

**The window is blank or garbled after an update.** Rebuild or re-download the jar for your platform — a jar built for
another OS gives JavaFX errors. See [INSTALL.md § Common problems](INSTALL.md#common-problems).

**Reporting a bug.** Open an issue at <https://github.com/skip17331/WM3J-ARS-Suite/issues> with what you did, what you
expected and what happened. If you started the app from a terminal, include any red error text it printed.

---

## 8. Appendix: where your data lives

| Path | Contents |
|------|----------|
| `~/.j-hub/config.json` | All settings, including station, rig, cluster, macros and upload logins |
| `~/.j-log/j-log.db` | Normal log |
| `~/.j-log/contest-logs/` | Contest databases (one per contest and year) |
| `~/.j-log/archive/<Contest>_<Year>/` | Archived contest databases |
| `~/.j-log/backups/` | ADIF and CSV exports |
| `~/.j-vault/inventory.db` | J-Vault |
| `~/.j-learn/content/` | J-Learn chapters; `content.backup` is the copy before the last update |
| `~/.j-sat/tles.txt` | Downloaded orbital elements |
| `~/.j-map/` | Cached map data |

Your login details for upload services and callsign lookups are stored in `config.json`; keep that file private.
To move to a new computer, copy `~/.j-hub/` and `~/.j-log/`.
