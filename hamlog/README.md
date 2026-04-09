# HamLog — Amateur Radio Logging Application

A full-featured Java + JavaFX amateur radio logging application for Linux,
supporting both normal everyday logging and contest logging via a modular,
plugin-driven architecture.

---

## Technology Stack

| Layer          | Technology                              |
|----------------|-----------------------------------------|
| UI             | Java 21 + JavaFX 21 (FXML + CSS)        |
| Database       | SQLite via `org.xerial:sqlite-jdbc`     |
| JSON           | Jackson 2.17                            |
| Serial (CI-V)  | JSSC 2.9.4                              |
| Logging        | SLF4J + Logback                         |
| Build          | Maven 3.x                               |

---

## Project Structure

```
hamlog/
├── pom.xml
├── build.sh
├── run.sh
└── src/main/
    ├── java/module-info.java
    ├── java/com/hamlog/
    │   ├── app/
    │   │   ├── HamLogApp.java          ← JavaFX Application entry point
    │   │   ├── SplashScreen.java       ← Splash + mode chooser
    │   │   └── ContestChooser.java     ← Contest plugin selector
    │   ├── controller/
    │   │   ├── NormalLogController.java  ← Normal log window
    │   │   ├── ContestLogController.java ← Contest log window
    │   │   ├── DxSpotController.java     ← DX cluster panel (embedded)
    │   │   └── SetupController.java      ← Setup window
    │   ├── model/
    │   │   ├── QsoRecord.java          ← QSO data model
    │   │   ├── Macro.java              ← Macro + MacroAction model
    │   │   ├── DxSpot.java             ← DX spot model
    │   │   └── StationInfo.java        ← Station configuration model
    │   ├── db/
    │   │   ├── DatabaseManager.java    ← SQLite connection + schema init
    │   │   ├── QsoDao.java             ← Normal log CRUD
    │   │   ├── ContestQsoDao.java      ← Contest log CRUD + dupe checking
    │   │   └── MacroDao.java           ← Macro persistence
    │   ├── civ/
    │   │   ├── CivEngine.java          ← Full bidirectional CI-V engine
    │   │   └── CivConfig.java          ← CI-V connection config
    │   ├── cluster/
    │   │   ├── DxClusterEngine.java    ← DX cluster telnet engine
    │   │   └── DxNetwork.java          ← Cluster network config model
    │   ├── macro/
    │   │   └── MacroEngine.java        ← Macro executor
    │   ├── plugin/
    │   │   ├── ContestPlugin.java      ← Plugin data model (maps JSON)
    │   │   └── PluginLoader.java       ← Plugin discovery + import
    │   ├── export/
    │   │   ├── AdifExporter.java       ← ADIF + CSV export
    │   │   └── CabrilloExporter.java   ← Cabrillo export
    │   ├── util/
    │   │   ├── AppConfig.java          ← Application preferences
    │   │   ├── LoggingConfigurator.java ← Logback dynamic config
    │   │   └── QrzLookup.java          ← QRZ.com XML API lookup
    │   └── i18n/
    │       └── I18n.java               ← Internationalisation helper
    └── resources/com/hamlog/
        ├── fxml/
        │   ├── NormalLog.fxml          ← Normal log layout
        │   ├── ContestLog.fxml         ← Contest log layout
        │   ├── DxSpot.fxml             ← DX spotting panel
        │   └── Setup.fxml              ← Setup window
        ├── css/
        │   ├── base.css                ← Shared structural styles
        │   ├── light.css               ← Light theme colours
        │   └── dark.css                ← Dark theme colours
        ├── i18n/
        │   ├── messages.properties     ← Default (English)
        │   ├── messages_en.properties  ← English
        │   └── messages_de.properties  ← German
        └── plugins/
            ├── arrl_sweepstakes_cw.json
            ├── arrl_sweepstakes_ssb.json
            ├── cq_ww_cw.json
            └── cq_ww_ssb.json
```

---

## Databases

Three SQLite databases are created at `~/.hamlog/` on first run:

### `hamlog.db` — Normal Log
```sql
CREATE TABLE qso (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    callsign      TEXT NOT NULL,
    datetime_utc  TEXT NOT NULL,
    band          TEXT, mode TEXT, frequency TEXT,
    power_watts   INTEGER,
    rst_sent TEXT, rst_received TEXT,
    country TEXT, operator_name TEXT,
    state TEXT, county TEXT, notes TEXT,
    qsl_sent INTEGER DEFAULT 0,
    qsl_received INTEGER DEFAULT 0,
    created_at TEXT
);
```

### `contest.db` — Contest Log
```sql
CREATE TABLE contest_qso (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    contest_id      TEXT NOT NULL,
    callsign        TEXT NOT NULL,
    datetime_utc    TEXT NOT NULL,
    band TEXT, mode TEXT, frequency TEXT,
    operator        TEXT,
    serial_sent TEXT, serial_received TEXT,
    exchange TEXT,
    field1 TEXT, field2 TEXT, field3 TEXT,
    field4 TEXT, field5 TEXT,
    points INTEGER DEFAULT 0,
    is_dupe INTEGER DEFAULT 0,
    rst_sent TEXT, rst_received TEXT, notes TEXT
);
```

### `config.db` — Application Config
```sql
CREATE TABLE config    (key TEXT PRIMARY KEY, value TEXT);
CREATE TABLE macro     (id INTEGER PRIMARY KEY, name TEXT, fkey INTEGER, json TEXT);
CREATE TABLE dx_network(id INTEGER PRIMARY KEY, name TEXT, host TEXT, port INTEGER, callsign TEXT);
```

---

## Contest Plugin Schema

Each plugin is a JSON file with this structure:

```json
{
  "contestId":   "ARRL_SS_CW",
  "contestName": "ARRL November Sweepstakes (CW)",
  "version":     "1.0.0",
  "exchangeFormat": "Human-readable description",

  "entryFields": [
    { "id": "callsign", "label": "Callsign", "type": "text|number|combo",
      "width": 130, "required": true, "autoIncrement": false,
      "options": ["A","B"] }
  ],

  "scoringRules": {
    "pointsPerQso": 2,
    "modePoints": { "CW": 3 },
    "multiplierType": "sections|dxcc|states|custom",
    "scoreFormula": "qsoPoints * multipliers",
    "allowDupes": false
  },

  "multiplierModel": {
    "field": "section",
    "perBand": false,
    "validValues": [ "CT", "EMA", "..." ]
  },

  "row2Panes": [
    { "paneIndex": 1, "paneType": "dupe_checker|section_tracker|statistics|custom",
      "title": "Pane Title", "config": {} }
  ],

  "statistics": ["qso_count","total_score","multipliers","qso_per_hour"],

  "cabrilloMapping": {
    "fieldId": "sent_|rcvd_columnName"
  },

  "sections": ["CT","EMA","ME","..."]
}
```

**Plugin discovery order:**
1. Bundled plugins in the JAR (`resources/com/hamlog/plugins/*.json`)
2. User-installed plugins in `~/.hamlog/plugins/*.json`
3. Manual import via **Contest → Import Plugin...**

---

## CI-V Engine

The `CivEngine` (singleton) communicates over a serial port using Icom CI-V protocol.

### Capabilities

| Feature            | CI-V Command  |
|--------------------|---------------|
| Read frequency     | 0x03          |
| Write frequency    | 0x05          |
| Read mode          | 0x04          |
| Write mode         | 0x06          |
| PTT on/off         | 0x1C 0x00     |
| CW text keying     | 0x17          |
| VFO A/B select     | 0x07          |
| Split on/off       | 0x0F          |

### Auto-polling
Polls every 500 ms (configurable) for frequency and mode.

### Event-driven
Processes unsolicited frames from the radio (e.g. VFO knob turns).

### Usage
```java
CivEngine.getInstance().connect("/dev/ttyUSB0", 19200, (byte) 0x94);
CivEngine.getInstance().setFrequencyListener(hz -> System.out.println("Freq: " + hz));
CivEngine.getInstance().setPtt(true);
CivEngine.getInstance().sendCw("CQ CQ DE W1AW");
CivEngine.getInstance().setFrequency(14_025_000L);
```

---

## Macro System

Macros are stored in `config.db` as JSON action arrays.

### Supported action types

| Type             | Effect                                     |
|------------------|--------------------------------------------|
| `CIV_COMMAND`    | Send raw CI-V hex string                   |
| `PTT_ON`         | Key the transmitter                        |
| `PTT_OFF`        | Unkey the transmitter                      |
| `VOICE_PLAY`     | Play audio file via JavaFX AudioClip       |
| `CW_TEXT`        | Send CW via CI-V (data = text to send)     |
| `INSERT_EXCHANGE`| Paste text into the active entry field     |
| `AUTOFILL_FIELDS`| Trigger QRZ callsign lookup + autofill     |
| `DELAY_MS`       | Wait N milliseconds (intData = ms)         |

### Example macro JSON
```json
[
  { "type": "PTT_ON" },
  { "type": "CW_TEXT", "data": "CQ CQ DE W1AW W1AW K" },
  { "type": "DELAY_MS", "intData": 2000 },
  { "type": "PTT_OFF" }
]
```

Macros can be bound to **F1–F12** function keys and triggered from either
the macro button bar or the keyboard.

---

## DX Cluster Engine

The `DxClusterEngine` (singleton) maintains a **single** telnet connection.

- Auto-logs in with your callsign when the server prompts
- Parses `DX de` spot lines into `DxSpot` objects
- Delivers spots to the **Filtered** tab via `spotListener`
- Delivers raw lines to the **Raw** tab via `rawLineListener`
- User can send arbitrary cluster commands from the Raw tab

---

## Themes

Two built-in themes: **Light** and **Dark**.

Theme selection persists via Java `Preferences`.  
`HamLogApp.applyTheme(scene)` applies the current theme to any scene.

---

## Internationalisation

String keys are externalized in `messages.properties` files:

```
resources/com/hamlog/i18n/
├── messages.properties       ← Default (English)
├── messages_en.properties
└── messages_de.properties
```

To add a language: create `messages_<lang>.properties` and add the
language code to the combo in Setup → Display.

---

## Building

```bash
# Requires Java 21, Maven 3.x
./build.sh
# or
mvn clean package
```

## Running

```bash
./run.sh
# or with explicit JavaFX module path:
java --module-path /path/to/javafx/lib \
     --add-modules javafx.controls,javafx.fxml,javafx.media \
     -jar target/hamlog-1.0.0-shaded.jar
```

---

## Error Logging

| Mode   | Level | What is logged                                        |
|--------|-------|-------------------------------------------------------|
| Normal | INFO+ | Application events, warnings, errors                  |
| Debug  | DEBUG | All CI-V frames, SQL queries, DX cluster traffic, UI events |

Logs are written to `~/.hamlog/logs/hamlog.log` with daily rotation (7 days retained).

---

## Data Files

All data is stored in `~/.hamlog/`:

```
~/.hamlog/
├── hamlog.db          ← Normal log QSOs
├── contest.db         ← Contest log QSOs
├── config.db          ← App config, macros, networks
├── plugins/           ← User-installed contest plugins
└── logs/              ← Application log files
```

---

## Extending with New Contest Plugins

1. Create a JSON file following the plugin schema above.
2. Place it in `~/.hamlog/plugins/` **or** import via
   **Contest → Import Plugin...** from the contest chooser.
3. Restart HamLog (or the plugin loader will detect it on next chooser open).

The `ContestPlugin.FieldDef` entries map directly to UI controls:
- `type: "text"` → `TextField`
- `type: "number"` → `TextField` (numeric)
- `type: "combo"` → `ComboBox<String>` with the `options` list

Field values are saved to `field1`–`field5` columns in `contest_qso`,
and the `cabrilloMapping` drives Cabrillo export column placement.
