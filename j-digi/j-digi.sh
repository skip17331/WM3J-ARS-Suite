#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

mvn clean install

java \
    --module-path "$SCRIPT_DIR/lib/javafx" \
    --add-modules javafx.controls,javafx.graphics \
    -Dfile.encoding=UTF-8 \
    -jar "$SCRIPT_DIR/target/j-digi-0.1.0-jar-with-dependencies.jar" \
    "$@"
