#!/usr/bin/env bash
# J-Sat launcher
# Usage:
#   ./j-sat.sh                      # connect to j-hub on localhost
#   ./j-sat.sh --hub 192.168.1.50   # connect to j-hub on remote host
#   ./j-sat.sh --launched-by-hub    # skip auto-start of j-hub
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# Resolve the jar by glob so a version bump never breaks this launcher.
# Prefer the runnable fat/uber jar (shade adds a -shaded/-fat classifier);
# fall back to the in-place shaded main jar; skip thin original-/sources/javadoc.
JAR="$( { ls -t "$SCRIPT_DIR"/target/j-sat-*-shaded.jar "$SCRIPT_DIR"/target/j-sat-*-fat.jar "$SCRIPT_DIR"/target/j-sat-*-jar-with-dependencies.jar 2>/dev/null; ls -t "$SCRIPT_DIR"/target/j-sat-*.jar 2>/dev/null | grep -Ev -- '-(sources|javadoc)\.jar$'; } | grep -v '/original-' | head -n1)"

if [ -z "$JAR" ]; then
    echo "j-sat jar missing; building..."
    (cd "$SCRIPT_DIR" && mvn -q clean package -DskipTests)
    JAR="$( { ls -t "$SCRIPT_DIR"/target/j-sat-*-shaded.jar "$SCRIPT_DIR"/target/j-sat-*-fat.jar "$SCRIPT_DIR"/target/j-sat-*-jar-with-dependencies.jar 2>/dev/null; ls -t "$SCRIPT_DIR"/target/j-sat-*.jar 2>/dev/null | grep -Ev -- '-(sources|javadoc)\.jar$'; } | grep -v '/original-' | head -n1)"
fi

cd "$SCRIPT_DIR"
exec java -Dfile.encoding=UTF-8 -jar "$JAR" "$@"
