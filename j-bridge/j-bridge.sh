#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$SCRIPT_DIR/target/j-bridge-1.0.17.jar"

if [ ! -f "$JAR" ]; then
    echo "j-bridge jar missing; building..."
    (cd "$SCRIPT_DIR" && mvn -q clean package -DskipTests)
fi

exec java -Dfile.encoding=UTF-8 -jar "$JAR" "$@"
