# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
./run.sh
# which runs:
mvn clean install -f pom.xml
java --module-path ./lib/javafx \
     --add-modules javafx.controls,javafx.graphics \
     -Dfile.encoding=UTF-8 \
     -jar target/j-digi-0.1.0-jar-with-dependencies.jar
```

JavaFX 21 must be on the module path at `./lib/javafx`. Java 17+ at runtime.

## Status

**Version 0.1.0 — early phase.** Decode side is implemented and audio-loopback validated across all bundled modes. Transmit side has a real PTT path now (see "PTT / rig keying" below); operator selects the method via Java Preferences.

## Architecture

JavaFX desktop app, single window. `ModemService` is the central singleton; it owns:

| Component | Role |
|---|---|
| `AudioEngine` | Opens the chosen `TargetDataLine`, runs a capture thread, delivers `float[]` frames to listeners |
| `FftAnalyzer` | Real-time FFT for the waterfall + per-mode demodulators |
| `ModeManager` | EnumMap of mode decoders; `process()` dispatches each frame to whichever mode is active |
| `SignalClassifier` | Rolling ~16 s audio buffer; classifies "what mode is this?" when the operator clicks a signal in the waterfall |
| `AudioTxEngine` | TX-side state machine; emits PCM samples for outbound CW/RTTY/PSK31 (rig keying is `NoOpRigControl` for now) |
| `HubClient` | WebSocket to J-Hub on 8080; sends `WSJTX_DECODE`/equivalent messages, receives `RIG_STATUS` |

Decoders run on the audio thread; the UI receives decoded text on the JavaFX Application Thread via `Platform.runLater`.

## PTT / rig keying

`AudioTxEngine` accepts any `RigControl` implementation. Three ship:

| Implementation | Use when |
|---|---|
| `NoOpRigControl` (default) | You only want to drive audio for monitoring/tuning; no real rig is keyed |
| `HamlibRigControl` | Any rig — connects to `rigctld` on a configurable host/port (default `localhost:4532`). Reconnects per PTT toggle so a daemon restart doesn't sticky-fail |
| `CivRigControl` | Icom rigs — direct, no `rigctld`. Wraps the shared `CivEngine` from `j-log-engine`. Not currently wired into `ModemService` (j-digi doesn't spin up its own CivEngine yet) |

Configured via Java `Preferences` under `com.hamradio.modem.ModemService`:

| Pref | Values | Default |
|---|---|---|
| `ptt.method` | `NONE` / `HAMLIB` | `NONE` |
| `ptt.hamlib.host` | hostname / IP | `127.0.0.1` |
| `ptt.hamlib.port` | TCP port | `4532` |

Wire protocol uses the plain rigctld command set — `T 1\n` / `T 0\n`, expecting `RPRT 0\n` on success. Any non-zero `RPRT` is surfaced as an `IOException` that bubbles up through `AudioTxEngine`'s `onError` callback.

## Mode selection

Persisted via Java `Preferences` (typically `~/.java/.userPrefs/com/hamradio/modem`) — **not** a file in `~/.j-digi/`. `ModemService` reads `PREF_MODE` on init and writes it on every mode switch.

## Packages

| Package | Responsibility |
|---|---|
| `com.hamradio.modem` | `ModemMain` (Application entry), `ModemService`, `ConfigManager`, `CrashHandler` |
| `com.hamradio.modem.audio` | Audio device discovery, `AudioEngine` capture thread, sample-rate handling |
| `com.hamradio.modem.dsp` | FFT, waterfall rendering, `SignalClassifier`, signal-metrics |
| `com.hamradio.modem.mode` | One class per mode — CW, RTTY, PSK31, Olivia, MFSK16, Domino-EX, AX.25 |
| `com.hamradio.modem.tx` | CW/RTTY/PSK31 transmitters, WAV file writer, `NoOpRigControl` placeholder |
| `com.hamradio.modem.hub` | `HubClient` WebSocket wrapper to J-Hub |
| `com.hamradio.modem.ui` | `MainWindow`, `WaterfallPane`, `SpectrumPane`, settings dialogs |
| `com.hamradio.modem.model` | Decode/spot/rig-status POJOs |

## Loopback testing

The decoder suite is exercised by routing tone output through a PulseAudio loopback device (`jdigi_loop` is the conventional name). Capture device is the monitor of that loopback. See `reference_jdigi_testing.md` in memory for the exact setup. Most decoders have per-mode DEBUG toggles in their respective classes.

## External integrations

- WebSocket to J-Hub (port 8080) — RIG_STATUS in, decode messages out.
- Depends on `j-log-engine` for shared types (QSO records, hub-client primitives). Build `j-log-engine` first if it's missing from `.m2`.

## What's NOT here

- No `~/.j-digi/` directory — all state lives in Java Preferences.
- No FXML — UI is built programmatically.
- No bundled data files (no plugins, no JSON resources beyond logback.xml + icons).
