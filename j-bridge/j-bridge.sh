#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# Resolve the jar by glob so a version bump never breaks this launcher.
# Prefer the runnable fat/uber jar (shade adds a -shaded/-fat classifier);
# fall back to the in-place shaded main jar; skip thin original-/sources/javadoc.
JAR="$( { ls -t "$SCRIPT_DIR"/target/j-bridge-*-shaded.jar "$SCRIPT_DIR"/target/j-bridge-*-fat.jar "$SCRIPT_DIR"/target/j-bridge-*-jar-with-dependencies.jar 2>/dev/null; ls -t "$SCRIPT_DIR"/target/j-bridge-*.jar 2>/dev/null | grep -Ev -- '-(sources|javadoc)\.jar$'; } | grep -v '/original-' | head -n1)"

if [ -z "$JAR" ]; then
    echo "j-bridge jar missing; building..."
    (cd "$SCRIPT_DIR" && mvn -q clean package -DskipTests)
    JAR="$( { ls -t "$SCRIPT_DIR"/target/j-bridge-*-shaded.jar "$SCRIPT_DIR"/target/j-bridge-*-fat.jar "$SCRIPT_DIR"/target/j-bridge-*-jar-with-dependencies.jar 2>/dev/null; ls -t "$SCRIPT_DIR"/target/j-bridge-*.jar 2>/dev/null | grep -Ev -- '-(sources|javadoc)\.jar$'; } | grep -v '/original-' | head -n1)"
fi

cd "$SCRIPT_DIR"
exec java -Dfile.encoding=UTF-8 -jar "$JAR" "$@"
