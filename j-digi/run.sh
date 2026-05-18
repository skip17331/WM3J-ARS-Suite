#!/usr/bin/env bash
# J-Digi — Run script for Linux
# Requires Java 17+ and JavaFX 21 runtime under ./lib/javafx

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$SCRIPT_DIR/target/j-digi-1.0.41.jar"

mvn clean install -f "$SCRIPT_DIR/pom.xml"

echo "Starting J-Digi..."
java \
    -Dfile.encoding=UTF-8 \
    -jar "$JAR" \
    "$@"
