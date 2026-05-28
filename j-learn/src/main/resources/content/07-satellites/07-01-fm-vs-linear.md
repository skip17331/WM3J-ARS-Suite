---
id: 07-01
title: FM vs Linear Satellites
chapter: 07
section: 01
level: simple
status: draft
---

# FM vs Linear Satellites

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Amateur satellites fall into two operational families, distinguished by what they do with your transmitted signal:

- **FM repeater satellites** ("FM birds"): take an FM-modulated signal on the uplink, demodulate it, and re-transmit it on the downlink. One QSO at a time, like a terrestrial FM repeater.
- **Linear transponder satellites**: take a slice of spectrum on the uplink (e.g., 60 kHz wide) and re-transmit it inverted (or non-inverted) on the downlink. Multiple SSB or CW QSOs can run simultaneously, each on its own frequency within the transponder's passband.

The two require different gear, different procedures, and very different operator etiquette. This section covers both.

## FM repeater satellites

The "easy sats." Most beginning satellite operators start here.

### How they work

The satellite has an FM receiver tuned to its uplink frequency and an FM transmitter on its downlink frequency. When you transmit on the uplink, the satellite hears it, demodulates, and re-transmits on the downlink.

Inside the satellite:
- Receive uplink FM signal → demodulate to audio → re-modulate FM on downlink frequency → transmit.

Most FM birds are **U/V** mode: UHF uplink (435 MHz region), VHF downlink (145 MHz region). A few are V/U.

### Equipment required

Minimum:

- **Dual-band HT capable of full duplex** (transmit on one band, receive on the other simultaneously). Yaesu FT-2DR, FT-3DR, FT-5DR; Kenwood TH-D74; Icom IC-W2A; etc. Most BaoFengs are *not* full duplex — they can do FM cross-band, but only half-duplex. Full duplex is needed because you can't hear yourself if you're not receiving while transmitting.
- **A directional antenna with some gain.** The Arrow II ("dual-band Yagi-on-a-stick") is the canonical choice — three elements 70 cm side, two elements 2 m side, hand-held. ~7 dBd gain on each band.
- **A handheld pass-prediction app** (e.g., AMSAT-NA's pass predictor app, ISS Detector, Heavens Above).

That's the kit. The cost is about $300-500 for a quality setup.

### Operating procedure

A typical FM bird pass:

1. **Predict the pass.** AOS (Acquisition Of Signal) is when the satellite first appears above your horizon; LOS is when it disappears. A typical LEO (Low Earth Orbit) pass is 10-15 minutes from AOS to LOS, with maximum elevation somewhere in the middle.
2. **Set up the radio.** Program the satellite's uplink frequency on transmit, downlink on receive. Set the **CTCSS subaudible tone** (most FM birds require 67.0 Hz; some use 88.5 or 100.0).
3. **Wait for AOS.** The satellite signal appears as the satellite climbs above the horizon.
4. **Listen first.** Don't transmit immediately; verify the bird is operational and not already in use.
5. **Make your QSO.** When you hear a station calling CQ, respond. Or call CQ yourself once it sounds clear: "This is WM3J, FM19, calling on AO-91."
6. **Be brief.** A 12-minute pass shared by 30 stations means you have ~24 seconds of total mic time. Two-line exchanges are normal: "WM3J grid FM19" → "WM3J this is W1AAA grid FN42" → "73 W1AAA, WM3J QRT."
7. **Doppler-correct your downlink** as the pass progresses. See §07-02. Your radio's display jumps as the bird climbs and descends; expect ±5 kHz on 70 cm during a pass, ±2 kHz on 2 m.

### FM bird etiquette

The shared-resource nature of an FM bird's pass demands strict etiquette:

- **Don't dominate.** One short QSO per pass. Make room.
- **Don't QSY mid-pass.** Stay on the same frequency; don't tune around or look for another station.
- **Don't talk over others.** FM is "first one transmitting wins" — capture effect. If two stations transmit simultaneously, the stronger one wins; the weaker is unintelligible. Take turns.
- **Identify with grid square.** Standard satellite QSO is callsign + grid; signal report is implicit (you heard them; they're "5" by definition).
- **Don't run the bird as a chat repeater.** Save the social conversation for the next time you're on a terrestrial repeater. The bird is a tool, not a coffee shop.

## Linear transponder satellites

The serious-amateur birds. Most experienced satellite operators prefer these because they support multiple simultaneous QSOs and SSB/CW (more bandwidth-efficient than FM).

### How they work

The transponder is essentially a "frequency-shift relay": it takes a chunk of spectrum on the uplink (e.g., 60 kHz wide), shifts it to a different chunk of spectrum on the downlink, and broadcasts the shifted spectrum continuously. Whatever you transmit in the uplink slice appears, shifted, in the downlink slice.

The shift can be **non-inverting** (signals go up at the same offset they're at on the uplink) or **inverting** (a higher-frequency uplink becomes a lower-frequency downlink — the spectrum is mirrored).

Most amateur linear transponders are **inverting**. This means:
- A station calling CQ at the LOW end of the uplink range is heard at the HIGH end of the downlink range.
- A station calling CQ at the HIGH end of the uplink is heard at the LOW end of the downlink.
- USB on the uplink becomes LSB on the downlink (because the spectrum is mirrored).

> **Advanced —** The frequency relationship for an inverting transponder is: f_downlink = f_inv_center − (f_uplink − f_inv_center) = (2 × f_inv_center) − f_uplink. The center frequency of inversion is determined by the satellite's local oscillator; it's published as part of the satellite's spec sheet. For non-inverting transponders, f_downlink = f_uplink + offset (constant).

### Equipment required

More than for FM birds:

- **Full-duplex SSB/CW radio** — most modern HF/VHF/UHF base stations (IC-9700, IC-905, FT-991A, Kenwood TS-2000) support full-duplex satellite operation.
- **Crossed Yagis or two separate beams** for VHF/UHF — needed because linear transponder signals are weaker than FM (more bandwidth, but less power per Hz).
- **A pre-amplifier at the antenna** — recommended for marginal signals.
- **Automated rotator** — strongly recommended; manual aiming during a pass while operating the radio is a lot to juggle.

### Operating procedure

For a linear transponder pass:

1. **Predict the pass and configure the satellite.** Set your radio's uplink and downlink frequencies. Note the inversion (typical) and the inversion-center frequency.
2. **Listen on the downlink** as the satellite rises. You'll hear the transponder noise floor first, then individual signals as the pass progresses.
3. **Find an empty slot** in the downlink passband — somewhere there isn't already a QSO.
4. **Calculate the corresponding uplink** for that downlink slot (using the inversion formula above, or your radio's auto-set feature).
5. **Call CQ** in the appropriate sideband (LSB if the satellite is inverting and you'd normally use USB; vice versa).
6. **Doppler-correct as you go** — typically this is done by tuning your *uplink* frequency (the satellite re-transmits the shifted result, so adjusting your uplink keeps your downlink at a constant frequency).
7. **Make your QSO.** SSB voice or CW; same callsign + grid as on FM, but slower-paced because you can hear other QSOs simultaneously and there's not the same urgency.
8. **Be courteous.** Sharing the transponder bandwidth means everyone can talk simultaneously; just don't camp on a single frequency for the entire pass.

### Linear transponder etiquette

Different than FM:

- **Don't sit on one frequency for the whole pass.** Several QSOs are happening; rotate so others can use the same slot.
- **Use minimum power on the uplink.** Some transponders have AGC issues — a strong uplink station "captures" disproportionate downlink power and reduces the apparent strength of weaker stations.
- **Listen to the entire downlink.** A QSO at one end may be near a beacon, an AMSAT operator's check-in, or a pile-up for a rare grid square.

## Notable amateur satellites (as of 2026)

| Satellite | Type | Status | Notes |
|-----------|------|--------|-------|
| AO-91 (Fox-1B) | FM repeater | Active | LEO; standard FM bird |
| AO-92 (Fox-1D) | FM repeater | Active | LEO; standard FM bird |
| AO-95 (Fox-1Cliff) | FM repeater | Some availability | LEO |
| ISS (FM repeater mode) | FM repeater | Intermittent | When activated by crew |
| ISS (voice contact mode) | FM voice | Intermittent | Astronaut SSTV / voice |
| FO-29 | Linear transponder | Active | Inverting, V/U |
| RS-44 | Linear transponder | Active | Inverting, V/U; Russian |
| HO-113 | FM repeater | Active | Chinese amateur sat |
| TEVEL-1 through 8 | Linear (small) | Some active | Israeli educational sats |
| ARISS | (ISS amateur radio) | Active when scheduled | Includes scheduled school contacts with astronauts |
| Es'hail-2 / QO-100 | Linear (geostationary) | Active | Africa/Europe/Mid-East coverage; not visible from US |

The list changes — check AMSAT's status page (amsat.org or amsat-na.org) before each session for current operational satellites.

## Geostationary: a special case

**Es'hail-2 / QO-100** is a geostationary commercial satellite that hosts an amateur transponder section. It sits over the equator at 26°E longitude, visible from Africa, Europe, the Middle East, and parts of South America. Coverage is **continuous** — no Doppler, no pass scheduling, the satellite is just *there*.

QO-100 is a different operating experience — it's like a 24/7 always-on FM-and-linear repeater for half the world. North American hams cannot directly use it (no line-of-sight to the equator at 26°E from continental US) but can listen via internet-streamed receivers.

The next-generation geostationary amateur satellite (CAMSAT-X, AMSAT-DL planning) is in development, potentially with global coverage via multi-orbit constellations.

## Choosing FM vs. linear

| Factor | FM | Linear |
|--------|-----|--------|
| Difficulty | Easy | Moderate-Advanced |
| Equipment cost | $300-500 (Arrow + HT) | $1500+ (full-duplex base + dual-Yagis + rotator) |
| QSO style | Quick FM voice | SSB voice, CW |
| Operators per pass | One at a time (turns) | Multiple simultaneous |
| Skill required | Listen, talk, manage Doppler manually | Tracking, Doppler, sideband choice, multiple QSOs |
| Best first satellite | AO-91 or AO-92 | RS-44 or FO-29 |

Most amateurs start with FM and graduate to linear as their interest deepens.

## Common mistakes

- **Forgetting to set CTCSS tone for FM birds.** No CTCSS = the satellite's receiver doesn't open; nothing transmitted appears on the downlink. Verify before each pass.
- **Half-duplex radio on FM birds.** You can't hear yourself, can't tell if you got through, can't tell if someone is responding. Use full-duplex.
- **Calling CQ on a busy linear transponder slot.** Listen first; pick an empty slot.
- **Operating the wrong sideband for an inverting transponder.** USB uplink → LSB downlink. Get this right before keying up.
- **Not Doppler-correcting.** A pass without correction has the audio dropping in pitch through pass; on FM the squelch can drop out; on SSB the audio becomes unreadable.
- **Working only the satellite's main pass without checking grid for the bonus.** Working AO-91 from FM19 once is fine; working it from FM18 the next pass adds a different grid contact.

## See also

- §07-00 — Chapter overview
- §07-02 — Doppler shift (the math behind the frequency tuning)
- §07-04 — Tracking strategies (manual and automated)
- §07-07 — Pass prediction
- §20-04 — Satellite sub-bands
