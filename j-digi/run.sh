#!/usr/bin/env bash
# J-Digi — Run script for Linux
# Requires Java 17+ and JavaFX 21 runtime under ./lib/javafx

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$SCRIPT_DIR/target/j-digi-0.1.0-jar-with-dependencies.jar"

if [ ! -f "$JAR" ]; then
    echo "J-Digi: jar not found — building..."
    cd "$SCRIPT_DIR"
    mvn -q package -DskipTests
fi

echo "Starting J-Digi..."
java \
    --module-path "$SCRIPT_DIR/lib/javafx" \
    --add-modules javafx.controls,javafx.graphics \
    -Dfile.encoding=UTF-8 \
    -jar "$JAR" \
    "$@"
