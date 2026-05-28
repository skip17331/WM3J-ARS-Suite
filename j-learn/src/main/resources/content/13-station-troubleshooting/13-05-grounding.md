---
id: 13-05
title: Grounding
chapter: 13
section: 05
level: mixed
status: draft
---

# Grounding

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

"Ground" means three different things in a ham shack, and confusing them is the source of half the station problems we see:

1. **AC safety ground** — the green wire in your AC plug. Required by code; protects against shock if a hot wire touches a chassis.
2. **Lightning ground** — drains static and lightning surges from the antenna system to the earth, ideally bypassing your equipment.
3. **RF ground** — gives the radio's transmit chain a low-impedance reference at radio frequencies. Required for many antennas to work; required to avoid RF feedback (chapter §13-04).

Done right, you have all three working together. Done wrong, your AC ground and lightning ground actually fight each other and create the very RF problems they were supposed to prevent.

## The single-point ground principle

The cardinal rule: **all grounds in your shack should connect to one common point**, and that point should connect to one ground rod outside.

Why? Because if Point A and Point B are both "ground" but they're connected to different rods (or different parts of your house wiring), they're at different potentials during a TX or a lightning event. Current flows between them, through whatever cable runs between them — your audio chain, your USB cable, your data line.

A single-point ground means there's only one path; current can't take a parasitic detour.

## How to build a single-point ground

### The bus bar

A copper bus bar (a strip of copper or brass, ~1/2" wide and 6+ inches long) mounted on the back wall of the shack. Every piece of equipment grounds to this bar with a short, thick conductor (4 AWG braid is excellent; #10 stranded copper wire is acceptable; thin 18 AWG wire is not).

Available pre-made from DX Engineering, MFJ, and others. Cost: $30–60.

### The ground wire

From the bus bar, a single short, thick wire (4 AWG copper braid, or #6 solid wire) runs to a ground rod outside. **Short and straight** — a long, twisted ground wire has high RF impedance and defeats the purpose.

Best path: bus bar → wall feedthrough → ground rod immediately outside the shack wall. Total length: ideally 6 feet or less.

### The ground rod

An 8-foot copper-clad steel rod, driven into the earth outside the shack. Connected to the ground wire with a non-corroding clamp (Burndy or similar approved fittings).

For maximum effect: multiple ground rods spaced 8+ feet apart, all connected together. The more rods, the lower the impedance.

### The bonding to AC ground

**Critical safety point.** Your shack ground rod **must be bonded to the house's AC service ground** with at least #6 wire. This isn't optional — it's required by the National Electrical Code (NEC Section 250-58). Without bonding, a fault could put dangerous voltage between the two grounds.

The bond is also good for lightning: it gives the surge another path to dissipate, and it equalizes potentials between your shack equipment and the house wiring.

## What about RF ground?

For HF, the single-point ground above provides RF ground for most situations. For wider-bandwidth or high-power stations, consider:

### Ground radials

A horizontal mat of wires (typically 1/4 wavelength on the lowest band, 30+ wires) buried just below the surface. Used for HF verticals; lowers ground impedance dramatically. Doesn't help much for receive.

### Ground plane

For VHF/UHF, a small set of 1/4-wavelength radials (4–8 of them) at the antenna feedpoint serves as the ground plane. The single ground rod in your shack doesn't help much at these frequencies — wavelengths are too short.

### Counterpoise

A short, isolated wire (a few feet long, in air) connected to the radio's ground binding post and routed away from the operator. Provides a high-impedance "fake ground" reference for portable operations where a real ground rod isn't available.

## Common grounding mistakes

### Multiple ground rods, not bonded together

Each piece of equipment to its own ground rod. Sounds thorough; actually creates ground loops. The house AC ground and the shack ground rod must be bonded.

### Long, thin, looped ground wires

A 30-foot length of 18 AWG wire winding around the shack to reach a ground rod has high RF impedance — it's effectively not connected for RF purposes. Use thick, short conductors.

### Ground plane = AC ground

Some operators ground the radio chassis to the AC outlet's third pin and call it done. This works for AC safety; it does NOT work for RF. The AC ground is many feet of household wiring away from earth — high RF impedance.

### Lightning protection on the wrong side

A lightning arrestor (PolyPhaser, etc.) on the coax must be installed at the building entry point and grounded directly to the ground rod with a short, thick wire. Putting it inside the shack defeats most of its purpose — the surge enters the building first and finds your equipment along the way.

### "Floating" station

Some operators leave their gear ungrounded, reasoning "it's safer that way." This is wrong. AC safety ground is required for shock protection. RF ground is required for clean station operation. The only safe ground is a properly designed one.

> **Advanced —** True RF ground at HF is impossible to achieve with a single ground rod. The skin depth in earth at 14 MHz is about 1 meter, so an 8-foot rod connects you to the top meter of soil — which has nontrivial impedance (typically 5–20 Ω at HF for moist soil, much higher for dry). The "RF ground" concept is partly fiction; what matters is providing a low-impedance reference for the local equipment loop, which is what the single-point bus bar gives you. Verticals achieve a true RF ground via a radial mat that bypasses the soil entirely. The shack ground is for safety and equalization, not as a primary RF reference.

## Lightning protection — a brief detour

Grounding intersects with lightning protection in important ways:

- **Disconnect the antenna from the radio when not in use.** A coax disconnect at the entry point (with the disconnected end earthed via a knife switch or simply a clamp to the ground rod) is the gold standard.
- **PolyPhaser arrestors at the entry point** for cases when you can't (or won't) disconnect every time. Cost ~$60 each.
- **Bond all metal at the entry point**: coax shield, rotor cable, ground wire from the tower base, all to the same single point.
- **Replace arrestors after a strike** — they're consumables.
- **Lift coax off the floor** in the shack if disconnected — a surge that does enter the shack should not have a path through your gear.

A station that survives a nearby lightning strike isn't lucky; it's well-grounded.

## Test your grounding

A few things you can check:

- With your radio off, AC unplugged: measure resistance from the radio chassis to the AC outlet's ground pin. Should be near zero (a few ohms at most).
- Measure resistance from your single-point ground bus to the AC outlet's ground pin. Should also be near zero (your bus is bonded, right?).
- Use a clamp-on RF current meter on the AC cord during TX. Should read essentially zero (no common-mode getting into the AC line).
- Touch all the equipment chassis during TX (low power). No tingles. No SWR changes.

If any of these fail, your grounding system has gaps. Walk it from scratch.

## See also

- §13-04 — RF feedback (the symptom of bad grounding)
- §12-06 — feedline routing (a related issue)
- §14-05 — RFI isolation workflow (also about ground/common-mode)
