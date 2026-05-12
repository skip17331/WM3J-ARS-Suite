# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
./run.sh
# or
mvn clean package -q
java -Dfile.encoding=UTF-8 -jar target/morse-trainer-1.0.0.jar
```

Java 21 + JavaFX 21. **Standalone — no network, no dependencies on other ARS Suite modules.**

## Architecture

JavaFX desktop app. Shared audio is owned by `ToneGenerator` (configurable WPM + frequency sine wave) and `MorsePlayer` (handles `SourceDataLine` lifecycle). Each trainer mode is a `Session` consuming `KeyEvent`s from a pluggable `KeyEventSource`. Pipeline:

```
KeyEventSource → TimingDecoder → MorseCode → ScoringEngine
   (hardware)    (timings→dits)   (decode)    (compare to expected)
```

`KeyEventSource` is the hardware abstraction:

```java
public interface KeyEventSource extends AutoCloseable {
    void start(Consumer<KeyEvent> consumer);
    void stop();
    String name();
    boolean isConnected();
}
```

Three implementations ship:

| Implementation | Source | Wire format |
|---|---|---|
| `KeyboardKeyer` | local keyboard (space bar) | JavaFX `KeyEvent` on the app thread |
| `ArduinoKeyer` | serial port (jSerialComm, configurable baud) | line-based ASCII `DOWN/UP` timestamps or `ELEM DIT/DAH` packets |
| `PiZeroKeyer` | UDP listener (default port 8889) | text packets `DOWN\|ts\|bat`, `UP\|ts\|bat`, `ELEM\|DIT\|ts\|bat`, `PING\|ts\|bat` |

`PiZeroKeyer` also exposes a direct-injection hook for a BLE adapter.

## Trainer modes

| Session | Purpose |
|---|---|
| `LetterSession` | Drill a single character, then groups |
| `GroupSession` | Random groups of 5 — playback + verification |
| `SendPracticeSession` | User keys, app scores accuracy + timing |
| `QsoTrainerSession` | Simulated QSO dialog (template phrases) |

`ScoringEngine` evaluates: character accuracy, dit/dah/space timing distributions, per-character stats (`CharStats`).

## Packages

| Package | Responsibility |
|---|---|
| `com.morsetrainer` | `Main` (entry) |
| `com.morsetrainer.ui` | `MorseTrainerApp`, `HomeView`, per-mode views, `SettingsView` |
| `com.morsetrainer.audio` | `ToneGenerator`, `MorsePlayer` |
| `com.morsetrainer.hardware` | `KeyEventSource`, `KeyboardKeyer` |
| `com.morsetrainer.hardware.arduino` | `ArduinoKeyer` |
| `com.morsetrainer.hardware.pizero` | `PiZeroKeyer` |
| `com.morsetrainer.decoder` | `TimingDecoder`, `MorseCode`, `KeyEvent`, `DecodedElement` |
| `com.morsetrainer.trainer.letters` / `groups` / `sendpractice` / `qso` | Mode-specific sessions |
| `com.morsetrainer.analytics` | `ScoringEngine`, `SendingDiagnostics`, `CharStats` |
| `com.morsetrainer.core` | `AppConfig` (JSON), `Logger` |

## Data storage

- `~/.morse-trainer/config.json` — user prefs: WPM, tone freq, keyer choice, hardware device port
- Bundled defaults in `src/main/resources/config/app-config.json`

## Resources

- `config/app-config.json` — startup defaults
- `data/qso-phrases.json` — QSO dialog templates
- `css/app.css`

## Tests

5 files, 33 cases — `MorseCodeTest`, `TimingDecoderTest`, `TrainerModulesTest`, `AnalyticsTest`, `ArduinoKeyerParserTest`.

## What's NOT here

- No J-Hub integration — fully standalone (the agreement is that "Morse trainer" is one of the chapters inside J-Learn, but the trainer app itself does not need to talk to anything else).
- No CW transmit-out — this is a learning tool, not a CAT keyer.
