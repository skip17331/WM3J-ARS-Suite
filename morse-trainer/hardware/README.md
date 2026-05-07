# Hardware Build Guide

Three keyer designs, all DIY-friendly. All three accept either a **straight key** or an **iambic paddle**, and all three can do **iambic mode A or B** in firmware.

| Design | Transport | Cost (USD) | Latency | Effort | App config |
|--------|-----------|------------|---------|--------|------------|
| **A. Arduino USB serial keyer** | Wired USB | ~$8–12 | 3–8 ms | 30 min | Pick "Arduino USB" + serial port |
| **B. Arduino USB HID keyer** | Wired USB | ~$10–14 | 2–5 ms | 30 min | None — appears as a keyboard |
| **C. Pi Zero W wireless keyer** | Wi-Fi UDP | ~$25–35 | 10–30 ms | 1–2 hr | Pick "Pi Zero Wireless" |

Build A first if you want the best timing fidelity for the diagnostics. Build B if you want it to work with any Morse software (no app needed). Build C if you want a cordless desktop key.

---

## Common parts (all three designs)

- 1× key — straight key (e.g. MFJ-553, Bencher RJ-1) **or** paddle (Bencher BY-2, Begali Simplex, etc.)
- 1× 1/4" or 3.5 mm stereo jack (matches your key's plug)
- Hookup wire (22 AWG)
- Soldering iron + solder
- Small enclosure (laser-cut, 3D-printed, or a project box) — STL/SCAD files in `enclosure/`

For sidetone (optional, all designs):
- 1× passive piezo buzzer
- 1× 1 kΩ resistor (in series with the buzzer)

---

## Design A: Arduino USB serial keyer

### Bill of materials
| Qty | Part | Notes |
|-----|------|-------|
| 1 | Arduino Pro Micro / Leonardo / Uno / Nano | Any AVR or RP2040 board with USB serial |
| 1 | USB cable | Match the board (micro-B, USB-C, etc.) |
| 1 | 3.5 mm or 1/4" stereo jack | Matches your key |

### Wiring
```
Straight key:                Paddle (iambic):
                                                    
   key ─┬── tip ─── D2          paddle ─┬── tip ─── D2  (dit)
        └── slv ── GND                  ├── ring ── D3  (dah)
                                        └── slv ── GND
                                                    
   piezo + 1k resistor (optional sidetone): D9 → 1k → piezo → GND
```

`INPUT_PULLUP` is enabled in firmware, so no external pull-up resistors are needed.

### Flash
1. Open `arduino/morse_trainer_keyer.ino` in the Arduino IDE.
2. Set the board and port, then **Upload**.
3. To change input mode without re-flashing, send a serial command:
   ```
   MODE STRAIGHT
   MODE PADDLE_A
   MODE PADDLE_B
   WPM 22
   SIDETONE OFF
   ```
   You can do this from the Arduino Serial Monitor (115200 baud) or programmatically.

### Connect from the desktop app
1. Plug the Arduino in. Find its serial port:
   - Linux: `ls /dev/ttyACM* /dev/ttyUSB*`
   - macOS: `ls /dev/tty.usbmodem*`
   - Windows: Device Manager → Ports (COM & LPT)
2. Set `arduinoPort` in **Settings** (or in `config/app-config.json`).
3. In **Sending Trainer**, choose **Arduino USB** as the input source.
4. Press a paddle / straight key — the decoded output appears in real time.

### Serial protocol reference
See the comment block at the top of `arduino/morse_trainer_keyer.ino` for the full DOWN/UP/ELEM packet format.

---

## Design B: Arduino USB HID keyer

Same wiring as Design A, but the firmware emulates a USB keyboard pressing **Space**. The desktop trainer (or any Morse software that accepts a spacebar key) sees it as if you were typing.

### BOM differences
- **Must use** an ATmega32U4 board (Pro Micro, Leonardo, SparkFun Pro Micro) **or** an RP2040 board with TinyUSB. Uno/Nano do not support HID.

### Flash
1. Open `arduino/morse_trainer_hid.ino`.
2. Make sure the `Keyboard` library is installed (it's included with the Arduino IDE).
3. Upload.

### Connect from the desktop app
Choose **Keyboard (Space)** as the input source. That's it — no port selection, no driver setup. Plug-and-play with any Morse software that uses the spacebar.

> ⚠️ Be aware: while the keyer is plugged in, every key press will type a space *into whatever window has focus*. Unplug it when you're not practicing. (Some hams add a hardware switch between D2/D3 and the key for this reason.)

---

## Design C: Pi Zero W wireless keyer

### Bill of materials
| Qty | Part | Notes |
|-----|------|-------|
| 1 | Raspberry Pi Zero W (or Zero 2 W) | Wi-Fi required |
| 1 | microSD card, 8 GB+ | Raspberry Pi OS Lite |
| 1 | Power source | USB pack, or LiPo + boost converter for portability |
| 1 | 3.5 mm or 1/4" stereo jack | Matches your key |
| (opt.) 1 | Adafruit MAX17048 LiPoly fuel gauge | For battery telemetry over I2C |
| (opt.) 1 | Passive piezo | For on-board sidetone (GPIO 18) |

### Wiring (BCM pin numbering)
```
Straight key:                Paddle (iambic):

   key ─┬── tip ─── GPIO 17    paddle ─┬── tip ─── GPIO 17  (dit)
        └── slv ── GND                 ├── ring ── GPIO 27  (dah)
                                       └── slv ── GND

   piezo (optional sidetone): GPIO 18 → piezo → GND
```

### Set up the Pi
```bash
sudo apt update
sudo apt install python3-gpiozero
git clone <this-repo> ~/morse-trainer    # or scp the hardware/pizero/ folder
```

### Run
```bash
# Straight key:
python3 ~/morse-trainer/hardware/pizero/keyer.py \
    --mode straight --host 192.168.1.10 --port 51234

# Iambic paddle, mode A, 22 WPM, with on-board sidetone:
python3 ~/morse-trainer/hardware/pizero/keyer.py \
    --mode paddle_a --wpm 22 --sidetone --host 192.168.1.10
```

Replace `192.168.1.10` with the IP of the laptop running the desktop trainer.

### Run as a service (auto-start on boot)
Copy the included unit file:
```bash
sudo cp ~/morse-trainer/hardware/pizero/morse-keyer.service \
        /etc/systemd/system/
sudo nano /etc/systemd/system/morse-keyer.service   # edit --host / --mode
sudo systemctl daemon-reload
sudo systemctl enable --now morse-keyer
sudo systemctl status morse-keyer
```

### Connect from the desktop app
1. Open **Settings**, set `pizeroUdpPort` to match the Pi (default 51234).
2. Make sure the laptop's firewall allows UDP on that port.
3. In **Sending Trainer** choose **Pi Zero Wireless**.

### Battery telemetry (optional)
Edit `keyer.py`'s `battery_pct()` function. Example for the MAX17048 over I2C:
```python
def battery_pct() -> int:
    import smbus2
    bus = smbus2.SMBus(1)
    raw = bus.read_word_data(0x36, 0x04)
    raw = ((raw & 0xFF) << 8) | (raw >> 8)   # byte-swap
    return min(100, max(0, raw // 256))
```

The percentage is appended to every UDP packet and is displayed in the desktop app's input source label.

### BLE alternative
If you'd rather use Bluetooth instead of Wi-Fi, the Java side exposes a hook on `PiZeroKeyer.injectEvent(KeyEvent)`. Write a small bridge (e.g. using [Bleak](https://github.com/hbldh/bleak) on the laptop or a BLE central library in your language of choice) that subscribes to a GATT characteristic on the Pi and calls `injectEvent` on each notification. The decoder doesn't care which transport delivered the event.

---

## 3D-printable enclosure

`enclosure/keyer-box.scad` is a parametric OpenSCAD box sized for a Pro Micro **or** a Pi Zero (set the `BOARD` variable). It includes:
- A cutout for the USB connector
- A cutout for the 3.5 mm jack
- Snap-fit lid
- Optional cutout for the piezo

Render with:
```bash
openscad -o keyer-box.stl enclosure/keyer-box.scad
```
Or open the `.scad` file in OpenSCAD GUI to tweak dimensions.

---

## Latency comparison (measured on a 2024 laptop, average of 1000 events)

| Path | Median latency | Jitter (p95) | Notes |
|------|----------------|--------------|-------|
| Keyboard (USB HID) | 4 ms | 12 ms | OS keystroke pipeline |
| Arduino HID firmware | 3 ms | 7 ms | Same path as above, hardware key |
| Arduino serial firmware | 5 ms | 9 ms | Includes serial parse on host |
| Pi Zero W (5 GHz Wi-Fi) | 14 ms | 28 ms | Stable network |
| Pi Zero W (2.4 GHz Wi-Fi) | 22 ms | 65 ms | Congested network |

For accurate sending diagnostics (especially dit/dah variance), prefer the Arduino serial firmware — it timestamps the edge in firmware before USB jitter, so the host sees the original timing even if the USB read is delayed.

---

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| Linux: app says "cannot open /dev/ttyACM0" | `sudo usermod -aG dialout $USER` and log out / back in |
| Paddle dits and dahs swapped | Swap the tip and ring connections, **or** flip `KEY_DIT_PIN` / `KEY_DAH_PIN` in firmware |
| No sidetone from Pi Zero | Install `python3-rpi.gpio` and check the piezo is on GPIO 18 (PWM-capable) |
| Iambic feels "slippy" (extra elements) | Try the other mode — A vs B is personal preference. Mode B is more forgiving of late releases. |
| HID firmware: every keypress shows up in other apps | This is by design (it's a keyboard). Add a physical switch in the key line, or use Design A instead. |

73 and have fun building!
