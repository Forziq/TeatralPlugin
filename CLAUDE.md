# forzammilmod (TeatralPlugin repo)

Private Spigot plugin for Egor's friends' server: emotes first, WWII-style vehicles second. Read this file, then `progress.md`. Read `docs/brief.md` only when planning a phase.

## Fixed decisions (override the brief where they differ)
- Plugin name: `forzammilmod` (jar, data folder, sound keys). Command root: `/fmod` (from T25 on; older code used `/forzammilmod`). Commands: `/fmod <sub>`, plus `/emote wave|stop`.
- Package `dev.egorgrigorciuc.forzammilmod`, Java 21, Gradle Kotlin DSL, Spigot API 1.21.4 (not Paper). Use only Spigot API, e.g. `PlayerInputEvent` / `Player#getCurrentInput()` for driving.
- Rendering: BetterModel (version pinned in `docs/versions.md`). Skins: SkinsRestorer. No AuthMe, no whitelist, no test range world.
- Server: exaroton, Spigot 1.21.4, offline mode. Egor deploys himself. Everyone must accept the resource pack (kick on decline).
- Permissions during MVPs: emotes for everyone. Only the player named `Forzammil` may spawn or remove vehicles and run `/fmod update`. Anyone may drive a spawned vehicle. Hard-code the name in one constant `Access.OWNER_NAME`.
- Languages: English and Russian, picked from the player's client locale (`ru_*` gets Russian, everything else English). No language command. Every player-facing string lives in `src/main/resources/lang/en.yml` and `ru.yml`. Russian uses proper plural forms (one / few / many). Item names in the resource pack use pack lang files.
- Releases: private GitHub repo `Forziq/TeatralPlugin`. Jar and `release.json` go to GitHub Releases. The resource pack is published to the repo's GitHub Pages site (public URL) with its SHA-1.
- Updates: the plugin checks GitHub on startup and downloads a newer jar into Bukkit's update folder (applied on next restart). `/fmod update` does the same on demand. GitHub token lives in `plugins/forzammilmod/config.yml` on the server, never in the repo.
- Music: a shared server radio (`/fmod music play|stop|shuffle|loop|list`), anyone can use it, everyone hears it. Tracks and their English/Russian names live in `assets/music/tracks.yml`. Every new command goes under `/fmod`; `/emote` is the only exception.
- MVP rule (MVPs passed at G2): placeholder art and hard-coded values until a task says otherwise. Phase tasks may add menus, real models and sounds. Config holds only the GitHub token and, from T50, the greeting emote's id, names and sound.
- Getting vehicles (from T26): command-block pads. A command block running `fmod vehicle spawn <type>` with a button spawns the vehicle 2 blocks above it, for anyone. Typing the command yourself stays owner-only.
- Parked until Egor says otherwise: protected zones and block regrowth (shells keep today's block damage), tank ammo, two-player emotes.

## Agents and who does what
- **Main session (Opus, medium effort):** reads the task, plans, writes a brief, delegates, reviews. Keep your own turns few: Egor is on Pro.
- **opus-deep (Opus, high):** tasks marked `hard` in the task file, or anything that failed twice.
- **sonnet-builder:** writes code, models (.bbmodel JSON), scripts and Russian text from a brief.
- **sonnet-tester:** writes and runs unit tests, runs the smoke test, reads logs. Never fixes code itself.
- **haiku-scout:** doc lookups (Context7 first, then web), file searches, `./gradlew build` for S checks, git commit/push, GitHub releases, and writing `progress.md` (always with the uk-writing skill).

## Checks by task size
- **S:** haiku-scout runs `./gradlew build`.
- **M:** sonnet-tester runs `./gradlew test` (adds tests for new pure logic).
- **L:** M plus `scripts/smoke-test.sh` on the local server, then main reviews `git diff`.

## Token rules
- Egor runs `/clear` after every task, so everything the next session needs must be in `progress.md` or the task files.
- Never paste whole logs or large files into context. Use `grep`, `tail -n 60`, or ask haiku-scout for a summary.
- Doc lookups go to haiku-scout, not the main session.
- Give subagents a complete brief so they don't need to re-read the repo.

## Dashboard
Report what you're doing so Egor can watch it at http://localhost:4321:
`node .claude/dashboard/status.mjs <agent> "<plain-English status>"`
Agents: `main`, `opus-deep`, `sonnet-builder`, `sonnet-tester`, `haiku-scout`. Call it when you start, at each real step change, and when done. Tool calls are logged automatically by hooks.

Egor reads these to follow along without looking at code, so write them the way you'd tell a friend what you're up to:
- Plain everyday words, present tense, under 80 characters. Say what you're doing and, if it helps, why.
- No task ids at the start, file paths, commands, flags, tool or library names, or jargon (commit, push, scaffold, smoke test, M-size check).
- The same goes for the `description` you give a Bash command and the description you give a subagent job: "Check which sounds have finished", "Make the sound effects", not "T04 generate ElevenLabs sounds".

| Instead of | Write |
|---|---|
| T04: committing sound library and pushing | Saving the new sounds and uploading them to GitHub |
| T02: scaffolding gradle project | Setting up the empty plugin project |
| T03 dashboard checks | Checking the dashboard page works |
| T04: blocked - ffmpeg has no libvorbis | Stuck: the sound converter can't make Minecraft's sound format |
| T06: running smoke-test.sh | Starting a test server to see if the plugin loads |

## Git
One commit per task: `Txx: short summary`. Commit only after checks pass. Push to `origin main` if a remote exists. Never commit `server/`, tokens, or `.claude/dashboard/events.jsonl`.

## Paths
- Plugin source: `src/`. Models: `src/main/resources/models/*.bbmodel` (the plugin copies them into BetterModel's folder on startup).
- Sounds: `assets/sounds/*.ogg` (mono). Pack extras: `assets/pack/`.
- Local test server: `server/` (gitignored). Scripts: `scripts/`.
- Manual steps for Egor: `manual.md`. Add to it whenever you hit something you can't do yourself, even with Claude in Chrome and 1Password.
