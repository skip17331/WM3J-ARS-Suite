---
id: 30-07
title: Aeronautical Mobile Operation
chapter: 30
section: 07
level: advanced
status: published
---

# Aeronautical Mobile Operation

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What it is

**Aeronautical mobile** (`/AM`) is amateur-radio operation from an aircraft. The `/AM` suffix on your callsign tells the other station you're transmitting from inside or attached to a flying aircraft. Confusingly, this collides with the **AM** modulation type — context distinguishes them (a CW or SSB QSO labeled `WM3J/AM` is "WM3J aeronautical mobile," not "WM3J on AM mode").

`/AM` is one of the rarer modes of operation in amateur radio for one big reason: most jurisdictions either prohibit it outright or require the pilot-in-command's explicit permission for every transmission.

## Legality (read this carefully)

The rules differ by country and by aircraft type. As of current FCC interpretation in the US (verify with the FCC and your country's regulator before operating):

- **Crewed aircraft (US):** Amateur radio operation is permitted with the pilot-in-command's authorization. The pilot is legally responsible for any interference with aircraft systems. Most commercial airlines prohibit it under their passenger-service rules even when it would be legal under FCC §97. Private pilots may authorize it on their own aircraft.
- **Crewed aircraft (other countries):** Highly variable. Many European national regulators prohibit it entirely. UK, France, Germany — generally not allowed without specific permits. Always check national rules.
- **Drones (US):** Amateur radio operation from a drone is technically legal under §97 but must comply with FAA Part 107 rules (visual line of sight, weight limits, registration). The drone itself must be registered if heavy enough.
- **Amateur balloons (US):** High-altitude balloons carrying amateur radio payloads are common in the amateur community. The balloon itself is not "controlled" by an operator the way an aircraft is, so the regulatory situation is different — the FCC regulates the radio transmission, not the platform.

> **Advanced —** In the US, FCC §97.11 prohibits operation that "endangers the safety of any aircraft" — and the pilot-in-command has the final authority on what does or does not endanger safety. This effectively makes amateur radio in commercial aircraft impossible: airline crews are trained to prohibit any non-aircraft RF, and challenging that in flight is a fast track to being met by FBI agents on landing.

## Where `/AM` actually happens in practice

Despite the legal cautions, `/AM` does happen in three contexts:

1. **Amateur balloon / pico-balloon payloads.** A small APRS or WSPR transmitter attached to a latex balloon. Reaches 30,000 feet, drifts globally. Common amateur project. The transmitter has a fixed callsign + `/AM` suffix; the operator on the ground is identified separately.
2. **Drone-mounted radio for short flights.** A 5 W HT zip-tied to a quad-copter is `/AM`. Less common; pilots tend to prefer separate command/control radios from amateur radios.
3. **Private aircraft (rare).** A licensed amateur who is also a private pilot, with the pilot's hat on, sometimes operates while in the air. Their own aircraft, their own decision. Extremely rare in the air-to-ground QSO log.

In the third case, the antenna is usually a HF whip clipped to the airframe or a 2 m whip on a window mount. Power is typically 5–25 W. The QSO is generally brief — pilots have other things to manage.

## What `/AM` sounds like on the air

Like any other QSO, except:

- The `/AM` suffix is part of the callsign.
- Doppler shift on VHF/UHF is detectable but small (~30 kHz at 432 MHz for a 600 mph aircraft — same order as satellite Doppler).
- The signal usually has a faint propeller chop or jet hum in the audio. Modern noise-reduction DSP largely removes this.
- The other station gets a unique grid square (calculated from GPS) often updating during the QSO.

Logging: confirm receipt of the operator's grid square explicitly. `/AM` contacts count for some awards (notably FFMA — Fred Fish Memorial Award for grid-by-grid 6m work).

## Technical challenges

- **Aircraft electrical interference.** A small aircraft's alternator, ignition system, and electronic instruments generate significant broadband noise. RFI suppression is a major engineering effort.
- **Antenna performance.** Aircraft-mounted antennas have unpredictable patterns (the aircraft skin distorts the radiation pattern). Most signals come off the bottom rather than the sides.
- **Range vs altitude.** A 30,000-foot aircraft has a radio horizon of ~250 statute miles on VHF — much more than ground operation. Balloon payloads at 100,000 feet talk to ~500 miles of ground stations simultaneously.
- **Coordination with ATC.** On HF, voice operation can interfere with aviation HF (which uses 3 MHz and 8 MHz frequencies). Always avoid aviation frequencies. On VHF/UHF, amateur bands don't overlap aviation bands, so this is less of a concern.

## What NOT to do

- Don't operate `/AM` on a commercial flight. Period. Even if it were legal (it isn't, on most carriers), it's a fast way to end up in the cargo bay or in custody.
- Don't run higher power than necessary. 5 W is plenty from 30,000 feet — the path budget is favorable.
- Don't operate near or during aircraft control transmissions. Air traffic controllers don't appreciate amateur traffic on adjacent frequencies.
- Don't fly drones over crowds while keying up. The drone's flight control system must work cleanly while the amateur radio operates — verify on the ground first.

## Comparison to `/MM` (maritime mobile)

`/MM` from a boat is much easier and more common than `/AM` from an aircraft. Compare:

|  | `/MM` (boat) | `/AM` (aircraft) |
|--|-------------|------------------|
| Legality | Common, well-established | Restrictive, rare |
| Antenna real estate | Plenty (insulated backstay, full vertical) | Tiny |
| RFI environment | Manageable | Severe |
| Range | Excellent (saltwater ground) | Excellent (line-of-sight + altitude) |
| ATC/airspace overlap | None | Significant |
| Risk to operator | Low | Higher (pilot distraction) |

See [§30-06 Maritime Mobile](30-06-maritime-mobile.md) for the much more common case.

## See also

- [§30-06 Maritime Mobile Operation](30-06-maritime-mobile.md) — the common-case mobile-from-vessel
- [§30-08 SOTA](30-08-sota.md), [§30-09 POTA](30-09-pota.md) — the much more common portable specialties
- [§22-03 Identifying](../22-operating-practice/22-03-identifying.md) — `/AM` suffix conventions
- FCC §97.11 (aircraft) — read the regulator before operating
