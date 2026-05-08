---
id: 14-01
title: Battery Maintenance
chapter: 14
section: 01
level: simple
status: draft
---

# Battery Maintenance

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Batteries are the part of your station that *will* fail without warning if you don't maintain them. A 7-Ah AGM in the closet for emergency use, fully charged the day you bought it, may be at 30% capacity three years later — and you'll discover that during the power outage when the rig key-downs once and shuts off.

This section covers the three battery chemistries common in amateur use (lead-acid AGM, LiFePO₄, and Li-ion) and the practical care each one needs.

## The chemistries at a glance

| Property | AGM (lead-acid) | LiFePO₄ | Li-ion (NMC/LCO) |
|----------|-----------------|---------|-------------------|
| Nominal voltage / cell | 2.0 V | 3.2 V | 3.6–3.7 V |
| Typical 12 V pack | 6 cells | 4 cells | 3 cells (10.8–11.1 V nominal) |
| Cycle life (80% retained) | 300–500 | 2000–5000 | 500–1500 |
| Calendar life | 5–8 years | 8–15 years | 3–6 years |
| Self-discharge / month | 3–5% | 1–3% | 2–5% |
| Memory effect | None | None | None |
| Tolerates partial-state-of-charge storage | Poorly — sulfation | Excellently | Moderate |
| Can be left on float | Yes (designed for it) | Discouraged at 100% | Discouraged at 100% |
| Cold weather (below 0 °C) | Capacity drops; charging OK | Charging FORBIDDEN below 0 °C; discharge OK to −20 °C | Charging not recommended below 0 °C |
| Hot weather (above 40 °C) | Accelerated aging | Mild aging | Significant aging |
| Energy density (Wh/kg) | 30–50 | 90–120 | 150–250 |

The headline takeaways: **AGM is what most amateur emergency power has been for decades**, increasingly being replaced by **LiFePO₄ for longevity and weight**. **Li-ion (NMC, LCO)** is what's in your phone, your laptop, and most HTs — but is rarely used as a station-level battery because of its narrower temperature tolerance and shorter calendar life.

## AGM (Sealed Lead-Acid) maintenance

The dominant emergency-power chemistry in amateur stations and emcomm. Cheap, recyclable, available everywhere. Maintenance discipline matters because lead-acid batteries fail through a slow process called **sulfation**: lead-sulfate crystals form on plates that sit at low charge, and once formed, they progressively reduce the battery's effective capacity.

### Float charging

The single best thing you can do for an AGM battery is **keep it on a float charger** when not in use. A quality charger holds the battery at 13.6–13.8 V continuously, which prevents sulfation and keeps the cells topped up against self-discharge.

Brands that work well: **NOCO Genius**, **CTEK**, **BatteryMINDer**, **Battery Tender**. All are "smart" — they detect cell condition and adjust voltage accordingly. Avoid old-style transformer chargers without electronic regulation; they overcharge AGM batteries.

A 7-Ah AGM kept on a NOCO float charger for five years will likely still hold 85%+ capacity. The same battery in a closet, untouched, will be at 50% or worse.

### Monthly checks

Even on float, do a quick visual + voltage check monthly:

- **Resting voltage**: disconnect from the float charger; let sit for 30 minutes; measure. A healthy AGM at full charge reads **12.7–12.8 V**. Below 12.4 V indicates significant capacity loss or insufficient float.
- **Visual**: any bulging case, terminal corrosion, or weeping electrolyte means the battery is at end of life — replace.
- **Terminal cleanliness**: corrosion (white/blue powder) gets cleaned with a baking-soda paste and a wire brush; terminals get re-greased with dielectric grease.

### Annual capacity test

Once a year, run a load test. With a known load (a 12-V incandescent bulb of known wattage, or a programmable electronic load), draw the battery from full to ~10.8 V (the minimum safe voltage under load) at the **20-hour rate** (e.g., a 7-Ah battery: 0.35 A draw). Time how long it takes. **Total energy delivered = current × time.**

A new 7-Ah AGM should deliver close to 7 Ah. If it delivers 4 Ah or less, capacity is at 57% or worse — replace before next emergency. Document the number; trend over years tells you the slope.

> ⚙️ **Advanced —** AGM battery internal resistance grows with sulfation. A simple test: with a known DC load (say 5 A), measure the voltage drop from open-circuit to under-load. Divide by current. A new 7-Ah AGM has internal resistance around 30 mΩ; a sulfated one near end-of-life rises to 100–200 mΩ. Hawker Genesis, Concorde, Lifeline, and Optima all publish internal-resistance specs in their datasheets — useful for comparing measurements over time.

### Storage rules

If you must store an AGM battery without float:

- **Charge it fully** before storage.
- **Cool, dry place** — sulfation accelerates with heat.
- **Top-charge every 3 months** at minimum.
- **Avoid concrete floors** — old myth; not actually a problem with modern AGM cases. Don't sweat it.

## LiFePO₄ maintenance

The growing favorite for portable / off-grid amateur power. Long cycle life, lightweight, no ventilation required, no significant self-discharge issue. Maintenance discipline is **different** from AGM, not less.

### The big rule: do not float-charge LiFePO₄ at 100%

LiFePO₄ cells age faster when held at 100% state of charge for extended periods. The optimal long-term storage state is **40-60% charge**. The fix: a smart LiFePO₄ charger that *charges* the battery (typically to 14.4 V for a 4-cell 12 V pack), then *disconnects* — does not hold a continuous float voltage.

Many "lead-acid replacement" LiFePO₄ batteries have **built-in BMS (Battery Management System)** that protects against over/under-charge. Some are designed to live on a 13.6 V float intended for AGM; check your specific battery's spec. Bioenno, Battle Born, and SOK explicitly support AGM-replacement floating; cheaper drop-in batteries often don't.

### The other big rule: never charge below 0 °C

Charging a LiFePO₄ cell below freezing can plate metallic lithium on the anode — irreversible damage and potential thermal-runaway risk. Many BMS designs lock out charging below 0 °C; verify yours does. Discharging below 0 °C is fine (down to about −20 °C); the cell warms up from internal resistance during discharge.

For winter portable operating, **either keep the battery in an insulated bag with a chemical hand-warmer**, or **only charge indoors after warm-up**.

### Storage

- **Discharge to ~50%** before long storage (months).
- **Cool, dry place** — same as AGM.
- **No float needed** for storage; LiFePO₄ self-discharges at ~1-3% per month.
- **Annual top-up to 50%** is plenty.

### Capacity check

Far less critical than for AGM (LiFePO₄ degrades much more slowly), but worth doing annually. Same procedure as AGM: known load, time to discharge cutoff (typically 10.8 V for a 4-cell 12 V pack). Compare to rated capacity.

### BMS visibility

A LiFePO₄ pack's BMS will silently disconnect the battery when something goes wrong (low cell voltage, over-current, over-temperature). Some BMS designs reset automatically; others require manual intervention. **Know which you have.** A pack that's "dead" may simply have a tripped BMS — many are reset by briefly applying a charger.

## Li-ion (NMC, LCO, etc.) maintenance

Most often encountered in HT batteries (BaoFeng, Yaesu, Icom HTs all use Li-ion packs). Less common as station batteries.

### The big rules

- **Don't deep-discharge.** Below ~3.0 V/cell, the chemistry can degrade catastrophically. Most HT BMS designs cut out before this; trust them.
- **Don't overcharge.** Same — most HT chargers are smart enough to terminate charging at 4.20 V/cell.
- **Calendar age matters more than cycle count** for these chemistries. A Li-ion battery 5 years old is at end of life *even if barely used*. The electrolyte degrades over time.
- **Heat ages them.** A spare HT battery on top of an inverter that runs warm will be at half capacity in 2 years.

### HT battery rotation

A practical pattern for amateurs with an HT: **two batteries**, alternated weekly. Each gets used; each gets stored at moderate (50-70%) charge. Both age at similar rates and there's always a backup. After ~3 years, both will need replacement together.

### "Dead" HT battery — try this first

Before throwing out an HT pack that won't charge:

1. **Wiggle the contacts** — corrosion at the spring contacts is common.
2. **Try a different charger.** Some chargers refuse to start charging below 3.0 V/cell ("dead" pack), but a different charger may have less paranoid logic.
3. **Try a "boost" trick**: if you have a benchtop power supply, gently apply 7-8 V (for a 7.4-V pack) at low current (50 mA) for 5 minutes. If the BMS comes back to life, the battery sees its normal charger as functional again. **Only do this if you understand the risks** — a damaged pack can vent or fire.

If none of these revive the pack, replace.

> ⚙️ **Advanced —** Li-ion calendar aging follows Arrhenius kinetics: degradation rate doubles roughly every 8-10 °C above ~25 °C. A pack stored at 40 °C will age twice as fast as one at 25 °C; at 60 °C, eight times as fast. State of charge during storage also matters: a pack stored at 100% ages 2-3× faster than one stored at 50%. The combined heat + high SoC effect explains why a phone battery left on a hot car dashboard plugged in for an afternoon may lose visibly more capacity than one left in a cool drawer for the same calendar time.

## Common mistakes

- **AGM left in a closet for years without float.** Show up to use it during an emergency; battery delivers 30 minutes instead of 8 hours. Use a float charger.
- **Charging a frozen LiFePO₄ pack.** Permanent damage. Warm up first.
- **HT batteries stored in a hot car all summer.** Calendar age accelerates 2–4×. Bring spare batteries inside in summer.
- **Trusting the in-rig "battery indicator."** These read voltage, which is a poor proxy for state-of-charge for any chemistry. Use a watt-hour meter or a calibrated charger to know your true capacity.
- **Buying the cheapest LiFePO₄ "drop-in" replacement.** The BMS quality varies enormously; cheap cells age unevenly, lose balance, and become unreliable. Buy from established brands.
- **Mixing old and new cells in a series pack.** Always replace in matched sets; mixing produces a chain only as good as its weakest cell.

## See also

- §14-00 — Maintenance overview
- §14-03 — Inspections (battery terminal corrosion shows up here)
- §13 — Noise sources (battery chargers can be RFI sources — see SMPS)
- §11-06 — Power supply troubleshooting
- **J-Vault** — Shack inventory (track battery purchase dates)
