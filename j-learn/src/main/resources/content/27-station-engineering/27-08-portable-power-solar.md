---
id: 27-08
title: Portable Power — Solar
chapter: 27
section: 08
level: mixed
status: draft
---

# Portable Power — Solar

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A solar panel and charge controller turn an LFP battery from a finite-runtime device into a sustainable power source. For SOTA, multi-day POTA, Field Day, and any off-grid deployment, solar is the standard. Sized correctly, the panel keeps the battery topped up faster than the radio drains it. Sized incorrectly, it's a heavy decoration.

## Panel watts vs effective amps

A solar panel's nameplate **watts** is its peak output under **Standard Test Conditions** (STC): 1000 W/m² irradiance, 25 °C cell temperature, AM1.5 spectrum. These conditions occur briefly around solar noon on a perfect day — and almost never in real portable operation.

A useful rule of thumb:

```
  Effective peak amps (12 V system) ≈ panel_watts / 17
```

The 17 comes from 14 V nominal charging voltage divided by ~0.82 average derating (panel temperature above 25 °C, off-angle sun, cable losses, dust, etc.).

| Panel rating | Peak amps at noon | Daily Wh in 6 sun-hours |
|--------------|-------------------|--------------------------|
| 25 W | 1.5 A | 100 Wh |
| 50 W | 3 A | 200 Wh |
| 100 W | 6 A | 400 Wh |
| 200 W | 12 A | 800 Wh |
| 400 W | 24 A | 1600 Wh |

**6 sun-hours** is a reasonable summer day in temperate latitudes (US Midwest, Europe). Winter, cloudy weather, or northern latitudes drop this to 2–4 sun-hours; equatorial summer can hit 7–8. The variation matters: a 100 W panel that gives 400 Wh on a June day in Texas might give 80 Wh on a December day in Maine.

For sizing, divide *expected* sun-hours by 2 for a margin against bad weather.

## The daily power budget

Plan around what you actually run, not nameplate ratings.

**Example: light POTA station with QRP HF rig and HT**

```
  HF rig RX:  0.3 A × 14 V = 4.2 W
  HF rig TX:  2 A × 14 V × 20% duty = 5.6 W
  Average HF: ~10 W active, 4 W standby
  HT:         5 W when transmitting (rare)

  Daily active operating: 4 hours
  Standby + listening: 4 hours
  Energy: (10 × 4) + (4 × 4) + (5 × 0.5) = 58 Wh
```

A 25 W panel produces ~100 Wh on a moderate day — comfortably more than the 58 Wh budget, with margin for cloud and recharging the battery overnight loss.

**Example: 100 W HF station, contest-pace operation**

```
  HF rig RX:  1 A × 14 V = 14 W
  HF rig TX:  22 A × 14 V × 30% duty = 92 W TX
  Average:    14 × 0.7 + (22 × 14) × 0.3 = 102 W

  Daily active: 6 hours at this rate
  Energy: 6 × 102 = 612 Wh
```

A 100 W panel gives ~400 Wh on a good day — *not enough*. Need 200 W of panel, or a 100 W panel plus a partly-charged 100 Ah battery as buffer.

The trap: panels are sized for the *average* power draw, not the *peak*. The battery buffers the peaks. A 100 Ah battery can swing the entire 612 Wh draw without flinching; the panel just needs to refill it over the day.

## MPPT vs PWM charge controllers

A solar panel produces a voltage that varies with light and temperature — typically 17–22 V for a "12 V nominal" panel. Connecting it directly to a 13 V battery wastes most of the difference. A charge controller manages this with one of two techniques:

### PWM (Pulse Width Modulation)

The simpler, older approach. The controller acts as a fast on-off switch — when the battery needs charge, switch the panel through; when not, switch off. Effective voltage at the battery: roughly 14 V regardless of panel voltage, but **current at the battery equals current from the panel**. So a 100 W panel at 18 V outputs 5.5 A; PWM delivers 5.5 A at 14 V = 77 W. About 30% loss.

Cheap. ~$15–$30. Adequate for small panels (< 50 W) where absolute efficiency doesn't matter.

### MPPT (Maximum Power Point Tracking)

A DC-DC buck converter that tracks the panel's optimum operating point and converts excess voltage to extra current. Same 100 W panel at 18 V × 5.5 A = 100 W; MPPT delivers 7 A at 14 V = ~98 W. ~30% more usable power than PWM for the same panel.

More expensive. ~$50–$300. Worth it for panels ≥ 50 W, especially in cold weather (when panel voltage rises) and partial shade (when the V/I curve becomes more complex).

**Recommended units:**

- **Victron SmartSolar MPPT 75/15** ($90) — 15 A, Bluetooth, full data logging.
- **Victron SmartSolar MPPT 100/30** ($150) — 30 A, handles up to 400 W panels.
- **EPEver Tracer 4210AN** ($80) — 40 A, no Bluetooth, very popular budget choice.
- **Renogy Rover 40** ($120) — 40 A, with LCD.

The Victron units are the de-facto amateur standard for high-quality portable solar — well-built, Bluetooth-monitored, and rated for years of field use.

## Folding panels for SOTA / POTA

Hard-frame panels (the rigid aluminum-framed glass panels common on RV roofs) are durable but unwieldy for hike-in operation. **Folding/portable panels** are the SOTA standard:

- **PowerFilm FM-15** — flexible, 15 W, weighs 8 oz. ~$120. SOTA standard for years.
- **Renogy 100 W Eclipse folding** — 7.5 lb, briefcase-style. ~$170.
- **Bioenno Solar 100 W folding** — 6 lb. ~$280. High quality.
- **BougeRV CIGS 100 W flexible** — 4 lb, glues to a backpack. ~$200.

For a typical SOTA station (QRP HF + HT, 4-hour activation), **a 25–50 W folding panel** plus an 8–12 Ah LFP pack is the standard kit. Total weight ~8 lb. Net runtime: indefinite.

For a Field Day or weekend POTA expedition, **a 100 W folding panel** plus a 30–50 Ah LFP pack supports 100 W HF at moderate duty cycle for the full weekend.

## Mounting and aiming

For static deployment (Field Day, base camp), the panel is laid on the ground or propped against a tree facing south (northern hemisphere) at an angle roughly equal to your latitude minus 15° in summer, plus 15° in winter.

For hike-and-park (SOTA), the panel is just *put somewhere sunny* — laid flat on a rock, draped across a backpack, hung off a wire fence. Reflectivity from the ground and angle matter less when the panel is only running for a few hours.

A solar tracking mount that follows the sun gains 20–30% more daily energy but is rarely worth the complexity for amateur use.

## Cable losses

The panel-to-controller and controller-to-battery cables add resistance. At 10 A in 10 ft of #14 wire, that's 0.4 V drop — about 3% loss. Not trivial when you're trying to scavenge every watt from a cloudy day.

**Cable sizing for solar:**

- Panel to controller: #10 AWG for runs over 5 ft and currents over 5 A.
- Controller to battery: #10 AWG, often 1–3 ft (mounted near the battery).
- Use **MC4 connectors** (the solar industry standard) on the panel side; Powerpole on the battery side.

> **Advanced —** For higher-power systems (200 W+), wire the panels in series rather than parallel. Two 100 W panels in series produce 36 V at the controller input, halving the current and reducing cable losses by a factor of 4. The MPPT controller converts the higher voltage down to 14 V for battery charging. Series-string mounting also tolerates partial shading better than parallel for some controller designs. The catch: open-circuit voltage on a cold morning can exceed the controller's input rating — check the panel's Voc spec and the controller's max input.

## What can go wrong

Common solar-charging failures:

- **Panel facing the wrong way.** Trivial mistake; massive impact. A panel at 45° off-axis produces ~70% of peak; at 90° off-axis, 0%. Aim once and verify with the controller's reported current.
- **MPPT not tracking properly.** Some cheap controllers fail to find the actual MPP and run the panel at sub-optimal voltage. Verify with a clamp meter; compare to expected per-watt amps.
- **Hot battery.** LFP charging in 40 °C+ ambient is slower and harder on the cells. Shade the battery; let it cool overnight before charging.
- **Cold morning ramp.** The first hour of sun produces almost no usable power. Plan accordingly; don't expect a full-current charge until 9 AM in summer or 10 AM in winter.

## A finished solar kit

For a moderately serious POTA / Field Day station:

| Item | Spec | Cost |
|------|------|------|
| Solar panel | Renogy 100 W folding | $170 |
| Charge controller | Victron MPPT 75/15 + temp sensor | $120 |
| Battery | Bioenno 12V/30Ah LFP | $230 |
| Powerpole distribution | RIGrunner 4005 | $90 |
| MC4-to-Powerpole adapter | (homemade or $20 commercial) | $20 |
| Cables and fuses | | $40 |
| **Total** | | **~$670** |

Net result: 100 W HF transceiver running indefinitely under full sun. Total weight ~12 lb. Total volume: one ammo can plus the folded panel.

## Cross-references

- [§27-07 — Portable Power LiFePO4](27-07-portable-power-lifepo4.md) — the battery side of the system
- [§27-06 — Power Distribution](27-06-power-distribution.md) — the wiring conventions
- [§21 — Emcomm](../21-emcomm/21-00-overview.md) — deployment scenarios
- [§16-01 — Battery Maintenance](../16-maintenance/16-01-battery-maintenance.md) — keeping the battery healthy long-term
