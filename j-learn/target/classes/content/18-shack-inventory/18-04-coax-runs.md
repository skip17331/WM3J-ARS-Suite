---
id: 18-04
title: Coax Runs
chapter: 18
section: 04
level: simple
status: draft
---

# Coax Runs

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A coax run is a specific physical cable installed between two endpoints — typically rig to antenna, or antenna switch to antenna. Each run is its own inventory item because each has its own install date, its own length, its own connectors, and its own life cycle. A station with five antennas might have five to ten coax runs, each tracked separately.

This is the inventory category most-overlooked by operators new to inventorying. Treat coax runs as first-class items.

## Schema applied to coax runs

| Core field | Coax-run-specific notes |
|------------|--------------------------|
| Manufacturer | DX Engineering, Times Microwave, Belden, Andrew, etc. |
| Model | Cable type (RG-58, RG-213, LMR-400, LDF4-50A, etc.) |
| Serial number | Often "(none)" — coax doesn't carry serials; lot/batch number from purchase if available |
| Date acquired | When the coax was purchased (from spool) |
| Purchase price | Cost of the cable + connectors at time of install |
| Estimated value | Salvage value (low for old, weathered cable; near-original for unused new spool) |
| Disposition | Working / Repairable / Not Repairable |

Coax-run-specific extra fields:

| Extra field | Why it matters |
|-------------|----------------|
| **Cable type** | RG-58, RG-213, LMR-400, etc. (cross-references §22-01) |
| **Length (feet)** | Total physical length |
| **Install date** | When this specific run was installed |
| **Endpoints** | "From" and "To" — what the cable connects |
| **Connectors at each end** | PL-259, N, BNC, etc. (cross-references §22-04) |
| **Routing** | Indoors / outdoors / buried / above-ground / inside conduit |
| **Last swept date** | When loss/SWR was last measured |
| **Last weatherproofed** | When connectors were last re-sealed |

## Categories of coax runs

### Antenna feedline run

The classic case — coax from a station endpoint to an antenna:

```
Item ID: R-001
Category: Coax Run
Cable Type: LMR-400
Manufacturer: Times Microwave
Model: LMR-400 Original
Serial Number: (none) - lot 2018-Q2
Date Acquired: 2018-06-01
Install Date: 2018-06-15
Purchase Price: 110.00 (100 ft + 2 connectors + heat shrink)
Estimated Value: 50.00 (salvage, given age)
Disposition: Working
Length: 100 ft
Endpoints: From shack bulkhead (S-1) → To antenna A-002 (vertical)
Connectors: PL-259 (shack end), N-female (antenna end)
Routing: Buried (12" deep) from shack to antenna base
Last Swept: 2026-04-02 — matched-line loss 0.45 dB at 14 MHz (matches spec)
Last Weatherproofed: 2025-04-15 (annual)
Notes: Original install. No visible jacket damage. Working as expected.
       Plan replacement at 2028 (10 years) per §20-04.
```

### Patch cables (shack interconnects)

Short cables inside the shack:

```
Item ID: R-002
Category: Coax Run (patch)
Cable Type: RG-8X
Manufacturer: DX Engineering
Model: 9258-equivalent
Serial Number: (none)
Date Acquired: 2022-04-15
Install Date: 2022-04-15
Purchase Price: 25.00 (5 ft + 2 PL-259)
Estimated Value: 5.00
Disposition: Working
Length: 5 ft
Endpoints: From IC-7610 → To LDG AT-100ProII
Connectors: PL-259 both ends
Routing: Indoor
Last Swept: 2026-04-02 — clean
```

### Antenna switch / matrix runs

Coax connecting an antenna switch to each antenna:

```
Item ID: R-003
Category: Coax Run
Cable Type: LMR-240
Manufacturer: Times Microwave
Model: LMR-240
Serial Number: (none)
Date Acquired: 2020-08-10
Install Date: 2020-08-15
Purchase Price: 85.00 (75 ft + connectors)
Estimated Value: 30.00
Disposition: Working
Length: 75 ft
Endpoints: From antenna switch (S-2 port 4) → To antenna A-001 (EFHW unun)
Connectors: PL-259 (switch end), PL-259 (antenna end with reducer for unun)
Routing: Outdoors, run along soffit and tied to rope
Last Swept: 2026-04-02 — clean
Last Weatherproofed: 2025-04-15
```

## Coax-run-specific inventory considerations

### Why "from"/"to" endpoints matter

Without endpoints, a "100 ft of LMR-400" inventory entry tells you almost nothing about the actual installation. Knowing endpoints lets you:

- **Identify which run is which** during troubleshooting.
- **Plan a replacement campaign** — replace the longest, oldest run first.
- **Understand the antenna system topology** without re-tracing every cable.

Use unique IDs (R-001, R-002, etc.) to make endpoints clear; cross-reference radio inventory (R-001 connects to "Rig 1" or specific endpoint).

### Sweep documentation

For each run, record:

- **The matched-line loss** measured with a NanoVNA terminated in 50 Ω.
- **The frequency** at which loss was measured.
- **The date** of the measurement.

This becomes the baseline. When the run is re-swept (annually, or after any damage), compare to baseline. A 30%+ increase in loss = the run is approaching end of life.

### Connector inventory

The connectors themselves are part of the cable's installation. Track them:

- What type at each end (PL-259, N, BNC, SMA).
- Solder vs. crimp (relevant for re-termination work).
- Original vs. replacement (some connectors get swapped out over time).

Failures often occur at connectors before failing in the cable proper; tracking them helps diagnose.

### Routing notes

Routing affects life expectancy:

- **Indoor**: 20+ years typical.
- **Outdoor exposed (sun, weather)**: 8-15 years.
- **Outdoor buried**: 10-15 years for direct-burial-rated cable; 5-7 years for non-DB cable.
- **In conduit**: similar to indoor.
- **Through conduit with drainage**: longer than direct-bury for non-DB cable.

The routing notes inform the maintenance scheduling.

### When to mark "Repairable" or "Not Repairable"

Coax runs hit a non-Working disposition when:

- **Visible damage**: kinked, cut jacket, water in dielectric, animal damage.
- **Measured loss exceeds spec by 30%+**: aging has degraded the cable.
- **Physical disconnect at a connector**: connector pulled off, water has destroyed contact.
- **Sustained high SWR with no antenna change**: usually feedline (often connector) failure.

For coax, the practical disposition is usually **Not Repairable** — a failed coax run is essentially "needs replacement," and you can't effectively splice or patch a damaged run while keeping its electrical characteristics. Mark it Not Repairable and plan replacement (§20-04). The exception: a connector failure on an otherwise-good cable is **Repairable** — re-terminate the bad end.

### "Not Repairable" for coax = parts donor

Old coax can have value as a parts donor:

- Cut into short patches for bench-test cables.
- Strip for connector recovery (PL-259, N, BNC bodies).
- Salvage center conductor (copper).
- Educational sectioning for show-and-tell.

A run pulled from outdoor service can serve as bench cable in the shack at QRP power for years.

## Cable type aging

Different cable types age differently. The inventory should track when each type was installed:

| Cable type | Outdoor lifetime | Notes |
|------------|-------------------|-------|
| RG-58 | 5-10 years | Smaller jacket, faster UV degradation |
| RG-8X | 7-12 years | Slightly tougher than RG-58 |
| RG-213 | 10-20 years | Original mil-spec; durable |
| LMR-400 | 10-20 years | Modern equivalent of RG-213 |
| LMR-400-DB (direct burial) | 15-20 years buried | DB jacket withstands water |
| Hardline | 25-40+ years | Longest-lived; periodic re-termination needed |

Inventory's install date + cable type tells you when to plan replacement.

## Photographing coax runs

For long permanent installs, photograph:

- The route from a high vantage point (so you can see where it goes).
- Each connector and entry point.
- Any unusual junctions (splice, lightning arrestor, etc.).

Photos taken at install + photos taken at annual inspection let you compare changes over time. Storage with the inventory.

## Common coax-run inventory mistakes

- **Lumping multiple runs into one entry.** Each cable, each endpoint pair, is its own row.
- **Forgetting to record install date.** Without it, you don't know when to plan replacement.
- **Skipping connector type documentation.** Replacement cables need connector consistency; record what's at each end.
- **No baseline sweep.** Without it, future deviations aren't detectable.
- **Treating salvage cable as "Not Repairable" prematurely.** A run pulled because it looked old may still test fine; sweep before assuming.

## Connecting to estate planning

For estate disposition (§19): coax runs are typically not separately resaleable. A buyer of the station wants the radios, antennas, and accessories; the coax in place is replaced or kept by the seller. Document estimated value as low-but-not-zero (salvage of connectors/copper).

For insurance: coax runs are typically not separately claimable; they're part of the antenna system value. Document with the antenna entries (cross-reference R-001 from A-002's notes).

## See also

- §18-00 — Schema overview
- §18-02 — Antennas (linked to coax runs)
- §22-01 — Coax types (full reference)
- §22-04 — Connectors
- §22-02 — Loss tables
- §20-04 — Coax replacement
- §13-01 — Coax issues troubleshooting
- §19 — Estate / SK (coax not typically separately disposed)
