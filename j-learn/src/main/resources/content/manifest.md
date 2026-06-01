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
> **Advanced —** The MUF varies with the Earth's magnetic field
> direction relative to the path because the Appleton-Hartree equation
> depends on the angle between propagation and B…
```

Renderers in **simple mode** elide every blockquote whose first non-whitespace text starts with `**Advanced —**`. **Advanced mode** renders them inline with a distinct visual style. The marker is a literal string, not a regex — keep the spacing and emoji exactly as shown.

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
| 06-01   | Smith Charts                                         | 06-antennas/06-01-smith-charts.md                                 | advanced   |
| 06-02   | Feedline Effects                                     | 06-antennas/06-02-feedline-effects.md                             | mixed      |
| 06-03   | Impedance Transformation                             | 06-antennas/06-03-impedance-transformation.md                     | advanced   |
| 06-04   | Baluns and Chokes                                    | 06-antennas/06-04-baluns-and-chokes.md                            | mixed      |
| 06-05   | Ground-Plane Effects                                 | 06-antennas/06-05-ground-plane-effects.md                         | mixed      |
| 06-06   | Modeling Concepts                                    | 06-antennas/06-06-modeling-concepts.md                            | advanced   |
| 06-07   | Radiation Patterns                                   | 06-antennas/06-07-radiation-patterns.md                           | mixed      |
| 06-08   | Polarization                                         | 06-antennas/06-08-polarization.md                                 | mixed      |
| 06-09   | Diversity                                            | 06-antennas/06-09-diversity.md                                    | mixed      |
| 06-10   | Dipoles                                              | 06-antennas/06-10-dipoles.md                                      | mixed      |
| 06-11   | Inverted V                                           | 06-antennas/06-11-inverted-v.md                                   | mixed      |
| 06-12   | Verticals                                            | 06-antennas/06-12-verticals.md                                    | mixed      |
| 06-13   | End-Fed Half-Wave (EFHW)                             | 06-antennas/06-13-efhw.md                                         | mixed      |
| 06-14   | Magnetic Loops                                       | 06-antennas/06-14-magnetic-loops.md                               | mixed      |
| 06-15   | Full-Wave Loops                                      | 06-antennas/06-15-full-wave-loops.md                              | mixed      |
| 06-16   | Rhombic (Terminated & Unterminated)                  | 06-antennas/06-16-rhombic.md                                      | advanced   |
| 06-17   | Traps                                                | 06-antennas/06-17-traps.md                                        | advanced   |
| 06-18   | Folded Dipole                                        | 06-antennas/06-18-folded-dipole.md                                | mixed      |
| 06-19   | Off-Center-Fed Dipole (OCFD / Windom)                | 06-antennas/06-19-ocfd-windom.md                                  | mixed      |
| 06-20   | G5RV and ZS6BKW                                       | 06-antennas/06-20-g5rv-zs6bkw.md                                  | mixed      |
| 06-21   | Doublet (All-Band Tuned)                             | 06-antennas/06-21-doublet.md                                      | mixed      |
| 06-22   | Fan Dipole (Parallel Dipole)                         | 06-antennas/06-22-fan-dipole.md                                   | mixed      |
| 06-23   | Sloper and Half-Sloper                               | 06-antennas/06-23-sloper.md                                       | mixed      |
| 06-24   | Random-Wire and Non-Resonant Long Wire               | 06-antennas/06-24-random-wire.md                                  | mixed      |
| 06-25   | Inverted-L                                           | 06-antennas/06-25-inverted-l.md                                   | mixed      |
| 06-26   | Linked Dipole                                        | 06-antennas/06-26-linked-dipole.md                                | mixed      |
| 06-27   | Phasing Harnesses & Stacking                         | 06-antennas/06-27-phasing-harnesses.md                            | mixed      |
| 06-28   | Shunt-Fed Towers                                     | 06-antennas/06-28-shunt-fed-towers.md                             | mixed      |
| 06-29   | Phased Arrays & Directivity                          | 06-antennas/06-29-phased-arrays.md                                | mixed      |
| 06-30   | Beams — Yagis, Quads & Log-Periodics                 | 06-antennas/06-30-beams.md                                        | mixed      |
| 06-31   | Receive Antennas — Beverages, Loops & Flags          | 06-antennas/06-31-receive-antennas.md                             | mixed      |
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
| 23-00   | HF Operating Techniques — Overview                   | 23-hf-operating/23-00-overview.md                                 | simple     |
| 23-01   | DXing                                                | 23-hf-operating/23-01-dxing.md                                    | mixed      |
| 23-02   | Contesting Basics                                    | 23-hf-operating/23-02-contesting-basics.md                        | simple     |
| 23-03   | Run vs. Search-and-Pounce                            | 23-hf-operating/23-03-run-vs-sp.md                                | mixed      |
| 23-04   | Tail-ending                                          | 23-hf-operating/23-04-tail-ending.md                              | simple     |
| 23-05   | Working Rare DX                                      | 23-hf-operating/23-05-working-rare-dx.md                          | mixed      |
| 23-06   | Timing                                               | 23-hf-operating/23-06-timing.md                                   | mixed      |
| 23-07   | Grayline Exploitation                                | 23-hf-operating/23-07-grayline-exploitation.md                    | mixed      |
| 23-08   | Regional Propagation Quirks                          | 23-hf-operating/23-08-regional-propagation-quirks.md              | mixed      |
| 23-09   | Pile-up Strategy                                     | 23-hf-operating/23-09-pile-up-strategy.md                         | mixed      |
| 23-10   | Split Tactics                                        | 23-hf-operating/23-10-split-tactics.md                            | mixed      |
| 23-11   | HF Mobile                                            | 23-hf-operating/23-11-hf-mobile.md                                | mixed      |
| 23-12   | HF Portable                                          | 23-hf-operating/23-12-hf-portable.md                              | mixed      |
| 24-00   | Digital Voice & Hotspot Systems — Overview           | 24-digital-voice-hotspots/24-00-overview.md                       | simple     |
| 24-01   | DMR Overview                                         | 24-digital-voice-hotspots/24-01-dmr-overview.md                   | mixed      |
| 24-02   | DMR Talkgroups                                       | 24-digital-voice-hotspots/24-02-dmr-talkgroups.md                 | mixed      |
| 24-03   | BrandMeister vs IPSC2                                | 24-digital-voice-hotspots/24-03-brandmeister-vs-ipsc2.md          | mixed      |
| 24-04   | D-STAR                                               | 24-digital-voice-hotspots/24-04-dstar.md                          | mixed      |
| 24-05   | D-STAR Routing — Reflectors and Callsign Routing     | 24-digital-voice-hotspots/24-05-dstar-routing.md                  | mixed      |
| 24-06   | Yaesu System Fusion / WIRES-X                        | 24-digital-voice-hotspots/24-06-fusion-wires-x.md                 | mixed      |
| 24-07   | Pi-Star Hotspot Setup                                | 24-digital-voice-hotspots/24-07-hotspot-pistar.md                 | mixed      |
| 24-08   | OpenSpot Hotspots                                    | 24-digital-voice-hotspots/24-08-hotspot-openspot.md               | mixed      |
| 24-09   | Duplex vs Simplex Hotspots                           | 24-digital-voice-hotspots/24-09-duplex-vs-simplex-hotspots.md     | mixed      |
| 24-10   | BER (Bit Error Rate) Explained                       | 24-digital-voice-hotspots/24-10-ber-explained.md                  | mixed      |
| 24-11   | Cross-Mode Linking                                   | 24-digital-voice-hotspots/24-11-cross-mode-linking.md             | mixed      |
| 25-00   | Test Equipment & Measurement — Overview              | 25-test-equipment/25-00-overview.md                               | mixed      |
| 25-01   | NanoVNA — Advanced Techniques                        | 25-test-equipment/25-01-nanovna-advanced.md                       | advanced   |
| 25-02   | Oscilloscopes for RF Work                            | 25-test-equipment/25-02-oscilloscopes-for-rf.md                   | mixed      |
| 25-03   | Spectrum Analyzers                                   | 25-test-equipment/25-03-spectrum-analyzers.md                     | mixed      |
| 25-04   | Tracking Generators                                  | 25-test-equipment/25-04-tracking-generators.md                    | mixed      |
| 25-05   | Field Strength Meters                                | 25-test-equipment/25-05-field-strength-meters.md                  | mixed      |
| 25-06   | Power & SWR Meters                                   | 25-test-equipment/25-06-power-swr-meters.md                       | mixed      |
| 25-07   | TDR — Time-Domain Reflectometry                      | 25-test-equipment/25-07-tdr.md                                    | advanced   |
| 25-08   | Measuring Station Noise Floor                        | 25-test-equipment/25-08-noise-floor-measurement.md                | mixed      |
| 25-09   | Calibration Workflows                                | 25-test-equipment/25-09-calibration-workflows.md                  | mixed      |
| 25-10   | Antenna Analyzers                                    | 25-test-equipment/25-10-antenna-analyzers.md                      | mixed      |
| 25-11   | Dip Meters                                           | 25-test-equipment/25-11-dip-meters.md                             | mixed      |
| 26-00   | Homebrewing & RF Construction — Overview             | 26-homebrewing/26-00-overview.md                                  | simple     |
| 26-01   | RF Amplifier Topologies                              | 26-homebrewing/26-01-rf-amplifier-topologies.md                   | advanced   |
| 26-02   | Low-Pass Filters for Harmonic Suppression            | 26-homebrewing/26-02-low-pass-filters.md                          | mixed      |
| 26-03   | High-Pass Filters                                    | 26-homebrewing/26-03-high-pass-filters.md                         | mixed      |
| 26-04   | Bandpass & Notch Filters                             | 26-homebrewing/26-04-bandpass-notch-filters.md                    | mixed      |
| 26-05   | Toroid Selection (Powdered Iron)                     | 26-homebrewing/26-05-toroid-selection.md                          | mixed      |
| 26-06   | Ferrite Mix Selection                                | 26-homebrewing/26-06-ferrite-mix-selection.md                     | mixed      |
| 26-07   | Linear vs Switching Power Supplies                   | 26-homebrewing/26-07-linear-vs-switching-supplies.md              | mixed      |
| 26-08   | Enclosures & Shielding                               | 26-homebrewing/26-08-enclosures-shielding.md                      | mixed      |
| 26-09   | Grounding for Homebrew                               | 26-homebrewing/26-09-grounding-for-homebrew.md                    | mixed      |
| 26-10   | PCB Layout Basics for RF                             | 26-homebrewing/26-10-pcb-layout-basics.md                         | mixed      |
| 26-11   | RF Safety in Homebrew                                | 26-homebrewing/26-11-rf-safety-in-homebrew.md                     | mixed      |
| 27-00   | Overview — Station Engineering & Grounding           | 27-station-engineering/27-00-overview.md                          | mixed      |
| 27-01   | Single-Point Grounding                               | 27-station-engineering/27-01-single-point-grounding.md            | mixed      |
| 27-02   | RF Bonding                                           | 27-station-engineering/27-02-rf-bonding.md                        | mixed      |
| 27-03   | Lightning Protection                                 | 27-station-engineering/27-03-lightning-protection.md              | mixed      |
| 27-04   | Station Layout                                       | 27-station-engineering/27-04-station-layout.md                    | simple     |
| 27-05   | Ferrite Deployment Strategy                          | 27-station-engineering/27-05-ferrite-deployment-strategy.md       | mixed      |
| 27-06   | Power Distribution                                   | 27-station-engineering/27-06-power-distribution.md                | mixed      |
| 27-07   | Portable Power — LiFePO4                             | 27-station-engineering/27-07-portable-power-lifepo4.md            | mixed      |
| 27-08   | Portable Power — Solar                               | 27-station-engineering/27-08-portable-power-solar.md              | mixed      |
| 27-09   | Noise Mitigation at the Power Supply                 | 27-station-engineering/27-09-noise-mitigation-at-power-supply.md  | mixed      |
| 28-00   | Overview — Additional Digital Modes                  | 28-additional-digital-modes/28-00-overview.md                     | simple     |
| 28-01   | Winlink                                              | 28-additional-digital-modes/28-01-winlink.md                      | mixed      |
| 28-02   | VARA HF                                              | 28-additional-digital-modes/28-02-vara-hf.md                      | mixed      |
| 28-03   | VARA FM                                              | 28-additional-digital-modes/28-03-vara-fm.md                      | simple     |
| 28-04   | Olivia                                               | 28-additional-digital-modes/28-04-olivia.md                       | mixed      |
| 28-05   | MFSK16 and MFSK32                                    | 28-additional-digital-modes/28-05-mfsk.md                         | mixed      |
| 28-06   | Hellschreiber                                        | 28-additional-digital-modes/28-06-hellschreiber.md                | mixed      |
| 28-07   | Pactor                                               | 28-additional-digital-modes/28-07-pactor.md                       | advanced   |
| 28-08   | FSQ — Fast Simple QSO                                | 28-additional-digital-modes/28-08-fsq.md                          | mixed      |
| 28-09   | Robust Packet                                        | 28-additional-digital-modes/28-09-robust-packet.md                | mixed      |
| 28-10   | Digital Messaging Workflows                          | 28-additional-digital-modes/28-10-digital-messaging-workflows.md  | mixed      |
| 29-00   | Satellite Advanced Topics                            | 29-satellite-advanced/29-00-overview.md                           | mixed      |
| 29-01   | Full-Duplex Operation                                | 29-satellite-advanced/29-01-full-duplex.md                        | mixed      |
| 29-02   | Linear Transponder Etiquette                         | 29-satellite-advanced/29-02-transponder-etiquette.md              | mixed      |
| 29-03   | Arrow Handheld Yagi                                  | 29-satellite-advanced/29-03-antennas-arrow.md                     | mixed      |
| 29-04   | Eggbeater Omnidirectional Antennas                   | 29-satellite-advanced/29-04-antennas-eggbeater-omni.md            | mixed      |
| 29-05   | Helical Antennas                                     | 29-satellite-advanced/29-05-antennas-helical.md                   | mixed      |
| 29-06   | Polarization Switching                               | 29-satellite-advanced/29-06-polarization-switching.md             | advanced   |
| 29-07   | Mast-Mounted Preamps                                 | 29-satellite-advanced/29-07-preamps-lna-placement.md              | mixed      |
| 29-08   | Doppler Automation                                   | 29-satellite-advanced/29-08-doppler-automation.md                 | advanced   |
| 29-09   | Linear Transponder Strategy                          | 29-satellite-advanced/29-09-linear-transponder-strategy.md        | advanced   |
| 29-10   | Mode Identification (V/U, U/V, L/U, etc.)            | 29-satellite-advanced/29-10-mode-identification.md                | mixed      |
| 30-00   | Overview — Operating Specialties                     | 30-operating-specialties/30-00-overview.md                        | mixed      |
| 30-01   | NVIS — Near-Vertical-Incidence Skywave               | 30-operating-specialties/30-01-nvis.md                            | mixed      |
| 30-02   | Meteor Scatter Operating                             | 30-operating-specialties/30-02-meteor-scatter.md                  | advanced   |
| 30-03   | EME — Earth-Moon-Earth Basics                        | 30-operating-specialties/30-03-eme-basics.md                      | advanced   |
| 30-04   | Tropospheric Ducting                                 | 30-operating-specialties/30-04-tropo-ducting.md                   | mixed      |
| 30-05   | Aircraft Scatter                                     | 30-operating-specialties/30-05-aircraft-scatter.md                | mixed      |
| 30-06   | Maritime Mobile Operating                            | 30-operating-specialties/30-06-maritime-mobile.md                 | mixed      |
| 30-07   | Aeronautical Mobile Operation                        | 30-operating-specialties/30-07-aeronautical-mobile.md             | advanced   |
| 30-08   | SOTA — Summits On The Air                            | 30-operating-specialties/30-08-sota.md                            | simple     |
| 30-09   | POTA — Parks On The Air                              | 30-operating-specialties/30-09-pota.md                            | simple     |
