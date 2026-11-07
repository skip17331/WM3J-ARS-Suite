# WM3J ARS Suite — User Guide

How to set up and operate the suite. To install it, see **[INSTALL.md](INSTALL.md)**. The project's purpose and
licence are in [README.md](README.md).

> **Wiring up real radio gear?** Read [docs/HARDWARE_GUIDE.md](docs/HARDWARE_GUIDE.md) first. It covers cables,
> USB-to-serial adapters, USB hubs and stable port names. This guide assumes your radio is already plugged in and shows
> up as a serial port on the computer.

## Contents

1. [Overview](#1-overview)
2. [First-run setup](#2-first-run-setup)
3. [J-Hub — the dashboard and Setup](#3-j-hub--the-dashboard-and-setup)
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
| **Info** | Upcoming contests, DXpeditions and hamfests, and ham-radio news headlines |
| **J-Hub** | The dashboard, where normal logging happens (the QSO form, the DX cluster and the **Log tools** column), and **Setup**, the one screen for every setting |
| **J-Log** | Contest mode: a page of its own for logging a contest. Normal logging is on the J-Hub dashboard; J-Log no longer has a dock entry |
| **J-Map** | DX map with grayline, propagation, spots and beam headings |
| **J-Digi** | Built-in digital-mode decoder: CW, RTTY, PSK31, Olivia, MFSK16, DominoEX, AX.25 |
| **J-Bridge** | Brings WSJT-X decodes (FT8, FT4 and others) into the suite |
| **J-Sat** | Satellite passes, rig and rotor tracking, Doppler |
| **J-Vault** | Shack inventory and first-call contacts |
| **J-Learn** | Reference library, with the Morse Trainer and a QSO simulator |

The **Awards** button at the foot of the dock opens the awards screen from anywhere, and the **gear** below it opens
**Setup**. You can turn modules off in **Setup ▸ Modules**; a disabled module disappears from the dock.

**J-Digi** and **J-Bridge** are still in **beta**: their pages carry a notice and their dock tooltips say "(beta)". Check
what they do before you rely on them. QSL label printing is beta too.

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

1. **Open Setup ▸ Station** (the gear at the foot of the dock). Enter your **callsign**, **operator name**, **grid square**, latitude and longitude, and
   elevation. Pick your **IARU region**. These drive band-plan captions and every
   bearing, grayline and satellite pass in the suite — set your location before using J-Map or J-Sat.
2. **Choose units.** *Station ▸ Units* switches the whole app between **Metric** (km, °C, km/h, hPa, m) and
   **Standard (US)** (mi, °F, mph, inHg, ft).
3. **Connect your radio** under **Setup ▸ Rig control** (see [§3.2](#32-hardware-rig-rotor-amplifier-antenna-switch)).
4. **Pick a DX cluster** under **Setup ▸ DX cluster & RBN** and press **Connect** on the cluster panel
   (see [§3.3](#33-data-logbook-and-system-pages)).
5. **Open the J-Hub dashboard** and log a contact in the **New QSO** form. Your normal log is created automatically
   at `~/.j-log/j-log.db`.

Nothing else is needed to start. Settings are saved as you change them to `~/.j-hub/config.json` and take effect
straight away — in loose mode the change reaches every open window.

### Optional software

| | Linux (Debian/Ubuntu) | Fedora | Arch | macOS | Windows |
|---|---|---|---|---|---|
| **Hamlib** | `sudo apt install libhamlib-utils` | `sudo dnf install hamlib` | `sudo pacman -S hamlib` | `brew install hamlib` | <https://hamlib.github.io/> |
| **WSJT-X** | `sudo apt install wsjtx` | download from <https://wsjt.sourceforge.io/wsjtx.html> | AUR `wsjtx` | `brew install --cask wsjtx` | download from the same page |

Hamlib is needed only to control a rig, rotor or amplifier. WSJT-X is needed only for J-Bridge.

---

## 3. J-Hub — the dashboard and Setup

J-Hub is the first screen. It has two parts: the **dashboard**, where you operate, and **Setup**, where every setting
lives. The gear at the foot of the dock opens Setup; the J-Hub entry at the top of the dock returns to the dashboard.

**Setup** is a single screen. A second menu down its left edge (it slides open when the mouse is over it; **Pin** keeps
it open) groups the pages:

| Group | Pages |
|-------|-------|
| **Station** | Station |
| **Radio** | Rig control, Rotor control, Amplifier, Antenna switch, Transmit & macros |
| **Data & spotting** | DX cluster & RBN, WSJT-X / digital, Callsign lookup, Satellites |
| **Logbook** | Log database & backup, Online upload, Export & Cabrillo, Label printers |
| **System** | Modules, Remote sharing, J-Learn content |

Choosing a page swaps the right-hand side without leaving the screen.

### 3.1 Dashboard and Station

**Dashboard** is where normal logging happens. It holds:

- the full **New QSO** form with its F1–F8 macro bar: callsign, band, mode, frequency, RST, name, QTH, state, county, grid, power,
  comment, IOTA / SOTA / POTA references and the QSL / LoTW / eQSL boxes (see [§4.1](#41-j-log));
- a live **DX cluster** list with band, mode and "needed only" filters, and a **Saved QSOs** strip;
- the **Log tools** column on the left, which folds to a strip and slides open on hover (**Pin** keeps it open): *Log mode*, *RTTY/CW TX & RX*, *Heard By*, *Awards*, *Skimmer*, *Translator* and *Database tools*;
- a right-hand rail with the **rig**, **rotor**, **amplifier**, propagation, solar and space-weather, and band conditions.

**Station** holds your identity (callsign, operator name), location (grid, latitude, longitude, elevation), ITU and CQ
zones, IARU region, default TX power, the **Units** switch, and **Updates**: the daily check for a new version and the
weekly refresh of the country files (see [§3.3](#33-data-logbook-and-system-pages)).

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

### 3.3 Data, logbook and system pages

Everything that carries data in or out is on these Setup pages.

| Page | What it does |
|------|--------------|
| **Log database & backup** | Which database and table the normal log uses, with its QSO count and size (**Open folder**); the **contest databases** (each with its contest, start date and QSO count: **Archive** moves one into storage, **Restore** brings it back; archived logs sit in `~/.j-log/archive/<Contest>_<Year>/`); the **Call history** (see [§4.1](#41-j-log)); and **Backup, export & import**: **Export ADIF** or **Export CSV** to a timestamped file, and **Import ADIF…** into the normal log (QSOs already in it are skipped). |
| **Online upload** | **LoTW** (signed with TrustedQSL), **eQSL.cc**, **QRZ Logbook** and **Club Log**, each with its login, an **Upload** button and an auto-upload tick. **Get confirmations** downloads your LoTW and eQSL confirmations and ticks the matching QSOs. |
| **Export & Cabrillo** | The Cabrillo / CSV character set; the **N1MM+ UDP broadcast** (off by default), which sends your contest log to GridTracker, Log4OM and the like; the **contest network** (host or join a multi-operator contest over the LAN); and the categories printed in your Cabrillo file (operator, power, assisted, band, mode, station, club) plus Sweepstakes precedence, check and section. |
| **Label printers (beta)** | Printer model (Brother QL, DYMO LabelWriter, Zebra, Rollo or any system printer), label stock, printer or network address, copies and a **Test print** button. Used by **Print QSL label** on the New QSO form. |
| **DX cluster & RBN** | A **pick list of clusters** (or type your own `host:port`; one runs at a time), login callsign, auto-connect, band and mode filters, and **connection tries** (the retry limit; 0 keeps trying); the Reverse Beacon Network has its own auto-connect and retry limit. |
| **WSJT-X / digital** | UDP port (2237), auto-log FT8/FT4 contacts, PSK Reporter upload, and the local CW skimmer. |
| **Callsign lookup** | Callook.info (free, US), HamQTH, QRZ.com XML, QRZCQ, HamCall, and a local CSV callbook file. Tried top to bottom; the first answer fills name, QTH, grid, state and county. |
| **Satellites** | See [§3.4](#34-satellites). |
| **Transmit & macros** | CW speed, the keyer, the **PTT delay**, the voice output device, and the F1–F8 station macros — see [§5](#5-macros-and-transmitting). |
| **Modules** | See [§3.5](#35-modules). |
| **Remote sharing** | Share live data with solo J-Map / J-Sat windows on other machines over the LAN (port 8090). |
| **J-Learn content** | Where J-Learn's chapters come from and how to refresh them — see [§4.8](#48-j-learn). |

The **Station** page's **Updates** section also has **Refresh country files**. Once a week the suite downloads the
current Contest Country Files (`cty.dat`, from country-files.com; nothing about you is sent) so DXCC prefixes and zones
stay current between releases. Switch it off there if you would rather not.

### 3.4 Satellites

**Setup ▸ Satellites** is where you choose what J-Sat tracks.

- **Orbital data.** *Refresh now* downloads fresh elements from Celestrak and AMSAT; *Refresh automatically* does it
  daily. Without a download the suite uses a bundled snapshot.
- **Selection.** Search and filter everything the data covers, tick the satellites you want, and use *Select shown*,
  *Clear shown* or *Reset to defaults*. "Only satellites with orbital data" and "Has a pass in the next 24 h above N°"
  narrow the list. A pick that has no data is looked up on Celestrak by its NORAD number.

### 3.5 Modules

Switch each module on or off under **Setup ▸ Modules**. J-Hub itself is always on.

---

## 4. The modules

### 4.1 J-Log

Logging is in two places: **normal logging** on the J-Hub dashboard, and **contest mode**, a page of its own. A bar
across the top of each always tells you which database you are writing to, with **Switch database** and
**Database tools ›** (which opens Setup ▸ Log database & backup).

**Normal logging (the J-Hub dashboard)**

- **New QSO** — callsign, band, mode, frequency, RST sent and received, name, QTH, state, county, grid, power and
  comment, then **Log QSO ⏎**. Typing a call looks it up (see *Callsign lookup* above) and shows its DXCC entity,
  beam heading and your previous contacts with it. **Print QSL label** prints a label for each new QSO when you save it; pick the printer in Setup ▸ Label printers. *(Beta.)*
- **IOTA, SOTA and POTA** — optional fields for the other station's island, summit and park (hunter credit), with lookups, and **Confirmed** boxes for a paper QSL, LoTW or eQSL. Import an ADIF file downloaded from LoTW or eQSL, or use **Get confirmations** (Setup ▸ Online upload), and the QSOs already in your log are marked confirmed.
- **Recent log** — your latest QSOs; select one to edit or delete it. **Browse full log ⤢** opens every QSO in a sortable, searchable table (sort by any column heading, search by call, name, QTH, grid, state, county or notes, filter by band and mode; double-click a row to edit it).
- **DX Cluster** — live spots with a **Console** button that opens a raw-feed pop-up with a command line (history, and
  shortcuts such as `SH/DX 20` and `SET/FT8`). The status is always **Disconnected**, **Connecting** or **Connected**,
  and **Stop** cancels a connection that is not coming up. A spot that would be a **new DXCC entity, band or mode** for you is tagged.
- **Log tools column** — *Log mode* (the way into contest mode), *RTTY / CW · TX & RX* (a text pane for the modem), *Heard By* and *Skimmer* (who has heard you on the Reverse Beacon Network, and CW skimmer decodes), *Awards*, *Translator* and *Database tools*.

**Awards.** The Awards drawer, or the **Awards** button on the dock, shows DXCC, WAS, WAC, grid squares, IOTA, POTA, SOTA and the CQ / ITU zones, each as worked / confirmed of its target. **Open awards ⤢** opens the full screen, with tabs for a *Summary*, a *DXCC* band-by-band matrix, *WAS*, *WAC*, *Grids*, *IOTA*, *POTA*, *SOTA*, *Zones*, endorsement *Levels* and **My awards**. A QSO's own CQ / ITU zone counts when it has one; otherwise the zone comes from the callsign's country and, in countries that span several CQ zones, from the worked station's grid square. **My awards** tracks award sheets of your own, such as a special-event certificate: use **New award…**, or **From template** for the 13 Colonies. A call whose DXCC entity cannot be worked out is left out of the DXCC count.

**Contest mode** — in the *Log mode* drawer, press **⇄ Contest mode…**.

1. **Choose a contest** from the list of about 87 supported contests (there is a search box). J-Log offers the last one you used first.
2. **Choose a database.** Either **continue an existing one** (each is shown with its contest and start date), or
   **start a new one**. A new log gets a suggested name such as `NovSwpSSB2026.db`, which you can change. Asked each time you enter contest mode.
3. **Say what you send.** Anything of your own exchange that Setup does not know (for example your Sweepstakes section) is asked once per contest.
4. **Log on one screen.** Down the left: the entry bar built from the contest's exchange, with the **SEND** line (exactly what to say or key, built from your Setup details; **Edit** changes it) and a live score line: QSOs, mults, points, score, 10- and 60-minute rates, a **mults left** tile, and a **contest clock** once you have set the times. Below the entry are the last QSOs, with **Edit** and **Erase** on each (and **All QSOs →** for the lot). On the right: the DX cluster over the contest's own tabs (multiplier trackers and maps, dupe and worked-before panes, statistics).
5. **LOG TOOLS** is a column beside the dock (it folds to a strip; **Pin** keeps it open). Its **Contest** drawer holds **Export Cabrillo ⤓** (writes the file for submission, using the categories set in Setup ▸ Export & Cabrillo), **Contest times…**, **Layout…** (a reminder note, the order of the tabs and of the exchange fields), **Change contest** and **Leave contest**. Its **Translator** drawer follows the call you are logging.

What the contest page does for you:

- **Validation.** A section, precedence, zone or callsign that can never be valid is refused. Where the list is not definitive (a state or county in a QSO party that is not recognised) it warns and asks for a second **Enter**.
- **Dupes** ask permission before they are logged.
- **Exchange memory and call history.** A station you have worked before in this contest has its exchange filled in. For a station you have not, the **call history** fills it from what that station usually sends (state, section, zone, grid, name, Sweepstakes precedence and check); anything filled in is marked, and you check it before you log. The history learns from your own contest QSOs and can be rebuilt from your logs (**Setup ▸ Log database & backup ▸ Call history ▸ Build from my logs**) or seeded from a call history CSV file (**Import file…**; N1MM+ call history files work). Your own QSOs always win over imported values.
- **NEW MULT tags** on cluster spots. Where the multiplier comes from the callsign (CQ WW, All Asian, Oceania, WAE and most QSO parties) a spot that would add a multiplier is tagged **NEW MULT**. Where the multiplier is in the exchange (Sweepstakes sections, ARRL DX states, Field Day...) the tag is **NEW MULT?**: a guess from the call history, shown only when the history has a value the contest accepts.
- **Super Check Partial** and the **band map** are on the page as well.
- **Network mode.** Under Setup ▸ Export & Cabrillo, one station can host and others join; every station keeps a full copy of the contest log, and a strip on the contest page shows who is joined and what each is on.

Contest databases live in `~/.j-log/contest-logs/`. When you finish with one, archive it from Setup ▸ Log database & backup.

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
- **NCDXF beacons** — show the beacon cycle for the band you pick. The map shows the frequency to tune ("QSY 14.100 MHz CW") beside the beacon on the air, and the drawer shows it live with who is on now and who is next. Switch on **Auto QSY the rig** and picking a band tunes a connected rig to that band's beacon frequency in CW (never while it transmits).
- **Projection** — **Azimuthal** (centred on you, distances read straight off the rings), **Rectangular** (flat, with region
  buttons and zoom) or **Globe** (a 3D Earth: drag to turn it, wheel to zoom, double-click to centre on your station). The
  globe shows the same spots, beam, range rings and gray line, with the sun out in space on its daily track (the line of
  latitude it rides today; turn the globe or zoom out with the wheel to bring it into view), and the same beacons, who's-hearing-me, spot paths and tracks, and a
  click on a spot opens the same details card. Globe is hidden on a computer without 3D support.
- **Tracks (ham to ham)** are drawn in red, with the distance and heading at the middle. On the globe, tracks and spot paths arch up off the surface, higher the longer the path.
- **Spot paths · DX cluster** — draws a line from each spotted station to the station that spotted it, in the band's
  colour, following the mode and band filters. A line fades over ten minutes and then goes; **Clear** starts again from
  the next spot. Off by default. Click a spot and every path to it, one for each station that spotted it, turns red and heavier on top of the rest, even with the switch off; click empty map (or **Clear**) to let go of it.
- **Beam heading** shows where the rotor is pointing, on both map views. With no rotor answering there is no beam and no
  heading.

Clicking a spot on the map or in the list tunes the rig and pops up the station's details. **Log this** opens the
**New QSO** form, filled in from the spot. Press **Enter** to log it to your **normal log**, or **Esc** or **Clear** to
drop it. Drag the form by its top row. **Pin** moves it into a drawer in the right column, where it stays and empties after each QSO; while it is docked, a
spot click fills it directly and the pop-up has no **Log this** button. **+
QSO** at the top left of the map opens a blank card. Contest logging is in J-Log contest mode.

Set your latitude and longitude first. Distances follow your Units setting.

### 4.3 J-Digi

*Beta.* The built-in modem decodes from your sound card. Pick a mode tab — **CW, RTTY, PSK31, Olivia, MFSK16, DominoEX** or
**AX.25** — and watch the **waterfall** and the **decoder** text. The audio spectrum is live. J-Digi starts its own
audio when the app starts; no hub link is needed.

### 4.4 J-Bridge

*Beta.* Receives WSJT-X on UDP port **2237**. The **WSJT-X link** panel shows whether WSJT-X is talking; decodes arrive in a
table (**UTC**, **dB**, **DT** and **message**) with the 15-second period shown. Double-click a station in WSJT-X to set
the DX call. With *Auto-log FT8/FT4 QSOs* on (Setup ▸ WSJT-X / digital), completed contacts go to your log.

In WSJT-X, open *File ▸ Settings ▸ Reporting* and tick **Accept UDP requests**.

### 4.5 J-Sat

Shows the satellites you chose in Setup ▸ Satellites: the selected satellite's position, **Next passes**, and a
**Radio · Doppler** panel. A satellite with no orbital data is flagged with a warning; satellites with no radio data
still get position and passes. The switch at the top right chooses the view: **Polar** (the sky above you),
**Blue marble** (a flat world map with ground tracks) or **Globe** (a 3D Earth: drag to turn it, wheel to zoom, click a
satellite to select it, double-click to centre on your station; the selected satellite shows its footprint and, in a pass,
a line to you). Globe is hidden on a computer without 3D support. On the world map and the globe satellites are drawn as little satellites (the polar plot keeps dots); each orbit line glows ahead of the satellite and is dashed behind it, and the selected one has arrowheads. Every satellite's footprint is tinted with its colour, and stronger when your station is inside it; a line runs from your station to each satellite above your horizon. Click a satellite to select it and dim the others; click anywhere off a satellite to bring them all back. A link at the top takes you back to the picker. With *Auto-track satellites* on, the
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
- **Setup ▸ J-Learn content** refreshes it. Type or paste a **source** — a `.zip` address (the default is the
  project's GitHub archive), a local `.zip`, or a folder — and press **Update files**. The new files are checked
  first, and the old ones are kept as `content.backup`.
- **Restore bundled copy** puts back the chapters that shipped with your version, and **Open folder** shows the files.

You may edit the markdown files directly; **Update files** and **Restore** will replace your edits (the backup keeps
them).

---

### 4.9 Info

The **Info** page (first in the dock) is a look-ahead and a news board, in four tiles: **Contests**, **DXpeditions**,
**Hamfests** and **News**.

- The calendar tiles list what is coming up in the range you choose, with a search box; click an entry to open its page.
  DXpeditions come from NG3K's announced operations by default. **Sources…** sets your own contest or hamfest calendar (an
  `.ics` link) and switches the DXpedition list off or on; the suite ships no built-in contest feed, so the Contests tile is
  empty until you add a link.
- **News** shows headlines from RSS / Atom feeds, filtered by source. **Feeds…** picks which feeds are on and lets you add
  your own. Click a headline to read it on the source site, in your browser.
- **⟳ Refresh** fetches again (feeds are kept six hours, and a manual refresh never goes to the network more than once
  every fifteen minutes); if a fetch fails the saved copy is shown.
- **Request a change** starts an email to the project contact, to ask for a feed or calendar to be added.
- The row of links along the bottom opens the project's web pages, and the **licence**, **Code of Conduct** and **legal
  notes**, which open from copies kept on your computer.

---

## 5. Macros and transmitting

The F1–F8 keys are set in **Setup ▸ Transmit & macros**. Each key has a label, a **CW / RTTY text** and an **SSB
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
- A **PTT delay** (one number, in Setup ▸ Transmit & macros) is applied both between keying and the start of audio, and
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
Switch on *Share with solo modules on the LAN* in **Setup ▸ Remote sharing**, open port **8090** to your LAN, and
start the display with a launch file — the steps and a sample are in
[INSTALL.md § Second-machine display](INSTALL.md#second-machine-display).

---

## 7. Troubleshooting

**A loose window shows no spots or rig data.** The background hub isn't running. Open **ARS Suite Hub**, run
`ars-fx.sh --hub`, or open any module; the first one starts it. Check it is listening with `ss -ltn | grep 8090`. For a
display on another machine, allow inbound TCP 8090 on the station's firewall, LAN only.

**The DX cluster won't connect.** Look at the status: **Connecting** means the server hasn't answered after login
yet. Press **Stop** to cancel, then check the server and your login callsign in Setup ▸ DX cluster & RBN. The
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

**J-Learn shows "This section's text isn't available".** Open Setup ▸ J-Learn content and press **Update
files** or **Restore bundled copy**.

**J-Learn shows old text after I edited a file.** If a file is missing on disk J-Learn falls back to the copy bundled
in the app. Restore the file, or press **Restore bundled copy**.

**A module is missing from the dock.** Turn it on in Setup ▸ Modules.

**A QSO won't save.** A callsign is required. Band and mode may be left empty, but anything typed must be a known
value.

**Log import skipped some QSOs.** *Import ADIF…* skips a QSO whose call, minute, band and mode are already in the
log, and reports records it could not read (no call or no date).

**Switching to contest mode shows no database.** Each contest keeps its own database in `~/.j-log/contest-logs/`;
choose **start a new one**, or **Restore** an archived log in Setup ▸ Log database & backup.

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
| `~/.j-log/callhistory.db` | Call history: each station's usual state, section, zone, grid and name |
| `~/.j-log/contest-logs/` | Contest databases (one per contest and year) |
| `~/.j-log/archive/<Contest>_<Year>/` | Archived contest databases |
| `~/.j-log/backups/` | ADIF and CSV exports |
| `~/.j-hub/cty/cty.dat` | The refreshed country files (the suite also carries a copy) |
| `~/.j-hub/info/` | Saved copies of the Info page's calendars and news feeds |
| `~/.j-hub/updates/` | Update-check log and downloaded updates |
| `~/.j-hub/legal/` | Local copies of the licence, Code of Conduct and legal notes |
| `~/.j-vault/inventory.db` | J-Vault |
| `~/.j-learn/content/` | J-Learn chapters; `content.backup` is the copy before the last update |
| `~/.j-sat/tles.txt` | Downloaded orbital elements |
| `~/.j-map/` | Cached map data |

Your login details for upload services and callsign lookups are stored in `config.json`; keep that file private.
To move to a new computer, copy `~/.j-hub/` and `~/.j-log/`.
