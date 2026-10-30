# J-Digi — ARS Digital Modem

The digital-modes engine of the WM3J ARS Suite: soundcard capture, FFT/waterfall,
decoders and transmitters for CW, RTTY, PSK31, Olivia, MFSK16, Domino-EX and AX.25.

Most operators use it **inside the ARS Suite app** (`ars-fx`), which embeds this
module as its J-Digi surface — see the [User Guide](../USER_GUIDE.md). This
directory is the modem library plus a standalone JavaFX window.

## Modes

| Mode | Decode | Transmit |
|---|---|---|
| CW | yes | yes (audio keyer, or the rig's own keyer over Hamlib) |
| RTTY | yes | yes |
| PSK31 | yes | yes |
| Olivia | yes | yes |
| MFSK16 | yes | yes |
| Domino-EX | yes | yes |
| AX.25 | yes | **no** — decode-only |

Push-to-talk is VOX (default) or Hamlib `rigctld`. Decoders are validated by audio
loopback; on-air testing against real signals is still outstanding.

## Build and run

Needs Java 21 and Maven.

```bash
./run.sh          # builds, then runs the newest target/j-digi-*.jar
```

or:

```bash
mvn package
java -jar target/j-digi-<version>-jar-with-dependencies.jar
```

Shared libraries (`j-log-common`, `j-log-engine`) must be installed in your local
Maven repository first; `../build-release.sh` does that.

## Standalone vs embedded

- **Embedded (normal):** `ars-fx` calls `ModemService.startupEmbedded()`, which
  starts audio capture only. The suite supplies station data, rig control and macros.
- **Standalone:** `ModemService.startup()` also opens a WebSocket to a legacy hub
  (default `ws://127.0.0.1:8080`, saved as the `hub.url` preference). The old
  standalone J-Hub that listened there has been retired, so with nothing on that
  port the hub link fails and standalone mode runs without station data. For
  day-to-day use run the full app instead.

The standalone window keeps its own settings (mode, audio devices, PTT, CW
keyer) in Java Preferences, not in a file under `~/.j-digi/`.

See [CLAUDE.md](CLAUDE.md) for the architecture and keying details.
