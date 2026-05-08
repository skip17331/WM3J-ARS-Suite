#!/usr/bin/env bash
# J-Learn launcher — starts the standalone reference-library web app on
# port 8082. The app keeps running in the foreground; Ctrl-C stops it.
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$SCRIPT_DIR/target/j-learn-1.1.0.jar"

if [ ! -f "$JAR" ]; then
  echo "Error: j-learn jar not found at $JAR" >&2
  echo "Build it first:  mvn -DskipTests -f \"$SCRIPT_DIR/pom.xml\" install" >&2
  exit 1
fi

exec java -jar "$JAR" "$@"
