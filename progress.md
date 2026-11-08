# progress
now: G2 gate | G1 passed | T20 done
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
G2|-|egor|T20|gate|Gate 2: tank test with friends (manual.md G2)
T21|L|main|G2|todo|Plan phases 3 to 6 from the gate notes

## log (newest first, keep 8)
- 2026-11-08 Fix: barrel model tilted the wrong way (shells were fine), BARREL_PITCH_SIGN now -1f. v0.3.3 released, pack SHA-1 de475fe9. Turret turning and no-shot-on-mount confirmed working in game on v0.3.2.
- 2026-11-08 Fix: turret, barrel and jeep wheels didn't move (BetterModel caches modifier rotation with no animation; we now write lastModifiedRotation + firstTick by reflection, barrel carries turret yaw). Clicks within 0.5 s of mounting ignored, so getting in no longer fires. v0.3.2 released, pack SHA-1 583df984. Not tried in game.
- 2026-11-08 Fix: couldn't get into vehicles ("not ready") because seat lookup used p_driver; BetterModel strips the p_ tag, so it's now "driver". v0.3.1 released, pack SHA-1 d8b739cb. Seat failures now log a 'Driver check:' warning. Server: /fmod update, then restart.
- 2026-11-08 T20: v0.3.0 released (tank and jeep). Pack forzammilmod-pack-0.3.0.zip, SHA-1 de83929d. manual.md G2 has the checklist and commands. Server needs /fmod update and a restart first.
- 2026-11-08 T19: jeep wheels spin with speed (radius 5 px, roll about X). No gun, so no turret. 116 tests pass, not tried in game. Watch 'Wheel check:' log; if backwards, flip WHEEL_SPIN_SIGN in BetterModelHook.
- 2026-11-08 T18: tank fires on left-click. Shells reach 150 blocks, explode at power 2.5, credited to driver. 4 s reload. Not tried in game. Check 'Fire check:' log for the click path. Known: mounting may fire; walls hurt driver.
- 2026-11-08 T17: turret and barrel follow the driver's look. Unit tests pass (104), not tried in game. Watch 'Turret check' lines on tank spawn; if wrong way, flip TURRET_YAW_SIGN or BARREL_PITCH_SIGN in BetterModelHook. T18: use Vehicle.muzzle().
- 2026-11-08 T16: driving. /fmod vehicle spawn tank|jeep, remove (owner), list. Driver in p_driver seat, controller NONE. Untested: check the three "Driver check:" log lines. See docs/notes/vehicles.md. T17 builds on Vehicle/VehicleRig.

## blockers
none

## gate notes
G1: -
G2: -
