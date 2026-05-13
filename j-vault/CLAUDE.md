# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
mvn clean package -DskipTests
java -Dfile.encoding=UTF-8 -jar target/j-vault-1.0.0.jar
# Optional flags:
#   --no-browser        skip the browser auto-open
#   --launched-by-hub   J-Hub owns the lifecycle; skip the heartbeat watchdog
# Optional system property:
#   -Djvault.port=8084  override default port 8083
```

## Architecture

**J-Vault has no native window.** Standalone Jetty HTTP server on port **8083** serving a single-page web app from `src/main/resources/web/` plus a REST API. UI is browser-only — closing the browser tab triggers an orphan-process shutdown via a heartbeat watchdog (see "Browser-close handling" below).

Three Java classes total:

| Class | Role |
|---|---|
| `JVaultMain` | Entry point (`public static void main`). Parses flags, starts the server, opens the browser, installs a JVM shutdown hook |
| `JVaultServer` | Jetty wiring. Mounts servlets, owns the `HeartbeatWatchdog` daemon |
| `InventoryDao` | Singleton SQLite DAO. CRUD over the three tables, CSV export |

Most of the application logic lives in the browser (vanilla JS + bundled jsPDF for the Estate Handoff PDF wizard) — Java is just the storage shim.

## REST API

All under `/api/`:

| Method | Path | Purpose |
|---|---|---|
| GET    | `/api/health` | `{"ok":true,"app":"j-vault","version":"1.0.0"}` |
| GET    | `/api/inventory/types` | list equipment types |
| POST   | `/api/inventory/types` | create type |
| DELETE | `/api/inventory/types/:id` | delete type |
| GET    | `/api/inventory/items` | list items |
| GET    | `/api/inventory/items/:id` | get one item |
| POST   | `/api/inventory/items` | create item |
| PUT    | `/api/inventory/items/:id` | update item |
| DELETE | `/api/inventory/items/:id` | delete item |
| GET    | `/api/inventory/export.csv` | CSV download |
| GET    | `/api/inventory/contacts` | list first-call contacts |
| POST   | `/api/inventory/contacts` | create contact |
| PUT    | `/api/inventory/contacts/:id` | update contact |
| DELETE | `/api/inventory/contacts/:id` | delete contact |
| POST   | `/api/heartbeat` | (standalone mode only) browser-tab keep-alive |
| POST   | `/api/close`     | (standalone mode only) browser asked us to shut down |

## Data storage

`~/.j-vault/inventory.db` (SQLite). Three tables:

- `equipment_types(id INTEGER PK AUTOINCREMENT, name TEXT UNIQUE, display_order INT)`
- `inventory_items(id, type_id FK, manufacturer, model, serial_number, date_acquired, purchase_price REAL, estimated_value REAL, disposition TEXT DEFAULT 'working', install_status TEXT DEFAULT 'installed', storage_location, notes, extra_fields TEXT, created_at, updated_at)`
- `first_call_contacts(id, name, callsign, phone, email, relationship, items_wanted, notes, priority INT DEFAULT 100, created_at, updated_at)`

**Legacy migration:** if `~/.j-vault/inventory.db` is missing but `~/.j-hub/inventory.db` exists (from the pre-spin-out era), the latter is copied (not moved) on first run.

## Browser-close handling

When started without `--launched-by-hub`, `JVaultServer` mounts a `HeartbeatWatchdog` daemon. The browser POSTs `/api/heartbeat` every 4 s; if `lastPing` is stale by more than **12 s**, the JVM exits. On `pagehide` / `beforeunload`, the page also `sendBeacon`s to `/api/close` for a fast 150 ms shutdown.

When launched with `--launched-by-hub`, J-Hub owns the lifecycle, so the watchdog and `/api/close` endpoint are **not** mounted.

## Resources

- `src/main/resources/web/` — `index.html`, `config.js`, `config.css`, bundled jsPDF for Estate PDF
- `src/main/resources/icons/`

## What's NOT here

- No JavaFX.
- No WebSocket to J-Hub — J-Vault is self-contained.
- Tests live in `src/test/java/com/jvault/InventoryDaoTest.java` — 9 cases covering equipment-type CRUD, item CRUD with type-join, empty-string → SQL NULL handling, contact CRUD, default priority, and CSV export with quoting. Browser UI is exercised manually.
