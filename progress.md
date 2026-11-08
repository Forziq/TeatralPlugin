# progress
now: M02 manual | G1 todo | G2 todo
updated: 2026-11-08

## tasks
id|size|who|deps|status|title
T01|S|haiku-scout|-|done|Install tools, create the GitHub repo, add Context7
M01|-|egor|T01|done|Restart Claude Code so new MCP servers load (manual.md M01)
T02|M|sonnet-builder|M01|done|Gradle scaffold and plugin skeleton
T03|M|sonnet-builder|T02|done|Build and test the dashboard (/dashboard on/off)
T04|M|sonnet-builder|T03|done|Generate the sound library with ElevenLabs
T05|M|sonnet-builder|T02|done|Language system (English, Russian, plurals)
T06|M|sonnet-builder|T02|done|Local test server and smoke test script
T07|M|sonnet-builder|T06|done|Resource pack build and release pipeline (Pages)
T08|L|sonnet-builder|T05,T07|done|Updater, pack sender and owner check
T09|S|haiku-scout|T08|done|Publish starter release v0.1.0
M02|-|egor|T09|manual|Upload the starter to exaroton (manual.md M02)
T10|S|haiku-scout|T06|todo|Research BetterModel player-model API
T11|L|opus-deep|T10|todo|Model installer and the wave animation
T12|L|sonnet-builder|T11,T05|todo|Emote commands and cancel rules
T13|S|haiku-scout|T12|todo|Publish v0.2.0 for Gate 1
G1|-|egor|T13,M02|gate|Gate 1: emote test with friends (manual.md G1)
T14|S|haiku-scout|G1|todo|Research vehicle approach
T15|M|sonnet-builder|T14|todo|Tank and jeep placeholder models
T16|L|opus-deep|T15|todo|Driving core (spawn, mount, W/A/S/D, step-up)
T17|L|sonnet-builder|T16|todo|Turret and barrel aim
T18|L|sonnet-builder|T17|todo|Firing, shells and explosions
T19|M|sonnet-builder|T16|todo|Jeep on the same driving code
T20|S|haiku-scout|T18,T19|todo|Publish v0.3.0 for Gate 2
G2|-|egor|T20|gate|Gate 2: tank test with friends (manual.md G2)
T21|L|main|G2|todo|Plan phases 3 to 6 from the gate notes

## log (newest first, keep 8)
- 2026-11-08 T09: v0.1.0 released: GitHub release with jar and release.json, copy in dist/forzammilmod-0.1.0.jar. Pack returns 200 at forziq.github.io/TeatralPlugin/pack/forzammilmod-pack-0.1.0.zip. GitHub token NOT made (sudo code). M02 has the steps.
- 2026-11-08 T08: updater checks GitHub with the config.yml token and fetches newer jars into the update folder. `/forzammilmod update` is owner-only. Packs are forced on join. Untested live: T09 checks it. Notes: docs/notes/updater.md.
- 2026-11-08 T07: `scripts/build-pack.sh <v>` makes build/pack/forzammilmod-pack-<v>.zip. `scripts/release.sh <v> [--dry-run]` puts pack.properties in the jar, publishes to Pages and creates the GitHub release. Live publish untested until T09.
- 2026-11-08 T06: local Spigot 1.21.4 test server in `server/`: `setup-server.sh`, `server-start.sh`, `smoke-test.sh [--fresh]` (PASS/FAIL). Needs Java 21. BetterModel pinned 2.2.0, not 3.x (needs Java 25). T10/T11 use the 2.2.0 API.
- 2026-11-08 T05: `Lang` loads `lang/en.yml` and `ru.yml` from the jar; `ru*` locales get Russian. Add every message to both files (`LangTest` checks). Use `lang.msg` or `lang.plural` via `plugin.lang()`. Details: docs/notes/lang.md.
- 2026-11-08 T04: 20 sounds, two takes each, made with ElevenLabs Sound Effects v2 through the creative flow (flow id `6EXEYeied2tVleSPsw1f`, prompt influence 0.5, the four engine and track sounds looped). About 330 credits, no quota errors. Take 1 is `assets/sounds/<name>.ogg`, take 2 is in `assets/sounds/alt/`, raw mp3s are in the ignored `assets/sounds/raw/`. Mono Vorbis q5 at 44.1 kHz: ffmpeg to wav, then `oggenc -q 5`, because Homebrew ffmpeg has no libvorbis. Installed vorbis-tools 1.4.3. Leading silence trimmed on one-shots only. `sounds.json` has 20 entries, `stream: false`. T07 must copy `assets/sounds/*.ogg` into `assets/forzammilmod/sounds/`. Egor has not listened yet: check the loop seams and swap any weak take (copy `alt/<name>.ogg` over `<name>.ogg`).
- 2026-11-08 T03: live agent dashboard at http://localhost:4321, switched with `/dashboard on|off|status` (`.claude/dashboard/ctl.mjs`). Node built-ins only. `lib.mjs` attributes events to agents (the `agent` field, else a stack fed by Agent PreToolUse and popped by PostToolUse and SubagentStop) and parses progress.md. Five Node tests pass; run them with `node --test ".claude/dashboard/test/*.test.mjs"` (a bare directory path fails on Node 26). Sonnet-tester checked on, off and double-on. Known limit: two parallel subagents of the same type can be mixed up on the fallback path. The design-taste-frontend skill was not used, so the page is plain; Egor may want a polish later.
- 2026-11-08 T01: installed Java 21 (openjdk@21 21.0.13), gh 2.103.3 and 1Password CLI 2.42.0 with Homebrew. Already there: Java 19 (default), Node 26.7.0, git 2.39.1, ffmpeg 8.0.1, Homebrew 7.0.11. Pushed main to the private repo Forziq/TeatralPlugin after Egor logged in to gh. Context7 is in .mcp.json, awaiting approval until Claude Code restarts (M01). ElevenLabs connector is visible. Context7 tools were already loading in the session that finished T01, so M01 is likely done.

## blockers
none

## gate notes
G1: -
G2: -
