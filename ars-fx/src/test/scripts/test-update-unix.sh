#!/usr/bin/env bash
# Exercises update-unix.sh with throwaway jars: good swap, crash -> rollback, corrupt -> refused.
# Needs: bash, a JDK (javac + jar). Run from anywhere:  ars-fx/src/test/scripts/test-update-unix.sh
set -u
HERE="$(cd "$(dirname "$0")" && pwd)"
SCRIPT="$HERE/../../main/resources/com/ars/fx/update/update-unix.sh"
WORK="$(mktemp -d)"
trap 'pkill -f "java.* $WORK" 2>/dev/null; rm -rf "$WORK"' EXIT
fail=0
ok()  { echo "  ok   $*"; }
bad() { echo "  FAIL $*"; fail=1; }

# fake apps: one stays up, one dies on start
mkdir -p "$WORK/src/good" "$WORK/src/crash"
echo 'public class Main { public static void main(String[] a) throws Exception { Thread.sleep(120000); } }' > "$WORK/src/good/Main.java"
echo 'public class Main { public static void main(String[] a) { throw new RuntimeException("boom"); } }'     > "$WORK/src/crash/Main.java"
for k in good crash; do
  ( cd "$WORK/src/$k" && javac Main.java && printf 'Main-Class: Main\n' > m.txt && jar cfm "$WORK/$k.jar" m.txt Main.class ) || { echo "could not build fake jars"; exit 2; }
done
echo notajar > "$WORK/corrupt.jar"

run() {   # run NAME NEWJAR  -> sets $d, $rc
  d="$WORK/$1"; mkdir -p "$d/ars-fx/target"
  cp "$WORK/good.jar" "$d/ars-fx/target/ars-fx-linux.jar"
  printf '#!/usr/bin/env bash\nDIR="$(cd "$(dirname "$0")" && pwd)"\nexec java -jar "$DIR/target/ars-fx-linux.jar"\n' > "$d/ars-fx/ars-fx.sh"
  cp "$2" "$d/new.jar"
  ( : ) & dead=$!; wait $dead          # an app pid that is already gone
  sh "$SCRIPT" "$dead" "$d/ars-fx/target/ars-fx-linux.jar" "$d/new.jar" "$d/ars-fx/ars-fx.sh" "$d/update.log" 3 2
  rc=$?
}

echo "== success"; run success "$WORK/good.jar"
[ $rc -eq 0 ] && ok "exit 0" || bad "exit $rc"
[ -f "$d/ars-fx/target/ars-fx-linux.jar.bak" ] && ok "old jar kept as .bak" || bad "no .bak"
grep -q '^ok' "$d/last-result.txt" && ok "result ok" || bad "result: $(cat "$d/last-result.txt" 2>&1)"

echo "== crash -> rollback"; run crash "$WORK/crash.jar"
[ $rc -eq 2 ] && ok "exit 2" || bad "exit $rc"
cmp -s "$d/ars-fx/target/ars-fx-linux.jar" "$WORK/good.jar" && ok "old jar restored" || bad "jar not restored"
[ -f "$d/ars-fx/target/ars-fx-linux.jar.failed" ] && ok "bad jar kept as .failed" || bad "no .failed"
grep -q '^rolled back' "$d/last-result.txt" && ok "result rolled back" || bad "result: $(cat "$d/last-result.txt" 2>&1)"

echo "== corrupt -> refused"; run corrupt "$WORK/corrupt.jar"
if command -v unzip >/dev/null 2>&1; then
  [ $rc -eq 1 ] && ok "exit 1" || bad "exit $rc"
  cmp -s "$d/ars-fx/target/ars-fx-linux.jar" "$WORK/good.jar" && ok "installed jar untouched" || bad "jar changed"
  grep -q '^failed' "$d/last-result.txt" && ok "result failed" || bad "result: $(cat "$d/last-result.txt" 2>&1)"
else echo "  skip (no unzip)"; fi

echo "== missing download"; run missing "$WORK/good.jar"; rm -f "$d/new.jar"
sh "$SCRIPT" 999999 "$d/ars-fx/target/ars-fx-linux.jar" "$d/new.jar" "$d/ars-fx/ars-fx.sh" "$d/update.log" 3 2; rc=$?
[ $rc -eq 1 ] && ok "exit 1" || bad "exit $rc"

[ $fail -eq 0 ] && echo "ALL PASSED" || { echo "FAILURES"; exit 1; }
