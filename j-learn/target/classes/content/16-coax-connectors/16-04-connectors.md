---
id: 16-04
title: Connectors
chapter: 16
section: 04
level: simple
status: draft
---

# Connectors

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

This section is the per-connector reference: what each common amateur RF connector is, what it's good for, what it isn't good for, and how to install it correctly.

The amateur connector zoo is small — about a dozen types matter — but each has its own quirks, fitments, and failure modes. Picking the right connector for the job and installing it well is the difference between an antenna that works for ten years and one that fails the first wet weekend.

## The connector lineup at a glance

| Connector | Z₀ | Frequency limit | Power | Cost (each) | Use case |
|-----------|------|------------------|-------|-------------|----------|
| PL-259 / SO-239 (UHF) | ~50 Ω (not constant) | ~300 MHz | 1500 W | $1–4 | Amateur HF, classic |
| N | 50 Ω (or 75 Ω) | 11 GHz | 5000 W | $5–15 | VHF/UHF, weather-resistant |
| BNC | 50 Ω (or 75 Ω) | 4 GHz | 500 W | $3–8 | Test gear, scope probes |
| SMA | 50 Ω | 18 GHz | 100 W | $2–8 | HT, VNA, small antennas |
| RP-SMA | 50 Ω | 18 GHz | 100 W | $2–8 | Wi-Fi gear, some HTs |
| SMB | 50 Ω | 4 GHz | 50 W | $3–6 | Snap-on, low-power |
| TNC | 50 Ω | 11 GHz | 1500 W | $5–15 | Threaded BNC, mil/aviation |
| F-type | 75 Ω | 1 GHz | low | $1 | Cable TV, satellite TV |
| 7/16 DIN | 50 Ω | 7.5 GHz | 30 kW | $50+ | Broadcast, cellular tower |
| MCX, MMCX | 50 Ω | 6 GHz | 30 W | $3 | Tiny — RTL-SDR dongles |
| FME | 50 Ω | 4 GHz | 100 W | $5 | Mobile (small, weather-resistant) |
| Mini-UHF | ~50 Ω | 1 GHz | 200 W | $5 | Mobile rigs (smaller than PL-259) |

## PL-259 / SO-239 ("UHF connector")

**The most common amateur RF connector**, despite its serious electrical limitations. Threaded coupling, ~5/8-inch outer diameter. Designed in the 1930s for HF military use; the "UHF" name is a marketing artifact from when 30 MHz was considered UHF.

- **PL-259**: the male plug. Goes on cable.
- **SO-239**: the female bulkhead jack. Goes on equipment.

### Pros

- Cheap (~$1–2 in volume).
- Available everywhere.
- Easy to install with basic soldering tools.
- Mechanically robust; tolerates abuse.
- Widely standardized for amateur HF (every amateur rig has SO-239 jacks for the antenna ports).

### Cons

- **Not a constant-impedance connector.** The geometry inside the connector is not 50 Ω — there's an impedance bump at the connector joint that creates a small reflection. Negligible at HF; measurable at 50 MHz; significant at 144 MHz; bad at 432 MHz.
- **Not weather-resistant.** Without taping, water tracks down the threads into the cable.
- **Center-pin mating relies on solder.** The center pin is soldered to the inner conductor; if your solder joint is bad, the connection is bad.

### When to use

- **HF (1.8–30 MHz)**: ideal. Any amateur HF run.
- **6 m, 2 m**: marginal. Works, but the connector contributes 0.1–0.3 dB of loss and a small reflection. Acceptable for casual use; replace with N for serious VHF stations.
- **70 cm and up**: avoid. Use N.

### Installation

Two methods:

**Method A — solder type (the classic):**
1. Strip the cable: 1.25 in jacket, 1/2 in shield braid.
2. Slide the coupling ring onto the cable.
3. Tin the center conductor.
4. Push the connector body onto the cable; the center conductor protrudes through the center pin.
5. Solder the center pin (use a hot iron, fast).
6. Solder the shield braid through the four solder holes in the connector body (this is where most installers fail — needs serious heat to flow through to the braid).
7. Slide the coupling ring up; tighten when mating.

**Method B — crimp type:**
1. Strip per the crimp-die instructions (varies by manufacturer).
2. Slide the ferrule onto the cable.
3. Slide the connector body onto the cable.
4. Crimp the ferrule with the correct die.

Crimp PL-259s are faster and more consistent but require the matching tool.

### The "reducer" question

PL-259s for RG-213-class cable have a specific bore size. For smaller cables (RG-58, RG-8X), you need a **reducer**: an adapter that fits inside the connector body to take up the space around the smaller cable. Different reducers for different cable diameters.

Buy reducers when buying connectors. Forgetting to buy reducers when you need them is a 30-minute trip-to-the-store moment.

## N connector

**The serious-amateur connector for VHF and above.** Threaded, ~5/8-inch outer diameter (similar to PL-259 size, different thread pitch). Designed by Bell Labs in 1942 (Paul Neill, hence "N"). Constant 50 Ω impedance to 11 GHz.

### Pros

- True 50 Ω constant impedance (or 75 Ω in CATV variant).
- Weather-resistant when properly torqued (rubber gasket inside).
- Excellent VHF/UHF performance.
- Available in male (plug) and female (jack) versions.
- Standardized; every brand interchanges.

### Cons

- More expensive than PL-259 (~$5–15 per connector).
- Harder to install correctly.
- 75 Ω and 50 Ω versions look identical but mate poorly across types — buy the right impedance.

### When to use

- **VHF and above**: default choice. Especially 144 MHz and up.
- **Outdoor / weather-exposed**: even at HF, N's weather seal beats PL-259's.
- **High-power transmissions**: better impedance match means lower SWR-induced reflections.
- **Test equipment**: most modern instruments use N (also SMA for handheld).

### Installation

Three methods:

- **Crimp**: most common modern install. Specific crimp die per cable type. Center pin solders in; ferrule crimps on shield.
- **Solder**: similar to PL-259 — solder center pin, solder shield through holes.
- **Compression**: spring-loaded internal mechanism grips the shield; no crimp tool needed but each connector is single-use.

For amateur use, crimp N connectors are easiest if you have the tool; solder N is best if you don't.

## BNC

**Bayonet quick-disconnect.** Quarter-turn lock; grasp the outer ring, pull and twist. ~3/8-inch outer diameter.

### Pros

- Quick-connect/disconnect (faster than threaded).
- Compact.
- 50 Ω (also 75 Ω); reasonable through 4 GHz.
- Available everywhere.

### Cons

- Not load-bearing — the bayonet lock can come loose under cable tension.
- Not weather-resistant.
- Power handling lower than N.

### When to use

- **Test equipment**: oscilloscope probes, function generators, NanoVNA-equivalent gear.
- **Receiver / SDR antennas**: low power, frequent connect/disconnect cycles.
- **Patch panels**: where you need to swap inputs frequently.

### Installation

Crimp or solder, similar to N. The crimp BNC is fast and reliable; solder BNC requires careful technique.

## SMA

**Small Subminiature A.** Threaded, ~1/4-inch outer diameter. Designed in the 1960s for miniaturized military RF. 50 Ω, useful through 18 GHz. The connector on most modern HTs, all NanoVNAs, RTL-SDR dongles, and small antennas.

### Pros

- Tiny — ideal for small equipment.
- Excellent electrical performance.
- Threaded — secure mating.
- 50 Ω constant impedance.

### Cons

- Easy to over-torque and damage threads.
- Center pin in male SMA bends easily if mated wrong.
- Power handling limited (100 W typical).

### When to use

- **HT antennas**: every modern HT uses SMA-male antenna jacks (with rubber duck attached) or SMA-female (BaoFeng UV-5R, etc., have SMA-female on the radio, SMA-male on the antenna).
- **NanoVNAs and antenna analyzers**: SMA-female on the instrument, SMA-male on cables.
- **GPS and Wi-Fi gear**: the everywhere-connector for small RF.

### RP-SMA: the trap

**Reverse-polarity SMA** — same body as SMA, but the male/female of the *center pin* is swapped. Used to be a regulatory thing for unlicensed Wi-Fi gear (FCC required non-standard antenna connectors for consumer Wi-Fi). Now mostly a quirk of Wi-Fi heritage.

- Standard SMA male: outer thread male, center pin male.
- RP-SMA male: outer thread male, center pin female (= a hole).
- They look almost identical but **don't mate** with standard SMA.

Many BaoFeng-class radios are sold with RP-SMA antennas; many aftermarket antennas are standard SMA. **Always check** before buying replacement antennas.

### Installation

SMA connectors come in soldered or crimped form. Stripping tolerances are tight; use the manufacturer's strip-and-prep guide, not estimation.

## TNC

**Threaded Neill-Concelman** — a threaded version of BNC. Same impedance and frequency range as BNC; threaded coupling instead of bayonet. Used in mil/aviation/some commercial equipment.

Less common in amateur than BNC or N. If you find one, it's a TNC.

## F-type

**75-Ω cable TV connector.** Threaded, hexagonal nut. The connector on every cable-TV outlet in every house in North America.

### When to use

- **TV / satellite TV systems**: standard.
- **Amateur**: rare. Sometimes seen with 75-Ω matching sections (quad antennas) or repurposed RG-6.

Don't use F-type for 50-Ω applications — the impedance mismatch creates SWR issues.

## 7/16 DIN

**Big professional connector** — much bigger than N (about 1-inch OD). Used by broadcast, cellular tower installations, professional commercial sites. Power handling to 30 kW.

Rarely encountered in amateur use except at large contest stations or repeater sites with commercial heritage. Mostly a "good to recognize" type.

## Mini-UHF

A miniaturized PL-259 — about half the diameter, similar electrical characteristics. Used on **mobile transceivers** where panel space is at a premium. Not interchangeable with PL-259.

If your mobile has a connector smaller than the antenna jack on your home rig, it's likely Mini-UHF.

## MCX, MMCX

**Tiny snap-fit connectors** — about the size of a pencil eraser. Used in dongles (RTL-SDR has MCX), tiny SDRs, small Wi-Fi gear.

Hand-installation is challenging; these are usually pre-installed.

## FME

**Compact threaded connector** for mobile / portable use. Smaller than N but with better weather-resistance than PL-259. Found on some commercial mobile antennas.

## Adapter cables and inter-conversion

Common adapter combinations you'll need:

| From | To | Use |
|------|------|-----|
| PL-259 | N | HF rig (PL-259) to VHF/UHF antenna (N) |
| BNC | PL-259 | Test gear (BNC) to amateur antenna jack (PL-259) |
| SMA | BNC | NanoVNA (SMA) to amateur antenna jack (BNC) |
| SMA | PL-259 | NanoVNA (SMA) to amateur antenna jack (PL-259) |
| RP-SMA | SMA | Wi-Fi antenna (RP-SMA) to standard amateur use (SMA) |

Each adapter adds 0.1–0.3 dB of loss and a small impedance bump. Use minimum adapters; use quality adapters when you do.

## Weatherproofing connectors

All outdoor connectors — even N — need additional sealing for long-term reliability. The standard procedure:

1. **Clean the threads** dry; no oil, no flux residue.
2. **Apply dielectric grease** sparingly to mating surfaces (silicone or specifically RF dielectric grease; not standard machine oil).
3. **Mate and torque** to manufacturer spec (N: 1.0 N·m; PL-259: hand-tight).
4. **Wrap with self-fusing rubber tape** (3M 130C, "self-amalgamating" tape) — overlap 50%, all the way past the connector body and a few inches onto the cable. The tape fuses to itself and creates a weatherproof seal.
5. **Wrap with electrical tape** (3M Scotch 33 is the gold standard) over the rubber tape — overlap 50%, in the opposite direction. The outer tape protects the rubber tape from UV.
6. **Optional**: apply Coax-Seal mastic over critical seams. Sticky, semi-permanent, removable.

Rule of thumb: **5 minutes of careful sealing buys 5 years of life**. Don't skip; don't speed-run it.

## Common mistakes

- **No reducer in PL-259 with smaller cable.** Center pin doesn't reach; shield connection is loose; signal is intermittent.
- **Cold solder joint.** Iron not hot enough; shield isn't actually bonded. Symptom: SWR fine when cable is laid one way, terrible when it's flexed.
- **Mating PL-259 to N adapter and not weatherproofing the adapter.** Water enters the adapter, causes corrosion, ruins both connectors.
- **Buying RP-SMA when you wanted SMA.** Read the spec; verify before installing.
- **Over-torquing N or SMA.** Damages threads; either won't mate or won't seal.
- **Hand-tightening connectors meant for torque-spec install.** N connectors specifically need ~1 N·m to fully seat the rubber gasket.

## See also

- §16-00 — Overview
- §16-01 — Coax types (which connectors fit which cables)
- §16-05 — Baluns/chokes (sometimes have specific connector requirements)
- §10-02 — Connector issues (troubleshooting)
- §14-04 — Coax replacement (and connector re-installation)
