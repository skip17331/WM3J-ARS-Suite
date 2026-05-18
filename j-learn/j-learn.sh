#!/usr/bin/env bash
# J-Learn launcher - starts the standalone reference-library web app on
# port 8082. The app keeps running in the foreground; Ctrl-C stops it.
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# Resolve the jar by glob so a version bump never breaks this launcher.
# Newest match wins; skip sources/javadoc side-artifacts.
JAR="$(ls -t "$SCRIPT_DIR"/target/j-learn-*.jar 2>/dev/null | grep -Ev -- '-(sources|javadoc|shaded|fat)\.jar$' | head -n1)"

if [ -z "$JAR" ]; then
  echo "Error: j-learn jar not found in $SCRIPT_DIR/target" >&2
  echo "Build it first:  mvn -DskipTests -f \"$SCRIPT_DIR/pom.xml\" install" >&2
  exit 1
fi

exec java -jar "$JAR" "$@"
