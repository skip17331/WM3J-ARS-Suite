---
id: 03-09
title: Hardware Keyer Builds
chapter: 03
section: 09
level: simple
status: draft
---

# Hardware Keyer Builds

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The bundled Morse Trainer (see §03-00) accepts the keyboard out of the box, but it really shines once you plug a real key into a hardware adapter. This section summarizes the three reference designs that ship with the trainer.

The full build guide — BOMs, wiring diagrams, firmware listings, systemd unit, OpenSCAD enclosure — lives at:

```
morse-trainer/hardware/README.md
morse-trainer/hardware/arduino/         (.ino sketches)
morse-trainer/hardware/pizero/          (keyer.py + morse-keyer.service)
morse-trainer/hardware/enclosure/       (keyer-box.scad + wiring-diagram.svg)
```

What follows here is a J-Learn-level summary so you can decide which one to build.

## Choosing a design

| Design | Transport | Cost (USD) | Latency | Effort | App config |
|--------|-----------|------------|---------|--------|------------|
| **A. Arduino USB serial keyer** | Wired USB | ~$8–12 | 3–8 ms | 30 min | Pick "Arduino USB" + serial port |
| **B. Arduino USB HID keyer** | Wired USB | ~$10–14 | 2–5 ms | 30 min | None — appears as a keyboard |
| **C. Pi Zero W wireless keyer** | Wi-Fi UDP | ~$25–35 | 10–30 ms | 1–2 hr | Pick "Pi Zero Wireless" |

Quick decision tree:

- **Want the cleanest sending diagnostics?** Build A. The firmware timestamps each edge before USB jitter, so the trainer's dit/dah variance and consistency scores reflect *your* timing, not the USB stack's.
- **Want it to work with any Morse software, no app config?** Build B. The Arduino emulates a USB keyboard pressing Space; every Morse trainer that watches for spacebar will work, including the bundled one and free web apps like LCWO.
- **Want a cordless desk-friendly setup?** Build C. Wi-Fi UDP from a Pi Zero W. Optional battery telemetry shows the charge level in the trainer's input source label.

All three designs accept either a **straight key** or an **iambic paddle**, and all three implement iambic mode A or B in firmware. Mode is selected over the wire (serial or UDP), so you don't re-flash to switch.

## Common parts (all designs)

- 1× key — straight key (MFJ-553, Bencher RJ-1) or paddle (Bencher BY-2, Begali Simplex, etc.)
- 1× 1/4" or 3.5 mm stereo jack matching your key's plug
- Hookup wire (22 AWG)
- Soldering iron + solder
- Small enclosure — `morse-trainer/hardware/enclosure/keyer-box.scad` is parametric for either an Arduino Pro Micro **or** a Pi Zero (set the `BOARD` variable, render with OpenSCAD)

Optional sidetone (any design): a passive piezo buzzer + 1 kΩ series resistor.

## Design A — Arduino USB serial keyer

Any AVR or RP2040 board with USB serial works (Pro Micro, Leonardo, Uno, Nano, etc.).

Wiring summary (full diagram in `morse-trainer/hardware/enclosure/wiring-diagram.svg`):

```
Straight key:                Paddle (iambic):

   key ─┬── tip ─── D2          paddle ─┬── tip ─── D2  (dit)
        └── slv ── GND                  ├── ring ── D3  (dah)
                                        └── slv ── GND
```

`INPUT_PULLUP` is enabled in firmware, so no external pull-up resistors are needed.

Flash `morse-trainer/hardware/arduino/morse_trainer_keyer.ino` from the Arduino IDE and that's it. The firmware accepts simple text commands at 115200 baud:

```
MODE STRAIGHT
MODE PADDLE_A
MODE PADDLE_B
WPM 22
SIDETONE OFF
```

In the trainer's **Settings**, set `arduinoPort` to the device path (`/dev/ttyACM0` on Linux, `/dev/tty.usbmodem*` on macOS, `COMx` on Windows) and pick **Arduino USB** as the input source.

> ⚙️ **Advanced —** The serial protocol uses `DOWN`, `UP`, and `ELEM` packets with millisecond-resolution timestamps from the Arduino's `micros()` clock. Edges are timestamped at the ISR level, so USB-side latency doesn't pollute the dit/dah measurement. Full packet format is in the comment block at the top of `morse_trainer_keyer.ino`.

## Design B — Arduino USB HID keyer

Same wiring as Design A. The firmware (`morse_trainer_hid.ino`) emulates a USB keyboard pressing **Space**, so the trainer (or any Morse software watching the spacebar) sees keystrokes as if you were typing.

Constraint: the board must support USB HID — that means **ATmega32U4** (Pro Micro, Leonardo) or **RP2040 with TinyUSB**. Plain Uno or Nano won't work.

In the trainer, choose **Keyboard (Space)** as the input source. No port selection, no driver setup.

> ⚠️ While the keyer is plugged in, every key press will type a space *into whatever window has focus*. Unplug it when you're not practicing, or wire a hardware switch into the key line.

## Design C — Pi Zero W wireless keyer

Bill of materials:

- Raspberry Pi Zero W (or Zero 2 W) — Wi-Fi required
- microSD 8 GB+ with Raspberry Pi OS Lite
- USB power pack, or LiPo + boost converter for portability
- 3.5 mm or 1/4" stereo jack
- *(optional)* Adafruit MAX17048 LiPoly fuel gauge for I2C battery telemetry
- *(optional)* Passive piezo on GPIO 18 for on-board sidetone

Wiring (BCM pin numbering):

```
Straight key:                Paddle (iambic):

   key ─┬── tip ─── GPIO 17    paddle ─┬── tip ─── GPIO 17  (dit)
        └── slv ── GND                 ├── ring ── GPIO 27  (dah)
                                       └── slv ── GND
```

Setup:

```bash
sudo apt update
sudo apt install python3-gpiozero
# copy hardware/pizero/ from this repo onto the Pi, then:

python3 ~/morse-trainer/hardware/pizero/keyer.py \
    --mode paddle_a --wpm 22 --sidetone --host 192.168.1.10 --port 51234
```

Replace `192.168.1.10` with the IP of the laptop running the desktop trainer.

For auto-start on boot, the included `morse-keyer.service` systemd unit is ready to drop into `/etc/systemd/system/`:

```bash
sudo cp ~/morse-trainer/hardware/pizero/morse-keyer.service /etc/systemd/system/
sudo nano /etc/systemd/system/morse-keyer.service       # edit --host / --mode
sudo systemctl daemon-reload
sudo systemctl enable --now morse-keyer
```

In the trainer's **Settings**, set `pizeroUdpPort` (default 51234), make sure the laptop's firewall allows that UDP port, and pick **Pi Zero Wireless** as the input source.

> ⚙️ **Advanced —** If you'd rather use Bluetooth than Wi-Fi, the Java side exposes `PiZeroKeyer.injectEvent(KeyEvent)`. A small bridge (e.g. [Bleak](https://github.com/hbldh/bleak) subscribing to a GATT characteristic on the Pi) can call `injectEvent` for each notification — the decoder doesn't care which transport delivered the event.

## Latency comparison

Measured on a 2024 laptop, average of 1000 events:

| Path | Median latency | Jitter (p95) | Notes |
|------|----------------|--------------|-------|
| Keyboard (USB HID) | 4 ms | 12 ms | OS keystroke pipeline |
| Arduino HID firmware | 3 ms | 7 ms | Same path as above, hardware key |
| Arduino serial firmware | 5 ms | 9 ms | Includes serial parse on host |
| Pi Zero W (5 GHz Wi-Fi) | 14 ms | 28 ms | Stable network |
| Pi Zero W (2.4 GHz Wi-Fi) | 22 ms | 65 ms | Congested network |

For accurate sending diagnostics — especially dit/dah variance and consistency scoring — prefer the Arduino serial firmware. It timestamps edges in firmware before USB jitter, so the host sees the original timing even if the USB read is delayed.

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| Linux: app says "cannot open /dev/ttyACM0" | `sudo usermod -aG dialout $USER` and log out / back in |
| Paddle dits and dahs swapped | Swap tip and ring at the jack, **or** flip `KEY_DIT_PIN` / `KEY_DAH_PIN` in firmware |
| No sidetone from Pi Zero | Install `python3-rpi.gpio`; piezo must be on a PWM-capable pin (GPIO 18) |
| Iambic feels "slippy" (extra elements) | Try the other mode. A vs B is personal preference; B is more forgiving of late releases |
| HID firmware: every keypress shows up in other apps | By design — it's a keyboard. Add a physical switch in the key line, or use Design A instead |

## See also

- §03-00 — Morse overview (launches the trainer)
- §03-06 — Send practice (the trainer mode that uses these keyers)
- §03-07 — Speed tracking (where the trainer's diagnostics live)
- `morse-trainer/hardware/README.md` — full build guide
- `morse-trainer/hardware/enclosure/wiring-diagram.svg` — reference wiring diagram
- `morse-trainer/hardware/enclosure/keyer-box.scad` — 3D-printable enclosure
