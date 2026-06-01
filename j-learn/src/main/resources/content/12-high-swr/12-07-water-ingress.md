---
id: 12-07
title: Water Ingress
chapter: 12
section: 07
level: simple
status: published
---

# Water Ingress

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Water inside coax, connectors, baluns, or matching networks is a common cause of high SWR — usually one that gets blamed on something else for months before being correctly identified. This section is the consolidated water-ingress diagnosis and the prevention practices that actually work.

## Symptoms that point at water

- **SWR worse after rain or snow**, sometimes settling back to normal after the system dries.
- **SWR rises gradually over a humid summer**, drops in a dry winter.
- **Cable feels heavier than it should** when you pick it up; you can hear water sloshing.
- **Visible drops in the connector** when you disconnect.
- **Green/white deposits** on connector pins (corrosion product from water + dissimilar metals).
- **A balun enclosure that's wet inside** when you open it.

## How water gets in

| Entry point | Why it fails | Prevention |
|-------------|--------------|------------|
| Coax connector at the antenna | Coax sealant cracks from UV; tape unwound; bare connector exposed | Wrap with rubber-mastic seal then PVC tape; renew every 5 years |
| Coax jacket cracks | UV degradation, rodent damage, mechanical kink | Use UV-resistant coax (LMR-400, etc.); replace when cracked |
| Coax connector at the rig (indoor) | Rare but possible from condensation in a humid garage shack | Mate connectors fully; consider drip loops |
| Balun enclosure | Sealing gasket compressed wrong; mounting bolt holes leaked; cracked plastic | Pot the balun in epoxy; mount with bolt holes facing down; check yearly |
| Antenna feedpoint | Open lugs at the feedpoint; uncovered solder joints | Shrink-tube and weather-seal the feedpoint |
| Inside ferrule of an end-fed UNUN | Sealant on the SO-239 connector failed | Re-seal periodically |

## Why water is bad

1. **Increases dielectric loss in coax.** The plastic dielectric absorbs water; loss can climb 5–10 dB per 100 ft when soaked.
2. **Shorts low-impedance points.** A wet connector can read as a partial short, making SWR shoot up.
3. **Corrodes contacts.** Water + dissimilar metals = galvanic corrosion. Brass center pins go green; stainless connector shells stay shiny but the brass insert pits.
4. **Saturates ferrite cores.** Wet ferrite changes its magnetic properties; baluns no longer transform impedance correctly.
5. **Detunes resonant elements.** Water films on antenna elements change effective length and capacitance; the resonant frequency shifts.

## How to dry out a wet system

Once water is in, you can sometimes dry the system without replacement:

### Coax connectors

1. Disconnect.
2. Inspect for corrosion. If pitting is visible, replace the connector.
3. Spray with isopropyl alcohol (90%+) to displace water.
4. Compressed air or a hair dryer to evaporate.
5. Re-mate, re-seal.

### Coax cable (jacket intact)

There is no practical way to dry the dielectric of foam coax. Solid PE coax (like older RG-213) sometimes recovers if you can dry the connectors and let the cable sit warm and dry for weeks. Foam dielectric never fully recovers — replace.

### Balun

1. Open the enclosure (carefully; don't damage seals you'll re-use).
2. Inspect for corrosion on windings, leads, the core itself.
3. If corrosion is mild — clean with brushed-on isopropyl alcohol, dry thoroughly (sun, low-temp oven 100°F for an hour).
4. Re-seal the enclosure with new gaskets.
5. Reinstall with mounting holes pointing down.

If the core has visible cracks or the windings have visible green corrosion at the connection points, replace the balun.

### Antenna feedpoint

Unsoldered/unsealed lugs at a feedpoint will rust within a year. Re-solder, then weather-proof:

1. Cover the joint with shrink tubing extending 1 inch on each side of the connection.
2. Wrap the shrink tubing area with rubber-mastic tape.
3. Cover the mastic with PVC tape pulled tight.

This three-layer system survives 5+ years outdoors in most climates.

## Prevention is much easier than cure

The single best practice: **seal every outdoor connection with rubber-mastic tape**, not just regular electrical tape.

### How to seal a PL-259 properly

1. Mate the connectors fully (hand-tight + 1/8 turn with a wrench).
2. Wrap the joint with **rubber-mastic tape** (Coax-Seal, 3M 2228, or equivalent). Two full overlapping wraps. Stretch it slightly as you wrap — it self-fuses.
3. Wrap the mastic with **PVC electrical tape** (3M Super 33 or 88) for UV protection. Two layers, overlapping.
4. Final wrap should extend 1 inch beyond the mastic on each side.

This gives 5+ years of weather resistance. Cheap regular electrical tape alone will fail in 12 months.

### Weather-resistant connector choices

For new outdoor installations, consider connectors rated for direct outdoor use:

- **N-type connectors** with their threaded coupling and rubber gasket are inherently more weather-resistant than PL-259/SO-239.
- **Sealed bulkhead connectors** with O-rings — common in commercial telecom gear; available for amateur use.
- **Heat-shrink boots** with sealing compound — slip over the connector after termination, shrink, done.

### Drip loops

Always include a **drip loop** in any coax that approaches a building entry — let the cable form a U that hangs below the entry point so water runs along the cable to the bottom of the U and drips off, instead of running into the building or into a connector.

## Annual inspection checklist

Once a year:

- [ ] Walk the coax run end-to-end. Look for jacket cracks or animal damage.
- [ ] Check every outdoor connector for tape integrity.
- [ ] Open balun enclosures briefly. Look for moisture, corrosion, intact seals. Re-close with fresh gasket if questionable.
- [ ] Inspect antenna feedpoints. Tape intact? Solder joints clean?
- [ ] Sweep antenna with NanoVNA. Compare to last year's sweep — significant change suggests something has degraded.
- [ ] Re-seal anything questionable.

A 30-minute annual inspection catches problems before they become "the radio isn't working tonight" emergencies.

## See also

- §12-01 — coax issues (water often shows up as coax problems)
- §12-02 — connectors (water enters via connectors)
- §12-05 — baluns (water in the balun enclosure)
- §16-04 — coax replacement schedule
