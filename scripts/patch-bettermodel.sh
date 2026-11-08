#!/usr/bin/env bash
# Usage: patch-bettermodel.sh <input.jar> <output.jar>
# Patches BetterModel's Spigot jar so it works on Spigot (not just Paper).
set -euo pipefail
[ $# -eq 2 ] || { echo "usage: $0 <input.jar> <output.jar>" >&2; exit 2; }
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAVA21=${JAVA21:-/opt/homebrew/opt/openjdk@21/bin/java}
LIBS="$ROOT/build/patcher-libs"
V=9.7.1
mkdir -p "$LIBS"
for m in asm asm-tree; do
  [ -f "$LIBS/$m-$V.jar" ] || curl -fL -o "$LIBS/$m-$V.jar" \
    "https://repo1.maven.org/maven2/org/ow2/asm/$m/$V/$m-$V.jar"
done
"$JAVA21" -cp "$LIBS/asm-$V.jar:$LIBS/asm-tree-$V.jar" \
  "$ROOT/scripts/patcher/PatchBetterModel.java" "$1" "$2"
