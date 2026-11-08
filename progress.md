# progress
now: M01 manual | G1 todo | G2 todo
updated: 2026-11-08

## tasks
id|size|who|deps|status|title
T01|S|haiku-scout|-|done|Install tools, create the GitHub repo, add Context7
M01|-|egor|T01|manual|Restart Claude Code so new MCP servers load (manual.md M01)
T02|M|sonnet-builder|M01|todo|Gradle scaffold and plugin skeleton
T03|M|sonnet-builder|T02|todo|Build and test the dashboard (/dashboard on/off)
T04|M|sonnet-builder|T03|todo|Generate the sound library with ElevenLabs
T05|M|sonnet-builder|T02|todo|Language system (English, Russian, plurals)
T06|M|sonnet-builder|T02|todo|Local test server and smoke test script
T07|M|sonnet-builder|T06|todo|Resource pack build and release pipeline (Pages)
T08|L|sonnet-builder|T05,T07|todo|Updater, pack sender and owner check
T09|S|haiku-scout|T08|todo|Publish starter release v0.1.0
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
- 2026-11-08 T01: installed Java 21 (openjdk@21 21.0.13), gh 2.103.3 and 1Password CLI 2.42.0 with Homebrew. Already there: Java 19 (default), Node 26.7.0, git 2.39.1, ffmpeg 8.0.1, Homebrew 7.0.11. Repo has no commits and is not pushed: gh is not logged in (see manual.md M00). Context7 is in .mcp.json, awaiting approval until Claude Code restarts (M01). ElevenLabs connector is visible.
- 2026-11-08 setup: harness and task plan written in the claude.ai chat. Hooks already log agent activity to .claude/dashboard/events.jsonl; the page itself is T03. Start with /next.

## blockers
none

## gate notes
G1: -
G2: -
