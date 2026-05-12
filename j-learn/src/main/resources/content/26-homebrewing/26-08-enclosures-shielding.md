---
id: 26-08
title: Enclosures & Shielding
chapter: 26
section: 08
level: mixed
status: draft
---

# Enclosures & Shielding

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## Why the box matters

A homebrew project lives or dies by its enclosure. The same circuit, *unshielded*, may:

- Pick up local broadcast stations and intermix them with your CW signal
- Radiate harmonics from internal traces that the LPF was supposed to suppress
- Couple between input and output stages, oscillating spontaneously
- Catch the field from a nearby switching supply and add hum to clean audio

A proper enclosure does three things: **shields** internal circuits from external RF, **shields** the world from your circuit's RF, and **provides a ground reference** for the layout inside. The box is part of the circuit, not just a container.

## Enclosure types — picking the right box

| Type | Material | Cost | RF-tight? | Use case |
|------|----------|------|-----------|----------|
| **Hammond 1590-series die-cast** | Cast aluminum | $8–25 | **Excellent** | RF projects, especially with through-bulkhead connectors |
| **Hammond 1455-series extruded** | Aluminum extrusion + end caps | $10–30 | Good | RF projects where Eddystone look isn't required |
| **Tin-plate steel hobby box** | Steel sheet | $5–15 | **Excellent** at HF | Builder-favorite for small RF circuits |
| **Diecast pressure box (BUD CU-3000)** | Aluminum | $15–40 | Excellent | Larger amplifier projects |
| **Plastic project box** | ABS / polystyrene | $3–10 | **Useless** for RF | Audio-only or DC-only projects |
| **Bud Industries minibox** | Painted steel | $8–20 | Good | Two-piece slip-cover; classic ham look |
| **Servers / surplus 19" rack chassis** | Steel | $20+ | Good | Big amplifier projects, station integration |

### Hammond 1590 — the QRP classic

The Hammond 1590-series ("Eddystone-style") die-cast aluminum boxes are the most-used amateur enclosure. Sizes:

| Part # | Inside dim (in) | Typical use |
|--------|-----------------|-------------|
| 1590A | 1.5 × 1.0 × 0.6 | Tiny QRP, attenuators |
| **1590B** | **3.6 × 1.5 × 1.1** | **Most QRP transmitters, filters** |
| **1590BB** | **4.7 × 2.6 × 1.2** | **5 W class-E PAs, multi-band BPF** |
| 1590C | 4.7 × 2.6 × 2.1 | Deeper version for taller parts |
| 1590D | 7.4 × 4.7 × 2.0 | Larger projects, antenna tuners |

Die-cast aluminum has excellent RF properties: lossy enough at HF to absorb stray fields, conductive enough to be a good Faraday cage. The lid is held on by 4 screws into threaded inserts; the box-to-lid seam is the weakest shielding point and benefits from EMI gasket for above-30-MHz work.

### Tin-plate steel boxes

The "tin box" tradition comes from QRP construction culture. These are steel boxes coated in tin (so they solder easily) typically 3" × 4" × 1" or similar. They cost almost nothing, the lid solders to the box for a fully-sealed shield, and they're inherently grounded along the entire seam.

Sources: Mouser stocks the Bud Industries CU-2 (tin-plated, 3 × 4 × 1 in). KitsAndParts.com used to ship custom-cut tin sheet for the QRP crowd; Manhattan-style construction culture sometimes builds the entire enclosure from soldered tin sheet.

### Copper-tape DIY shielding

A workaround when a project outgrows its enclosure: line the inside walls with **copper foil tape** (the same kind used for guitar pickguards or stained-glass work). The tape is conductive on both sides; overlap the seams and solder the joints. A plastic project box can be retrofitted into a competent HF shield with $10 of copper tape.

Caveat: copper tape adhesive degrades over years in heat. For permanent installations, prefer a real metal box.

## Feedthrough capacitors — the unsung hero

A **feedthrough capacitor** is a capacitor with a wire passing through its center, designed to be mounted *through a chassis wall*. The wire side is the signal lead; the capacitor body bonds to the chassis. The result:

```
     │   Chassis wall (ground)
     │
─────┤●●●●●●●●●●●●─── Signal lead
     │   ↑
     │   capacitor body solders to chassis, providing
     │   shunt-to-ground capacitance right at the wall
     │
     │
```

This is the **only correct way** to bring a DC power lead, audio signal, or control line *into* a shielded RF enclosure. Without a feedthrough, the wire itself acts as a transmitting antenna inside the box and a receiving antenna outside — defeating the shielding entirely.

Typical values:

| Application | Feedthrough cap value | Why |
|-------------|------------------------|-----|
| DC supply input | 1000 pF – 10 nF | Bypasses RF to chassis |
| Audio output | 1 nF – 10 nF | Bypasses RF without affecting audio |
| Control lines (TX/RX switching) | 1 nF | Bypasses RF while passing slow DC |
| RF input/output | **N/A — use a proper coax connector** | A feedthrough cap on RF input is a 6 dB attenuator |

Suppliers: Spectrum Control, Tusonix, AVX, Murata — all stock standard 6-32 thread feedthroughs. Cost: $1–5 each.

> ⚙️ **Advanced —** For very high-RF applications (a kilowatt PA filter chamber), use **multilayer ceramic feedthroughs** (Murata DSS series) that integrate a small ferrite bead into the feedthrough body. The bead adds series impedance, the cap adds shunt impedance — together they form an L-C low-pass that buys you 30+ dB of additional attenuation per lead. They're $5–15 each, but for a legal-limit PA where 200 W of harmonics on the DC bias lead can cause TVI, they're cheap.

## Ground-plane PCBs

For RF projects on PCB, use **double-sided board** with the top side as the **ground plane** (large unbroken copper area) and the bottom side as the signal layer. Or vice versa — what matters is that one entire side is a continuous ground reference.

Why ground plane matters:

1. **Low-inductance return paths.** Every signal current returns through the ground plane directly under the signal trace. The loop area is tiny, so radiation is minimal.
2. **Shielding between traces.** Adjacent signal traces on the bottom side don't couple through the ground plane.
3. **Lower impedance for bypass caps.** Bypass caps to the ground plane have a few mΩ of path; bypass caps to a wire have hundreds of mΩ.

**Via stitching** — drill many vias from top to bottom ground plane along the edges of the board — keeps the two planes at the same RF potential. Without stitching, the planes can resonate at a high frequency and become an unwanted antenna.

For Manhattan-style or Ugly-Construction (which see, §26-10), the unetched copper-clad PCB *is* the ground plane. You glue small islands of cut-up PCB to it for component pads, and every component's "ground" lead solders directly to the plane.

## Pi-network shielding (compartmentalized layouts)

In a multi-stage receiver or transmitter, **each stage gets its own shielded compartment** inside a larger enclosure. The stages are connected only through carefully filtered/coupled points.

```
    ┌──────────┬──────────┬──────────┐
    │  RF amp  │  Mixer   │   IF amp │
    │          ●          ●          │
    │  shield  │  shield  │  shield  │
    │   wall   │   wall   │   wall   │
    └──────────┴──────────┴──────────┘
        ● = feedthrough capacitor or coupling cap
        through the shield wall

```

This is **expensive** to build (each wall is a piece of soldered tin or aluminum, fitted to the main box) but it's how every high-performance receiver from the 1950s through the 1980s was constructed. Modern surface-mount design and dense PCB ground planes have made compartmentalization less critical, but for amateur tube-era construction or RF-amplifier output filter stages, it's still relevant.

A simple example: an HF LPF for a kilowatt amplifier has three or four pi-sections, each in its own compartment, connected only through feedthrough capacitors at the wall interfaces. Without compartmentalization, the output of the filter capacitively couples back to the input through stray paths, and the harmonic suppression you designed for is destroyed.

## Common shielding mistakes

- **Plastic enclosure for an RF project.** It just won't work. Use metal.
- **Painted enclosure interior.** The paint is an insulator. Scrape paint off where the lid mates and where you solder ground connections.
- **DC lead entering through a hole, no feedthrough cap.** The lead is an antenna; the box no longer shields anything. Add a feedthrough cap on every wire that crosses the enclosure wall.
- **Coax connector grounded only at one end.** The shield must bond to the chassis at the connector. Use bulkhead connectors (SO-239 bulkhead, BNC bulkhead) that bond to the chassis at the mounting hole.
- **Lid not bonded to box.** Painted lid screws can be high-resistance. Use star washers under the lid screws to bite through the paint, or scrape paint where they contact.
- **Long ground straps.** A 6-inch wire to ground has ~6 nH of inductance — at 30 MHz that's 1 Ω of reactance, ruining the shield. Keep ground straps as short and wide as possible.

## See also

- [§26-09 — Grounding for Homebrew](26-09-grounding-for-homebrew.md) — star grounding within the chassis
- [§26-10 — PCB Layout Basics](26-10-pcb-layout-basics.md) — ground planes and stitching
- [§26-02 — Low-Pass Filters](26-02-low-pass-filters.md) — filters that *need* shielding to work
- [§14 — RFI](../14-rfi/) — why shielding matters at the station level
- [§27 — Station Engineering](../27-station-engineering/) — broader grounding context
