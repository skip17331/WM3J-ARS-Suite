#!/bin/sh
# ARS Suite - Linux self-update helper (POSIX sh, written so macOS can reuse it).
#
# Started by the app (UpdateApplier) right before it exits:
#   update-unix.sh APP_PID JAR NEW_JAR LAUNCHER LOG [STARTUP_SECS] [HUB_WAIT_SECS]
#
#   APP_PID        process id of the app that is exiting
#   JAR            the installed jar to replace (e.g. .../ars-fx/target/ars-fx-linux.jar)
#   NEW_JAR        the downloaded, checksum-verified jar
#   LAUNCHER       the installed ars-fx.sh
#   LOG            append progress here; "last-result.txt" is written next to it for the app to show
#   STARTUP_SECS   the new version must stay up this long to count as a good start (default 20)
#   HUB_WAIT_SECS  how long to let the background hub exit by itself (default 30)
#
# Exit: 0 updated, 1 failed (old version left in place and relaunched), 2 new version crashed (rolled back).

APP_PID=$1; JAR=$2; NEW=$3; LAUNCHER=$4; LOG=$5; STARTUP=${6:-20}; HUB_WAIT=${7:-30}
RESULT="$(dirname "$LOG")/last-result.txt"

log()    { printf '%s  %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*" >>"$LOG" 2>/dev/null; }
result() { printf '%s\n' "$*" >"$RESULT" 2>/dev/null; }
alive()  { kill -0 "$1" 2>/dev/null; }

# Pids of OTHER processes running this jar (the app, or the background hub it started). Never ourselves.
jar_pids() {
    command -v pgrep >/dev/null 2>&1 || return 0
    for p in $(pgrep -f "java.* $JAR" 2>/dev/null); do
        [ "$p" = "$$" ] && continue
        echo "$p"
    done
}
wait_gone() {   # wait_gone SECS - true once nothing is running the jar
    n=0
    while [ "$n" -lt "$1" ]; do
        [ -z "$(jar_pids)" ] && return 0
        sleep 1; n=$((n + 1))
    done
    [ -z "$(jar_pids)" ]
}

start_app() { nohup bash "$LAUNCHER" >/dev/null 2>&1 & APP_NEW_PID=$!; }

fail() {
    log "update failed: $*"
    result "failed: $*"
    [ -f "$JAR" ] && start_app      # leave the user with a working app
    exit 1
}

log "update start: app pid $APP_PID, jar $JAR, new $NEW"
[ -f "$NEW" ] || fail "downloaded jar is missing"
if command -v unzip >/dev/null 2>&1; then
    unzip -tq "$NEW" >/dev/null 2>&1 || fail "downloaded file is not a valid jar"
fi

# 1. the app that asked for the update is exiting - wait for it
n=0; while alive "$APP_PID" && [ "$n" -lt 60 ]; do sleep 1; n=$((n + 1)); done

# 2. the background hub must not keep running the old code. An auto-started hub quits by itself a few
#    seconds after its last client leaves, and its shutdown hook un-keys the transmitter, so give it time.
#    Anything left gets SIGTERM (shutdown hooks still run); SIGKILL is the very last resort.
if ! wait_gone "$HUB_WAIT"; then
    log "hub still running - asking it to stop"
    for p in $(jar_pids); do kill -TERM "$p" 2>/dev/null; done
    if ! wait_gone 15; then
        log "hub ignored SIGTERM - killing"
        for p in $(jar_pids); do kill -KILL "$p" 2>/dev/null; done
        wait_gone 5 || fail "a process is still running the old version"
    fi
fi

# 3. swap. Copy next to the target first so the final mv is a same-filesystem atomic rename.
BAK="$JAR.bak"; STAGE="$JAR.new"
cp -f "$NEW" "$STAGE"            || fail "could not stage the new jar (disk full or no permission?)"
rm -f "$BAK"
mv -f "$JAR" "$BAK"              || { rm -f "$STAGE"; fail "could not move the current jar aside"; }
if ! mv -f "$STAGE" "$JAR"; then
    mv -f "$BAK" "$JAR"; rm -f "$STAGE"; fail "could not install the new jar"
fi
rm -f "$NEW"
log "swapped in the new jar"

# 4. relaunch and make sure it stays up
start_app
i=0
while [ "$i" -lt "$STARTUP" ]; do
    sleep 1; i=$((i + 1))
    if ! alive "$APP_NEW_PID"; then
        log "new version exited after ${i}s - rolling back"
        mv -f "$JAR" "$JAR.failed"
        mv -f "$BAK" "$JAR"
        start_app
        log "rolled back to the previous version"
        result "rolled back: the new version did not start, so the previous version was restored"
        exit 2
    fi
done
log "new version is running; previous jar kept as .bak"
result "ok"
exit 0
