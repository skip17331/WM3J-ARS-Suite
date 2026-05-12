# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Build the runnable jar
mvn clean install -DskipTests

# Run the standalone web server (default port 8082)
./start.sh
# or
java -jar target/j-learn-1.0.0.jar
```

J-Hub iframes this server at `http://localhost:8082/`. J-Learn is independent of JavaFX and depends only on Jetty + Jackson + SLF4J.

## Two integration paths

**1. Standalone web app** (the original shipping path)
- Entry: `com.hamradio.jlearn.server.JLearnMain`
- Embedded Jetty serves `src/main/resources/web/` and the REST API.
- Content is **seeded** to `~/.j-learn/content/` on first run from the bundled `/content/` classpath copy. `ContentResolver` prefers the on-disk copy and falls back to the jar — so users can edit chapters without rebuilding.

**2. In-process library API** (shipped 2026-05-12, commit `3645a26`)
- Entry: `com.hamradio.jlearn.JLearnModule`
- Lets other modules embed content programmatically (deep-link from J-Log into a formula chapter, render a single section in a JavaFX WebView, etc.) without iframing the web app.
- Pure JDK — no Jackson, no YAML lib, no SLF4J on the loaders.

Use the standalone app when the host is already a browser-based UI (J-Hub config) and the library API when the host wants tight in-process integration.

## Library API surface

```java
ContentManifest m = JLearnModule.manifest();      // cached singleton
ContentEntry e    = m.byId("01-04");              // O(1) lookup
List<ContentEntry> ch = m.byChapter("01");        // chapter index
String body       = ContentLoader.read(e);        // raw markdown (front-matter + body)
Map<String,String> fm = ContentLoader.frontMatter(body);

ReadingStateStore store = JLearnModule.stateStore();
store.save(ReadingState.newSnapshot("01-04", 0.42));
store.load();                                      // Optional<ReadingState>
```

`JLearnModule.setStateStore(null)` reverts after a test override.

## Content layout

```
src/main/resources/content/
├── manifest.md                 ← machine-parseable index
├── 00-about/
│   ├── 00-00-readme.md
│   └── 00-01-how-to-read.md
├── 01-propagation/
│   ├── 01-00-overview.md
│   └── 01-01-solar-indices.md
└── ...                         (~200 sections across 31 chapters)
```

Every markdown file begins with YAML front-matter:
```yaml
---
id: 01-04
title: Ionospheric Layers (D / E / F1 / F2)
chapter: 01
section: 04
level: mixed                    # simple | advanced | mixed
status: draft                   # stub | draft | review | published
---
```

`manifest.md` contains pipe-delimited tables under "Part I…V" headers. `ContentManifest` reads rows whose first cell matches `NN-NN` and ignores everything else.

## Server REST endpoints

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/jlearn/manifest` | flat array of section metadata |
| GET | `/api/jlearn/section/<id>` | markdown body for one section |
| GET | `/api/jlearn/state` | per-user reading-state (last id, scroll fraction) |
| POST | `/api/jlearn/state` | persist reading-state |

## Data storage

`~/.j-learn/`:
- `content/` — seeded copy of bundled markdown; user-editable
- `state.properties` — last-opened section + scroll fraction (Properties format, atomic tmp+rename writes)
- `settings.json` — port override

## Packages

| Package | Responsibility |
|---|---|
| `com.hamradio.jlearn` | `JLearnModule` — library facade |
| `com.hamradio.jlearn.content` | `ContentManifest`, `ContentEntry`, `ContentLoader` — classpath-only readers |
| `com.hamradio.jlearn.state` | `ReadingState`, `ReadingStateStore` interface, `FileReadingStateStore` |
| `com.hamradio.jlearn.server` | `JLearnMain` (Application entry), `JLearnServer`, `ContentResolver`, `Settings` |

## Java version

Source/target: **11**. No records, no pattern-matching instanceof, no switch expressions in this module — keep it compatible.

## Tests

19 cases under `src/test/java/com/hamradio/jlearn/`:
- `ContentEntryTest` — id validation, equality, parse fallbacks
- `ContentManifestTest` — loads bundled manifest, every entry resolves to a real markdown body, chapter ordering, front-matter parser
- `FileReadingStateStoreTest` — round-trip, missing-file, clear, no tmp-file leak, fraction-clamp, ReadingState input validation
- `JLearnModuleTest` — singleton manifest, end-to-end host flow with injected temp-path store

## Editing content

Edits in `~/.j-learn/content/*.md` take effect on browser reload — no rebuild. To ship a change to all new installs, edit `j-learn/src/main/resources/content/` too and `mvn install` j-learn (the seed runs on first launch only; existing users keep their disk copy).
