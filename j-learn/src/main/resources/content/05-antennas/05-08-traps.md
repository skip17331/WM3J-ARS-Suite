---
id: 05-08
title: Traps
chapter: 05
section: 08
level: advanced
status: draft
---

# Traps

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A **trap** is a parallel LC circuit installed in the body of an antenna, tuned to a specific frequency. At its tuned frequency the trap presents a high impedance — effectively *open-circuiting* the antenna at that point. Below the trap's tuned frequency it acts mostly as an inductor; above, mostly as a capacitor. This trick lets a single physical antenna behave as if it had **different effective lengths on different bands**, so one piece of wire (or one set of aluminum elements) covers multiple bands.

Trap antennas are a fixture of mid-tier commercial HF antennas: trap dipoles, trap verticals, the Hustler 4/5/6BTV, the Cushcraft R series, the Hy-Gain TH series triband Yagis. Understanding traps tells you what they cost in performance and where they fail.

## What a trap does, intuitively

Imagine a 40 m dipole. It's 65 ft long total — a half-wavelength at 7 MHz. Now suppose you install a **trap tuned to 14 MHz** at a point 16.5 ft from each end (so each leg becomes 16.5 ft of wire, a trap, and another 16 ft of wire).

- **On 20 m (14 MHz)**: each trap presents a high impedance and effectively disconnects the outer 16 ft of wire. Each leg is now 16.5 ft — about a quarter-wavelength at 14 MHz. The antenna behaves as a 20 m dipole.
- **On 40 m (7 MHz)**: each trap is well below resonance, so it acts mostly as a small inductor. The full 65 ft of wire still works, with the trap inductors slightly shortening the *electrical* length (so a 40+20 m trap dipole is physically a few feet shorter than a plain 40 m dipole).

You get two bands from one wire and two supports.

## What a trap costs

Three real-world penalties for using traps:

1. **Loss in the trap.** Real coils have finite Q (typically 80–150 for a practical antenna trap). A pair of traps in a dipole adds **0.3 to 1 dB of loss** per band on the band where the trap is *near* resonance. For a multi-trap antenna (3- or 4-band trap dipole, or a trap vertical with 4–6 traps), the cumulative loss can reach 1.5–3 dB on the lowest band.
2. **Bandwidth narrows.** A trap's reactance varies sharply near resonance, so the antenna's SWR is sharper on the higher-band resonances. A trap dipole tuned for 7.150 MHz might cover all of 40 m; the same dipole's 20 m response might only cover 14.000–14.250 MHz.
3. **Power handling.** Each trap dissipates a fraction of the RF power as heat (the loss number above tells you how much). At 1500 W, **a 3% loss trap dissipates 45 W as heat** — enough to soften a poorly-built plastic-encapsulated trap over a long contest weekend. Quality traps use ceramic, fiberglass, or weatherproof potted construction with adequate thermal mass.

## Trap construction

Three common types:

| Type | Description | Q | Power | Notes |
|------|-------------|---|-------|-------|
| Coaxial trap (W3DZZ-style) | Coil wound from a length of coax, capacitor formed by the coax's distributed capacitance | 80–120 | Modest (200–500 W) | Easy to homebrew; the classic ARRL design |
| Discrete LC trap | Air-wound inductor + dedicated transmitting-grade capacitor (mica or ceramic), in a weatherproof enclosure | 100–200 | High (1500 W+) | Used in high-end commercial antennas |
| "Trap" formed by a parallel-tuned stub | Quarter-wave stub of coax shorted at the far end, presents a high impedance on its tuned frequency | 100+ | High | Mostly used at VHF and UHF |

For homebrew use, the coaxial trap is simple and rugged. For 1500 W operation, look at the discrete LC types (manufactured) or build with care.

## Multi-band trap antenna designs

### Trap dipole

The simplest and most common: a horizontal wire with one trap per "extra" band, on each leg. A 40/20 m dipole has 2 traps (one each leg, tuned to 14 MHz). A 40/20/15 m tribander has 4 traps; the inner trap is tuned to 21 MHz and the outer to 14 MHz.

Trap dipoles are typical attic and stealth installs, where one wire run gives 2–3 bands.

### Trap vertical

A vertical conductor with multiple traps along its length. Hustler 4BTV: 40/20/15/10 m on a single 20-ft mast, one trap per band-transition. The Cushcraft R5/R7/R9: 5–9 bands on a single 24-ft mast, with multiple traps and matching networks at the top.

These are popular because they're physically compact (one mast, no horizontal wire), but they pay the trap-loss penalty *more* than horizontal trap antennas because each band has to "see through" multiple traps to reach the radiating section above.

### Trap Yagi

A horizontally-polarized Yagi (rotatable beam) with traps in the elements. The classic example is the Mosley TA-33, Hy-Gain TH3/TH7, Cushcraft A3S — 3 to 7 elements covering 20/15/10 m. A non-trap tribander Yagi would be impossibly large and complicated; the trap version fits on a tower and rotates with one rotor.

**Trap Yagis are the most common tribander tower antennas in the hobby**, despite their loss budget, because the alternative (a separate Yagi per band, three times the tower wind load) is impractical for most operators.

## How trap loss varies with frequency

A trap is most lossy near its tuned frequency (where its current is high) and progressively less lossy below it (where it acts as a small inductor). On the band *above* the trap's resonance, the antenna still has the trap in circuit, but the reactance is now capacitive — the trap's loss is small there too.

Practically:

- On the band the trap is tuned for: 0.3–0.5 dB loss per trap.
- On the next band lower (where the trap is below resonance and acting as an inductor): 0.1–0.3 dB.
- Two bands or more below: typically <0.1 dB.

For a 4-band trap vertical, the **40 m operation passes through three traps each acting as inductors** — small loss per trap, but 3× the count adds up.

> ⚙️ **Advanced —** Trap loss can be derived from the trap's loaded Q at the operating frequency. At resonance, P_loss / P_total ≈ 1/Q_loaded; for Q_L = 100, that's 1% (or 0.04 dB) — but this is per-trap, and most dipoles use traps in pairs. Off-resonance, the loss falls roughly as the square of the frequency offset (Δf/f₀)². The trap is also re-tuned by adjacent loading: any inductive loading from the wire connected through the trap shifts the effective resonance. Real-world measurements (W8JI's experiments published on his website) consistently show 0.3–1 dB total loss per band for well-built trap dipoles.

## When traps are right

- You want multi-band coverage on one antenna and don't mind a few tenths of a dB.
- Tower-mounted Yagi: there's no practical alternative for a tribander on one mast.
- Stealth wire installs where a fan dipole's parallel wires would be too visible.
- Portable / emergency: one trap dipole + two ropes covers 3 bands.

## When to skip traps

- Maximum-efficiency installs. A fan dipole or separate antennas per band always beat a trap antenna by 0.5–1 dB per band.
- Very low frequencies (160 m, 80 m) where trap loss compounds with already-marginal antenna efficiency.
- High-power digital operation. 100% duty-cycle modes drive trap heating.

## Common mistakes

- **Buying the cheapest trap antenna and assuming "loss is minimal."** Loss is not minimal in poorly-built traps; the cheap ones can show 2 dB per band.
- **Operating a trap antenna outside its design bands.** A 40/20/15 m trap dipole used on 17 m via a tuner will work, but the traps may dissipate significant power as heat on a band they were not designed for. Performance is unpredictable.
- **Ignoring weather-sealing.** Water in a trap retunes it (capacitance changes) and can short the LC circuit. Trap antennas in humid climates need annual inspection.
- **Trap order matters.** On a multi-band trap antenna, the highest-frequency trap goes nearest the feedpoint, with progressively lower-frequency traps further out. Reversing the order makes the antenna not work.

## See also

- §05-01 — Dipoles (the trap dipole's reference)
- §05-03 — Verticals (trap verticals are common)
- §05-12 — Baluns (trap antennas still need them)
- §13-05 — Faulty balun (and faulty traps; the symptoms can look similar)
- §22 — Coax & Connectors (W3DZZ-style traps are made of coax)
