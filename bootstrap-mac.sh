#!/usr/bin/env bash
#
# ARS Suite — macOS one-command bootstrap.
#
# Builds the whole suite, runs the installer (which plants .app bundles
# in ~/Applications/), and clears the Gatekeeper quarantine bit so
# first-launch double-click works without the right-click-Open dance.
#
# JavaFX is not installed separately: every module's fat jar bundles the
# JavaFX runtime, and Maven's pom profiles auto-select the Apple-Silicon
# or Intel native classifier at build time.
#
# Idempotent: re-running is the upgrade path. Existing config, logs,
# and databases are never touched.
#
# Prerequisites: Homebrew, Java 21+, Maven 3.8+, git.
# Run from inside the repo checkout.

set -euo pipefail

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# ── Pretty-print helpers ───────────────────────────────────────────
bold()  { printf "\033[1m%s\033[0m\n" "$*"; }
info()  { printf "\033[36m[info]\033[0m %s\n" "$*"; }
ok()    { printf "\033[32m  ✓\033[0m %s\n" "$*"; }
warn()  { printf "\033[33m[warn]\033[0m %s\n" "$*"; }
die()   { printf "\033[31m[fail]\033[0m %s\n" "$*" >&2; exit 1; }

# ── Sanity: are we actually on macOS? ─────────────────────────────
if [[ "$(uname -s)" != "Darwin" ]]; then
    die "This script is macOS-only. Use ./install.sh on Linux."
fi

bold "ARS Suite — macOS bootstrap"
echo

# ── Step 1: Prerequisites ──────────────────────────────────────────
info "Step 1 / 5 — checking prerequisites"
for cmd in java mvn git; do
    if ! command -v "$cmd" >/dev/null 2>&1; then
        case "$cmd" in
            java) die "java not found. Install with: brew install --cask temurin@21" ;;
            mvn)  die "mvn not found. Install with:  brew install maven" ;;
            git)  die "git not found. Install with:  brew install git" ;;
            *)    die "$cmd not found on PATH" ;;
        esac
    fi
    ok "$cmd"
done

JAVA_MAJOR=$(java -version 2>&1 | awk -F[\".] '/version/ {print $2}' | head -1)
if [[ -z "$JAVA_MAJOR" || "$JAVA_MAJOR" -lt 21 ]]; then
    die "Java 21 or newer required; found $(java -version 2>&1 | head -1)
       Fix: brew install --cask temurin@21"
fi
ok "Java ${JAVA_MAJOR}"
echo

# ── Step 2: Build the engine + shared web apps (mvn install) ──────
info "Step 2 / 5 — installing shared engine + web apps to local Maven repo"
mvn -q -DskipTests -f "$SCRIPT_DIR/j-log-engine/pom.xml" install \
    || die "j-log-engine build failed"
ok "j-log-engine"
mvn -q -DskipTests -f "$SCRIPT_DIR/j-learn/pom.xml" install \
    || die "j-learn build failed"
ok "j-learn"
mvn -q -DskipTests -f "$SCRIPT_DIR/j-vault/pom.xml" install \
    || die "j-vault build failed"
ok "j-vault"
echo

# ── Step 3: Package each user-facing module ────────────────────────
info "Step 3 / 5 — packaging modules"
for m in j-hub j-log j-map j-digi j-bridge j-sat morse-trainer; do
    mvn -q -DskipTests -f "$SCRIPT_DIR/$m/pom.xml" package \
        || die "$m build failed"
    ok "$m"
done
echo

# ── Step 4: Run the installer (writes .app bundles) ────────────────
info "Step 4 / 5 — installing .app bundles to ~/Applications/"
bash "$SCRIPT_DIR/install.sh"
echo

# ── Step 5: Clear Gatekeeper quarantine so double-click works ──────
info "Step 5 / 5 — clearing Gatekeeper quarantine on installed bundles"
shopt -s nullglob
QUARANTINED=0
for app in "$HOME/Applications/"WM3J*.app; do
    if xattr -p com.apple.quarantine "$app" >/dev/null 2>&1; then
        xattr -dr com.apple.quarantine "$app" 2>/dev/null || true
        QUARANTINED=$((QUARANTINED+1))
    fi
done
if [[ $QUARANTINED -gt 0 ]]; then
    ok "cleared quarantine bit on $QUARANTINED bundle(s)"
else
    ok "no quarantine bits set (bundles were not downloaded; nothing to clear)"
fi
echo

# ── Done ────────────────────────────────────────────────────────────
bold "All done."
echo
echo "  Launch J-Hub:"
echo "      open ~/Applications/WM3J\\ J-Hub.app"
echo "      # or Spotlight (⌘-Space): type \"J-Hub\""
echo
echo "  Then open the web UI:  http://localhost:8081/"
echo
echo "  Logs per module:       ~/Library/Logs/ARS-Suite/<name>.log"
echo "  Re-run this script after a 'git pull' to update everything in one shot."
