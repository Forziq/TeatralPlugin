package dev.egorgrigorciuc.forzammilmod;

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
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;

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

    /** Driver seat bone in our vehicle models (BetterModel "p" tag = seat). */
    static final String SEAT_BONE = "p_driver";
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
    static VehicleRig attachVehicle(LivingEntity anchor, String model, Supplier<Float> yaw) {
        ModelRenderer renderer = BetterModel.modelOrNull(model);
        if (renderer == null) return null;
        EntityTracker tracker = renderer.getOrCreate(BukkitAdapter.adapt(anchor));
        tracker.rotation(() -> new ModelRotation(0f, yaw.get()));
        tracker.update(TrackerUpdateAction.moveDuration(MOVE_TICKS));
        return new BmRig(tracker);
    }

    /**
     * BetterModel builds the seat (an invisible armour stand with an Interaction on it) a tick after the tracker,
     * and moves it with the anchor. We switch its controller to NONE so BetterModel never steers the anchor or
     * turns it to the rider's look; our tick loop does the driving.
     */
    private record BmRig(EntityTracker tracker) implements VehicleRig {

        private HitBox seatBox() {
            if (tracker.isClosed()) return null;
            RenderedBone bone = tracker.bone(SEAT_BONE);
            return bone == null ? null : bone.getHitBox();
        }

        @Override
        public boolean seat(Player player) {
            HitBox box = seatBox();
            if (box == null) return false;
            box.mountController(MountControllers.NONE);
            box.mount(BukkitAdapter.adapt(player));
            return isSeated(player);
        }

        @Override
        public boolean isSeated(Player player) {
            Entity vehicle = player.getVehicle();
            if (vehicle == null) return false;
            HitBox box = seatBox();
            if (box == null) return false;
            UUID seatId = box.uuid();
            // The player rides the seat's Interaction, which rides the seat armour stand.
            if (seatId.equals(vehicle.getUniqueId())) return true;
            Entity below = vehicle.getVehicle();
            return below != null && seatId.equals(below.getUniqueId());
        }

        @Override
        public void unseat(Player player) {
            HitBox box = seatBox();
            if (box != null) box.dismount(BukkitAdapter.adapt(player));
            if (player.isInsideVehicle() && isSeated(player)) player.leaveVehicle();
        }

        @Override
        public void close() {
            HitBox box = seatBox();
            if (box != null) box.dismountAll();
            tracker.close();
        }
    }
}
