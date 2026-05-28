---
id: 16-03
title: Scheduled Inspections
chapter: 16
section: 03
level: simple
status: draft
---

# Scheduled Inspections

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The discipline of inspecting your station on a calendar — not in response to symptoms — is what separates stations that work reliably for ten years from stations that limp along until something breaks badly. Most degradation is gradual: water creeping under a connector boot, UV slowly cracking a coax jacket, terminal corrosion advancing one millimeter a year. Catch it during inspections; fix it before it becomes a fault.

This section is the inspection checklist, organized by frequency.

## Monthly checks (10 minutes each)

These are the lightweight checks you do without leaving the shack:

- **Sweep all antennas** with the analyzer. Compare results to your baseline (the SWR plot you saved when you knew everything was working). A 0.2:1 SWR shift is normal weather variation; a 1:1 shift indicates something has changed — investigate.
- **Note the noise floor** on each band you use. A 6+ dB increase from baseline indicates a new noise source somewhere (yours or someone else's; see §15).
- **Check rig logs / error counters.** Most modern rigs and accessories track error counts (high SWR fold-back events, CAT errors, USB disconnects). A sudden uptick means something has degraded.
- **Float-charger status.** Verify the green "float" LED is on, no red errors.
- **Visual scan of the shack**: any cables warm to the touch? Any connectors visibly loose? Any error lights on?

Do this on the same day every month (the first Saturday, the last Sunday — pick one and stick with it). Log the results, even one line: "2026-04-01 monthly: 80m SWR 1.4, 40m 1.2, 20m 1.3, all unchanged from March."

## Quarterly walks (30 minutes each)

Once every three months, take 30 minutes to walk the station with a clipboard:

### Outside the house

- **Antenna ground inspection** with binoculars. Look for:
  - Bent or sagging elements (Yagi)
  - Drooping wire (dipole, EFHW) — note the sag at center
  - Visible water staining at connectors or balun enclosures
  - Bird damage, squirrel damage, branch contact
  - Loose hardware (clamps, U-bolts, mast guy lines)
- **Coax routing**. Walk the run from antenna to entry point. Look for:
  - UV cracking on jacket (small white cracks running along the cable)
  - Animal chewing
  - Sharp bends (radius below the manufacturer's minimum)
  - Bare metal touching coax
  - Coax loops near house electrical (creates RFI risk; see §14)
- **Grounding and bonding**. Check ground rod connections, lightning protectors, station ground bus:
  - Visible corrosion at clamps?
  - Stranded copper wire still flexible (not work-hardened/breaking)?
  - Ground rod still well-driven (some rods drift up over years from frost cycling)?
  - Lightning arrestors still intact (no charred or cracked housings)?
- **Mast / tower (from ground)**. With binoculars:
  - Plumb? (look up along the mast; should be straight)
  - Guy lines tight? (look at the catenary; if a guy is sagging visibly, retension)
  - No visible rust at attachment points
  - Hardware tight at base
- **House entry point**. The bulkhead where coax enters:
  - Sealing intact (silicone, tape, foam — whatever you used)?
  - Drip loops in place?
  - Any visible water staining on the house wall below the entry?

### Inside the shack

- **Cable management**. Has anything migrated? Is anything pinched, kinked, or under tension?
- **Connection check**. Wiggle each PL-259 / N / BNC. None should move. If one is loose, retighten.
- **Filter status**. RF filters, lightning arrestors, common-mode chokes — all in place, none cracked or burned?
- **Power supplies**. Fan running quietly (no scraping/whining)? Vents not dust-clogged? No discoloration on the case (heat damage)?
- **Battery terminals**. Clean, no corrosion?
- **Computer connections**. USB cables seated, no flaky ones (a good cable doesn't disconnect when you wiggle it).

### Firmware status

- **Check firmware notification feeds** for your major equipment. Note any new releases since last quarter. Read release notes and decide whether to update (see §16-02).

## Spring inspection (annual, ~2-3 hours)

A deeper inspection that usually requires getting close to the antennas:

- **Climb / ladder / rope-pull each antenna** for hands-on inspection.
- **Open every weatherproof connection** at the antenna feedpoint, balun, or trap. Look for:
  - Moisture inside (any sign of water = open it up, dry it, re-seal — you've found the next failure)
  - Corrosion (green or white powder on copper, gray on aluminum)
  - Any insect or animal nesting
- **Re-seal**. Every connector outdoors should be re-sealed *every spring*, even if it looks fine. The cycle: peel off old tape, apply self-fusing tape, apply outer tape (3M Scotch 33 is the canonical), confirm no gaps. The detail matters; water finds every gap eventually.
- **Replace any visibly degraded coax**. UV-cracked jacket means the dielectric below it is at risk. See §16-04.
- **Tighten everything**. U-bolts, mast clamps, stub mast hardware, rotator hardware. Use a torque wrench if the manufacturer spec'd one (most tower hardware has a recommended torque).
- **Battery capacity test**. Run the annual load test (per §16-01).
- **Lightning protection check**. With an ohmmeter, verify continuity from each grounded element to the main ground. Note any resistance drift over years.
- **Document**. Photograph everything. Update the inventory (**J-Vault**). Date the inspection.

## Fall inspection (~2 hours)

Repeat much of the spring inspection, with emphasis on **getting ready for winter**:

- **Re-seal everything** that needs it (re-do connectors that looked even slightly suspect in spring).
- **Drain low points** from any antenna with internal cavities (some traps have drain holes — check that they're not clogged).
- **Verify guy lines** are properly tensioned (winter ice loads can cause rope/wire creep).
- **Storm prep**: confirm all surge protectors / lightning arrestors are in place and bonded.
- **Test the battery backup** under load — you'll be using it more in winter (storms, power outages).
- **Software / firmware backups** — make sure all your config exports are recent.

## What "looks fine" looks like

After a few years of inspections, you'll start to recognize what *normal* looks like for your installation. Some examples:

- **Connector boots**: should be flexible. If brittle or cracking, replace.
- **Coax jacket**: should be smooth, no chalkiness. Chalkiness = UV degradation in progress.
- **Aluminum elements**: should have a uniform light-gray oxidation. Bright spots = recent damage; black spots = water/salt corrosion.
- **Stainless hardware**: should be shiny. Brown rust = it wasn't actually stainless or was scratched in install.
- **Coax shield braid through PL-259**: should look like new copper or be tinned. Black or green = corrosion has reached inside.

## Annual inventory audit (**J-Vault**)

Once a year, **reconcile your shack inventory**: every radio, antenna, accessory, coax run, length, connector type, and serial number. Update the inventory database. This is the basis for:

- Insurance claims (in case of theft or damage)
- Selling or estate planning (**J-Vault**)
- Knowing what you actually own

The J-Hub Shack Inventory tab feeds back into J-Hub's database.

## Common mistakes

- **Inspecting only when something breaks.** By then it's too late — fix it, then start the inspection schedule.
- **Looking at antennas only from the ground.** Many problems (corrosion at the feedpoint, water in the balun) are invisible from below.
- **Skipping the boring ones.** Connectors that have always been fine for 5 years are exactly the ones that fail in year 6 because no one's looked at them.
- **Inspecting in February in Wisconsin.** Schedule inspections for weather you can actually work in. Record the schedule; reschedule if you must, but don't skip.
- **No documentation.** "I think the SWR was about the same as last year" is not data. "20m SWR 1.3 at 14.175 in spring 2025, 1.4 in spring 2026" is data.

## See also

- §16-00 — Overview (this is part of the broader maintenance discipline)
- §16-04 — Coax replacement (when an inspection turns into a replacement)
- §12 — High-SWR troubleshooting
- §13 — Station troubleshooting
- **J-Vault** — Shack inventory
- §18 — Coax & connectors (reference)
