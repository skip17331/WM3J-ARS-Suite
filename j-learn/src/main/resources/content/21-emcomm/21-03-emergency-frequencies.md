---
id: 21-03
title: Emergency Frequencies & Major Nets
chapter: 21
section: 03
level: simple
status: draft
---

# Emergency Frequencies & Major Nets

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

This section catalogs the **major U.S. amateur emergency communication nets** — long-running organized nets that activate during specific emergencies (hurricanes, maritime distress, severe weather) or operate continuously as standing emergency-response capability. It also covers conventional **calling frequencies** that operators monitor or use during emergencies even outside formal nets.

Net frequencies and times can change; the values below reflect long-established conventions as of the 2020s. Always verify current operating schedule with the net's web page or the net manager before relying on it for an active operation.

## Hurricane Watch Net (HWN)

- **Frequency**: 14.325 MHz USB (primary); 7.268 MHz LSB (backup nighttime).
- **Mission**: Gather and relay observed weather reports and damage information from amateur stations in hurricane-affected areas to the National Hurricane Center.
- **Activation**: Whenever a hurricane is within 300 miles of populated land in the Atlantic basin, eastern Pacific, or Gulf of Mexico.
- **Operations**: Net Control runs continuously during activation, often 12+ hours per day. Stations within the storm's path provide real-time reports — wind speed, barometric pressure, storm conditions, damage observations.
- **Reports go to**: WX4NHC, the amateur radio station at the National Hurricane Center in Miami; from there, into the official forecast products.

The Hurricane Watch Net has been operating since 1965. During major storms (Hurricane Andrew, Katrina, Maria, Helene, etc.), it has been a critical real-time source of ground-truth weather data when official monitoring infrastructure was disrupted.

## Maritime Mobile Service Net (MMSN)

- **Frequency**: 14.300 MHz USB.
- **Mission**: Health and welfare traffic for maritime-mobile amateurs (boats), emergency relay, and weather information for vessels at sea.
- **Operating hours**: Daily, 1600-0200 UTC (continuously).
- **Use case**: A boat at sea with no other communication can call into MMSN for medical emergency, mechanical breakdown assistance, or simply check-in to let family know they're safe.

The 14.300 frequency is **kept clear** for MMSN traffic; even outside its formal hours, it's by convention not used for casual contesting or general operating in deference to maritime emergency potential.

## SATERN (Salvation Army Team Emergency Radio Network)

- **Frequency**: 14.265 MHz USB (international); regional repeaters varies.
- **Mission**: Health and welfare traffic during disasters; coordinated with Salvation Army disaster response.
- **Activation**: During major disasters — hurricanes, earthquakes, large-scale events.
- **Operations**: Coordinated by SATERN net coordinators; works in conjunction with ARES, RACES, and other emergency networks.

SATERN has been particularly active during international disasters (Haiti 2010 earthquake, etc.) where Salvation Army personnel are deployed.

## 7290 Traffic Net (Tony McCorkle Memorial Net)

- **Frequency**: 7.290 MHz LSB.
- **Operating hours**: Daily 0001-0400 UTC and continuously during emergencies.
- **Mission**: NTS-style traffic handling, plus emergency activation as needed.
- **Notable**: One of the longest-running daily HF amateur nets in the U.S.

The 7.290 frequency is widely monitored; many amateurs check it during severe weather even when not formally activated.

## Salvation Army Maritime Net & related

In addition to SATERN, several other Salvation Army-coordinated nets operate during specific events. Frequencies vary; check sat-internal.org or current SATERN documentation.

## ARES regional nets

Each ARRL Section operates its own ARES nets — typically:

- **A daily section traffic net** on a regional repeater or HF frequency.
- **An emergency-activation net** that goes live during declared events.
- **Multiple county-level nets** below the section level.

Specific frequencies vary by section. To find your local section's nets, consult the ARRL's Section Manager listing (arrl.org/sections) and contact the Section Emergency Coordinator.

## RACES nets

Similar pattern to ARES — each state and county has its own RACES schedule. Frequencies are typically published by the responsible civil-defense agency (county OEM, state OES). Many RACES nets share frequencies with ARES nets in the same area.

## Skywarn nets

**Skywarn** is the National Weather Service's network of trained spotters reporting severe weather observations. Many Skywarn nets activate via amateur radio during severe weather:

- **Local NWS office** activates the net on a regional repeater or HF frequency.
- **Trained Skywarn-certified amateurs** report in observations: hail size, wind damage, tornado sightings, flooding.
- **NWS forecasters** use the reports to confirm radar indications and issue warnings.

The exact frequencies vary by NWS office. Skywarn training (free, 2-hour class) is offered by NWS regularly; certification is required to formally report into Skywarn nets.

## VHF/UHF emergency calling frequencies

For local FM voice communications during an emergency:

| Frequency | Mode | Use |
|-----------|------|-----|
| **146.520 MHz** | FM simplex | National FM simplex calling — universal "no repeater" emergency frequency |
| **446.000 MHz** | FM simplex | National 70 cm FM simplex calling |
| **52.525 MHz** | FM simplex | National 6 m FM simplex calling |
| Regional repeaters | FM | Each metro area has 2-5 widely-monitored repeaters; many are coordinated for emergency use |

If you need to call for assistance during an emergency and you don't know what frequency the local emcomm network is on, **start at 146.520** — it's monitored by many operators continuously. Many amateurs program 146.520 into "scan" mode on their HT and respond to traffic on it.

## HF emergency-monitoring frequencies

Beyond the dedicated nets, several HF frequencies are widely monitored for emergency calls:

| Frequency | Use |
|-----------|-----|
| 14.300 MHz USB | MMSN (always); emergency calling outside formal MMSN hours |
| 14.265 MHz USB | SATERN; emergency calling |
| 7.268 MHz LSB | HWN backup |
| 3.965 MHz LSB | Common East Coast emergency frequency (varies by region) |
| 3.873 MHz LSB | Common West Coast emergency frequency |

If you have working HF and need to make an emergency call without knowing local conventions, **calling on 14.300 or 7.268 will reach someone**.

## SHARES (HF emergency federal network)

**SHARES** (Shared Resources High Frequency Radio Program) is a federal program operated by DHS for inter-agency HF backup communications. SHARES uses non-amateur HF frequencies (some adjacent to amateur bands) and is typically not directly accessible to amateur operators except through MARS or specifically-authorized agreements.

SHARES exists in case all federal communications fail; it's a tier above amateur emcomm in scope but conceptually similar — a backup HF network for critical communications.

## Activation triggers for major nets

| Net | Triggered by |
|-----|--------------|
| HWN | Hurricane within 300 mi of land |
| SATERN | Major disaster requiring Salvation Army response |
| MMSN | Maritime distress; vessels in difficulty |
| 7290 Traffic | NTS daily; emergency activation as called |
| Local ARES/RACES | Local agency request; section EC decision |
| Skywarn | NWS local-office decision based on severe weather threat |

You can monitor any of these nets by tuning to their frequency at the right time. During an active event, listening for 5-10 minutes will reveal what's happening and whether check-ins from your area are being requested.

## Operating during a net activation

When you join a formal emergency net:

1. **Listen first.** Determine current net state and what's being requested.
2. **Wait for "call for check-ins"** or specific request for your area.
3. **Identify briefly**: callsign + grid + status ("WM3J in FM19, no traffic, ready to assist").
4. **Take direction from Net Control.** Don't volunteer information they didn't ask for.
5. **Pass traffic when assigned**: typically using ICS-213 form or NTS radiogram format.
6. **Stay on frequency** unless released; emergency nets have priority over other operations.

## Common emergency-frequency mistakes

- **Tuning to a frequency without listening first.** Don't transmit on top of an active net or QSO.
- **Joining without need.** If you have nothing to add, listening is sufficient. Don't clog the net with check-ins of "just monitoring."
- **Using emergency frequencies for casual contacts during normal times.** 14.300 is OK for ragchew when MMSN isn't active, but the convention is to keep it relatively clear in case maritime traffic comes in.
- **Failing to coordinate with local emergency networks.** Joining HWN to report from your back yard during a storm is fine if you're observing weather; reporting "everything is calm here" doesn't help. Report what's actually happening.
- **Operating non-emergency traffic on an active net frequency.** During a Hurricane Watch activation, 14.325 is dedicated to that activation; casual ragchews there are inappropriate.

## See also

- §21-00 — Chapter overview
- §21-01 — NTS (the long-distance traffic-handling system)
- §21-02 — ICS basics
- §21-04 — Message forms
- §21-05 — Operating procedures
- §04 — Repeaters & bandplans (where local emcomm nets often operate)
- §20-01 — HF band plan (legal allocations)
