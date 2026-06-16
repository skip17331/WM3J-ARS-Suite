#!/usr/bin/env bash
# Launch a solo J-Map / J-Sat from a JSON file on the Pi (or any box with a
# Liberica Full JDK 21). Copy this next to ars-fx-pi.jar and your launch JSON.
#
#   ./run-solo.sh j-map-remote.json
#
set -euo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
CFG="${1:-$DIR/j-map-remote.json}"
JAR="$DIR/ars-fx-pi.jar"

[ -f "$JAR" ] || { echo "ars-fx-pi.jar not found next to this script (build with build-pi.sh)"; exit 1; }
[ -f "$CFG" ] || { echo "launch file not found: $CFG"; exit 1; }

# Liberica Full puts JavaFX on the module path automatically, so a plain
# -jar works. (With a non-Full JDK, add: --module-path <javafx>/lib --add-modules javafx.controls)
exec java -jar "$JAR" --config "$CFG"
