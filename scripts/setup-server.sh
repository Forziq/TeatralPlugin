#!/usr/bin/env bash
# Sets up the local Spigot 1.21.4 test server in server/ (idempotent).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAVA21=${JAVA21:-/opt/homebrew/opt/openjdk@21/bin/java}
SRV="$ROOT/server"
JAR="$SRV/spigot-1.21.4.jar"

# Pinned plugin versions (Modrinth, loader "spigot", game version 1.21.4)
BM_FILE="BetterModel-2.2.0-spigot.jar"
BM_URL="https://cdn.modrinth.com/data/4h8rX3rt/versions/96RWpAP2/BetterModel-2.2.0-spigot.jar"
SR_FILE="SkinsRestorer-15.12.6.jar"
SR_URL="https://cdn.modrinth.com/data/TsLS8Py5/versions/ziIzW16f/SkinsRestorer.jar"

mkdir -p "$SRV/buildtools" "$SRV/plugins"

if [ ! -f "$JAR" ]; then
  echo "Building Spigot 1.21.4 with BuildTools (several minutes)..."
  cd "$SRV/buildtools"
  [ -f BuildTools.jar ] || curl -fL -o BuildTools.jar \
    https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar
  "$JAVA21" -jar BuildTools.jar --rev 1.21.4
  cp spigot-1.21.4.jar "$JAR"
else
  echo "Spigot jar already present."
fi

echo "eula=true" > "$SRV/eula.txt"
cat > "$SRV/server.properties" <<PROPS
online-mode=false
level-type=minecraft\:flat
spawn-protection=0
max-players=5
motd=forzammilmod test
PROPS

fetch() { # file url
  if [ -f "$SRV/plugins/$1" ]; then echo "$1 already present."; else
    curl -fL -o "$SRV/plugins/$1" "$2"; fi
}
fetch "$SR_FILE" "$SR_URL"

# BetterModel: download the original into cache/, patch it for Spigot, load only the patched copy
BM_PATCHED="BetterModel-2.2.0-spigot-fmod.jar"
mkdir -p "$SRV/cache" "$ROOT/dist"
[ -f "$SRV/cache/$BM_FILE" ] || curl -fL -o "$SRV/cache/$BM_FILE" "$BM_URL"
rm -f "$SRV/plugins/$BM_FILE"
if [ ! -f "$SRV/plugins/$BM_PATCHED" ]; then
  "$ROOT/scripts/patch-bettermodel.sh" "$SRV/cache/$BM_FILE" "$SRV/plugins/$BM_PATCHED"
else
  echo "$BM_PATCHED already present."
fi
cp "$SRV/plugins/$BM_PATCHED" "$ROOT/dist/$BM_PATCHED"
echo "Server ready."
