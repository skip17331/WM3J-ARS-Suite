#!/usr/bin/env bash
# Loose (un-docked) launch: one ARS Suite module in its own window, attached to a
# shared background hub. The hub owns the Hamlib daemons + live data feeds (DX
# cluster, RBN, rig, rotor) and sits in the system tray; each module you open is
# its own window talking to it over ws://127.0.0.1:8090.
#
#   ./run-loose.sh log      -> open J-Log in its own window
#   ./run-loose.sh map      -> open J-Map   (log | logc | map | sat | digi | vault | learn | bridge)
#   ./run-loose.sh hub      -> start ONLY the background hub (tray), open nothing
#
# The first module you open brings the hub up automatically; closing the last
# window leaves the hub running in the tray until you quit it from there.
#
# This is the building block a per-module desktop shortcut calls — one shortcut
# per module, each passing a different id. The docked app is unchanged: run.sh.
set -euo pipefail
DIR="$(cd "$(dirname "$0")/.." && pwd)"      # ars-fx module root (this script lives in launch/)
TARGET="$DIR/target"

# Resolve the runnable jar by glob so a version bump never breaks this launcher;
# prefer the desktop jar, then the Pi jar, then any non-sources/javadoc ars-fx jar.
JAR="$( { ls -t "$TARGET"/ars-fx-linux.jar "$TARGET"/ars-fx-pi.jar 2>/dev/null; \
          ls -t "$TARGET"/ars-fx-*.jar 2>/dev/null | grep -Ev -- '-(sources|javadoc)\.jar$'; } \
        | grep -v '/original-' | head -n1)"
if [ -z "${JAR:-}" ]; then
  echo "ars-fx jar not found in $TARGET — build it:  mvn -f \"$DIR/pom.xml\" -DskipTests package" >&2
  exit 1
fi

MOD="${1:-log}"
if [ "$MOD" = "hub" ]; then
  exec java -Dfile.encoding=UTF-8 -cp "$JAR" com.ars.fx.HubServer
fi
exec java -Dfile.encoding=UTF-8 -jar "$JAR" --module "$MOD"
