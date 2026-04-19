#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

java \
    --module-path "$SCRIPT_DIR/lib/javafx" \
    --add-modules javafx.controls,javafx.fxml \
    -Dfile.encoding=UTF-8 \
    -jar "$SCRIPT_DIR/target/j-hub-1.0.0.jar" \
    "$@"
