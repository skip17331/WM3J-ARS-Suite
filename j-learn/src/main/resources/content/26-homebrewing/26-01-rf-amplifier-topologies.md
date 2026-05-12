---
id: 26-01
title: RF Amplifier Topologies
chapter: 26
section: 01
level: advanced
status: draft
---

# RF Amplifier Topologies

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## Amplifier classes — what the letter means

An amplifier's **class** describes what fraction of the input cycle the active device (tube, BJT, MOSFET) conducts. That single parameter cascades into linearity, efficiency, gain, harmonic content, and bias network — so the class drives almost every design choice downstream.

```
Input signal (one full sine cycle, 360°)

Class A:  conducts all 360°       → cleanest, least efficient
Class AB: conducts 180–360°       → SSB workhorse
Class B:  conducts exactly 180°   → push-pull only, crossover distortion
Class C: conducts <180°          → FM/CW only, high efficiency
Class D: switching (square wave) → audio mostly; RF use rare
Class E: switching, soft-on/off  → modern QRP CW/FT8 favorite
Class F: harmonic-shaped switching → kilowatt PA research / commercial
```

The trade is simple and unavoidable: **more conduction angle = more linearity = less efficiency**. There is no free lunch.

## Class A — linear, lossy, easy

The device is biased to conduct the entire input cycle. No part of the waveform is clipped, so the output is a faithful (amplified) copy of the input. This is what you want for any **modulation with amplitude variation**: SSB, AM, multi-tone digital modes.

- **Theoretical max efficiency:** 50 % (and you'll see 20–35 % in practice)
- **Linearity:** excellent
- **Bias:** device idles at half its peak current (a 100-W class-A PA dissipates ~200 W on key-down with no signal)
- **Use:** small-signal stages, IF strips, driver stages, audiophile-tier QRP

Class A is rare for finals because the heat is brutal. A 100 W output class-A final dissipates 200 W as heat *continuously*, including on receive. Class A is essentially a driver-stage technology in modern ham gear.

## Class AB — the SSB workhorse

Bias is set so the device conducts more than 180° but less than 360°. In a push-pull pair, the two devices overlap slightly near zero-crossing, eliminating Class-B's crossover distortion while approaching its efficiency.

- **Theoretical max efficiency:** 60–78 %
- **Practical efficiency:** 50–60 % at full output, lower at backoff
- **Linearity:** good with proper bias; IMD3 typically –30 dB
- **Bias current:** "trickle" — typically 5–10 % of peak (a 1500 W PA idles at 50–100 mA per device)
- **Use:** **every legal-limit SSB amplifier ever sold to amateurs.** Acom, Alpha, SPE Expert, Ameritron, Yaesu's built-in finals — all class AB.

This is the default for any amp that has to handle SSB or AM cleanly. The bias point is critical and must be re-checked when devices age or temperatures shift.

## Class B — push-pull only

Each device conducts exactly 180°. Two devices, push-pull, share the cycle. Theoretical efficiency 78.5 %, but the unavoidable **crossover distortion** at the zero-crossing makes it unsuitable for SSB without significant linearization.

Mostly seen in old AM broadcast finals and a handful of MOSFET designs. Modern amateur use is essentially zero — class AB is just class B with a little forward bias, and the IMD improvement is enormous.

## Class C — FM and CW only

Bias is below cutoff; the device conducts only on signal peaks (typically 120° or less). The output is a series of current pulses; an output tank circuit (L-C) rings to reconstruct the sine wave.

- **Theoretical max efficiency:** 80–90 %
- **Practical efficiency:** 70–80 %
- **Linearity:** terrible — output amplitude is not proportional to input
- **Use:** **constant-envelope modes only** — FM, CW (with shaping), unmodulated carriers

You cannot use a class-C amp on SSB. Period. The amplifier sees only the peaks of an SSB envelope, throws away the variation, and produces a hash-and-splatter signal that violates every emission standard. Class C is for FM repeaters, CW transmitters with proper keying envelopes, and AM final-stage plate modulation (the modulating audio reconstructs the envelope, not the RF stage).

> ⚙️ **Advanced —** A class-C stage *can* be used on AM if the carrier is plate-modulated (or in modern terms, drain/collector modulated). The RF stage runs flat-out at full carrier; the audio swings the supply voltage up and down. This is how every AM broadcast transmitter ever built worked through the 1970s and how the AM portion of an HF rig like the Heathkit DX-60B works internally.

## Class E — modern QRP favorite

A switching topology where the device is driven hard into saturation, so it acts as a switch rather than a linear amplifier. A tuned output network shapes the voltage waveform so that **voltage is zero when current is high, and current is zero when voltage is high** — minimizing device dissipation.

- **Theoretical max efficiency:** 100 % (88–94 % real-world)
- **Practical for:** CW, FT8, FT4, FM, RTTY — anything constant-envelope
- **Active devices:** RF MOSFETs (IRF510, IRF520, RD16HHF1, BLF188)
- **Use:** QRP CW transmitters, FT8 power amps, low-power constant-envelope rigs

Class E is the modern darling of QRP construction. A simple IRF510 class-E PA can deliver 5–10 W at 90+ % efficiency from a 12 V supply with a single device. The output filter does double duty as both the class-E load network and the harmonic LPF.

The catch: like class C, **you cannot use it on SSB**. The output amplitude is not modulated by the input — it's set by the supply voltage and the load network.

## Class F — high efficiency, high complexity

Class F is class C with a multi-tuned output that presents the **fundamental** as a load while presenting an **open** to odd harmonics and a **short** to even harmonics (or vice versa for inverse class F). This shapes the device voltage and current waveforms toward square/rectangular, minimizing the overlap region where dissipation occurs.

- **Theoretical max efficiency:** ~90 % (3rd-harmonic tuned); up to 100 % asymptotically with more harmonics
- **Practical efficiency:** 80–90 %
- **Use:** commercial cellular base-station PAs, some kilowatt-class amateur SSB amps with envelope-restoration tricks

For pure amateur work, class F is exotic and rare. The output network is fiddly — three or four resonators tuned at fundamental, 2f, 3f, etc. The Q has to be high on each, and component tolerance pulls efficiency away from the theoretical sweet spot. Most amateurs who think they need class F should look at class AB and accept the efficiency hit.

## Side-by-side comparison

| Class | Conduct angle | Max η | Linearity | Modes | Typical amateur use |
|-------|---------------|-------|-----------|-------|---------------------|
| A | 360° | 50 % | Excellent | Any | Drivers, IF strips |
| AB | 200–250° | 60–70 % | Good | Any | **All legal-limit SSB amps** |
| B | 180° | 78 % | OK (push-pull) | Constant env. | Rare |
| C | 90–150° | 80 % | None | FM, CW, AM-plate | FM repeater finals |
| E | switching | 90 %+ | None | Constant env. | **QRP CW/FT8** |
| F | switching + harmonic tune | 90 %+ | None | Constant env. | Research / commercial |

## Why SSB demands class A or AB

An SSB signal has an envelope that varies from zero (no audio) to peak (full audio). The RF amplifier must reproduce that envelope faithfully — every dip and peak. Only A and AB do this. Run an SSB signal through a class-C or class-E amplifier and the envelope is destroyed; what comes out is a "splatter" signal with sidebands extending kHz beyond the intended bandwidth, interfering with every adjacent operator on the band.

The 1500-W legal-limit SSB amp is **always** class AB. Anyone advertising a "1500 W class-E SSB linear" is either confused or selling something that will get you a citation from your national regulator.

## Common amateur designs by class

- **Pacific Antenna 40A QRP** — class A, 1 W, BS170 FET. Pure linearity at low cost.
- **NorCal 40A** — class AB CW, 2N3866 driver, IRF510 final.
- **Elecraft K3 internal PA** — class AB, MRF150 push-pull pair, 100 W out.
- **Ameritron AL-82** — class AB1 grounded-grid, 3-500ZG pair, legal-limit.
- **WB6IGP "Beach 40" class-E** — single IRF510 class E, ~5 W on 40 m.
- **G0UPL QCX-mini** — class E, BS170×3 parallel, 5 W CW on a single band.
- **W1FB / Doug DeMaw class-C FM repeater finals** — generations of 2 m / 70 cm amplifier designs in *Solid State Design for the Radio Amateur*.

> ⚙️ **Advanced —** Modern commercial designs sometimes use **envelope-tracking class AB**, where the supply rail to the final is dynamically adjusted to track the SSB envelope. This recovers some of class AB's efficiency loss without sacrificing linearity. It's complex (requires a fast envelope detector and a switching supply that follows it), but it's how cellular base stations get 50 %+ efficiency on linear modulation. Amateur examples are rare but exist (e.g., some homebrew GaN-FET designs from N4FH and AB4OJ).

## See also

- [§26-02 — Low-Pass Filters](26-02-low-pass-filters.md) — every class-C/E/F output **requires** an LPF
- [§26-11 — RF Safety in Homebrew](26-11-rf-safety-in-homebrew.md) — HV plate supplies in class-AB tube amps
- [§17 — Formulas](../17-formulas/) — bias point, conduction angle, efficiency formulas
- [§25 — Test Equipment](../25-test-equipment/) — spectrum analyzer to verify IMD3
