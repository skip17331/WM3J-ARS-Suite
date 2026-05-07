---
id: 18-01
title: Radios
chapter: 18
section: 01
level: simple
status: draft
---

# Radios

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Radios — transceivers, receivers, and HTs — are usually the most expensive items in the inventory and the ones most worth tracking precisely. A modern HF transceiver may be valued at several thousand dollars; a small fleet of HTs may total another thousand; an amplifier, more. This section applies the unified inventory schema (§18-00) to radio equipment and adds a few radio-specific fields.

## Schema applied to radios

The seven core fields from §18-00, plus radio-specific extras:

| Core field | Radio-specific notes |
|------------|----------------------|
| Manufacturer | Icom, Yaesu, Kenwood, Elecraft, FlexRadio, Ten-Tec, BaoFeng, Quansheng, etc. |
| Model | Specific model designation; include any sub-variants (e.g., FT-991A vs. FT-991, IC-7610 vs. IC-7600) |
| Serial number | Critical; the unique identifier on the chassis |
| Date acquired | When you got it — first-purchase date, not first-use |
| Purchase price | What you paid; note new vs. used acquisition |
| Estimated value | Current resale value; **update annually** for radios — depreciation is significant |
| Disposition | Working / Not working / Parts only |

Radio-specific extra fields:

| Extra field | Why it matters |
|-------------|----------------|
| **Firmware version** | Identifies what features/bug-fixes are loaded; dated for upgrade tracking |
| **Modes / capabilities** | HF + 6m + 2m + 70cm? SSB/CW/AM/FM/data? Built-in TNC? Built-in tuner? |
| **PA condition** | For amplifiers and rigs with high-power finals; "tube replacement due 2026" type notes |
| **Notes on use** | "Daily driver," "mobile install," "backup," "field-day station" |

## Categories of radio in the inventory

Radio inventory typically covers four categories:

### 1. HF transceivers (base)

The high-value items. A typical inventory entry:

```
Category: HF Base Radio
Manufacturer: Icom
Model: IC-7610
Serial Number: 0123456789
Date Acquired: 2022-04-15
Purchase Price: 3500.00 (new)
Estimated Value: 2800.00 (used, market 2026)
Disposition: Working
Firmware Version: 2.30 (updated 2026-03-01)
Modes: HF + 6m, SSB/CW/AM/FM/RTTY/PSK
Notes: Daily driver. Connected to PSU + tuner + amp. Tower antenna.
```

For an HF base radio, photographs are useful — front panel, back panel showing connections, the serial number plate. Store with the inventory.

### 2. HF / VHF / UHF mobile rigs

Vehicle-installed radios — typically one or two units. Less expensive than base radios but still in the few-hundred-dollar range:

```
Category: Mobile Radio
Manufacturer: Yaesu
Model: FT-857D
Serial Number: 8M0987654
Date Acquired: 2010-08-20
Purchase Price: 700.00 (new in 2010)
Estimated Value: 250.00 (used, market 2026)
Disposition: Working
Modes: HF + 2m + 70cm
Notes: Permanent install in pickup truck (truck inventory: GMC Sierra)
```

Mobile rigs often outlast their original vehicle. Track which vehicle each is in; when you sell the truck, the rig stays with you (or transfers separately).

### 3. Handhelds (HTs)

Many operators have multiple HTs — the daily-driver, a backup, a "park" HT for outdoor use, an emcomm one programmed with all the regional repeaters. Each is its own inventory item:

```
Category: HT
Manufacturer: Yaesu
Model: FT-5DR
Serial Number: (none visible) - "ID" sticker on inside of battery compartment is BD0123456
Date Acquired: 2023-02-10
Purchase Price: 280.00 (new)
Estimated Value: 220.00
Disposition: Working
Notes: Daily-driver HT. Charger + spare battery in inventory.
```

Some HTs (BaoFeng, Quansheng) come with no visible serial number. In that case, mark "(none)" — the model becomes the identifier.

### 4. Amplifiers

Linear amplifiers, especially older tube-based units, can have significant value but also significant maintenance overhead:

```
Category: Amplifier
Manufacturer: Heathkit
Model: SB-220
Serial Number: 2-3456
Date Acquired: 2008-12-01
Purchase Price: 800.00 (used)
Estimated Value: 600.00 (vintage market)
Disposition: Not working
Notes: One 3-500Z tube has gas; replacement tube on order. Will be Working again ~Q2 2026.
PA Condition: One tube needs replacement.
```

Amplifier values can move dramatically with collector interest. An old Heathkit may be worth more in collector market than its operational value suggests.

### 5. Receivers (separate from transceivers)

Pure receivers (SDR-Play RSP1A, IC-R8600, AOR receivers, vintage Hammarlund / Hallicrafters) are inventory items:

```
Category: Receiver
Manufacturer: SDRplay
Model: RSP1A
Serial Number: 1A0123456
Date Acquired: 2019-09-15
Purchase Price: 110.00
Estimated Value: 100.00 (still in production)
Disposition: Working
Notes: USB-connected. Used with SDR# software.
```

## Special inventory considerations for radios

### Firmware version tracking

Radio firmware changes; tracking the version helps you know whether bugs/features have been addressed. Re-inventory the firmware version when you update (see §18-08 and §20-02).

### Photography

Photo inventory (front, back, serial number plate close-up) is essential for radios. Store with the spreadsheet — Google Drive folder linked from a "Photos" column in the spreadsheet, or a separate photo file with the same naming as the inventory entry.

### Insurance documentation

For high-value radios, save:

- The original receipt (PDF in cloud).
- A photo of the radio in service.
- A photo of the serial number plate.
- A note in the inventory of "insured value at $X as of [date]."

If a homeowner's claim is needed, the insurance company will want this documentation.

### Theft recovery

Serial numbers are the **single most-important** field for theft recovery. If a radio is stolen and you have the serial number, the manufacturer can flag it as stolen in their records; pawn shops and online marketplaces (eBay) sometimes catch stolen goods this way. Without the serial number, recovery is essentially impossible.

Also: many manufacturers maintain a "stolen radio" registry; report to them after a theft.

### Disposition values for radios specifically

- **Working**: powers up, transmits and receives correctly on all rated bands and modes; PA delivers rated power into 50 Ω; no known menu/firmware bugs that prevent normal use.
- **Not working**: a specific component failure (PA, audio chain, control board, display, etc.). May be repairable; estimate cost and weigh against replacement.
- **Parts only**: not economically repairable; chassis, knobs, controls, transformer, etc., useful as parts donor for other radios.

A rig at "Not working" with a $200 fix that would restore $400 of value is worth fixing. A rig at "Not working" with a $500 fix to restore $300 of value should be moved to "Parts only" or sold as-is.

## Tracking radio accessories

Each radio often comes with accessories that should be inventoried with it:

- **Power cord** (especially the proprietary plug for some Yaesu and Icom mobiles).
- **Microphone** (the original; the upgrade if you bought a Heil or similar).
- **DC cable**.
- **Cooling fan** (some rigs need an external fan).
- **Manual / quick-start guide** (PDFs, paper copies).
- **Original packaging** (matters for resale; vintage gear especially).

Track accessories under §18-05; cross-reference from the radio's notes field.

## Example: a complete radio entry

```
Item ID: R-001
Category: HF Base Radio
Manufacturer: Icom
Model: IC-7610
Serial Number: 0123456789
Date Acquired: 2022-04-15
Purchase Price: 3500.00 (new from HRO)
Estimated Value: 2800.00 (eBay sold price avg, March 2026)
Disposition: Working
Firmware Version: 2.30
Modes: HF + 6m, SSB/CW/RTTY/PSK/AM/FM
Notes: Main daily driver. Stored on top of Astron PS, connected
       to LDG AT-100ProII, then to coax run R1 (LMR-400, 75 ft).
       Used with Heil PR-781 mic + Heil headset. PC connected via
       USB for CAT control.
Photos: Drive folder /shack-photos/IC-7610-2022/
Receipt: HRO order #123456 (cloud-stored)
Insurance value claim base: 3500.00 (replacement)
```

## Common radio-inventory mistakes

- **Forgetting to update firmware version after upgrade.** The inventory drifts; future-you can't tell which version is loaded.
- **Skipping the serial number.** The most-important field; never skip.
- **Lumping multiple HTs into one entry.** Each gets its own row; their batteries and chargers may differ.
- **Using "$3500" as the estimated value when the radio is now used.** New-price ≠ used-price. Update annually.
- **Not photographing.** Pictures-as-documentation are worth a lot in insurance claims; do this once at acquisition.
- **Losing the manuals.** Save PDF copies in cloud; future repair work depends on them.

## See also

- §18-00 — Schema overview
- §18-05 — Accessories (mics, batteries, chargers)
- §18-06 — Serial numbers (what they're for, how to handle missing ones)
- §18-07 — Purchase dates (warranty status, depreciation)
- §18-08 — Firmware versions (tracking pattern)
- §20-02 — Firmware updates (when to bump the version)
- §19 — Estate / SK (where the radio inventory feeds the disposition plan)
