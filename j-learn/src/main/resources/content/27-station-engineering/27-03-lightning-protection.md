---
id: 27-03
title: Lightning Protection
chapter: 27
section: 03
level: mixed
status: draft
---

# Lightning Protection

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Lightning is a low-probability, high-cost event. A direct strike on an exposed amateur antenna is rare — maybe once in a decade for a typical suburban tower in a moderate-lightning region, much more often in Florida or the Gulf Coast. **Induced surges** from nearby strikes, on the other hand, are *routine*. A strike a quarter-mile away dumps several kV onto every long conductor in the neighborhood — power lines, phone lines, and, especially, your 100-foot horizontal antenna. The station equipment doesn't care whether the energy came from a direct hit or an induced pulse; it dies the same way.

This section is about what to install, where, and why — to survive both.

## Direct strikes vs induced surges

A direct strike is 10–200 kA delivered in ~10 μs. The peak voltage at the strike point is in the millions of volts. *Nothing* short-circuits a direct hit completely; the goal is to dissipate as much of it as possible to earth before it reaches the equipment.

An induced surge is much smaller — typically 1–10 kV at peak, with current in the hundreds of amps for tens of microseconds. **This is the case a properly-installed surge protector handles cleanly.** A station with good surge suppression at the coax entry, AC entry, and any other long-cable entry will shrug off most induced surges with no damage.

The math of why induced surges happen: a strike's leader-stroke EMF couples to nearby conductors via *magnetic induction* (changing current in the strike channel → changing flux through any loop nearby → voltage). A 100-foot dipole 25 ft above ground, parallel to a strike channel 1000 ft away, sees something like:

```
  V = -dΦ/dt
    ≈ μ₀ × I × (length × height) / (2π × distance × rise_time)
    ≈ 4π × 10⁻⁷ × 30,000 A × (30 × 7.5) / (2π × 300 × 10⁻⁵)
    ≈ 4500 V
```

(approximate, ignoring geometry detail). 4.5 kV on the coax. Without protection, that's at your radio's antenna port. With a $40 Polyphaser, it's clamped to about 600 V and dissipated to ground.

## The four protection layers

A complete amateur lightning protection scheme has four layers:

1. **Lightning rods on the structure (sometimes).** For towers, the top of the mast is the natural air terminal; no separate rod is needed if the tower is well-bonded. For roof-mounted antennas, a separate air terminal *above* the antenna may be worth adding.
2. **Bonding to multiple ground rods.** A single 8-foot rod can't handle a direct strike. Multiple rods spaced at least 6 ft apart (per NEC), all bonded together with #6 AWG or strap, give a much lower impedance to earth.
3. **Coax surge protectors at the entry panel.** This is where 95% of amateurs put their money. One arrestor per coax line, all mounted to a common copper bulkhead bonded to the ground-rod field.
4. **AC mains surge suppression.** Whole-house surge protector at the service panel, plus point-of-use surge strips at the shack.

Layer 3 is the one most often missing in amateur installations, and the one with the best cost/benefit ratio.

## Coax surge protectors

Three vendors dominate the amateur and commercial markets:

- **Polyphaser** (Smiths Interconnect) — the de-facto standard since the 1980s. The IS-50UX series (HF) and IS-B50LU (VHF/UHF) are the common amateur choices. Replaceable gas tube. ~$50–$100 each.
- **Times Microwave** — LP series. Comparable specs to Polyphaser; sometimes available cheaper from surplus distributors. ~$40–$80.
- **ICE (Industrial Communication Engineers)** — older brand, still available. The 300 series is HF; the 350 is VHF. Often found used; specs are conservative.

Other names: PolySwitch, Citel, Huber+Suhner, Andrew/CommScope. All work on similar principles.

### How they work

Two main technologies:

- **Gas-discharge tube (GDT)** — a sealed gas tube between the center conductor and shield. At normal voltages it's an open circuit; above its breakdown (typically 90–600 V) it ionizes and conducts heavily to ground. Reseals after the event. Replaceable cartridge in most models.
- **Quarter-wave stub** — a shorted λ/4 piece of transmission line at the operating frequency. Looks like an open circuit at the design frequency, looks like a DC short below it. Works only on a single band; common in VHF/UHF where bandwidth is narrow.

Most amateur HF arrestors are GDT-based. Most VHF/UHF arrestors are quarter-wave-stub-based.

### Where to mount

The arrestor must be mounted at the **entry panel** — the bulkhead where the coax enters the building — and bonded to ground with a strap, not a wire.

```
           OUTSIDE                  │      INSIDE
                                    │
                                    │
   Coax from antenna ──[arrestor]───┼─── coax to radio
                          │         │
                          │ 2" strap, 6" long, to ↓
                          ▼         │
                    ─────────────   │     copper bulkhead plate
                                    │
                          │         │      (bonded to ground rod
                          ▼         │       via #6 or strap)
                      Ground rod    │
```

The arrestor does nothing useful if its ground path is a long thin wire. **Short, wide, direct.** Inductance kills surge protection.

## NEC §810 outline

NEC §810 ("Radio and Television Equipment") is the relevant code section for amateur stations. The key requirements:

- **§810.20 — Antenna discharge unit.** Each antenna lead-in must have an antenna discharge unit (i.e., a lightning arrestor) located outside the building, or inside as close as practicable to the entry point.
- **§810.21 — Bonding.** The discharge unit's ground conductor must run as straight as possible to the ground electrode. Bends must be gradual.
- **§810.21(F)(1) — Conductor size.** Not less than #10 copper for amateur work. (Most amateurs use #6 or strap — exceeds code.)
- **§810.21(F)(2) — Length.** The ground conductor should be as short as practicable. Code suggests under 20 ft where feasible.
- **§810.21(J) — Bonding to other grounds.** The antenna ground must be bonded to the building's electrical service ground. **This is critical** — separate grounds with no bond create the worst-case voltage-gradient problem described in §27-01.

§810 is short (a few pages). Read it once. Most jurisdictions don't inspect amateur stations against it, but compliance is good engineering and good defense if an insurance claim ever arises.

## NFPA 780 — for towers with multiple antennas

NFPA 780 ("Standard for the Installation of Lightning Protection Systems") applies to structures with formal lightning protection, including towers. It calls for:

- A minimum of two ground rods bonded together for the tower base.
- Earth resistance under 25 Ω at each rod (10 Ω preferred).
- Specific conductor types and routing.
- Annual inspection.

Amateur towers under 50 ft on a single home don't typically need NFPA 780 compliance, but the principles (multiple bonded rods, low earth resistance) are sound and worth following.

## The cheapest protection: disconnect

If you're going to be away from the station during a thunderstorm, **physically unplug the coax from the radio** and move it to a safe location (ideally outside, or in a grounded box). This is the most reliable protection that exists. A disconnected coax can't deliver surge energy to a radio that isn't connected to it.

A common arrangement:

- Bulkhead-mounted coax connector on the shack wall.
- Short jumper from bulkhead to radio.
- During storms (or extended absences), unplug the jumper at the *outside* end of the bulkhead, so the antenna's coax terminates at an arrestor outside, and the jumper inside is floating.

For a station you actively use, you can't always disconnect. That's what the arrestor is for. But for a weekend trip during summer storms — unplug.

## A complete entry panel layout

```
                     OUTSIDE WALL
   ┌──────────────────────────────────────────────────┐
   │                                                  │
   │   Coax 1 ──[Polyphaser]──┐                       │
   │                          │                       │
   │   Coax 2 ──[Polyphaser]──┼── 2" Cu bulkhead ──┐  │
   │                          │   ┌────────────┐   │  │
   │   Coax 3 ──[Polyphaser]──┘   │ (entry panel)  │  │
   │                              └──────┬─────────┘  │
   │   Rotor cable ──[arrestor]──────────┤            │
   │                                     │            │
   │   AC line ──── (separate at service entry)       │
   │                                     │            │
   │                                     ▼            │
   │                                #6 Cu strap       │
   │                                     │            │
   │                                     ▼            │
   │                              ━━━━━━━━━━━━━       │
   │                              Ground rod #1       │
   │                                     ┃            │
   │                                     ┃ #6 (6ft+)  │
   │                                     ┃            │
   │                              ━━━━━━━━━━━━━       │
   │                              Ground rod #2       │
   │                                     ┃            │
   │                                     ┃ bond to    │
   │                                     ▼            │
   │                              AC service ground   │
   └──────────────────────────────────────────────────┘
```

This is the standard. Two rods, bonded together and to the AC service. All coax through arrestors on a common bulkhead. The bulkhead bonded with a short strap. Total cost for a 3-coax installation: ~$300–$500 depending on arrestor selection.

> ⚙️ **Advanced —** For high-value stations (amp, transverter, SDR), consider a *secondary* protection stage inside the shack: a smaller arrestor between the bulkhead jumper and the radio, plus a switched bypass. The first stage (outside) dissipates the bulk of the energy; the second stage clamps anything that gets through. This is two-stage protection and is standard in commercial sites. Polyphaser sells dedicated "secondary protectors" rated for the post-bulkhead voltages. Not necessary for most amateur stations but worth considering for a $5,000 amplifier.

## Related sections

- [§16-07 — Ground System Inspection](../16-maintenance/16-07-ground-system.md) — maintenance of the ground side of all this
- [§16-09 — Cable Entry](../16-maintenance/16-09-cable-entry.md) — weatherproofing the entry panel
- [§27-01 — Single-Point Grounding](27-01-single-point-grounding.md) — the bus the bulkhead bonds to
- [§13 — Station Troubleshooting](../13-station-troubleshooting/13-00-overview.md) — diagnosing post-strike damage
