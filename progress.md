# progress
now: T18 doing | G1 passed | G2 todo
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
T15|M|sonnet-builder|T14|done|Tank and jeep placeholder models
T16|L|opus-deep|T15|done|Driving core (spawn, mount, W/A/S/D, step-up)
T17|L|sonnet-builder|T16|done|Turret and barrel aim
T18|L|sonnet-builder|T17|doing|Firing, shells and explosions
T19|M|sonnet-builder|T16|todo|Jeep on the same driving code
T20|S|haiku-scout|T18,T19|todo|Publish v0.3.0 for Gate 2
G2|-|egor|T20|gate|Gate 2: tank test with friends (manual.md G2)
T21|L|main|G2|todo|Plan phases 3 to 6 from the gate notes

## log (newest first, keep 8)
- 2026-11-08 T17: turret and barrel follow the driver's look. Unit tests pass (104), not tried in game. Watch 'Turret check' lines on tank spawn; if wrong way, flip TURRET_YAW_SIGN or BARREL_PITCH_SIGN in BetterModelHook. T18: use Vehicle.muzzle().
- 2026-11-08 T16: driving. /fmod vehicle spawn tank|jeep, remove (owner), list. Driver in p_driver seat, controller NONE. Untested: check the three "Driver check:" log lines. See docs/notes/vehicles.md. T17 builds on Vehicle/VehicleRig.
- 2026-11-08 T15: tank and jeep placeholder models. Tank: hull > turret > barrel > muzzle (points -Z), p_driver under hull. Jeep: body, wheel_fl/fr/rl/rr, p_driver. No h_/b_ tags, so no head tracking. T16: confirm BetterModel.model("tank") resolves.
- 2026-11-08 T14: plan in docs/notes/vehicles.md. Invisible ArmorStand anchor, seat bones, input from getCurrentInput(). Test in T16: idle-bone rotation, rider left-click, arm swing while riding. No free tank plugin, so no M04.
- 2026-11-08 T25: commands now /fmod (no old alias). v0.2.1 pack: https://forziq.github.io/TeatralPlugin/pack/forzammilmod-pack-0.2.1.zip. Server still old: Egor types /forzammilmod update, restarts, then /fmod (manual.md G2). 67 tests pass.
- 2026-11-08 G1 passed: friends tested emotes. Armour shows on the emote model but without trims. Not a quick fix: the pack has the trim textures and item cases, so the gap is in what BetterModel sends at runtime on Spigot. Needs an in-game test with trimmed armour.
- 2026-11-08 BM fix: emotes crash on Spigot (BetterModel reads EntityHuman.bW). scripts/patch-bettermodel.sh makes BetterModel-2.2.0-spigot-fmod.jar; setup-server.sh applies it. Swapped on exaroton, emotes work. Re-patch on BetterModel update.
- 2026-11-08 T13: v0.2.0 released (jar and release.json on GitHub, pack at forziq.github.io/TeatralPlugin/pack/forzammilmod-pack-0.2.0.zip). manual.md G1 is one checklist: update steps, skins, emotes, radio. Not tried in game yet. Note: release.sh needs a clean tree, so stash progress.md first.

## blockers
none

## gate notes
G1: -
G2: -
