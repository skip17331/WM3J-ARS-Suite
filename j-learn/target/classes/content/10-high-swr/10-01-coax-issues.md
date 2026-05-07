---
id: 10-01
title: Coax Issues
chapter: 10
section: 01
level: simple
status: draft
---

# Coax Issues

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The coax cable itself is the most common cause of high SWR after about 10 years of outdoor service. Coax dies slowly — UV, water, ice cycling, rodent gnawing, mechanical fatigue. By the time the SWR shows it, the cable is usually well past its prime.

## Symptoms that point at the coax

- **High SWR on every band**, not just one.
- **SWR worse after rain or snow.**
- **Loss higher than the spec sheet** says (signals weaker on RX, less power reaching the antenna on TX).
- **SWR that wanders or jumps** when you flex or twist the cable.
- **Visible damage** — cracks, exposed shield, melted spots, animal teeth marks.

## Quick tests

1. **Wiggle test**: with the radio at low power, transmit a steady carrier and watch the SWR meter while you flex the cable along its length. A reading that jumps when you flex one section means damaged cable at that section.
2. **Disconnect-and-short test**: remove the antenna at the antenna end and short the center conductor to the shield. SWR at the rig end should now read 1.0 → infinity (depending on the radio's response to a short). If you can't get a clean short, the coax has a problem.
3. **Open test**: same as above but leave the antenna end open (no short, no antenna). SWR should again be infinite. If it isn't, the cable is leaking somewhere.
4. **NanoVNA TDR (Time Domain Reflectometry)**: Sweep the cable with a NanoVNA in TDR mode. A clean cable shows one reflection at the far end. Any other peaks indicate damage at known distances.

## Common coax failure modes

### Water in the dielectric

Water wicks into the foam or solid plastic dielectric through cracks, damaged jackets, or unsealed connectors. Once water is inside, it dramatically increases loss and changes the impedance.

**Sign:** SWR spikes after rain, settles after the cable dries (sometimes days later). Cable feels heavier than it should, or sloshes faintly when you flex it.

**Fix:** Replace the affected section. There is no way to dry water out of foam coax dielectric; once it's wet, it's done. Re-seal the connectors with quality coax seal (the soft mastic kind) when you install the replacement.

### Cracked or eroded jacket

UV exposure makes PVC jackets brittle, then cracks open them up to water and animals.

**Sign:** Visible jacket cracks; flaking or chalky surface; jacket comes off in your hand when you tug it.

**Fix:** Inspect the entire run. Replace any visibly bad sections. UV-resistant coax (LMR-400, Davis Bury-Flex) lasts much longer than basic RG-58 / RG-8 outdoors — consider upgrading.

### Crushed or kinked cable

A kink permanently deforms the dielectric and changes the local impedance, creating a reflection point. You can't see it from the outside if the jacket isn't broken.

**Sign:** SWR spike that doesn't go away when you replace the connectors. NanoVNA TDR shows a reflection at the kink location.

**Fix:** Replace. Kinked coax cannot be uncrushed. Don't cut and re-splice with barrel connectors except as a temporary fix — every connector adds loss.

### Rodent or insect damage

Squirrels, mice, and certain wasps love coax. Squirrels chew through the jacket; mice nest inside the jacket; wasps build mud nests around the connectors.

**Sign:** Visible bites, holes, or paper-like nesting material in the jacket.

**Fix:** Replace the chewed section. If the run goes through an attic or garage where rodents are likely, sleeve the new coax in PVC conduit.

### Heat damage

Coax run too close to a heating duct, sun-baked attic, or in direct contact with a hot exhaust pipe degrades faster.

**Sign:** Jacket is discolored or sticky in spots. Cable feels stiff and crumbly when bent.

**Fix:** Replace and re-route away from heat sources.

### Internal corrosion of the shield

Especially on cheaper coax (RG-58 with thin braid), the copper braid can corrode from moisture without visible jacket damage.

**Sign:** Loss measurably worse than spec; SWR climbs slowly over years.

**Fix:** Replace. Use coax with a braid + foil construction (LMR-400 has both) for moisture resistance.

## What to look for when buying replacement coax

For most amateur HF runs:

- **LMR-400** or equivalent (Times Microwave), 0.405" diameter, ~3.9 dB/100 ft loss at 100 MHz, much better at HF.
- **DX Engineering DXE-400MAX**, **Davis Bury-Flex**, **Andrew Heliax** (overkill for HF; sometimes worth it for VHF/UHF runs).
- **RG-213** is acceptable for short runs (~50 ft or less) but loses more than LMR-400.
- **RG-58** is fine for jumpers (under 6 ft) but should not be used for outdoor antenna runs.

For VHF/UHF or very long runs, consider **hardline** (Heliax LDF4-50A, etc.) — much lower loss but much more expensive and harder to handle.

> ⚙️ **Advanced —** Coax loss scales with the square root of frequency: a cable that's 1 dB/100 ft at 30 MHz is about 1.8 dB/100 ft at 100 MHz and 5.7 dB/100 ft at 1 GHz. Loss also rises with high SWR — the standing wave causes higher RMS current at the loss-maximizing points along the line. A short 100-ft run at 1.5:1 SWR has only marginally more loss than at 1:1; the same run at 5:1 SWR can have 2-3 dB more loss. This is the "additional loss due to SWR" curve printed in every coax catalog.

## Connector compatibility check

When replacing coax, double-check that you have the right connectors for the new cable. LMR-400 doesn't fit standard RG-8 connectors (different size). Most coax suppliers sell pre-terminated cables in standard lengths, which is the easiest path for most operators.

## Maintenance schedule

For outdoor coax runs:

- **Annual visual inspection.** Walk the run, check for new damage, tightness of connectors.
- **Re-seal connectors every 5 years** even if they look fine. Coax seal degrades from UV.
- **Plan to replace every 10–15 years** in outdoor service. Faster if your environment is harsh (coastal salt, desert UV, severe winters).

## See also

- §10-02 — connectors specifically (often the actual problem)
- §10-07 — water ingress (overlaps with this section)
- §19 — coax types reference
