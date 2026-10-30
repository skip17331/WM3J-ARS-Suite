# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Role in the suite

`j-digi` is the modem library behind the **J-Digi** surface of `ars-fx` (see `../ars-fx/CLAUDE.md`). It also
builds a standalone JavaFX window (`ModemMain` / `Launcher`). `ars-fx` depends on this module and drives it through
`com.ars.fx.data.DigiEngine`, which calls `ModemService.startupEmbedded()`.

- **Embedded** (normal): `startupEmbedded()` starts audio capture only and reports audio failures to the decode log.
  There is no hub connection; `ars-fx` supplies station identity, macros and rig control itself.
- **Standalone**: `startup()` first connects `HubClient` to a WebSocket hub (default `ws://127.0.0.1:8080`, pref
  `hub.url`) and only then starts audio. That hub was the old standalone J-Hub, which has been retired, so on its
  own the connect fails. Keep the code path working for tests, but do not build new features on it.

## Build & Run

```bash
./run.sh                # mvn clean install, then java -jar on the newest target/j-digi-*.jar
```

Java 21, Maven. `j-log-common` and `j-log-engine` must be in `~/.m2` first (`../build-release.sh` installs them).
The fat jar carries JavaFX, so no `--module-path` is needed.

## Status

Decode is implemented and audio-loopback validated for every bundled mode. Transmit exists for CW, RTTY, PSK31,
Olivia, MFSK16 and Domino-EX. **AX.25 is decode-only** (`ModemService` throws `UnsupportedOperationException` for
AX.25 WAV export). Bump to 1.0-quality claims gates on on-air testing against real signals.

## Architecture

`ModemService` is the central singleton; it owns:

| Component | Role |
|---|---|
| `AudioEngine` | Opens the chosen `TargetDataLine`, runs a capture thread, delivers `float[]` frames to listeners |
| `FftAnalyzer` | Real-time FFT for the waterfall + per-mode demodulators |
| `ModeManager` | EnumMap of mode decoders; `process()` dispatches each frame to the active mode |
| `SignalClassifier` | Rolling ~16 s audio buffer; classifies the mode when the operator clicks a signal in the waterfall |
| `LocalSkimmer` / `SkimmerSpotPublisher` | Local CW skimmer that publishes spots |
| `AudioTxEngine` | TX state machine; emits PCM for outbound CW/RTTY/PSK31/Olivia/MFSK16/Domino-EX |
| `HubClient` | Legacy WebSocket link used only by standalone `startup()` |

Decoders run on the audio thread; the UI receives decoded text on the JavaFX Application Thread via `Platform.runLater`.

## PTT / rig keying

Two keying methods; pick one in Preferences (embedded: in the suite's J-Hub pages).

| Method | What happens | Use when |
|---|---|---|
| **VOX** (default) | Audio only, no key command. The rig's VOX or an audio-sense interface (SignaLink, DigiRig) keys PTT | Simplest; no CAT cable. Not usable for pure CW |
| **HAMLIB** | Opens a TCP socket to `rigctld` and sends `T 1\n` / `T 0\n` (`HamlibRigControl`). Reconnects each toggle | Any rig Hamlib supports. Required for pure CW with no audio to sense |

`NoOpRigControl` is the VOX implementation. The legacy value `NONE` is accepted as a synonym for `VOX`. Direct
CI-V control (`CivRigControl`/`CivEngine`) was removed; Hamlib's `icom` backend covers Icom rigs.

| Pref | Values | Default |
|---|---|---|
| `ptt.method` | `VOX` / `HAMLIB` | `VOX` |
| `ptt.hamlib.host` | hostname / IP | `127.0.0.1` |
| `ptt.hamlib.port` | TCP port | `4532` |

Embedded, `ars-fx` supplies these from its J-Hub settings; the Java Preferences
(`com.hamradio.modem.ModemService`) are the standalone store. Wire protocol is the plain rigctld set; a non-zero
`RPRT` surfaces as an `IOException` through `AudioTxEngine`'s `onError`.

## CW keyer — audio vs CAT

| Pref | What happens | Use when |
|---|---|---|
| `cw.keyer = AUDIO` (default) | `CwTransmitter` synthesises sidetone audio with raised-cosine keying through `AudioTxEngine` | Most setups; required with VOX |
| `cw.keyer = HAMLIB` | `HamlibCwKeyer` sends `b <text>\n` to `rigctld`; the rig's keyer plays the Morse | No audio path to the rig but CAT works |

`cw.wpm` (default 20) applies to both paths and is re-read on every CW transmit. Cancel works on both (audio cancels
the `SourceDataLine`; CAT sends `\stop_morse\n`). CAT completion timing is estimated from PARIS
(`length × 10 × 1200/wpm` ms + 200 ms) because `rigctld` returns `RPRT 0` as soon as it queues the command.

## Macros

`MacroBar` and `HubMacro` carry the F1–F8 macros. In the suite they are the same editable macros as J-Log, built from
station data with `{TAGS}` (see `ars-fx` `MacroConfig`), plus a DX call box for `{CALL}`.

## Band plan caption

`ModemService.captionFor(hz)` consults the shared `BandplanLoader` (j-log-common) and writes e.g. "20m / CW — CW (US)"
into `ModemStatus.bandplanCaption`; `bandFromRigHz` also delegates to it, so band names match the log and cluster.
Region and country prefs: `bandplan.region` (`IARU-R1/R2/R3`, default `IARU-R2`) and `bandplan.country` (`US` or blank).
In the suite the region comes from the Station "IARU region" setting.

## State and persistence

Mode, audio devices, PTT and keyer settings persist via Java `Preferences` (typically `~/.java/.userPrefs/com/hamradio/modem`),
**not** a file in `~/.j-digi/`. `ModemService` reads `PREF_MODE` on init and writes it on each mode switch.

## Packages

| Package | Responsibility |
|---|---|
| `com.hamradio.modem` | `ModemMain`, `Launcher`, `ModemService`, `CrashHandler` |
| `com.hamradio.modem.audio` | Audio device discovery, `AudioEngine` capture thread |
| `com.hamradio.modem.dsp` | FFT, window functions, Goertzel, Viterbi, `SignalClassifier`, `LocalSkimmer`, `MultiCarrierDecoder` |
| `com.hamradio.modem.mode` | One class per mode — CW, RTTY, PSK31, Olivia, MFSK16, Domino-EX, AX.25 |
| `com.hamradio.modem.tx` | Transmitters, `AudioTxEngine`, `HamlibRigControl`, `HamlibCwKeyer`, `NoOpRigControl`, `WavFileWriter` |
| `com.hamradio.modem.hub` | `HubClient` legacy WebSocket wrapper |
| `com.hamradio.modem.i18n` | `I18n` |
| `com.hamradio.modem.ui` | `MainWindow`, `WaterfallPane`, `SpectrumPane`, `MacroBar`, `LogEntryPane`, `ContestEntryPane`, `DxSpotPane`, `RightPanel`, `SplashScreen` |
| `com.hamradio.modem.model` | Decode/spot/rig-status POJOs |

## Loopback testing

Route tone output through a PulseAudio loopback device (`jdigi_loop` by convention) and capture from its monitor.
`generate_*_test_suite.py`, the `*_test_suite/` folders, `run_cw_test.sh` and `run_psk31_test.sh` live in
`../tools/j-digi-test-signals/` (run the generators from that folder; `Ax25ModeTest` reads the AX.25 WAVs from there). Most decoders have per-mode DEBUG toggles.

## Dependencies

Depends on `j-log-engine` and `j-log-common` for shared types. Build them first if missing from `.m2`.

## What's NOT here

- No `~/.j-digi/` directory — state lives in Java Preferences.
- No FXML — the UI is built programmatically.
- No bundled data files beyond `logback.xml` and icons.
