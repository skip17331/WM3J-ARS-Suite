#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# Resolve the jar by glob so a version bump never breaks this launcher.
# Newest match wins; skip sources/javadoc side-artifacts.
JAR="$(ls -t "$SCRIPT_DIR"/target/j-bridge-*.jar 2>/dev/null | grep -Ev -- '-(sources|javadoc|shaded|fat)\.jar$' | head -n1)"

if [ -z "$JAR" ]; then
    echo "j-bridge jar missing; building..."
    (cd "$SCRIPT_DIR" && mvn -q clean package -DskipTests)
    JAR="$(ls -t "$SCRIPT_DIR"/target/j-bridge-*.jar 2>/dev/null | grep -Ev -- '-(sources|javadoc|shaded|fat)\.jar$' | head -n1)"
fi

exec java -Dfile.encoding=UTF-8 -jar "$JAR" "$@"
