#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# j-log depends on j-log-engine — install it first
mvn clean install -f "$SCRIPT_DIR/../j-log-engine/pom.xml"

mvn clean install

java \
    --module-path "$SCRIPT_DIR/lib/javafx" \
    --add-modules javafx.controls,javafx.fxml,javafx.media \
    -Dfile.encoding=UTF-8 \
    -jar "$SCRIPT_DIR/target/j-log-1.0.0-shaded.jar" \
    "$@"
