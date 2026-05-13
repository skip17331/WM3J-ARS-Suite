#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

java -Dfile.encoding=UTF-8 \
     -jar "$SCRIPT_DIR/target/j-vault-1.0.18.jar" \
     "$@"
