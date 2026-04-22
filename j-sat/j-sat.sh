#!/usr/bin/env bash
# J-Sat launcher
# Usage:
#   ./j-sat.sh                      # connect to j-hub on localhost
#   ./j-sat.sh --hub 192.168.1.50   # connect to j-hub on remote host
#   ./j-sat.sh --launched-by-hub    # skip auto-start of j-hub

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

mvn clean package -DskipTests -f "$SCRIPT_DIR/pom.xml"
mvn javafx:run -f "$SCRIPT_DIR/pom.xml" -Djavafx.args="$*"
