# progress
now: T50 todo | G1 passed | G2 passed
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
T18|L|sonnet-builder|T17|done|Firing, shells and explosions
T19|M|sonnet-builder|T16|done|Jeep on the same driving code
T20|S|haiku-scout|T18,T19|done|Publish v0.3.0 for Gate 2
G2|-|egor|T20|done|Gate 2: tank test with friends (manual.md G2)
T21|L|main|G2|done|Plan phases 3 to 6 from the gate notes
T26|M|sonnet-builder|G2|done|Vehicle spawn pads (command block and button)
T50|M|sonnet-builder|T26|todo|Our own greeting emote, configurable name and sound
T28|M|sonnet-builder|T50|todo|Held items and armour during emotes
T32|S|haiku-scout|T28|todo|Publish v0.4.0 for Gate 3
G3|-|egor|T32|gate|Gate 3: greeting test with friends (manual.md G3)
T33|L|sonnet-builder|G3|todo|Vehicle health, damage and repairs
T34|L|sonnet-builder|T33|todo|Crew seats: gunner and passengers
T35|L|opus-deep|T34|todo|Proper tank and jeep models, with track animation
T36|M|sonnet-builder|T34|todo|Vehicle sounds
T37|M|sonnet-builder|T36|todo|Vehicle HUD
T38|S|haiku-scout|T35,T37|todo|Publish v0.5.0 for Gate 4
G4|-|egor|T38|gate|Gate 4: tank battle with friends (manual.md G4)
T39|M|sonnet-builder|G4|todo|Army truck
T40|L|sonnet-builder|T39|todo|Field artillery
T41|S|haiku-scout|T40|todo|Publish v0.6.0 for Gate 5
G5|-|egor|T41|gate|Gate 5: truck and artillery (manual.md G5)
T42|S|haiku-scout|G5|todo|Research how to fly a plane
T43|L|opus-deep|T42|todo|Rough plane
T44|S|haiku-scout|T43|todo|Publish v0.7.0 for Gate 6
G6|-|egor|T44|gate|Gate 6: plane test, keep or drop (manual.md G6)
T45|L|sonnet-builder|G6|todo|Finish the plane, or remove it
T46|M|sonnet-builder|T45|todo|Version checks between plugin and pack
T47|L|sonnet-builder|T46|todo|Performance pass
T48|M|sonnet-builder|T47|todo|Help, guide and tidy-up
T49|S|haiku-scout|T48|todo|Publish v1.0.0 for the final gate
G7|-|egor|T49|gate|Final gate: a full evening on the server (manual.md G7)

## log (newest first, keep 8)
- 2026-11-08 T26: command blocks spawn vehicles (`fmod vehicle spawn tank|jeep`), 2 blocks up, facing the block. Refused within 3 blocks or at 10 out. Idle 10 min despawns ('Idle check:'). 126 tests pass, not tried in game. manual.md 'Vehicle pads'.
- 2026-11-08 G2 passed ("all fine, carry on"). T21: planned phases 3 to 6 as T26 to T50 with gates G3 to G7. Decided with Egor: vehicles come from command-block pads with a button, block damage and protected zones are parked, planes get their own keep-or-drop test (G6). Added T50, the group's own greeting emote with its name and sound in config.yml. Egor then dropped the other planned emotes and the emote menu (old T27, T29, T30, T31): the greeting is the only new emote.
- 2026-11-08 Fix: barrel model tilted the wrong way (shells were fine), BARREL_PITCH_SIGN now -1f. v0.3.3 released, pack SHA-1 de475fe9. Turret visibly moves in game on v0.3.2; no-shot-on-mount not yet confirmed.
- 2026-11-08 Fix: turret, barrel and jeep wheels didn't move (BetterModel caches modifier rotation with no animation; we now write lastModifiedRotation + firstTick by reflection, barrel carries turret yaw). Clicks within 0.5 s of mounting ignored, so getting in no longer fires. v0.3.2 released, pack SHA-1 583df984. Not tried in game.
- 2026-11-08 Fix: couldn't get into vehicles ("not ready") because seat lookup used p_driver; BetterModel strips the p_ tag, so it's now "driver". v0.3.1 released, pack SHA-1 d8b739cb. Seat failures now log a 'Driver check:' warning. Server: /fmod update, then restart.
- 2026-11-08 T20: v0.3.0 released (tank and jeep). Pack forzammilmod-pack-0.3.0.zip, SHA-1 de83929d. manual.md G2 has the checklist and commands. Server needs /fmod update and a restart first.
- 2026-11-08 T19: jeep wheels spin with speed (radius 5 px, roll about X). No gun, so no turret. 116 tests pass, not tried in game. Watch 'Wheel check:' log; if backwards, flip WHEEL_SPIN_SIGN in BetterModelHook.
- 2026-11-08 T18: tank fires on left-click. Shells reach 150 blocks, explode at power 2.5, credited to driver. 4 s reload. Not tried in game. Check 'Fire check:' log for the click path. Known: mounting may fire; walls hurt driver.

## blockers
none

## gate notes
G1: -
G2: passed. Egor: all fine, carry on.

## parked (not planned yet, ask Egor before starting)
- Block damage on the real world: protected zones and blocks growing back. Egor: "we will work on that later".
- Tank ammo limits (reload timer only for now).
