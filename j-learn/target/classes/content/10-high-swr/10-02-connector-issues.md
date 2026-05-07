---
id: 10-02
title: Connector Issues
chapter: 10
section: 02
level: simple
status: draft
---

# Connector Issues

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

If the coax tests fine, the connectors are next. They fail more often than the cable does because they're the weakest link mechanically and because most operators install them poorly.

## Symptoms that point at a connector

- **SWR worse on TX than RX** (high SWR = poor match; the connector also adds loss on RX, but the meter on TX is more obvious).
- **SWR jumps when you wiggle the connector.**
- **Visible corrosion** — green crud, white powder, or dark stains on the metal.
- **Loose-feeling connector** — the shell wobbles, the center pin pulls back when unscrewing.
- **Recently re-terminated** — half of new connector failures happen within 3 months of installation, mostly from soldering errors or poor crimps.

## Quick tests

1. **Wiggle test** at each connector along the chain. SWR jump = bad connector.
2. **Disconnect each connector and inspect.** Look at the center pin, the dielectric inside, the shield contact area. Any corrosion or burning means rebuild the connector.
3. **Continuity test** with a multimeter (radio off, antenna disconnected): center pin to center pin = should read 0 Ω. Shield to shield = 0 Ω. Center to shield = open (infinite). If center-to-shield reads anything but open, the connector or coax is shorted.
4. **NanoVNA on a known-good cable** with the suspect connector — if SWR is high here too, the connector is the problem.

## Common connector failure modes

### Cold solder joint on the center pin

The classic. The center conductor of the coax was soldered to the connector's center pin without the joint properly heating, leaving a poor metallic connection. Works for a few months, then resistance rises with thermal cycling.

**Sign:** Intermittent SWR; sometimes works, sometimes doesn't. Often worse when the connector is cold (winter mornings).

**Fix:** Cut off the connector. Strip fresh coax. Re-terminate. **Use enough heat** — most cold joints come from a too-small soldering iron. A 60–80 W iron or temperature-controlled station at 700°F is appropriate for PL-259 connectors.

### Corroded center pin

Water gets in, oxidizes the brass center pin and the matching socket on the mating connector. Resistance climbs.

**Sign:** Green or white deposits visible inside the connector. Connector mates loosely.

**Fix:** Clean the contact surface with isopropyl alcohol and a small wire brush or fine emery cloth. If the corrosion is deep (pitting, not just surface), replace the connector. Re-seal with coax sealant when re-mating.

### Loose shell

The connector shell unscrews freely instead of being tight against the coax jacket. Either the original installation was loose, or the strain relief failed.

**Sign:** You can rotate the shell against the cable with your fingers.

**Fix:** Cut the connector off and re-install. The shell should be tight enough that you can't rotate it without pliers and significant torque.

### Crushed dielectric

Over-tightening the connector shell (especially on PL-259 with a too-tight crimp) crushes the white plastic dielectric between center pin and shield, shorting them or changing impedance.

**Sign:** Center-to-shield resistance is low (under a few hundred ohms). Connector won't pass DC continuity test.

**Fix:** Replace.

### Wrong pin size

Mating two connectors with mismatched center pin diameters (PL-259 vs SO-239 with a worn-out socket; SMA-male vs SMA-RP-female) creates a loose contact. Common when mixing connector brands or after many mate/unmate cycles.

**Sign:** Connector mates but feels loose; SWR slightly elevated; gets worse over time as the contact wears.

**Fix:** Replace both connectors with a matching pair from one supplier. SMA in particular has many subtle variants — RP-SMA (reverse-polarity) and standard SMA look identical externally but don't mate properly with each other.

### Water in the connector

Coax sealant failed (or was never applied). Rain runs down the cable and pools at the connector.

**Sign:** Water visibly drips from the connector when you remove it. SWR worse after rain.

**Fix:** Disassemble. Dry thoroughly (sun, hairdryer, or 24 hours indoors). If corrosion is mild, clean and re-seal. If corrosion is deep, replace.

## How to terminate a PL-259 properly

The single most-installed connector in ham radio. The standard procedure (for solder-style PL-259, which is what most older cable + connector combos use):

1. **Cut the coax cleanly** with a sharp blade. Square cut.
2. **Strip the jacket** back about 1 1/4 inches. Don't nick the braid.
3. **Comb out the braid** and bend it back over the jacket. Trim to about 3/4 inch.
4. **Strip the dielectric** back about 1 inch from the end of the now-folded braid, exposing the center conductor. Don't nick the center conductor.
5. **Tin the center conductor** lightly with solder.
6. **Slide the PL-259 shell** over the cable.
7. **Push the dielectric through the connector** so the center conductor enters the center pin. Continue until the braid is captured between the connector body and the cable jacket.
8. **Solder the center pin** through the small hole at the connector tip. Use enough heat. Don't blob.
9. **Solder the braid** through the four holes around the side. Each hole gets a quick tack — the goal is electrical contact, not flooding.
10. **Let cool. Test continuity** before mating to anything.

If you can do this in under 10 minutes per connector with no failures, you're a competent installer. Most newcomers need 30 minutes and re-do the first half-dozen attempts. That's normal.

> ⚙️ **Advanced —** PL-259 / SO-239 are constant-impedance connectors only at HF. Above ~150 MHz, the air-spaced gap inside the connector body causes a measurable impedance discontinuity (typically a 1.1:1 SWR added per connector at 432 MHz). For UHF and above, switch to N-type or, even better, BNC (which is constant-impedance up to about 4 GHz). The "UHF connector" name is a misnomer — it was descriptive of the technology at the time it was introduced (1930s); it has never been a good UHF connector by modern standards.

## Connector type quick reference

| Connector | Use for | Caveats |
|-----------|---------|---------|
| PL-259 / SO-239 | HF, lower VHF | Cheapest, most common; limited above 150 MHz |
| BNC | All amateur bands; jumpers; test equipment | Bayonet mount; quick connect/disconnect |
| N | All amateur bands, especially UHF and above | Threaded, weatherproof; more expensive than UHF |
| SMA | Handhelds, GPS, low-power test gear | Small, fragile; max 25 W typically |
| Type-F | TV, satellite | 75 Ω — wrong impedance for ham radio; avoid |

## Tools

- **Soldering iron** (60–80 W or temperature-controlled).
- **Sharp coax stripper** — Greenlee, Klein, or DX Engineering preset strippers. Save your fingers.
- **Pliers, side cutters, third-hand jig** — basic shack equipment.
- **Crimp tool** for crimp-on connectors (PL-259-CR, BNC-crimp). Cheap on Amazon; works fine for amateur use. Pro versions cost $200+ and pay off if you do this for a living.

## See also

- §10-01 — coax issues
- §10-07 — water ingress (also a connector concern)
- §19-04 — connector reference (full table of types)
