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
| 04-00 | Exam Prep — Overview                               | 04-exam-prep/04-00-overview.md                                  | simple   |
| 04-01 | Question Pools                                     | 04-exam-prep/04-01-question-pools.md                            | mixed    |
| 04-02 | Flashcards                                         | 04-exam-prep/04-02-flashcards.md                                | simple   |
| 04-03 | Mini Quizzes                                       | 04-exam-prep/04-03-mini-quizzes.md                              | simple   |
| 04-04 | Answer Explanations                                | 04-exam-prep/04-04-answer-explanations.md                       | mixed    |
| 04-05 | Topic Drills                                       | 04-exam-prep/04-05-topic-drills.md                              | simple   |
| 04-06 | Visual Aids (Circuits, Smith Charts, Block Diagrams) | 04-exam-prep/04-06-visual-aids.md                            | mixed    |
| 04-07 | Weak-Area Analytics                                | 04-exam-prep/04-07-weak-area-analytics.md                       | simple   |
| 05-00 | Antennas — Overview                                | 05-antennas/05-00-overview.md                                   | simple   |
| 05-01 | Dipoles                                            | 05-antennas/05-01-dipoles.md                                    | mixed    |
| 05-02 | Inverted V                                         | 05-antennas/05-02-inverted-v.md                                 | mixed    |
| 05-03 | Verticals                                          | 05-antennas/05-03-verticals.md                                  | mixed    |
| 05-04 | End-Fed Half-Wave (EFHW)                           | 05-antennas/05-04-efhw.md                                       | mixed    |
| 05-05 | Magnetic Loops                                     | 05-antennas/05-05-magnetic-loops.md                             | mixed    |
| 05-06 | Full-Wave Loops                                    | 05-antennas/05-06-full-wave-loops.md                            | mixed    |
| 05-07 | Rhombic (Terminated & Unterminated)                | 05-antennas/05-07-rhombic.md                                    | advanced |
| 05-08 | Traps                                              | 05-antennas/05-08-traps.md                                      | advanced |
| 05-09 | Smith Charts                                       | 05-antennas/05-09-smith-charts.md                               | advanced |
| 05-10 | Feedline Effects                                   | 05-antennas/05-10-feedline-effects.md                           | mixed    |
| 05-11 | Impedance Transformation                           | 05-antennas/05-11-impedance-transformation.md                   | advanced |
| 05-12 | Baluns and Chokes                                  | 05-antennas/05-12-baluns-and-chokes.md                          | mixed    |
| 05-13 | Ground-Plane Effects                               | 05-antennas/05-13-ground-plane-effects.md                       | mixed    |
| 05-14 | Modeling Concepts                                  | 05-antennas/05-14-modeling-concepts.md                          | advanced |
| 05-15 | Radiation Patterns                                 | 05-antennas/05-15-radiation-patterns.md                         | mixed    |
| 06-00 | Satellites — Overview                              | 06-satellites/06-00-overview.md                                 | simple   |
| 06-01 | FM vs Linear                                       | 06-satellites/06-01-fm-vs-linear.md                             | simple   |
| 06-02 | Doppler Shift                                      | 06-satellites/06-02-doppler-shift.md                            | mixed    |
| 06-03 | Keplerian Elements                                 | 06-satellites/06-03-keplerian-elements.md                       | advanced |
| 06-04 | Tracking Strategies                                | 06-satellites/06-04-tracking-strategies.md                      | simple   |
| 06-05 | ISS Packet & APRS                                  | 06-satellites/06-05-iss-packet-aprs.md                          | simple   |
| 06-06 | Footprints                                         | 06-satellites/06-06-footprints.md                               | simple   |
| 06-07 | Pass Prediction                                    | 06-satellites/06-07-pass-prediction.md                          | simple   |
| 06-08 | Doppler Correction Tables                          | 06-satellites/06-08-doppler-correction-tables.md                | advanced |
| 07-00 | RF Safety — Overview                               | 07-rf-safety/07-00-overview.md                                  | simple   |
| 07-01 | FCC Rules                                          | 07-rf-safety/07-01-fcc-rules.md                                 | simple   |
| 07-02 | MPE Limits                                         | 07-rf-safety/07-02-mpe-limits.md                                | mixed    |
| 07-03 | Controlled vs Uncontrolled                         | 07-rf-safety/07-03-controlled-vs-uncontrolled.md                | simple   |
| 07-04 | Duty Cycle                                         | 07-rf-safety/07-04-duty-cycle.md                                | mixed    |
| 07-05 | ERP                                                | 07-rf-safety/07-05-erp.md                                       | mixed    |
| 07-06 | Safe Antenna Placement                             | 07-rf-safety/07-06-safe-antenna-placement.md                    | simple   |
| 07-07 | RF Burns                                           | 07-rf-safety/07-07-rf-burns.md                                  | simple   |

### Part II — Practical Tools & Calculators

| id    | title                                                 | path                                                          | level    |
|-------|-------------------------------------------------------|---------------------------------------------------------------|----------|
| 08-00 | Antenna Calculator — Overview                         | 08-antenna-calc/08-00-overview.md                             | simple   |
| 08-01 | Dipole Length                                         | 08-antenna-calc/08-01-dipole-length.md                        | simple   |
| 08-02 | Inverted-V Correction                                 | 08-antenna-calc/08-02-inverted-v-correction.md                | simple   |
| 08-03 | Vertical Quarter-Wave                                 | 08-antenna-calc/08-03-vertical-quarter-wave.md                | simple   |
| 08-04 | EFHW Length                                           | 08-antenna-calc/08-04-efhw-length.md                          | simple   |
| 08-05 | Loop Circumference                                    | 08-antenna-calc/08-05-loop-circumference.md                   | simple   |
| 08-06 | Magnetic Loop Capacitor                               | 08-antenna-calc/08-06-mag-loop-capacitor.md                   | advanced |
| 08-07 | Rhombic Dimensions                                    | 08-antenna-calc/08-07-rhombic-dimensions.md                   | advanced |
| 08-08 | Trap Design                                           | 08-antenna-calc/08-08-trap-design.md                          | advanced |
| 08-09 | Trimming Tables                                       | 08-antenna-calc/08-09-trimming-tables.md                      | simple   |
| 08-10 | Velocity Factor                                       | 08-antenna-calc/08-10-velocity-factor.md                      | mixed    |
| 08-11 | Test Equipment (NanoVNA, Analyzers, Dip Meters, SWR, FS Meters) | 08-antenna-calc/08-11-test-equipment.md             | mixed    |
| 09-00 | Feedline & SWR — Overview                             | 09-feedline-swr/09-00-overview.md                             | simple   |
| 09-01 | Coax Loss by Frequency                                | 09-feedline-swr/09-01-coax-loss-by-frequency.md               | mixed    |
| 09-02 | SWR & Reflected Power                                 | 09-feedline-swr/09-02-swr-reflected-power.md                  | mixed    |
| 09-03 | Mismatch Loss                                         | 09-feedline-swr/09-03-mismatch-loss.md                        | mixed    |
| 09-04 | Power Delivered vs Lost                               | 09-feedline-swr/09-04-power-delivered-vs-lost.md              | mixed    |
| 09-05 | Velocity Factor                                       | 09-feedline-swr/09-05-velocity-factor.md                      | mixed    |
| 09-06 | Impedance Transformation                              | 09-feedline-swr/09-06-impedance-transformation.md             | advanced |
| 11-00 | Power Budget & ERP — Overview                         | 11-power-budget-erp/11-00-overview.md                         | simple   |
| 11-01 | TX Power                                              | 11-power-budget-erp/11-01-tx-power.md                         | simple   |
| 11-02 | Feedline Loss                                         | 11-power-budget-erp/11-02-feedline-loss.md                    | mixed    |
| 11-03 | Antenna Gain                                          | 11-power-budget-erp/11-03-antenna-gain.md                     | mixed    |
| 11-04 | ERP Output                                            | 11-power-budget-erp/11-04-erp-output.md                       | mixed    |
| 11-05 | Portable Budget                                       | 11-power-budget-erp/11-05-portable-budget.md                  | simple   |

### Part III — Troubleshooting Systems

| id    | title                                  | path                                                | level    |
|-------|----------------------------------------|-----------------------------------------------------|----------|
| 13-00 | High SWR — Overview                    | 13-high-swr/13-00-overview.md                       | simple   |
| 13-01 | Coax Issues                            | 13-high-swr/13-01-coax-issues.md                    | simple   |
| 13-02 | Connector Issues                       | 13-high-swr/13-02-connector-issues.md               | simple   |
| 13-03 | Incorrect Length                       | 13-high-swr/13-03-incorrect-length.md               | simple   |
| 13-04 | Nearby Metal                           | 13-high-swr/13-04-nearby-metal.md                   | simple   |
| 13-05 | Faulty Balun                           | 13-high-swr/13-05-faulty-balun.md                   | simple   |
| 13-06 | Feedline Routing                       | 13-high-swr/13-06-feedline-routing.md               | simple   |
| 13-07 | Water Ingress                          | 13-high-swr/13-07-water-ingress.md                  | simple   |
| 14-00 | Station Troubleshooting — Overview     | 14-station-troubleshooting/14-00-overview.md        | simple   |
| 14-01 | No Transmit                            | 14-station-troubleshooting/14-01-no-transmit.md     | simple   |
| 14-02 | No Receive                             | 14-station-troubleshooting/14-02-no-receive.md      | simple   |
| 14-03 | Distorted Audio                        | 14-station-troubleshooting/14-03-distorted-audio.md | simple   |
| 14-04 | RF Feedback                            | 14-station-troubleshooting/14-04-rf-feedback.md     | mixed    |
| 14-05 | Grounding                              | 14-station-troubleshooting/14-05-grounding.md       | mixed    |
| 14-06 | Power Supply                           | 14-station-troubleshooting/14-06-power-supply.md    | simple   |
| 15-00 | RFI — Overview                         | 15-rfi/15-00-overview.md                            | simple   |
| 15-01 | What is RFI                            | 15-rfi/15-01-what-is-rfi.md                         | simple   |
| 15-02 | Household Sources                      | 15-rfi/15-02-household-sources.md                   | simple   |
| 15-03 | Identifying Buzzing                    | 15-rfi/15-03-identifying-buzzing.md                 | simple   |
| 15-04 | Ferrite Selection                      | 15-rfi/15-04-ferrite-selection.md                   | mixed    |
| 15-05 | Isolation Workflow                     | 15-rfi/15-05-isolation-workflow.md                  | simple   |
| 15-06 | Step-by-Step Elimination               | 15-rfi/15-06-step-by-step-elimination.md            | simple   |
| 15-07 | AM Radio Sniffer                       | 15-rfi/15-07-am-radio-sniffer.md                    | simple   |
| 15-08 | SDR Waterfall                          | 15-rfi/15-08-sdr-waterfall.md                       | simple   |
| 16-00 | Noise Sources — Overview               | 16-noise-sources/16-00-overview.md                  | simple   |
| 16-01 | Switching Power Supplies               | 16-noise-sources/16-01-switching-supplies.md        | simple   |
| 16-02 | LED Lights                             | 16-noise-sources/16-02-led-lights.md                | simple   |
| 16-03 | Solar Inverters                        | 16-noise-sources/16-03-solar-inverters.md           | simple   |
| 16-04 | Ethernet Over Power                    | 16-noise-sources/16-04-ethernet-over-power.md       | simple   |
| 16-05 | HVAC                                   | 16-noise-sources/16-05-hvac.md                      | simple   |
| 16-06 | Battery Chargers                       | 16-noise-sources/16-06-battery-chargers.md          | simple   |
| 16-07 | Motor Brushes                          | 16-noise-sources/16-07-motor-brushes.md             | simple   |
| 17-00 | Power-Line Noise — Overview            | 17-power-line-noise/17-00-overview.md               | simple   |
| 17-01 | Arcing Insulators                      | 17-power-line-noise/17-01-arcing-insulators.md      | simple   |
| 17-02 | Bad Transformers                       | 17-power-line-noise/17-02-bad-transformers.md       | simple   |
| 17-03 | Loose Hardware                         | 17-power-line-noise/17-03-loose-hardware.md         | simple   |
| 17-04 | Corona Discharge                       | 17-power-line-noise/17-04-corona-discharge.md       | mixed    |
| 17-05 | AM Radio ID                            | 17-power-line-noise/17-05-am-radio-id.md            | simple   |
| 17-06 | SDR ID                                 | 17-power-line-noise/17-06-sdr-id.md                 | simple   |
| 17-07 | Utility Documentation                  | 17-power-line-noise/17-07-utility-documentation.md  | simple   |

### Part IV — Station Management

| id    | title                          | path                                                | level    |
|-------|--------------------------------|-----------------------------------------------------|----------|
| 18-00 | Shack Inventory — Overview     | 18-shack-inventory/18-00-overview.md                | simple   |
| 18-01 | Radios                         | 18-shack-inventory/18-01-radios.md                  | simple   |
| 18-02 | Antennas                       | 18-shack-inventory/18-02-antennas.md                | simple   |
| 18-03 | Tuners                         | 18-shack-inventory/18-03-tuners.md                  | simple   |
| 18-04 | Coax Runs                      | 18-shack-inventory/18-04-coax-runs.md               | simple   |
| 18-05 | Accessories                    | 18-shack-inventory/18-05-accessories.md             | simple   |
| 18-06 | Serial Numbers                 | 18-shack-inventory/18-06-serial-numbers.md          | simple   |
| 18-07 | Purchase Dates                 | 18-shack-inventory/18-07-purchase-dates.md          | simple   |
| 18-08 | Firmware Versions              | 18-shack-inventory/18-08-firmware-versions.md       | simple   |
| 19-00 | Estate / SK — Overview         | 19-estate-sk/19-00-overview.md                      | simple   |
| 19-01 | Resale Value                   | 19-estate-sk/19-01-resale-value.md                  | simple   |
| 19-02 | Rare Collectible               | 19-estate-sk/19-02-rare-collectible.md              | simple   |
| 19-03 | Printable Export               | 19-estate-sk/19-03-printable-export.md              | simple   |
| 19-04 | Family Guidance                | 19-estate-sk/19-04-family-guidance.md               | simple   |
| 20-00 | Maintenance — Overview         | 20-maintenance/20-00-overview.md                    | simple   |
| 20-01 | Battery Maintenance            | 20-maintenance/20-01-battery-maintenance.md         | simple   |
| 20-02 | Firmware Updates               | 20-maintenance/20-02-firmware-updates.md            | simple   |
| 20-03 | Scheduled Inspections          | 20-maintenance/20-03-scheduled-inspections.md       | simple   |
| 20-04 | Coax Replacement               | 20-maintenance/20-04-coax-replacement.md            | simple   |

### Part V — Reference Library

| id    | title                                    | path                                                | level    |
|-------|------------------------------------------|-----------------------------------------------------|----------|
| 21-00 | Formula Appendix — Overview              | 21-formula-appendix/21-00-overview.md               | simple   |
| 21-01 | Ohms Law                                 | 21-formula-appendix/21-01-ohms-law.md               | simple   |
| 21-02 | Power Law                                | 21-formula-appendix/21-02-power-law.md              | simple   |
| 21-03 | Reactance (Capacitive & Inductive)       | 21-formula-appendix/21-03-reactance.md              | mixed    |
| 21-04 | Impedance                                | 21-formula-appendix/21-04-impedance.md              | mixed    |
| 21-05 | Resonant Frequency                       | 21-formula-appendix/21-05-resonant-frequency.md     | mixed    |
| 21-06 | Wavelength                               | 21-formula-appendix/21-06-wavelength.md             | simple   |
| 21-07 | SWR                                      | 21-formula-appendix/21-07-swr.md                    | mixed    |
| 21-08 | ERP                                      | 21-formula-appendix/21-08-erp.md                    | mixed    |
| 21-09 | Feedline Loss                            | 21-formula-appendix/21-09-feedline-loss.md          | mixed    |
| 21-10 | Decibels                                 | 21-formula-appendix/21-10-decibels.md               | simple   |
| 21-11 | Q Factor                                 | 21-formula-appendix/21-11-q-factor.md               | advanced |
| 21-12 | Bandwidth                                | 21-formula-appendix/21-12-bandwidth.md              | mixed    |
| 21-13 | Smith Chart Basics                       | 21-formula-appendix/21-13-smith-chart-basics.md     | advanced |
| 21-14 | Formula Calculator (RF Exposure Worked Example)         | 21-formula-appendix/21-14-formula-calculator.md     | mixed    |
| 22-00 | Coax & Connectors — Overview             | 22-coax-connectors/22-00-overview.md                | simple   |
| 22-01 | Coax Types                               | 22-coax-connectors/22-01-coax-types.md              | simple   |
| 22-02 | Loss Tables                              | 22-coax-connectors/22-02-loss-tables.md             | mixed    |
| 22-03 | Velocity Factor                          | 22-coax-connectors/22-03-velocity-factor.md         | mixed    |
| 22-04 | Connectors                               | 22-coax-connectors/22-04-connectors.md              | simple   |
| 22-05 | Baluns & Chokes                          | 22-coax-connectors/22-05-baluns-chokes.md           | mixed    |
| 23-00 | Q-Codes & Prosigns — Overview            | 23-qcodes-prosigns/23-00-overview.md                | simple   |
| 23-01 | Q-Codes                                  | 23-qcodes-prosigns/23-01-q-codes.md                 | simple   |
| 23-02 | CW Prosigns                              | 23-qcodes-prosigns/23-02-cw-prosigns.md             | simple   |
| 23-03 | Abbreviations                            | 23-qcodes-prosigns/23-03-abbreviations.md           | simple   |
| 23-04 | Phonetic Alphabet                        | 23-qcodes-prosigns/23-04-phonetic-alphabet.md       | simple   |
| 24-00 | Band Plans — Overview                    | 24-band-plans/24-00-overview.md                     | simple   |
| 24-01 | HF                                       | 24-band-plans/24-01-hf.md                           | mixed    |
| 24-02 | VHF                                      | 24-band-plans/24-02-vhf.md                          | simple   |
| 24-03 | UHF                                      | 24-band-plans/24-03-uhf.md                          | simple   |
| 24-04 | Satellite                                | 24-band-plans/24-04-satellite.md                    | simple   |
| 24-05 | Regional Variations                      | 24-band-plans/24-05-regional-variations.md          | mixed    |
| 25-00 | Digital Modes — Overview                 | 25-digital-modes/25-00-overview.md                  | simple   |
| 25-01 | FT8 / FT4                                | 25-digital-modes/25-01-ft8-ft4.md                   | mixed    |
| 25-02 | RTTY                                     | 25-digital-modes/25-02-rtty.md                      | mixed    |
| 25-03 | PSK31                                    | 25-digital-modes/25-03-psk31.md                     | mixed    |
| 25-04 | JS8Call                                  | 25-digital-modes/25-04-js8call.md                   | simple   |
| 25-05 | APRS                                     | 25-digital-modes/25-05-aprs.md                      | simple   |
| 25-06 | Packet                                   | 25-digital-modes/25-06-packet.md                    | mixed    |
| 26-00 | Emergency & Public Service Comms — Overview | 26-emcomm/26-00-overview.md                      | simple   |
| 26-01 | NTS — National Traffic System            | 26-emcomm/26-01-nts.md                              | simple   |
| 26-02 | ICS Basics for Amateur Operators         | 26-emcomm/26-02-ics-basics.md                       | simple   |
| 26-03 | Emergency Frequencies & Major Nets       | 26-emcomm/26-03-emergency-frequencies.md            | simple   |
| 26-04 | Message Forms                            | 26-emcomm/26-04-message-forms.md                    | simple   |
| 26-05 | Operating Procedures                     | 26-emcomm/26-05-operating-procedures.md             | simple   |
