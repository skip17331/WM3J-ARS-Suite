#!/usr/bin/env bash
#
# ARS Suite — macOS one-command bootstrap.
#
# Detects Apple Silicon vs Intel, installs the matching macOS JavaFX 21
# SDK into every module that needs it, builds the whole suite, runs the
# installer (which plants .app bundles in ~/Applications/), and clears
# the Gatekeeper quarantine bit so first-launch double-click works
# without the right-click-Open dance.
#
# Idempotent: re-running is the upgrade path. Existing config, logs,
# and databases are never touched.
#
# Prerequisites: Homebrew, Java 21+, Maven 3.8+, git, curl, unzip.
# Run from inside the repo checkout.

set -euo pipefail

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
JAVAFX_VERSION="21.0.5"

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
info "Step 1 / 6 — checking prerequisites"
for cmd in java mvn git curl unzip; do
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

# ── Step 2: Architecture detection + JavaFX SDK ────────────────────
info "Step 2 / 6 — installing macOS JavaFX ${JAVAFX_VERSION} SDK"

ARCH=$(uname -m)
case "$ARCH" in
    arm64)  JFX_CLASSIFIER="osx-aarch64" ; CHIP_LABEL="Apple Silicon" ;;
    x86_64) JFX_CLASSIFIER="osx-x64"     ; CHIP_LABEL="Intel"          ;;
    *)      die "Unsupported architecture: $ARCH (expected arm64 or x86_64)" ;;
esac
ok "Detected $CHIP_LABEL ($ARCH)"

JFX_URL="https://download2.gluonhq.com/openjfx/${JAVAFX_VERSION}/openjfx-${JAVAFX_VERSION}_${JFX_CLASSIFIER}_bin-sdk.zip"
CACHE_DIR="$HOME/.cache/ars-suite/javafx"
CACHE_ZIP="$CACHE_DIR/openjfx-${JAVAFX_VERSION}-${JFX_CLASSIFIER}.zip"
CACHE_SDK="$CACHE_DIR/openjfx-${JAVAFX_VERSION}-${JFX_CLASSIFIER}"

mkdir -p "$CACHE_DIR"
if [[ ! -f "$CACHE_ZIP" ]]; then
    info "  downloading $JFX_URL"
    if ! curl -fL --progress-bar -o "$CACHE_ZIP.partial" "$JFX_URL"; then
        rm -f "$CACHE_ZIP.partial"
        die "JavaFX SDK download failed. Check network or visit https://gluonhq.com/products/javafx/ manually."
    fi
    mv "$CACHE_ZIP.partial" "$CACHE_ZIP"
    ok "downloaded ($(du -h "$CACHE_ZIP" | awk '{print $1}'))"
else
    ok "cached ($(du -h "$CACHE_ZIP" | awk '{print $1}'))"
fi

if [[ ! -d "$CACHE_SDK" ]]; then
    info "  extracting"
    rm -rf "$CACHE_SDK"
    mkdir -p "$CACHE_SDK"
    unzip -q "$CACHE_ZIP" -d "$CACHE_SDK"
fi
JFX_LIB="$(ls -d "$CACHE_SDK"/javafx-sdk-*/lib 2>/dev/null | head -1)"
[[ -d "$JFX_LIB" ]] || die "JavaFX SDK extraction failed (no lib/ dir in $CACHE_SDK)"

# Modules that hard-code --module-path lib/javafx in their .sh launchers.
# j-vault, j-learn, morse-trainer are web-only / Java Sound — no JavaFX.
JFX_MODULES=(j-hub j-log j-map j-digi j-bridge j-sat)
for m in "${JFX_MODULES[@]}"; do
    DEST="$SCRIPT_DIR/$m/lib/javafx"
    mkdir -p "$DEST"
    # Wipe any existing (likely Linux) jars and dylibs so we don't mix arches.
    find "$DEST" -maxdepth 1 -type f \( -name '*.jar' -o -name '*.dylib' \) -delete 2>/dev/null || true
    cp -f "$JFX_LIB"/*.jar "$DEST/" 2>/dev/null
    cp -f "$JFX_LIB"/*.dylib "$DEST/" 2>/dev/null || true
    ok "  $m/lib/javafx ← $(ls "$DEST" | wc -l | tr -d ' ') files"
done
echo

# ── Step 3: Build the engine + shared web apps (mvn install) ──────
info "Step 3 / 6 — installing shared engine + web apps to local Maven repo"
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

# ── Step 4: Package each user-facing module ────────────────────────
info "Step 4 / 6 — packaging modules"
for m in j-hub j-log j-map j-digi j-bridge j-sat morse-trainer; do
    mvn -q -DskipTests -f "$SCRIPT_DIR/$m/pom.xml" package \
        || die "$m build failed"
    ok "$m"
done
echo

# ── Step 5: Run the installer (writes .app bundles) ────────────────
info "Step 5 / 6 — installing .app bundles to ~/Applications/"
bash "$SCRIPT_DIR/install.sh"
echo

# ── Step 6: Clear Gatekeeper quarantine so double-click works ──────
info "Step 6 / 6 — clearing Gatekeeper quarantine on installed bundles"
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
