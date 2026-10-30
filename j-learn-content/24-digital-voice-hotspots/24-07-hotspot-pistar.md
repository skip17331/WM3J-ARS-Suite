---
id: 24-07
title: Pi-Star Hotspot Setup
chapter: 24
section: 07
level: mixed
status: published
---

# Pi-Star Hotspot Setup

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What it is

**Pi-Star** is the dominant open-source firmware for amateur digital-voice hotspots. It runs on a Raspberry Pi (typically a Pi Zero W, Pi 3A+, or Pi 4) with an **MMDVM HAT** (Multi-Mode Digital Voice Modem — a small radio daughterboard) stacked on top. The whole package is a low-power transceiver running ~10 mW at 70 cm or 2 m that bridges your handheld radio to the global DV networks.

Author: **Andy MW0MWZ** (with a large community of contributors). First release ~2016; current versions support DMR, D-STAR, Fusion (C4FM), NXDN, P25, M17, and POCSAG pager protocols. The dominant fork **WPSD** adds more features and is increasingly popular but is mostly drop-in compatible with Pi-Star workflows.

## Hardware

Three categories of MMDVM-based hotspot hardware:

### Simplex hotspots

A single radio chip, single antenna, simplex operation. The hotspot transmits and receives on the same frequency, half-duplex (only one direction at a time).

Common simplex hotspots:
- **MMDVM Simplex HAT for Pi Zero W** — the canonical $30–$50 board
- **ZUMspot RPi** — popular branded variant
- **Jumbospot** — Chinese-clone variant, cheap (~$25), works but worth less in quality control

### Duplex hotspots

Two radio chips, two antennas, frequency-duplex operation. The hotspot can transmit and receive simultaneously on a different frequency pair — like a real repeater.

Common duplex hotspots:
- **MMDVM Duplex HAT for Pi 3/4** — proper duplex board, ~$80–$120
- **ZUMspot Duplex** — branded version
- **MMDVM-Pi by BI7JTA** — Chinese duplex board, ~$60

See [§24-09](24-09-duplex-vs-simplex-hotspots.md) for the deeper "should I pay double for duplex?" discussion.

### Self-contained units

Boxes that bundle the Pi + HAT + battery + display into one product:

- **OpenSpot 4 / 4 Pro** — by SharkRF (Hungary). Pricier (~$280–$330), proprietary firmware (not Pi-Star), more polished UI. See [§24-08](24-08-hotspot-openspot.md).
- **MMDVM-Cal** — calibration tool, not really a hotspot
- **DV-Mega** — a different MMDVM-compatible HAT, older, less common now

For this section we'll assume Pi-Star running on a Raspberry Pi Zero W with an MMDVM HAT — the standard cheap-but-capable build.

## What "Pi-Star" actually is, technically

Pi-Star is a Raspbian-based OS image preconfigured with:

- **MMDVMHost** — the daemon that talks to the MMDVM HAT over the Pi's serial port. Handles modulation/demodulation and frame routing.
- **YSFGateway / DMRGateway / DStarGateway** — protocol-specific gateways that connect MMDVMHost's frames to the relevant internet network (YSF reflectors, BrandMeister, REFxxx).
- **A web dashboard** at `http://pi-star.local/` (or the hotspot's IP) — Apache + PHP, exposes a configuration UI and a live last-heard log.
- **Optional bits** — APRS gateway, dashboard widgets, automatic updates.

The "stack" looks like this:

```
   Your handheld radio (Anytone, Icom, Yaesu — whatever DV)
      │
      │  RF (UHF or VHF, 10-20 mW)
      ▼
   MMDVM HAT (the modem)
      │
      │  Serial (UART) at 115200 bps
      ▼
   Raspberry Pi Zero W
      │
      ├─ MMDVMHost daemon (decodes/encodes)
      ├─ Protocol gateway (DMR/D-STAR/YSF)
      ├─ Web dashboard (Apache + PHP)
      │
      │  TCP/IP over Wi-Fi
      ▼
   Your home router → internet → BrandMeister / REF / YSF
```

## Initial setup workflow

1. **Download the image.** Pi-Star.uk → Downloads → latest 4.x image (~700 MB ZIP). Flash to a microSD card with Raspberry Pi Imager or Etcher.
2. **First boot — no network yet.** Insert SD into the Pi, plug in MMDVM HAT, power on. The Pi boots into a "no Wi-Fi configured" state.
3. **Connect to the configuration network.** Pi-Star advertises a Wi-Fi network called `Pi-Star-Setup`. Connect your phone or laptop to it. Browse to `http://pi-star.local/` or `http://192.168.50.1/`.
4. **Fill in the configuration form.** Required:
   - **Callsign** — your callsign (used for all DV identities)
   - **DMR ID** — from radioid.net (required if using DMR)
   - **Radio Frequency** — the UHF or VHF frequency you'll use (e.g., 438.800 MHz)
   - **Latitude / Longitude / Town / Country** — for the network map
   - **Wi-Fi SSID + password** — your home Wi-Fi
   - **Mode selection** — DMR, D-STAR, Fusion, NXDN, P25 — enable whichever you want
   - **DMR Master** — BM_3101_United_States (or whichever closest to you)
   - **D-STAR Module** — REF030 C (or your chosen reflector)
   - **YSF Reflector** — YSF31226 America Link (or your choice)
5. **Apply changes.** The Pi reboots, joins your home Wi-Fi, and Pi-Star-Setup network disappears.
6. **Find the hotspot on your home network.** Browse to `http://pi-star.local/` from your home network. Login is `pi-star` / `raspberry` (change immediately).
7. **Program your radio.** Set up channels on your DMR/D-STAR/Fusion radio pointing at the hotspot's frequency. Your local "repeater" is now this hotspot.

## The dashboard

The Pi-Star dashboard is the heart of the operating experience. Key sections:

- **Dashboard (home)** — live last-heard, current talkgroup, RF signal indicators.
- **Configuration** — the main settings page. Modes, networks, callsign, frequency.
- **Admin → Power** — reboot / shutdown.
- **Admin → Update** — pull latest Pi-Star release.
- **Admin → Live Logs** — real-time view of MMDVMHost's serial conversation. Useful for debugging.
- **Admin → SSH Access** — a web SSH client; you can also SSH directly with `ssh pi-star@<ip>`.

The dashboard updates in real time as transmissions arrive. When you key up your radio, you should see:

```
   Time    Mode    Callsign   Target          RSSI  BER
   14:32   DMR     WM3J       TG 3100         -42  0.1%
```

That `BER` is bit error rate — see [§24-10](24-10-ber-explained.md). Low BER (<1%) means clean audio.

## Common gotchas

### TX power gotcha — RF level setting

The MMDVM HAT's TX power is software-controlled, ranging from 0 to 255 (arbitrary units, *not* milliwatts directly). Default value in Pi-Star is usually **50** which corresponds to roughly 5–10 mW. Setting it higher than ~100 can cause:

- The radio's PA chip to overheat (no heatsink on a Pi Zero hat).
- Increased adjacent-channel spurs (regulatory concern).
- Modulation distortion above some saturation point.

Most users set TX power 50–80. Higher doesn't reach further reliably because the modulation degrades.

### Frequency drift on cheap MMDVM HATs

Cheap clone MMDVM HATs (the $25 Jumbospot-style ones) typically use a TCXO of marginal stability. They can drift several hundred Hz over a 10-minute warmup period. Symptoms:

- BER starts at 5% on first key-up, drops to 0.5% after the hotspot has been on for 30 minutes.
- D-STAR (6.25 kHz channel) is more sensitive to drift than DMR (12.5 kHz channel).

Fix: SSH in and run the **MMDVMCal** utility. It generates a calibration tone you measure with a calibrated receiver (or another known-good radio), and computes the **RXOffset** and **TXOffset** in Hz that you put into the Pi-Star "Modem" settings page. Better fix: buy a hotspot with a genuine TCXO (HRO sells these for ~$80 with documented frequency stability).

### ID.dat updates

D-STAR's gateway uses a file called `ID.dat` mapping callsigns to last-heard repeaters. Pi-Star refreshes this from a master server periodically (default: weekly). If you can't be heard by callsign routing, force an update:

```bash
sudo pistar-update
sudo pistar-remote
```

Or via dashboard: *Admin → Update*.

### Wi-Fi flakiness on Pi Zero W

The Pi Zero W's onboard Wi-Fi is okay but not great. Symptoms of Wi-Fi issues: hotspot drops off the dashboard, BrandMeister disconnects every few minutes, audio cuts mid-QSO. Fixes:

- Move the hotspot closer to the access point.
- Pi 3/4 instead of Zero W (much better Wi-Fi).
- Hardwire with USB ethernet adapter.

### Default password — change it

The default `pi-star` / `raspberry` login is *globally known*. If your hotspot is exposed to the public internet (some router setups expose it via port forwarding for remote dashboard access), this is a serious vulnerability. Always change passwords on first boot:

```
Configuration → Login Password → new password
```

### Time sync

Pi-Star uses NTP for time, which D-STAR and DMR both depend on for frame timing. If your Pi-Star can't reach a time server (firewall blocking UDP 123), TDMA slot alignment in DMR fails and BER will be 30%+. Verify NTP is working:

```bash
ssh pi-star@<ip>
timedatectl status
```

Should show "System clock synchronized: yes".

> **Advanced —** The MMDVMHost daemon expects UTC time from the OS and aligns DMR slot timing to the second boundary. Drift greater than ~30 ms causes the BrandMeister master server to reject your frames with a "frame alignment error". Pi-Star runs `chronyd` by default; if you've replaced it with systemd-timesyncd, double-check that NTP is actually working — systemd-timesyncd's once-on-boot strategy is not aggressive enough for DMR timing requirements.

## Common configuration recipes

### "Worldwide DMR static, regional dynamic"

- DMR Mode: enabled
- DMR Master: BM_3101_United_States (or local equivalent)
- Static TGs (set via brandmeister.network dashboard, not Pi-Star): TG 91, TG 3100, TG 31480 (your state)
- Dynamic TG hangtime: 15 min

This gives you Worldwide, USA, and your state always on, plus anything you key up for 15 minutes.

### "D-STAR with auto-link to REF030 C"

- D-STAR Mode: enabled
- Default Reflector: REF030 C
- Auto link on startup: enabled (Configuration → D-STAR Configuration)

The hotspot auto-links to REF030 C every time it boots. Local CQs on your hotspot are forwarded to REF030 C; audio from REF030 C plays out your local RF.

### "Fusion on YSF America Link"

- YSF Mode: enabled
- YSF Reflector: YSF31226 America Link

Conventional Fusion ragchew on the dominant US YSF reflector.

## See also

- [§24-08](24-08-hotspot-openspot.md) — Commercial alternative (OpenSpot)
- [§24-09](24-09-duplex-vs-simplex-hotspots.md) — Simplex vs duplex choice
- [§24-10](24-10-ber-explained.md) — Tuning by BER
- [§24-02](24-02-dmr-talkgroups.md) — Static vs dynamic TGs in detail
- [§24-05](24-05-dstar-routing.md) — D-STAR reflector linking from a hotspot
