#!/usr/bin/env bash
# Wrapper used by J-Hub's AppLauncher.
exec "$(dirname "$0")/start.sh" "$@"
