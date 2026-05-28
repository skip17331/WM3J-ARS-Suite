---
id: 16-07
title: Ground System Inspection
chapter: 16
section: 07
level: mixed
status: draft
---

# Ground System Inspection

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The ground system is the silent partner of every amateur station. It does its job — equalizing voltages, providing a path for fault currents, dissipating lightning energy, completing the antenna's image plane — and it does it invisibly until the day it doesn't. A bad ground means a worse antenna, more RFI, more risk of equipment damage during storms, and possibly a hazard to people.

This section is the inspection guide for a station's ground system. The math behind grounding is in §13-05 (the troubleshooting view); this section is the maintenance view.

## Three different grounds

Amateur stations involve three distinct ground concepts:

| Ground type | Purpose | Connection |
|-------------|---------|------------|
| **AC safety ground** | Protect humans from electrical shock | NEC §250 — bonded to the AC service neutral at one point |
| **Lightning ground** | Dissipate strike energy to earth | NFPA 780 / UL 96A — multiple ground rods, low-impedance path |
| **RF ground** | Antenna image plane and TX-line common-mode return | Application-specific; counterpoise, radials, or station ground |

These are **not the same**, but they should be **bonded together** at a single point — the **single-point ground (SPG)** — to prevent voltage differences during faults or strikes.

## The single-point ground

The single-point ground is the conceptual anchor of a station's ground system. All three ground types meet at this one point:

- AC ground (from the service entrance / panel ground rod) connects to it.
- Lightning ground (from the tower ground rod, antenna feedline lightning arrestor) connects to it.
- RF ground (station common ground bus) connects to it.

The reason: during a lightning strike, the energy injects into the ground network at the strike point. If different ground points have different impedances, a voltage difference forms between them — that voltage appears across equipment connected between them, often destructively.

Bonding everything to a single point ensures that even during a strike, the voltage at every ground reference is the same (approximately — current flows so quickly that there's still some difference, but the bonding makes it survivable for properly-rated equipment).

## Inspection cadence

| Inspection | Frequency | Time |
|------------|-----------|------|
| **Visual ground bus** at the station | Quarterly | 5 min |
| **Tower ground rod** at base | Quarterly + after storms | 10 min |
| **Bonding straps** at antenna entry | Annual | 30 min |
| **Ground-rod resistance test** | Annual (where required by NFPA 780) | 30-60 min |
| **Full ground audit** | Every 5 years | 2-3 hours |

## Quarterly station-side inspection

The station's common ground bus is typically a copper bar or strip mounted near the equipment. Inspect:

- **All ground straps connected**: each piece of equipment (radio, amplifier, tuner, computer, power supply) has a green-wire ground that should land on the same bus.
- **Strap material**: copper braid, copper strap (1/2" or wider), or heavy copper wire (8 AWG minimum). NOT regular green-jacketed wire — the impedance is too high at RF.
- **Strap length**: ground straps should be **as short as possible** (under 12" ideally; under 6" for high-frequency work). A long ground strap is an inductor at RF, not a ground.
- **Connections clean**: no green corrosion on copper; no oxidation on brass or copper-alloy lugs.
- **Connections tight**: no movement when pushed.
- **Common bonding**: every ground in the station should land on the same bus, not on separate plates or rods.

## Quarterly tower-side inspection

At the base of every tower:

- **Ground rod visible**: at least 6-12" of rod above grade should be visible (the rest is driven 8 ft into the ground per NEC).
- **Rod material**: copper-clad steel (most common) or solid copper. Galvanized rods deteriorate faster.
- **Connection clamp**: an exothermic weld (Cadweld) or a pre-formed fitting (acorn clamp) is preferred over hose clamps or pipe clamps. Inspect:
  - Weld is intact, no corrosion; or
  - Clamp is tight, threads are clean, no green corrosion at the contact.
- **Strap from tower leg to rod**: heavy copper (typically 4 AWG or 1/2" copper strap); minimum length; no sharp bends.
- **Multiple rods bonded together**: NEC §250 requires multiple rods spaced at least 6 ft apart for high-soil-resistivity sites. Bonding wire between rods should be #6 AWG minimum.

## Annual antenna-entry inspection

Where each coax enters the house (typically through a bulkhead panel), there should be a **lightning arrestor** (also called surge protector or coaxial lightning protector) for each line.

Inspect each arrestor:

- **Visible damage**: no charred housing, no cracks, no swelling.
- **Connectors tight**: input and output PL-259 / N still hand-tight; no looseness.
- **Ground strap**: arrestor body bonded to a copper bus or directly to the ground rod with a heavy strap, NOT a thin wire.
- **Strap length**: as short as possible — direct connection to the bus is ideal.
- **Bypass switch (if present)**: in the proper position for current operating mode (some surge protectors can be bypassed during operation, then re-engaged when leaving).
- **Cycle count (if marked)**: gas-discharge tubes (GDT) and metal-oxide varistors (MOV) have finite lifetimes. Some commercial protectors have a "cycle count" or LED indicator showing remaining capacity. Replace at end of life.

> **Advanced —** A surge arrestor of any quality has a "single-shot" rating (the largest single strike it can absorb without failure) and a "cycles to fail" rating (how many average-magnitude strikes it can take before degrading). Polyphaser, Andrew, and Surplus Sales make professional-grade arrestors with documented specs. The PolyPhaser PT-100 series for commercial communications towers handles 50 kA single-shot and many strikes; their amateur-priced arrestors handle 25-50 kA per cycle. The **clamping voltage** (where the arrestor begins to conduct) should be set well below the equipment's input withstand rating — typically 600 V for HF; 200 V for VHF; lower for sensitive receivers.

## Ground-rod resistance test (annual or 5-year)

The single most important measurement of a ground system is its **earth resistance** — how much the ground rod actually couples to the surrounding earth. This is measured with a **3-pole earth resistance tester** (Megger DET3, AEMC 1620, or equivalent).

### How the test works

The 3-pole method:

1. Drive two auxiliary "test" rods into the ground at known distances from the rod under test.
2. The instrument injects current between the test rod and the auxiliary rods, measures the voltage drop, calculates resistance.
3. The result is expressed in ohms — typically 5-25 ohms for a properly installed home ground rod.

### Acceptable values

| Resistance | Quality |
|------------|---------|
| < 5 ohms | Excellent (often required for commercial broadcast or telco) |
| 5-15 ohms | Good (typical for a properly installed home ground rod) |
| 15-25 ohms | Acceptable (per NEC §250.56 for residential) |
| 25-50 ohms | Marginal (additional rods needed) |
| > 50 ohms | Poor (rod isn't doing its job; soil is too dry or rod is too short) |

NEC §250.56 says: "A single electrode consisting of a rod, pipe or plate that does not have a resistance to earth of 25 ohms or less shall be augmented by one additional electrode."

### Without a 3-pole tester

If you don't have an earth-resistance tester, use:

- **A clamp-on ground tester** (AEMC 6470 or Fluke 1623) — clips around the ground strap and reads earth resistance non-invasively. Uses a different physical principle.
- **A simple multimeter** is **not** adequate for earth-resistance testing. Use a dedicated tester.

### Improving a marginal ground

If your rod tests poorly:

- **Drive another rod** at least 6 ft away. Bond it to the existing rod with #6 AWG copper. Test again — should be lower.
- **Use a longer rod**: standard rods are 8 ft; some installations need 10 ft or 16 ft for low resistance.
- **Use a chemical ground rod** (Erico Cadweld kit; chemical mix in trench; achieves 1-3 ohm resistance). Expensive but effective.
- **Add ground-radial wires**: 4-8 wires laid horizontally from the rod, 3-12" deep, 30-100 ft long. Especially useful for stations with vertical antennas.

## Common ground-system mistakes

- **One ground rod for everything.** A single rod can't handle a major lightning strike. Multiple rods, bonded, with surge protectors and proper distribution.
- **Long, thin ground straps.** A 30-foot run of #14 AWG wire has 60 microhenries of inductance — at 14 MHz that's >5000 ohms of impedance. Use copper strap or braid; keep the run short.
- **Sharp bends in ground strap.** Each bend adds inductance. Smooth, gradual bends only.
- **Different ground potentials.** Multiple ground rods that aren't bonded together. During a strike, current flows between them, voltages develop, equipment fails.
- **Bare conductor in the ground.** NEC requires insulated wire below grade unless the conductor is rated for direct burial (typically copper or copper-clad). Bare aluminum corrodes; bare untinned copper develops oxidation that increases resistance over time.
- **No surge protectors at antenna entry.** Strikes inject energy through the coax. Without a properly-grounded surge protector at the entry point, the strike arrives at your radio.
- **Bonding to a water pipe.** OK as a *supplement* per NEC §250.52(A)(1), but plumbing systems are increasingly plastic; modern code prefers direct rod-bonding.
- **Not checking grounding after concrete or earthwork.** New construction or landscaping can disturb buried connections. Inspect after any significant outdoor work.

## RF ground specifics

For amateur stations, the RF ground considerations differ from AC and lightning:

### Vertical antennas

A vertical antenna without ground radials performs at maybe 50% of theoretical efficiency. With a proper radial system (4-8 wires for HF, 32-64 for serious DX/contest stations), efficiency approaches 95-98%.

Inspect ground radials:

- **Wire intact**: no broken sections from animals, mowing, or work.
- **End connections** at the feedpoint: still bonded to the antenna's ground radial connector.
- **Length appropriate**: typically λ/4 of the operating band; longer is OK (counterpoise role).
- **Burial depth**: 3-12" below ground is typical. Surface-laid radials work but degrade faster.

### Mobile / portable

Mobile installations and portable setups have specific RF-ground concerns:

- **Vehicle body as counterpoise**: works for VHF; poor for HF unless you add radials.
- **Counterpoise wires for portable**: 30-100 ft of wire on the ground beneath an HF antenna.
- **Radials at QRP**: 4 short radials are better than none. 32+ approach a "free space" reference.

## Inspecting bonding

In addition to ground rods, the **bonding** of all metal parts is critical:

| Part | Should be bonded to |
|------|---------------------|
| Tower base | Single-point ground |
| Tower mast | Tower base (flexible bonding strap) |
| Antenna (each, in metallic mounting) | Mast |
| Coax shield (at antenna and at entry) | Local ground |
| Power amplifier chassis | Station ground bus |
| All equipment chassis | Station ground bus |
| AC mains ground | Station ground bus (via AC outlet ground or directly) |

Visual inspection: walk the path from any antenna element to the station ground bus. The path should be **continuous, low-impedance, and short**.

## Documentation

For every ground-system inspection, document:

- **Date and inspector** (you, professional, or an electrician).
- **Earth-resistance reading** at each rod (with date and conditions — soil moisture matters).
- **Visual condition** of each strap, clamp, and connection.
- **Any items found** — photographed and noted.
- **Action items** — what needs replacement or improvement.

This becomes baseline data; the next inspection compares against it.

## Common ground-system maintenance items

- **Re-tighten clamp screws** after a few seasons of weather.
- **Re-cadweld a corroded connection** (small Cadweld kits handle on-site repairs).
- **Replace any galvanized hardware** that shows corrosion, with stainless or copper.
- **Bond newly-added equipment** to the station ground bus.
- **Test resistance annually** if NFPA 780-rated; every 3-5 years for amateur use.
- **Inspect after lightning events** even if no equipment damage was visible — a strike can damage clamps invisibly.

## See also

- §16-00 — Maintenance overview
- §16-05 — Tower & mast inspection
- §16-09 — Cable entry & water intrusion
- §13-05 — Grounding (troubleshooting view)
- §16-08 — Coax inspection (for the connector-side of the ground system)
- §08 — RF safety (related considerations for AC and equipment grounding)
