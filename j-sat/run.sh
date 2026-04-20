#!/usr/bin/env bash
# J-Sat launcher
# Usage:
#   ./run.sh                      # connect to j-hub on localhost
#   ./run.sh --hub 192.168.1.50   # connect to j-hub on remote host
#   ./run.sh --launched-by-hub    # skip auto-start of j-hub

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

mvn javafx:run -q -Djavafx.run.args="$*"
