#!/usr/bin/env bash
#
# ARS Suite — Linux installer bootstrap.
#
# Assumes:
#   - Java 21 is installed          (java --version)
#   - Maven is installed            (mvn --version)
#   - All six modules have been built (mvn package each)
#
# What this script does:
#   1. Builds the installer jar if it isn't present.
#   2. Runs the installer, which writes .desktop entries for each built
#      module to ~/.local/share/applications/ and copies icons to
#      ~/.local/share/icons/.
#   3. Leaves all existing files (j-hub.json, logs, databases) untouched —
#      safe to run any number of times as an upgrade.

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
INSTALLER_JAR="$SCRIPT_DIR/installer/target/j-installer-1.0.0.jar"

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
