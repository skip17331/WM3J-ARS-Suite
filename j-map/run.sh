#!/usr/bin/env bash
# Usage:
#   ./run.sh                          # local mode — auto-starts j-hub if not running
#   ./run.sh --hub 192.168.1.50       # remote mode — connect to j-hub on another machine
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

mvn clean install -f "$SCRIPT_DIR/pom.xml"

echo "Starting J-Map..."
java \
    --module-path "$SCRIPT_DIR/lib/javafx" \
    --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.media,javafx.web \
    -Dfile.encoding=UTF-8 \
    -jar "$SCRIPT_DIR/target/j-map-1.0.0-fat.jar" \
    "$@"
