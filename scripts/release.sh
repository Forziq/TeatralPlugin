#!/usr/bin/env bash
# Builds jar + resource pack and publishes a release. Usage: release.sh <x.y.z> [--dry-run] [--skip-server]
set -euo pipefail
VERSION=""; DRY=0; SKIP=""
for a in "$@"; do
  case "$a" in
    --help|-h) echo "Usage: release.sh <x.y.z> [--dry-run] [--skip-server]"
               echo "  --dry-run      build everything, publish nothing, restore changed files"
               echo "  --skip-server  reuse BetterModel's existing pack instead of starting the test server"
               exit 0;;
    --dry-run) DRY=1;;
    --skip-server) SKIP="--skip-server";;
    *) VERSION="$a";;
  esac
done
[[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || { echo "FAIL: version must look like 1.2.3"; exit 1; }
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; cd "$ROOT"

REMOTE=$(git remote get-url origin)
SLUG=$(echo "$REMOTE" | sed -E 's#^.*github\.com[:/]##; s#\.git$##')   # Owner/Repo
OWNER="${SLUG%%/*}"; REPO="${SLUG##*/}"
OWNER_LC=$(echo "$OWNER" | tr 'A-Z' 'a-z')
PACK_URL="https://$OWNER_LC.github.io/$REPO/pack/forzammilmod-pack-$VERSION.zip"
PROPS=src/main/resources/pack.properties

if [ "$DRY" = 0 ]; then
  [ -z "$(git status --porcelain)" ] || { echo "FAIL: git tree is dirty"; exit 1; }
  ! git rev-parse -q --verify "refs/tags/v$VERSION" >/dev/null || { echo "FAIL: tag v$VERSION exists"; exit 1; }
fi

# Remember prior state so a dry run can restore it
PREV_GRADLE=$(cat gradle.properties)
PREV_PROPS_EXISTED=0; [ -f "$PROPS" ] && { PREV_PROPS_EXISTED=1; PREV_PROPS=$(cat "$PROPS"); }
restore() {
  if [ "$DRY" = 1 ]; then
    echo "==> Restoring gradle.properties and pack.properties"
    printf '%s\n' "$PREV_GRADLE" > gradle.properties
    if [ "$PREV_PROPS_EXISTED" = 1 ]; then printf '%s\n' "$PREV_PROPS" > "$PROPS"; else rm -f "$PROPS"; fi
  fi
}
trap restore EXIT

echo "==> Setting version $VERSION"
sed -i '' -E "s/^version=.*/version=$VERSION/" gradle.properties

echo "==> Building resource pack"
BP_OUT=$(scripts/build-pack.sh "$VERSION" $SKIP | tee /dev/stderr) || exit 1
SHA1=$(echo "$BP_OUT" | tail -1 | sed 's/^sha1=//')
[[ "$SHA1" =~ ^[0-9a-f]{40}$ ]] || { echo "FAIL: could not read pack SHA-1"; exit 1; }
ZIP="build/pack/forzammilmod-pack-$VERSION.zip"

echo "==> Writing pack.properties"
printf 'url=%s\nsha1=%s\nversion=%s\n' "$PACK_URL" "$SHA1" "$VERSION" > "$PROPS"

echo "==> Building plugin jar"
./gradlew build -q
JAR="build/libs/forzammilmod-$VERSION.jar"
[ -f "$JAR" ] || { echo "FAIL: $JAR missing"; exit 1; }
unzip -p "$JAR" pack.properties

echo "==> Writing release files"
mkdir -p build/release
node -e '
const [v, url, sha] = process.argv.slice(1);
require("fs").writeFileSync("build/release/release.json", JSON.stringify(
  { version: v, jar: "forzammilmod-" + v + ".jar", packUrl: url, packSha1: sha }, null, 2) + "\n");
' "$VERSION" "$PACK_URL" "$SHA1"
PREV_TAG=$(git describe --tags --abbrev=0 --match 'v*' 2>/dev/null || true)
{
  echo "# forzammilmod $VERSION"; echo
  echo "Resource pack: $PACK_URL"; echo "SHA-1: $SHA1"; echo
  echo "## Changes"
  if [ -n "$PREV_TAG" ]; then git log --oneline "$PREV_TAG..HEAD"; else git log --oneline; fi
} > build/release/notes.md

if [ "$DRY" = 1 ]; then
  echo "==> DRY RUN summary (nothing published)"
  echo "  would publish $ZIP to gh-pages as pack/$(basename "$ZIP") (+ .nojekyll)"
  echo "  would enable Pages for $SLUG if needed"
  echo "  would commit gradle.properties + pack.properties as 'release v$VERSION', tag v$VERSION, push main + tag"
  echo "  would create GitHub release v$VERSION with $JAR and release.json"
  echo "  pack url: $PACK_URL"; echo "  sha1: $SHA1"
  exit 0
fi

echo "==> Publishing pack to gh-pages"
WT=build/gh-pages
git worktree remove --force "$WT" 2>/dev/null || true
rm -rf "$WT"
git fetch origin gh-pages 2>/dev/null || true
if git show-ref -q --verify refs/remotes/origin/gh-pages; then
  git worktree add -B gh-pages "$WT" origin/gh-pages
elif git show-ref -q --verify refs/heads/gh-pages; then
  git worktree add "$WT" gh-pages
else
  git worktree add --detach "$WT"
  (cd "$WT" && git checkout -q --orphan gh-pages && git rm -rf -q . 2>/dev/null || true)
fi
mkdir -p "$WT/pack"
cp "$ZIP" "$WT/pack/"
touch "$WT/.nojekyll"
(cd "$WT" && git add -A && git commit -q -m "pack $VERSION" && git -c http.postBuffer=524288000 push origin gh-pages)
git worktree remove --force "$WT"

echo "==> Making sure GitHub Pages is on"
if ! gh api "repos/$SLUG/pages" >/dev/null 2>&1; then
  gh api -X POST "repos/$SLUG/pages" -f "source[branch]=gh-pages" -f "source[path]=/"
fi

echo "==> Committing, tagging, pushing"
git add gradle.properties "$PROPS"
git commit -q -m "release v$VERSION

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git tag "v$VERSION"
git push origin main "v$VERSION"

echo "==> Creating GitHub release"
gh release create "v$VERSION" "$JAR" build/release/release.json --notes-file build/release/notes.md

echo "==> Waiting for the pack URL to go live"
for i in $(seq 1 18); do
  code=$(curl -s -o /dev/null -w '%{http_code}' -I "$PACK_URL" || true)
  [ "$code" = 200 ] && { echo "Pack is live: $PACK_URL"; exit 0; }
  sleep 10
done
echo "WARNING: $PACK_URL not returning 200 after 3 minutes (Pages may still be building)"
