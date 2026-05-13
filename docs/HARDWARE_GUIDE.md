# Hardware Connection Guide

This guide explains how to plug your ham radio gear into your computer so the WM3J ARS Suite (J-Hub, J-Log, and friends) can talk to it. It is written for people who are new to this. You do not need to know anything about software or wiring before you start.

If a word looks new, do not worry. We will explain it the first time we use it.

---

## Table of contents

1. [The big picture](#1-the-big-picture)
2. [Words you will see a lot](#2-words-you-will-see-a-lot)
3. [What kind of cable do I need?](#3-what-kind-of-cable-do-i-need)
4. [Connecting your radio (rig)](#4-connecting-your-radio-rig)
5. [Connecting your antenna rotator](#5-connecting-your-antenna-rotator)
6. [Connecting your amplifier](#6-connecting-your-amplifier)
7. [Connecting your antenna switch](#7-connecting-your-antenna-switch)
8. [Sound — for SSB and digital modes](#8-sound--for-ssb-and-digital-modes)
9. [USB hubs — when to use one](#9-usb-hubs--when-to-use-one)
10. [Stable port names — so things don't get mixed up](#10-stable-port-names--so-things-dont-get-mixed-up)
11. [Putting your computer far from the shack (network serial)](#11-putting-your-computer-far-from-the-shack-network-serial)
12. [A simple shopping list](#12-a-simple-shopping-list)
13. [Common problems and fixes](#13-common-problems-and-fixes)

---

## 1. The big picture

Your computer cannot send a signal straight to a radio knob. It needs a path. The path looks like this:

```
[ Your PC ]
     │  (USB cable)
     ▼
[ Adapter or built-in USB chip ]
     │  (serial signal)
     ▼
[ Control port on the device ]
     │
     ▼
[ The device does the thing — change frequency, turn the rotor, key the amp ]
```

For each device you want to control — radio, rotator, amplifier, antenna switch — you need one of these paths.

A small program on the computer, called a **daemon**, opens the port and listens for commands. Then J-Hub sends the daemon a command like "tune to 14.250 MHz" and the daemon turns that into the right wiggle of voltage on the cable.

For most of your gear, that daemon is part of a project called **Hamlib**. Hamlib has three of them:

| Daemon | What it controls | Listens on |
|--------|------------------|------------|
| `rigctld` | Your radio (rig) | TCP port 4532 |
| `rotctld` | Your antenna rotator | TCP port 4533 |
| `ampctld` | Your amplifier | TCP port 4531 |

For antenna switches, J-Hub talks to the switch directly over the serial port — there is no Hamlib daemon for switches.

You do not need to start these daemons by hand on Linux if you set them up to start when the computer boots. On Windows you may run them from a shortcut. The User Guide has the exact commands.

---

## 2. Words you will see a lot

| Word | What it means in plain English |
|------|--------------------------------|
| **USB** | The flat or oval plug everyone knows. Carries power and data. |
| **Serial port** | An older style of port that sends data one bit at a time. Looks like a 9-pin "D" shape (called DB-9). Modern radios have built-in serial inside; you just see a USB jack. |
| **USB-to-serial adapter** | A small dongle that plugs into USB on one end and has a serial plug on the other. It tricks the computer into thinking it has a real serial port. |
| **CAT** | "Computer Aided Tuning." The set of commands a radio understands so a PC can change its frequency, mode, and so on. Each radio brand uses a slightly different CAT language. Hamlib hides those differences. |
| **CI-V** | The CAT system Icom radios use. Sends data over a 3.5 mm headphone-style plug. The voltage levels are different from a normal serial port, so Icom radios usually need a small **level converter** in the middle. |
| **PTT** | "Push to Talk." A signal that tells the radio to start transmitting. Some radios accept PTT over the CAT cable; some need a separate wire. |
| **TTL levels** | A type of electrical signal used by tiny chips. Lower voltage than a normal serial port. If you connect TTL straight to a real serial port you can damage things. Always check before you wire something up. |
| **FTDI** | A brand of chip that goes inside good USB-to-serial adapters. FTDI chips are reliable. Cheap clones cause headaches. |
| **COM port** | Windows' name for a serial port. It looks like `COM3` or `COM7`. |
| **/dev/ttyUSB0** | Linux's name for the same thing. The number can change if you unplug things. |
| **udev rule** | A short text file on Linux that gives your USB-to-serial adapter a fixed name like `/dev/ttyRig` so it does not change between reboots. We will show you one. |

---

## 3. What kind of cable do I need?

This depends on the device. The short answer:

- **Modern radios** (made roughly after 2010): one USB cable. You may need to install a driver on Windows the first time. On Linux it usually just works.
- **Older radios with a DB-9 serial CAT port:** a USB-to-serial adapter, plus the cable that came with the radio (or a generic 9-pin straight-through cable).
- **Icom radios with a CI-V (3.5 mm) jack only:** a USB-to-serial adapter **and** a CI-V level converter such as the Icom CT-17. Or buy a single dongle that already has the CI-V converter built in.
- **Antenna rotators:** almost always DB-9 serial → USB-to-serial adapter. A few new ones have USB or Ethernet built in.
- **Amplifiers:** depends on the brand. The newer Elecraft amps have built-in USB. Most ACOM and Ameritron amps use DB-9 serial → USB-to-serial.
- **Antenna switches:** depends on the brand. Most use DB-9 serial. Some bare relay boards use USB.

When in doubt, look at the back panel of the device. Count the pins. If you see a 9-pin "D"-shaped plug labeled "RS-232" or "CAT" or "Computer," you need a USB-to-serial adapter.

**Buy good adapters.** Cheap adapters from no-name sellers often use a chip called a "Prolific" knock-off. Drivers fight with the operating system. Spend $10 more and get one with an FTDI chip. Brands like StarTech, Tripp Lite, Sabrent (the ones that say FTDI), and Plugable are safe bets.

---

## 4. Connecting your radio (rig)

### 4.1 Modern radios with built-in USB

This is the easy case. Examples:

- Icom IC-7300, IC-7610, IC-9700, IC-705
- Yaesu FT-991A, FT-DX10, FT-710
- Kenwood TS-590S/SG, TS-890, TS-990
- FlexRadio (uses Ethernet, not USB, but the idea is the same)
- Most newer Elecraft K3S and K4

**Steps:**

1. Plug a USB cable from the back of the radio to your PC.
2. The PC will see one or two new devices appear. On Linux they show up as `/dev/ttyUSB0` and maybe `/dev/ttyUSB1`. On Windows they show up as `COM4` and `COM5` (numbers vary).
3. If it does not appear on Windows, go to the radio's manufacturer website and download the USB driver. Install it, then plug in the radio again.
4. In the radio's menu, set the CAT speed (sometimes called "baud rate"). 19200 or 38400 are common. Write down what you pick — you will need it again.
5. In the radio's menu, set the CAT address if it asks. Icom default is `94`. Yaesu and Kenwood do not ask.
6. Start `rigctld` with the right model number and the port you saw in step 2:

   Linux:
   ```bash
   rigctld -m 3081 -r /dev/ttyUSB0 -s 19200
   ```
   Windows (in a Command Prompt):
   ```
   rigctld.exe -m 3081 -r COM4 -s 19200
   ```
   Replace `3081` with **your** radio's Hamlib model number. Run `rigctld -l` to see the full list — it is long.

7. Open the J-Hub web page (http://localhost:8081), go to the **Rig Control** tab, set the backend to **Hamlib**, host to `localhost`, port to `4532`, and click **Save**. Within a few seconds you should see a frequency reading appear.

### 4.2 Older radios with a DB-9 CAT port

Examples: many older Yaesu, Kenwood, and Ten-Tec rigs. Also Icom rigs that come with a CI-V to RS-232 adapter cable.

**Steps:**

1. Buy a USB-to-serial adapter with an FTDI chip.
2. Connect the adapter's serial end to the radio's CAT port using the cable that came with the radio (or a 9-pin straight-through cable).
3. Plug the USB end into your PC.
4. The PC sees a new serial port. On Linux it is usually `/dev/ttyUSB0`. On Windows it is `COMn` where n is some number.
5. Continue with steps 4–7 from section 4.1.

### 4.3 Icom rigs with only a CI-V (3.5 mm) jack

Examples: IC-706, IC-718, IC-746, IC-7000, and many more.

The CI-V port uses a small voltage that a regular serial port cannot read directly. You need a level converter.

**Two ways to do this:**

**A. Use a real Icom CT-17 (or a CT-17 clone):**

1. Buy a CT-17 (or clone). It has a USB-to-serial part and the CI-V converter inside.
2. Plug the USB end into your PC. Plug the 3.5 mm end into the radio's CI-V jack.
3. Done — it is a single dongle.

**B. Use a regular FTDI USB-to-serial adapter plus a small CI-V interface:**

1. Buy a USB-to-serial adapter (FTDI).
2. Buy or build a small CI-V interface that goes between the serial port and the radio. This is a small box with a transistor or two.
3. Wire it up per the instructions that come with it.

For most people, option A is easier and worth the small extra cost.

After it is wired up, follow steps 4–7 from section 4.1. Use the right Icom model number for your rig (run `rigctld -l | grep -i icom` to find it).

### 4.4 Where does PTT come from?

PTT means "tell the radio to transmit." You have three choices:

1. **CAT PTT.** The Hamlib command `T 1` keys the radio over the CAT cable. Works on most modern rigs. This is what J-Hub uses by default. No extra wiring needed.
2. **VOX.** The radio listens to its mic input and starts transmitting when it hears sound. Works for voice macros. No wiring needed but slow to react and can clip the start of audio.
3. **Hardware PTT line.** A separate wire (usually from a sound interface like a SignaLink) keys the radio. Best for digital modes that need precise timing.

If you are just starting out, leave it on CAT PTT (option 1). Switch to a hardware PTT line later if you have timing trouble.

---

## 5. Connecting your antenna rotator

Most rotators have a separate **controller box** that sits on your desk. The rotator itself is up on the tower. You always plug the computer into the controller box, not the rotator.

### 5.1 Yaesu G-450 / G-800 / G-1000 / G-2800 with the GS-232 interface

1. The GS-232 box (sometimes built into the controller) has a DB-9 serial port labeled "Computer."
2. Connect a USB-to-serial adapter from your PC to that port.
3. Start `rotctld`:
   ```bash
   rotctld -m 603 -r /dev/ttyUSB1 -s 9600
   ```
   `603` is the Hamlib model number for the GS-232A. Use `601` for the older GS-232 protocol.
4. In the J-Hub web UI, go to **Rotor Control**, set backend to **Hamlib**, host `localhost`, port `4533`, click **Save**. The compass display should start showing your current heading.

### 5.2 Hy-Gain DCU-1 / Tail Twister (T2X) / Ham IV

Same pattern. Use Hamlib model `1101` for DCU-1, `1102` for AlfaSpid, etc.

### 5.3 Green Heron RT-21

This is one of the few rotators with USB built in. Plug it in. Use Hamlib model `1404`. No adapter needed.

### 5.4 Heavy duty (Prosistel, Orion, Yaesu G-2800DXC)

Same as 5.1 — DB-9 serial → USB-serial adapter — but check the model number list because there are several.

---

## 6. Connecting your amplifier

Amplifier control is more recent than rig control, so older amps simply cannot be controlled from the computer. Check the back of your amp for a port labeled "Remote," "Computer," or "RS-232."

### 6.1 Elecraft KPA1500

Has built-in USB. Plug in one cable. Start `ampctld`:
```bash
ampctld -m 4 -r /dev/ttyUSB2 -s 38400
```
In J-Hub: **Amp Control** tab, backend **Hamlib ampctld**, host `localhost`, port `4531`, click **Save**.

### 6.2 ACOM 600S / 700S / 1500 / 2000A

ACOM ships a serial cable in the box. Use it with a USB-to-serial adapter.
- ACOM 600S: model `5`
- ACOM 1500: model `5` (same protocol)
- ACOM 2000A: model `1`

### 6.3 Ameritron ALS-1306 / ALS-1300

Some come with a USB module, some need an add-on. Check the manual.

### 6.4 Old tube amps (no remote port)

If your amp has no computer port at all, you cannot control it from software. You can still get **band-follow** to work by adding a relay board that reads BCD band data from the rig and switches the amp's input filters — but that is a project for another day.

---

## 7. Connecting your antenna switch

J-Hub talks to switches over a plain serial port. There is no Hamlib daemon for switches. The exact command bytes are different for each switch, so the **Antenna Switch** tab in J-Hub lets you write a "command template" with `{switch}` and `{antenna}` placeholders.

### 7.1 DX Engineering RR8B-MS

DB-9 serial → USB-to-serial adapter. The command template is `SW{switch}={antenna}\r`.

### 7.2 Microham µStation

Built-in USB. Different commands per model — check the µStation manual.

### 7.3 ARCO RC-1A

This one talks over Ethernet (TCP), not serial. J-Hub today only supports serial switches; let us know if you have one and we will add TCP support.

### 7.4 Bare relay boards (Numato, KMTronic, DLP)

These are simple boards with USB on one end and 4 or 8 relay outputs. You wire each relay to one of your antenna feedlines. Use the command template field to send whatever the board's data sheet says — for a Numato 8-channel relay it is something like `relay on {antenna}\r`.

### 7.5 Adding a switch in J-Hub

1. Go to **Antenna Switch** tab.
2. Check **Enable automatic switching**.
3. Fill in the **Serial Port** (e.g., `/dev/ttyUSB3` or `COM7`) and **Baud** (usually 9600).
4. Click **+ Add Switch**, give it a friendly ID like `main` and a name like `Tower 1`. Set the antenna count.
5. Click **+ Add Rule**. Pick a band, optionally a mode and heading window, the switch ID, and which antenna should be selected. Set the command template.
6. Click **Save Switches & Rules**. The first matching rule (top to bottom) wins, so put more-specific rules above less-specific ones.

---

## 8. Sound — for SSB and digital modes

CAT control turns knobs but does not move audio. For voice macros, FT8, RTTY, and other modes that need sound to or from the radio, you need an audio path too.

### 8.1 Built-in USB audio (modern radios)

If your radio shows up on the PC as both a serial port **and** a sound card, you are done. Pick that sound card in J-Digi or in WSJT-X and you are ready to transmit and receive audio.

Typical examples:
- **Icom IC-7300 / IC-705 / IC-7610** — single USB cable carries CAT + audio in + audio out
- **Yaesu FT-991A / FT-DX10 / FTDX-101D** — same
- **Kenwood TS-590SG / TS-890** — built-in USB sound card

Pick it in J-Digi's **Audio** menu (look for "USB Audio CODEC" or the rig's model name) and in WSJT-X's **Settings → Audio**.

### 8.2 SignaLink USB

Tigertronics' SignaLink is the most common external audio interface in the hobby. It handles audio in, audio out, and PTT (via audio-sensing — no CAT cable needed for PTT).

Plug it in. It shows up as **"USB Audio CODEC"** (yes, identical to a built-in USB radio — check the device count to tell them apart). One DIN-8 / DIN-13 / RJ-45 cable to the rig (model-specific). One USB cable to the PC. Done.

**Settings:**
- Set the SignaLink's **DLY** knob just past minimum so PTT releases promptly after each transmission.
- Set **RX** and **TX** knobs to roughly 9 o'clock as a starting point; adjust by watching levels in J-Digi's spectrum/decoder.
- PTT in J-Digi: leave at **VOX** (the default). The SignaLink keys the rig when it sees audio; you don't need Hamlib PTT.

### 8.3 DigiRig Mobile / DigiRig Lite

DigiRig is a newer, smaller alternative to SignaLink — a single USB-C device that exposes a serial port (for CAT) **and** a sound card (for audio), with hardware PTT triggered from the CAT side.

After plugging in, you'll see:
- A serial port (Linux: `/dev/ttyUSB0`, Windows: `COM3` etc.) — point Hamlib `rigctld` at this for CAT
- A sound card called **"USB Audio CODEC"** — pick it in J-Digi / WSJT-X

PTT options:
- **VOX** (default in J-Digi) — DigiRig keys the rig when audio appears. Simplest.
- **HAMLIB** — J-Digi sends `T 1` / `T 0` to `rigctld`, which keys DigiRig's PTT line. Required for pure CW since there's no audio for VOX to sense. Set in **J-Hub → J-Digi → Transmit & CW → PTT Method**.

### 8.4 RIGblaster / microHAM USB Interface III

Older external boxes; same audio idea as SignaLink. Single USB connection, PTT via audio sensing or via a separate keying line. Treat them like a SignaLink for purposes of this guide.

### 8.5 Virtual audio cables — when you need them

The default audio routing for a typical shack is:

```
   Radio audio out ───▶ [sound card]  ───▶  one digital app
   one digital app ───▶ [sound card]  ───▶  Radio audio in
```

You hit a wall the moment you want **two digital apps to share the same audio simultaneously** — say, WSJT-X decoding FT8 while a logging spotter watches the same audio, or J-Digi monitoring RTTY while you also feed it to a recording app.

The fix: **virtual audio cables**. These are software-only "wires" that look like a sound card to applications but route to/from another application instead of physical hardware. They cost nothing on Linux/macOS and very little on Windows.

#### 8.5.1 Linux — PulseAudio / PipeWire loopback

Linux ships with this built in. Create a loopback device that any number of applications can record from:

```bash
# PulseAudio (most distros pre-PipeWire):
pactl load-module module-null-sink sink_name=jdigi_loop sink_properties=device.description=JDigiLoop

# PipeWire (Fedora 35+, Ubuntu 22.10+, Arch):
pactl load-module module-null-sink media.class=Audio/Sink sink_name=jdigi_loop
```

Then route your radio's audio into it via `pavucontrol` (or `qpwgraph` on PipeWire), and have J-Digi / WSJT-X / anything else read from `jdigi_loop.monitor`.

This is the exact setup J-Digi uses for its loopback test suite — see the [J-Digi CLAUDE.md](../j-digi/CLAUDE.md) for the canonical recipe.

To make it persistent across reboots, drop the `pactl` line into `~/.config/pulse/default.pa` (PulseAudio) or `~/.config/pipewire/pipewire.conf.d/` (PipeWire).

#### 8.5.2 Windows — VB-Cable

Windows has nothing built in. Free third-party options:

- **VB-Audio Virtual Cable** — `https://vb-audio.com/Cable/` — one virtual cable, free, donationware. Most operators only need one cable.
- **VB-Audio VoiceMeeter Banana** — same vendor; full mixer with multiple cables. Free, more complex. Use if you need three or more virtual channels.

**Install VB-Cable:**

1. Download the ZIP from vb-audio.com, extract it.
2. Right-click `VBCABLE_Setup_x64.exe` (or `_Setup.exe` for 32-bit), select **Run as administrator**.
3. Click **Install Driver**. Reboot when it asks.
4. Open **Sound Settings** (right-click the speaker icon → Sound settings):
   - You should now see **"CABLE Input"** in the playback list and **"CABLE Output"** in the recording list.

**Wire your radio into VB-Cable:**

5. Set the radio's sound card output as the recording source for VB-Cable using the **"Listen to this device"** trick:
   - Recording → right-click your radio's USB Audio CODEC → **Properties → Listen tab → Listen to this device** → playback through "CABLE Input".
6. Now anything that reads from **CABLE Output** sees the radio's audio.

**Configure each app:**

7. In **WSJT-X**: Settings → Audio → Input = **CABLE Output**; Output = your radio's USB Audio CODEC (direct).
8. In **J-Digi**: Audio menu → Input = **CABLE Output**; Output = your radio's USB Audio CODEC.

Both apps now decode the same audio in parallel. Transmit still goes direct to the radio (not through the cable) so you don't get double-routing.

#### 8.5.3 macOS — BlackHole

Apple removed Soundflower years ago; the modern free replacement is **BlackHole** from Existential Audio.

1. Download from `https://existential.audio/blackhole/` (free, open source, no account needed). Pick the **2ch** edition unless you specifically need 16 channels.
2. Open the `.pkg` installer, accept the prompts. No reboot needed.
3. Open **Audio MIDI Setup** (Spotlight → "Audio MIDI Setup"):
   - You should see **"BlackHole 2ch"** in the device list.
4. Create a **Multi-Output Device** so the radio's audio reaches both your speakers (so you can hear it) and BlackHole:
   - Click `+` (bottom-left) → **Create Multi-Output Device**.
   - Tick both "Built-in Output" (your speakers/headphones) and "BlackHole 2ch".
   - Rename it to "Radio + BlackHole".
5. In **System Settings → Sound → Output**, set the system output to the Multi-Output Device when you want both speakers and apps to hear the radio. (Or leave default and pick per-app.)
6. **In each digital-mode app**, set the audio input to **BlackHole 2ch**.

**Paid alternative:** *Loopback* by Rogue Amoeba (~$99) — much friendlier UI, lets you build named virtual cables with a drag-and-drop graph. Worth it if you're running a complex setup; overkill for a single FT8 + J-Digi split.

### 8.6 Levels, sample rate, and common pitfalls

A few rules that save hours of confusion regardless of which interface you use:

| Knob | Setting |
|---|---|
| **Sample rate** (PC + radio) | **48 000 Hz** — everything in the digital-mode world expects this. If the radio is set to 44.1 kHz the decoders will work but timing is slightly off. |
| **RX audio level** | Aim for the decoder's "happy zone" — for WSJT-X that's ~30 dB on the meter; for J-Digi the waterfall should show clear signals without hot peaks. Too loud = ALC compresses + signals smear; too quiet = decoder misses weak ones. |
| **TX audio level** | Set the rig so ALC just barely flickers on peaks. More than that means audio is over-driving the rig and your signal will splatter. |
| **PC mic boost** | **Off**. The "boost" or "AGC" toggle on the PC's recording device wrecks dynamic range for digital modes. |
| **PC monitor / echo cancellation** | **Off**. Same reason — anything that thinks it's "improving" voice audio destroys digital decoding. |
| **CPU power profile** | "Performance" or "High" — power-save profiles cause USB-audio dropouts that show up as missed decodes. |

### 8.7 If you only need voice macros

The voice macros in J-Hub record from the PC's default microphone and play back through the PC's default speaker. If your radio has a mic input that accepts line-level sound, run a wire from the PC's headphone jack (or USB sound card output) to the radio's mic input. PTT goes through the CAT cable.

A safer option is one of the boxes in 8.2 or 8.3 — they include the right resistors so you don't blow up the radio's mic preamp.

---

## 9. USB hubs — when to use one

If you have more than three devices to plug in (say: rig, rotator, amp, switch, sound interface), you will run out of USB ports on the PC. Use a hub.

**Three rules for picking a USB hub:**

1. **Use a powered hub.** It plugs into a wall outlet. A bus-powered hub (no wall plug) shares the PC's USB power across every port and falls over when several devices wake up at the same time. Cheap, frustrating, and avoidable.
2. **Get USB 2.0, not just USB 3.0.** All your radio gear runs at USB 1.1 or 2.0 speeds. Some USB 3.0 hubs cause weird radio-frequency noise that gets into your receiver. A boring USB 2.0 hub is quieter.
3. **Keep the hub away from the radio.** A few feet of cable between the hub and the rig helps.

A 7-port powered USB 2.0 hub costs about $25 and lasts a long time.

---

## 10. Stable port names — so things don't get mixed up

This is the most important section in this document.

If you plug your rig, rotator, and amp into a USB hub in a different order tomorrow, your computer might call them by different names. Linux might decide your rig is now `/dev/ttyUSB1` instead of `/dev/ttyUSB0`. Windows might number them as `COM6` instead of `COM4`. J-Hub will fail to connect. You will be confused. You will swear.

Fix it once, never deal with it again.

### 10.1 Linux: udev rules

Each USB-to-serial adapter has a unique serial number burned into the chip. We can write a small file that says, "whenever this serial number shows up, give it the name `/dev/ttyRig`."

**Steps:**

1. Plug in your rig adapter alone. Find its serial number:
   ```bash
   udevadm info -a -n /dev/ttyUSB0 | grep '{serial}' | head -1
   ```
   You will see something like `ATTRS{serial}=="A50285BI"`. Write that down.
2. Repeat for each adapter (rotator, amp, switch). Plug them in one at a time so you know which serial number is which.
3. Open a new file as root:
   ```bash
   sudo nano /etc/udev/rules.d/99-ham-radio.rules
   ```
4. Put your rules in:
   ```
   SUBSYSTEM=="tty", ATTRS{serial}=="A50285BI", SYMLINK+="ttyRig"
   SUBSYSTEM=="tty", ATTRS{serial}=="A1009X3F", SYMLINK+="ttyRot"
   SUBSYSTEM=="tty", ATTRS{serial}=="AB0LL5DF", SYMLINK+="ttyAmp"
   SUBSYSTEM=="tty", ATTRS{serial}=="A902V47K", SYMLINK+="ttySw"
   ```
   Save and close (in nano: Ctrl-O, Enter, Ctrl-X).
5. Reload the rules:
   ```bash
   sudo udevadm control --reload-rules
   sudo udevadm trigger
   ```
6. Now you have stable names: `/dev/ttyRig`, `/dev/ttyRot`, etc. Use those in J-Hub config and in your `rigctld -r` commands.

### 10.2 Windows: stable COM port assignment

Windows tries to keep COM port assignments stable per USB port (the physical socket on the hub or PC). If you always plug each adapter into the same socket, you get the same number.

If Windows insists on shuffling them anyway:

1. Open Device Manager → Ports (COM & LPT).
2. Right-click the adapter → Properties → Port Settings → Advanced.
3. Set **COM Port Number** to a high number (like COM20) so it never collides with anything Windows wants to assign on its own.

Repeat for each adapter, picking a different number each time. Write down which number is which. Use those in J-Hub config.

---

## 11. Putting your computer far from the shack (network serial)

If your shack is in the basement and your PC is in the living room, you have two options.

**Option A — long USB.** USB cables longer than 15 feet (5 m) are unreliable. You can buy "active" USB extenders that go up to 65 feet, or USB-over-Ethernet hubs that go anywhere your house network reaches.

**Option B — serial-over-Ethernet.** A small box (Lantronix UDS-1100, Moxa NPort 5110, Digi PortServer TS) sits in the shack with all your USB-to-serial adapters plugged into it. It exposes each serial port over the network. On the PC side, you point Hamlib at a TCP address instead of a local COM port:

```bash
rigctld -m 3081 -r tcp://192.168.1.50:10001 -s 19200
```

Cleaner if you have many devices, more parts to fail. Worth it for a permanent install where the shack is somewhere awkward.

---

## 12. A simple shopping list

For a typical small station with one rig, one rotator, one amp, and one antenna switch:

| Item | Why | Cost (USD, approx) |
|------|-----|--------------------|
| Powered USB 2.0 hub, 7-port | Plug everything in | $25 |
| 4 × FTDI-based USB-to-serial adapters | One per device with a DB-9 | $15 each |
| 1 × short USB cable | For the modern rig that has built-in USB | $5 |
| Sound interface (SignaLink USB or built-in radio audio) | For SSB and digital modes | $0–$120 |
| 1 × Cat 5 patch cable | If you go the network-serial route | $5 |
| Wall outlet near the radio | For the powered hub | — |

**Total for a basic setup:** $90–$210.

---

## 13. Common problems and fixes

### "rigctld can't open the port"

- Wrong port name. On Linux, run `ls /dev/ttyUSB*` and try each. On Windows, look in Device Manager.
- Another program is already using it. Close J-Bridge, WSJT-X, or any other software that might have grabbed the port.
- On Linux, you may need to add yourself to the `dialout` group: `sudo usermod -aG dialout $USER`. Log out and back in.

### "Connected but no frequency shows up"

- Wrong baud rate. The number you set in `rigctld -s` must match what the radio's menu says.
- Wrong CI-V address (Icom only). Check the radio menu; default is `94` hex, which is `148` decimal. Pass it on the command line: `rigctld -m 3081 -r /dev/ttyUSB0 -s 19200 --civaddr=0x94`.

### "Frequency updates but PTT does nothing"

- Some rigs need PTT enabled in their menu before CAT can key them. Look for "RTTY/Data" or "USB PTT" or similar settings.
- Try a different `--ptt-type` setting in `rigctld`: `--ptt-type=RIG` (key over CAT), `--ptt-type=DTR` (key by raising the DTR pin), `--ptt-type=RTS` (key by raising the RTS pin).

### "It works for a while, then disconnects"

- Cheap USB-to-serial adapter. Replace it with an FTDI-based one.
- Bus-powered USB hub. Get a powered one.
- Bad USB cable. Try another one.
- Strong RF getting into the cable. Wind the USB cable through a ferrite choke, or shorten it.

### "Numbers shuffle around when I reboot"

- Set up udev rules (Linux) or stable COM numbers (Windows). See section 10.

### "I hear a buzz on receive when the PC is connected"

- USB 3.0 hubs are noisy. Switch to USB 2.0.
- Run the USB cable through a clip-on ferrite (about $1 at Radio Shack-style stores).
- Ground your PC and your radio to the same ground point.

### "Two rotators on one PC?"

- No problem. Run two `rotctld` processes on different ports: one on 4533, one on 4534. J-Hub today connects to one rotator. If you need two, file a feature request.

---

## You are done

If you can plug in a USB cable, install a small driver, and edit one text file with stable port names, you are 90% of the way to a working station. The rest is in the J-Hub web UI, where you fill in the host and port for each daemon and click Save. The User Guide covers that part.

Welcome to the hobby. Have fun.
