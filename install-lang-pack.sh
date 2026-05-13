#!/usr/bin/env bash
#
# Install one of the bundled language packs (de / fr / it / pt) into
# ~/.j-hub/lang/<module>/ so every module picks it up on next launch
# (or live, for modules that listen for STATION_CONFIG).
#
# Usage:
#   ./install-lang-pack.sh <lang>          # install for every module
#   ./install-lang-pack.sh <lang> <module> # install for one module only
#
#   lang   : de | fr | it | pt
#   module : j-digi | j-bridge | j-map | j-sat | morse-trainer
#            (en + es are embedded in every jar — no pack needed)
#
# Re-running is safe; it just overwrites the previous file.

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
SRC_ROOT="$SCRIPT_DIR/i18n-packs"
DEST_ROOT="$HOME/.j-hub/lang"

lang="$1"
module="$2"

if [[ -z "$lang" ]]; then
    echo "Usage: $0 <lang> [module]"
    echo "  lang   : de | fr | it | pt"
    echo "  module : j-digi | j-bridge | j-map | j-sat | morse-trainer"
    echo "           (omit to install pack for every module)"
    exit 2
fi

case "$lang" in
    de|fr|it|pt) ;;
    en|es)
        echo "$lang is embedded in every module — no pack to install."
        echo "Just set Language = $lang in J-Hub → Station → Regional Settings."
        exit 0 ;;
    *)
        echo "error: unknown language '$lang'. Supported: de, fr, it, pt." >&2
        exit 1 ;;
esac

if [[ ! -d "$SRC_ROOT" ]]; then
    echo "error: $SRC_ROOT not found. Run this from your ARS Suite checkout." >&2
    exit 1
fi

install_one() {
    local mod="$1"
    local src="$SRC_ROOT/$mod/messages_$lang.properties"
    if [[ ! -f "$src" ]]; then
        echo "  skip $mod — no $lang pack shipped for this module"
        return
    fi
    local dst_dir="$DEST_ROOT/$mod"
    mkdir -p "$dst_dir"
    cp "$src" "$dst_dir/messages_$lang.properties"
    echo "  installed $mod → $dst_dir/messages_$lang.properties"
}

echo "Installing $lang language pack…"
if [[ -n "$module" ]]; then
    install_one "$module"
else
    for mod in j-digi j-bridge j-map j-sat morse-trainer; do
        install_one "$mod"
    done
fi

echo
echo "Done. Set Language = $lang in J-Hub → Station → Regional Settings"
echo "(or pass --lang $lang to a module on next launch)."
