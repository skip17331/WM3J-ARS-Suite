---
id: 16-05
title: Tower & Mast Inspection
chapter: 16
section: 05
level: mixed
status: draft
---

# Tower & Mast Inspection

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A tower is a structural element. Unlike coax (which fails gradually and quietly) or batteries (which lose capacity on a curve), a tower fails **catastrophically** when something gives way — the welds let go, a leg buckles, the base bolts pull, and the entire structure can come down in seconds, often onto something or someone. Tower inspection is therefore the most safety-critical maintenance task on the station-keeping list.

This section covers what to inspect, how often, and where to draw the line on do-it-yourself work versus hiring a professional rigger.

## Inspection cadence

| Inspection | Frequency | Time |
|------------|-----------|------|
| **Ground-level visual** with binoculars | Monthly + after every storm with sustained winds > 40 mph | 5 min |
| **Walk-around base inspection** | Quarterly (every 3 months) | 15 min |
| **Climb inspection** of all hardware | Annually (spring) | 2-4 hours |
| **Professional rigger inspection** | Every 5 years (more often for lattice towers > 60 ft) | half day |
| **Post-incident inspection** | After any storm with ice loading, lightning strike, or known impact | varies |

The "I climbed it five years ago and it looked fine" approach is how towers fail. Inspect on the schedule.

## Ground-level monthly check

What you look for from the ground with binoculars (or a 60-300mm zoom lens — better than binoculars for documentation):

### Plumb

A tower should be plumb (vertical) within ±0.5° in any direction. Look up the centerline of the tower from at least two perpendicular angles:

- Sight along one face from 20-30 ft away. The top should appear directly above the base.
- Move 90° around. Sight along the perpendicular face. Top should still appear directly above the base.

Slight lean is normal in a guyed tower — guy tension never balances perfectly. Significant lean (visible without sighting) means a guy has slipped, the foundation has shifted, or a member has yielded. **Stop using the tower until investigated.**

### Sway

Wind makes towers sway. The amount of sway is normal-engineering-tolerance:

| Tower type | Acceptable sway at top in 30 mph wind |
|------------|---------------------------------------|
| Self-supporting (Rohn 25G, Rohn 45G) | 1-3 inches |
| Guyed lattice (Rohn 25G with guys) | 0.5-1.5 inches |
| Crank-up (US Tower TX455, MA-770) | 1-4 inches per section |
| Heavy-duty commercial (Rohn 55G) | < 1 inch |

If your tower sways more than expected, something has changed — a guy is loose, a connection has weakened, the antenna load increased.

### Vibration / harmonic motion

A healthy tower in steady wind moves predictably. Vibration that **builds up** rhythmically (sympathetic resonance) means the tower or guy system is at a marginal natural frequency for the wind speed. Action: stop using; inspect anchors, retension guys, look for loose hardware.

### Visible damage

- **Bent legs or bracing**: any visible bend, twist, or crease is structural damage. Don't climb. Hire a rigger.
- **Missing bolts or hardware**: count the bolts on accessible joints. A leg connection in Rohn 25G has 6 bolts; a missing one means 17% reduced strength at that joint.
- **Rust streaks**: brown vertical streaks below a connection point indicate steel oxidation in progress, often inside a joint. Inspect at next climb.
- **Bird damage or nests**: a hawk's nest in the upper section can add 10-20 lbs and shift wind loading. Remove gently if accessible; some nests are protected.

### Antenna load

The antennas at the top should look the same as last month. Note:

- **Tilt of a Yagi boom** — should be horizontal (within ±5°). Drooping boom indicates loose mast clamps, broken element, or wind-bent boom.
- **Element symmetry** — Yagi elements should appear identical in length on both sides (note: visually it's hard, but obvious bends or breaks stand out).
- **Rotor / mast clamp** — should hold the mast vertical and rotate without complaint. Bent mast or wobble in rotor indicates trouble.

Photograph the tower at the same focal length and angle each month. A side-by-side comparison reveals slow drift that you can't see in real time.

## Quarterly base inspection

Walk around the tower base. Look for:

### Foundation

- **Concrete pad**: clean, intact, no spalling, no visible rebar.
- **Cracks**: hairline cracks in the surface are cosmetic. Crack > 1/4" wide, or any crack that has rust staining (rebar reaching surface), is a structural issue. Document and consult an engineer.
- **Settling**: is the foundation level? A tilt of 1" over a 4 ft pad is significant.
- **Drainage**: water should drain away from the base, not pool. Pooled water at the base accelerates corrosion and freeze-thaw damage.

### Anchor bolts (if surface-mounted)

- **Hex caps**: tight, no rust on threads, no corrosion at the base where they enter concrete.
- **Galvanized coating**: should be intact. Bare steel = active corrosion path.
- **Bolt projection**: should match installation specs (typically 2-3" of thread above the nut). If shorter, the bolts have slipped down — investigate the foundation.
- **Locking method**: jam nuts, cotter pins, lock washers, or Loctite. Verify whatever method was originally specified is still in place.

### Tower legs at the base

- **Galvanizing intact**: no flaking, no rust spots.
- **Welds visible at the base joint** (Rohn-style): inspect for any cracks (use a magnifier; cracks may be very fine).
- **Drainage holes** (lattice towers): clear, not blocked by debris.
- **Termite or rodent activity** in any wood structure near the base.

### Ground rod and lightning system

Covered in detail in §16-07. Quick check at the tower base:

- **Ground strap**: still bonded to the tower with a clean clamp, no corrosion.
- **Ground rod**: visible above grade by 2-4"; not pulled up; no chewing on the strap.
- **Connector clamp**: tight; no green corrosion (which means electrolysis, not just oxidation — bad).

## Annual climb inspection

The deep inspection requires getting to height. **Read the climbing safety section** before doing this; if you are not trained and equipped for tower climbing, hire a rigger.

What to inspect at each level (typically every 10 ft of tower):

### Joint hardware

- **Bolt torque**: each bolt should be at the manufacturer's spec (Rohn 25G: 35 ft-lbs for 5/16" bolts on legs; Rohn 45G: 75 ft-lbs for 3/8" bolts). Use a torque wrench, not feel.
- **Bolt threads**: clean threads, no galling, no broken-off bolts inside the holes.
- **Lock washers / locking method**: in place, properly seated.
- **Galvanized coating**: at all hardware. Brown rust = compromised.

### Welds

Climb-up inspection of welds, especially at:

- **Leg-to-bracing joints** (lattice towers).
- **Bearing joints** (where antenna mast clamps attach).
- **Star-mount junctions** (where multiple bracing pieces meet).

Look for hairline cracks. Use a small mirror to view the back side. Photograph any suspect weld; consult a rigger.

### Mast and rotator

- **Mast straightness**: sight along the mast from below. Should be straight; any bow indicates wind-bent mast or insufficient support.
- **Rotator alignment**: mast clamp at the rotator output should be evenly tightened; no slop in the rotor when rotated.
- **Antenna boom orientation**: Yagi booms should be perpendicular to the mast.
- **Element-to-boom clamps**: tight; no element sliding.

### Antenna feedpoints

- **Connector**: still weatherproofed; no visible water staining at the boot.
- **Coax routing**: drip loop in place; coax not pulling on the connector; strain relief intact.
- **Balun / feedline transformer**: visible damage, water staining, broken weatherproofing.

### Element joints (Yagi, log periodic)

- **Element-to-boom clamps**: tight.
- **Element joints** (where elements telescope): tight, no movement when wiggled.
- **Tip caps**: in place (closed-end caps prevent water ingress into elements).

### General hardware

- **Loose anything**: anything that wiggles when pushed should be retightened or replaced.
- **Set screws**: check that every set screw is still seated; loose screws back out over years of vibration.
- **Cable ties**: degraded UV cable ties (white, brittle) need replacement; use UV-stable ties (typically black).

## Crank-up tower specific

Crank-up towers have additional inspection items:

- **Lift cables**: visible signs of wear, corrosion, or kinks. Replace at first sign of fraying.
- **Pulleys**: spin freely; no flat spots.
- **Brake mechanism**: holds when locked.
- **Sections sliding properly**: no binding, no scraping during raise/lower.
- **Lubrication**: per manufacturer schedule (US Tower recommends quarterly grease on the cable pulleys).

> ⚙️ **Advanced —** The cable in a crank-up tower is the single most critical safety item. ASTM A-1023 spec for galvanized aircraft-cable lifting wire applies; replace at any sign of broken strands. The "10% rule" from rigging best practice: if 10% or more of the visible strands in a 12-inch section are broken or corroded, retire the cable. The crank-up cable is also temperature-sensitive — never operate at extreme cold (sub-zero F); the lubricant binds and the cable can fail in a crank attempt. US Tower's manuals specify the recommended cable inspection and replacement intervals; follow them.

## Rohn-specific notes

Rohn 25G and 45G are the dominant amateur towers. Specifics:

- **Bolt heads**: Grade 5 (yellow-zinc) for legs; Grade 8 for the heaviest joints. Replace with the same grade.
- **Top section** (first 10-ft section): often loosens first because of antenna-induced rotation. Inspect every climb.
- **Star-bracket** (where the antenna mast attaches): a common failure point. Inspect welds carefully.
- **Section pinning**: Rohn sections pin together with 5/8" pins. Pins should be lock-pinned (cotter pin or proprietary lock); a missing lock-pin = a section that can lift in high wind.

For 45G+ heavier towers and any multi-tower installation, professional inspection becomes increasingly important.

## What "no climb" means

Stop, descend, and don't operate the tower until investigated, if any of these are present:

- **Tower visibly leaning** beyond the normal slight lean.
- **Cracked or broken member** (leg, brace, bearing).
- **Missing bolt** at any joint.
- **Loose mast** (any wobble at the antenna level).
- **Active corrosion** (orange rust streaks visible from ground).
- **Recent lightning strike** (any tower hit by lightning needs full inspection before re-use).
- **Recent storm with > 40 mph winds**: walk-around at minimum; climb if anything looks suspect.
- **Foundation issue** (settling, large crack, water pooling).

When in doubt: descend. Don't climb on a tower you have any concern about.

## Climbing safety baseline

Even if this isn't a full safety chapter, the basic climbing equipment list:

- **Body harness** (full-body, not waist-only — fall arrest requires full-body).
- **Lanyards** with double-locking carabiners.
- **Rope** (12-strand or static rope rated for fall arrest, 11 mm minimum).
- **Helmet**, eye protection, gloves.
- **Trained climber**: ANSI Z359 or equivalent training. NIA (National Institute for the Inspection of Towers) certification or NATE (National Association of Tower Erectors) is the professional standard.

If you don't have the training, the equipment, and a buddy on the ground: **hire a rigger.** A typical professional inspection of a 60-ft amateur tower runs $500-$1500 depending on the area; this is far less than the cost of an injury or a tower failure.

## Common tower-inspection mistakes

- **"It looked fine from the ground."** Many failure modes are invisible from below. A bent member 40 ft up may look perfectly straight from ground level.
- **Skipping the post-storm walk.** Storms cause more cumulative tower damage than time alone. Inspect after every significant weather event.
- **Tightening but not torquing.** "Tight by feel" varies dramatically. Use a torque wrench; manufacturers specify torque values for a reason.
- **Adding antennas without recalculating loading.** A new VHF Yagi added to a tower designed for an HF beam may exceed wind-loading specs. Check the tower's wind-loading capacity at the antenna height before adding mass.
- **DIY climb on questionable hardware.** If anything looks suspect from the ground, don't climb. Hire someone qualified.
- **Ignoring the rotator.** A failing rotator stresses the mast and the upper section. Diagnose mechanical complaints early.

## See also

- §16-00 — Maintenance overview
- §16-06 — Guy lines, turnbuckles, thimbles, clamps (next-section depth)
- §16-07 — Ground system inspection
- §10 — High SWR (related to antenna and feedline issues at the tower top)
- §11-05 — Grounding (ties to lightning protection)
- §06 — RF safety (RF exposure near the antenna)
