---
id: 27-06
title: Power Distribution
chapter: 27
section: 06
level: mixed
status: published
---

# Power Distribution

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Every amateur station needs two power systems: 12 V DC for the radios, and 120 V (or 240 V) AC for the computer, monitor, lighting, and high-power amplifier. How those two distribute determines whether the station is convenient and quiet, or a maze of barrel-plug adapters that hums on receive.

## The 12 V DC bus

The amateur convention is **single-voltage 13.8 V** ("12 V nominal") for every radio, plus accessories like a 12 V LED desk lamp, an antenna switch, and a CW keyer. Centralizing distribution is much cleaner than running individual leads from the PSU to each device.

### Powerpole connectors

The **Anderson Powerpole** is the de-facto amateur standard for 12 V interconnect. Adopted by ARES/RACES, the Red Cross, and every emcomm group, it has these advantages:

- **Polarity-keyed** — the red and black housings snap together so you can only mate red-to-red, black-to-black. No accidental reverse polarity.
- **Genderless** — every connector is both male and female. Two ends of a cable have the same connector; either can plug into the other.
- **Color-coded** — red for positive, black for negative. Visible at a glance.
- **Rated 15–45 A** depending on contact size (15 A, 30 A, and 45 A contacts are common).
- **Inexpensive** — $0.50 per pair in bulk.

The 30 A "PP30" is the universal amateur choice. Use it for radios, accessories, the PSU output — everything.

To crimp Powerpoles, use the **TRIcrimp** ($45) or **PWRcrimp** ($50) tools — both produce reliable, gas-tight crimps that handle full rated current. Pliers and a soldering iron can substitute but produce inferior joints prone to overheating at high current.

### Fused branches

Every device on the 12 V bus should have its **own fuse**, sized for that device's current draw. Reasons:

- A short in one device's cable doesn't take down the whole station.
- A short doesn't dump the PSU's full 30+ A into the failing cable, starting a fire.
- A bad device can be isolated by pulling its fuse.

Typical sizing:

| Device | Current | Fuse |
|--------|---------|------|
| HF radio (100 W TX) | 22 A peak | 25 A |
| VHF/UHF radio (50 W) | 10 A | 15 A |
| Handheld charger | 2 A | 5 A |
| Antenna switch | 1 A | 3 A |
| Keyer | 0.5 A | 1 A |
| LED light | 0.5 A | 1 A |

Fuses sit in the distribution panel, not at the radio.

### RIGrunner-style distribution

The **West Mountain Radio RIGrunner** is the standard amateur 12 V distribution panel. Variants:

- **RIGrunner 4005** — 4 fused Powerpole outlets, $90. Good for a small station.
- **RIGrunner 4008** — 8 outlets, $130. Most popular.
- **RIGrunner 4012** — 12 outlets, $170. Full station.
- **RIGrunner 4007U** — 7 outlets + 2 USB charge ports, $130.

Each outlet has an integral fuse (ATC blade style) and an LED indicator showing fuse health. The PSU connects via 2-pin Powerpole; each device gets its own outlet.

Generic equivalents: **MFJ-1129**, **Bioenno BPP-PD-XX**, **Pyramid PDP-30**. All similar in concept. ~$50–$150.

### Wire gauge

| Distance | Current | Gauge |
|----------|---------|-------|
| < 3 ft, < 25 A | | #10 AWG |
| < 3 ft, < 40 A | | #8 AWG |
| 3–6 ft, < 25 A | | #8 AWG |
| 6–10 ft, < 25 A | | #6 AWG |
| Any distance, > 40 A | | #6 AWG minimum, #4 if amp-class |

The rule: **size for voltage drop, not just current**. A #14 wire can carry 15 A safely, but at 6 ft of run length, the drop is 0.7 V — that's 5% of the supply, which causes radios to brown out during transmit peaks. #10 or #8 for amateur radio distribution, even for short runs.

## AC power conditioning

The AC mains delivers nominal 120 V (or 240 V) RMS, but in real homes it includes:

- **Sags and surges** (±10% from heavy appliances cycling, lightning, utility events).
- **High-frequency hash** from neighbor's appliances (vacuum cleaners, dimmer switches, EV chargers).
- **DC offset** from half-wave devices on the same circuit.

A small amount of AC conditioning improves both equipment lifetime and receive performance.

### The Tripp-Lite Isobar tier

The **Tripp-Lite Isobar series** is the amateur sweet spot:

- **Isobar 2-6** — 6 outlets, 3330 J surge rating, ~$50. Good for a desktop station.
- **Isobar 6 Ultra** — 6 outlets, 3840 J, EMI/RFI filtering. ~$80. The recommended baseline.
- **Isobar 8 Ultra** — 8 outlets, 3840 J, EMI/RFI filtering. ~$100.
- **Isobar 12 Ultra** — 12 outlets, rack-mountable. ~$200.

These are *not* uninterruptible power supplies (UPS). They provide:

- Whole-strip surge suppression (MOV-based).
- EMI/RFI filtering across all outlets.
- Isolated outlet banks (one bank for radios, another for switching PSUs, etc., reducing inter-outlet coupling).

For a station with a high-end SDR or sensitive receiver, the EMI filtering measurably lowers the receiver noise floor — 3–6 dB on a typical urban QTH.

### Beyond Isobar

For higher-end stations:

- **Online double-conversion UPS** (APC Smart-UPS, Eaton 5PX): regenerates AC continuously. Excellent isolation; ~$500–$1500.
- **Power conditioner** (Furman P-1800 PF, AR-PRO): voltage regulation plus filtering. ~$300–$800.
- **Isolation transformer** (Tripp-Lite IS series): galvanic isolation from mains. ~$200–$500.

Most amateur stations don't need these. The Isobar 6 Ultra is plenty for 99% of cases.

## Separating digital and RF supplies

A subtle but important rule: **digital noise should not share a supply with the radio**.

Modern computers (especially the PSU) generate broadband noise from DC to GHz. If the same PSU feeds both the computer and a 12 V radio accessory, the radio sees that noise on its DC input — and it ends up in the receiver as a raised noise floor.

The fix:

- **Computer PSU** and **radio PSU** are separate units, on different AC outlets if possible (different banks of the Isobar).
- **Audio interface** (soundcard) should be **isolated** from the rig's DC ground via a transformer (MFJ-1224, SignaLink) or USB isolator.
- **Common-mode chokes** on all USB/Ethernet between computer and rig (see §27-05).

A station that obeys this rule has dramatically lower noise floor than one where everything shares a single 30 A switching supply.

```
                ┌────────────────┐
   AC ─→ Isobar├─→ Bank 1 (low noise) ─→ Linear PSU ─→ Radio
                │                       (Astron RS-35M
                │                        or Daiwa SS-330W)
                │
                ├─→ Bank 2 (high noise) ─→ Switching PSU ─→ Amp
                │                          (PowerWerx
                │                           SS-30DV)
                │
                ├─→ Bank 3 (digital) ─→ Computer
                │
                └─→ Bank 4 (lights/aux) ─→ Desk lamp, monitor
```

This is the "four-bank" layout. Each bank handles a class of load; cross-contamination is minimal.

## Surge suppression — the basics

Every AC outlet bank should have surge suppression. The **Joule rating** is a rough indicator of capacity:

- < 1000 J — entry-level, single use against a moderate strike.
- 1000–2500 J — typical good consumer strip.
- 2500–4500 J — good for an amateur station with multiple events.
- > 4500 J — commercial-grade.

A typical Isobar 6 Ultra is 3840 J — solid. Note that this is *only* mains-side protection; coax-side surge protection is separate (see §27-03).

> **Advanced —** AC surge suppression alone is not enough during a real lightning event. The strike's primary path is *through the antenna and coax* — that's why §27-03 emphasizes coax surge protectors at the entry panel. AC suppression handles surges *originating* on the mains (utility events, neighbor's HVAC startup transients, etc.). The two are complementary, not interchangeable. A station with only AC suppression and no coax suppression is essentially unprotected against actual lightning. A station with only coax suppression and no AC suppression is exposed to common utility events. Use both.

## A finished power layout

```
   AC mains ──┬── [whole-house surge protector at panel]
              │
              └─→ Isobar 6 Ultra ──┬─→ Linear PSU 13.8V/30A ─→ RIGrunner ──┬─→ HF rig (25 A fuse)
                                   │                                       │
                                   │                                       ├─→ VHF rig (15 A fuse)
                                   │                                       │
                                   │                                       ├─→ Keyer (1 A fuse)
                                   │                                       │
                                   │                                       └─→ Lamp (1 A fuse)
                                   │
                                   ├─→ Switching PSU (amp) ─→ HF amp
                                   │
                                   ├─→ Computer ─→ Monitor
                                   │
                                   └─→ Misc (lights, charger station)
```

Total cost: ~$80 (Isobar) + $300 (Astron linear PSU) + $130 (RIGrunner) + Powerpoles and fuses (~$30) = **~$540** for a comprehensively distributed 12 V/AC plant. Half that with a switching PSU instead of linear.

## Related sections

- [§27-09 — Noise Mitigation at PSU](27-09-noise-mitigation-at-power-supply.md) — what to do when the PSU is itself the noise source
- [§27-07 — Portable Power (LiFePO4)](27-07-portable-power-lifepo4.md) — battery-based distribution for portable
- [§16-01 — Battery Maintenance](../16-maintenance/16-01-battery-maintenance.md)
- [§27-01 — Single-Point Grounding](27-01-single-point-grounding.md) — the bus that ties this all to earth
