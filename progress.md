# progress
now: T23 todo | G1 todo | G2 todo
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
M02|-|egor|T09|done|Upload the starter to exaroton (manual.md M02)
T10|S|haiku-scout|T06|done|Research BetterModel player-model API
T11|L|opus-deep|T10|done|Model installer and the wave animation
T22|M|sonnet-builder|T04|done|WWII-style music with ElevenLabs (5 instrumentals, 1 comic song)
T24|M|sonnet-builder|T22|done|Swap two tracks for tragic, intense ones (Posledniy Rubezh, Pepel Goroda)
T12|L|sonnet-builder|T11,T05|done|Emote commands and cancel rules
T23|L|sonnet-builder|T24,T05|todo|Server radio: /forzammilmod music play, stop, shuffle, loop, list
T13|S|haiku-scout|T12,T23|todo|Publish v0.2.0 for Gate 1
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
- 2026-11-08 T12: /emote, /emote wave, /emote stop. One emote each. Walking (over 0.05 sideways, or onto a new block), damage, death, teleport or quit ends it. Each stop closes the BetterModel tracker. 54 tests pass; server loads it. Not tried in game (M03). BetterModel errors on shutdown: theirs, not ours.
- 2026-11-08 T24: two cheerful tracks swapped for tragic ones: posledniy_rubezh and pepel_goroda. Still 7 tracks. Music runs cost 4x the estimate (4 takes per run), see docs/notes. Nobody has listened yet. T23 unblocked.
- 2026-11-08 T22: seven tracks in assets/music (five instrumentals, two Russian comic songs). tracks.yml lists ids and names. Sounds are forzammilmod:music.<id>. Pack now 15.3 MB. Nobody has listened yet. T23: 7 tracks, not 6.
- 2026-11-08 T11: models copy into BetterModel on startup (SHA-1 checked). `forzammil_player` has a 2 s `wave`. T12: use `BetterModel.limb("forzammil_player")`; `stopAnimation` skips onEnd. Not seen in game yet. Notes: docs/notes/models.md
- 2026-11-08 T10: BetterModel notes in docs/notes/bettermodel.md. T11 checks in game: held items and armour during animations, whether stopAnimation fires the end callback, offline skin fallback, and the 1.21.4 Modrinth jar against tag 2.2.0.
- 2026-11-08 M02 done: Egor uploaded 0.1.2 (with BetterModel and SkinsRestorer) to exaroton, set the token, and checked `/forzammilmod version` and `/forzammilmod pack` in game.
- 2026-11-08 v0.1.2: new `/forzammilmod pack` (anyone) reports what the client said about our pack (loaded, downloading, failed, not reported) and plays `forzammilmod:emote_cheer` as a hands-on test. PackService keeps each player's last status for our pack. Released with release.sh; pack at forziq.github.io/TeatralPlugin/pack/forzammilmod-pack-0.1.2.zip. M02 now points at dist/forzammilmod-0.1.2.jar. Not tried in game yet.
- 2026-11-08 Pack fix (after T09): PackService now uses `addResourcePack`, so our pack stacks on top of the server.properties pack Egor's exaroton server already forces (160 MB), and the kick listener only reacts to our pack's UUID. Shipped as v0.1.1 (release.sh ran end to end, including the gh-pages push, after the postBuffer fix). Pack: forziq.github.io/TeatralPlugin/pack/forzammilmod-pack-0.1.1.zip. M02 now points at dist/forzammilmod-0.1.1.jar; the 0.1.0 copy was removed. Not checked live with both packs yet: check at M02.
## blockers
none

## gate notes
G1: -
G2: -
