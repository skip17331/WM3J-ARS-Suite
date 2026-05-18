#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$SCRIPT_DIR/target/j-log-1.0.52.jar"

if [ ! -f "$JAR" ]; then
    echo "j-log jar missing; building (j-log-engine first)..."
    mvn -q clean install -DskipTests -f "$SCRIPT_DIR/../j-log-engine/pom.xml"
    (cd "$SCRIPT_DIR" && mvn -q clean package -DskipTests)
fi

exec java -Dfile.encoding=UTF-8 -jar "$JAR" "$@"
