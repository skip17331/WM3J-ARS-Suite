---
id: 30-03
title: EME — Earth-Moon-Earth Basics
chapter: 30
section: 03
level: advanced
status: draft
---

# EME — Earth-Moon-Earth Basics

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What EME is

You aim a high-gain antenna at the Moon. You transmit. 2.5 seconds later — the round-trip time of light to the Moon and back — your own signal comes back to you, having traveled approximately 770,000 kilometers via reflection off the lunar surface. A second station, anywhere on Earth where the Moon is also visible, can hear that same reflection and can answer the same way.

EME ("Earth-Moon-Earth," or colloquially **moonbounce**) is the most extreme weak-signal VHF/UHF mode amateur radio offers. It works on 2 m, 70 cm, 23 cm, and increasingly the higher microwave bands. The path is essentially independent of weather, season, and ionospheric conditions — what matters is whether the Moon is mutually visible to both stations.

## Why it's hard — the link budget

The headline number is path loss. Let's tabulate one-way path loss for the round trip:

| Band | Path loss (one-way Moon round trip) |
|------|-------------------------------------|
| 50 MHz | ~250 dB |
| 144 MHz | ~252 dB |
| 432 MHz | ~262 dB |
| 1296 MHz | ~272 dB |
| 10 GHz | ~290 dB |

Compare that to HF DX, where free-space + ionospheric losses might total 100–140 dB. EME is **100+ dB harder than HF DX**. The Moon's surface reflects only about 6.5% of incident power (limb-corrected to ~12% effective for narrow-beam systems), which adds another ~12 dB of loss in the radar equation.

To close this link, three things must add up:

1. **High ERP at the TX end** — power × antenna gain × feedline efficiency.
2. **Low system noise temperature at the RX end** — quiet preamp, sky-pointing antenna, narrow filtering.
3. **A digital mode with extreme processing gain** — narrowband detection over many seconds.

## Equipment baseline

EME ranges from "huge expensive station" to "very-huge expensive station." Here's what each tier looks like on the most common band, 2 m:

### Entry station (works moderate-station EME contacts)

| Component | Spec |
|-----------|------|
| Antenna | 4× 17-element Yagis (about 22 dBi gain) on a stacked array |
| Az/el rotator | Yaesu G-5500 or similar; needs to track ±0.5° |
| Power | 500 W with low-loss feedline |
| Preamp | Mast-mounted GaAs LNA, ~0.4 dB NF |
| Rig | IC-9700, FT-991A, or K3 + VHF module |
| Software | WSJT-X with Q65-60A mode |

Build cost in 2026: roughly $4–8k for the antenna array alone, plus the rig and amplifier.

### Big station (works almost everyone)

| Component | Spec |
|-----------|------|
| Antenna | 8× 22-element Yagis stacked, ~26 dBi |
| Power | 1500 W (legal limit in US) |
| Preamp | 0.2 dB NF, tower-top |
| Tower | 60–80 ft, with rotator and elevation control |

These are the stations whose call letters you'll see on every EME contest — typically half-acre of antenna and a separate building for the equipment.

### Dish stations (23 cm and up)

Above 1 GHz, parabolic dishes become practical. A 3-meter dish at 1296 MHz has ~30 dBi gain — equivalent to many stacked Yagi systems but mechanically simpler. Microwave EME above 10 GHz is the domain of 5–10 meter satellite-TV dishes.

## Bands and characteristics

| Band | Notes |
|------|-------|
| **2 m (144 MHz)** | Most active EME band. Big Yagi arrays. High sky noise during galactic-plane transits, low otherwise. |
| **70 cm (432 MHz)** | Second most popular. Smaller Yagis, more total gain. Bigger Doppler. |
| **23 cm (1296 MHz)** | Dish territory. Moon noise begins to dominate; preamp NF less critical. |
| **13 cm (2304 MHz)** | Very active in Europe; less so in US (uses non-allocated freqs in some regions). |
| **9 cm / 6 cm / 3 cm** | Specialist territory. Very narrow beams; very precise pointing. |

## The dominant mode — Q65

Until 2020, EME at the entry-station level was difficult because JT65 (the legacy mode) couldn't reliably decode below about −24 dB SNR. Then **Q65** appeared in WSJT-X 2.4. Q65 uses Reed-Solomon-like forward error correction, narrow-bin tone sequences (5–6 Hz), and 60-second sequence lengths. The threshold:

| Mode | Decoder threshold | Sequence | Use case |
|------|-------------------|----------|----------|
| Q65-60A | −28 dB SNR | 60 s | Standard EME |
| Q65-30A | −24 dB SNR | 30 s | Better paths (big stations) |
| JT65A | −24 dB SNR | 60 s | Legacy; still in use |
| MSK144 | −8 dB SNR | 15 s | Only useful when libration is very low |

Q65-60A is the practical answer. It made 4-Yagi / 500 W stations viable EME stations on 2 m. The mode is in WSJT-X under the "Q65" tab; select the EME sub-band frequency (144.116 MHz on 2 m), point at the Moon, and call CQ on the 60-second sequence.

## The on-air rhythm

```
00:00  TX: CQ WM3J FN20
01:00  RX: WM3J DL9XYZ JO62      (decoded after sequence completes)
02:00  TX: DL9XYZ WM3J -23
03:00  RX: WM3J DL9XYZ R-21
04:00  TX: DL9XYZ WM3J RRR
05:00  RX: WM3J DL9XYZ 73
```

Every sequence is 60 s. A complete contact is 6 minutes. Marginal contacts that lose a decode here and there can take 20–40 minutes.

Conventional sequence rule: **even-minute station transmits 00–60**, **odd-minute station transmits 01–02**, etc. Both stations agree on who's even and who's odd before the QSO begins (in the chat room or on the email reflector).

## Doppler

The Moon moves relative to each station. Each station's signal is Doppler-shifted on the way up and again on the way down. The total Doppler can be hundreds of Hz on 2 m, several kHz on 23 cm:

| Band | Worst-case Doppler shift (TX or RX) |
|------|--------------------------------------|
| 2 m | ±300 Hz |
| 70 cm | ±900 Hz |
| 23 cm | ±2.5 kHz |
| 10 GHz | ±20 kHz |

WSJT-X automatically computes and applies Doppler corrections if you tell it your station's location and the other station's location (or both stations' Moon-tracking data). Without correction, signals smear out of the narrow Q65 detection bins.

## Libration noise — the EME-specific problem

The Moon's surface doesn't reflect like a mirror. It's rough at radio wavelengths, and the rotation called **libration** (the Moon's slight rocking as seen from Earth) means different parts of the surface contribute slightly different Doppler shifts. The net effect is spectral broadening — your signal isn't a pure tone; it's a tone smeared across a few Hz on 2 m, tens of Hz at 23 cm, hundreds of Hz at 10 GHz.

This libration broadening **sets the minimum useful detection bandwidth** for each band:

| Band | Libration spread (typical) |
|------|----------------------------|
| 2 m | ~2 Hz |
| 70 cm | ~6 Hz |
| 23 cm | ~18 Hz |
| 10 GHz | ~250 Hz |

Q65's tone spacing is tuned to match this. Narrower would lose energy to libration smearing; wider would let in more noise.

## The community

EME is a small, organized community:

- **WSJT EME group** — K1JT's reflector. Schedules and announcements.
- **HB9Q logger** — the de-facto sked-and-chat page during contests.
- **ARRL EME contest** — late October and late November weekends.
- **DUBUS / REF EME Contest** — European-led, similar weekend.
- **EME conferences** every two years rotating between continents.

These channels are where the activity lives. If you're not on the chat during a contest, you'll hear very few contacts.

## When EME shines

- **Working DXCC entities on VHF/UHF** — many rare entities are only workable via EME because they don't have HF or have nobody on terrestrial VHF.
- **VUCC and DXCC on 432 MHz / 1296 MHz** — almost impossible without EME.
- **Contests** — the ARRL EME contest is the calendar highlight; serious operators plan years of station-building for it.
- **Quiet-band weekends** — when HF is dead due to a solar storm, EME continues to work.

## When EME doesn't help

- **General-purpose ragchew.** Q65 is short-message-only.
- **Time-of-day operation.** You operate when the Moon is up and in a useful position; that's a few hours per day, shifting nightly.
- **Modest stations on 2 m looking for any contact.** EME needs the 4-Yagi tier minimum.

> ⚙️ **Advanced —** The signal-to-noise calculation for EME uses the radar equation modified for Moon reflection: SNR (dB) = P_TX + G_TX + G_RX − L_path − L_lunar − 10 log(kT_sys B). On 2 m with 500 W (+27 dBW), 22 dBi each end, 252 dB one-way path × 2, 12 dB lunar loss, T_sys ≈ 200 K, and B = 5 Hz (Q65 tone bandwidth), the result is about −25 dB SNR — within Q65's −28 dB decode threshold by a 3 dB margin. That's the link budget that defines "entry-level EME station." The margin is what gets eaten by mispointing, feedline loss, libration broadening, and the inevitable Faraday rotation on 2 m (where ionospheric birefringence rotates linear polarization in unpredictable ways over the round trip). Many serious 2 m EME stations use circularly polarized antennas or switched H/V polarization to mitigate Faraday losses, which can be 20+ dB at worst alignment.

## See also

- [§01-09 — Weak-Signal VHF/UHF](../01-propagation/01-09-weak-signal.md) — EME in propagation context
- [§30-02 — Meteor Scatter Operating](30-02-meteor-scatter.md) — the related VHF specialty
- [§03 — Digital Modes](../03-digital-modes/) — Q65 / JT65 in the WSJT-X family
- [§07 — Satellites](../07-satellites/) — Doppler tracking; same mechanical infrastructure
- [§06 — Antennas](../06-antennas/) — long Yagis and stacked arrays
- [§11 — Power Budget / ERP](../11-power-budget-erp/) — link budget calculation
