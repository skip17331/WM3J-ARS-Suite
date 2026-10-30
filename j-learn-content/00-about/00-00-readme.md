---
id: 00-00
title: README — what J-Learn is
chapter: 00
section: 00
level: simple
status: published
---

# README — what J-Learn is

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

J-Learn is the **in-app reference library** for the WM3J ARS Suite. It's not a textbook, not a magazine, not a forum thread. It's a focused, opinionated set of cards that explain how amateur-radio operating works — written for the operator who needs answers fast.

## Who it's for

- **A licensee who wants to actually operate** rather than chase another textbook chapter on the math.
- **An experienced ham** who needs to look up a specific calculation, regulation, or procedure without grepping through stacks of saved PDFs.
- **A returning ham** who hasn't operated in years and needs the modern context (FT8, FCC §97 changes, modern band plans, current digital modes, RF-safety rules from 2021 forward).
- **A homebrewer** who wants the math (§17), the antenna designs (§09), and the construction recipes in one place.

J-Learn assumes **you have a license**. It's not a Technician / General / Extra study guide — there are excellent free question-pool drill sites (HamStudy.org, ExamCram, QRZ practice exams) for that. J-Learn picks up after the test, when you start operating.

## What's in it

Twenty-two chapters organized into four loose clusters:

| Cluster | Chapters | What it covers |
|---------|---------|----------------|
| **Operating fundamentals** | 01 Propagation · 02 Repeaters & Bandplans · 03 Morse · 18 Band Plans · 19 Digital Modes · 20 Emergency Comms · 21 Operating Practice | The day-to-day "how to operate" cluster. Where signals go, where to put yours, how to send / decode each mode, regulations, on-air etiquette |
| **Antennas & RF** | 04 Antennas · 05 Satellites · 06 RF Safety · 07 Antenna Workshop · 08 Feedline & SWR · 09 Power Budget · 16 Coax & Connectors | Antenna theory, antenna designs and calculators, feedline behavior, RF exposure |
| **Troubleshooting** | 10 High SWR · 11 Station Troubleshooting · 12 RFI · 13 Noise Sources (household + power-line) · 14 Maintenance | When things break, why, and what to do about it |
| **Reference** | 15 Formulas · 17 Q-Codes & Prosigns | The math behind everything; vocabulary |

Shack inventory and estate-handoff features have moved out of J-Learn into a dedicated **J-Vault** module — it owns the SQLite-backed inventory database, the first-call contacts, and the Estate PDF wizard. Launch J-Vault from J-Hub's Module Connections panel.

## How it's structured

Every section is a **card** with the same shape:

- Front matter (id, title, chapter, section, level, status)
- An opening paragraph stating what this section is for
- Tables, formulas, or worked examples
- A "common mistakes" or "gotchas" list where applicable
- An optional `> **Advanced —**` callout for engineering-depth material
- A "see also" footer with cross-links to related sections

Most sections are 4–8 KB. A few (formula calculators, RF-exposure walkthrough, antenna recommender) are longer because the material genuinely needs the room.

## What J-Learn isn't

- **Not the ARRL Handbook.** No shared content. The Handbook is great; this is different — narrower, more current, more opinionated.
- **Not a forum.** No discussion, no comments, no posting. Send corrections to the J-Hub project's GitHub.
- **Not a buyers' guide.** Specific products are named only when they're effectively standard (Astron RS-50, Bird 43, Heil PR-781, Hustler 5-BTV) — not as recommendations.
- **Not a substitute for the FCC rules.** §97 is authoritative; J-Learn cites and summarizes but you should know how to look up the actual regulation.
- **Content-complete.** Every section is written and marked `published`; refinements and corrections are ongoing. See **Status** below.

## How J-Learn integrates with the suite

Two J-Hub features deep-link into J-Learn chapters and back:

- **Chapter 05 (Morse)** has a "▶ Launch Trainer" button at the top of every section that opens the bundled Morse Code Trainer app.
- **Chapter 09 (Antenna Workshop)** has a "▶ Open in Workshop" button that opens the matching antenna calculator panel in the J-Hub Antenna Workshop tab.
- **Chapter 17 (Formulas)** has the same "▶ Open in Workshop" pattern, mapping each formula card to a per-formula calculator (Ohm's, Power, Reactance, Impedance, Resonance, Wavelength, SWR, ERP, Feedline Loss, Decibels, Q, Bandwidth, RF Exposure).

The recommender in §09-01 also walks the operator backward from goals → chapter sections that might apply.

Cross-references between sections are written as `§NN-NN` (e.g., §17-06 for the Wavelength formula card). Click a chapter or section in the left-side TOC to navigate.

## Status

Every section's front matter has a **status** field that moves through `stub` → `draft` → `review` → `published`. As of late 2026 every section is `published` and the library is content-complete; refinements and corrections are ongoing.

J-Learn ships with the suite. To see what's been written, open the J-Learn tab in J-Hub and use the search box at the top of the left-side index. §00-01 covers how to navigate effectively.

## See also

- §00-01 — How to read this library (search, filter, the Advanced toggle, deep-links)
- §09-00 — Antenna Workshop overview (the calculator's tab in J-Hub)
- §05-00 — Morse Code overview (the bundled trainer app)
