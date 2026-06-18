#!/usr/bin/env bash
#
# ARS Suite — Linux / macOS installer bootstrap.
#
# Assumes:
#   - Java 21 is installed          (java --version)
#   - Maven is installed            (mvn --version)
#   - The ars-fx app has been built (mvn -f ars-fx/pom.xml -DskipTests package),
#     which produces ars-fx/target/ars-fx-linux.jar — the single jar all the
#     suite shortcuts run.
#
# What this script does:
#   1. Builds the installer jar if it isn't present.
#   2. Runs the installer, which detects the platform and writes shortcuts for
#      the ARS Suite (one jar, launched different ways):
#        - "ARS Suite"      — the docked app (J-Hub + all modules in one window)
#        - J-Log / J-Map / J-Sat / J-Digi / J-Vault / J-Learn — each module in
#          its own window (loose), attached to the background hub
#        - "ARS Suite Hub"  — the background hub (system tray)
#      Platform targets:
#        - Linux : .desktop entries in ~/.local/share/applications/
#                  + icons in ~/.local/share/icons/
#        - macOS : .app bundles in ~/Applications/
#                  (log output goes to ~/Library/Logs/ARS-Suite/)
#   3. Leaves all existing files (j-hub.json, logs, databases) untouched —
#      safe to run any number of times as an upgrade.

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
INSTALLER_TARGET="$SCRIPT_DIR/installer/target"

# Resolve the built installer jar regardless of version. The shade plugin
# writes the runnable j-installer-<version>.jar next to the thin
# original-j-installer-<version>.jar, so glob for the former and pick the
# highest version — a version bump must never break this bootstrap.
find_installer_jar() {
    ls -1 "$INSTALLER_TARGET"/j-installer-*.jar 2>/dev/null \
        | grep -v '/original-' | sort -V | tail -n1
}

command -v java >/dev/null 2>&1 || {
    echo "error: Java 21 is required but 'java' was not found on PATH."
    echo "       Install it first, then re-run $0"
    exit 1
}

INSTALLER_JAR="$( find_installer_jar )"

if [[ -z "$INSTALLER_JAR" ]]; then
    echo "[bootstrap] Installer jar not found — building…"
    command -v mvn >/dev/null 2>&1 || {
        echo "error: Maven is required to build the installer but 'mvn' was not found."
        exit 1
    }
    mvn -q -DskipTests -f "$SCRIPT_DIR/installer/pom.xml" package
    INSTALLER_JAR="$( find_installer_jar )"
fi

if [[ -z "$INSTALLER_JAR" || ! -f "$INSTALLER_JAR" ]]; then
    echo "error: installer jar still not found in $INSTALLER_TARGET after build."
    exit 1
fi

exec java -jar "$INSTALLER_JAR" --root "$SCRIPT_DIR" "$@"
