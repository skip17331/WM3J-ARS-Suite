#!/usr/bin/env bash
# Start / screenshot / stop the ARS Suite from source. Run from anywhere; paths are resolved from this file.
#   app.sh start [surface]   surface: hub (default) | map | sat | log | digi | bridge | vault | learn
#   app.sh shot OUT.png      focus the window and take a full-screen screenshot
#   app.sh stop              stop THIS dev run only (never the background hub)
UNIT="$(cd "$(dirname "$0")/../../.." && pwd)"          # .../ars-fx
LOG="${ARS_RUN_LOG:-/tmp/ars-fx-run.log}"

stop() { kill $(pgrep -f "[D]surface=") $(pgrep -f "[j]avafx:run") 2>/dev/null; sleep 2; }

case "$1" in
  start)
    stop
    (cd "$UNIT" && nohup ./run.sh ${2:+"$2"} > "$LOG" 2>&1 &)
    for i in $(seq 1 90); do grep -q "PTT method" "$LOG" && break; sleep 2; done   # the modem logs this once the app is up
    grep -q "PTT method" "$LOG" || { echo "app did not come up; see $LOG"; tail -20 "$LOG"; exit 1; }
    sleep 10; wmctrl -a "ARS Suite"; echo "up; log $LOG" ;;
  shot)
    wmctrl -a "ARS Suite"; sleep 1; gnome-screenshot -f "$2" && echo "$2" ;;
  stop)
    stop; echo "stopped" ;;
  *) sed -n '2,5p' "$0"; exit 2 ;;
esac
