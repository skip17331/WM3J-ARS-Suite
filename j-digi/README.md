# J-Digi — ARS Digital Modem Client

This is a first milestone JavaFX modem client for the ARS Suite hub.

## What works
- JavaFX GUI
- Hub-compatible WebSocket client
- Required `APP_CONNECTED` handshake
- Receives `RIG_STATUS`, `SPOT_SELECTED`, `APP_LIST`, `HUB_WELCOME`
- Soundcard audio capture via Java Sound
- FFT/spectrum + simple waterfall rendering
- Signal metrics: RMS, peak frequency
- Mode selection UI: RTTY, PSK31, AX.25
- Decoder skeletons with basic tone/energy analysis
- Sends `MODEM_DECODE` messages to the hub

## What is intentionally incomplete
- Full protocol-grade PSK31 decode
- Full protocol-grade RTTY decode
- Full AX.25 frame extraction / CRC validation
- TX path
- Shared config screen integration beyond hub connection settings

## Run
```bash
mvn javafx:run
```

Or build a fat jar:
```bash
mvn package
java -jar target/j-digi-0.1.0-jar-with-dependencies.jar
```

## Default hub endpoint
`ws://127.0.0.1:8080`

## Hub integration
On connect the app sends:
```json
{"type":"APP_CONNECTED","appName":"j-digi","version":"0.1.0"}
```

The app currently emits `MODEM_DECODE` messages with:
- mode
- text
- frequency
- offsetHz
- snr
- confidence
- timestamp

You can add handling for `MODEM_DECODE` to the hub router or just let other apps listen for it directly.
