#!/usr/bin/env bash
# Starts the test server headless, checks the plugin loads, stops it. Usage: smoke-test.sh [--fresh]
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAVA_SERVER=${JAVA_SERVER:-/opt/homebrew/opt/openjdk@21/bin/java}  # Spigot 1.21.4 supports Java 21-24; BetterModel 3.x needs 25
cd "$ROOT" || exit 1
[ -f server/spigot-1.21.4.jar ] || { echo "FAIL: run scripts/setup-server.sh first"; exit 1; }
./gradlew build -q || { echo "FAIL: build failed"; exit 1; }
rm -f server/plugins/forzammilmod*.jar
JAR=$(ls build/libs/forzammilmod*.jar | grep -v -E -- '-(sources|javadoc|plain)\.jar$' | head -1)
cp "$JAR" server/plugins/
if [ "${1:-}" = "--fresh" ]; then rm -rf server/world server/world_nether server/world_the_end; fi

cd server || exit 1
mkdir -p logs; rm -f logs/latest.log
PIPE=$(mktemp -u /tmp/forzammilmod-pipe.XXXXXX)
mkfifo "$PIPE"
SPID=""
cleanup() {
  [ -n "$SPID" ] && kill -0 "$SPID" 2>/dev/null && kill "$SPID" 2>/dev/null
  exec 3>&- 2>/dev/null
  rm -f "$PIPE"
}
trap cleanup EXIT
exec 3<>"$PIPE"
"$JAVA_SERVER" -Xms512M -Xmx2G -jar spigot-1.21.4.jar nogui <"$PIPE" >/tmp/forzammilmod-smoke.out 2>&1 &
SPID=$!

LOG=logs/latest.log
DONE=0
for i in $(seq 1 120); do
  if ! kill -0 "$SPID" 2>/dev/null; then break; fi
  if [ -f "$LOG" ] && grep -q 'Done (' "$LOG"; then DONE=1; break; fi
  sleep 1
done

FAILS=""
if [ "$DONE" != 1 ]; then FAILS="server did not reach Done within 120s"; fi
chk() { grep -q "$1" "$LOG" 2>/dev/null || FAILS="$FAILS
missing: $2"; }
chk '\[forzammilmod\] Enabling forzammilmod' "forzammilmod enabled"
chk '\[BetterModel\] Enabling' "BetterModel enabled"
chk '\[SkinsRestorer\] Enabling' "SkinsRestorer enabled"
BAD=$(grep -n -E 'Error occurred while enabling|Could not load|at dev\.egorgrigorciuc|dev\.egorgrigorciuc.*(Exception|Error)|(Exception|Error).*dev\.egorgrigorciuc' "$LOG" 2>/dev/null | head -40)

echo "stop" >&3
for i in $(seq 1 30); do kill -0 "$SPID" 2>/dev/null || break; sleep 1; done
kill -0 "$SPID" 2>/dev/null && kill -9 "$SPID" 2>/dev/null

if [ -z "$FAILS" ] && [ -z "$BAD" ]; then echo "PASS"; exit 0; fi
echo "FAIL"
[ -n "$FAILS" ] && echo "$FAILS"
[ -n "$BAD" ] && echo "$BAD"
exit 1
