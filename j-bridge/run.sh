#!/usr/bin/env bash
# J-Bridge — Run script
# Part of the WM3j ARS Suite
# Requires Java 21+. The jar is a shaded fat jar carrying JavaFX.
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Resolve the built jar by glob so a version bump never breaks this script
# (skip thin original-/sources/javadoc jars).
find_jar() {
    ls -t "$SCRIPT_DIR"/target/j-bridge-*.jar 2>/dev/null \
        | grep -Ev -- '-(sources|javadoc)\.jar$' | grep -v '/original-' | head -n1 || true
}
JAR="$(find_jar)"

if [ -z "$JAR" ]; then
    echo "JAR not found — building J-Bridge..."
    (cd "$SCRIPT_DIR" && mvn -q package -DskipTests)
    JAR="$(find_jar)"
fi

echo "Starting J-Bridge..."
java \
    -Dfile.encoding=UTF-8 \
    -jar "$JAR" \
    "$@"
