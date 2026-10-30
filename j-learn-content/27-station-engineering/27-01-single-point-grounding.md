---
id: 27-01
title: Single-Point Grounding
chapter: 27
section: 01
level: mixed
status: published
---

# Single-Point Grounding

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The **single-point ground (SPG)** is the most important concept in station engineering. It says: every ground reference in the station — every chassis, every coax shield, every arrestor, every power-supply return — converges at **one physical bus bar**, and that bus bar is bonded to earth at one place.

If you do nothing else from this chapter, do this.

## The problem the SPG solves

Imagine three pieces of gear, each with its own ground wire running to a separate ground rod in the back yard:

```
   Radio ──────── (long #14 wire) ──────► Rod A
   Amp   ──────── (long #14 wire) ──────► Rod B  (12 ft from A)
   PC    ──────── (long #14 wire) ──────► Rod C  (20 ft from A)
```

In the static state, with nothing happening, everything looks fine. A multimeter would read 0 V between any two grounds.

Now imagine **a nearby lightning strike** dumps 10 kA into the earth somewhere between Rod B and Rod C. The earth's resistance turns that current into a voltage gradient — for a few microseconds, Rod C might sit at +2,000 V relative to Rod A. The PC's chassis is now at +2,000 V. The PC is connected to the radio via a USB cable. The USB shield tries to equalize those potentials. It fails. The radio's USB interface and the PC's port both die.

This is a **ground loop** in its most destructive form. The same physics, at lower amplitudes, produces audible hum, RF "in the shack," intermittent USB dropouts, and CAT control failures.

The SPG fixes this. All three grounds tie to **one bus**. Bus ties to **one ground point**. There is no longer a voltage gradient *between* equipment grounds because there is no longer a path between them via the earth.

## Star topology vs daisy-chain

There are two ways to wire multiple devices to the SPG bus:

### Star (correct)

```
                              ┌─── Radio
                              │
   Ground Bus ────────────────┼─── Amp
        │                     │
        │                     ├─── PC
        │                     │
        │                     ├─── Tuner
        │                     │
        ▼                     └─── PSU
   Ground Rod
```

Each device has its own dedicated strap back to the bus. The bus is one continuous piece of copper. Current from any device's fault returns directly to the bus without crossing another device's strap.

### Daisy-chain (wrong)

```
   Ground Rod ── Radio ── Amp ── PC ── Tuner ── PSU
```

Each device is chained to the next. The radio's ground strap also carries the amp's fault current. A fault at the PSU end has to traverse four jumpers to reach earth. Inductance compounds. RF currents from each chassis modulate the others.

**Always star. Never chain.** The bus bar is the physical embodiment of the star.

## The bus bar itself

The SPG bus is typically a **flat copper bar** — 1/4" thick, 2" wide, 12-36" long — with tapped holes (usually 1/4"-20) every inch or two. Each device's ground strap terminates in a lug bolted to the bar. Examples:

- **Harger CB-2x12** — copper bus, 2" × 12", pre-tapped. ~$50.
- **DX Engineering UNHB-2** — bonded copper bus with ground-rod terminal. ~$80.
- **Home-built from 1/4" × 2" × 24" copper bar stock** with self-tapped holes. ~$40.

The bus is mounted on a wall behind the gear, or on the wall *outside* the shack at the entry panel, depending on the philosophy (see §27-03).

## Strap material: 2" copper vs #6 AWG

The two standard ground conductors for amateur stations are **2"-wide copper strap** (about 0.025" thick, sold as "flashing" or "ground strap") and **#6 AWG stranded copper wire** (green-jacketed or bare).

| Property | 2" copper strap | #6 AWG wire |
|----------|-----------------|-------------|
| DC resistance | ~0.0004 Ω/ft | ~0.0004 Ω/ft |
| Inductance | ~0.2 μH/ft | ~0.4 μH/ft |
| Impedance @ 14 MHz, 3 ft | ~50 Ω | ~100 Ω |
| Impedance @ 28 MHz, 3 ft | ~100 Ω | ~200 Ω |
| Cost per foot | $4–6 | $1.50 |
| Flexibility | Low (sharp bends crease) | High |
| Aesthetics | Industrial | Wire-like |

**For DC and 60 Hz safety current**, they're identical. The green-wire safety path back to the service panel can be #6 (or even #10 for branch circuits) without issue.

**For RF**, the strap wins by a factor of 2 in inductance. At HF the difference matters; at VHF/UHF it matters a lot. The skin effect at RF makes wide, flat conductors more effective than round ones — current flows on the surface, and 2" of strap presents more surface than #6 wire.

**Practical recommendation:**
- **Strap** for: chassis-to-bus bonds (the short runs), arrestor-to-bus, anywhere RF currents flow.
- **#6 AWG** for: bus-to-ground-rod (one continuous run, no joints), supplemental rod-to-rod bonding.

Where you mix the two, use a proper transition lug (Polyphaser, Harger, or generic copper compression lugs).

## Where the bus mounts

Two philosophies:

**1. Inside the shack** — bus on the wall behind the desk, everything bonds to it, single #6 (or strap) leaves the wall to the ground rod outside. Simple. Easy to inspect. Easy to add new gear. Downside: lightning energy entering via coax has to traverse the whole shack to reach the outside ground.

**2. At the entry panel (outside)** — bus mounted on an aluminum or copper bulkhead plate where every coax enters the building. All arrestors mount here. Inside, each chassis bonds to a smaller "shack bus" which has its own short strap out to the entry bus. This is the **professional standard** (Motorola R-56, broadcast practice). Best lightning behavior. More work to install.

For the typical home shack, philosophy #1 is fine if the ground-rod run is short (< 10 ft) and there's a properly-rated arrestor at the entry. Philosophy #2 is mandatory if the shack is on an upper floor or far from grade.

> **Advanced —** The reason philosophy #2 is preferred for serious installations is that the *entry panel* becomes the lightning surge boundary. Energy is dissipated to earth *before* it enters the building. Inside the shack, the SPG bus is at the same potential as the equipment chassis and the operator — even during a strike. The shack is essentially inside a *Faraday cage* for surge purposes. This is the same principle that protects commercial sites with thousands of dollars of equipment from strikes that would atomize an unprotected amateur station.

## What lands on the bus

Every metal-cased item in the station, plus every coax shield (via the arrestor):

- Radio chassis (the rear-panel ground lug)
- Amplifier chassis
- Antenna tuner chassis
- Power supply chassis (12 V negative is *also* bonded here in most stations — see §27-09)
- Computer chassis (if it has a ground lug; otherwise via the AC mains green wire is acceptable)
- Coax arrestors (at the entry, before the coax enters the shack)
- Rotor control cable shield
- Headphone/mic line shields (if separate)
- Any external metal fixture (lamp arm, metal desk frame)

Things that do **not** go on the bus:

- The AC neutral. Neutral is bonded to ground *only at the service entrance*, never anywhere else (this is a NEC requirement; bonding neutral to ground elsewhere creates parallel return paths and trips GFCIs).
- Telephone or cable-TV grounds — they have their own service-entrance bond.

## A sketch of the finished arrangement

```
            ┌──────────────────────────────────────────┐
            │              SHACK WALL                  │
            │                                          │
   Radio ───┤  ┌──────────────────┐                    │
   Amp ─────┤  │   SPG bus (Cu)   │                    │
   Tuner ───┤  │ ━━━━━━━━━━━━━━━━ │                    │
   PSU ─────┤  └────────┬─────────┘                    │
   PC ──────┤           │ 2" strap or #6 AWG           │
            │           │                              │
            │           ▼                              │
            └──────────────────────────────────────────┘
                        │
                        ▼ (through wall, shortest path)
                  ━━━━━━━━━━━━━━ Ground rod, 8 ft, copper-clad
                                  Cadwelded connection
```

See [§16-07 — Ground System Inspection](../16-maintenance/16-07-ground-system.md) for the maintenance perspective on the same arrangement, and §27-02 for how RF bonding at each chassis ties in.
