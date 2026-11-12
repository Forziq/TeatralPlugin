# progress
now: T48 doing | G1 passed | G2 passed
updated: 2026-11-12

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
T36|M|sonnet-builder|T34|done|Vehicle sounds
T37|M|sonnet-builder|T36|done|Vehicle HUD
T38|S|haiku-scout|T35,T37|done|Publish v0.5.0 for Gate 4
G4|-|egor|T38|done|Gate 4: tank battle with friends (manual.md G4)
T39|M|sonnet-builder|G4|done|Army truck
T40|L|sonnet-builder|T39|done|Field artillery
T41|S|haiku-scout|T40|done|Publish v0.6.0 for Gate 5
G5|-|egor|T41|done|Gate 5: truck and artillery (manual.md G5)
T42|S|haiku-scout|G5|done|Research how to fly a plane
T43|L|opus-deep|T42|done|Rough plane
T44|S|haiku-scout|T43|done|Publish v0.7.0 for Gate 6
G6|-|egor|T44|done|Gate 6: plane test, keep or drop (manual.md G6)
T45|L|sonnet-builder|G6|done|Finish the plane, or remove it
T46|M|sonnet-builder|T45|done|Version checks between plugin and pack
T47|L|sonnet-builder|T46|done|Performance pass
T48|M|sonnet-builder|T47|doing|Help, guide and tidy-up
T49|S|haiku-scout|T48|todo|Publish v1.0.0 for the final gate
G7|-|egor|T49|gate|Final gate: a full evening on the server (manual.md G7)

## gate notes
- G6 (2026-11-11): passed, planes stay. Egor wants climb and dive on keys, not on where you look: Space climbs, Sprint dives (in tasks/T45.md step 1). No other checklist notes given.

## log (newest first, keep 8)
- 2026-11-12 T47: speed pass. 20 TPS before and after, 10 vehicles firing. Our cost ~1.1 ms a tick before, 0.8 ms after (budget 50). Hidden `/fmod debug load` and `/fmod debug perf`. 301 tests pass. Not measured: players aboard, music, emotes.
- 2026-11-12 T46: `/fmod version` shows plugin, pack and BetterModel versions, OK or mismatched. Startup warns on mismatch. Pack has forzammilmod/version.txt; set version= in pack.properties on release. 291 tests pass, not tried on a server.
- 2026-11-12 T45: plane finished. Space climbs, Sprint dives, looking no longer steers. Click fires bursts, 40 health. 276 tests pass, not flown. Unverified: Sprint while seated, Ctrl+click on Mac. Tuning in MachineGun.java.
- 2026-11-11 T44: v0.7.0 published: jar on GitHub Releases, pack at forziq.github.io/TeatralPlugin/pack/forzammilmod-pack-0.7.0.zip (SHA-1 acb3e758...). Plane model is in (names hidden in pack). G6 checklist in manual.md.
- 2026-11-11 T43: rough plane (`/fmod vehicle spawn plane`), 30 health. W/S throttle, A/D bank, look to climb. 264 tests pass, not flown. T44: rebuild pack with plane.bbmodel. Wrong nose or bank: flip Vehicle.PLANE_PITCH_SIGN or BetterModelHook.PLANE_ROLL_SIGN.
- 2026-11-11 T42: plane notes in docs/notes/planes.md. Input via Player#getCurrentInput; move like the tank (BetterModel, 2-3 ticks). Kick: setAllowFlight on pilot, else global allow-flight (unverified). T43: start with open questions.
- 2026-11-10 T41: v0.6.0 published: jar on GitHub Releases, pack at https://forziq.github.io/TeatralPlugin/pack/forzammilmod-pack-0.6.0.zip (SHA-1 93205b21fbcf7509b484bc3283cf8c707cb7dd65). manual.md G5 checklist added.
- 2026-11-10 T40: field gun added (`/fmod vehicle spawn artillery`), no driving, A/D turn it, barrel follows gunner's look. Arcing shells, 8 s reload, 50 health. 244 tests pass, not tried in game. T41: pack needs artillery.bbmodel.
