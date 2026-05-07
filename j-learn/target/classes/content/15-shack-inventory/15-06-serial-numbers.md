---
id: 15-06
title: Serial Numbers
chapter: 15
section: 06
level: simple
status: draft
---

# Serial Numbers

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The serial number is the **single most-important field** in the equipment inventory. It's the only attribute that uniquely identifies a specific unit (everything else can apply to multiple identical units). For insurance claims, theft recovery, warranty service, firmware version tracking, and resale documentation, the serial number is the primary key.

Recording serial numbers is the discipline that separates a "list of stuff I own" from an inventory.

## Why serial numbers matter

### Insurance claims

When a claim is filed, the insurance company asks: what was the brand, what was the model, what was the serial number? Without the serial, a claim becomes generic: "an Icom HF radio." The insurer may ask follow-up questions or limit the payout.

With the serial: "an Icom IC-7610, serial 0123456789, purchase price $3500 from HRO on 2022-04-15, currently valued at $2800." Specific, documented, supportable.

For high-value items ($1000+), serial numbers are the difference between a claim that goes through and a claim that's challenged.

### Theft recovery

Manufacturers maintain "stolen radio" registries. When you report a theft:

1. Contact the manufacturer's customer support (Icom, Yaesu, Kenwood all have stolen-radio reporting).
2. Provide the serial number and date/circumstances of theft.
3. The serial is flagged in their database.
4. If the radio is later sent in for service, sold via dealer, or shows up in pawn-shop databases, it gets caught.

This is **only possible if you have the serial number**. Without it, recovery is essentially impossible.

The same applies to amateur radio classifieds (QRZ, eHam, etc.) — buyers commonly check serial numbers against stolen lists before purchase. Sellers without documented serials are flagged as suspicious.

### Warranty and service

Manufacturers track warranty status by serial number. Sending a radio in for service requires the serial; the manufacturer looks up:

- Original purchase date.
- Warranty status (active, expired, extended).
- Service history.
- Known issues with that production batch.

Without the serial, service may be possible but more cumbersome.

### Firmware version tracking

Many radios store firmware version associated with the serial. When you check "what firmware is on the IC-7610?", the rig reports a version that's tied to its serial. The inventory's firmware-version field should match.

### Resale documentation

A buyer of a used radio wants to verify it's not stolen. They ask for the serial number. You provide it; they verify against stolen-radio registries. Without a documented serial, you can't make this verification, and the resale is harder.

## How to read a serial number

Serial number format varies by manufacturer:

| Manufacturer | Format | Example | Where to find it |
|--------------|--------|---------|------------------|
| Icom | 10 digits, sometimes prefixed | 0123456789 | Sticker on bottom of rig; battery compartment for HTs |
| Yaesu | 8-9 alphanumeric (year + sequential) | 8M0987654 | Sticker on bottom; first chars often year code |
| Kenwood | 8-10 alphanumeric | 12345678 | Sticker on rear or bottom |
| Elecraft | 6-7 digits | 12345 | Sticker on rear |
| FlexRadio | 6-9 digits | 654321 | Sticker on bottom; also via SmartSDR software |
| MFJ | 6-8 alphanumeric | 989D-12345 | Sticker on rear |
| Heil (microphones) | 4-6 alphanumeric | PR-987654 | Sticker on cable strain relief or unit base |
| BaoFeng / Quansheng | Often missing | (none) | If present: under battery; usually no exterior |

## Recording the serial number

### Take a photo

Photograph the serial number plate. Store with the inventory entry. The photo is:
- Backup against transcription errors.
- Proof of ownership for insurance.
- Visual reference if the original sticker degrades.

A 30-second photo per item is the minimum.

### Record in inventory exactly as printed

If the sticker shows "S/N: 0123456789", record exactly "0123456789" (without prefix). Or with — but be consistent. The matching algorithm against stolen-radio registries needs exact match.

### Where to look on each radio class

| Class | Typical serial location |
|-------|-------------------------|
| HF base radio | Bottom panel; sometimes side panel |
| HF mobile | Bottom; sometimes inside the front panel under microphone connector |
| HT | Inside battery compartment; on the back of the battery if removable |
| Amplifier | Rear panel near power connector |
| Power supply | Rear panel |
| Antenna analyzer | Rear panel; sometimes inside battery compartment |
| Tuner | Rear panel |
| Test equipment (NanoVNA, Bird) | Rear panel or label on case |

For accessories with no visible serial (BaoFeng HT batteries, no-name accessories): record "(none)" in the inventory and rely on the model number + acquisition date + photograph for identification.

## When the serial number is missing

Several scenarios:

### No serial was ever printed

Common for consumer electronics: BaoFeng HTs, no-name accessories, kit-built equipment. Record "(none on device)" and proceed with model + photograph.

### Serial worn off or damaged

Stickers on the bottom of mobile radios can wear off in vehicles. Aluminum chassis with engraved serials sometimes get scratched. If the original is illegible:

- Check if the serial is also stored in software (the rig's "About" screen may show it).
- Contact the manufacturer with the model and circumstances; they may be able to identify the unit by service history.
- Document "(serial illegible; original was 0123XXXX)" in inventory if you can recall some characters.

### Sticker has come off (unattached but in a drawer)

If you have the original sticker, scan or photograph it and store with the inventory. Note "(sticker preserved separately, see /docs/IC-7610-sticker.jpg)".

### Replacement under warranty

If a unit was warranty-replaced, the serial changes. Update the inventory to the new serial; note in the "Notes" field "Original serial 0123456789 replaced under warranty 2024-03; current serial 9876543210."

## Serial number formats and patterns

Some manufacturer serial number patterns reveal information:

### Year prefixes

Yaesu serials often start with a 2-character year code:
- "8M" = 2008 manufacture
- "9A" = 2009
- "0J" = 2010
- etc.

(The exact mapping varies by Yaesu's internal scheme; this is approximate.)

This means an inventory entry's serial can verify the manufacture date. A "1968 Heathkit" with a serial starting "21-..." would be suspicious.

### Sequential numbers

Many manufacturers use sequential numbers within a model:
- IC-7610 #0001 was the first ever made.
- IC-7610 #15000 is the 15,000th (approximately).

This lets researchers/collectors estimate the production run and rarity. For vintage gear, an early serial number can add collector value.

### Lot codes

Some accessories include lot codes (e.g., date of manufacture for batteries). Useful for tracking which production batch had a particular issue.

## Recording serial numbers in the spreadsheet

Format options:

- **Plain text in a single cell**: simplest; works for any format.
- **Hyphen-separated codes**: if the printed serial uses hyphens, preserve them (e.g., "989D-12345").
- **All-numeric, prefixes added in notes**: cleanest if you're sorting by serial.

Be consistent within the inventory.

## Periodic verification

Once a year, verify the recorded serials against the actual equipment. Quick check:

1. Pull out the inventory list.
2. For each high-value item, find it physically.
3. Verify the serial number sticker is intact and matches the inventory.
4. If a sticker has come off or is unreadable, address now (re-attach, photograph, etc.).

Annual verification catches transcription errors and degrading stickers before they become problems.

## Insurance documentation kit

For each high-value item ($500+), assemble:

- **Serial number** in inventory.
- **Photo of the serial number plate**.
- **Photo of the item in service**.
- **Receipt PDF** (cloud-stored).
- **Manufacturer's product page bookmark** (in case you need to verify model details later).

Keep these accessible (not in the burning house). Cloud storage with strong access control is ideal.

## Stolen-radio reporting

If you discover a theft:

1. **File a police report** immediately. Get a case number.
2. **Notify the manufacturer** with the serial(s). Most manufacturers have a "report stolen equipment" form online.
3. **Notify amateur radio publications** (QST, eHam) — they often publish stolen lists.
4. **Notify insurance** with the documentation.
5. **Watch the local market** (Craigslist, Facebook Marketplace, eBay) for the serial.

Theft recovery is rare but happens — usually from manufacturer service records when the thief tries to get the radio repaired.

## Common serial-number inventory mistakes

- **Skipping serial numbers because they're "tedious."** They're the most important field. Take 5 minutes per item.
- **Recording approximate or partial serials.** The manufacturer's stolen-radio match requires exact characters; "0123456-something" won't trigger.
- **Photographing serial numbers but not transcribing.** The photo is a backup; the inventory's text field is the searchable record.
- **Trusting that the serial sticker will last.** Stickers degrade. Photograph and transcribe at acquisition.
- **Not updating after warranty replacement.** A new serial means the old one is no longer the unit you own.

## See also

- §15-00 — Schema overview
- §15-01 — Radios (where serials are most-important)
- §15-05 — Accessories (some have serials, some don't)
- §15-07 — Purchase dates (paired with serials for warranty)
- §16 — Estate / SK (serial-documented items have higher resale value)
