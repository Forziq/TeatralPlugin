# progress
now: T36 todo | G1 passed | G2 passed
updated: 2026-11-10

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
T50|M|sonnet-builder|T26|done|Our own greeting emote, configurable name and sound
T28|M|sonnet-builder|T50|done|Held items and armour during emotes
T32|S|haiku-scout|T28|done|Publish v0.4.0 for Gate 3
G3|-|egor|T32|done|Gate 3: greeting test with friends (manual.md G3)
T33|L|sonnet-builder|G3|done|Vehicle health, damage and repairs
T34|L|sonnet-builder|T33|done|Crew seats: gunner and passengers
T35|L|opus-deep|T34|done|Proper tank and jeep models, with track animation
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
- 2026-11-10 Extra: tank sounds redone (old in sounds/old/). Redone: armour_hit, explosion_small, reload_clank, shell_whistle. New: cannon_fire_tank, turret_traverse_loop, turret_traverse_stop, armour_ricochet. Unheard, unwired.
- 2026-11-09 Extra (no task id): bulkier look-alikes tank_heavy and jeep_heavy beside the plain ones, for Egor to pick. `/fmod vehicle spawn tank_heavy|jeep_heavy`. New VehicleType entries (same driving and health, bigger hit boxes), same script makes all four models. 185 tests pass, models load on the test server, not tried in game. Practice release 0.4.1 built fine with them in the pack (nothing published); the server sees them only after a real release. Drop the losing pair once Egor picks.
- 2026-11-09 T35: new T-34 style tank (red star, 10 road wheels) and jeep. Made by scripts/make-vehicle-models.py, overwriting Blockbench edits. Tank wheels spin via WheelSpin. BARREL_LENGTH 42/16. Pack 16.1 MB. 182 tests pass, not tried in game.
- 2026-11-09 T35 check in game: wheels roll the right way (WHEEL_SPIN_SIGN), log shows 10 wheel bones, seats sit right, slopes lean right. No scrolling tracks (BetterModel can't tie texture to speed). T36: wheel bones resent each tick; slow if laggy.
- 2026-11-08 T34: tank gunner seat, jeep two passenger seats. Right-click takes first free seat, sneak exits. Gunner aims and fires; with no gunner the driver does both. All ejected when destroyed. Jeep punches via arm swing; blasts don't punch.
- 2026-11-08 T34: 181 tests pass, not tried in game. T35 check: seat positions are guesses, and BetterModel mounting on sp_ bones is unconfirmed ('Seat check' in log). BetterModel's shutdown error isn't ours.
- 2026-11-08 T33: tank 100 HP, jeep 40. Shell hull hits: tank 25 front, 35 side, 50 back. Iron ingot repairs 10%. 170 tests pass, untested in game. T34 check: front/back may flip, jeep melee may miss Interaction, blasts may count as jeep punches.
- 2026-11-08 G3 passed, no notes from Egor. Next: T33 vehicle health, damage and repairs.
- 2026-11-08 T32: v0.4.0 released for Gate 3, pack SHA-1 ad7aec7e. manual.md G3 has the checklist. Server: /fmod update, then restart.

## blockers
none

## gate notes
G1: -
G2: passed. Egor: all fine, carry on.
G3: passed. Egor gave no notes on the checklist items.

## parked (not planned yet, ask Egor before starting)
- Block damage on the real world: protected zones and blocks growing back. Egor: "we will work on that later".
