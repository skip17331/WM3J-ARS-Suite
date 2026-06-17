#!/usr/bin/env bash
#
# WM3J ARS Suite — reproducible release builder + arch guard
# Part of the WM3J ARS Suite.  Requires: Java 21, Maven 3.x.
#
# WHY THIS EXISTS
#   The JavaFX modules ship one fat jar per CPU-arch family (a single jar can't
#   hold two same-OS arches — the JavaFX native filenames collide). The shade
#   plugin always writes the SAME bare filename (e.g. j-hub-1.5.0.jar), so the
#   per-arch artifacts are made by building a profile and copying that bare jar
#   to a classified name. The trap: whichever profile builds LAST leaves its jar
#   in target/. If that's `arm-release` (linux-aarch64, JavaFX 23.0.1), the dev
#   box then can't launch anything — FX dies with
#     UnsatisfiedLinkError: libprism_es2.so ... can't load AARCH64 .so on AMD 64
#   and since JHubMain boots the FX toolkit before the servers, J-Hub never
#   binds 8080/8081. (Happened 2026-06-02; see memory project_launcher_jar_resolution.)
#
# WHAT THIS GUARANTEES
#   - The cross-platform (universal) build runs LAST for every JavaFX module, so
#     the bare jar left in target/ always carries host-runnable natives.
#   - A final GUARD inspects each bare jar's bundled prism native and FAILS the
#     run if any wrong-arch jar slipped through — so this can't silently recur.
#
# USAGE
#   ./build-release.sh                 full release build into dist/ + guard
#   ./build-release.sh --verify        no build; just check target/ bare jars are host-arch
#   ./build-release.sh --restore-host  no release; just rebuild host-arch bare jars (quick fix)
#
set -euo pipefail
cd "$(dirname "$0")"
ROOT="$(pwd)"
DIST="$ROOT/dist"

# JavaFX desktop modules — three single-arch artifacts each. Discovered as the
# modules whose pom declares the arm-release profile, so a newly added JavaFX
# module can't be silently forgotten here (which would skip it from the build
# AND from the arch guard). Falls back to the explicit list if discovery finds
# nothing (e.g. run from an unexpected cwd).
mapfile -t JAVAFX_MODULES < <(grep -l '<id>arm-release</id>' */pom.xml 2>/dev/null | sed 's#/pom\.xml$##' | sort -u)
[ ${#JAVAFX_MODULES[@]} -gt 0 ] || JAVAFX_MODULES=(j-hub j-log j-map j-digi j-bridge j-sat morse-trainer)
# Platform-independent modules (pure Java/Jetty) — one jar each. `installer` -> j-installer-*.jar.
PLAIN_MODULES=(j-learn j-vault installer)

MVN="mvn -q -DskipTests"

# Path to a module's bare shaded jar in target/ (classified -mac-aarch64 /
# -linux-aarch64 jars only ever live in dist/, never target/, so they need no
# filtering here — just skip the thin original-/sources/javadoc jars).
bare_jar() {
  ls "$1"/target/*.jar 2>/dev/null | grep -vE 'original-|sources|javadoc' | head -1
}

# Print x86-64 / aarch64 for the host-OS JavaFX prism native bundled in a jar
# (blank if absent). Reads the entry from stdin — no temp dir — and picks the
# native by host OS (Linux ELF .so vs macOS Mach-O .dylib) so the guard works
# on a macOS build host, not just Linux.
prism_arch() {
  local jar="$1" lib desc
  case "$(uname -s)" in
    Darwin) lib='libprism_es2.dylib' ;;
    *)      lib='libprism_es2.so' ;;
  esac
  desc="$(unzip -p "$jar" "$lib" 2>/dev/null | file -b - 2>/dev/null || true)"
  case "$desc" in
    *x86-64*|*x86_64*) echo x86-64 ;;
    *aarch64*|*arm64*) echo aarch64 ;;
  esac
}

want_arch() { case "$(uname -m)" in aarch64|arm64) echo aarch64;; *) echo x86-64;; esac; }

# GUARD: every JavaFX bare jar in target/ must match the host arch, or we abort.
verify_host_jars() {
  local want; want="$(want_arch)"
  echo "==> Verifying host-runnable bare jars (host=$(uname -m), want prism=$want)"
  local fail=0 m got jar
  for m in "${JAVAFX_MODULES[@]}"; do
    jar="$(bare_jar "$m")"
    if [ -z "$jar" ]; then echo "   -- $m: no jar (not built yet)"; continue; fi
    got="$(prism_arch "$jar")"
    if [ "$got" = "$want" ]; then
      echo "   ok $m  prism=$got"
    else
      echo "   !! $m  prism=${got:-none}  (expected $want) — NOT host-runnable: $(basename "$jar")"
      fail=1
    fi
  done
  [ "$fail" = 0 ] || { echo "GUARD FAILED — target/ holds wrong-arch jar(s). Run: ./build-release.sh --restore-host"; return 1; }
}

# Rebuild just the host-arch bare jars (default OS-activated profile). The quick fix.
restore_host_jars() {
  echo "==> Restoring host-arch bare jars for JavaFX modules"
  ( cd j-log-common && $MVN clean install )   # engine + j-bridge depend on it in .m2
  ( cd j-log-engine && $MVN clean install )   # j-log/j-digi depend on it in .m2
  local m
  for m in "${JAVAFX_MODULES[@]}"; do
    echo "   -> $m (default host profile)"
    ( cd "$m" && $MVN clean package )
  done
  verify_host_jars
}

# Preflight: the universal (crossplatform) jar is only correct when built on an
# x86-64 host — on an aarch64 host the auto-activated linux-aarch64 profile forces
# JavaFX 23.0.1 + an aarch64 native, so the "universal" jar would be wrong. Also
# assert every JavaFX pom still pins the ARM JavaFX version in BOTH its
# linux-aarch64 and arm-release profiles, so a dropped override (the 2026-06-02
# Pi-build regression) fails the release loudly instead of shipping a 404-on-Pi jar.
ARM_FX_VERSION="23.0.1"
preflight() {
  case "$(uname -m)" in
    x86_64|amd64) ;;
    *) echo "ERROR: build the release on an x86-64 host. On $(uname -m) the crossplatform profile resolves JavaFX $ARM_FX_VERSION + an aarch64 native, producing a broken 'universal' jar."; exit 2 ;;
  esac
  local m n fail=0
  for m in "${JAVAFX_MODULES[@]}"; do
    n="$(grep -c "<javafx.version>${ARM_FX_VERSION}</javafx.version>" "$m/pom.xml" || true)"
    [ "$n" -ge 2 ] || { echo "   !! $m/pom.xml: expected the linux-aarch64 AND arm-release profiles to pin javafx $ARM_FX_VERSION (found $n) — ARM override drift"; fail=1; }
  done
  [ "$fail" = 0 ] || { echo "PREFLIGHT FAILED — fix the ARM JavaFX version override(s) above before releasing."; exit 1; }
}

full_release() {
  preflight
  echo "==> Clean dist/"
  rm -rf "$DIST"; mkdir -p "$DIST"

  echo "==> Installing shared libraries j-log-common + j-log-engine to local repo"
  ( cd j-log-common && $MVN clean install )   # engine depends on common
  ( cd j-log-engine && $MVN clean install )

  local m jar base
  for m in "${PLAIN_MODULES[@]}"; do
    echo "==> [plain] $m"
    ( cd "$m" && $MVN clean package )
    cp "$(bare_jar "$m")" "$DIST/"
  done

  for m in "${JAVAFX_MODULES[@]}"; do
    echo "==> [javafx] $m — arm-release (linux-aarch64, JavaFX 23.0.1)"
    ( cd "$m" && $MVN -Parm-release clean package )
    jar="$(bare_jar "$m")"; base="$(basename "$jar" .jar)"
    cp "$jar" "$DIST/${base}-linux-aarch64.jar"

    echo "==> [javafx] $m — mac-aarch64-release (Apple Silicon)"
    ( cd "$m" && $MVN -Pmac-aarch64-release clean package )
    jar="$(bare_jar "$m")"; base="$(basename "$jar" .jar)"
    cp "$jar" "$DIST/${base}-mac-aarch64.jar"

    # LAST on purpose: the universal jar (win + mac-intel + linux-x64) is also
    # host-runnable, so it's what we leave behind in target/.
    echo "==> [javafx] $m — crossplatform / universal  [LAST → stays in target/]"
    ( cd "$m" && $MVN -Pcrossplatform clean package )
    cp "$(bare_jar "$m")" "$DIST/"
  done

  verify_host_jars

  echo
  echo "==> Release artifacts in $DIST :"
  ls -1 "$DIST"
  echo "==> target/ bare jars are host-arch and ready to run locally."
}

case "${1:-}" in
  --verify)        verify_host_jars ;;
  --restore-host)  restore_host_jars ;;
  "" )             full_release ;;
  *) echo "usage: $0 [--verify | --restore-host]"; exit 2 ;;
esac
