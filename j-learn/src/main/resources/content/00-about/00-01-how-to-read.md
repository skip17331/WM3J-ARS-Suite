---
id: 00-01
title: How to read this library
chapter: 00
section: 01
level: simple
status: draft
---

# How to read this library

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

J-Learn is reference material. You don't read it cover-to-cover — you find what you need, then close it. This section explains how to do that efficiently with the J-Hub UI, the search index, the Advanced toggle, and the deep-links into other parts of the suite.

## The J-Learn tab layout

```
┌──────────────────────────────────────────────────────────────────┐
│  📖 J-Learn — In-App Reference Library                           │
├──────────────────────────────────────────────────────────────────┤
│ ┌────────────┐ ┌────────────────────────────────────────────────┐│
│ │ Search box │ │                                                ││
│ │ [□Adv]     │ │  Banner (if applicable)                        ││
│ │            │ │                                                ││
│ │ TOC tree   │ │  Section content rendered from markdown        ││
│ │  01·...    │ │                                                ││
│ │  02·...    │ │                                                ││
│ │  03·...    │ │                                                ││
│ │   03-00    │ │                                                ││
│ │   03-01    │ │                                                ││
│ │   ...      │ │                                                ││
│ │ ...        │ │                                                ││
│ └────────────┘ └────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────────┘
```

Three controls on the left, one viewer on the right.

## The TOC

The left-side **Table of Contents** is a hierarchical tree:

- **Bold rows** are chapters (clickable to open the chapter overview, e.g. §08-00).
- **Indented rows** are sections within a chapter (e.g. §08-02 MPE Limits).

Click any row to open it in the viewer. The library remembers the last-opened section between J-Hub restarts (stored in browser `localStorage`).

## Search / filter

The text box at the top filters the TOC by title or section ID. Type:

- **`balun`** to find every section mentioning balun in the title (§06-12, §18-05).
- **`15-`** to filter to chapter 15 (Formulas) only.
- **`emcomm`** to find chapter 20.
- **`21-`** to filter to chapter 21 (Operating Practice) only.
- **(empty)** restores the full TOC.

Filtering is case-insensitive and matches anywhere in the title or ID.

## The Advanced toggle

Many sections include `> ⚙️ **Advanced —**` callouts — short engineering-depth sidebars that explain the underlying physics or math, the kind of detail an Extra-class operator might want but a casual reader can skip.

Toggle the **Advanced** checkbox above the TOC to show or hide these callouts in the rendered content. Simple mode is the default.

When advanced is on, callouts appear as orange-bordered blockquotes; in simple mode they're hidden entirely.

## Cross-references

Every section ends with a "See also" footer. Click a chapter title in the TOC to jump there directly. Within prose, references look like `§NN-NN` — these are the same section IDs used in the TOC.

## Per-chapter banners

Some chapters surface integrated tools as a banner at the top of every section:

| Chapter | Banner | Action |
|---------|--------|--------|
| **§05 Morse** | 🎧 Morse Code Trainer | Launches the bundled JavaFX trainer app |
| **§09 Antenna Workshop** | 📡 Antenna Workshop | Opens the matching antenna calculator panel in the J-Hub Antenna Workshop tab |
| **§17 Formulas** | 📐 Formula Calculator | Opens the matching per-formula calculator in the same Antenna Workshop tab |

The banner appears for every section in the chapter, not just the overview. From §09-10 (Yagi-Uda) the banner button takes you straight to the Yagi calculator pre-filled with example values; from §17-08 (ERP) the banner opens the ERP calculator.

## A recommended reading path for new arrivals

If you've just started using the suite and want a tour of what's here, in roughly this order:

1. **§00-00** — README (you're here / one-back)
2. **§01-00** — Propagation overview (the most-asked-about topic)
3. **§04-00** — Repeaters & Bandplans (where to put your signal)
4. **§06-00** — Antennas overview (then jump to whatever interests you)
5. **§08-00** — RF Safety (you're legally responsible for this — read once, refer back)
6. **§12–13** — Troubleshooting cluster (skim; come back when something breaks)
7. **J-Vault** — Shack Inventory (separate module — launch from J-Hub's Module Connections panel)
8. **§20-00** — Band Plans (operating-frequency reference; bookmark)

Beyond that, J-Learn is fully reference. Search for what you need.

## Working with the API

J-Learn is also accessible programmatically. J-Hub serves two endpoints:

```
GET /api/jlearn/manifest
   → JSON list of all sections with id, title, path, chapter, section, level

GET /api/jlearn/content?id=NN-NN
   → raw markdown body of one section (text/markdown)
```

The manifest is parsed from `content/manifest.md`; content lives at the per-section paths listed there. You can write your own tools against the API — quick CLI search, custom rendering, etc.

## Updates and corrections

Content is in markdown files in the `j-learn` Maven module under `src/main/resources/content/`. Corrections, typos, and additions are welcome via pull request to the WM3J-ARS-Suite GitHub repo.

When chapters are revised, the section's front-matter `status` field reflects current state (`stub` / `draft` / `final`). Until "final" content review is done, treat all `draft` material as authoritative-but-revisable.

## See also

- §00-00 — README — what J-Learn is
- §09-01 — Antenna Recommender (navigates antennas backward from your QTH)
- §05-09 — Hardware Keyer Builds (cross-references the trainer's hardware module)
- **J-Vault** — Shack Inventory (where the suite stores your station record)
