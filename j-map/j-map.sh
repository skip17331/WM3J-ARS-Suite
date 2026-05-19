#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# Resolve the jar by glob so a version bump never breaks this launcher.
# Prefer the runnable fat/uber jar (shade adds a -shaded/-fat classifier);
# fall back to the in-place shaded main jar; skip thin original-/sources/javadoc.
JAR="$( { ls -t "$SCRIPT_DIR"/target/j-map-*-shaded.jar "$SCRIPT_DIR"/target/j-map-*-fat.jar "$SCRIPT_DIR"/target/j-map-*-jar-with-dependencies.jar 2>/dev/null; ls -t "$SCRIPT_DIR"/target/j-map-*.jar 2>/dev/null | grep -Ev -- '-(sources|javadoc)\.jar$'; } | grep -v '/original-' | head -n1)"

if [ -z "$JAR" ]; then
    echo "j-map jar missing; building..."
    (cd "$SCRIPT_DIR" && mvn -q clean package -DskipTests)
    JAR="$( { ls -t "$SCRIPT_DIR"/target/j-map-*-shaded.jar "$SCRIPT_DIR"/target/j-map-*-fat.jar "$SCRIPT_DIR"/target/j-map-*-jar-with-dependencies.jar 2>/dev/null; ls -t "$SCRIPT_DIR"/target/j-map-*.jar 2>/dev/null | grep -Ev -- '-(sources|javadoc)\.jar$'; } | grep -v '/original-' | head -n1)"
fi

exec java -Dfile.encoding=UTF-8 -jar "$JAR" "$@"
