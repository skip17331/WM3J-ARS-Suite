#!/usr/bin/env bash
# Pipe a CW test WAV (or the whole suite) into a virtual PulseAudio sink
# so j-digi can pick it up as its mic input.
#
# Usage:
#   ./run_cw_test.sh                       # play every WAV in cw_test_suite/
#   ./run_cw_test.sh cw_20wpm_700hz_soft   # play one variant (with or without .wav)
#   ./run_cw_test.sh list                  # show variants
#   ./run_cw_test.sh unload                # remove the virtual sink
#
# In j-digi, select input device:  "Monitor of JDigiLoop"
set -euo pipefail

cd "$(dirname "$0")"

SINK_NAME="jdigi_loop"
SUITE_DIR="cw_test_suite"
GAP_SECONDS=1.5

sink_loaded() {
    pactl list short sinks | awk '{print $2}' | grep -qx "$SINK_NAME"
}

load_sink() {
    if sink_loaded; then
        echo "[loop] sink '$SINK_NAME' already loaded"
        return
    fi
    pactl load-module module-null-sink \
        sink_name="$SINK_NAME" \
        sink_properties=device.description=JDigiLoop >/dev/null
    echo "[loop] created sink '$SINK_NAME' — pick 'Monitor of JDigiLoop' in j-digi"
}

unload_sink() {
    local id
    id=$(pactl list short modules | awk -v s="sink_name=$SINK_NAME" '$0 ~ s {print $1}')
    if [[ -n "$id" ]]; then
        pactl unload-module "$id"
        echo "[loop] unloaded sink '$SINK_NAME'"
    else
        echo "[loop] sink '$SINK_NAME' not loaded"
    fi
}

play_one() {
    local wav="$1"
    if [[ ! -f "$wav" ]]; then
        echo "[loop] not found: $wav" >&2
        return 1
    fi
    echo "[loop] >>> $wav"
    paplay -d "$SINK_NAME" "$wav"
}

case "${1:-all}" in
    list)
        ls "$SUITE_DIR"/*.wav | sed "s|$SUITE_DIR/||; s|\.wav$||"
        exit 0
        ;;
    unload)
        unload_sink
        exit 0
        ;;
    all)
        load_sink
        for wav in "$SUITE_DIR"/*.wav; do
            play_one "$wav"
            sleep "$GAP_SECONDS"
        done
        ;;
    *)
        load_sink
        name="${1%.wav}"
        play_one "$SUITE_DIR/${name}.wav"
        ;;
esac
