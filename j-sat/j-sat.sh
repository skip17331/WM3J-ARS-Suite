#!/usr/bin/env bash
# J-Sat launcher
# Usage:
#   ./j-sat.sh                      # connect to j-hub on localhost
#   ./j-sat.sh --hub 192.168.1.50   # connect to j-hub on remote host
#   ./j-sat.sh --launched-by-hub    # skip auto-start of j-hub
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR="$SCRIPT_DIR/target/j-sat-1.0.18.jar"

if [ ! -f "$JAR" ]; then
    echo "j-sat jar missing; building..."
    (cd "$SCRIPT_DIR" && mvn -q clean package -DskipTests)
fi

exec java -Dfile.encoding=UTF-8 -jar "$JAR" "$@"
