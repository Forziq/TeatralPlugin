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
   - `/Users/egorgrigorciuc/Desktop/Projects/TeatralPlugin/dist/forzammilmod-0.1.2.jar`
   - `BetterModel-2.2.0-spigot-fmod.jar` from `/Users/egorgrigorciuc/Projectsnosync/TeatralPlugin/dist/` (our patched copy; the plain Modrinth jar crashes emotes on Spigot)
   - `SkinsRestorer-15.12.6.jar` from the same folder
   (exact versions are in `docs/versions.md`)
4. Start the server, wait until it's fully up, then stop it.
5. Save the GitHub token. Claude Code made it on 2026-11-08 (`forzammilmod-server`, read-only, TeatralPlugin only, expires 2027-11-08) and left the GitHub tab open in Chrome:
   - Click the copy icon next to the token on that page. GitHub only shows it once.
   - Save it in 1Password as a new item called `forzammilmod GitHub token`. Don't paste it into any file in the project.
   - If the tab got closed before you copied it, delete `forzammilmod-server` at https://github.com/settings/personal-access-tokens and make a new one with the same settings (Only select repositories: TeatralPlugin; Contents: Read-only).
6. Open `plugins/forzammilmod/config.yml` in exaroton's file editor. Paste the token between the quotes on the `github-token:` line (the key is `github-token`). Save.
7. Start the server again and join. You should be asked to download the resource pack; accept it. Type `/fmod version` and check it says 0.1.2. Then type `/fmod pack`: you should hear a cheer, which means our pack loaded. If exaroton also forces its own resource pack, players get both, and ours loads on top.

From now on, new versions download by themselves when the server starts and go live on the next restart. If you want one straight away, type `/fmod update` in game and then restart the server from exaroton.

Claude Code will add any BetterModel setting the server needs here (T07 and T09 find out).

## M03 Try the wave locally (optional, after T12)

1. Run `scripts/server-start.sh` in Terminal, or ask Claude Code to start the local test server.
2. Open Minecraft 1.21.4 in TLauncher, go to Multiplayer and add the server `localhost`.
3. Type `/emote` to list the emotes, then `/emote wave` to start one.
4. Type `/emote stop` to stop it. Start a wave again and walk a few steps: walking should cancel it.
5. Start a wave again and take damage (fall off a block, or hit the player with a second account): damage should cancel it.
6. Open `src/main/resources/models/players/forzammil_player.bbmodel` in Blockbench, go to the Animate tab and play `wave` once. Check that only the right arm moves: it should rise out to the side and over the head, wave side to side three times, then drop back down by the 2-second mark. If it swings into the head or across the body instead, tell Claude the arm goes the wrong way.
7. Watch for these in game and note anything odd:
   - Does the arm wave outward? If it swings across the body, the Z signs need flipping.
   - After the wave ends or is stopped, does the player look normal again, with no leftover or half-shown model?
   - Does a second `/emote wave` straight after the first work?
   - Does looking around without walking keep the wave going?
8. Type `stop` in the server window when you're finished.

### Music radio (after T23)

1. Join the local server as before. Type `/fmod music list`. You should see the track list with English and Russian names.
2. Type `/fmod music play stalnoy_marsh`. The track should start and everyone on the server should hear it.
3. Type `/fmod music loop` twice. Check the chat says the loop state changed each time, and that the track repeats when it ends.
4. Type `/fmod music shuffle`. Check the next track is different from the one playing.
5. Type `/fmod music stop`. The music should stop for everyone.
6. Type `/fmod music play nope`. You should get a clear error, not a crash.
7. Tab completion: type `/fmod mu` and press Tab. It should finish to `music`. Then check that `music` offers `play`, `stop`, `shuffle`, `loop` and `list`, and that `play` offers the track names.
8. Late joiner: start a track, then join with a second account (offline mode, any name). The second account should get a chat note saying what is playing, and hear music from the next track, not the one already running.
9. Volume: open the Jukebox or Note Blocks volume slider in the Options menu (Music or Blocks). Move it down and up. It should change the radio volume.
10. Watch for these and note anything odd: a track that cuts out early, two tracks playing at once, or a player who gets no sound after rejoining.
11. Type `stop` in the server window when you're finished.

## G1 Gate 1: emote and radio test with friends (after T13)

Update the server first:
1. In game on the exaroton server, type `/fmod update`.
2. Restart the server from the exaroton panel.
3. Type `/fmod version`. It should say 0.2.0. If it doesn't, tell Claude.

Get ready:
- Each friend needs TLauncher on 1.21.4 and should set a skin with `/skin <name>` once they're in.
- Get at least two friends on, ideally one with the game set to Russian.

Jot down a few words for each check:

Skins and model:
- Your SkinsRestorer skin shows on the emote model, not Steve
- Changing your skin with `/skin` updates the model
- Others see the wave play smoothly (note any flicker at the start or end)
- How it looks to you in first person and in F5
- What happens to held items and armour
- The Russian text reads naturally

Emotes:
- `/emote` lists the emotes, `/emote wave` starts one, `/emote stop` stops it
- Walking cancels a wave, and so does taking damage
- Does the arm wave outward, not across the body? Does the player look normal afterwards? Does a second `/emote wave` straight after the first work? Does looking around keep it going?

Radio:
- `/fmod music list` shows the tracks in English and Russian
- `/fmod music play stalnoy_marsh` plays for everyone. `loop` and `shuffle` change the state, and `stop` stops it for everyone
- `/fmod music play nope` gives a clear error, not a crash
- A player who joins mid-track gets a chat note and hears the next track
- The Jukebox or Note Blocks volume slider in Options changes the radio volume
- The comic song's words come through clearly enough to be funny

Then type `/next` in Claude Code. It'll ask whether the gate passed and take your notes.

## M04 Try a free vehicle plugin for an hour (optional, after T14)

T14 checks whether a free Spigot 1.21.4 plugin with drivable vehicles exists. If it finds one, the install steps go here. If not, skip this.

## M05 Try the tank locally (optional, after T16 to T19)

1. Start the local server (`scripts/smoke-test.sh` only checks it loads; to play, run the server in `server/` as usual) and join `localhost` as `Forzammil`.
2. Look at a block on flat ground and type `/fmod vehicle spawn tank`. The tank appears on that block, facing away from you.
3. Right-click the tank to get in. You should be sitting on it with the camera free to look around.
4. Drive on flat ground: W forward, S brake then reverse, A/D turn the hull (the tank turns on the spot). Looking around must not turn the tank.
5. Build a one-block step and drive into it: the tank should climb it. Then a two-block wall: the tank should stop against it and not go inside.
6. Drive off a ledge: it should drop and land, not sink into the ground.
7. Sneak to get out. You should land beside the tank, not inside it.
8. `/fmod vehicle list` shows it; `/fmod vehicle remove` (within 10 blocks) takes it away.
9. Repeat with `/fmod vehicle spawn jeep`: it is faster and only turns while moving.
10. After T18: get in the tank and left-click at a wall about 20 blocks away. You should see a smoke trail fly out and an explosion break blocks.
11. Click again straight away. The action bar counts down the reload and nothing fires. Wait 4 seconds and fire again.
12. Fire a few shells in a row and type `/tps`. It should stay near 20.
13. Look in the server log for the line "Fire check: click seen via ..." and tell Claude which word it says (interact, swing or hit).

What to watch for and tell Claude Code:
- If nothing moves when you press W, send the server log lines that start with "Driver check". They say whether the keys reach the server while you ride.
- If the model drives backwards (barrel at the back), say so: it is a one-number fix.
- Ask a second player (or a second game on the same PC) to watch while you drive: does the tank jitter or lag behind?
- Does your camera shake while driving or stepping up a block?
- Getting stuck in or on blocks, or getting out somewhere odd.

## G2 Gate 2: tank test with friends (after T20)

Switch the server to /fmod (v0.2.1):
The server is still on the old version, so it only knows the long command this one time.
1. In game on the exaroton server, type `/forzammilmod update`.
2. Restart the server from the exaroton panel.
3. From now on every command is `/fmod` (for example `/fmod update` or `/fmod music list`). `/emote` stays the same.

Get ready:
1. Do the switch above first, so the server is on the latest version.
2. Go well away from spawn, because shells break blocks.
3. Only you can spawn vehicles (`/fmod vehicle spawn tank` or `jeep`, and `/fmod vehicle remove`), but friends can drive the ones you spawn.

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

## Swap in the patched BetterModel (2026-11-08)
Emotes crashed with "Emotes aren't available right now" because of a bug in BetterModel's Spigot jar. Claude Code made a fixed copy.
1. Stop the exaroton server.
2. In Files > `plugins`, delete `BetterModel-2.2.0-spigot.jar`. Leave the `BetterModel` folder alone.
3. Upload `/Users/egorgrigorciuc/Projectsnosync/TeatralPlugin/dist/BetterModel-2.2.0-spigot-fmod.jar`.
4. Start the server and try `/emote wave` again. If it still fails, paste the log into Claude Code.

## M05 (optional): look at the tank and jeep in Blockbench
1. Open `src/main/resources/models/tank.bbmodel` and `jeep.bbmodel` in Blockbench (free, blockbench.net).
2. Check they look like a tank and a jeep, and that the turret, barrel and wheels are separate parts you can select.
3. Tell Claude Code if anything looks wrong.
