#!/usr/bin/env bash
set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$SCRIPT_DIR/target/morse-trainer-1.0.8.jar"
if [ ! -f "$JAR" ]; then
    echo "morse-trainer jar missing; building..."
    (cd "$SCRIPT_DIR" && mvn -q clean package)
fi
exec java -Dfile.encoding=UTF-8 -jar "$JAR" "$@"
