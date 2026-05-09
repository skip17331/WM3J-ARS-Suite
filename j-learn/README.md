# J-Learn

In-app learning and reference library for the WM3J ARS Suite. ~200 markdown chapters covering propagation, antennas, RF safety, troubleshooting, formulas, band plans, exam prep, and more — accessible from any other module in the suite.

## Purpose

J-Learn is a **content + skeleton** module today. It ships:

- The full chapter tree under `src/main/resources/content/` (every section is a stub at the moment — front matter, headings, and `<!-- TODO: content -->` markers only).
- A machine-parseable `manifest.md` that the loader uses to build a navigation tree.
- Java skeleton classes for the manifest parser, content loader, and reading-state persistence.

There is **no UI here yet** — host modules (j-log, j-map, j-digi, j-bridge, j-sat, j-hub) are responsible for opening J-Learn and rendering its content.

## How content is organized

All chapters live under `src/main/resources/content/`. Directories are numbered `NN-slug/`; each section inside a directory is `NN-NN-slug.md`. The full table of contents is in [`src/main/resources/content/manifest.md`](src/main/resources/content/manifest.md), grouped into five Parts:

| Part | Theme | Chapters |
|------|-------|----------|
| I    | Foundations & Primers       | 00–07 |
| II   | Practical Tools & Calculators | 08–12 |
| III  | Troubleshooting Systems     | 13–17 |
| IV   | Station Management          | 18–20 |
| V    | Reference Library           | 21–26 |

The manifest's section-index table is the source of truth — `ContentManifest.load()` parses it at startup. Anything outside those tables is documentation for humans.

## Front-matter schema

Every content file starts with a YAML block:

```yaml
---
id: 01-01
title: Solar Indices (SFI, A-index, K-index)
chapter: 01
section: 01
level: mixed
status: stub
---
```

| Field    | Allowed values                                       |
|----------|------------------------------------------------------|
| `id`     | `NN-NN` (chapter-section), zero-padded, unique       |
| `title`  | free text, single line                               |
| `chapter`| `NN`, must match the directory prefix                |
| `section`| `NN`; `00` is the chapter overview                   |
| `level`  | `simple` / `advanced` / `mixed`                      |
| `status` | `stub` / `draft` / `review` / `published`            |

## Simple vs Advanced callout convention

Two granularities:

1. **File-level**: the front-matter `level` field tags the whole file as `simple`, `advanced`, or `mixed`.
2. **Paragraph-level** (only inside `mixed` files): individual paragraphs intended for Extra-class / engineering depth are marked with a markdown blockquote that begins with the literal callout marker:

```markdown
> ⚙️ **Advanced —** The MUF varies with the Earth's magnetic field
> direction relative to the path because the Appleton-Hartree equation
> depends on the angle between propagation and B…
```

Renderers in **simple mode** elide every blockquote whose first non-whitespace text starts with `⚙️ **Advanced —**`. **Advanced mode** renders them inline with a distinct visual style. Keep the marker spelling, spacing, and emoji exactly — it's a literal-string match, not a regex.

## How "last read" state is persisted

| Aspect    | Value                                                                  |
|-----------|------------------------------------------------------------------------|
| Path      | `~/.j-learn/state.properties` (matches the per-module `~/.<module>/` convention used by j-hub, j-log, j-map, j-sat) |
| Format    | Java `Properties` — `lastSectionId=…`, `scrollFraction=…`, `savedAt=…` |
| Atomicity | Write to `state.properties.tmp`, then `Files.move(... ATOMIC_MOVE)`    |

Implementation lives in `com.hamradio.jlearn.state.FileReadingStateStore`. The `ReadingStateStore` interface lets host modules swap in alternative backings (in-memory for tests, shared store for multi-user setups).

## How to add a new chapter or section

1. Pick a chapter number. New chapters use the next free `NN`; new sections within an existing chapter use the next free `NN-NN`. **Never renumber existing files** — IDs are stable across releases so reading-state pointers don't go stale.
2. Drop the file in the right folder, named `NN-NN-slug.md`. Use lowercase-hyphenated slugs.
3. Start the file with the front-matter block (all six fields required).
4. Add one row to the section-index table in `manifest.md` — pipe-delimited, with the same `id`, `title`, `path`, and `level` as the file's front matter.
5. Run `mvn -pl j-learn package` to verify the file is picked up.

## Build instructions

```bash
# Build just j-learn
cd /home/mike/ARS_Suite/j-learn && mvn package

# Build with tests skipped (no tests yet)
mvn package -DskipTests

# Install to local Maven repo so other modules can depend on it
mvn install
```

The repo has no parent POM — each module is built independently. Java 21 is required (matches the newer modules: j-sat, j-vault, j-map).

## Roadmap

The skeleton is in place. Next steps (in rough order):

1. Implement `ContentManifest`, `ContentEntry`, `ContentLoader`, `ReadingState`, `FileReadingStateStore`.
2. Wire a J-Learn tab into the J-Hub web UI (server-side markdown rendering + browser-side navigation).
3. Add deep-link menu items to j-log / j-map / j-digi / j-bridge / j-sat that open J-Learn at a specific section.
4. Begin filling in real chapter content, starting with the high-impact sections (Solar Indices, Dipole Length, Koch Method, RFI Step-by-Step Elimination).
