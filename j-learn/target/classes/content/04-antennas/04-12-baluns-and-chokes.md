---
id: 04-12
title: Baluns and Chokes
chapter: 04
section: 12
level: mixed
status: draft
---

# Baluns and Chokes

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A **balun** (BALanced-to-UNbalanced) connects a balanced antenna (dipole, loop, folded element) to an unbalanced feedline (coax). A **choke** (or *common-mode choke*) breaks unwanted RF currents on the *outside* of a coax shield. The two functions are related but not identical, and which one you need depends on what's going wrong in your station.

If you've ever had hot mics, computer freezes when keying, weird SWR readings that change when you touch things, or audio reports of "your signal sounds raspy," you have a common-mode current problem. **Most of the time, the fix is a properly-sized current balun (= choke balun) at the antenna feedpoint.** This section explains why.

## What "common-mode current" actually means

Coax has three currents:

1. **Inner conductor current**: the signal traveling toward the antenna.
2. **Inside-of-shield current**: equal and opposite to the inner-conductor current, returning. These two together are the *differential-mode* current — what's supposed to be there.
3. **Outside-of-shield current**: the *common-mode* current. This is the bonus current flowing on the outer surface of the coax shield, treating the entire coax run as if it were just another antenna wire.

The first two are how transmission lines work normally. The third is what makes your station weird.

Common-mode current happens when:

- An unbalanced feedline (coax) connects to a balanced load (dipole) without compensation.
- The antenna's two halves are not actually balanced (asymmetrical install, ground proximity differences).
- Stray RF couples back into the feedline shield from the antenna fields.

Once common-mode current exists, the coax shield itself **radiates and receives** as if it were part of the antenna. Symptoms:

- SWR reads differently when you touch the coax or move it.
- TX RFI to nearby electronics, especially the same room as the rig.
- Hot mic, RF burns from the rig's metal case.
- Pattern distortion (the antenna doesn't go where it should).
- Computer crashes / USB cable disconnects when keying.

## Voltage balun vs. current balun

Two fundamentally different transformer designs, both called "baluns" in marketing.

**Voltage balun**: forces equal voltages, opposite phase, on the two output terminals. Does not enforce equal currents. Cheap, simple, but if your antenna is asymmetric (most are), the currents will differ — and common-mode current flows.

**Current balun (= choke balun)**: forces equal and opposite currents on the two output terminals. Common-mode current is suppressed by the choke's high impedance to common-mode signals. **This is what you almost always want.** A current balun is constructed by winding the coax (or the balanced output windings) on a high-permeability ferrite core, like a series of identical coiled sections that present high impedance to outer-shield current but pass differential current freely.

A **1:1 current balun** (no impedance ratio change) is the basic choke balun. It's what you put at the feedpoint of a 50 Ω coax-fed dipole.

A **4:1 current balun** is a current balun that also transforms 200 Ω to 50 Ω, used for off-center-fed and folded-dipole antennas. It's a current balun *with* impedance ratio.

## How much choking impedance is enough?

The standard target: **common-mode impedance of at least 1000–5000 Ω** across the operating bands. A few values worth remembering:

| Choke quality | Common-mode Z | Real-world result |
|---------------|---------------|-------------------|
| < 500 Ω | Bad | Common-mode current still flows; symptoms persist |
| 500–1000 Ω | Marginal | Helps a little; high-power and asymmetrical installs still have problems |
| 1000–3000 Ω | Good | Solves problems for typical installations |
| 3000–10000 Ω | Excellent | Solid for high-power, high-asymmetry installs |
| > 10000 Ω | Boutique | Diminishing returns; some installations need this |

Quality commercial 1:1 current baluns (Balun Designs Model 1115, DX Engineering DXE-RBA-2T1, MFJ-915, Palomar CC-50, Maxwell W2DU bead chokes) hit 2000+ Ω across HF.

## Ferrite materials

The ferrite mix determines the operating range:

| Mix | Frequency range | Use case |
|-----|-----------------|----------|
| Type 31 | 1–50 MHz, peak around 5–20 MHz | Best for HF choking; high permeability, lossy at HF (= good for chokes) |
| Type 43 | 1–250 MHz, peak around 30–50 MHz | The classic "all-band HF" choking material; FT240-43 is the go-to core for transformer-style baluns |
| Type 61 | 50–1000 MHz | VHF/UHF chokes |
| Type 73 | 100 kHz–50 MHz | Lower-frequency emphasis |
| Type 77 | 100 kHz–10 MHz | LF/MF/lower HF |

For most amateur HF work, **Type 31 or Type 43 ferrite** is correct. Wrong-mix ferrite cores are a common reason a homebrew choke "doesn't work" — using a Type 73 core (made for switching-supply EMI suppression at low frequencies) on a 30 m antenna won't choke much.

## Common balun designs (pictures of)

### W2DU bead choke (Maxwell choke)

Type-43 ferrite beads slid over a length of coax (typically 50 beads on RG-303 or RG-400 small-OD coax). Each bead is a small, loose magnetic transformer. The total adds up to several kΩ across HF.

Pros: weatherproof (it's just coax with beads), no leads to break, cheap to homebrew.
Cons: you have to commit to a specific cable length, and slipping 50 beads on takes time.

### Coax-coil choke (folded loop on a former)

Wind 8–12 turns of coax in a loose coil (8" diameter or so), or wrap it around a Type 31 toroid. Cheap and effective for HF.

Pros: easy, just bend the coax.
Cons: less impedance per unit weight than a ferrite-loaded version; performance varies with cable type.

### FT240-43 transformer balun

Two windings on a single big toroid (FT240-43 is a 2.4-inch outer diameter Type-43 ferrite ring). Used for both 1:1 current baluns and impedance-ratio transformers (4:1, 9:1, 49:1).

Pros: handles serious power (200 W to 1500 W depending on winding); compact; easy to enclose.
Cons: needs a weatherproof box; gets hot at high power on lossy installs.

## When to use what

| Antenna | Choking strategy |
|---------|-----------------|
| Dipole, fan dipole, OCFD | 1:1 current balun + (for OCFD) 4:1 ratio at feedpoint |
| Vertical with elevated radials | 1:1 current choke at the base; balun unnecessary |
| Vertical with ground radials | 1:1 current choke is good practice but not strictly needed if grounded properly |
| EFHW | 49:1 unun (matching) + separate 1:1 choke 5 ft down the feedline |
| Loops (full-wave) | 1:1 current balun, or 4:1 if loop impedance is around 200 Ω |
| Magnetic loop | 1:1 choke on coax; the loop's inherent balance helps |
| Yagi (driven element via gamma match) | 1:1 choke |
| Yagi (driven element via folded dipole or hairpin) | 4:1 current balun |

## "Where on the feedline?" — putting the choke in the right place

Choke baluns (1:1 current baluns) belong **at the antenna feedpoint** as the primary location. Some installations also benefit from a *second* choke 0.05–0.1 λ further down the feedline (about 5–10 ft on 80 m), which helps stabilize installations where the feedline acts as part of the radiating structure.

For an EFHW, the choke goes 5 feet down from the unun (not at the unun itself), defining the counterpoise length and breaking common-mode current there.

For a vertical, the choke goes at the base, just before the coax enters the radial system.

## "I can't tell if my balun is working" — the diagnostic test

The classic test for common-mode current: while transmitting, **slide a clamp-on RF current meter (an inexpensive RFCM, like an MFJ-854 or a homemade clamp on a turn of feedline) along the coax shield**. A working choke balun shows near-zero current downstream of the choke. A failed one shows substantial current all the way back to the rig.

Lacking a clamp-on meter: touch the coax shield. If you can feel RF (warmth at high power, or a small fizzing sensation), you have common-mode current.

> ⚙️ **Advanced —** A ferrite choke's common-mode impedance has both a real (loss) and an imaginary (reactive) part. Type 31 and Type 43 are deliberately *lossy* materials at HF — you want the choke to dissipate common-mode energy as heat rather than just reflecting it (a purely reactive choke can produce standing waves of common-mode current down the feedline). The ratio of loss to reactance is the "Q" of the choke; for choking purposes you want a **low Q** (lossy core). This is the opposite of what you want in a tuning inductor, where high Q = low loss. A choke that runs warm at TX is doing its job.

## Common mistakes

- **Voltage balun where you need current balun.** Dipole feeds are the canonical case. The Vbalun lets common-mode current flow; the current balun stops it.
- **Wrong ferrite mix.** A Type 73 or Type 77 core in a 14-MHz application is mostly transparent. Use Type 31 or 43 for HF.
- **Choke too small for the power.** A few-bead W2DU at 1500 W can run hot enough to crack the bead enclosure.
- **No choke on a coax-fed loop or OCFD.** These antennas are *more* asymmetric than a centered dipole; common-mode current is worse.
- **Leaving the unun-only EFHW choke off.** "I have a balun" — yes, but it's a transformer, not a choke. Add a 1:1 current choke 5 ft down.

## See also

- §04-01 — Dipole feeding (where most baluns go)
- §04-04 — EFHW (49:1 unun + separate choke is the standard)
- §04-11 — Impedance transformation (the matching aspect)
- §10-05 — Faulty balun (failure modes)
- §10-06 — Feedline routing (common-mode current symptoms)
- §12 — RFI (common-mode current is a major cause)
