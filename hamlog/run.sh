#!/usr/bin/env bash
# HamLog — Run script for Linux
# Requires Java 21+ and JavaFX 21 runtime on the module path
# OR use the fat jar produced by mvn package

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$SCRIPT_DIR/target/hamlog-1.0.0-shaded.jar"

if [ ! -f "$JAR" ]; then
    echo "Building HamLog..."
    cd "$SCRIPT_DIR"
    mvn -q package -DskipTests
fi

echo "Starting HamLog..."
java \
    --module-path "$SCRIPT_DIR/lib/javafx" \
    --add-modules javafx.controls,javafx.fxml,javafx.media \
    -Dfile.encoding=UTF-8 \
    -jar "$JAR" \
    "$@"
