---
id: 27-07
title: Portable Power — LiFePO4
chapter: 27
section: 07
level: mixed
status: draft
---

# Portable Power — LiFePO4

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Portable amateur radio — POTA, SOTA, Field Day, emcomm deployment — is power-budget-constrained in a way the home shack isn't. The choice of battery determines how long the station runs, how much it weighs, and how safe it is to throw in a backpack. Since around 2020, **LiFePO4** (lithium iron phosphate) has displaced lead-acid as the amateur standard for portable. This section covers why, how to use it, and what to watch for.

## The chemistry, briefly

Lithium iron phosphate (LFP, sometimes "LiFePO4") is one of several lithium-ion chemistries. The others amateurs encounter are:

- **LiCoO2 (lithium cobalt oxide)** — laptop and phone batteries. High energy density. *Burns enthusiastically* if mishandled.
- **LiNiMnCoO2 (NMC)** — electric vehicles, power tools. Higher density than LFP, still combustible.
- **LiFePO4 (LFP)** — slightly lower energy density (~110 Wh/kg vs ~160 for NMC) but **dramatically safer**. Doesn't go into thermal runaway under typical fault conditions.

For amateur portable, the LFP safety advantage outweighs the slightly larger and heavier pack. A 12V/20Ah LFP weighs ~6 lb and won't ignite if punctured, overcharged, or short-circuited (within limits). The same energy in lead-acid weighs ~25 lb.

## The voltage profile

LFP discharges at a remarkably **flat voltage** compared to lead-acid:

```
   Voltage (V)
   14 ┤                                         charging cutoff (~14.6 V)
   13 ┤  ●─────────────────────────────●
   12 ┤                                  ●         ← knee
   11 ┤                                    ●
   10 ┤                                      ●     ← deep discharge cutoff
       0%   20%   40%   60%   80%   100%
                  State of charge
```

A 4-cell LFP "12 V" pack sits at:

- **13.6 V** at 100% SOC (just after charging).
- **13.2 V** for most of the discharge cycle (10% to 90% SOC).
- **12.0 V** around 5% SOC — the "knee" where voltage starts to drop rapidly.
- **10 V** at full discharge — BMS cutoff.

A 12 V radio that brown-outs at 11.5 V (typical for HF rigs) runs the entire useful discharge of an LFP pack with no transmit-peak sag, because the pack stays well above 11.5 V until almost empty. Lead-acid, by contrast, drops below 12 V at ~50% SOC and below 11 V at 70%, making the second half of a lead-acid pack unusable for high-current radio operation.

## Real capacities and form factors

Common amateur sizes:

| Capacity | Weight | Energy | Use case | Typical price |
|----------|--------|--------|----------|---------------|
| 12V / 8Ah | 2.4 lb | 96 Wh | QRP, HT base | $80 |
| 12V / 12Ah | 3.5 lb | 144 Wh | SOTA, light POTA | $100 |
| 12V / 20Ah | 5.5 lb | 240 Wh | Long POTA, light Field Day | $150 |
| 12V / 30Ah | 8 lb | 360 Wh | Field Day, emcomm | $230 |
| 12V / 50Ah | 14 lb | 600 Wh | Multi-day off-grid | $400 |
| 12V / 100Ah | 26 lb | 1200 Wh | Off-grid station baseline | $600 |

A 100 W HF rig pulls roughly 1 A receive and 22 A transmit. Assume 20% transmit duty cycle:

```
  Average current = 0.8 × 1 + 0.2 × 22 = 5.2 A
  Run time on 20 Ah = 20 / 5.2 = ~3.8 hours
  Run time on 100 Ah = ~19 hours
```

For QRP (5 W, ~0.3 A RX, ~2 A TX), the same 20 Ah pack runs for **30+ hours**. SOTA operators routinely complete activations on a single 8 Ah pack.

Reputable brands: **Bioenno**, **Dakota Lithium**, **Battle Born**, **Renogy**. Avoid no-name Amazon listings without UL listings or BMS specs.

## BMS — the most important thing in the pack

Every safe LFP pack contains a **Battery Management System (BMS)** — a small circuit board that:

- **Cuts off discharge** below the low-voltage threshold (typically 10 V) to prevent permanent damage.
- **Cuts off charge** above the high-voltage threshold (typically 14.6 V) to prevent overcharge.
- **Balances cells** during charging — keeps the 4 series cells at matched voltages.
- **Cuts off on overcurrent** — protects against short-circuit (typically 100 A trip).
- **Cuts off on overtemperature** — protects against thermal runaway (rare in LFP, but possible).

The BMS is the difference between a safe LFP pack and a bomb. A cheap pack without a real BMS — or with a BMS that's been disabled for "higher current" — is unsafe.

When buying, verify the BMS has:

- Discharge cutoff at 10.0–10.5 V.
- Charge cutoff at 14.4–14.6 V.
- Balance circuit during charge.
- Short-circuit and overcurrent protection.
- Temperature monitoring.

Bioenno, Dakota Lithium, and Battle Born all publish their BMS specs. If the seller can't tell you the BMS specs, don't buy.

## Charging — do not improvise

The single most common LFP failure mode in amateur use: **charging with a lead-acid charger**.

LFP charge profile:

- **Constant current** to ~14.4 V (CC phase).
- **Constant voltage** at 14.4–14.6 V until current tapers to ~5% of capacity (CV phase).
- **Float at zero** or near-zero — LFP doesn't need maintenance float charging like lead-acid does.

Lead-acid chargers, by contrast:

- Charge to ~14.7 V (gassing point).
- **Float at 13.6–13.8 V indefinitely** to compensate for self-discharge.

Putting an LFP pack on a lead-acid charger's float stage exposes it to mild overcharge for hours. **The BMS protects against this short-term**, but repeated cycles degrade the pack: capacity drops 10–20% per year of misuse.

Use a **smart charger designed for LiFePO4**:

- **Bioenno BPC-1505** (15 A, $130) — purpose-built LFP, includes temperature sensor.
- **NOCO Genius GENM2** ($75) — auto-detects chemistry, has LFP mode.
- **Victron Blue Smart IP67 12/15** ($180) — Bluetooth, full charge profile, weatherproof.
- **PowerWerx PSC-30PWii** ($200) — combines PSU and LFP charger.

For solar charging, see [§27-08](27-08-portable-power-solar.md).

## Capacity over time

LFP cycle life is roughly:

- **2000 cycles** to 80% of original capacity (typical spec).
- **3000+ cycles** to 80% if cycled gently (10–90% SOC, not 0–100%).
- **5000+ cycles** to 80% if cycled in the 20–80% SOC window.

At one full cycle per week of regular portable use, that's 40 years to 80% capacity. Realistically, LFP packs outlast the operator's interest in the hobby.

Calendar aging (just sitting on a shelf) is also kind: ~2% per year if stored at 50% SOC and room temperature. Storage at 100% SOC accelerates aging; storage at 0% SOC kills the pack.

**Best storage practice**: charge to 50–80% SOC, store cool and dry, check every 3–6 months and top up if it's dropped below 30%.

## Temperature

LFP is the most temperature-tolerant lithium chemistry:

- **Discharge** works from -20 °C to +60 °C with reduced capacity at extremes.
- **Charge** must not be performed below 0 °C (lithium plating damages cells). Most BMS units cut off charging below 0 °C automatically.
- **Optimal range** is 15–35 °C.

In cold-weather portable (winter SOTA), insulate the pack against direct snow contact, or carry it inside an insulated bag close to the body. Don't try to charge a cold pack — wait until it warms up.

## Mixing batteries — don't

**Never parallel two LFP packs of different age, brand, or SOC.** The packs will try to equalize through whatever connection you've made, and the BMS in the higher pack will see large currents flowing into the lower pack. Best case, the BMS cuts off. Worst case, one BMS misjudges the situation and lets through a high current that ages both packs prematurely.

If you need more capacity, buy a bigger single pack or buy a matched pair from the same vendor with the same date code and a proper parallel-connection harness.

## Safe handling

Despite LFP's safety advantage over NMC and LiCoO2, it's still a lithium battery. Reasonable precautions:

- **Don't short** the terminals. LFP can deliver 100 A+ into a short for seconds before the BMS trips — long enough to weld a wrench to the case.
- **Don't puncture** the case. The BMS can't help if you mechanically damage cells.
- **Don't expose to fire**. LFP doesn't go into thermal runaway easily, but it will burn if you put it in a campfire.
- **Use proper terminal protection** when storing — Powerpole connectors cap themselves; bare ring terminals should be wrapped.

> **Advanced —** Inside a 12 V LFP pack are four cells in series. The BMS monitors each cell individually. As the pack ages, cells drift apart in capacity — a 1% difference at year zero becomes a 5–10% difference at year five. A "balancing" BMS slowly shunts current around the high cell during charging, allowing the low cell to catch up. Without balancing (cheap BMS), one cell hits cutoff before the others, and the pack's usable capacity drops. The Bioenno BPC chargers actively perform a balance charge every few cycles. For high-cycle applications (daily off-grid), this is worth the extra $30.

## When LFP isn't the right choice

LFP is the right answer for almost every amateur portable application. Exceptions:

- **Tiny QRP HTs** where weight matters more than runtime — the rig's internal NiMH or built-in LiCoO2 is fine.
- **Extremely cold** operation (< -20 °C sustained) — lead-acid handles cold slightly better.
- **One-time emergency standby** where the battery sits for 5+ years untouched — sealed lead-acid (AGM) is more tolerant of long storage at 100% SOC.
- **High instantaneous current** beyond LFP's BMS rating — e.g., starting a large engine. Lead-acid is better at sustained 1000+ A surges.

## Cross-references

- [§16-01 — Battery Maintenance](../16-maintenance/16-01-battery-maintenance.md) — long-term care and inspection
- [§27-06 — Power Distribution](27-06-power-distribution.md) — how to wire the pack into the station
- [§27-08 — Solar](27-08-portable-power-solar.md) — recharging LFP from a solar panel
- [§21 — Emcomm](../21-emcomm/21-00-overview.md) — deployment context
