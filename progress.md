# progress
now: T15 todo | G1 passed | G2 todo
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
T23|L|sonnet-builder|T24,T05|done|Server radio: /forzammilmod music play, stop, shuffle, loop, list
T13|S|haiku-scout|T12,T23|done|Publish v0.2.0 for Gate 1
T25|M|sonnet-builder|T13|done|Rename the commands to /fmod and publish v0.2.1
G1|-|egor|T13,T25,M02|done|Gate 1: emote test with friends (manual.md G1)
T14|S|haiku-scout|G1|done|Research vehicle approach
T15|M|sonnet-builder|T14|todo|Tank and jeep placeholder models
T16|L|opus-deep|T15|todo|Driving core (spawn, mount, W/A/S/D, step-up)
T17|L|sonnet-builder|T16|todo|Turret and barrel aim
T18|L|sonnet-builder|T17|todo|Firing, shells and explosions
T19|M|sonnet-builder|T16|todo|Jeep on the same driving code
T20|S|haiku-scout|T18,T19|todo|Publish v0.3.0 for Gate 2
G2|-|egor|T20|gate|Gate 2: tank test with friends (manual.md G2)
T21|L|main|G2|todo|Plan phases 3 to 6 from the gate notes

## log (newest first, keep 8)
- 2026-11-08 T14: plan in docs/notes/vehicles.md. Invisible ArmorStand anchor, seat bones, input from getCurrentInput(). Test in T16: idle-bone rotation, rider left-click, arm swing while riding. No free tank plugin, so no M04.
- 2026-11-08 T25: commands now /fmod (no old alias). v0.2.1 pack: https://forziq.github.io/TeatralPlugin/pack/forzammilmod-pack-0.2.1.zip. Server still old: Egor types /forzammilmod update, restarts, then /fmod (manual.md G2). 67 tests pass.
- 2026-11-08 G1 passed: friends tested emotes. Armour shows on the emote model but without trims. Not a quick fix: the pack has the trim textures and item cases, so the gap is in what BetterModel sends at runtime on Spigot. Needs an in-game test with trimmed armour.
- 2026-11-08 BM fix: emotes crash on Spigot (BetterModel reads EntityHuman.bW). scripts/patch-bettermodel.sh makes BetterModel-2.2.0-spigot-fmod.jar; setup-server.sh applies it. Swapped on exaroton, emotes work. Re-patch on BetterModel update.
- 2026-11-08 T13: v0.2.0 released (jar and release.json on GitHub, pack at forziq.github.io/TeatralPlugin/pack/forzammilmod-pack-0.2.0.zip). manual.md G1 is one checklist: update steps, skins, emotes, radio. Not tried in game yet. Note: release.sh needs a clean tree, so stash progress.md first.
- 2026-11-08 T23: radio at /forzammilmod music (play, stop, shuffle, loop, list), on the Jukebox/Note Blocks slider. Late joiners get a chat note, next track only. Not tried in game (M03). BetterModel 2.2.0 errors on shutdown, not ours.
- 2026-11-08 T12: /emote, /emote wave, /emote stop. One emote each. Walking (over 0.05 sideways or a new block), damage, death, teleport or quit ends it. Each stop closes the BetterModel tracker. 54 tests pass. Not tried in game (M03).
- 2026-11-08 T24: two cheerful tracks swapped for tragic ones: posledniy_rubezh and pepel_goroda. Still 7 tracks. Music runs cost 4x the estimate (4 takes per run), see docs/notes. Nobody has listened yet. T23 unblocked.
## blockers
none

## gate notes
G1: -
G2: -
