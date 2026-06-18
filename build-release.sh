#!/usr/bin/env bash
#
# WM3J ARS Suite — release builder for the ars-fx app.
# Part of the WM3J ARS Suite.  Requires: Java 21, Maven 3.x.
#
# The suite ships as ONE app (ars-fx): a single fat jar per desktop OS plus a
# JavaFX-less Raspberry Pi jar. Each profile writes a DISTINCT finalName
# (ars-fx-<dist>.jar), so unlike the old per-module builds there's no same-name
# "last profile wins" trap — all targets can sit in target/ at once. We still
# rebuild the linux desktop jar LAST so the host-runnable one is what's left
# behind for local runs.
#
# A native-leak guard (--verify, also run at the end of a full build) asserts
# each desktop jar carries ONLY its own platform's JavaFX prism native — OpenJFX's
# classified POMs are self-referential (javafx-base:win pulls javafx-base:linux),
# so a missing exclusion would silently leak foreign natives into the fat jar.
#
# USAGE
#   ./build-release.sh            full build into dist/ + native guard
#   ./build-release.sh --verify   guard only: check dist/ jars aren't cross-contaminated
#
# Override Maven (e.g. offline) with:  MVN="mvn -o -q -DskipTests" ./build-release.sh
set -euo pipefail
cd "$(dirname "$0")"
ROOT="$(pwd)"
DIST="$ROOT/dist"
MVN="${MVN:-mvn -q -DskipTests}"

ars_fx_version() { grep -m1 '<version>' ars-fx/pom.xml | sed -E 's/.*<version>([^<]+)<.*/\1/'; }

# Build the ars-fx app: one fat jar per desktop OS + the Pi jar, classified into dist/.
build_ars_fx() {
  local ver; ver="$(ars_fx_version)"
  echo "==> installing shared libs to local repo (common, engine, j-digi, j-bridge)"
  for lib in j-log-common j-log-engine j-digi j-bridge; do ( cd "$lib" && $MVN clean install ); done
  echo "==> ars-fx per-platform fat jars (v$ver)"
  ( cd ars-fx && $MVN clean package );               cp ars-fx/target/ars-fx-linux.jar       "$DIST/ars-fx-${ver}-linux-x86_64.jar"
  ( cd ars-fx && $MVN -Pwin clean package );         cp ars-fx/target/ars-fx-windows.jar     "$DIST/ars-fx-${ver}-windows-x86_64.jar"
  ( cd ars-fx && $MVN -Pmac clean package );         cp ars-fx/target/ars-fx-mac.jar         "$DIST/ars-fx-${ver}-macos-x86_64.jar"
  ( cd ars-fx && $MVN -Pmac-aarch64 clean package ); cp ars-fx/target/ars-fx-mac-aarch64.jar "$DIST/ars-fx-${ver}-macos-aarch64.jar"
  ( cd ars-fx && $MVN -Ppi clean package );          cp ars-fx/target/ars-fx-pi.jar          "$DIST/ars-fx-${ver}-linux-aarch64.jar"
  ( cd ars-fx && $MVN clean package )                # LAST → host-runnable linux jar stays in target/
}

# Each desktop jar must contain its own platform's JavaFX prism native and NONE of the
# others'. (Root-level JavaFX native names only; the bundled sqlite/jssc cross-platform
# natives live under paths and are matched exactly, so they don't trip this.)
verify_dist() {
  local fail=0 j
  # Capture entries first: `unzip | grep -q` would SIGPIPE unzip (grep exits early),
  # and pipefail would then report the match as a failure.
  _has()  { local e; e="$(unzip -Z1 "$1" 2>/dev/null || true)"; grep -qxE "$2" <<<"$e"; }
  _need() { _has "$1" "$2" || { echo "   !! $(basename "$1"): missing $3";  fail=1; }; }
  _deny() { _has "$1" "$2" && { echo "   !! $(basename "$1"): leaked $3";   fail=1; } || true; }
  echo "==> Verifying per-platform JavaFX natives in dist/"
  for j in "$DIST"/ars-fx-*-linux-x86_64.jar;  do [ -e "$j" ] || continue
    _need "$j" 'libprism_es2\.so'     'linux prism native';  _deny "$j" 'glass\.dll' 'windows .dll';        _deny "$j" 'libprism_es2\.dylib' 'macos .dylib'; done
  for j in "$DIST"/ars-fx-*-windows-x86_64.jar; do [ -e "$j" ] || continue
    _need "$j" 'glass\.dll'           'windows native';      _deny "$j" 'libprism_es2\.so' 'linux .so';     _deny "$j" 'libprism_es2\.dylib' 'macos .dylib'; done
  for j in "$DIST"/ars-fx-*-macos-*.jar;        do [ -e "$j" ] || continue
    _need "$j" 'libprism_es2\.dylib'  'macos prism native';  _deny "$j" 'libprism_es2\.so' 'linux .so';     _deny "$j" 'glass\.dll' 'windows .dll'; done
  [ "$fail" = 0 ] && echo "   ok — every desktop jar is platform-clean" || { echo "GUARD FAILED — a jar carries foreign/absent natives."; return 1; }
}

full_release() {
  echo "==> Clean dist/"
  rm -rf "$DIST"; mkdir -p "$DIST"
  build_ars_fx
  verify_dist
  echo
  echo "==> Release artifacts in $DIST :"
  ls -1 "$DIST"
  echo "==> target/ holds the host-runnable ars-fx-linux.jar for local runs."
}

case "${1:-}" in
  --verify) verify_dist ;;
  "" )      full_release ;;
  *) echo "usage: $0 [--verify]"; exit 2 ;;
esac
