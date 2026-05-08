---
id: 14-09
title: Cable Entry & Water Intrusion
chapter: 14
section: 09
level: simple
status: draft
---

# Cable Entry & Water Intrusion

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The cable entry is where outdoor coax (and rotor cable, control lines, ground straps, etc.) meets indoor air. It's the most failure-prone single point in any station — it sees thermal cycling, UV, ice, animals, water, and sometimes lightning, all in one location. Done right, it lasts 20+ years; done poorly, it's the single biggest reason stations have intermittent problems and indoor water damage.

This section covers what a proper cable entry looks like, how to inspect it, how to find and fix water intrusion, and how to prevent the recurring problems.

## What "cable entry" means

The cable entry is the physical transition point where coax (and other lines) move from outdoor exposure to indoor conditioned space. Common implementations:

| Implementation | Description | Pros | Cons |
|----------------|-------------|------|------|
| **Bulkhead panel** | A weatherproof panel mounted on the exterior wall, with feed-through connectors | Clean, professional, weather-rated | Most expensive; locked-in |
| **Window passthrough** | Coax through a window-pass panel or removable window section | Simple, removable, no permanent damage | Less weatherproof |
| **Through-wall hole** | Caulked hole through siding, sheet rock, or block wall | Simple | Risk of water intrusion if poorly sealed |
| **Foundation pass** | Sleeve through foundation block; coax through PVC | Below ground level, less visible | Can collect water; needs drainage |

The bulkhead-panel approach is the gold standard. Window pass-throughs are common for renters or temporary installations. Through-wall holes work if executed carefully but require diligent re-sealing.

## Why cable entries fail

Three failure modes account for most cable-entry problems:

### Direct water intrusion

Rain or snow finds a way through the seal — old caulk, missing tape, gap in the bulkhead gasket. Water enters the wall cavity, runs down the inside of the wall, and eventually finds:

- The shack interior (visible water, peeling paint, mildew).
- The wall studs (less visible, but rot starts).
- The electrical outlet box or radio chassis (potentially dangerous).

### Capillary water

Water doesn't even need a hole to get in. **Capillary action** — water following the cable jacket through a small gap — can creep along the outside of a coax, across a tiny gap, and pool on the inside of the wall. The cable itself is the water path.

This is why drip loops and proper sealing matter. Without them, even a "perfect" hole can leak.

### Condensation

Warm humid indoor air meets cool exterior surfaces (the back side of the cable as it enters cold outside walls). Condensation forms inside the wall. Over years, this dampens insulation, encourages rot, and degrades anything stored in the wall cavity.

The drip loop and exterior-only sealing approach mitigates this by keeping the wet path on the outside.

## Inspection cadence

| Inspection | Frequency | Time |
|------------|-----------|------|
| **Exterior visual** | Quarterly + after each major storm | 5 min |
| **Interior wall scan** | Quarterly | 5 min |
| **Sealant integrity check** | Annually (spring) | 15 min |
| **Disassemble and re-seal** | Every 3-5 years | 1 hour |

## Quarterly exterior inspection

At the cable-entry point, look for:

### The bulkhead or pass-through

- **Panel intact**: no cracks, no missing screws, no separation from the wall.
- **Gasket intact**: rubber or foam seal between the panel and the wall, still flexible (not cracked or shrunken).
- **Mounting bolts**: tight, no rust streaks running down the wall below them.
- **Cable feedthroughs**: connectors clean, weatherproofed, no visible gaps where the cable enters.
- **Drainage**: any drainage holes or weep paths are clear, not blocked by debris.

### Cables entering the bulkhead

- **Drip loops**: each cable should rise above and then fall down to its connector — never a straight horizontal entry.
- **Jacket condition**: where the cable bends through the entry, no UV cracking or kinks.
- **Strain relief**: the cable shouldn't pull on the connector at the bulkhead.

### Sealant condition

If the entry uses caulking around the cable hole (typical for non-bulkhead installs):

- **Caulk intact**: continuous bead, no gaps, no separation from wall or cable.
- **Caulk pliable**: should still be flexible. Hardened caulk has lost its sealing function.
- **No discoloration**: brown stains around the caulk indicate water passing through.
- **Bead width**: at least 1/4" continuous, more for vibration-prone areas.

### Below the entry

Look at the wall surface directly **below** the cable entry:

- **Water staining**: brown streaks or spots, especially extending downward from the entry.
- **Paint peeling or bubbling**: indicates moisture in the wall.
- **Mildew or moss**: green or black growth at the wall surface.
- **Soil erosion**: at the foundation, if water is funneling there.

## Quarterly interior inspection

Inside the shack, near the cable entry point:

- **Wall surface**: any visible water staining, peeling paint, or mildew.
- **Floor near the entry wall**: stains, swelling, or warping of flooring.
- **Outlet boxes** in the entry wall: any moisture, corrosion at terminals.
- **Indoor cable ends**: where the coax comes through and connects to surge arrestors or equipment, no visible water intrusion.

A **moisture meter** ($30 at hardware stores) gives a numerical reading of wall-cavity humidity. A normal interior wall reads 5-15% moisture; >25% indicates active wetness.

## Annual sealant inspection

Once a year, inspect the seal more carefully:

### Caulk inspection

- **Inspect every joint** where caulk has been applied:
  - Between bulkhead and wall
  - Between cable and bulkhead pass-through
  - At any flashing or trim around the entry
- **Test by pressing**: a fingernail should leave a slight indentation in healthy silicone caulk. If it bounces back hardened, caulk is past life.
- **Look for shrinkage**: caulk shrinks 5-15% over its lifetime. Visible gaps in the bead = action needed.
- **Look for cracking**: hairline cracks on the caulk surface. Indicates aging.
- **Look for separation**: caulk pulling away from the wall or the cable. Re-seal.

### Tape inspection

If you used self-fusing tape (3M 130C or equivalent) plus outer tape (Scotch 33):

- **Outer tape adheres**: any peeling at the edges?
- **Color change**: vinyl tape grays or whitens with UV; replace if extensive.
- **No water intrusion under the tape** (you may need to slightly peel a corner to check, then re-tape).

### Foam / sealing tape

If foam compression seal is used (typical at the bulkhead-to-wall interface):

- **Foam still compressed**: not relaxed back to original shape (relaxed = no longer sealing).
- **Foam intact**: not crushed flat, not shredded.

## How to fix water intrusion

When you find water has entered or is entering through the cable entry:

### Step 1: Find the path

Look in this order:

1. **Above the visible water**: water flows downward. The entry point is somewhere above the leak.
2. **Around the cable**: gaps along the cable jacket, capillary path?
3. **Around the bulkhead**: gasket failed? Mounting bolts loose?
4. **At the caulk bead**: visible gap or hardened section?
5. **At a separate ingress** (window leak, roof penetration above, etc.): not the cable entry — investigate elsewhere.

### Step 2: Stop the active leak

- **Cover with tarp or plastic** if rain is imminent.
- **Apply temporary tape** (self-fusing tape, then duct tape) over the visible gap.
- **Plan the permanent repair** for a dry day.

### Step 3: Fully reseal

For the permanent fix:

1. **Disassemble** if a bulkhead or pass-through. Remove old caulk completely (don't paint over).
2. **Clean the surface** — no dirt, no old caulk residue. A wire brush + isopropyl alcohol works.
3. **Apply primer** if recommended by the caulk manufacturer (some silicones bond better with primer; some don't need it).
4. **Apply new caulk** — silicone (50-year rated, marine-grade for coastal areas) or polyurethane (NP1 is a long-time amateur favorite). Apply a continuous bead.
5. **Tool the caulk** (smooth with a wet finger or putty knife) for proper bonding.
6. **Allow cure time** (24-48 hours typical for silicone; 24 hours for poly).
7. **Test with a hose** after cure — gently spray the entry area with water; check inside for any new infiltration.

### Step 4: Address the wall damage

- **Drying**: open the wall cavity if extensive water has entered; let it dry for several days with fans.
- **Structural assessment**: any visible rot, warped framing, or compromised insulation? Replace.
- **Mold remediation**: if mold is visible, remediate per local code. May require a professional.
- **Repaint/repaper**: after the wall is dry, repair the cosmetic damage.

## Sealing materials reference

| Material | Best for | Lifespan | Notes |
|----------|---------|----------|-------|
| **Silicone caulk (50-year, marine grade)** | Cable-to-bulkhead, perimeter | 20-30 years | Stays flexible, paint-not-required, expensive |
| **Polyurethane (NP1, sika 1A)** | Wall-to-bulkhead, structural | 15-25 years | Bonds to many surfaces, paintable |
| **3M 130C self-fusing tape** | Direct cable wrapping at connector | 10+ years | Compresses to itself; no adhesive |
| **3M Scotch 33 vinyl tape** | Outer protection over self-fusing tape | 5-7 years | UV-degrades; replace periodically |
| **Coax-Seal** (proprietary putty) | Connector-fill | 20+ years | One-shot; can't be removed easily |
| **Foam compression seal** | Bulkhead-to-wall | 5-10 years | Compresses around irregularities |
| **Boot cover** (rubber or silicone) | Connector boot | 5-15 years | Slip-on; easy replacement |

## A typical cable-entry build sequence

For the do-it-once-do-it-right approach to a new cable entry:

1. **Plan the location** — south or west wall to minimize freeze-thaw; below the eave to minimize direct rain; above grade by 1-2 feet to avoid splash damage.
2. **Drill the wall hole** — slightly oversized for the bulkhead body. Use a hole saw matched to your bulkhead.
3. **Mount the bulkhead** on the exterior with stainless or galvanized screws into framing or stud (not just sheetrock).
4. **Apply structural caulk** between bulkhead and wall, sealing all four sides.
5. **Pass the cables through** the bulkhead's individual pass-through holes.
6. **Connect each cable** to its arrestor or terminal block on the inside.
7. **Seal each cable** at the bulkhead pass-through with self-fusing tape + outer tape.
8. **Form drip loops** on the outside before the cable rises to the antenna.
9. **Test for leaks**: spray with a hose; check inside for any moisture.

Done well, a properly built bulkhead lasts 20+ years before needing significant rework.

## Common cable-entry mistakes

- **No drip loop.** Water flows along the cable directly into the wall. Mandatory drip loop on every entry.
- **Sealing only the inside.** Water on the outside doesn't care about your interior caulk; it enters from the outside.
- **Painting over caulk.** Many caulk types (especially silicones) won't bond to paint. Apply caulk on raw surface; paint after.
- **Using kitchen-grade silicone.** Bathroom or kitchen silicone has additives for mildew resistance but lower flexibility. Use a marine-grade or 50-year exterior silicone for cable entries.
- **Through-wall hole without flashing.** Without flashing or proper trim, water tracks down the wall to the hole.
- **Ignoring the foundation entry.** A buried PVC sleeve through a foundation can collect water; ensure drainage at the bottom.
- **Reusing old caulk.** New caulk doesn't bond well to old caulk. Remove all old caulk before resealing.
- **No moisture monitoring.** Without periodic checks, slow water intrusion goes undetected for years.
- **Mounting too close to the eave.** A drip from the eave above creates direct water flow on the entry.

## See also

- §14-00 — Maintenance overview
- §14-05 — Tower & mast inspection
- §14-08 — Coax inspection (the cable side)
- §10-07 — Water ingress (the symptom side, especially in the antenna)
- §14-04 — Coax replacement (when a cable has been wet for too long)
- §11-05 — Grounding (entry-point lightning protection)
