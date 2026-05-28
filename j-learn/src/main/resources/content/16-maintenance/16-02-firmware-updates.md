---
id: 16-02
title: Firmware Updates
chapter: 16
section: 02
level: simple
status: draft
---

# Firmware Updates

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Modern radios are computers with antenna ports. Every transceiver from the last 15 years runs firmware that the manufacturer continues to update — sometimes for years after release. Updates fix bugs, add modes, improve receive performance, and (occasionally) introduce new bugs. Maintaining your station means keeping firmware current — *deliberately*, not blindly.

This section covers when to update, when not to, how to back up before updating, and the recovery procedure when an update goes wrong.

## What firmware fixes (and what it doesn't)

Firmware updates address:

- **Bugs**: documented operating quirks (audio glitches, menu behavior, CAT-control issues, dropped frames in digital modes).
- **Compatibility**: new digital modes, new logging/contest software protocols, updated rig-control APIs.
- **Performance tuning**: adjusted DSP filter coefficients, improved AGC behavior, better AF audio quality.
- **Security**: rare on amateur gear, common on networked devices (e.g., Wi-Fi-equipped rigs, network-attached SDRs).

Firmware does **not** fix:

- Hardware aging (capacitors, relays, mechanical components).
- Bad install (RF in shack, ground loops, antenna problems).
- User error (wrong CAT settings, wrong modulation source).

If your problem is "the rig sounds noisy on 40 m," firmware update is unlikely to be the answer. If your problem is "the menu freezes when I press F4 on this specific dial position," it might be.

## Where firmware lives

Different manufacturers handle firmware differently:

| Manufacturer | Update method | Typical cadence |
|--------------|---------------|-----------------|
| Icom | USB cable + Windows utility (CS-7610, RS-BA1, etc.); some via SD card | 1–3 updates per major model per year |
| Yaesu | USB cable + Windows utility (SCU-17 + ADMS); some via SD card | 1–2 updates per major model per year |
| Kenwood | USB cable + Windows utility (MCP-7A, ARCP-* series) | Less frequent; established models stable |
| Elecraft | USB-serial cable + cross-platform utility | Very frequent (weekly to monthly during active development) |
| FlexRadio | Network-based updater (SmartSDR) | Frequent (monthly) |
| Yaesu (FT-* mobile/HT) | Programming cable + Windows utility, or SD card | 0–2 per year |
| BaoFeng / Quansheng / clones | Often community-developed firmware (e.g., Quansheng UV-K5 has open-source firmware) | Community-driven, frequent |

The manufacturer-utility programs are mostly Windows-only (a few work in Wine on Linux). Mac/Linux users typically run the updater inside a Windows VM.

## When to update

The general rule: **update when you have a reason, not on every release.** A new firmware version may fix a bug you don't have, while introducing one you didn't have before. Reasons to update:

- **You're hitting a known bug** that's listed in the release notes as fixed.
- **You need a new feature** (a new digital mode the rig now decodes natively, an updated CAT command set, etc.).
- **It's been more than a year** and the manufacturer has released several stability/security updates.
- **You're integrating with new software** (e.g., a new logging program that requires a recent firmware version for full CAT control).

Reasons **not** to update:

- "Because it's there." A new firmware version doesn't necessarily mean a better radio.
- **Right before a contest or DXpedition.** Update *months* in advance, then run the rig long enough to find any new issues.
- **First week after release.** Let early adopters find the show-stoppers. Wait a week or two.

## Read the release notes — actually read them

Manufacturer release notes are usually written tersely but contain the actual important information. Look for:

- **List of fixed bugs.** Does the bug you're worried about appear? Has any function you actively depend on been "improved" (= changed)?
- **List of known issues.** Most release notes include "what's still broken." Read this.
- **Migration notes.** Does the update reset settings? Does it require a re-cal? Does it require a specific previous version as a starting point?
- **Compatibility notes.** Has the rig-control protocol changed? Will your logging program still talk to the rig?

A release-notes-free update from an obscure brand is a yellow flag. Wait for community feedback (eHam reviews, QRZ forums, brand-specific user groups) before installing.

## Before you update

1. **Back up everything writable on the rig.** Modern rigs have huge configuration databases — memories, scope settings, macros, CW messages. The manufacturer's utility usually has an "export configuration" function. Use it. Save the backup file *somewhere besides the same PC*, ideally cloud-backed.
2. **Photograph the current settings** of any setup you might lose: menu deep-dives, audio levels, custom display configurations. Phone camera + organized folder.
3. **Note the current firmware version.** You may need to roll back; you'll want to know what you came from.
4. **Charge or power the rig adequately.** If the update fails mid-flash because of a power glitch, your rig may be bricked. Use the AC-mains DC supply, not a partially-discharged battery.
5. **Use the manufacturer's recommended cable.** Icom rigs use Icom-specified USB cables; Yaesu uses SCU-17 or equivalent FT232-based interfaces. A no-name $5 USB-serial cable from Amazon may work for CAT control but fail mid-firmware-flash.

## During the update

- **Do not power-cycle the rig** during an update. Most updaters take 5–20 minutes.
- **Do not unplug the cable** during an update.
- **Watch the progress indicator.** If it stops moving for more than 5 minutes, *do not* press cancel; let the manufacturer's recovery process complete or research the recovery procedure first.

## When an update fails

Three failure modes:

### Soft failure (settings reset, otherwise OK)

Common. The rig boots, all is well, but your memories, screen colors, and custom settings are gone. Restore from the backup you made.

### Hard failure (rig boots into "recovery mode" or doesn't boot to operating screen)

Less common. The rig has detected a corrupt firmware load and is sitting at a boot prompt or "recovery mode." Most manufacturers provide a documented recovery procedure: hold a specific button while powering on, plug into the updater, re-flash. Find the procedure in the rig's manual or on the manufacturer's support page.

### Bricked (rig is dead, doesn't respond)

Rare but possible. A power glitch during the critical write phase can leave the rig in an unrecoverable state. The fix is usually:

1. Try the manufacturer's recovery procedure first (it works for most "won't boot" cases).
2. If that doesn't work, **contact the manufacturer's service department** — they have low-level reflash tools (JTAG/SWD programmers) that can recover most bricked rigs.
3. Last resort: factory service repair, which on a 5-year-old rig may not be cost-effective.

> **Advanced —** Most modern rigs have a small bootloader (typically a few KB) in a separate flash region from the main firmware. The bootloader is what handles "if main firmware is corrupt, accept a reload." A bricked-state happens when the bootloader itself was corrupted — possible during the small fraction of update time when the bootloader region is being written. Manufacturers minimize this risk by checksumming, by only writing the bootloader during major firmware revisions (not minor ones), and by allowing two bootloader copies. JTAG/SWD recovery rewrites the bootloader from outside the rig's normal interfaces; it's a service-department procedure.

## Updates for accessories

Don't forget:

- **Antenna tuners** (autotuners with smart matching algorithms have firmware).
- **Amplifiers** (modern transistor amps with digital controls).
- **Tower rotators** (some have firmware in the controller).
- **Antenna analyzers** (NanoVNA firmware is community-developed and frequently updated; H4-V2 is the current popular community fork).
- **Digital interfaces** (SignaLink USB, IC-7610 codec, etc.).
- **Software-defined radios** (RTL-SDR drivers, KiwiSDR firmware, OpenWebRX).

Most accessories have less aggressive firmware schedules than rigs do, but check annually.

## Software-defined radio nuances

For SDRs (FlexRadio, Anan, KiwiSDR, OpenWebRX, RTL-SDR, etc.), firmware updates are often paired with **software updates on the host PC**. The radio firmware and the controlling software must match versions. A firmware update may force a software update; a software update may require a firmware update.

This is one place where "wait a week before updating" is especially important — community reports of "after the update, my SDR doesn't see the old SmartSDR client" are common.

## Common mistakes

- **No backup before update.** Settings reset; hours lost re-entering memories.
- **Update mid-contest weekend.** Just don't.
- **Power glitch during flash.** Use AC supply, full battery, or both.
- **Cheap USB cable.** Use the manufacturer's, or an established brand.
- **Updating without reading release notes.** You may install a "fix" for a bug you don't have and an "improvement" that breaks your workflow.
- **Updating ten things at once.** If something breaks afterward, you don't know which update caused it. Update one thing at a time, run the station for a few days, then update the next.
- **Forgetting accessories.** A rig firmware update that adds a new CAT command may not work if the antenna tuner's firmware is from 2018 and doesn't recognize the new command.

## See also

- §16-00 — Maintenance overview
- §16-03 — Scheduled inspections (firmware-check is part of the quarterly inspection)
- §13 — Station troubleshooting (when "weird stuff started after the update")
- **J-Vault** — Shack inventory (track firmware versions per device)
