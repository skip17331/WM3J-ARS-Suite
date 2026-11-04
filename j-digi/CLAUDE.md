# CLAUDE.md — j-digi

Guidance for Claude Code when working in this module.

## Role in the suite

`j-digi` is the modem **library** behind the J-Digi surface of `ars-fx` (see `../ars-fx/CLAUDE.md`). It has no window,
launcher or hub link of its own — the standalone UI, `ModemMain`, `HubClient` and the launcher scripts were removed;
the whole suite is now the single `ars-fx` app. `ars-fx` drives the modem through `com.ars.fx.data.DigiEngine`, which
calls `ModemService.startupEmbedded()` (audio capture only) and registers the decode, spectrum and status listeners.
`ars-fx` supplies station identity, macros and rig control itself.

## Build

```bash
mvn install             # tests run; installs j-digi-<version>.jar to ~/.m2
```

Java 21, Maven. `j-log-common` and `j-log-engine` must be in `~/.m2` first (`../build-release.sh` installs them).

## Status

Decode is implemented and audio-loopback validated for every bundled mode. Transmit exists for CW, RTTY, PSK31,
Olivia, MFSK16 and Domino-EX. **AX.25 is decode-only** (`ModemService` throws `UnsupportedOperationException` for
AX.25 WAV export). Don't describe the modem as production-grade until it has been tested on air against real signals.

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

Decoders run on the audio thread and call the host's listeners there; the library has no JavaFX code, so a host that
touches the UI (`ars-fx`) must hop to the JavaFX Application Thread itself.

## PTT / rig keying

Two keying methods, chosen by the `ptt.method` preference.

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

These live in Java Preferences (`com.hamradio.modem.ModemService`). A host application sets them with
`ModemService.applyPttSettings(method, host, port)`, which saves them and rebuilds the PTT implementation (deferred if a
transmission is under way). `ars-fx` calls it from `DigiEngine.syncPtt()` so J-Digi follows the J-Hub rig settings:
`rig.ptt` `VOX` → `VOX`; `CAT`, `RTS` or `DTR` → `HAMLIB` (the rigctld daemon does the keying); host and port from
`rig.rigctldHost` / `rig.rigctldPort`. The Hamlib CW keyer reads the same host and port.

CW speed and keyer are set the same way with `applyCwSettings(keyer, wpm)`: `ars-fx` maps J-Hub ▸ Data ▸ Transmit's *CW speed*
(`tx.cwWpm`) to `cw.wpm` and its *J-Digi CW keyer* (`tx.cwKeyer`: Audio → `AUDIO`, Rig keyer → `HAMLIB`) to `cw.keyer`. Wire protocol is the plain rigctld set; a non-zero
`RPRT` surfaces as an `IOException` through `AudioTxEngine`'s `onError`.

## CW keyer — audio vs CAT

| Pref | What happens | Use when |
|---|---|---|
| `cw.keyer = AUDIO` (default) | `CwTransmitter` synthesises sidetone audio with raised-cosine keying through `AudioTxEngine` | Most setups; required with VOX |
| `cw.keyer = HAMLIB` | `HamlibCwKeyer` sends `b <text>\n` to `rigctld`; the rig's keyer plays the Morse | No audio path to the rig but CAT works |

`cw.wpm` (default 20) applies to both paths and is re-read on every CW transmit. Cancel works on both (audio cancels
the `SourceDataLine`; CAT sends `\stop_morse\n`). CAT completion timing is estimated from PARIS
(`length × 10 × 1200/wpm` ms + 200 ms) because `rigctld` returns `RPRT 0` as soon as it queues the command.

## Rig state and band plan caption

The host tells the modem where the rig is tuned with `ModemService.setRigState(frequencyHz, mode)` (0 Hz when the rig is
not connected); it only notifies the status listener when the frequency or mode changed, so it is cheap to call on every
poll. `ars-fx` does this from `DigiEngine`, which listens to `RigClient`. The frequency feeds `ModemStatus` (J-Digi's
"Rig freq" readout), the decoders, `bandFromRigHz()` and the caption: `captionFor(hz)` asks the shared `BandplanLoader`
(j-log-common) for e.g. "20m / CW — CW (US)" and stores it in `ModemStatus.bandplanCaption`. The caption follows the
band plan's *active region* (set by `ars-fx` from Station ▸ IARU region), not a preference of this module.

## Local CW skimmer

Off until the host turns it on with `setLocalSkimmerEnabled(true)` (saved). While on, `LocalSkimmer` finds CW carriers in the audio
passband, `MultiCarrierDecoder` decodes each one, and `CallsignScorer` promotes callsigns it is confident of (needs a `CQ`/`DE`
context or a repeat). `SkimmerSpotPublisher` then turns each into a `LocalSpot` (callsign, dial + audio offset in Hz, SNR,
context) with a 5-minute per-(callsign, kHz) dedup, and hands it to the host's `setSpotListener` on the audio thread. A spot needs
the rig frequency (`setRigState`); until the host provides one nothing is produced. `ars-fx` adds each spot to the shared spot list.

Limits: the channel decoder copies the synthetic test signal cleanly at about 12 wpm; from about 15 wpm up it runs letters together
(`DE W1AW` comes out as one wrong character), so few callsigns reach the scorer at normal contest speeds. That is decoder accuracy,
not the spot path, and it has not been investigated. `ModemServiceSkimmerTest` covers audio → spot at 12 wpm.

## State and persistence

Mode (`modem.mode`), RTTY reverse (`rtty.reverse`), audio devices (`audio.input.device`, `audio.output.device`), PTT and
keyer settings, and the skimmer switch (`skimmer.local.enabled`) persist via Java `Preferences`
(typically `~/.java/.userPrefs/com/hamradio/modem`), **not** a file in `~/.j-digi/`. `ModemService` reads the saved mode
on init and writes it on each mode switch.

## Packages

| Package | Responsibility |
|---|---|
| `com.hamradio.modem` | `ModemService` |
| `com.hamradio.modem.audio` | Audio device discovery, `AudioEngine` capture thread |
| `com.hamradio.modem.dsp` | FFT, window functions, Goertzel, Viterbi, `SignalClassifier`, `LocalSkimmer`, `MultiCarrierDecoder`, `CallsignScorer` |
| `com.hamradio.modem.mode` | One class per mode — CW, RTTY, PSK31, Olivia, MFSK16, Domino-EX, AX.25 |
| `com.hamradio.modem.tx` | Transmitters, `AudioTxEngine`, `HamlibRigControl`, `HamlibCwKeyer`, `NoOpRigControl`, `WavFileWriter` |
| `com.hamradio.modem.model` | `ModemStatus`, `ModeType`, `DecodeMessage`, `SignalSnapshot` |

## Loopback testing

Route tone output through a PulseAudio loopback device (`jdigi_loop` by convention) and capture from its monitor.
`generate_*_test_suite.py`, the `*_test_suite/` folders, `run_cw_test.sh` and `run_psk31_test.sh` live in
`../tools/j-digi-test-signals/` (run the generators from that folder; `Ax25ModeTest` reads the AX.25 WAVs from there). Most decoders have per-mode DEBUG toggles.

## Dependencies

Depends on `j-log-engine` and `j-log-common` for shared types. Build them first if missing from `.m2`.

`pom.xml` still declares JavaFX dependencies and per-platform profiles, but no source file imports JavaFX; they are
leftovers from the removed UI and can be dropped.

## What's NOT here

- No window, FXML or launcher — the UI belongs to `ars-fx`.
- No hub connection and no `~/.j-digi/` directory — state lives in Java Preferences.
- No bundled data files beyond `logback.xml`.
