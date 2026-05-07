---
id: 07-01
title: FCC Rules
chapter: 07
section: 01
level: simple
status: draft
---

# FCC Rules

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

This section covers the actual federal regulations governing RF exposure for amateur stations. The rules are spread across two parts of 47 CFR (the FCC's regulations): **Part 1** sets the underlying exposure rules for *all* FCC licensees, and **Part 97** (the amateur radio rules) tells amateur licensees how to comply.

The headline: **as of 2 May 2021, every amateur licensee is responsible for RF exposure evaluation of every station, on every band, regardless of power level**. The pre-2021 categorical exemptions for low-power amateur stations are gone.

## The core rules in plain English

### 47 CFR 1.1307(b) — when an exposure evaluation is required

The general rule: **any FCC-licensed transmitter is subject to the FCC's RF exposure limits** (the "Maximum Permissible Exposure" or MPE limits, defined elsewhere in Part 1). If your station can produce fields exceeding the MPE limits in any place where humans might be, you must evaluate.

For amateur radio, the FCC's position (per the 2019 final rule) is that **all amateur stations require evaluation** because amateur stations operate at power levels and antenna distances where MPE exceedance is possible. The pre-2021 amateur-specific categorical exemption (which let most low-power hams skip the formal evaluation) was eliminated.

### 47 CFR 1.1310 — the MPE limits themselves

These are the actual numbers — the maximum power density (in mW/cm²) or field strength (in V/m or A/m) at any place humans might be exposed. The numbers are frequency-dependent and split into "occupational/controlled" and "general public/uncontrolled." See §07-02 for the detailed table.

### 47 CFR 97.13(c) — the amateur-specific evaluation requirement

Within the amateur rules, this section reiterates that **before placing an amateur station in service, the licensee shall determine whether routine RF radiation evaluation is required** and, if so, conduct it. After the 2021 rule change, the answer to "is evaluation required" is *always yes* for any meaningful station.

## What counts as "evaluation"

The FCC has been pragmatic. The evaluation can be:

1. **Worst-case calculation.** Using the maximum power your station can transmit, the gain of the antenna, and the distance to occupied spaces, calculate the predicted exposure level. Compare to the MPE table. If you're under, you're compliant — no measurements needed.
2. **Time-averaged calculation.** Account for actual duty cycle and time fraction (the FCC's MPE limits are time-averaged over 6 minutes for controlled environments and 30 minutes for uncontrolled). A station that transmits 1 minute out of 6 contributes only 1/6 of its peak field strength to the time-averaged result. SSB voice's intermittent waveform contributes ~30–50% of the time-averaged field of CW or digital. (See §07-04.)
3. **Field strength measurement** — using a calibrated RF field-strength meter. This is rarely needed for amateur compliance work; it's a backup if calculations are ambiguous.
4. **Modeling** — for unusual antennas, NEC modeling can produce near-field intensity maps that you compare to MPE. The OET-65 supplemental document covers this in detail.

### What records to keep

The FCC does not require submission of evaluation results, but you must **maintain records that demonstrate compliance**. Common-sense documentation:

- A short written calculation showing power, antenna gain, distance, and the resulting field strength compared to MPE limits.
- For each band/antenna combination you operate.
- Updated whenever you change power, antenna, or installation geometry.
- Stored in your station log or a separate "RF safety binder."

The **ARRL RF Exposure Calculator** (free, online) and equivalent tools (HamWaves' calculator, the FCC's own bulletin OET-65 worksheets) produce printable reports that satisfy this documentation requirement. The J-Hub §10 calculator does the same.

## Special cases the rules call out

### 47 CFR 1.1307(b)(3) — categorical exemption (the new one)

After 2021, **a single power threshold below which evaluation may be skipped** still exists, but it's much lower than the old amateur-specific threshold. The new general exemption is based on a formula involving frequency and ERP. For typical amateur HF and VHF, the threshold ranges from a few watts to maybe 50 W ERP, depending on band.

For most practical amateur operation, **you'll be above the categorical exemption threshold** and required to evaluate. The arithmetic is so easy that you should just evaluate every band/antenna anyway.

### 47 CFR 97.13(b) — antenna structure rules

Separate from RF exposure, this rule deals with antenna structure registration with the FAA for tall structures (above ~200 ft) and for structures near airports. Most amateur installs aren't subject to it; if your tower is over 200 ft above ground level or you live near an airport, see the rule directly.

### 47 CFR 97.15 — limit on transmitter output and antenna height

This is a state/local-preemption rule (PRB-1 codified) more than a safety rule. It establishes federal preemption of state and local rules that unreasonably restrict amateur antennas. Not directly RF-safety related but often comes up in the same conversations.

## What about the operator vs. the bystander?

The FCC distinguishes between **controlled environments** (where the operator and trained personnel are aware of RF exposure and can move out of the field) and **uncontrolled environments** (everyone else: family, neighbors, anyone who can't be expected to know). The MPE limits for uncontrolled are roughly 5× lower than for controlled — uncontrolled is the harder threshold to meet.

For your own operating position, you can typically use the controlled limit (you're the operator, you know).

For your spouse's office in the next room, your kids' play area in the yard, the next-door neighbor's deck, **uncontrolled limits apply**. This is usually what determines whether your installation passes the MPE evaluation. See §07-03.

## The "shall not exceed" wording

47 CFR 1.1310 is written in **"shall not exceed"** language, with explicit time-averaging windows. That means:

- **Brief excursions above the time-averaged limit are not violations** if the time-averaged value is under the limit.
- **The time-averaging window is the operative measure**, not instantaneous peak field strength.
- **CW and digital modes** (full duty cycle in TX time) are evaluated as if always-on during the TX period.
- **SSB voice** can use a ~50% effective duty-cycle factor (the modulation envelope) plus the actual TX-time fraction (typically 50% or less in QSO).

The math: **time-averaged power = peak power × (duty cycle of mode) × (fraction of time in TX)**.

## Penalties

The FCC has *very rarely* taken enforcement action against amateurs for RF exposure violations — the rule's purpose is procedural (require evaluation and documentation), not punitive. The most likely scenarios:

- **Documented neighbor complaint** + investigation finds you cannot demonstrate compliance: warning letter, sometimes fines.
- **Demonstrated harm** to a person from station emissions (extremely rare): possible criminal/civil action plus license consequences.
- **Deliberate non-compliance** (knowingly operating where MPE is exceeded for bystanders): notice of violation, license revocation possible.

The actual enforcement risk is low. The actual goal of the rules is to make hams **think about RF safety** and design responsibly.

> ⚙️ **Advanced —** The FCC's MPE limits are derived from IEEE C95.1 and the older ANSI C95.1-1992 standards. The threshold for biological effect under IEEE's "expert panel" framework is set at a 4 W/kg whole-body Specific Absorption Rate (SAR) — the level at which significant tissue heating is documented. The MPE field-strength values are then derived to keep SAR below 0.4 W/kg for occupational and 0.08 W/kg for general public, applying safety factors of 10 and 50 respectively. The SAR-to-field-strength conversion is frequency-dependent and assumes a "standard adult body." This is why MPE limits drop in the 30–300 MHz range — that's where the human body is most resonant and absorbs most efficiently. Below 30 MHz and above 300 MHz, MPE limits relax somewhat.

## Common mistakes

- **Skipping evaluation at "low power."** The post-2021 rules apply to all stations. Even at 5 W, evaluate.
- **Evaluating only the operating position.** The harder check is bystander areas with uncontrolled MPE limits.
- **Ignoring far-field vs. near-field.** Standard MPE calculators assume far-field conditions, which require distances of at least 2 D²/λ from the antenna (D = largest antenna dimension). At low frequencies, you may be in the near field at most household distances; near-field exposure must be evaluated differently. See §07-02 advanced section.
- **Assuming "horizontal antenna pointed up" means safe at ground level.** Pattern lobes change with height; an antenna at 1 λ up has a near-field that still extends well below.
- **Not updating evaluation after changes.** New amplifier, new antenna, new feedline, new house addition → re-evaluate.

## See also

- §07-02 — MPE limits (the actual numbers)
- §07-03 — Controlled vs uncontrolled (the threshold question)
- §07-04 — Duty cycle (the time-averaging factor)
- §07-05 — ERP (the input to all MPE calculations)
- §07-06 — Safe antenna placement
- §10 — J-Hub RF Exposure Calculator
