#!/usr/bin/env bash
# Builds the resource pack zip. Usage: build-pack.sh <version> [--skip-server]
set -euo pipefail
if [ "${1:-}" = "--help" ] || [ -z "${1:-}" ]; then
  echo "Usage: build-pack.sh <version> [--skip-server]"
  echo "  Merges BetterModel's generated pack with assets/pack and assets/sounds."
  echo "  --skip-server  reuse the existing BetterModel build.zip instead of starting the test server"
  exit 0
fi
VERSION="$1"; SKIP="${2:-}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
BM_ZIP="server/plugins/BetterModel/build.zip"
STAGE="build/pack/stage"
OUT="build/pack/forzammilmod-pack-$VERSION.zip"

if [ "$SKIP" != "--skip-server" ]; then
  echo "==> Starting test server so BetterModel regenerates its pack"
  scripts/smoke-test.sh || { echo "FAIL: smoke test failed"; exit 1; }
fi
[ -f "$BM_ZIP" ] || { echo "FAIL: $BM_ZIP missing (run without --skip-server)"; exit 1; }

echo "==> Staging pack"
rm -rf "$STAGE"; mkdir -p "$STAGE"
unzip -q "$BM_ZIP" -d "$STAGE"
cp "$STAGE/pack.mcmeta" build/pack/bm-pack.mcmeta
[ -d assets/pack ] && cp -R assets/pack/. "$STAGE/"
mkdir -p "$STAGE/assets/forzammilmod/sounds"
cp assets/sounds/*.ogg "$STAGE/assets/forzammilmod/sounds/"
mkdir -p "$STAGE/assets/forzammilmod/sounds/music"
cp assets/music/*.ogg "$STAGE/assets/forzammilmod/sounds/music/"

echo "==> Writing pack.mcmeta (BetterModel overlays kept)"
node -e '
const fs = require("fs");
const m = JSON.parse(fs.readFileSync("build/pack/bm-pack.mcmeta", "utf8"));
m.pack.description = "forzammilmod resource pack " + process.argv[1];
fs.writeFileSync(process.argv[2], JSON.stringify(m, null, 2));
' "$VERSION" "$STAGE/pack.mcmeta"
rm -f build/pack/bm-pack.mcmeta
mkdir -p "$STAGE/forzammilmod"
printf '%s\n' "$VERSION" > "$STAGE/forzammilmod/version.txt"

echo "==> Zipping"
rm -f "$OUT"
(cd "$STAGE" && zip -X -r -q "$ROOT/$OUT" .)
echo "sha1=$(shasum -a 1 "$OUT" | cut -d' ' -f1)"
