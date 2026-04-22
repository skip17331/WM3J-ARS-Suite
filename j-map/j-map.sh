#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

mvn clean package -DskipTests -f "$SCRIPT_DIR/pom.xml"
mvn javafx:run -f "$SCRIPT_DIR/pom.xml" "$@"
