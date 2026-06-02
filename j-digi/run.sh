#!/usr/bin/env bash
# J-Digi — Run script for Linux
# Requires Java 17+ and JavaFX 21 runtime under ./lib/javafx

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

mvn clean install -f "$SCRIPT_DIR/pom.xml"

# Resolve the built jar by glob so a version bump never breaks this script
# (skip thin original-/sources/javadoc jars).
JAR="$(ls -t "$SCRIPT_DIR"/target/j-digi-*.jar 2>/dev/null | grep -Ev -- '-(sources|javadoc)\.jar$' | grep -v '/original-' | head -n1)"

echo "Starting J-Digi... ($(basename "$JAR"))"
java \
    -Dfile.encoding=UTF-8 \
    -jar "$JAR" \
    "$@"
