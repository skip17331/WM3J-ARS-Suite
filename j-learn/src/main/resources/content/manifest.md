# J-Learn Master Manifest

This file is the machine-parseable index for every section in `~/j-learn/src/main/resources/content/`. The Java loader (`ContentManifest.load()`) reads it once at startup and builds the navigation tree from the table rows in the **Section Index** below. Anything outside the rows of the section-index table is documentation for humans and is ignored by the parser.

---

## Front-matter schema

Every content file (`*.md` under this directory, excluding this manifest) starts with a YAML front-matter block:

```yaml
---
id: 01-01                     # chapter-NN-section-NN, zero-padded
title: Solar Indices …        # human-readable section title
chapter: 01                   # chapter number, zero-padded
section: 01                   # section number within the chapter, zero-padded
level: mixed                  # one of: simple | advanced | mixed
status: stub                  # one of: stub | draft | review | published
---
```

**Field rules**

| Field    | Allowed values                                       | Notes |
|----------|------------------------------------------------------|-------|
| `id`     | `NN-NN` (chapter-section)                            | Must be unique. Matches the row in the Section Index below. |
| `title`  | free text                                            | Single line. No leading/trailing whitespace. |
| `chapter`| `NN` zero-padded                                     | Same as the directory prefix. |
| `section`| `NN` zero-padded                                     | `00` is reserved for the chapter overview. |
| `level`  | `simple` / `advanced` / `mixed`                      | Drives default rendering. `mixed` files use inline callouts. |
| `status` | `stub` / `draft` / `review` / `published`            | Set to `stub` until first content is written. |

---

## Simple vs Advanced callout convention

Sections are tagged at the *file* level by the `level` field. Within `mixed` files, individual paragraphs intended for Extra-class / engineering depth are marked with a markdown blockquote that begins with the literal callout marker:

```markdown
> ⚙️ **Advanced —** The MUF varies with the Earth's magnetic field
> direction relative to the path because the Appleton-Hartree equation
> depends on the angle between propagation and B…
```

Renderers in **simple mode** elide every blockquote whose first non-whitespace text starts with `⚙️ **Advanced —**`. **Advanced mode** renders them inline with a distinct visual style. The marker is a literal string, not a regex — keep the spacing and emoji exactly as shown.

---

## ID numbering scheme

- Chapter directories are named `NN-slug/` with two-digit zero-padded chapter numbers (`01-propagation/`, `26-emcomm/`).
- Inside a chapter, files are named `NN-NN-slug.md`. The first `NN` is the chapter, the second is the section. Section `00` is always the chapter overview.
- IDs in front matter and in the index table use the dash-separated `NN-NN` form (e.g. `01-01`).
- Adding a new section: append it to the next free `NN` in its chapter, drop the file in the right folder, and add one row to the table below. Do not renumber existing files — IDs are stable across releases so reading-state pointers don't go stale.

---

## Section index

The table is grouped by Part (I–V) for human readability. The Java loader reads every row whose `id` cell starts with two digits, a dash, and two more digits, and ignores all other lines. Columns are pipe-separated; a header row precedes each Part block.

### Part I — Foundations & Primers

| id    | title                                              | path                                                            | level    |
|-------|----------------------------------------------------|-----------------------------------------------------------------|----------|
| 00-00   | README — what J-Learn is                             | 00-about/00-00-readme.md                                          | simple     |
| 00-01   | How to read this library                             | 00-about/00-01-how-to-read.md                                     | simple     |
| 01-00   | Propagation — Overview                               | 01-propagation/01-00-overview.md                                  | simple     |
| 01-01   | Solar Indices (SFI, A-index, K-index)                | 01-propagation/01-01-solar-indices.md                             | mixed      |
| 01-02   | MUF and LUF                                          | 01-propagation/01-02-muf-luf.md                                   | mixed      |
| 01-03   | Greyline Propagation                                 | 01-propagation/01-03-greyline.md                                  | simple     |
| 01-04   | Ionospheric Layers (D / E / F1 / F2)                 | 01-propagation/01-04-ionospheric-layers.md                        | mixed      |
| 01-05   | Solar Cycle                                          | 01-propagation/01-05-solar-cycle.md                               | mixed      |
| 01-06   | Sporadic E, TEP, Skip                                | 01-propagation/01-06-sporadic-e-tep-skip.md                       | mixed      |
| 01-07   | Prediction Models                                    | 01-propagation/01-07-prediction-models.md                         | advanced   |
| 01-08   | Band Choice Right Now                                | 01-propagation/01-08-band-choice-now.md                           | simple     |
| 01-09   | Weak-Signal VHF/UHF (Tropo, EME, Meteor, Aurora)     | 01-propagation/01-09-weak-signal.md                               | mixed      |
| 02-00   | Voice Modes — Overview                               | 02-voice-modes/02-00-overview.md                                  | simple     |
| 02-01   | AM — Amplitude Modulation                            | 02-voice-modes/02-01-am.md                                        | mixed      |
| 02-02   | FM — Frequency Modulation                            | 02-voice-modes/02-02-fm.md                                        | mixed      |
| 02-03   | SSB — Single Sideband                                | 02-voice-modes/02-03-ssb.md                                       | mixed      |
| 03-00   | Digital Modes — Overview                             | 03-digital-modes/03-00-overview.md                                | simple     |
| 03-01   | FT8 / FT4                                            | 03-digital-modes/03-01-ft8-ft4.md                                 | mixed      |
| 03-02   | RTTY                                                 | 03-digital-modes/03-02-rtty.md                                    | mixed      |
| 03-03   | PSK31                                                | 03-digital-modes/03-03-psk31.md                                   | mixed      |
| 03-04   | JS8Call                                              | 03-digital-modes/03-04-js8call.md                                 | simple     |
| 03-05   | APRS                                                 | 03-digital-modes/03-05-aprs.md                                    | simple     |
| 03-06   | Packet                                               | 03-digital-modes/03-06-packet.md                                  | mixed      |
| 04-00   | Repeaters & Bandplans — Overview                     | 04-repeaters-bandplans/04-00-overview.md                          | simple     |
| 04-01   | What is a Repeater                                   | 04-repeaters-bandplans/04-01-what-is-a-repeater.md                | simple     |
| 04-02   | Offsets, Tones, CTCSS, DCS                           | 04-repeaters-bandplans/04-02-offsets-tones-ctcss-dcs.md           | simple     |
| 04-03   | Band Plans (HF / VHF / UHF)                          | 04-repeaters-bandplans/04-03-band-plans.md                        | mixed      |
| 04-04   | Simplex Calling Frequencies                          | 04-repeaters-bandplans/04-04-simplex-calling-freqs.md             | simple     |
| 04-05   | Linked Systems (AllStar / DMR / Fusion / D-STAR)     | 04-repeaters-bandplans/04-05-linked-systems.md                    | mixed      |
| 04-06   | Duplexers                                            | 04-repeaters-bandplans/04-06-duplexers.md                         | advanced   |
| 04-07   | Frequency Coordination                               | 04-repeaters-bandplans/04-07-frequency-coordination.md            | simple     |
| 04-08   | Custom Offset Calculator                             | 04-repeaters-bandplans/04-08-custom-offset-calc.md                | simple     |
| 04-09   | Where to Find Repeater Frequencies                   | 04-repeaters-bandplans/04-09-finding-repeaters.md                 | simple     |
| 05-00   | Morse — Overview                                     | 05-morse/05-00-overview.md                                        | simple     |
| 05-01   | Koch Method                                          | 05-morse/05-01-koch-method.md                                     | simple     |
| 05-02   | Farnsworth Spacing                                   | 05-morse/05-02-farnsworth-spacing.md                              | simple     |
| 05-03   | Character Groups                                     | 05-morse/05-03-character-groups.md                                | simple     |
| 05-04   | Words and Callsigns                                  | 05-morse/05-04-words-and-callsigns.md                             | simple     |
| 05-05   | QSO Simulation                                       | 05-morse/05-05-qso-simulation.md                                  | simple     |
| 05-06   | Send Practice                                        | 05-morse/05-06-send-practice.md                                   | simple     |
| 05-07   | Speed Tracking                                       | 05-morse/05-07-speed-tracking.md                                  | simple     |
| 05-08   | Mini Tests                                           | 05-morse/05-08-mini-tests.md                                      | simple     |
| 05-09   | Hardware Keyer Builds                                | 05-morse/05-09-hardware-keyer-builds.md                           | simple     |
| 06-00   | Antennas — Overview                                  | 06-antennas/06-00-overview.md                                     | simple     |
| 06-01   | Dipoles                                              | 06-antennas/06-01-dipoles.md                                      | mixed      |
| 06-02   | Inverted V                                           | 06-antennas/06-02-inverted-v.md                                   | mixed      |
| 06-03   | Verticals                                            | 06-antennas/06-03-verticals.md                                    | mixed      |
| 06-04   | End-Fed Half-Wave (EFHW)                             | 06-antennas/06-04-efhw.md                                         | mixed      |
| 06-05   | Magnetic Loops                                       | 06-antennas/06-05-magnetic-loops.md                               | mixed      |
| 06-06   | Full-Wave Loops                                      | 06-antennas/06-06-full-wave-loops.md                              | mixed      |
| 06-07   | Rhombic (Terminated & Unterminated)                  | 06-antennas/06-07-rhombic.md                                      | advanced   |
| 06-08   | Traps                                                | 06-antennas/06-08-traps.md                                        | advanced   |
| 06-09   | Smith Charts                                         | 06-antennas/06-09-smith-charts.md                                 | advanced   |
| 06-10   | Feedline Effects                                     | 06-antennas/06-10-feedline-effects.md                             | mixed      |
| 06-11   | Impedance Transformation                             | 06-antennas/06-11-impedance-transformation.md                     | advanced   |
| 06-12   | Baluns and Chokes                                    | 06-antennas/06-12-baluns-and-chokes.md                            | mixed      |
| 06-13   | Ground-Plane Effects                                 | 06-antennas/06-13-ground-plane-effects.md                         | mixed      |
| 06-14   | Modeling Concepts                                    | 06-antennas/06-14-modeling-concepts.md                            | advanced   |
| 06-15   | Radiation Patterns                                   | 06-antennas/06-15-radiation-patterns.md                           | mixed      |
| 06-16   | Polarization                                         | 06-antennas/06-16-polarization.md                                 | mixed      |
| 06-17   | Diversity                                            | 06-antennas/06-17-diversity.md                                    | mixed      |
| 07-00   | Satellites — Overview                                | 07-satellites/07-00-overview.md                                   | simple     |
| 07-01   | FM vs Linear                                         | 07-satellites/07-01-fm-vs-linear.md                               | simple     |
| 07-02   | Doppler Shift                                        | 07-satellites/07-02-doppler-shift.md                              | mixed      |
| 07-03   | Keplerian Elements                                   | 07-satellites/07-03-keplerian-elements.md                         | advanced   |
| 07-04   | Tracking Strategies                                  | 07-satellites/07-04-tracking-strategies.md                        | simple     |
| 07-05   | ISS Packet & APRS                                    | 07-satellites/07-05-iss-packet-aprs.md                            | simple     |
| 07-06   | Footprints                                           | 07-satellites/07-06-footprints.md                                 | simple     |
| 07-07   | Pass Prediction                                      | 07-satellites/07-07-pass-prediction.md                            | simple     |
| 07-08   | Doppler Correction Tables                            | 07-satellites/07-08-doppler-correction-tables.md                  | advanced   |
| 08-00   | RF Safety — Overview                                 | 08-rf-safety/08-00-overview.md                                    | simple     |
| 08-01   | FCC Rules                                            | 08-rf-safety/08-01-fcc-rules.md                                   | simple     |
| 08-02   | MPE Limits                                           | 08-rf-safety/08-02-mpe-limits.md                                  | mixed      |
| 08-03   | Controlled vs Uncontrolled                           | 08-rf-safety/08-03-controlled-vs-uncontrolled.md                  | simple     |
| 08-04   | Duty Cycle                                           | 08-rf-safety/08-04-duty-cycle.md                                  | mixed      |
| 08-05   | ERP                                                  | 08-rf-safety/08-05-erp.md                                         | mixed      |
| 08-06   | Safe Antenna Placement                               | 08-rf-safety/08-06-safe-antenna-placement.md                      | simple     |
| 08-07   | RF Burns                                             | 08-rf-safety/08-07-rf-burns.md                                    | simple     |
| 09-00   | Antenna Workshop — Overview                          | 09-antenna-calc/09-00-overview.md                                 | simple     |
| 09-01   | Antenna Recommender (Questionnaire)                  | 09-antenna-calc/09-01-recommender.md                              | simple     |
| 09-02   | Flat Dipole                                          | 09-antenna-calc/09-02-flat-dipole.md                              | simple     |
| 09-03   | Inverted-V Dipole                                    | 09-antenna-calc/09-03-inverted-v-dipole.md                        | simple     |
| 09-04   | Fan Dipole                                           | 09-antenna-calc/09-04-fan-dipole.md                               | mixed      |
| 09-05   | Trapped Dipole                                       | 09-antenna-calc/09-05-trapped-dipole.md                           | mixed      |
| 09-06   | OCF Dipole (Windom)                                  | 09-antenna-calc/09-06-ocf-dipole.md                               | mixed      |
| 09-07   | EFHW (No Traps)                                      | 09-antenna-calc/09-07-efhw-no-traps.md                            | mixed      |
| 09-08   | EFHW (Trapped)                                       | 09-antenna-calc/09-08-efhw-trapped.md                             | mixed      |
| 09-09   | J-Pole                                               | 09-antenna-calc/09-09-j-pole.md                                   | simple     |
| 09-10   | Yagi-Uda                                             | 09-antenna-calc/09-10-yagi-uda.md                                 | mixed      |
| 09-11   | Vertical Antennas                                    | 09-antenna-calc/09-11-vertical.md                                 | mixed      |
| 09-12   | Loading for Shortened Antennas                       | 09-antenna-calc/09-12-loading-coils.md                            | mixed      |
| 09-13   | Trap Design & Manufacturing                          | 09-antenna-calc/09-13-trap-design.md                              | advanced   |
| 09-14   | Magnetic Loop                                        | 09-antenna-calc/09-14-magnetic-loop.md                            | advanced   |
| 09-15   | NanoVNA Trim Workflow                                | 09-antenna-calc/09-15-nanovna-trim.md                             | simple     |
| 10-00   | Feedline & SWR — Overview                            | 10-feedline-swr/10-00-overview.md                                 | simple     |
| 10-01   | Coax Loss by Frequency                               | 10-feedline-swr/10-01-coax-loss-by-frequency.md                   | mixed      |
| 10-02   | SWR & Reflected Power                                | 10-feedline-swr/10-02-swr-reflected-power.md                      | mixed      |
| 10-03   | Mismatch Loss                                        | 10-feedline-swr/10-03-mismatch-loss.md                            | mixed      |
| 10-04   | Power Delivered vs Lost                              | 10-feedline-swr/10-04-power-delivered-vs-lost.md                  | mixed      |
| 10-05   | Velocity Factor                                      | 10-feedline-swr/10-05-velocity-factor.md                          | mixed      |
| 10-06   | Impedance Transformation                             | 10-feedline-swr/10-06-impedance-transformation.md                 | advanced   |
| 11-00   | Power Budget & ERP — Overview                        | 11-power-budget-erp/11-00-overview.md                             | simple     |
| 11-01   | TX Power                                             | 11-power-budget-erp/11-01-tx-power.md                             | simple     |
| 11-02   | Feedline Loss                                        | 11-power-budget-erp/11-02-feedline-loss.md                        | mixed      |
| 11-03   | Antenna Gain                                         | 11-power-budget-erp/11-03-antenna-gain.md                         | mixed      |
| 11-04   | ERP Output                                           | 11-power-budget-erp/11-04-erp-output.md                           | mixed      |
| 11-05   | Portable Budget                                      | 11-power-budget-erp/11-05-portable-budget.md                      | simple     |
| 12-00   | High SWR — Overview                                  | 12-high-swr/12-00-overview.md                                     | simple     |
| 12-01   | Coax Issues                                          | 12-high-swr/12-01-coax-issues.md                                  | simple     |
| 12-02   | Connector Issues                                     | 12-high-swr/12-02-connector-issues.md                             | simple     |
| 12-03   | Incorrect Length                                     | 12-high-swr/12-03-incorrect-length.md                             | simple     |
| 12-04   | Nearby Metal                                         | 12-high-swr/12-04-nearby-metal.md                                 | simple     |
| 12-05   | Faulty Balun                                         | 12-high-swr/12-05-faulty-balun.md                                 | simple     |
| 12-06   | Feedline Routing                                     | 12-high-swr/12-06-feedline-routing.md                             | simple     |
| 12-07   | Water Ingress                                        | 12-high-swr/12-07-water-ingress.md                                | simple     |
| 13-00   | Station Troubleshooting — Overview                   | 13-station-troubleshooting/13-00-overview.md                      | simple     |
| 13-01   | No Transmit                                          | 13-station-troubleshooting/13-01-no-transmit.md                   | simple     |
| 13-02   | No Receive                                           | 13-station-troubleshooting/13-02-no-receive.md                    | simple     |
| 13-03   | Distorted Audio                                      | 13-station-troubleshooting/13-03-distorted-audio.md               | simple     |
| 13-04   | RF Feedback                                          | 13-station-troubleshooting/13-04-rf-feedback.md                   | mixed      |
| 13-05   | Grounding                                            | 13-station-troubleshooting/13-05-grounding.md                     | mixed      |
| 13-06   | Power Supply                                         | 13-station-troubleshooting/13-06-power-supply.md                  | simple     |
| 14-00   | RFI — Overview                                       | 14-rfi/14-00-overview.md                                          | simple     |
| 14-01   | What is RFI                                          | 14-rfi/14-01-what-is-rfi.md                                       | simple     |
| 14-02   | Household Sources                                    | 14-rfi/14-02-household-sources.md                                 | simple     |
| 14-03   | Identifying Buzzing                                  | 14-rfi/14-03-identifying-buzzing.md                               | simple     |
| 14-04   | Ferrite Selection                                    | 14-rfi/14-04-ferrite-selection.md                                 | mixed      |
| 14-05   | Isolation Workflow                                   | 14-rfi/14-05-isolation-workflow.md                                | simple     |
| 14-06   | Step-by-Step Elimination                             | 14-rfi/14-06-step-by-step-elimination.md                          | simple     |
| 14-07   | AM Radio Sniffer                                     | 14-rfi/14-07-am-radio-sniffer.md                                  | simple     |
| 14-08   | SDR Waterfall                                        | 14-rfi/14-08-sdr-waterfall.md                                     | simple     |
| 15-00   | Noise Sources — Overview                             | 15-noise-sources/15-00-overview.md                                | simple     |
| 15-01   | Switching Power Supplies                             | 15-noise-sources/15-01-switching-supplies.md                      | simple     |
| 15-02   | LED Lights                                           | 15-noise-sources/15-02-led-lights.md                              | simple     |
| 15-03   | Solar Inverters                                      | 15-noise-sources/15-03-solar-inverters.md                         | simple     |
| 15-04   | Ethernet Over Power                                  | 15-noise-sources/15-04-ethernet-over-power.md                     | simple     |
| 15-05   | HVAC                                                 | 15-noise-sources/15-05-hvac.md                                    | simple     |
| 15-06   | Battery Chargers                                     | 15-noise-sources/15-06-battery-chargers.md                        | simple     |
| 15-07   | Motor Brushes                                        | 15-noise-sources/15-07-motor-brushes.md                           | simple     |
| 15-08   | Power-Line Noise — Overview                          | 15-noise-sources/15-08-overview.md                                | simple     |
| 15-09   | Arcing Insulators                                    | 15-noise-sources/15-09-arcing-insulators.md                       | simple     |
| 15-10   | Bad Transformers                                     | 15-noise-sources/15-10-bad-transformers.md                        | simple     |
| 15-11   | Loose Hardware                                       | 15-noise-sources/15-11-loose-hardware.md                          | simple     |
| 15-12   | Corona Discharge                                     | 15-noise-sources/15-12-corona-discharge.md                        | mixed      |
| 15-13   | AM Radio ID                                          | 15-noise-sources/15-13-am-radio-id.md                             | simple     |
| 15-14   | SDR ID                                               | 15-noise-sources/15-14-sdr-id.md                                  | simple     |
| 15-15   | Utility Documentation                                | 15-noise-sources/15-15-utility-documentation.md                   | simple     |
| 16-00   | Maintenance — Overview                               | 16-maintenance/16-00-overview.md                                  | simple     |
| 16-01   | Battery Maintenance                                  | 16-maintenance/16-01-battery-maintenance.md                       | simple     |
| 16-02   | Firmware Updates                                     | 16-maintenance/16-02-firmware-updates.md                          | simple     |
| 16-03   | Scheduled Inspections                                | 16-maintenance/16-03-scheduled-inspections.md                     | simple     |
| 16-04   | Coax Replacement                                     | 16-maintenance/16-04-coax-replacement.md                          | simple     |
| 16-05   | Tower & Mast Inspection                              | 16-maintenance/16-05-tower-inspection.md                          | mixed      |
| 16-06   | Guy Lines, Turnbuckles, Clamps                       | 16-maintenance/16-06-guy-hardware.md                              | mixed      |
| 16-07   | Ground System Inspection                             | 16-maintenance/16-07-ground-system.md                             | mixed      |
| 16-08   | Coax Inspection                                      | 16-maintenance/16-08-coax-inspection.md                           | simple     |
| 16-09   | Cable Entry & Water Intrusion                        | 16-maintenance/16-09-cable-entry.md                               | simple     |
| 17-00   | Formulas — Overview                                  | 17-formulas/17-00-overview.md                                     | simple     |
| 17-01   | Ohms Law                                             | 17-formulas/17-01-ohms-law.md                                     | simple     |
| 17-02   | Power Law                                            | 17-formulas/17-02-power-law.md                                    | simple     |
| 17-03   | Reactance (Capacitive & Inductive)                   | 17-formulas/17-03-reactance.md                                    | mixed      |
| 17-04   | Impedance                                            | 17-formulas/17-04-impedance.md                                    | mixed      |
| 17-05   | Resonant Frequency                                   | 17-formulas/17-05-resonant-frequency.md                           | mixed      |
| 17-06   | Wavelength                                           | 17-formulas/17-06-wavelength.md                                   | simple     |
| 17-07   | SWR                                                  | 17-formulas/17-07-swr.md                                          | mixed      |
| 17-08   | ERP                                                  | 17-formulas/17-08-erp.md                                          | mixed      |
| 17-09   | Feedline Loss                                        | 17-formulas/17-09-feedline-loss.md                                | mixed      |
| 17-10   | Decibels                                             | 17-formulas/17-10-decibels.md                                     | simple     |
| 17-11   | Q Factor                                             | 17-formulas/17-11-q-factor.md                                     | advanced   |
| 17-12   | Bandwidth                                            | 17-formulas/17-12-bandwidth.md                                    | mixed      |
| 17-13   | Smith Chart Basics                                   | 17-formulas/17-13-smith-chart-basics.md                           | advanced   |
| 17-14   | RF Exposure                                          | 17-formulas/17-14-rf-exposure.md                                  | mixed      |
| 17-15   | Cheat Sheet                                          | 17-formulas/17-15-cheat-sheet.md                                  | simple     |
| 18-00   | Coax & Connectors — Overview                         | 18-coax-connectors/18-00-overview.md                              | simple     |
| 18-01   | Coax Types                                           | 18-coax-connectors/18-01-coax-types.md                            | simple     |
| 18-02   | Loss Tables                                          | 18-coax-connectors/18-02-loss-tables.md                           | mixed      |
| 18-03   | Velocity Factor                                      | 18-coax-connectors/18-03-velocity-factor.md                       | mixed      |
| 18-04   | Connectors                                           | 18-coax-connectors/18-04-connectors.md                            | simple     |
| 18-05   | Baluns & Chokes                                      | 18-coax-connectors/18-05-baluns-chokes.md                         | mixed      |
| 19-00   | Q-Codes & Prosigns — Overview                        | 19-qcodes-prosigns/19-00-overview.md                              | simple     |
| 19-01   | Q-Codes                                              | 19-qcodes-prosigns/19-01-q-codes.md                               | simple     |
| 19-02   | CW Prosigns                                          | 19-qcodes-prosigns/19-02-cw-prosigns.md                           | simple     |
| 19-03   | Abbreviations                                        | 19-qcodes-prosigns/19-03-abbreviations.md                         | simple     |
| 19-04   | Phonetic Alphabet                                    | 19-qcodes-prosigns/19-04-phonetic-alphabet.md                     | simple     |
| 20-00   | Band Plans — Overview                                | 20-band-plans/20-00-overview.md                                   | simple     |
| 20-01   | HF                                                   | 20-band-plans/20-01-hf.md                                         | mixed      |
| 20-02   | VHF                                                  | 20-band-plans/20-02-vhf.md                                        | simple     |
| 20-03   | UHF                                                  | 20-band-plans/20-03-uhf.md                                        | simple     |
| 20-04   | Satellite                                            | 20-band-plans/20-04-satellite.md                                  | simple     |
| 20-05   | Regional Variations                                  | 20-band-plans/20-05-regional-variations.md                        | mixed      |
| 21-00   | Emergency & Public Service Comms — Overview          | 21-emcomm/21-00-overview.md                                       | simple     |
| 21-01   | NTS — National Traffic System                        | 21-emcomm/21-01-nts.md                                            | simple     |
| 21-02   | ICS Basics for Amateur Operators                     | 21-emcomm/21-02-ics-basics.md                                     | simple     |
| 21-03   | Emergency Frequencies & Major Nets                   | 21-emcomm/21-03-emergency-frequencies.md                          | simple     |
| 21-04   | Message Forms                                        | 21-emcomm/21-04-message-forms.md                                  | simple     |
| 21-05   | Operating Procedures                                 | 21-emcomm/21-05-operating-procedures.md                           | simple     |
| 22-00   | Operating Practice — Overview                        | 22-operating-practice/22-00-overview.md                           | simple     |
| 22-01   | Is the Frequency in Use?                             | 22-operating-practice/22-01-frequency-in-use.md                   | simple     |
| 22-02   | Tune-up Etiquette                                    | 22-operating-practice/22-02-tune-up-etiquette.md                  | simple     |
| 22-03   | Identifying — 10-Min Rule and §97.119                | 22-operating-practice/22-03-identifying.md                        | simple     |
| 22-04   | Calling CQ and the Standard QSO Flow                 | 22-operating-practice/22-04-calling-cq.md                         | simple     |
| 22-05   | Pile-up Etiquette                                    | 22-operating-practice/22-05-pile-up-etiquette.md                  | simple     |
| 22-06   | Power Minimum and Polite Operating                   | 22-operating-practice/22-06-power-minimum.md                      | simple     |
| 22-07   | Common Operating Mistakes                            | 22-operating-practice/22-07-common-mistakes.md                    | simple     |
| 22-08   | Split-Frequency Operation                            | 22-operating-practice/22-08-split-frequency.md                    | mixed      |
