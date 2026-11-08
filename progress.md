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
- 2026-11-08 v0.1.2: new `/forzammilmod pack` (anyone) reports what the client said about our pack (loaded, downloading, failed, not reported) and plays `forzammilmod:emote_cheer` as a hands-on test. PackService keeps each player's last status for our pack. Released with release.sh; pack at forziq.github.io/TeatralPlugin/pack/forzammilmod-pack-0.1.2.zip. M02 now points at dist/forzammilmod-0.1.2.jar. Not tried in game yet.
- 2026-11-08 Pack fix (after T09): PackService now uses `addResourcePack`, so our pack stacks on top of the server.properties pack Egor's exaroton server already forces (160 MB), and the kick listener only reacts to our pack's UUID. Shipped as v0.1.1 (release.sh ran end to end, including the gh-pages push, after the postBuffer fix). Pack: forziq.github.io/TeatralPlugin/pack/forzammilmod-pack-0.1.1.zip. M02 now points at dist/forzammilmod-0.1.1.jar; the 0.1.0 copy was removed. Not checked live with both packs yet: check at M02.
- 2026-11-08 T09: v0.1.0 released: GitHub release with jar and release.json, copy in dist/forzammilmod-0.1.0.jar. Pack returns 200 at forziq.github.io/TeatralPlugin/pack/forzammilmod-pack-0.1.0.zip. GitHub token `forzammilmod-server` made later the same day (read-only, TeatralPlugin only, expires 2027-11-08). The `op` CLI wasn't signed in, so Egor copies it into 1Password himself (M02 step 5).
- 2026-11-08 T08: updater checks GitHub with the config.yml token and fetches newer jars into the update folder. `/forzammilmod update` is owner-only. Packs are forced on join. Untested live: T09 checks it. Notes: docs/notes/updater.md.
- 2026-11-08 T07: `scripts/build-pack.sh <v>` makes build/pack/forzammilmod-pack-<v>.zip. `scripts/release.sh <v> [--dry-run]` puts pack.properties in the jar, publishes to Pages and creates the GitHub release. Live publish untested until T09.
- 2026-11-08 T06: local Spigot 1.21.4 test server in `server/`: `setup-server.sh`, `server-start.sh`, `smoke-test.sh [--fresh]` (PASS/FAIL). Needs Java 21. BetterModel pinned 2.2.0, not 3.x (needs Java 25). T10/T11 use the 2.2.0 API.
- 2026-11-08 T05: `Lang` loads `lang/en.yml` and `ru.yml` from the jar; `ru*` locales get Russian. Add every message to both files (`LangTest` checks). Use `lang.msg` or `lang.plural` via `plugin.lang()`. Details: docs/notes/lang.md.
- 2026-11-08 T04: 20 sounds, two takes each, made with ElevenLabs Sound Effects v2 through the creative flow (flow id `6EXEYeied2tVleSPsw1f`, prompt influence 0.5, the four engine and track sounds looped). About 330 credits, no quota errors. Take 1 is `assets/sounds/<name>.ogg`, take 2 is in `assets/sounds/alt/`, raw mp3s are in the ignored `assets/sounds/raw/`. Mono Vorbis q5 at 44.1 kHz: ffmpeg to wav, then `oggenc -q 5`, because Homebrew ffmpeg has no libvorbis. Installed vorbis-tools 1.4.3. Leading silence trimmed on one-shots only. `sounds.json` has 20 entries, `stream: false`. T07 must copy `assets/sounds/*.ogg` into `assets/forzammilmod/sounds/`. Egor has not listened yet: check the loop seams and swap any weak take (copy `alt/<name>.ogg` over `<name>.ogg`).
## blockers
none
## gate notes
G1: -
G2: -
