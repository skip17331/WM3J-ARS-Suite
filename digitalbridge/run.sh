#!/usr/bin/env bash
# Digital Bridge — Run script
# Part of the WM3j ARS Suite
# Requires Java 21+ and JavaFX 21 runtime on the module path.
# JavaFX libs expected at ./lib/javafx — same location as j-log.
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$SCRIPT_DIR/target/digital-bridge-1.0.0-shaded.jar"

if [ ! -f "$JAR" ]; then
    echo "JAR not found — building Digital Bridge..."
    cd "$SCRIPT_DIR"
    mvn -q package -DskipTests
fi

echo "Starting Digital Bridge..."
java \
    --module-path "$SCRIPT_DIR/lib/javafx" \
    --add-modules javafx.controls,javafx.fxml \
    -Dfile.encoding=UTF-8 \
    -jar "$JAR" \
    "$@"
