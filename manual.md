# Manual steps for Egor

These are the things Claude Code can't do for you, even with Claude in Chrome and 1Password. When `/next` reaches one of them it'll stop and tell you which section to follow, then ask whether you've done it.

Claude Code adds to this file whenever it hits something new, so check the bottom now and then.

## M00 Before the first `/next` (once)

1. Install the Claude Code setup. It couldn't be written straight into the hidden `.claude` folder from the claude.ai app, so it's waiting in `claude-setup/`. Have a look through it if you like, then in Terminal run:
   ```
   cd ~/Desktop/Projects/TeatralPlugin
   sh scripts/install-harness.sh
   ```
   This moves the agents, the `/next`, `/progress` and `/dashboard` commands, the activity logger, the uk-writing skill and `settings.json` into `.claude/`. The settings file sets the model to Opus and runs a small logger on every tool call so the dashboard knows what each agent is doing. It doesn't pre-approve any commands, so Claude Code will ask before running things; choose "always allow" for the ones you're happy with (builds, git, the dashboard and status scripts).
2. Make sure the Claude in Chrome extension and the 1Password extension are both installed and signed in in Chrome.
3. In the 1Password app go to Settings, then Developer, and turn on "Integrate with 1Password CLI". This lets Claude Code read a token from 1Password when it needs one, without ever seeing your master password.
4. Open Terminal and start Claude Code in the project folder with Chrome enabled:
   ```
   cd ~/Desktop/Projects/TeatralPlugin
   claude --chrome
   ```
   If your version doesn't recognise `--chrome`, start it with plain `claude` and type `/chrome`.
5. Type `/model`, pick Opus, and set the effort to Medium if it offers you a choice. The hard tasks switch to high effort on their own through the opus-deep agent.
6. Type `/next`. The first task (T01) installs Java 21, the GitHub CLI and the other tools, and creates the private repo.

If T01 asks for either of these, do them yourself in Terminal:
- **Java symlink** (optional, T01 did not run it): `sudo ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-21.jdk`. It needs your Mac password. The scripts don't need it: they use `JAVA_HOME=$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home`.
- **GitHub login** (T01 could not finish the push without it): run `gh auth login --web --git-protocol https` in Terminal, choose GitHub.com and "Login with a web browser". Then ask Claude Code to finish T01 (repo create or push to `origin main`).

## M01 Restart Claude Code (after T01)

T01 adds the Context7 documentation server, and Claude Code only loads new servers when it starts.

1. Type `/exit`, then start it again with `claude --chrome`.
2. Type `/mcp` and check that `context7` shows as connected. Note whether you can see ElevenLabs in the list too.
3. Type `/next` and answer "Done" when it asks about M01.

## E1 If Claude Code can't see ElevenLabs (only if T04 is blocked)

Option A, using your claude.ai connector:
1. Make sure Claude Code is logged in with the same Claude account you use in the app (`/login` shows it).
2. On claude.ai, open Settings, then Connectors, and check ElevenLabs is connected.
3. Restart Claude Code and check `/mcp` again.

Option B, if A doesn't work:
1. On elevenlabs.io, open your profile, then API Keys, and create a key.
2. Save it in 1Password as an API Credential called `ElevenLabs API key`.
3. Tell Claude Code: "Add the ElevenLabs MCP server for this project using the key in 1Password". It'll install `uv` and add the server with local scope, so the key never ends up in git.
4. Restart Claude Code, then `/next`.

Do this soon. The sounds need your ElevenLabs plan while it's still active.

## M02 Put the starter version on exaroton (after T09)

You only do this once. After that the plugin updates itself.

1. On exaroton, open the server's Software page and make sure it's Spigot 1.21.4. If it's on Paper or another version, tell Claude Code before going further.
2. In Options or server.properties, check `online-mode` is `false`.
3. Open the Files page, go into `plugins`, and upload these three jars (uploading doesn't work on exaroton's free trial):
   - `dist/forzammilmod-0.1.0.jar` from the project folder
   - the BetterModel and SkinsRestorer jars from `server/plugins/` in the project folder (exact versions are in `docs/versions.md`)
4. Start the server, wait until it's fully up, then stop it.
5. Open `plugins/forzammilmod/config.yml` in exaroton's file editor. Copy the token from the 1Password item `forzammilmod GitHub token` and paste it between the quotes on the `github-token:` line. Save.
6. Start the server again and join. You should be asked to download the resource pack; accept it. Type `/forzammilmod version` and check it says 0.1.0.

From now on, new versions download by themselves when the server starts and go live on the next restart. If you want one straight away, type `/forzammilmod update` in game and then restart the server from exaroton.

Claude Code will add any BetterModel setting the server needs here (T07 and T09 find out).

## M03 Try the wave locally (optional, after T12)

1. Ask Claude Code to start the local test server, or run `scripts/server-start.sh` in Terminal.
2. Open Minecraft 1.21.4 in TLauncher, go to Multiplayer and add the server `localhost`.
3. Type `/emote wave`, then walk to check it stops. `/emote stop` should also stop it.
4. If you like, open `src/main/resources/models/players/forzammil_player.bbmodel` in Blockbench, go to the Animate tab and play `wave`.
5. Type `stop` in the server window when you're finished.

## G1 Gate 1: emote test with friends (after T13)

Get ready:
1. On the exaroton server type `/forzammilmod update`, then restart it.
2. Each friend needs TLauncher on 1.21.4 and should set a skin with `/skin <name>` once they're in.
3. Get at least two friends on, ideally one with the game set to Russian.

Check each of these and jot down a few words for each:
- Your SkinsRestorer skin shows on the emote model, not Steve
- Changing your skin with `/skin` updates the model
- Others see the wave play smoothly (note any flicker at the start or end)
- How it looks to you in first person and in F5
- What happens to held items and armour
- The Russian text reads naturally

Then type `/next` in Claude Code. It'll ask whether the gate passed and take your notes.

## M04 Try a free vehicle plugin for an hour (optional, after T14)

T14 checks whether a free Spigot 1.21.4 plugin with drivable vehicles exists. If it finds one, the install steps go here. If not, skip this.

## M05 Try the tank locally (optional, after T16 to T19)

1. Start the local server and join `localhost` as `Forzammil`.
2. `/forzammilmod vehicle spawn tank`, right-click it to get in, drive with W/A/S/D, sneak to get out.
3. Try flat ground, a one-block step and a two-block wall.
4. After T18, fire at a wall and type `/tps` to see how the server copes.
5. After T19, try `/forzammilmod vehicle spawn jeep`.

## G2 Gate 2: tank test with friends (after T20)

Get ready:
1. `/forzammilmod update` on exaroton, then restart.
2. Go well away from spawn, because shells break blocks.
3. Only you can spawn vehicles (`/forzammilmod vehicle spawn tank` or `jeep`, and `/forzammilmod vehicle remove`), but friends can drive the ones you spawn.

Check each of these and jot down a few words for each:
- The delay between pressing a key and the tank moving
- Jitter when you watch someone else drive
- Climbing blocks and slopes, and getting stuck on terrain
- Whether turret aim feels natural
- Whether the explosions and the amount of block damage feel right
- Server TPS with two tanks firing
- Whether the jeep is still playable at speed
- The Russian text reads naturally

Then type `/next`. It'll take your notes and plan the next phase with you.

## Added by Claude Code
(nothing yet)
