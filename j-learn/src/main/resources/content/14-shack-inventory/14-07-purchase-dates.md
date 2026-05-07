---
id: 14-07
title: Purchase Dates
chapter: 14
section: 07
level: simple
status: draft
---

# Purchase Dates

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The date you acquired an item is the simplest field in the inventory and the easiest one to forget to record at the moment of acquisition. Without it, you can't compute warranty status, depreciation, expected remaining life, or replacement timing. With it, the inventory becomes a living record of the station's age profile.

This section covers what dates to track, in what format, and how they're used.

## Two dates to distinguish

For most items, two dates matter:

| Date | Meaning |
|------|---------|
| **Date acquired** | When the item came into your possession (purchase, gift, auction, transfer) |
| **Date installed / put in service** | When the item was first used or installed in its current role |

For radios in continuous service, the two are often the same week or month. For antennas (parts ordered, then installed weeks later) or backup gear (purchased, then sat unused for years), they differ significantly.

The schema's "Date acquired" field captures the first date. Add a "Date installed" field where the distinction matters (antennas, coax runs, batteries kept on the shelf for emergency use).

## Format: ISO 8601

Use **YYYY-MM-DD** format consistently:

- ✅ "2022-04-15"
- ❌ "April 15, 2022"
- ❌ "4/15/22"
- ❌ "15-04-2022"

ISO 8601 sorts correctly in any spreadsheet, parses unambiguously, and avoids the US/UK date-format confusion. Use it for every date field.

## Why purchase date matters

### Warranty status

Most amateur equipment has 1-3 year manufacturer warranty (some longer for premium gear). The warranty starts from the original retail purchase date — knowing this date tells you whether warranty service is still available.

Track:
- Date acquired (warranty start).
- Warranty length (typically in the manual or on the receipt).
- Warranty end date (computed: acquired + warranty length).

When something breaks, check whether warranty applies before paying for repair.

### Depreciation tracking

Equipment loses value over time, but at different rates by category:

| Category | Typical depreciation curve |
|----------|----------------------------|
| Modern HF transceiver | 25-40% in year 1; ~10% per year thereafter |
| Modern HT | 30-50% in year 1; faster after model is replaced by newer |
| Yagi antenna | 10-15% per year for 5 years; flatter thereafter |
| Wire antenna | 30-50% in year 1; minimal thereafter (functional value) |
| Power supply (linear) | 10-15% per year for 5 years; minimal thereafter |
| Power supply (switching) | 25-40% in year 1; faster as efficiency tech improves |
| Vintage gear (40+ years) | Stable or appreciating (collector market) |
| Hardline coax | Minimal depreciation while in service; salvage value at end-of-life |

For estate planning, knowing the purchase date lets you apply a depreciation curve and arrive at a reasonable current value.

### Replacement planning

Knowing when something was put in service tells you when to plan replacement:

| Item | Typical service life |
|------|----------------------|
| Coax (outdoor) | 8-15 years |
| Battery (AGM) | 5-8 years |
| Battery (LiFePO₄) | 8-15 years |
| Power supply (linear) | 20+ years |
| Power supply (switching) | 5-15 years |
| Rotator | 10-20 years (mechanical wear) |
| HF transceiver | indefinite (firmware updates extend life) |
| Wire dipole | 5-10 years (UV, weather) |
| Aluminum vertical | 15-25 years |

Looking at the inventory's install dates, you can identify items approaching end-of-life and plan replacement before failure.

### Tax records

For amateurs running a business presence (consulting, contesting prizes, club support), purchase dates and prices feed:

- **Depreciation schedules** (Section 179, 5-year property, etc.).
- **Capital-gains calculations** when equipment is sold.
- **Charitable-donation values** for equipment given to clubs.

Most amateurs don't have business deductions, but the documentation prepares for the case.

### Personal recall

Beyond practical purposes: knowing when you got each piece of gear is a useful memory aid. "When did I buy that radio?" — looking at the inventory tells you.

## Tracking related dates

Beyond "Date acquired," several related dates matter:

### Date installed / first put in service

Different from acquisition. A power supply purchased 2018-01-15 but installed 2018-09-15 has 8 months of "unused" time before it began aging. Some items care about this difference (batteries especially).

```
Date Acquired: 2018-01-15
Date Installed: 2018-09-15
Notes: Sat unused 8 months between purchase and install.
```

### Date last serviced

For items that need maintenance (rotators, capacitors, fans):

```
Date Acquired: 2014-03-15
Date Last Serviced: 2024-04-15 (rotor brake adjusted, gears greased)
Notes: Annual service. Next due 2025-04.
```

This becomes the source for the maintenance schedule.

### Date warranty expires

```
Date Acquired: 2024-09-15
Warranty End: 2026-09-15 (2-year manufacturer warranty)
```

Calendar your warranty expiration so you know when extended warranty is no longer available.

### Date moved between stations

If equipment moves between QTHs (rental, retirement to a winter home, etc.):

```
Date Acquired: 2020-05-15 (purchased for primary station)
Date Moved: 2024-08-01 (to winter home in FL)
Notes: Moved as part of seasonal station setup.
```

## Spreadsheet templates with date fields

A useful set of date columns:

```
Item ID | ... | Date Acquired | Date Installed | Date Last Inspected | Date Last Serviced | Warranty End | Replacement Due
```

For most items, only "Date Acquired" matters. The rest are filled in only where applicable.

## Recording the date at acquisition

The discipline of recording the date when you receive equipment:

- **Order receipt or invoice has the date.** Use the receipt date.
- **Email confirmation has the date.** Use the order date.
- **Used purchase via QRZ classifieds or eBay.** Use the date the item arrived (not the purchase date — they may differ by a week+).

If the date isn't certain, record an approximate date (e.g., "2018-06" for "June 2018"). Better to have an approximate date than no date.

## Computing approximate dates for old items

If you have an old item and don't know when you got it:

1. **Check the manufacturer's production date** (sometimes derivable from serial number; see §14-06).
2. **Check tax / receipt records** if you have them.
3. **Estimate based on memory** — record as "circa YYYY" if precise dating isn't possible.
4. **Use install evidence** — coax replaced in 2015 means the antenna was installed by 2015 at latest.

For very old items (vintage gear), exact dates may not be recoverable. Approximate is fine.

## Date-related estate considerations

For estate planning (§15):

- **Purchase price + acquisition date** establishes the cost basis for capital-gains calculation if the equipment is sold.
- **Recent purchase + good condition** = high resale value.
- **Old purchase + good condition** = collector value (potentially higher than replacement value).
- **Recent purchase + poor condition** = low resale (something's wrong).
- **Old purchase + poor condition** = not repairable / parts donor.

The date tells the story.

## Common date-tracking mistakes

- **Skipping the date because "I'll remember."** You won't. Record at acquisition.
- **Using inconsistent date formats.** ISO 8601 always.
- **Forgetting to update install date when an item moves.** A vertical moved from one location to another has a new install date.
- **No follow-up dates.** "Date Acquired" alone doesn't track the maintenance cycle. Add "Date Last Inspected" and "Date Last Serviced" for items that need them.
- **Missing dates on used acquisitions.** Even for items bought used, record when *you* acquired it; that's what matters for *your* inventory.

## See also

- §14-00 — Schema overview
- §14-06 — Serial numbers (paired with date for warranty)
- §14-08 — Firmware versions (firmware update dates)
- §16-00 — Maintenance overview (where dates feed the schedule)
- §16-01 — Battery maintenance (calendar age matters)
- §16-04 — Coax replacement (install date sets replacement schedule)
- §15 — Estate / SK (acquisition date supports resale)
