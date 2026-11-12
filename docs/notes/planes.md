# Planes: input, movement, chunks, flight kick (T42)

Research for a rough, flyable plane. No code written. Pinned stack: Spigot 1.21.4 (not Paper), BetterModel 2.2.0 (docs/versions.md), exaroton, offline mode. Our driving pattern (docs/notes/vehicles.md, T14 to T17) is the starting point: an invisible armour stand anchor, a BetterModel `p_` seat with controller `NONE`, and our own tick loop that moves the anchor. A plane can reuse that pattern.

Sources: Spigot javadoc through Context7 (https://hub.spigotmc.org/javadocs/spigot/), the BetterModel 2.2.0 tag (https://github.com/toxicity188/BetterModel/tree/2.2.0), and web search for the rest. The SpigotMC resource pages returned 403 earlier, so their facts are unverified. Anything not confirmed by a source or a test is marked **unverified**.

## 1. Inputs while riding

**API (Spigot javadoc via Context7):**
- `org.bukkit.Input`: `isForward()`, `isBackward()`, `isLeft()`, `isRight()`, `isJump()`, `isSneak()`, each returns `boolean`. Source: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Input.html
- `Player.getCurrentInput()` returns `Input`, "the current movement input, as last provided by the player". Source: https://hub.spigotmc.org/javadocs/spigot/index-all.html
- `PlayerInputEvent.getInput()` returns the new `Input`. Source: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/event/player/PlayerInputEvent.html
- `isSprint()`: **unverified** (not in the Context7 extracts). Test before using it.
- Which Spigot build added these: **unverified**. The vehicle work already uses `getCurrentInput()` on 1.21.4, so it works there (per vehicles.md, not yet tried in game).
- How often `PlayerInputEvent` fires: **unverified**. It probably fires on change, not every tick. Poll `getCurrentInput()` in the tick loop, as the tank does, unless a test shows the event is needed.

**Suggested mapping (a plane):**

| Input | Plane action |
|---|---|
| Forward / backward | Throttle up / down. Speed ramps, it does not jump. |
| Left / right | Roll (bank) and yaw. Bank first, then turn with the bank. |
| Jump | Pitch up (nose up). |
| Sneak | Pitch down, **or** nothing (see below). |
| Sprint | Boost, if `isSprint()` is confirmed. Otherwise leave it out. |

**Pitch option:** use the pilot's look direction (`player.getLocation().getPitch()`) instead of jump or sneak. Looking up climbs, looking down dives. This needs no extra keys, but the view then steers the plane, and a pilot who looks around will change course by accident. Keeping the look for the gunner and the view only avoids that. Pick one before building.

**Sneak dismounts by default.** In vanilla, sneak leaves a vehicle. BetterModel's `EntityManager` cancels the dismount only when the seat controller can fly or cannot be dismounted by the rider (EntityManager.kt, line 92, per docs/notes/vehicles.md). With controller `NONE`, `canDismountBySelf()` is true, so sneak dismounts. Options:
1. Leave sneak as dismount. Simple, and it gives the pilot a way out. Use it as "bail out" only. Do not use it for pitch.
2. Cancel the dismount with a custom controller where `canDismountBySelf()` returns false (BetterModel `MountController`, https://github.com/toxicity188/BetterModel/blob/2.2.0/api/src/main/java/kr/toxicity/model/api/mount/MountController.java). Then the pilot needs a separate exit, such as a command or a right-click. **Unverified in game.**

**While riding:** BetterModel reads the rider's input on its own seats (HitBoxImpl.kt, `rideInput`, lines 262 to 276, per vehicles.md). So input reaches the server while a player sits in a seat. The tank already relies on this. **Unverified on Spigot 1.21.4 for a plane.**

## 2. Smooth movement and jitter

**What exists:**
- `TrackerUpdateAction.moveDuration(int)`, "movement interpolation duration", in ticks (https://github.com/toxicity188/BetterModel/blob/2.2.0/api/src/main/java/kr/toxicity/model/api/tracker/TrackerUpdateAction.java, line 164). This is BetterModel's wrapper for the display-entity teleport duration.
- `Display.setTeleportDuration(int)`, "the teleport duration of this display", in ticks. Source: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/entity/Display.html . Use it if we ever move item displays directly. BetterModel does this for us.
- `Display.setTransformation(Transformation)`. Same source.
- Per-bone animation interpolation: `RenderedBone.interpolationDuration()` (RenderedBone.java, lines 603 to 607, per vehicles.md). `BetterModelConfig.lerpFrameTime()` sets the global value in milliseconds (BetterModelConfig.java, line 189).

**Why a moving plane can look jerky:**
- The anchor is moved by our code, so the rider's view and the model lag behind the server by about one tick plus the rider's ping. At 50 to 150 ms ping, that is roughly 1 to 3 ticks of delay.
- Each tick the anchor moves 1 block at the top speed we plan. If each move is a hard jump, the model steps once per tick (visible as stutter).
- A `moveDuration` of 2 to 3 ticks blends the steps. vehicles.md uses 2 for the tank. The plane will need the same, or a test that shows 3 is needed.

**Speed and jitter thresholds: unverified.** I found no source that gives a speed at which jitter starts for a given ping. The plan is to test:
1. Fly straight at 0.5, 1 and 1.5 blocks per tick at ping 50, 100 and 150 ms (use a proxy or a friend's connection).
2. Note the speed where the model stutters.
3. Record it in this file.

Vanilla clients smooth their own vehicle movement with client prediction. Our anchor is not predicted, so expect worse smoothing than a boat. **Unverified.**

## 3. Chunk loading at about 1 block per tick (20 blocks per second)

**What loads chunks:** the player (not the plane). The pilot's view and simulation distance decide what is loaded around them. Entities do not keep their own chunks loaded here.

**Crossing a chunk:** a chunk is 16 blocks. At 20 blocks per second a plane crosses one every 0.8 s. The concern is new chunks generating ahead of the pilot. Loading existing chunks is cheap. Generating new ones is slow and can stall the server thread. **Unverified how bad this is on exaroton.**

**Settings that matter (server.properties, spigot.yml):**
- `view-distance`: how far chunks are sent to the client. Community guides suggest 6 to 10, and warn against going much above 10 to 12. Sources: https://wabbanode.com/help/minecraft/how-to-adjust-view-distance-and-simulation-distance-on-your-minecraft-server , https://shockbyte.com/help/knowledgebase/articles/changing-view-and-simulation-distances-on-a-minecraft-server
- `simulation-distance`: how far chunks tick around each player. Guides suggest about 4 (same sources). It must cover the plane's flight area, because an unticked chunk stops the plane's model updates.
- Spigot's `spigot.yml` `view-distance` defaults to reading `server.properties`. Keep it at the default (same guides). Exact key names: **unverified** for 1.21.4.
- `entity-tracking-range` in `spigot.yml`: how far away entities are sent to players. Guides give players 48, animals and monsters 32 to 48, misc 32 to 48, other 64, and display 96 in one guide. These are guides, not Spigot docs, so treat them as **unverified** until read in the 1.21.4 `spigot.yml` (https://hub.spigotmc.org/javadocs/ is not the config reference; check the Spigot wiki for the 1.21 config).
- Entity tracking matters for the BetterModel item displays. If they stop being sent, the model vanishes at range. Raise the display value before flying far.

**exaroton:** I could not find exaroton's own help page for these settings. Community sources say exaroton offers higher render distances on paid plans. Whether the panel exposes `view-distance` and `simulation-distance`, or only lets us edit `server.properties` by file, is **unverified**. Egor should check the exaroton console or file editor and tell us which it is.

**Practical plan:** keep `simulation-distance` at 4 or 5. Fly over a pre-generated area (`/fmod` or a world pre-generator, unverified which). Raise `view-distance` only if the pilot sees the world pop in.

## 4. Stopping the pilot being kicked for flying

**The kick:** "Flying is not enabled on this server" is the vanilla and Spigot anti-fly check. It applies to a player who is airborne with no flight permission.

**Global switch: `allow-flight=true` in server.properties.**
- Turns flight on for the whole server. Players can then double-tap jump to fly (creative style). That is a side effect everyone gets.
- Hosting guides recommend this as the fix and say it does not add `/fly`. Source: https://xgamingserver.com/docs/minecraft/enable-flight (community guide, **unverified** as a Spigot doc).
- Only the flag matters for the kick. It does not stop the check per player.

**Per-player: `Player.setAllowFlight(boolean)`.** Javadoc: "Sets if the Player is allowed to fly via jump key double-tap like in creative mode." Source: https://hub.spigotmc.org/javadocs/spigot/index-all.html . Whether this stops the kick for a passenger without the global flag: **unverified**. It is the first thing to test, because it would limit the side effect to the pilot. Use `Player.isFlying()` to read the state.

**Passengers and vehicles:** Mojang and Spigot bug reports show a second check, "floating a vehicle too long", which kicks players riding an airborne vehicle with no gravity (boats with a NoGravity tag). Sources:
- https://hub.spigotmc.org/jira/si/jira.issueviews:issue-html/SPIGOT-2244/SPIGOT-2244.html (kicked for flying when cancelling VehicleExitEvent)
- https://bugs-legacy.mojang.com/browse/MC-100661 (vehicle kick, allow-flight false only)

Whether our pilot (a player riding a BetterModel seat on an invisible armour stand, via an Interaction) is checked by that vehicle rule: **unverified**. The reports suggest a player only triggers it when the vehicle itself floats. Our anchor is a living entity, not a boat, so the risk should be low. Test it.

**Recommendation:**
1. Test first with `player.setAllowFlight(true)` on the pilot when they board, and `false` on dismount. Watch the server log for "kicked for flying".
2. If that does not stop the kick, set `allow-flight=true` globally on the test server. Tell Egor it lets everyone double-tap jump to fly, and ask him whether that is acceptable.
3. Do not leave `allow-flight` on without telling him. It removes one anti-cheat check for everyone.

## 5. Free plane or aircraft plugins

| Plugin | What it is | Versions | Licence | Status |
|---|---|---|---|---|
| uPlanes (https://dev.bukkit.org/projects/uplanes) | Smooth flyable planes on Minecarts, with spawn and autopilot admin commands | 1.21.4: **unverified** | **unverified** (check the page) | Worth an hour. Closest match found. |
| MTVehicles (https://www.boxtoplay.com/en/minecraft-hosting/minecraft-server/plugins/spigot-mtvehicles-vehicle-plugin-free-downloadable-43599) | Drivable cars, motorcycles, helicopters and tanks | 1.21.4: **unverified** (SpigotMC page 403) | Listed as free; licence **unverified** | Also in docs/notes/vehicles.md. Helicopter is the nearest to a plane. |
| VehiclesPlus (https://docs.sbdevelopment.tech/vehiclesplus-v3/setup) | Vehicle plugin with 1.21 mention | 1.21.x: **unverified** | Price and licence **unverified** | Plane support **unverified**. |

Not useful for this: the Choco Fly, Fly, SimpleFly, wFly and GFly results are fly-command plugins (players fly, no aircraft). Source list: https://www.boxtoplay.com/en/minecraft-hosting/minecraft-server/plugins/spigot-choco-fly-1-21-x-52752 (example). Do not use them for a plane.

Suggested hour: install uPlanes (or MTVehicles) on the local test server, fly it at our ping, and note the speed where it stutters. Check the licence before copying any code. Do not copy code from a plugin without Egor's go-ahead.

## 6. Open questions to test first

1. Does `PlayerInputEvent` fire on change, and is `isSprint()` on 1.21.4? (Log line in a test.)
2. Does `Player.setAllowFlight(true)` stop the kick for a pilot with no global flag?
3. Is the vehicle "floating too long" rule applied to our seat?
4. Smooth speed at 50, 100 and 150 ms ping (see section 2).
5. Whether exaroton's panel exposes `view-distance` and `simulation-distance`, or only the file.
6. Whether a 2 to 3 tick `moveDuration` is smooth for a plane at 1 block per tick.

## 7. Quick plan for the rough plane

- Reuse the vehicles.md pattern: invisible armour stand anchor, BetterModel seat with controller `NONE`, tick loop moves the anchor.
- Input: forward and back for throttle, left and right for bank and turn, jump for pitch up. Sneak dismounts (leave it).
- `moveDuration` 2 to 3 ticks. Speed capped at 1 block per tick until the ping test says otherwise.
- Flight: `setAllowFlight(true)` on the pilot first. Global `allow-flight` only if the test needs it, and only after Egor agrees.
- Keep the flight inside a pre-generated area. Raise display `entity-tracking-range` before long flights.

## T45: finished plane (controls and guns)

- Keys, not the look: W/S throttle, A/D bank, hold Space to climb (`isJump()`), hold Sprint to dive (`isSprint()`), neither or both levels the nose. Sneak bails out. `FlightModel.Input` takes a pitch command -1/0/+1; `DriveInput.pitch()` builds it from `getCurrentInput()` each tick. On the ground Space lifts the nose to 12 degrees (take-off = speed over 0.55 b/tick plus Space), Sprint cannot push it below level.
- Machine guns: the pilot's left click (same three click paths as the tank, via `ShellService.fireClick`) starts a `MachineGun` burst: 5 bullets over 10 ticks, 30 ticks cooldown after the burst. `PlaneGuns` raytraces each bullet from the nose along the plane's yaw and pitch, range 60. 3.0 damage to mobs and players, 2 health to vehicles via `VehicleService.damage`, no block damage. The plane and its pilot are skipped. Sound `mg_burst`.
- Health 40, health bar and speed and height read-out for the pilot. `FLIGHT_LOG` is off.
- Unverified in game: whether `isSprint()` is reported while seated (Sprint is the Ctrl key; on a Mac Ctrl plus click is a right click), and that left click reaches the plane pilot on all three paths.
