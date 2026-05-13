#!/usr/bin/env bash
#
# ARS Suite — Linux / macOS installer bootstrap.
#
# Assumes:
#   - Java 21 is installed          (java --version)
#   - Maven is installed            (mvn --version)
#   - j-log-engine has been installed first (mvn install) — shared library
#   - All nine user-facing modules have been built (mvn package each):
#       j-hub
#       j-log
#       j-map
#       j-digi
#       j-bridge
#       j-sat
#       j-vault         (shack inventory + estate handoff PDF)
#       j-learn         (standalone reference library web app, port 8082)
#       morse-trainer   (Morse code learning + sending practice)
#
# What this script does:
#   1. Builds the installer jar if it isn't present.
#   2. Runs the installer, which detects the platform and writes
#      appropriate shortcuts:
#        - Linux : .desktop entries in ~/.local/share/applications/
#                  + icons in ~/.local/share/icons/
#        - macOS : .app bundles in ~/Applications/
#                  (log output goes to ~/Library/Logs/ARS-Suite/)
#   3. Leaves all existing files (j-hub.json, logs, databases) untouched —
#      safe to run any number of times as an upgrade.

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
INSTALLER_JAR="$SCRIPT_DIR/installer/target/j-installer-1.0.8.jar"

command -v java >/dev/null 2>&1 || {
    echo "error: Java 21 is required but 'java' was not found on PATH."
    echo "       Install it first, then re-run $0"
    exit 1
}

if [[ ! -f "$INSTALLER_JAR" ]]; then
    echo "[bootstrap] Installer jar not found — building…"
    command -v mvn >/dev/null 2>&1 || {
        echo "error: Maven is required to build the installer but 'mvn' was not found."
        exit 1
    }
    mvn -q -DskipTests -f "$SCRIPT_DIR/installer/pom.xml" package
fi

exec java -jar "$INSTALLER_JAR" --root "$SCRIPT_DIR" "$@"
