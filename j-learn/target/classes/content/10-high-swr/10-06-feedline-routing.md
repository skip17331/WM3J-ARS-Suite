---
id: 10-06
title: Feedline Routing
chapter: 10
section: 06
level: simple
status: draft
---

# Feedline Routing

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

How the coax leaves the antenna and where it goes matters as much as what cable you used. Bad routing causes **common-mode current** — RF flowing on the outside of the coax shield instead of staying inside between conductor and shield. Common-mode currents make the coax act as part of the antenna, distorting the pattern, raising SWR, and dumping RF into the shack where it doesn't belong.

## Symptoms that point at feedline routing

- **SWR changes when you touch the coax** in the shack.
- **RFI in your house** — TV, stereo, computer, lights — when you transmit.
- **The S-meter on RX changes when you walk near the coax inside.**
- **A low-power signal reads strong on a nearby radio**, suggesting the coax is radiating.
- **SWR is fine outside but high inside** — the feedline picks up reflections from the coupling.
- **The radio's case is "hot" with RF** — you get a tingle touching the metal chassis.

These are all common-mode current symptoms. The SWR may not be wildly high, but the system is not behaving correctly.

## Why coax leaves the antenna direction matters

A balanced antenna (dipole, loop, Yagi) wants its feedline to leave **at 90° to the antenna** (perpendicular). If the coax runs along the antenna for any distance, currents on the antenna couple to the coax shield, and the shield carries a fraction of the antenna current.

**Best:** coax drops straight down from the center of a dipole.
**Acceptable:** coax leaves at 60–90° to the dipole axis.
**Bad:** coax runs parallel to the dipole for several feet before turning away.
**Worst:** coax runs parallel to the dipole and is the same length as one of the dipole legs (creates a re-radiating element of resonant length).

For verticals, the coax should drop straight down or run perpendicular to the radial system, not along a radial.

## How to fix bad routing

### Add a current choke ("line isolator")

The most universal fix. A **1:1 current balun** at the antenna feedpoint, plus optionally a second one at the rig end of the coax, breaks the common-mode current path. Discussed in §10-05.

A homemade choke can be as simple as winding 8–10 turns of the coax through a stack of FT-240-43 toroids near the antenna feedpoint. A commercial choke (Balun Designs, MyAntennas, DX Engineering) costs $40–80 and saves the build time.

### Re-route the coax

If the coax is parallel to the antenna, change the route. Pull it perpendicular for at least the first 1/4 wavelength on the lowest operating band.

### Use enough turns of choking

A "1 turn" wrap of coax around a toroid gives some choking. 8 turns gives a lot. The common-mode impedance scales as the square of turns count, so doubling turns quadruples the choking impedance. Aim for at least 3000 Ω of common-mode Z at the lowest band you operate.

### Loop the coax (the "ugly balun")

If you don't have a ferrite core, just coil the coax in a tight loop near the antenna feedpoint — about 6–8 turns of 6-inch diameter coil. Less effective than a ferrite-cored choke, but free and helpful.

### Run the coax through grounded conduit indoors

Inside the shack, putting the coax inside grounded metal conduit shields the feedline from radiating into the room. Effective for serious RFI cases.

## Common routing mistakes

| Mistake | Why it's bad | Fix |
|---------|--------------|-----|
| Coax wound around tower next to a vertical | Couples to vertical; tower also carries RF | Run coax inside the tower or away from it |
| Coax laid in roof gutter | Gutter is a long resonant conductor; couples strongly | Re-route away from gutter |
| Coax along power line for 50 ft | Capacitive coupling to AC | Re-route or put coax in conduit |
| Sharp 90° bends | Local impedance discontinuity, raises SWR slightly; mechanical stress | Use bending radius ≥ 10× cable diameter |
| Excess slack coiled in a loop right at the feedpoint | Acts as an unintentional inductor | Coil at the rig end if you need slack, not at the antenna |
| Multiple feed lines bundled together | Cross-coupling between lines | Separate by at least the cable diameter |

## How to test for common-mode current

Several ways:

### RF current meter

A clamp-on RF current meter (commercial: MFJ-854; DIY: a small toroid with a few turns and a diode detector) measures the current on the outside of the coax shield. Healthy choking should bring this to single milliamps even at 100 W TX.

### Touch test

With the radio on at low power transmitting a steady carrier, touch the metal chassis or the coax shell. Tingle? RF burn? You have common-mode current. (Always test at 5 W or less; 100 W can give a real burn.)

### Indicator light bulb

A neon bulb (NE-2, "indicator lamp") held near the coax indoors lights up if the coax is radiating. Useful for finding hot spots along the run.

### SWR-vs-touching test

If the SWR meter reading changes when you touch the coax, the system is sensitive to common-mode and you have routing/choking problems.

## Coax routing and lightning safety

A separate but related concern: outdoor coax should be **grounded at the building entry point** to drain static and lightning surges. A bulkhead-mount lightning arrestor (PolyPhaser is the brand most operators trust) at the entry, bonded to a station ground rod, is best practice.

This is a safety issue, not just an SWR one — but a damaged arrestor or a bad ground bond can cause weird SWR symptoms too.

> ⚙️ **Advanced —** Common-mode current isn't always bad in itself — it's a problem when the antenna designer didn't intend it. Some antenna designs (off-center-fed dipoles, end-fed half-waves with insufficient counterpoise) deliberately use common-mode current as part of the radiating system. In these cases, removing the common mode (with too much choking) can actually make the antenna work worse. Match the choking strategy to the antenna design — read the manual, or model the system in NEC if you're building from scratch.

## See also

- §10-05 — balun (also relates to common-mode)
- §04-12 — baluns and chokes theory
- §12-rfi — RFI in the shack (often caused by feedline radiation)
