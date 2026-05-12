---
id: 26-11
title: RF Safety in Homebrew
chapter: 26
section: 11
level: mixed
status: draft
---

# RF Safety in Homebrew

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What can kill you (and what can just hurt)

Homebrew RF work has three distinct hazard categories. Understand which one you're working with:

| Hazard | What it does | Where it lives |
|--------|--------------|----------------|
| **High DC voltage** (≥ 50 V) | Cardiac arrest, burns, electrocution | Tube-amp plate supplies, capacitor banks, voltage doublers |
| **High DC current at low voltage** (12 V, 50 A+) | Arc burns, fire, melted metal | Solid-state PA supplies, lead-acid batteries, jumper cables |
| **RF burns and tissue heating** | Surface burns, deep heating, possibly cataracts | Antenna feedpoints under TX, final amp output, any exposed RF conductor at power |

The first one kills directly. The second one starts fires. The third one cooks you slowly. All three require disciplined practice; none are negotiable.

## High-voltage tube amplifiers — the dangerous classic

A typical legal-limit tube linear (e.g., Ameritron AL-82, Heathkit SB-220, Henry 2KD) uses a pair of triodes (3-500ZG, 3CX800A7, GS-35B) running on a **2000–4000 V plate supply** that can deliver hundreds of milliamps. The capacitor bank in the plate supply stores enough energy to cause **cardiac fibrillation in under 200 ms** if you touch the wrong node.

A typical AL-82 plate-supply capacitor:

```
Two 480 µF caps in series → 240 µF at 4000 V working voltage
Stored energy = 0.5 × C × V² = 0.5 × 240×10⁻⁶ × 4000² = 1920 joules
```

For comparison, a defibrillator discharge is 200 joules. The plate cap stores **10× a defibrillator's energy**. Direct contact with both terminals through the chest is fatal essentially every time.

The rule: **never work on a tube amp with mains plugged in. Never. Even "just to look."** The first step in any tube-amp maintenance is to unplug the AC mains, then short the plate caps to chassis with a **shorting stick** (a wooden dowel with a copper hook at the end, connected to chassis through a 100 Ω 50 W resistor). Hold the short for 30 seconds. Then *verify with a meter* that the caps read 0 V before any hands enter the chassis.

### Bleeder resistors — mandatory, not optional

Every high-voltage power supply **must** include a **bleeder resistor** across each filter capacitor. The bleeder drains the cap to a safe voltage within 30 seconds of power-off. Without a bleeder, the cap holds dangerous voltage for *hours* — and someone, eventually, opens the chassis without realizing.

Sizing:

```
R_bleeder = V / 0.01 = V × 100 (ohms per volt of working voltage)
P_bleeder = V² / R (watts)
```

For a 4000 V supply: R = 400 kΩ. P = 16,000,000 / 400,000 = 40 W. Use **two 200 kΩ 25 W resistors in series**, mounted on ceramic standoffs (they get hot in normal operation). Cost: ~$8. The cost of leaving them out: a dead amateur.

### Interlock switches

A second mandatory protection: the lid of the amplifier should have an **interlock switch** — a microswitch wired in series with the AC mains primary so the supply *cannot* be energized with the lid off. If someone forgets to discharge before opening, the supply is at least not actively powered.

Cheap microswitches (~$2 each) and a chassis cut-out solve this. Manufacturers include interlocks in commercial amps; some hams remove them "because they're annoying." This is exactly the kind of safety bypass that produces obituaries.

## High-current solid-state finals

A 1500 W solid-state PA (Acom 600S, Elecraft KPA1500, SPE Expert) uses 50 V supply rails at 30+ amps. The supply itself isn't lethal in the cardiac sense — 50 V won't stop your heart in normal hand-contact — but the **current** is.

What it can do:

- **Melt a wedding ring in 2 seconds.** A 50 V short-circuit across a gold ring on your finger delivers > 100 A through a fraction-of-an-ohm path; instant smolder. Always remove jewelry when working on high-current circuits.
- **Start fires.** Lead-acid batteries can deliver 1000 A into a short. A dropped tool across the terminals welds the tool to the post in milliseconds, sprays molten metal, and may rupture the cell.
- **Cause arc-flash burns.** A short on a 12 V 100 A circuit produces an arc with > 5000 °C flame temperature briefly. Eye damage and surface burns within inches of the arc are possible.

The safety practices:

- **Fuse every supply rail** at the supply, not at the load. A short downstream blows the fuse before the wires melt.
- **Remove jewelry.** Always.
- **Insulate everything** that doesn't need to be exposed. Use heat-shrink on every connection, even "temporary" ones.
- **Twist battery terminal posts together with a wrench, not by hand.** A momentary contact through your hand from + to – is enough current to weld the wrench to the post.

## RF burns at the final amplifier

At 1500 W into 50 Ω, the RF voltage is 274 V_rms across the output. Touching the output node mid-transmit with bare skin gives you a **deep RF burn** — a tiny, painless-at-first spot that's actually cooked through several millimeters of flesh, taking weeks to heal and risking infection.

The classic amateur injury: leaning over the bench to adjust an inductor while the rig is in TX (because you forgot to drop to RX). Even briefly touching a "hot" tank circuit gives a smoking thumbprint and a memorable lesson.

Rules:

- **Drop to RX before adjusting anything in the RF path.** Use the rig's PTT, not just "I won't push the key."
- **Wear safety glasses when tuning live RF.** The first failure mode of an overdriven tank circuit is the tank capacitor flashing over; molten metal can spatter.
- **Use a wooden chopstick** as a non-conductive tuning tool. Don't put a screwdriver in a live amp.
- **Don't probe with the scope while transmitting** unless you have a documented HV probe and you know what you're doing.

## Thermal — the fire risk

A poorly-heatsinked solid-state PA running at 50 % efficiency at 100 W RF output dissipates 100 W of waste heat. A poorly-heatsinked tube amp at 60 % efficiency at 1500 W dissipates 1000 W. That's a kitchen stove burner inside your equipment.

What can catch fire:

- **Wood-and-rubber tabletops** under hot amps without ventilation gap
- **Carpet** under a vertical-mount amp's exhaust fan
- **Plastic enclosures** holding components that exceed their thermal limits
- **Solder joints** running too hot at full duty cycle (RTTY, FT8)

Practical safety:

- **Heatsink for max continuous power**, not max peak. SSB is 30 % duty cycle; FT8 is 100 %. A heatsink rated for SSB legal-limit may run too hot on FT8 legal-limit.
- **Active cooling for solid-state PAs > 100 W**. Fan-cooled heatsinks add reliability margin.
- **Ventilation around the chassis** — 2 inches minimum on every side of an amplifier.
- **Smoke detector in the shack.** Cheap, mandatory, ignored too often.
- **A 5-lb Class-C fire extinguisher within reach.** Class C is rated for electrical fires. Water on a live circuit is a worse problem.

## A pre-power-on safety checklist

Before energizing any new homebrew project for the first time:

1. **Visual inspection.** No solder bridges, no loose wires, no upside-down components, no missing fuses.
2. **Continuity check.** No shorts between supply rails (e.g., +12 V to ground). All grounds connected. No open signal paths where there shouldn't be.
3. **Bleeder resistors confirmed installed** on every supply filter cap.
4. **Fuses installed** on the supply rail, sized appropriately (~1.5× expected current).
5. **Initial power-up at reduced voltage** if possible — variac for tube amps, current-limited bench supply for solid-state.
6. **Watch for smoke, smell, or audible buzz** in the first 5 seconds. Power off immediately if any of these.
7. **Measure supply voltage** with a meter before connecting load.
8. **Connect dummy load** for any RF stage. Never key into open-circuit or short.
9. **Drop power to lowest setting** if the device has variable drive. Turn up slowly.
10. **Spectrum analyzer or NanoVNA verification** before connecting to antenna.

For a tube amp the first time, also:

- **Shorting stick within arm's reach** before plugging in.
- **Plate-current meter visible** to detect runaway from the first second.
- **No skin contact possible** with anything inside the chassis — close the lid and look through ventilation slots.

> ⚙️ **Advanced —** Modern lithium-iron-phosphate (LiFePO4) batteries are amateur-safe — they don't catch fire if shorted (the way old lithium-cobalt cells do). A 12 V LiFePO4 pack rated for 100 A continuous can still spot-weld a dropped tool to a buss bar, but it won't burn the shack down. This is one reason LiFePO4 has replaced lead-acid in serious portable stations. The trade is cost (~3× lead-acid per amp-hour) and a small risk-mitigation BMS that some hams remove "to save weight" — undoing the safety advantage.

## Common mistakes that hurt people

- **Working on a tube amp with mains connected.** The #1 amateur fatality cause in homebrew.
- **No bleeder resistor.** Cap holds charge for hours. Someone opens the chassis the next day, contacts a node, dies.
- **Wedding ring on, working on 12 V high-current.** Ring melts; finger damaged or amputated.
- **Skipping safety glasses around tuned circuits.** Arc-over sprays molten metal into the eye.
- **Using a metal screwdriver in a live tube amp.** Screwdriver welds to a node; HV arcs to the hand holding it.
- **Trusting "I know what I'm doing."** Every amateur who got hurt was sure they did, the moment before.

## See also

- [§08 — RF Safety](../08-rf-safety/) — RF exposure limits and station-level safety
- [§26-07 — Linear vs Switching Power Supplies](26-07-linear-vs-switching-supplies.md) — supply bleeders, fuses
- [§26-01 — RF Amplifier Topologies](26-01-rf-amplifier-topologies.md) — what HV is used for
- [§27 — Station Engineering](../27-station-engineering/) — mains safety, lightning, bonding
- [§25 — Test Equipment](../25-test-equipment/) — HV probes, dummy loads
