#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"
# JavaFX libs live in the sibling j-log module; override with $JFX env var if
# they're somewhere else.
JFX="${JFX:-$SCRIPT_DIR/../j-log/lib/javafx}"
java --module-path "$JFX" --add-modules javafx.controls,javafx.fxml \
     -Dfile.encoding=UTF-8 \
     -jar target/j-wae-0.1.0-shaded.jar "$@"
