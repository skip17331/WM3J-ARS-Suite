#!/usr/bin/env bash
# j-Log — Run script for Linux
# Requires Java 21+ and JavaFX 21 runtime under ./lib/javafx

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$SCRIPT_DIR/target/j-log-1.0.0-shaded.jar"

if [ ! -f "$JAR" ]; then
    echo "j-Log: jar not found — building..."
    cd "$SCRIPT_DIR"
    mvn -q package -DskipTests
fi

echo "Starting j-Log..."
java \
    --module-path "$SCRIPT_DIR/lib/javafx" \
    --add-modules javafx.controls,javafx.fxml,javafx.media \
    -Dfile.encoding=UTF-8 \
    -jar "$JAR" \
    "$@"
