#!/usr/bin/env bash
# Builds the plugin, copies it to the test server and starts it in the foreground.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAVA_SERVER=${JAVA_SERVER:-/opt/homebrew/opt/openjdk@21/bin/java}  # Spigot 1.21.4 supports Java 21-24; BetterModel 3.x needs 25
cd "$ROOT"
./gradlew build -q
rm -f server/plugins/forzammilmod*.jar
JAR=$(ls build/libs/forzammilmod*.jar | grep -v -E -- '-(sources|javadoc|plain)\.jar$' | head -1)
cp "$JAR" server/plugins/
cd server
exec "$JAVA_SERVER" -Xms1G -Xmx3G -jar spigot-1.21.4.jar nogui
