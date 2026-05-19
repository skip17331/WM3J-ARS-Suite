#!/usr/bin/env bash
set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# Resolve the jar by glob so a version bump never breaks this launcher.
# Prefer the runnable fat/uber jar (shade adds a -shaded/-fat classifier);
# fall back to the in-place shaded main jar; skip thin original-/sources/javadoc.
JAR="$( { ls -t "$SCRIPT_DIR"/target/morse-trainer-*-shaded.jar "$SCRIPT_DIR"/target/morse-trainer-*-fat.jar "$SCRIPT_DIR"/target/morse-trainer-*-jar-with-dependencies.jar 2>/dev/null; ls -t "$SCRIPT_DIR"/target/morse-trainer-*.jar 2>/dev/null | grep -Ev -- '-(sources|javadoc)\.jar$'; } | grep -v '/original-' | head -n1)"
if [ -z "$JAR" ]; then
    echo "morse-trainer jar missing; building..."
    (cd "$SCRIPT_DIR" && mvn -q clean package)
    JAR="$( { ls -t "$SCRIPT_DIR"/target/morse-trainer-*-shaded.jar "$SCRIPT_DIR"/target/morse-trainer-*-fat.jar "$SCRIPT_DIR"/target/morse-trainer-*-jar-with-dependencies.jar 2>/dev/null; ls -t "$SCRIPT_DIR"/target/morse-trainer-*.jar 2>/dev/null | grep -Ev -- '-(sources|javadoc)\.jar$'; } | grep -v '/original-' | head -n1)"
fi
exec java -Dfile.encoding=UTF-8 -jar "$JAR" "$@"
