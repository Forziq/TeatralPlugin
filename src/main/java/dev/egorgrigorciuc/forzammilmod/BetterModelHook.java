package dev.egorgrigorciuc.forzammilmod;

import dev.egorgrigorciuc.forzammilmod.CrewSeats.Seat;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.BetterModelPlatform.ReloadResult;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.data.renderer.ModelRenderer;
import kr.toxicity.model.api.mount.MountControllers;
import kr.toxicity.model.api.nms.HitBox;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.tracker.ModelRotation;
import kr.toxicity.model.api.tracker.TrackerUpdateAction;
import org.joml.Quaternionf;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.reflect.Field;

/**
 * The only class that touches the BetterModel API. Only load it after checking BetterModel is enabled,
 * otherwise its imports fail to resolve.
 */
final class BetterModelHook {

    /** Player-animation models we ship; BetterModel names a model after its file name. */
    static final List<String> PLAYER_MODELS = List.of("forzammil_player");

    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_TICKS = 100L; // 5 s

    private BetterModelHook() {}

    /** Logs after every BetterModel reload (including its own first load) whether our player models are present. */
    static void logReloads(JavaPlugin plugin) {
        BetterModel.platform().addReloadEndHandler(result -> {
            if (!plugin.isEnabled() || !(result instanceof ReloadResult.Success)) return;
            var keys = BetterModel.limbKeys();
            for (String name : PLAYER_MODELS) {
                if (keys.contains(name)) {
                    plugin.getLogger().info("BetterModel has loaded player model " + name);
                } else {
                    plugin.getLogger().warning("BetterModel did not load player model " + name + "; check its log above");
                }
            }
        });
    }

    /** Runs one BetterModel reload off the main thread; retries a few times if one is already running. */
    static void reloadAsync(JavaPlugin plugin) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> attempt(plugin, 1));
    }

    private static void attempt(JavaPlugin plugin, int n) {
        if (!plugin.isEnabled()) return;
        ReloadResult result;
        try {
            result = BetterModel.platform().reload();
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.WARNING, "BetterModel reload threw an error", e);
            return;
        }
        if (result instanceof ReloadResult.Success s) {
            plugin.getLogger().info("BetterModel reloaded with our models in " + s.totalTime() + " ms");
        } else if (result instanceof ReloadResult.OnReload) {
            if (n < MAX_ATTEMPTS) {
                plugin.getLogger().info("BetterModel is already reloading; trying again in 5 seconds");
                plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> attempt(plugin, n + 1), RETRY_TICKS);
            } else {
                plugin.getLogger().warning("BetterModel stayed busy; run /bm reload to pick up our models");
            }
        } else if (result instanceof ReloadResult.Failure f) {
            plugin.getLogger().log(Level.WARNING, "BetterModel reload failed", f.throwable());
        }
    }

    /**
     * Starts a play-once animation on the player's model. Returns a handle (close it to stop the emote and
     * give the player their normal look back), or null if the model or animation is unavailable.
     * {@code onFinish} may run on any thread.
     */
    static AutoCloseable play(Player player, String animation, Runnable onFinish) {
        EntityTracker tracker = BetterModel.limb(PLAYER_MODELS.get(0))
                .map(r -> r.getOrCreate(BukkitAdapter.adapt(player))).orElse(null);
        if (tracker == null) return null;
        if (!tracker.animate(animation, AnimationModifier.DEFAULT_WITH_PLAY_ONCE, onFinish)) {
            tracker.close();
            return null;
        }
        return tracker;
    }

    // ---- vehicles ----

    // Seat bones: the groups are named p_driver (driver), sp_gunner, sp_passenger1 and sp_passenger2 in the
    // models ("p" tag = seat, "sp" tag = sub seat), but BetterModel strips tags off the name, so lookups use the
    // bare names in CrewSeats.Seat.bone.
    /** Display movement interpolation in ticks, per docs/notes/vehicles.md section 4. */
    static final int MOVE_TICKS = 2;

    /** True if BetterModel has loaded a model with this name. */
    static boolean hasModel(String model) {
        return BetterModel.modelOrNull(model) != null;
    }

    /**
     * Draws {@code model} on the anchor entity, facing {@code yaw} (read from BetterModel's own thread, so the
     * supplier must be thread-safe). Returns null if the model isn't loaded.
     */
    static VehicleRig attachVehicle(LivingEntity anchor, String model, Supplier<Float> yaw, Logger log) {
        ModelRenderer renderer = BetterModel.modelOrNull(model);
        if (renderer == null) return null;
        EntityTracker tracker = renderer.getOrCreate(BukkitAdapter.adapt(anchor));
        tracker.rotation(() -> new ModelRotation(0f, yaw.get()));
        tracker.update(TrackerUpdateAction.moveDuration(MOVE_TICKS));
        return new BmRig(tracker, setupTurret(tracker, log), setupWheels(tracker, log), log);
    }

    /** Flip to -1f if the turret turns the wrong way in game. */
    static final float TURRET_YAW_SIGN = 1f;
    /** Flip to -1f if the barrel tilts the wrong way in game. */
    static final float BARREL_PITCH_SIGN = -1f;

    /** Flip to -1f if the wheels roll backwards in game. */
    static final float WHEEL_SPIN_SIGN = 1f;
    private static final String[] WHEEL_BONES = {"wheel_fl", "wheel_fr", "wheel_rl", "wheel_rr"};

    /** Roll angle the wheel modifiers read. */
    private static final class Wheels {
        volatile float angle;

        Quaternionf rotation() {
            return new Quaternionf().rotateX((float) Math.toRadians(angle * WHEEL_SPIN_SIGN));
        }
    }

    /** Adds a roll modifier to each wheel bone. Returns null (and does nothing) if any of the four is missing. */
    private static Wheels setupWheels(EntityTracker tracker, Logger log) {
        RenderedBone[] bones = new RenderedBone[WHEEL_BONES.length];
        for (int i = 0; i < bones.length; i++) {
            bones[i] = tracker.bone(WHEEL_BONES[i]);
            if (bones[i] == null) {
                if (i > 0) log.info("Wheel check: wheel bone " + WHEEL_BONES[i] + " missing, no wheel spin");
                return null;
            }
        }
        Wheels w = new Wheels();
        for (RenderedBone b : bones) {
            // Forward is -Z, so wheels roll about X.
            b.addRotationModifier(x -> true, q -> new Quaternionf(q).mul(w.rotation()));
        }
        log.info("Wheel check: found 4 wheel bones, spin added");
        return w;
    }

    /** Angles the bone modifiers read; written by the main thread, read by BetterModel's thread. */
    private static final class Angles {
        volatile float yaw;
        volatile float pitch;

        /** MC yaw grows clockwise seen from above, a positive rotateY turns anticlockwise, hence the minus. */
        Quaternionf turret() {
            return new Quaternionf().rotateY((float) Math.toRadians(-yaw * TURRET_YAW_SIGN));
        }

        /**
         * BetterModel does not pass a parent's modifier rotation on to its children (it divides it back out), so
         * the barrel carries the turret's turn itself, then tilts about its own X axis (it points -Z, so a
         * positive rotateX raises it).
         */
        Quaternionf barrel() {
            return turret().rotateX((float) Math.toRadians(pitch * BARREL_PITCH_SIGN));
        }
    }

    /**
     * Adds one rotation modifier to the turret bone (spin about the vertical axis) and one to the barrel bone
     * (tilt about its local X axis). They read {@link Angles} and are added once, because modifiers stack.
     * Returns null if the model has no turret bone.
     */
    private static Angles setupTurret(EntityTracker tracker, Logger log) {
        RenderedBone turret = tracker.bone("turret");
        RenderedBone barrel = tracker.bone("barrel");
        if (turret == null || barrel == null) return null;
        Angles a = new Angles();
        turret.addRotationModifier(b -> true, q -> new Quaternionf(q).mul(a.turret()));
        barrel.addRotationModifier(b -> true, q -> new Quaternionf(q).mul(a.barrel()));
        log.info("Turret check: found the turret and barrel bones, rotation modifiers added");
        return a;
    }

    // BetterModel 2.2.0 only re-runs a bone's rotation modifier while an animation is playing on it; with none
    // running (interpolation 0) it reuses the cached RenderedBone.lastModifiedRotation and never sends a new
    // transform. So we write that cache ourselves and set the bone's firstTick flag, which makes the next
    // BetterModel tick send the bone again. Do NOT touch updateAfter: tick() must flip it false -> true itself,
    // or it skips sending (that was the T17 bug). This reaches into BetterModel internals; re-check after updates.
    private static Field fLastRotation, fGlobalState, fFirstTick;
    private static boolean refreshBroken;

    private static void forceRotation(RenderedBone bone, Quaternionf rotation, Logger log) {
        if (refreshBroken || bone == null) return;
        try {
            if (fLastRotation == null) {
                fLastRotation = RenderedBone.class.getDeclaredField("lastModifiedRotation");
                fLastRotation.setAccessible(true);
                fGlobalState = RenderedBone.class.getDeclaredField("globalState");
                fGlobalState.setAccessible(true);
            }
            Object handler = fGlobalState.get(bone);
            if (fFirstTick == null) {
                fFirstTick = handler.getClass().getDeclaredField("firstTick");
                fFirstTick.setAccessible(true);
            }
            fLastRotation.set(bone, rotation);
            fFirstTick.setBoolean(handler, true);
        } catch (ReflectiveOperationException | RuntimeException e) {
            refreshBroken = true;
            log.warning("Turret check: could not force a bone refresh (" + e + "); the turret and wheels may stay still");
        }
    }

    /**
     * BetterModel builds the seat (an invisible armour stand with an Interaction on it) a tick after the tracker,
     * and moves it with the anchor. We switch its controller to NONE so BetterModel never steers the anchor or
     * turns it to the rider's look; our tick loop does the driving.
     */
    private record BmRig(EntityTracker tracker, Angles angles, Wheels wheels, Logger log, boolean[] logged) implements VehicleRig {

        BmRig(EntityTracker tracker, Angles angles, Wheels wheels, Logger log) {
            this(tracker, angles, wheels, log, new boolean[2]);
        }

        private HitBox seatBox(Seat seat) {
            if (tracker.isClosed()) return null;
            RenderedBone bone = tracker.bone(seat.bone);
            return bone == null ? null : bone.getHitBox();
        }

        @Override
        public boolean seat(Player player, Seat seat) {
            HitBox box = seatBox(seat);
            if (box == null) {
                if (!logged[1]) {
                    logged[1] = true;
                    String why = tracker.isClosed() ? "the model is closed"
                            : tracker.bone(seat.bone) == null ? "the model has no " + seat.bone + " seat bone"
                            : "BetterModel has not built the seat yet";
                    log.warning("Seat check: could not seat " + player.getName() + " as " + seat + ": " + why);
                }
                return false;
            }
            box.mountController(MountControllers.NONE);
            box.mount(BukkitAdapter.adapt(player));
            boolean seated = isSeated(player, seat);
            if (!seated && !logged[1]) {
                logged[1] = true;
                log.warning("Seat check: mounted " + player.getName() + " on the " + seat + " seat but they are not riding it"
                        + " (riding " + player.getVehicle() + ")");
            }
            return seated;
        }

        @Override
        public boolean isSeated(Player player, Seat seat) {
            Entity vehicle = player.getVehicle();
            if (vehicle == null) return false;
            HitBox box = seatBox(seat);
            if (box == null) return false;
            UUID seatId = box.uuid();
            // The player rides the seat's Interaction, which rides the seat armour stand.
            if (seatId.equals(vehicle.getUniqueId())) return true;
            Entity below = vehicle.getVehicle();
            return below != null && seatId.equals(below.getUniqueId());
        }

        @Override
        public void unseat(Player player, Seat seat) {
            HitBox box = seatBox(seat);
            if (box != null) box.dismount(BukkitAdapter.adapt(player));
            if (player.isInsideVehicle() && isSeated(player, seat)) player.leaveVehicle();
        }

        @Override
        public void wheels(float angleDeg) {
            if (wheels == null || tracker.isClosed()) return;
            wheels.angle = angleDeg;
            for (String name : WHEEL_BONES) forceRotation(tracker.bone(name), wheels.rotation(), log);
        }

        @Override
        public boolean hasTurret() {
            return angles != null;
        }

        @Override
        public void turret(float yawRel, float pitchUp) {
            if (angles == null || tracker.isClosed()) return;
            if (Math.abs(angles.yaw - yawRel) < 0.01f && Math.abs(angles.pitch - pitchUp) < 0.01f) return;
            angles.yaw = yawRel;
            angles.pitch = pitchUp;
            forceRotation(tracker.bone("turret"), angles.turret(), log);
            forceRotation(tracker.bone("barrel"), angles.barrel(), log);
            if (!logged[0]) {
                logged[0] = true;
                RenderedBone muzzle = tracker.bone("muzzle");
                log.info("Turret check: first turn, turret " + yawRel + ", barrel " + pitchUp
                        + ", muzzle bone at " + (muzzle == null ? "missing" : muzzle.worldPosition().toString())
                        + " (compare with the computed muzzle; look at the tank to see if the turret moved)");
            }
        }

        @Override
        public void close() {
            for (Seat seat : Seat.values()) {
                HitBox box = seatBox(seat);
                if (box != null) box.dismountAll();
            }
            tracker.close();
        }
    }
}
