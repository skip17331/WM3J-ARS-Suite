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
| 00-00 | README — what J-Learn is                           | 00-about/00-00-readme.md                                        | simple   |
| 00-01 | How to read this library                           | 00-about/00-01-how-to-read.md                                   | simple   |
| 01-00 | Propagation — Overview                             | 01-propagation/01-00-overview.md                                | simple   |
| 01-01 | Solar Indices (SFI, A-index, K-index)              | 01-propagation/01-01-solar-indices.md                           | mixed    |
| 01-02 | MUF and LUF                                        | 01-propagation/01-02-muf-luf.md                                 | mixed    |
| 01-03 | Greyline Propagation                               | 01-propagation/01-03-greyline.md                                | simple   |
| 01-04 | Ionospheric Layers (D / E / F1 / F2)               | 01-propagation/01-04-ionospheric-layers.md                      | mixed    |
| 01-05 | Solar Cycle                                        | 01-propagation/01-05-solar-cycle.md                             | mixed    |
| 01-06 | Sporadic E, TEP, Skip                              | 01-propagation/01-06-sporadic-e-tep-skip.md                     | mixed    |
| 01-07 | Prediction Models                                  | 01-propagation/01-07-prediction-models.md                       | advanced |
| 01-08 | Band Choice Right Now                              | 01-propagation/01-08-band-choice-now.md                         | simple   |
| 02-00 | Repeaters & Bandplans — Overview                   | 02-repeaters-bandplans/02-00-overview.md                        | simple   |
| 02-01 | What is a Repeater                                 | 02-repeaters-bandplans/02-01-what-is-a-repeater.md              | simple   |
| 02-02 | Offsets, Tones, CTCSS, DCS                         | 02-repeaters-bandplans/02-02-offsets-tones-ctcss-dcs.md         | simple   |
| 02-03 | Band Plans (HF / VHF / UHF)                        | 02-repeaters-bandplans/02-03-band-plans.md                      | mixed    |
| 02-04 | Simplex Calling Frequencies                        | 02-repeaters-bandplans/02-04-simplex-calling-freqs.md           | simple   |
| 02-05 | Linked Systems (AllStar / DMR / Fusion / D-STAR)   | 02-repeaters-bandplans/02-05-linked-systems.md                  | mixed    |
| 02-06 | Duplexers                                          | 02-repeaters-bandplans/02-06-duplexers.md                       | advanced |
| 02-07 | Frequency Coordination                             | 02-repeaters-bandplans/02-07-frequency-coordination.md          | simple   |
| 02-08 | Custom Offset Calculator                           | 02-repeaters-bandplans/02-08-custom-offset-calc.md              | simple   |
| 03-00 | Morse — Overview                                   | 03-morse/03-00-overview.md                                      | simple   |
| 03-01 | Koch Method                                        | 03-morse/03-01-koch-method.md                                   | simple   |
| 03-02 | Farnsworth Spacing                                 | 03-morse/03-02-farnsworth-spacing.md                            | simple   |
| 03-03 | Character Groups                                   | 03-morse/03-03-character-groups.md                              | simple   |
| 03-04 | Words and Callsigns                                | 03-morse/03-04-words-and-callsigns.md                           | simple   |
| 03-05 | QSO Simulation                                     | 03-morse/03-05-qso-simulation.md                                | simple   |
| 03-06 | Send Practice                                      | 03-morse/03-06-send-practice.md                                 | simple   |
| 03-07 | Speed Tracking                                     | 03-morse/03-07-speed-tracking.md                                | simple   |
| 03-08 | Mini Tests                                         | 03-morse/03-08-mini-tests.md                                    | simple   |
| 03-09 | Hardware Keyer Builds                              | 03-morse/03-09-hardware-keyer-builds.md                         | simple   |
| 04-00 | Antennas — Overview                                | 04-antennas/04-00-overview.md                                   | simple   |
| 04-01 | Dipoles                                            | 04-antennas/04-01-dipoles.md                                    | mixed    |
| 04-02 | Inverted V                                         | 04-antennas/04-02-inverted-v.md                                 | mixed    |
| 04-03 | Verticals                                          | 04-antennas/04-03-verticals.md                                  | mixed    |
| 04-04 | End-Fed Half-Wave (EFHW)                           | 04-antennas/04-04-efhw.md                                       | mixed    |
| 04-05 | Magnetic Loops                                     | 04-antennas/04-05-magnetic-loops.md                             | mixed    |
| 04-06 | Full-Wave Loops                                    | 04-antennas/04-06-full-wave-loops.md                            | mixed    |
| 04-07 | Rhombic (Terminated & Unterminated)                | 04-antennas/04-07-rhombic.md                                    | advanced |
| 04-08 | Traps                                              | 04-antennas/04-08-traps.md                                      | advanced |
| 04-09 | Smith Charts                                       | 04-antennas/04-09-smith-charts.md                               | advanced |
| 04-10 | Feedline Effects                                   | 04-antennas/04-10-feedline-effects.md                           | mixed    |
| 04-11 | Impedance Transformation                           | 04-antennas/04-11-impedance-transformation.md                   | advanced |
| 04-12 | Baluns and Chokes                                  | 04-antennas/04-12-baluns-and-chokes.md                          | mixed    |
| 04-13 | Ground-Plane Effects                               | 04-antennas/04-13-ground-plane-effects.md                       | mixed    |
| 04-14 | Modeling Concepts                                  | 04-antennas/04-14-modeling-concepts.md                          | advanced |
| 04-15 | Radiation Patterns                                 | 04-antennas/04-15-radiation-patterns.md                         | mixed    |
| 05-00 | Satellites — Overview                              | 05-satellites/05-00-overview.md                                 | simple   |
| 05-01 | FM vs Linear                                       | 05-satellites/05-01-fm-vs-linear.md                             | simple   |
| 05-02 | Doppler Shift                                      | 05-satellites/05-02-doppler-shift.md                            | mixed    |
| 05-03 | Keplerian Elements                                 | 05-satellites/05-03-keplerian-elements.md                       | advanced |
| 05-04 | Tracking Strategies                                | 05-satellites/05-04-tracking-strategies.md                      | simple   |
| 05-05 | ISS Packet & APRS                                  | 05-satellites/05-05-iss-packet-aprs.md                          | simple   |
| 05-06 | Footprints                                         | 05-satellites/05-06-footprints.md                               | simple   |
| 05-07 | Pass Prediction                                    | 05-satellites/05-07-pass-prediction.md                          | simple   |
| 05-08 | Doppler Correction Tables                          | 05-satellites/05-08-doppler-correction-tables.md                | advanced |
| 06-00 | RF Safety — Overview                               | 06-rf-safety/06-00-overview.md                                  | simple   |
| 06-01 | FCC Rules                                          | 06-rf-safety/06-01-fcc-rules.md                                 | simple   |
| 06-02 | MPE Limits                                         | 06-rf-safety/06-02-mpe-limits.md                                | mixed    |
| 06-03 | Controlled vs Uncontrolled                         | 06-rf-safety/06-03-controlled-vs-uncontrolled.md                | simple   |
| 06-04 | Duty Cycle                                         | 06-rf-safety/06-04-duty-cycle.md                                | mixed    |
| 06-05 | ERP                                                | 06-rf-safety/06-05-erp.md                                       | mixed    |
| 06-06 | Safe Antenna Placement                             | 06-rf-safety/06-06-safe-antenna-placement.md                    | simple   |
| 06-07 | RF Burns                                           | 06-rf-safety/06-07-rf-burns.md                                  | simple   |

### Part II — Practical Tools & Calculators

| id    | title                                                 | path                                                          | level    |
|-------|-------------------------------------------------------|---------------------------------------------------------------|----------|
| 07-00 | Antenna Workshop — Overview                           | 07-antenna-calc/07-00-overview.md                             | simple   |
| 07-01 | Antenna Recommender (Questionnaire)                   | 07-antenna-calc/07-01-recommender.md                          | simple   |
| 07-02 | Flat Dipole                                           | 07-antenna-calc/07-02-flat-dipole.md                          | simple   |
| 07-03 | Inverted-V Dipole                                     | 07-antenna-calc/07-03-inverted-v-dipole.md                    | simple   |
| 07-04 | Fan Dipole                                            | 07-antenna-calc/07-04-fan-dipole.md                           | mixed    |
| 07-05 | Trapped Dipole                                        | 07-antenna-calc/07-05-trapped-dipole.md                       | mixed    |
| 07-06 | OCF Dipole (Windom)                                   | 07-antenna-calc/07-06-ocf-dipole.md                           | mixed    |
| 07-07 | EFHW (No Traps)                                       | 07-antenna-calc/07-07-efhw-no-traps.md                        | mixed    |
| 07-08 | EFHW (Trapped)                                        | 07-antenna-calc/07-08-efhw-trapped.md                         | mixed    |
| 07-09 | J-Pole                                                | 07-antenna-calc/07-09-j-pole.md                               | simple   |
| 07-10 | Yagi-Uda                                              | 07-antenna-calc/07-10-yagi-uda.md                             | mixed    |
| 07-11 | Vertical Antennas                                     | 07-antenna-calc/07-11-vertical.md                             | mixed    |
| 07-12 | Loading for Shortened Antennas                        | 07-antenna-calc/07-12-loading-coils.md                        | mixed    |
| 07-13 | Trap Design & Manufacturing                           | 07-antenna-calc/07-13-trap-design.md                          | advanced |
| 07-14 | Magnetic Loop                                         | 07-antenna-calc/07-14-magnetic-loop.md                        | advanced |
| 07-15 | NanoVNA Trim Workflow                                 | 07-antenna-calc/07-15-nanovna-trim.md                         | simple   |
| 08-00 | Feedline & SWR — Overview                             | 08-feedline-swr/08-00-overview.md                             | simple   |
| 08-01 | Coax Loss by Frequency                                | 08-feedline-swr/08-01-coax-loss-by-frequency.md               | mixed    |
| 08-02 | SWR & Reflected Power                                 | 08-feedline-swr/08-02-swr-reflected-power.md                  | mixed    |
| 08-03 | Mismatch Loss                                         | 08-feedline-swr/08-03-mismatch-loss.md                        | mixed    |
| 08-04 | Power Delivered vs Lost                               | 08-feedline-swr/08-04-power-delivered-vs-lost.md              | mixed    |
| 08-05 | Velocity Factor                                       | 08-feedline-swr/08-05-velocity-factor.md                      | mixed    |
| 08-06 | Impedance Transformation                              | 08-feedline-swr/08-06-impedance-transformation.md             | advanced |
| 09-00 | Power Budget & ERP — Overview                         | 09-power-budget-erp/09-00-overview.md                         | simple   |
| 09-01 | TX Power                                              | 09-power-budget-erp/09-01-tx-power.md                         | simple   |
| 09-02 | Feedline Loss                                         | 09-power-budget-erp/09-02-feedline-loss.md                    | mixed    |
| 09-03 | Antenna Gain                                          | 09-power-budget-erp/09-03-antenna-gain.md                     | mixed    |
| 09-04 | ERP Output                                            | 09-power-budget-erp/09-04-erp-output.md                       | mixed    |
| 09-05 | Portable Budget                                       | 09-power-budget-erp/09-05-portable-budget.md                  | simple   |

### Part III — Troubleshooting Systems

| id    | title                                  | path                                                | level    |
|-------|----------------------------------------|-----------------------------------------------------|----------|
| 10-00 | High SWR — Overview                    | 10-high-swr/10-00-overview.md                       | simple   |
| 10-01 | Coax Issues                            | 10-high-swr/10-01-coax-issues.md                    | simple   |
| 10-02 | Connector Issues                       | 10-high-swr/10-02-connector-issues.md               | simple   |
| 10-03 | Incorrect Length                       | 10-high-swr/10-03-incorrect-length.md               | simple   |
| 10-04 | Nearby Metal                           | 10-high-swr/10-04-nearby-metal.md                   | simple   |
| 10-05 | Faulty Balun                           | 10-high-swr/10-05-faulty-balun.md                   | simple   |
| 10-06 | Feedline Routing                       | 10-high-swr/10-06-feedline-routing.md               | simple   |
| 10-07 | Water Ingress                          | 10-high-swr/10-07-water-ingress.md                  | simple   |
| 11-00 | Station Troubleshooting — Overview     | 11-station-troubleshooting/11-00-overview.md        | simple   |
| 11-01 | No Transmit                            | 11-station-troubleshooting/11-01-no-transmit.md     | simple   |
| 11-02 | No Receive                             | 11-station-troubleshooting/11-02-no-receive.md      | simple   |
| 11-03 | Distorted Audio                        | 11-station-troubleshooting/11-03-distorted-audio.md | simple   |
| 11-04 | RF Feedback                            | 11-station-troubleshooting/11-04-rf-feedback.md     | mixed    |
| 11-05 | Grounding                              | 11-station-troubleshooting/11-05-grounding.md       | mixed    |
| 11-06 | Power Supply                           | 11-station-troubleshooting/11-06-power-supply.md    | simple   |
| 12-00 | RFI — Overview                         | 12-rfi/12-00-overview.md                            | simple   |
| 12-01 | What is RFI                            | 12-rfi/12-01-what-is-rfi.md                         | simple   |
| 12-02 | Household Sources                      | 12-rfi/12-02-household-sources.md                   | simple   |
| 12-03 | Identifying Buzzing                    | 12-rfi/12-03-identifying-buzzing.md                 | simple   |
| 12-04 | Ferrite Selection                      | 12-rfi/12-04-ferrite-selection.md                   | mixed    |
| 12-05 | Isolation Workflow                     | 12-rfi/12-05-isolation-workflow.md                  | simple   |
| 12-06 | Step-by-Step Elimination               | 12-rfi/12-06-step-by-step-elimination.md            | simple   |
| 12-07 | AM Radio Sniffer                       | 12-rfi/12-07-am-radio-sniffer.md                    | simple   |
| 12-08 | SDR Waterfall                          | 12-rfi/12-08-sdr-waterfall.md                       | simple   |
| 13-00 | Noise Sources — Overview               | 13-noise-sources/13-00-overview.md                  | simple   |
| 13-01 | Switching Power Supplies               | 13-noise-sources/13-01-switching-supplies.md        | simple   |
| 13-02 | LED Lights                             | 13-noise-sources/13-02-led-lights.md                | simple   |
| 13-03 | Solar Inverters                        | 13-noise-sources/13-03-solar-inverters.md           | simple   |
| 13-04 | Ethernet Over Power                    | 13-noise-sources/13-04-ethernet-over-power.md       | simple   |
| 13-05 | HVAC                                   | 13-noise-sources/13-05-hvac.md                      | simple   |
| 13-06 | Battery Chargers                       | 13-noise-sources/13-06-battery-chargers.md          | simple   |
| 13-07 | Motor Brushes                          | 13-noise-sources/13-07-motor-brushes.md             | simple   |
| 14-00 | Power-Line Noise — Overview            | 14-power-line-noise/14-00-overview.md               | simple   |
| 14-01 | Arcing Insulators                      | 14-power-line-noise/14-01-arcing-insulators.md      | simple   |
| 14-02 | Bad Transformers                       | 14-power-line-noise/14-02-bad-transformers.md       | simple   |
| 14-03 | Loose Hardware                         | 14-power-line-noise/14-03-loose-hardware.md         | simple   |
| 14-04 | Corona Discharge                       | 14-power-line-noise/14-04-corona-discharge.md       | mixed    |
| 14-05 | AM Radio ID                            | 14-power-line-noise/14-05-am-radio-id.md            | simple   |
| 14-06 | SDR ID                                 | 14-power-line-noise/14-06-sdr-id.md                 | simple   |
| 14-07 | Utility Documentation                  | 14-power-line-noise/14-07-utility-documentation.md  | simple   |

### Part IV — Station Management

| id    | title                          | path                                                | level    |
|-------|--------------------------------|-----------------------------------------------------|----------|
| 15-00 | Shack Inventory — Overview     | 15-shack-inventory/15-00-overview.md                | simple   |
| 15-01 | Radios                         | 15-shack-inventory/15-01-radios.md                  | simple   |
| 15-02 | Antennas                       | 15-shack-inventory/15-02-antennas.md                | simple   |
| 15-03 | Tuners                         | 15-shack-inventory/15-03-tuners.md                  | simple   |
| 15-04 | Coax Runs                      | 15-shack-inventory/15-04-coax-runs.md               | simple   |
| 15-05 | Accessories                    | 15-shack-inventory/15-05-accessories.md             | simple   |
| 15-06 | Serial Numbers                 | 15-shack-inventory/15-06-serial-numbers.md          | simple   |
| 15-07 | Purchase Dates                 | 15-shack-inventory/15-07-purchase-dates.md          | simple   |
| 15-08 | Firmware Versions              | 15-shack-inventory/15-08-firmware-versions.md       | simple   |
| 16-00 | Estate / SK — Overview         | 16-estate-sk/16-00-overview.md                      | simple   |
| 16-01 | Resale Value                   | 16-estate-sk/16-01-resale-value.md                  | simple   |
| 16-02 | Rare Collectible               | 16-estate-sk/16-02-rare-collectible.md              | simple   |
| 16-03 | Printable Export               | 16-estate-sk/16-03-printable-export.md              | simple   |
| 16-04 | Family Guidance                | 16-estate-sk/16-04-family-guidance.md               | simple   |
| 17-00 | Maintenance — Overview         | 17-maintenance/17-00-overview.md                    | simple   |
| 17-01 | Battery Maintenance            | 17-maintenance/17-01-battery-maintenance.md         | simple   |
| 17-02 | Firmware Updates               | 17-maintenance/17-02-firmware-updates.md            | simple   |
| 17-03 | Scheduled Inspections          | 17-maintenance/17-03-scheduled-inspections.md       | simple   |
| 17-04 | Coax Replacement               | 17-maintenance/17-04-coax-replacement.md            | simple   |

### Part V — Reference Library

| id    | title                                    | path                                                | level    |
|-------|------------------------------------------|-----------------------------------------------------|----------|
| 18-00 | Formulas — Overview              | 18-formulas/18-00-overview.md               | simple   |
| 18-01 | Ohms Law                                 | 18-formulas/18-01-ohms-law.md               | simple   |
| 18-02 | Power Law                                | 18-formulas/18-02-power-law.md              | simple   |
| 18-03 | Reactance (Capacitive & Inductive)       | 18-formulas/18-03-reactance.md              | mixed    |
| 18-04 | Impedance                                | 18-formulas/18-04-impedance.md              | mixed    |
| 18-05 | Resonant Frequency                       | 18-formulas/18-05-resonant-frequency.md     | mixed    |
| 18-06 | Wavelength                               | 18-formulas/18-06-wavelength.md             | simple   |
| 18-07 | SWR                                      | 18-formulas/18-07-swr.md                    | mixed    |
| 18-08 | ERP                                      | 18-formulas/18-08-erp.md                    | mixed    |
| 18-09 | Feedline Loss                            | 18-formulas/18-09-feedline-loss.md          | mixed    |
| 18-10 | Decibels                                 | 18-formulas/18-10-decibels.md               | simple   |
| 18-11 | Q Factor                                 | 18-formulas/18-11-q-factor.md               | advanced |
| 18-12 | Bandwidth                                | 18-formulas/18-12-bandwidth.md              | mixed    |
| 18-13 | Smith Chart Basics                       | 18-formulas/18-13-smith-chart-basics.md     | advanced |
| 18-14 | RF Exposure         | 18-formulas/18-14-rf-exposure.md     | mixed    |
| 19-00 | Coax & Connectors — Overview             | 19-coax-connectors/19-00-overview.md                | simple   |
| 19-01 | Coax Types                               | 19-coax-connectors/19-01-coax-types.md              | simple   |
| 19-02 | Loss Tables                              | 19-coax-connectors/19-02-loss-tables.md             | mixed    |
| 19-03 | Velocity Factor                          | 19-coax-connectors/19-03-velocity-factor.md         | mixed    |
| 19-04 | Connectors                               | 19-coax-connectors/19-04-connectors.md              | simple   |
| 19-05 | Baluns & Chokes                          | 19-coax-connectors/19-05-baluns-chokes.md           | mixed    |
| 20-00 | Q-Codes & Prosigns — Overview            | 20-qcodes-prosigns/20-00-overview.md                | simple   |
| 20-01 | Q-Codes                                  | 20-qcodes-prosigns/20-01-q-codes.md                 | simple   |
| 20-02 | CW Prosigns                              | 20-qcodes-prosigns/20-02-cw-prosigns.md             | simple   |
| 20-03 | Abbreviations                            | 20-qcodes-prosigns/20-03-abbreviations.md           | simple   |
| 20-04 | Phonetic Alphabet                        | 20-qcodes-prosigns/20-04-phonetic-alphabet.md       | simple   |
| 21-00 | Band Plans — Overview                    | 21-band-plans/21-00-overview.md                     | simple   |
| 21-01 | HF                                       | 21-band-plans/21-01-hf.md                           | mixed    |
| 21-02 | VHF                                      | 21-band-plans/21-02-vhf.md                          | simple   |
| 21-03 | UHF                                      | 21-band-plans/21-03-uhf.md                          | simple   |
| 21-04 | Satellite                                | 21-band-plans/21-04-satellite.md                    | simple   |
| 21-05 | Regional Variations                      | 21-band-plans/21-05-regional-variations.md          | mixed    |
| 22-00 | Digital Modes — Overview                 | 22-digital-modes/22-00-overview.md                  | simple   |
| 22-01 | FT8 / FT4                                | 22-digital-modes/22-01-ft8-ft4.md                   | mixed    |
| 22-02 | RTTY                                     | 22-digital-modes/22-02-rtty.md                      | mixed    |
| 22-03 | PSK31                                    | 22-digital-modes/22-03-psk31.md                     | mixed    |
| 22-04 | JS8Call                                  | 22-digital-modes/22-04-js8call.md                   | simple   |
| 22-05 | APRS                                     | 22-digital-modes/22-05-aprs.md                      | simple   |
| 22-06 | Packet                                   | 22-digital-modes/22-06-packet.md                    | mixed    |
| 23-00 | Emergency & Public Service Comms — Overview | 23-emcomm/23-00-overview.md                      | simple   |
| 23-01 | NTS — National Traffic System            | 23-emcomm/23-01-nts.md                              | simple   |
| 23-02 | ICS Basics for Amateur Operators         | 23-emcomm/23-02-ics-basics.md                       | simple   |
| 23-03 | Emergency Frequencies & Major Nets       | 23-emcomm/23-03-emergency-frequencies.md            | simple   |
| 23-04 | Message Forms                            | 23-emcomm/23-04-message-forms.md                    | simple   |
| 23-05 | Operating Procedures                     | 23-emcomm/23-05-operating-procedures.md             | simple   |
