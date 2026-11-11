# Vehicles: BetterModel seats, bones, input and clicks (T14)

Research for the tank and jeep driving core. Pinned BetterModel is 2.2.0 (docs/versions.md). I read the source at the `2.2.0` tag, commit `8d35ff5805bfeb87d13e01d2a950636aaf85563e`: https://github.com/toxicity188/BetterModel/tree/2.2.0. Context7 and the wiki often show 3.x code, so I checked the tag wherever they differ. The earlier player-model note is docs/notes/bettermodel.md (T10) and I did not repeat it.

Two limits on this work. The SpigotMC javadoc (hub.spigotmc.org) and the SpigotMC resource pages returned HTTP 403 to my fetches, so Spigot API facts come from Context7's copy of the javadoc. The BetterModel 2.2.0 code was read, not run. Anything not confirmed by one of those is marked **unverified**.

Note: the 2.2.0 tag builds against Minecraft 1.21.11. Our Spigot 1.21.4 jar (patched, versions.md) uses the NMS module `nms/v1_21_R3`, which I read for the hitbox code (unverified that the Modrinth jar matches the tag).

## 1. Attaching a model to our entity and moving bones

**Attach.** `ModelRenderer.getOrCreate(PlatformEntity)` exists at 2.2.0 (https://github.com/toxicity188/BetterModel/blob/2.2.0/api/src/main/java/kr/toxicity/model/api/data/renderer/ModelRenderer.java, line 282). The lookup is `BetterModel.model(name)` (https://github.com/toxicity188/BetterModel/blob/2.2.0/api/src/main/java/kr/toxicity/model/api/BetterModel.java, line 62). Context7 and the README show `BukkitAdapter.adapt(entity)`; I did not confirm that name at 2.2.0 (unverified).

```java
EntityTracker tracker = BetterModel.model("tank")
    .map(r -> r.getOrCreate(BukkitAdapter.adapt(anchor))) // unverified import
    .orElse(null);
```

**Find a bone.** `Tracker.bone(String)` returns `RenderedBone` or null (Tracker.java, line 772). `RenderedBone.name()` gives the parsed name.

**Rotate a bone.** Two routes exist.

1. `RenderedBone.addRotationModifier(Predicate<RenderedBone>, Function<Quaternionf, Quaternionf>)` (https://github.com/toxicity188/BetterModel/blob/2.2.0/api/src/main/java/kr/toxicity/model/api/bone/RenderedBone.java, line 290). It chains with `andThen`, so calling it every tick stacks modifiers. Add it once, and let the function read a mutable field (yaw, pitch) that our tick loop updates.
   - **Caveat (unverified in game):** the modifier output is cached in `after()` and only recomputed when the bone's animation state ticks true (RenderedBone.java, lines 553 to 600; AnimationStateHandler.java, line 113, returns false when there are no animators). So an idle turret may not refresh each tick. Test before relying on it.
2. Built-in head tracking. Bones tagged `h` (BoneTags `HEAD("h")`, https://github.com/toxicity188/BetterModel/blob/2.2.0/api/src/main/java/kr/toxicity/model/api/bone/BoneTags.java, line 28) get a rotation modifier from the entity's head pitch and head-minus-body yaw (EntityTracker.java, lines 120 to 123). `hi` (HEAD_WITH_CHILDREN) does the same for children. The yaw and pitch are clamped by `EntityBodyRotator` (`clampHead`, lines 95 to 97). The clamp limits look like about ±75 degrees (unverified exact values). A full 360 degree turret would need a custom modifier or a changed clamp. `EntityBodyRotator.setValue(Consumer<RotatorData>)` is public (line 161), so the clamp may be adjustable (unverified).

**Bone names.** Tags come from the prefix before `_` (BoneTagRegistry.java, `TAG_SPLITTER = "_"`, line 20). So a group named `h_turret` is a head bone, `b_hull` a hitbox, `p_driver` a seat, `sp_gunner` a passenger seat. BetterModel strips the tags from the name, so look bones up by the bare name (`tracker.bone("driver")`, not `"p_driver"`). Source: https://github.com/toxicity188/BetterModel/blob/2.2.0/api/src/main/java/kr/toxicity/model/api/bone/BoneTagRegistry.java

**Whole-model rotation.** `Tracker.rotation(Supplier<ModelRotation>)` and `rotator(ModelRotator)` (Tracker.java, lines 234 and 244). `ModelRotation(x, y)` is pitch and yaw. This rotates the model root, so it can carry the hull yaw.

## 2. Base entity, seats and mounting

**What BetterModel builds for a hitbox.** `HitBoxImpl` (nms/v1_21_R3, https://github.com/toxicity188/BetterModel/blob/2.2.0/nms/v1_21_R3/src/main/kotlin/kr/toxicity/model/bukkit/nms/v1_21_R3/HitBoxImpl.kt) is an invisible, silent ArmorStand per hitbox or seat bone (lines 60 to 105). A vanilla `Interaction` entity rides on it (`HitBoxInteraction`, same folder). The player rides the Interaction. Lines 136 to 150 show `mount`, which calls `interaction.addPassenger`.

**The anchor is our entity.** `delegate` is the model's source entity. `mountControl` returns early unless `delegate is LivingEntity` (HitBoxImpl.kt, line 227). So the anchor must be a LivingEntity. Options: an invisible, silent ArmorStand (LivingEntity), or a silent mob with AI off. The model is drawn by item displays, so the anchor itself is never seen. An ArmorStand is my pick, because it has no AI to fight the physics (unverified in game).

**Seat tags.** `SEAT("p")` is "can be used as a seat" and `SUB_SEAT("sp")` is "seat but not controllable" (BoneTags.java, lines 38 to 44). In `RendererGroup`, a `p_` group gets `BetterModel.config().defaultMountController()` and an `sp_` group gets `MountControllers.NONE` (RendererGroup.java, lines 84 to 88). The default controller is WALK (BetterModelConfigImpl.kt, lines 65 to 69, `default-mount-controller`).

**Seat offset.** The seat is the bone's position relative to the anchor (HitBoxImpl `relativePosition`, line 124). So the seat offset is set in the model, not in code.

**The WALK controller does not suit a tank.** While the rider moves, `mountControl` forces `delegate.yRot = player.yRot` (HitBoxImpl.kt, lines 232 to 236). The input is also rotated to the rider's camera yaw (lines 262 to 276). So the hull turns with the look, and the turret cannot point somewhere else. A custom `MountController` can change the returned vector, but not this yaw override (unverified whether a subclass can avoid it; I read the code, not a test).

**Recommended split.** Use BetterModel for rendering, the seat and the hitbox. Give the seat a controller that does not drive the anchor (`MountControllers.NONE`, which has `canControl()` false, `MountControllers.java`). Read the rider's input in our own plugin and move the anchor ourselves. The seat then only does the mounting. `HitBox.mountController(MountController)` setter exists (HitBox.java, api/nms), so this can be set per hitbox (unverified call path at runtime).

**Mount API details.** `MountController` (https://github.com/toxicity188/BetterModel/blob/2.2.0/api/src/main/java/kr/toxicity/model/api/mount/MountController.java) has `canControl()`, `canDismountBySelf()`, `canJump()`, `canFly()` and `canBeDamagedByRider()`, all default methods. `MountController.Modifier` builds variants.

**Events.** `Tracker.listenHitBox(...)` takes a `HitBoxListener.Builder` with `mount`, `dismount`, `interact`, `damage`, `create` and `remove` (Tracker.java, line 630; HitBoxListener.java in api/nms). Sample from the 2.1.0 changelog: `tracker.listenHitBox(...)` with `HitBoxInteractEvent` (https://github.com/toxicity188/BetterModel/blob/2.2.0/changelog/2.1.0.md).

**Vanilla alternative (not chosen).** `Entity.addPassenger` plus our own seat offset. This loses the model bone seat, so it is only a fallback.

## 3. PlayerInputEvent and Player#getCurrentInput()

- `org.bukkit.Input` has `isForward()`, `isBackward()`, `isLeft()`, `isRight()`, `isJump()` and `isSneak()`, each returning boolean. Source: Spigot javadoc via Context7 (https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Input.html). `isSprint()` was not confirmed (unverified).
- `Player.getCurrentInput()` returns the last input (javadoc via Context7, https://hub.spigotmc.org/javadocs/spigot/index-all.html).
- `PlayerInputEvent.getInput()` returns `Input` (javadoc via Context7, https://hub.spigotmc.org/javadocs/spigot/org/bukkit/event/player/PlayerInputEvent.html).
- **Frequency:** unverified. My guess is that it fires when the client sends a changed input, not every tick. Test with a log line.
- **While riding:** BetterModel's own seat logic reads the same client input while a rider sits in the hitbox (HitBoxImpl.kt, `rideInput`, lines 262 to 276; the input comes from the player's last input packet, via `lastClientInput` in nms/v1_21_R4 Functions.kt, lines 123 to 136). So input does arrive while riding (inferred from BetterModel's own use, not tested on Spigot).
- **Sneak dismount:** EntityManager cancels a dismount only when the controller can fly or cannot be dismounted by the rider (EntityManager.kt, line 92). With our `NONE` controller, the default `canDismountBySelf()` is true, so sneaking should dismount, as in vanilla. Unverified in game.
- **Paper-only check:** `PlayerInputEvent`, `Player#getCurrentInput()` and `Input` are listed in the Spigot javadoc through Context7. I did not confirm which Spigot version added them (unverified). Paper's version is not checked here.

## 4. Smooth movement and jitter

- `TrackerUpdateAction.moveDuration(int moveDuration)`, "movement interpolation duration", in ticks (https://github.com/toxicity188/BetterModel/blob/2.2.0/api/src/main/java/kr/toxicity/model/api/tracker/TrackerUpdateAction.java, line 164). This is the closest thing to a teleport duration in the BetterModel API. Apply with `tracker.update(TrackerUpdateAction.moveDuration(n), ...)`.
- Per-bone interpolation comes from the animation frame time: `interpolationDuration()` rounds `state.frame()` to ticks (RenderedBone.java, lines 603 to 607). `skipInterpolation` zeroes it.
- `BetterModelConfig.lerpFrameTime()` (BetterModelConfig.java, line 189) is the global lerp frame time in milliseconds.
- **Jitter:** unverified. The rider's view is driven by the vanilla client, so the anchor's teleport path is the likely source. Plan: move the anchor each tick with a short `moveDuration` (for example 1 to 2 ticks), and test on flat ground and on slopes.

## 5. Left and right clicks while riding

- **Right-click on the hull.** `PlayerInteractEntityEvent` and `PlayerInteractAtEntityEvent` are handled by BetterModel's EntityManager (EntityManager.kt, lines 170 to 190). A right-click on a hitbox calls `triggerInteract`, which fires `HitBoxInteractEvent` through the listener (HitBoxImpl.kt, lines 324 to 327, and `interact`, lines 361 to 372). Our listener can use `Builder.interact(...)`. Note: `interact` returns FAIL unless the listener returns true (HitBoxListener builder code), so our handler must not cancel the event by accident.
- **Left-click by the rider.** `hurtServer` returns false when the attacker is the controlling passenger and `canBeDamagedByRider()` is false (HitBoxImpl.kt, lines 415 to 418). EntityManager also cancels a rider's hit on the vehicle (EntityManager.kt, around line 160). So the default blocks rider left-clicks. Setting `canBeDamagedByRider()` to true on the seat controller lets `HitBoxDamagedEvent` reach our `damage(...)` consumer (unverified in game). Side effect: the damage can reach the anchor (unverified).
- **Alternative for fire:** `PlayerInteractEvent` (LEFT_CLICK_AIR and LEFT_CLICK_BLOCK) and `PlayerAnimationEvent` (ARM_SWING). Unverified whether ARM_SWING fires when a player hits an Interaction or hitbox. Test with a log line.
- Recommendation: use `PlayerInteractEvent` plus `PlayerAnimationEvent`, with a short debounce, and keep BetterModel's `interact` for right-click if needed.

## 6. Free drivable vehicle plugins for Spigot 1.21.4

Result: **none confirmed**. Nothing fits all three of free, Spigot 1.21.4 and a tank. Per the brief, I have not added an M04 section to manual.md.

- **MTVehicles.** A boxtoplay listing says it adds drivable cars, motorcycles, helicopters and tanks, and that it is free: https://www.boxtoplay.com/en/minecraft-hosting/minecraft-server/plugins/spigot-mtvehicles-vehicle-plugin-free-downloadable-43599 . Its SpigotMC page (https://www.spigotmc.org/resources/mtvehicles.84985/ , unverified ID) returned 403, so 1.21.4 support and the licence are **unverified**. This is the one to try if Egor wants a quick hour test.
- **VehiclesPlus.** Setup docs and a Polymart listing mention 1.21 support: https://docs.sbdevelopment.tech/vehiclesplus-v3/setup and https://polymart.org/product/633/vehiclesplus-1-17-1-21/updates . Tank support in the current build and the price are **unverified**.
- **InfiniteVehicles.** Lists 1.21 support, but a review suggests it is paid and I found no tank. Not recommended for this test.

## 7. Recommendation

- **Base entity:** an invisible, silent ArmorStand as the anchor (LivingEntity). BetterModel draws the model with item displays and places its hitboxes around it.
- **Seat:** a `p_driver` bone for the driver and an `sp_gunner` bone for a passenger. Set the seat's controller to `NONE` so BetterModel does not steer the anchor.
- **Movement:** our own plugin reads `Player#getCurrentInput()` (or `PlayerInputEvent`) and moves the anchor each tick. Use `moveDuration` of 1 to 2 ticks. Test jitter and slopes in game.
- **Turret and barrel:** start with built-in head tracking on `h_turret` and check the ±75 clamp in game. If the turret must turn a full 360 degrees, add an `addRotationModifier` with a mutable yaw and pitch field, and check it updates each tick (unverified).
- **Fire:** left-click through `PlayerInteractEvent` and `PlayerAnimationEvent` with a debounce. Right-click through BetterModel's `HitBoxInteractEvent` if needed.
- **Open questions to test first:** the modifier refresh behaviour (section 1), `PlayerInputEvent` frequency and version (section 3), and whether ARM_SWING fires while riding (section 5).

## T16 driving core (as built, not yet tried in game)

- Spawn: `/fmod vehicle spawn tank|jeep`. Remove is owner only (`Access.OWNER_NAME`). List is open to anyone. Anyone may drive a spawned vehicle.
- Driver sits in BetterModel's `p_driver` seat with controller NONE. A vanilla passenger would freeze the anchor's teleport, so the seat is not a passenger in that sense.
- The invisible armour stand anchor is teleported every tick with moveDuration 2.
- Input comes from `Player#getCurrentInput()`, polled each tick.
- Click box is square, so the front and back ends of the tank do not take clicks. Known limit, fix later if it matters.
- If the tank drives backwards, set `Vehicle.MODEL_YAW_OFFSET` to 180.
- Log lines to check in game: three "Driver check:" lines (seated, keys polled, keys by event).
- Still to test: jitter with a second player nearby.
- T17 builds on `Vehicle` and `VehicleRig` in `BetterModelHook`.

## T17 turret (as built, not yet tried in game)

- `TurretModel` (pure) holds turret yaw (relative to the hull, 40 deg/s, shortest way round) and barrel pitch (-5 down to +20 up, 25 deg/s). Pitch is "up positive"; Minecraft pitch is the opposite, so `pitchUpFromMc` flips it.
- `Vehicle.aimTurret(driver)` runs each tick after `move`, using the driver's head yaw against `renderYaw`. With no driver nothing runs, so the turret holds. The jeep has no turret, so it does nothing there.
- Rendering: one rotation modifier on the `turret` bone (rotateY) and one on `barrel` (rotateX), added once at attach in `BetterModelHook.setupTurret`. They read volatile angles.
- Refresh mechanism (a guess): BetterModel only re-applies modifiers when a bone's animation state ticks, which an idle bone never does. `forceRefresh` uses reflection to set the private `firstTick` and `updateAfter` flags on the bone's state each time the angle changes. If it fails, a "Turret check: could not force a bone refresh" warning appears.
- Muzzle: computed by `TurretModel.muzzle` from the anchor position, hull yaw, turret yaw, barrel pitch and the pivots in `tank.bbmodel` (barrel 12 px forward, 21.5 px up; barrel length 32 px). `Vehicle.muzzle()` returns position and unit direction. BetterModel's `muzzle` bone `worldPosition()` is logged once for comparison.
- Sign constants in `BetterModelHook`: `TURRET_YAW_SIGN` and `BARREL_PITCH_SIGN` (both 1). Flip if the turret turns, or the barrel tilts, the wrong way.
- Log lines to check: "Turret check: found the turret and barrel bones" at spawn, and "Turret check: first turn ..." on the first movement.

## T17 turret and barrel aim
- Turret yaw follows the driver's look at 40 degrees per second. Barrel pitch runs -5 to +20 degrees at 25 degrees per second. Logic is in TurretModel (unit tested).
- BetterModel caches the idle bone rotation, so BetterModelHook forces a refresh by reflection. Check the 'Turret check' log lines when a tank spawns.
- If the turret turns the wrong way, flip TURRET_YAW_SIGN or BARREL_PITCH_SIGN in BetterModelHook. Not yet tried in game.
- T18: fire from Vehicle.muzzle() for the shell start point and direction.

## T18 firing (as built, not yet tried in game)
- Left-click while driving a tank fires. `FireListener` listens on three paths, because we don't yet know which reaches the server while riding: `PlayerInteractEvent` (LEFT_CLICK_AIR/BLOCK), `PlayerAnimationEvent` (ARM_SWING) and `EntityDamageByEntityEvent` (a hit on an entity). Interact and hit are cancelled for drivers, block breaking is cancelled too.
- Each path logs once per player: "Fire check: click seen via interact|swing|hit". All three feed `ShellService.fireClick`, which ignores a second click within 5 ticks of the last one, so one click fires once.
- `ShellModel` (pure): 3 blocks per tick, 150 blocks max, last segment shortened. `ShellService` ray-traces each segment with `World.rayTrace` (ray size 0.3, ignores the driver, vehicle entities and non-living entities), then `createExplosion(power 2.5, no fire, breaks blocks, source = driver)`. Otherwise it draws a smoke trail plus flame and crit at the head. A shell ends at max range or when the next chunk isn't loaded.
- Reload: `ReloadTimer` (pure), 80 ticks, one per `Vehicle`. The action bar shows whole seconds left (rounded up), then "ready" once. Keys `reload.seconds.*` and `reload.ready`.
- Constants live in `ShellService` (`RELOAD_TICKS`, `CLICK_DEBOUNCE_TICKS`, `EXPLOSION_POWER`, `SHELL_RAY_SIZE`) and `ShellModel` (`SPEED`, `MAX_RANGE`). Sounds are vanilla (generic explode and large firework blast).
- Vehicle entities are marker/invulnerable and `VehicleListener.onDamage` cancels damage, so explosions should not hurt them. A wall right at the muzzle can still hurt the driver.

## Army truck (T39)
- Speed between the jeep and the tank. Wide turning circle. Health 60. No gun. Eight seats: driver, cab passenger, six in the back. Six wheels spin via WheelSpin.
- Jeep engine sound, pitched lower (soundPitch() on VehicleType, 0.75 for the truck).
- Model comes from scripts/make-vehicle-models.py. Add new models there; the existing models regenerate byte-identical.
- CrewSeats now has PASSENGER1 to PASSENGER7.
- Not tested in game: how the truck looks, the back seat positions, the 0.75-pitch sound, and whether its 2.4-block height fits the command-block pad clearance.
- Gate 4 passed with no notes from Egor.

## Field artillery (T40)
- Doesn't drive. A/D swing the whole gun at 10 degrees a second. The barrel follows the gunner's look, 0 to 45 degrees.
- Shells fly a real arc: speed 3.87 blocks a tick, gravity 0.05, about 300 blocks at 45 degrees, about 5.5 s in the air. Ray-stepped in 1-block chords so they can't skip through walls.
- Blast about tank size. Reload 8 s. Health 50.
- Sounds: cannon_fire on firing. shell_whistle plays once per player when a shell falls within 40 blocks.
- Shells landing in unloaded chunks vanish silently.
- Model: src/main/resources/models/artillery.bbmodel. The pack must include it (T41).
- 244 tests pass. Smoke test loads the model on the test server. Not tried in game.
- G5 checks: trail-leg splay, shell visibility at long range, turning in tight spots.

## Plane (T43, rough, not tried in game)
- `/fmod vehicle spawn plane` (pads work). One pilot seat. Health 30, no boss bar: one shell brings it down. No guns.
- Flight is `FlightModel` (pure, unit tested), not `VehicleMover`. The taxi numbers in `VehicleType.PLANE` are only there for the shared driving tests.
- W/S move the throttle (idle to full in 2 s). A/D bank up to 50 degrees, which turns it at up to 3 degrees a tick. The nose follows the pilot's look, -45 to +35 degrees. Sneak bails out (slow falling for 10 s if 4+ blocks up).
- Speeds (blocks a second): top speed level 20, dive limit 26, take-off 11 (and the pilot must look up 3+ degrees), stall below 8, recovers above 9.6. Take-off run about 15 blocks.
- Climbing costs speed (0.025 x sine of pitch per tick), so a steep climb ends in a stall: the nose drops to -30 and it falls until it is fast again.
- Landing: at most 15 b/s, sinking at most 6 b/s, bank under 30, nose no more than 20 down. Otherwise a crash. Hitting anything taller than 1 block faster than 6 b/s crashes too.
- Crash: a 2.0 explosion (tank shell is 2.5), breaks blocks, no fire. The crew is shielded from the blast and gets the usual 6 damage instead.
- The hit box covers the body and inner wing (3 x 4.4 blocks, 1.9 tall); the wing tips poke past it.
- The plane pauses if the land ahead isn't loaded, rather than crashing into it.
- Model: `plane.bbmodel` from the model script. Bones: `airframe` (banks), `propeller` (spins, on the bank axis), `p_driver`. Whole-model pitch uses BetterModel's default rotator.
- Signs to flip if wrong in game: `Vehicle.PLANE_PITCH_SIGN` (nose tilts the wrong way), `BetterModelHook.PLANE_ROLL_SIGN` (banks the wrong way).
- Pilot gets `setAllowFlight(true)` while seated; the old value comes back on exit, death, quit, removal or shutdown.
- Sounds: `plane_engine_loop` (4 s) while a pilot is in; `plane_flyby` to anyone within 24 blocks of a plane doing 10+ b/s in the air, once per 10 s each.
- Log lines (`VehicleService.FLIGHT_LOG`): the first 12 key events with tick numbers and sprint, first take-off, first landing, each crash with its numbers, the first wait for land, and any kick of vehicle crew.
