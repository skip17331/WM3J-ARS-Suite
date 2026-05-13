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

Two keying methods ship out of the box; pick one in Preferences.

| Method | What happens | Use when |
|---|---|---|
| **VOX** (default) | j-digi outputs audio with no key command. The rig's VOX, or an audio-sense interface like SignaLink / DigiRig, handles PTT itself | Simplest setup, no CAT cable required. Works for any digital mode that puts audio out at a reasonable level |
| **HAMLIB** | j-digi opens a TCP socket to `rigctld` and sends `T 1\n` / `T 0\n` for PTT. Reconnects each toggle so a daemon restart isn't sticky | Any rig Hamlib supports (Yaesu, Kenwood, Elecraft, Icom, etc.). Required for pure CW where there's no audio for VOX to sense |

A third option, `CivRigControl`, is built (direct CI-V to Icom via `CivEngine` from j-log-engine) but not wired into `ModemService` — Hamlib's `icom` backend covers Icom rigs without a second serial allocation. The class is there if a future demand justifies plumbing it in.

Java Preferences under `com.hamradio.modem.ModemService`:

| Pref | Values | Default |
|---|---|---|
| `ptt.method` | `VOX` / `HAMLIB` | `VOX` |
| `ptt.hamlib.host` | hostname / IP | `127.0.0.1` |
| `ptt.hamlib.port` | TCP port | `4532` |

The legacy value `NONE` (from earlier builds) is still accepted as a synonym for `VOX`.

Wire protocol for HAMLIB uses the plain rigctld command set — `T 1\n` / `T 0\n`, expecting `RPRT 0\n` on success. Any non-zero `RPRT` is surfaced as an `IOException` that bubbles up through `AudioTxEngine`'s `onError` callback.

## CW keyer — audio vs CAT

For CW specifically there's a separate choice: should j-digi generate the dots and dashes as audio, or hand the text to the rig's built-in keyer over CAT?

| Pref | What happens | Use when |
|---|---|---|
| `cw.keyer = AUDIO` (default) | `CwTransmitter` synthesises sidetone audio with raised-cosine keying; goes through `AudioTxEngine` with PTT toggled per the `ptt.method` setting | Most setups — works without CAT, gives you full control over sidetone frequency and key-click filtering. Required if you're using VOX |
| `cw.keyer = HAMLIB` | `HamlibCwKeyer` sends `b <text>\n` to `rigctld`; the rig's own keyer plays the Morse at its configured WPM. No audio path used | Cleanest timing — rig handles keying internally with no soundcard routing. Required if you have no audio path to the rig but do have CAT |

Additional pref `cw.wpm` sets the WPM for the CAT-keyer path (default 20). The audio path's WPM lives in `CwTransmitter`'s constructor for now.

Cancel works on both paths — audio cancels the `SourceDataLine`, CAT sends `\stop_morse\n`. Both fire the same `onCancelled` callback shape.

Completion timing for the CAT path is estimated from PARIS formula (`length × 10 × 1200/wpm` ms + 200 ms baseline) since `rigctld` returns `RPRT 0` as soon as it queues the command — the rig is still keying. A future polish could poll `\get_ptt` for real end-of-transmission detection.

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
