---
id: 14-08
title: Firmware Versions
chapter: 14
section: 08
level: simple
status: draft
---

# Firmware Versions

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Tracking firmware versions in the inventory tells you what software-level state each piece of equipment is in. Combined with an "update applied" date, the inventory becomes the audit trail for what was installed and when. This is essential when troubleshooting ("did this rig get the bug-fix update?"), planning maintenance ("which radios are overdue for the latest firmware?"), and recovering from a problem ("we updated last Tuesday and now this isn't working — what changed?").

This section covers what to track, how to record it, and how it integrates with the maintenance schedule (§16-02).

## What firmware lives where

Most modern amateur equipment has firmware:

| Category | Has firmware? | How to read version |
|----------|---------------|---------------------|
| HF / VHF / UHF transceiver | Yes | Menu (typically "About" or "Version" screen) |
| HT | Yes | Menu (varies by model) |
| Amplifier | Modern transistor amps yes; tube amps usually no | Front-panel display or menu |
| Antenna tuner (auto) | Yes | Menu or boot screen |
| Antenna analyzer (NanoVNA) | Yes | Boot screen or "info" menu |
| Power supply | Usually no (passive electronics) | n/a |
| Rotator controller | Sometimes (digital controllers) | Boot screen |
| Computer (shack PC) | Has BIOS / UEFI version + OS version + applications | Multiple sources |

The **rig firmware version** is the most-important to track for amateur use.

## Recording firmware versions in the inventory

For each device with firmware, the inventory entry includes:

```
Firmware Version: [version string]
Firmware Updated: [YYYY-MM-DD]
```

Example for a typical HF rig:

```
Item ID: R-001
Manufacturer: Icom
Model: IC-7610
... (other fields) ...
Firmware Version: Main 2.30 / DSP 1.10 / FPGA 3.05
Firmware Updated: 2026-03-01
Notes: Updated to 2.30 to address audio glitch bug from 2.29.
       Backup of pre-update settings stored in cloud.
```

Some rigs have multiple firmware components (main, DSP, FPGA, scope module) — track each.

## Why version tracking matters

### Bug awareness

When a manufacturer publishes a firmware update with a list of fixed bugs, you can check your inventory: which of my radios have a version older than the fix?

For example: "Icom firmware 2.29 had an SSB audio glitch on certain CW-mode-to-SSB transitions. Fixed in 2.30." Looking at the inventory, you see your IC-7610 is at 2.30 (good) but your secondary IC-7300 is at 1.42 (need to check release notes for that model).

### Troubleshooting recall

When a station behavior changes mysteriously, "what changed recently?" is the first question. The firmware-update log answers it directly:

- "I updated the IC-7610 firmware on 2026-03-01."
- "Three days later, the audio level dropped intermittently."
- "Possible cause: the firmware update; possibly an unrelated hardware issue."

Without the dated log, this kind of inference is impossible.

### Pre-deployment readiness

Before a contest, before a SOTA hike, before an emcomm activation — verify firmware is current. The inventory tells you which radios are at recent versions and which are overdue.

### Compatibility tracking

When digital-mode software (Fldigi, WSJT-X, JS8Call) is updated, the new version may require specific rig firmware versions for full compatibility. If your rig firmware is older, certain features may not work. The inventory tells you which radios support which software versions.

### Service history

For high-value gear, the firmware history is part of the service log. When selling a rig, "always kept on current firmware" is a positive selling point; "last updated 2018" is a flag.

## Update strategy

The discipline:

1. **Quarterly review**: check manufacturer support pages for firmware updates. (Calendar this.)
2. **Read release notes**: do you need this update? Does it fix something you experience? Are known issues acceptable?
3. **Schedule the update**: not during a contest weekend. After hours, with backup of settings, with stable power.
4. **Update one device at a time**: so if something breaks, you know which update caused it.
5. **Verify after update**: full operating check on each band/mode you use.
6. **Update inventory**: record new version and date.
7. **Document in notes**: any issues, unexpected behavior, settings that needed re-entry.

## Firmware backup

Most modern rigs let you export the entire configuration before an update:

- **Settings file** (memories, scope settings, macros, CW messages, custom configurations).
- **Calibration data** (some rigs include user-cal in the export).

Save these in cloud storage, separately from the radio. If an update goes catastrophically wrong, the backup lets you restore.

The inventory should track:

```
Notes: Firmware backup exported pre-2.30 update.
       File: /shack-data/IC-7610/2026-03-01-firmware-backup-pre-2.30.icf
       Cloud: /Drive/Shack/IC-7610/firmware-backups/
```

## Computer / software firmware

For shack PCs and accessory software:

```
Item ID: C-001
Category: Computer
Manufacturer: Apple
Model: Mac mini (M2, 2023)
... (other fields) ...
OS Version: macOS 14.5 (Sonoma)
OS Updated: 2026-04-15
Software:
  - J-Hub 1.0.0 (custom build, Mike's repo)
  - J-Log 1.0.0
  - Fldigi 4.2.04
  - WSJT-X 2.7.0
  - SDR# 2.0.5
  - SmartSDR 3.7.0 (paired with FLEX-6500)
```

For computers, the OS version is the analogous "firmware" — the platform-level version that affects everything else.

## When firmware tracking matters most

Three scenarios where the firmware-version field is critical:

### Recovering from a bad update

A rig firmware update sometimes goes wrong (rare but real). Possible outcomes:

- **Settings reset** (most common, mild). Restore from backup.
- **Boot loop or no boot** (less common). Recovery procedure (manufacturer-specific).
- **Bricked** (rare). Manufacturer service required.

In all cases, knowing what version you came from and what version you went to is essential. The inventory log tells you.

### Coordinating with other operators

When working with someone else (helping debug their station, swapping radios for an event, etc.), knowing both firmware versions tells you whether you're on the same baseline.

### Selling or transferring

Buyers want to know firmware status: "current" sells better than "old." Documentation helps verify.

## A firmware-update log table

Add a tab to the inventory spreadsheet:

```
Date       | Item ID | From Version | To Version | Issues / Notes
-----------|---------|--------------|------------|----------------
2026-03-01 | R-001   | 2.29         | 2.30       | Settings preserved. Audio glitch fixed.
2025-11-15 | R-002   | 1.40         | 1.42       | Routine update; no issues.
2025-09-20 | R-003   | 5.5.10       | 5.5.12     | Settings reset; restored from backup.
2024-08-03 | R-001   | 2.27         | 2.29       | New band-edge labels.
```

This becomes a chronological audit trail.

## Firmware policies for emcomm equipment

For radios used in emcomm deployments:

- **Don't update right before deployment.** Last-minute updates can introduce surprises in the field.
- **Stay 1-2 versions behind cutting edge.** Let early adopters find bugs.
- **Update during the off-season** so issues can be discovered in casual operation.
- **Document in the inventory** which radios are emcomm-deployed.

## Common firmware-tracking mistakes

- **Updating without recording the new version.** The inventory lags reality.
- **Forgetting to date the update.** Without dates, debugging "what changed when" is impossible.
- **No backup before update.** Settings reset = hours of re-entry. Always export first.
- **Updating multiple radios on the same day.** If something goes weird afterwards, you don't know which update is responsible. Update sequentially, not in parallel.
- **Trusting memory.** "I think we're current on the IC-7610" → check the actual version, don't guess.

## Where this leads

The firmware-tracking discipline feeds:

- **Maintenance scheduling (§16-02)**: the quarterly firmware-review cycle.
- **Troubleshooting (§11)**: "what changed recently" lookup.
- **Resale (§15)**: documenting current-firmware status.
- **Pre-deployment readiness**: verify before contests, drills, emergencies.

## See also

- §14-00 — Schema overview
- §14-01 — Radios (where firmware most commonly lives)
- §14-07 — Purchase dates (paired with firmware update dates)
- §16-02 — Firmware updates (the maintenance side)
- §11 — Station troubleshooting (firmware as a variable)
- §15 — Estate / SK (current firmware adds resale value)
