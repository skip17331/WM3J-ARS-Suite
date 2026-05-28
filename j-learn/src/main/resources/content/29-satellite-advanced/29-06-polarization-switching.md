---
id: 29-06
title: Polarization Switching
chapter: 29
section: 06
level: advanced
status: draft
---

# Polarization Switching

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

LEO satellites tumble. Their transmitted polarization rotates relative to the ground observer throughout a pass — sometimes slowly (a few degrees per minute, on stable spin-stabilized birds like AO-7), sometimes rapidly (tens of degrees per minute on tumbling CubeSats). A fixed-polarization ground antenna sees the satellite's signal **fade in and out** as the polarization drifts in and out of match.

The fades can be severe — a perfect polarization mismatch (linear horizontal vs linear vertical) is a 20-30 dB loss, more than enough to drop the signal below the receiver's noise floor. On linear birds where you're already working at marginal signal levels, polarization fades are the single biggest cause of "the bird disappeared mid-QSO" frustration.

**Polarization switching** is the engineered solution: a ground antenna whose effective polarization can be electrically switched between several options (typically RHCP, LHCP, and linear horizontal/vertical), so the operator can match whatever polarization the satellite happens to be transmitting at any given moment.

## The crossed Yagi with switchable phasing harness

The dominant amateur implementation uses **two perpendicular Yagis** on a common boom — one polarized horizontally, one vertically — with a **switchable phasing network** at the feedpoint that combines them with various phase relationships.

The phasing options:

- **In-phase (0° offset):** the two Yagis add to produce a single linear polarization at 45° (slant). Not commonly useful but easy to build.
- **90° phase shift (one Yagi delayed by λ/4 relative to the other):** circular polarization. The handedness (RHCP or LHCP) depends on which Yagi is delayed and the direction of the delay.
- **180° phase shift:** linear polarization at the other 45° slant (orthogonal to the in-phase case).
- **270° phase shift (or -90°):** the opposite circular polarization from the 90° case.

Four phase states give four polarizations: linear-slant-1, RHCP, linear-slant-2, LHCP. (Some implementations add linear horizontal and linear vertical via separate switching, for six polarizations total — useful when working a tumbling sat that happens to be in pure horizontal or pure vertical orientation for a few seconds.)

The switching is done with a **relay box at the antenna feedpoint** — typically a sealed box with N connectors, containing coax-relay switches that route the two antenna feeds through different phasing-line lengths. The operator selects polarization from the shack via a control cable that powers the relays.

## Commercial implementations

The **M2 OP series** ("Operational Polarization") is the dominant commercial crossed Yagi for amateur satellite work:

- **M2 OP-2-LXP** (2 m): 11 elements per Yagi (22 total), ~13 dBi circular gain, integrated polarization-switching harness with RHCP/LHCP/H/V positions.
- **M2 OP-4-LXP** (70 cm): 18 elements per Yagi (36 total), ~16 dBi circular gain, same switching options.

A pair of OP-LXP antennas on an az/el rotor — the M2 2MCP14 / 436CP30 cross-Yagi pairs are an older but still common configuration — and a remote polarization controller in the shack is the canonical full-up amateur satellite station.

The InnovAntennas LFA series is a more recent alternative; they use loop-feed Yagi elements that have intrinsically lower side lobes and noise temperature, which matters at marginal signal levels.

## The phasing-line math

The 90° phase delay is implemented as a **λ/4 length of coax** between the two Yagi feedpoints. For 145 MHz: λ = 2.07 m, so λ/4 = 0.52 m of free-space length. But coax has a **velocity factor** less than 1 — typically 0.66 for RG-58 (PE foam), 0.84 for LMR-400 — so the physical length is shorter:

- RG-58 (VF 0.66): physical length for λ/4 at 145 MHz = 0.52 × 0.66 = 34 cm.
- LMR-400 (VF 0.84): 0.52 × 0.84 = 44 cm.

For 437 MHz: λ/4 = 17.2 cm free-space; with LMR-400, physical length 14.4 cm.

Get the length wrong and the polarization is degraded — not horribly, but enough that you lose some of the benefit of having a switchable antenna at all. Most commercial harnesses are factory-trimmed and tested. Homebrew harnesses require careful measurement, ideally with a vector network analyzer to verify the phase shift at the operating frequency.

> **Advanced —** A perfectly-phased crossed Yagi gives 3 dB more gain over a linearly-polarized antenna when receiving the matching circular polarization (because you're capturing both polarization components instead of half of them), and infinite cross-polarization rejection of the opposite circular handedness. In practice, manufacturing tolerances limit the cross-pol rejection to about 20 dB. The "axial ratio" spec — the ratio of major-to-minor axis of the polarization ellipse — should be under 1 dB for a good circular antenna; commercial M2 OP-series antennas typically deliver 0.5-0.8 dB axial ratio across the operating band.

## Auto-rotors with sequenced switching

The next level of automation is a rotor system that **sequences** through polarization positions during the pass, looking for the strongest signal.

Implementation options:

- **Manual switching with a four-position rotary switch** in the shack. Operator hears a fade, switches to the next polarization, listens, repeats. Crude but functional.
- **Computer-controlled switching via parallel port or USB-GPIO.** Software monitors the downlink signal level (via the radio's S-meter readout over CAT) and automatically rotates the polarization to the strongest position. Tracking programs like SatPC32 and MacDoppler support this.
- **Auto-tracking polarization "polarization rotator" hardware:** specialized boxes that continuously vary the phase between the two Yagi feeds, sweeping through all polarizations and locking on the strongest. The N0GSG polarization tracker and similar commercial products do this; they're rare and expensive (~$800) but the right tool for serious linear-bird operators chasing very weak signals.

## Sequenced switching gotchas

When you switch polarization while transmitting, the antenna's match presented to the rig can change for a few milliseconds during the relay switching. This can:

- **Trip the rig's SWR protection** — modern rigs back off power when they see SWR transient. Annoying mid-QSO.
- **Damage the relay contacts** — switching under load (RF current flowing through the relay during the switch event) arcs the contacts and shortens relay life.

The fix: **switch polarization only while not transmitting**. On full-duplex satellite operation (§29-01) this is easy — you switch during the downlink-only period between your TX and the bird's response. Computerized polarization controllers should be configured to inhibit switching during PTT or to use sequenced delays (drop TX, switch, re-enable TX) to avoid the issue.

## Faraday rotation on linear satellites

The atmosphere isn't a passive medium for radio waves — the ionosphere's free electrons, combined with the Earth's magnetic field, cause **Faraday rotation**: a continuous rotation of the polarization plane of any linear-polarized signal passing through.

The rotation rate depends on frequency (inversely proportional to f²), the total electron content along the path, and the orientation of the geomagnetic field. At VHF and lower, Faraday rotation can be **dozens of full rotations** per single satellite pass. At UHF and microwave it's much less.

Practical implications:

- **HF satellites** (the AMSAT-OSCAR-7 mode A operation, 29 MHz downlink): Faraday rotation is severe and continuous. Linear polarized antennas are nearly useless; circular is essentially mandatory.
- **2 m / 70 cm satellites:** Faraday rotation is modest (a few rotations per pass at 2 m, less at 70 cm) and can be tracked by polarization switching. Not severe enough to make linear unworkable, but circular gives a much smoother operating experience.
- **Microwave satellites (23 cm and up):** Faraday rotation is negligible; satellite tumble is the dominant polarization variation.

This is why crossed-Yagi-with-switching is the standard for 2 m / 70 cm satellite stations — it handles both Faraday rotation and physical satellite tumble cleanly.

## The "every-pass" workflow with switching

A typical linear-bird pass on a switchable crossed-Yagi station:

1. **AOS:** start with RHCP (the most common amateur satellite polarization). Listen.
2. **First minute:** signal is fluctuating. Cycle through RHCP, LHCP, H, V, listening to each for a few seconds. Pick the best.
3. **Mid-pass:** signal level changes; cycle again. The polarization that was best at AOS may not be best at zenith.
4. **Late pass:** repeat. The polarization at LOS is often different from the polarization at AOS because the geometry of the geomagnetic field through the line of sight has changed.

With computer-automated switching, this happens transparently — you just keep working the pass and the software tracks. With manual switching, it adds an active task to the operator's job during the pass; an extra knob to manage alongside Doppler and aiming.

## See also

- §29-03 — Arrow handheld (manual physical polarization rotation, the portable equivalent)
- §29-05 — Helical antennas (inherently circular, no switching)
- §29-07 — Mast-mounted preamps (paired with switchable Yagis)
- §06 — Antennas (polarization theory)
