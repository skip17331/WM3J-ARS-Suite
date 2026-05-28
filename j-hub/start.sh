#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# Resolve the jar by glob so a version bump never breaks this launcher.
# Prefer the runnable fat/uber jar (shade adds a -shaded/-fat classifier);
# fall back to the in-place shaded main jar; skip thin original-/sources/javadoc.
JAR="$( { ls -t "$SCRIPT_DIR"/target/j-hub-*-shaded.jar "$SCRIPT_DIR"/target/j-hub-*-fat.jar "$SCRIPT_DIR"/target/j-hub-*-jar-with-dependencies.jar 2>/dev/null; ls -t "$SCRIPT_DIR"/target/j-hub-*.jar 2>/dev/null | grep -Ev -- '-(sources|javadoc)\.jar$'; } | grep -v '/original-' | head -n1)"

if [ -z "$JAR" ]; then
  echo "Error: j-hub jar not found in $SCRIPT_DIR/target - build it:  mvn -DskipTests -f \"$SCRIPT_DIR/pom.xml\" package" >&2
  exit 1
fi

cd "$SCRIPT_DIR"
exec java \
    -Dfile.encoding=UTF-8 \
    -jar "$JAR" \
    "$@"
