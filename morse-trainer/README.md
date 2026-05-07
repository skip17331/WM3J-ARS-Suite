# Morse Code Trainer

A production-grade desktop application for learning, practicing, and refining Morse code skills. Built in **Java 21 + JavaFX** with a modular MVVM architecture, real-time decoding, multiple training modes, deep analytics, and optional hardware keyer support (Arduino USB and Raspberry Pi Zero wireless).

---

## Features

### Receiving practice
- **Letter Trainer** – random characters at user-set WPM, immediate feedback, Koch-method progression.
- **Letter Group Trainer** – random 2–5 character groups, custom alphabets, per-character accuracy tracking.
- **QSO Simulator** – realistic CW exchanges (CQ, callsigns, RST, name, QTH, rig, weather, sign-off) at three difficulty levels: Training, Casual, Contest (5NN + serials).

### Sending practice
- **Sending Trainer** – decodes your keying in real time and grades it against a target (Guided) or freely (Free-send).
- Real-time timing diagnostics: dit/dah variance, dit:dah ratio, achieved WPM, consistency score, smoothness index.
- Per-character trouble-spot reporting: which characters you mis-send, which have unstable timing, which need work.

### Analytics & scoring
- Character-level and word-level scoring.
- Error heatmap and per-character statistics (presented count, error rate, slowest response).
- Recommended drills based on weakest characters.
- Session export to JSON (`logs/session-<timestamp>.json`).

### Audio engine
- Java Sound API sine-wave generator with configurable tone (default 650 Hz).
- Smooth attack/release envelope to eliminate key clicks.
- Configurable WPM (5–40+) and ARRL Farnsworth spacing for slow-character / fast-spacing practice.

### Input methods
- **Keyboard** – Spacebar (configurable) with auto-repeat suppression and audible sidetone.
- **Arduino USB keyer** – serial protocol with timestamped DOWN/UP events. Supports straight key and iambic paddle (modes A & B). See [hardware/README.md](hardware/README.md).
- **Arduino USB HID keyer** – alternative firmware emulates a USB keyboard pressing Space; works with any Morse software, no app config. See [hardware/README.md](hardware/README.md).
- **Raspberry Pi Zero wireless keyer** – UDP over Wi-Fi with optional battery telemetry. Supports straight key and iambic paddle. (BLE bridge hook also exposed.)

> **Building a keyer?** Full BOMs, wiring diagrams, 3D-printable enclosure, and step-by-step assembly are in [hardware/README.md](hardware/README.md).

---

## Project layout

```
morse-trainer/
├── pom.xml
├── src/main/java/com/morsetrainer/
│   ├── Main.java                  # launcher
│   ├── audio/                     # ToneGenerator, MorsePlayer
│   ├── core/                      # AppConfig (JSON), Logger
│   ├── decoder/                   # MorseCode, TimingDecoder, KeyEvent, DecodedElement
│   ├── analytics/                 # ScoringEngine, SendingDiagnostics, CharStats
│   ├── trainer/letters|groups|qso|sendpractice/
│   ├── hardware/                  # KeyEventSource, KeyboardKeyer
│   ├── hardware/arduino/          # ArduinoKeyer (jSerialComm)
│   ├── hardware/pizero/           # PiZeroKeyer (UDP listener)
│   └── ui/                        # JavaFX views (programmatic)
├── src/main/resources/
│   ├── config/app-config.json     # default config
│   └── css/app.css
├── src/test/java/...              # JUnit 5 tests
├── hardware/
│   ├── arduino/morse_trainer_keyer.ino
│   └── pizero/keyer.py
└── README.md
```

---

## Build & run

### Prerequisites
- **JDK 21** (Temurin, OpenJDK, or Oracle).
- **Maven 3.9+**.
- An audio output device.
- (Optional) Arduino IDE for the keyer sketch.
- (Optional) Raspberry Pi Zero W with Python 3 and `gpiozero`.

### Build
```bash
mvn clean package
```
This produces a runnable shaded JAR at `target/morse-trainer-1.0.0.jar`.

### Run

**Recommended (handles JavaFX classpath automatically):**
```bash
mvn javafx:run
```

**Or run the shaded JAR** (works on the platform you built on; JavaFX native libs are platform-specific):
```bash
java -jar target/morse-trainer-1.0.0.jar
```

If you hit `Error: JavaFX runtime components are missing`, prefer `mvn javafx:run` or download the matching JavaFX SDK and run with:
```bash
java --module-path /path/to/javafx-sdk/lib \
     --add-modules javafx.controls,javafx.fxml \
     -jar target/morse-trainer-1.0.0.jar
```

### Tests
```bash
mvn test
```
Pure-Java tests cover the timing decoder, character mapping, analytics engine, trainer generators, and Arduino packet parser — none require JavaFX or audio hardware.

---

## Configuration

On first run, the app reads `src/main/resources/config/app-config.json`. Saving from the Settings view writes to `config/app-config.json` in the working directory, which takes precedence next launch.

Key fields:

| Field | Default | Notes |
|-------|---------|-------|
| `toneFrequencyHz` | 650 | Sidetone pitch |
| `wpm` | 18 | Character speed |
| `farnsworthWpm` | 12 | Effective speed (≤ wpm enables Farnsworth spacing) |
| `rampMillis` | 5 | Tone attack/release |
| `adaptiveDecoder` | true | Adapts to operator's actual dit length |
| `kochOrder` | `"KMURESNAPTLWI..."` | Koch character introduction order |
| `arduinoPort` | `/dev/ttyACM0` | Override per OS (`COM3` on Windows) |
| `pizeroUdpPort` | 51234 | Must match Pi Zero `--port` |

---

## Usage walkthrough

### Letter Trainer
1. Pick **Letter Trainer** from the home screen.
2. Set Koch level (1 = K & M only), WPM, and Farnsworth WPM.
3. Press **Play** – type what you hear; the trainer auto-advances on Enter or after the expected character length.
4. The right pane shows a live heatmap and recommended drills.

### Group Trainer
Similar, but plays 2–5 character groups. Word-boundary scoring runs at end of each group.

### QSO Simulator
1. Choose difficulty (Training / Casual / Contest).
2. Click **Generate & Play**.
3. Type your copy into the text area, then **Reveal & Score**. Final grade weights character accuracy 50%, word accuracy 30%, completeness 20%.

### Sending Trainer
1. Choose input source: Keyboard (Space), Arduino USB, or Pi Zero Wireless.
2. Pick mode: **Free** (decode whatever you send) or **Guided** (matches against a target).
3. Key Morse — the right pane updates timing diagnostics ~5 Hz.
4. After a session, the Trouble Report ranks characters by miscoding and timing variance.

### Settings
Edit any AppConfig field and click **Save**. Some changes (audio frequency) take effect on next session start.

---

## Hardware: Arduino USB keyer

### Wiring
- Single straight key or paddle common across `KEY_PIN` (default GPIO 2) and `GND`.
- `INPUT_PULLUP` is enabled — no external resistor needed.

### Flash the sketch
1. Open `hardware/arduino/morse_trainer_keyer.ino` in Arduino IDE.
2. Select board (Uno, Pro Micro, Nano, etc.) and port.
3. Upload.

### Connect from the app
1. Plug in the Arduino. Identify the serial port:
   - Linux: `ls /dev/ttyACM* /dev/ttyUSB*`
   - macOS: `ls /dev/tty.usbmodem*`
   - Windows: Device Manager → Ports (COM & LPT)
2. Set `arduinoPort` in Settings (or `app-config.json`).
3. In Sending Trainer, choose **Arduino USB** as the input source.

The sketch emits lines of the form:
```
DOWN 12345
UP   12389
```
where the number is `millis()` since boot. The Java parser is tolerant of trailing whitespace and also accepts the simple tokens `DITDOWN/DITUP/DAHDOWN/DAHUP` if you use a custom firmware.

---

## Hardware: Raspberry Pi Zero W wireless keyer

### Wiring
- Key common across **GPIO 17** (BCM) and **GND**. (Edit `KEY_PIN` in `keyer.py` to change.)
- Optional: small LiPo + INA219 for battery telemetry (the `battery_pct()` function is a stub you can fill in).

### Install dependencies
```bash
sudo apt install python3-gpiozero
```

### Run
```bash
python3 hardware/pizero/keyer.py --host 192.168.1.10 --port 51234
```
where `--host` is the IP of the laptop running Morse Trainer.

### Run as a service (optional)
Create `/etc/systemd/system/morse-keyer.service`:
```ini
[Unit]
Description=Morse Trainer wireless keyer
After=network-online.target
Wants=network-online.target

[Service]
ExecStart=/usr/bin/python3 /home/pi/morse-trainer/hardware/pizero/keyer.py --host 192.168.1.10 --port 51234
Restart=always
User=pi

[Install]
WantedBy=multi-user.target
```
Then:
```bash
sudo systemctl daemon-reload
sudo systemctl enable --now morse-keyer
```

### Connect from the app
1. Set `pizeroUdpPort` in Settings to match the Pi (default 51234).
2. In Sending Trainer choose **Pi Zero Wireless** — the app starts a UDP listener and processes packets:
   ```
   DOWN|<millis>|<batteryPct>
   UP|<millis>|<batteryPct>
   ```

A BLE-bridge hook (`PiZeroKeyer.injectEvent()`) is exposed for users who prefer Bluetooth — pipe parsed BLE events into that method.

---

## Keyboard shortcuts

| Mode | Key | Action |
|------|-----|--------|
| Sending Trainer | **Space** | Key down / key up (auto-repeat suppressed) |
| Letter / Group / QSO | **Enter** | Submit answer |
| Any view | **Esc** | (System) close window |

---

## Architecture notes

- **MVVM-style**: views (`ui/`) hold no business state; sessions (`trainer/*`) and analytics own logic and are unit-testable.
- **Deterministic timing**: `TimingDecoder` accepts an injected dit estimate (`setEstimatedDitMs`) and an `adaptive` flag, allowing tests to feed synthetic `KeyEvent` streams and assert exact decoded characters.
- **Streaming decoder**: events are classified mark/gap and folded into characters via configurable thresholds (`gapMarkThresholdMillis`, `charBoundaryRatio`, `wordBoundaryRatio`).
- **Adaptive operator model**: when enabled, the decoder maintains an EMA of dit length so it tracks an operator who speeds up or slows down mid-session.
- **ARRL Farnsworth**: implemented per the standard formula `t_a = (60·c − 37.2·s) / (s·c)` distributed between character and word gaps.
- **Per-character timing buckets**: in Sending Trainer, marks decoded between character boundaries are tagged to the resulting character so the Trouble Report can show *worst dit-variance for X*, *worst dah-variance for Q*, etc.

---

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| No audio on Linux | Ensure your user is in the `audio` group; check that PulseAudio/PipeWire is exposing the default sink. |
| `SerialPortInvalidPortException` | Verify the port name in Settings matches your OS; on Linux add yourself to `dialout` (`sudo usermod -aG dialout $USER`). |
| Decoder always shows `?` | Lower WPM, or enable adaptive decoder. The decoder needs ~3 well-formed elements to lock its dit estimate. |
| Pi Zero never connects | Check the laptop firewall allows UDP on `pizeroUdpPort`; confirm both devices are on the same subnet. |
| Build fails on JavaFX | Use `mvn javafx:run` instead of running the shaded JAR; JavaFX native libs differ per OS. |

---

## License

This project is provided as-is for educational use. All third-party libraries retain their original licenses (JavaFX – GPLv2+CE, jSerialComm – LGPL/Apache, Jackson – Apache 2.0, JUnit – EPL).

73 and good copy!
