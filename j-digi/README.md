# J-Digi — ARS Digital Modem engine

The digital-modes engine of the WM3J ARS Suite: soundcard capture, FFT/waterfall data, decoders and transmitters for
CW, RTTY, PSK31, Olivia, MFSK16, Domino-EX and AX.25.

This is a **library**, not an app. It has no window or launcher of its own: `ars-fx` embeds it as the J-Digi surface
(see the [User Guide](../USER_GUIDE.md)). The only standalone module in the suite is J-Map.

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

Push-to-talk is VOX (default) or Hamlib `rigctld`. Decoders are validated by audio loopback; on-air testing against
real signals is still outstanding.

## Build

Needs Java 21 and Maven. `j-log-common` and `j-log-engine` must be installed in your local Maven repository first
(`../build-release.sh` does that).

```bash
mvn install
```

Test signals and the scripts that generate and play them are in
[`../tools/j-digi-test-signals/`](../tools/j-digi-test-signals/). See [CLAUDE.md](CLAUDE.md) for the architecture and
keying details.
