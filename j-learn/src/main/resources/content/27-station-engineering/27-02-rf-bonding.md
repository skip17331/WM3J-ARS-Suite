---
id: 27-02
title: RF Bonding
chapter: 27
section: 02
level: mixed
status: draft
---

# RF Bonding

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The single-point ground (§27-01) gets every chassis to the same DC potential. **RF bonding** keeps them at the same *RF* potential — across the frequencies the station actually operates on. The two are related but distinct, and confusing them is the single most common reason amateurs experience "RF in the shack."

## Why RF bonding is different from DC grounding

At DC, a copper wire is a copper wire. Its resistance is its impedance. A 6-foot #14 ground wire has 0.0015 Ω. That's perfectly adequate for AC safety — fault current of 20 A produces 30 mV across it, which is nothing.

At HF, that same wire is an **inductor**. A 6-foot #14 wire has about 2.4 μH of inductance. At 14 MHz:

```
  X_L = 2 π f L
      = 2 π × 14e6 × 2.4e-6
      = 211 Ω
```

The "ground wire" presents 211 Ω of impedance to RF currents trying to flow through it. That's not a ground anymore — it's a *bandpass-blocking inductor* that keeps RF *out* of the ground. Whatever common-mode current was flowing on a coax shield or DC return now has nowhere to go except back into the equipment chassis, where it produces hum, oscillation, audible RF "biting," and confused logic in CAT interfaces.

This is the heart of the bonding problem. **The "ground wire" your radio came with is not an RF ground.** It's a DC and 60 Hz fault-current path. Anything else needs separate consideration.

## The fix: chassis-to-chassis braid

Instead of relying on long wires from each chassis to a remote bus, **bond adjacent chassis directly to each other** with the shortest, widest, lowest-inductance conductor available — typically **tinned copper braid** or **flat copper strap**.

A good rule: any two pieces of gear that are physically next to each other should have a braid bond no longer than 6 inches connecting their rear-panel chassis lugs.

```
   ┌──────────┐ ──6" braid── ┌──────────┐ ──6" braid── ┌──────────┐
   │  RADIO   │              │   AMP    │              │  TUNER   │
   └─────┬────┘              └─────┬────┘              └─────┬────┘
         │                         │                         │
         └─────── all three ───────┴─── to SPG bus ──────────┘
                  (one short strap to the bus from any of them)
```

The reasoning: a 6" braid at 14 MHz has about 0.15 μH and 13 Ω of inductive reactance — about 16× lower impedance than the same length of #14 wire, and 20× lower than a 6-foot wire. The chassis are now electrically *one piece of metal* across the HF spectrum, even though they're physically three separate boxes.

## Why short bonds matter — quantitatively

The skin effect at RF means the actual conductor material is irrelevant — what matters is **surface area** and **length**. Inductance scales roughly linearly with length and roughly logarithmically with width. So:

| Conductor | Length | L (μH) | X_L @ 14 MHz |
|-----------|--------|--------|--------------|
| #14 wire | 12 in | 0.4 | 35 Ω |
| #14 wire | 6 in | 0.2 | 18 Ω |
| #14 wire | 3 in | 0.1 | 9 Ω |
| 1" braid | 6 in | 0.1 | 9 Ω |
| 2" strap | 6 in | 0.06 | 5 Ω |
| 2" strap | 12 in | 0.12 | 11 Ω |
| 2" strap | 24 in | 0.24 | 22 Ω |

A short, wide strap is better than a long, narrow one — but a *short* wire isn't terrible. **Length matters more than width.** This is why the chassis-to-chassis approach works: it makes the bonds short by physically putting equipment close together, regardless of strap width.

## "RF ground" vs "safety ground"

There are two philosophies on whether RF and safety grounds should share a single bus:

**Unified school:** One bus, both safety and RF returns. Simpler. Works fine for HF stations where the wavelength is long compared to the station footprint. This is the recommended approach for almost every amateur station.

**Separate school:** A dedicated RF bus (often a "halo" of copper strap running behind the gear) separate from the green-wire AC ground. The two are bonded at one point. Useful in stations with severe ground-loop hum, or where the AC mains is itself a noise source.

For most amateurs: **unified.** A single SPG bus with both safety and RF traffic. The fault current path (60 Hz, low frequency, large) and the RF path (HF/VHF, low current, common-mode) don't interfere with each other if the bus is a substantial chunk of copper.

Where the unified approach breaks down is at VHF/UHF, where wavelengths become comparable to the station footprint. A 6" bond at 144 MHz is ~50 Ω — significant. At 432 MHz it's ~150 Ω. For VHF+ operation, **add a second short bond** at each chassis directly to a nearby ground point, or co-locate equipment so bonds are 2" or less.

## Bonding the coax shields

The coax shield is itself a ground conductor. Where each coax enters the equipment, its shield is bonded to the chassis at the connector. But the shield is *also* trying to carry RF currents from the antenna back to the station — and if the chassis ground isn't a real RF ground, those currents flow on the *outside* of the shield and into anything attached to the chassis (mic cable, USB, AC line).

The standard fix: **a common-mode choke on the coax at the entry to the shack** (cross-link [§18-05](../18-coax-connectors/18-05-baluns-chokes.md)). This forces the differential RF (the wanted signal on the inner conductor and inside of the shield) through, while blocking the common-mode RF (the unwanted current on the outside of the shield). A typical choke is 5–10 turns of coax through an FT-240-31 or FT-240-43 toroid, mounted at the entry panel or at the antenna feedpoint.

The choke and the bond work together. Without the bond, the choke just sees common-mode current with nowhere to go and pushes it elsewhere. Without the choke, the bond carries common-mode current it shouldn't have to.

## Practical bonding kit

For a typical 4-piece HF station (radio, amp, tuner, PSU), a starter bonding kit:

- 4 × 6" copper braid jumpers (Harger BJ-6 or equivalent) with 5/16" eyelets — ~$30 total
- 1 × 2" × 18" copper strap to the SPG bus — ~$20
- Stainless 5/16" hardware (bolts, nuts, lock washers) — already at the lug points on each chassis
- One bottle of NoAlOx or Penetrox antioxidant for between copper and aluminum — ~$10

Total: under $70 for a station-wide bonding upgrade. The improvement in HF noise floor (typically 6–15 dB on receive) and elimination of RF-in-the-shack feedback is dramatic and immediate.

> **Advanced —** For stations operating on 6 m and up, consider a *bonding plane* — a sheet of copper foil or aluminum plate behind/under the equipment, with each chassis bolted directly to it via the rear-panel ground lugs. The plane itself bonds to the SPG bus. This brings inter-chassis impedance to near zero across all amateur bands through 70 cm. Used in EME stations, weak-signal VHF setups, and contest stations where every dB of noise floor matters. A 24" × 36" × 0.04" copper sheet costs ~$80 and ties together everything bolted to it.

## Bonding antennas and feedlines

The same principles extend outside:

- **Tower-mounted equipment** (rotors, preamps, switches) is bonded to the tower with a short strap at each junction.
- **Each coax shield** is bonded to the tower at the top (where it leaves the antenna) and at the bottom (where it leaves the tower toward the entry panel). This is sometimes called *shield grounding* and prevents the coax shield from being an unintentional antenna.
- **Tower-to-SPG bond** uses copper strap, never round wire, and is as short as the layout allows.

See [§16-07](../16-maintenance/16-07-ground-system.md) for the inspection cadence on these outdoor bonds.

## Symptoms of bad bonding

Recognize them when they show up:

- **Mic "bites" you** when transmitting (RF on the mic shell — common at higher power).
- **CAT control disconnects** on transmit (USB-serial chip resets due to common-mode current).
- **Hum on FT8 audio** that disappears when you unplug the radio's chassis ground.
- **The amp's meter swings** when you key the radio without the amp on (RF leaking onto the amp chassis through the bond path).
- **TVI on your own TV** that disappears when you touch the radio chassis (you're providing a better ground than the rig has).

These are all RF bonding problems, not RFI problems. Fix the bonding first; the symptoms vanish.
