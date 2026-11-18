#!/usr/bin/env bash
# Start, drive, screenshot and stop a dev run of the ARS Suite. Works on any Linux desktop (X11 or Wayland): the app takes
# its own screenshots and sends its own input through DevHook (-Dars.devhook), so no wmctrl / xdotool / screen grabber.
#   app.sh start [surface]      hub (default) | map | sat | log | digi | bridge | vault | learn; waits until it answers
#   app.sh shot OUT.png         the app window only (scene pixels; other windows can never appear in it)
#   app.sh size                 window size in scene pixels: "ok W H"
#   app.sh click X Y | dbl X Y | drag X1 Y1 X2 Y2 | wheel X Y N | type TEXT | key NAME [N] | nav SURFACE
#   app.sh stop                 stop THIS dev run only (never the background hub)
# Coordinates are pixels of a `shot` picture. Keys are JavaFX KeyCode names: ENTER, BACK_SPACE, TAB, ESCAPE, F1, ...
UNIT="$(cd "$(dirname "$0")/../../.." && pwd)"          # .../ars-fx
LOG="${ARS_RUN_LOG:-/tmp/ars-fx-run.log}"
HOOK="${ARS_DEVHOOK:-/tmp/ars-fx-devhook}"

stop() { kill $(pgrep -f "[D]surface=") $(pgrep -f "[j]avafx:run") 2>/dev/null; sleep 2; }

# send one command to the running app and print its answer
cmd() {
  [ -d "$HOOK" ] || { echo "error: no dev run with a hook ($HOOK); use: app.sh start"; return 1; }
  local id="c$$_$RANDOM"
  rm -f "$HOOK/$id.done"
  printf '%s\n' "$*" > "$HOOK/$id.tmp" && mv "$HOOK/$id.tmp" "$HOOK/$id.cmd"
  for i in $(seq 1 100); do
    if [ -f "$HOOK/$id.done" ]; then
      local r; r="$(cat "$HOOK/$id.done")"; rm -f "$HOOK/$id.done"; echo "$r"
      [[ "$r" == ok* ]]; return
    fi
    sleep 0.2
  done
  rm -f "$HOOK/$id.cmd"; echo "error: no answer in 20 s (is the app up? see $LOG)"; return 1
}

case "$1" in
  start)
    stop
    rm -rf "$HOOK"; mkdir -p "$HOOK"
    (cd "$UNIT" && nohup mvn -q javafx:run -Dsurface="${2:-hub}" -Dars.devhook="$HOOK" > "$LOG" 2>&1 < /dev/null &)
    for i in $(seq 1 90); do grep -q "\[devhook\] watching" "$LOG" && break; sleep 2; done
    grep -q "\[devhook\] watching" "$LOG" || { echo "app did not come up; see $LOG"; tail -20 "$LOG"; exit 1; }
    sleep 8; cmd size >/dev/null; echo "up; log $LOG" ;;
  shot)
    out="$(cd "$(dirname "$2")" && pwd)/$(basename "$2")"; cmd snap "$out" ;;
  size|click|dbl|drag|wheel|type|key|nav)
    cmd "$@" ;;
  stop)
    stop; rm -rf "$HOOK"; echo "stopped" ;;
  *) sed -n '3,9p' "$0"; exit 2 ;;
esac
