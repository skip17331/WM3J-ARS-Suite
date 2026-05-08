---
id: 14-06
title: Guy Lines, Turnbuckles, Thimbles, and Clamps
chapter: 14
section: 06
level: mixed
status: draft
---

# Guy Lines, Turnbuckles, Thimbles, and Clamps

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A guyed tower stands because three or four cables in tension hold it vertical against wind load and asymmetric antenna mass. Every component along the guy run — the cable, the thimble at each loop, the cable clamps that secure the loop, the turnbuckle at the anchor end, the anchor itself, the insulators along the line — is a single point of failure. A guy that snaps doesn't degrade your performance; it drops the tower.

This section is about inspecting and maintaining those parts.

## Guy materials

Three common types in amateur installations:

| Material | Strength | Pros | Cons |
|----------|---------|------|------|
| **EHS (Extra-High-Strength) galvanized steel cable** | 14,400 lb tensile (1/4") | Standard amateur choice; cheap; durable | Conducts RF; needs insulators on guys near antennas |
| **Phillystran HPTG (Kevlar/aramid)** | 8,000-13,000 lb tensile | Non-conductive; lighter | Expensive; UV-degrades over years; pricing volatile |
| **Stainless aircraft cable (1×19 or 7×19)** | 1,500-4,200 lb (smaller sizes) | Highly corrosion-resistant; small towers | Lower strength; more elastic; expensive |
| **Dacron / synthetic rope** | varies; specify rated rope | Non-conductive; light | Stretches under load; UV-sensitive; lower-strength |

For a typical Rohn 25G amateur installation, **3/16" or 1/4" EHS** is standard, with insulators at calculated intervals to break it up electrically.

## Guy inspection checklist

### Visual inspection (annual climb)

For each guy from anchor to tower:

- **Cable surface**: galvanizing intact? No bare-steel spots? No flaking?
- **Cable shape**: straight under tension; no kinks, no crushed sections, no broken strands.
- **Loops at the tower and anchor**: smooth bends through the thimbles; no sharp bends.
- **Cable clamps**: properly installed (saddle on the live side, U-bolt on the dead side); tight to spec; no corrosion.
- **Turnbuckles**: not seized; threads engaged; locking method intact.
- **Anchor connection**: pin or shackle in place; clean clamp at anchor; no visible movement.
- **Insulators**: intact; no cracks; no UV chalking on Phillystran-segment insulators.

### Tension check

Guys should be tensioned per manufacturer specs. For Rohn 25G with 3/16" EHS:

- **Initial tension**: ~10% of rated cable strength = ~1,400 lbs (more than a person can pull by hand)
- **Working tension**: 5-10% of cable strength
- **Pre-load**: tower top should not move under sustained 30 mph wind

The tension can be measured with a dynamometer or estimated by **how the guys behave**:

- **Loose guy**: visible sag (catenary) under no wind. Should be retensioned.
- **Properly tensioned**: a slight catenary, almost straight; the cable pings audibly when struck.
- **Over-tensioned**: cable rings high-pitched when struck; guys may pull on the tower legs at attach point.

> ⚙️ **Advanced —** Pre-tensioning math for a guyed tower is an exercise in vector statics. The horizontal component of guy tension must balance the wind-load horizontal force at each guy attach point. For Rohn 25G with 3 guys at 120° at each level, the tension required to resist a given wind speed is calculated per ANSI/TIA-222 (the structural standard for towers). Most amateur installations use the manufacturer's recommended tension for their tower height + guy count + antenna load. Over-tension causes leg buckling; under-tension causes excessive sway and accelerated fatigue.

### Anchor inspection

The anchor is where the guy meets the ground (or wall, or rock, depending on installation). For a buried earth anchor:

- **Above ground**: no soil erosion exposing the anchor below grade.
- **Anchor head**: clean threads; pin or shackle in place; not bent.
- **Drainage**: water doesn't pool around the anchor (causes corrosion).
- **Vegetation control**: roots growing around the anchor can shift it over years.
- **Backfill compaction**: the soil around the anchor should be firm; loose backfill = settling = anchor pulling up.

For a screw-in anchor (less common in amateur use): inspect that the helix is still buried at the proper depth. Any visible movement of the anchor head means it's pulling out — replace immediately.

For a slab/concrete anchor: same as foundation in §14-05.

## Turnbuckles

A turnbuckle has two threaded eyes (one left-hand thread, one right-hand) connected by a body that, when rotated, draws or releases the eyes. They're the tension-adjustment point of every guy.

### Types

| Type | Strength | Use |
|------|---------|-----|
| **Galvanized eye-eye turnbuckle** (jaw style) | Rated to capacity | Standard amateur use |
| **Galvanized eye-eye turnbuckle** (eye style) | Lower-rated | Lighter installations |
| **Stainless turnbuckle** | Rated to capacity | Marine / coastal |
| **Open-body turnbuckle** | Visible threads; easy inspection | Common in amateur |
| **Closed-body turnbuckle** | Concealed threads; weatherproof | Better for severe weather |

Sizing: the rated working load of the turnbuckle should match or exceed the guy cable's working load. A 3/16" EHS at 5% pre-tension carries ~700 lb working load — a 1/2" turnbuckle (rated to ~3000 lb) is appropriate.

### Inspection

For each turnbuckle:

- **Body intact**: no cracks, no bends, no rust pitting.
- **Threads engaged**: at least 2-3 thread turns showing on each end (more is fine).
- **Threads not corroded**: should turn freely with a wrench or by hand. Frozen threads = corroded threads = retire.
- **Locking in place**: see below.
- **No bending**: the body shouldn't be cocked relative to the cable line — the load should be in pure tension.

### Locking turnbuckles

A turnbuckle under high cyclic load (wind, ice) can self-loosen. Several locking methods:

- **Safety wire** (lock wire): galvanized wire (typically 0.041" — about 18 gauge) wrapped through the body and around the eye. Standard practice in amateur and aviation. Prevents the body from rotating.
- **Cotter pin through hole** in the body: simpler than safety wire; same effect.
- **Jam nut**: a second nut on each threaded end, jammed against the body. Less common in turnbuckles; more in eyebolt installations.
- **Loctite**: thread-locking compound. Less reliable in amateur use because of weather; use safety wire.

**Safety wire is the standard for amateur tower work.** Inspect the wire annually:
- Wire should be intact (not broken or corroded).
- Wraps should be tight against the body — if loose, the wire is no longer doing its job.
- Wire should pass through both eyes (or through hole + wrap around the body) such that any rotation tightens the wire (called "anti-tampering" wrap).

## Cable clamps (Crosby clips, U-bolt clips, fist-grip clamps)

Cable clamps hold the loop at each end of a guy cable. The cable goes around a thimble, doubles back on itself, and is clamped to the live cable.

### Types

- **Crosby U-bolt clamp** (also called "Crosby clip"): U-bolt with saddle. Most common.
- **Fist grip clamp** (Crosby G-450): plate-and-saddle design; both halves are saddles. Better than U-bolt for several reasons.

| Property | U-bolt clamp | Fist grip |
|----------|---------------|-----------|
| Cost | Cheaper | More expensive |
| Strength | Slightly less | Slightly more |
| Inspection | "Saddle on the live, U-bolt on the dead" rule | Symmetric; can't be wrong |
| Wear on cable | More potential damage if mis-installed | Even pressure distribution |

For amateur work, fist-grip clamps are preferred when cost is acceptable.

### Number of clamps

Industry standard: **at least three clamps** at each loop, spaced 6 cable diameters apart. For 3/16" EHS: 3 clamps, each ~1.125" apart.

| Cable diameter | Minimum clamps per loop | Spacing |
|----------------|------------------------|---------|
| 3/16" | 3 | 1-1/8" |
| 1/4" | 3 | 1-1/2" |
| 5/16" | 3 | 1-7/8" |
| 3/8" | 3 | 2-1/4" |

Going below the minimum is unsafe; going above adds redundancy at little cost.

### Installation orientation

For U-bolt clamps: **"never saddle a dead horse."** The saddle (curved part) goes on the **live side** (the long, load-bearing side) of the loop; the U-bolt goes on the **dead side** (the short tail). Reversing this dramatically reduces the clamp's effectiveness.

The mnemonic: **"Saddle on the strong side, U-bolt on the weak side."**

### Inspection

For each clamp:

- **Tight**: torque to spec (varies by clamp size; typical 7/16" Crosby clamp wants 30 ft-lbs).
- **Saddle correct side**: per the rule above.
- **No corrosion**: galvanizing intact.
- **Cable not pinching out**: cable should be evenly compressed under the clamp; no bunching or unraveling visible.
- **Distance correct**: 3 clamps at the proper spacing.

After installation, the clamps should be re-checked for tightness within a few days (cable stretches slightly under tension; clamps slightly loosen) and again after the first 30 days of weather.

## Thimbles

A **thimble** is a teardrop-shaped metal piece that fits inside a cable loop, providing a smooth bearing surface for the cable to bend around without sharp angles.

### Why thimbles matter

A cable loop without a thimble bends around the eye/turnbuckle/anchor at a tight radius. The bend causes:

- **Stress concentration** at the bend — fatigue failure starts there.
- **Galling** of the cable against the metal eye.
- **Reduced effective strength** (10-30% reduction depending on the bend radius).

A thimble distributes the load over a smooth curve, eliminating these problems.

### Types

- **Galvanized steel thimble**: standard amateur use. Sizes match cable diameter (3/16", 1/4", etc.).
- **Stainless steel thimble**: marine/coastal.
- **Nylon thimble**: lower-cost; OK for synthetic rope; not for steel cable under high load.

Sizing: the thimble's groove should match the cable diameter. A 3/16" thimble for 3/16" cable; oversized thimbles let the cable migrate; undersized prevent proper seating.

### Inspection

- **Thimble in place**: not bent, not flattened, still seating the cable groove.
- **Cable groove clean**: cable rides smoothly in the thimble; no chafing on the metal.
- **Thimble alignment**: should be in line with the load direction; not cocked.
- **No corrosion**: galvanizing intact at all visible surfaces.

A bent or flattened thimble is a sign of overload — the cable has been pulled hard enough to deform the thimble. Replace and investigate.

## Insulators along guys

For guyed towers near antennas, the guy cables must be **electrically broken** at calculated intervals to prevent re-radiation that affects antenna pattern, or to prevent the guys from acting as loaded antennas at HF.

### Insulator types

| Type | Material | Strength | Notes |
|------|---------|---------|-------|
| **Egg insulator** (porcelain or plastic) | Porcelain or polycarbonate | Cable-rated | Cable threads through; in compression failure mode |
| **Strain insulator** (Glastic, fiberglass) | Glass-reinforced epoxy | High | Rated for high voltage and high mechanical load |
| **Plastic strain insulator** (Phillystran-style) | Engineering thermoplastic | Moderate | Lighter; UV-sensitive |

### Spacing

Insulators are placed at intervals such that no segment between insulators is resonant on the operating bands. For HF amateur use, segments shorter than 1/4 wavelength of the highest band of operation (typically 10 m, so segments < 2.5 m / ~8 ft) are safe.

A typical 60 ft tower with 3 guy levels and 3 guys at each level might have 12 insulators — one per guy segment, broken into ~6-8 ft pieces.

### Inspection

- **Visible damage**: no cracks in porcelain insulators; no surface chalking or UV breakdown on plastic ones.
- **Cable still through the insulator**: not migrated to one end.
- **Intact mechanical connection**: cable secured at each insulator end with proper clamps or knots.
- **No "burn marks"**: a strain insulator that has arcked over (RF or lightning) shows visible darkening; replace.

## Inspecting guy systems together

The whole guy system should be inspected as a system:

1. **Are all guys properly tensioned?** (Visual catenary check.)
2. **Are all guys at the same tension?** (One sagging guy means the others are over-loaded.)
3. **Is the geometry correct?** (Each guy makes the same angle with the tower; symmetric layout.)
4. **Are the anchors securely placed?**
5. **Are the turnbuckles all locked?**
6. **Are the cable clamps all properly installed and torqued?**
7. **Are the thimbles intact at every loop?**
8. **Are the insulators (if any) intact?**

A failure in any one component can cascade. Inspect all components every climb; replace any that show wear.

## Replacement schedule

Even healthy guy hardware doesn't last forever. Plan replacement on a calendar:

| Component | Replace at |
|-----------|------------|
| EHS guy cable | 25-30 years (longer in dry climates; less in coastal/wet) |
| Galvanized turnbuckles | 20-30 years (visual inspection drives this) |
| Cable clamps | At any sign of corrosion or every 25 years |
| Thimbles | At any sign of deformation |
| Phillystran cable | 15-20 years (UV-driven) |
| Porcelain insulators | indefinitely if no damage |
| Plastic insulators | 10-20 years (UV-driven) |

In a typical 30-year tower lifetime, expect to replace guy cable and turnbuckles at least once — usually as a single project replacing all guys simultaneously.

## Common mistakes

- **Saddle on the wrong side of the U-bolt clamp.** Universal failure mode; "never saddle a dead horse."
- **Insufficient clamps.** Two clamps per loop is unsafe; three is the minimum, sometimes four for redundancy.
- **No safety wire on turnbuckles.** Wind-cycle vibration unscrews them over years.
- **No thimble in the loop.** Cable wears through at the bend; failure within years.
- **Ignoring insulators.** Re-radiation distorts antenna patterns; segments resonate on HF; performance degrades.
- **Mixing materials**: stainless clamps on galvanized cable creates galvanic corrosion at the contact point. Match materials throughout.
- **Adjusting one guy without checking the others.** Tightening one increases lateral load on the tower; the geometry must be re-balanced.
- **Improper anchor**: pulling on the anchor at the wrong angle creates a force component the anchor wasn't designed for. Anchor and pulling angles should be aligned.
- **No replacement on schedule.** Guy hardware that's been "fine for 25 years" is at its end-of-life. Plan replacement before failure.

## See also

- §14-00 — Maintenance overview
- §14-05 — Tower & mast inspection
- §14-07 — Ground system (ground-bonding the guys is part of lightning protection)
- §11-05 — Grounding (the broader grounding picture)
- §14-09 — Cable entry & water intrusion
