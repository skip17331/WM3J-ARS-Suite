---
id: 15-05
title: Accessories
chapter: 15
section: 05
level: simple
status: draft
---

# Accessories

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

This section covers everything in the inventory that isn't a radio (§15-01), antenna (§15-02), tuner (§15-03), or coax run (§15-04). The accessories category is broad: power supplies, audio gear, computers, software, batteries, antennas analyzers, soldering equipment, books, manuals, parts. Each gets a row.

The unified schema applies to all of them. Some categories (software licenses, books, parts collections) deserve their own organizational notes within this chapter.

## Schema applied to accessories

Same seven core fields as everything else. The "category" column distinguishes them:

| Category subtype | Common items |
|------------------|--------------|
| Power | Power supplies, batteries, chargers, fuses |
| Audio | Microphones, headphones, audio cables, sound interfaces |
| Computers | PCs, laptops, tablets, monitors, keyboards |
| Software | Logging software, digital-mode software, OS, contest tools |
| Test equipment | Antenna analyzers, multimeters, oscilloscopes, dummy loads |
| Tools | Soldering iron, wire strippers, crimp tools, hand tools |
| Books / docs | Reference books, ARRL Handbook, study guides, manuals |
| Parts | Connectors, cables, components, kits |
| Antenna hardware | Insulators, baluns, ununs, switches |

## Power category

Power supplies and batteries. The bench supply, the emcomm battery, the auxiliary 12 V distribution, the wall warts that power individual accessories.

```
Item ID: P-001
Category: Power Supply
Manufacturer: Astron
Model: RS-50A (50 A linear)
Serial Number: 50A-9876
Date Acquired: 2009-01-15
Purchase Price: 220.00
Estimated Value: 130.00 (linear supplies hold value)
Disposition: Working
Notes: Main HF rig power. Quiet (low noise) — preferred over switching supply.
       Dedicated 20 A circuit. AC line filter installed.
```

```
Item ID: P-002
Category: Battery
Manufacturer: Bioenno
Model: BLF-1220A (LiFePO₄, 20 Ah)
Serial Number: BL12345
Date Acquired: 2021-09-15
Purchase Price: 200.00
Estimated Value: 150.00
Disposition: Working
Notes: Emcomm battery. Float-charged via Bioenno BPC-1503-DC2.
       Annual capacity test: 18 Ah (90% of new) as of 2026-04.
```

Battery and power supply specifics:

- **Battery installs**: track install date and capacity over time (see §17-01 for the maintenance side).
- **Power supplies**: track maintenance — fan replacements, cap replacements, the slow drift of regulators.
- **Solar / wind charge controllers**: separate items if you have them.

## Audio category

Microphones, headsets, headphones, audio interfaces:

```
Item ID: AU-001
Category: Audio - Microphone
Manufacturer: Heil
Model: PR-781
Serial Number: PR-987654
Date Acquired: 2022-04-15
Purchase Price: 230.00
Estimated Value: 180.00
Disposition: Working
Notes: Used with IC-7610. Foot switch attached.
```

```
Item ID: AU-002
Category: Audio - Headphones
Manufacturer: Heil
Model: Pro 7 (DX edition)
Serial Number: H-12345
Date Acquired: 2021-08-10
Purchase Price: 350.00
Estimated Value: 250.00
Disposition: Working
Notes: Headset with mic; used as primary station audio.
```

Audio cables get their own item if they're significant:

```
Item ID: AU-003
Category: Audio - Cable
Manufacturer: SignaLink
Model: CB-USB
Serial Number: CB-9876
Date Acquired: 2020-12-01
Purchase Price: 75.00
Estimated Value: 50.00
Disposition: Working
Notes: USB sound interface for digital modes. SignaLink USB.
```

## Computers and software

Hardware:

```
Item ID: C-001
Category: Computer
Manufacturer: Apple
Model: Mac mini (M2, 2023)
Serial Number: SC0X12345
Date Acquired: 2023-04-15
Purchase Price: 800.00
Estimated Value: 550.00
Disposition: Working
Notes: Shack computer; runs J-Hub, J-Log, Fldigi.
       Dedicated to ham radio use.
```

Software licenses:

```
Item ID: SW-001
Category: Software - License
Manufacturer: Fldigi (open source)
Model: Fldigi (latest)
Serial Number: (n/a - open source)
Date Acquired: 2020-01-01
Purchase Price: 0.00
Estimated Value: 0.00
Disposition: Working
Notes: Open-source digital mode software. Used with SignaLink.
```

```
Item ID: SW-002
Category: Software - License
Manufacturer: N1MM Logger+ (open source)
Model: N1MM Logger+
Serial Number: (n/a)
Date Acquired: 2018-06-01
Purchase Price: 0.00
Estimated Value: 0.00
Disposition: Working
Notes: Contest logging.
```

Software with paid licenses:

```
Item ID: SW-003
Category: Software - License
Manufacturer: SmartSDR (FlexRadio)
Model: SmartSDR for FLEX-6500
Serial Number: License key 12345-ABC
Date Acquired: 2023-09-15
Purchase Price: 0.00 (bundled with rig purchase)
Estimated Value: 0.00 (tied to specific rig hardware)
Disposition: Working
Notes: License tied to FLEX-6500 rig; transferable on rig sale.
```

For software, "Disposition" is more about whether the license is currently usable than whether it works:

- **Working**: license is active and you can use the software.
- **Repairable**: license expired or key lost — recoverable by contacting the vendor or repurchasing.
- **Not Repairable**: legacy software no longer supported by the manufacturer; might still be installable but support is gone.

## Test equipment

NanoVNAs, antenna analyzers, multimeters, dummy loads:

```
Item ID: TE-001
Category: Test Equipment - Antenna Analyzer
Manufacturer: NanoVNA-Saver community / EPK
Model: NanoVNA H4 V2.0
Serial Number: (none on device)
Date Acquired: 2021-03-15
Purchase Price: 80.00
Estimated Value: 70.00
Disposition: Working
Notes: 50 kHz - 1.5 GHz vector network analyzer. Daily-use test gear.
       Calibrated 2026-03-15.
```

```
Item ID: TE-002
Category: Test Equipment - Dummy Load
Manufacturer: Ameritron
Model: AT-2K (2 kW dummy load, oil-filled)
Serial Number: AT-12345
Date Acquired: 2010-05-01
Purchase Price: 200.00
Estimated Value: 150.00
Disposition: Working
Notes: Used for tune-up at high power. Re-fill oil every 5 years.
       Last oil change: 2025-04.
```

## Tools

Soldering equipment, hand tools, crimp tools:

```
Item ID: TL-001
Category: Tool - Soldering
Manufacturer: Hakko
Model: FX-888D (digital soldering station, 70 W)
Serial Number: 888-12345
Date Acquired: 2017-09-15
Purchase Price: 130.00
Estimated Value: 100.00
Disposition: Working
Notes: Bench-top digital station. Used for connector work + repairs.
       Tip replaced annually.
```

Tools have a long service life and rarely need value updates.

## Books and reference materials

Books and reference docs are inventory items:

```
Item ID: B-001
Category: Book - Reference
Manufacturer: ARRL
Model: ARRL Antenna Book (24th Edition)
Serial Number: (ISBN 978-1-62595-149-5)
Date Acquired: 2024-09-15
Purchase Price: 60.00
Estimated Value: 45.00
Disposition: Working
Notes: Primary antenna reference. Hardcover.
```

```
Item ID: B-002
Category: Book - Reference
Manufacturer: ARRL
Model: ARRL Handbook (104th Edition, 2027)
Serial Number: (ISBN tbd)
Date Acquired: 2026-09-15
Purchase Price: 65.00
Estimated Value: 65.00 (current edition)
Disposition: Working
Notes: General reference. Updated annually.
```

For the estate planning side: vintage editions of these references can have collector value (early ARRL Handbooks from the 1940s-50s have collector premiums).

## Parts collections

Parts are harder to inventory item-by-item. Group them by category:

```
Item ID: PT-001
Category: Parts - Connectors
Manufacturer: Various
Model: PL-259/SO-239/N/BNC connector inventory
Serial Number: (lot)
Date Acquired: ongoing
Purchase Price: ~150.00 (cumulative)
Estimated Value: 100.00 (replacement value)
Disposition: Working
Notes: Bin of connector parts. PL-259 (12), SO-239 (8), N male (10),
       N female (5), BNC (15), SMA (10). Re-stock as needed.
```

```
Item ID: PT-002
Category: Parts - Wire / Cable
Manufacturer: Various
Model: Wire and cable inventory
Serial Number: (lot)
Date Acquired: ongoing
Purchase Price: 200.00 (cumulative)
Estimated Value: 100.00
Disposition: Working
Notes: 200 ft #14 stranded copper for antennas; 50 ft RG-58;
       100 ft RG-8X partial spool; 30 ft 450-ohm window line.
```

For estate purposes, parts are usually "as a lot" — buyers value the bin, not the individual components.

## Antenna hardware (separate from antennas themselves)

```
Item ID: AH-001
Category: Antenna Hardware - Balun
Manufacturer: Balun Designs
Model: Model 1115 (1:1 current balun, 5 kW PEP)
Serial Number: BD-1115-9876
Date Acquired: 2018-06-01
Purchase Price: 90.00
Estimated Value: 60.00
Disposition: Working
Notes: At feedpoint of vertical (A-002). Re-sealed annually.
```

Switches:

```
Item ID: AH-002
Category: Antenna Hardware - Switch
Manufacturer: DX Engineering
Model: DXE-RR8B-HD (8-position antenna switch, 5 kW)
Serial Number: RR8B-1234
Date Acquired: 2020-10-15
Purchase Price: 280.00
Estimated Value: 200.00
Disposition: Working
Notes: Indoor remote-control 8-port antenna switch. PL-259 inputs.
```

## Items that get easy to over-document

Some accessories accumulate quickly and are tedious to track individually:

- **Cables and patch cables under $20**: track in a "patch cables (~12 short coax patches)" group.
- **Adapter connectors** (PL-to-N, BNC-to-PL): grouped count.
- **Bench supplies (under $50)** like extra power strips, wall warts: skip individual tracking; grouped notes.
- **Stickers, swag, club merchandise**: inventory per-item is overkill.

The rule: **track individual items when they have $20+ value or specific significance**; group the rest.

## Software-license caveats

Software gets special handling:

- **Tied to hardware**: SmartSDR is tied to a Flex rig; the license transfers with the rig sale.
- **Subscription-based**: HRD, ARRL DXCC, etc. — the license expires; track renewal date.
- **One-time purchase, perpetual**: most amateur software is this; track to ensure key isn't lost.
- **Open source / free**: track to ensure you have a backup of the installer.

For estate disposition: software licenses tied to a rig transfer with the rig. Subscription accounts may not transfer (read the EULA). Document license keys securely (cloud password manager) so they're recoverable.

## Common accessory inventory mistakes

- **Skipping all the small items.** Even $30 wattmeters add up; track them.
- **Forgetting software licenses.** If the inventory is for estate planning, software keys matter.
- **No location field.** "I have a SignaLink USB" — yes, but where? Add a location column ("operating desk drawer," "antenna closet," "field bag").
- **Not tracking warranty expiration.** Some accessories (computers, batteries) have multi-year warranties; track the warranty end date.
- **Treating books as not-inventory.** A $200 ARRL Handbook collection is real value.

## Where this leads

Once accessories are inventoried, the estate-disposition planning (§16) can group them sensibly:

- "Sell with main rig as a package": main rig + matching tuner + matching mic.
- "Sell separately": amplifier, accessories with broad market.
- "Donate or pass to club": tools, parts, books that have community value.
- "Family / personal keepsakes": vintage gear with sentimental value.

The accessories inventory is the input to that disposition decision.

## See also

- §15-00 — Schema overview
- §15-06 — Serial numbers (especially relevant for accessories with high theft value)
- §15-07 — Purchase dates (warranty tracking)
- §15-08 — Firmware versions (some accessories have firmware too)
- §17 — Maintenance (accessories need maintenance just like radios)
- §16 — Estate / SK (where accessories get disposition decisions)
