---
id: 01-09
title: Weak-Signal VHF/UHF (Tropo, EME, Meteor Scatter, Aurora)
chapter: 01
section: 09
level: mixed
status: draft
---

# Weak-Signal VHF/UHF — Tropo, EME, Meteor Scatter, Aurora

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The HF bands ride the ionosphere. VHF and UHF mostly don't — they go where line-of-sight, the atmosphere's lower layers, or unusual physics will take them. **Weak-signal operating** is the umbrella term for chasing those unusual paths: tropospheric ducting, Earth-Moon-Earth, meteor scatter, and aurora. Each has its own physics, its own season, its own operating playbook.

These modes share a few traits:

- Signals are usually **near the noise floor** — readability hinges on antenna gain, low-noise receivers, and digital modes designed for it.
- The bands of choice are **6 m, 2 m, 70 cm, 23 cm**, sometimes higher.
- The contacts feel **earned** — none of these are casual ragchew modes.

Sporadic E and TEP are also "weak signal" in a sense, but they're covered in §01-06 because their seasonal patterns and ionospheric mechanisms put them with the HF-style propagation. This section covers the four modes that don't fit anywhere else.

## Tropospheric ducting

The troposphere — the lowest 10–15 km of the atmosphere — usually attenuates VHF/UHF as you go beyond line-of-sight. But under specific weather conditions, a **tropospheric duct** forms: a layer of atmosphere with a sharp temperature/humidity gradient that bends signals along its length, sometimes thousands of kilometers.

### When it happens

| Condition | Where / when | Bands |
|-----------|--------------|-------|
| **Stable high-pressure systems** with temperature inversion | Anywhere; common in summer/early autumn | 6 m → 23 cm |
| **Coastal / marine ducts** along sea-air interface | Mediterranean, Caribbean, Sea of Japan, Gulf coast | 2 m, 70 cm, 23 cm |
| **Subsidence inversions** under stagnant high pressure | Inland US, Europe in summer | 2 m, 70 cm |

The classic continental enhancement is **summer high-pressure** sitting over a region for days: warm dry air aloft over cooler/moister air at the surface produces a sharp inversion at 1500–3000 ft, and 2 m/70 cm contacts open between cities 800–2000 km apart that normally couldn't hear each other.

### Signatures on the air

- **Quiet-band 2 m suddenly full of distant grid squares** (FN31 worked from EM84, etc.).
- **6 m and 2 m beacons** from across the continent become audible.
- **Slow fading** with periods of seconds-to-minutes, not the rapid Es flutter.
- **Repeaters from far away** unexpectedly coming through full-quieting — the first hint for many operators that a duct has formed.

### Operating playbook

- **Watch the band openings on DX clusters and APRS.** Tropo enhancements light up the spotting networks.
- **Use SSB or CW on the SSB calling frequencies** (50.125, 144.200, 432.100 MHz in IARU Region 2). FT8 also works well at the digital sub-band frequencies.
- **Beam horizontally** at the duct-entry direction. Tropo ducts are roughly horizontal channels — high-angle antennas waste your signal.
- **Listen first.** Tropo openings can be one-way; hearing the other end doesn't mean they hear you.
- **Watch the weather map.** A persistent surface high pressure system over the path corridor is the trigger.

> **Advanced —** The mathematical condition for ducting is dN/dh < −157 N-units per km, where N is the radio refractive index (a function of pressure, temperature, and water-vapor partial pressure). The duct height and thickness determine which frequencies are trapped: very thin ducts only work above 1 GHz; thick ducts can trap 50 MHz down to 2 m. The ITU-R P.452 model is the standard reference for predicting tropo path loss including duct enhancement. WSJT-X's MSK144 and FT8 modes work brilliantly through tropo because they're designed for shallow-fade weak-signal paths.

## EME — Earth-Moon-Earth ("moonbounce")

You point a high-gain antenna at the Moon, transmit, and 2.5 seconds later your own signal comes back having traveled ~770,000 km via lunar reflection. Another station does the same, and your signals reach each other off the Moon's surface.

### What you need

| Resource | Typical commitment |
|----------|---------------------|
| **Power** | 500 W on 2 m; 1 kW on 70 cm; less for digital modes |
| **Antenna** | 4 × 17-element Yagis on 2 m; 4 × 25-element on 70 cm; or a 3+ m dish on 23 cm and up |
| **Az/el rotator** | Needed; the Moon moves fast |
| **Low-noise preamp** | Critical — Moon-noise dominates at 1.4 GHz, sky-noise dominates at lower bands |
| **WSJT-X (Q65 or JT65)** | Standard digital mode for marginal contacts |

CW or SSB EME contacts are still possible but require monster stations on both ends. Q65 (introduced 2020) made EME accessible to 4-Yagi-and-1-kW stations and is now the dominant mode.

### Bands and path loss

| Band | One-way path loss |
|------|-------------------|
| 50 MHz | ~250 dB |
| 144 MHz | ~252 dB |
| 432 MHz | ~262 dB |
| 1296 MHz | ~272 dB |
| 10 GHz | ~290 dB |

That sounds catastrophic — and it is. But the Moon's 12% reflectivity, plus your gain on both ends, plus the receiver's narrow bandwidth (Q65 uses ~2 Hz bins) gives a closed link budget for many station combinations.

### The on-air rhythm

- **Even-minute** transmits one direction, **odd-minute** the other. Q65 sequences are 60 s long.
- **Doppler shift** is significant: ±300 Hz on 144 MHz, ±2.5 kHz on 1296 MHz over a single Moon pass. Operators correct for it manually or with software (WSJT-X auto-tracks).
- **Library**: WSJT-X has a "Lunar Astronomical Position" tool that displays EME-relevant info: Moon altitude, range, Doppler, sky temperature.
- **Weekly nets**: K1JT's WSJT EME group, the EME-Net on 2 m, and the 70 cm EME chat on the air give you skeds with stations of comparable capability.

### Practical first contacts

If you're new to EME:

1. **Borrow time on a moderate station** before building one. Many EME ops run public events where they QSL contacts for novices using their gear.
2. **Start with 2 m** — antennas are smaller and cheaper, sky temperature is manageable.
3. **Use Q65 mode** in WSJT-X, with the EME sub-band setting. Listen first; learn the rhythm; then call.
4. **Don't expect a ragchew.** Q65 EME is callsign + grid + report exchanges only. A complete contact is 6-10 minutes.

> **Advanced —** EME signal-to-noise depends on the radar equation modified for Moon reflection: SNR (dB) = ERP_dBW + Gr_dB − 41 − 20 log(f_GHz) + cos(elev) corrections. Moon noise (libration broadening — the Moon's rotation makes a 2 Hz spread on 144 MHz, 18 Hz on 1296) limits how narrow your detection bandwidth can be. The optimal mode tradeoff is bandwidth vs symbol time: Q65-60A uses 60 s sequences and ~6 Hz tones — narrow enough to dig out −28 dB SNR signals, wide enough to tolerate libration and Doppler smearing.

## Meteor scatter

Every clear night, tons of dust-sized particles burn up in the upper atmosphere, leaving brief trails of ionized gas at 80–120 km altitude. Each trail can reflect VHF signals for **fractions of a second** to a few seconds. Your transmission has to overlap with one of those windows.

### Bands and ranges

| Band | Typical reflection probability | Range |
|------|--------------------------------|-------|
| 6 m | High | 800–2200 km |
| 2 m | Moderate | 500–2000 km |
| 70 cm | Low | rare; 200–1500 km |

The sweet spot is **2 m for organized contacts** (digital modes plus high power and long Yagis make it routine) and **6 m for casual chasing during meteor showers**.

### The shower calendar

Meteor showers concentrate the activity into predictable nights:

| Shower | Peak | Typical ZHR | Notes |
|--------|------|-------------|-------|
| Quadrantids | early Jan | 110 | Brief peak (~6 hr); cold-weather op |
| Lyrids | late April | 18 | Modest |
| Eta Aquariids | early May | 50 | Better in southern hemisphere |
| Perseids | mid August | 100 | The classic; consistent year-to-year |
| Orionids | late October | 20 | Modest |
| Leonids | mid November | 15 (occasional storm years 1000+) | Storm cycle ~33 yr |
| Geminids | mid December | 150 | Best of the year |

Outside showers, "sporadic background" still produces 2-5 reflections per minute on 6 m if you're listening in the early morning hours (4–6 AM local).

### Operating modes

- **MSK144** (in WSJT-X) — the standard digital meteor-scatter mode, 15 s sequences. Designed to fit a complete callsign+report exchange into a single ping.
- **FSK441** — older meteor-scatter digital mode, slower, less efficient.
- **High-speed CW** — 1000+ WPM machine-generated, decoded from recordings. Mostly historical; replaced by digital.
- **SSB on the calling frequency** (50.260 USB, 144.200 USB) during the peak hours of a major shower — you'll hear bursts of actual voice if signal is strong.

### A typical Perseids night

```
21:00 local — set up: 144.150 MSK144 with 100 W and a single 17-element Yagi
22:00       — start listening; first pings appearing every 30-90 s
01:00       — peak hours; pings every 5-15 s, several lasting 1+ s
03:00       — 8 contacts complete with stations 800-1500 km away
05:00       — shower past peak; back to scattered pings
```

A serious meteor-scatter session is patient: long-Yagi station, a digital-mode-friendly waterfall display, and the willingness to try a contact for 20-30 minutes before catching the right ping at the right moment.

> **Advanced —** Meteor trails are classified as **underdense** (electron density too low to fully reflect — signal builds up exponentially then decays) or **overdense** (full reflection during the trail's lifetime). Underdense trails dominate at meteor sizes below a few mg; they last ~0.1 s and produce the sharp "pings" common in MS QSOs. Overdense trails from larger meteors last seconds and produce continuous sustained signals. The radio reflection coefficient depends on the angle between the trail and the path geometry — there's a "meteor scatter geometry" optimum where your beam and the other station's beam both point at a common reflection volume in the sky. WSJT-X's meteor-scatter map tool shows the optimum aiming for any given path.

## Auroral propagation

When the geomagnetic activity gets high enough — A-index > 30, K-index ≥ 6 — the **auroral oval** drops to mid-latitudes and the ionized particles in the aurora can scatter VHF signals. The result is a distinctive "aurora-burble" propagation that opens 6 m and sometimes 2 m, but with severe distortion.

### When and where

- **Storm conditions**: Kp ≥ 6, A-index >30. Watch noaa.gov/swpc.
- **Mid-latitude auroral oval**: typically extends to the US northern tier states during minor storms; major storms push it as far south as Florida.
- **Best for stations in middle-to-northern latitudes** — VE, northern US, Scandinavia, northern UK.

### Signature on the air

- Signals sound **buzzy, hashy, distorted** — the rapid time-varying scattering produces a "hissing" character.
- **CW** is the preferred mode — modulation is barely intelligible on SSB during strong aurora.
- **Bearing**: from the northern hemisphere, signals come from **roughly due north**, regardless of where the other station actually is. The aurora reflects them off the magnetic-equator-pointing direction.

### Operating notes

- **6 m** is the most common auroral band; 2 m occasionally; 70 cm rarely.
- **Beam north**, regardless of where you're trying to reach. Both stations beam north and the auroral patch in the sky reflects signals between them.
- **Use CW** during aurora openings — SSB can be unintelligible.
- Watch **PSK Reporter, RBN, and the Cluster** for early indicators that the aurora is producing 6 m / 2 m contacts.

> **Advanced —** Auroral scattering is field-aligned: the ionization density irregularities along the geomagnetic field lines act like a corner reflector in the sky. The reflection bandwidth is finite — wide modulation gets distorted as different frequencies scatter from slightly different positions in the reflective volume. The "buzz" is the time-domain manifestation of this dispersion. Aurora can also produce **auroral E** propagation on HF — the high-latitude D-layer thickens during a storm and absorbs HF signals, but the E-layer ionization can scatter 28-50 MHz signals for short hops (300-1500 km) along the auroral oval.

## Combining the modes

Real weak-signal operators chase opportunities. A single late-summer evening might offer:

- Tropo ducting opens a 700 km path on 2 m.
- A passing meteor produces a 0.5 s ping on 6 m.
- An EME schedule for a rare DX entity at 02:00 UTC.
- An aurora alert from PSK Reporter at 04:00 UTC.

The serious operator runs all four — 6 m, 2 m, 70 cm radios, multiple antennas (or one quickly switchable Yagi), WSJT-X for digital decoding across the bands, and an alarm clock for the EME schedule. Each contact is a small victory because no single mode dominates a single night.

## Equipment baseline for the four modes

What a "starter" weak-signal station looks like:

| Mode | Antenna | Power | Mode |
|------|---------|-------|------|
| Tropo | Long single Yagi (10 elements 2 m, 12 elements 70 cm) | 25–100 W | SSB / CW / FT8 |
| EME | 4 × 17 el 2 m or 1.5 m dish 23 cm + | 500 W – 1 kW | Q65 |
| Meteor scatter | 9–17 el 2 m Yagi | 50–100 W | MSK144 |
| Aurora | Same 2 m Yagi as tropo | 50–100 W | CW |

A station built for tropo and meteor scatter on 2 m can serve all four with the addition of a moderate-gain 6 m Yagi and (later) the EME upgrade to 4-Yagi array.

## See also

- §01-04 — Ionospheric Layers (E and F2 — the regular paths)
- §01-06 — Sporadic E, TEP, Skip (the other "anomalous" propagation modes)
- §01-08 — Band Choice Right Now (when to suspect one of these is happening)
- §06-07 — Radiation Patterns (low takeoff angle for tropo, high gain for EME)
- §06-09 — Diversity (sometimes useful for weak-signal recovery)
- §07 — Satellites (Doppler, polarization — share concerns with EME)
- §17-08 — ERP (link budget for EME)
- §03-01 — FT8 / FT4 / Q65 / MSK144 (the digital modes used here)
