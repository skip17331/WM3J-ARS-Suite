---
id: 15-02
title: Antennas
chapter: 15
section: 02
level: simple
status: draft
---

# Antennas

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Antennas are the most-installed-then-forgotten part of the inventory. A wire dipole strung between two trees twelve years ago — does it still work? When was it last swept? What was the original cost? An automated rotator-and-Yagi system on the tower — what's the rotator's model, when was it serviced last? The shack inventory captures all of this so the answers are documented rather than guessed.

This section applies the unified inventory schema (§15-00) to antennas, with antenna-specific fields for installation context.

## Schema applied to antennas

| Core field | Antenna-specific notes |
|------------|------------------------|
| Manufacturer | DX Engineering, MFJ, Cushcraft, Hy-Gain, etc. — or "(homebrew)" for self-built |
| Model | Specific designation (e.g., "DXE-MBVE-5", "TH3-MK4", "G5RV") |
| Serial number | Often "(none)" for wire antennas; aluminum antennas sometimes have stamped serials |
| Date acquired | When the antenna components arrived (not necessarily install date) |
| Purchase price | Total spent on the antenna parts/kit |
| Estimated value | Resale value if disassembled — wire antennas have negligible resale |
| Disposition | Working / Repairable / Not Repairable |

Antenna-specific extra fields:

| Extra field | Why it matters |
|-------------|----------------|
| **Antenna type** | Dipole / vertical / Yagi / loop / EFHW / etc. |
| **Bands** | Which bands it covers — single, multi, all-band |
| **Install date** | When mounted in service (often different from acquisition date) |
| **Install location** | "Tower 1," "back yard tree," "roof mount," "attic," etc. |
| **Height / orientation** | Top elevation, broadside compass direction, polarization |
| **Last swept date** | When you last verified SWR with an analyzer |
| **Baseline SWR sweep** | A reference SWR-vs-frequency snapshot (file path or sheet reference) |
| **Condition notes** | Wear, weatherproofing status, hardware tension, etc. |

## Categories of antennas in the inventory

### Wire antennas

Dipoles, EFHWs, doublets, slopers — the cheap-but-effective family:

```
Item ID: A-001
Category: Wire Antenna
Type: 80m EFHW
Manufacturer: (homebrew, MyAntennas-style 49:1 unun)
Model: 65 ft EFHW for 80-10 m
Serial Number: (none)
Date Acquired: 2020-03-10
Install Date: 2020-03-15
Purchase Price: 80.00 (parts: wire + unun + insulators)
Estimated Value: 30.00 (the unun has resale; wire is salvage)
Disposition: Working
Bands: 80, 40, 20, 15, 10 m (resonant); 60, 30, 17, 12 m via tuner
Install Location: 65 ft wire run from house corner (S) to oak tree (NE)
Height: 30 ft at high end, 8 ft at low end
Last Swept: 2026-04-02 — SWR < 1.5 on 80, 40, 20, 15, 10
Notes: 49:1 unun in weatherproof box at high end; 5 ft counterpoise.
       Outer end re-sealed every spring (last: 2026-04-15).
```

### Vertical antennas

Ground-mounted or elevated, with or without radials:

```
Item ID: A-002
Category: Vertical Antenna
Type: HF multi-band vertical (no radials)
Manufacturer: DX Engineering
Model: DXE-MBVE-5 (5-band end-fed vertical)
Serial Number: (none on antenna)
Date Acquired: 2018-06-01
Install Date: 2018-06-15
Purchase Price: 750.00
Estimated Value: 500.00 (used; aluminum antennas hold value moderately well)
Disposition: Working
Bands: 80, 40, 20, 15, 10 m
Install Location: Back yard, 30 ft from house
Height: 24 ft (ground-mounted)
Last Swept: 2026-04-02 — SWR clean on all 5 bands
Notes: 4 elevated radials at 8 ft. Re-tightened mast hardware spring 2025.
```

### Beams (Yagis, log periodics)

Higher-investment items, often with rotators:

```
Item ID: A-003
Category: Beam
Type: 3-element triband Yagi
Manufacturer: Mosley
Model: TA-33-M
Serial Number: TA33-9876
Date Acquired: 2015-09-12
Install Date: 2015-10-01
Purchase Price: 850.00
Estimated Value: 600.00 (used)
Disposition: Working
Bands: 20, 15, 10 m
Install Location: Tower 1 at 50 ft
Last Swept: 2026-04-02 — clean; F/B 22 dB
Notes: Yaesu G-5500 rotator below.
       Last full inspection (climb): 2025-04-15 by W1ABC (climber).
       Hardware re-torqued; minor corrosion on aluminum joints addressed.
```

### Loops and other less-common forms

Magnetic loops, full-wave loops, delta loops:

```
Item ID: A-004
Category: Loop
Type: 80m full-wave skyloop
Manufacturer: (homebrew)
Model: 280 ft horizontal loop
Serial Number: (none)
Date Acquired: 2019-05-20
Install Date: 2019-06-01
Purchase Price: 60.00 (wire + insulators)
Estimated Value: 30.00 (salvage)
Disposition: Working
Bands: 80m (resonant); all others via balanced tuner
Install Location: Back yard, supported at 4 corners (one mast, three trees)
Height: 35 ft average
Last Swept: 2026-04-02 — clean on 80m; tunes through all other HF bands
Notes: Fed via 450-ohm ladder line + balun + tuner.
```

### Permanent antenna structures (tower, mast)

The supporting structure is also an inventory item:

```
Item ID: A-005
Category: Tower
Type: Crank-up tower
Manufacturer: US Tower
Model: HDX-555 (50 ft crank-up)
Serial Number: 555-23456
Date Acquired: 2014-11-01
Install Date: 2015-03-15
Purchase Price: 4500.00
Estimated Value: 4000.00 (towers hold value)
Disposition: Working
Install Location: SE corner of property
Notes: Concrete base 6'x6'x6' (calculated for 100 lb wind load).
       Inspected and lubricated annually.
```

## Antenna-specific inventory considerations

### Sweep documentation

Every antenna should have a **baseline SWR sweep** recorded with a NanoVNA or antenna analyzer. The sweep file (CSV or screenshot) is the reference for future inspections — if SWR has shifted significantly from baseline, something has changed.

Store sweep data with the inventory entry. Either:
- Reference the file path: `Sweep file: /shack-data/sweeps/A-001-2026-04.csv`
- Reference a Google Sheets tab: `Sweep tab: 'A-001-sweeps' in shack-inventory.xlsx`

Update on every annual inspection.

### Install date matters more than purchase date

For maintenance scheduling, **install date** is the relevant metric. A coax run installed 2020-06-01 is at year 6 of service; replace at year 10-15 depending on conditions (see §17-04).

### Material aging notes

Aluminum elements (Yagis, verticals) develop natural oxide layers — light gray. Black or green spots = water/salt corrosion; track and address. Wire antennas (copper, copper-clad steel) corrode at termination points; insulators degrade in UV.

The inventory's notes field should capture observed degradation: "Light corrosion at NW element joint, addressed with anti-corrosion paste 2025-04."

### Estimated value for antennas

Antenna resale follows different patterns than radios:

- **Wire antennas**: minimal resale; salvage value of wire + components (unun, balun) only.
- **Aluminum antennas (Yagis, verticals)**: moderate resale (40-70% of new) for popular models in good condition.
- **Towers**: hold value well (60-80% of new) — the manufacturing cost is in the steel, which doesn't depreciate fast.
- **Used components (rotors, mast hardware)**: modest resale.

For estate planning, the antenna's resale value is often less than the labor cost of removing it. Many estate sales include "antennas in place — buyer to remove." This affects the estate strategy in §16.

### Antennas marked "Repairable" or "Not Repairable"

Antenna failure modes:
- Visible damage (broken element, fallen insulator, water in balun) → typically Repairable.
- High SWR with no obvious cause → Repairable; diagnosis needed (could be coax, balun, or antenna proper).
- Off-resonance → Repairable; length adjustment.
- Interaction with new structures (gutter, tree growth) → Repairable via re-tune or relocation.
- Element corroded through, balun cracked open, structural failure → Not Repairable.

Document the failure mode in notes regardless of which disposition you pick.

### Weatherproofing inspection log

Antennas in service need annual re-sealing of connectors. Track in inventory:

```
Notes: Connector at feedpoint re-sealed 2025-04-15.
       Connector at balun re-sealed 2025-04-15.
       Next re-seal due 2026-04.
```

This becomes the annual maintenance schedule for the antenna.

## Multiple antennas per "system"

Some installations have multiple coordinated antennas. Track each as a separate row:

- The Yagi (one row).
- The mast/tower it's on (separate row).
- The rotator (separate row).
- The coax run from antenna to switch (separate row in §15-04).
- The antenna switch itself (in §15-05).

Cross-reference via notes: "Connected to coax run R-001; rotator A-006."

## Common antenna-inventory mistakes

- **Skipping wire antennas because "they're cheap."** Wire is cheap; the time and effort to install isn't. Track them like any other item.
- **Confusing acquisition date and install date.** Antenna parts may sit in a closet for months before installation. Track both.
- **No sweep documentation.** Without a baseline, future deviations aren't detectable.
- **Lumping a multi-antenna system into one entry.** Tower, antenna, rotator, coax: each its own row.
- **Assuming zero resale for old antennas.** Vintage Cushcraft beams and Hy-Gain antennas can have collector value; check before assuming salvage.
- **Forgetting to update install location after a move.** The antenna at "back yard, 30 ft from house" may have moved when you re-arranged the antenna farm.

## See also

- §15-00 — Schema overview
- §15-04 — Coax runs (linked to antennas)
- §15-05 — Accessories (rotators, switches)
- §04 — Antennas (full reference for antenna types)
- §10 — High-SWR troubleshooting (failure modes)
- §17-03 — Scheduled inspections (where antenna inspections are scheduled)
- §17-04 — Coax replacement (linked to coax-run inventory)
- §16 — Estate / SK (antenna disposition planning)
