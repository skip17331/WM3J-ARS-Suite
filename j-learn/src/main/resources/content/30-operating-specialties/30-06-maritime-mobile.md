---
id: 30-06
title: Maritime Mobile Operating
chapter: 30
section: 06
level: mixed
status: draft
---

# Maritime Mobile Operating

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What maritime mobile is

Amateur radio operation from a vessel at sea — a yacht, a sailing cruiser, a commercial ship, a houseboat — is called **maritime mobile** and is signed with the **/MM** suffix:

```
WM3J/MM   (US callsign operating maritime mobile, anywhere in international waters)
```

The /MM operator is a small but devoted community. Long-distance sailors crossing oceans rely on amateur radio for weather, position reporting, and connection to family. Yacht clubs run dedicated maritime nets daily. Commercial ships in some countries still carry amateur stations as crew morale and supplemental communications.

This section covers what makes maritime mobile distinct from shore operating — antennas, grounding, power, propagation, and the regulatory framework where amateur radio meets marine radio.

## Amateur radio vs. marine radio — two separate services

A vessel typically carries two completely separate radio services:

| Service | Bands | Purpose | License |
|---------|-------|---------|---------|
| **Marine VHF** | 156–162 MHz | Distress / commercial / boat-to-boat | Vessel station license; operator endorsement |
| **Marine HF SSB** | 2–22 MHz duplex channels | Long-distance distress / weather / commercial | Vessel station license + operator certificate |
| **Amateur radio** | All ham bands | All amateur purposes | Your individual amateur license |

These services share neither frequencies nor regulations. Marine VHF channel 16 (156.800 MHz) is the international distress frequency — you do not transmit there from your amateur rig, and your amateur 2 m rig is not authorized to transmit on marine channels. They use overlapping band ranges but completely different allocations.

A well-equipped cruising yacht has *both* — a marine VHF for hailing other vessels and reaching the Coast Guard, plus the amateur rig for the operator's hobby and informal long-distance communications. The two have different antennas, different installations, and (usually) different operators if more than one crew member is licensed.

## Where you can operate amateur radio /MM

- **In your home country's territorial waters** (within 12 nm of shore): your domestic amateur license applies normally.
- **In international waters** (beyond 12 nm): your home country's license applies; you operate as `<your call>/MM`.
- **In another country's territorial waters**: you must have reciprocal operating privileges (CEPT, IARP, or specific bilateral agreement). Some countries don't permit foreign amateur operation in their waters at all.
- **At a foreign port**: same as territorial waters — reciprocal agreement required.

Outside international waters, the rules of the host country apply. Plan your route and pre-clear paperwork before sailing.

## Antennas on a sailing yacht

A monohull sailing yacht has antenna real estate that a power boat lacks: the mast.

### HF — the insulated backstay

The classic /MM HF antenna is the **insulated backstay**: the rear shroud that holds the mast up, electrically isolated from the boat's rigging with two ceramic insulators (one at the masthead, one above deck level), used as a vertical wire antenna roughly 30–40 ft long.

- Fed through an automatic antenna tuner (Icom AH-4, SGC SG-237, etc.) mounted near the antenna feedpoint.
- Tuner matches the random-length wire across 80 m – 10 m.
- The mast itself is grounded through the standing rigging to the sea.

The backstay is usually a great 80–20 m antenna in saltwater. Performance on higher bands (15 m, 10 m) depends on the wire length and matches the tuner can find.

### HF — alternatives

- **Whip antenna mounted on the stern** — a 23-ft fiberglass whip with autotuner is the standard powerboat HF antenna. Less efficient than a backstay but easy to install.
- **End-fed wire trailed off the stern** — old-school but works; the wire literally floats in the wake while you're underway.
- **Vertical helicaly-loaded HF whip** — like a Hustler 6BTV adapted for marine mount; rare but possible.

### VHF — masthead vertical

A simple 5/8 wave 2 m vertical on the top of the mast (~50 ft above sea level) gives excellent VHF range. Line-of-sight to the horizon at 50 ft is about 8 nm; tropo and ducting over saltwater can extend that significantly. The same antenna serves 70 cm with reduced gain.

A separate, dedicated **marine VHF** antenna (channels 1–88, 156–162 MHz) sits at the masthead alongside the amateur VHF antenna — they're physically close but on different frequencies and don't usefully interact.

## Grounding to saltwater — the /MM advantage

Saltwater is one of the best RF grounds on Earth. Conductivity is ~5 S/m, compared to 0.001–0.01 S/m for typical inland soil. This is a real advantage for vertical-antenna operation:

- A short vertical whip on a metal-hulled boat performs nearly as well as a full-size 1/4-wave because the saltwater ground provides a perfect counterpoise.
- Ground losses that plague shore stations (especially on 160 m and 80 m) are largely absent at sea.
- A simple keel-bolt or through-hull strap is a sufficient ground connection on a fiberglass hull.
- Aluminum or steel hulls use the hull itself as the ground.

The classic result: a 23-ft whip on a steel-hulled vessel at sea regularly works DX that a 90-ft tower with 100 radials struggles to match from a poor-conductivity inland site.

## Power and electrical system

A cruising yacht's electrical system is built around the 12 V DC house battery bank. A typical /MM HF rig draws 20–25 A on transmit (100 W output), which is substantial:

- **Battery bank**: 200–600 Ah at 12 V, charged by alternator (engine), solar, and sometimes wind generator.
- **DC distribution**: 4 AWG cables from battery to rig location, short runs to minimize voltage drop.
- **Linear amplifier**: Most /MM ops run barefoot at 100 W. A 500-1000 W solid-state amp doubles current draw and is uncommon on smaller boats.
- **Bonding system**: All metallic underwater hardware (prop shaft, keel, through-hulls) is electrically bonded to prevent galvanic corrosion. The bonding system is *also* the radio ground but you don't bond the rig to the engine block — separate RF ground, capacitively coupled through the hull and bonding.

## Propagation at sea

A /MM station has propagation advantages and disadvantages:

**Advantages:**
- **Excellent ground conductivity** — vertical antennas radiate efficiently.
- **No QRM** from local industrial / urban noise. The Atlantic mid-ocean noise floor is the receiver's own; you hear DX you couldn't from shore.
- **Open horizon** — no buildings or hills blocking signals.
- **Long surface paths** over saltwater have minimal ground-wave attenuation.

**Disadvantages:**
- **Motion** — the antenna pitches and rolls, modulating signal strength. The on-shore "fading" you sometimes hear from /MM stations is the boat rolling, not the band.
- **Limited antenna height** — even a 60-ft mast doesn't beat a 100-ft tower for low-angle HF.
- **Power constraints** — running 1.5 kW continuously drains the house battery.

## Logging by ITU region

When you cross from one ITU region to another (most commonly between Region 1, 2, 3), you log your contacts noting which region you were in:

```
2025-04-15 14:30Z  ZS5XYZ  20m SSB  5x9 5x9  /MM ITU Region 1, near Cape Town
```

This matters for contests, awards (DXCC entity counts, IOTA — Islands On The Air), and for the receiving station's records (you don't count as a "South Africa contact" because you happen to be in South African waters; your home callsign and /MM status preserve the original DXCC entity).

The DXCC rules treat /MM contacts as not counting toward any DXCC entity — you're not "in" any country when /MM in international waters. Some other awards (like IOTA when within an island's territorial waters) do credit specific positions.

## Maritime mobile nets and operating

Long-distance sailors keep daily skeds with shore-based nets:

| Net | Frequency | Time | Notes |
|-----|-----------|------|-------|
| **Pacific Maritime Mobile Net** | 14.300 MHz | 0300 UTC | Bluewater cruisers Pacific basin |
| **Trans-Atlantic Maritime Mobile Net** | 21.400 MHz | varies | Atlantic crossings |
| **Maritime Mobile Service Net** | 14.300 MHz | 1700 UTC | General /MM and shore traffic |
| **Caribbean Maritime Mobile Net** | 7.241 MHz | 1100 UTC | Caribbean cruisers |

The 14.300 MHz frequency is by long-standing tradition the international "/MM rendezvous" frequency on 20 m. Tuning there on a 20 m opening, you'll find /MM stations almost every day.

## When /MM operating shines

- **Cruising sailors crossing oceans** — daily position reports to family, weather routing, emergency backup to satellite phones.
- **Crew morale on long passages** — connecting with other cruisers and shore operators breaks the isolation of multi-week passages.
- **Vessel emergency comms** beyond marine VHF range — amateur HF can reach shore when marine SSB can't (HF DSC distress is a separate marine service but the same physics).
- **Working DX from rare DXCC entities by sea** — the islands-on-the-air community runs /MM operations to activate uninhabited islands.

## When /MM has limits

- **Operating while in port** in a country without reciprocal privileges — you're effectively QRT until clearing customs.
- **Heavy weather** — neither you nor your antenna want to be on the air during a 40-knot squall. /MM ops shut down for safety in storms.
- **Battery-budget operating** — running a kilowatt linear amp drains the house bank fast; most /MM is barefoot or modest-power.

> ⚙️ **Advanced —** The "ground constants" advantage of saltwater appears in the radiated efficiency equation for a short vertical: η = R_radiation / (R_radiation + R_ground_loss). For a 23-ft whip on 40 m, R_radiation ≈ 5 Ω, R_ground_loss is ~30 Ω inland (poor ground) but only ~2 Ω over saltwater. The result: a saltwater whip is ~70% efficient where the same whip inland is ~14% efficient — a 7 dB advantage. This is also why coastal contest stations (Multi/Multi installations on the Atlantic or Pacific coast) routinely outperform inland competition: not bigger antennas, just better ground. Maritime mobile takes this to the limit.

## See also

- [§30-07 — Aeronautical Mobile](30-07-aeronautical-mobile.md) — the aerial analog
- [§30-08 — SOTA](30-08-sota.md) and [§30-09 — POTA](30-09-pota.md) — terrestrial portable operating
- [§06 — Antennas](../06-antennas/) — vertical antenna design
- [§17 — Formulas](../17-formulas/) — link budget over saltwater
- [§20 — Band Plans](../20-band-plans/) — knowing where /MM activity congregates
- [§21 — Emergency Communications](../21-emcomm/) — distress and welfare traffic
