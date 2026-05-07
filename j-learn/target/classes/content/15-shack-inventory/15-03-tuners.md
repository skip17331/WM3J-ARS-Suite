---
id: 15-03
title: Tuners
chapter: 15
section: 03
level: simple
status: draft
---

# Tuners

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Antenna tuners — manual, automatic, in-rig, external, and remote-base — are accessory items that can range in value from a $50 used MFJ box to a $1500 Palstar or DX Engineering remote-base unit. Each gets its own inventory row.

## Schema applied to tuners

| Core field | Tuner-specific notes |
|------------|----------------------|
| Manufacturer | LDG, MFJ, Palstar, DX Engineering, Elecraft, ATU built into a rig (note "(in-rig)"), etc. |
| Model | AT-100ProII, MFJ-993B, AT2K, KAT-3, etc. |
| Serial number | Usually present on a label on the back |
| Date acquired | Original purchase or transfer date |
| Purchase price | What you paid |
| Estimated value | Used market value — many older tuners hold value moderately well |
| Disposition | Working / Repairable / Not Repairable |

Tuner-specific extra fields:

| Extra field | Why it matters |
|-------------|----------------|
| **Type** | Manual, automatic (ATU), remote-base |
| **Power rating** | 100 W, 200 W, 600 W, 1.5 kW (matters for high-power compatibility) |
| **Frequency range** | HF only? VHF support? Specific bands? |
| **SWR matching range** | What load impedance can it match (typical: 8:1, 10:1, etc.) |
| **Memory presets** | Auto-tuners with memory: how many band/freq combinations stored |
| **Connection mode** | Standalone or rig-controlled (which radios via what cable) |

## Categories of tuners

### Manual tuners

Knobs and dials; operator manually adjusts capacitors and inductors:

```
Item ID: T-001
Category: Manual Tuner
Manufacturer: MFJ
Model: MFJ-989D
Serial Number: 989D-12345
Date Acquired: 2014-03-20
Purchase Price: 350.00
Estimated Value: 250.00
Disposition: Working
Type: Manual T-network
Power Rating: 1500 W
Frequency Range: 1.8 - 30 MHz
SWR Matching: up to 10:1
Notes: Roller inductor + L/C combo. Used when tuning balanced ladder line.
       Connected to balanced output of antenna switch.
```

Manual tuners require operator engagement on each tune-up; their value is the wide matching range and high power capability.

### Auto-tuners (ATUs)

Self-tuning, often integrated with a radio:

```
Item ID: T-002
Category: Auto-Tuner (ATU)
Manufacturer: LDG
Model: AT-100ProII
Serial Number: 1234567
Date Acquired: 2015-11-30
Purchase Price: 250.00
Estimated Value: 175.00
Disposition: Working
Type: Automatic L-network
Power Rating: 125 W (digital), 100 W (CW), 100 W (SSB)
Frequency Range: 1.8 - 30 MHz
Memory: 1000 presets per radio
Connection: Inline between IC-7610 and feedline (T-001 used for ladder line only)
```

Auto-tuners are the most-common modern choice — they "just work" most of the time, with rapid (~5 sec) tune cycles.

### Remote-base tuners

Mounted at the antenna feedpoint, controlled from the shack:

```
Item ID: T-003
Category: Remote-Base ATU
Manufacturer: DX Engineering
Model: DXE-RBA-2T1 (remote-base auto-tuner kit)
Serial Number: RBA-9876
Date Acquired: 2018-06-05
Purchase Price: 850.00
Estimated Value: 600.00
Disposition: Working
Type: Remote-base automatic
Power Rating: 1500 W
Frequency Range: 1.8 - 30 MHz
Connection: Mounted at antenna feedpoint of vertical (A-002); controlled via dedicated coax
Notes: Eliminates feedline mismatch loss. Re-tunes when band changes.
```

Remote-base tuners eliminate the "tuner at the rig hides feedline mismatch" problem (§08-03). Higher upfront cost; better RF performance.

### In-rig built-in tuners

Many modern HF radios have built-in ATUs. Track as part of the radio's notes, not as a separate inventory item:

In radio notes (§15-01): "Built-in 100W ATU; matching range to 3:1; effectively a free auto-tuner for typical antennas."

If the rig has a separate external tuner option (Icom AH-705, Yaesu FC-50, etc.), the external unit gets its own inventory row.

## Tuner-specific inventory considerations

### Power rating verification

The tuner's power rating must match or exceed your transmit power:

- A 200 W tuner cannot safely handle a 1500 W amplifier output.
- Operating beyond rating risks the tuner's components (capacitors arc, switches melt, transformer cores saturate).

Match tuner rating to maximum power you'll run through it.

### Working with old tuners

Tuners from the 1960s-80s often still work and have collector interest:

- Drake, Heathkit, Hy-Gain, MFJ early models — vintage value plus operational value.
- Vacuum capacitors in old high-power tuners can be very expensive replacements.
- Belts and pulleys in roller-inductor tuners wear and break; spare parts may not be available.

For estate planning, vintage tuners may be more valuable to collectors than modern equivalents.

### Disposition for tuners

- **Working**: tunes properly, all bands, hands a 1:1 SWR to the rig with normal-range antennas.
- **Repairable**: a specific failure (capacitor arc, switch failure, motor in auto-tuner failed) where the fix is feasible. Compare repair cost to replacement.
- **Not Repairable**: chassis, knobs, capacitors as donor for repairs of similar tuners.

## Tuner accessories

Track:

- **Control cable** (rig-to-tuner; specific to rig manufacturer for auto-tuners).
- **Audio cable** (some ATUs interface with rig CAT for band-change signaling).
- **Power supply** (if AC powered or battery-powered).

## Where tuners go in the signal chain

Documenting where the tuner is in the signal chain helps the inventory tell the full story:

```
T-001 (manual, MFJ-989D): used for balanced ladder line antennas only.
       Inline: Rig → Antenna switch → Balanced port → Manual tuner → Ladder line → Antenna.

T-002 (auto, LDG AT-100ProII): everyday auto-tuner.
       Inline: IC-7610 → AT-100ProII → Coax → Antenna.

T-003 (remote-base, DXE-RBA): at the antenna; not in shack chain.
       Coax run R-005 → DXE-RBA at antenna feedpoint.
```

This documentation makes troubleshooting and equipment swaps easier.

## Common tuner inventory mistakes

- **Skipping the power rating.** Critical for matching to amplifier output; record explicitly.
- **Assuming an old tuner has no value.** Vintage tuners can have collector value; check resale before disposing.
- **Forgetting the control cable.** A separate-purchase auto-tuner cable is part of the system; track with the tuner.
- **Not tracking ATU memory state.** If you re-program the ATU's memory, document what's stored — a database of band/frequency presets.
- **Treating in-rig ATUs as separate inventory items.** They live with the rig (§15-01).

## See also

- §15-00 — Schema overview
- §15-01 — Radios (where in-rig ATUs live)
- §04-11 — Impedance transformation (what tuners do)
- §04-12 — Baluns and chokes (sometimes paired with tuners)
- §08-03 — Mismatch loss (why tuners at the rig don't always help)
- §16 — Estate / SK (tuner disposition planning)
