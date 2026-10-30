#!/usr/bin/env bash
# Pipe a PSK31 test WAV (or the whole suite) into the JDigiLoop sink.
# Usage matches run_cw_test.sh — see that file for details.
set -euo pipefail

cd "$(dirname "$0")"

SINK_NAME="jdigi_loop"
SUITE_DIR="psk31_test_suite"
GAP_SECONDS=3

sink_loaded() { pactl list short sinks | awk '{print $2}' | grep -qx "$SINK_NAME"; }

load_sink() {
    if sink_loaded; then return; fi
    pactl load-module module-null-sink \
        sink_name="$SINK_NAME" \
        sink_properties=device.description=JDigiLoop >/dev/null
    pactl set-default-source "${SINK_NAME}.monitor" >/dev/null
    echo "[loop] created sink '$SINK_NAME'"
}

case "${1:-all}" in
    list)
        ls "$SUITE_DIR"/*.wav | sed "s|$SUITE_DIR/||; s|\.wav$||"
        ;;
    all)
        load_sink
        for wav in "$SUITE_DIR"/*.wav; do
            echo "[loop] >>> $wav"
            paplay -d "$SINK_NAME" "$wav"
            sleep "$GAP_SECONDS"
        done
        ;;
    *)
        load_sink
        name="${1%.wav}"
        echo "[loop] >>> $SUITE_DIR/${name}.wav"
        paplay -d "$SINK_NAME" "$SUITE_DIR/${name}.wav"
        ;;
esac
