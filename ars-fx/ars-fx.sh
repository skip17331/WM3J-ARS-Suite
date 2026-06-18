#!/usr/bin/env bash
# Production launcher for the installed ARS Suite (runs the packaged jar — the
# dev counterpart is run.sh, which uses mvn javafx:run).
#
#   ars-fx.sh                 -> docked app: J-Hub + all modules in one window
#   ars-fx.sh --module log    -> one module un-docked (loose), attached to the hub
#                                (log | logc | map | sat | digi | vault | learn | bridge)
#   ars-fx.sh --hub           -> background hub only (system tray), open nothing
#
# The installer's shortcuts all call this script with different args (see
# installer/src/main/resources/manifest.json), so the docked, loose, and hub
# entries share one launcher off the single ars-fx jar.
set -euo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
TARGET="$DIR/target"

# Resolve the runnable jar by glob so a version/dist bump never breaks this launcher.
JAR="$( { ls -t "$TARGET"/ars-fx-linux.jar "$TARGET"/ars-fx-pi.jar 2>/dev/null; \
          ls -t "$TARGET"/ars-fx-*.jar 2>/dev/null | grep -Ev -- '-(sources|javadoc)\.jar$'; } \
        | grep -v '/original-' | head -n1)"
if [ -z "${JAR:-}" ]; then
  echo "ars-fx jar not found in $TARGET — build it:  mvn -f \"$DIR/pom.xml\" -DskipTests package" >&2
  exit 1
fi

cd "$DIR"
if [ "${1:-}" = "--hub" ]; then
  exec java -Dfile.encoding=UTF-8 -cp "$JAR" com.ars.fx.HubServer
fi
exec java -Dfile.encoding=UTF-8 -jar "$JAR" "$@"
