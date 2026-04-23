#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
JFX=/home/mike/ARS_Suite/j-log/lib/javafx
java --module-path "$JFX" --add-modules javafx.controls,javafx.fxml \
     -Dfile.encoding=UTF-8 \
     -jar target/j-wae-0.1.0-shaded.jar "$@"
