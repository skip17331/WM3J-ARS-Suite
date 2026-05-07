#!/usr/bin/env bash
# J-Learn launcher — opens the J-Learn tab in J-Hub's web UI.
# If J-Hub isn't running, starts it in the background first.
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
HUB_DIR="$SCRIPT_DIR/../j-hub"
HUB_PORT=8081
LEARN_URL="http://localhost:${HUB_PORT}/#learn"

is_hub_up() {
  curl -fsS -o /dev/null --max-time 1 "http://localhost:${HUB_PORT}/" 2>/dev/null
}

open_browser() {
  if command -v xdg-open >/dev/null 2>&1; then
    xdg-open "$1" >/dev/null 2>&1 &
  elif command -v open >/dev/null 2>&1; then
    open "$1"
  else
    echo "Open this URL in your browser: $1"
  fi
}

if is_hub_up; then
  echo "J-Hub already running."
  open_browser "$LEARN_URL"
  exit 0
fi

if [ ! -d "$HUB_DIR" ]; then
  echo "Error: cannot find j-hub at $HUB_DIR" >&2
  exit 1
fi

if [ ! -f "$HUB_DIR/target/j-hub-1.0.0.jar" ]; then
  echo "Error: j-hub jar not found at $HUB_DIR/target/j-hub-1.0.0.jar" >&2
  echo "Build it first: (cd $HUB_DIR && mvn package -DskipTests)" >&2
  exit 1
fi

echo "Starting J-Hub..."
mkdir -p "$HUB_DIR/logs"
( cd "$HUB_DIR" && nohup bash ./start.sh > logs/j-learn-launcher.log 2>&1 & )

for _ in $(seq 1 30); do
  if is_hub_up; then
    open_browser "$LEARN_URL"
    echo "J-Hub up. J-Learn tab opened in browser."
    exit 0
  fi
  sleep 1
done

echo "Error: J-Hub did not come up within 30 seconds." >&2
echo "Check $HUB_DIR/logs/j-learn-launcher.log" >&2
exit 1
