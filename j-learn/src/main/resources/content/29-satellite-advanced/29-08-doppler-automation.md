---
id: 29-08
title: Doppler Automation
chapter: 29
section: 08
level: advanced
status: published
---

# Doppler Automation

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Manual Doppler tuning works for FM birds. It's tolerable for a relaxed linear transponder QSO if you're patient and your rig has fast tuning. But for serious linear work — especially at UHF and above where the shift is large and the receiver bandwidth is narrow — manual tuning is exhausting and error-prone. The standard fix is to let a computer track the satellite's predicted position, compute the live Doppler shift, and send frequency commands to the rig via **CAT (Computer-Aided Transceiver)** control.

This section covers the three main tracking applications, the CAT-control infrastructure that makes them work, and the practical issues (USB latency, update-rate limits) that bite operators who set it up for the first time.

## The three dominant applications

### SatPC32

The original. Written by Erich Eichmann (DK1TB) starting in the late 1990s and continuously updated since, **SatPC32** is the de facto standard for serious amateur satellite operating on Windows. It's commercial (registered version costs about $40) but widely used and well-supported.

Capabilities:

- **Real-time Doppler tracking** with sub-100ms update rates on supported rigs.
- **Rotor control** (az/el via Yaesu GS-232, ARS, or HyGain protocols) integrated with the pass prediction.
- **Sub-tone (CTCSS) handling** for FM birds.
- **Auto-uplink-only or auto-downlink-only tuning** modes for different operating strategies.
- **Pass scheduling** with calendar export.
- **Logging integration** with most logging programs.

Rig support is broad — essentially every modern Icom, Yaesu, and Kenwood, plus most Elecraft K-series. The rig is connected via CAT over USB-to-serial, and SatPC32 sends frequency commands at intervals fast enough to keep the rig within a few Hz of the predicted Doppler-corrected frequency.

### MacDoppler

The macOS-native equivalent. Written by Donald Agro (VE3VRW), **MacDoppler** has been the standard for Mac-based satellite operators since the mid-2000s. Feature-comparable to SatPC32 with a more polished UI. Commercial; about $90.

Strong points: integrates well with the Mac's serial-port and Bluetooth stack, very clean visual representation of the pass, and a per-satellite memory of polarization and band preferences.

### GPredict + Hamlib

The open-source option. **GPredict** is a cross-platform (Linux, Windows, macOS) satellite tracker, with a separate rig-control daemon (**rigctld**, part of the **Hamlib** project) handling CAT communication. Both are free.

GPredict by itself just predicts and displays satellite positions; rig control comes through Hamlib. The architecture:

1. **GPredict** computes the satellite's Doppler shift from current Keplerian elements.
2. **GPredict** sends frequency commands to **rigctld** over a TCP socket.
3. **rigctld** translates the commands to the rig's specific CAT protocol and sends them over serial.
4. The rig tunes.

This separation makes the system flexible — you can use rigctld for any other CAT-controlled program (logging, contesting) without affecting the satellite tracking. The downside is more configuration steps, more places for errors, and a less-polished UI than SatPC32 or MacDoppler.

GPredict + Hamlib is the only option on Linux and is also a popular choice for low-budget operators on any platform. The combination is fully functional but takes more setup work.

## Configuring CAT control

CAT (Computer-Aided Transceiver) is the rig's serial-port command interface — a protocol over a serial line that lets a computer set frequency, mode, power, and other parameters, and read them back. Every modern amateur rig supports it; the specific protocol is rig-specific (Icom CI-V, Yaesu's various CAT versions, Kenwood's command set, Elecraft K-series).

The CAT connection between the computer and the rig:

- **Direct USB:** modern rigs (IC-7300, IC-9700, IC-705, FT-991A, FTDX-10, K4) have a built-in USB-to-serial converter accessible from a single USB cable. The computer sees a virtual COM port; the rig is on the other end.
- **USB-to-serial cable + rig's serial port:** older rigs (IC-7200, FT-857, TS-2000) need an external USB-serial adapter, often with rig-specific level shifters. The Icom CI-V cable family (CT-17 and similar third-party clones) is the canonical example.
- **TTL or RS-232 direct:** rare these days; some hardcore homebrew operators run a TTL-level serial connection from a Raspberry Pi GPIO to the rig's CAT port.

The serial-port settings (baud rate, parity, stop bits, address byte for CI-V) must match between the computer and the rig. The rig's manual specifies these; SatPC32, MacDoppler, and Hamlib have rig-specific defaults that usually work out of the box.

> **Advanced —** CI-V (Icom's protocol) is a multi-drop bus — you can chain multiple Icom rigs to one CAT cable, distinguished by a 1-byte address per rig. The default CI-V address for an IC-9700 is `0xA2`; for an IC-705 it's `0xA4`. If you're running both at once on the same cable, set distinct addresses and configure the tracking software to talk to each by address. Most operators with multiple Icom rigs use separate USB cables to avoid the configuration complexity.

## The half-second-latency problem

USB-to-serial introduces latency. For a USB CAT cable on a typical Windows or Linux system, the round-trip command-to-response time is **30-100 milliseconds**. That's fine for casual rig control — set frequency, read frequency back, no human notices.

For Doppler tracking it's a problem. At closest approach of an LEO satellite at 435 MHz, the Doppler shift is changing at about **30 Hz/second** (rate-of-change peak). If your tracking loop has 500 ms of total latency (command queue + USB + rig response + display), the rig's actual frequency lags the predicted frequency by 15 Hz. On SSB with a 2.5 kHz bandwidth, 15 Hz is inaudible. On CW with a 250 Hz filter, 15 Hz is the difference between solid copy and intermittent.

Sources of latency:

- **Tracking software update rate:** SatPC32 defaults to 1 Hz updates; can be set as fast as 10 Hz. GPredict defaults to 1 Hz.
- **OS USB scheduling:** Windows USB sub-system can queue commands for 10-50 ms. Linux is typically faster.
- **Rig command processing:** the rig takes 5-30 ms to actually execute a frequency change command.
- **Display refresh:** the rig's display catches up to the new frequency a few ms after the change.

The cumulative effect is the 30-100 ms typical latency. Operators chasing the last bit of performance use:

- **Faster update rates** (10 Hz instead of 1 Hz) to keep the prediction error small.
- **Lower-latency USB-serial drivers** (FTDI FT232R with low-latency mode enabled, instead of the default 16 ms polling interval).
- **Direct serial connections** on Raspberry Pi (sub-millisecond latency, no USB stack involved).

For most amateur satellite operating, 1 Hz update rate is sufficient and you don't notice the latency. The exception is fast-Doppler microwave work (10 GHz CW from a 13 cm satellite), where every dB of frequency stability counts.

## IC-9700 native Doppler tracking

The Icom IC-9700 includes **firmware-level Doppler tracking** for satellites in its memory channels. The operator selects a satellite (e.g., "AO-91" from the satellite memory bank), the rig knows the uplink and downlink frequencies and the orbital parameters (loaded from a Keplerian elements file via SD card), and the rig automatically adjusts both VFOs continuously through the pass.

The advantage: no computer, no CAT cable, no software to configure. The radio just does it.

The disadvantage: the Keplerian elements file needs to be updated periodically (manually transferred to the SD card from amsat.org's element files). If the elements are stale, the predicted frequency drifts from the actual frequency, and the rig "tracks" the satellite at the wrong frequency.

The IC-9700's native tracking is a major convenience for operators who don't want to set up a computer for every satellite session — it makes the IC-9700 a "just turn on and work the bird" radio in a way no previous amateur rig has been. As of 2026 it remains the dominant new-purchase choice for amateur satellite stations.

## Practical setup workflow

For a first-time operator setting up Doppler automation:

1. **Choose your tracker:** SatPC32 on Windows, MacDoppler on Mac, GPredict on Linux (or any platform, free).
2. **Connect the rig via CAT.** Verify in the rig's menu that CAT is enabled at the right baud rate. Plug in the USB cable and verify the computer sees a serial port.
3. **In the tracking software, configure the rig:** select the rig model, COM port, baud rate, and (for Icom) CI-V address. Test that the software can read the current frequency from the rig.
4. **Load current Keplerian elements.** Download from celestrak.org or amsat.org. Most tracking software has a one-click update.
5. **Pick a satellite and start tracking.** The software computes the predicted Doppler-corrected frequencies and starts sending them to the rig.
6. **Verify by listening.** During an actual pass, listen to a beacon or a known active linear bird. The downlink should be on-frequency with no audible Doppler.

Once it works for one satellite, it works for all of them — the configuration is per-rig, not per-satellite.

## Common automation failures

- **Stale Keplerian elements.** Predictions drift by hundreds of Hz per week. Update weekly minimum.
- **Wrong CI-V address** on Icom rigs. The rig won't respond to commands; the tracker tries to set frequency and fails silently. Most apps display a "no response from rig" status — check it.
- **Two applications fighting for the same serial port.** Tracking software and logging software both wanting CAT control simultaneously will conflict. Either run rigctld as a single intermediary (Hamlib's approach) or close the logger while satellite-operating.
- **Wrong rig type selected** in the tracking software. Each rig has subtle protocol differences; sending IC-9700 commands to an IC-705 will partially work and then break in unexpected ways.
- **PTT triggered by CAT instead of by mic button.** Some configurations route PTT through the CAT line; if the tracking software accidentally sets a non-zero PTT in a frequency command, the rig keys up. Verify PTT source in the tracking software's config.

## See also

- §29-09 — Linear transponder strategy (where Doppler automation pays off)
- §07-02 — Doppler shift (the underlying physics)
- §27 — Station Engineering (rig integration topics)
