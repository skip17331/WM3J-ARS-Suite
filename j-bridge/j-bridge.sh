#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

mvn clean install

java \
    --module-path "$SCRIPT_DIR/lib/javafx" \
    --add-modules javafx.controls,javafx.fxml \
    -Dfile.encoding=UTF-8 \
    -jar "$SCRIPT_DIR/target/j-bridge-1.0.0-shaded.jar" \
    "$@"
