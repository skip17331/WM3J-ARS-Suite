---
id: 15-00
title: Shack Inventory — Overview
chapter: 15
section: 00
level: simple
status: draft
---

# Shack Inventory — Overview

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A working amateur station accumulates equipment over the years — radios, amplifiers, tuners, antennas, rotators, coax runs, accessories, computers, software licenses, batteries, books, parts. Some of it is current and well-loved; some is in service but aging; some is in a closet for the day you finally finish that project; some you barely remember buying.

A **shack inventory** is the written record of all of it. Done well, it gives you:

- **Insurance** — a documented value to claim if a tornado, fire, or theft destroys the station.
- **Estate planning** — a record your family can hand to a buyer or executor when you become a Silent Key, so your gear ends up in the hands of operators who'll use it instead of being sold for pennies at a yard sale (see §16).
- **Operational decisions** — knowing what you have helps decide whether to repair, replace, or upgrade.
- **Tax records** — purchase history for any business deductions or capital-gains calculations on resale.
- **Maintenance scheduling** — install dates flag when coax should be replaced, when batteries are end-of-life, when firmware is overdue.

This chapter covers the data structure for an inventory and the practical guidance for each common equipment category. The data structure is consistent across all categories so the **same fields** describe a radio, an antenna, a battery, or a power supply.

## The unified schema

Every item in the inventory uses the same core fields:

| Field | Description |
|-------|-------------|
| **Type** | Equipment category (Radio, Antenna, Tuner, Coax Run, Power Supply, etc.) — references the equipment_types table |
| **Manufacturer** | The maker (e.g., Icom, Yaesu, Kenwood, Diamond, MFJ) |
| **Model** | The specific product designation (IC-7610, FT-991A, MA-5B, etc.) |
| **Serial number** | The unique identifier on the unit (essential for insurance, theft recovery) |
| **Date acquired** | When the item came into your possession (yyyy-mm-dd) |
| **Purchase price** | What you paid for it, in original currency |
| **Estimated value** | Current market value (what it would sell for today on eBay or QRZ classifieds) |
| **Disposition** | Working / Repairable / Not Repairable |
| **Install status** | Installed (in service) / Storage (boxed up, not currently in use) |
| **Storage location** | Where it's stored if boxed (e.g., "basement shelf 3"; only used when install_status = Storage) |
| **Notes** | Free-form text — context, history, accessories included, anything else |

Some categories add a few extra fields (firmware version on radios, installed length on coax runs, last-swept date on antennas), but the fields above are the constant baseline.

## The disposition values

Three values, deliberately limited:

| Disposition | Meaning |
|-------------|---------|
| **Working** | Functions to spec; ready to use today; no known issues |
| **Repairable** | Doesn't currently work, but the fix is known/feasible — needs a tube, a cap, a connector, etc. Worth keeping or selling at "needs work" pricing |
| **Not Repairable** | Cannot be economically repaired. Either parts donor for similar equipment, or scrap |

Three buckets are enough. Fine-grained categories ("partially working," "intermittent," "almost dead") tend to drift over time and confuse the inventory. **If it works, it's "working"; if it doesn't, you have to decide between "repairable" (worth fixing or selling as a fixer-upper) and "not repairable" (parts donor or scrap).**

## Install status & storage location

Two related fields that track *where the item physically is*:

| Install status | Meaning |
|----------------|---------|
| **Installed** | Currently in service — connected to the station, on the tower, in the vehicle, etc. |
| **Storage** | Boxed up, off the air. Not currently being used. |

When install_status is **Storage**, the **storage_location** field captures *where* the item is — "basement shelf 3," "garage cabinet," "shack closet," "rental storage unit B-204," etc.

This matters for two reasons:

1. **Inventory verification**: when you do an annual review (§17-03), you can find each item physically. Without a storage location, "I have a spare 2 m radio somewhere" doesn't help.
2. **Estate planning**: when your family disposes of the station (§16), the printable handoff lists *where* each item physically is. Otherwise they spend hours hunting through boxes.

Update the storage location whenever you move things — moving a radio from the operating desk to the closet means flipping it to "Storage" and adding the location.

## How the chapter is organized

| § | Topic | What it covers |
|---|-------|----------------|
| 18-01 | Radios | Transceivers, receivers, HTs; the schema applied + radio-specific fields |
| 18-02 | Antennas | The schema applied to antennas + install/condition specifics |
| 18-03 | Tuners | Antenna tuners, ATUs; the schema + tuner-specific fields |
| 18-04 | Coax runs | The cable runs themselves, indexed as inventory items |
| 18-05 | Accessories | Power supplies, audio gear, computers, software, books, parts |
| 18-06 | Serial numbers | Why they matter, how to record them, what to do if lost |
| 18-07 | Purchase dates | Depreciation, warranty status, replacement planning |
| 18-08 | Firmware versions | Tracking firmware across the equipment fleet |

## Where to keep the inventory

Three options, with tradeoffs:

| Format | Pros | Cons |
|--------|------|------|
| **Spreadsheet (Excel, Google Sheets, Airtable)** | Easy to start; flexible columns; sortable; cloud-synced | Risk of corruption; no built-in audit history |
| **J-Hub Inventory Tab** | Integrated with the rest of the suite; backed by SQLite; portable | Limited to suite users |
| **Paper notebook** | No power, no software, no failure modes | Manual entry; no search; awkward to update |
| **Plain-text Markdown** | Greppable; version-controlled (git); future-proof | No tables for non-technical readers |

For most operators, **a spreadsheet** (one row per item, columns matching the schema) is the right answer. Cloud-synced (Google Sheets) so it survives a house fire; printable so it can be handed to an insurance adjuster or executor.

The J-Hub Inventory Tab is in development and will eventually offer a structured database with the same schema; for now, a spreadsheet works.

## A starter spreadsheet template

Header row:

```
Category | Manufacturer | Model | Serial Number | Date Acquired | Purchase Price | Estimated Value | Disposition | Notes
```

Sample data:

```
Radio    | Icom         | IC-7610 | 0123456789  | 2022-04-15  | 3500.00       | 2800.00         | Working   | Daily driver
Radio    | Yaesu        | FT-857  | 8M0987654   | 2010-08-20  | 700.00        | 250.00          | Working   | Mobile install in truck
Antenna  | DX Engineering | DXE-MBVE-5  | n/a    | 2018-06-01  | 750.00        | 500.00          | Working   | Vertical at NW corner
Antenna  | (homebrew)   | 80m EFHW    | n/a    | 2020-03-15  | 80.00         | 80.00           | Working   | Wire run W to E across yard
Coax     | DX Engineering | LMR-400 | n/a       | 2018-06-01  | 110.00        | 50.00           | Working   | 100ft run to vertical
Tuner    | LDG          | AT-100PROII | 1234567 | 2015-11-30  | 250.00        | 175.00          | Working   | Inline with HF rig
Battery  | Bioenno      | BLF-1220A   | BL12345 | 2021-09-15  | 200.00        | 150.00          | Working   | Emcomm battery
Amp      | Heathkit     | SB-220      | 2-3456  | 2008-12-01  | 800.00        | 600.00          | Repairable | Tube needs replacement; project
PS       | Astron       | RS-50A      | 50A-9876 | 2009-01-15 | 220.00        | 130.00          | Working   | Main HF supply
HT       | Yaesu        | FT-3DR      | (none)   | 2023-02-10 | 280.00        | 220.00          | Working   | Daily HT
HT       | BaoFeng      | UV-5R       | (none)   | 2018-05-01 | 35.00         | 20.00           | Not Repairable | Backup; LCD damaged
```

The "Category" column is what links each row to its chapter section in this guide. Filter by category and you get the radios subset, the antennas subset, etc.

## Why "estimated value" matters

Original purchase price tells you what you spent. **Estimated value** tells you what the inventory is worth today.

For insurance: if a fire takes the station, the insurance company wants the replacement-value or current-market-value figure, not the original-purchase price. An IC-7610 that sold for $3500 in 2022 is now $2800 used; replacing with current new gear is $3800. Insurance valuation depends on whether your policy is "replacement cost" or "actual cash value" — review your policy wording.

For estate planning: when your family sells the station, they need to know what the gear is *actually worth*, not what you paid. A 1970s Heathkit that cost $300 may now be worth $1500 to a vintage collector, or $50 to a parts buyer — depends on condition.

**Update estimated values annually.** A 30-minute spreadsheet review keeps the numbers current. Use eBay completed-listings, QRZ classifieds, and ham forums to spot-check.

## What this chapter does not cover

- **Personal items not directly related to amateur radio** — tools, computers used for non-radio work, etc. (Track separately if you want; not the scope of this chapter.)
- **Software licenses** — covered briefly in §15-05; full software inventory is a different beast.
- **Insurance policy details** — talk to your agent; this chapter feeds the inventory portion of the insurance discussion.
- **Estate planning legal questions** — see §16; that chapter covers what to do *with* the inventory data.

## What you will get from this chapter

- A schema you can apply to any equipment item.
- Per-category guidance for the unique aspects of radios, antennas, etc.
- A starter spreadsheet template.
- The basis for the estate module (§16) which uses this data to plan equipment disposition.

## See also

- §16 — Estate / SK (uses this inventory data)
- §17 — Maintenance (install dates and condition feed maintenance scheduling)
- §19 — Coax & connectors (reference for coax-run inventory)
- §04 — Antennas (reference for antenna inventory)
