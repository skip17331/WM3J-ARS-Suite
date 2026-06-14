#!/usr/bin/env bash
# Launch the ARS Suite JavaFX app (new J-Hub + all module surfaces).
#   ./run.sh            -> opens J-Hub (default)
#   ./run.sh map        -> opens on a specific surface
#                          hub | hubcfg | log | map | sat | digi | bridge | vault | learn
# Navigate at runtime via the dock / J-Hub nav; ◑ Theme toggles light/dark.
cd "$(dirname "$0")" || exit 1
exec mvn -q javafx:run ${1:+-Dsurface="$1"}
