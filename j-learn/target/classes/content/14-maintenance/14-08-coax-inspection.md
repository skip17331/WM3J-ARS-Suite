---
id: 14-08
title: Coax Inspection
chapter: 14
section: 08
level: simple
status: draft
---

# Coax Inspection

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

§14-04 covers when and how to **replace** coax. This section covers how to **inspect** coax — the diagnostic process you do before, during, and between replacements. Inspection catches the early signs of degradation; replacement responds to the late ones.

## Inspection cadence

| Inspection | What you check | When |
|------------|----------------|------|
| **Visual walk-down** | Outdoor jacket condition, routing, animal damage | Quarterly + after storms |
| **Connector inspection** | Boot integrity, strain relief, weatherproofing | Quarterly |
| **End-to-end electrical** | Continuity, shorts, resistance | Annually |
| **SWR sweep comparison** | Performance baseline | Annually + after any weather event |
| **TDR / cable-loss measurement** | Internal cable health | Annually + when symptoms appear |

## Visual walk-down (quarterly)

Walk every coax run from antenna to entry point. Look for these specific items:

### Jacket condition

- **UV chalking**: a white, powdery surface on the black jacket. Indicates plasticizer migration; the jacket is becoming brittle. Cosmetic for a year or two; structural beyond that.
- **UV cracking**: small cracks running along the cable jacket. Once cracking starts, water reaches the dielectric within 1-2 winters. Replace promptly.
- **Animal damage**: rodent bite marks on the jacket. Inspect more closely — they may have penetrated to the shield.
- **Mechanical wear**: scrape marks where the cable rubs against the tower, antenna mast, or building. These are structural failure points; relocate the cable if possible.
- **Burn marks**: charred sections on the jacket. Indicates a high-power arc or electrical fault. The cable is compromised; replace.
- **Discoloration**: any brown or black spots on the jacket beyond normal aging. May indicate water trapped between the jacket and the shield, slowly working through.

### Routing and support

- **Drip loops**: should be present at every entry into a building or enclosure. Coax should rise *above* and then *fall down* before entering, so water flows away.
- **Bend radius**: minimum bend radius is typically 5-10× the cable diameter (RG-213 wants 2-3 inches; LMR-400 wants ~3 inches). Sharp bends below this fold the dielectric.
- **Cable ties**: UV-stable (typically black). Old ones turn white and brittle; replace.
- **Strain relief**: at every connector and every transition point, the cable should not pull on the connector body.
- **Sag**: along long runs (especially overhead cable), should not pool water or strain support points.

### Connection points

At each junction in the run:

- **Connector boot intact**: no cracks, no missing sections.
- **Boot sealed**: should fit closely against both the cable and the connector body.
- **Visible discoloration around connector**: water staining (brown trail), green corrosion at brass parts, or arcing signs.
- **Heat-shrink intact**: any peeling or splitting.
- **Tape secured**: vinyl outer tape (3M Scotch 33) should still adhere.

### Indoor end of run

- **Inside the shack**, where coax ends at the equipment:
  - Coax not pulling on the connector at the back of the radio.
  - No sharp bends near the connector.
  - Coax at the proper length — extra cable coiled gently if any (no tight coils).
  - Patch cables in good condition.

## Connector inspection (quarterly)

For each PL-259, N, or BNC connector in the system:

### Mechanical

- **Tightness**: connector body to mating piece should be hand-tight; no looseness when wiggled.
- **Body**: no bends, no compression, no discoloration.
- **Threads**: clean threads with no visible corrosion.
- **Center pin**: not bent, not corroded, properly sized for the cable.
- **Weatherproofing**: tape and/or boot intact.

### Electrical (continuity test)

With the cable disconnected at both ends:

| Test | Expected reading |
|------|------------------|
| **Center conductor to shield** (continuity) | Should NOT conduct (open circuit) |
| **Center conductor to itself** (end-to-end) | Should conduct (low resistance, < 1 Ω for typical 50-100 ft) |
| **Shield to itself** (end-to-end) | Should conduct (low resistance) |
| **Connector body** (between center pin and threaded shell) | Should NOT conduct |

A center-to-shield short means the cable is failed (water in the dielectric, or a connector with a shorted center pin). An open in the center conductor means a broken pin or break in the cable. Investigate and repair before reconnecting to equipment.

### Common connector failure modes

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| High SWR | Poor solder joint inside the PL-259, water in the dielectric near the connector | Replace connector; if cable is wet, replace section |
| Intermittent contact | Loose center pin, corrosion at the contact | Replace connector |
| Visible corrosion at the body | Water has penetrated the boot | Reseal; if extensive, replace connector |
| Connector pulls off easily | Strain relief failure; cable has been pulled at | Replace connector, improve strain relief |
| Burned mark inside | High-current arc | Replace connector; check antenna/SWR for cause |

## Connector replacement workflow

When inspection reveals a damaged or suspect connector, replace it:

1. **Cut off the old connector** at the cable. Cut behind the strain-relief area, above any visible damage.
2. **Strip the cable** per the connector's manufacturer specs:
   - Outer jacket back (typically 1/2" to 1")
   - Shield braid back
   - Dielectric back (exposing center conductor)
   - Center conductor (typically 1/4" exposed)
3. **Slide on the heat-shrink boot** (before installing the connector — easier).
4. **Install the new connector** per its instructions:
   - PL-259 (UHF): solder the center pin to the center conductor; solder the shield to the shell holes.
   - N: crimp the ferrule (solder isn't typical for N); insert and tighten the body.
   - BNC: insert center pin, tighten the body, secure the strain relief.
5. **Test** with a multimeter: continuity center-to-center, shield-to-shield, no shorts.
6. **Heat-shrink the boot** over the connector body, sealing the cable side.
7. **Tape the connector** with self-fusing tape (3M 130C), then outer vinyl tape (3M Scotch 33). Three wraps total.
8. **Test the run end-to-end** with an analyzer.

## Annual SWR sweep comparison

If you've been keeping baseline data (per §14-00), compare today's SWR sweep to your historical record. Look at:

### Resonance frequency

- Same as baseline → cable resonance unchanged → cable is healthy.
- Shifted by < 50 kHz → small change, possibly weather; recheck in 30 days.
- Shifted by > 100 kHz → meaningful change; cable has aged or antenna has changed.

### SWR magnitude

- Same as baseline → no change.
- Slightly higher → match has degraded slightly; could be weather or aging.
- Significantly higher → real problem; investigate.

### Sweep shape

- Same shape as baseline → all is well.
- "Wandering" or "messy" sweep → cable shield corrosion or impedance disturbance.
- New dip in the middle of the sweep → mid-cable problem (kink, water, bad connector).

## Annual cable-loss measurement

A cable that has been wet, kinked, or has aged shield will show **higher matched-line loss** than the manufacturer spec. Measure with a NanoVNA (or similar):

1. Connect NanoVNA port 1 to the cable's near end.
2. Terminate the far end with a precision 50 Ω load.
3. Measure return loss in S11 mode at the operating band.
4. Compare to spec.

| Cable | Spec at 14 MHz | At 144 MHz | At 432 MHz |
|-------|----------------|------------|------------|
| RG-58 | 1.5 dB / 100 ft | 5.2 dB | 9.7 dB |
| RG-8X | 1.0 dB / 100 ft | 4.0 dB | 8.0 dB |
| RG-213 | 0.6 dB / 100 ft | 2.0 dB | 4.0 dB |
| LMR-400 | 0.5 dB / 100 ft | 1.5 dB | 2.7 dB |
| Hardline (Heliax LDF7-50A) | 0.13 dB / 100 ft | 0.4 dB | 0.7 dB |

If your measured loss exceeds spec by 30% or more, the cable is at end of life — schedule replacement.

> ⚙️ **Advanced —** The NanoVNA's S11 reading isn't directly cable loss; it's the **return loss** through the cable + termination + cable. To extract one-way cable loss: measure return loss with the cable terminated; subtract the termination's known mismatch (a 50 Ω load typically has 35-40 dB return loss); divide by 2 for one-way. NanoVNA-Saver and similar software does this calculation automatically. For higher accuracy, sweep the cable with a known short or open at the far end and use the round-trip loss directly.

## TDR sweep (annual or symptom-driven)

A NanoVNA in TDR mode shows reflections along the cable, with distance scaled by the cable's velocity factor. Use this to:

- **Locate a kink, water bubble, or bad mid-run connector** by the position of the spike on the TDR trace.
- **Verify cable length** — the trace should show the cable's full length without extra reflections.
- **Compare to baseline TDR** (from when the cable was installed) — any new spikes indicate new problems.

The math: distance to a reflection = (TDR time × velocity factor × c) / 2. NanoVNA software does this automatically if you set the velocity factor.

## Inspection results — what to do

| Finding | Action |
|---------|--------|
| Visible jacket cracking | Replace cable run within 6 months (plan now, do it on a good weather day) |
| Loose connector | Tighten; if can't tighten, replace |
| Water staining at connector | Re-seal connector; if extensive, replace |
| Center-to-shield short | Replace cable section immediately |
| SWR shift > 100 kHz | Investigate cable + antenna; replace if cable confirmed bad |
| Cable loss exceeds spec by 30%+ | Plan replacement |
| TDR shows mid-run spike | Locate and replace section; or replace whole run |
| Extensive corrosion at any connection | Replace |

## Common inspection mistakes

- **Looking only at connectors.** The cable jacket along a long run is also worth inspecting.
- **Skipping post-storm walks.** Storms cause UV-stress and physical damage simultaneously.
- **Forgetting the indoor end.** The shack-side connectors degrade slowly but predictably.
- **No baseline to compare against.** Without a baseline SWR sweep, you can't tell what's changed.
- **Tightening instead of replacing.** A connector that's loose enough to wiggle in your hand is a connector that needs replacement, not just retightening.
- **Patching with bad connectors.** A bargain-bin connector saves $5; reconnecting after weatherproofing fails costs an hour. Use quality connectors.

## See also

- §14-00 — Maintenance overview
- §14-04 — Coax replacement (the next-level decision)
- §14-09 — Cable entry & water intrusion
- §10-01 — Coax issues (troubleshooting symptoms)
- §10-07 — Water ingress (what wet coax looks like)
- §16 — Coax & connectors reference
- §08 — Feedline & SWR (the math behind cable loss)
